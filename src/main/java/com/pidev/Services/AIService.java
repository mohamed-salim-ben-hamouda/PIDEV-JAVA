package com.pidev.Services;

import com.google.gson.*;
import com.pidev.models.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AIService {

    /**
     * SOLUTION FINALE : GROQ API (Compatible avec votre clé gsk_...)
     * Votre clé commence par 'gsk_', ce qui signifie qu'elle appartient à GROQ, pas Gemini.
     * Groq est l'API la plus rapide au monde et elle est GRATUITE.
     */
    private static final String API_KEY = "";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL_ID = "llama-3.1-8b-instant"; // Version stable et supportée en 2026

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                    LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(java.time.LocalDateTime.class, (JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class, (JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) ->
                    java.time.LocalDateTime.parse(json.getAsString()))
            .create();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Cv generateCvWithAI(String jobTitle, String notes, String language, List<String> sections) throws IOException, InterruptedException {

        String prompt = constructPrompt(jobTitle, notes, language, sections);

        // Construction du corps de la requête (Format OpenAI compatible utilisé par Groq)
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL_ID);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are a professional CV expert. Return ONLY valid JSON.");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        // Optionnel : Forcer le format JSON pour plus de stabilité
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY.trim())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erreur API Groq (Status " + response.statusCode() + "): " + response.body());
        }

        return parseAiResponse(response.body());
    }

    public Cv translateCvWithAI(Cv cv, String targetLanguage) throws IOException, InterruptedException {
        String cvJson = gson.toJson(cv);

        // Map common language names to English for better AI understanding
        String languageInEnglish = targetLanguage;
        if (targetLanguage.equalsIgnoreCase("Français") || targetLanguage.equalsIgnoreCase("Francais")) languageInEnglish = "French";
        else if (targetLanguage.equalsIgnoreCase("Anglais")) languageInEnglish = "English";
        else if (targetLanguage.equalsIgnoreCase("Allemand")) languageInEnglish = "German";
        else if (targetLanguage.equalsIgnoreCase("Arabe")) languageInEnglish = "Arabic";

        String prompt = "Translate the following CV content into the target language: " + targetLanguage + " (" + languageInEnglish + ").\n" +
                " \n" +
                " STRICT RULES: \n" +
                " - You must return ONLY valid JSON. \n" +
                " - Do NOT include any explanation, text, or comments. \n" +
                " - Do NOT include markdown (no ```json). \n" +
                " - Do NOT add or remove fields. \n" +
                " - Do NOT change the structure. \n" +
                " - Do NOT translate the JSON keys (keep them exactly as they are). \n" +
                " - Do NOT invent any information. \n" +
                " - Translate ONLY textual content. \n" +
                " - Keep all dates EXACTLY as they are (format YYYY-MM-DD). \n" +
                " - Keep boolean values unchanged (true/false). \n" +
                " - Keep empty arrays if they exist. \n" +
                " - If a field is already in the target language, keep it as is. \n" +
                " - Preserve formatting consistency. \n" +
                " \n" +
                " FIELDS TO TRANSLATE (Values only): \n" +
                " - summary \n" +
                " - experiences.jobTitle \n" +
                " - experiences.company \n" +
                " - experiences.location \n" +
                " - experiences.description \n" +
                " - educations (all text fields) \n" +
                " - skills (all text fields) \n" +
                " - languages (all text fields) \n" +
                " \n" +
                " INPUT CV (JSON): \n" +
                cvJson + " \n" +
                " \n" +
                " EXPECTED OUTPUT FORMAT (JSON keys must be identical): \n" +
                " { \n" +
                " \"summary\": \"...\", \n" +
                " \"experiences\": [ \n" +
                " { \n" +
                " \"jobTitle\": \"...\", \n" +
                " \"company\": \"...\", \n" +
                " \"location\": \"...\", \n" +
                " \"startDate\": \"YYYY-MM-DD\", \n" +
                " \"endDate\": \"YYYY-MM-DD\", \n" +
                " \"currentlyWorking\": false, \n" +
                " \"description\": \"...\" \n" +
                " } \n" +
                " ], \n" +
                " \"educations\": [{\"degree\": \"...\", \"fieldOfStudy\": \"...\", \"school\": \"...\", \"city\": \"...\", \"startDate\": \"YYYY-MM-DD\", \"endDate\": \"YYYY-MM-DD\", \"description\": \"...\"}], \n" +
                " \"skills\": [{\"nom\": \"...\", \"type\": \"...\", \"level\": \"...\"}], \n" +
                " \"languages\": [{\"nom\": \"...\", \"niveau\": \"...\"}] \n" +
                " }";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL_ID);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are a professional CV translator. Return ONLY valid JSON.");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY.trim())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erreur API Groq (Status " + response.statusCode() + "): " + response.body());
        }

        return parseAiResponse(response.body());
    }

    private String constructPrompt(String jobTitle, String notes, String language, List<String> sections) {
        String sectionsStr = String.join(", ", sections);

        return "Generate a professional CV in " + language + " for the position: " + jobTitle + ".\n" +
                "Details: " + notes + "\n" +
                "Mandatory sections: " + sectionsStr + ".\n\n" +
                "CRITICAL: Return ONLY a JSON object with this exact structure:\n" +
                "{\n" +
                "  \"summary\": \"...\",\n" +
                "  \"experiences\": [{\"jobTitle\": \"...\", \"company\": \"...\", \"location\": \"...\", \"startDate\": \"YYYY-MM-DD\", \"endDate\": \"YYYY-MM-DD\", \"currentlyWorking\": false, \"description\": \"...\"}],\n" +
                "  \"educations\": [{\"degree\": \"...\", \"fieldOfStudy\": \"...\", \"school\": \"...\", \"city\": \"...\", \"startDate\": \"YYYY-MM-DD\", \"endDate\": \"YYYY-MM-DD\", \"description\": \"...\"}],\n" +
                "  \"skills\": [{\"nom\": \"...\", \"type\": \"hard/soft\", \"level\": \"Expert\"}],\n" +
                "  \"languages\": [{\"nom\": \"...\", \"niveau\": \"...\"}]\n" +
                "}";
    }

    private Cv parseAiResponse(String responseBody) {
        try {
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            // Format OpenAI/Groq : choices[0].message.content
            String text = responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            JsonObject json = JsonParser.parseString(text).getAsJsonObject();

            Cv cv = new Cv();
            cv.setSummary(getString(json, "summary"));
            cv.setExperiences(parseExperiences(json));
            cv.setEducations(parseEducations(json));
            cv.setSkills(parseSkills(json));
            cv.setLanguages(parseLanguages(json));

            return cv;
        } catch (Exception e) {
            throw new RuntimeException("Échec du parsing de la réponse Groq: " + e.getMessage());
        }
    }

    private List<Experience> parseExperiences(JsonObject json) {
        List<Experience> list = new ArrayList<>();
        if (json.has("experiences") && json.get("experiences").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("experiences")) {
                JsonObject obj = e.getAsJsonObject();
                Experience ex = new Experience();
                ex.setJobTitle(getString(obj, "jobTitle"));
                ex.setCompany(getString(obj, "company"));
                ex.setLocation(getString(obj, "location"));
                ex.setStartDate(parseDate(getString(obj, "startDate")));
                ex.setEndDate(parseDate(getString(obj, "endDate")));
                ex.setCurrentlyWorking(getBool(obj, "currentlyWorking"));
                ex.setDescription(getString(obj, "description"));
                list.add(ex);
            }
        }
        return list;
    }

    private List<Education> parseEducations(JsonObject json) {
        List<Education> list = new ArrayList<>();
        if (json.has("educations") && json.get("educations").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("educations")) {
                JsonObject obj = e.getAsJsonObject();
                Education ed = new Education();
                ed.setDegree(getString(obj, "degree"));
                ed.setFieldOfStudy(getString(obj, "fieldOfStudy"));
                ed.setSchool(getString(obj, "school"));
                ed.setCity(getString(obj, "city"));
                ed.setStartDate(parseDate(getString(obj, "startDate")));
                ed.setEndDate(parseDate(getString(obj, "endDate")));
                ed.setDescription(getString(obj, "description"));
                list.add(ed);
            }
        }
        return list;
    }

    private List<Skill> parseSkills(JsonObject json) {
        List<Skill> list = new ArrayList<>();
        if (json.has("skills") && json.get("skills").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("skills")) {
                JsonObject obj = e.getAsJsonObject();
                Skill s = new Skill();
                s.setNom(getString(obj, "nom"));
                s.setType(getString(obj, "type"));
                s.setLevel(getString(obj, "level"));
                list.add(s);
            }
        }
        return list;
    }

    private List<Langue> parseLanguages(JsonObject json) {
        List<Langue> list = new ArrayList<>();
        if (json.has("languages") && json.get("languages").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("languages")) {
                JsonObject obj = e.getAsJsonObject();
                Langue l = new Langue();
                l.setNom(getString(obj, "nom"));
                l.setNiveau(getString(obj, "niveau"));
                list.add(l);
            }
        }
        return list;
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private boolean getBool(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean();
    }

    private LocalDate parseDate(String date) {
        try {
            return (date == null || date.isBlank() || date.equals("null")) ? null : LocalDate.parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}
