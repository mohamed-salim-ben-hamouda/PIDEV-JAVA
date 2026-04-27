package com.pidev.Services;

import com.pidev.models.Answer;
import com.pidev.models.Course;
import com.pidev.models.Question;
import com.pidev.models.Quiz;
import com.pidev.models.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiFeedbackService {

    private static final String[][] MODEL_ENDPOINTS = {
            {"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=", "gemini-1.5-flash"},
            {"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=", "gemini-1.5-flash-latest"},
            {"https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=", "v1/gemini-1.5-flash"}
    };

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}", Pattern.DOTALL);
    private static final int MAX_OUTPUT_TOKENS = 2048;
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final HttpClient httpClient;

    public AiFeedbackService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public FeedbackResult generateFeedback(Quiz quiz,
                                           List<Question> questions,
                                           List<Answer> answers,
                                           Map<Integer, Integer> selectedAnswerByQuestionId) {
        return generateFeedback(quiz, questions, answers, selectedAnswerByQuestionId, null);
    }

    public FeedbackResult generateFeedback(Quiz quiz,
                                           List<Question> questions,
                                           List<Answer> answers,
                                           Map<Integer, Integer> selectedAnswerByQuestionId,
                                           User student) {
        List<Question> safeQuestions = questions == null ? List.of() : questions;
        List<Answer> safeAnswers = answers == null ? List.of() : answers;
        Map<Integer, Integer> safeSelections = selectedAnswerByQuestionId == null
                ? Map.of()
                : selectedAnswerByQuestionId;

        ResponseData responseData = buildResponseData(safeQuestions, safeAnswers, safeSelections);
        Course course = quiz == null ? null : quiz.getCourse();
        List<String> sectionsToReview = sanitizeSections(course == null ? null : course.getSectionsToReview());

        double score = computeScorePercent(responseData.correctCount(), responseData.total());
        double passingScore = resolvePassingScore(quiz);
        ParsedFeedback parsed;

        try {
            String prompt = buildPrompt(
                    resolveStudentName(student),
                    course == null ? "Cours" : defaultText(course.getTitle(), "Cours"),
                    course == null ? "" : defaultText(course.getDescription(), ""),
                    course == null ? "BEGINNER" : defaultText(course.getDifficulty(), "BEGINNER"),
                    quiz == null ? "Quiz" : defaultText(quiz.getTitle(), "Quiz"),
                    score,
                    passingScore,
                    responseData,
                    sectionsToReview
            );
            String aiResponse = requestGemini(prompt);
            parsed = parseAiResponse(aiResponse);
        } catch (Exception e) {
            System.err.println("[AiFeedbackService] AI feedback generation failed, using fallback: " + e.getMessage());
            parsed = buildFallbackFeedback(responseData, sectionsToReview, score);
        }

        return new FeedbackResult(
                roundOneDecimal(score),
                score >= passingScore,
                responseData.total(),
                responseData.correctCount(),
                responseData.wrongCount(),
                parsed.feedback(),
                parsed.strengths(),
                parsed.weaknesses(),
                parsed.sectionsToReview(),
                parsed.actionPlan(),
                parsed.encouragement()
        );
    }

    public String formatForDisplay(FeedbackResult result) {
        if (result == null) {
            return "Aucun feedback disponible.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(result.feedback()).append("\n\n");
        appendList(sb, "Points forts", result.strengths());
        appendList(sb, "Lacunes a travailler", result.weaknesses());
        appendList(sb, "Sections a revoir", result.sectionsToReview());
        appendList(sb, "Plan d'action", result.actionPlan());
        sb.append("Encouragement : ").append(result.encouragement());
        return sb.toString().trim();
    }

    /**
     * Compatibilite avec l'ancien appel du projet.
     */
    public String generateFeedback(List<Question> failedQuestions) {
        if (failedQuestions == null || failedQuestions.isEmpty()) {
            return "Bravo, aucune lacune detectee !";
        }

        StringBuilder feedback = new StringBuilder("Feedback pedagogique :\n");
        feedback.append("J'ai remarque des points a consolider sur :\n");

        int count = 0;
        for (Question q : failedQuestions) {
            if (count >= 3) {
                feedback.append("- ... et d'autres notions a revoir.\n");
                break;
            }
            feedback.append("- ").append(shorten(defaultText(q.getContent(), "Question"), 90)).append("\n");
            count++;
        }

        feedback.append("\nConseil : relisez le chapitre correspondant avant une nouvelle tentative.");
        return feedback.toString();
    }

    private ResponseData buildResponseData(List<Question> questions,
                                           List<Answer> answers,
                                           Map<Integer, Integer> selectedAnswerByQuestionId) {
        List<ResponseEntry> correct = new ArrayList<>();
        List<ResponseEntry> wrong = new ArrayList<>();

        for (Question question : questions) {
            if (question == null || question.getId() == null) {
                continue;
            }

            List<Answer> answersForQuestion = findAnswersForQuestion(question.getId(), answers);
            Answer correctAnswer = answersForQuestion.stream()
                    .filter(Answer::isCorrect)
                    .findFirst()
                    .orElse(null);

            Integer selectedAnswerId = selectedAnswerByQuestionId.get(question.getId());
            Answer selectedAnswer = answersForQuestion.stream()
                    .filter(answer -> Objects.equals(answer.getId(), selectedAnswerId))
                    .findFirst()
                    .orElse(null);

            boolean isCorrect = selectedAnswer != null && selectedAnswer.isCorrect();
            float maxPoints = question.getPoint();
            float pointsEarned = isCorrect ? maxPoints : 0f;

            ResponseEntry entry = new ResponseEntry(
                    defaultText(question.getContent(), "Question sans contenu"),
                    selectedAnswer == null
                            ? "Aucune reponse"
                            : defaultText(selectedAnswer.getContent(), "Reponse sans contenu"),
                    isCorrect,
                    pointsEarned,
                    maxPoints,
                    correctAnswer == null
                            ? "Bonne reponse indisponible"
                            : defaultText(correctAnswer.getContent(), "Bonne reponse indisponible")
            );

            if (isCorrect) {
                correct.add(entry);
            } else {
                wrong.add(entry);
            }
        }

        return new ResponseData(
                List.copyOf(correct),
                List.copyOf(wrong),
                correct.size(),
                wrong.size(),
                correct.size() + wrong.size()
        );
    }

    private String buildPrompt(String studentName,
                               String courseName,
                               String courseDescription,
                               String courseDifficulty,
                               String quizTitle,
                               double score,
                               double passingScore,
                               ResponseData responseData,
                               List<String> sectionsToReview) {
        boolean passed = score >= passingScore;
        String wrongAnswersText = buildWrongAnswersText(responseData.wrong());
        String correctAnswersText = buildCorrectAnswersText(responseData.correct());
        String sectionsText = sectionsToReview.isEmpty()
                ? "Aucune section specifique identifiee."
                : "- " + String.join("\n- ", sectionsToReview);

        return ""
                + "Tu es un tuteur pedagogique expert et bienveillant. "
                + "Tu dois analyser les resultats d'un quiz d'un etudiant et generer un feedback personnalise, "
                + "constructif et encourageant.\n\n"
                + "=== CONTEXTE ===\n"
                + "- Etudiant : " + studentName + "\n"
                + "- Cours : " + courseName + " (Niveau : " + courseDifficulty + ")\n"
                + "- Description du cours : " + courseDescription + "\n"
                + "- Quiz : " + quizTitle + "\n"
                + "- Score obtenu : " + formatPercent(score) + "% (Score requis : " + formatPercent(passingScore) + "%)\n"
                + "- Resultat : " + (passed ? "REUSSI" : "ECHOUE") + "\n"
                + "- Questions correctes : " + responseData.correctCount() + "/" + responseData.total() + "\n\n"
                + "=== REPONSES INCORRECTES (a analyser en detail) ===\n"
                + wrongAnswersText + "\n\n"
                + "=== REPONSES CORRECTES (points forts) ===\n"
                + correctAnswersText + "\n\n"
                + "=== SECTIONS DU COURS A REVOIR (identifiees par l'enseignant) ===\n"
                + sectionsText + "\n\n"
                + "=== INSTRUCTIONS ===\n"
                + "Genere un feedback pedagogique personnalise au format JSON strict suivant. "
                + "Reponds UNIQUEMENT avec le JSON, sans texte avant ni apres :\n\n"
                + "{\n"
                + "  \"feedback\": \"Un paragraphe de 3-4 phrases avec une analyse globale personnalisee de la performance. Mentionne le score, ce qui a ete bien fait, et les axes d'amelioration.\",\n"
                + "  \"strengths\": [\"Point fort 1 identifie a partir des bonnes reponses\", \"Point fort 2\"],\n"
                + "  \"weaknesses\": [\"Lacune 1 avec explication pedagogique claire de pourquoi la bonne reponse est correcte\", \"Lacune 2 avec explication\"],\n"
                + "  \"sectionsToReview\": [\"Section ou concept 1 a revoir en priorite\", \"Section 2\"],\n"
                + "  \"actionPlan\": [\"Action concrete 1 que l'etudiant devrait faire\", \"Action 2\", \"Action 3\"],\n"
                + "  \"encouragement\": \"Un message d'encouragement personnalise et motivant adapte au score obtenu.\"\n"
                + "}\n\n"
                + "REGLES IMPORTANTES :\n"
                + "1. Sois specifique aux questions posees, pas generique.\n"
                + "2. Pour chaque erreur, explique POURQUOI la bonne reponse est correcte.\n"
                + "3. Relie les lacunes aux sections du cours a revoir quand c'est possible.\n"
                + "4. Adapte le ton au score : encourageant si faible, felicitant si eleve.\n"
                + "5. Le plan d'action doit etre concret et realisable.\n"
                + "6. Reponds en francais.\n"
                + "7. Les tableaux doivent contenir 2 a 5 elements chacun quand c'est pertinent.";
    }

    private String requestGemini(String prompt) throws Exception {
        String apiKey = resolveGeminiApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is missing.");
        }

        String requestBody = buildRequestBody(prompt);
        String lastError = "No Gemini model returned a response.";

        for (String[] endpoint : MODEL_ENDPOINTS) {
            String url = endpoint[0] + apiKey;
            String modelName = endpoint[1];

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new Exception("Erreur reseau Gemini: " + e.getMessage(), e);
            }

            int status = response.statusCode();
            if (status == 200) {
                return extractTextFromGeminiResponse(response.body());
            }
            if (status == 404) {
                lastError = "Modele Gemini indisponible: " + modelName;
                continue;
            }

            String apiError = extractApiErrorMessage(response.body());
            if (status == 400 || status == 401 || status == 403) {
                throw new Exception("Appel Gemini refuse (" + status + "): " + apiError);
            }

            lastError = "Gemini HTTP " + status + " sur " + modelName + ": " + apiError;
        }

        throw new Exception(lastError);
    }

    private String buildRequestBody(String prompt) {
        JSONObject generationConfig = new JSONObject()
                .put("temperature", DEFAULT_TEMPERATURE)
                .put("maxOutputTokens", MAX_OUTPUT_TOKENS);

        JSONObject payload = new JSONObject()
                .put("contents", new JSONArray().put(
                        new JSONObject().put("parts", new JSONArray().put(
                                new JSONObject().put("text", prompt)
                        ))
                ))
                .put("generationConfig", generationConfig);

        return payload.toString();
    }

    private String extractTextFromGeminiResponse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new Exception("Gemini n'a retourne aucun resultat.");
        }

        JSONObject candidate = candidates.getJSONObject(0);
        if ("SAFETY".equalsIgnoreCase(candidate.optString("finishReason"))) {
            throw new Exception("La reponse Gemini a ete bloquee pour des raisons de securite.");
        }

        JSONObject content = candidate.optJSONObject("content");
        if (content == null) {
            throw new Exception("Reponse Gemini sans contenu exploitable.");
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) {
            throw new Exception("Reponse Gemini vide.");
        }

        return parts.getJSONObject(0).optString("text", "").trim();
    }

    private ParsedFeedback parseAiResponse(String aiResponse) {
        String cleaned = cleanJsonFence(aiResponse);
        JSONObject data = tryParseJson(cleaned);

        if (data == null) {
            Matcher matcher = JSON_OBJECT_PATTERN.matcher(cleaned);
            if (matcher.find()) {
                data = tryParseJson(matcher.group());
            }
        }

        if (data == null) {
            return new ParsedFeedback(
                    aiResponse,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    "Continue tes efforts, chaque tentative te rapproche de la reussite."
            );
        }

        return new ParsedFeedback(
                data.optString("feedback", ""),
                jsonArrayToList(data.optJSONArray("strengths")),
                jsonArrayToList(data.optJSONArray("weaknesses")),
                jsonArrayToList(data.optJSONArray("sectionsToReview")),
                jsonArrayToList(data.optJSONArray("actionPlan")),
                data.optString("encouragement", "Continue tes efforts !")
        );
    }

    private ParsedFeedback buildFallbackFeedback(ResponseData responseData,
                                                 List<String> sectionsToReview,
                                                 double score) {
        List<String> strengths = new ArrayList<>();
        for (ResponseEntry entry : responseData.correct().stream().limit(3).toList()) {
            strengths.add("Bonne maitrise : " + shorten(entry.question(), 80));
        }

        List<String> weaknesses = new ArrayList<>();
        for (ResponseEntry entry : responseData.wrong()) {
            weaknesses.add("Erreur sur \"" + shorten(entry.question(), 60)
                    + "\" - La bonne reponse etait : " + defaultText(entry.correctAnswer(), "N/A"));
        }

        String feedback = String.format(Locale.US,
                "Vous avez obtenu %.0f%% avec %d/%d reponses correctes. %s",
                score,
                responseData.correctCount(),
                responseData.total(),
                score >= 70
                        ? "Bon travail ! Quelques points restent a consolider."
                        : "Des efforts supplementaires sont necessaires. Revoyez les concepts ci-dessous.");

        return new ParsedFeedback(
                feedback,
                strengths.isEmpty() ? List.of("Vous avez fait l'effort de passer le quiz, c'est deja un bon debut.") : List.copyOf(strengths),
                weaknesses.isEmpty() ? List.of("Aucune lacune majeure identifiee.") : List.copyOf(weaknesses),
                sectionsToReview.isEmpty() ? List.of("Revoyez l'ensemble du cours.") : List.copyOf(sectionsToReview),
                List.of(
                        "Relire les sections identifiees comme faibles.",
                        "Refaire le quiz apres revision.",
                        "Consulter les ressources complementaires du cours."
                ),
                score >= 70
                        ? "Bravo pour cette performance ! Continue sur cette lancee."
                        : "Ne te decourage pas ! Chaque erreur est une opportunite d'apprentissage."
        );
    }

    private List<String> sanitizeSections(List<String> sections) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String section : sections) {
            if (section != null && !section.isBlank()) {
                sanitized.add(section.trim());
            }
        }
        return List.copyOf(sanitized);
    }

    private List<Answer> findAnswersForQuestion(Integer questionId, List<Answer> answers) {
        if (questionId == null || answers == null || answers.isEmpty()) {
            return List.of();
        }

        List<Answer> result = new ArrayList<>();
        for (Answer answer : answers) {
            if (answer != null
                    && answer.getQuestion() != null
                    && Objects.equals(answer.getQuestion().getId(), questionId)) {
                result.add(answer);
            }
        }
        return result;
    }

    private String buildWrongAnswersText(List<ResponseEntry> wrongEntries) {
        if (wrongEntries == null || wrongEntries.isEmpty()) {
            return "Aucune erreur - l'etudiant a tout juste !";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wrongEntries.size(); i++) {
            ResponseEntry entry = wrongEntries.get(i);
            int num = i + 1;
            sb.append("Erreur #").append(num).append(":\n")
                    .append("- Question : ").append(entry.question()).append("\n")
                    .append("- Reponse de l'etudiant : ").append(entry.studentAnswer()).append("\n")
                    .append("- Bonne reponse : ").append(entry.correctAnswer()).append("\n")
                    .append("- Points perdus : ").append(trimTrailingZero(entry.maxPoints())).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String buildCorrectAnswersText(List<ResponseEntry> correctEntries) {
        if (correctEntries == null || correctEntries.isEmpty()) {
            return "Aucune reponse correcte.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < correctEntries.size(); i++) {
            ResponseEntry entry = correctEntries.get(i);
            sb.append("- Reussite #").append(i + 1).append(" : ").append(entry.question()).append("\n");
        }
        return sb.toString().trim();
    }

    private JSONObject tryParseJson(String raw) {
        try {
            return new JSONObject(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> jsonArrayToList(JSONArray array) {
        if (array == null || array.length() == 0) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private String cleanJsonFence(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private String extractApiErrorMessage(String body) {
        try {
            JSONObject json = new JSONObject(body);
            JSONObject error = json.optJSONObject("error");
            if (error == null) {
                return shorten(body, 180);
            }
            return error.optString("message", shorten(body, 180));
        } catch (Exception e) {
            return shorten(body, 180);
        }
    }

    private String resolveGeminiApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        String property = System.getProperty("gemini.api.key");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }

        return "";
    }

    private String resolveStudentName(User student) {
        if (student == null) {
            return "Etudiant";
        }

        String prenom = defaultText(student.getPrenom(), "").trim();
        String nom = defaultText(student.getNom(), "").trim();
        String fullName = (prenom + " " + nom).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            return student.getEmail().trim();
        }
        return "Etudiant";
    }

    private double computeScorePercent(int correctCount, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return (correctCount * 100.0) / total;
    }

    private double resolvePassingScore(Quiz quiz) {
        if (quiz == null || quiz.getPassingScore() <= 0) {
            return 70.0;
        }
        return quiz.getPassingScore();
    }

    private void appendList(StringBuilder sb, String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        sb.append(title).append(" :\n");
        for (String value : values) {
            sb.append("- ").append(value).append("\n");
        }
        sb.append("\n");
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String trimTrailingZero(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatPercent(double value) {
        double rounded = roundOneDecimal(value);
        if (Math.abs(rounded - Math.rint(rounded)) < 0.0001) {
            return String.valueOf((int) Math.round(rounded));
        }
        return String.format(Locale.US, "%.1f", rounded);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record FeedbackResult(
            double score,
            boolean passed,
            int totalQuestions,
            int correctAnswers,
            int wrongAnswers,
            String feedback,
            List<String> strengths,
            List<String> weaknesses,
            List<String> sectionsToReview,
            List<String> actionPlan,
            String encouragement
    ) {
        public FeedbackResult {
            strengths = immutableList(strengths);
            weaknesses = immutableList(weaknesses);
            sectionsToReview = immutableList(sectionsToReview);
            actionPlan = immutableList(actionPlan);
            feedback = feedback == null ? "" : feedback;
            encouragement = encouragement == null ? "" : encouragement;
        }

        private static List<String> immutableList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            return Collections.unmodifiableList(new ArrayList<>(values));
        }
    }

    private record ResponseData(
            List<ResponseEntry> correct,
            List<ResponseEntry> wrong,
            int correctCount,
            int wrongCount,
            int total
    ) {
    }

    private record ResponseEntry(
            String question,
            String studentAnswer,
            boolean isCorrect,
            float pointsEarned,
            float maxPoints,
            String correctAnswer
    ) {
    }

    private record ParsedFeedback(
            String feedback,
            List<String> strengths,
            List<String> weaknesses,
            List<String> sectionsToReview,
            List<String> actionPlan,
            String encouragement
    ) {
        private ParsedFeedback {
            strengths = immutableList(strengths);
            weaknesses = immutableList(weaknesses);
            sectionsToReview = immutableList(sectionsToReview);
            actionPlan = immutableList(actionPlan);
            feedback = feedback == null ? "" : feedback;
            encouragement = encouragement == null ? "" : encouragement;
        }

        private static List<String> immutableList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }

            Map<String, Boolean> unique = new LinkedHashMap<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    unique.put(value.trim(), Boolean.TRUE);
                }
            }
            return List.copyOf(unique.keySet());
        }
    }
}
