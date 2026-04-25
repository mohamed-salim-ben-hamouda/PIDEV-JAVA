package com.pidev.Services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service de génération de quiz via Gemini API.
 * Gère automatiquement les erreurs 429 (quota dépassé) en attendant
 * le délai suggéré par l'API avant de réessayer.
 */
public class GeminiQuizService {

    private static final String[][] MODEL_ENDPOINTS = {
            { "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=",        "gemini-2.0-flash" },
            { "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=",        "gemini-1.5-flash" },
            { "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=", "gemini-1.5-flash-latest" },
            { "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=",   "gemini-2.0-flash-lite" },
            { "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=",            "v1/gemini-1.5-flash" },
    };

    // Nombre max de retries sur 429 avant d'abandonner
    private static final int MAX_429_RETRIES = 3;
    private static final int PDF_MAX_CHARS   = 80_000;

    // Callback optionnel pour notifier l'UI de l'attente en cours
    // Peut être null si non utilisé
    public interface ProgressCallback {
        void onWaiting(String modelName, int waitSeconds, int attempt, int maxAttempts);
    }

    private ProgressCallback progressCallback;

    public void setProgressCallback(ProgressCallback cb) {
        this.progressCallback = cb;
    }

    public String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    public String generateQuiz(File pdfFile, String apiKey, int nbQuestions) throws Exception {

        // 1. Extraction PDF
        String pdfText = extractTextFromPdf(pdfFile);
        if (pdfText == null || pdfText.isBlank()) {
            throw new Exception(
                    "Le PDF ne contient pas de texte extractible.\n"
                            + "Utilisez un PDF avec du texte sélectionnable (pas un scan d'image)."
            );
        }
        if (pdfText.length() > PDF_MAX_CHARS) {
            pdfText = pdfText.substring(0, PDF_MAX_CHARS);
        }

        String requestBody = buildRequestBody(buildPrompt(nbQuestions, pdfText));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // 2. Essai de chaque modèle
        String lastError = "";
        for (String[] endpoint : MODEL_ENDPOINTS) {
            String url  = endpoint[0] + apiKey;
            String name = endpoint[1];

            System.out.println("[Gemini] Tentative modèle : " + name);

            String result = tryModelWithRetry(client, url, name, requestBody);

            if (result != null) {
                return result; // Succès
            }

            // null = 404, on passe au modèle suivant
            lastError = "Modèle " + name + " non disponible.";
        }

        throw new Exception(
                "Aucun modèle Gemini disponible.\n\n"
                        + "Vérifiez vos modèles disponibles dans un navigateur :\n"
                        + "https://generativelanguage.googleapis.com/v1beta/models?key=VOTRE_CLE"
        );
    }

