package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.ConfigLoader;
import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Reconnaissance vocale via Whisper (Groq API) - 100% gratuit.
 * Enregistre le micro en WAV, envoie à l'API Groq Whisper, reçoit le texte.
 * Fonctionne en FR, EN, ES, DE, AR et +95 langues.
 * Aucun module JavaFX supplémentaire requis.
 */
public class SpeechRecognitionService {

    private static final String GROQ_WHISPER_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String API_KEY =
            ConfigLoader.get("groq.api.key", "");

    private TargetDataLine     microphone;
    private final AudioFormat  format      =
            new AudioFormat(16000.0f, 16, 1, true, false);
    private boolean            isRecording = false;
    private ByteArrayOutputStream audioBuffer;
    private Thread             recordingThread;
    private Consumer<String>   onResultCallback;
    private String             langueCode;

    public SpeechRecognitionService() {
        this.langueCode = "fr";
    }

    public SpeechRecognitionService(String langueCode) {
        this.langueCode = langueCode != null ? langueCode : "fr";
    }

    // ── Démarrer l'enregistrement ─────────────────────────────────
    public void startRecording(Consumer<String> onResult) {
        if (isRecording) return;
        this.onResultCallback = onResult;

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                Platform.runLater(() ->
                        showError("Microphone non supporté sur ce système."));
                if (onResultCallback != null) onResultCallback.accept("");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            isRecording   = true;
            audioBuffer   = new ByteArrayOutputStream();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioBuffer.write(buffer, 0, bytesRead);
                    }
                }
            });
            recordingThread.setDaemon(true);
            recordingThread.start();

            LoggerUtil.info("Recording started", "langue", langueCode);

        } catch (LineUnavailableException e) {
            LoggerUtil.error("Microphone unavailable", e);
            Platform.runLater(() ->
                    showError("Microphone non disponible : " + e.getMessage()));
            if (onResultCallback != null) onResultCallback.accept("");
        }
    }

    public void startRecording() {
        startRecording(null);
    }

    // ── Arrêter et transcrire ─────────────────────────────────────
    public CompletableFuture<String> stopRecordingAndRecognize() {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (!isRecording || microphone == null) {
            LoggerUtil.warning("Not recording, nothing to transcribe");
            future.complete("");
            return future;
        }

        isRecording = false;

        new Thread(() -> {
            try {
                if (recordingThread != null) recordingThread.join(2000);
                microphone.stop();
                microphone.close();

                byte[] audioData = audioBuffer.toByteArray();
                LoggerUtil.info("Audio captured",
                        "bytes", String.valueOf(audioData.length));

                if (audioData.length < 8000) {
                    LoggerUtil.warning("Audio too short");
                    future.complete("");
                    if (onResultCallback != null)
                        Platform.runLater(() -> onResultCallback.accept(""));
                    return;
                }

                String result = transcribeWithWhisper(audioData);
                LoggerUtil.info("Whisper result", "text", result);

                future.complete(result);
                if (onResultCallback != null)
                    Platform.runLater(() -> onResultCallback.accept(result));

            } catch (Exception e) {
                LoggerUtil.error("Error during transcription", e);
                future.complete("");
                if (onResultCallback != null)
                    Platform.runLater(() -> onResultCallback.accept(""));
            }
        }).start();

        return future;
    }

    // ── Envoi à Groq Whisper ──────────────────────────────────────
    private String transcribeWithWhisper(byte[] audioData) throws IOException {
        // Convertir les données PCM brutes en fichier WAV valide
        byte[] wavData = pcmToWav(audioData, 16000, 1, 16);

        // Boundary pour multipart/form-data
        String boundary = "fluently_boundary_" + System.currentTimeMillis();

        URL url = new URL(GROQ_WHISPER_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

            // -- Fichier audio
            writer.println("--" + boundary);
            writer.println("Content-Disposition: form-data; " +
                    "name=\"file\"; filename=\"audio.wav\"");
            writer.println("Content-Type: audio/wav");
            writer.println();
            writer.flush();
            os.write(wavData);
            os.flush();
            writer.println();

            // -- Modèle Whisper
            writer.println("--" + boundary);
            writer.println("Content-Disposition: form-data; name=\"model\"");
            writer.println();
            writer.println("whisper-large-v3-turbo");

            // -- Langue (optionnel mais améliore la précision)
            writer.println("--" + boundary);
            writer.println("Content-Disposition: form-data; name=\"language\"");
            writer.println();
            writer.println(langueCode);

            // -- Format de réponse
            writer.println("--" + boundary);
            writer.println("Content-Disposition: form-data; name=\"response_format\"");
            writer.println();
            writer.println("json");

            // -- Fermeture
            writer.println("--" + boundary + "--");
            writer.flush();
        }

        int status = conn.getResponseCode();
        LoggerUtil.info("Whisper API status", "code", String.valueOf(status));

        if (status != 200) {
            // Lire l'erreur
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder err = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) err.append(line);
                LoggerUtil.error("Whisper API error", "body", err.toString());
            }
            return "";
        }

        // Lire la réponse JSON : {"text": "le texte transcrit"}
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(),
                        StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);

            String body = response.toString();
            LoggerUtil.info("Whisper response", "body", body);

            return extractTextField(body);
        }
    }

    // ── Conversion PCM brut → WAV ─────────────────────────────────
    private byte[] pcmToWav(byte[] pcmData, int sampleRate,
                            int channels, int bitsPerSample) throws IOException {
        ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(wavOut);

        int byteRate    = sampleRate * channels * bitsPerSample / 8;
        int blockAlign  = channels * bitsPerSample / 8;
        int dataSize    = pcmData.length;
        int chunkSize   = 36 + dataSize;

        // RIFF header
        dos.writeBytes("RIFF");
        writeIntLE(dos, chunkSize);
        dos.writeBytes("WAVE");

        // fmt chunk
        dos.writeBytes("fmt ");
        writeIntLE(dos, 16);                  // chunk size
        writeShortLE(dos, (short) 1);         // PCM format
        writeShortLE(dos, (short) channels);
        writeIntLE(dos, sampleRate);
        writeIntLE(dos, byteRate);
        writeShortLE(dos, (short) blockAlign);
        writeShortLE(dos, (short) bitsPerSample);

        // data chunk
        dos.writeBytes("data");
        writeIntLE(dos, dataSize);
        dos.write(pcmData);
        dos.flush();

        return wavOut.toByteArray();
    }

    private void writeIntLE(DataOutputStream dos, int v) throws IOException {
        dos.write(v & 0xFF);
        dos.write((v >> 8) & 0xFF);
        dos.write((v >> 16) & 0xFF);
        dos.write((v >> 24) & 0xFF);
    }

    private void writeShortLE(DataOutputStream dos, short v) throws IOException {
        dos.write(v & 0xFF);
        dos.write((v >> 8) & 0xFF);
    }

    // ── Extraire "text" du JSON sans dépendance ───────────────────
    private String extractTextField(String json) {
        if (json == null || json.isEmpty()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<?, ?> map = mapper.readValue(json, java.util.Map.class);
            Object text = map.get("text");
            return text != null ? text.toString().trim() : "";
        } catch (Exception e) {
            // Fallback regex
            int idx = json.indexOf("\"text\"");
            if (idx == -1) return "";
            int q1 = json.indexOf('"', idx + 7);
            int q2 = json.indexOf('"', q1 + 1);
            if (q1 == -1 || q2 == -1) return "";
            return json.substring(q1 + 1, q2).trim();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur Microphone");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void close() {
        isRecording = false;
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
        }
    }
}