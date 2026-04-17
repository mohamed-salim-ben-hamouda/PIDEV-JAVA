package com.pidev.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class flowiseSuggestChallengeInputs {
    private static final String API_HOST = getConfig("flowise.apiHost", "FLOWISE_API_HOST", "http://localhost:3000");
    private static final String FLOW_ID = getConfig("flowise.flowId", "FLOWISE_FLOW_ID", "0297e941-8e3e-4bb3-82bd-35e5acaf1d7d");

    private static final String API_KEY = getConfig("flowise.apiKey", "FLOWISE_API_KEY", "");

    public static JSONObject suggestChallenge(String challengePath){
        try {
            String ChallengeText = ExtractTextFromPdfUtil.extractText(challengePath);
            if (ChallengeText.trim().isEmpty()) {
                return new JSONObject()
                        .put("error", true)
                        .put("message", "Extracted text is empty");
            }

            ChallengeText = ChallengeText.replaceAll("\\s+", " ").trim();
            String fullPayload = "Challenge:\n" + ChallengeText;
            JSONObject body = new JSONObject();
            System.out.println("PDF TEXT >>> " + fullPayload);
            body.put("question", fullPayload);
            body.put("chatId", "javafx_" + System.currentTimeMillis());
            return sendRequest(body);


        }catch (Exception e) {
            return new JSONObject()
                    .put("error", true)
                    .put("message", e.getMessage());
        }
    }
    private static JSONObject sendRequest(JSONObject body) {

        HttpURLConnection conn = null;

        try {
            URL url = new URL(API_HOST + "/api/v1/prediction/" + FLOW_ID);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            if (API_KEY != null && !API_KEY.isBlank() && !"your_api_key".equalsIgnoreCase(API_KEY.trim())) {
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("X-API-Key", API_KEY);
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);

            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            int status = conn.getResponseCode();

            BufferedReader reader = (status >= 200 && status < 300)
                    ? new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    : new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            String responseText = response.toString();
            return parseFlowiseResponse(responseText, status);

        } catch (Exception e) {
            return new JSONObject()
                    .put("error", true)
                    .put("message", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    private static JSONObject parseFlowiseResponse(String text, int status) {

        try {
            JSONObject json = new JSONObject(text);
            if (json.has("json")) {
                Object inner = json.get("json");

                if (inner instanceof JSONObject) {
                    return (JSONObject) inner;
                }

                if (inner instanceof String) {
                    return extractJsonObject((String) inner);
                }
            }
            if (json.has("text")) {
                return extractJsonObject(json.getString("text"));
            }

            return json;

        } catch (Exception e) {
            return extractJsonObject(text, status);
        }
    }
    private static JSONObject extractJsonObject(String text) {
        return extractJsonObject(text, -1);
    }

    private static JSONObject extractJsonObject(String text, int status) {

        try {
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}");

            if (start == -1 || end == -1 || end <= start) {
                return new JSONObject()
                        .put("error", true)
                        .put("status", status)
                        .put("raw", text);
            }

            String jsonPart = text.substring(start, end + 1);

            return new JSONObject(jsonPart);

        } catch (Exception e) {
            return new JSONObject()
                    .put("error", true)
                    .put("status", status)
                    .put("raw", text);
        }
    }
    private static String getConfig(String systemProperty, String envVar, String fallback) {
        String fromSys = System.getProperty(systemProperty);
        if (fromSys != null && !fromSys.isBlank()) {
            return fromSys.trim();
        }
        String fromEnv = System.getenv(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return fallback;
    }
}
