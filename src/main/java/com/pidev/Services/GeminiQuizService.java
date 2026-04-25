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
import java.util.ArrayList;
import java.util.List;

/**
 * Service gérant la génération de Quiz via l'Intelligence Artificielle (Gemini API)
 * à partir d'un fichier de cours au format PDF.
 */
public class GeminiQuizService {

    // On utilise le modèle gemini-1.5-flash qui est très rapide et adapté à ce besoin.
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    /**
     * 1. Extrait le texte brut d'un fichier PDF donné.
     * 
     * @param pdfFile Le fichier PDF importé par le professeur.
     * @return Le texte contenu dans le PDF.
     */
    public String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    /**
     * 2. Envoie le texte du PDF à Gemini pour générer un Quiz structuré (JSON).
     * 
     * @param pdfFile Le fichier PDF du cours.
     * @param apiKey  La clé d'API Google Gemini.
     * @param nbQuestions Le nombre de questions à générer.
     * @return Une chaîne de caractères JSON contenant les questions générées.
     */
    public String generateQuiz(File pdfFile, String apiKey, int nbQuestions) throws Exception {
        // --- Étape 1 : Extraction du texte ---
        String pdfText = extractTextFromPdf(pdfFile);
        
        // Sécurité : limiter la taille du texte pour ne pas dépasser les tokens max de l'API
        // 100 000 caractères représentent un très gros document.
        if (pdfText.length() > 100000) {
            pdfText = pdfText.substring(0, 100000);
        }

        // --- Étape 2 : Construction du Prompt pour l'IA ---
        String prompt = "Tu es un professeur expert. Génère un quiz pertinent de " + nbQuestions + " questions à choix multiples basé STRICTEMENT sur le cours suivant.\n\n" +
                "--- DEBUT DU COURS ---\n" +
                pdfText + "\n" +
                "--- FIN DU COURS ---\n\n" +
                "Règles :\n" +
                "- Chaque question doit avoir 4 options.\n" +
                "- L'index de la bonne réponse doit être 0, 1, 2 ou 3.\n" +
                "- Renvoie UNIQUEMENT un tableau JSON valide. Pas de markdown (```json ... ```), juste le JSON brut.\n" +
                "\nFormat attendu :\n" +
                "[\n" +
                "  {\n" +
                "    \"question\": \"Le texte de la question ?\",\n" +
                "    \"options\": [\"Choix A\", \"Choix B\", \"Choix C\", \"Choix D\"],\n" +
                "    \"correctAnswerIndex\": 2,\n" +
                "    \"explanation\": \"Explication de la réponse\"\n" +
                "  }\n" +
                "]";

        // --- Étape 3 : Création du payload (Body HTTP) ---
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        // Forcer Gemini à renvoyer un JSON strict
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("response_mime_type", "application/json");
        requestBody.put("generationConfig", generationConfig);

        // --- Étape 4 : Appel HTTP à l'API Gemini ---
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // --- Étape 5 : Traitement de la réponse ---
        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject candidateContent = firstCandidate.getJSONObject("content");
                JSONArray candidateParts = candidateContent.getJSONArray("parts");
                if (candidateParts.length() > 0) {
                    // C'est ici qu'on récupère le JSON brut généré par l'IA
                    String generatedJson = candidateParts.getJSONObject(0).getString("text");
                    return generatedJson; 
                }
            }
            throw new Exception("Format de réponse inattendu de l'API Gemini.");
        } else {
            throw new Exception("Erreur API Gemini (Code " + response.statusCode() + ") : " + response.body());
        }
    }
}
