package com.pidev.Controllers.admin;

import com.pidev.Services.QuizServer;
import com.pidev.Services.QuizSocketClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Contrôleur JavaFX pour le Quiz Multijoueur en temps réel.
 *
 * Écrans gérés (visibilité par code) :
 *   setupPane   → Configuration (nom, rôle hôte/client)
 *   lobbyPane   → Salle d'attente
 *   gamePane    → Jeu en cours
 *   resultPane  → Résultats finaux
 */
public class MultiplayerQuizController {

    // ── Setup ──────────────────────────────────────────────────────────────────
    @FXML private VBox setupPane;
    @FXML private TextField playerNameField;
    @FXML private TextField serverHostField;
    @FXML private RadioButton hostRadio;
    @FXML private RadioButton clientRadio;
    @FXML private Label setupStatusLabel;

    // ── Lobby ──────────────────────────────────────────────────────────────────
    @FXML private VBox lobbyPane;
    @FXML private Label lobbyTitleLabel;
    @FXML private VBox playersListBox;
    @FXML private Label waitingLabel;

    // ── Game ───────────────────────────────────────────────────────────────────
    @FXML private HBox  gamePane;
    @FXML private Label questionIndexLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsBox;
    @FXML private Label timerLabel;
    @FXML private VBox scoresBox;

    // ── Result ─────────────────────────────────────────────────────────────────
    @FXML private VBox resultPane;
    @FXML private Label winnerLabel;
    @FXML private VBox finalScoresBox;

    // ── State ──────────────────────────────────────────────────────────────────
    private QuizServer server;
    private QuizSocketClient client;
    private String myName;
    private int currentQuestionIndex = -1;
    private boolean answered = false;
    private Timeline countdownTimer;

    // Questions de démonstration (utilisées quand le serveur est lancé en mode hôte)
    private static final List<JSONObject> DEMO_QUESTIONS = List.of(
        new JSONObject()
            .put("question", "Quel est le langage principal utilisé dans JavaFX ?")
            .put("options", new JSONArray(List.of("Python", "Java", "Kotlin", "Scala")))
            .put("correctAnswerIndex", 1)
            .put("explanation", "JavaFX est un framework Java pour les interfaces graphiques."),
        new JSONObject()
            .put("question", "Quel protocole est utilisé par les sockets Java ?")
            .put("options", new JSONArray(List.of("UDP", "HTTP", "TCP/IP", "FTP")))
            .put("correctAnswerIndex", 2)
            .put("explanation", "java.net.Socket utilise le protocole TCP/IP."),
        new JSONObject()
            .put("question", "Quelle classe JavaFX permet de lancer l'application ?")
            .put("options", new JSONArray(List.of("Application", "Scene", "Stage", "Platform")))
            .put("correctAnswerIndex", 0)
            .put("explanation", "La méthode main() appelle Application.launch()."),
        new JSONObject()
            .put("question", "Quel format est utilisé pour les messages dans notre protocole ?")
            .put("options", new JSONArray(List.of("XML", "CSV", "JSON", "YAML")))
            .put("correctAnswerIndex", 2)
            .put("explanation", "Les messages transitent en JSON sur le canal TCP."),
        new JSONObject()
            .put("question", "Que fait Platform.runLater() en JavaFX ?")
            .put("options", new JSONArray(List.of(
                "Lance un nouveau thread",
                "Met à jour l'UI depuis un thread non-JavaFX",
                "Arrête l'application",
                "Charge un FXML")))
            .put("correctAnswerIndex", 1)
            .put("explanation", "Platform.runLater() soumet une tâche au fil JavaFX Application Thread.")
    );

    @FXML
    public void initialize() {
        showOnly(setupPane);
        ToggleGroup modeGroup = new ToggleGroup();
        hostRadio.setToggleGroup(modeGroup);
        clientRadio.setToggleGroup(modeGroup);
        hostRadio.setSelected(true);
        serverHostField.setDisable(true);

        hostRadio.selectedProperty().addListener((obs, o, n) -> serverHostField.setDisable(n));
    }

