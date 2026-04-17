package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SpeechRecognitionService {

    private TargetDataLine microphone;
    private final AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
    private boolean isRecording = false;
    private ByteArrayOutputStream audioBuffer;
    private Thread recordingThread;
    private Consumer<String> onResultCallback;
    private String langue = "fr";

    public SpeechRecognitionService() {}

    public SpeechRecognitionService(String langue) {
        this.langue = langue;
    }

    public void startRecording(Consumer<String> onResult) {
        if (isRecording) return;
        this.onResultCallback = onResult;

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                Platform.runLater(() -> showError("Microphone non supporté sur ce système."));
                if (onResultCallback != null) onResultCallback.accept("");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            isRecording = true;
            audioBuffer = new ByteArrayOutputStream();

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

            LoggerUtil.info("Recording started");

        } catch (LineUnavailableException e) {
            LoggerUtil.error("Microphone unavailable", e);
            Platform.runLater(() -> showError("Microphone non disponible : " + e.getMessage()));
            if (onResultCallback != null) onResultCallback.accept("");
        }
    }

    public void startRecording() {
        startRecording(null);
    }

    public CompletableFuture<String> stopRecordingAndRecognize() {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (!isRecording || microphone == null) {
            LoggerUtil.warning("stopRecordingAndRecognize called but not recording");
            future.complete("");
            return future;
        }

        isRecording = false;

        new Thread(() -> {
            try {
                if (recordingThread != null) {
                    recordingThread.join(2000);
                }

                microphone.stop();
                microphone.close();

                byte[] audioData = audioBuffer.toByteArray();
                LoggerUtil.info("Audio captured", "bytes", String.valueOf(audioData.length));

                if (audioData.length < 2000) {
                    LoggerUtil.warning("Audio too short, skipping recognition");
                    future.complete("");
                    if (onResultCallback != null)
                        Platform.runLater(() -> onResultCallback.accept(""));
                    return;
                }

                String recognizedText = recognizeWithVosk(audioData);
                LoggerUtil.info("Recognized text", "result", recognizedText);

                future.complete(recognizedText);
                if (onResultCallback != null) {
                    Platform.runLater(() -> onResultCallback.accept(recognizedText));
                }

            } catch (Exception e) {
                LoggerUtil.error("Error during recognition", e);
                future.complete("");
                if (onResultCallback != null)
                    Platform.runLater(() -> onResultCallback.accept(""));
            }
        }).start();

        return future;
    }

    private String recognizeWithVosk(byte[] audioData) {
        // Choisir le bon dossier modèle selon la langue
        // IMPORTANT : dans ton projet le dossier s'appelle "model" (pas "models")
        String modelFolder = langue.equals("fr")
                ? "model/vosk-model-small-fr-0.22"
                : "model/vosk-model-small-en-us-0.15";

        LoggerUtil.info("Looking for Vosk model", "folder", modelFolder);

        try {
            // Méthode 1 : via ClassLoader (fonctionne avec Maven resources)
            URL modelUrl = getClass().getClassLoader().getResource(modelFolder);

            if (modelUrl == null) {
                LoggerUtil.error("Vosk model not found via ClassLoader: " + modelFolder);
                // Méthode 2 : chemin absolu relatif au projet
                File modelFile = new File("src/main/resources/" + modelFolder);
                if (!modelFile.exists()) {
                    LoggerUtil.error("Vosk model not found at: " + modelFile.getAbsolutePath());
                    return "";
                }
                return runVosk(modelFile.getAbsolutePath(), audioData);
            }

            File modelFile = new File(modelUrl.toURI());
            LoggerUtil.info("Vosk model found", "path", modelFile.getAbsolutePath());
            return runVosk(modelFile.getAbsolutePath(), audioData);

        } catch (Exception e) {
            LoggerUtil.error("Vosk recognition failed", e);
            return "";
        }
    }

    private String runVosk(String modelPath, byte[] audioData) {
        try (Model model = new Model(modelPath);
             Recognizer recognizer = new Recognizer(model, 16000)) {

            // Traiter l'audio par chunks
            int chunkSize = 4096;
            for (int i = 0; i < audioData.length; i += chunkSize) {
                int end = Math.min(i + chunkSize, audioData.length);
                byte[] chunk = new byte[end - i];
                System.arraycopy(audioData, i, chunk, 0, chunk.length);
                recognizer.acceptWaveForm(chunk, chunk.length);
            }

            String finalResult = recognizer.getFinalResult();
            LoggerUtil.info("Vosk raw result", "json", finalResult);
            return parseVoskResult(finalResult);

        } catch (Exception e) {
            LoggerUtil.error("Error running Vosk", e);
            return "";
        }
    }

    private String parseVoskResult(String jsonResult) {
        if (jsonResult == null || jsonResult.isEmpty()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<?, ?> map = mapper.readValue(jsonResult, java.util.Map.class);
            Object text = map.get("text");
            return text != null ? text.toString().trim() : "";
        } catch (Exception e) {
            // Fallback manuel
            int start = jsonResult.indexOf("\"text\"");
            if (start == -1) return "";
            int q1 = jsonResult.indexOf('"', start + 7);
            int q2 = jsonResult.indexOf('"', q1 + 1);
            if (q1 == -1 || q2 == -1) return "";
            return jsonResult.substring(q1 + 1, q2).trim();
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