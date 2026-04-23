package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.NotificationService;
import com.example.pijava_fluently.services.UserService;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label     navUsername;
    @FXML private HBox      navUserPill;

    @FXML private Button btnAccueil;
    @FXML private Button btnLangues;
    @FXML private Button btnTests;
    @FXML private Button btnGroupes;
    @FXML private Button btnSessions;
    @FXML private Button btnObjectifs;
    @FXML private Button btnTheme;
    @FXML private VBox   rootPane;

    private ContextMenu   userMenu;
    private User          currentUser;
    private final UserService userService = new UserService();

    // ============================================================
    // INITIALISATION
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        createUserMenu();
        if (navUsername != null && currentUser == null) {
            navUsername.setText("Utilisateur");
        }

        // Initialiser NotificationService avec HomeController
        NotificationService.setHomeController(this);

        // Charger la page d'accueil
        showAccueil();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (navUsername != null && user != null) {
            navUsername.setText(user.getPrenom() + " " + user.getNom());
        }


    }

    // ============================================================
    // THEME SOMBRE / CLAIR
    // ============================================================

    @FXML
    private void toggleTheme() {
        Scene scene = resolveScene();
        if (scene == null) { System.err.println("toggleTheme : Scene introuvable."); return; }

        ObservableList<String> sheets = scene.getStylesheets();
        var res = getClass().getResource("/com/example/pijava_fluently/css/dark.css");
        if (res == null) { System.err.println("dark.css introuvable"); return; }
        String darkCss = res.toExternalForm();

        if (sheets.contains(darkCss)) {
            sheets.remove(darkCss);
            if (btnTheme != null) btnTheme.setText("Mode sombre");
        } else {
            sheets.add(darkCss);
            if (btnTheme != null) btnTheme.setText("Mode clair");
        }
    }

    private Scene resolveScene() {
        if (rootPane    != null && rootPane.getScene()    != null) return rootPane.getScene();
        if (btnTheme    != null && btnTheme.getScene()    != null) return btnTheme.getScene();
        if (navUsername != null && navUsername.getScene() != null) return navUsername.getScene();
        if (contentArea != null && contentArea.getScene() != null) return contentArea.getScene();
        return null;
    }

    // ============================================================
    // USER MENU
    // ============================================================

    private void createUserMenu() {
        userMenu = new ContextMenu();
        MenuItem profile  = new MenuItem("Mon Profil");
        MenuItem settings = new MenuItem("Parametres");
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem logout   = new MenuItem("Deconnexion");
        profile.setOnAction(e  -> showProfile());
        settings.setOnAction(e -> showSettings());
        logout.setOnAction(e   -> handleLogout());
        userMenu.getItems().addAll(profile, settings, sep, logout);
    }

    @FXML
    private void showUserMenu(MouseEvent event) {
        if (userMenu != null && navUserPill != null)
            userMenu.show(navUserPill, event.getScreenX(), event.getScreenY() + 8);
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    @FXML public void showAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/home-content.fxml"));
            Node view = loader.load();
            HomeContentController contentController = loader.getController();
            contentController.setHomeController(this);
            setContent(view);
            setActiveButton(btnAccueil);
        } catch (IOException e) {
            System.err.println("Erreur chargement home-content.fxml");
            e.printStackTrace();
        }
    }

    @FXML public void showLangues() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/langues_etudiant.fxml"));
            Node view = loader.load();
            LanguesEtudiantController ctrl = loader.getController();
            ctrl.setHomeController(this);
            ctrl.setCurrentUser(currentUser);
            setContent(view);
            setActiveButton(btnLangues);
        } catch (IOException e) {
            System.err.println("langues_etudiant.fxml introuvable");
            e.printStackTrace();
        }
    }

    @FXML public void showMesTests() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/mes-tests.fxml"));
            Node view = loader.load();
            MesTestsController ctrl = loader.getController();
            ctrl.setHomeController(this);
            ctrl.setCurrentUser(currentUser);
            setContent(view);
            setActiveButton(btnTests);
        } catch (IOException e) {
            System.err.println("mes-tests.fxml introuvable");
            e.printStackTrace();
        }
    }

    @FXML public void showGroupes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/groupes.fxml"));
            Node view = loader.load();
            setContent(view);
            setActiveButton(btnGroupes);
        } catch (IOException e) {
            System.err.println("groupes.fxml introuvable");
            e.printStackTrace();
        }
    }

    @FXML public void showSessions() {
        boolean isProf = currentUser != null
                && currentUser.getRoles() != null
                && (currentUser.getRoles().contains("ROLE_PROF")
                || currentUser.getRoles().contains("ROLE_PROFESSEUR"));

        if (isProf) {
            showSessionsProf();
        } else {
            showSessionsEtudiant();
        }
        setActiveButton(btnSessions);
    }

    @FXML public void showSessionsProf() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/session-prof-view.fxml"));
            Node content = loader.load();
            Sessionprofcontroller ctrl = loader.getController();
            int profId = (currentUser != null) ? currentUser.getId() : 4;
            ctrl.setProfId(profId);
            setContent(content);
            setActiveButton(btnSessions);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur chargement sessions prof : " + e.getMessage());
        }
    }

    private void showSessionsEtudiant() {
        var url = getClass().getResource(
                "/com/example/pijava_fluently/fxml/session-etudiant-view.fxml");
        if (url == null) {
            showError("session-etudiant-view.fxml introuvable");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Node content = loader.load();
            Sessionetudiantcontroller ctrl = loader.getController();
            int userId = (currentUser != null) ? currentUser.getId() : 3;
            ctrl.setUserId(userId);
            setContent(content);
        } catch (IOException e) {
            e.printStackTrace();
            showError(e.getMessage());
        }
    }

    @FXML public void showObjectifs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/Objectif-view.fxml"));
            Node view = loader.load();
            ObjectifController ctrl = loader.getController();
            ctrl.setHomeController(this);
            ctrl.setCurrentUser(currentUser);

            // Lier ObjectifController à NotificationService
            NotificationService.setObjectifController(ctrl);

            setContent(view);
            setActiveButton(btnObjectifs);
        } catch (IOException e) {
            System.err.println("Objectif-view.fxml introuvable");
            e.printStackTrace();
        }
    }

    // ============================================================
    // CONTENU CENTRAL
    // ============================================================

    public void setContent(Node view) {
        if (contentArea != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        }
    }

    // ============================================================
    // HELPERS PRIVES
    // ============================================================

    private void setActiveButton(Button activeBtn) {
        if (activeBtn == null) return;
        Button[] all = {btnAccueil, btnLangues, btnTests, btnGroupes, btnSessions, btnObjectifs};
        for (Button b : all) {
            if (b != null) {
                b.getStyleClass().remove("nav-link-active");
            }
        }
        activeBtn.getStyleClass().add("nav-link-active");
    }

    // ============================================================
    // ACTIONS MENU UTILISATEUR
    // ============================================================

    private void showProfile() {
        if (currentUser == null) {
            System.out.println("Aucun utilisateur connecte");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/front-profile.fxml"));
            Parent root = loader.load();
            FrontProfileController ctrl = loader.getController();
            ctrl.setUser(currentUser);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                    "/com/example/pijava_fluently/css/fluently.css").toExternalForm());
            Stage stage = (Stage) navUsername.getScene().getWindow();
            stage.setTitle("Fluently - Mon Profil");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSettings() {
        System.out.println("Parametres ouverts");
    }

    private void handleLogout() {

        if (currentUser != null) {
            try {
                userService.updateStatut(currentUser.getId(), "offline");
            } catch (SQLException e) {
                System.err.println("Statut non mis a jour : " + e.getMessage());
            }
        }
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                    "/com/example/pijava_fluently/css/fluently.css").toExternalForm());
            Stage stage = (Stage) navUsername.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Fluently - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}