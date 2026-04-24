package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.UserService;
import com.example.pijava_fluently.utils.EmailService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;

public class ForgotPasswordController {

    private final UserService userService = new UserService();

    private String generatedCode;
    private long   codeExpiry;      // epoch seconds
    private String pendingEmail;

    // ── Step tracking ─────────────────────────────────────────────────────────
    private enum Step { EMAIL, CODE, NEW_PASSWORD }
    private Step currentStep = Step.EMAIL;

    // ── UI nodes (reused across steps) ───────────────────────────────────────
    private Stage        dialog;
    private VBox         content;
    private Label        titleLabel;
    private Label        subtitleLabel;
    private TextField    inputField;
    private PasswordField passField;
    private PasswordField confirmField;
    private Label        errorLabel;
    private Button       actionBtn;
    private Button       backBtn;

    public void show(Stage owner) {
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        content = buildUI();
        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm());
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.show();
    }

    private VBox buildUI() {
        // ── Card container ────────────────────────────────────────────────────
        VBox card = new VBox(18);
        card.setPadding(new Insets(36, 40, 32, 40));
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 8);
            """);
        card.setPrefWidth(400);
        card.setAlignment(Pos.TOP_LEFT);

        // ── Close button ──────────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("""
            -fx-background-color: transparent; -fx-text-fill: #aaa;
            -fx-font-size: 16px; -fx-cursor: hand; -fx-border-color: transparent;
            """);
        closeBtn.setOnAction(e -> dialog.close());
        topBar.getChildren().add(closeBtn);

        // ── Title ─────────────────────────────────────────────────────────────
        titleLabel = new Label("Mot de passe oublié ?");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #1a1a2e;");

        subtitleLabel = new Label("Entrez votre adresse email pour recevoir un code de vérification.");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(320);
        subtitleLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        // ── Input ─────────────────────────────────────────────────────────────
        inputField = new TextField();
        inputField.setPromptText("exemple@email.com");
        inputField.getStyleClass().add("text-field");
        inputField.setMaxWidth(Double.MAX_VALUE);

        passField = new PasswordField();
        passField.setPromptText("Nouveau mot de passe (min. 8 caractères)");
        passField.getStyleClass().add("text-field");
        passField.setVisible(false); passField.setManaged(false);

        confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer le mot de passe");
        confirmField.getStyleClass().add("text-field");
        confirmField.setVisible(false); confirmField.setManaged(false);

        // ── Error label ───────────────────────────────────────────────────────
        errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(320);
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // ── Buttons ───────────────────────────────────────────────────────────
        actionBtn = new Button("Envoyer le code");
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.getStyleClass().add("btn-primary");
        actionBtn.setOnAction(e -> handleAction());

        backBtn = new Button("← Retour");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setStyle("""
            -fx-background-color: transparent; -fx-text-fill: #888;
            -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: transparent;
            """);
        backBtn.setOnAction(e -> {
            currentStep = Step.EMAIL;
            renderStep();
        });
        backBtn.setVisible(false);

        card.getChildren().addAll(
                topBar, titleLabel, subtitleLabel,
                inputField, passField, confirmField,
                errorLabel, actionBtn, backBtn
        );
        return card;
    }

    private void handleAction() {
        switch (currentStep) {
            case EMAIL       -> handleEmailStep();
            case CODE        -> handleCodeStep();
            case NEW_PASSWORD -> handleNewPasswordStep();
        }
    }

    // ── Step 1: ask for email, send code ─────────────────────────────────────
    private void handleEmailStep() {
        String email = inputField.getText().trim();
        if (email.isEmpty()) { showError("Veuillez entrer votre email."); return; }

        try {
            User user = userService.findByEmail(email);
            if (user == null) { showError("Aucun compte trouvé pour cet email."); return; }

            pendingEmail   = email;
            generatedCode  = generateCode();
            codeExpiry     = Instant.now().getEpochSecond() + 600; // 10 min

            actionBtn.setDisable(true);
            actionBtn.setText("Envoi en cours...");

            new Thread(() -> {
                try {
                    EmailService.sendResetCode(email, generatedCode);
                    Platform.runLater(() -> {
                        currentStep = Step.CODE;
                        renderStep();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        actionBtn.setDisable(false);
                        actionBtn.setText("Envoyer le code");
                        showError("Erreur d'envoi: " + ex.getMessage());
                    });
                }
            }).start();

        } catch (SQLException ex) {
            showError("Erreur base de données: " + ex.getMessage());
        }
    }

    // ── Step 2: verify code ───────────────────────────────────────────────────
    private void handleCodeStep() {
        String entered = inputField.getText().trim();
        if (entered.isEmpty()) { showError("Veuillez entrer le code."); return; }

        if (Instant.now().getEpochSecond() > codeExpiry) {
            showError("Code expiré. Recommencez.");
            currentStep = Step.EMAIL;
            renderStep();
            return;
        }

        if (!entered.equals(generatedCode)) {
            showError("Code incorrect. Réessayez.");
            return;
        }

        currentStep = Step.NEW_PASSWORD;
        renderStep();
    }

    // ── Step 3: set new password ──────────────────────────────────────────────
    private void handleNewPasswordStep() {
        String pass    = passField.getText();
        String confirm = confirmField.getText();

        if (pass.length() < 8) { showError("Minimum 8 caractères."); return; }
        if (!pass.equals(confirm)) { showError("Les mots de passe ne correspondent pas."); return; }

        try {
            User user = userService.findByEmail(pendingEmail);
            user.setPassword(UserService.hashPassword(pass));
            userService.modifier(user);

            // Success — show message then close
            titleLabel.setText("✅ Mot de passe modifié !");
            subtitleLabel.setText("Tu peux maintenant te connecter avec ton nouveau mot de passe.");
            inputField.setVisible(false);  inputField.setManaged(false);
            passField.setVisible(false);   passField.setManaged(false);
            confirmField.setVisible(false); confirmField.setManaged(false);
            errorLabel.setVisible(false);
            backBtn.setVisible(false);
            actionBtn.setText("Fermer");
            actionBtn.setOnAction(e -> dialog.close());

        } catch (SQLException ex) {
            showError("Erreur: " + ex.getMessage());
        }
    }

    // ── Render current step ───────────────────────────────────────────────────
    private void renderStep() {
        clearError();
        actionBtn.setDisable(false);

        switch (currentStep) {
            case EMAIL -> {
                titleLabel.setText("Mot de passe oublié ?");
                subtitleLabel.setText("Entrez votre adresse email pour recevoir un code de vérification.");
                inputField.clear();
                inputField.setPromptText("exemple@email.com");
                inputField.setVisible(true); inputField.setManaged(true);
                passField.setVisible(false); passField.setManaged(false);
                confirmField.setVisible(false); confirmField.setManaged(false);
                actionBtn.setText("Envoyer le code →");
                backBtn.setVisible(false);
            }
            case CODE -> {
                titleLabel.setText("Vérification");
                subtitleLabel.setText("Un code à 6 chiffres a été envoyé à " + pendingEmail + ". Vérifiez votre boîte mail.");
                inputField.clear();
                inputField.setPromptText("Code à 6 chiffres");
                inputField.setVisible(true); inputField.setManaged(true);
                passField.setVisible(false); passField.setManaged(false);
                confirmField.setVisible(false); confirmField.setManaged(false);
                actionBtn.setText("Vérifier le code →");
                backBtn.setVisible(true);
            }
            case NEW_PASSWORD -> {
                titleLabel.setText("Nouveau mot de passe");
                subtitleLabel.setText("Choisissez un nouveau mot de passe sécurisé.");
                inputField.setVisible(false); inputField.setManaged(false);
                passField.clear(); passField.setVisible(true); passField.setManaged(true);
                confirmField.clear(); confirmField.setVisible(true); confirmField.setManaged(true);
                actionBtn.setText("Changer le mot de passe →");
                backBtn.setVisible(false);
            }
        }
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
    }
}