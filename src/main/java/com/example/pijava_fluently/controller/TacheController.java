package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
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

public class TacheController {

    @FXML private Label     pageTitle;
    @FXML private Label     pageSubtitle;
    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private Label     formTitleIcon;
    @FXML private TextField fieldTitre;
    @FXML private TextArea  fieldDescription;
    @FXML private DatePicker fieldDateLimite;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<String> comboPriorite;
    @FXML private FlowPane  cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    private final TacheService service = new TacheService();
    private ObservableList<Tache> allData = FXCollections.observableArrayList();
    private Tache    selectedTache    = null;
    private Objectif currentObjectif  = null;
    private ObjectifController objectifController;

    private static final String[] STATUTS   = {"À faire", "En cours", "Terminée", "Annulée"};
    private static final String[] PRIORITES = {"Basse", "Normale", "Haute", "Urgente"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[][] CARD_COLORS = {
            {"#6C63FF", "#8B5CF6"},
            {"#3B82F6", "#2563EB"},
            {"#10B981", "#059669"},
            {"#F59E0B", "#D97706"},
            {"#EF4444", "#DC2626"},
            {"#8B5CF6", "#7C3AED"},
            {"#06B6D4", "#0891B2"},
            {"#EC4899", "#DB2777"},
    };

    public void setObjectif(Objectif o) {
        this.currentObjectif = o;
        if (pageTitle != null)    pageTitle.setText("📋  Tâches — " + o.getTitre());
        if (pageSubtitle != null) pageSubtitle.setText("Gérez les tâches liées à cet objectif");
        loadData();
    }

    public void setObjectifController(ObjectifController oc) {
        this.objectifController = oc;
    }

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList(STATUTS));
        comboPriorite.setItems(FXCollections.observableArrayList(PRIORITES));
    }

    private void loadData() {
        if (currentObjectif == null) return;
        try {
            allData = FXCollections.observableArrayList(service.recupererParObjectif(currentObjectif.getId()));
            renderCards(allData);
            updateCountLabel(allData.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void renderCards(List<Tache> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Tache t : list) {
            cardsContainer.getChildren().add(buildCard(t, i % CARD_COLORS.length));
            i++;
        }
        if (list.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucune tâche pour cet objectif");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            emptyBox.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(emptyBox);
        }
    }

    private VBox buildCard(Tache t, int colorIdx) {
        String c1 = CARD_COLORS[colorIdx][0];
        String c2 = CARD_COLORS[colorIdx][1];

        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);"
        );

        // ── Header ─────────────────────────────────────────────────
        VBox header = new VBox(6);
        header.setPadding(new Insets(18, 18, 14, 18));
        header.setStyle(
                "-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");" +
                        "-fx-background-radius:18 18 0 0;"
        );

        HBox top = new HBox(8); top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getPrioriteIcon(t.getPriorite()));
        iconLbl.setStyle(
                "-fx-font-size:16px;-fx-background-color:rgba(255,255,255,0.22);" +
                        "-fx-background-radius:50;-fx-padding:5 7 5 7;"
        );
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge priorité
        Label prioBadge = new Label(t.getPriorite() != null ? t.getPriorite() : "—");
        prioBadge.setStyle(
                "-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                        "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;"
        );
        top.getChildren().addAll(iconLbl, spacer, prioBadge);

        Label titreLabel = new Label(t.getTitre() != null ? t.getTitre() : "Sans titre");
        titreLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titreLabel.setWrapText(true);
        header.getChildren().addAll(top, titreLabel);

        // ── Corps ──────────────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(12, 16, 8, 16));

        // Description
        String desc = t.getDescription() != null && !t.getDescription().isBlank()
                ? (t.getDescription().length() > 70 ? t.getDescription().substring(0, 67) + "…" : t.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);

        // Date limite
        VBox dateLimBox = new VBox(2);
        Label dateLbl = new Label("⏰ Date limite");
        dateLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;");
        Label dateVal = new Label(t.getDateLimite() != null ? t.getDateLimite().format(FMT) : "—");
        boolean enRetard = t.getDateLimite() != null && t.getDateLimite().isBefore(LocalDate.now())
                && !"Terminée".equals(t.getStatut());
        dateVal.setStyle("-fx-background-color:" + (enRetard ? "#FFF1F2" : "#EFF6FF") + ";" +
                "-fx-text-fill:" + (enRetard ? "#E11D48" : "#3B82F6") + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:2 8 2 8;");
        dateLimBox.getChildren().addAll(dateLbl, dateVal);

        // Statut
        Label statutBadge = buildStatutBadge(t.getStatut());

        body.getChildren().addAll(descLabel, dateLimBox, statutBadge);

        // ── Séparateur ─────────────────────────────────────────────
        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        // ── Boutons ────────────────────────────────────────────────
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(10, 14, 12, 14));
        actions.setAlignment(Pos.CENTER);

        Button btnVoir = new Button("👁 Détails");
        Button btnEdit = new Button("✏ Modifier");
        Button btnDel  = new Button("🗑 Suppr.");

        btnVoir.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 8 6 8;-fx-cursor:hand;");
        btnEdit.setStyle("-fx-background-color:" + c1 + "22;-fx-text-fill:" + c1 + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 8 6 8;-fx-cursor:hand;");
        btnDel.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 8 6 8;-fx-cursor:hand;");

        HBox.setHgrow(btnVoir, Priority.ALWAYS); btnVoir.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEdit, Priority.ALWAYS); btnEdit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDel,  Priority.ALWAYS); btnDel.setMaxWidth(Double.MAX_VALUE);

        btnVoir.setOnAction(e -> showDetailsTache(t));
        btnEdit.setOnAction(e -> openEditForm(t));
        btnDel.setOnAction(e  -> handleDelete(t));

        actions.getChildren().addAll(btnVoir, btnEdit, btnDel);
        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    // ── Détails d'une tâche ────────────────────────────────────────
    private void showDetailsTache(Tache t) {
        int colorIdx = (int)(t.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + t.getTitre()); dialog.setHeaderText(null);

        VBox content = new VBox(0); content.setPrefWidth(480);

        // Header
        VBox header = new VBox(8);
        header.setPadding(new Insets(22, 26, 18, 26));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");

        HBox topRow = new HBox(10); topRow.setAlignment(Pos.CENTER_LEFT);
        Label prioLbl = new Label(getPrioriteIcon(t.getPriorite()) + "  " + (t.getPriorite() != null ? t.getPriorite() : "—"));
        prioLbl.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
        topRow.getChildren().add(prioLbl);

        Label titleLbl = new Label(t.getTitre() != null ? t.getTitre() : "—");
        titleLbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);

        Label statLbl = new Label(getStatutIcon(t.getStatut()) + "  " + (t.getStatut() != null ? t.getStatut() : "—"));
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(topRow, titleLbl, statLbl);

        // Corps
        VBox body = new VBox(14); body.setPadding(new Insets(20, 26, 24, 26));

        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(120);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);
        String ls = "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";

        addRow(grid, 0, "ID",           String.valueOf(t.getId()), ls, vs);
        addRow(grid, 1, "Date limite",  t.getDateLimite() != null ? t.getDateLimite().format(FMT) : "—", ls, vs);
        addRow(grid, 2, "Objectif #",   String.valueOf(t.getIdObjectifId()), ls, vs);
        addRow(grid, 3, "Priorité",     t.getPriorite() != null ? t.getPriorite() : "—", ls, vs);

        Label descTitle = new Label("📝  Description");
        descTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        TextArea descArea = new TextArea(t.getDescription() != null ? t.getDescription() : "Aucune description.");
        descArea.setEditable(false); descArea.setWrapText(true); descArea.setPrefHeight(80);
        descArea.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-font-size:13px;");

        body.getChildren().addAll(grid, descTitle, descArea);
        content.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;-fx-font-size:13px;" +
                "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private void addRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll = new Label(l); ll.setStyle(ls);
        Label vv = new Label(v); vv.setStyle(vs);
        g.add(ll, 0, row); g.add(vv, 1, row);
    }

    // ── Helpers badges ─────────────────────────────────────────────
    private Label buildStatutBadge(String statut) {
        String bg, fg, icon;
        switch (statut != null ? statut : "") {
            case "Terminée" -> { bg = "#ECFDF5"; fg = "#059669"; icon = "✅"; }
            case "En cours" -> { bg = "#EEF2FF"; fg = "#6C63FF"; icon = "🔄"; }
            case "Annulée"  -> { bg = "#FFF1F2"; fg = "#E11D48"; icon = "❌"; }
            default          -> { bg = "#F8FAFC"; fg = "#64748B"; icon = "📋"; }
        }
        Label badge = new Label(icon + "  " + (statut != null ? statut : "—"));
        badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return badge;
    }

    private String getStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "Terminée" -> "✅";
            case "En cours" -> "🔄";
            case "Annulée"  -> "❌";
            default          -> "📋";
        };
    }

    private String getPrioriteIcon(String p) {
        return switch (p != null ? p : "") {
            case "Urgente" -> "🔴";
            case "Haute"   -> "🟠";
            case "Normale" -> "🟡";
            default         -> "🟢";
        };
    }

    private void updateCountLabel(int count) {
        countLabel.setText(count + " tâche(s)");
    }

    // ── Recherche ──────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) { renderCards(allData); updateCountLabel(allData.size()); return; }
        List<Tache> filtered = allData.stream()
                .filter(t -> (t.getTitre() != null && t.getTitre().toLowerCase().contains(q)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)) ||
                        (t.getStatut() != null && t.getStatut().toLowerCase().contains(q)) ||
                        (t.getPriorite() != null && t.getPriorite().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderCards(filtered);
        updateCountLabel(filtered.size());
    }

    // ── Ajouter ────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() {
        selectedTache = null; clearForm();
        formTitle.setText("Nouvelle Tâche"); formTitleIcon.setText("✚");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    // ── Modifier ───────────────────────────────────────────────────
    private void openEditForm(Tache t) {
        selectedTache = t;
        fieldTitre.setText(t.getTitre() != null ? t.getTitre() : "");
        fieldDescription.setText(t.getDescription() != null ? t.getDescription() : "");
        fieldDateLimite.setValue(t.getDateLimite());
        comboStatut.setValue(t.getStatut());
        comboPriorite.setValue(t.getPriorite());
        formTitle.setText("Modifier la Tâche"); formTitleIcon.setText("✎");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    // ── Enregistrer ────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        try {
            String titre    = fieldTitre.getText().trim();
            String desc     = fieldDescription.getText().trim();
            LocalDate dl    = fieldDateLimite.getValue();
            String statut   = comboStatut.getValue();
            String priorite = comboPriorite.getValue();
            int idObj       = currentObjectif.getId();

            if (selectedTache == null) {
                service.ajouter(new Tache(titre, desc, dl, statut, priorite, idObj));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Tâche ajoutée !");
            } else {
                selectedTache.setTitre(titre); selectedTache.setDescription(desc);
                selectedTache.setDateLimite(dl); selectedTache.setStatut(statut);
                selectedTache.setPriorite(priorite);
                service.modifier(selectedTache);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Tâche modifiée !");
            }
            handleCancel(); loadData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    // ── Supprimer ──────────────────────────────────────────────────
    private void handleDelete(Tache t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la tâche \"" + t.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation"); confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(t.getId()); loadData(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage()); }
            }
        });
    }

    // ── Retour aux objectifs ───────────────────────────────────────
    @FXML
    private void handleRetour() {
        if (objectifController != null) objectifController.retourObjectifs();
    }

    // ── Annuler formulaire ─────────────────────────────────────────
    @FXML
    private void handleCancel() {
        formCard.setVisible(false); formCard.setManaged(false);
        clearForm(); selectedTache = null;
    }

    // ── Validation ────────────────────────────────────────────────
    private boolean validateForm() {
        String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ Le titre est obligatoire."); fieldTitre.requestFocus(); return false; }
        if (titre.length() < 3) { showAlert(Alert.AlertType.WARNING, "Trop court", "⚠ Minimum 3 caractères."); fieldTitre.requestFocus(); return false; }
        if (titre.length() > 50) { showAlert(Alert.AlertType.WARNING, "Trop long", "⚠ Maximum 50 caractères."); fieldTitre.requestFocus(); return false; }
        String desc = fieldDescription.getText().trim();
        if (desc.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ La description est obligatoire."); fieldDescription.requestFocus(); return false; }
        if (desc.length() > 100) { showAlert(Alert.AlertType.WARNING, "Trop long", "⚠ Maximum 100 caractères."); fieldDescription.requestFocus(); return false; }
        if (fieldDateLimite.getValue() == null) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ La date limite est obligatoire."); return false; }
        if (fieldDateLimite.getValue().isBefore(LocalDate.now())) { showAlert(Alert.AlertType.WARNING, "Date invalide", "⚠ La date limite ne peut pas être dans le passé."); return false; }
        if (comboStatut.getValue() == null) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ Sélectionnez un statut."); return false; }
        if (comboPriorite.getValue() == null) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ Sélectionnez une priorité."); return false; }
        return true;
    }

    private void clearForm() {
        fieldTitre.clear(); fieldDescription.clear();
        fieldDateLimite.setValue(null);
        comboStatut.setValue(null); comboPriorite.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}