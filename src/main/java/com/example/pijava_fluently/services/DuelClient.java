package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;

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
        Thread clientThread = new Thread(() -> {
            try {
                socket = new Socket(hostIp, PORT);
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                running = true;
                LoggerUtil.info("Connected to host", "ip", hostIp);

                Platform.runLater(onConnected);
                startListening();

            } catch (Exception e) {
                LoggerUtil.error("DuelClient connect error", e);
                Platform.runLater(() -> LoggerUtil.error("Connexion échouée — vérifiez l'IP et le WiFi", e));
            }
        }, "DuelClient-MainThread");
        clientThread.setDaemon(true);
        clientThread.start();
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