package com.pidev.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FlowiseGraderUtil {
    private static final String api_host = getRequiredConfig("flowise.apiHost", "FLOWISE_api_host", "FLOWISE_API_HOST");
    private static final String flow_id = getRequiredConfig("flowise.flowId", "FLOWISE_flow_id", "FLOWISE_FLOW_ID");
    private static final String api_key = getRequiredConfig("flowise.apiKey", "FLOWISE_api_key", "FLOWISE_API_KEY");

    public static JSONObject gradeFromPdfPaths(String challengePath, String submissionPath) {

        try {

            String requirementText = ExtractTextFromPdfUtil.extractText(challengePath);
            String submissionText = ExtractTextFromPdfUtil.extractText(submissionPath);

            if (requirementText.trim().isEmpty() || submissionText.trim().isEmpty()) {
                return new JSONObject()
                        .put("error", true)
                        .put("message", "Extracted text is empty");
            }
            requirementText = requirementText.replaceAll("\\s+", " ").trim();
            submissionText = submissionText.replaceAll("\\s+", " ").trim();

            if (submissionText.length() > 15000) {
                submissionText = submissionText.substring(0, 15000);
            }
            String fullPayload =
                    "REQUIREMENTS:\n" + requirementText +
                            "\n\nSTUDENT WORK:\n" + submissionText;

            JSONObject body = new JSONObject();
            body.put("question", fullPayload);
            body.put("chatId", "javafx_" + System.currentTimeMillis());
            return sendRequest(body);

        } catch (Exception e) {
            return new JSONObject()
                    .put("error", true)
                    .put("message", e.getMessage());
        }
    }
    private static JSONObject sendRequest(JSONObject body) {

        HttpURLConnection conn = null;

        try {
            URL url = new URL(api_host + "/api/v1/prediction/" + flow_id);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            if (api_key != null && !api_key.isBlank() && !"your_api_key".equalsIgnoreCase(api_key.trim())) {
                conn.setRequestProperty("Authorization", "Bearer " + api_key);
                conn.setRequestProperty("X-API-Key", api_key);
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

    private static String getRequiredConfig(String... keys) {
        for (String key : keys) {
            String fromSys = System.getProperty(key);
            if (fromSys != null && !fromSys.isBlank()) {
                return fromSys.trim();
            }

            String fromEnv = EnvLoader.get(key, null);
            if (fromEnv != null && !fromEnv.isBlank()) {
                return fromEnv.trim();
            }
        }
        throw new IllegalStateException("Missing required config: " + String.join(", ", keys));
    }
}
