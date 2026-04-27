package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class FaceLoginController {

    @FXML private Label statusLabel;
    @FXML private Button scanBtn;

    private final UserService userService = new UserService();

    @FXML
    private void handleScan() {
        setStatus("📷 Ouverture de la caméra... Appuyez sur ESPACE pour scanner.", "info");
        scanBtn.setDisable(true);

        new Thread(() -> {
            try {
                String scriptPath = "C:/Users/emnab/Documents/PI_Java/PIJava_Fluently/face_capture.py";

                ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
                // DO NOT use pb.inheritIO() — it sends output to terminal, not to Java
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                // Only grab the line that contains valid JSON (starts with '{')
                String jsonLine = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Python]: " + line); // debug
                    String trimmed = line.trim();
                    if (trimmed.startsWith("{")) {
                        jsonLine = trimmed;
                    }
                }

                process.waitFor();
                String output = jsonLine != null ? jsonLine : "";
                System.out.println("[Java] JSON output: " + output);

                if (output.isEmpty() || !output.contains("descriptor")) {
                    Platform.runLater(() -> {
                        setStatus("❌ Aucun visage détecté. Réessayez.", "error");
                        scanBtn.setDisable(false);
                    });
                    return;
                }

                double[] captured = parseDescriptor(output);
                if (captured == null) {
                    Platform.runLater(() -> {
                        setStatus("❌ Erreur lecture descripteur. Réessayez.", "error");
                        scanBtn.setDisable(false);
                    });
                    return;
                }

                // Compare with all users in DB
                List<User> users = userService.recuperer();
                User matched = null;
                double bestDistance = Double.MAX_VALUE;

                for (User u : users) {
                    if (u.getFaceDescriptor() == null || u.getFaceDescriptor().isEmpty()) continue;
                    double[] stored = parseDescriptor(u.getFaceDescriptor());
                    if (stored == null) continue;
                    double distance = euclideanDistance(captured, stored);
                    System.out.println("[Java] Distance with " + u.getEmail() + ": " + distance);
                    if (distance < 0.6 && distance < bestDistance) {
                        bestDistance = distance;
                        matched = u;
                    }
                }

                final User finalMatch = matched;
                final double finalDist = bestDistance;
                Platform.runLater(() -> {
                    if (finalMatch != null) {
                        setStatus("✅ Bienvenue " + finalMatch.getPrenom() + "!", "success");
                        loginUser(finalMatch);
                    } else {
                        setStatus("❌ Visage non reconnu (dist: " + String.format("%.3f", finalDist) + "). Réessayez.", "error");
                        scanBtn.setDisable(false);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    setStatus("❌ Erreur: " + e.getMessage(), "error");
                    scanBtn.setDisable(false);
                });
            }
        }).start();
    }

    private double[] parseDescriptor(String json) {
        try {
            String clean = json.trim();
            if (clean.startsWith("{")) {
                int start = clean.indexOf('[');
                int end = clean.lastIndexOf(']');
                if (start < 0 || end < 0) return null;
                clean = clean.substring(start + 1, end);
            } else if (clean.startsWith("[")) {
                clean = clean.substring(1, clean.length() - 1);
            }
            String[] parts = clean.split(",");
            double[] result = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Double.parseDouble(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            System.out.println("[Java] Parse error: " + e.getMessage());
            return null;
        }
    }

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private void loginUser(User user) {
        try {
            userService.updateStatut(user.getId(), "online");
            String fxml = user.isAdmin()
                    ? "/com/example/pijava_fluently/fxml/admin-dashboard.fxml"
                    : "/com/example/pijava_fluently/fxml/home.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            if (user.isAdmin()) {
                AdminDashboardController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
            } else {
                HomeController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
            }

            Stage stage = (Stage) scanBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css")
                            .toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setStatus(String msg, String type) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) scanBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css")
                            .toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}