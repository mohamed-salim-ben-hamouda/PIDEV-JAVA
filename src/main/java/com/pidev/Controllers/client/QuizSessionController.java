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
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
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
import java.util.Optional;


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
    @FXML private StackPane questionSectionWrap;

    @FXML private Label questionNavLabel;
    @FXML private Label answeredCounterLabel;
    @FXML private Label liveScoreLabel;
    @FXML private ProgressBar questionProgressBar;
    @FXML private Label timerBadgeLabel;
    @FXML private StackPane timerBadgeContainer;
    @FXML private Region quizWarningOverlay;
    @FXML private Arc    timerRingFill;
    @FXML private Label  timerDigitsLabel;
    @FXML private Label  timerUnitLabel;
    @FXML private Label  timerStatusLabel;
    @FXML private StackPane timerRingContainer;
    @FXML private Label questionTextLabel;
    @FXML private Label questionFeedbackLabel;
    @FXML private VBox answersContainer;
    @FXML private HBox quickNavContainer;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;
    @FXML private Button retryButton;

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

    // État statique pour la persistance du timer (même si on ferme la fenêtre)
    private static final Map<Integer, Long> activeQuizStartTimes = new HashMap<>();
    private static final Map<Integer, Map<Integer, Integer>> activeQuizAnswers = new HashMap<>();

    private final Map<Integer, Integer> selectedAnswerByQuestionId = new HashMap<>();
    private com.pidev.models.User currentUser; // l'étudiant connecté
    private List<Question> questions = new ArrayList<>();
    private List<Answer> answers = new ArrayList<>();

    private Quiz quiz;
    private int currentQuestionIndex;
    private int remainingSeconds;
    private int attemptsUsed;
    private int maxAttempts;
    private Timeline countdownTimeline;
    private Timeline flashTimeline;
    private FadeTransition warningFadeTransition;
    private boolean  flashVisible   = true;
    
    // Longueur totale de l'arc = 360° (cercle complet)
    // On utilise un Arc JavaFX dont la propriété "length" va de 360 → 0
    private int totalSeconds = 0;
    
    // false = quiz en français (état original)
    // true  = quiz en anglais (après traduction)
    private boolean isTranslatedToEn = false;

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

    public void setCurrentUser(com.pidev.models.User user) {
        this.currentUser = user;
    }

    public void setQuizContext(Quiz quiz, String chapterName) {
        this.quiz = quiz;
        String title = quiz != null && quiz.getTitle() != null ? quiz.getTitle() : "Quiz";
        quizTitleLabel.setText(title);
        quizContextLabel.setText(chapterName == null || chapterName.isBlank() ? "Quiz du cours" : chapterName);

        maxAttempts = quiz != null && quiz.getMaxAttempts() > 0 ? quiz.getMaxAttempts() : 3;
        int passing = quiz != null ? Math.round(quiz.getPassingScore() <= 0 ? 70f : quiz.getPassingScore()) : 70;
        int time = quiz != null ? quiz.getTimeLimit() : 0;
        attemptsUsed = resolveAttemptsUsed();

        attemptLabel.setText(formatAttemptLabel());
        requiredScoreLabel.setText(passing + "%");
        timeLimitLabel.setText(time <= 0 ? "Libre" : time + " min");

        // Vérifier si une session est déjà en cours pour ce quiz
        if (quiz != null && quiz.getId() != null && activeQuizStartTimes.containsKey(quiz.getId())) {
            long startTime = activeQuizStartTimes.get(quiz.getId());
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            remainingSeconds = (time * 60) - (int) elapsedSeconds;
            
            // Recharger les réponses déjà sélectionnées
            Map<Integer, Integer> savedAnswers = activeQuizAnswers.get(quiz.getId());
            if (savedAnswers != null) {
                selectedAnswerByQuestionId.putAll(savedAnswers);
            }
            
            if (remainingSeconds < 0) remainingSeconds = 0;
            
            System.out.println("[QuizSessionController] Reprise de la session active. Temps restant: " + remainingSeconds + "s");
            
            // On bypass l'intro pour retourner au quiz
            Platform.runLater(this::onStartQuiz);
        } else {
            remainingSeconds = Math.max(0, time) * 60;
        }

        timerBadgeLabel.setText(time <= 0 ? "Libre" : formatTime(remainingSeconds));
        if (retryButton != null) {
            retryButton.setDisable(attemptsUsed >= maxAttempts);
        }

        loadQuizData();
        
        // NOUVEAU : Si le quiz est déjà réussi, on affiche le bouton de téléchargement 
        // dès l'intro pour éviter de devoir le repasser.
        checkIfAlreadyPassed();
    }

    private void checkIfAlreadyPassed() {
        try {
            // RÉCUPÉRATION DU STUDENT_ID DEPUIS LA SESSION
            com.pidev.models.User sessionUser = com.pidev.utils.UserSession.getCurrentUser();
            Integer studentId = (sessionUser != null) ? sessionUser.getId() : null;

            List<com.pidev.models.QuizAttemptDetail> history = quizStatisticsService.findAttemptsForStudentQuiz(quiz.getId(), quiz.getPassingScore(), studentId);
            Optional<com.pidev.models.QuizAttemptDetail> bestPass = history.stream()
                .filter(com.pidev.models.QuizAttemptDetail::isPassed)
                .max(Comparator.comparingDouble(com.pidev.models.QuizAttemptDetail::getScore));
            
            if (bestPass.isPresent()) {
                com.pidev.models.QuizAttemptDetail attempt = bestPass.get();
                int percent = (int)Math.round(attempt.getScore());
                
                // On pré-remplit l'affichage des résultats
                resultTitleLabel.setText("Quiz déjà validé");
                resultSummaryLabel.setText("Vous avez réussi ce quiz lors d'une tentative précédente.");
                resultScoreLabel.setText(percent + "%");
                resultScoreLabel.getStyleClass().setAll("result-score-badge", "success");
                
                if (downloadCertButton != null) {
                    downloadCertButton.setVisible(true);
                    downloadCertButton.setManaged(true);
                }
                
                // Afficher la section résultat au lieu de l'intro
                showSection(false, false, true);
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification historique quiz: " + e.getMessage());
        }
    }

    @FXML
    private void onStartQuiz() {
        if (quiz != null && attemptsUsed >= maxAttempts && !activeQuizStartTimes.containsKey(quiz.getId())) {
            showWarning("Tentatives epuisees", "Vous avez atteint la limite de " + maxAttempts + " tentative(s) pour ce quiz.");
            return;
        }
        if (questions.isEmpty()) {
            showError("Quiz", "Aucune question disponible pour ce quiz.");
            return;
        }

        // Initialiser l'état de session si nouveau
        if (quiz != null && quiz.getId() != null && !activeQuizStartTimes.containsKey(quiz.getId())) {
            activeQuizStartTimes.put(quiz.getId(), System.currentTimeMillis());
            activeQuizAnswers.put(quiz.getId(), selectedAnswerByQuestionId);
            selectedAnswerByQuestionId.clear();
            currentQuestionIndex = 0;
        }

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
        onFinishQuiz(false);
    }

    private void onFinishQuiz(boolean isAutoSubmit) {
        if (questions.isEmpty() || quiz == null) {
            return;
        }

        // Si c'est le timer qui appelle, on passe la confirmation
        if (!isAutoSubmit) {
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
        }

        // Nettoyage de la session active
        activeQuizStartTimes.remove(quiz.getId());
        activeQuizAnswers.remove(quiz.getId());

        stopCountdown();

        int total = questions.size();
        int score = computeScore();
        int percent = total == 0 ? 0 : Math.round(score * 100f / total);
        int passing = Math.round(quiz.getPassingScore() <= 0 ? 70f : quiz.getPassingScore());
        boolean passed = percent >= passing;
        int attemptNumber = attemptsUsed + 1;

        try {
            // RÉCUPÉRATION DU STUDENT_ID DEPUIS LA SESSION
            Integer studentId = (currentUser != null) ? currentUser.getId() : null;
            if (studentId == null) {
                com.pidev.models.User sessionUser = com.pidev.utils.UserSession.getCurrentUser();
                if (sessionUser != null) {
                    studentId = sessionUser.getId();
                    this.currentUser = sessionUser;
                }
            }
            
            // On passe enfin le vrai studentId (au lieu de null)
            attemptNumber = quizStatisticsService.saveQuizAttempt(quiz.getId(), studentId, percent);
        } catch (SQLException e) {
            showError("Sauvegarde quiz", "Tentative non enregistree en base: " + e.getMessage());
            e.printStackTrace();
        }
        attemptsUsed = Math.max(attemptsUsed, attemptNumber);
        attemptLabel.setText(formatAttemptLabel());

        resultTitleLabel.setText(passed ? "Quiz valide" : "Quiz non valide");
        boolean attemptsRemaining = attemptsUsed < maxAttempts;
        resultSummaryLabel.setText(buildResultSummary(passed, attemptNumber, attemptsRemaining));
        // legacy summary removed
        /* old summary branch removed
                        : "Vous pouvez relancer le quiz pour améliorer votre resultat. Tentative " + attemptNumber + "."
        */
        resultScoreLabel.setText(score + " / " + total + " (" + percent + "%)");
        resultScoreLabel.getStyleClass().setAll("result-score-badge", passed ? "success" : "fail");
        if (retryButton != null) {
            retryButton.setDisable(!attemptsRemaining);
        }

        if (!passed) {
            AiFeedbackService.FeedbackResult feedbackResult = aiFeedbackService.generateFeedback(
                    quiz,
                    questions,
                    answers,
                    selectedAnswerByQuestionId
            );
            aiFeedbackLabel.setText(aiFeedbackService.formatForDisplay(feedbackResult));
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
                // Toujours afficher le bouton si réussi
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
        if (quiz != null && attemptsUsed >= maxAttempts) {
            showWarning("Tentatives epuisees", "Vous avez atteint la limite de " + maxAttempts + " tentative(s) pour ce quiz.");
            return;
        }
        onStartQuiz();
    }

    @FXML
    private void onDownloadCertificate() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le certificat");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        // Nom de fichier suggéré avec le nom de l'étudiant
        String safeStudentName = buildStudentName().replaceAll("[^a-zA-ZÀ-ÿ0-9_\\-]", "_");
        String safeCourseName  = (quiz != null && quiz.getTitle() != null)
                ? quiz.getTitle().replaceAll("\\s+", "_") : "Quiz";
        fileChooser.setInitialFileName("Certificat_" + safeStudentName + "_" + safeCourseName + ".pdf");

        javafx.stage.Window window = quizTitleLabel.getScene() != null
                ? quizTitleLabel.getScene().getWindow() : null;
        if (window == null) return;

        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) return;

        try {
            int total   = questions.size();
            int score   = computeScore();
            int percent = total == 0 ? 0 : Math.round(score * 100f / total);

            // Si le score actuel est 0 (ex: on vient d'ouvrir un quiz déjà réussi), 
            // on cherche le meilleur score dans l'historique.
            if (percent == 0) {
                com.pidev.models.User sessionUser = com.pidev.utils.UserSession.getCurrentUser();
                Integer studentId = (sessionUser != null) ? sessionUser.getId() : null;
                
                List<com.pidev.models.QuizAttemptDetail> history = quizStatisticsService.findAttemptsForStudentQuiz(quiz.getId(), quiz.getPassingScore(), studentId);
                percent = history.stream()
                    .filter(com.pidev.models.QuizAttemptDetail::isPassed)
                    .mapToInt(a -> (int)Math.round(a.getScore()))
                    .max()
                    .orElse(percent);
            }

            // RÉCUPÉRATION DIRECTE DE LA SESSION (plus fiable que l'injection si celle-ci a échoué)
            com.pidev.models.User sessionUser = com.pidev.utils.UserSession.getCurrentUser();
            if (sessionUser != null) {
                this.currentUser = sessionUser;
            }

            String studentName = buildStudentName(); // ← vrai nom ici
            String courseName  = quizContextLabel.getText();
            
            System.out.println("[QuizSessionController] Nom détecté pour certificat : '" + studentName + "'");

            certificateGeneratorService.generateCertificate(
                    file.getAbsolutePath(),
                    studentName,   // ← plus de "Étudiant" hardcodé
                    courseName,
                    percent
            );

            javafx.scene.control.Alert success = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            success.setTitle("Certificat généré");
            success.setHeaderText("Félicitations " + studentName + " !");
            success.setContentText("Certificat sauvegardé :\n" + file.getAbsolutePath());
            success.showAndWait();

            // Ouvrir le PDF automatiquement
            try { java.awt.Desktop.getDesktop().open(file); }
            catch (Exception ignored) {}

        } catch (Exception e) {
            showError("Erreur", "Impossible de générer le certificat : " + e.getMessage());
        }
    }

    private int resolveAttemptsUsed() {
        if (quiz == null || quiz.getId() == null) {
            return 0;
        }
        try {
            com.pidev.models.User sessionUser = com.pidev.utils.UserSession.getCurrentUser();
            Integer studentId = (sessionUser != null) ? sessionUser.getId() : null;
            return quizStatisticsService.countStudentAttemptsForQuiz(quiz.getId(), studentId);
        } catch (SQLException e) {
            return 0;
        }
    }

    private String formatAttemptLabel() {
        int nextAttempt = Math.min(attemptsUsed + 1, maxAttempts);
        if (attemptsUsed >= maxAttempts) {
            nextAttempt = maxAttempts;
        }
        return nextAttempt + "/" + maxAttempts;
    }

    private String buildResultSummary(boolean passed, int attemptNumber, boolean attemptsRemaining) {
        if (passed) {
            return "Bravo, vous avez atteint le score requis. Tentative " + attemptNumber + ".";
        }
        if (attemptsRemaining) {
            return "Vous pouvez relancer le quiz pour ameliorer votre resultat. Tentative " + attemptNumber + ".";
        }
        return "Limite de tentatives atteinte. Vous ne pouvez plus relancer ce quiz.";
    }

    private void startCountdown() {
        stopCountdown();
        int timeLimitMinutes = quiz == null ? 0 : quiz.getTimeLimit();

        if (timeLimitMinutes <= 0) {
            // Mode libre : pas de timer
            totalSeconds    = 0;
            remainingSeconds = 0;
            applyTimerState("free");
            if (timerDigitsLabel != null) timerDigitsLabel.setText("∞");
            if (timerUnitLabel   != null) timerUnitLabel.setText("libre");
            if (timerStatusLabel != null) timerStatusLabel.setText("Sans limite");
            // Compatibilité avec l'ancien timerBadgeLabel
            if (timerBadgeLabel != null) timerBadgeLabel.setText("Libre");
            return;
        }

        totalSeconds     = timeLimitMinutes * 60;
        // On ne réinitialise remainingSeconds que si ce n'est pas une reprise
        if (quiz != null && !activeQuizStartTimes.containsKey(quiz.getId())) {
            remainingSeconds = totalSeconds;
        }
        updateTimerDisplay();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerDisplay();
            if (remainingSeconds <= 0) {
                stopCountdown();
                Platform.runLater(() -> onFinishQuiz(true));
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
        applyTimerState("normal");
        if (timerRingFill != null) timerRingFill.setOpacity(1.0);
        if (quizWarningOverlay != null) quizWarningOverlay.setOpacity(0.0);
        clearQuizPanelAlertState();
    }

    private String formatTime(int seconds) {
        int s = Math.max(0, seconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }
    
    private void updateTimerDisplay() {
        int safe = Math.max(0, remainingSeconds);
        String timeText = formatTime(safe);

        // Mettre à jour les deux systèmes (nouveau anneau + ancien badge)
        if (timerDigitsLabel != null) timerDigitsLabel.setText(timeText);
        if (timerBadgeLabel  != null) timerBadgeLabel.setText(timeText);

        // Mise à jour de l'anneau : arc proportionnel au temps restant
        if (timerRingFill != null && totalSeconds > 0) {
            double pct    = (double) safe / totalSeconds;
            double length = 360.0 * pct; // 360° = plein, 0° = vide
            timerRingFill.setStartAngle(90);  // départ en haut
            timerRingFill.setLength(length);
        }

        if (safe <= 0) {
            stopFlashAnimation();
            applyTimerState("done");
            if (timerStatusLabel != null) timerStatusLabel.setText("Temps écoulé !");
            if (timerUnitLabel   != null) timerUnitLabel.setText("");
            return;
        }

        if (safe <= 10) {
            // DANGER : clignotement rouge rapide
            applyTimerState("danger");
            if (timerStatusLabel != null) timerStatusLabel.setText("Terminez !");
            if (timerUnitLabel   != null) timerUnitLabel.setText("secondes !");
            startFlashAnimation();

        } else if (safe <= 30) {
            // WARNING : pulse orange (opacity gérée par Timeline)
            applyTimerState("warning");
            if (timerStatusLabel != null) timerStatusLabel.setText("Dépêchez-vous");
            if (timerUnitLabel   != null) timerUnitLabel.setText("restantes");
            startPulseAnimation();

        } else {
            // NORMAL
            stopFlashAnimation();
            applyTimerState("normal");
            if (timerStatusLabel != null) timerStatusLabel.setText("En cours");
            if (timerUnitLabel   != null) timerUnitLabel.setText("restantes");
        }
    }

    // -------------------------------------------------------
    // applyTimerState() — gère les classes CSS sur le conteneur
    // -------------------------------------------------------
    private void applyTimerState(String state) {
        if (timerRingContainer == null) {
            updateTimerBadgeState(state);
        } else {
            // Retirer tous les états précédents
            timerRingContainer.getStyleClass().removeAll(
                "timer-warning", "timer-danger", "timer-done", "timer-free"
            );
            // Appliquer le nouvel état
            switch (state) {
                case "warning" -> timerRingContainer.getStyleClass().add("timer-warning");
                case "danger"  -> timerRingContainer.getStyleClass().add("timer-danger");
                case "done"    -> timerRingContainer.getStyleClass().add("timer-done");
                case "free"    -> timerRingContainer.getStyleClass().add("timer-free");
                default -> {
                }
            }
        }
        updateTimerBadgeState(state);
        updateQuizPanelState(state);
    }

    private void updateTimerBadgeState(String state) {
        if (timerBadgeContainer != null) {
            timerBadgeContainer.getStyleClass().removeAll(
                    "timer-badge-warning",
                    "timer-badge-danger",
                    "timer-badge-done",
                    "timer-badge-free"
            );
        }

        if (timerBadgeLabel != null) {
            timerBadgeLabel.setStyle("");
        }

        switch (state) {
            case "warning" -> {
                if (timerBadgeContainer != null) {
                    timerBadgeContainer.getStyleClass().add("timer-badge-warning");
                } else if (timerBadgeLabel != null) {
                    timerBadgeLabel.setStyle("-fx-text-fill: #b45309;");
                }
            }
            case "danger" -> {
                if (timerBadgeContainer != null) {
                    timerBadgeContainer.getStyleClass().add("timer-badge-danger");
                } else if (timerBadgeLabel != null) {
                    timerBadgeLabel.setStyle("-fx-text-fill: #b91c1c;");
                }
            }
            case "done" -> {
                if (timerBadgeContainer != null) {
                    timerBadgeContainer.getStyleClass().add("timer-badge-done");
                } else if (timerBadgeLabel != null) {
                    timerBadgeLabel.setStyle("-fx-text-fill: #991b1b;");
                }
            }
            case "free" -> {
                if (timerBadgeContainer != null) {
                    timerBadgeContainer.getStyleClass().add("timer-badge-free");
                } else if (timerBadgeLabel != null) {
                    timerBadgeLabel.setStyle("-fx-text-fill: #475569;");
                }
            }
            default -> {
            }
        }
    }

    // -------------------------------------------------------
    // startFlashAnimation() — clignotement 10s finales
    // -------------------------------------------------------
    private void startFlashAnimation() {
        stopPulseAnimation(); // S'assurer qu'il n'y a pas de pulse en cours
        if (flashTimeline != null
                && flashTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) return;

        flashVisible = true;
        if (quizWarningOverlay != null) {
            quizWarningOverlay.setOpacity(1.0);
        }
        flashTimeline = new Timeline(new KeyFrame(Duration.millis(380), e -> {
            flashVisible = !flashVisible;
            double opacity = flashVisible ? 1.0 : 0.12;
            if (timerRingFill    != null) timerRingFill.setOpacity(opacity);
            if (timerDigitsLabel != null) timerDigitsLabel.setOpacity(opacity);
            if (quizWarningOverlay != null) quizWarningOverlay.setOpacity(flashVisible ? 1.0 : 0.18);
        }));
        flashTimeline.setCycleCount(Timeline.INDEFINITE);
        flashTimeline.play();
    }

    // -------------------------------------------------------
    // startPulseAnimation() — pulse 30→11s
    // (opacity 1.0 → 0.5 → 1.0, lent et doux)
    // -------------------------------------------------------
    private Timeline pulseTimeline = null;
    private boolean  pulseGrowing  = false;
    private double   pulseOpacity  = 1.0;

    private void startPulseAnimation() {
        if (pulseTimeline != null
                && pulseTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) return;

        pulseOpacity = 1.0;
        pulseGrowing = false;
        if (warningFadeTransition == null && quizWarningOverlay != null) {
            warningFadeTransition = new FadeTransition(Duration.millis(700), quizWarningOverlay);
        }
        if (warningFadeTransition != null && quizWarningOverlay != null) {
            warningFadeTransition.stop();
            warningFadeTransition.setFromValue(quizWarningOverlay.getOpacity());
            warningFadeTransition.setToValue(1.0);
            warningFadeTransition.playFromStart();
        }
        pulseTimeline = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            pulseOpacity += pulseGrowing ? 0.04 : -0.04;
            if (pulseOpacity <= 0.45) { pulseOpacity = 0.45; pulseGrowing = true; }
            if (pulseOpacity >= 1.0)  { pulseOpacity = 1.0;  pulseGrowing = false; }
            if (timerRingFill    != null) timerRingFill.setOpacity(pulseOpacity);
            if (timerDigitsLabel != null) timerDigitsLabel.setOpacity(pulseOpacity);
        }));
        pulseTimeline.setCycleCount(Timeline.INDEFINITE);
        pulseTimeline.play();
    }

    private void stopPulseAnimation() {
        if (pulseTimeline != null) { pulseTimeline.stop(); pulseTimeline = null; }
        if (warningFadeTransition != null) {
            warningFadeTransition.stop();
        }
        if (timerRingFill    != null) timerRingFill.setOpacity(1.0);
        if (timerDigitsLabel != null) timerDigitsLabel.setOpacity(1.0);
    }

    // -------------------------------------------------------
    // stopFlashAnimation()
    // -------------------------------------------------------
    private void stopFlashAnimation() {
        stopPulseAnimation();
        if (flashTimeline != null) { flashTimeline.stop(); flashTimeline = null; }
        if (timerRingFill    != null) timerRingFill.setOpacity(1.0);
        if (timerDigitsLabel != null) timerDigitsLabel.setOpacity(1.0);
        if (quizWarningOverlay != null) quizWarningOverlay.setOpacity(0.0);
    }

    private void updateQuizPanelState(String state) {
        if (questionSection == null) {
            return;
        }

        questionSection.getStyleClass().removeAll(
                "quiz-question-panel-warning",
                "quiz-question-panel-danger",
                "quiz-question-panel-done"
        );

        switch (state) {
            case "warning" -> questionSection.getStyleClass().add("quiz-question-panel-warning");
            case "danger" -> questionSection.getStyleClass().add("quiz-question-panel-danger");
            case "done" -> questionSection.getStyleClass().add("quiz-question-panel-done");
            default -> {
            }
        }
    }

    private void clearQuizPanelAlertState() {
        if (questionSection != null) {
            questionSection.getStyleClass().removeAll(
                    "quiz-question-panel-warning",
                    "quiz-question-panel-danger",
                    "quiz-question-panel-done"
            );
        }
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
            e.printStackTrace();
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
        
        // Mettre à jour la session active
        if (quiz != null && activeQuizAnswers.containsKey(quiz.getId())) {
            activeQuizAnswers.get(quiz.getId()).put(question.getId(), answer.getId());
        }
        
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

        if (questionSectionWrap != null) {
            questionSectionWrap.setVisible(questionVisible);
            questionSectionWrap.setManaged(questionVisible);
        } else {
            questionSection.setVisible(questionVisible);
            questionSection.setManaged(questionVisible);
        }

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

    // ============================================================
// Dans QuizSessionController.java
// Remplacez UNIQUEMENT la méthode onTranslate() par celle-ci.
// Le reste du fichier reste identique.
// ============================================================

    @FXML
    private void onTranslate() {
        if (questions.isEmpty()) {
            showWarning("Traduction", "Aucune question disponible.");
            return;
        }

        // Le quiz est en FR par défaut.
        // Premier clic  → traduire FR → EN (isTranslatedToEn devient true)
        // Deuxième clic → revenir  EN → FR (isTranslatedToEn devient false)
        boolean goingToEnglish = !isTranslatedToEn;
        String targetLang      = goingToEnglish ? "en" : "fr";

        Button translateBtn = findTranslateButton();
        if (translateBtn != null) {
            translateBtn.setDisable(true);
            translateBtn.setText(goingToEnglish ? "⏳ FR → EN..." : "⏳ EN → FR...");
        }

        javafx.application.Platform.runLater(() -> {
            if (questionTextLabel != null) questionTextLabel.setText("Traduction en cours...");
        });

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // Collecter tous les textes en une seule liste
                List<String> allTexts = new ArrayList<>();
                for (Question q : questions) allTexts.add(q.getContent() != null ? q.getContent() : "");
                for (Answer  a : answers)   allTexts.add(a.getContent() != null ? a.getContent() : "");

                System.out.println("[QuizSessionController] Batch " + (goingToEnglish ? "FR→EN" : "EN→FR")
                        + " : " + allTexts.size() + " textes");

                // UN SEUL appel API
                List<String> translated = translationService.translateBatch(allTexts, targetLang);

                // Redistribuer dans les questions
                for (int i = 0; i < questions.size(); i++) {
                    String t = i < translated.size() ? translated.get(i) : null;
                    if (t != null && !t.isBlank()) questions.get(i).setContent(t);
                }

                // Redistribuer dans les réponses
                int offset = questions.size();
                for (int i = 0; i < answers.size(); i++) {
                    int    idx = offset + i;
                    String t   = idx < translated.size() ? translated.get(idx) : null;
                    if (t != null && !t.isBlank()) answers.get(i).setContent(t);
                }

                // Confirmer le changement d'état seulement si tout a réussi
                isTranslatedToEn = goingToEnglish;

                javafx.application.Platform.runLater(() -> {
                    if (translateBtn != null) {
                        translateBtn.setDisable(false);
                        // Bouton montre vers où on peut aller ensuite
                        translateBtn.setText(isTranslatedToEn ? "🌐 Voir en Français" : "🌐 Voir en Anglais");
                    }
                    if (questionSection.isVisible()) renderCurrentQuestion();
                    showInfo("Traduction terminée",
                            allTexts.size() + " textes traduits en "
                            + (isTranslatedToEn ? "anglais" : "français") + " ✓");
                });

            } catch (Exception e) {
                System.err.println("[QuizSessionController] Erreur traduction : " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    // Ne pas changer isTranslatedToEn — garder l'état précédent
                    if (translateBtn != null) {
                        translateBtn.setDisable(false);
                        translateBtn.setText(isTranslatedToEn ? "🌐 Voir en Français" : "🌐 Voir en Anglais");
                    }
                    if (questionSection.isVisible()) renderCurrentQuestion();
                    showError("Traduction", "Erreur : " + e.getMessage());
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
    /**
     * Retourne le nom complet de l'étudiant connecté.
     * Priorité : prénom + nom → displayName → email → "Étudiant"
     */
    private String buildStudentName() {
        com.pidev.models.User user = currentUser;
        
        // Sécurité : si l'injection a échoué, on tente de récupérer via le singleton
        if (user == null) {
            user = com.pidev.utils.UserSession.getCurrentUser();
        }

        if (user == null) return "";

        // Essayer prénom + nom
        String prenom = user.getPrenom();
        String nom    = user.getNom();
        
        // Nettoyage des chaînes "null" qui pourraient venir de la DB
        if ("null".equalsIgnoreCase(prenom)) prenom = null;
        if ("null".equalsIgnoreCase(nom)) nom = null;

        if (prenom != null && !prenom.isBlank() && nom != null && !nom.isBlank()) {
            return (prenom.trim() + " " + nom.trim());
        }
        if (prenom != null && !prenom.isBlank()) return prenom.trim();
        if (nom    != null && !nom.isBlank())    return nom.trim();

        // Fallback : displayName
        String display = user.getDisplayName();
        if (display != null && !display.isBlank() && !"null".equalsIgnoreCase(display)) return display.trim();

        // Fallback : email
        String email = user.getEmail();
        if (email != null && !email.isBlank() && !"null".equalsIgnoreCase(email)) return email.trim();

        return "";
    }
}





