package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class DuelClient {
    private static final int PORT = 9090;
    private static final int TIMEOUT = 8000; // 8 secondes
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = false;
    private Thread listenThread;

    public Consumer<DuelMessage> onMessageReceived;
    private final Runnable onConnected;

    public DuelClient(Consumer<DuelMessage> onMessage, Runnable onConnected) {
        this.onMessageReceived = onMessage;
        this.onConnected = onConnected;
    }

    public void connect(String hostIp) {
        Thread t = new Thread(() -> {
            try {
                LoggerUtil.info("Connecting to host", "ip", hostIp, "port", String.valueOf(PORT));

                socket = new Socket();
                socket.connect(new InetSocketAddress(hostIp, PORT), TIMEOUT);
                socket.setSoTimeout(0); // Pas de timeout en lecture

                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                running = true;
                LoggerUtil.info("Connected to host", "ip", hostIp);
                Platform.runLater(onConnected);

                // Démarrer l'écoute
                startListening();

            } catch (SocketTimeoutException e) {
                LoggerUtil.error("Connexion timeout", e);
                Platform.runLater(() -> showError(hostIp, "Timeout — Aucune réponse de l'hôte en 8 secondes.\n\n" +
                        "Vérifiez que :\n" +
                        "1. L'hôte a cliqué 'Créer le duel' AVANT vous\n" +
                        "2. Le pare-feu Windows est désactivé sur le PC hôte\n" +
                        "3. Vous êtes sur le même réseau WiFi\n" +
                        "4. L'IP est correcte : " + hostIp));
            } catch (ConnectException e) {
                LoggerUtil.error("Connexion refusée", e);
                Platform.runLater(() -> showError(hostIp, "Connexion refusée.\n\n" +
                        "Vérifiez que :\n" +
                        "1. Le pare-feu Windows est désactivé sur le PC hôte\n" +
                        "2. L'hôte a bien cliqué 'Créer le duel'\n" +
                        "3. L'IP est correcte : " + hostIp));
            } catch (Exception e) {
                if (running) LoggerUtil.error("DuelClient error", e);
                Platform.runLater(() -> showError(hostIp, "Erreur : " + e.getMessage()));
            }
        }, "DuelClient-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void startListening() {
        listenThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    final DuelMessage dm = DuelMessage.fromJson(line);
                    Platform.runLater(() -> {
                        if (onMessageReceived != null) onMessageReceived.accept(dm);
                    });
                }
            } catch (SocketException e) {
                if (running) LoggerUtil.warning("Socket closed", e.getMessage());
            } catch (Exception e) {
                if (running) LoggerUtil.error("DuelClient listen error", e);
            }
        }, "DuelClient-ListenThread");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public void send(DuelMessage msg) {
        try {
            if (out != null && !out.checkError()) {
                out.println(msg.toJson());
                out.flush();
            }
        } catch (Exception e) {
            LoggerUtil.error("DuelClient send error", e);
        }
    }

    public void stop() {
        running = false;

        try {
            if (listenThread != null) listenThread.interrupt();
        } catch (Exception ignored) {}

        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}

        try {
            if (in != null) in.close();
        } catch (Exception ignored) {}

        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}

        LoggerUtil.info("DuelClient stopped");
    }

    private void showError(String ip, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connexion impossible");
        alert.setHeaderText("Impossible de rejoindre " + ip);
        alert.setContentText(message);
        alert.showAndWait();
    }
}