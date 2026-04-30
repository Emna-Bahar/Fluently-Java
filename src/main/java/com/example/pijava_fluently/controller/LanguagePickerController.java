package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.AvatarService;
import com.example.pijava_fluently.services.UserService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Shown immediately after registration.
 * The user picks the language they want to study;
 * Claude AI generates a themed SVG avatar which is saved to DB,
 * then the user is sent to home.fxml.
 */
public class LanguagePickerController {

    @FXML private Label           welcomeLabel;
    @FXML private FlowPane        languagePane;   // language choice buttons
    @FXML private VBox            generatingBox;  // spinner area (hidden by default)
    @FXML private Label           statusLabel;
    @FXML private ProgressIndicator spinner;

    /** Languages offered at sign-up. Add/remove freely. */
    private static final String[] LANGUAGES = {
            "French", "Spanish", "Arabic", "Japanese", "German",
            "Italian", "Chinese", "Portuguese", "Korean", "English"
    };

    private static final java.util.Map<String, String> FLAGS = new java.util.HashMap<>();
    static {
        FLAGS.put("French",     "🇫🇷");  FLAGS.put("Spanish",    "🇪🇸");
        FLAGS.put("Arabic",     "🇸🇦");  FLAGS.put("Japanese",   "🇯🇵");
        FLAGS.put("German",     "🇩🇪");  FLAGS.put("Italian",    "🇮🇹");
        FLAGS.put("Chinese",    "🇨🇳");  FLAGS.put("Portuguese", "🇧🇷");
        FLAGS.put("Korean",     "🇰🇷");  FLAGS.put("English",    "🇬🇧");
    }

    private User currentUser;
    private final UserService userService = new UserService();

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    public void setCurrentUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getPrenom() + "! 🎉\nQuelle langue veux-tu apprendre ?");
        buildLanguageButtons();
    }

    // ── INIT ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        generatingBox.setVisible(false);
        generatingBox.setManaged(false);
    }

    // ── LANGUAGE BUTTONS ──────────────────────────────────────────────────────

    private void buildLanguageButtons() {
        languagePane.getChildren().clear();
        for (String lang : LANGUAGES) {
            Button btn = new Button(FLAGS.getOrDefault(lang, "") + "  " + lang);
            btn.getStyleClass().add("lang-choice-btn");
            btn.setOnAction(e -> onLanguageChosen(lang));
            languagePane.getChildren().add(btn);
        }
    }

    // ── HANDLE CHOICE ─────────────────────────────────────────────────────────

    private void onLanguageChosen(String language) {
        languagePane.setDisable(true);

        generatingBox.setVisible(true);
        generatingBox.setManaged(true);
        statusLabel.setText("Création de ton avatar pour " + language + "…");

        // Persist chosen language immediately
        saveChosenLanguage(language);

        // Call Claude API in background thread
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return AvatarService.generateAvatar(language);
            }
        };

        task.setOnSucceeded(ev -> {
            String svg = task.getValue();
            saveAvatarToDb(svg);
            currentUser.setAvatarSvg(svg);
            currentUser.setChosenLanguage(language);
            statusLabel.setText("Avatar prêt ! Chargement…");
            pauseThenNavigate();
        });

        task.setOnFailed(ev -> {
            // API failed → use coloured-circle fallback (no crash)
            String fallback = AvatarService.fallbackAvatarSvg(language);
            saveAvatarToDb(fallback);
            currentUser.setAvatarSvg(fallback);
            currentUser.setChosenLanguage(language);
            statusLabel.setText("Avatar par défaut. Chargement…");
            pauseThenNavigate();
        });

        new Thread(task).start();
    }

    // ── DB HELPERS ────────────────────────────────────────────────────────────

    private void saveChosenLanguage(String language) {
        try {
            userService.updateChosenLanguage(currentUser.getId(), language);
        } catch (SQLException e) {
            System.err.println("Could not save chosen language: " + e.getMessage());
        }
    }

    private void saveAvatarToDb(String svg) {
        try {
            userService.updateAvatar(currentUser.getId(), svg);
        } catch (SQLException e) {
            System.err.println("Could not save avatar: " + e.getMessage());
        }
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private void pauseThenNavigate() {
        new Thread(() -> {
            try { Thread.sleep(900); } catch (InterruptedException ignored) {}
            Platform.runLater(this::navigateToHome);
        }).start();
    }

    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/home.fxml")
            );
            Parent root = loader.load();
            HomeController homeCtrl = loader.getController();
            homeCtrl.setCurrentUser(currentUser);

            Stage stage = (Stage) languagePane.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            var groupsCss = getClass().getResource("/com/example/pijava_fluently/css/groups-enhanced.css");
            if (groupsCss != null) scene.getStylesheets().add(groupsCss.toExternalForm());
            stage.setTitle("Fluently – Mon Espace");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}