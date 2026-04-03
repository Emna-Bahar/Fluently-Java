package com.example.pijava_fluently.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    // Nav buttons
    @FXML private Button navEtudiants;
    @FXML private Button navLangues;
    @FXML private Button navTestsToggle;
    @FXML private Button navTests;
    @FXML private Button navQuestions;
    @FXML private Button navReponses;
    @FXML private Button navPassages;
    @FXML private Button navGroupes;
    @FXML private Button navSessionsToggle;
    @FXML private Button navSessionsList;
    @FXML private Button navReservations;
    @FXML private Button navObjectifsToggle;
    @FXML private Button navObjectifsList;
    @FXML private Button navTaches;

    // Submenus
    @FXML private VBox testsSubmenu;
    @FXML private VBox sessionsSubmenu;
    @FXML private VBox objectifsSubmenu;

    // Topbar
    @FXML private Label pageTitle;
    @FXML private Label pageBreadcrumb;
    @FXML private Label topbarUsername;
    @FXML private Label adminName;

    // Stats
    @FXML private Label statTests;
    @FXML private Label statPassages;
    @FXML private Label statEtudiants;
    @FXML private Label statScore;

    // Table
    @FXML private TableView<?> recentPassagesTable;
    @FXML private TableColumn<?, ?> colEtudiant;
    @FXML private TableColumn<?, ?> colTest;
    @FXML private TableColumn<?, ?> colScore;
    @FXML private TableColumn<?, ?> colStatut;
    @FXML private TableColumn<?, ?> colDate;

    // Views
    @FXML private VBox dashboardView;
    @FXML private VBox testsView;
    @FXML private VBox questionsView;
    @FXML private VBox reponsesView;
    @FXML private VBox passagesView;
    @FXML private VBox etudiantsView;
    @FXML private VBox languesView;
    @FXML private VBox groupesView;
    @FXML private VBox sessionsView;
    @FXML private VBox reservationsView;
    @FXML private VBox objectifsView;
    @FXML private VBox tachesView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (adminName != null)      adminName.setText("Admin");
        if (topbarUsername != null) topbarUsername.setText("Admin");
        loadStats();
        // Afficher le dashboard par défaut
        showDashboard();
    }

    // ── TOGGLE SUBMENUS ─────────────────────────────────

    @FXML private void toggleTests() {
        toggle(testsSubmenu, navTestsToggle, "📝  Tests");
    }

    @FXML private void toggleSessions() {
        toggle(sessionsSubmenu, navSessionsToggle, "📅  Sessions");
    }

    @FXML private void toggleObjectifs() {
        toggle(objectifsSubmenu, navObjectifsToggle, "🎯  Objectifs");
    }

    private void toggle(VBox submenu, Button toggleBtn, String baseText) {
        if (submenu == null) return;
        boolean open = submenu.isVisible();
        submenu.setVisible(!open);
        submenu.setManaged(!open);
        if (toggleBtn != null) {
            toggleBtn.setText(open ? baseText + "  ▾" : baseText + "  ▴");
        }
    }

    // ── NAVIGATION ───────────────────────────────────────

    @FXML private void showDashboard() {
        showView(dashboardView, "Dashboard", "Administration › Dashboard");
    }

    @FXML private void showTests() {
        showView(testsView, "Gestion des Tests", "Administration › Tests › Tests");
        loadModuleInto(testsView, "tests.fxml");
        setActive(navTests);
    }

    @FXML private void showQuestions() {
        showView(questionsView, "Gestion des Questions", "Administration › Tests › Questions");
        loadModuleInto(questionsView, "questions.fxml");
        setActive(navQuestions);
    }

    @FXML private void showReponses() {
        showView(reponsesView, "Gestion des Réponses", "Administration › Tests › Réponses");
        loadModuleInto(reponsesView, "reponses.fxml");
        setActive(navReponses);
    }

    @FXML private void showPassages() {
        showView(passagesView, "Passages de Tests", "Administration › Tests › Passages");
        loadModuleInto(passagesView, "passages.fxml");
        setActive(navPassages);
    }

    @FXML private void showEtudiants() {
        showView(etudiantsView, "Gestion des Utilisateurs", "Administration › Utilisateurs");
        setActive(navEtudiants);
    }

    @FXML private void showLangues() {
        showView(languesView, "Gestion des Langues", "Administration › Langues");
        setActive(navLangues);
    }

    @FXML private void showGroupes() {
        showView(groupesView, "Gestion des Groupes", "Administration › Groupes");
        setActive(navGroupes);
    }

    @FXML private void showSessions() {
        showView(sessionsView, "Gestion des Sessions", "Administration › Sessions");
        setActive(navSessionsList);
    }

    @FXML private void showReservations() {
        showView(reservationsView, "Réservations", "Administration › Sessions › Réservations");
        setActive(navReservations);
    }

    @FXML private void showObjectifs() {
        showView(objectifsView, "Gestion des Objectifs", "Administration › Objectifs");
        setActive(navObjectifsList);
    }

    @FXML private void showTaches() {
        showView(tachesView, "Gestion des Tâches", "Administration › Objectifs › Tâches");
        setActive(navTaches);
    }

    @FXML private void handleLogout() {
        navigateToLogin();
    }

    // ── HELPERS ──────────────────────────────────────────

    private void showView(VBox view, String title, String breadcrumb) {
        VBox[] all = { dashboardView, testsView, questionsView, reponsesView,
                passagesView, etudiantsView, languesView, groupesView,
                sessionsView, reservationsView, objectifsView, tachesView };
        for (VBox v : all) {
            if (v != null) { v.setVisible(false); v.setManaged(false); }
        }
        if (view != null) { view.setVisible(true); view.setManaged(true); }
        if (pageTitle != null)      pageTitle.setText(title);
        if (pageBreadcrumb != null) pageBreadcrumb.setText(breadcrumb);
    }

    private void setActive(Button active) {
        Button[] all = { navEtudiants, navLangues, navTests, navQuestions, navReponses,
                navPassages, navGroupes, navSessionsList, navReservations,
                navObjectifsList, navTaches };
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("nav-active");
        }
        if (active != null) active.getStyleClass().add("nav-active");
    }

    private void loadStats() {
        if (statTests != null)     statTests.setText("4");
        if (statPassages != null)  statPassages.setText("2");
        if (statEtudiants != null) statEtudiants.setText("12");
        if (statScore != null)     statScore.setText("78%");
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
            Stage stage = (Stage) pageTitle.getScene().getWindow();
            stage.setTitle("Fluently - Connexion");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadModuleInto(VBox container, String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/" + fxmlName)
            );
            Node content = loader.load();
            container.getChildren().setAll(content);
            VBox.setVgrow(content, javafx.scene.layout.Priority.ALWAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}