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
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TacheAdminController {

    @FXML private Label     countLabel;
    @FXML private TextField searchField;
    @FXML private FlowPane  cardsContainer;
    @FXML private Button    btnExportPdf;

    // Filtres
    @FXML private ComboBox<String> filterStatut;
    @FXML private ComboBox<String> filterPriorite;
    @FXML private Button btnResetFilters;

    private final TacheService service = new TacheService();
    private final ObjectifService objectifService = new ObjectifService();
    private ObservableList<Tache> allData = FXCollections.observableArrayList();
    private ObservableList<Tache> filteredData = FXCollections.observableArrayList();
    private final Map<Integer, String> objectifTitreMap = new HashMap<>();

    // Pagination
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label pageInfoLabel;

    private int currentPage = 0;
    private int itemsPerPage = 3;
    private int totalPages = 0;

    private static final String[][] CARD_COLORS = {
            {"#6C63FF","#8B5CF6"}, {"#3B82F6","#2563EB"}, {"#10B981","#059669"},
            {"#F59E0B","#D97706"}, {"#EF4444","#DC2626"}, {"#8B5CF6","#7C3AED"},
            {"#06B6D4","#0891B2"}, {"#EC4899","#DB2777"},
    };

    private static final String[] STATUTS = {"Tous", "À faire", "En cours", "Terminée", "Annulée"};
    private static final String[] PRIORITES = {"Toutes", "Basse", "Normale", "Haute", "Urgente"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        loadObjectifs();
        setupFilters();
        loadData();
        setupPagination();
    }

    private void setupFilters() {
        // Initialiser les ComboBox
        filterStatut.setItems(FXCollections.observableArrayList(STATUTS));
        filterStatut.setValue("Tous");

        filterPriorite.setItems(FXCollections.observableArrayList(PRIORITES));
        filterPriorite.setValue("Toutes");

        // Ajouter les listeners
        filterStatut.valueProperty().addListener((obs, old, val) -> applyFilters());
        filterPriorite.valueProperty().addListener((obs, old, val) -> applyFilters());

        // Bouton reset
        btnResetFilters.setOnAction(e -> resetFilters());
    }

    private void resetFilters() {
        filterStatut.setValue("Tous");
        filterPriorite.setValue("Toutes");
        searchField.clear();
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedStatut = filterStatut.getValue();
        String selectedPriorite = filterPriorite.getValue();

        List<Tache> filtered = allData.stream()
                .filter(t -> {
                    // Filtre recherche
                    if (!searchText.isEmpty()) {
                        boolean match = (t.getTitre() != null && t.getTitre().toLowerCase().contains(searchText)) ||
                                (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchText)) ||
                                (getObjectifTitre(t.getIdObjectifId()).toLowerCase().contains(searchText));
                        if (!match) return false;
                    }

                    // Filtre statut
                    if (selectedStatut != null && !selectedStatut.equals("Tous")) {
                        String tStatut = t.getStatut() != null ? t.getStatut() : "";
                        if (!tStatut.equals(selectedStatut)) return false;
                    }

                    // Filtre priorité
                    if (selectedPriorite != null && !selectedPriorite.equals("Toutes")) {
                        String tPriorite = t.getPriorite() != null ? t.getPriorite() : "";
                        if (!tPriorite.equals(selectedPriorite)) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        filteredData.clear();
        filteredData.addAll(filtered);
        currentPage = 0;
        updatePagination();
        updateCountLabel(filteredData.size());
    }

    private void setupPagination() {
        if (btnPrev != null) btnPrev.setOnAction(e -> previousPage());
        if (btnNext != null) btnNext.setOnAction(e -> nextPage());
    }

    private void loadObjectifs() {
        objectifTitreMap.clear();
        try {
            List<Objectif> objectifs = objectifService.recuperer();
            for (Objectif o : objectifs) {
                objectifTitreMap.put(o.getId(), o.getTitre());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getObjectifTitre(int objectifId) {
        return objectifTitreMap.getOrDefault(objectifId, "Objectif #" + objectifId);
    }

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            filteredData.addAll(allData);
            updatePagination();
            updateCountLabel(allData.size());
        } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    private void updatePagination() {
        totalPages = (int) Math.ceil((double) filteredData.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;
        updatePageInfo();
        updateButtonsState();
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredData.size());

        List<Tache> currentList;
        if (start < filteredData.size()) {
            currentList = filteredData.subList(start, end);
        } else {
            currentList = List.of();
        }
        renderCards(currentList);
    }

    private void updatePageInfo() {
        if (pageInfoLabel != null) {
            int start = currentPage * itemsPerPage + 1;
            int end = Math.min((currentPage + 1) * itemsPerPage, filteredData.size());
            if (filteredData.isEmpty()) {
                pageInfoLabel.setText("Page 0 / 0");
            } else {
                pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages + " (" + start + "-" + end + " sur " + filteredData.size() + ")");
            }
        }
    }

    private void updateButtonsState() {
        if (btnPrev != null) btnPrev.setDisable(currentPage == 0);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @FXML private void previousPage() { if (currentPage > 0) { currentPage--; updatePagination(); } }
    @FXML private void nextPage() { if (currentPage < totalPages - 1) { currentPage++; updatePagination(); } }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void renderCards(List<Tache> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Tache t : list) {
            cardsContainer.getChildren().add(buildCard(t, i++ % CARD_COLORS.length));
        }
        if (list.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucune tâche trouvée");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            emptyBox.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(emptyBox);
        }
    }

    private VBox buildCard(Tache t, int colorIdx) {
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];
        boolean enRetard = t.getDateLimite() != null && t.getDateLimite().isBefore(LocalDate.now()) && !"Terminée".equals(t.getStatut());
        String objectifTitre = getObjectifTitre(t.getIdObjectifId());

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
        VBox header = new VBox(6);
        header.setPadding(new Insets(18, 18, 14, 18));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getPrioriteIcon(t.getPriorite()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:50;-fx-padding:5 7 5 7;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label readOnly = new Label("🔒 Admin");
        readOnly.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:10px;-fx-background-radius:20;-fx-padding:3 8 3 8;");
        top.getChildren().addAll(iconLbl, spacer, readOnly);

        Label titreLabel = new Label(t.getTitre() != null ? t.getTitre() : "Sans titre");
        titreLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titreLabel.setWrapText(true);
        header.getChildren().addAll(top, titreLabel);

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(12, 16, 10, 16));

        String desc = t.getDescription() != null && !t.getDescription().isBlank()
                ? (t.getDescription().length() > 70 ? t.getDescription().substring(0, 67) + "…" : t.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);

        // Date limite
        VBox dateLimBox = new VBox(2);
        Label dateLbl2 = new Label("⏰ Date limite");
        dateLbl2.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;");
        Label dateVal = new Label(t.getDateLimite() != null ? t.getDateLimite().format(FMT) : "—");
        dateVal.setStyle("-fx-background-color:" + (enRetard ? "#FFF1F2" : "#EFF6FF") + ";-fx-text-fill:" + (enRetard ? "#E11D48" : "#3B82F6") + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:2 8 2 8;");
        dateLimBox.getChildren().addAll(dateLbl2, dateVal);

        // Badge statut
        Label statutBadge = buildStatutBadge(t.getStatut());

        // Badge objectif (avec TITRE au lieu de ID)
        Label objBadge = new Label("🎯  " + objectifTitre);
        objBadge.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:5 12 5 12;");
        objBadge.setMaxWidth(Double.MAX_VALUE);

        // Badge priorité
        Label prioriteBadge = new Label(getPrioriteIcon(t.getPriorite()) + "  " + (t.getPriorite() != null ? t.getPriorite() : "Normale"));
        prioriteBadge.setStyle("-fx-background-color:" + c1 + "22;-fx-text-fill:" + c1 + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:5 12 5 12;");

        HBox badgesRow = new HBox(8);
        badgesRow.setAlignment(Pos.CENTER_LEFT);
        badgesRow.getChildren().addAll(statutBadge, prioriteBadge);

        if (enRetard) {
            Label retardBadge = new Label("⚠ En retard");
            retardBadge.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            body.getChildren().addAll(descLabel, dateLimBox, retardBadge, badgesRow, objBadge);
        } else {
            body.getChildren().addAll(descLabel, dateLimBox, badgesRow, objBadge);
        }

        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(8, 0, 0, 0));

        // Action : Détails
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(12, 16, 14, 16));
        actions.setAlignment(Pos.CENTER);
        Button btnVoir = makeBtn("👁  Détails", "#EFF6FF", "#3B82F6");
        HBox.setHgrow(btnVoir, Priority.ALWAYS);
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> showDetails(t));
        actions.getChildren().add(btnVoir);

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    private void showDetails(Tache t) {
        int colorIdx = (int)(t.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];
        boolean enRetard = t.getDateLimite() != null && t.getDateLimite().isBefore(LocalDate.now()) && !"Terminée".equals(t.getStatut());
        String objectifTitre = getObjectifTitre(t.getIdObjectifId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails Tâche — " + t.getTitre());
        dialog.setHeaderText(null);
        VBox content = new VBox(0);
        content.setPrefWidth(500);

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
        Label statLbl = new Label(getStatutIcon(t.getStatut()) + "  " + (t.getStatut() != null ? t.getStatut() : "—"));
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(topRow, titleLbl, statLbl);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 26, 24, 26));
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
        String vsRed = "-fx-font-size:13px;-fx-text-fill:#E11D48;-fx-font-weight:bold;";

        addGridRow(grid, 0, "ID", String.valueOf(t.getId()), ls, vs);
        addGridRow(grid, 1, "Statut", t.getStatut() != null ? t.getStatut() : "—", ls, vs);
        addGridRow(grid, 2, "Priorité", t.getPriorite() != null ? t.getPriorite() : "—", ls, vs);
        addGridRow(grid, 3, "Date limite", t.getDateLimite() != null ? t.getDateLimite().format(FMT) + (enRetard ? "  ⚠ EN RETARD" : "") : "—", ls, enRetard ? vsRed : vs);
        addGridRow(grid, 4, "Objectif", objectifTitre, ls, vs);

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

    @FXML
    private void exportToPDF() {
        if (allData.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Export", "Aucune donnée à exporter");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les tâches en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(cardsContainer.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID;Titre;Description;Statut;Priorité;Date Limite;Objectif\n");
                for (Tache t : filteredData) {
                    writer.write(String.format("%d;%s;%s;%s;%s;%s;%s\n",
                            t.getId(),
                            escapeCSV(t.getTitre()),
                            escapeCSV(t.getDescription()),
                            t.getStatut() != null ? t.getStatut() : "",
                            t.getPriorite() != null ? t.getPriorite() : "",
                            t.getDateLimite() != null ? t.getDateLimite().format(FMT) : "",
                            escapeCSV(getObjectifTitre(t.getIdObjectifId()))
                    ));
                }
                showAlert(Alert.AlertType.INFORMATION, "Export réussi", "Fichier exporté : " + file.getName());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur d'export", e.getMessage());
            }
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace(";", ",").replace("\n", " ");
    }

    private Label buildStatutBadge(String statut) {
        String bg, fg, icon;
        switch (statut != null ? statut : "") {
            case "Terminée" -> { bg = "#ECFDF5"; fg = "#059669"; icon = "✅"; }
            case "En cours" -> { bg = "#EEF2FF"; fg = "#6C63FF"; icon = "🔄"; }
            case "Annulée"  -> { bg = "#FFF1F2"; fg = "#E11D48"; icon = "❌"; }
            default          -> { bg = "#F8FAFC"; fg = "#64748B"; icon = "📋"; }
        }
        Label badge = new Label(icon + "  " + (statut != null ? statut : "—"));
        badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return badge;
    }

    private String getStatutIcon(String s) {
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
        if (countLabel != null) countLabel.setText(count + " tâche(s)");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}