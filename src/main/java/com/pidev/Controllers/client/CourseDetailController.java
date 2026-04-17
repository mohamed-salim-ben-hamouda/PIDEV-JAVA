package com.pidev.Controllers.client;

import com.pidev.Services.AnswerService;
import com.pidev.Services.ChapterService;
import com.pidev.Services.QuestionService;
import com.pidev.Services.QuizService;
import com.pidev.models.Answer;
import com.pidev.models.Chapter;
import com.pidev.models.Course;
import com.pidev.models.Question;
import com.pidev.models.Quiz;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CourseDetailController {
    @FXML private Label courseTitleLabel;
    @FXML private Button openCoursePdfButton;
    @FXML private Label chapterCompletionLabel;
    @FXML private Label quizTitleLabel;
    @FXML private Label quizMetaLabel;
    @FXML private Label quizHintLabel;
    @FXML private Label quizStateBadge;
    @FXML private VBox chapterContainer;
    @FXML private Button startQuizButton;
    @FXML private VBox quizSection;
    @FXML private Label quizProgressLabel;
    @FXML private Label quizScoreLabel;
    @FXML private Label questionNavLabel;
    @FXML private VBox questionCardContainer;
    @FXML private Button prevQuestionButton;
    @FXML private Button nextQuestionButton;
    @FXML private Button finishQuizButton;
    @FXML private VBox resultCard;
    @FXML private Label resultTitleLabel;
    @FXML private Label resultScoreBadge;
    @FXML private Label resultSummaryLabel;
    @FXML private Label attemptInfoLabel;
    @FXML private VBox resultDetailsBox;
    @FXML private Button backButton;

    private final ChapterService chapterService = new ChapterService();
    private final QuizService quizService = new QuizService();
    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();

    private final Map<Integer, Integer> selectedAnswerByQuestionId = new HashMap<>();
    private final Map<Integer, QuizAttemptState> attemptsByQuizId = new HashMap<>();

    private List<Chapter> chapters = new ArrayList<>();
    private final Map<Integer, Quiz> quizByChapterId = new LinkedHashMap<>();
    private List<Question> questions = new ArrayList<>();
    private List<Answer> answers = new ArrayList<>();

    private Chapter selectedChapter;
    private Quiz quiz;
    private Course course;
    private int currentQuestionIndex;

    @FXML
    public void initialize() {
        showQuizSection(false);
        resultCard.setVisible(false);
        resultCard.setManaged(false);

        chapterCompletionLabel.setText("0 / 0 chapitres valides");
        questionNavLabel.setText("Question 0 sur 0");
        quizProgressLabel.setText("0 / 0");
        quizMetaLabel.setText("Aucun quiz selectionne");
        quizHintLabel.setText("Selectionnez un chapitre pour commencer.");
        quizStateBadge.setText("EN ATTENTE");
        quizStateBadge.getStyleClass().setAll("status-badge", "status-pending");

        prevQuestionButton.setDisable(true);
        nextQuestionButton.setDisable(true);
        finishQuizButton.setDisable(true);
        quizScoreLabel.setText("Score: 0 / 0");
    }

    public void setCourse(Course course) {
        this.course = course;
        courseTitleLabel.setText(course != null && course.getTitle() != null ? course.getTitle() : "Cours");
        String pdfRef = course != null ? course.getContent() : null;
        if (pdfRef != null && !pdfRef.isBlank()) {
            openCoursePdfButton.setText("Ouvrir le PDF du cours");
            openCoursePdfButton.setDisable(false);
        } else {
            openCoursePdfButton.setText("PDF du cours indisponible");
            openCoursePdfButton.setDisable(true);
        }
        loadCourseStructure();
    }

    @FXML
    private void onOpenCoursePdf() {
        if (course == null || course.getContent() == null || course.getContent().isBlank()) {
            showError("PDF", "Aucun PDF n'est renseigne pour ce cours.");
            return;
        }

        String pdfRef = course.getContent().trim();
        try {
            if (!Desktop.isDesktopSupported()) {
                showError("PDF", "Ouverture du PDF non supportee sur cette machine.");
                return;
            }

            if (pdfRef.startsWith("http://") || pdfRef.startsWith("https://")) {
                Desktop.getDesktop().browse(URI.create(pdfRef));
                return;
            }

            File pdfFile = new File(pdfRef);
            if (!pdfFile.exists()) {
                showError("PDF", "Fichier introuvable: " + pdfRef);
                return;
            }

            Desktop.getDesktop().open(pdfFile);
        } catch (Exception e) {
            showError("PDF", "Impossible d'ouvrir le PDF du cours.");
        }
    }

    @FXML
    private void onBack() {
        Scene scene = backButton.getScene();
        if (scene != null) {
            Object marker = scene.getRoot() != null ? scene.getRoot().getUserData() : null;
            Window window = scene.getWindow();
            if ("course-detail-window".equals(marker) && window != null) {
                window.hide();
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/base.fxml"));
            Parent root = loader.load();
            BaseController controller = loader.getController();
            controller.loadCourses();
            
            if (scene != null) {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            showError("Navigation", "Impossible de revenir a la liste des cours.");
        }
    }

    @FXML
    private void onStartQuiz() {
        openSelectedQuizWindow();
    }

    @FXML
    private void onFinishQuiz() {
        if (quiz == null || questions.isEmpty()) {
            return;
        }

        QuizComputation computation = computeQuizResult();
        int threshold = Math.round(quiz.getPassingScore() <= 0 ? 70f : quiz.getPassingScore());
        boolean passed = computation.percentage >= threshold;

        QuizAttemptState previous = attemptsByQuizId.get(quiz.getId());
        int currentAttempt = previous == null ? 1 : previous.attemptNumber + 1;
        attemptsByQuizId.put(quiz.getId(), new QuizAttemptState(currentAttempt, computation.percentage, passed));

        applyResultState(computation, threshold, currentAttempt, passed);
        updateChapterProgressLabel();
        renderChapterCards();
        updateQuizHeader();

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Resultat du quiz");
        alert.setHeaderText(passed ? "Quiz valide" : "Quiz non valide");
        alert.setContentText("Score: " + computation.earnedPoints + " / " + computation.totalPoints
                + " (" + computation.percentage + "%). Seuil requis: " + threshold + "%.");
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

    private void loadCourseStructure() {
        selectedAnswerByQuestionId.clear();
        quiz = null;
        selectedChapter = null;
        chapters = new ArrayList<>();
        quizByChapterId.clear();
        questions = new ArrayList<>();
        answers = new ArrayList<>();

        showQuizSection(false);
        resultCard.setVisible(false);
        resultCard.setManaged(false);
        questionCardContainer.getChildren().clear();

        try {
            List<Chapter> allChapters = chapterService.findAll();
            List<Quiz> quizzes = quizService.findAll();

            if (course != null) {
                chapters = allChapters.stream()
                        .filter(chapter -> chapter.getCourse() != null && Objects.equals(chapter.getCourse().getId(), course.getId()))
                        .sorted(Comparator.comparingInt(Chapter::getChapterOrder))
                        .toList();

                List<Quiz> quizzesForCourse = quizzes.stream()
                        .filter(item -> item.getCourse() != null && Objects.equals(item.getCourse().getId(), course.getId()))
                        .toList();

                for (Chapter chapter : chapters) {
                    Quiz linkedQuiz = quizzesForCourse.stream()
                            .filter(item -> item.getChapter() != null && Objects.equals(item.getChapter().getId(), chapter.getId()))
                            .findFirst()
                            .orElse(null);
                    if (linkedQuiz != null) {
                        quizByChapterId.put(chapter.getId(), linkedQuiz);
                    }
                }

                if (quizByChapterId.isEmpty() && !quizzesForCourse.isEmpty()) {
                    quiz = quizzesForCourse.get(0);
                } else if (!chapters.isEmpty()) {
                    for (Chapter chapter : chapters) {
                        Quiz chapterQuiz = quizByChapterId.get(chapter.getId());
                        if (chapterQuiz != null) {
                            selectedChapter = chapter;
                            quiz = chapterQuiz;
                            break;
                        }
                    }
                }
            }

            renderChapterCards();
            updateChapterProgressLabel();

            if (quiz == null) {
                quizTitleLabel.setText("Aucun quiz disponible pour ce cours.");
                quizMetaLabel.setText("Le cours n'a pas encore de quiz associe.");
                quizHintLabel.setText("Ajoutez des quiz en backoffice pour activer cette section.");
                quizStateBadge.setText("SANS QUIZ");
                quizStateBadge.getStyleClass().setAll("status-badge", "status-danger");
                startQuizButton.setDisable(true);
                return;
            }

            updateQuizHeader();
            startQuizButton.setDisable(false);
        } catch (SQLException e) {
            quizTitleLabel.setText("Impossible de charger le quiz.");
            quizMetaLabel.setText("Erreur de chargement.");
            quizHintLabel.setText("Verifiez la connexion a la base de donnees.");
            quizStateBadge.setText("ERREUR");
            quizStateBadge.getStyleClass().setAll("status-badge", "status-danger");
            startQuizButton.setDisable(true);
            chapterContainer.getChildren().setAll(buildInlineHint("Impossible de charger les chapitres du cours."));
        }
    }

    private void renderCurrentQuestion() {
        if (questions.isEmpty()) {
            questionCardContainer.getChildren().setAll(new Label("Aucune question pour ce quiz."));
            quizProgressLabel.setText("0 / 0");
            questionNavLabel.setText("Question 0 sur 0");
            quizScoreLabel.setText("Score: 0 / 0");
            prevQuestionButton.setDisable(true);
            nextQuestionButton.setDisable(true);
            finishQuizButton.setDisable(true);
            return;
        }

        Question question = questions.get(currentQuestionIndex);
        quizProgressLabel.setText((currentQuestionIndex + 1) + " / " + questions.size());
        questionNavLabel.setText("Question " + (currentQuestionIndex + 1) + " sur " + questions.size());

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

                if (selectedAnswerId != null && Objects.equals(selectedAnswerId, answer.getId())) {
                    answerButton.getStyleClass().add("selected");
                }

                answerButton.setOnAction(event -> {
                    selectedAnswerByQuestionId.put(question.getId(), answer.getId());
                    updateScoreLabel();

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
                });
                answersBox.getChildren().add(answerButton);
            }
        }

        questionCard.getChildren().add(answersBox);
        questionCardContainer.getChildren().setAll(questionCard);
        prevQuestionButton.setDisable(currentQuestionIndex == 0);
        nextQuestionButton.setDisable(currentQuestionIndex >= questions.size() - 1);
        finishQuizButton.setDisable(selectedAnswerByQuestionId.isEmpty());
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

    private QuizComputation computeQuizResult() {
        int totalPoints = 0;
        int earnedPoints = 0;
        for (Question question : questions) {
            int questionPoints = Math.max(1, Math.round(question.getPoint()));
            totalPoints += questionPoints;

            Integer selectedAnswerId = selectedAnswerByQuestionId.get(question.getId());
            boolean isCorrect = selectedAnswerId != null && answers.stream()
                    .anyMatch(answer -> answer.getQuestion() != null
                            && Objects.equals(answer.getQuestion().getId(), question.getId())
                            && answer.isCorrect()
                            && Objects.equals(answer.getId(), selectedAnswerId));

            if (isCorrect) {
                earnedPoints += questionPoints;
            }
        }

        int percentage = totalPoints == 0 ? 0 : Math.round((earnedPoints * 100f) / totalPoints);
        return new QuizComputation(totalPoints, earnedPoints, percentage);
    }

    private void applyResultState(QuizComputation computation, int threshold, int attemptNumber, boolean passed) {
        resultCard.setVisible(true);
        resultCard.setManaged(true);

        resultTitleLabel.setText(passed ? "Quiz valide" : "Quiz non valide");
        resultSummaryLabel.setText(passed
                ? "Excellent travail. Le chapitre est marque comme complete."
                : "Vous pouvez recommencer pour atteindre le score minimal.");
        resultScoreBadge.setText(computation.percentage + "%");
        resultScoreBadge.getStyleClass().setAll("result-score-badge", passed ? "success" : "fail");

        attemptInfoLabel.setText("Tentative " + attemptNumber + " | Seuil: " + threshold + "% | Points: "
                + computation.earnedPoints + " / " + computation.totalPoints);

        renderResultDetails();
    }

    private void renderResultDetails() {
        resultDetailsBox.getChildren().clear();

        for (Question question : questions) {
            VBox line = new VBox(6);
            line.getStyleClass().add("result-line");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label qLabel = new Label(question.getContent() == null ? "Question" : question.getContent());
            qLabel.getStyleClass().add("result-question");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Integer selectedAnswerId = selectedAnswerByQuestionId.get(question.getId());
            boolean good = selectedAnswerId != null && answers.stream()
                    .anyMatch(answer -> answer.getQuestion() != null
                            && Objects.equals(answer.getQuestion().getId(), question.getId())
                            && answer.isCorrect()
                            && Objects.equals(answer.getId(), selectedAnswerId));

            Label state = new Label(good ? "Correct" : "Incorrect");
            state.getStyleClass().setAll("mini-pill", good ? "mini-success" : "mini-danger");

            header.getChildren().addAll(qLabel, spacer, state);

            String selectedAnswerText = answers.stream()
                    .filter(answer -> Objects.equals(answer.getId(), selectedAnswerId))
                    .map(Answer::getContent)
                    .findFirst()
                    .orElse("Non repondu");
            Label selectedLabel = new Label("Votre reponse: " + selectedAnswerText);
            selectedLabel.getStyleClass().add("result-subline");

                line.getChildren().addAll(header, selectedLabel);
            resultDetailsBox.getChildren().add(line);
        }
    }

    private void renderChapterCards() {
        chapterContainer.getChildren().clear();

        if (chapters == null || chapters.isEmpty()) {
            chapterContainer.getChildren().add(buildInlineHint("Aucun chapitre configure pour ce cours."));
            return;
        }

        for (Chapter chapter : chapters) {
            chapterContainer.getChildren().add(buildChapterCard(chapter));
        }
    }

    private VBox buildChapterCard(Chapter chapter) {
        VBox card = new VBox(10);
        card.getStyleClass().add("chapter-item-card");
        card.setPadding(new Insets(14));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label order = new Label(String.valueOf(chapter.getChapterOrder()));
        order.getStyleClass().add("chapter-order-pill");

        VBox textWrap = new VBox(4);
        Label title = new Label(chapter.getTitle() == null ? "Chapitre" : chapter.getTitle());
        title.getStyleClass().add("chapter-item-title");
        Label excerpt = new Label(truncate(chapter.getContent(), 120));
        excerpt.getStyleClass().add("chapter-item-excerpt");
        excerpt.setWrapText(true);
        textWrap.getChildren().addAll(title, excerpt);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Quiz chapterQuiz = quizByChapterId.get(chapter.getId());
        Label badge = new Label();
        if (chapterQuiz == null) {
            badge.setText("Sans quiz");
            badge.getStyleClass().setAll("mini-pill", "mini-neutral");
        } else {
            QuizAttemptState state = attemptsByQuizId.get(chapterQuiz.getId());
            if (state != null && state.passed) {
                badge.setText("Valide " + state.scorePercent + "%");
                badge.getStyleClass().setAll("mini-pill", "mini-success");
            } else if (state != null) {
                badge.setText("A refaire " + state.scorePercent + "%");
                badge.getStyleClass().setAll("mini-pill", "mini-danger");
            } else {
                badge.setText("A faire");
                badge.getStyleClass().setAll("mini-pill", "mini-warning");
            }
        }

        header.getChildren().addAll(order, textWrap, spacer, badge);

        if (chapterQuiz != null) {
            Button openQuiz = new Button("Ouvrir session quiz");
            openQuiz.getStyleClass().setAll("courses-action-btn", "secondary");
            openQuiz.setOnAction(event -> {
                selectedChapter = chapter;
                quiz = chapterQuiz;
                updateQuizHeader();
                startQuizButton.setDisable(false);
                openSelectedQuizWindow();
            });
            card.getChildren().addAll(header, openQuiz);
        } else {
            card.getChildren().add(header);
        }

        return card;
    }

    private Label buildInlineHint(String text) {
        Label hint = new Label(text);
        hint.getStyleClass().add("course-empty-subtitle");
        return hint;
    }

    private void updateQuizHeader() {
        quizTitleLabel.setText("Quiz: " + (quiz.getTitle() == null ? "Sans titre" : quiz.getTitle()));

        int questionCount = countQuizQuestions(quiz.getId());
        int attempts = quiz.getMaxAttempts() <= 0 ? 3 : quiz.getMaxAttempts();
        int passing = Math.round(quiz.getPassingScore() <= 0 ? 70f : quiz.getPassingScore());
        quizMetaLabel.setText(questionCount + " questions | " + attempts + " tentatives max | score requis " + passing + "%");

        if (selectedChapter != null && selectedChapter.getTitle() != null) {
            quizHintLabel.setText("Chapitre: " + selectedChapter.getTitle());
        } else {
            quizHintLabel.setText("Quiz global du cours");
        }

        QuizAttemptState state = attemptsByQuizId.get(quiz.getId());
        if (state == null) {
            quizStateBadge.setText("NOUVEAU");
            quizStateBadge.getStyleClass().setAll("status-badge", "status-pending");
            return;
        }
        if (state.passed) {
            quizStateBadge.setText("VALIDE " + state.scorePercent + "%");
            quizStateBadge.getStyleClass().setAll("status-badge", "status-success");
        } else {
            quizStateBadge.setText("A REFAIRE " + state.scorePercent + "%");
            quizStateBadge.getStyleClass().setAll("status-badge", "status-danger");
        }
    }

    private int countQuizQuestions(Integer quizId) {
        if (quizId == null) {
            return 0;
        }
        if (questions != null && !questions.isEmpty() && questions.get(0).getQuiz() != null
                && Objects.equals(questions.get(0).getQuiz().getId(), quizId)) {
            return questions.size();
        }
        try {
            return (int) questionService.findAll().stream()
                    .filter(item -> item.getQuiz() != null && Objects.equals(item.getQuiz().getId(), quizId))
                    .count();
        } catch (SQLException e) {
            return 0;
        }
    }

    private void updateChapterProgressLabel() {
        if (chapters == null || chapters.isEmpty()) {
            chapterCompletionLabel.setText("0 / 0 chapitres valides");
            return;
        }

        int quizChapters = 0;
        int completed = 0;
        for (Chapter chapter : chapters) {
            Quiz chapterQuiz = quizByChapterId.get(chapter.getId());
            if (chapterQuiz == null) {
                continue;
            }
            quizChapters++;
            QuizAttemptState state = attemptsByQuizId.get(chapterQuiz.getId());
            if (state != null && state.passed) {
                completed++;
            }
        }
        chapterCompletionLabel.setText(completed + " / " + quizChapters + " chapitres valides");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Aucun contenu detaille.";
        }
        String normalized = value.replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
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

    private void openSelectedQuizWindow() {
        if (quiz == null) {
            showWarning("Quiz", "Aucun quiz disponible pour ce chapitre.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/QuizSessionView.fxml"));
            Parent root = loader.load();
            QuizSessionController controller = loader.getController();
            controller.setQuizContext(quiz, selectedChapter != null ? selectedChapter.getTitle() : "Quiz du cours");

            Stage stage = new Stage();
            stage.setTitle("Session Quiz - " + (quiz.getTitle() == null ? "Quiz" : quiz.getTitle()));
            stage.setScene(new Scene(root, 1080, 820));
            stage.setMinWidth(980);
            stage.setMinHeight(760);
            if (backButton.getScene() != null && backButton.getScene().getWindow() != null) {
                stage.initOwner(backButton.getScene().getWindow());
            }
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            showError("Quiz", "Impossible d'ouvrir la fenetre du quiz.");
        }
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private record QuizAttemptState(int attemptNumber, int scorePercent, boolean passed) {
    }

    private record QuizComputation(int totalPoints, int earnedPoints, int percentage) {
    }
}
