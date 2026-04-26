package com.example.pijava_fluently.utils;

public class JitsiUtil {

    // Génère un lien Jitsi stable basé sur l'ID de session
    public static String genererLienJitsi(int sessionId) {
        return "https://meet.jit.si/fluently-session-" + sessionId;
    }

    // Ouvre l'URL dans le navigateur par défaut
    public static void ouvrirDansNavigateur(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}