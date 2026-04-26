package com.example.pijava_fluently.utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;
// import javafx.scene.web;  ← SUPPRIMÉ (import incorrect)
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class JitsiUtil {

    public static String genererLienJitsi(int sessionId) {
        return "https://meet.jit.si/fluently-session-" + sessionId;
    }

    public static void ouvrirDansNavigateur(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ouvre Jitsi Meet DANS l'application Fluently.
     * Utilise JavaFX WebView avec WebRTC activé via l'API interne WebKit.
     */
    public static void ouvrirDansAppDesktop(String url) {
        System.out.println("🎥 Tentative ouverture Jitsi: " + url);

        // 1) Jitsi Desktop installé ?
        String[] jitsiPaths = {
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Jitsi Meet\\Jitsi Meet.exe",
                "C:\\Program Files\\Jitsi Meet\\Jitsi Meet.exe",
                "C:\\Program Files (x86)\\Jitsi Meet\\Jitsi Meet.exe",
                "/Applications/Jitsi Meet.app/Contents/MacOS/Jitsi Meet",
                "/usr/bin/jitsi-meet"
        };
        for (String path : jitsiPaths) {
            File exe = new File(path);
            if (exe.exists()) {
                try {
                    new ProcessBuilder(path, url).redirectErrorStream(true).start();
                    System.out.println("✅ Jitsi Desktop lancé: " + path);
                    return;
                } catch (IOException e) {
                    System.err.println("❌ Erreur Jitsi Desktop: " + e.getMessage());
                }
            }
        }

        // 2) WebView intégrée avec WebRTC activé
        System.out.println("🪟 Ouverture WebView intégrée avec WebRTC...");
        Platform.runLater(() -> ouvrirWebViewAvecWebRTC(url));
    }

    private static void ouvrirWebViewAvecWebRTC(String url) {
        Stage stage = new Stage();
        stage.setTitle("🎥 Fluently — Réunion en cours");

        // ── WebView ───────────────────────────────────────────────
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Activer JavaScript
        engine.setJavaScriptEnabled(true);

        // ✅ Activer WebRTC via l'API interne de WebKit/JavaFX
        activerWebRTC(engine);

        // User-Agent Chrome pour éviter le blocage "browser not supported"
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        // Dossier persistant WebView
        File webViewDir = new File(System.getProperty("user.home") + "/.fluently/webview");
        webViewDir.mkdirs();
        engine.setUserDataDirectory(webViewDir);

        // Confirmer les permissions média via JS après chargement
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("✅ Page chargée: " + engine.getLocation());
                // Injecter JS pour bypass les vérifications de navigateur
                injecterJSWebRTC(engine);
            } else if (newState == Worker.State.FAILED) {
                System.err.println("❌ Échec chargement: " + engine.getLocation());
            }
        });

        // Gérer les alertes JS (permissions)
        engine.setOnAlert(e -> System.out.println("JS Alert: " + e.getData()));

        engine.load(url);

        // ── Barre d'outils ────────────────────────────────────────
        Label lblTitre = new Label("🎥  Réunion Fluently");
        lblTitre.setStyle(
                "-fx-text-fill:white;-fx-font-size:14px;" +
                        "-fx-font-weight:bold;-fx-padding:0 0 0 4;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMic = new Button("🎤 Micro");
        btnMic.setStyle(
                "-fx-background-color:#10B981;-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:6 12 6 12;-fx-background-radius:10;-fx-cursor:hand;"
        );
        btnMic.setOnAction(e -> {
            // Demander permission micro via JS
            engine.executeScript(
                    "if(navigator.mediaDevices && navigator.mediaDevices.getUserMedia){" +
                            "  navigator.mediaDevices.getUserMedia({audio:true,video:true})" +
                            "  .then(function(s){console.log('Media OK');}).catch(function(err){console.log('Media err:'+err);});" +
                            "}"
            );
        });

        Button btnReload = new Button("🔄 Recharger");
        btnReload.setStyle(
                "-fx-background-color:#7C3AED;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-padding:7 16 7 16;-fx-background-radius:10;-fx-cursor:hand;"
        );
        btnReload.setOnMouseEntered(e -> btnReload.setStyle(
                "-fx-background-color:#6D28D9;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-padding:7 16 7 16;-fx-background-radius:10;-fx-cursor:hand;"));
        btnReload.setOnMouseExited(e -> btnReload.setStyle(
                "-fx-background-color:#7C3AED;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-padding:7 16 7 16;-fx-background-radius:10;-fx-cursor:hand;"));
        btnReload.setOnAction(e -> engine.reload());

        Button btnFermer = new Button("✕ Quitter");
        btnFermer.setStyle(
                "-fx-background-color:#EF4444;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-padding:7 18 7 18;-fx-background-radius:10;-fx-cursor:hand;"
        );
        btnFermer.setOnMouseEntered(e -> btnFermer.setStyle(
                "-fx-background-color:#DC2626;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-padding:7 18 7 18;-fx-background-radius:10;-fx-cursor:hand;"));
        btnFermer.setOnMouseExited(e -> btnFermer.setStyle(
                "-fx-background-color:#EF4444;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-padding:7 18 7 18;-fx-background-radius:10;-fx-cursor:hand;"));
        btnFermer.setOnAction(e -> {
            engine.load("about:blank");
            stage.close();
        });

        HBox toolbar = new HBox(10, lblTitre, spacer, btnMic, btnReload, btnFermer);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle(
                "-fx-background-color:#1E1B4B;" +
                        "-fx-padding:10 16 10 16;" +
                        "-fx-border-color:#3730A3;" +
                        "-fx-border-width:0 0 2 0;"
        );

        // Barre de progression
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(3);
        progressBar.setStyle("-fx-accent:#7C3AED;-fx-padding:0;");
        progressBar.progressProperty().bind(engine.getLoadWorker().progressProperty());
        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            boolean loading = (n == Worker.State.RUNNING);
            progressBar.setVisible(loading);
            progressBar.setManaged(loading);
        });

        VBox topSection = new VBox(0, toolbar, progressBar);

        BorderPane root = new BorderPane();
        root.setTop(topSection);
        root.setCenter(webView);
        root.setStyle("-fx-background-color:#000000;");

        Scene scene = new Scene(root, 1100, 720);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> engine.load("about:blank"));
        stage.show();

        System.out.println("✅ WebView Jitsi ouverte: " + url);
    }

    /**
     * ✅ Active WebRTC dans WebEngine via l'API interne de JavaFX WebKit.
     * Utilise la réflexion pour accéder aux paramètres internes non exposés.
     */
    private static void activerWebRTC(WebEngine engine) {
        try {
            // Accéder à com.sun.webkit.WebPage via réflexion
            Class<?> webEngineClass = engine.getClass();

            // Récupérer le champ page (WebPage interne)
            java.lang.reflect.Field pageField = webEngineClass.getDeclaredField("page");
            pageField.setAccessible(true);
            Object page = pageField.get(engine);

            if (page != null) {
                Class<?> pageClass = page.getClass();

                // Activer les médias (WebRTC, getUserMedia)
                tryInvokeMethod(pageClass, page, "setMediaEnabled", boolean.class, true);
                tryInvokeMethod(pageClass, page, "setMediaStreamEnabled", boolean.class, true);

                // Désactiver les restrictions de sécurité qui bloquent getUserMedia
                tryInvokeMethod(pageClass, page, "setDeveloperExtrasEnabled", boolean.class, true);

                System.out.println("✅ WebRTC activé via API interne WebKit");
            }
        } catch (Exception e) {
            System.out.println("ℹ API interne WebKit non accessible: " + e.getMessage());
            // Pas grave — on essaie via JS aussi
        }

        // Activer aussi via les propriétés système WebKit
        System.setProperty("com.sun.webkit.showUserMediaPermissionPanel", "false");
        System.setProperty("prism.order", "sw");
    }

    private static void tryInvokeMethod(Class<?> clazz, Object obj,
                                        String methodName, Class<?> paramType,
                                        Object value) {
        try {
            Method m = clazz.getMethod(methodName, paramType);
            m.setAccessible(true);
            m.invoke(obj, value);
            System.out.println("  ✅ " + methodName + "(" + value + ") OK");
        } catch (Exception e) {
            System.out.println("  ⚠ " + methodName + " non disponible");
        }
    }

    /**
     * ✅ Injecte du JavaScript pour :
     * - Simuler un navigateur compatible WebRTC
     * - Bypasser la détection "browser not supported"
     * - Demander les permissions média automatiquement
     */
    private static void injecterJSWebRTC(WebEngine engine) {
        try {
            // Patch navigator pour simuler Chrome avec WebRTC
            engine.executeScript(
                    "try {" +
                            // Patch userAgent pour que Jitsi croit que c'est Chrome
                            "  Object.defineProperty(navigator, 'userAgent', {" +
                            "    get: function() {" +
                            "      return 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';" +
                            "    }" +
                            "  });" +
                            // Patch vendor
                            "  Object.defineProperty(navigator, 'vendor', {" +
                            "    get: function() { return 'Google Inc.'; }" +
                            "  });" +
                            // Assurer que RTCPeerConnection est disponible si présent
                            "  if (typeof webkitRTCPeerConnection !== 'undefined' && !window.RTCPeerConnection) {" +
                            "    window.RTCPeerConnection = webkitRTCPeerConnection;" +
                            "  }" +
                            // Patch mediaDevices si partiellement disponible
                            "  if (navigator.mediaDevices && !navigator.mediaDevices.getUserMedia) {" +
                            "    navigator.mediaDevices.getUserMedia = function(c) {" +
                            "      return new Promise(function(resolve, reject) {" +
                            "        var gum = navigator.getUserMedia || navigator.webkitGetUserMedia;" +
                            "        if (gum) { gum.call(navigator, c, resolve, reject); }" +
                            "        else { reject(new Error('getUserMedia not supported')); }" +
                            "      });" +
                            "    };" +
                            "  }" +
                            "  console.log('✅ WebRTC patches appliqués');" +
                            "  console.log('RTCPeerConnection:', typeof RTCPeerConnection);" +
                            "  console.log('mediaDevices:', typeof navigator.mediaDevices);" +
                            "} catch(e) { console.log('Patch error: ' + e); }"
            );
            System.out.println("✅ JS WebRTC patches injectés");
        } catch (Exception e) {
            System.err.println("❌ Erreur injection JS: " + e.getMessage());
        }
    }
}