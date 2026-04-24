package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.example.pijava_fluently.utils.TranslateButton;
import java.io.IOException;
import java.sql.SQLException;
import com.example.pijava_fluently.utils.LanguageManager;

public class LoginController {

    private String capturedFaceDescriptor = null;

    // ── LOGIN FIELDS ──────────────────────────────────────────────────────────
    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;
    @FXML private Label         faceStatusLabel;

    // ── REGISTER FIELDS ───────────────────────────────────────────────────────
    @FXML private TextField     regPrenom;
    @FXML private TextField     regNom;
    @FXML private TextField     regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label         registerError;
    @FXML private ToggleButton  btnEtudiant;
    @FXML private ToggleButton  btnProf;

    @FXML private javafx.scene.control.Button btnLang;

    // ── CONTAINERS ────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox loginForm;
    @FXML private javafx.scene.layout.VBox registerForm;

    // ── TRANSLATABLE LABELS ───────────────────────────────────────────────────
    @FXML private Label loginWelcomeLabel;
    @FXML private Label loginSubtitleLabel;
    @FXML private Label loginEmailLabel;
    @FXML private Label loginPasswordLabel;
    @FXML private Label loginNoAccountLabel;
    @FXML private Label regTitleLabel;
    @FXML private Label regSubtitleLabel;

    // ── TABS ─────────────────────────────────────────────────────────────────
    @FXML private Button loginTabBtn;
    @FXML private Button registerTabBtn;

    private String selectedRole = null;
    private final UserService userService = new UserService();

