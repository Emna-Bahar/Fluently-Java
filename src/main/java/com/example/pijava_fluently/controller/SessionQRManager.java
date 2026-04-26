package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Reservationservice;
import com.example.pijava_fluently.services.Sessionservice;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionQRManager {

    public static final String QR_PREFIX = "fluently://session/";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int HTTP_PORT = 8765;

    private static final String[][] CARD_COLORS = {
            {"#3B82F6","#2563EB"}, {"#6C63FF","#8B5CF6"}, {"#10B981","#059669"},
            {"#F59E0B","#D97706"}, {"#06B6D4","#0891B2"}, {"#EC4899","#DB2777"},
    };

    private final int currentUserId;
    private final Sessionservice     sessionservice     = new Sessionservice();
    private final Reservationservice reservationservice = new Reservationservice();

    private ScheduledExecutorService pollingExecutor;
    private ScheduledFuture<?>       pollingFuture;
    private final AtomicBoolean      reservationDetected = new AtomicBoolean(false);

    private HttpServer      httpServer;
    private ExecutorService httpExecutor;

    public SessionQRManager(int currentUserId) { this.currentUserId = currentUserId; }
    public SessionQRManager() { this.currentUserId = -1; }

    // ══════════════════════════════════════════════════════════════
    // IP LOCALE
    // ══════════════════════════════════════════════════════════════

    private static String getLocalIp() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 80);
            String ip = socket.getLocalAddress().getHostAddress();
            System.out.println("[QR] IP locale : " + ip);
            return ip;
        } catch (Exception ignored) {}

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))
                            return ip;
                    }
                }
            }
        } catch (Exception ignored) {}

        return "127.0.0.1";
    }

    // ══════════════════════════════════════════════════════════════
    // GÉNÉRATION QR — URL HTTP complète
    // ══════════════════════════════════════════════════════════════

    private Image generateQRForPhone(Session session, int size) throws WriterException {
        String ip  = getLocalIp();
        String url = "http://" + ip + ":" + HTTP_PORT
                + "/reserver?sessionId=" + session.getId()
                + "&userId=" + currentUserId;
        System.out.println("[QR] URL encodée dans le QR : " + url);
        return generateQRCodeFromContent(url, size);
    }

    // ══════════════════════════════════════════════════════════════
    // SERVEUR HTTP EMBARQUÉ
    // ══════════════════════════════════════════════════════════════

    private void startHttpServer(int sessionId, Runnable onReservationCreated) {
        try {
            stopHttpServer();

            httpServer   = HttpServer.create(new InetSocketAddress(HTTP_PORT), 10);
            httpExecutor = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r); t.setDaemon(true); return t;
            });
            httpServer.setExecutor(httpExecutor);

            httpServer.createContext("/", (HttpExchange exchange) -> {
                try {
                    String path  = exchange.getRequestURI().getPath();
                    String query = exchange.getRequestURI().getQuery();

                    System.out.println("[HTTP] " + exchange.getRequestMethod()
                            + " " + path + " | query=" + query);

                    // Ignorer favicon
                    if (path.contains("favicon")) {
                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                        return;
                    }

                    // Pas de params → redirection JS vers URL complète (Fix 3 : JS redirect)
                    if (query == null || !query.contains("sessionId")) {
                        String ip  = getLocalIp();
                        String redirect = "http://" + ip + ":" + HTTP_PORT
                                + "/reserver?sessionId=" + sessionId
                                + "&userId=" + currentUserId;
                        byte[] html = buildHtmlRedirect(redirect).getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                        exchange.getResponseHeaders().set("Connection", "close");
                        exchange.sendResponseHeaders(200, html.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(html); }
                        exchange.close();
                        return;
                    }

                    // Parser les paramètres
                    Map<String, String> params = parseQuery(query);
                    int sId, uId;
                    try {
                        sId = Integer.parseInt(params.getOrDefault("sessionId", "-1"));
                        uId = Integer.parseInt(params.getOrDefault("userId",    "-1"));
                    } catch (NumberFormatException ex) {
                        sId = -1; uId = -1;
                    }

                    System.out.println("[HTTP] Params → sessionId=" + sId + " userId=" + uId
                            + " (attendu session=" + sessionId + " user=" + currentUserId + ")");

                    String responseBody;

                    if (sId != sessionId || uId != currentUserId) {
                        responseBody = buildHtmlPage("❌ Erreur",
                                "Paramètres invalides. Rescannez le QR code.", "#E11D48", false);
                    } else {
                        boolean dejaReserve = false;
                        try { dejaReserve = reservationservice.dejaReserve(uId, sId); }
                        catch (SQLException e) {
                            System.err.println("[HTTP] Erreur dejaReserve : " + e.getMessage());
                        }

                        if (dejaReserve) {
                            responseBody = buildHtmlPage("⚠️ Déjà réservé",
                                    "Vous avez déjà une réservation active pour cette session.", "#D97706", false);
                        } else {
                            try {
                                Reservation resa = new Reservation();
                                resa.setDateReservation(LocalDate.now());
                                resa.setStatut("en attente");
                                resa.setIdSessionId(sId);
                                resa.setIdUserId(uId);
                                reservationservice.ajouter(resa);
                                System.out.println("[HTTP] ✅ Réservation créée en BD — session=" + sId + " user=" + uId);

                                responseBody = buildHtmlPage(
                                        "🎉 Réservation envoyée !",
                                        "Votre demande pour la session <strong>#" + sId + "</strong> "
                                                + "a été transmise avec succès au professeur.<br><br>"
                                                + "Statut : <strong>EN ATTENTE DE VALIDATION</strong><br>"
                                                + "Consultez l'application pour suivre votre demande.",
                                        "#10B981", true);

                                // Envoyer la réponse AVANT de notifier l'application
                                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                                exchange.getResponseHeaders().set("Connection", "close");
                                exchange.sendResponseHeaders(200, bytes.length);
                                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                                exchange.close();

                                // Notifier l'application APRÈS que la réponse HTTP soit envoyée
                                Platform.runLater(onReservationCreated);
                                return;

                            } catch (SQLException e) {
                                System.err.println("[HTTP] Erreur ajouter réservation : " + e.getMessage());
                                responseBody = buildHtmlPage("❌ Erreur serveur",
                                        "Impossible de créer la réservation : " + e.getMessage(),
                                        "#E11D48", false);
                            }
                        }
                    }

                    byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.getResponseHeaders().set("Connection", "close");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    exchange.close();

                } catch (Exception e) {
                    System.err.println("[HTTP] Exception : " + e.getMessage());
                    e.printStackTrace();
                    try {
                        byte[] err = "Erreur interne".getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(500, err.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(err); }
                    } catch (Exception ignored) {}
                    exchange.close();
                }
            });

            httpServer.start();
            System.out.println("[QR] Serveur HTTP démarré → http://" + getLocalIp() + ":" + HTTP_PORT
                    + "/reserver?sessionId=" + sessionId + "&userId=" + currentUserId);

        } catch (IOException e) {
            System.err.println("[QR] Impossible de démarrer le serveur HTTP : " + e.getMessage());
        }
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            System.out.println("[QR] Serveur HTTP arrêté.");
        }
        if (httpExecutor != null && !httpExecutor.isShutdown()) {
            httpExecutor.shutdownNow();
            httpExecutor = null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS HTTP
    // ══════════════════════════════════════════════════════════════

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    // FIX 3 : Redirection JS au lieu de meta http-equiv="refresh" (ignoré par iOS/Android)
    private static String buildHtmlRedirect(String url) {
        return "<!DOCTYPE html><html><head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Fluently</title>" +
                "<script>window.location.replace('" + url + "');</script>" +
                "</head><body style='font-family:sans-serif;display:flex;align-items:center;" +
                "justify-content:center;min-height:100vh;margin:0;background:#667eea;'>" +
                "<div style='background:white;border-radius:16px;padding:32px;text-align:center;" +
                "max-width:360px;width:90%;'>" +
                "<div style='font-size:36px;margin-bottom:12px;'>⏳</div>" +
                "<p style='font-size:16px;color:#334155;margin:0 0 16px;'>Redirection en cours...</p>" +
                "<a href='" + url + "' style='display:inline-block;background:#3B82F6;color:white;" +
                "padding:12px 24px;border-radius:12px;text-decoration:none;font-weight:bold;" +
                "font-size:14px;'>Appuyez ici si la page ne change pas</a>" +
                "</div></body></html>";
    }

    private static String buildHtmlPage(String titre, String message, String couleur, boolean success) {
        String emoji = success ? "🎉" : "ℹ️";
        return "<!DOCTYPE html><html lang='fr'><head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Fluently</title>" +
                "<style>" +
                "* { box-sizing:border-box; margin:0; padding:0; }" +
                "body { font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;" +
                "       background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);" +
                "       min-height:100vh; display:flex; align-items:center;" +
                "       justify-content:center; padding:16px; }" +
                ".card { background:white; border-radius:24px; padding:36px 28px;" +
                "        max-width:420px; width:100%; text-align:center;" +
                "        box-shadow:0 20px 60px rgba(0,0,0,0.3); }" +
                ".emoji { font-size:60px; margin-bottom:16px; }" +
                ".titre { font-size:24px; font-weight:800; color:" + couleur + "; margin-bottom:14px; }" +
                ".msg   { font-size:15px; color:#475569; line-height:1.6; }" +
                ".badge { display:inline-block; background:" + couleur + "22; color:" + couleur + ";" +
                "         border-radius:20px; padding:8px 20px; font-weight:700;" +
                "         font-size:13px; margin-top:20px; }" +
                ".logo  { margin-bottom:20px; font-size:22px; font-weight:900;" +
                "         background:linear-gradient(to right,#3B82F6,#7C3AED);" +
                "         -webkit-background-clip:text; -webkit-text-fill-color:transparent; }" +
                "</style></head><body>" +
                "<div class='card'>" +
                "<div class='logo'>✦ Fluently</div>" +
                "<div class='emoji'>" + emoji + "</div>" +
                "<div class='titre'>" + titre + "</div>" +
                "<div class='msg'>" + message + "</div>" +
                (success ? "<div class='badge'>✅ Confirmation enregistrée</div>" : "") +
                "</div></body></html>";
    }

    // ══════════════════════════════════════════════════════════════
    // GÉNÉRATION QR CODE (générique)
    // ══════════════════════════════════════════════════════════════

    public static Image generateQRCode(int sessionId, int size) throws WriterException {
        return generateQRCodeFromContent(QR_PREFIX + sessionId, size);
    }

    public static Image generateQRCodeFromContent(String content, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        return bitMatrixToFxImage(matrix, size);
    }

    public static int extractSessionId(String qrContent) {
        if (qrContent == null || !qrContent.startsWith(QR_PREFIX)) return -1;
        try {
            return Integer.parseInt(qrContent.substring(QR_PREFIX.length()).trim());
        } catch (NumberFormatException e) { return -1; }
    }

    private static Image bitMatrixToFxImage(BitMatrix matrix, int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter pw = image.getPixelWriter();
        javafx.scene.paint.Color dark  = javafx.scene.paint.Color.web("#1E3A8A");
        javafx.scene.paint.Color light = javafx.scene.paint.Color.WHITE;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                pw.setColor(x, y, matrix.get(x, y) ? dark : light);
        return image;
    }

    // ══════════════════════════════════════════════════════════════
    // FENÊTRE QR CODE PROF
    // ══════════════════════════════════════════════════════════════

    public static void showProfQR(Session session, Stage owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("QR Code - Session #" + session.getId());
        stage.setResizable(false);

        Image  qrImage  = null;
        String errorMsg = null;
        try { qrImage = generateQRCode(session.getId(), 320); }
        catch (WriterException e) { errorMsg = "Impossible de generer le QR code : " + e.getMessage(); }

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F8FAFC;");

        VBox header = new VBox(8);
        header.setPadding(new Insets(22, 28, 18, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#3B82F6,#7C3AED);");
        Label titleLbl = new Label("QR Code de la session");
        titleLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");
        String dateStr = session.getDateHeure() != null ? session.getDateHeure().format(FMT) : "-";
        Label subLbl = new Label("Session #" + session.getId() + "  ·  " + dateStr);
        subLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);");
        HBox infoBox = new HBox(8); infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-background-radius:8;-fx-padding:8 12 8 12;");
        Label infoText = new Label("Partagez ce QR avec vos etudiants pour reserver la session.");
        infoText.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.90);-fx-wrap-text:true;");
        infoText.setWrapText(true); HBox.setHgrow(infoText, Priority.ALWAYS);
        infoBox.getChildren().add(infoText);
        header.getChildren().addAll(titleLbl, subLbl, infoBox);

        VBox qrBox = new VBox(16); qrBox.setAlignment(Pos.CENTER);
        qrBox.setPadding(new Insets(28, 36, 20, 36));
        qrBox.setStyle("-fx-background-color:#FFFFFF;");

        if (errorMsg != null) {
            Label errLbl = new Label(errorMsg);
            errLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#E11D48;");
            qrBox.getChildren().add(errLbl);
        } else {
            VBox qrFrame = new VBox(0); qrFrame.setAlignment(Pos.CENTER);
            qrFrame.setPadding(new Insets(16));
            qrFrame.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                    "-fx-border-color:#E2E8F0;-fx-border-radius:16;-fx-border-width:2;" +
                    "-fx-effect:dropshadow(gaussian,rgba(59,130,246,0.15),20,0,0,6);");
            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(280); qrView.setFitHeight(280);
            qrView.setPreserveRatio(true); qrView.setSmooth(false);
            qrFrame.getChildren().add(qrView);
            qrBox.getChildren().add(qrFrame);
            HBox chips = new HBox(10); chips.setAlignment(Pos.CENTER);
            if (session.getDuree() != null) chips.getChildren().add(makeChip("Duree " + session.getDuree() + " min", "#EFF6FF", "#3B82F6"));
            if (session.getPrix()  != null) chips.getChildren().add(makeChip(String.format("%.0f TND", session.getPrix()), "#F0FDF4", "#16A34A"));
            chips.getChildren().add(makeChip(session.getStatut() != null ? session.getStatut() : "-", "#EEF2FF", "#6C63FF"));
            qrBox.getChildren().add(chips);
        }

        HBox btnRow = new HBox(12); btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(14, 28, 24, 28));
        btnRow.setStyle("-fx-background-color:#F8FAFC;-fx-border-color:#E2E8F0;-fx-border-width:1 0 0 0;");
        if (qrImage != null) {
            final Image finalQr = qrImage;
            Button btnSave = new Button("Sauvegarder PNG");
            btnSave.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;" +
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:12;" +
                    "-fx-padding:10 22 10 22;-fx-cursor:hand;");
            btnSave.setOnAction(e -> saveQRCodeToPng(finalQr, session, stage));
            btnRow.getChildren().add(btnSave);
        }
        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color:linear-gradient(to right,#3B82F6,#7C3AED);" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:12;-fx-padding:10 28 10 28;-fx-cursor:hand;");
        btnClose.setOnAction(e -> stage.close());
        btnRow.getChildren().add(btnClose);

        root.getChildren().addAll(header, qrBox, btnRow);
        stage.setScene(new Scene(root, 400, 530));
        stage.show();
    }

    private static void saveQRCodeToPng(Image qrImage, Session session, Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sauvegarder le QR Code");
        chooser.setInitialFileName("qr_session_" + session.getId() + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;
        try {
            BufferedImage buffered = SwingFXUtils.fromFXImage(qrImage, null);
            ImageIO.write(buffered, "png", file);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("QR Code sauvegarde"); ok.setHeaderText(null);
            ok.setContentText("Sauvegarde : " + file.getAbsolutePath());
            ok.showAndWait();
        } catch (IOException e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setContentText("Impossible de sauvegarder : " + e.getMessage());
            err.showAndWait();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // FENÊTRE RÉSERVATION ÉTUDIANT
    // ══════════════════════════════════════════════════════════════

    public void showReservationDialog(Session session, Stage owner, Runnable onReservationDone) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Réserver - Session #" + session.getId());
        stage.setResizable(false);

        reservationDetected.set(false);

        // FIX 1 : On n'arrête le serveur que si la réservation N'A PAS été détectée (= annulation)
        stage.setOnHidden(e -> {
            stopPolling();
            if (!reservationDetected.get()) {
                stopHttpServer();
            }
        });

        int ci = (int)(session.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];

        final boolean[] canBookRef    = {true};
        final String[]  warningMsgRef = {null};
        final int[]     placesRef     = {0};
        try {
            if (reservationservice.dejaReserve(currentUserId, session.getId())) {
                warningMsgRef[0] = "Vous avez déjà une réservation pour cette session.";
                canBookRef[0]    = false;
            } else if ("terminee".equals(session.getStatut()) || "annulee".equals(session.getStatut())) {
                warningMsgRef[0] = "Cette session est " + session.getStatut() + " — réservation impossible.";
                canBookRef[0]    = false;
            } else if (session.getCapaciteMax() != null) {
                int nbAcc = sessionservice.compterReservationsAcceptees(session.getId());
                placesRef[0] = session.getCapaciteMax() - nbAcc;
                if (placesRef[0] <= 0) {
                    warningMsgRef[0] = "Cette session est complète — plus aucune place disponible.";
                    canBookRef[0]    = false;
                }
            }
        } catch (SQLException ignored) {}

        final boolean canBook    = canBookRef[0];
        final String  warningMsg = warningMsgRef[0];
        final int     places     = placesRef[0];

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F8FAFC;");

        VBox header = new VBox(10);
        header.setPadding(new Insets(22, 28, 18, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");
        Label hTitle = new Label("Confirmer votre réservation");
        hTitle.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hSub = new Label("Session #" + session.getId()
                + (session.getDateHeure() != null ? "  ·  " + session.getDateHeure().format(FMT) : ""));
        hSub.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);");
        HBox headerChips = new HBox(8); headerChips.setAlignment(Pos.CENTER_LEFT);
        if (session.getDuree()  != null) headerChips.getChildren().add(makeChipWhite(session.getDuree() + " min"));
        if (session.getPrix()   != null) headerChips.getChildren().add(makeChipWhite(String.format("%.0f TND", session.getPrix())));
        if (canBook && session.getCapaciteMax() != null)
            headerChips.getChildren().add(makeChipWhite(places + " place(s)"));
        header.getChildren().addAll(hTitle, hSub, headerChips);

        VBox body = new VBox(0);
        body.setStyle("-fx-background-color:#FFFFFF;");

        if (warningMsg != null) {
            VBox warnBox = new VBox(16);
            warnBox.setAlignment(Pos.CENTER);
            warnBox.setPadding(new Insets(36, 28, 32, 28));
            Label warnIcon = new Label("⚠");
            warnIcon.setStyle("-fx-font-size:44px;");
            Label warnLbl = new Label(warningMsg);
            warnLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#D97706;" +
                    "-fx-background-color:#FFF7ED;-fx-background-radius:12;" +
                    "-fx-padding:14 18 14 18;-fx-border-color:#FDE68A;" +
                    "-fx-border-radius:12;-fx-border-width:1;-fx-wrap-text:true;");
            warnLbl.setWrapText(true); warnLbl.setMaxWidth(380);
            warnBox.getChildren().addAll(warnIcon, warnLbl);
            body.getChildren().add(warnBox);

        } else {
            VBox instrBox = new VBox(6);
            instrBox.setPadding(new Insets(20, 28, 10, 28));
            instrBox.setAlignment(Pos.CENTER);
            Label instrTitle = new Label("Scannez ce QR Code avec votre téléphone");
            instrTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
            Label instrSub = new Label("Ouvrez votre navigateur mobile et scannez. La réservation sera créée automatiquement.");
            instrSub.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;-fx-wrap-text:true;-fx-text-alignment:center;");
            instrSub.setWrapText(true); instrSub.setMaxWidth(400);
            Label ipLbl = new Label("Réseau : " + getLocalIp() + ":" + HTTP_PORT);
            ipLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;-fx-font-style:italic;");
            instrBox.getChildren().addAll(instrTitle, instrSub, ipLbl);
            body.getChildren().add(instrBox);

            VBox qrArea = new VBox(12);
            qrArea.setAlignment(Pos.CENTER);
            qrArea.setPadding(new Insets(4, 28, 16, 28));

            VBox qrFrame = new VBox(0); qrFrame.setAlignment(Pos.CENTER);
            qrFrame.setPadding(new Insets(16));
            qrFrame.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                    "-fx-border-color:#E2E8F0;-fx-border-radius:16;-fx-border-width:2;" +
                    "-fx-effect:dropshadow(gaussian,rgba(59,130,246,0.12),18,0,0,5);");
            ImageView qrView = new ImageView();
            qrView.setFitWidth(240); qrView.setFitHeight(240);
            qrView.setPreserveRatio(true); qrView.setSmooth(false);
            qrFrame.getChildren().add(qrView);

            HBox scanIndicator = new HBox(8);
            scanIndicator.setAlignment(Pos.CENTER);
            scanIndicator.setPadding(new Insets(8, 16, 8, 16));
            scanIndicator.setStyle("-fx-background-color:#EEF2FF;-fx-background-radius:20;");
            Label scanDot = new Label("●");
            scanDot.setStyle("-fx-font-size:10px;-fx-text-fill:#6C63FF;");
            Label scanText = new Label("En attente du scan...");
            scanText.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");
            scanIndicator.getChildren().addAll(scanDot, scanText);

            FadeTransition pulse = new FadeTransition(Duration.millis(800), scanDot);
            pulse.setFromValue(1.0); pulse.setToValue(0.2);
            pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();

            Label hintLbl = new Label("Pointez l'appareil photo vers ce code — le navigateur s'ouvre automatiquement");
            hintLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;-fx-font-style:italic;-fx-text-alignment:center;");
            hintLbl.setWrapText(true); hintLbl.setMaxWidth(360);

            qrArea.getChildren().addAll(qrFrame, scanIndicator, hintLbl);
            body.getChildren().add(qrArea);

            javafx.concurrent.Task<Image> qrTask = new javafx.concurrent.Task<>() {
                @Override protected Image call() throws Exception {
                    return generateQRForPhone(session, 260);
                }
            };

            qrTask.setOnSucceeded(ev -> {
                qrView.setImage(qrTask.getValue());
                qrFrame.setOpacity(0); qrFrame.setScaleX(0.85); qrFrame.setScaleY(0.85);
                FadeTransition  fade  = new FadeTransition(Duration.millis(400), qrFrame);
                fade.setFromValue(0); fade.setToValue(1);
                ScaleTransition scale = new ScaleTransition(Duration.millis(400), qrFrame);
                scale.setFromX(0.85); scale.setToX(1.0);
                scale.setFromY(0.85); scale.setToY(1.0);
                scale.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(fade, scale).play();

                // FIX 2 : Le serveur est démarré ici et ne s'arrête QUE dans showSuccessWindow.setOnHidden
                startHttpServer(session.getId(), () -> {
                    if (!reservationDetected.getAndSet(true)) {
                        pulse.stop();
                        scanDot.setStyle("-fx-font-size:12px;-fx-text-fill:#10B981;");
                        scanText.setText("✔  Réservation créée !");
                        scanText.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#10B981;");
                        PauseTransition pauseAnim = new PauseTransition(Duration.millis(600));
                        pauseAnim.setOnFinished(e -> {
                            stage.close();
                            showSuccessWindow(session, owner, onReservationDone);
                        });
                        pauseAnim.play();
                    }
                });
            });

            qrTask.setOnFailed(ev -> {
                scanText.setText("Erreur de génération QR");
                scanText.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#E11D48;");
            });

            new Thread(qrTask) {{ setDaemon(true); }}.start();
        }

        HBox btnRow = new HBox();
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(16, 28, 22, 28));
        btnRow.setStyle("-fx-background-color:#F8FAFC;-fx-border-color:#E2E8F0;-fx-border-width:1 0 0 0;");
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#64748B;" +
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:12;" +
                "-fx-padding:11 44 11 44;-fx-cursor:hand;");
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
                "-fx-background-color:#E2E8F0;-fx-text-fill:#475569;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:12;" +
                        "-fx-padding:11 44 11 44;-fx-cursor:hand;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle(
                "-fx-background-color:#F1F5F9;-fx-text-fill:#64748B;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:12;" +
                        "-fx-padding:11 44 11 44;-fx-cursor:hand;"));
        btnAnnuler.setOnAction(e -> stage.close());
        btnRow.getChildren().add(btnAnnuler);

        root.getChildren().addAll(header, body, btnRow);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F8FAFC;-fx-background:#F8FAFC;");
        stage.setScene(new Scene(scroll, 480, canBook ? 560 : 380));
        stage.show();
    }

    // ══════════════════════════════════════════════════════════════
    // POLLING (conservé comme fallback)
    // ══════════════════════════════════════════════════════════════

    private void stopPolling() {
        if (pollingFuture  != null && !pollingFuture.isCancelled())   pollingFuture.cancel(false);
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) pollingExecutor.shutdownNow();
    }

    // ══════════════════════════════════════════════════════════════
    // FENÊTRE SUCCÈS
    // ══════════════════════════════════════════════════════════════

    private void showSuccessWindow(Session session, Stage owner, Runnable onReservationDone) {
        Stage s = new Stage();
        s.initModality(Modality.WINDOW_MODAL);
        if (owner != null) s.initOwner(owner);
        s.setTitle("Réservation confirmée !");
        s.setResizable(false);

        // FIX 2 : Le serveur HTTP s'arrête ici, après que l'utilisateur ferme la fenêtre succès
        s.setOnHidden(e -> {
            stopHttpServer();
            if (onReservationDone != null) onReservationDone.run();
        });

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F0FDF4;");

        VBox header = new VBox(14);
        header.setPadding(new Insets(36, 32, 28, 32));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,#10B981,#059669,#047857);");
        Label iconLbl = new Label("🎉");
        iconLbl.setStyle("-fx-font-size:56px;");
        Label titleLbl = new Label("Félicitations !");
        titleLbl.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label subtitleLbl = new Label("Votre demande de réservation a été envoyée avec succès !");
        subtitleLbl.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(255,255,255,0.93);" +
                "-fx-wrap-text:true;-fx-text-alignment:center;-fx-font-weight:bold;");
        subtitleLbl.setWrapText(true); subtitleLbl.setMaxWidth(360);
        String dateStr = session.getDateHeure() != null ? session.getDateHeure().format(FMT) : "-";
        Label sessionLbl = new Label("Session #" + session.getId() + "  ·  " + dateStr);
        sessionLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.80);" +
                "-fx-background-color:rgba(255,255,255,0.18);-fx-background-radius:20;" +
                "-fx-padding:5 14 5 14;");
        header.getChildren().addAll(iconLbl, titleLbl, subtitleLbl, sessionLbl);

        VBox body = new VBox(10);
        body.setPadding(new Insets(24, 28, 16, 28));
        String[][] lignes = {
                {"✅","Demande transmise au professeur",                      "#D1FAE5","#065F46","#A7F3D0"},
                {"⏳","Statut : EN ATTENTE DE VALIDATION",                    "#DBEAFE","#1E40AF","#BFDBFE"},
                {"📋","Consultez Mes Réservations pour suivre votre demande", "#EDE9FE","#4C1D95","#DDD6FE"},
                {"👁","La session a disparu de Sessions disponibles",          "#FEF3C7","#92400E","#FDE68A"},
        };
        for (String[] ligne : lignes) {
            HBox row = new HBox(14); row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12, 16, 12, 16));
            row.setStyle("-fx-background-color:" + ligne[2] + ";-fx-background-radius:12;" +
                    "-fx-border-color:" + ligne[4] + ";-fx-border-radius:12;-fx-border-width:1;");
            Label icon = new Label(ligne[0]); icon.setStyle("-fx-font-size:18px;-fx-min-width:26;");
            Label tx = new Label(ligne[1]);
            tx.setStyle("-fx-font-size:13px;-fx-text-fill:" + ligne[3] + ";-fx-wrap-text:true;-fx-font-weight:bold;");
            tx.setWrapText(true); HBox.setHgrow(tx, Priority.ALWAYS);
            row.getChildren().addAll(icon, tx);
            body.getChildren().add(row);
        }

        HBox btnRow = new HBox(); btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(20, 32, 32, 32));
        Button btnOk = new Button("🎉  Parfait, merci !");
        String styleBtnOk = "-fx-background-color:linear-gradient(to right,#10B981,#059669);" +
                "-fx-text-fill:white;-fx-font-size:15px;-fx-font-weight:bold;" +
                "-fx-background-radius:16;-fx-padding:14 48 14 48;-fx-cursor:hand;";
        btnOk.setStyle(styleBtnOk);
        btnOk.setOnMouseEntered(e -> btnOk.setStyle(
                "-fx-background-color:linear-gradient(to right,#059669,#047857);" +
                        "-fx-text-fill:white;-fx-font-size:15px;-fx-font-weight:bold;" +
                        "-fx-background-radius:16;-fx-padding:14 48 14 48;-fx-cursor:hand;"));
        btnOk.setOnMouseExited(e -> btnOk.setStyle(styleBtnOk));
        btnOk.setOnAction(ev -> {
            ScaleTransition down = new ScaleTransition(Duration.millis(100), btnOk);
            down.setToX(0.93); down.setToY(0.93);
            ScaleTransition up = new ScaleTransition(Duration.millis(150), btnOk);
            up.setToX(1.0); up.setToY(1.0); up.setInterpolator(Interpolator.EASE_OUT);
            SequentialTransition bounce = new SequentialTransition(down, up);
            bounce.setOnFinished(finish -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), root);
                fadeOut.setFromValue(1); fadeOut.setToValue(0);
                fadeOut.setOnFinished(done -> s.close());
                fadeOut.play();
            });
            bounce.play();
        });
        btnRow.getChildren().add(btnOk);
        root.getChildren().addAll(header, body, btnRow);

        s.setOnShown(ev -> {
            root.setOpacity(0); root.setTranslateY(32);
            root.setScaleX(0.88); root.setScaleY(0.88);
            FadeTransition      fade  = new FadeTransition(Duration.millis(500), root);
            fade.setFromValue(0); fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(500), root);
            slide.setFromY(32); slide.setToY(0); slide.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition     scale = new ScaleTransition(Duration.millis(500), root);
            scale.setFromX(0.88); scale.setToX(1.0);
            scale.setFromY(0.88); scale.setToY(1.0); scale.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, slide, scale).play();

            iconLbl.setScaleX(0); iconLbl.setScaleY(0);
            ScaleTransition iconBounce = new ScaleTransition(Duration.millis(600), iconLbl);
            iconBounce.setFromX(0); iconBounce.setToX(1.0);
            iconBounce.setFromY(0); iconBounce.setToY(1.0);
            iconBounce.setInterpolator(Interpolator.EASE_OUT);
            iconBounce.setDelay(Duration.millis(250));
            iconBounce.play();

            int delay = 350;
            for (javafx.scene.Node node : body.getChildren()) {
                node.setOpacity(0); node.setTranslateX(-24);
                FadeTransition  f = new FadeTransition(Duration.millis(320), node);
                f.setFromValue(0); f.setToValue(1);
                TranslateTransition t = new TranslateTransition(Duration.millis(320), node);
                t.setFromX(-24); t.setToX(0); t.setInterpolator(Interpolator.EASE_OUT);
                ParallelTransition pt = new ParallelTransition(f, t);
                pt.setDelay(Duration.millis(delay)); pt.play();
                delay += 90;
            }

            btnOk.setOpacity(0); btnOk.setScaleX(0.8); btnOk.setScaleY(0.8);
            FadeTransition  btnFade  = new FadeTransition(Duration.millis(300), btnOk);
            btnFade.setFromValue(0); btnFade.setToValue(1);
            ScaleTransition btnScale = new ScaleTransition(Duration.millis(300), btnOk);
            btnScale.setFromX(0.8); btnScale.setToX(1.0);
            btnScale.setFromY(0.8); btnScale.setToY(1.0);
            btnScale.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition btnAnim = new ParallelTransition(btnFade, btnScale);
            btnAnim.setDelay(Duration.millis(delay)); btnAnim.play();
        });

        s.setScene(new Scene(root));
        s.sizeToScene();
        s.show();
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS UI
    // ══════════════════════════════════════════════════════════════

    private static Label makeChip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return l;
    }

    private static Label makeChipWhite(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return l;
    }
}