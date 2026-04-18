package com.example.pijava_fluently.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SpellCheckerService {

    private static final String API_URL = "https://api.languagetool.org/v2/check";

    private final OkHttpClient client;

    private static final Map<String, String> LANGUE_CODES = new HashMap<>();
    static {
        LANGUE_CODES.put("Français", "fr");
        LANGUE_CODES.put("Anglais", "en-US");
        LANGUE_CODES.put("Espagnol", "es");
        LANGUE_CODES.put("Allemand", "de");
        LANGUE_CODES.put("Italien", "it");
        LANGUE_CODES.put("Portugais", "pt");
        LANGUE_CODES.put("Néerlandais", "nl");
    }

    public SpellCheckerService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public List<SpellingError> checkText(String text, String langue) {
        List<SpellingError> errors = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return errors;
        }

        String langueCode = LANGUE_CODES.getOrDefault(langue, "fr");

        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("text", text)
                    .add("language", langueCode)
                    .add("enabledOnly", "false")
                    .build();

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(formBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
                    JsonArray matches = root.getAsJsonArray("matches");

                    if (matches != null) {
                        for (int i = 0; i < matches.size(); i++) {
                            JsonObject match = matches.get(i).getAsJsonObject();
                            SpellingError error = new SpellingError();
                            error.setOffset(match.get("offset").getAsInt());
                            error.setLength(match.get("length").getAsInt());
                            error.setMessage(match.get("message").getAsString());

                            JsonArray replacements = match.getAsJsonArray("replacements");
                            if (replacements != null && replacements.size() > 0) {
                                List<String> suggestions = new ArrayList<>();
                                for (int j = 0; j < replacements.size() && j < 5; j++) {
                                    suggestions.add(replacements.get(j).getAsJsonObject().get("value").getAsString());
                                }
                                error.setSuggestions(suggestions);
                            }
                            errors.add(error);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur vérification: " + e.getMessage());
        }

        return errors;
    }

    public static class SpellingError {
        private int offset;
        private int length;
        private String message;
        private List<String> suggestions;

        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }

        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }
}