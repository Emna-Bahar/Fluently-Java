package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.function.Consumer;

public class DuelServer {
    private static final int PORT = 9090;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = false;
    private Thread listenThread;

    public Consumer<DuelMessage> onMessageReceived;
    private final Runnable onClientConnected;

    public DuelServer(Consumer<DuelMessage> onMessage, Runnable onConnected) {
        this.onMessageReceived = onMessage;
        this.onClientConnected = onConnected;
    }

    public void start() {
        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                String localIP = getBestLocalIP();
                LoggerUtil.info("DuelServer listening", "ip", localIP, "port", String.valueOf(PORT));

                // Attendre la connexion du client
                clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(0);

                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream())), true);
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                running = true;
                LoggerUtil.info("Client connected", "ip", clientSocket.getInetAddress().getHostAddress());
                Platform.runLater(onClientConnected);

                startListening();
            } catch (Exception e) {
                if (running) LoggerUtil.error("DuelServer error", e);
            }
        }, "DuelServer-MainThread");
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
                if (running) LoggerUtil.error("DuelServer listen error", e);
            }
        }, "DuelServer-ListenThread");
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
            LoggerUtil.error("DuelServer send error", e);
        }
    }

    /**
     * Retourne la meilleure IP locale (celle qui est routable vers internet)
     */
    public static String getBestLocalIP() {
        try {
            // Méthode 1 : Connexion UDP à 8.8.8.8 (Google DNS)
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 80);
                String ip = socket.getLocalAddress().getHostAddress();
                if (ip != null && !ip.equals("0.0.0.0") && !ip.startsWith("127.")) {
                    return ip;
                }
            }
        } catch (Exception ignored) {}

        // Méthode 2 : Parcourir les interfaces réseau
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                // Ignorer les interfaces inactives ou loopback
                if (ni.isLoopback() || !ni.isUp()) continue;

                // Ignorer les interfaces virtuelles (VMware, VirtualBox, Hamachi, Docker)
                String name = ni.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("vmware") ||
                        name.contains("vbox") || name.contains("hamachi") ||
                        name.contains("docker") || name.contains("hyper-v") ||
                        name.contains("bluetooth") || name.contains("wi-fi direct") ||
                        name.contains("virtualbox")) {
                    continue;
                }

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        // Vérifier que l'IP n'est pas APIPA (169.254.x.x)
                        if (!ip.startsWith("169.254")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Méthode 3 : fallback - hostname
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();
            if (!ip.startsWith("127.")) return ip;
        } catch (Exception ignored) {}

        return "127.0.0.1";
    }

    /**
     * Retourne TOUTES les IP locales (pour affichage)
     */
    public static String getAllLocalIPs() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                String name = ni.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("vmware") ||
                        name.contains("vbox") || name.contains("hamachi") ||
                        name.contains("docker")) {
                    continue;
                }

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append("• ").append(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}

        return sb.length() > 0 ? sb.toString() : "127.0.0.1";
    }

    public String getLocalIPForDisplay() {
        return getBestLocalIP();
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
            if (clientSocket != null) clientSocket.close();
        } catch (Exception ignored) {}

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}

        LoggerUtil.info("DuelServer stopped");
    }
}