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
 * Service de génération de quiz via l'API Groq (groq.com).
 *
 * Groq expose une API compatible OpenAI très rapide :
 *   POST https://api.groq.com/openai/v1/chat/completions
 *   Authorization: Bearer <API_KEY>
 */
public class GrokQuizService {

    private static final String GROK_API_URL  = "https://api.groq.com/openai/v1/chat/completions";
    // llama-3.3-70b-versatile : rapide, gratuit, très bon pour la génération structurée
    private static final String GROK_MODEL    = "llama-3.3-70b-versatile";
    private static final int    MAX_RETRIES   = 3;
    private static final int    PDF_MAX_CHARS = 80_000;


    // ── Extraction PDF ────────────────────────────────────────────────────────

    public String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    // ── Génération du Quiz ────────────────────────────────────────────────────

    /**
     * Extrait le texte du PDF, l'envoie à Grok et retourne un JSON de questions.
     *
     * @param pdfFile     Fichier PDF du cours
     * @param apiKey      Clé API Groq
     * @param nbQuestions Nombre de questions à générer
     * @return String JSON : tableau de questions QCM
     */
    public String generateQuiz(File pdfFile, String apiKey, int nbQuestions) throws Exception {

        // 1. Extraction du texte
        String pdfText = extractTextFromPdf(pdfFile);
        if (pdfText == null || pdfText.isBlank()) {
            throw new Exception("Le PDF ne contient pas de texte extractible.\n"
                    + "Utilisez un PDF avec du texte sélectionnable (pas un scan).");
        }
        if (pdfText.length() > PDF_MAX_CHARS) {
            pdfText = pdfText.substring(0, PDF_MAX_CHARS);
        }

        // 2. Construction du prompt
        String prompt = String.format(
                "Tu es un professeur expert. Génère exactement %d questions QCM basées "
                        + "UNIQUEMENT sur ce cours.\n\n"
                        + "=== COURS ===\n%s\n=== FIN ===\n\n"
                        + "Règles : 4 options par question, 1 bonne réponse (index 0-3), "
                        + "répondre UNIQUEMENT en JSON brut sans markdown.\n\n"
                        + "Format attendu :\n"
                        + "[{\"question\":\"?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],"
                        + "\"correctAnswerIndex\":0,\"explanation\":\"...\"}]",
                nbQuestions, pdfText
        );

        // 3. Construction du body (format OpenAI / Grok)
        String requestBody = new JSONObject()
                .put("model", GROK_MODEL)
                .put("messages", new JSONArray().put(
                        new JSONObject()
                                .put("role", "user")
                                .put("content", prompt)))
                .put("temperature", 0.4)
                .put("max_tokens", 4096)
                .toString();

        // 4. Client HTTP
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // 5. Retry automatique
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROK_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            System.out.printf("[GrokQuizService] HTTP %d (tentative %d/%d)%n", status, attempt, MAX_RETRIES);

            if (status == 200) {
                return extractJsonFromResponse(response.body());
            }

            if (status == 429) {
                int waitSeconds = extractRetryDelay(response.body());
                System.out.printf("[GrokQuizService] Quota dépassé — attente %ds...%n", waitSeconds);
                if (attempt >= MAX_RETRIES) {
                    throw new Exception("Quota Grok dépassé après " + MAX_RETRIES + " tentatives.\n"
                            + "Réessayez dans quelques instants.");
                }
                Thread.sleep(waitSeconds * 1000L);
                continue;
            }

            if (status == 401 || status == 403) {
                throw new Exception("Clé API Grok invalide (HTTP " + status + ").\n"
                        + "Vérifiez votre clé sur https://console.x.ai/");
            }

            // Autre erreur
            lastException = new Exception("Erreur API Grok (HTTP " + status + ") : "
                    + truncate(response.body(), 300));
            break;
        }

        throw lastException != null ? lastException : new Exception("Génération échouée.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Extrait le contenu JSON de la réponse OpenAI-compatible de Grok. */
    private String extractJsonFromResponse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new Exception("Grok n'a retourné aucun résultat. Réessayez.");
        }
        String raw = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
        return cleanJson(raw);
    }

    /** Nettoie les balises Markdown éventuelles autour du JSON. */
    private String cleanJson(String raw) throws Exception {
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```"))  s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        s = s.trim();

        // Chercher le tableau JSON si la réponse contient du texte autour
        if (!s.startsWith("[")) {
            int start = s.indexOf('['), end = s.lastIndexOf(']');
            if (start != -1 && end > start) s = s.substring(start, end + 1);
            else throw new Exception("Réponse non JSON : " + truncate(s, 200));
        }

        // Validation finale
        try { new JSONArray(s); }
        catch (Exception e) { throw new Exception("JSON invalide retourné par Grok : " + e.getMessage()); }

        return s;
    }

    private int extractRetryDelay(String body) {
        try {
            JSONObject json = new JSONObject(body);
            // Certains fournisseurs utilisent "retry_after" dans le corps
            if (json.has("retry_after")) return json.getInt("retry_after") + 2;
        } catch (Exception ignored) {}
        return 30;
    }

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max) + "…");
    }
}
