package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class DuelClient {

    private static final int PORT = 9090;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = false;

    // Public pour réassignation
    public Consumer<DuelMessage> onMessageReceived;
    private final Runnable onConnected;

    public DuelClient(Consumer<DuelMessage> onMessage, Runnable onConnected) {
        this.onMessageReceived = onMessage;
        this.onConnected = onConnected;
    }

    public void connect(String hostIp) {
        new Thread(() -> {
            try {
                // Timeout de connexion : 5 secondes maximum
                socket = new Socket();
                socket.connect(
                        new java.net.InetSocketAddress(hostIp, PORT),
                        5000  // 5 secondes
                );

                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                in  = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                running = true;
                LoggerUtil.info("Connected to host", "ip", hostIp);
                Platform.runLater(onConnected);
                listen();

            } catch (java.net.SocketTimeoutException e) {
                LoggerUtil.error("Connexion timeout", e);
                Platform.runLater(() -> showConnectError(
                        "Connexion impossible à " + hostIp + "\n\n" +
                                "Causes possibles :\n" +
                                "• Le pare-feu Windows bloque le port 9090\n" +
                                "• L'hôte n'a pas encore cliqué 'Créer le duel'\n" +
                                "• Vous n'êtes pas sur le même réseau WiFi\n\n" +
                                "Solution : l'hôte doit ouvrir le port 9090 dans son pare-feu."));
            } catch (java.net.ConnectException e) {
                LoggerUtil.error("Connexion refusée", e);
                Platform.runLater(() -> showConnectError(
                        "Connexion refusée par " + hostIp + "\n\n" +
                                "Causes possibles :\n" +
                                "• Le pare-feu Windows bloque le port 9090\n" +
                                "• L'IP est incorrecte\n" +
                                "• L'hôte n'a pas encore cliqué 'Créer le duel'\n\n" +
                                "Solution : l'hôte doit autoriser le port 9090 dans son pare-feu."));
            } catch (Exception e) {
                LoggerUtil.error("Erreur connexion inattendue", e);
                Platform.runLater(() -> showConnectError(
                        "Erreur : " + e.getMessage()));
            }
        }, "DuelClient-Thread").start();
    }

    private void showConnectError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connexion impossible");
        alert.setHeaderText("Impossible de rejoindre le duel");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startListening() {
        Thread listenThread = new Thread(this::listen, "DuelClient-ListenThread");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void listen() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                final String msg = line;
                DuelMessage dm = DuelMessage.fromJson(msg);
                Platform.runLater(() -> {
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(dm);
                    }
                });
            }
        } catch (Exception e) {
            if (running) LoggerUtil.error("DuelClient listen error", e);
        }
    }

    public void send(DuelMessage msg) {
        try {
            if (out != null) out.println(msg.toJson());
        } catch (Exception e) {
            LoggerUtil.error("DuelClient send error", e);
        }
    }

    public void stop() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}