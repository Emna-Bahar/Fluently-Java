package com.example.pijava_fluently.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClientUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> postJson(String urlString, Map<String, Object> requestBody,
                                               Map<String, String> headers) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Ajouter les headers personnalisés
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        // Écrire le body JSON
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        // Lire la réponse
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode != 200) {
            throw new IOException("API returned status " + responseCode + ": " + response.toString());
        }

        return objectMapper.readValue(response.toString(), Map.class);
    }

    public static class IOException extends Exception {
        public IOException(String message) {
            super(message);
        }
    }
}