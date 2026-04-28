package com.example.pijava_fluently.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.example.pijava_fluently.utils.ConfigLoader;

/**
 * Generates an SVG avatar via the Claude (Anthropic) API
 * based on the language the user chose to study.
 *
 * Credential style matches the rest of the project
 * (GoogleAuthController, EmailService → hardcoded constants).
 *
 * Replace YOUR_API_KEY_HERE with your real sk-ant-... key.
 */
public class AvatarService {

    // ── CREDENTIALS ───────────────────────────────────────────────────────────
    private static final String API_KEY = ConfigLoader.get("anthropic.api.key");// sk-ant-api03-…
    private static final String MODEL             = "claude-sonnet-4-20250514";
    private static final String ENDPOINT          = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    /**
     * Calls Claude and returns a compact, self-contained SVG avatar
     * themed around the chosen language / culture.
     *
     * @param languageName  e.g. "French", "Japanese", "Arabic" ...
     * @return SVG markup starting with <svg ...>
     */
    public static String generateAvatar(String languageName) throws IOException {
        String body = buildRequestBody(languageName);

        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",      "application/json");
        conn.setRequestProperty("x-api-key",         API_KEY);
        conn.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();

        if (status != 200) {
            throw new IOException("Claude API error " + status + ": " + response);
        }

        return extractSvg(response);
    }

    // ── REQUEST BUILDER ───────────────────────────────────────────────────────

    private static String buildRequestBody(String language) {
        String prompt = "You are an SVG avatar generator for a language-learning app called Fluently.\n"
                + "Create a circular human face avatar (viewBox=\"0 0 100 100\") for a student learning " + language + ".\n"
                + "The avatar must look like a cartoon human face/bust (head, eyes, nose, mouth, neck, shoulders).\n"
                + "The face skin colour AND hair colour must be inspired by the FLAG colours of the country associated with " + language + ".\n"
                + "For example: Korean flag is white+red+blue → use those as skin/hair/clothing colours.\n"
                + "Rules:\n"
                + "- Output ONLY the raw SVG — no markdown, no explanation, no code fence.\n"
                + "- Start with <svg viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\"> and end with </svg>.\n"
                + "- Keep it under 1200 characters.\n"
                + "- Draw: circular background, shoulders/bust, neck, round head, two eyes, nose, smiling mouth, and hair.\n"
                + "- Use flag-inspired colours for: background circle, skin, hair, and clothing.\n"
                + "- Do NOT include any text or letters.\n"
                + "- Must look great at both 36x36 px (table row) and 96x96 px (profile card).";

        return "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"max_tokens\":1500,"
                + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}]"
                + "}";
    }

    // ── RESPONSE PARSER ───────────────────────────────────────────────────────

    private static String extractSvg(String json) {
        int textIdx = json.indexOf("\"text\":\"");
        if (textIdx < 0) {
            throw new RuntimeException("No 'text' field in Claude response: " + json);
        }

        StringBuilder sb = new StringBuilder();
        int i = textIdx + 8;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(next);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            i++;
        }

        String text = sb.toString().trim();
        int svgStart = text.indexOf("<svg");
        int svgEnd   = text.lastIndexOf("</svg>");
        if (svgStart < 0 || svgEnd < 0) {
            throw new RuntimeException("Claude did not return valid SVG. Got: " + text);
        }
        return text.substring(svgStart, svgEnd + 6).trim();
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    private static String jsonString(String s) {
        return "\""
                + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    // ── FALLBACK ─────────────────────────────────────────────────────────────

    /** Simple coloured-circle fallback when the API call fails. */
    public static String fallbackAvatarSvg(String language) {
        String bg, fg;
        switch (language) {
            case "French":     bg = "#002395"; fg = "#EF4135"; break;
            case "Spanish":    bg = "#c60b1e"; fg = "#ffc400"; break;
            case "Arabic":     bg = "#007A3D"; fg = "#FFFFFF"; break;
            case "Japanese":   bg = "#BC002D"; fg = "#FFFFFF"; break;
            case "German":     bg = "#000000"; fg = "#DD0000"; break;
            case "Italian":    bg = "#009246"; fg = "#CE2B37"; break;
            case "Chinese":    bg = "#DE2910"; fg = "#FFDE00"; break;
            case "Portuguese": bg = "#006600"; fg = "#FFD700"; break;
            case "Korean":     bg = "#FFFFFF"; fg = "#CD2E3A"; break;
            case "English":    bg = "#012169"; fg = "#C8102E"; break;
            default:           bg = "#6366f1"; fg = "#FFFFFF"; break;
        }
        String initial = language.substring(0, 1).toUpperCase();
        return "<svg viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">"
                + "<circle cx=\"50\" cy=\"50\" r=\"50\" fill=\"" + bg + "\"/>"
                + "<text x=\"50\" y=\"65\" font-size=\"42\" text-anchor=\"middle\" fill=\"" + fg
                + "\" font-family=\"Arial,sans-serif\" font-weight=\"bold\">" + initial + "</text>"
                + "</svg>";
    }
}