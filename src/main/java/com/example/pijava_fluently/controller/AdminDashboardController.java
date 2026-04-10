package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.models.User;
import com.example.pijava_fluently.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    // ── NAV ───────────────────────────────────────────────────────────────────
    @FXML private Button navEtudiants, navLangues, navTestsToggle, navTests;
    @FXML private Button navQuestions, navReponses, navPassages, navGroupes;
    @FXML private Button navSessionsToggle, navSessionsList, navReservations;
    @FXML private Button navObjectifsToggle, navObjectifsList, navTaches;
    @FXML private VBox   testsSubmenu, sessionsSubmenu, objectifsSubmenu;

    // ── TOPBAR ────────────────────────────────────────────────────────────────
    @FXML private Label  pageTitle, pageBreadcrumb, topbarUsername, adminName;
    @FXML private Label  statTests, statPassages, statEtudiants, statScore;
    @FXML private HBox   topbarUserPill;

    // ── VIEWS ─────────────────────────────────────────────────────────────────
    @FXML private VBox dashboardView, testsView, questionsView, reponsesView, passagesView;
    @FXML private VBox etudiantsView, languesView, groupesView, sessionsView;
    @FXML private VBox reservationsView, objectifsView, tachesView;
    @FXML private VBox profileView;   // inline profile view inside dashboard

    // ── USERS TABLE ───────────────────────────────────────────────────────────
    @FXML private TableView<User>          usersTable;
    @FXML private TableColumn<User,String> colUserAvatar, colUserEmail, colUserStatut, colUserRoles;
    @FXML private TableColumn<User,Void>   colUserActions;
    @FXML private TextField                fieldSearch;
    @FXML private Label                    userCountLabel;

    // ── INLINE PROFILE FIELDS ─────────────────────────────────────────────────
    @FXML private Label  heroInitials, heroFullName, heroRoleBadge, heroStatusBadge, heroEmail, heroId;
    @FXML private TextField        fieldPrenom, fieldNom, fieldEmail;
    @FXML private ComboBox<String> comboStatut, comboRole;
    @FXML private PasswordField    fieldNewPassword, fieldConfirmPassword;
    @FXML private HBox   statusRoleRow;
    @FXML private VBox   pwdStrengthBox;
    @FXML private Label  pwdStrengthLabel, profileMessage;
    @FXML private Region bar1, bar2, bar3, bar4;

    // ── STATE ─────────────────────────────────────────────────────────────────
    private final UserService          userService  = new UserService();
    private final ObservableList<User> usersList    = FXCollections.observableArrayList();
    private       User                 currentUser  = null;
    private       User                 profiledUser = null;  // user whose profile is open
    private       ContextMenu          adminUserMenu;

    // ── INIT ──────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initUsersTable();
        loadStats();
        showDashboard();
        createAdminUserMenu();
        if (pwdStrengthBox != null) { pwdStrengthBox.setVisible(false); pwdStrengthBox.setManaged(false); }
        if (profileMessage  != null) { profileMessage.setVisible(false);  profileMessage.setManaged(false); }
        setupPasswordStrengthListener();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        String name = user.getPrenom() + " " + user.getNom();
        if (adminName      != null) adminName.setText(name);
        if (topbarUsername != null) topbarUsername.setText(name);
    }

    public void showEtudiantsTab() { showEtudiants(); }

    // ── ADMIN TOPBAR MENU ─────────────────────────────────────────────────────

    private void createAdminUserMenu() {
        adminUserMenu = new ContextMenu();
        MenuItem profile = new MenuItem("👤  Mon Profil");
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem logout  = new MenuItem("⏻  Déconnexion");
        profile.setOnAction(e -> { if (currentUser != null) openProfileView(currentUser); });
        logout.setOnAction(e -> handleLogout());
        adminUserMenu.getItems().addAll(profile, sep, logout);
    }

    @FXML
    private void showAdminUserMenu(MouseEvent event) {
        if (adminUserMenu != null && topbarUserPill != null)
            adminUserMenu.show(topbarUserPill, event.getScreenX(), event.getScreenY() + 8);
    }

    // ── INLINE PROFILE VIEW ───────────────────────────────────────────────────

    /** Open profile inline inside the dashboard — sidebar/topbar stay intact */
    private void openProfileView(User user) {
        this.profiledUser = user;
        populateProfileView(user);
        showView(profileView, "Profil Utilisateur",
                 "Administration › Utilisateurs › " + user.getPrenom() + " " + user.getNom());
    }

    private void populateProfileView(User user) {
        String initials = initials(user.getPrenom(), user.getNom());
        if (heroInitials    != null) heroInitials.setText(initials);
        if (heroFullName    != null) heroFullName.setText(user.getPrenom() + " " + user.getNom());
        if (heroEmail       != null) heroEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        if (heroId          != null) heroId.setText("#" + user.getId());

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

        if (fieldPrenom  != null) fieldPrenom.setText(nvl(user.getPrenom()));
        if (fieldNom     != null) fieldNom.setText(nvl(user.getNom()));
        if (fieldEmail   != null) fieldEmail.setText(nvl(user.getEmail()));
        if (comboStatut  != null) comboStatut.setValue(statut);
        if (comboRole    != null) comboRole.setValue(isAdmin ? "Administrateur" : "Utilisateur");
        if (fieldNewPassword     != null) fieldNewPassword.clear();
        if (fieldConfirmPassword != null) fieldConfirmPassword.clear();
        if (pwdStrengthBox != null) { pwdStrengthBox.setVisible(false); pwdStrengthBox.setManaged(false); }
        if (profileMessage  != null) { profileMessage.setVisible(false); profileMessage.setManaged(false); }
    }

    private void setupPasswordStrengthListener() {
        if (fieldNewPassword == null) return;
        fieldNewPassword.textProperty().addListener((obs, old, val) -> {
            boolean show = !val.isEmpty();
            if (pwdStrengthBox != null) { pwdStrengthBox.setVisible(show); pwdStrengthBox.setManaged(show); }
            if (show) updateStrength(val);
        });
    }

    private void updateStrength(String pwd) {
        int score = 0;
        if (pwd.length() >= 8)               score++;
        if (pwd.matches(".*[A-Z].*"))         score++;
        if (pwd.matches(".*[0-9].*"))         score++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) score++;
        Region[] bars   = { bar1, bar2, bar3, bar4 };
        String[] colors = { "#EF4444","#F97316","#EAB308","#22C55E" };
        String[] labels = { "Très faible","Faible","Moyen","Fort" };
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
        String prenom  = fieldPrenom.getText().trim();
        String nom     = fieldNom.getText().trim();
        String email   = fieldEmail.getText().trim();
        String statut  = comboStatut.getValue();
        String roleVal = comboRole.getValue();
        String newPwd  = fieldNewPassword.getText();
        String confirm = fieldConfirmPassword.getText();

        // Validation
        if (prenom.isEmpty()) { showProfileMsg("Le prénom est obligatoire.", false); return; }
        if (!prenom.matches("[\\p{L} '\\-]+")) { showProfileMsg("Le prénom ne doit pas contenir de chiffres ou caractères spéciaux.", false); return; }
        if (nom.isEmpty())    { showProfileMsg("Le nom est obligatoire.", false); return; }
        if (!nom.matches("[\\p{L} '\\-]+"))    { showProfileMsg("Le nom ne doit pas contenir de chiffres ou caractères spéciaux.", false); return; }
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) { showProfileMsg("Adresse email invalide.", false); return; }
        if (!newPwd.isEmpty()) {
            if (newPwd.length() < 8) { showProfileMsg("Mot de passe : 8 caractères minimum.", false); return; }
            if (!newPwd.equals(confirm)) { showProfileMsg("Les mots de passe ne correspondent pas.", false); return; }
        }

        try {
            User existing = userService.findByEmail(email);
            if (existing != null && existing.getId() != profiledUser.getId()) {
                showProfileMsg("Cet email est déjà utilisé par un autre compte.", false); return;
            }
            profiledUser.setPrenom(prenom);
            profiledUser.setNom(nom);
            profiledUser.setEmail(email);
            profiledUser.setStatut(statut != null ? statut : "actif");
            profiledUser.setRoles("Administrateur".equals(roleVal) ? "[\"ROLE_ADMIN\"]" : "[\"ROLE_USER\"]");
            if (!newPwd.isEmpty()) profiledUser.setPassword(newPwd);

            userService.modifier(profiledUser);

            // Refresh hero live
            if (heroInitials != null) heroInitials.setText(initials(prenom, nom));
            if (heroFullName != null) heroFullName.setText(prenom + " " + nom);
            if (heroEmail    != null) heroEmail.setText(email);
            if (fieldNewPassword     != null) fieldNewPassword.clear();
            if (fieldConfirmPassword != null) fieldConfirmPassword.clear();
            if (pwdStrengthBox != null) { pwdStrengthBox.setVisible(false); pwdStrengthBox.setManaged(false); }

            showProfileMsg("Modifications enregistrées avec succès ✓", true);
            loadUsers(); // refresh table in background

        } catch (SQLException e) { showProfileMsg("Erreur SQL : " + e.getMessage(), false); }
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
        profileMessage.setVisible(true); profileMessage.setManaged(true);
    }

    // ── USERS TABLE ───────────────────────────────────────────────────────────

    private void initUsersTable() {
        if (usersTable == null) return;

        colUserAvatar.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                User u = (User) getTableRow().getItem();
                StackPane av = new StackPane();
                av.getStyleClass().add("row-avatar");
                av.setPrefSize(36,36); av.setMinSize(36,36); av.setMaxSize(36,36);
                Label lbl = new Label(initials(u.getPrenom(), u.getNom()));
                lbl.getStyleClass().add("row-avatar-text");
                av.getChildren().add(lbl);
                Label name = new Label(u.getPrenom() + " " + u.getNom());
                name.getStyleClass().add("row-name");
                HBox cell = new HBox(12, av, name);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell); setText(null);
            }
        });

        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserEmail.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label l = new Label(item); l.getStyleClass().add("row-email");
                setGraphic(l); setText(null);
            }
        });

        colUserStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colUserStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item);
                b.getStyleClass().addAll("status-badge",
                    "actif".equalsIgnoreCase(item) || "online".equalsIgnoreCase(item)
                        ? "status-actif" : "status-inactif");
                setGraphic(b); setText(null);
            }
        });

        colUserRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colUserRoles.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String label; String css;
                if (item.contains("ROLE_ADMIN"))     { label = "Admin";      css = "role-chip-admin"; }
                else if (item.contains("ROLE_PROF")) { label = "Prof";       css = "role-chip-prof";  }
                else                                 { label = "Étudiant";   css = "role-chip-user";  }
                Label b = new Label(label);
                b.getStyleClass().addAll("role-chip", css);
                setGraphic(b); setText(null);
            }
        });

        colUserActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnProfile = new Button("👤 Profil");
            private final Button btnDelete  = new Button("🗑");
            {
                btnProfile.getStyleClass().add("btn-row-profile");
                btnDelete.getStyleClass().add("btn-row-delete");
                btnProfile.setOnAction(e -> openProfileView(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> confirmAndDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(8, btnProfile, btnDelete);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        usersTable.setItems(usersList);
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadUsers();
    }

    private void loadUsers() {
        try {
            List<User> all = userService.recuperer();
            usersList.setAll(all);
            if (userCountLabel != null)
                userCountLabel.setText(all.size() + " utilisateur" + (all.size() > 1 ? "s" : "") + " au total");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleSearchUser() {
        String kw = fieldSearch != null ? fieldSearch.getText().trim() : "";
        try {
            List<User> res = kw.isEmpty() ? userService.recuperer() : userService.search(kw);
            usersList.setAll(res);
            if (userCountLabel != null)
                userCountLabel.setText(res.size() + " résultat" + (res.size() > 1 ? "s" : ""));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pijava_fluently/fxml/user-profile-new.fxml")
            );
            Parent root = loader.load();
            UserNewController ctrl = loader.getController();
            ctrl.setOnSavedCallback(() -> { loadUsers(); loadStats(); });
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm()
            );
            Stage stage = (Stage) pageTitle.getScene().getWindow();
            stage.setTitle("Fluently - Nouvel utilisateur");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void confirmAndDelete(User u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer " + u.getPrenom() + " " + u.getNom() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try { userService.supprimer(u.getId()); loadUsers(); loadStats(); }
            catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ── TOGGLE SUBMENUS ───────────────────────────────────────────────────────

    @FXML private void toggleTests()     { toggle(testsSubmenu,     navTestsToggle,     "📝  Tests"); }
    @FXML private void toggleSessions()  { toggle(sessionsSubmenu,  navSessionsToggle,  "📅  Sessions"); }
    @FXML private void toggleObjectifs() { toggle(objectifsSubmenu, navObjectifsToggle, "🎯  Objectifs"); }

    private void toggle(VBox sub, Button btn, String label) {
        if (sub == null) return;
        boolean open = sub.isVisible();
        sub.setVisible(!open); sub.setManaged(!open);
        if (btn != null) btn.setText(label + (open ? "  ▾" : "  ▴"));
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    @FXML private void showDashboard()    { showView(dashboardView,    "Dashboard",          "Administration › Dashboard"); }
    @FXML private void showTests()        { showView(testsView,        "Tests",              "Administration › Tests"); setActive(navTests); }
    @FXML private void showQuestions()    { showView(questionsView,    "Questions",          "Administration › Questions"); setActive(navQuestions); }
    @FXML private void showReponses()     { showView(reponsesView,     "Réponses",           "Administration › Réponses"); setActive(navReponses); }
    @FXML private void showPassages()     { showView(passagesView,     "Passages",           "Administration › Passages"); setActive(navPassages); }
    @FXML private void showLangues()      { showView(languesView,      "Langues",            "Administration › Langues"); setActive(navLangues); }
    @FXML private void showGroupes()      { showView(groupesView,      "Groupes",            "Administration › Groupes"); setActive(navGroupes); }
    @FXML private void showSessions()     { showView(sessionsView,     "Sessions",           "Administration › Sessions"); setActive(navSessionsList); }
    @FXML private void showReservations() { showView(reservationsView, "Réservations",       "Administration › Réservations"); setActive(navReservations); }
    @FXML private void showObjectifs()    { showView(objectifsView,    "Objectifs",          "Administration › Objectifs"); setActive(navObjectifsList); }
    @FXML private void showTaches()       { showView(tachesView,       "Tâches",             "Administration › Tâches"); setActive(navTaches); }

    @FXML public void showEtudiants() {
        showView(etudiantsView, "Gestion des Utilisateurs", "Administration › Utilisateurs");
        setActive(navEtudiants);
        loadUsers();
    }

    @FXML private void handleLogout() {
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── VIEW HELPERS ──────────────────────────────────────────────────────────

    private void showView(VBox view, String title, String crumb) {
        VBox[] all = { dashboardView, testsView, questionsView, reponsesView, passagesView,
                       etudiantsView, languesView, groupesView, sessionsView,
                       reservationsView, objectifsView, tachesView, profileView };
        for (VBox v : all) if (v != null) { v.setVisible(false); v.setManaged(false); }
        if (view != null) { view.setVisible(true); view.setManaged(true); }
        if (pageTitle      != null) pageTitle.setText(title);
        if (pageBreadcrumb != null) pageBreadcrumb.setText(crumb);
    }

    private void setActive(Button active) {
        Button[] all = { navEtudiants, navLangues, navTests, navQuestions, navReponses,
                         navPassages, navGroupes, navSessionsList, navReservations,
                         navObjectifsList, navTaches };
        for (Button b : all) if (b != null) b.getStyleClass().remove("nav-active");
        if (active != null) active.getStyleClass().add("nav-active");
    }

    private void loadStats() {
        try { if (statEtudiants != null) statEtudiants.setText(String.valueOf(userService.count())); }
        catch (SQLException e) { if (statEtudiants != null) statEtudiants.setText("?"); }
        if (statTests    != null) statTests.setText("4");
        if (statPassages != null) statPassages.setText("2");
        if (statScore    != null) statScore.setText("78%");
    }

    private static String initials(String p, String n) {
        String a = (p != null && !p.isEmpty()) ? p.substring(0,1).toUpperCase() : "?";
        String b = (n != null && !n.isEmpty()) ? n.substring(0,1).toUpperCase() : "?";
        return a + b;
    }
    private static String nvl(String s) { return s != null ? s : ""; }
}
