package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.models.User;
import com.example.pijava_fluently.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class UserNewController {

    @FXML private TextField     fieldPrenom, fieldNom, fieldEmail;
    @FXML private ComboBox<String> comboStatut, comboRole;
    @FXML private PasswordField fieldPassword, fieldConfirmPassword;
    @FXML private VBox          pwdStrengthBox;
    @FXML private Label         pwdStrengthLabel, newUserMessage;
    @FXML private Region        bar1, bar2, bar3, bar4;

    private final UserService userService = new UserService();
    private Runnable onSavedCallback;

    public void setOnSavedCallback(Runnable cb) { this.onSavedCallback = cb; }

    @FXML
    public void initialize() {
        if (comboStatut != null) comboStatut.setValue("actif");
        if (comboRole   != null) comboRole.setValue("Étudiant");
        fieldPassword.textProperty().addListener((obs, old, val) -> {
            boolean show = !val.isEmpty();
            pwdStrengthBox.setVisible(show); pwdStrengthBox.setManaged(show);
            if (show) updateStrength(val);
        });
    }

    private void updateStrength(String pwd) {
        int score = 0;
        if (pwd.length() >= 8)               score++;
        if (pwd.matches(".*[A-Z].*"))         score++;
        if (pwd.matches(".*[0-9].*"))         score++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) score++;
        Region[] bars   = { bar1, bar2, bar3, bar4 };
        String[] colors = { "#EF4444","#F97316","#EAB308","#22C55E" };
        String[] labels = { "Très faible","Faible","Moyen","Fort" };
        int idx = Math.max(0, score - 1);
        for (int i = 0; i < 4; i++) {
            bars[i].setStyle(i < score
                ? "-fx-background-color:" + colors[idx] + ";-fx-background-radius:4;"
                : "-fx-background-color:#E2E8F0;-fx-background-radius:4;");
            bars[i].setPrefHeight(6);
        }
        pwdStrengthLabel.setText("Force : " + labels[idx]);
        pwdStrengthLabel.setStyle("-fx-text-fill:" + colors[idx] + ";-fx-font-weight:bold;");
    }

    @FXML
    private void handleCreate() {
        String prenom  = fieldPrenom.getText().trim();
        String nom     = fieldNom.getText().trim();
        String email   = fieldEmail.getText().trim();
        String pwd     = fieldPassword.getText();
        String confirm = fieldConfirmPassword.getText();
        String statut  = comboStatut.getValue();
        String roleVal = comboRole.getValue();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
            showMsg("Tous les champs marqués * sont obligatoires.", false); return;
        }
        if (!prenom.matches("[\\p{L} '\\-]+")) {
            showMsg("Le prénom ne doit pas contenir de chiffres ou caractères spéciaux.", false); return;
        }
        if (!nom.matches("[\\p{L} '\\-]+")) {
            showMsg("Le nom ne doit pas contenir de chiffres ou caractères spéciaux.", false); return;
        }
        if (!email.contains("@"))  { showMsg("Adresse email invalide.", false); return; }
        if (pwd.length() < 8)      { showMsg("Mot de passe : 8 caractères minimum.", false); return; }
        if (!pwd.equals(confirm))  { showMsg("Les mots de passe ne correspondent pas.", false); return; }

        try {
            if (userService.findByEmail(email) != null) {
                showMsg("Cet email est déjà utilisé.", false); return;
            }
            String roleJson;
            switch (roleVal != null ? roleVal : "Étudiant") {
                case "Administrateur": roleJson = "[\"ROLE_ADMIN\"]"; break;
                case "Professeur":     roleJson = "[\"ROLE_PROF\"]";  break;
                default:               roleJson = "[\"ROLE_ETUDIANT\"]";
            }
            User u = new User();
            u.setPrenom(prenom); u.setNom(nom); u.setEmail(email); u.setPassword(pwd);
            u.setStatut(statut != null ? statut : "actif");
            u.setRoles(roleJson);
            userService.ajouter(u);
            showMsg("Utilisateur créé avec succès ✓ Retour dans 2 secondes…", true);
            if (onSavedCallback != null) onSavedCallback.run();
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(this::goBack);
            }).start();
        } catch (SQLException e) { showMsg("Erreur SQL : " + e.getMessage(), false); }
    }

    @FXML private void handleCancel() { goBack(); }

    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/admin-dashboard.fxml")
            );
            Parent root = loader.load();
            AdminDashboardController ctrl = loader.getController();
            ctrl.showEtudiantsTab();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            Stage stage = (Stage) fieldPrenom.getScene().getWindow();
            stage.setTitle("Fluently - Administration");
            stage.setScene(scene); stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showMsg(String msg, boolean ok) {
        newUserMessage.setText(msg);
        newUserMessage.getStyleClass().removeAll("pf-msg-error","pf-msg-success");
        newUserMessage.getStyleClass().add(ok ? "pf-msg-success" : "pf-msg-error");
        newUserMessage.setVisible(true); newUserMessage.setManaged(true);
    }
}
