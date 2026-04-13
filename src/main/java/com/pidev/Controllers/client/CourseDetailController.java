package com.pidev.Controllers.client;

import com.pidev.Services.AnswerService;
import com.pidev.Services.QuestionService;
import com.pidev.Services.QuizService;
import com.pidev.models.Answer;
import com.pidev.models.Course;
import com.pidev.models.Question;
import com.pidev.models.Quiz;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CourseDetailController {
    @FXML private Label courseTitleLabel;
    @FXML private Label courseContentLabel;
    @FXML private Label quizTitleLabel;
    @FXML private Button startQuizButton;
    @FXML private VBox quizSection;
    @FXML private Label quizProgressLabel;
    @FXML private Label quizScoreLabel;
    @FXML private VBox questionCardContainer;
    @FXML private Button nextQuestionButton;
    @FXML private Button finishQuizButton;
    @FXML private Button backButton;

    private final QuizService quizService = new QuizService();
    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();
    private final Map<Integer, Integer> selectedAnswerByQuestionId = new HashMap<>();
    private List<Question> questions = new ArrayList<>();
    private List<Answer> answers = new ArrayList<>();
    private Quiz quiz;
    private Course course;
    private int currentQuestionIndex;

    @FXML
    public void initialize() {
        quizSection.setVisible(false);
        quizSection.setManaged(false);
        nextQuestionButton.setDisable(true);
        finishQuizButton.setDisable(true);
        quizScoreLabel.setText("Score: 0 / 0");
    }

    public void setCourse(Course course) {
        this.course = course;
        courseTitleLabel.setText(course != null && course.getTitle() != null ? course.getTitle() : "Cours");
        courseContentLabel.setText(course != null && course.getContent() != null && !course.getContent().isBlank()
                ? course.getContent()
                : "Contenu du cours non renseigne.");
        loadQuizForCourse();
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/base.fxml"));
            Parent root = loader.load();
            BaseController controller = loader.getController();
            controller.loadCourses();
            Scene scene = backButton.getScene();
            if (scene != null) {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            showError("Navigation", "Impossible de revenir a la liste des cours.");
        }
    }

    @FXML
    private void onStartQuiz() {
        if (quiz == null) {
            return;
        }
        selectedAnswerByQuestionId.clear();

        try {
            questions = questionService.findAll().stream()
                    .filter(item -> item.getQuiz() != null && Objects.equals(item.getQuiz().getId(), quiz.getId()))
                    .toList();
            answers = answerService.findAll();

            if (questions.isEmpty()) {
                questionCardContainer.getChildren().setAll(new Label("Aucune question pour ce quiz."));
                quizProgressLabel.setText("0 / 0");
                quizScoreLabel.setText("Score: 0 / 0");
                nextQuestionButton.setDisable(true);
                finishQuizButton.setDisable(true);
                showQuizSection(true);
                return;
            }

            currentQuestionIndex = 0;
            updateScoreLabel();
            renderCurrentQuestion();
            showQuizSection(true);
        } catch (SQLException e) {
            questionCardContainer.getChildren().setAll(new Label("Impossible de charger les questions et reponses du quiz."));
            quizProgressLabel.setText("0 / 0");
            quizScoreLabel.setText("Score: 0 / 0");
            nextQuestionButton.setDisable(true);
            finishQuizButton.setDisable(true);
            showQuizSection(true);
        }
    }

    @FXML
    private void onFinishQuiz() {
        int total = questions.size();
        int score = computeScore();
        int percentage = total == 0 ? 0 : Math.round((score * 100f) / total);
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Resultat du quiz");
        alert.setHeaderText("Quiz termine");
        alert.setContentText("Votre score: " + score + " / " + total + " (" + percentage + "%)");
        alert.showAndWait();
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

    private void loadQuizForCourse() {
        quiz = null;
        questions = new ArrayList<>();
        answers = new ArrayList<>();
        quizSection.setVisible(false);
        quizSection.setManaged(false);

        try {
            List<Quiz> quizzes = quizService.findAll();
            if (course != null) {
                quiz = quizzes.stream()
                        .filter(item -> item.getCourse() != null && Objects.equals(item.getCourse().getId(), course.getId()))
                        .findFirst()
                        .orElse(null);
            }

            if (quiz == null) {
                quizTitleLabel.setText("Aucun quiz disponible pour ce cours.");
                startQuizButton.setDisable(true);
                return;
            }

            quizTitleLabel.setText("Quiz: " + (quiz.getTitle() == null ? "Sans titre" : quiz.getTitle()));
            startQuizButton.setDisable(false);
        } catch (SQLException e) {
            quizTitleLabel.setText("Impossible de charger le quiz.");
            startQuizButton.setDisable(true);
        }
    }

    private void renderCurrentQuestion() {
        if (questions.isEmpty()) {
            questionCardContainer.getChildren().setAll(new Label("Aucune question pour ce quiz."));
            quizProgressLabel.setText("0 / 0");
            quizScoreLabel.setText("Score: 0 / 0");
            nextQuestionButton.setDisable(true);
            finishQuizButton.setDisable(true);
            return;
        }

        Question question = questions.get(currentQuestionIndex);
        quizProgressLabel.setText((currentQuestionIndex + 1) + " / " + questions.size());

        VBox questionCard = new VBox(12);
        questionCard.getStyleClass().add("question-block");

        Label questionLabel = new Label(question.getContent() == null ? "Sans contenu" : question.getContent());
        questionLabel.getStyleClass().add("question-title");
        questionLabel.setWrapText(true);
        questionCard.getChildren().add(questionLabel);

        VBox answersBox = new VBox(10);
        List<Answer> questionAnswers = answers.stream()
                .filter(answer -> answer.getQuestion() != null && Objects.equals(answer.getQuestion().getId(), question.getId()))
                .toList();

        if (questionAnswers.isEmpty()) {
            answersBox.getChildren().add(new Label("Aucune reponse configuree."));
        } else {
            Integer selectedAnswerId = selectedAnswerByQuestionId.get(question.getId());
            for (Answer answer : questionAnswers) {
                Button answerButton = new Button(answer.getContent() == null ? "Sans contenu" : answer.getContent());
                answerButton.getStyleClass().add("quiz-answer-option");
                answerButton.setMaxWidth(Double.MAX_VALUE);

                if (selectedAnswerId != null) {
                    if (answer.isCorrect()) {
                        answerButton.getStyleClass().add("correct-answer");
                    }
                    if (Objects.equals(selectedAnswerId, answer.getId())) {
                        answerButton.getStyleClass().add(answer.isCorrect() ? "selected-correct" : "selected-wrong");
                    }
                } else if (selectedAnswerByQuestionId.getOrDefault(question.getId(), -1) == answer.getId()) {
                    answerButton.getStyleClass().add("selected");
                }

                answerButton.setOnAction(event -> {
                    selectedAnswerByQuestionId.put(question.getId(), answer.getId());
                    updateScoreLabel();
                    if (currentQuestionIndex < questions.size() - 1) {
                        currentQuestionIndex++;
                    }
                    renderCurrentQuestion();
                });
                answersBox.getChildren().add(answerButton);
            }
        }

        questionCard.getChildren().add(answersBox);
        questionCardContainer.getChildren().setAll(questionCard);
        nextQuestionButton.setDisable(currentQuestionIndex >= questions.size() - 1);
        finishQuizButton.setDisable(selectedAnswerByQuestionId.size() < questions.size());
    }

    private void updateScoreLabel() {
        quizScoreLabel.setText("Score: " + computeScore() + " / " + questions.size());
    }

    private int computeScore() {
        int score = 0;
        for (Question question : questions) {
            Integer selectedAnswerId = selectedAnswerByQuestionId.get(question.getId());
            if (selectedAnswerId == null) {
                continue;
            }
            boolean good = answers.stream()
                    .anyMatch(answer -> answer.getQuestion() != null
                            && Objects.equals(answer.getQuestion().getId(), question.getId())
                            && answer.isCorrect()
                            && Objects.equals(answer.getId(), selectedAnswerId));
            if (good) {
                score++;
            }
        }
        return score;
    }

    private void showQuizSection(boolean visible) {
        quizSection.setVisible(visible);
        quizSection.setManaged(visible);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
