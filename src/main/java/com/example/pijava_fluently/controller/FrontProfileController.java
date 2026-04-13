package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
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

public class FrontProfileController {

    // Navbar
    @FXML private Label navUsername;

    // Avatar card
    @FXML private Label fpInitials, fpFullName, fpRoleBadge, fpStatusBadge;
    @FXML private Label fpEmailMeta, fpIdMeta;

    // Form fields
    @FXML private TextField     fpFieldPrenom, fpFieldNom, fpFieldEmail;
    @FXML private PasswordField fpNewPwd, fpConfirmPwd;

    // Inline field errors
    @FXML private Label errPrenom, errNom, errEmail;

    // Password strength
    @FXML private VBox   fpPwdStrengthBox;
    @FXML private Label  fpPwdStrengthLabel;
    @FXML private Region fpBar1, fpBar2, fpBar3, fpBar4;

    // Global message
    @FXML private Label fpMessage;

    private User user;
    private final UserService userService = new UserService();

    public void setUser(User u) {
        this.user = u;
        populate();
        setupPwdListener();
    }

    // ── POPULATE ─────────────────────────────────────────────────────────────

    private void populate() {
        if (user == null) return;

        String initials = initials(user.getPrenom(), user.getNom());
        fpInitials.setText(initials);
        fpFullName.setText(user.getPrenom() + " " + user.getNom());
        navUsername.setText(user.getPrenom() + " " + user.getNom());
        fpEmailMeta.setText(nvl(user.getEmail()));
        fpIdMeta.setText("ID #" + user.getId());

        // Role badge
        String roleLabel = roleLabel(user.getRoles());
        fpRoleBadge.setText(roleLabel);
        fpRoleBadge.getStyleClass().removeAll("fp-role-etudiant","fp-role-prof","fp-role-admin");
        fpRoleBadge.getStyleClass().add(roleCss(user.getRoles()));

        // Status badge
        String statut = nvl(user.getStatut()).isEmpty() ? "actif" : user.getStatut();
        fpStatusBadge.setText(statut);
        fpStatusBadge.getStyleClass().removeAll("fp-status-actif","fp-status-other");
        fpStatusBadge.getStyleClass().add("actif".equalsIgnoreCase(statut) ? "fp-status-actif" : "fp-status-other");

        // Fields
        fpFieldPrenom.setText(nvl(user.getPrenom()));
        fpFieldNom.setText(nvl(user.getNom()));
        fpFieldEmail.setText(nvl(user.getEmail()));

        clearErrors();
        hideMsg();
    }

    // ── PASSWORD STRENGTH ────────────────────────────────────────────────────

    private void setupPwdListener() {
        fpNewPwd.textProperty().addListener((obs, old, val) -> {
            boolean show = !val.isEmpty();
            fpPwdStrengthBox.setVisible(show);
            fpPwdStrengthBox.setManaged(show);
            if (show) updateStrength(val);
        });
    }

