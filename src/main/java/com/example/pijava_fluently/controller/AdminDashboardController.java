package com.example.pijava_fluently.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    // ── Boutons navigation sidebar ─────────────────────────────────
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
    @FXML private Button navUserProgress;

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

    // ── Sous-menus sidebar (accordéon) ─────────────────────────────
    @FXML private VBox languesSubmenu;
    @FXML private VBox testsSubmenu;
    @FXML private VBox sessionsSubmenu;
    @FXML private VBox objectifsSubmenu;

    // ── Conteneurs de vues (StackPane interne) ─────────────────────
    @FXML private VBox dashboardView;

    // Langues / Cours / Niveau / UserProgress
    @FXML private VBox languesView;
    @FXML private VBox niveauxView;
    @FXML private VBox coursView;
    @FXML private VBox userProgressView;

    // Utilisateurs
    @FXML private VBox etudiantsView;

    // Tests (module principal)
    @FXML private VBox testsView;
    @FXML private VBox questionsView;
    @FXML private VBox reponsesView;
    @FXML private VBox passagesView;

    // Groupes / Sessions / Réservations
    @FXML private VBox groupesView;
    @FXML private VBox sessionsView;
    @FXML private VBox reservationsView;

    // Objectifs / Tâches
    @FXML private VBox objectifsView;
    @FXML private VBox tachesView;

    // Liste complète pour hideAll()
    private final List<VBox> allViews = new ArrayList<>();

    // ══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Enregistrement sécurisé de toutes les vues
        addIfNotNull(dashboardView);
        addIfNotNull(languesView);
        addIfNotNull(niveauxView);
        addIfNotNull(coursView);
        addIfNotNull(userProgressView);
        addIfNotNull(etudiantsView);
        addIfNotNull(testsView);
        addIfNotNull(questionsView);
        addIfNotNull(reponsesView);
        addIfNotNull(passagesView);
        addIfNotNull(groupesView);
        addIfNotNull(sessionsView);
        addIfNotNull(reservationsView);
        addIfNotNull(objectifsView);
        addIfNotNull(tachesView);

        // Dashboard affiché par défaut
        showDashboard();
    }

    private void addIfNotNull(VBox v) {
        if (v != null) allViews.add(v);
    }

    // ══════════════════════════════════════════════════════════════
    //  TOGGLE SOUS-MENUS (accordéon sidebar)
    // ══════════════════════════════════════════════════════════════

    @FXML private void toggleLangues()  { toggle(languesSubmenu);  }
    @FXML private void toggleTests()    { toggle(testsSubmenu);    }
    @FXML private void toggleSessions() { toggle(sessionsSubmenu); }

    @FXML
    private void toggleObjectifs() {
        boolean wasVisible = objectifsSubmenu != null && objectifsSubmenu.isVisible();
        toggle(objectifsSubmenu);
        // Ouvre la page objectifs automatiquement au premier clic
        if (!wasVisible) showObjectifs();
    }

    private void toggle(VBox submenu) {
        if (submenu == null) return;
        boolean nowVisible = !submenu.isVisible();
        submenu.setVisible(nowVisible);
        submenu.setManaged(nowVisible);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — DASHBOARD
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showDashboard() {
        hideAll();
        show(dashboardView);
        setTitle("Dashboard", "Administration › Dashboard");
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — UTILISATEURS / ÉTUDIANTS
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showEtudiants() {
        hideAll();
        // Si une vue statique est déclarée dans le FXML principal, on l'affiche.
        // Sinon on charge le FXML dédié.
        if (etudiantsView != null && !etudiantsView.getChildren().isEmpty()) {
            show(etudiantsView);
        } else {
            loadPage(etudiantsView, "/com/example/pijava_fluently/fxml/etudiants-view.fxml");
        }
        setTitle("Utilisateurs", "Administration › Utilisateurs");
        setActive(navEtudiants);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — LANGUES / NIVEAUX / COURS / USER PROGRESS
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showLangues() {
        hideAll();
        loadPage(languesView, "/com/example/pijava_fluently/fxml/langue-view.fxml");
        setTitle("Langues", "Administration › Langues");
        setActive(navLangues);
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

    @FXML
    private void showUserProgress() {
        hideAll();
        loadPage(userProgressView, "/com/example/pijava_fluently/fxml/user-progress.fxml");
        setTitle("Progression des Étudiants", "Administration › Langues › Progression");
        setActive(navUserProgress);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — TESTS / QUESTIONS / RÉPONSES / PASSAGES
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showTests() {
        hideAll();
        loadPage(testsView, "/com/example/pijava_fluently/fxml/tests.fxml");
        setTitle("Gestion des Tests", "Administration › Tests › Tests");
        setActive(navTests);
    }

    @FXML
    private void showQuestions() {
        hideAll();
        loadPage(questionsView, "/com/example/pijava_fluently/fxml/questions.fxml");
        setTitle("Gestion des Questions", "Administration › Tests › Questions");
        setActive(navQuestions);
    }

    @FXML
    private void showReponses() {
        hideAll();
        loadPage(reponsesView, "/com/example/pijava_fluently/fxml/reponses.fxml");
        setTitle("Gestion des Réponses", "Administration › Tests › Réponses");
        setActive(navReponses);
    }

    @FXML
    private void showPassages() {
        hideAll();
        loadPage(passagesView, "/com/example/pijava_fluently/fxml/passages.fxml");
        setTitle("Passages de Tests", "Administration › Tests › Passages");
        setActive(navPassages);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — GROUPES / SESSIONS / RÉSERVATIONS
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showGroupes() {
        hideAll();
        show(groupesView);
        setTitle("Groupes", "Administration › Groupes");
        setActive(navGroupes);
    }

    @FXML
    private void showSessions() {
        hideAll();
        show(sessionsView);
        setTitle("Sessions", "Administration › Sessions");
        setActive(navSessionsList);
    }

    @FXML
    private void showReservations() {
        hideAll();
        show(reservationsView);
        setTitle("Réservations", "Administration › Sessions › Réservations");
        setActive(navReservations);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — OBJECTIFS / TÂCHES
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showObjectifs() {
        hideAll();
        loadPage(objectifsView, "/com/example/pijava_fluently/fxml/ObjectifAdmin-view.fxml");
        setTitle("Objectifs", "Administration › Objectifs");
        setActive(navObjectifsList);
    }

    @FXML
    private void showTaches() {
        hideAll();
        loadPage(tachesView, "/com/example/pijava_fluently/fxml/TacheAdmin-view.fxml");
        setTitle("Tâches", "Administration › Objectifs › Tâches");
        setActive(navTaches);
    }

    // ══════════════════════════════════════════════════════════════
    //  DÉCONNEXION
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/login-view.fxml")
            );
            Node loginPage = loader.load();
            // Récupérer la scène depuis n'importe quel nœud visible
            if (dashboardView != null && dashboardView.getScene() != null) {
                dashboardView.getScene().setRoot((javafx.scene.Parent) loginPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Déconnexion — retour login impossible : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ══════════════════════════════════════════════════════════════

    /** Cache toutes les vues enregistrées. */
    private void hideAll() {
        for (VBox v : allViews) {
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }
    }

    /** Rend visible et géré un VBox. */
    private void show(VBox view) {
        if (view != null) {
            view.setVisible(true);
            view.setManaged(true);
        }
    }

    /** Met à jour le titre et le fil d'Ariane. */
    private void setTitle(String title, String breadcrumb) {
        if (pageTitle != null)      pageTitle.setText(title);
        if (pageBreadcrumb != null) pageBreadcrumb.setText(breadcrumb);
    }

    /**
     * Met en surbrillance le bouton actif dans la sidebar.
     * Retire la classe CSS "nav-active" de tous les boutons, puis l'ajoute au bouton actif.
     */
    private void setActive(Button active) {
        Button[] all = {
                navEtudiants, navLangues, navTests, navQuestions,
                navReponses, navPassages, navGroupes, navSessionsList,
                navReservations, navObjectifsList, navTaches, navUserProgress
        };
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("nav-active");
        }
        if (active != null) active.getStyleClass().add("nav-active");
    }

    /**
     * Charge un FXML dans un conteneur VBox.
     * Utilise un cache par chemin pour éviter de recharger inutilement.
     * Si le FXML est introuvable, affiche un message d'erreur inline.
     *
     * @param container  Le VBox cible déclaré dans le FXML principal.
     * @param fxmlPath   Chemin absolu de la ressource FXML à charger.
     */
    private void loadPage(VBox container, String fxmlPath) {
        if (container == null) return;

        try {
            // Cache : on ne recharge pas si le même FXML est déjà chargé
            String loadedPath = (String) container.getProperties().get("loadedFxmlPath");
            if (!fxmlPath.equals(loadedPath)) {
                var resource = getClass().getResource(fxmlPath);
                if (resource == null) {
                    throw new IOException("Ressource FXML introuvable : " + fxmlPath);
                }
                FXMLLoader loader = new FXMLLoader(resource);
                Node page = loader.load();
                VBox.setVgrow(page, Priority.ALWAYS);
                container.getChildren().setAll(page);
                container.getProperties().put("loadedFxmlPath", fxmlPath);
            }
            container.setVisible(true);
            container.setManaged(true);

        } catch (Exception e) {
            e.printStackTrace();
            showErrorInContainer(container,
                    "Impossible de charger : " + fxmlPath + "\n" + e.getMessage());
        }
    }

    /**
     * Affiche un message d'erreur stylisé directement dans le conteneur,
     * sans bloquer l'UI avec une boîte de dialogue.
     */
    private void showErrorInContainer(VBox container, String msg) {
        Label title = new Label("⚠ Erreur de chargement");
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#991B1B;");

        Label detail = new Label(msg);
        detail.setWrapText(true);
        detail.setStyle("-fx-font-size:12px;-fx-text-fill:#B91C1C;");

        VBox errorBox = new VBox(8, title, detail);
        errorBox.setStyle(
                "-fx-background-color:#FEF2F2;" +
                        "-fx-border-color:#FECACA;" +
                        "-fx-border-radius:10;" +
                        "-fx-background-radius:10;" +
                        "-fx-padding:20;"
        );

        container.getChildren().setAll(errorBox);
        container.setVisible(true);
        container.setManaged(true);
    }
}