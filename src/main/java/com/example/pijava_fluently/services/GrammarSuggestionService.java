package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.AppConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrammarSuggestionService {

    private static final String API_KEY = AppConfig.get("groq.api.key");
    private static final String API_URL  = AppConfig.get("groq.api.url");
    private static final String MODEL    = AppConfig.get("groq.model");
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public String suggest(String text, String targetLanguage) throws IOException {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IOException("groq.api.key not configured");
        }

        String systemPrompt = buildSystemPrompt(targetLanguage);
        String requestBody  = buildRequestBody(systemPrompt, text);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Groq API error " + response.code() + ": " + body);
            }
            return extractSuggestion(body);
        }
    }

    private String buildSystemPrompt(String targetLanguage) {
        if (targetLanguage != null && !targetLanguage.isBlank()) {
            return "You are a grammar correction assistant. The user will send a message written in "
                    + targetLanguage + ". Reply with ONLY the grammatically corrected version in "
                    + targetLanguage + ". No explanation, no quotes, no extra text.";
        }
        return "You are a grammar correction assistant. Reply with ONLY the grammatically corrected "
                + "version of the user's message in the same language. No explanation, no quotes, no extra text.";
    }

    private String buildRequestBody(String systemPrompt, String userText) {
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userText);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("max_tokens", 1024);
        body.addProperty("temperature", 0.1);

        return body.toString();
    }

    private String extractSuggestion(String responseBody) throws IOException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString()
                    .trim();
        } catch (Exception e) {
            throw new IOException("Failed to parse Groq response: " + e.getMessage());
        }
    }
}
