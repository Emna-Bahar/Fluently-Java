package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.services.*;
import java.text.SimpleDateFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    // ============================================================
    // 1. BOUTONS NAVIGATION SIDEBAR
    // ============================================================
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

    // ============================================================
    // 2. LABELS TOPBAR
    // ============================================================
    @FXML private Label pageTitle;
    @FXML private Label pageBreadcrumb;
    @FXML private Label adminName;
    @FXML private Label topbarUsername;
    @FXML private HBox topbarUserPill;

    // ============================================================
    // 3. STATS DASHBOARD
    // ============================================================
    @FXML private Label statTests;
    @FXML private Label statPassages;
    @FXML private Label statEtudiants;
    @FXML private Label statScore;
    @FXML private TableView<?> recentPassagesTable;
    @FXML private TableColumn<?,?> colEtudiant, colTest, colScore, colStatut, colDate;

    // ============================================================
    // 4. SOUS-MENUS SIDEBAR (ACCORDÉON)
    // ============================================================
    @FXML private VBox languesSubmenu;
    @FXML private VBox testsSubmenu;
    @FXML private VBox sessionsSubmenu;
    @FXML private VBox objectifsSubmenu;

    // ============================================================
    // 5. CONTENEURS DE VUES
    // ============================================================
    @FXML private VBox dashboardView;
    @FXML private VBox languesView;
    @FXML private VBox niveauxView;
    @FXML private VBox coursView;
    @FXML private VBox userProgressView;
    @FXML private VBox etudiantsView;
    @FXML private VBox testsView;
    @FXML private VBox questionsView;
    @FXML private VBox reponsesView;
    @FXML private VBox passagesView;
    @FXML private VBox groupesView;
    @FXML private VBox sessionsView;
    // Groups table fields
    @FXML private TableView<Groupe> groupesTable;
    @FXML private TableColumn<Groupe, Integer> colGroupeId, colGroupeCapacite;
    @FXML private TableColumn<Groupe, String> colGroupeNom, colGroupeDescription, colGroupeStatut, colGroupeDate;
    @FXML private TableColumn<Groupe, Void> colGroupeActions;
    @FXML private TextField searchField;
    @FXML private Label lblResultCount;
    @FXML private StackPane groupFormOverlay;
    @FXML private StackPane groupFormHost;
    @FXML private VBox reservationsView;
    @FXML private VBox objectifsView;
    @FXML private VBox tachesView;
    @FXML private VBox profileView;

    // ============================================================
    // 6. TABLEAU UTILISATEURS (module de l'autre personne)
    // ============================================================
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUserAvatar, colUserEmail, colUserStatut, colUserRoles;
    @FXML private TableColumn<User, Void> colUserActions;
    @FXML private TextField fieldSearch;
    @FXML private Label userCountLabel;

    // ============================================================
    // 7. PROFIL INLINE (module de l'autre personne)
    // ============================================================
    @FXML private Label heroInitials, heroFullName, heroRoleBadge, heroStatusBadge, heroEmail, heroId;
    @FXML private TextField fieldPrenom, fieldNom, fieldEmail;
    @FXML private ComboBox<String> comboStatut, comboRole;
    @FXML private PasswordField fieldNewPassword, fieldConfirmPassword;
    @FXML private HBox statusRoleRow;
    @FXML private VBox pwdStrengthBox;
    @FXML private Label pwdStrengthLabel, profileMessage;
    @FXML private Region bar1, bar2, bar3, bar4;

    // ============================================================
    // 8. SERVICES & LISTES
    // ============================================================
    private final UserService userService = new UserService();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private User currentUser = null;
    private User profiledUser = null;
    // Groups services
    private GroupService groupService;
    private LangueService langueService;
    private NiveauService niveauService;
    private ObservableList<Groupe> groupesList;
    private ObservableList<Groupe> filteredList;
    private ContextMenu adminUserMenu;

    // Liste complète des vues pour hideAll()
    private final List<VBox> allViews = new ArrayList<>();

    // ============================================================
    // 9. INITIALISATION
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Enregistrement des vues
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
        addIfNotNull(profileView);

        // Initialisation groupes
        groupService = new GroupService();
        langueService = new LangueService();
        niveauService = new NiveauService();
        groupesList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();
        setupGroupesTable();

        // Initialisation du tableau utilisateurs
        initUsersTable();
        loadStats();
        createAdminUserMenu();

        // Setup profil
        if (pwdStrengthBox != null) {
            pwdStrengthBox.setVisible(false);
            pwdStrengthBox.setManaged(false);
        }
        if (profileMessage != null) {
            profileMessage.setVisible(false);
            profileMessage.setManaged(false);
        }
        setupPasswordStrengthListener();

        // Dashboard par défaut
        showDashboard();
    }

    private void addIfNotNull(VBox v) {
        if (v != null) allViews.add(v);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        String name = user.getPrenom() + " " + user.getNom();
        if (adminName != null) adminName.setText(name);
        if (topbarUsername != null) topbarUsername.setText(name);
    }

    public void showEtudiantsTab() {
        showEtudiants();
    }

    // ============================================================
    // 10. ADMIN TOPBAR MENU
    // ============================================================

    private void createAdminUserMenu() {
        adminUserMenu = new ContextMenu();
        MenuItem profile = new MenuItem("👤 Mon Profil");
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem logout = new MenuItem("⏻ Déconnexion");
        profile.setOnAction(e -> {
            if (currentUser != null) openProfileView(currentUser);
        });
        logout.setOnAction(e -> handleLogout());
        adminUserMenu.getItems().addAll(profile, sep, logout);
    }

    @FXML
    private void showAdminUserMenu(MouseEvent event) {
        if (adminUserMenu != null && topbarUserPill != null) {
            adminUserMenu.show(topbarUserPill, event.getScreenX(), event.getScreenY() + 8);
        }
    }

    // ============================================================
    // 11. PROFIL INLINE
    // ============================================================

    private void openProfileView(User user) {
        this.profiledUser = user;
        populateProfileView(user);
        showView(profileView, "Profil Utilisateur",
                "Administration › Utilisateurs › " + user.getPrenom() + " " + user.getNom());
    }

    private void populateProfileView(User user) {
        String initials = initials(user.getPrenom(), user.getNom());
        if (heroInitials != null) heroInitials.setText(initials);
        if (heroFullName != null) heroFullName.setText(user.getPrenom() + " " + user.getNom());
        if (heroEmail != null) heroEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        if (heroId != null) heroId.setText("#" + user.getId());

        boolean isAdmin = user.isAdmin();
        if (heroRoleBadge != null) {
            heroRoleBadge.setText(isAdmin ? "Administrateur" : "Utilisateur");
            heroRoleBadge.getStyleClass().removeAll("hero-role-chip-admin", "hero-role-chip-user");
            heroRoleBadge.getStyleClass().add(isAdmin ? "hero-role-chip-admin" : "hero-role-chip-user");
        }

        String statut = user.getStatut() != null ? user.getStatut() : "actif";
        if (heroStatusBadge != null) {
            heroStatusBadge.setText(statut);
            heroStatusBadge.getStyleClass().removeAll("hero-status-actif", "hero-status-other");
            heroStatusBadge.getStyleClass().add("actif".equalsIgnoreCase(statut) ? "hero-status-actif" : "hero-status-other");
        }

        if (fieldPrenom != null) fieldPrenom.setText(nvl(user.getPrenom()));
        if (fieldNom != null) fieldNom.setText(nvl(user.getNom()));
        if (fieldEmail != null) fieldEmail.setText(nvl(user.getEmail()));
        if (comboStatut != null) comboStatut.setValue(statut);
        if (comboRole != null) comboRole.setValue(isAdmin ? "Administrateur" : "Utilisateur");
        if (fieldNewPassword != null) fieldNewPassword.clear();
        if (fieldConfirmPassword != null) fieldConfirmPassword.clear();
        if (pwdStrengthBox != null) {
            pwdStrengthBox.setVisible(false);
            pwdStrengthBox.setManaged(false);
        }
        if (profileMessage != null) {
            profileMessage.setVisible(false);
            profileMessage.setManaged(false);
        }
    }

    private void setupPasswordStrengthListener() {
        if (fieldNewPassword == null) return;
        fieldNewPassword.textProperty().addListener((obs, old, val) -> {
            boolean show = !val.isEmpty();
            if (pwdStrengthBox != null) {
                pwdStrengthBox.setVisible(show);
                pwdStrengthBox.setManaged(show);
            }
            if (show) updateStrength(val);
        });
    }

    private void updateStrength(String pwd) {
        int score = 0;
        if (pwd.length() >= 8) score++;
        if (pwd.matches(".*[A-Z].*")) score++;
        if (pwd.matches(".*[0-9].*")) score++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) score++;

        Region[] bars = {bar1, bar2, bar3, bar4};
        String[] colors = {"#EF4444", "#F97316", "#EAB308", "#22C55E"};
        String[] labels = {"Très faible", "Faible", "Moyen", "Fort"};
        int idx = Math.max(0, score - 1);

        for (int i = 0; i < 4; i++) {
            if (bars[i] == null) continue;
            bars[i].setStyle(i < score
                    ? "-fx-background-color:" + colors[idx] + ";-fx-background-radius:4;"
                    : "-fx-background-color:#E2E8F0;-fx-background-radius:4;");
            bars[i].setPrefHeight(6);
        }
        if (pwdStrengthLabel != null) {
            pwdStrengthLabel.setText("Force : " + labels[idx]);
            pwdStrengthLabel.setStyle("-fx-text-fill:" + colors[idx] + ";-fx-font-weight:bold;");
        }
    }

    @FXML
    private void handleProfileSave() {
        if (profiledUser == null) return;
        String prenom = fieldPrenom.getText().trim();
        String nom = fieldNom.getText().trim();
        String email = fieldEmail.getText().trim();
        String statut = comboStatut.getValue();
        String roleVal = comboRole.getValue();
        String newPwd = fieldNewPassword.getText();
        String confirm = fieldConfirmPassword.getText();

        // Validations
        if (prenom.isEmpty()) {
            showProfileMsg("Le prénom est obligatoire.", false);
            return;
        }
        if (!prenom.matches("[\\p{L} '\\-]+")) {
            showProfileMsg("Le prénom ne doit pas contenir de chiffres ou caractères spéciaux.", false);
            return;
        }
        if (nom.isEmpty()) {
            showProfileMsg("Le nom est obligatoire.", false);
            return;
        }
        if (!nom.matches("[\\p{L} '\\-]+")) {
            showProfileMsg("Le nom ne doit pas contenir de chiffres ou caractères spéciaux.", false);
            return;
        }
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            showProfileMsg("Adresse email invalide.", false);
            return;
        }
        if (!newPwd.isEmpty()) {
            if (newPwd.length() < 8) {
                showProfileMsg("Mot de passe : 8 caractères minimum.", false);
                return;
            }
            if (!newPwd.equals(confirm)) {
                showProfileMsg("Les mots de passe ne correspondent pas.", false);
                return;
            }
        }

        try {
            User existing = userService.findByEmail(email);
            if (existing != null && existing.getId() != profiledUser.getId()) {
                showProfileMsg("Cet email est déjà utilisé par un autre compte.", false);
                return;
            }

            profiledUser.setPrenom(prenom);
            profiledUser.setNom(nom);
            profiledUser.setEmail(email);
            profiledUser.setStatut(statut != null ? statut : "actif");
            profiledUser.setRoles("Administrateur".equals(roleVal) ? "[\"ROLE_ADMIN\"]" : "[\"ROLE_USER\"]");
            if (!newPwd.isEmpty()) profiledUser.setPassword(newPwd);

            userService.modifier(profiledUser);

            if (heroInitials != null) heroInitials.setText(initials(prenom, nom));
            if (heroFullName != null) heroFullName.setText(prenom + " " + nom);
            if (heroEmail != null) heroEmail.setText(email);
            if (fieldNewPassword != null) fieldNewPassword.clear();
            if (fieldConfirmPassword != null) fieldConfirmPassword.clear();
            if (pwdStrengthBox != null) {
                pwdStrengthBox.setVisible(false);
                pwdStrengthBox.setManaged(false);
            }

            showProfileMsg("Modifications enregistrées avec succès ✓", true);
            loadUsers();

        } catch (SQLException e) {
            showProfileMsg("Erreur SQL : " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleProfileCancel() {
        if (profiledUser != null) populateProfileView(profiledUser);
    }

    private void showProfileMsg(String msg, boolean ok) {
        if (profileMessage == null) return;
        profileMessage.setText(msg);
        profileMessage.getStyleClass().removeAll("pf-msg-error", "pf-msg-success");
        profileMessage.getStyleClass().add(ok ? "pf-msg-success" : "pf-msg-error");
        profileMessage.setVisible(true);
        profileMessage.setManaged(true);
    }

    // ============================================================
    // 12. TABLEAU UTILISATEURS
    // ============================================================

    private void initUsersTable() {
        if (usersTable == null) return;

        colUserAvatar.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                User u = (User) getTableRow().getItem();
                StackPane av = new StackPane();
                av.getStyleClass().add("row-avatar");
                av.setPrefSize(36, 36);
                Label lbl = new Label(initials(u.getPrenom(), u.getNom()));
                lbl.getStyleClass().add("row-avatar-text");
                av.getChildren().add(lbl);
                Label name = new Label(u.getPrenom() + " " + u.getNom());
                name.getStyleClass().add("row-name");
                HBox cell = new HBox(12, av, name);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
            }
        });

        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserEmail.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label l = new Label(item);
                l.getStyleClass().add("row-email");
                setGraphic(l);
                setText(null);
            }
        });

        colUserStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colUserStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label b = new Label(item);
                b.getStyleClass().addAll("status-badge",
                        "actif".equalsIgnoreCase(item) ? "status-actif" : "status-inactif");
                setGraphic(b);
                setText(null);
            }
        });

        colUserRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colUserRoles.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String label;
                String css;
                if (item.contains("ROLE_ADMIN")) {
                    label = "Admin";
                    css = "role-chip-admin";
                } else {
                    label = "Étudiant";
                    css = "role-chip-user";
                }
                Label b = new Label(label);
                b.getStyleClass().addAll("role-chip", css);
                setGraphic(b);
                setText(null);
            }
        });

        colUserActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnProfile = new Button("👤 Profil");
            private final Button btnDelete = new Button("🗑");
            {
                btnProfile.getStyleClass().add("btn-row-profile");
                btnDelete.getStyleClass().add("btn-row-delete");
                btnProfile.setOnAction(e -> openProfileView(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> confirmAndDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(8, btnProfile, btnDelete);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        usersTable.setItems(usersList);
        loadUsers();
    }

    private void loadUsers() {
        try {
            List<User> all = userService.recuperer();
            usersList.setAll(all);
            if (userCountLabel != null) {
                userCountLabel.setText(all.size() + " utilisateur" + (all.size() > 1 ? "s" : "") + " au total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearchUser() {
        String kw = fieldSearch != null ? fieldSearch.getText().trim() : "";
        try {
            List<User> res = kw.isEmpty() ? userService.recuperer() : userService.search(kw);
            usersList.setAll(res);
            if (userCountLabel != null) {
                userCountLabel.setText(res.size() + " résultat" + (res.size() > 1 ? "s" : ""));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/user-profile-new.fxml")
            );
            Parent root = loader.load();
            UserNewController ctrl = loader.getController();
            ctrl.setOnSavedCallback(() -> {
                loadUsers();
                loadStats();
            });
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            Stage stage = (Stage) pageTitle.getScene().getWindow();
            stage.setTitle("Fluently - Nouvel utilisateur");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void confirmAndDelete(User u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer " + u.getPrenom() + " " + u.getNom() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            try {
                userService.supprimer(u.getId());
                loadUsers();
                loadStats();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ============================================================
    // 13. TOGGLE SOUS-MENUS
    // ============================================================

    @FXML private void toggleLangues() { toggle(languesSubmenu); }
    @FXML private void toggleTests() { toggle(testsSubmenu); }
    @FXML private void toggleSessions() { toggle(sessionsSubmenu); }

    @FXML
    private void toggleObjectifs() {
        boolean wasVisible = objectifsSubmenu != null && objectifsSubmenu.isVisible();
        toggle(objectifsSubmenu);
        if (!wasVisible) showObjectifs();
    }

    private void toggle(VBox submenu) {
        if (submenu == null) return;
        boolean nowVisible = !submenu.isVisible();
        submenu.setVisible(nowVisible);
        submenu.setManaged(nowVisible);
    }

    // ============================================================
    // 14. NAVIGATION
    // ============================================================

    @FXML
    private void showDashboard() {
        hideAll();
        show(dashboardView);
        setTitle("Dashboard", "Administration › Dashboard");
    }

    @FXML
    private void showEtudiants() {
        hideAll();
        if (etudiantsView != null && !etudiantsView.getChildren().isEmpty()) {
            show(etudiantsView);
        } else {
            loadPage(etudiantsView, "/com/example/pijava_fluently/fxml/etudiants-view.fxml");
        }
        setTitle("Utilisateurs", "Administration › Utilisateurs");
        setActive(navEtudiants);
    }

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
    // Ajoutez cette déclaration avec les autres @FXML
    @FXML private Button navLangueStats;

    @FXML
    private void showLangueStats() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/langues-stats.fxml")
            );
            Node view = loader.load();

            // Cacher toutes les vues existantes
            hideAll();

            // Vider et ajouter la nouvelle vue dans languesView (qui est un VBox)
            if (languesView != null) {
                languesView.getChildren().clear();
                languesView.getChildren().add(view);
                VBox.setVgrow(view, Priority.ALWAYS);
                languesView.setVisible(true);
                languesView.setManaged(true);
            } else {
                // Fallback: créer un conteneur temporaire
                VBox tempContainer = new VBox(view);
                tempContainer.setStyle("-fx-background-color: #F8FAFC;");
                tempContainer.setPadding(new Insets(20));
                VBox.setVgrow(tempContainer, Priority.ALWAYS);

                // Trouver le scrollPane et remplacer son contenu
                ScrollPane scrollPane = (ScrollPane) pageTitle.getParent().getParent();
                if (scrollPane != null) {
                    scrollPane.setContent(tempContainer);
                }
            }

            setTitle("📊 Statistiques des Langues", "Administration › Langues › Statistiques");
            setActive(navLangueStats);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page des statistiques: " + e.getMessage());
        }
    }

    // Ajoutez aussi cette méthode showAlert si elle n'existe pas
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
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

    @FXML
    private void showGroupes() {
        hideAll();
        show(groupesView);
        setTitle("Groupes", "Administration › Groupes");
        setActive(navGroupes);
        loadGroupes();
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

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/login-view.fxml")
            );
            Node loginPage = loader.load();
            if (dashboardView != null && dashboardView.getScene() != null) {
                dashboardView.getScene().setRoot((javafx.scene.Parent) loginPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // 15. UTILITAIRES PRIVÉS
    // ============================================================

    private void hideAll() {
        for (VBox v : allViews) {
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }
    }

    private void show(VBox view) {
        if (view != null) {
            view.setVisible(true);
            view.setManaged(true);
        }
    }

    private void showView(VBox view, String title, String breadcrumb) {
        hideAll();
        if (view != null) {
            view.setVisible(true);
            view.setManaged(true);
        }
        if (pageTitle != null) pageTitle.setText(title);
        if (pageBreadcrumb != null) pageBreadcrumb.setText(breadcrumb);
    }

    private void setTitle(String title, String breadcrumb) {
        if (pageTitle != null) pageTitle.setText(title);
        if (pageBreadcrumb != null) pageBreadcrumb.setText(breadcrumb);
    }

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

    private void loadPage(VBox container, String fxmlPath) {
        if (container == null) return;
        try {
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
            showErrorInContainer(container, "Impossible de charger : " + fxmlPath + "\n" + e.getMessage());
        }
    }

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

    private void loadStats() {
        // Utilisateurs
        try {
            if (statEtudiants != null)
                statEtudiants.setText(String.valueOf(userService.count()));
        } catch (SQLException e) {
            if (statEtudiants != null) statEtudiants.setText("?");
        }

        // Tests
        try {
            TestService ts = new TestService();
            List<Test> tests = ts.recuperer();
            if (statTests != null)
                statTests.setText(String.valueOf(tests.size()));

            // Passages
            TestPassageService tps = new TestPassageService();
            List<TestPassage> passages = tps.recuperer();
            if (statPassages != null)
                statPassages.setText(String.valueOf(passages.size()));

            // Score moyen
            if (statScore != null && !passages.isEmpty()) {
                double avg = passages.stream()
                        .filter(p -> p.getScoreMax() > 0)
                        .mapToDouble(p -> (double) p.getScore() / p.getScoreMax() * 100)
                        .average()
                        .orElse(0);
                statScore.setText(String.format("%.0f%%", avg));
            } else if (statScore != null) {
                statScore.setText("—");
            }

            // Table récents passages (5 derniers)
            // Si tu as typé recentPassagesTable<TestPassage>, sinon ignore
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String initials(String p, String n) {
        String a = (p != null && !p.isEmpty()) ? p.substring(0, 1).toUpperCase() : "?";
        String b = (n != null && !n.isEmpty()) ? n.substring(0, 1).toUpperCase() : "?";
        return a + b;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    // ============================================================
    // GROUPS MANAGEMENT (gestion-groupe branch)
    // ============================================================

    private void setupGroupesTable() {
        if (groupesTable == null) return;

        colGroupeId.setCellValueFactory(c ->
            new javafx.beans.property.SimpleIntegerProperty(c.getValue().getId()).asObject());
        colGroupeNom.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getNom()));
        colGroupeDescription.setCellValueFactory(c -> {
            String desc = c.getValue().getDescription();
            if (desc != null && desc.length() > 60) desc = desc.substring(0, 57) + "...";
            return new javafx.beans.property.SimpleStringProperty(desc);
        });
        colGroupeCapacite.setCellValueFactory(c ->
            new javafx.beans.property.SimpleIntegerProperty(c.getValue().getCapacite()).asObject());
        colGroupeStatut.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getStatut()));

        colGroupeStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { setGraphic(badge); setContentDisplay(ContentDisplay.GRAPHIC_ONLY); }
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { badge.setText(null); badge.setStyle(""); return; }
                badge.setText(statut);
                String base = "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
                badge.setStyle(base + switch (statut.toLowerCase()) {
                    case "actif"   -> "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;";
                    case "inactif" -> "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
                    case "complet" -> "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;";
                    default        -> "-fx-background-color: #E2E8F0; -fx-text-fill: #374151;";
                });
            }
        });

        colGroupeDate.setCellValueFactory(c -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String date = c.getValue().getDateCreation() != null
                    ? sdf.format(c.getValue().getDateCreation()) : "—";
            return new javafx.beans.property.SimpleStringProperty(date);
        });

        colGroupeActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnMessages = new Button("Messages");
            private final Button btnEdit     = new Button("Editer");
            private final Button btnDelete   = new Button("Suppr.");
            private final HBox pane = new HBox(5, btnMessages, btnEdit, btnDelete);
            {
                pane.setAlignment(Pos.CENTER);
                btnMessages.getStyleClass().add("table-action-messages");
                btnEdit.getStyleClass().add("table-action-edit");
                btnDelete.getStyleClass().add("table-action-delete");
                btnMessages.setTooltip(new Tooltip("Voir les messages"));
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                btnMessages.setOnAction(e -> openGroupMessages(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> handleEditGroup(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteGroup(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        groupesTable.setItems(filteredList);
    }

    @FXML private void handleAddGroup()  { openGroupForm(null); }

    private void handleEditGroup(Groupe groupe) { openGroupForm(groupe); }

    private void handleDeleteGroup(Groupe groupe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le groupe");
        alert.setContentText("Supprimer \"" + groupe.getNom() + "\" ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                groupService.supprimer(groupe.getId());
                loadGroupes();
            } catch (SQLException e) {
                showAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    private void openGroupForm(Groupe groupe) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/group-form.fxml"));
            Parent form = loader.load();
            GroupFormController ctrl = loader.getController();
            ctrl.setGroupService(groupService);
            ctrl.setParentController(this);
            ctrl.setOnCloseRequest(this::closeInlineGroupForm);
            if (groupe != null) ctrl.setGroupe(groupe);
            if (groupFormHost != null && groupFormOverlay != null) {
                groupFormHost.getChildren().setAll(form);
                groupFormOverlay.setVisible(true);
                groupFormOverlay.setManaged(true);
            }
        } catch (IOException e) {
            showAlert("Erreur formulaire", e.getMessage());
        }
    }

    private void openGroupMessages(Groupe groupe) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/admin-group-messages.fxml"));
            Parent view = loader.load();
            AdminGroupMessagesController ctrl = loader.getController();
            ctrl.setGroupe(groupe);
            ctrl.setOnBack(() -> {
                if (groupFormHost != null) groupFormHost.getChildren().clear();
                if (groupFormOverlay != null) {
                    groupFormOverlay.setVisible(false);
                    groupFormOverlay.setManaged(false);
                }
                loadGroupes();
            });
            if (groupFormHost != null && groupFormOverlay != null) {
                groupFormHost.getChildren().setAll(view);
                groupFormOverlay.setVisible(true);
                groupFormOverlay.setManaged(true);
            }
        } catch (IOException e) {
            showAlert("Erreur messages", e.getMessage());
        }
    }

    private void closeInlineGroupForm() {
        if (groupFormHost != null) groupFormHost.getChildren().clear();
        if (groupFormOverlay != null) {
            groupFormOverlay.setVisible(false);
            groupFormOverlay.setManaged(false);
        }
    }

    @FXML private void handleSearch()        { applyFilters(); }
    @FXML private void handleResetFilters()  { if (searchField != null) searchField.clear(); applyFilters(); }
    @FXML private void handleRefreshGroups() { loadGroupes(); }

    private void applyFilters() {
        filteredList.clear();
        String term = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        for (Groupe g : groupesList) {
            if (term.isEmpty()
                    || g.getNom().toLowerCase().contains(term)
                    || (g.getDescription() != null && g.getDescription().toLowerCase().contains(term))) {
                filteredList.add(g);
            }
        }
        if (lblResultCount != null) {
            int n = filteredList.size();
            lblResultCount.setText(n + " groupe" + (n > 1 ? "s" : ""));
        }
    }

    public void loadGroupes() {
        try {
            groupesList.setAll(groupService.recuperer());
            applyFilters();
        } catch (SQLException e) {
            showAlert("Erreur chargement groupes", e.getMessage());
        }
    }
}