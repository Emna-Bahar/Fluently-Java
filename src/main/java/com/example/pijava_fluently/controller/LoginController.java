package com.example.pijava_fluently.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    // --- Login form fields ---
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    // --- Register form fields ---
    @FXML private TextField regPrenom;
    @FXML private TextField regNom;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label registerError;

    // --- Form containers ---
    @FXML private javafx.scene.layout.VBox loginForm;
    @FXML private javafx.scene.layout.VBox registerForm;

    // --- Tab buttons ---
    @FXML private Button loginTabBtn;
    @FXML private Button registerTabBtn;

    // ========================
    // TAB SWITCHING
    // ========================

    @FXML
    private void switchToLogin() {
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);

        loginTabBtn.getStyleClass().add("tab-active");
        registerTabBtn.getStyleClass().remove("tab-active");
    }

    @FXML
    private void switchToRegister() {
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        loginForm.setVisible(false);
        loginForm.setManaged(false);

        registerTabBtn.getStyleClass().add("tab-active");
        loginTabBtn.getStyleClass().remove("tab-active");
    }

    // ========================
    // LOGIN
    // ========================

    @FXML
    private void handleLogin() {
        String email = loginEmail.getText().trim();
        String password = loginPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showLoginError("Veuillez remplir tous les champs.");
            return;
        }

        // TODO: Replace with real DB authentication via MyDatabase
        // Example check (hardcoded for demo):
        if (email.equals("admin@fluently.com") && password.equals("admin")) {
            navigateTo("admin-dashboard.fxml", "Fluently - Administration");
        } else if (email.contains("@") && password.length() >= 4) {
            navigateTo("home.fxml", "Fluently - Mon Espace");
        } else {
            showLoginError("Email ou mot de passe incorrect.");
        }
    }

    @FXML
    private void forgotPassword() {
        // TODO: Open password reset dialog
        System.out.println("Mot de passe oublié cliqué");
    }

    // ========================
    // REGISTER
    // ========================

    @FXML
    private void handleRegister() {
        String prenom = regPrenom.getText().trim();
        String nom = regNom.getText().trim();
        String email = regEmail.getText().trim();
        String password = regPassword.getText();
        String confirm = regConfirmPassword.getText();

        // Validation
        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showRegisterError("Veuillez remplir tous les champs.");
            return;
        }
        if (!email.contains("@")) {
            showRegisterError("Adresse email invalide.");
            return;
        }
        if (password.length() < 8) {
            showRegisterError("Le mot de passe doit contenir au moins 8 caractères.");
            return;
        }
        if (!password.equals(confirm)) {
            showRegisterError("Les mots de passe ne correspondent pas.");
            return;
        }

        // TODO: Insert user into DB via MyDatabase
        System.out.println("Nouveau compte : " + prenom + " " + nom + " - " + email);

        // After registration, go to home
        navigateTo("home.fxml", "Fluently - Mon Espace");
    }

    // ========================
    // HELPERS
    // ========================

    private void showLoginError(String msg) {
        loginError.setText(msg);
        loginError.setVisible(true);
        loginError.setManaged(true);
    }

    private void showRegisterError(String msg) {
        registerError.setText(msg);
        registerError.setVisible(true);
        registerError.setManaged(true);
    }

    private void navigateTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pijava_fluently/fxml/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
