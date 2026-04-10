package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class UserProfileController {

    @FXML private Label  heroInitials, heroFullName, heroRoleBadge, heroStatusBadge, heroEmail, heroId;
    @FXML private Label  sidebarName, breadcrumbLabel;

    @FXML private TextField        fieldPrenom, fieldNom, fieldEmail;
    @FXML private ComboBox<String> comboStatut, comboRole;
    @FXML private PasswordField    fieldNewPassword, fieldConfirmPassword;

    // The HBox that wraps both Statut + Role combos — hidden for front-office users
    @FXML private HBox statusRoleRow;

    @FXML private VBox   pwdStrengthBox;
    @FXML private Label  pwdStrengthLabel, profileMessage;
    @FXML private Region bar1, bar2, bar3, bar4;

    private User user;
    private final UserService userService = new UserService();
    private Runnable onSavedCallback;

    // "admin" = back to admin dashboard, "front" = back to home front-office
    private String backTarget = "admin";
    private User   currentUserForBack = null;
    private User   adminUser = null;  // the logged-in admin (for restoring session after back)

    public void setUser(User u)                 { this.user = u; }
    public void setOnSavedCallback(Runnable cb) { this.onSavedCallback = cb; }
    public void setCurrentUserForBack(User u)   { this.currentUserForBack = u; }
    public void setAdminUser(User u)            { this.adminUser = u; }

    public void setBackTarget(String target) {
        this.backTarget = target;
        // Now that we know the mode, populate and apply restrictions
        if (this.user != null) {
            populate();
            setupPasswordListener();
        }
        applyModeRestrictions();
    }

    // ── RESTRICTIONS BY MODE ──────────────────────────────────────────────────

    private void applyModeRestrictions() {
        boolean isAdmin = "admin".equals(backTarget);
        // Front-office users cannot change their role or status
        if (statusRoleRow != null) {
            statusRoleRow.setVisible(isAdmin);
            statusRoleRow.setManaged(isAdmin);
        }
    }

    // ── POPULATE ──────────────────────────────────────────────────────────────

    private void populate() {
        if (user == null) return;

        String initials = initials(user.getPrenom(), user.getNom());
        heroInitials.setText(initials);
        heroFullName.setText(user.getPrenom() + " " + user.getNom());
        heroEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        heroId.setText("#" + user.getId());

        boolean isAdmin = user.isAdmin();
        String roles = user.getRoles() != null ? user.getRoles() : "";
        String heroRole = roles.contains("ROLE_ADMIN") ? "Admin"
                        : roles.contains("ROLE_PROF")  ? "Professeur"
                        : "Étudiant";
        heroRoleBadge.setText(heroRole);
        heroRoleBadge.getStyleClass().removeAll("hero-role-chip-admin", "hero-role-chip-user");
        heroRoleBadge.getStyleClass().add(isAdmin ? "hero-role-chip-admin" : "hero-role-chip-user");

        String statut = user.getStatut() != null ? user.getStatut() : "actif";
        heroStatusBadge.setText(statut);
        heroStatusBadge.getStyleClass().removeAll("hero-status-actif", "hero-status-other");
        heroStatusBadge.getStyleClass().add("actif".equalsIgnoreCase(statut) ? "hero-status-actif" : "hero-status-other");

        String name = user.getPrenom() + " " + user.getNom();
        if (sidebarName     != null) sidebarName.setText(name);
        if (breadcrumbLabel != null) {
            breadcrumbLabel.setText("front".equals(backTarget)
                ? "Accueil › Mon Profil"
                : "Administration › Utilisateurs › " + name);
        }

        fieldPrenom.setText(nvl(user.getPrenom()));
        fieldNom.setText(nvl(user.getNom()));
        fieldEmail.setText(nvl(user.getEmail()));
        String comboRoleVal = roles.contains("ROLE_ADMIN") ? "Administrateur"
                            : roles.contains("ROLE_PROF")  ? "Professeur"
                            : "Étudiant";
        comboStatut.setValue(statut);
        comboRole.setValue(comboRoleVal);

        // Apply mode restrictions after populating
        applyModeRestrictions();
    }

    // ── PASSWORD STRENGTH ─────────────────────────────────────────────────────

    private void setupPasswordListener() {
        fieldNewPassword.textProperty().addListener((obs, old, val) -> {
            boolean show = !val.isEmpty();
            pwdStrengthBox.setVisible(show);
            pwdStrengthBox.setManaged(show);
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

    // ── VALIDATION ────────────────────────────────────────────────────────────

    /**
     * Returns true if the string contains only letters, spaces, hyphens or apostrophes.
     * No digits, no special characters like @, #, $, !, etc.
     */
    private boolean isValidName(String value) {
        return value.matches("[\\p{L} '\\-]+");
    }

    private boolean validateForm(String prenom, String nom, String email,
                                  String newPwd, String confirm) {
        // Prénom
        if (prenom.isEmpty()) {
            showMsg("Le prénom est obligatoire.", false); return false;
        }
        if (!isValidName(prenom)) {
            showMsg("Le prénom ne doit pas contenir de chiffres ou de caractères spéciaux.", false); return false;
        }

        // Nom
        if (nom.isEmpty()) {
            showMsg("Le nom est obligatoire.", false); return false;
        }
        if (!isValidName(nom)) {
            showMsg("Le nom ne doit pas contenir de chiffres ou de caractères spéciaux.", false); return false;
        }

        // Email
        if (email.isEmpty()) {
            showMsg("L'email est obligatoire.", false); return false;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showMsg("Adresse email invalide.", false); return false;
        }

        // Password (only if the user wants to change it)
        if (!newPwd.isEmpty()) {
            if (newPwd.length() < 8) {
                showMsg("Le mot de passe doit contenir au moins 8 caractères.", false); return false;
            }
            if (!newPwd.equals(confirm)) {
                showMsg("Les mots de passe ne correspondent pas.", false); return false;
            }
        }

        return true;
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        String prenom  = fieldPrenom.getText().trim();
        String nom     = fieldNom.getText().trim();
        String email   = fieldEmail.getText().trim();
        String statut  = comboStatut.getValue();
        String roleVal = comboRole.getValue();
        String newPwd  = fieldNewPassword.getText();
        String confirm = fieldConfirmPassword.getText();

        if (!validateForm(prenom, nom, email, newPwd, confirm)) return;

        try {
            User existing = userService.findByEmail(email);
            if (existing != null && existing.getId() != user.getId()) {
                showMsg("Cet email est déjà utilisé par un autre compte.", false); return;
            }

            user.setPrenom(prenom);
            user.setNom(nom);
            user.setEmail(email);

            // Only admins can change status and role
            if ("admin".equals(backTarget)) {
                user.setStatut(statut != null ? statut : "actif");
                String roleJson2;
            switch (roleVal != null ? roleVal : "Étudiant") {
                case "Administrateur": roleJson2 = "[\"ROLE_ADMIN\"]"; break;
                case "Professeur":     roleJson2 = "[\"ROLE_PROF\"]";  break;
                default:               roleJson2 = "[\"ROLE_ETUDIANT\"]";
            }
            user.setRoles(roleJson2);
            }

            if (!newPwd.isEmpty()) user.setPassword(newPwd);

            userService.modifier(user);

            // Refresh hero section live
            heroInitials.setText(initials(prenom, nom));
            heroFullName.setText(prenom + " " + nom);
            heroEmail.setText(email);
            if (sidebarName != null) sidebarName.setText(prenom + " " + nom);
            fieldNewPassword.clear();
            fieldConfirmPassword.clear();
            pwdStrengthBox.setVisible(false); pwdStrengthBox.setManaged(false);

            showMsg("Modifications enregistrées avec succès ✓", true);
            if (onSavedCallback != null) onSavedCallback.run();

        } catch (SQLException e) { showMsg("Erreur SQL : " + e.getMessage(), false); }
    }

    @FXML private void handleCancel() { populate(); hideMsg(); }

    // ── BACK ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        try {
            if ("front".equals(backTarget)) {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/home.fxml")
                );
                Parent root = loader.load();
                HomeController ctrl = loader.getController();
                User backUser = currentUserForBack != null ? currentUserForBack : user;
                // Sync any name changes
                backUser.setPrenom(user.getPrenom());
                backUser.setNom(user.getNom());
                ctrl.setCurrentUser(backUser);
                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
                );
                Stage stage = (Stage) fieldPrenom.getScene().getWindow();
                stage.setTitle("Fluently - Mon Espace");
                stage.setScene(scene);
                stage.centerOnScreen();
            } else {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/admin-dashboard.fxml")
                );
                Parent root = loader.load();
                AdminDashboardController ctrl = loader.getController();
                // Restore the logged-in admin's session
                if (adminUser != null) ctrl.setCurrentUser(adminUser);
                ctrl.showEtudiantsTab();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
                );
                Stage stage = (Stage) fieldPrenom.getScene().getWindow();
                stage.setTitle("Fluently - Administration");
                stage.setScene(scene);
                stage.centerOnScreen();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void showMsg(String msg, boolean ok) {
        profileMessage.setText(msg);
        profileMessage.getStyleClass().removeAll("pf-msg-error","pf-msg-success");
        profileMessage.getStyleClass().add(ok ? "pf-msg-success" : "pf-msg-error");
        profileMessage.setVisible(true); profileMessage.setManaged(true);
    }
    private void hideMsg() { profileMessage.setVisible(false); profileMessage.setManaged(false); }
    private static String initials(String p, String n) {
        return ((p!=null&&!p.isEmpty()) ? p.substring(0,1).toUpperCase() : "?")
             + ((n!=null&&!n.isEmpty()) ? n.substring(0,1).toUpperCase() : "?");
    }
    private static String nvl(String s) { return s != null ? s : ""; }
}
