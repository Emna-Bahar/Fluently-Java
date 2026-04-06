package com.example.pijava_fluently.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label navUsername;
    @FXML
    private HBox navUserPill;

    @FXML
    private Button btnAccueil;
    @FXML
    private Button btnLangues;
    @FXML
    private Button btnTests;
    @FXML
    private Button btnGroupes;
    @FXML
    private Button btnSessions;
    @FXML
    private Button btnObjectifs;

    private ContextMenu userMenu;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (navUsername != null) {
            navUsername.setText("Emna");
        }
        createUserMenu();
        loadView("home-content.fxml");
    }

    // ========================
    // USER MENU
    // ========================

    private void createUserMenu() {
        userMenu = new ContextMenu();

        MenuItem profile = new MenuItem("👤  Mon Profil");
        MenuItem settings = new MenuItem("⚙️  Paramètres");
        SeparatorMenuItem separator = new SeparatorMenuItem();
        MenuItem logout = new MenuItem("⏻  Déconnexion");

        profile.setOnAction(e -> showProfile());
        settings.setOnAction(e -> showSettings());
        logout.setOnAction(e -> handleLogout());

        userMenu.getItems().addAll(profile, settings, separator, logout);
    }

    @FXML
    private void showUserMenu(MouseEvent event) {
        if (userMenu != null && navUserPill != null) {
            userMenu.show(navUserPill, event.getScreenX(), event.getScreenY() + 8);
        }
    }

    public void setContent(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }
    // ========================
    // NAVIGATION
    // ========================

    @FXML
    public void showAccueil() {
        loadView("home-content.fxml");
        setActiveButton(btnAccueil);
    }

    @FXML
    public void showLangues() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pijava_fluently/fxml/langues_etudiant.fxml"));
            Node view = loader.load();

            // Récupérer le contrôleur et lui passer homeController
            LanguesEtudiantController controller = loader.getController();
            controller.setHomeController(this);  // ← AJOUTE CETTE LIGNE !

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
            setActiveButton(btnLangues);
        } catch (IOException e) {
            System.err.println("❌ Impossible de charger : langues_etudiant.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void showMesTests() {
        loadView("mes-tests.fxml");
        setActiveButton(btnTests);
    }

    @FXML
    public void showGroupes() {
        loadView("groupes.fxml");
        setActiveButton(btnGroupes);
    }

    @FXML
    public void showSessions() {
        loadView("sessions.fxml");
        setActiveButton(btnSessions);
    }

    @FXML
    public void showObjectifs() {
        loadView("objectifs.fxml");
        setActiveButton(btnObjectifs);
    }

    // ========================
    // LOAD VIEW INTO CENTER
    // ========================

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/" + fxmlFile)
            );
            Node view = loader.load();
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("❌ Impossible de charger : " + fxmlFile + " (fichier manquant)");
        }
    }

    private void setActiveButton(Button activeBtn) {
        if (activeBtn == null) return;
        Button[] allButtons = {btnAccueil, btnLangues, btnTests, btnGroupes, btnSessions, btnObjectifs};
        for (Button btn : allButtons) {
            if (btn != null) btn.getStyleClass().remove("nav-link-active");
        }
        activeBtn.getStyleClass().add("nav-link-active");
    }

    // ========================
    // USER MENU ACTIONS
    // ========================

    private void showProfile() {
        System.out.println("Profil ouvert");
    }

    private void showSettings() {
        System.out.println("Paramètres ouverts");
    }

    private void handleLogout() {
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/login.fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            Stage stage = (Stage) navUsername.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Fluently - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}