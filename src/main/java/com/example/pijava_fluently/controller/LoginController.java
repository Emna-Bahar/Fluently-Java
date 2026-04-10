package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.models.User;
import com.example.pijava_fluently.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    // Login
    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;

    // Register
    @FXML private TextField     regPrenom;
    @FXML private TextField     regNom;
    @FXML private TextField     regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label         registerError;
    @FXML private ToggleButton  btnEtudiant;
    @FXML private ToggleButton  btnProf;

    // Containers
    @FXML private javafx.scene.layout.VBox loginForm;
    @FXML private javafx.scene.layout.VBox registerForm;

    // Tabs
    @FXML private Button loginTabBtn;
    @FXML private Button registerTabBtn;

    private String selectedRole = null;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        selectEtudiant();
    }

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

            // Mark user as online
            userService.updateStatut(user.getId(), "online");
            user.setStatut("online");

            if (user.isAdmin()) {
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
            } else {
                // Pass the real logged-in user to HomeController
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
        } catch (SQLException e) {
            showLoginError("Erreur de connexion à la base de données."); e.printStackTrace();
        } catch (IOException e) {
            showLoginError("Erreur lors du chargement de la page."); e.printStackTrace();
        }
    }

    @FXML private void forgotPassword() { System.out.println("Mot de passe oublié"); }

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

            userService.ajouter(newUser);

            // After register, load home and pass user
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/home.fxml")
            );
            Parent root = loader.load();
            HomeController homeCtrl = loader.getController();
            homeCtrl.setCurrentUser(newUser);
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setTitle("Fluently - Mon Espace");
            stage.setScene(buildScene(root));
            stage.centerOnScreen();

        } catch (SQLException e) {
            showRegisterError("Erreur lors de la création du compte."); e.printStackTrace();
        } catch (IOException e) {
            showRegisterError("Erreur lors du chargement de la page."); e.printStackTrace();
        }
    }

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
        return scene;
    }
}
