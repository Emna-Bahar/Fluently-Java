package com.example.pijava_fluently.services;

import javafx.application.Platform;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PrononciationService {

    /**
     * Convertit un texte en audio et le joue (version système uniquement)
     * @param texte Le texte à prononcer
     * @param langue Code langue (fr-fr, en-us, es-es, de-de, it-it, etc.)
     */
    public void prononcer(String texte, String langue) {
        if (texte == null || texte.trim().isEmpty()) {
            System.err.println("Texte vide");
            return;
        }

        new Thread(() -> {
            try {
                jouerAvecSysteme(texte, langue);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> System.err.println("Erreur de prononciation : " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Utilise la synthèse vocale du système d'exploitation
     */
    private void jouerAvecSysteme(String texte, String langue) {
        String os = System.getProperty("os.name").toLowerCase();

        // Échapper les guillemets simples pour éviter les problèmes de commande
        String texteEchappe = texte.replace("'", "'\\''").replace("\"", "\\\"");

        try {
            if (os.contains("win")) {
                // Windows - Utiliser PowerShell avec System.Speech
                String commande = "powershell -Command \"Add-Type –AssemblyName System.Speech; " +
                        "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$synth.Speak('" + texteEchappe + "')\"";

                Process process = Runtime.getRuntime().exec(commande);
                // Attendre un peu que la lecture se termine
                try { Thread.sleep(texte.length() * 50); } catch (InterruptedException e) {}

            } else if (os.contains("mac")) {
                // macOS - Utiliser la commande 'say'
                String voice = getMacVoice(langue);
                String commande = "say -v " + voice + " '" + texteEchappe + "'";
                Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", commande});

            } else if (os.contains("linux")) {
                // Linux - Vérifier si espeak est installé
                try {
                    Process check = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "which espeak"});
                    check.waitFor();
                    if (check.exitValue() == 0) {
                        String voice = getEspeakVoice(langue);
                        String commande = "espeak -v " + voice + " \"" + texte + "\"";
                        Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", commande});
                    } else {
                        System.err.println("espeak n'est pas installé. Installez-le avec: sudo apt-get install espeak");
                    }
                } catch (Exception e) {
                    System.err.println("espeak non trouvé: " + e.getMessage());
                }
            } else {
                System.err.println("Système d'exploitation non supporté pour la synthèse vocale");
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtient la voix macOS appropriée selon la langue
     */
    private String getMacVoice(String langue) {
        switch (langue) {
            case "fr-fr": return "Thomas";
            case "en-us": return "Samantha";
            case "en-gb": return "Daniel";
            case "es-es": return "Monica";
            case "de-de": return "Anna";
            case "it-it": return "Alice";
            default: return "Thomas";
        }
    }

    /**
     * Obtient la voix eSpeak appropriée selon la langue (Linux)
     */
    private String getEspeakVoice(String langue) {
        switch (langue) {
            case "fr-fr": return "fr";
            case "en-us": return "en-us";
            case "en-gb": return "en";
            case "es-es": return "es";
            case "de-de": return "de";
            case "it-it": return "it";
            default: return "fr";
        }
    }

    /**
     * Obtient le code langue pour la synthèse vocale
     */
    public String getLangueCode(String langueNom) {
        switch (langueNom.toLowerCase()) {
            case "français": return "fr-fr";
            case "anglais": return "en-us";
            case "espagnol": return "es-es";
            case "allemand": return "de-de";
            case "italien": return "it-it";
            default: return "fr-fr";
        }
    }
}