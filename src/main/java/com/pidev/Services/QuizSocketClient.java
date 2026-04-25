package com.pidev.Services;

import org.json.JSONObject;
import java.io.*;
import java.net.Socket;

/**
 * Client socket pour se connecter au QuizServer.
 * Écoute dans un thread dédié et délègue les messages via MessageListener.
 */
public class QuizSocketClient {

    private Socket socket;
    private PrintWriter out;
    private volatile boolean running = false;

    public interface MessageListener {
        void onMessage(JSONObject message);
        void onDisconnected();
    }

    private final MessageListener listener;

    public QuizSocketClient(MessageListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        running = true;
        startListening();
    }

    public void send(JSONObject message) {
        if (out != null && running) out.println(message.toString());
    }

    public void join(String playerName) {
        send(new JSONObject().put("type", "JOIN").put("playerName", playerName));
    }

    public void sendAnswer(String playerName, int questionIndex, int answerIndex) {
        send(new JSONObject()
                .put("type", "ANSWER")
                .put("playerName", playerName)
                .put("questionIndex", questionIndex)
                .put("answerIndex", answerIndex));
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void startListening() {
        Thread t = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                String line;
                while (running && (line = in.readLine()) != null) {
                    try { listener.onMessage(new JSONObject(line.trim())); }
                    catch (Exception e) { System.err.println("[QuizClient] JSON invalide: " + line); }
                }
            } catch (IOException e) {
                if (running) System.err.println("[QuizClient] Connexion perdue: " + e.getMessage());
            } finally {
                running = false;
                listener.onDisconnected();
            }
        }, "QuizClient-Listener");
        t.setDaemon(true);
        t.start();
    }
}
