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
import javafx.scene.layout.VBox;
import javafx.collections.ObservableList;
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
    @FXML private VBox rootPane;
    private ContextMenu userMenu;
    private static int currentUserId = -1;
    private static String currentUserRole = "";

    // Méthode pour définir l'utilisateur connecté (appelée après login)
    public static void setCurrentUser(int userId, String role) {
        currentUserId = userId;
        currentUserRole = role;
    }

    // Méthode pour récupérer l'ID de l'utilisateur connecté
    private int getCurrentUserId() {
        if (currentUserId == -1) {
            // Valeur par défaut pour le développement
            return 4; // ID du professeur par défaut
        }
        return currentUserId;
    }
    @FXML
    private void toggleTheme() {
        Scene scene = rootPane.getScene();
        ObservableList<String> sheets = scene.getStylesheets();
        String darkCss = getClass().getResource(
                "/com/example/pijava_fluently/css/dark.css").toExternalForm();

        if (sheets.contains(darkCss)) {
            sheets.remove(darkCss);
            btnTheme.setText("🌙 Mode sombre");
        } else {
            sheets.add(darkCss);
            btnTheme.setText("☀️ Mode clair");
        }
    }
    // Méthode pour afficher les erreurs
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (navUsername != null) navUsername.setText("Emna");
        createUserMenu();
        loadView("home-content.fxml");
    }

    private void createUserMenu() {
        userMenu = new ContextMenu();
        MenuItem profile  = new MenuItem("👤  Mon Profil");
        MenuItem settings = new MenuItem("⚙️  Paramètres");
        SeparatorMenuItem separator = new SeparatorMenuItem();
        MenuItem logout   = new MenuItem("⏻  Déconnexion");
        profile.setOnAction(e  -> showProfile());
        settings.setOnAction(e -> showSettings());
        logout.setOnAction(e   -> handleLogout());
        userMenu.getItems().addAll(profile, settings, separator, logout);
    }

    @FXML
    private void showUserMenu(MouseEvent event) {
        if (userMenu != null && navUserPill != null)
            userMenu.show(navUserPill, event.getScreenX(), event.getScreenY() + 8);
    }

    // ── Navigation ────────────────────────────────────────────────

    @FXML public void showAccueil()   { loadView("home-content.fxml");       setActiveButton(btnAccueil);   }
    @FXML public void showLangues()   { loadView("langues.fxml");             setActiveButton(btnLangues);   }
    @FXML public void showMesTests()  { loadView("mes-tests.fxml");           setActiveButton(btnTests);     }
    @FXML public void showGroupes()   { loadView("groupes.fxml");             setActiveButton(btnGroupes);   }

    @FXML
    public void showSessions() {
        var url = getClass().getResource(
                "/com/example/pijava_fluently/fxml/session-etudiant-view.fxml");
        if (url == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Node content = loader.load();
            Sessionetudiantcontroller ctrl = loader.getController();
            ctrl.setUserId(3); // ← votre ID étudiant
            contentArea.getChildren().setAll(content);
            setActiveButton(btnSessions);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    public void showSessionsProf() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/session-prof-view.fxml")
            );
            Node content = loader.load();
            Sessionprofcontroller controller = loader.getController();
            controller.setProfId(getCurrentUserId()); // appelé APRÈS load()
            contentArea.getChildren().setAll(content);
            setActiveButton(btnSessions);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void showObjectifs() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/Objectif-view.fxml"));
            Node view = loader.load();
            setContent(view);
            setActiveButton(btnObjectifs);
        } catch (IOException e) {
            System.err.println("❌ Impossible de charger : Objectif-view.fxml");
            e.printStackTrace();
        }
    }

    // ── Contenu central ───────────────────────────────────────────

    public void setContent(Node view) {
        if (contentArea != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        }
    }

    // ── Helpers privés ────────────────────────────────────────────

    private void loadView(String fxmlName) {
        var url = getClass().getResource("/com/example/pijava_fluently/fxml/" + fxmlName);
        System.out.println("URL résolue : " + url);
        if (url == null) {
            new Alert(Alert.AlertType.ERROR, "FXML introuvable : " + fxmlName, ButtonType.OK).showAndWait();
            return;
        }
        try {
            Node content = new FXMLLoader(url).load();
            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur chargement : " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void setActiveButton(Button activeBtn) {
        if (activeBtn == null) return;
        Button[] all = {btnAccueil, btnLangues, btnTests, btnGroupes, btnSessions, btnObjectifs};
        for (Button b : all) if (b != null) b.getStyleClass().remove("nav-link-active");
        activeBtn.getStyleClass().add("nav-link-active");
    }

    // ── Actions menu utilisateur ──────────────────────────────────

    private void showProfile()  { System.out.println("Profil ouvert"); }
    private void showSettings() { System.out.println("Paramètres ouverts"); }
    private void handleLogout() { navigateToLogin(); }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm());
            Stage stage = (Stage) navUsername.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Fluently - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}