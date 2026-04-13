package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.services.ObjectifService;
import com.example.pijava_fluently.services.TacheService;
import com.example.pijava_fluently.utils.MyDatabase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectifAdminController {

    @FXML private Label     countLabel;
    @FXML private TextField searchField;
    @FXML private FlowPane  cardsContainer;

    // Cartes de statistiques
    @FXML private Label totalObjectifsLabel;
    @FXML private Label totalTachesLabel;
    @FXML private Label tauxCompletionLabel;
    @FXML private Label objectifsActifsLabel;

    // Pagination
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label pageInfoLabel;

    private final ObjectifService service      = new ObjectifService();
    private final TacheService    tacheService = new TacheService();

    private ObservableList<Objectif>  allData  = FXCollections.observableArrayList();
    private ObservableList<Objectif>  currentPageData = FXCollections.observableArrayList();
    private final Map<String, Integer> userMap = new LinkedHashMap<>();
    private final Map<Integer, String> userIdToNameMap = new HashMap<>();

    // Pagination variables
    private int currentPage = 0;
    private int itemsPerPage = 3;
    private int totalPages = 0;

    private static final String[][] CARD_COLORS = {
            {"#6C63FF","#8B5CF6"}, {"#3B82F6","#2563EB"}, {"#10B981","#059669"},
            {"#F59E0B","#D97706"}, {"#EF4444","#DC2626"}, {"#8B5CF6","#7C3AED"},
            {"#06B6D4","#0891B2"}, {"#EC4899","#DB2777"},
    };

    private static final String[][] TACHE_COLORS = {
            {"#3B82F6","#2563EB"}, {"#10B981","#059669"}, {"#8B5CF6","#7C3AED"},
            {"#F59E0B","#D97706"}, {"#06B6D4","#0891B2"}, {"#EC4899","#DB2777"},
    };

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AdminDashboardController dashboardController;
    public void setDashboardController(AdminDashboardController dc) { this.dashboardController = dc; }

    @FXML
    public void initialize() {
        loadUsers();
        loadData();
        setupSearchListener();
        setupPagination();
    }

    private void setupPagination() {
        if (btnPrev != null) {
            btnPrev.setOnAction(e -> previousPage());
        }
        if (btnNext != null) {
            btnNext.setOnAction(e -> nextPage());
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, old, val) -> handleSearch());
    }

    private void loadUsers() {
        userMap.clear();
        userIdToNameMap.clear();
        try {
            Connection cnx = MyDatabase.getInstance().getConnection();
            ResultSet rs = cnx.createStatement().executeQuery(
                    "SELECT id, nom, prenom FROM user ORDER BY nom, prenom");
            while (rs.next()) {
                String label = rs.getString("nom") + " " + rs.getString("prenom");
                int userId = rs.getInt("id");
                userMap.put(label, userId);
                userIdToNameMap.put(userId, label);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            updateStatistics();
            updatePagination();
            updateCountLabel(allData.size());
        } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    private void updatePagination() {
        totalPages = (int) Math.ceil((double) allData.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        updatePageInfo();
        updateButtonsState();
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allData.size());

        currentPageData.clear();
        if (start < allData.size()) {
            currentPageData.addAll(allData.subList(start, end));
        }
        renderCards(currentPageData);
    }

    private void updatePageInfo() {
        if (pageInfoLabel != null) {
            int start = currentPage * itemsPerPage + 1;
            int end = Math.min((currentPage + 1) * itemsPerPage, allData.size());
            if (allData.isEmpty()) {
                pageInfoLabel.setText("Page 0 / 0");
            } else {
                pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages + " (" + start + "-" + end + " sur " + allData.size() + ")");
            }
        }
    }

    private void updateButtonsState() {
        if (btnPrev != null) btnPrev.setDisable(currentPage == 0);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePagination();
        }
    }

    private void updateStatistics() {
        if (allData.isEmpty()) {
            totalObjectifsLabel.setText("0");
            totalTachesLabel.setText("0");
            tauxCompletionLabel.setText("0%");
            objectifsActifsLabel.setText("0");
            return;
        }

        totalObjectifsLabel.setText(String.valueOf(allData.size()));

        int totalTaches = 0;
        int tachesTerminees = 0;
        int objectifsActifs = 0;

        for (Objectif o : allData) {
            try {
                List<Tache> taches = tacheService.recupererParObjectif(o.getId());
                totalTaches += taches.size();
                for (Tache t : taches) {
                    if ("Terminée".equals(t.getStatut())) tachesTerminees++;
                }
            } catch (SQLException e) {}

            String statut = o.getStatut();
            if (statut != null && !"Terminé".equals(statut) && !"Annulé".equals(statut)) {
                objectifsActifs++;
            }
        }

        totalTachesLabel.setText(String.valueOf(totalTaches));
        objectifsActifsLabel.setText(String.valueOf(objectifsActifs));

        int taux = totalTaches > 0 ? (tachesTerminees * 100 / totalTaches) : 0;
        tauxCompletionLabel.setText(taux + "%");
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            currentPage = 0;
            updatePagination();
            updateCountLabel(allData.size());
            return;
        }

        List<Objectif> filtered = allData.stream()
                .filter(o -> (o.getTitre()       != null && o.getTitre().toLowerCase().contains(q))
                        ||   (o.getDescription() != null && o.getDescription().toLowerCase().contains(q))
                        ||   (o.getStatut()      != null && o.getStatut().toLowerCase().contains(q))
                        ||   (getUserNameById(o.getIdUserId()).toLowerCase().contains(q)))
                .collect(Collectors.toList());

        currentPage = 0;
        totalPages = (int) Math.ceil((double) filtered.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filtered.size());

        currentPageData.clear();
        if (start < filtered.size()) {
            currentPageData.addAll(filtered.subList(start, end));
        }
        renderCards(currentPageData);
        updateCountLabel(filtered.size());
        updatePageInfoForFiltered(filtered.size());
        updateButtonsState();
    }

    private void updatePageInfoForFiltered(int filteredSize) {
        if (pageInfoLabel != null) {
            int start = currentPage * itemsPerPage + 1;
            int end = Math.min((currentPage + 1) * itemsPerPage, filteredSize);
            if (filteredSize == 0) {
                pageInfoLabel.setText("Page 0 / 0");
            } else {
                pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages + " (" + start + "-" + end + " sur " + filteredSize + ")");
            }
        }
    }

    private String getUserNameById(int userId) {
        return userIdToNameMap.getOrDefault(userId, "Utilisateur #" + userId);
    }

    private void renderCards(List<Objectif> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Objectif o : list) {
            cardsContainer.getChildren().add(buildCard(o, i++ % CARD_COLORS.length));
        }
        if (list.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucun objectif trouvé");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            emptyBox.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(emptyBox);
        }
    }

    private VBox buildCard(Objectif o, int colorIdx) {
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];

        List<Tache> taches;
        try { taches = tacheService.recupererParObjectif(o.getId()); }
        catch (SQLException e) { taches = List.of(); }

        int nbTaches = taches.size();
        int tachesTerminees = (int) taches.stream().filter(t -> "Terminée".equals(t.getStatut())).count();
        int progression = nbTaches > 0 ? (tachesTerminees * 100 / nbTaches) : 0;

        final List<Tache> tachesFinal = taches;

        VBox card = new VBox(0);
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),26,0,0,8);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;"));

        // Header
        VBox header = new VBox(8);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");

        HBox headerTop = new HBox(10);
        headerTop.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getStatutIcon(o.getStatut()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:50;-fx-padding:6 8 6 8;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label readOnly = new Label("🔒 Lecture seule");
        readOnly.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 8 3 8;");
        headerTop.getChildren().addAll(iconLbl, sp, readOnly);

        Label titreLabel = new Label(o.getTitre() != null ? o.getTitre() : "Sans titre");
        titreLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titreLabel.setWrapText(true);
        header.getChildren().addAll(headerTop, titreLabel);

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 18, 10, 18));

        String desc = o.getDescription() != null && !o.getDescription().isBlank()
                ? (o.getDescription().length() > 75 ? o.getDescription().substring(0, 72) + "…" : o.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);

        HBox datesBox = new HBox(10);
        datesBox.setAlignment(Pos.CENTER_LEFT);
        datesBox.getChildren().addAll(
                dateBadge("📅 Début", o.getDateDeb() != null ? o.getDateDeb().format(FMT) : "—", "#EFF6FF", "#3B82F6"),
                dateBadge("🏁 Fin",   o.getDateFin() != null ? o.getDateFin().format(FMT) : "—", "#FFF7ED", "#EA580C"));

        Label statutBadge = buildStatutBadge(o.getStatut());

        Label userBadge = new Label("👤  " + getUserNameById(o.getIdUserId()));
        userBadge.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");

        // Barre de progression
        VBox progressBox = new VBox(4);
        Label progressLabel = new Label("📊 Progression : " + progression + "%");
        progressLabel.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;-fx-font-weight:bold;");

        ProgressBar progressBar = new ProgressBar(progression / 100.0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + c1 + "; -fx-background-radius: 10;");
        progressBar.setMaxHeight(6);
        progressBox.getChildren().addAll(progressLabel, progressBar);

        // Badge tâches
        Button tasksBtn = new Button("📋  " + nbTaches + " tâche" + (nbTaches > 1 ? "s" : "") + " (" + tachesTerminees + " terminée" + (tachesTerminees > 1 ? "s" : "") + ")");
        tasksBtn.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;" +
                "-fx-padding:8 14 8 14;-fx-cursor:hand;-fx-border-color:transparent;");
        tasksBtn.setOnMouseEntered(e -> tasksBtn.setStyle(
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;" +
                        "-fx-padding:8 14 8 14;-fx-cursor:hand;-fx-border-color:transparent;"));
        tasksBtn.setOnMouseExited(e -> tasksBtn.setStyle(
                "-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;" +
                        "-fx-padding:8 14 8 14;-fx-cursor:hand;-fx-border-color:transparent;"));

        tasksBtn.setOnAction(e -> showTachesDialog(o, tachesFinal, c1, c2));

        body.getChildren().addAll(descLabel, datesBox, statutBadge, userBadge, progressBox, tasksBtn);

        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        // Actions
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(12, 16, 14, 16));
        actions.setAlignment(Pos.CENTER);
        Button btnVoir = makeBtn("👁  Détails", "#EFF6FF", "#3B82F6");
        HBox.setHgrow(btnVoir, Priority.ALWAYS);
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> showDetails(o));
        actions.getChildren().add(btnVoir);

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    private void showTachesDialog(Objectif o, List<Tache> taches, String c1, String c2) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Tâches — " + o.getTitre());
        dialog.setHeaderText(null);

        VBox content = new VBox(0);
        content.setPrefWidth(600);

        VBox header = new VBox(6);
        header.setPadding(new Insets(22, 28, 18, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");

        HBox headerTop = new HBox(10);
        headerTop.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label("📋  Tâches de — " + o.getTitre());
        titleLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        int tachesTerminees = (int) taches.stream().filter(t -> "Terminée".equals(t.getStatut())).count();
        Label statLabel = new Label(tachesTerminees + "/" + taches.size() + " terminées");
        statLabel.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        headerTop.getChildren().addAll(titleLbl, spacer, statLabel);

        Label subLbl = new Label(taches.size() + " tâche" + (taches.size() > 1 ? "s" : "") + " associée" + (taches.size() > 1 ? "s" : ""));
        subLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.80);-fx-font-weight:bold;");
        header.getChildren().addAll(headerTop, subLbl);

        VBox body = new VBox(12);
        body.setPadding(new Insets(20, 28, 24, 28));

        if (taches.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(30));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size:36px;");
            Label msg = new Label("Aucune tâche pour cet objectif");
            msg.setStyle("-fx-font-size:14px;-fx-text-fill:#9CA3AF;");
            emptyBox.getChildren().addAll(icon, msg);
            body.getChildren().add(emptyBox);
        } else {
            int idx = 0;
            for (Tache t : taches) {
                body.getChildren().add(buildTacheRow(t, idx % TACHE_COLORS.length));
                idx++;
            }
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(450);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        content.getChildren().addAll(header, scroll);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private HBox buildTacheRow(Tache t, int colorIdx) {
        String c1 = TACHE_COLORS[colorIdx][0];
        boolean enRetard = t.getDateLimite() != null && t.getDateLimite().isBefore(LocalDate.now()) && !"Terminée".equals(t.getStatut());

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:12;-fx-border-color:#E2E8F0;-fx-border-radius:12;-fx-border-width:1;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(40, 40);
        iconBox.setMinSize(40, 40);
        iconBox.setStyle("-fx-background-color:" + c1 + "22;-fx-background-radius:50;");
        Label iconLbl = new Label(getPrioriteIcon(t.getPriorite()));
        iconLbl.setStyle("-fx-font-size:18px;");
        iconBox.getChildren().add(iconLbl);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titreLbl = new Label(t.getTitre() != null ? t.getTitre() : "—");
        titreLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        String bg, fg, icon;
        switch (t.getStatut() != null ? t.getStatut() : "") {
            case "Terminée" -> { bg = "#ECFDF5"; fg = "#059669"; icon = "✅"; }
            case "En cours" -> { bg = "#EEF2FF"; fg = "#6C63FF"; icon = "🔄"; }
            case "Annulée"  -> { bg = "#FFF1F2"; fg = "#E11D48"; icon = "❌"; }
            default          -> { bg = "#F8FAFC"; fg = "#64748B"; icon = "📋"; }
        }
        Label statBadge = new Label(icon + " " + (t.getStatut() != null ? t.getStatut() : "—"));
        statBadge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");

        Label prioBadge = new Label(t.getPriorite() != null ? t.getPriorite() : "—");
        prioBadge.setStyle("-fx-background-color:" + c1 + "22;-fx-text-fill:" + c1 + ";-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");

        badges.getChildren().addAll(statBadge, prioBadge);

        Label dateLbl = new Label((enRetard ? "⚠ " : "⏰ ") +
                (t.getDateLimite() != null ? t.getDateLimite().format(FMT) : "—") +
                (enRetard ? "  EN RETARD" : ""));
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + (enRetard ? "#E11D48" : "#64748B") + ";-fx-font-weight:" + (enRetard ? "bold" : "normal") + ";");

        String desc = t.getDescription() != null && !t.getDescription().isBlank()
                ? (t.getDescription().length() > 60 ? t.getDescription().substring(0, 57) + "…" : t.getDescription())
                : "Aucune description.";
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");

        info.getChildren().addAll(titreLbl, badges, dateLbl, descLbl);

        Button btnDetail = new Button("👁 Détails");
        btnDetail.setStyle("-fx-background-color:" + c1 + "22;-fx-text-fill:" + c1 + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 12 6 12;-fx-cursor:hand;-fx-border-color:transparent;");
        btnDetail.setOnAction(e -> showTacheDetail(t));

        row.getChildren().addAll(iconBox, info, btnDetail);
        return row;
    }

    private void showTacheDetail(Tache t) {
        int colorIdx = (int)(t.getId() % TACHE_COLORS.length);
        String c1 = TACHE_COLORS[colorIdx][0], c2 = TACHE_COLORS[colorIdx][1];
        boolean enRetard = t.getDateLimite() != null && t.getDateLimite().isBefore(LocalDate.now()) && !"Terminée".equals(t.getStatut());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détail tâche — " + t.getTitre());
        dialog.setHeaderText(null);
        VBox content = new VBox(0);
        content.setPrefWidth(480);

        VBox header = new VBox(8);
        header.setPadding(new Insets(22, 26, 18, 26));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label prioLbl = new Label(getPrioriteIcon(t.getPriorite()) + "  " + (t.getPriorite() != null ? t.getPriorite() : "—"));
        prioLbl.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label adminBadge = new Label("🔒 Consultation Admin");
        adminBadge.setStyle("-fx-background-color:rgba(255,255,255,0.18);-fx-text-fill:white;-fx-font-size:10px;-fx-background-radius:6;-fx-padding:3 8 3 8;");
        topRow.getChildren().addAll(prioLbl, spacer, adminBadge);

        Label titleLbl = new Label(t.getTitre() != null ? t.getTitre() : "—");
        titleLbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Label statLbl = new Label(getStatutIconTache(t.getStatut()) + "  " + (t.getStatut() != null ? t.getStatut() : "—"));
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(topRow, titleLbl, statLbl);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 26, 24, 26));
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(120);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);
        String ls = "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";
        String vsRed = "-fx-font-size:13px;-fx-text-fill:#E11D48;-fx-font-weight:bold;";

        addGridRow(grid, 0, "ID", String.valueOf(t.getId()), ls, vs);
        addGridRow(grid, 1, "Statut", t.getStatut() != null ? t.getStatut() : "—", ls, vs);
        addGridRow(grid, 2, "Priorité", t.getPriorite() != null ? t.getPriorite() : "—", ls, vs);
        addGridRow(grid, 3, "Date limite", t.getDateLimite() != null ? t.getDateLimite().format(FMT) + (enRetard ? "  ⚠ EN RETARD" : "") : "—", ls, enRetard ? vsRed : vs);
        addGridRow(grid, 4, "Objectif #", String.valueOf(t.getIdObjectifId()), ls, vs);

        Label descTitle = new Label("📝  Description");
        descTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        TextArea descArea = new TextArea(t.getDescription() != null ? t.getDescription() : "Aucune description.");
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefHeight(80);
        descArea.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-font-size:13px;");
        body.getChildren().addAll(grid, descTitle, descArea);
        content.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private void showDetails(Objectif o) {
        int colorIdx = (int)(o.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + o.getTitre());
        dialog.setHeaderText(null);
        VBox content = new VBox(0);
        content.setPrefWidth(520);

        VBox header = new VBox(8);
        header.setPadding(new Insets(24, 28, 20, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");

        HBox headerTop = new HBox(10);
        headerTop.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(o.getTitre() != null ? o.getTitre() : "—");
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label adminBadge = new Label("🔒 Consultation Admin");
        adminBadge.setStyle("-fx-background-color:rgba(255,255,255,0.18);-fx-text-fill:white;-fx-font-size:10px;-fx-background-radius:6;-fx-padding:4 10 4 10;");
        headerTop.getChildren().addAll(titleLbl, spacer, adminBadge);

        Label statLbl = new Label(getStatutIcon(o.getStatut()) + "  " + (o.getStatut() != null ? o.getStatut() : "—"));
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(headerTop, statLbl);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 28, 24, 28));
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(130);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);
        String ls = "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";

        int nbTaches = 0;
        int tachesTerminees = 0;
        try {
            List<Tache> taches = tacheService.recupererParObjectif(o.getId());
            nbTaches = taches.size();
            tachesTerminees = (int) taches.stream().filter(t -> "Terminée".equals(t.getStatut())).count();
        } catch (SQLException ignored) {}

        addGridRow(grid, 0, "ID", String.valueOf(o.getId()), ls, vs);
        addGridRow(grid, 1, "Statut", o.getStatut() != null ? o.getStatut() : "—", ls, vs);
        addGridRow(grid, 2, "Date début", o.getDateDeb() != null ? o.getDateDeb().format(FMT) : "—", ls, vs);
        addGridRow(grid, 3, "Date fin", o.getDateFin() != null ? o.getDateFin().format(FMT) : "—", ls, vs);
        addGridRow(grid, 4, "Utilisateur", getUserNameById(o.getIdUserId()), ls, vs);
        addGridRow(grid, 5, "Tâches", nbTaches + " tâche" + (nbTaches > 1 ? "s" : "") + " (" + tachesTerminees + " terminée" + (tachesTerminees > 1 ? "s" : "") + ")", ls, vs);

        int progression = nbTaches > 0 ? (tachesTerminees * 100 / nbTaches) : 0;
        VBox progressBox = new VBox(4);
        Label progressLabel = new Label("📊 Progression : " + progression + "%");
        progressLabel.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        ProgressBar progressBar = new ProgressBar(progression / 100.0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + c1 + "; -fx-background-radius: 10;");
        progressBar.setMaxHeight(8);
        progressBox.getChildren().addAll(progressLabel, progressBar);

        Label descTitle = new Label("📝  Description");
        descTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        TextArea descArea = new TextArea(o.getDescription() != null ? o.getDescription() : "Aucune description.");
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefHeight(80);
        descArea.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-font-size:13px;");
        body.getChildren().addAll(grid, progressBox, descTitle, descArea);
        content.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private VBox dateBadge(String label, String value, String bg, String fg) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;");
        Label val = new Label(value);
        val.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:2 8 2 8;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private Label buildStatutBadge(String statut) {
        String bg, fg, icon;
        switch (statut != null ? statut : "") {
            case "Terminé"  -> { bg = "#ECFDF5"; fg = "#059669"; icon = "✅"; }
            case "En pause" -> { bg = "#FFFBEB"; fg = "#D97706"; icon = "⏸"; }
            case "Annulé"   -> { bg = "#FFF1F2"; fg = "#E11D48"; icon = "❌"; }
            default          -> { bg = "#EEF2FF"; fg = "#6C63FF"; icon = "🔄"; }
        }
        Label badge = new Label(icon + "  " + (statut != null ? statut : "—"));
        badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return badge;
    }

    private String getStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "Terminé" -> "✅";
            case "En pause" -> "⏸";
            case "Annulé" -> "❌";
            default -> "🎯";
        };
    }

    private String getStatutIconTache(String s) {
        return switch (s != null ? s : "") {
            case "Terminée" -> "✅";
            case "En cours" -> "🔄";
            case "Annulée" -> "❌";
            default -> "📋";
        };
    }

    private String getPrioriteIcon(String p) {
        return switch (p != null ? p : "") {
            case "Urgente" -> "🔴";
            case "Haute" -> "🟠";
            case "Normale" -> "🟡";
            default -> "🟢";
        };
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 12 8 12;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private void addGridRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll = new Label(l); ll.setStyle(ls);
        Label vv = new Label(v); vv.setStyle(vs);
        g.add(ll, 0, row); g.add(vv, 1, row);
    }

    private void updateCountLabel(int count) {
        if (countLabel != null) countLabel.setText(count + " objectif(s)");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}