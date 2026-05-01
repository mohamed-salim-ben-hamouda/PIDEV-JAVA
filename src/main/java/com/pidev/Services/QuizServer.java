package com.pidev.Services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Serveur TCP de Quiz Multijoueur.
 * 
 * Protocole JSON (type → direction) :
 *   JOIN        client → server    {"type":"JOIN","playerName":"Alice"}
 *   LOBBY       server → client    {"type":"LOBBY","players":["Alice","Bob"],"ready":false}
 *   START       server → client    {"type":"START","totalQuestions":5}
 *   QUESTION    server → client    {"type":"QUESTION","index":0,"total":5,"question":"...","options":["A","B","C","D"],"timeSeconds":20}
 *   ANSWER      client → server    {"type":"ANSWER","questionIndex":0,"answerIndex":2,"playerName":"Alice"}
 *   RESULT      server → client    {"type":"RESULT","correct":true,"correctIndex":2,"explanation":"...","scores":{"Alice":10,"Bob":0}}
 *   FINAL       server → client    {"type":"FINAL","scores":{"Alice":30,"Bob":20},"winner":"Alice"}
 *   ERROR       server → client    {"type":"ERROR","message":"..."}
 */
public class QuizServer {

    public static final int PORT = 9876;
    private static final int MIN_PLAYERS = 2;
    private static final int QUESTION_TIME_SECONDS = 20;

    // ── État global du serveur ──────────────────────────────────────────────────
    private final List<PlayerHandler> players = new CopyOnWriteArrayList<>();
    private final List<JSONObject>    questions;
    private volatile boolean gameStarted = false;
    private volatile int currentQuestion = -1;

    // Scores : playerName → points
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    // Réponses pour la question courante : playerName → answerIndex
    private final Map<String, Integer> currentAnswers = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    // ───────────────────────────────────────────────────────────────────────────

