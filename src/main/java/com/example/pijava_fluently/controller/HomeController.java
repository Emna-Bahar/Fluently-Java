package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.models.User;
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
    @FXML private Label     navUsername;
    @FXML private HBox      navUserPill;

    @FXML private Button btnAccueil, btnLangues, btnTests, btnGroupes, btnSessions, btnObjectifs;

    private ContextMenu userMenu;
    private User currentUser;
    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        createUserMenu();
        loadView("home-content.fxml");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (navUsername != null && user != null)
            navUsername.setText(user.getPrenom() + " " + user.getNom());
    }

    // ── USER MENU ────────────────────────────────────────────────────────────

    private void createUserMenu() {
        userMenu = new ContextMenu();
        MenuItem profile  = new MenuItem("👤  Mon Profil");
        MenuItem settings = new MenuItem("⚙️  Paramètres");
        MenuItem logout   = new MenuItem("⏻  Déconnexion");
        profile.setOnAction(e  -> showProfile());
        settings.setOnAction(e -> showSettings());
        logout.setOnAction(e   -> handleLogout());
        userMenu.getItems().addAll(profile, settings, new SeparatorMenuItem(), logout);
    }

    @FXML
    private void showUserMenu(MouseEvent event) {
        if (userMenu != null && navUserPill != null)
            userMenu.show(navUserPill, event.getScreenX(), event.getScreenY() + 8);
    }

    // ── NAVIGATION ───────────────────────────────────────────────────────────

    @FXML public void showAccueil()   { loadView("home-content.fxml"); setActive(btnAccueil); }
    @FXML public void showLangues()   { loadView("langues.fxml");      setActive(btnLangues); }
    @FXML public void showMesTests()  { loadView("mes-tests.fxml");    setActive(btnTests); }
    @FXML public void showGroupes()   { loadView("groupes.fxml");      setActive(btnGroupes); }
    @FXML public void showSessions()  { loadView("sessions.fxml");     setActive(btnSessions); }
    @FXML public void showObjectifs() { loadView("objectifs.fxml");    setActive(btnObjectifs); }

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
            System.err.println("Impossible de charger : " + fxmlFile);
        }
    }

    private void setActive(Button btn) {
        Button[] all = {btnAccueil, btnLangues, btnTests, btnGroupes, btnSessions, btnObjectifs};
        for (Button b : all) if (b != null) b.getStyleClass().remove("nav-link-active");
        if (btn != null) btn.getStyleClass().add("nav-link-active");
    }

    // ── PROFILE (independent front-office page) ───────────────────────────────

    private void showProfile() {
        if (currentUser == null) return;
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showSettings() { System.out.println("Paramètres"); }

    // ── LOGOUT: set statut = offline, then go to login ────────────────────────

    private void handleLogout() {
        if (currentUser != null) {
            try {
                userService.updateStatut(currentUser.getId(), "offline");
            } catch (SQLException e) {
                System.err.println("Impossible de mettre à jour le statut : " + e.getMessage());
            }
        }
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
        } catch (IOException e) { e.printStackTrace(); }
    }
}
