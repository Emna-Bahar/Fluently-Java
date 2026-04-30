package com.example.pijava_fluently.services;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.example.pijava_fluently.utils.ConfigLoader;

/**
 * Generates an SVG avatar via the Groq API (free)
 * based on the language the user chose to study.
 */
public class AvatarService {

    private static final String API_KEY  = ConfigLoader.get("groq.api.key");
    private static final String MODEL    = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    public static String generateAvatar(String languageName) throws IOException {
        System.out.println("🎨 AvatarService: generating for " + languageName);

        String body = buildRequestBody(languageName);

        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        System.out.println("📡 Groq API status: " + status);

        InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();

        if (status != 200) {
            throw new IOException("Groq API error " + status + ": " + response);
        }

        // Use org.json to properly parse the response (handles unicode escapes automatically)
        JSONObject json = new JSONObject(response);
        JSONArray choices = json.getJSONArray("choices");
        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        System.out.println("📝 Content (first 200): " + content.substring(0, Math.min(200, content.length())));

        // Extract SVG block
        int svgStart = content.indexOf("<svg");
        int svgEnd   = content.lastIndexOf("</svg>");

        if (svgStart < 0 || svgEnd < 0) {
            throw new RuntimeException("No valid SVG in response. Got: " + content.substring(0, Math.min(300, content.length())));
        }

        String svg = content.substring(svgStart, svgEnd + 6).trim();
        System.out.println("✅ SVG generated, length: " + svg.length());
        return svg;
    }

    private static String buildRequestBody(String language) {
        String prompt = "You are a professional SVG avatar artist for a language-learning app called Fluently.\n"
                + "Create a circular cartoon avatar (viewBox=\"0 0 100 100\") for a student learning " + language + ".\n"
                + "\n"
                + "CONCEPT: The avatar is a cartoon human face where the FACE SKIN is painted\n"
                + "with the FLAG PATTERN of the country associated with " + language + ".\n"
                + "Like face paint at a sports event — the flag design is painted ON the face.\n"
                + "\n"
                + "For example:\n"
                + "- French flag (blue/white/red vertical stripes) → paint those 3 vertical stripes on the face\n"
                + "- Japanese flag (white + red circle) → white face with red circle painted in center\n"
                + "- German flag (black/red/gold horizontal stripes) → 3 horizontal stripes on the face\n"
                + "- Korean flag (white + red/blue yin-yang) → white face with red and blue yin-yang painted on it\n"
                + "\n"
                + "Draw these elements:\n"
                + "1. Circular background in neutral dark colour\n"
                + "2. Round head shape\n"
                + "3. FLAG PATTERN painted on the face as skin (use clipPath to clip flag to face shape)\n"
                + "4. Eyes on top of the flag face: white sclera + dark iris + black pupil\n"
                + "5. Simple nose\n"
                + "6. Smiling mouth with white teeth\n"
                + "7. Hair in a solid colour above the head\n"
                + "8. Shoulders/clothing at bottom\n"
                + "\n"
                + "Technical rules:\n"
                + "- Output ONLY the raw SVG — no markdown, no explanation, no code fence.\n"
                + "- Start with <svg viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\"> and end with </svg>.\n"
                + "- Use <clipPath> to clip the flag pattern inside the face circle.\n"
                + "- Keep it under 2000 characters.\n"
                + "- Do NOT include any text or letters.\n"
                + "- Must look great at both 36x36 px and 96x96 px.";

        return "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"max_tokens\":1500,"
                + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}]"
                + "}";
    }

    private static String jsonString(String s) {
        return "\""
                + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

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