    private void updateStrength(String pwd) {
        int score = 0;
        if (pwd.length() >= 8)               score++;
        if (pwd.matches(".*[A-Z].*"))         score++;
        if (pwd.matches(".*[0-9].*"))         score++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) score++;
        Region[] bars   = { fpBar1, fpBar2, fpBar3, fpBar4 };
        String[] colors = { "#EF4444","#F97316","#EAB308","#22C55E" };
        String[] labels = { "Très faible","Faible","Moyen","Fort" };
        int idx = Math.max(0, score - 1);
        for (int i = 0; i < 4; i++) {
            bars[i].setStyle(i < score
                ? "-fx-background-color:" + colors[idx] + ";-fx-background-radius:4;"
                : "-fx-background-color:#E2E8F0;-fx-background-radius:4;");
        }
        fpPwdStrengthLabel.setText("Force : " + labels[idx]);
        fpPwdStrengthLabel.setStyle("-fx-text-fill:" + colors[idx] + ";-fx-font-weight:bold;");
    }

    // ── VALIDATION ───────────────────────────────────────────────────────────

    private boolean isValidName(String v) {
        return v.matches("[\\p{L} '\\-]+");
    }

    private boolean validate(String prenom, String nom, String email, String pwd, String confirm) {
        clearErrors();
        boolean ok = true;

        if (prenom.isEmpty()) {
            showFieldError(errPrenom, "Le prénom est obligatoire."); ok = false;
        } else if (!isValidName(prenom)) {
            showFieldError(errPrenom, "Le prénom ne doit pas contenir de chiffres ou caractères spéciaux."); ok = false;
        }

        if (nom.isEmpty()) {
            showFieldError(errNom, "Le nom est obligatoire."); ok = false;
        } else if (!isValidName(nom)) {
            showFieldError(errNom, "Le nom ne doit pas contenir de chiffres ou caractères spéciaux."); ok = false;
        }

        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            showFieldError(errEmail, "Adresse email invalide."); ok = false;
        }

        if (!pwd.isEmpty()) {
            if (pwd.length() < 8) { showMsg("Mot de passe : 8 caractères minimum.", false); ok = false; }
            else if (!pwd.equals(confirm)) { showMsg("Les mots de passe ne correspondent pas.", false); ok = false; }
        }
        return ok;
    }

    // ── SAVE ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        String prenom  = fpFieldPrenom.getText().trim();
        String nom     = fpFieldNom.getText().trim();
        String email   = fpFieldEmail.getText().trim();
        String pwd     = fpNewPwd.getText();
        String confirm = fpConfirmPwd.getText();

        if (!validate(prenom, nom, email, pwd, confirm)) return;

        try {
            User existing = userService.findByEmail(email);
            if (existing != null && existing.getId() != user.getId()) {
                showFieldError(errEmail, "Cet email est déjà utilisé par un autre compte."); return;
            }

            user.setPrenom(prenom);
            user.setNom(nom);
            user.setEmail(email);
            if (!pwd.isEmpty()) user.setPassword(pwd);
            userService.modifier(user);

            // Refresh card
            fpInitials.setText(initials(prenom, nom));
            fpFullName.setText(prenom + " " + nom);
            navUsername.setText(prenom + " " + nom);
            fpEmailMeta.setText(email);
            fpNewPwd.clear(); fpConfirmPwd.clear();
            fpPwdStrengthBox.setVisible(false); fpPwdStrengthBox.setManaged(false);

            showMsg("Modifications enregistrées ✓", true);

        } catch (SQLException e) { showMsg("Erreur SQL : " + e.getMessage(), false); }
    }

    @FXML private void handleCancel() { populate(); }

    // ── BACK TO HOME ─────────────────────────────────────────────────────────

    @FXML
    private void goHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/home.fxml")
            );
            Parent root = loader.load();
            HomeController ctrl = loader.getController();
            ctrl.setCurrentUser(user);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            Stage stage = (Stage) fpFieldPrenom.getScene().getWindow();
            stage.setTitle("Fluently - Mon Espace");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private void showFieldError(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }
    private void clearErrors() {
        for (Label l : new Label[]{errPrenom, errNom, errEmail}) {
            l.setVisible(false); l.setManaged(false);
        }
    }
    private void showMsg(String msg, boolean ok) {
        fpMessage.setText(msg);
        fpMessage.getStyleClass().removeAll("fp-msg-ok","fp-msg-err");
        fpMessage.getStyleClass().add(ok ? "fp-msg-ok" : "fp-msg-err");
        fpMessage.setVisible(true); fpMessage.setManaged(true);
    }
    private void hideMsg() { fpMessage.setVisible(false); fpMessage.setManaged(false); }

    private static String roleLabel(String roles) {
        if (roles == null) return "Étudiant";
        if (roles.contains("ROLE_ADMIN")) return "Admin";
        if (roles.contains("ROLE_PROF"))  return "Professeur";
        return "Étudiant";
    }
    private static String roleCss(String roles) {
        if (roles == null) return "fp-role-etudiant";
        if (roles.contains("ROLE_ADMIN")) return "fp-role-admin";
        if (roles.contains("ROLE_PROF"))  return "fp-role-prof";
        return "fp-role-etudiant";
    }
    private static String initials(String p, String n) {
        return ((p!=null&&!p.isEmpty()) ? p.substring(0,1).toUpperCase() : "?")
             + ((n!=null&&!n.isEmpty()) ? n.substring(0,1).toUpperCase() : "?");
    }
    private static String nvl(String s) { return s != null ? s : ""; }
}
