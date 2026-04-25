package com.pidev.Services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * Service de traduction utilisant l'API MyMemory (gratuite, sans clé API).
 * Limite : 5000 mots/jour sur l'IP publique. Pour plus, ajoutez votre email
 * en paramètre &de=votre@email.com pour monter à 50 000 mots/jour.
 *
 * Endpoint : https://api.mymemory.translated.net/get?q=TEXT&langpair=en|fr
 */
public class TranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";

    // Email optionnel pour augmenter la limite à 50 000 mots/jour
    // Laissez vide ("") pour la limite gratuite de 5 000 mots/jour
    private static final String OPTIONAL_EMAIL = "nouh.mezned@esprit.tn";

    private static final int TIMEOUT_SECONDS = 8;
    private static final Map<String, String> cache = new HashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    /**
     * Traduit un texte vers la langue cible.
     *
     * @param text       Texte source
     * @param targetLang "fr" pour français, "en" pour anglais
     * @return Texte traduit, ou texte original en cas d'erreur
     */
    public String translate(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }

        // Tronquer les textes trop longs (limite MyMemory ~500 chars par requête)
        String textToTranslate = text.length() > 450 ? text.substring(0, 450) : text;

        String cacheKey = textToTranslate + "|" + targetLang;
        if (cache.containsKey(cacheKey)) {
            System.out.println("[TranslationService] Cache hit");
            return cache.get(cacheKey);
        }

        try {
            // Déterminer la paire de langues (MyMemory détecte la source automatiquement)
            String langPair = "en|fr";
            if ("en".equalsIgnoreCase(targetLang)) {
                langPair = "fr|en";
            }

            String encodedText = URLEncoder.encode(textToTranslate, StandardCharsets.UTF_8);
            String url = API_URL + "?q=" + encodedText + "&langpair=" + langPair;

            if (!OPTIONAL_EMAIL.isBlank()) {
                url += "&de=" + URLEncoder.encode(OPTIONAL_EMAIL, StandardCharsets.UTF_8);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String translated = json
                        .getJSONObject("responseData")
                        .getString("translatedText");

                // Vérifier que la réponse n'est pas une erreur déguisée
                if (translated != null && !translated.isBlank()
                        && !translated.toUpperCase().startsWith("MYMEMORY WARNING")) {
                    cache.put(cacheKey, translated);
                    System.out.println("[TranslationService] OK: " + text.substring(0, Math.min(30, text.length())) + "...");
                    return translated;
                }

                // Fallback : essayer la première alternative si la principale est invalide
                if (json.has("matches") && json.getJSONArray("matches").length() > 0) {
                    String alt = json.getJSONArray("matches")
                            .getJSONObject(0)
                            .getString("translation");
                    if (alt != null && !alt.isBlank()) {
                        cache.put(cacheKey, alt);
                        return alt;
                    }
                }
            }

            System.err.println("[TranslationService] HTTP " + response.statusCode() + " — retour texte original");
            return text;

        } catch (Exception e) {
            System.err.println("[TranslationService] Erreur : " + e.getMessage());
            return text;
        }
    }
}