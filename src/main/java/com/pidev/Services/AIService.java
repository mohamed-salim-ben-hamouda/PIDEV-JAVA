package com.pidev.Services;

import com.google.gson.*;
import com.pidev.models.*;
import com.pidev.utils.EnvConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AIService {

    private static final String API_KEY = EnvConfig.get("GROQ_API_KEY");
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL_ID = "llama-3.1-8b-instant";

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

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL_ID);

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", "You are a professional CV expert. Return ONLY valid JSON.\n\n" + prompt);
        messages.add(userMsg);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.1); // Lower temperature for more stable JSON extraction

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY.trim())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erreur Groq: " + response.body());
        }

        return parseAiResponse(response.body());
    }

    public Cv translateCvWithAI(Cv cv, String targetLanguage) throws IOException, InterruptedException {
        String cvJson = gson.toJson(cv);
        String prompt = "Translate the following CV JSON into " + targetLanguage + ".\n" +
                "STRICT RULES:\n" +
                "- Return ONLY valid JSON.\n" +
                "- Keep the EXACT same JSON structure.\n" +
                "- DO NOT translate the JSON keys (e.g., keep 'skills', 'nom', 'type', 'level', 'experiences', etc.).\n" +
                "- Translate ONLY the values of the fields.\n" +
                "- Maintain all arrays and nested objects.\n\n" +
                "CV JSON TO TRANSLATE:\n" + cvJson;

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL_ID);
        requestBody.addProperty("temperature", 0);
        requestBody.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY.trim())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Erreur Groq: " + response.body());

        return parseAiResponse(response.body());
    }

    public AtsAnalysis analyzeAtsWithAI(Cv cv, String jobTitle, String jobDescription) throws IOException, InterruptedException {
        String cvJson = gson.toJson(cv);
        String prompt = "Analyze this CV for ATS compatibility against the job description. Return ONLY valid JSON in French.\n\n" +
                "JOB: " + jobTitle + "\nDESC: " + jobDescription + "\nCV: " + cvJson;

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL_ID);

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY.trim())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Erreur Groq: " + response.body());

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        String content = root.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
        return gson.fromJson(cleanJson(content), AtsAnalysis.class);
    }

    public String generateMotivationLetter(Cv cv, Offer offer, String language) throws IOException, InterruptedException {
        String cvJson = gson.toJson(cv);
        String targetLang = (language == null || language.isEmpty()) ? (cv.getLangue() != null ? cv.getLangue() : "Français") : language;
        String prompt = "Write a professional motivation letter in " + targetLang + ". Return ONLY the letter text.\n\nOFFER: " + offer.getTitle() + "\nCV: " + cvJson;

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL_ID);

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY.trim())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Erreur Groq: " + response.body());

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        return root.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
    }

    private String constructPrompt(String jobTitle, String notes, String language, List<String> sections) {
        String sectionsStr = String.join(", ", sections);
        return "You are a professional CV data extractor. Your goal is to identify and extract ALL candidate details from the provided text.\n\n" +
                "CONTEXT:\n" +
                "- Target Job: " + jobTitle + "\n" +
                "- Output Language: " + language + "\n" +
                "- User Input Text: \"" + notes + "\"\n\n" +
                "EXTRACTION RULES:\n" +
                "1. LANGUAGES: You MUST extract every language mentioned (e.g. Français, Anglais, Arabe). For each, identify the level (e.g. Maternelle, Courant, Intermédiaire, Débutant). \n" +
                "   CRITICAL: Put these ONLY in the 'languages' array, NOT in 'skills'.\n" +
                "2. SKILLS: Extract every technical or soft skill mentioned. Put these ONLY in the 'skills' array.\n" +
                "3. EXPERIENCES/EDUCATION: Extract companies, schools, dates, and descriptions. If a date is missing, use null.\n" +
                "4. SUMMARY: Write a professional 3-4 sentence profile in " + language + " based on the input.\n\n" +
                "MANDATORY JSON FORMAT (No other text):\n" +
                "{\n" +
                "  \"summary\": \"...\",\n" +
                "  \"experiences\": [{\"jobTitle\": \"...\", \"company\": \"...\", \"location\": \"...\", \"startDate\": \"YYYY-MM-DD\", \"endDate\": \"YYYY-MM-DD\", \"currentlyWorking\": false, \"description\": \"...\"}],\n" +
                "  \"educations\": [{\"degree\": \"...\", \"fieldOfStudy\": \"...\", \"school\": \"...\", \"city\": \"...\", \"startDate\": \"YYYY-MM-DD\", \"endDate\": \"YYYY-MM-DD\", \"description\": \"...\"}],\n" +
                "  \"skills\": [{\"nom\": \"...\", \"type\": \"hard/soft\", \"level\": \"...\"}],\n" +
                "  \"languages\": [{\"nom\": \"...\", \"niveau\": \"...\"}],\n" +
                "  \"certifications\": [{\"name\": \"...\", \"issuedBy\": \"...\", \"issueDate\": \"YYYY-MM-DD\", \"expDate\": \"YYYY-MM-DD\"}]\n" +
                "}";
    }

    private Cv parseAiResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("L'IA a retourné une réponse vide.");
        }

        try {
            JsonElement root = JsonParser.parseString(responseBody);
            if (!root.isJsonObject()) {
                throw new RuntimeException("Réponse API invalide (pas un objet): " + responseBody);
            }

            JsonObject responseJson = root.getAsJsonObject();

            if (!responseJson.has("choices") || responseJson.getAsJsonArray("choices").size() == 0) {
                throw new RuntimeException("L'IA n'a retourné aucun contenu : " + responseBody);
            }

            String text = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            if (text == null || text.isBlank()) {
                throw new RuntimeException("Le contenu généré par l'IA est vide.");
            }

            String jsonText = cleanJson(text);
            System.out.println("--- DEBUG: AI CLEANED JSON ---\n" + jsonText + "\n----------------------------");

            JsonElement extractedRoot = JsonParser.parseString(jsonText);
            if (!extractedRoot.isJsonObject()) {
                throw new RuntimeException("L'IA n'a pas retourné un JSON valide : " + jsonText);
            }

            JsonObject json = extractedRoot.getAsJsonObject();

            Cv cv = new Cv();
            cv.setSummary(getString(json, "summary"));
            cv.setExperiences(parseExperiences(json));
            cv.setEducations(parseEducations(json));
            cv.setSkills(parseSkills(json));
            cv.setLanguages(parseLanguages(json));
            cv.setCertifs(parseCertifs(json));

            // --- RECOVERY LOGIC FOR LANGUAGES ---
            // If languages array is empty, check if AI put languages in skills
            if (cv.getLanguages().isEmpty() && !cv.getSkills().isEmpty()) {
                List<Skill> remainingSkills = new ArrayList<>();
                List<String> languageKeywords = List.of("français", "anglais", "arabe", "allemand", "espagnol", "italien", "french", "english", "arabic", "german", "spanish");

                for (Skill s : cv.getSkills()) {
                    if (s.getNom() == null) continue;
                    String skillName = s.getNom().toLowerCase();
                    boolean isLanguage = false;
                    for (String kw : languageKeywords) {
                        if (skillName.contains(kw)) {
                            isLanguage = true;
                            break;
                        }
                    }

                    if (isLanguage) {
                        Langue l = new Langue();
                        l.setNom(s.getNom());
                        l.setNiveau(s.getLevel());
                        cv.getLanguages().add(l);
                    } else {
                        remainingSkills.add(s);
                    }
                }
                cv.setSkills(remainingSkills);
            }
            // ------------------------------------

            return cv;
        } catch (Exception e) {
            System.err.println("Erreur de parsing AI: " + e.getMessage());
            throw new RuntimeException("Échec du parsing: " + e.getMessage());
        }
    }

    private String cleanJson(String text) {
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            if (text.contains("```")) text = text.substring(0, text.lastIndexOf("```"));
        } else if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            if (text.contains("```")) text = text.substring(0, text.lastIndexOf("```"));
        }
        return text.trim();
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
        String key = json.has("skills") ? "skills" : (json.has("compétences") ? "compétences" : (json.has("competences") ? "competences" : null));
        if (key != null && json.get(key).isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray(key)) {
                JsonObject obj = e.getAsJsonObject();
                Skill s = new Skill();
                s.setNom(getString(obj, "nom") != null ? getString(obj, "nom") : getString(obj, "name"));
                s.setType(getString(obj, "type"));
                s.setLevel(getString(obj, "level") != null ? getString(obj, "level") : getString(obj, "niveau"));
                list.add(s);
            }
        }
        return list;
    }

    private List<Langue> parseLanguages(JsonObject json) {
        List<Langue> list = new ArrayList<>();
        String key = json.has("languages") ? "languages" : (json.has("langues") ? "langues" : null);
        if (key != null && json.get(key).isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray(key)) {
                JsonObject obj = e.getAsJsonObject();
                Langue l = new Langue();

                // Try multiple common keys for the language name
                String name = getString(obj, "nom");
                if (name == null) name = getString(obj, "name");
                if (name == null) name = getString(obj, "language");
                if (name == null) name = getString(obj, "langue");
                l.setNom(name != null ? name : "");

                // Try multiple common keys for the level
                String level = getString(obj, "niveau");
                if (level == null) level = getString(obj, "level");
                if (level == null) level = getString(obj, "proficiency");
                if (level == null) level = getString(obj, "fluency");
                l.setNiveau(level != null ? level : "");

                if (!l.getNom().isEmpty()) {
                    list.add(l);
                }
            }
        }
        return list;
    }

    private List<Certif> parseCertifs(JsonObject json) {
        List<Certif> list = new ArrayList<>();
        String key = json.has("certifications") ? "certifications" : (json.has("certifs") ? "certifs" : null);
        if (key != null && json.get(key).isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray(key)) {
                JsonObject obj = e.getAsJsonObject();
                Certif c = new Certif();
                c.setName(getString(obj, "name"));
                c.setIssuedBy(getString(obj, "issuedBy"));
                c.setIssueDate(parseDate(getString(obj, "issueDate")));
                c.setExpDate(parseDate(getString(obj, "expDate")));
                list.add(c);
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