    // ── Actions Setup ──────────────────────────────────────────────────────────

    @FXML
    public void onConnect() {
        myName = playerNameField.getText().trim();
        if (myName.isEmpty()) {
            setupStatusLabel.setText("❌ Entrez votre nom.");
            return;
        }

        setupStatusLabel.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                if (hostRadio.isSelected()) {
                    // Démarrage du serveur local avec les questions de démo
                    server = new QuizServer(new ArrayList<>(DEMO_QUESTIONS));
                    server.start();
                    Thread.sleep(500); // Laisse le serveur démarrer
                }

                String host = hostRadio.isSelected() ? "localhost" : serverHostField.getText().trim();
                if (host.isEmpty()) host = "localhost";

                client = new QuizSocketClient(new QuizSocketClient.MessageListener() {
                    @Override
                    public void onMessage(JSONObject message) {
                        onServerMessage(message);
                    }
                    @Override
                    public void onDisconnected() {
                        Platform.runLater(() -> {
                            showOnly(setupPane);
                            setupStatusLabel.setText("⚠️ Déconnecté du serveur.");
                        });
                    }
                });
                client.connect(host, QuizServer.PORT);
                client.join(myName);

                Platform.runLater(this::showLobby);

            } catch (Exception e) {
                String err = e.getMessage();
                Platform.runLater(() -> setupStatusLabel.setText("❌ " + err));
            }
        }).start();
    }

    @FXML
    public void onDisconnect() {
        if (client != null) client.disconnect();
        if (server != null) server.stop();
        showOnly(setupPane);
        setupStatusLabel.setText("");
    }

    // ── Réception des messages serveur ────────────────────────────────────────

    private void onServerMessage(JSONObject msg) {
        Platform.runLater(() -> {
            String type = msg.optString("type", "");
            switch (type) {
                case "LOBBY"    -> handleLobby(msg);
                case "START"    -> handleStart(msg);
                case "QUESTION" -> handleQuestion(msg);
                case "RESULT"   -> handleResult(msg);
                case "FINAL"    -> handleFinal(msg);
                case "ERROR"    -> showAlert("Erreur Serveur", msg.optString("message", "Erreur inconnue"));
            }
        });
    }

    private void handleLobby(JSONObject msg) {
        showOnly(lobbyPane);
        playersListBox.getChildren().clear();
        JSONArray players = msg.optJSONArray("players");
        if (players != null) {
            for (int i = 0; i < players.length(); i++) {
                String name = players.getString(i);
                Label lbl = new Label("👤 " + name + (name.equals(myName) ? " (moi)" : ""));
                lbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #1e293b;");
                playersListBox.getChildren().add(lbl);
            }
        }
        boolean ready = msg.optBoolean("ready", false);
        waitingLabel.setText(ready ? "⏳ La partie démarre dans quelques instants..." : "⏳ En attente d'un 2ème joueur...");
        lobbyTitleLabel.setText("Salle d'attente – " + (hostRadio.isSelected() ? "Hôte" : "Invité"));
    }

    private void handleStart(JSONObject msg) {
        showOnly(gamePane);
    }

    private void handleQuestion(JSONObject msg) {
        answered = false;
        currentQuestionIndex = msg.optInt("index", 0);
        int total = msg.optInt("total", 5);
        int time  = msg.optInt("timeSeconds", 20);

        questionIndexLabel.setText("Question " + (currentQuestionIndex + 1) + " / " + total);
        questionTextLabel.setText(msg.optString("question", ""));

        // Options
        optionsBox.getChildren().clear();
        JSONArray options = msg.optJSONArray("options");
        if (options != null) {
            String[] letters = {"A", "B", "C", "D"};
            for (int i = 0; i < options.length(); i++) {
                final int idx = i;
                Button btn = new Button(letters[i] + "   " + options.getString(i));
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 12 20; -fx-cursor: hand;");
                btn.setOnAction(e -> onAnswer(idx));
                optionsBox.getChildren().add(btn);
            }
        }

        // Countdown timer
        startCountdown(time);
    }

    private void handleResult(JSONObject msg) {
        stopCountdown();
        int correctIdx = msg.optInt("correctIndex", -1);

        // Colorer les boutons selon bonne/mauvaise réponse
        List<javafx.scene.Node> buttons = new ArrayList<>(optionsBox.getChildren());
        for (int i = 0; i < buttons.size(); i++) {
            Button btn = (Button) buttons.get(i);
            btn.setDisable(true);
            if (i == correctIdx) {
                btn.setStyle(btn.getStyle() + "-fx-background-color: #d1fae5; -fx-border-color: #10b981;");
            }
        }

        // Mise à jour scores
        updateScores(msg.optJSONObject("scores"));
    }

    private void handleFinal(JSONObject msg) {
        stopCountdown();
        showOnly(resultPane);

        String winner = msg.optString("winner", "?");
        winnerLabel.setText("🏆 Vainqueur : " + winner);

        finalScoresBox.getChildren().clear();
        JSONObject scores = msg.optJSONObject("scores");
        if (scores != null) {
            scores.toMap().entrySet().stream()
                .sorted((a, b) -> Integer.compare((int) b.getValue(), (int) a.getValue()))
                .forEach(entry -> {
                    Label lbl = new Label(entry.getKey() + "   →   " + entry.getValue() + " pts");
                    lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                            "-fx-text-fill: " + (entry.getKey().equals(winner) ? "#10b981" : "#475569") + ";");
                    finalScoresBox.getChildren().add(lbl);
                });
        }
    }

    // ── Timer ──────────────────────────────────────────────────────────────────

    private void startCountdown(int seconds) {
        stopCountdown();
        final int[] remaining = {seconds};
        timerLabel.setText(remaining[0] + "s");
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            timerLabel.setText(remaining[0] + "s");
            if (remaining[0] <= 5) {
                timerLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 24px;");
            }
            if (remaining[0] <= 0) stopCountdown();
        }));
        countdownTimer.setCycleCount(seconds);
        timerLabel.setStyle("-fx-text-fill: #1f2937; -fx-font-size: 22px;");
        countdownTimer.play();
    }

    private void stopCountdown() {
        if (countdownTimer != null) { countdownTimer.stop(); countdownTimer = null; }
    }

    // ── Réponse joueur ─────────────────────────────────────────────────────────

    private void onAnswer(int answerIndex) {
        if (answered || client == null) return;
        answered = true;
        client.sendAnswer(myName, currentQuestionIndex, answerIndex);

        // Griser les boutons après réponse
        optionsBox.getChildren().forEach(n -> n.setDisable(true));
        ((Button) optionsBox.getChildren().get(answerIndex))
            .setStyle(((Button) optionsBox.getChildren().get(answerIndex)).getStyle() +
                    "-fx-background-color: #dbeafe; -fx-border-color: #3b82f6;");
    }

    // ── Scores sidebar ─────────────────────────────────────────────────────────

    private void updateScores(JSONObject scores) {
        if (scores == null) return;
        scoresBox.getChildren().clear();
        scores.toMap().entrySet().stream()
            .sorted((a, b) -> Integer.compare((int) b.getValue(), (int) a.getValue()))
            .forEach(entry -> {
                Label lbl = new Label(entry.getKey() + " : " + entry.getValue() + " pts");
                lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " +
                        (entry.getKey().equals(myName) ? "#6366f1" : "#64748b") + ";");
                scoresBox.getChildren().add(lbl);
            });
    }

    // ── UI utils ───────────────────────────────────────────────────────────────

    private void showOnly(javafx.scene.layout.Region pane) {
        for (javafx.scene.layout.Region p : new javafx.scene.layout.Region[]{setupPane, lobbyPane, gamePane, resultPane}) {
            p.setVisible(p == pane);
            p.setManaged(p == pane);
        }
    }

    private void showLobby() {
        showOnly(lobbyPane);
        lobbyTitleLabel.setText("Salle d'attente");
        waitingLabel.setText("⏳ Connexion établie...");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
