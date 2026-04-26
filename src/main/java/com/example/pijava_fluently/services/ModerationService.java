package com.example.pijava_fluently.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ModerationService {

    private static final String PROVIDER = "purgomalum";
    private static final String ENDPOINT = "https://www.purgomalum.com/service/containsprofanity";

    public ModerationResult moderate(String content) {
        if (content == null || content.isBlank()) {
            return ModerationResult.ok(PROVIDER);
        }

        HttpURLConnection connection = null;
        try {
            String encoded = URLEncoder.encode(content, StandardCharsets.UTF_8);
            URL url = new URL(ENDPOINT + "?text=" + encoded);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "FluentlyApp/1.0");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(7000);

            int responseCode = connection.getResponseCode();
            InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = readResponse(responseStream);

            if (responseCode < 200 || responseCode >= 300) {
                return ModerationResult.unavailable(PROVIDER, "HTTP " + responseCode, body);
            }

            boolean flagged = "true".equalsIgnoreCase(body == null ? "" : body.trim());
            return new ModerationResult(
                    PROVIDER,
                    true,
                    flagged,
                    flagged ? "profanity" : "none",
                    flagged ? 1.0 : 0.0,
                    null,
                    body
            );
        } catch (Exception e) {
            return ModerationResult.unavailable(PROVIDER, e.getMessage(), null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static class ModerationResult {
        private final String provider;
        private final boolean apiAvailable;
        private final boolean flagged;
        private final String topCategory;
        private final double topScore;
        private final String errorMessage;
        private final String rawResponse;

        public ModerationResult(String provider, boolean apiAvailable, boolean flagged, String topCategory,
                                double topScore, String errorMessage, String rawResponse) {
            this.provider = provider;
            this.apiAvailable = apiAvailable;
            this.flagged = flagged;
            this.topCategory = topCategory;
            this.topScore = topScore;
            this.errorMessage = errorMessage;
            this.rawResponse = rawResponse;
        }

        public static ModerationResult ok(String provider) {
            return new ModerationResult(provider, true, false, "none", 0.0, null, null);
        }

        public static ModerationResult unavailable(String provider, String errorMessage, String rawResponse) {
            return new ModerationResult(provider, false, false, "none", 0.0, errorMessage, rawResponse);
        }

        public String getProvider() {
            return provider;
        }

        public boolean isApiAvailable() {
            return apiAvailable;
        }

        public boolean isFlagged() {
            return flagged;
        }

        public String getTopCategory() {
            return topCategory;
        }

        public double getTopScore() {
            return topScore;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getRawResponse() {
            return rawResponse;
        }
    }
}
