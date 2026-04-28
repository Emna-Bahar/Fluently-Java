package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Groupe;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.services.*;
import com.example.pijava_fluently.utils.ConfigLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
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
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
    @FXML private Button navLangueStats;

    // ============================================================
    // 2. LABELS & BOUTONS TOPBAR
    // ============================================================
    @FXML private Label  pageTitle;
    @FXML private Label  pageBreadcrumb;
    @FXML private Label  adminName;
    @FXML private Label  topbarUsername;
    @FXML private HBox   topbarUserPill;
    @FXML private HBox   topbarUserPillRight;
    @FXML private Button btnTheme;

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
    @FXML private VBox reservationsView;
    @FXML private VBox objectifsView;
    @FXML private VBox tachesView;
    @FXML private VBox profileView;

    // ============================================================
    // 6. GROUPS MANAGEMENT
    // ============================================================
    @FXML private TableView<Groupe> groupesTable;
    @FXML private TableColumn<Groupe, Integer> colGroupeId, colGroupeCapacite;
    @FXML private TableColumn<Groupe, String> colGroupeNom, colGroupeDescription, colGroupeStatut, colGroupeDate;
    @FXML private TableColumn<Groupe, Void> colGroupeActions;
    @FXML private TextField searchField;
    @FXML private Label lblResultCount;
    @FXML private StackPane groupFormOverlay;
    @FXML private StackPane groupFormHost;

    // ============================================================
    // 7. TABLEAU UTILISATEURS
    // ============================================================
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User,String> colUserAvatar, colUserEmail, colUserStatut, colUserRoles;
    @FXML private TableColumn<User,Void> colUserActions;
    @FXML private TextField fieldSearch;
    @FXML private Label userCountLabel;

    // ============================================================
    // 8. PROFIL INLINE
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
    // 9. SERVICES & ÉTAT
    // ============================================================
    private final UserService userService = new UserService();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private User currentUser = null;
    private User profiledUser = null;
    private ContextMenu adminUserMenu;
    private boolean darkMode = false;

    // Groups services
    private GroupService groupService;
    private LangueService langueService;
    private NiveauService niveauService;
    private ObservableList<Groupe> groupesList;
    private ObservableList<Groupe> filteredList;

    private final List<VBox> allViews = new ArrayList<>();

    // ============================================================
    // 10. INITIALISATION
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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

        initUsersTable();
        loadStats();
        createAdminUserMenu();

        if (pwdStrengthBox != null) { pwdStrengthBox.setVisible(false); pwdStrengthBox.setManaged(false); }
        if (profileMessage != null) { profileMessage.setVisible(false); profileMessage.setManaged(false); }
        setupPasswordStrengthListener();

        showDashboard();
    }

    private void addIfNotNull(VBox v) {
        if (v != null) allViews.add(v);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) return;
        String name = user.getPrenom() + " " + user.getNom();
        if (adminName != null) adminName.setText(name);
        if (topbarUsername != null) topbarUsername.setText(name);
    }

    public void showEtudiantsTab() { showEtudiants(); }

    // ============================================================
    // 11. THÈME SOMBRE / CLAIR
    // ============================================================

    @FXML
    private void toggleTheme() {
        Scene scene = resolveScene();
        if (scene == null) {
            System.err.println("toggleTheme : impossible de résoudre la Scene.");
            return;
        }

        ObservableList<String> sheets = scene.getStylesheets();
        var darkRes = getClass().getResource("/com/example/pijava_fluently/css/dark.css");
        if (darkRes == null) {
            System.err.println("dark.css introuvable");
            return;
        }
        String darkCss = darkRes.toExternalForm();

        darkMode = !darkMode;
        if (darkMode) {
            if (!sheets.contains(darkCss)) sheets.add(darkCss);
            if (btnTheme != null) btnTheme.setText("☀️");
        } else {
            sheets.remove(darkCss);
            if (btnTheme != null) btnTheme.setText("🌙");
        }
    }

    private Scene resolveScene() {
        if (btnTheme != null && btnTheme.getScene() != null) return btnTheme.getScene();
        if (pageTitle != null && pageTitle.getScene() != null) return pageTitle.getScene();
        if (dashboardView != null && dashboardView.getScene() != null) return dashboardView.getScene();
        return null;
    }

    // ============================================================
    // 12. MENU UTILISATEUR ADMIN
    // ============================================================

    private void createAdminUserMenu() {
        adminUserMenu = new ContextMenu();
        MenuItem profile = new MenuItem("👤 Mon Profil");
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem logout = new MenuItem("⏻ Déconnexion");
        profile.setOnAction(e -> { if (currentUser != null) openProfileView(currentUser); });
        logout.setOnAction(e -> handleLogout());
        adminUserMenu.getItems().addAll(profile, sep, logout);
    }

    @FXML
    private void showAdminUserMenu(MouseEvent event) {
        Node source = (Node) event.getSource();
        if (adminUserMenu != null && source != null)
            adminUserMenu.show(source, event.getScreenX(), event.getScreenY() + 8);
    }

    // ============================================================
    // 13. PROFIL INLINE
    // ============================================================

    private void openProfileView(User user) {
        this.profiledUser = user;
        populateProfileView(user);
        showView(profileView,
                "Profil Utilisateur",
                "Administration › Utilisateurs › " + user.getPrenom() + " " + user.getNom());
    }

    private void populateProfileView(User user) {
        String initials = initials(user.getPrenom(), user.getNom());
        if (heroInitials != null) heroInitials.setText(initials);
        if (heroFullName != null) heroFullName.setText(user.getPrenom() + " " + user.getNom());
        if (heroEmail != null) heroEmail.setText(nvl(user.getEmail()));
        if (heroId != null) heroId.setText("#" + user.getId());

        boolean isAdmin = user.isAdmin();
        if (heroRoleBadge != null) {
            heroRoleBadge.setText(isAdmin ? "Administrateur" : "Utilisateur");
            heroRoleBadge.getStyleClass().removeAll("hero-role-chip-admin", "hero-role-chip-user");
            heroRoleBadge.getStyleClass().add(isAdmin ? "hero-role-chip-admin" : "hero-role-chip-user");
        }

        String statut = nvl(user.getStatut()).isEmpty() ? "actif" : user.getStatut();
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
        if (pwdStrengthBox != null) { pwdStrengthBox.setVisible(false); pwdStrengthBox.setManaged(false); }
        if (profileMessage != null) { profileMessage.setVisible(false); profileMessage.setManaged(false); }
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

        if (prenom.isEmpty()) { showProfileMsg("Le prénom est obligatoire.", false); return; }
        if (!prenom.matches("[\\p{L} '\\-]+")) { showProfileMsg("Le prénom ne doit pas contenir de chiffres ou caractères spéciaux.", false); return; }
        if (nom.isEmpty()) { showProfileMsg("Le nom est obligatoire.", false); return; }
        if (!nom.matches("[\\p{L} '\\-]+")) { showProfileMsg("Le nom ne doit pas contenir de chiffres ou caractères spéciaux.", false); return; }
        if (email.isEmpty() || !email.contains("@")) { showProfileMsg("Adresse email invalide.", false); return; }
        if (!newPwd.isEmpty()) {
            if (newPwd.length() < 8) { showProfileMsg("Mot de passe : 8 caractères minimum.", false); return; }
            if (!newPwd.equals(confirm)) { showProfileMsg("Les mots de passe ne correspondent pas.", false); return; }
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
            if (pwdStrengthBox != null) { pwdStrengthBox.setVisible(false); pwdStrengthBox.setManaged(false); }

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
    // 14. TABLEAU UTILISATEURS
    // ============================================================

    private void initUsersTable() {
        if (usersTable == null) return;

        colUserAvatar.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                User u = (User) getTableRow().getItem();

                StackPane av = new StackPane();
                av.getStyleClass().add("row-avatar");
                av.setPrefSize(36, 36);
                av.setMaxSize(36, 36);

                String svg = u.getAvatarSvg();
                if (svg != null && !svg.isBlank()) {
                    WebView wv = new WebView();
                    wv.setPrefSize(36, 36);
                    wv.setMaxSize(36, 36);
                    String html = "<!DOCTYPE html><html><head>"
                            + "<style>html,body{margin:0;padding:0;background:transparent;overflow:hidden;}"
                            + "svg{width:100%;height:100%;display:block;}</style>"
                            + "</head><body>" + svg + "</body></html>";
                    wv.getEngine().loadContent(html);
                    av.getChildren().add(wv);
                } else {
                    Label lbl = new Label(initials(u.getPrenom(), u.getNom()));
                    lbl.getStyleClass().add("row-avatar-text");
                    av.getChildren().add(lbl);
                }

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
                b.getStyleClass().addAll("status-badge", "actif".equalsIgnoreCase(item) ? "status-actif" : "status-inactif");
                setGraphic(b); setText(null);
            }
        });

        colUserRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colUserRoles.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String label = item.contains("ROLE_ADMIN") ? "Admin" : "Étudiant";
                String css = item.contains("ROLE_ADMIN") ? "role-chip-admin" : "role-chip-user";
                Label b = new Label(label); b.getStyleClass().addAll("role-chip", css);
                setGraphic(b); setText(null);
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
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(8, btnProfile, btnDelete); box.setAlignment(Pos.CENTER_LEFT);
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
            if (userCountLabel != null)
                userCountLabel.setText(all.size() + " utilisateur" + (all.size() > 1 ? "s" : "") + " au total");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleSearchUser() {
        String kw = fieldSearch != null ? fieldSearch.getText().trim() : "";
        try {
            List<User> res = kw.isEmpty() ? userService.recuperer() : userService.search(kw);
            usersList.setAll(res);
            if (userCountLabel != null)
                userCountLabel.setText(res.size() + " résultat" + (res.size() > 1 ? "s" : ""));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/user-profile-new.fxml"));
            Parent root = loader.load();
            UserNewController ctrl = loader.getController();
            ctrl.setOnSavedCallback(() -> { loadUsers(); loadStats(); });
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm());
            Stage stage = (Stage) pageTitle.getScene().getWindow();
            stage.setTitle("Fluently - Nouvel utilisateur");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── GOOGLE SHEETS EXPORT ───────────────────────────────────────────────────
    private static final String GOOGLE_CLIENT_ID = ConfigLoader.get("google.sheets.client.id");
    private static final String GOOGLE_CLIENT_SECRET = ConfigLoader.get("google.sheets.client.secret");
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SHEETS_API_BASE = "https://sheets.googleapis.com/v4/spreadsheets";

    @FXML
    private void handleExportGoogleSheets() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> showInfo("Export en cours…", "Ouverture de Google pour autorisation…"));
                String accessToken = authorizeGoogleSheets();
                if (accessToken == null) {
                    Platform.runLater(() -> showError("Export annulé", "Autorisation Google refusée ou expirée."));
                    return;
                }

                String spreadsheetId = createSheet(accessToken, "Fluently – Utilisateurs");
                writeUsersToSheet(accessToken, spreadsheetId);

                String url = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";
                Desktop.getDesktop().browse(new java.net.URI(url));

                Platform.runLater(() -> showInfo("✅ Export réussi !",
                        "Le tableau des utilisateurs a été créé dans Google Sheets.\n" +
                                "Il s'est ouvert dans votre navigateur."));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur export", e.getMessage()));
            }
        }).start();
    }

    private String authorizeGoogleSheets() throws Exception {
        final String[] result = {null};
        int port = 8888;
        String redirectUri = "http://localhost:" + port + "/callback";

        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);
        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2 && kv[0].equals("code"))
                        result[0] = java.net.URLDecoder.decode(kv[1], "UTF-8");
                }
            }
            String html = "<html><body style='font-family:sans-serif;text-align:center;padding:60px'>"
                    + "<h2>✅ Autorisé ! Retourne sur Fluently.</h2></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
            server.stop(1);
        });
        server.start();

        String scope = java.net.URLEncoder.encode("https://www.googleapis.com/auth/spreadsheets", "UTF-8");
        String authUrl = GOOGLE_AUTH_URL
                + "?client_id=" + java.net.URLEncoder.encode(GOOGLE_CLIENT_ID, "UTF-8")
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                + "&response_type=code"
                + "&scope=" + scope
                + "&access_type=offline"
                + "&prompt=consent";
        Desktop.getDesktop().browse(new java.net.URI(authUrl));

        long start = System.currentTimeMillis();
        while (result[0] == null && System.currentTimeMillis() - start < 120_000)
            Thread.sleep(300);

        if (result[0] == null) return null;

        String body = "code=" + java.net.URLEncoder.encode(result[0], "UTF-8")
                + "&client_id=" + java.net.URLEncoder.encode(GOOGLE_CLIENT_ID, "UTF-8")
                + "&client_secret=" + java.net.URLEncoder.encode(GOOGLE_CLIENT_SECRET, "UTF-8")
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                + "&grant_type=authorization_code";

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(GOOGLE_TOKEN_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        String tokenJson = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();

        return extractJsonField(tokenJson, "access_token");
    }

    private String createSheet(String accessToken, String title) throws Exception {
        String json = "{\"properties\":{\"title\":\"" + title + "\"}}";
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(SHEETS_API_BASE).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();
        return extractJsonField(response, "spreadsheetId");
    }

    private void writeUsersToSheet(String accessToken, String spreadsheetId) throws Exception {
        StringBuilder values = new StringBuilder();
        values.append("[");
        values.append("[\"ID\",\"Prénom\",\"Nom\",\"Email\",\"Rôle\",\"Statut\",\"Langue étudiée\"]");
        for (User u : usersList) {
            values.append(",[")
                    .append(jsonCell(String.valueOf(u.getId()))).append(",")
                    .append(jsonCell(u.getPrenom())).append(",")
                    .append(jsonCell(u.getNom())).append(",")
                    .append(jsonCell(u.getEmail())).append(",")
                    .append(jsonCell(u.getRoles() != null && u.getRoles().contains("ROLE_ADMIN") ? "Admin" :
                            u.getRoles() != null && u.getRoles().contains("ROLE_PROF") ? "Professeur" : "Etudiant")).append(",")
                    .append(jsonCell(u.getStatut())).append(",")
                    .append(jsonCell(u.getChosenLanguage()))
                    .append("]");
        }
        values.append("]");

        String body = "{\"values\":" + values + "}";
        String url = SHEETS_API_BASE + "/" + spreadsheetId + "/values/A1?valueInputOption=RAW";

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            conn.disconnect();
            throw new IOException("Sheets write error " + status + ": " + err);
        }
        conn.disconnect();
    }

    private static String jsonCell(String val) {
        if (val == null || val.isBlank()) return "\"\"";
        return "\"" + val.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String extractJsonField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end > 0 ? json.substring(start + 1, end) : null;
        }
        int end = start;
        while (end < json.length() && ",}\n\r".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(start, end).trim();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
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

    // ============================================================
    // 15. GROUPS MANAGEMENT
    // ============================================================
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
            if (currentUser != null) {
                ctrl.setAdminContext(currentUser.getId(), currentUser.getPrenom() + " " + currentUser.getNom());
            }
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

    // ============================================================
    // 16. TOGGLE SOUS-MENUS
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
        boolean now = !submenu.isVisible();
        submenu.setVisible(now);
        submenu.setManaged(now);
    }

    // ============================================================
    // 17. NAVIGATION
    // ============================================================

    @FXML private void showDashboard() { hideAll(); show(dashboardView); setTitle("Dashboard", "Administration › Dashboard"); }
    @FXML private void showEtudiants() { hideAll(); show(etudiantsView); setTitle("Utilisateurs", "Administration › Utilisateurs"); setActive(navEtudiants); }
    @FXML private void showLangues() { hideAll(); loadPage(languesView, "/com/example/pijava_fluently/fxml/langue-view.fxml"); setTitle("Langues", "Administration › Langues"); setActive(navLangues); }
    @FXML private void showNiveaux() { hideAll(); loadPage(niveauxView, "/com/example/pijava_fluently/fxml/niveau-view.fxml"); setTitle("Niveaux", "Administration › Langues › Niveaux"); }
    @FXML private void showCours() { hideAll(); loadPage(coursView, "/com/example/pijava_fluently/fxml/cours-view.fxml"); setTitle("Cours", "Administration › Langues › Cours"); }
    @FXML private void showUserProgress() { hideAll(); loadPage(userProgressView, "/com/example/pijava_fluently/fxml/user-progress.fxml"); setTitle("Progression des Étudiants", "Administration › Langues › Progression"); setActive(navUserProgress); }
    @FXML
    private void showLangueStats() {
        hideAll();
        loadPage(languesView, "/com/example/pijava_fluently/fxml/langues-stats.fxml");
        setTitle("Statistiques des Langues", "Administration › Langues › Statistiques");
        setActive(navLangueStats);
    }
    @FXML private void showTests() { hideAll(); loadPage(testsView, "/com/example/pijava_fluently/fxml/tests.fxml"); setTitle("Gestion des Tests", "Administration › Tests › Tests"); setActive(navTests); }
    @FXML private void showQuestions() { hideAll(); loadPage(questionsView, "/com/example/pijava_fluently/fxml/questions.fxml"); setTitle("Gestion des Questions", "Administration › Tests › Questions"); setActive(navQuestions); }
    @FXML private void showReponses() { hideAll(); loadPage(reponsesView, "/com/example/pijava_fluently/fxml/reponses.fxml"); setTitle("Gestion des Réponses", "Administration › Tests › Réponses"); setActive(navReponses); }
    @FXML private void showPassages() { hideAll(); loadPage(passagesView, "/com/example/pijava_fluently/fxml/passages.fxml"); setTitle("Passages de Tests", "Administration › Tests › Passages"); setActive(navPassages); }
    @FXML private void showGroupes() { hideAll(); show(groupesView); setTitle("Groupes", "Administration › Groupes"); setActive(navGroupes); loadGroupes(); }
    @FXML private void showSessions() { hideAll(); loadPage(sessionsView, "/com/example/pijava_fluently/fxml/session-admin-view.fxml"); setTitle("Sessions", "Administration › Sessions"); setActive(navSessionsList); }
    @FXML private void showReservations() { hideAll(); loadPage(reservationsView, "/com/example/pijava_fluently/fxml/reservation-admin-view.fxml"); setTitle("Réservations", "Administration › Sessions › Réservations"); setActive(navReservations); }
    @FXML private void showObjectifs() { hideAll(); loadPage(objectifsView, "/com/example/pijava_fluently/fxml/ObjectifAdmin-view.fxml"); setTitle("Objectifs", "Administration › Objectifs"); setActive(navObjectifsList); }
    @FXML private void showTaches() { hideAll(); loadPage(tachesView, "/com/example/pijava_fluently/fxml/TacheAdmin-view.fxml"); setTitle("Tâches", "Administration › Objectifs › Tâches"); setActive(navTaches); }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pijava_fluently/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/example/pijava_fluently/css/fluently.css").toExternalForm());
            Stage stage = (Stage) pageTitle.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Fluently - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ============================================================
    // 18. UTILITAIRES PRIVÉS
    // ============================================================

    private void hideAll() {
        for (VBox v : allViews) if (v != null) { v.setVisible(false); v.setManaged(false); }
    }

    private void show(VBox view) {
        if (view != null) { view.setVisible(true); view.setManaged(true); }
    }

    private void showView(VBox view, String title, String breadcrumb) {
        hideAll(); show(view); setTitle(title, breadcrumb);
    }

    private void setTitle(String title, String breadcrumb) {
        if (pageTitle != null) pageTitle.setText(title);
        if (pageBreadcrumb != null) pageBreadcrumb.setText(breadcrumb);
    }

    private void setActive(Button active) {
        Button[] all = {navEtudiants, navLangues, navTests, navQuestions, navReponses, navPassages,
                navGroupes, navSessionsList, navReservations, navObjectifsList, navTaches,
                navUserProgress, navLangueStats};
        for (Button b : all) if (b != null) b.getStyleClass().remove("nav-active");
        if (active != null) active.getStyleClass().add("nav-active");
    }

    private void loadPage(VBox container, String fxmlPath) {
        if (container == null) return;
        try {
            String loaded = (String) container.getProperties().get("loadedFxmlPath");
            if (!fxmlPath.equals(loaded)) {
                var resource = getClass().getResource(fxmlPath);
                if (resource == null) throw new IOException("Ressource introuvable : " + fxmlPath);
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
        Label detail = new Label(msg); detail.setWrapText(true);
        detail.setStyle("-fx-font-size:12px;-fx-text-fill:#B91C1C;");
        VBox errorBox = new VBox(8, title, detail);
        errorBox.setStyle("-fx-background-color:#FEF2F2;-fx-border-color:#FECACA;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:20;");
        container.getChildren().setAll(errorBox);
        container.setVisible(true); container.setManaged(true);
    }

    private void loadStats() {
        try {
            if (statEtudiants != null) statEtudiants.setText(String.valueOf(userService.count()));
        } catch (SQLException e) { if (statEtudiants != null) statEtudiants.setText("?"); }
        try {
            List<Test> tests = new TestService().recuperer();
            if (statTests != null) statTests.setText(String.valueOf(tests.size()));

            List<TestPassage> passages = new TestPassageService().recuperer();
            if (statPassages != null) statPassages.setText(String.valueOf(passages.size()));

            if (statScore != null && !passages.isEmpty()) {
                double avg = passages.stream().filter(p -> p.getScoreMax() > 0)
                        .mapToDouble(p -> (double) p.getScore() / p.getScoreMax() * 100)
                        .average().orElse(0);
                statScore.setText(String.format("%.0f%%", avg));
            } else if (statScore != null) { statScore.setText("—"); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }

    private static String initials(String p, String n) {
        String a = (p != null && !p.isEmpty()) ? p.substring(0, 1).toUpperCase() : "?";
        String b = (n != null && !n.isEmpty()) ? n.substring(0, 1).toUpperCase() : "?";
        return a + b;
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}