    /**
     * Tente d'appeler un modèle avec retry automatique sur 429.
     * @return JSON string si succès, null si 404 (modèle absent)
     * @throws Exception sur erreur métier (400, 403, JSON invalide)
     */
    private String tryModelWithRetry(HttpClient client, String url, String modelName,
                                     String body) throws Exception {
        for (int attempt = 1; attempt <= MAX_429_RETRIES; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("[Gemini] Erreur réseau : " + e.getMessage());
                return null;
            }

            int status = response.statusCode();
            System.out.printf("[Gemini] %s → HTTP %d (tentative %d/%d)%n",
                    modelName, status, attempt, MAX_429_RETRIES);

            switch (status) {
                case 200 -> { return extractJsonFromResponse(response.body()); }
                case 404 -> { return null; } // Modèle absent → essayer le suivant

                case 429 -> {
                    int waitSec = extractRetryDelay(response.body());
                    System.out.printf("[Gemini] Quota dépassé — attente de %ds avant retry%n", waitSec);

                    if (progressCallback != null) {
                        progressCallback.onWaiting(modelName, waitSec, attempt, MAX_429_RETRIES);
                    }

                    if (attempt >= MAX_429_RETRIES) {
                        throw new Exception(
                                "Quota Gemini dépassé après " + MAX_429_RETRIES + " tentatives.\n\n"
                                        + "Solutions :\n"
                                        + "  1. Attendez quelques minutes et réessayez.\n"
                                        + "  2. Réduisez le nombre de questions (3-5 au lieu de 10+).\n"
                                        + "  3. Obtenez une clé avec quota étendu sur https://aistudio.google.com/app/apikey"
                        );
                    }

                    // Attente avec countdown dans la console
                    for (int s = waitSec; s > 0; s--) {
                        System.out.print("\r[Gemini] Reprise dans " + s + "s...   ");
                        Thread.sleep(1000);
                    }
                    System.out.println("\r[Gemini] Reprise maintenant.         ");
                }

                case 400 -> throw new Exception(
                        "Clé API invalide (400).\n"
                                + "Vérifiez votre clé dans GenerateQuizAIController.java\n"
                                + "Obtenez une clé sur : https://aistudio.google.com/app/apikey\n\n"
                                + "Détail API : " + extractApiErrorMessage(response.body())
                );

                case 403 -> throw new Exception(
                        "Accès refusé (403).\n"
                                + "Activez l'API 'Generative Language API' dans Google Cloud Console :\n"
                                + "https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com"
                );

                default -> {
                    System.err.println("[Gemini] Erreur inattendue " + status
                            + " : " + truncate(response.body(), 150));
                    return null;
                }
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String buildPrompt(int n, String text) {
        return String.format(
                "Tu es un professeur expert. Génère exactement %d questions QCM basées "
                        + "UNIQUEMENT sur ce cours.\n\n"
                        + "=== COURS ===\n%s\n=== FIN ===\n\n"
                        + "Règles : 4 options par question, 1 bonne réponse (index 0-3), "
                        + "répondre UNIQUEMENT en JSON brut sans markdown.\n\n"
                        + "Format :\n"
                        + "[{\"question\":\"?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],"
                        + "\"correctAnswerIndex\":0,\"explanation\":\"...\"}]",
                n, text
        );
    }

    private String buildRequestBody(String prompt) {
        return new JSONObject()
                .put("contents", new JSONArray().put(
                        new JSONObject().put("parts", new JSONArray().put(
                                new JSONObject().put("text", prompt)))))
                .put("generationConfig", new JSONObject()
                        .put("temperature", 0.4)
                        .put("maxOutputTokens", 4096))
                .toString();
    }

    private String extractJsonFromResponse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        if (!root.has("candidates") || root.getJSONArray("candidates").isEmpty()) {
            throw new Exception("Gemini n'a retourné aucun résultat. Essayez avec un PDF différent.");
        }
        JSONObject candidate = root.getJSONArray("candidates").getJSONObject(0);
        if ("SAFETY".equals(candidate.optString("finishReason", ""))) {
            throw new Exception("Contenu bloqué par les filtres de sécurité Gemini.");
        }
        String raw = candidate
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text").trim();
        return cleanJson(raw);
    }

    private String cleanJson(String raw) throws Exception {
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        s = s.trim();
        if (!s.startsWith("[")) {
            int start = s.indexOf('['), end = s.lastIndexOf(']');
            if (start != -1 && end > start) s = s.substring(start, end + 1);
            else throw new Exception("Réponse non JSON : " + truncate(s, 200));
        }
        try { new JSONArray(s); }
        catch (Exception e) { throw new Exception("JSON invalide : " + e.getMessage()); }
        return s;
    }

    private String extractApiErrorMessage(String body) {
        try { return new JSONObject(body).getJSONObject("error").optString("message", body); }
        catch (Exception e) { return truncate(body, 120); }
    }

    private int extractRetryDelay(String body) {
        try {
            // Essayer d'abord le champ JSON retryDelay
            JSONObject json = new JSONObject(body);
            if (json.has("error")) {
                JSONObject error = json.getJSONObject("error");
                if (error.has("details")) {
                    JSONArray details = error.getJSONArray("details");
                    for (int i = 0; i < details.length(); i++) {
                        JSONObject detail = details.getJSONObject(i);
                        if (detail.has("retryDelay")) {
                            String delay = detail.getString("retryDelay"); // ex: "23s"
                            String digits = delay.replaceAll("[^0-9]", "");
                            if (!digits.isEmpty()) return Integer.parseInt(digits) + 3;
                        }
                    }
                }
            }
            // Fallback : cherche "retryDelay" dans le texte brut
            int idx = body.indexOf("retryDelay");
            if (idx != -1) {
                String digits = body.substring(idx + 13, Math.min(idx + 25, body.length()))
                        .replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) return Integer.parseInt(digits) + 3;
            }
        } catch (Exception ignored) {}
        return 30;
    }

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max) + "…");
    }
}