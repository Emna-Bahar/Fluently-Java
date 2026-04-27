package com.example.pijava_fluently.utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TranslationManager {

    private static String currentLang = "fr";
    private static final Map<String, Map<String, String>> cache = new HashMap<>();

    // Stores original French text for every node
    private static final Map<Node, String> originalTexts = new HashMap<>();

    public static String getCurrentLang() { return currentLang; }
    public static void setCurrentLang(String lang) { currentLang = lang; }

    public static void translateScene(Parent root, String targetLang) {
        currentLang = targetLang;
        if (targetLang.equals("fr")) {
            restoreOriginals();
        } else {
            walkAndTranslate(root, targetLang);
        }
    }

    // Restore all nodes to their original French text
    private static void restoreOriginals() {
        for (Map.Entry<Node, String> entry : originalTexts.entrySet()) {
            Node node = entry.getKey();
            String original = entry.getValue();
            Platform.runLater(() -> {
                if (node instanceof Label lbl)          lbl.setText(original);
                else if (node instanceof Button btn)    btn.setText(original);
                else if (node instanceof TextField tf)  tf.setPromptText(original);
                else if (node instanceof PasswordField pf) pf.setPromptText(original);
                else if (node instanceof TextArea ta)   ta.setPromptText(original);
                else if (node instanceof Hyperlink hl)  hl.setText(original);
                else if (node instanceof ToggleButton tb) tb.setText(original);
                else if (node instanceof CheckBox cb)   cb.setText(original);
                else if (node instanceof TitledPane tp) tp.setText(original);
            });
        }
    }

    private static void walkAndTranslate(Parent parent, String targetLang) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof ComboBox) continue;
            translateNode(node, targetLang);
            if (node instanceof Parent p) {
                walkAndTranslate(p, targetLang);
            }
        }
    }

    private static void translateNode(Node node, String targetLang) {
        if (node instanceof Label lbl) {
            String t = lbl.getText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t); // save original
                String translated = translate(t, targetLang);
                Platform.runLater(() -> lbl.setText(translated));
            }
        } else if (node instanceof Button btn) {
            String t = btn.getText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> btn.setText(translated));
            }
        } else if (node instanceof TextField tf) {
            String t = tf.getPromptText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> tf.setPromptText(translated));
            }
        } else if (node instanceof PasswordField pf) {
            String t = pf.getPromptText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> pf.setPromptText(translated));
            }
        } else if (node instanceof TextArea ta) {
            String t = ta.getPromptText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> ta.setPromptText(translated));
            }
        } else if (node instanceof Hyperlink hl) {
            String t = hl.getText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> hl.setText(translated));
            }
        } else if (node instanceof ToggleButton tb) {
            String t = tb.getText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> tb.setText(translated));
            }
        } else if (node instanceof CheckBox cb) {
            String t = cb.getText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> cb.setText(translated));
            }
        } else if (node instanceof TitledPane tp) {
            String t = tp.getText();
            if (shouldTranslate(t)) {
                originalTexts.putIfAbsent(node, t);
                String translated = translate(t, targetLang);
                Platform.runLater(() -> tp.setText(translated));
            }
        }
    }

    private static boolean shouldTranslate(String text) {
        if (text == null || text.isBlank()) return false;
        if (text.matches("[0-9%+/\\s]+")) return false;
        String stripped = text.replaceAll(
                "[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}\\s←→↑↓•·▾▸◀▶✓✗✕✚＋×\\-–—()!?*#@]", ""
        ).trim();
        if (stripped.length() <= 1) return false;
        if (text.startsWith("http")) return false;
        return true;
    }

    public static String translate(String text, String targetLang) {
        if (targetLang.equals("fr")) return text;

        cache.computeIfAbsent(targetLang, k -> new HashMap<>());
        if (cache.get(targetLang).containsKey(text)) {
            return cache.get(targetLang).get(text);
        }

        try {
            String cleanText = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", " ").trim();
            if (cleanText.isBlank()) return text;

            String encoded = URLEncoder.encode(cleanText, StandardCharsets.UTF_8);
            String apiUrl = "https://api.mymemory.translated.net/get?q="
                    + encoded + "&langpair=fr|" + targetLang;

            System.out.println("[Translation] Translating: " + cleanText);

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                int start = response.indexOf("\"translatedText\":\"") + 18;
                int end = response.indexOf("\"", start);
                if (start > 18 && end > start) {
                    String translated = response.substring(start, end);
                    System.out.println("[Translation] Result: " + translated);
                    cache.get(targetLang).put(text, translated);
                    return translated;
                }
            }
        } catch (Exception e) {
            System.out.println("[Translation] Error: " + e.getMessage());
        }

        return text;
    }
}