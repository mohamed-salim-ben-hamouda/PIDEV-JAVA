package com.pidev.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class GithubUtil {
    private static final String token = getRequiredConfig("github.token", "GITHUB_TOKEN");
    private static final String org = getRequiredConfig("github.org", "GITHUB_ORG");
    private final HttpClient client = HttpClient.newHttpClient();
    public String createRepository(String repoName) throws Exception {

        String json = "{ \"name\": \"" + repoName + "\", \"private\": true }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/orgs/" + org + "/repos"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            System.out.println("âœ… Repository created");
            return "https://github.com/" + org + "/" + repoName;
        } else {
            throw new RuntimeException(" Failed to create repo: " + response.body());
        }
    }

    public void addCollaborator(String repo, String username, String permission) throws Exception {

        String url = "https://api.github.com/repos/" + org + "/" + repo + "/collaborators/" + username;

        String json = "{ \"permission\": \"" + permission + "\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 204) {
            System.out.println( username + " added (" + permission + ")");
        } else {
            System.out.println(" Error adding " + username + ": " + response.body());
        }
    }

    public void addCollaborators(String repo, List<String> users, String permission) throws Exception {
        for (String user : users) {
            addCollaborator(repo, user, permission);
            Thread.sleep(800); // avoid rate issues
        }
    }

    public String setupRepository(String repoName, String supervisor, List<String> students) throws Exception {

        String repoUrl = createRepository(repoName);

        Thread.sleep(2000);
        addCollaborator(repoName, supervisor, "admin");
        addCollaborators(repoName, students, "push");

        return repoUrl;
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
