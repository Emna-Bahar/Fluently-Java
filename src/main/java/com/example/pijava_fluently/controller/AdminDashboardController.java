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


    // ── Labels topbar ──────────────────────────────────────────────
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
    @FXML private VBox userProgressView;

    // Liste de toutes les vues pour le hideAll()
    private List<VBox> allViews = new ArrayList<>();

    @FXML
    public void initialize() {
        // Initialisation sécurisée de la liste des vues
        if (dashboardView != null) allViews.add(dashboardView);
        if (languesView != null) allViews.add(languesView);
        if (niveauxView != null) allViews.add(niveauxView);
        if (coursView != null) allViews.add(coursView);
        if (etudiantsView != null) allViews.add(etudiantsView);
        if (testsView != null) allViews.add(testsView);
        if (questionsView != null) allViews.add(questionsView);
        if (reponsesView != null) allViews.add(reponsesView);
        if (passagesView != null) allViews.add(passagesView);
        if (userProgressView != null) allViews.add(userProgressView);
        if (groupesView != null) allViews.add(groupesView);
        if (sessionsView != null) allViews.add(sessionsView);
        if (reservationsView != null) allViews.add(reservationsView);
        if (objectifsView != null) allViews.add(objectifsView);
        if (tachesView != null) allViews.add(tachesView);

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
        // UX: au premier clic sur "Objectifs", on ouvre aussi la page objectifs.
        boolean wasVisible = objectifsSubmenu != null && objectifsSubmenu.isVisible();
        toggle(objectifsSubmenu);
        if (!wasVisible) {
            showObjectifs();
        }
    }

    private void toggle(VBox submenu) {
        if (submenu == null) return;
        boolean nowVisible = !submenu.isVisible();
        submenu.setVisible(nowVisible);
        submenu.setManaged(nowVisible);
    }

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

    private void loadModuleInto(VBox container, String fxmlName) {
        if (container == null) return;
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

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION — afficher les vues
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void showDashboard() {
        hideAll();
        if (dashboardView != null) {
            dashboardView.setVisible(true);
            dashboardView.setManaged(true);
        }
        setTitle("Dashboard", "Administration › Dashboard");
    }

    @FXML
    private void showEtudiants() {
        hideAll();
        if (etudiantsView != null) {
            etudiantsView.setVisible(true);
            etudiantsView.setManaged(true);
        }
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

    @FXML
    private void showUserProgress() {
        hideAll();
        loadPage(userProgressView, "/com/example/pijava_fluently/fxml/user-progress.fxml");
        setTitle("Progression des Étudiants", "Administration › Langues › Progression");
    }

    // ── Tests ──────────────────────────────────────────────────────

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

    // ── Groupes / Sessions / Objectifs ─────────────────────────────

    @FXML
    private void showGroupes() {
        hideAll();
        if (groupesView != null) {
            groupesView.setVisible(true);
            groupesView.setManaged(true);
        }
        setTitle("Groupes", "Administration › Groupes");
    }

    @FXML
    private void showSessions() {
        hideAll();
        if (sessionsView != null) {
            sessionsView.setVisible(true);
            sessionsView.setManaged(true);
        }
        setTitle("Sessions", "Administration › Sessions");
    }

    @FXML
    private void showReservations() {
        hideAll();
        if (reservationsView != null) {
            reservationsView.setVisible(true);
            reservationsView.setManaged(true);
        }
        setTitle("Réservations", "Administration › Sessions › Réservations");
    }

  /*  @FXML
    private void showObjectifs() {
        hideAll();
        if (objectifsView != null) {
            objectifsView.setVisible(true);
            objectifsView.setManaged(true);
        }
        setTitle("Objectifs", "Administration › Objectifs");
    }

    @FXML
    private void showTaches() {
        hideAll();
        if (tachesView != null) {
            tachesView.setVisible(true);
            tachesView.setManaged(true);
        }
        setTitle("Tâches", "Administration › Objectifs › Tâches");
    }*/
  @FXML
  private void showObjectifs() {
      hideAll();
      loadPage(objectifsView, "/com/example/pijava_fluently/fxml/ObjectifAdmin-view.fxml");
      setTitle("Objectifs", "Administration › Objectifs");
  }

    @FXML
    private void showTaches() {
        hideAll();
        loadPage(tachesView, "/com/example/pijava_fluently/fxml/TacheAdmin-view.fxml");
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
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }
    }

    /** Met à jour le titre et le fil d'Ariane */
    private void setTitle(String title, String breadcrumb) {
        if (pageTitle != null) pageTitle.setText(title);
        if (pageBreadcrumb != null) pageBreadcrumb.setText(breadcrumb);
    }

    /**
     * Charge un FXML dans un VBox conteneur.
     * Appelé uniquement pour les pages Langue / Niveau / Cours
     * qui ont leur propre contrôleur.
     */
    private void loadPage(VBox container, String fxmlPath) {
        if (container == null) return;
        try {
            // Recharge si le contenu courant ne correspond pas deja a la page demandee.
            // Cela permet de remplacer les placeholders presents dans le FXML du dashboard.
            String loadedPath = (String) container.getProperties().get("loadedFxmlPath");
            if (!fxmlPath.equals(loadedPath)) {
                var resource = getClass().getResource(fxmlPath);
                if (resource == null) {
                    throw new IOException("Ressource introuvable: " + fxmlPath);
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
            showErrorInContainer(container, "Impossible de charger la page: " + fxmlPath);
            showError("Impossible de charger la page : " + fxmlPath);
        }
    }

    private void showErrorInContainer(VBox container, String msg) {
        Label errorLabel = new Label(msg);
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill:#B91C1C;-fx-font-size:13px;-fx-font-weight:bold;");

        VBox box = new VBox(10, new Label("Erreur de chargement"), errorLabel);
        box.setStyle("-fx-background-color:#FEF2F2;-fx-border-color:#FECACA;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:16;");

        container.getChildren().setAll(box);
        container.setVisible(true);
        container.setManaged(true);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }
}