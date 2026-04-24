package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class FrontProfileController {

    // ── NAVBAR ────────────────────────────────────────────────────────────────
    @FXML private Label navUsername;

    // ── AVATAR CARD ───────────────────────────────────────────────────────────
    /** The StackPane that wraps the avatar circle (initials fallback). */
    @FXML private StackPane fpAvatarPane;
    @FXML private Label     fpInitials;
    @FXML private Label     fpFullName, fpRoleBadge, fpStatusBadge;
    @FXML private Label     fpEmailMeta, fpIdMeta;
    /** Shows which language the user is studying (may be null in FXML — safe). */
    @FXML private Label     fpLanguageLabel;

    // ── FORM FIELDS ───────────────────────────────────────────────────────────
    @FXML private TextField     fpFieldPrenom, fpFieldNom, fpFieldEmail;
    @FXML private PasswordField fpNewPwd, fpConfirmPwd;

    // ── INLINE FIELD ERRORS ───────────────────────────────────────────────────
    @FXML private Label errPrenom, errNom, errEmail;

    // ── PASSWORD STRENGTH ─────────────────────────────────────────────────────
    @FXML private VBox   fpPwdStrengthBox;
    @FXML private Label  fpPwdStrengthLabel;
    @FXML private Region fpBar1, fpBar2, fpBar3, fpBar4;

    // ── GLOBAL MESSAGE ────────────────────────────────────────────────────────
    @FXML private Label fpMessage;

    private User user;
    private final UserService userService = new UserService();

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    public void setUser(User u) {
        this.user = u;
        populate();
        setupPwdListener();
    }

    // ── POPULATE ─────────────────────────────────────────────────────────────

    private void populate() {
        if (user == null) return;

        // ── Avatar: render SVG if available, otherwise show initials ──────────
        renderAvatar();

        fpFullName.setText(user.getPrenom() + " " + user.getNom());
        navUsername.setText(user.getPrenom() + " " + user.getNom());
        fpEmailMeta.setText(nvl(user.getEmail()));
        fpIdMeta.setText("ID #" + user.getId());

        // Language badge (optional label in FXML)
        if (fpLanguageLabel != null) {
            String lang = user.getChosenLanguage();
            if (lang != null && !lang.isBlank()) {
                fpLanguageLabel.setText("🌍 " + lang);
                fpLanguageLabel.setVisible(true);
                fpLanguageLabel.setManaged(true);
            } else {
                fpLanguageLabel.setVisible(false);
                fpLanguageLabel.setManaged(false);
            }
        }

        // Role badge
        String roleLabel = roleLabel(user.getRoles());
        fpRoleBadge.setText(roleLabel);
        fpRoleBadge.getStyleClass().removeAll("fp-role-etudiant", "fp-role-prof", "fp-role-admin");
        fpRoleBadge.getStyleClass().add(roleCss(user.getRoles()));

        // Status badge
        String statut = nvl(user.getStatut()).isEmpty() ? "actif" : user.getStatut();
        fpStatusBadge.setText(statut);
        fpStatusBadge.getStyleClass().removeAll("fp-status-actif", "fp-status-other");
        fpStatusBadge.getStyleClass().add("actif".equalsIgnoreCase(statut) ? "fp-status-actif" : "fp-status-other");

        // Form fields
        fpFieldPrenom.setText(nvl(user.getPrenom()));
        fpFieldNom.setText(nvl(user.getNom()));
        fpFieldEmail.setText(nvl(user.getEmail()));

        clearErrors();
        hideMsg();
    }

    /**
     * Puts the AI-generated SVG avatar into fpAvatarPane.
     * Falls back to the initials Label when no SVG is available.
     */
    private void renderAvatar() {
        if (fpAvatarPane == null) return;  // guard if FXML not yet updated

        String svg = user.getAvatarSvg();

        if (svg != null && !svg.isBlank()) {
            // Build a transparent WebView sized to the pane
            WebView wv = new WebView();
            wv.setPrefSize(96, 96);
            wv.setMaxSize(96, 96);


            // Wrap SVG in a minimal HTML page that fills the viewport
            String html = "<!DOCTYPE html><html><head>"
                    + "<style>html,body{margin:0;padding:0;background:transparent;overflow:hidden;}"
                    + "svg{width:100%;height:100%;display:block;}</style>"
                    + "</head><body>" + svg + "</body></html>";
            wv.getEngine().loadContent(html);

            // Hide the initials label; show the WebView
            fpInitials.setVisible(false);
            fpInitials.setManaged(false);
            fpAvatarPane.getChildren().removeIf(n -> n instanceof WebView);
            fpAvatarPane.getChildren().add(wv);
        } else {
            // No avatar yet — show initials
            fpInitials.setText(initials(user.getPrenom(), user.getNom()));
            fpInitials.setVisible(true);
            fpInitials.setManaged(true);
            fpAvatarPane.getChildren().removeIf(n -> n instanceof WebView);
        }
    }

    // ── PASSWORD STRENGTH ─────────────────────────────────────────────────────

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
        String[] colors = { "#EF4444", "#F97316", "#EAB308", "#22C55E" };
        String[] labels = { "Très faible", "Faible", "Moyen", "Fort" };
        int idx = Math.max(0, score - 1);
        for (int i = 0; i < 4; i++) {
            bars[i].setStyle(i < score
                    ? "-fx-background-color:" + colors[idx] + ";-fx-background-radius:4;"
                    : "-fx-background-color:#E2E8F0;-fx-background-radius:4;");
        }
        fpPwdStrengthLabel.setText("Force : " + labels[idx]);
        fpPwdStrengthLabel.setStyle("-fx-text-fill:" + colors[idx] + ";-fx-font-weight:bold;");
    }

    // ── VALIDATION ────────────────────────────────────────────────────────────

    private boolean isValidName(String v) { return v.matches("[\\p{L} '\\-]+"); }

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

    // ── SAVE ──────────────────────────────────────────────────────────────────

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

            // Refresh card header
            renderAvatar();  // re-render in case nothing changed, keeps it consistent
            fpFullName.setText(prenom + " " + nom);
            navUsername.setText(prenom + " " + nom);
            fpEmailMeta.setText(email);
            fpNewPwd.clear(); fpConfirmPwd.clear();
            fpPwdStrengthBox.setVisible(false); fpPwdStrengthBox.setManaged(false);

            showMsg("Modifications enregistrées ✓", true);

        } catch (SQLException e) { showMsg("Erreur SQL : " + e.getMessage(), false); }
    }

    @FXML private void handleCancel() { populate(); }

    // ── BACK TO HOME ──────────────────────────────────────────────────────────

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

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void showFieldError(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }
    private void clearErrors() {
        for (Label l : new Label[]{ errPrenom, errNom, errEmail }) {
            l.setVisible(false); l.setManaged(false);
        }
    }
    private void showMsg(String msg, boolean ok) {
        fpMessage.setText(msg);
        fpMessage.getStyleClass().removeAll("fp-msg-ok", "fp-msg-err");
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
        return ((p != null && !p.isEmpty()) ? p.substring(0, 1).toUpperCase() : "?")
                + ((n != null && !n.isEmpty()) ? n.substring(0, 1).toUpperCase() : "?");
    }
    private static String nvl(String s) { return s != null ? s : ""; }
}