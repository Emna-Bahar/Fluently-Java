package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.UserService;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import com.example.pijava_fluently.utils.ConfigLoader;

/**
 * Handles the Google OAuth2 flow:
 *  1. Opens a browser to Google's consent screen
 *  2. Starts a local HTTP server on port 8080 to catch the redirect
 *  3. Exchanges the auth code for an ID token
 *  4. Decodes the JWT to get email + name
 *  5. Creates/updates the user in DB then navigates home
 */
public class GoogleAuthController {

    // ── Replace these with your real values from Google Cloud Console ────────
    private static final String CLIENT_ID     = ConfigLoader.get("google.auth.client.id");
    private static final String CLIENT_SECRET = ConfigLoader.get("google.auth.client.secret");
    // ─────────────────────────────────────────────────────────────────────────

    private static final String AUTH_URL      = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    private static volatile int chosenPort    = 0;  // set when server starts

    private final UserService userService = new UserService();
    private final Stage       stage;
    private final Label       statusLabel;   // pass a label to show feedback

    public GoogleAuthController(Stage stage, Label statusLabel) {
        this.stage       = stage;
        this.statusLabel = statusLabel;
    }

    /** Call this when the user clicks "Continue with Google" */
    public void startGoogleLogin() {
        setStatus("⏳ Ouverture de Google...", false);

        new Thread(() -> {
            try {
                // 1 ── Start local callback server FIRST to get the port
                String code = waitForCode();
                // Now chosenPort is set, build the redirect URI
                String redirectUri = "http://localhost:" + chosenPort + "/callback";
                if (code == null) { setStatus("❌ Connexion annulée.", true); return; }

                // 4 ── Exchange code for tokens
                String tokenJson = exchangeCode(code, redirectUri);
                System.out.println("[Google] tokenJson: " + tokenJson);
                if (tokenJson == null) { setStatus("❌ Erreur token Google.", true); return; }

                // 5 ── Decode ID token payload (middle base64 part)
                String idToken  = extractField(tokenJson, "id_token");
                System.out.println("[Google] id_token: " + (idToken != null ? idToken.substring(0, Math.min(40, idToken.length())) + "..." : "NULL"));
                String payload  = decodeJwtPayload(idToken);
                System.out.println("[Google] payload: " + payload);
                String email    = extractField(payload, "email");
                String given    = extractField(payload, "given_name");
                String family   = extractField(payload, "family_name");
                System.out.println("[Google] email=" + email + " given=" + given + " family=" + family);

                if (email == null) { setStatus("❌ Email introuvable. Voir console IntelliJ.", true); return; }

                // 6 ── Create or update user in DB
                User user = userService.findByEmail(email);
                if (user == null) {
                    user = new User();
                    user.setEmail(email);
                    user.setPrenom(given  != null ? given  : "");
                    user.setNom(family    != null ? family : "");
                    user.setPassword(UserService.hashPassword(java.util.UUID.randomUUID().toString()));
                    user.setRoles("[\"ROLE_ETUDIANT\"]");
                    user.setStatut("actif");
                    userService.ajouter(user);
                    user = userService.findByEmail(email);  // reload with generated id
                } else {
                    // Update name in case it changed
                    if (given  != null) user.setPrenom(given);
                    if (family != null) user.setNom(family);
                    userService.modifier(user);
                }

                userService.updateStatut(user.getId(), "online");
                final User finalUser = user;

                // 7 ── Navigate to home on JavaFX thread
                Platform.runLater(() -> navigateHome(finalUser));

            } catch (Exception e) {
                e.printStackTrace();
                setStatus("❌ Erreur: " + e.getMessage(), true);
            }
        }).start();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Spin up a one-shot HTTP server and wait for Google to redirect back */
    private String waitForCode() throws Exception {
        final String[] result = {null};
        // Find a free port automatically
        int port;
        try (java.net.ServerSocket tmp = new java.net.ServerSocket(0)) {
            port = tmp.getLocalPort();
        }
        chosenPort = port;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && kv[0].equals("code")) {
                        result[0] = URLDecoder.decode(kv[1], "UTF-8");
                    }
                }
            }
            String html = "<html><body style='font-family:sans-serif;text-align:center;padding:60px'>"
                    + "<h2>✅ Connecté ! Retourne sur Fluently.</h2></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
            server.stop(1);
        });
        server.start();
        System.out.println("[Google] Local server started on port: " + port);

        // NOW open browser — port is known
        String redirectUri = "http://localhost:" + port + "/callback";
        String url = AUTH_URL
                + "?client_id="     + URLEncoder.encode(CLIENT_ID, "UTF-8")
                + "&redirect_uri="  + URLEncoder.encode(redirectUri, "UTF-8")
                + "&response_type=code"
                + "&scope="         + URLEncoder.encode("openid email profile", "UTF-8")
                + "&access_type=offline"
                + "&prompt=select_account";
        Desktop.getDesktop().browse(new URI(url));

        // Wait up to 2 minutes
        long start = System.currentTimeMillis();
        while (result[0] == null && System.currentTimeMillis() - start < 120_000) {
            Thread.sleep(200);
        }
        return result[0];
    }

    /** POST to Google's token endpoint and get back JSON */
    private String exchangeCode(String code, String redirectUri) throws Exception {
        String body = "code="          + URLEncoder.encode(code,          "UTF-8")
                + "&client_id="    + URLEncoder.encode(CLIENT_ID,     "UTF-8")
                + "&client_secret="+ URLEncoder.encode(CLIENT_SECRET, "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(redirectUri,   "UTF-8")
                + "&grant_type=authorization_code";

        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Decode the middle (payload) part of a JWT — no external library needed */
    private String decodeJwtPayload(String jwt) {
        if (jwt == null) return null;
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return null;
        byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String padBase64(String s) {
        int pad = 4 - s.length() % 4;
        if (pad < 4) s += "=".repeat(pad);
        return s;
    }

    /** Simple JSON field extractor — avoids adding a JSON library */
    private String extractField(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    private void setStatus(String msg, boolean isError) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(msg);
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
                statusLabel.setStyle(isError
                        ? "-fx-text-fill: #e74c3c;"
                        : "-fx-text-fill: #27ae60;");
            }
        });
    }

    private void navigateHome(User user) {
        try {
            String fxml = user.isAdmin()
                    ? "/com/example/pijava_fluently/fxml/admin-dashboard.fxml"
                    : "/com/example/pijava_fluently/fxml/home.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            if (user.isAdmin()) {
                AdminDashboardController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
            } else {
                HomeController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Fluently - Mon Espace");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("❌ Erreur chargement page.", true);
        }
    }
}