    public QuizServer(List<JSONObject> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("La liste de questions ne peut pas être vide.");
        }
        this.questions = questions;
    }

    /** Démarre le serveur dans un thread dédié. */
    public void start() {
        pool.submit(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("[QuizServer] En écoute sur le port " + PORT);

                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    PlayerHandler handler = new PlayerHandler(clientSocket);
                    pool.submit(handler);
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[QuizServer] Erreur : " + e.getMessage());
                }
            }
        });
    }

    /** Arrête proprement le serveur. */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) { /* ignore */ }
        pool.shutdown();
        System.out.println("[QuizServer] Arrêté.");
    }

    // ──────────────────────────── Broadcast ────────────────────────────────────

    private synchronized void broadcast(JSONObject message) {
        String json = message.toString();
        for (PlayerHandler p : players) {
            p.send(json);
        }
    }

    // ──────────────────────────── Logique de jeu ───────────────────────────────

    private synchronized void onPlayerJoined(PlayerHandler newPlayer) {
        players.add(newPlayer);
        scores.put(newPlayer.name, 0);

        // Envoi de la mise à jour du lobby à tous
        broadcastLobby();

        System.out.printf("[QuizServer] %s rejoint. %d/%d joueurs.%n",
                newPlayer.name, players.size(), MIN_PLAYERS);

        // Lancer la partie si on a assez de joueurs
        if (players.size() >= MIN_PLAYERS && !gameStarted) {
            gameStarted = true;
            pool.submit(this::runGame);
        }
    }

    private void broadcastLobby() {
        JSONArray names = new JSONArray();
        players.forEach(p -> names.put(p.name));
        broadcast(new JSONObject()
                .put("type", "LOBBY")
                .put("players", names)
                .put("ready", players.size() >= MIN_PLAYERS));
    }

    /** Boucle principale du jeu, tourne dans son propre thread. */
    private void runGame() {
        try {
            // Petit délai avant le démarrage
            Thread.sleep(2000);

            broadcast(new JSONObject()
                    .put("type", "START")
                    .put("totalQuestions", questions.size()));

            Thread.sleep(1500);

            for (int i = 0; i < questions.size(); i++) {
                currentQuestion = i;
                currentAnswers.clear();

                JSONObject q = questions.get(i);

                // Envoi de la question
                broadcast(new JSONObject()
                        .put("type", "QUESTION")
                        .put("index", i)
                        .put("total", questions.size())
                        .put("question", q.getString("question"))
                        .put("options", q.getJSONArray("options"))
                        .put("timeSeconds", QUESTION_TIME_SECONDS));

                // Attente des réponses (avec compte à rebours)
                waitForAnswers(QUESTION_TIME_SECONDS);

                // Calcul des scores
                int correctIdx = q.getInt("correctAnswerIndex");
                String explanation = q.optString("explanation", "");

                for (PlayerHandler p : players) {
                    Integer ans = currentAnswers.get(p.name);
                    if (ans != null && ans == correctIdx) {
                        scores.merge(p.name, 10, Integer::sum);
                    }
                }

                // Envoi du résultat de la question
                JSONObject scoresJson = new JSONObject(scores);
                broadcast(new JSONObject()
                        .put("type", "RESULT")
                        .put("correctIndex", correctIdx)
                        .put("explanation", explanation)
                        .put("scores", scoresJson));

                // Pause entre les questions
                Thread.sleep(3000);
            }

            // Résultat final
            String winner = scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Égalité");

            broadcast(new JSONObject()
                    .put("type", "FINAL")
                    .put("scores", new JSONObject(scores))
                    .put("winner", winner));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Attend les réponses de tous les joueurs ou l'expiration du temps. */
    private void waitForAnswers(int seconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + (long) seconds * 1000;

        while (System.currentTimeMillis() < deadline) {
            // Tous les joueurs ont répondu → on passe
            if (currentAnswers.size() >= players.size()) break;
            Thread.sleep(100);
        }
    }

    /** Enregistre la réponse d'un joueur. */
    private synchronized void onAnswer(String playerName, int questionIndex, int answerIndex) {
        if (questionIndex == currentQuestion && !currentAnswers.containsKey(playerName)) {
            currentAnswers.put(playerName, answerIndex);
            System.out.printf("[QuizServer] Réponse de %s : option %d%n", playerName, answerIndex);
        }
    }

    private synchronized void onPlayerLeft(PlayerHandler player) {
        players.remove(player);
        scores.remove(player.name);
        System.out.printf("[QuizServer] %s déconnecté.%n", player.name);
        if (!gameStarted) broadcastLobby();
    }

    // ──────────────────────────── Handler par joueur ───────────────────────────

    private class PlayerHandler implements Runnable {
        final Socket socket;
        String name = "Joueur";
        private PrintWriter out;

        PlayerHandler(Socket socket) {
            this.socket = socket;
        }

        void send(String message) {
            if (out != null) out.println(message);
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
            ) {
                this.out = writer;

                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        JSONObject msg = new JSONObject(line.trim());
                        handleMessage(msg);
                    } catch (Exception e) {
                        send(new JSONObject().put("type", "ERROR").put("message", "JSON invalide").toString());
                    }
                }
            } catch (IOException e) {
                System.err.println("[QuizServer] " + name + " déconnecté brutalement.");
            } finally {
                onPlayerLeft(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleMessage(JSONObject msg) {
            String type = msg.optString("type", "");
            switch (type) {
                case "JOIN" -> {
                    this.name = msg.optString("playerName", "Joueur" + (players.size() + 1));
                    onPlayerJoined(this);
                }
                case "ANSWER" -> {
                    onAnswer(
                        msg.optString("playerName", this.name),
                        msg.optInt("questionIndex", -1),
                        msg.optInt("answerIndex", -1)
                    );
                }
                default -> send(new JSONObject()
                        .put("type", "ERROR")
                        .put("message", "Type de message inconnu : " + type)
                        .toString());
            }
        }
    }
}
