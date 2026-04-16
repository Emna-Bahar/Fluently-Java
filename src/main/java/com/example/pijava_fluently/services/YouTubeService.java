package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.ConfigLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YouTubeService {

    // Récupérer la clé API depuis config.properties
    private static final String API_KEY = ConfigLoader.getYouTubeApiKey();
    private static final String API_URL = "https://www.googleapis.com/youtube/v3/search";

    private final OkHttpClient client = new OkHttpClient();

    // Vérifier si la clé API est disponible
    static {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("⚠️ ATTENTION: Clé API YouTube non configurée dans config.properties !");
        }
    }

    public List<VideoInfo> rechercherVideos(String query, int maxResults) {
        List<VideoInfo> videos = new ArrayList<>();

        // Vérifier si la clé API est configurée
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("❌ Recherche YouTube impossible : clé API manquante");
            return videos;
        }

        try {
            String url = API_URL + "?part=snippet&maxResults=" + maxResults + "&q=" +
                    java.net.URLEncoder.encode(query, "UTF-8") + "&type=video&key=" + API_KEY;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return videos;
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray items = jsonResponse.getAsJsonArray("items");

                if (items != null) {
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        JsonObject snippet = item.getAsJsonObject("snippet");
                        JsonObject id = item.getAsJsonObject("id");

                        String videoId = id.get("videoId").getAsString();
                        String title = snippet.get("title").getAsString();
                        String description = snippet.get("description").getAsString();
                        String thumbnail = snippet.getAsJsonObject("thumbnails")
                                .getAsJsonObject("default").get("url").getAsString();

                        videos.add(new VideoInfo(videoId, title, description, thumbnail));
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return videos;
    }

    public static class VideoInfo {
        private final String videoId;
        private final String title;
        private final String description;
        private final String thumbnail;

        public VideoInfo(String videoId, String title, String description, String thumbnail) {
            this.videoId = videoId;
            this.title = title;
            this.description = description;
            this.thumbnail = thumbnail;
        }

        public String getVideoId() { return videoId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getThumbnail() { return thumbnail; }
        public String getVideoUrl() { return "https://www.youtube.com/watch?v=" + videoId; }
    }
}