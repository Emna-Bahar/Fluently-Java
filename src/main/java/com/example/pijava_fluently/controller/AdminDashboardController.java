package com.example.pijava_fluently.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;

public class AdminDashboardController {

    // ── Labels topbar ──────────────────────────────────────────────
    @FXML private Label pageTitle;
    @FXML private Label pageBreadcrumb;
    @FXML private Label adminName;
    @FXML private Label topbarUsername;

    // ── Stats dashboard ────────────────────────────────────────────
    @FXML private Label statTests;
    @FXML private Label statPassages;
    @FXML private Label statEtudiants;
    @FXML private Label statScore;
    @FXML private TableView<?> recentPassagesTable;
    @FXML private TableColumn<?,?> colEtudiant, colTest, colScore, colStatut, colDate;

    // ── Sous-menus sidebar ─────────────────────────────────────────
    @FXML private VBox languesSubmenu;
    @FXML private VBox testsSubmenu;
    @FXML private VBox sessionsSubmenu;
    @FXML private VBox objectifsSubmenu;

    // ── Toutes les vues ────────────────────────────────────────────
    @FXML private VBox dashboardView;
    @FXML private VBox languesView;
    @FXML private VBox niveauxView;
    @FXML private VBox coursView;
    @FXML private VBox etudiantsView;
    @FXML private VBox testsView;
    @FXML private VBox questionsView;
    @FXML private VBox reponsesView;
    @FXML private VBox passagesView;
    @FXML private VBox groupesView;
    @FXML private VBox sessionsView;
    @FXML private VBox reservationsView;
    @FXML private VBox objectifsView;
    @FXML private VBox tachesView;

    // Liste de toutes les vues pour le hideAll()
    private List<VBox> allViews;

    @FXML
    public void initialize() {
        allViews = List.of(
                dashboardView, languesView, niveauxView, coursView,
                etudiantsView, testsView, questionsView, reponsesView,
                passagesView, groupesView, sessionsView, reservationsView,
                objectifsView, tachesView
        );
        // Dashboard affiché par défaut
        showDashboard();
    }

    // ══════════════════════════════════════════════════════════════
    //  TOGGLE SOUS-MENUS
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void toggleLangues() {
        toggle(languesSubmenu);
    }

    @FXML
    private void toggleTests() {
        toggle(testsSubmenu);
    }

    @FXML
    private void toggleSessions() {
        toggle(sessionsSubmenu);
    }

    @FXML
    private void toggleObjectifs() {
        toggle(objectifsSubmenu);
    }

    private void toggle(VBox submenu) {
        boolean nowVisible = !submenu.isVisible();
        submenu.setVisible(nowVisible);
        submenu.setManaged(nowVisible);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — afficher les vues
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showDashboard() {
        hideAll();
        dashboardView.setVisible(true);
        dashboardView.setManaged(true);
        setTitle("Dashboard", "Administration › Dashboard");
    }

    @FXML
    private void showEtudiants() {
        hideAll();
        etudiantsView.setVisible(true);
        etudiantsView.setManaged(true);
        setTitle("Utilisateurs", "Administration › Utilisateurs");
    }

    // ── Langues ────────────────────────────────────────────────────

    @FXML
    private void showLangues() {
        hideAll();
        loadPage(languesView, "/com/example/pijava_fluently/fxml/langue-view.fxml");
        setTitle("Langues", "Administration › Langues");
    }

    @FXML
    private void showNiveaux() {
        hideAll();
        loadPage(niveauxView, "/com/example/pijava_fluently/fxml/niveau-view.fxml");
        setTitle("Niveaux", "Administration › Langues › Niveaux");
    }

    @FXML
    private void showCours() {
        hideAll();
        loadPage(coursView, "/com/example/pijava_fluently/fxml/cours-view.fxml");
        setTitle("Cours", "Administration › Langues › Cours");
    }

    // ── Tests ──────────────────────────────────────────────────────

    @FXML
    private void showTests() {
        hideAll();
        testsView.setVisible(true);
        testsView.setManaged(true);
        setTitle("Tests", "Administration › Tests");
    }

    @FXML
    private void showQuestions() {
        hideAll();
        questionsView.setVisible(true);
        questionsView.setManaged(true);
        setTitle("Questions", "Administration › Tests › Questions");
    }

    @FXML
    private void showReponses() {
        hideAll();
        reponsesView.setVisible(true);
        reponsesView.setManaged(true);
        setTitle("Réponses", "Administration › Tests › Réponses");
    }

    @FXML
    private void showPassages() {
        hideAll();
        passagesView.setVisible(true);
        passagesView.setManaged(true);
        setTitle("Passages de tests", "Administration › Tests › Passages");
    }

    // ── Groupes / Sessions / Objectifs ─────────────────────────────

    @FXML
    private void showGroupes() {
        hideAll();
        groupesView.setVisible(true);
        groupesView.setManaged(true);
        setTitle("Groupes", "Administration › Groupes");
    }

    @FXML
    private void showSessions() {
        hideAll();
        sessionsView.setVisible(true);
        sessionsView.setManaged(true);
        setTitle("Sessions", "Administration › Sessions");
    }

    @FXML
    private void showReservations() {
        hideAll();
        reservationsView.setVisible(true);
        reservationsView.setManaged(true);
        setTitle("Réservations", "Administration › Sessions › Réservations");
    }

    @FXML
    private void showObjectifs() {
        hideAll();
        objectifsView.setVisible(true);
        objectifsView.setManaged(true);
        setTitle("Objectifs", "Administration › Objectifs");
    }

    @FXML
    private void showTaches() {
        hideAll();
        tachesView.setVisible(true);
        tachesView.setManaged(true);
        setTitle("Tâches", "Administration › Objectifs › Tâches");
    }

    @FXML
    private void handleLogout() {
        // TODO : revenir à la page de login
        System.out.println("Déconnexion...");
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════

    /** Cache toutes les vues */
    private void hideAll() {
        for (VBox v : allViews) {
            v.setVisible(false);
            v.setManaged(false);
        }
    }

    /** Met à jour le titre et le fil d'Ariane */
    private void setTitle(String title, String breadcrumb) {
        pageTitle.setText(title);
        pageBreadcrumb.setText(breadcrumb);
    }

    /**
     * Charge un FXML dans un VBox conteneur.
     * Appelé uniquement pour les pages Langue / Niveau / Cours
     * qui ont leur propre contrôleur.
     */
    private void loadPage(VBox container, String fxmlPath) {
        try {
            // On recharge seulement si le conteneur est vide
            if (container.getChildren().isEmpty()) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(fxmlPath)
                );
                Node page = loader.load();
                VBox.setVgrow(page, Priority.ALWAYS);
                container.getChildren().add(page);
            }
            container.setVisible(true);
            container.setManaged(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible de charger la page : " + fxmlPath);
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
