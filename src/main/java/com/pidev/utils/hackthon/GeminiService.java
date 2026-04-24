package com.pidev.utils.hackthon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GeminiService {

    private static final String API_KEY = "xxxxxxxxxxxxxxxxxx";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            + API_KEY;

    /**
     * Demande un conseil à l'IA Gemini pour un hackathon spécifique.
     * 
     * @param hackathonTitle Le titre du hackathon
     * @param hackathonTheme Le thème
     * @return Un CompletableFuture contenant la réponse de l'IA
     */
    public static CompletableFuture<String> getAdvice(String hackathonTitle, String hackathonTheme) {
        String prompt = "Donne-moi 3 conseils courts et motivants pour réussir le hackathon suivant : \n" +
                "Titre: " + hackathonTitle + "\n" +
                "Thème: " + hackathonTheme + "\n" +
                "Réponds en français avec un ton encourageant et utilise des emojis.";

        JsonObject jsonBody = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject partObject = new JsonObject();

        partObject.addProperty("text", prompt);
        partsArray.add(partObject);
        contentObject.add("parts", partsArray);
        contentsArray.add(contentObject);
        jsonBody.add("contents", contentsArray);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                        return jsonResponse.get("candidates").getAsJsonArray()
                                .get(0).getAsJsonObject()
                                .get("content").getAsJsonObject()
                                .get("parts").getAsJsonArray()
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();
                    } else {
                        return "Désolé, l'IA est occupée pour le moment. Réessayez plus tard ! (Code: "
                                + response.statusCode() + ")";
                    }
                })
                .exceptionally(ex -> "Une erreur est survenue lors de la connexion à l'IA : " + ex.getMessage());
    }
}
