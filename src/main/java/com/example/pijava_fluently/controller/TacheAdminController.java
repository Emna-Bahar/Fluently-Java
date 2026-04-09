package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.services.TacheService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TacheAdminController {

    @FXML private Label     countLabel;
    @FXML private TextField searchField;
    @FXML private FlowPane  cardsContainer;

    private final TacheService service = new TacheService();
    private ObservableList<Tache> allData = FXCollections.observableArrayList();

    private static final String[][] CARD_COLORS = {
            {"#6C63FF","#8B5CF6"}, {"#3B82F6","#2563EB"}, {"#10B981","#059669"},
            {"#F59E0B","#D97706"}, {"#EF4444","#DC2626"}, {"#8B5CF6","#7C3AED"},
            {"#06B6D4","#0891B2"}, {"#EC4899","#DB2777"},
    };

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() { loadData(); }

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            renderCards(allData);
            updateCountLabel(allData.size());
        } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) { renderCards(allData); updateCountLabel(allData.size()); return; }
        List<Tache> filtered = allData.stream()
                .filter(t -> (t.getTitre()       != null && t.getTitre().toLowerCase().contains(q))
                        ||   (t.getDescription() != null && t.getDescription().toLowerCase().contains(q))
                        ||   (t.getStatut()      != null && t.getStatut().toLowerCase().contains(q))
                        ||   (t.getPriorite()    != null && t.getPriorite().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderCards(filtered);
        updateCountLabel(filtered.size());
    }

    private void renderCards(List<Tache> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Tache t : list) cardsContainer.getChildren().add(buildCard(t, i++ % CARD_COLORS.length));
        if (list.isEmpty()) {
            VBox emptyBox = new VBox(12); emptyBox.setAlignment(Pos.CENTER); emptyBox.setPadding(new Insets(60));
            Label icon = new Label("📭"); icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucune tâche trouvée"); msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            emptyBox.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(emptyBox);
        }
    }

    // ── Carte tâche en lecture seule (SANS bouton Supprimer) ───────
    private VBox buildCard(Tache t, int colorIdx) {
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];
        boolean enRetard = t.getDateLimite() != null && t.getDateLimite().isBefore(LocalDate.now()) && !"Terminée".equals(t.getStatut());

        VBox card = new VBox(0);
        card.setPrefWidth(285); card.setMaxWidth(285);
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
        VBox header = new VBox(6); header.setPadding(new Insets(18, 18, 14, 18));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");

        HBox top = new HBox(8); top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getPrioriteIcon(t.getPriorite()));
        iconLbl.setStyle("-fx-font-size:16px;-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:50;-fx-padding:5 7 5 7;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label readOnly = new Label("🔒");
        readOnly.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:10px;-fx-background-radius:20;-fx-padding:3 7 3 7;");
        Label prioBadge = new Label(t.getPriorite() != null ? t.getPriorite() : "—");
        prioBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
        top.getChildren().addAll(iconLbl, spacer, readOnly, prioBadge);

        Label titreLabel = new Label(t.getTitre() != null ? t.getTitre() : "Sans titre");
        titreLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;"); titreLabel.setWrapText(true);
        header.getChildren().addAll(top, titreLabel);

        // Corps
        VBox body = new VBox(10); body.setPadding(new Insets(12, 16, 8, 16));
        String desc = t.getDescription() != null && !t.getDescription().isBlank()
                ? (t.getDescription().length() > 70 ? t.getDescription().substring(0, 67) + "…" : t.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;"); descLabel.setWrapText(true);

        VBox dateLimBox = new VBox(2);
        Label dateLbl2 = new Label("⏰ Date limite"); dateLbl2.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;");
        Label dateVal = new Label(t.getDateLimite() != null ? t.getDateLimite().format(FMT) : "—");
        dateVal.setStyle("-fx-background-color:" + (enRetard ? "#FFF1F2" : "#EFF6FF") + ";-fx-text-fill:" + (enRetard ? "#E11D48" : "#3B82F6") + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:2 8 2 8;");
        dateLimBox.getChildren().addAll(dateLbl2, dateVal);

        if (enRetard) {
            Label retardBadge = new Label("⚠ En retard");
            retardBadge.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            body.getChildren().addAll(descLabel, dateLimBox, retardBadge, buildStatutBadge(t.getStatut()));
        } else {
            body.getChildren().addAll(descLabel, dateLimBox, buildStatutBadge(t.getStatut()));
        }

        Label objBadge = new Label("🎯  Objectif #" + t.getIdObjectifId());
        objBadge.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        body.getChildren().add(objBadge);

        Separator sep = new Separator(); VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        // ── Action : Détails SEULEMENT (pas de Supprimer) ──────────
        HBox actions = new HBox(8); actions.setPadding(new Insets(10, 14, 12, 14)); actions.setAlignment(Pos.CENTER);
        Button btnVoir = makeBtn("👁  Voir les détails", "#EFF6FF", "#3B82F6");
        HBox.setHgrow(btnVoir, Priority.ALWAYS); btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> showDetails(t));
        actions.getChildren().add(btnVoir);

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    // ── Dialog détails tâche ───────────────────────────────────────
    private void showDetails(Tache t) {
        int colorIdx = (int)(t.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];
        boolean enRetard = t.getDateLimite() != null && t.getDateLimite().isBefore(LocalDate.now()) && !"Terminée".equals(t.getStatut());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails Tâche — " + t.getTitre()); dialog.setHeaderText(null);
        VBox content = new VBox(0); content.setPrefWidth(480);

        VBox header = new VBox(8); header.setPadding(new Insets(22, 26, 18, 26));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");

        HBox topRow = new HBox(10); topRow.setAlignment(Pos.CENTER_LEFT);
        Label prioLbl = new Label(getPrioriteIcon(t.getPriorite()) + "  " + (t.getPriorite() != null ? t.getPriorite() : "—"));
        prioLbl.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label adminBadge = new Label("🔒 Mode consultation");
        adminBadge.setStyle("-fx-background-color:rgba(255,255,255,0.18);-fx-text-fill:white;-fx-font-size:10px;-fx-background-radius:6;-fx-padding:3 8 3 8;");
        topRow.getChildren().addAll(prioLbl, spacer, adminBadge);

        Label titleLbl = new Label(t.getTitre() != null ? t.getTitre() : "—");
        titleLbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;"); titleLbl.setWrapText(true);
        Label statLbl = new Label(getStatutIcon(t.getStatut()) + "  " + (t.getStatut() != null ? t.getStatut() : "—"));
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(topRow, titleLbl, statLbl);

        VBox body = new VBox(14); body.setPadding(new Insets(20, 26, 24, 26));
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(130); ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);
        String ls = "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";
        String vsRed = "-fx-font-size:13px;-fx-text-fill:#E11D48;-fx-font-weight:bold;";
        addGridRow(grid, 0, "ID",          String.valueOf(t.getId()), ls, vs);
        addGridRow(grid, 1, "Statut",      t.getStatut()    != null ? t.getStatut()    : "—", ls, vs);
        addGridRow(grid, 2, "Priorité",    t.getPriorite()  != null ? t.getPriorite()  : "—", ls, vs);
        addGridRow(grid, 3, "Date limite", t.getDateLimite() != null ? t.getDateLimite().format(FMT) + (enRetard ? "  ⚠ EN RETARD" : "") : "—", ls, enRetard ? vsRed : vs);
        addGridRow(grid, 4, "Objectif #",  String.valueOf(t.getIdObjectifId()), ls, vs);

        Label descTitle = new Label("📝  Description"); descTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        TextArea descArea = new TextArea(t.getDescription() != null ? t.getDescription() : "Aucune description.");
        descArea.setEditable(false); descArea.setWrapText(true); descArea.setPrefHeight(80);
        descArea.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-font-size:13px;");
        body.getChildren().addAll(grid, descTitle, descArea);
        content.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer"); close.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    // ── Utilitaires ────────────────────────────────────────────────
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
        return switch (s != null ? s : "") { case "Terminée" -> "✅"; case "En cours" -> "🔄"; case "Annulée" -> "❌"; default -> "📋"; };
    }

    private String getPrioriteIcon(String p) {
        return switch (p != null ? p : "") { case "Urgente" -> "🔴"; case "Haute" -> "🟠"; case "Normale" -> "🟡"; default -> "🟢"; };
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 12 8 12;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private void addGridRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll = new Label(l); ll.setStyle(ls); Label vv = new Label(v); vv.setStyle(vs);
        g.add(ll, 0, row); g.add(vv, 1, row);
    }

    private void updateCountLabel(int count) { if (countLabel != null) countLabel.setText(count + " tâche(s)"); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK); a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}