package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class DuelServer {

    private static final int PORT = 9090;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = false;

    // Public pour que DuelGameController puisse le réassigner
    public Consumer<DuelMessage> onMessageReceived;
    private final Runnable onClientConnected;

    public DuelServer(Consumer<DuelMessage> onMessage, Runnable onConnected) {
        this.onMessageReceived = onMessage;
        this.onClientConnected = onConnected;
    }

    public void start() {
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                LoggerUtil.info("DuelServer started", "port", String.valueOf(PORT));

                clientSocket = serverSocket.accept();
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                running = true;
                LoggerUtil.info("Client connected", "ip", clientSocket.getInetAddress().getHostAddress());

                Platform.runLater(onClientConnected);

                // Thread d'écoute en daemon
                startListening();

            } catch (Exception e) {
                if (running) LoggerUtil.error("DuelServer error", e);
            }
        }, "DuelServer-MainThread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void startListening() {
        Thread listenThread = new Thread(this::listen, "DuelServer-ListenThread");
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
            if (running) LoggerUtil.error("DuelServer listen error", e);
        }
    }

    public void send(DuelMessage msg) {
        try {
            if (out != null) out.println(msg.toJson());
        } catch (Exception e) {
            LoggerUtil.error("DuelServer send error", e);
        }
    }

    public static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public void stop() {
        running = false;
        try {
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
    }
}