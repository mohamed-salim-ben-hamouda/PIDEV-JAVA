package com.pidev.Services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HuggingFaceService {

    // IMPORTANT: Remplacez cette clé par votre propre Access Token Hugging Face
    // Vous pouvez obtenir un token gratuit sur https://huggingface.co/settings/tokens
    private static final String API_TOKEN = "";
    
    private static final String VISION_MODEL_URL = "https://api-inference.huggingface.co/models/Salesforce/blip-image-captioning-base";
    private static final String GENERATION_MODEL_URL = "https://router.huggingface.co/hf-inference/models/black-forest-labs/FLUX.1-schnell";

    private HttpClient client;

    public HuggingFaceService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    /**
     * Analyse l'image et retourne une description textuelle générée par l'IA (Image Captioning).
     */
    public CompletableFuture<String> analyzeImage(File imageFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(VISION_MODEL_URL))
                        .header("Authorization", "Bearer " + API_TOKEN)
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(fileContent))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
                    if (jsonArray.size() > 0) {
                        JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
                        String caption = jsonObject.get("generated_text").getAsString();
                        System.out.println("--- VISION IA : " + caption);
                        return caption;
                    }
                    System.err.println("Le modèle IA (Vision) est en cours de démarrage ou temporairement indisponible. Utilisation de la description par défaut.");
                    return "A beautiful person portrait, unique style " + System.currentTimeMillis();
                } else {
                    System.err.println("Erreur API Vision: HTTP " + response.statusCode() + " - " + response.body());
                    return "A beautiful person portrait, variation " + System.currentTimeMillis();
                }
            } catch (Exception e) {
                System.err.println("Échec de l'analyse de l'image: " + e.getMessage());
                return "A beautiful person portrait, random " + System.currentTimeMillis();
            }
        });
    }

    /**
     * Génère une image à partir d'un texte et la sauvegarde sur le disque.
     */
    public CompletableFuture<File> generateImage(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String finalPrompt = (prompt == null) ? "A beautiful person portrait" : prompt;
                
                // Ajouter des mots clés pour améliorer le style de l'avatar
                String enhancedPrompt = "A highly detailed, professional, beautiful digital art portrait avatar of " + finalPrompt + ", vibrant colors, masterpiece, 8k resolution, stylized, trending on artstation";
                
                JsonObject payload = new JsonObject();
                payload.addProperty("inputs", enhancedPrompt);
                
                JsonObject options = new JsonObject();
                options.addProperty("wait_for_model", true);
                payload.add("options", options);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GENERATION_MODEL_URL))
                        .header("Authorization", "Bearer " + API_TOKEN)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                if (client == null) {
                    throw new RuntimeException("HttpClient n'est pas initialisé.");
                }

                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response == null) {
                    throw new RuntimeException("L'API n'a retourné aucune réponse (null).");
                }

                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    byte[] body = response.body();
                    if (body == null || body.length == 0) {
                        throw new RuntimeException("L'API a retourné un corps vide.");
                    }

                    String projectPath = System.getProperty("user.dir");
                    File uploadDir = new File(projectPath + "/src/main/resources/images/profiles");
                    if (!uploadDir.exists()) uploadDir.mkdirs();

                    File outputFile = new File(uploadDir, "ai_avatar_" + System.currentTimeMillis() + ".png");
                    Files.write(outputFile.toPath(), body, StandardOpenOption.CREATE);
                    return outputFile;
                } else if (statusCode == 503) {
                    throw new RuntimeException("Le modèle de génération d'image est en cours de démarrage (Cold Start). Veuillez patienter environ 30 secondes et réessayer.");
                } else if (statusCode == 404) {
                    String errorBody = (response.body() != null) ? new String(response.body()) : "Pas de corps de réponse";
                    throw new RuntimeException("Erreur 404 : Le modèle n'est pas disponible à l'adresse : " + GENERATION_MODEL_URL + " - Détails : " + errorBody);
                } else if (statusCode == 401) {
                    throw new RuntimeException("Erreur 401 : Token API Hugging Face invalide ou expiré.");
                } else {
                    String errorBody = (response.body() != null) ? new String(response.body()) : "Pas de corps de réponse";
                    throw new RuntimeException("Erreur API Génération: HTTP " + statusCode + " - " + errorBody);
                }
            } catch (Exception e) {
                e.printStackTrace(); // Print full stack trace to console
                String msg = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
                throw new RuntimeException("Échec de la génération de l'image: " + msg, e);
            }
        });
    }


}
