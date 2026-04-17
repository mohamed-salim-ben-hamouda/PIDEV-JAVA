package com.pidev.Controllers.client;

import com.pidev.Services.AnswerService;
import com.pidev.Services.QuestionService;
import com.pidev.models.Answer;
import com.pidev.models.Question;
import com.pidev.models.Quiz;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QuizSessionController {

    @FXML private Label quizTitleLabel;
    @FXML private Label quizContextLabel;
    @FXML private Label questionCountLabel;
    @FXML private Label attemptLabel;
    @FXML private Label requiredScoreLabel;
    @FXML private Label timeLimitLabel;

    @FXML private VBox introSection;
    @FXML private VBox questionSection;
    @FXML private VBox resultSection;

    @FXML private Label questionNavLabel;
    @FXML private Label liveScoreLabel;
    @FXML private ProgressBar questionProgressBar;
    @FXML private Label timerBadgeLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox answersContainer;
    @FXML private HBox quickNavContainer;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;

    @FXML private Label resultTitleLabel;
    @FXML private Label resultSummaryLabel;
    @FXML private Label resultScoreLabel;

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();

    private final Map<Integer, Integer> selectedAnswerByQuestionId = new HashMap<>();
    private List<Question> questions = new ArrayList<>();
    private List<Answer> answers = new ArrayList<>();

    private Quiz quiz;
    private int currentQuestionIndex;
    private int remainingSeconds;
    private Timeline countdownTimeline;

    @FXML
    public void initialize() {
        showSection(true, false, false);
        timerBadgeLabel.setText("Libre");
    }

    public void setQuizContext(Quiz quiz, String chapterName) {
        this.quiz = quiz;
        String title = quiz != null && quiz.getTitle() != null ? quiz.getTitle() : "Quiz";
        quizTitleLabel.setText(title);
        quizContextLabel.setText(chapterName == null || chapterName.isBlank() ? "Quiz du cours" : chapterName);

        int attempts = quiz != null && quiz.getMaxAttempts() > 0 ? quiz.getMaxAttempts() : 3;
        int passing = quiz != null ? Math.round(quiz.getPassingScore() <= 0 ? 70f : quiz.getPassingScore()) : 70;
        int time = quiz != null ? quiz.getTimeLimit() : 0;

        attemptLabel.setText("1/" + attempts);
        requiredScoreLabel.setText(passing + "%");
        timeLimitLabel.setText(time <= 0 ? "Libre" : time + " min");
        remainingSeconds = Math.max(0, time) * 60;
        timerBadgeLabel.setText(time <= 0 ? "Libre" : formatTime(remainingSeconds));

        loadQuizData();
    }

    @FXML
    private void onStartQuiz() {
        if (questions.isEmpty()) {
            showError("Quiz", "Aucune question disponible pour ce quiz.");
            return;
        }
        selectedAnswerByQuestionId.clear();
        currentQuestionIndex = 0;
        startCountdown();
        showSection(false, true, false);
        renderCurrentQuestion();
    }

    @FXML
    private void onPrevQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            renderCurrentQuestion();
        }
    }

    @FXML
    private void onNextQuestion() {
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            renderCurrentQuestion();
        }
    }

    @FXML
    private void onFinishQuiz() {
        if (questions.isEmpty()) {
            return;
        }

        stopCountdown();

        int total = questions.size();
        int score = computeScore();
        int percent = total == 0 ? 0 : Math.round(score * 100f / total);
        int passing = Math.round(quiz.getPassingScore() <= 0 ? 70f : quiz.getPassingScore());
        boolean passed = percent >= passing;

        resultTitleLabel.setText(passed ? "Quiz valide" : "Quiz non valide");
        resultSummaryLabel.setText(
                passed
                        ? "Bravo, vous avez atteint le score requis."
                        : "Vous pouvez relancer le quiz pour améliorer votre resultat."
        );
        resultScoreLabel.setText(score + " / " + total + " (" + percent + "%)");
        resultScoreLabel.getStyleClass().setAll("result-score-badge", passed ? "success" : "fail");

        showSection(false, false, true);
    }

    @FXML
    private void onCloseWindow() {
        Window window = quizTitleLabel.getScene() != null ? quizTitleLabel.getScene().getWindow() : null;
        if (window != null) {
            window.hide();
        }
    }

    @FXML
    private void onRetry() {
        onStartQuiz();
    }

    private void startCountdown() {
        stopCountdown();
        int timeLimitMinutes = quiz == null ? 0 : quiz.getTimeLimit();
        if (timeLimitMinutes <= 0) {
            timerBadgeLabel.setText("Libre");
            return;
        }

        remainingSeconds = timeLimitMinutes * 60;
        timerBadgeLabel.setText(formatTime(remainingSeconds));
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            timerBadgeLabel.setText(formatTime(Math.max(remainingSeconds, 0)));
            if (remainingSeconds <= 0) {
                stopCountdown();
                onFinishQuiz();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private String formatTime(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        int minutes = safeSeconds / 60;
        int rest = safeSeconds % 60;
        return String.format("%02d:%02d", minutes, rest);
    }

    private void loadQuizData() {
        try {
            List<Question> loadedQuestions = questionService.findAll().stream()
                    .filter(item -> item.getQuiz() != null && quiz != null && Objects.equals(item.getQuiz().getId(), quiz.getId()))
                    .sorted(Comparator.comparingInt(item -> item.getId() == null ? Integer.MAX_VALUE : item.getId()))
                    .toList();
            this.questions = new ArrayList<>(loadedQuestions);
            this.answers = answerService.findAll();
        } catch (SQLException e) {
            this.questions = new ArrayList<>();
            this.answers = new ArrayList<>();
            showError("Quiz", "Impossible de charger les questions du quiz.");
        }

        questionCountLabel.setText(String.valueOf(questions.size()));
    }

    private void renderCurrentQuestion() {
        if (questions.isEmpty()) {
            return;
        }

        Question question = questions.get(currentQuestionIndex);
        questionNavLabel.setText("Question " + (currentQuestionIndex + 1) + " sur " + questions.size());
        liveScoreLabel.setText("Score: " + computeScore() + " / " + questions.size());
        double progress = questions.isEmpty() ? 0.0 : (currentQuestionIndex + 1) / (double) questions.size();
        questionProgressBar.setProgress(progress);
        questionTextLabel.setText(question.getContent() == null ? "Sans contenu" : question.getContent());

        answersContainer.getChildren().clear();
        Integer selectedId = selectedAnswerByQuestionId.get(question.getId());

        List<Answer> questionAnswers = answers.stream()
                .filter(answer -> answer.getQuestion() != null && Objects.equals(answer.getQuestion().getId(), question.getId()))
                .toList();

        for (Answer answer : questionAnswers) {
            Button answerButton = new Button(answer.getContent() == null ? "Sans contenu" : answer.getContent());
            answerButton.getStyleClass().setAll("quiz-answer-option");
            answerButton.setMaxWidth(Double.MAX_VALUE);
            if (selectedId != null && Objects.equals(selectedId, answer.getId())) {
                answerButton.getStyleClass().add("selected");
            }
            answerButton.setOnAction(evt -> onAnswerSelected(question, answer));
            answersContainer.getChildren().add(answerButton);
        }

        renderQuickNav();
        prevButton.setDisable(currentQuestionIndex == 0);
        nextButton.setDisable(currentQuestionIndex >= questions.size() - 1);
        finishButton.setDisable(selectedAnswerByQuestionId.isEmpty());
    }

    private void renderQuickNav() {
        quickNavContainer.getChildren().clear();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Button navButton = new Button(String.valueOf(i + 1));
            navButton.getStyleClass().setAll("page-btn");
            if (i == currentQuestionIndex) {
                navButton.getStyleClass().add("active-pill");
            } else if (selectedAnswerByQuestionId.containsKey(q.getId())) {
                navButton.getStyleClass().add("duration-pill");
            }
            final int idx = i;
            navButton.setOnAction(evt -> {
                currentQuestionIndex = idx;
                renderCurrentQuestion();
            });
            quickNavContainer.getChildren().add(navButton);
        }
    }

    private void onAnswerSelected(Question question, Answer answer) {
        selectedAnswerByQuestionId.put(question.getId(), answer.getId());

        if (answer.isCorrect()) {
            if (currentQuestionIndex < questions.size() - 1) {
                currentQuestionIndex++;
                renderCurrentQuestion();
            } else {
                onFinishQuiz();
            }
            return;
        }

        renderCurrentQuestion();
    }

    private int computeScore() {
        int score = 0;
        for (Question question : questions) {
            Integer selected = selectedAnswerByQuestionId.get(question.getId());
            if (selected == null) {
                continue;
            }
            boolean isCorrect = answers.stream()
                    .anyMatch(answer -> answer.getQuestion() != null
                            && Objects.equals(answer.getQuestion().getId(), question.getId())
                            && answer.isCorrect()
                            && Objects.equals(answer.getId(), selected));
            if (isCorrect) {
                score++;
            }
        }
        return score;
    }

    private void showSection(boolean introVisible, boolean questionVisible, boolean resultVisible) {
        introSection.setVisible(introVisible);
        introSection.setManaged(introVisible);

        questionSection.setVisible(questionVisible);
        questionSection.setManaged(questionVisible);

        resultSection.setVisible(resultVisible);
        resultSection.setManaged(resultVisible);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