    // ── INIT ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        selectEtudiant();
        setupLanguage();
    }

    private void setupLanguage() {
        LanguageManager lm = LanguageManager.getInstance();
        lm.languageProperty().addListener((obs, o, n) -> applyLanguage());
        applyLanguage();
    }

    private void applyLanguage() {
        LanguageManager lm = LanguageManager.getInstance();
        if (loginTabBtn    != null) loginTabBtn.setText(lm.t("login.tab"));
        if (registerTabBtn != null) registerTabBtn.setText(lm.t("register.tab"));
        if (btnLang        != null) btnLang.setText(lm.t("lang.btn"));
    }

    @FXML private void toggleLanguage() { LanguageManager.getInstance().toggle(); }

    // ── ROLE SELECTION ────────────────────────────────────────────────────────

    @FXML
    private void selectEtudiant() {
        selectedRole = "ROLE_ETUDIANT";
        btnEtudiant.setSelected(true);
        btnProf.setSelected(false);
        btnEtudiant.getStyleClass().add("role-toggle-active");
        btnProf.getStyleClass().remove("role-toggle-active");
    }

    @FXML
    private void selectProf() {
        selectedRole = "ROLE_PROF";
        btnProf.setSelected(true);
        btnEtudiant.setSelected(false);
        btnProf.getStyleClass().add("role-toggle-active");
        btnEtudiant.getStyleClass().remove("role-toggle-active");
    }

    // ── TAB SWITCHING ─────────────────────────────────────────────────────────

    @FXML
    private void switchToLogin() {
        loginForm.setVisible(true);     loginForm.setManaged(true);
        registerForm.setVisible(false); registerForm.setManaged(false);
        loginTabBtn.getStyleClass().add("tab-active");
        registerTabBtn.getStyleClass().remove("tab-active");
    }

    @FXML
    private void switchToRegister() {
        registerForm.setVisible(true); registerForm.setManaged(true);
        loginForm.setVisible(false);   loginForm.setManaged(false);
        registerTabBtn.getStyleClass().add("tab-active");
        loginTabBtn.getStyleClass().remove("tab-active");
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleLogin() {
        String email    = loginEmail.getText().trim();
        String password = loginPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showLoginError("Veuillez remplir tous les champs."); return;
        }

        try {
            User user = userService.authenticate(email, password);
            if (user == null) { showLoginError("Email ou mot de passe incorrect."); return; }

            userService.updateStatut(user.getId(), "online");
            user.setStatut("online");

            if (user.isAdmin()) {
                navigateToAdmin(user);
            } else {
                navigateToHome(user);
            }
        } catch (SQLException e) {
            showLoginError("Erreur de connexion à la base de données."); e.printStackTrace();
        } catch (IOException e) {
            showLoginError("Erreur lors du chargement de la page."); e.printStackTrace();
        }
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    @FXML
    private void handleRegister() {
        String prenom   = regPrenom.getText().trim();
        String nom      = regNom.getText().trim();
        String email    = regEmail.getText().trim();
        String password = regPassword.getText();
        String confirm  = regConfirmPassword.getText();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showRegisterError("Veuillez remplir tous les champs."); return;
        }
        if (!prenom.matches("[\\p{L} '\\-]+")) {
            showRegisterError("Le prénom ne doit pas contenir de chiffres ou caractères spéciaux."); return;
        }
        if (!nom.matches("[\\p{L} '\\-]+")) {
            showRegisterError("Le nom ne doit pas contenir de chiffres ou caractères spéciaux."); return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showRegisterError("Adresse email invalide."); return;
        }
        if (selectedRole == null) {
            showRegisterError("Veuillez choisir un rôle."); return;
        }
        if (password.length() < 8) {
            showRegisterError("Le mot de passe doit contenir au moins 8 caractères."); return;
        }
        if (!password.equals(confirm)) {
            showRegisterError("Les mots de passe ne correspondent pas."); return;
        }

        try {
            if (userService.findByEmail(email) != null) {
                showRegisterError("Cet email est déjà utilisé."); return;
            }

            User newUser = new User();
            newUser.setPrenom(prenom);
            newUser.setNom(nom);
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setStatut("actif");
            newUser.setRoles("[\"" + selectedRole + "\"]");
            newUser.setFaceDescriptor(capturedFaceDescriptor);

            userService.ajouter(newUser);

            // ── NEW: go to language picker so the user picks a language
            //        and gets an AI-generated avatar BEFORE reaching home ──
            navigateToLanguagePicker(newUser);

        } catch (SQLException e) {
            showRegisterError("Erreur lors de la création du compte."); e.printStackTrace();
        } catch (IOException e) {
            showRegisterError("Erreur lors du chargement de la page."); e.printStackTrace();
        }
    }

    // ── FACE AUTH ─────────────────────────────────────────────────────────────

    @FXML
    private void handleCaptureFace() {
        faceStatusLabel.setText("📷 Ouverture caméra...");
        faceStatusLabel.setVisible(true);
        faceStatusLabel.setManaged(true);

        new Thread(() -> {
            try {
                String scriptPath = "C:/Users/MSI/Desktop/java-git/Fluently-Java/face_register.py";
                ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                String descriptorJson = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("{\"descriptor\"")) descriptorJson = line;
                }
                process.waitFor();

                final String result = descriptorJson;
                javafx.application.Platform.runLater(() -> {
                    if (result != null) {
                        capturedFaceDescriptor = result
                                .replace("{\"descriptor\":", "")
                                .replace("}", "").trim();
                        faceStatusLabel.setText("✅ Visage enregistré !");
                    } else {
                        faceStatusLabel.setText("❌ Aucun visage détecté. Réessayez.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        faceStatusLabel.setText("❌ Erreur: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleFaceLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/face-login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setTitle("Fluently - Face Login");
            stage.setScene(buildScene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            showLoginError("Impossible d'ouvrir la page Face Login.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoogleLogin() {
        Stage stage = (Stage) loginEmail.getScene().getWindow();
        GoogleAuthController google = new GoogleAuthController(stage, loginError);
        google.startGoogleLogin();
    }

    @FXML
    private void forgotPassword() {
        Stage owner = (Stage) loginEmail.getScene().getWindow();
        new ForgotPasswordController().show(owner);
    }

    // ── NAVIGATION HELPERS ────────────────────────────────────────────────────

    /** After registration → language picker → avatar generation → home */
    private void navigateToLanguagePicker(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/language-picker.fxml")
        );
        Parent root = loader.load();
        LanguagePickerController ctrl = loader.getController();
        ctrl.setCurrentUser(user);
        Stage stage = (Stage) loginEmail.getScene().getWindow();
        stage.setTitle("Fluently – Choisissez votre langue");
        stage.setScene(buildScene(root));
        stage.centerOnScreen();
    }

    private void navigateToHome(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/home.fxml")
        );
        Parent root = loader.load();
        HomeController homeCtrl = loader.getController();
        homeCtrl.setCurrentUser(user);
        Stage stage = (Stage) loginEmail.getScene().getWindow();
        stage.setTitle("Fluently - Mon Espace");
        stage.setScene(buildScene(root));
        stage.centerOnScreen();
    }

    private void navigateToAdmin(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/admin-dashboard.fxml")
        );
        Parent root = loader.load();
        AdminDashboardController ctrl = loader.getController();
        ctrl.setCurrentUser(user);
        Stage stage = (Stage) loginEmail.getScene().getWindow();
        stage.setTitle("Fluently - Administration");
        stage.setScene(buildScene(root));
        stage.centerOnScreen();
    }

    // ── ERROR HELPERS ─────────────────────────────────────────────────────────

    private void showLoginError(String msg) {
        loginError.setText(msg); loginError.setVisible(true); loginError.setManaged(true);
    }

    private void showRegisterError(String msg) {
        registerError.setText(msg); registerError.setVisible(true); registerError.setManaged(true);
    }

    private Scene buildScene(Parent root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
        );
        TranslateButton.attachTo(scene);
        return scene;
    }
}