package com.pidev.Controllers.client;

import com.pidev.Services.AnswerService;
import com.pidev.Services.QuestionService;
import com.pidev.Services.QuizStatisticsService;
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
import javafx.application.Platform;
import javafx.scene.Node;
import java.util.concurrent.CompletableFuture;
import com.pidev.Services.AiFeedbackService;
import com.pidev.Services.TranslationService;
import com.pidev.Services.CertificateGeneratorService;
import javafx.stage.FileChooser;
import java.io.File;
import java.awt.Desktop;

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
    @FXML private Label answeredCounterLabel;
    @FXML private Label liveScoreLabel;
    @FXML private ProgressBar questionProgressBar;
    @FXML private Label timerBadgeLabel;
    @FXML private Label questionTextLabel;
    @FXML private Label questionFeedbackLabel;
    @FXML private VBox answersContainer;
    @FXML private HBox quickNavContainer;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;

    @FXML private Label resultTitleLabel;
    @FXML private Label resultSummaryLabel;
    @FXML private Label resultScoreLabel;
    @FXML private Label aiFeedbackLabel;
    @FXML private Button downloadCertButton;

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();
    private final QuizStatisticsService quizStatisticsService = new QuizStatisticsService();
    private final AiFeedbackService aiFeedbackService = new AiFeedbackService();
    private final TranslationService translationService = new TranslationService();
    private final CertificateGeneratorService certificateGeneratorService = new CertificateGeneratorService();

    private final Map<Integer, Integer> selectedAnswerByQuestionId = new HashMap<>();
    private List<Question> questions = new ArrayList<>();
    private List<Answer> answers = new ArrayList<>();

    private Quiz quiz;
    private int currentQuestionIndex;
    private int remainingSeconds;
    private Timeline countdownTimeline;
    private Timeline flashTimeline;       // NOUVEAU : Timeline dédiée au clignotement
    private boolean flashVisible = true;  // NOUVEAU : état du clignotement
    private boolean isTranslatedFr = false;

    @FXML
    public void initialize() {
        showSection(true, false, false);
        timerBadgeLabel.setText("Libre");
        if (answeredCounterLabel != null) {
            answeredCounterLabel.setText("0 / 0 repondues");
        }
        if (questionFeedbackLabel != null) {
            questionFeedbackLabel.setText("");
        }
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
        if (questionFeedbackLabel != null) {
            questionFeedbackLabel.setText("");
        }
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

        int unanswered = questions.size() - selectedAnswerByQuestionId.size();
        if (unanswered > 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Soumettre le quiz");
            confirm.setHeaderText("Questions non repondues: " + unanswered);
            confirm.setContentText("Voulez-vous terminer le quiz maintenant ? Les questions non repondues seront comptees comme incorrectes.");
            if (confirm.showAndWait().isEmpty() || confirm.getResult().getButtonData().isCancelButton()) {
                return;
            }
        }

        stopCountdown();

        int total = questions.size();
        int score = computeScore();
        int percent = total == 0 ? 0 : Math.round(score * 100f / total);
        int passing = Math.round(quiz.getPassingScore() <= 0 ? 70f : quiz.getPassingScore());
        boolean passed = percent >= passing;
        int attemptNumber = 1;

        try {
            attemptNumber = quizStatisticsService.saveQuizAttempt(quiz.getId(), null, percent);
        } catch (SQLException e) {
            showError("Sauvegarde quiz", "Tentative non enregistree en base: " + e.getMessage());
        }

        resultTitleLabel.setText(passed ? "Quiz valide" : "Quiz non valide");
        resultSummaryLabel.setText(
                passed
                        ? "Bravo, vous avez atteint le score requis. Tentative " + attemptNumber + "."
                        : "Vous pouvez relancer le quiz pour améliorer votre resultat. Tentative " + attemptNumber + "."
        );
        resultScoreLabel.setText(score + " / " + total + " (" + percent + "%)");
        resultScoreLabel.getStyleClass().setAll("result-score-badge", passed ? "success" : "fail");

        if (!passed) {
            List<Question> failedQuestions = questions.stream()
                .filter(q -> {
                    Integer sel = selectedAnswerByQuestionId.get(q.getId());
                    return sel == null || answers.stream().noneMatch(a -> a.getQuestion() != null && Objects.equals(a.getQuestion().getId(), q.getId()) && a.isCorrect() && Objects.equals(a.getId(), sel));
                })
                .toList();
            String feedback = aiFeedbackService.generateFeedback(failedQuestions);
            aiFeedbackLabel.setText(feedback);
            aiFeedbackLabel.setVisible(true);
            aiFeedbackLabel.setManaged(true);
            if (downloadCertButton != null) {
                downloadCertButton.setVisible(false);
                downloadCertButton.setManaged(false);
            }
        } else {
            aiFeedbackLabel.setVisible(false);
            aiFeedbackLabel.setManaged(false);
            if (downloadCertButton != null) {
                downloadCertButton.setVisible(true);
                downloadCertButton.setManaged(true);
            }
        }

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

    @FXML
    private void onDownloadCertificate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le certificat");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Certificat_" + (quiz != null && quiz.getTitle() != null ? quiz.getTitle().replaceAll("\\s+", "_") : "Quiz") + ".pdf");
        Window window = quizTitleLabel.getScene() != null ? quizTitleLabel.getScene().getWindow() : null;
        if (window == null) return;
        File file = fileChooser.showSaveDialog(window);
        
        if (file != null) {
            try {
                int total = questions.size();
                int score = computeScore();
                int percent = total == 0 ? 0 : Math.round(score * 100f / total);
                String courseName = quizContextLabel.getText();
                certificateGeneratorService.generateCertificate(file.getAbsolutePath(), "Étudiant", courseName, percent);
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Succès");
                success.setHeaderText("Certificat généré avec succès !");
                success.setContentText("Le certificat a été sauvegardé dans : " + file.getAbsolutePath());
                success.showAndWait();
                
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    // Ignorer
                }
            } catch (Exception e) {
                showError("Erreur", "Impossible de générer le certificat : " + e.getMessage());
            }
        }
    }

    private void startCountdown() {
        stopCountdown();
        int timeLimitMinutes = quiz == null ? 0 : quiz.getTimeLimit();
        if (timeLimitMinutes <= 0) {
            timerBadgeLabel.setText("Libre");
            timerBadgeLabel.getStyleClass().setAll("timer-normal");
            return;
        }

        remainingSeconds = timeLimitMinutes * 60;
        updateTimerDisplay();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateTimerDisplay();
            if (remainingSeconds <= 0) {
                stopCountdown();
                Platform.runLater(this::onFinishQuiz);
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
        stopFlashAnimation();
        // Réinitialiser le style du timer
        timerBadgeLabel.getStyleClass().setAll("timer-normal");
        timerBadgeLabel.setOpacity(1.0);
    }

    private String formatTime(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        int minutes = safeSeconds / 60;
        int rest = safeSeconds % 60;
        return String.format("%02d:%02d", minutes, rest);
    }
    
    private void updateTimerDisplay() {
        int safeSeconds = Math.max(0, remainingSeconds);
        timerBadgeLabel.setText(formatTime(safeSeconds));

        if (safeSeconds <= 0) {
            // Temps écoulé
            timerBadgeLabel.getStyleClass().setAll("flash-animation");
            timerBadgeLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        if (safeSeconds <= 10) {
            // 10 dernières secondes : clignotement rouge rapide (via Timeline)
            timerBadgeLabel.getStyleClass().setAll("flash-animation");
            timerBadgeLabel.setOpacity(1.0); // opacity gérée par flashTimeline
            startFlashAnimation();

        } else if (safeSeconds <= 30) {
            // 30 → 11 secondes : pulse orange (style uniquement, pas de clignotement)
            stopFlashAnimation();
            timerBadgeLabel.setOpacity(1.0);
            timerBadgeLabel.getStyleClass().setAll("pulse-animation");
            timerBadgeLabel.setStyle(""); // Laisser le CSS prendre le relais

        } else if (safeSeconds <= 120) {
            // 2 minutes → 31 secondes : rouge texte, pas d'animation
            stopFlashAnimation();
            timerBadgeLabel.setOpacity(1.0);
            timerBadgeLabel.getStyleClass().setAll("timer-normal");
            timerBadgeLabel.setStyle("-fx-text-fill: #dc2626;");

        } else {
            // Temps normal
            stopFlashAnimation();
            timerBadgeLabel.setOpacity(1.0);
            timerBadgeLabel.getStyleClass().setAll("timer-normal");
            timerBadgeLabel.setStyle("-fx-text-fill: #1f2937;");
        }
    }
    
    private void startFlashAnimation() {
        if (flashTimeline != null && flashTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return; // Déjà en cours
        }

        flashVisible = true;
        // Clignote toutes les 400ms (rouge vif ↔ quasi invisible)
        flashTimeline = new Timeline(
            new KeyFrame(Duration.millis(400), event -> {
                flashVisible = !flashVisible;
                timerBadgeLabel.setOpacity(flashVisible ? 1.0 : 0.15);
            })
        );
        flashTimeline.setCycleCount(Timeline.INDEFINITE);
        flashTimeline.play();
    }
    
    private void stopFlashAnimation() {
        if (flashTimeline != null) {
            flashTimeline.stop();
            flashTimeline = null;
        }
        timerBadgeLabel.setOpacity(1.0);
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
        answeredCounterLabel.setText(selectedAnswerByQuestionId.size() + " / " + questions.size() + " repondues");
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

        if (questionFeedbackLabel != null) {
            questionFeedbackLabel.setText("");
        }

        renderQuickNav();
        prevButton.setDisable(currentQuestionIndex == 0);
        nextButton.setDisable(currentQuestionIndex >= questions.size() - 1);
        finishButton.setDisable(false);
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
        if (questionFeedbackLabel != null) {
            questionFeedbackLabel.setText("");
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
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void onTranslate() {
        if (questions.isEmpty()) {
            showWarning("Traduction", "Aucune question disponible pour la traduction.");
            return;
        }
        
        isTranslatedFr = !isTranslatedFr;
        String targetLang = isTranslatedFr ? "fr" : "en";
        
        // Désactiver le bouton de traduction temporairement
        Button translateBtn = findTranslateButton();
        if (translateBtn != null) {
            translateBtn.setDisable(true);
            translateBtn.setText("⏳ Traduction en cours...");
        }

        // Lancer la traduction dans un thread séparé avec un timeout
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[QuizSessionController] Début de la traduction vers " + targetLang);
                
                // Traduire les questions
                for (int i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    if (q.getContent() != null && !q.getContent().isBlank()) {
                        System.out.println("[QuizSessionController] Traduction question " + (i+1) + "/" + questions.size());
                        String translated = translationService.translate(q.getContent(), targetLang);
                        q.setContent(translated);
                    }
                }
                
                // Traduire les réponses
                for (int i = 0; i < answers.size(); i++) {
                    Answer a = answers.get(i);
                    if (a.getContent() != null && !a.getContent().isBlank()) {
                        if (i % 10 == 0) {
                            System.out.println("[QuizSessionController] Traduction réponse " + (i+1) + "/" + answers.size());
                        }
                        String translated = translationService.translate(a.getContent(), targetLang);
                        a.setContent(translated);
                    }
                }
                
                System.out.println("[QuizSessionController] Traduction terminée avec succès");
                
                // Mettre à jour l'UI sur le thread JavaFX
                Platform.runLater(() -> {
                    if (translateBtn != null) {
                        translateBtn.setDisable(false);
                        translateBtn.setText(isTranslatedFr ? "🌐 En Français" : "🌐 En Anglais");
                    }
                    
                    if (questionSection.isVisible()) {
                        renderCurrentQuestion();
                    }
                    
                    showInfo("Traduction", "Traduction réussie en " + (isTranslatedFr ? "français" : "anglais"));
                });
                
            } catch (Exception e) {
                System.err.println("[QuizSessionController] Erreur lors de la traduction: " + e.getMessage());
                e.printStackTrace();
                
                // Restaurer l'état en cas d'erreur
                Platform.runLater(() -> {
                    isTranslatedFr = !isTranslatedFr; // Inverser pour revenir à l'état initial
                    if (translateBtn != null) {
                        translateBtn.setDisable(false);
                        translateBtn.setText("🌐 Traduire");
                    }
                    showError("Traduction", "Erreur lors de la traduction: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * Trouve le bouton de traduction dans le FXML
     */
    private Button findTranslateButton() {
        if (quizTitleLabel == null || quizTitleLabel.getScene() == null) {
            return null;
        }
        
        // Chercher le bouton avec le texte contenant le globe
        for (Node node : quizTitleLabel.getScene().getRoot().lookupAll("Button")) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn.getText() != null && btn.getText().contains("🌐")) {
                    return btn;
                }
            }
        }
        
        return null;
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
