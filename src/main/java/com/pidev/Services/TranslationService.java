package com.pidev.Services;

import com.pidev.utils.EnvLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de traduction via Groq API — stratégie BATCH.
 *
 * Le quiz est en FRANÇAIS par défaut.
 * translateBatch(texts, "en") → traduit FR → EN
 * translateBatch(texts, "fr") → traduit EN → FR (retour arrière)
 *
 * 1 seul appel API pour tout le quiz, pas de rate limit 429.
 */
public class TranslationService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";
    private static final String API_KEY      = EnvLoader.get("GROQ_API_KEY");

    private static final int TIMEOUT_SECONDS = 60;
    private static final int BATCH_SIZE      = 80;

    private static final Map<String, String> cache = new HashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    // ------------------------------------------------------------------
    // API simple (compatibilité avec l'ancien code)
    // ------------------------------------------------------------------

    public String translate(String text, String targetLang) {
        if (text == null || text.isBlank()) return text;
        String cacheKey = text + "|" + targetLang;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);
        List<String> result = translateBatch(List.of(text), targetLang);
        return result.isEmpty() ? text : result.get(0);
    }

    // ------------------------------------------------------------------
    // API batch — UN seul appel pour toute la liste
    // ------------------------------------------------------------------

    /**
     * @param texts      Liste de tous les textes à traduire
     * @param targetLang "en" pour traduire le français en anglais
     *                   "fr" pour revenir en français (retour arrière)
     */
    public List<String> translateBatch(List<String> texts, String targetLang) {
        if (texts == null || texts.isEmpty()) return List.of();

        // Vérifier le cache
        List<String>  results        = new ArrayList<>(texts.size());
        List<Integer> missingIndexes = new ArrayList<>();
        List<String>  missingTexts   = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String key = texts.get(i) + "|" + targetLang;
            if (cache.containsKey(key)) {
                results.add(cache.get(key));
            } else {
                results.add(null);
                missingIndexes.add(i);
                missingTexts.add(texts.get(i));
            }
        }

        if (missingTexts.isEmpty()) {
            System.out.println("[TranslationService] " + texts.size() + " textes depuis le cache");
            return results;
        }

        System.out.println("[TranslationService] Batch : " + missingTexts.size()
                + " textes → " + (int) Math.ceil((double) missingTexts.size() / BATCH_SIZE) + " requête(s)");

        // Appels par chunks
        List<String> allTranslated = new ArrayList<>();
        for (int start = 0; start < missingTexts.size(); start += BATCH_SIZE) {
            List<String> chunk      = missingTexts.subList(start, Math.min(start + BATCH_SIZE, missingTexts.size()));
            List<String> translated = callGroq(chunk, targetLang);
            allTranslated.addAll(translated);
        }

        // Remplir résultats + cache
        for (int i = 0; i < missingIndexes.size(); i++) {
            String original    = missingTexts.get(i);
            String translation = i < allTranslated.size() ? allTranslated.get(i) : original;
            results.set(missingIndexes.get(i), translation);
            cache.put(original + "|" + targetLang, translation);
        }

        return results;
    }

    // ------------------------------------------------------------------
    // Appel Groq
    // ------------------------------------------------------------------

    private List<String> callGroq(List<String> texts, String targetLang) {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("La variable d'environnement GROQ_API_KEY est vide. Configurez une clé Groq valide dans .env ou dans Windows.");
        }

        // Langue SOURCE = toujours l'opposé de la cible
        // Le quiz est en FR → si targetLang="en" : FR→EN
        //                   → si targetLang="fr" : EN→FR (retour)
        String sourceLang = "en".equalsIgnoreCase(targetLang) ? "French" : "English";
        String targetLangName = "en".equalsIgnoreCase(targetLang) ? "English" : "French";

        // Construire le tableau JSON numéroté
        JSONArray inputArray = new JSONArray();
        for (int i = 0; i < texts.size(); i++) {
            inputArray.put(new JSONObject().put("id", i).put("text", texts.get(i)));
        }

        String prompt = "You are a professional translator from " + sourceLang + " to " + targetLangName + ".\n\n"
                + "Translate EVERY 'text' value in the JSON array below from " + sourceLang + " to " + targetLangName + ".\n\n"
                + "Strict rules:\n"
                + "- Return ONLY a valid JSON array: [{\"id\": 0, \"text\": \"translated text\"}, ...]\n"
                + "- Keep the exact same 'id' values\n"
                + "- Translate only the 'text' field, never the keys\n"
                + "- Do NOT add explanations, markdown, or any text outside the JSON array\n\n"
                + "Input:\n" + inputArray;

        JSONObject body = new JSONObject()
                .put("model", GROQ_MODEL)
                .put("temperature", 0.1)
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "system")
                                .put("content", "You are a professional translator. Respond with valid JSON only, no markdown."))
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", prompt)));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body(), texts);
            }

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IllegalStateException("Clé Groq invalide ou expirée (HTTP " + response.statusCode() + "). Générez une nouvelle clé et mettez à jour GROQ_API_KEY.");
            }

            if (response.statusCode() == 429) {
                int wait = extractRetryDelay(response.body());
                System.out.println("[TranslationService] 429 — attente " + wait + "s puis retry...");
                Thread.sleep(wait * 1000L);
                HttpResponse<String> retry =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (retry.statusCode() == 200) return parseResponse(retry.body(), texts);
                System.err.println("[TranslationService] Retry échoué : " + retry.statusCode());
            } else {
                System.err.println("[TranslationService] Erreur Groq " + response.statusCode()
                        + " : " + response.body().substring(0, Math.min(200, response.body().length())));
            }

        } catch (Exception e) {
            System.err.println("[TranslationService] Exception : " + e.getMessage());
        }

        return new ArrayList<>(texts); // fallback
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    private List<String> parseResponse(String body, List<String> originals) {
        try {
            String content = new JSONObject(body)
                    .getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content").trim();

            // Nettoyer markdown
            if (content.startsWith("```json")) content = content.substring(7);
            else if (content.startsWith("```"))  content = content.substring(3);
            if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
            content = content.trim();

            // Extraire le tableau si texte parasite avant
            if (!content.startsWith("[")) {
                int s = content.indexOf('['), e = content.lastIndexOf(']');
                if (s != -1 && e > s) content = content.substring(s, e + 1);
            }

            JSONArray arr = new JSONArray(content);
            Map<Integer, String> byId = new HashMap<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                byId.put(obj.getInt("id"), obj.getString("text"));
            }

            List<String> result = new ArrayList<>();
            for (int i = 0; i < originals.size(); i++) {
                result.add(byId.getOrDefault(i, originals.get(i)));
            }
            System.out.println("[TranslationService] Batch OK : " + originals.size() + " textes traduits");
            return result;

        } catch (Exception e) {
            System.err.println("[TranslationService] Erreur parsing : " + e.getMessage());
            return new ArrayList<>(originals);
        }
    }

    private int extractRetryDelay(String body) {
        try {
            int idx = body.indexOf("try again in ");
            if (idx != -1) {
                String digits = body.substring(idx + 13, Math.min(idx + 20, body.length()))
                        .replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) return Integer.parseInt(digits) + 2;
            }
        } catch (Exception ignored) {}
        return 10;
    }
}