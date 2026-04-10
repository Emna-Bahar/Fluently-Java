package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.UserService;
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
import java.sql.SQLException;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label navUsername;
    @FXML private HBox navUserPill;

    @FXML private Button btnAccueil;
    @FXML private Button btnLangues;
    @FXML private Button btnTests;
    @FXML private Button btnGroupes;
    @FXML private Button btnSessions;
    @FXML private Button btnObjectifs;

    private ContextMenu userMenu;
    private User currentUser;
    private final UserService userService = new UserService();

    // ============================================================
    // INITIALISATION
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        createUserMenu();

        // Afficher un nom par défaut si aucun utilisateur n'est défini
        if (navUsername != null && currentUser == null) {
            navUsername.setText("Emna");
        }

        loadView("home-content.fxml");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (navUsername != null && user != null) {
            navUsername.setText(user.getPrenom() + " " + user.getNom());
        }
    }

    // ============================================================
    // USER MENU
    // ============================================================

    private void createUserMenu() {
        userMenu = new ContextMenu();

        MenuItem profile = new MenuItem("👤 Mon Profil");
        MenuItem settings = new MenuItem("⚙️ Paramètres");
        SeparatorMenuItem separator = new SeparatorMenuItem();
        MenuItem logout = new MenuItem("⏻ Déconnexion");

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

    // ============================================================
    // NAVIGATION
    // ============================================================

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

            LanguesEtudiantController controller = loader.getController();
            controller.setHomeController(this);

            setContent(view);
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
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/Objectif-view.fxml")
            );
            Node view = loader.load();

            ObjectifController ctrl = loader.getController();
            ctrl.setHomeController(this);

            setContent(view);
            setActiveButton(btnObjectifs);

        } catch (IOException e) {
            System.err.println("❌ Impossible de charger : Objectif-view.fxml");
            e.printStackTrace();
        }
    }

    // ============================================================
    // SET CONTENT (public — utilisé par les sous-contrôleurs)
    // ============================================================

    /**
     * Remplace le contenu central par le Node fourni.
     * Appelé par ObjectifController, TacheController, LanguesEtudiantController, etc.
     */
    public void setContent(Node view) {
        if (contentArea != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        }
    }

    // ============================================================
    // HELPERS PRIVÉS
    // ============================================================

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/" + fxmlFile)
            );
            Node view = loader.load();
            setContent(view);
        } catch (IOException e) {
            System.err.println("❌ Impossible de charger : " + fxmlFile);
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

    // ============================================================
    // USER MENU ACTIONS
    // ============================================================

    /**
     * Ouvre la page de profil front-office
     */
    private void showProfile() {
        if (currentUser == null) {
            System.out.println("Aucun utilisateur connecté");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/front-profile.fxml")
            );
            Parent root = loader.load();

            FrontProfileController ctrl = loader.getController();
            ctrl.setUser(currentUser);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            Stage stage = (Stage) navUsername.getScene().getWindow();
            stage.setTitle("Fluently - Mon Profil");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSettings() {
        System.out.println("Paramètres ouverts");
    }

    /**
     * Déconnexion : met à jour le statut de l'utilisateur puis redirige vers login
     */
    private void handleLogout() {
        if (currentUser != null) {
            try {
                userService.updateStatut(currentUser.getId(), "offline");
            } catch (SQLException e) {
                System.err.println("Impossible de mettre à jour le statut : " + e.getMessage());
            }
        }
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