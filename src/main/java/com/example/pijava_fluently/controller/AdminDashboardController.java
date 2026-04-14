package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.services.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
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

    // Groupes Management
    @FXML private TableView<Groupe> groupesTable;
    @FXML private TableColumn<Groupe, Integer> colGroupeId;
    @FXML private TableColumn<Groupe, String> colGroupeNom;
    @FXML private TableColumn<Groupe, String> colGroupeDescription;
    @FXML private TableColumn<Groupe, Integer> colGroupeCapacite;
    @FXML private TableColumn<Groupe, String> colGroupeStatut;
    @FXML private TableColumn<Groupe, String> colGroupeDate;
    @FXML private TableColumn<Groupe, Void> colGroupeActions;
    @FXML private TextField searchField;
    @FXML private Button btnAddGroup;
    @FXML private Label lblResultCount;
    @FXML private StackPane groupFormOverlay;
    @FXML private StackPane groupFormHost;

    private GroupService groupService;
    private LangueService langueService;
    private NiveauService niveauService;
    private ObservableList<Groupe> groupesList;
    private ObservableList<Groupe> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (adminName != null)      adminName.setText("Admin");
        if (topbarUsername != null) topbarUsername.setText("Admin");
        
        groupService = new GroupService();
        langueService = new LangueService();
        niveauService = new NiveauService();
        groupesList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();
        
        setupGroupesTable();
        loadStats();
        showDashboard();
    }

    // ── GROUPES CRUD ─────────────────────────────────────

    private void setupGroupesTable() {
        if (groupesTable == null) return;

        colGroupeId.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        
        colGroupeNom.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getNom()));
        
        colGroupeDescription.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            if (desc != null && desc.length() > 60) {
                desc = desc.substring(0, 57) + "...";
            }
            return new SimpleStringProperty(desc);
        });
        
        colGroupeCapacite.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getCapacite()).asObject());
        
        colGroupeStatut.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatut()));
        
        // Style statut cells with colors
        colGroupeStatut.setCellFactory(col -> new TableCell<Groupe, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);
                    String style = "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
                    switch (statut.toLowerCase()) {
                        case "actif":
                            style += " -fx-background-color: #d4edda; -fx-text-fill: #155724;";
                            break;
                        case "inactif":
                            style += " -fx-background-color: #f8d7da; -fx-text-fill: #721c24;";
                            break;
                        case "complet":
                            style += " -fx-background-color: #fff3cd; -fx-text-fill: #856404;";
                            break;
                    }
                    setStyle(style);
                }
            }
        });
        
        colGroupeDate.setCellValueFactory(cellData -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String date = cellData.getValue().getDateCreation() != null ? 
                sdf.format(cellData.getValue().getDateCreation()) : "—";
            return new SimpleStringProperty(date);
        });

        colGroupeActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑️");
            private final HBox pane = new HBox(6, btnEdit, btnDelete);

            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-cursor: hand; -fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 4; -fx-font-size: 12px;");
                btnDelete.setStyle("-fx-cursor: hand; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 4; -fx-font-size: 12px;");
                
                btnEdit.setOnAction(event -> {
                    Groupe groupe = getTableView().getItems().get(getIndex());
                    handleEditGroup(groupe);
                });
                
                btnDelete.setOnAction(event -> {
                    Groupe groupe = getTableView().getItems().get(getIndex());
                    handleDeleteGroup(groupe);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        groupesTable.setItems(filteredList);
    }

    @FXML
    private void handleAddGroup() {
        openGroupForm(null);
    }

    private void handleEditGroup(Groupe groupe) {
        openGroupForm(groupe);
    }

    private void handleDeleteGroup(Groupe groupe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le groupe");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer le groupe \"" + groupe.getNom() + "\" ?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                groupService.supprimer(groupe.getId());
                loadGroupes();
                showSuccess("Groupe supprimé avec succès !");
            } catch (SQLException e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void openGroupForm(Groupe groupe) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/group-form.fxml")
            );
            Parent form = loader.load();
            
            GroupFormController controller = loader.getController();
            controller.setGroupService(groupService);
            controller.setParentController(this);
            controller.setOnCloseRequest(this::closeInlineGroupForm);
            
            if (groupe != null) {
                controller.setGroupe(groupe);
            }

            if (groupFormHost != null && groupFormOverlay != null) {
                groupFormHost.getChildren().setAll(form);
                groupFormOverlay.setVisible(true);
                groupFormOverlay.setManaged(true);
            }

        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du formulaire : " + e.getMessage());
        }
    }

    private void closeInlineGroupForm() {
        if (groupFormHost != null) {
            groupFormHost.getChildren().clear();
        }
        if (groupFormOverlay != null) {
            groupFormOverlay.setVisible(false);
            groupFormOverlay.setManaged(false);
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) searchField.clear();
        applyFilters();
    }

    private void applyFilters() {
        filteredList.clear();
        
        String searchTerm = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        
        for (Groupe groupe : groupesList) {
            boolean matches = true;
            
            // Search filter
            if (!searchTerm.isEmpty()) {
                matches = groupe.getNom().toLowerCase().contains(searchTerm) ||
                         (groupe.getDescription() != null && groupe.getDescription().toLowerCase().contains(searchTerm));
            }
            
            if (matches) {
                filteredList.add(groupe);
            }
        }
        
        updateResultCount();
    }
    
    private void updateResultCount() {
        if (lblResultCount != null) {
            int count = filteredList.size();
            lblResultCount.setText(count + " groupe" + (count > 1 ? "s" : ""));
        }
    }

    @FXML
    private void handleRefreshGroups() {
        loadGroupes();
    }

    public void loadGroupes() {
        try {
            List<Groupe> groupes = groupService.recuperer();
            groupesList.setAll(groupes);
            applyFilters();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des groupes : " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
        setActive(navTests);
    }

    @FXML private void showQuestions() {
        showView(questionsView, "Gestion des Questions", "Administration › Tests › Questions");
        setActive(navQuestions);
    }

    @FXML private void showReponses() {
        showView(reponsesView, "Gestion des Réponses", "Administration › Tests › Réponses");
        setActive(navReponses);
    }

    @FXML private void showPassages() {
        showView(passagesView, "Passages de Tests", "Administration › Tests › Passages");
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
        loadGroupes();
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
}
