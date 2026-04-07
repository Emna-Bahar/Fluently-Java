package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.services.ObjectifService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectifController {

    // ── Formulaire ─────────────────────────────────────────────────
    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private Label     formTitleIcon;
    @FXML private TextField fieldTitre;
    @FXML private TextArea  fieldDescription;
    @FXML private DatePicker fieldDateDeb;
    @FXML private DatePicker fieldDateFin;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextField fieldIdUser;

    // ── Container cartes ───────────────────────────────────────────
    @FXML private FlowPane  cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    // ── State ──────────────────────────────────────────────────────
    private final ObjectifService service = new ObjectifService();
    private ObservableList<Objectif> allData = FXCollections.observableArrayList();
    private Objectif selectedObjectif = null;

    private static final String[] STATUTS = {"En cours", "Terminé", "En pause", "Annulé"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Couleurs pour les cartes (cyclique)
    private static final String[][] CARD_COLORS = {
            {"#6C63FF", "#8B5CF6"}, // violet
            {"#3B82F6", "#2563EB"}, // bleu
            {"#10B981", "#059669"}, // vert
            {"#F59E0B", "#D97706"}, // orange
            {"#EF4444", "#DC2626"}, // rouge
            {"#8B5CF6", "#7C3AED"}, // purple
            {"#06B6D4", "#0891B2"}, // cyan
            {"#EC4899", "#DB2777"}, // pink
    };

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList(STATUTS));
        loadData();
    }

    // ── Charger et afficher les cartes ─────────────────────────────
    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            renderCards(allData);
            updateCountLabel(allData.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les objectifs : " + e.getMessage());
        }
    }

    private void renderCards(List<Objectif> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Objectif o : list) {
            cardsContainer.getChildren().add(buildCard(o, i % CARD_COLORS.length));
            i++;
        }
        if (list.isEmpty()) {
            Label empty = new Label("📭  Aucun objectif trouvé");
            empty.setStyle("-fx-font-size:16px;-fx-text-fill:#9CA3AF;-fx-padding:40;");
            cardsContainer.getChildren().add(empty);
        }
    }

    private VBox buildCard(Objectif o, int colorIdx) {
        String c1 = CARD_COLORS[colorIdx][0];
        String c2 = CARD_COLORS[colorIdx][1];

        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),18,0,0,4);"
        );

        // ── Header coloré ──────────────────────────────────────────
        HBox header = new HBox();
        header.setPadding(new Insets(18, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");" +
                        "-fx-background-radius:16 16 0 0;"
        );

        // Icône cercle
        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(38, 38);
        iconCircle.setMinSize(38, 38);
        iconCircle.setStyle(
                "-fx-background-color:rgba(255,255,255,0.25);" +
                        "-fx-background-radius:50;"
        );
        Label iconLbl = new Label(getStatutIcon(o.getStatut()));
        iconLbl.setStyle("-fx-font-size:16px;");
        iconCircle.getChildren().add(iconLbl);

        // Titre
        Label titreLabel = new Label(o.getTitre() != null ? o.getTitre() : "Sans titre");
        titreLabel.setStyle(
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;" +
                        "-fx-wrap-text:true;-fx-max-width:200;"
        );
        titreLabel.setWrapText(true);
        titreLabel.setMaxWidth(200);

        HBox.setMargin(titreLabel, new Insets(0, 0, 0, 12));
        HBox.setHgrow(titreLabel, Priority.ALWAYS);
        header.getChildren().addAll(iconCircle, titreLabel);

        // ── Corps ──────────────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 18, 6, 18));

        // Description
        String desc = o.getDescription() != null && !o.getDescription().isBlank()
                ? (o.getDescription().length() > 80
                ? o.getDescription().substring(0, 77) + "…"
                : o.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);

        // Dates
        HBox datesBox = new HBox(12);
        datesBox.setAlignment(Pos.CENTER_LEFT);
        datesBox.getChildren().addAll(
                dateBadge("📅 Début", o.getDateDeb(), "#EFF6FF", "#3B82F6"),
                dateBadge("📅 Fin",   o.getDateFin(), "#FFF7ED", "#EA580C")
        );

        // Statut badge
        Label statutBadge = buildStatutBadge(o.getStatut());

        body.getChildren().addAll(descLabel, datesBox, statutBadge);

        // ── Séparateur ─────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#F3F4F6;");
        VBox.setMargin(sep, new Insets(6, 0, 0, 0));

        // ── Boutons actions ────────────────────────────────────────
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(12, 18, 14, 18));
        actions.setAlignment(Pos.CENTER);

        Button btnVoir = actionBtn("👁 Détails", "#EFF6FF", "#3B82F6", "#DBEAFE");
        Button btnEdit = actionBtn("✏ Modifier", c1.replace("FF", "15").replace("B6", "15"), c1, c1.replace("FF", "25").replace("B6", "25"));
        Button btnDel  = actionBtn("🗑 Supprimer", "#FFF1F2", "#E11D48", "#FFE4E6");

        // Style boutons overrides
        btnVoir.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;");
        btnEdit.setStyle("-fx-background-color:" + c1 + "22;-fx-text-fill:" + c1 + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;");
        btnDel.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;");

        btnVoir.setOnAction(e -> showDetails(o));
        btnEdit.setOnAction(e -> openEditForm(o));
        btnDel.setOnAction(e  -> handleDelete(o));

        HBox.setHgrow(btnVoir, Priority.ALWAYS);
        HBox.setHgrow(btnEdit, Priority.ALWAYS);
        HBox.setHgrow(btnDel,  Priority.ALWAYS);
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnDel.setMaxWidth(Double.MAX_VALUE);

        actions.getChildren().addAll(btnVoir, btnEdit, btnDel);

        card.getChildren().addAll(header, body, sep, actions);
        VBox.setMargin(card, new Insets(0, 0, 0, 0));

        return card;
    }

    private VBox dateBadge(String label, LocalDate date, String bg, String fg) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;");
        Label val = new Label(date != null ? date.format(FMT) : "—");
        val.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:2 8 2 8;");
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
        badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return badge;
    }

    private String getStatutIcon(String statut) {
        return switch (statut != null ? statut : "") {
            case "Terminé"  -> "✅";
            case "En pause" -> "⏸";
            case "Annulé"   -> "❌";
            default          -> "🎯";
        };
    }

    private Button actionBtn(String text, String bg, String fg, String hoverBg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;" +
                "-fx-padding:6 10 6 10;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:" + hoverBg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;"));
        btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;"));
        return btn;
    }

    private void updateCountLabel(int count) {
        countLabel.setText(count + " objectif(s)");
    }

    // ── Recherche ──────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            renderCards(allData);
            updateCountLabel(allData.size());
        } else {
            List<Objectif> filtered = allData.stream()
                    .filter(o ->
                            (o.getTitre() != null && o.getTitre().toLowerCase().contains(q)) ||
                                    (o.getDescription() != null && o.getDescription().toLowerCase().contains(q)) ||
                                    (o.getStatut() != null && o.getStatut().toLowerCase().contains(q))
                    ).collect(Collectors.toList());
            renderCards(filtered);
            updateCountLabel(filtered.size());
        }
    }

    // ── Ajouter ────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() {
        selectedObjectif = null;
        clearForm();
        formTitle.setText("Nouvel Objectif");
        formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
        formCard.setStyle(formCard.getStyle().replace("display:none;", ""));
    }

    // ── Modifier ───────────────────────────────────────────────────
    private void openEditForm(Objectif o) {
        selectedObjectif = o;
        fieldTitre.setText(o.getTitre() != null ? o.getTitre() : "");
        fieldDescription.setText(o.getDescription() != null ? o.getDescription() : "");
        fieldDateDeb.setValue(o.getDateDeb());
        fieldDateFin.setValue(o.getDateFin());
        comboStatut.setValue(o.getStatut());
        fieldIdUser.setText(String.valueOf(o.getIdUserId()));
        formTitle.setText("Modifier l'Objectif");
        formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    // ── Détails ────────────────────────────────────────────────────
    private void showDetails(Objectif o) {
        int colorIdx = (int)(o.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[colorIdx][0];
        String c2 = CARD_COLORS[colorIdx][1];

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + o.getTitre());
        dialog.setHeaderText(null);

        VBox content = new VBox(0);
        content.setPrefWidth(500);

        // Header gradient
        VBox header = new VBox(6);
        header.setPadding(new Insets(24, 28, 20, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");
        Label titleLbl = new Label(o.getTitre() != null ? o.getTitre() : "Sans titre");
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Label statutLbl = new Label(getStatutIcon(o.getStatut()) + "  " + (o.getStatut() != null ? o.getStatut() : "—"));
        statutLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(titleLbl, statutLbl);

        // Corps
        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 28, 20, 28));
        body.setStyle("-fx-background-color:#FFFFFF;");

        // Grille infos
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(110);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);

        String ls = "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";

        addRow(grid, 0, "ID",           String.valueOf(o.getId()), ls, vs);
        addRow(grid, 1, "Date début",   o.getDateDeb() != null ? o.getDateDeb().format(FMT) : "—", ls, vs);
        addRow(grid, 2, "Date fin",     o.getDateFin() != null ? o.getDateFin().format(FMT)  : "—", ls, vs);
        addRow(grid, 3, "Utilisateur",  "User #" + o.getIdUserId(), ls, vs);

        // Description
        Label descTitle = new Label("📝  Description");
        descTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        TextArea descArea = new TextArea(
                o.getDescription() != null && !o.getDescription().isBlank()
                        ? o.getDescription() : "Aucune description.");
        descArea.setEditable(false); descArea.setWrapText(true); descArea.setPrefHeight(80);
        descArea.setStyle("-fx-background-color:#F9FAFB;-fx-background-radius:10;" +
                "-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-font-size:13px;-fx-text-fill:#374151;");

        body.getChildren().addAll(grid, descTitle, descArea);
        content.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private void addRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll = new Label(l); ll.setStyle(ls);
        Label vv = new Label(v); vv.setStyle(vs);
        g.add(ll, 0, row); g.add(vv, 1, row);
    }

    // ── Enregistrer ────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        try {
            String titre       = fieldTitre.getText().trim();
            String description = fieldDescription.getText().trim();
            LocalDate dateDeb  = fieldDateDeb.getValue();
            LocalDate dateFin  = fieldDateFin.getValue();
            String statut      = comboStatut.getValue();
            int idUser         = Integer.parseInt(fieldIdUser.getText().trim());

            if (selectedObjectif == null) {
                service.ajouter(new Objectif(titre, description, dateDeb, dateFin, statut, idUser));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Objectif ajouté !");
            } else {
                selectedObjectif.setTitre(titre);
                selectedObjectif.setDescription(description);
                selectedObjectif.setDateDeb(dateDeb);
                selectedObjectif.setDateFin(dateFin);
                selectedObjectif.setStatut(statut);
                selectedObjectif.setIdUserId(idUser);
                service.modifier(selectedObjectif);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Objectif modifié !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "⚠ L'ID utilisateur doit être un entier.");
            fieldIdUser.requestFocus();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    // ── Supprimer ──────────────────────────────────────────────────
    private void handleDelete(Objectif o) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + o.getTitre() + "\" ?\nCette action est irréversible.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation"); confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(o.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "🗑 Objectif supprimé !");
                    loadData();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
                }
            }
        });
    }

    // ── Annuler ────────────────────────────────────────────────────
    @FXML
    private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        clearForm();
        selectedObjectif = null;
    }

    // ── Validation ────────────────────────────────────────────────
    private boolean validateForm() {
        String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ Le titre est obligatoire.");
            fieldTitre.requestFocus(); return false;
        }
        if (titre.length() < 3) {
            showAlert(Alert.AlertType.WARNING, "Titre trop court", "⚠ Le titre doit contenir au moins 3 caractères.");
            fieldTitre.requestFocus(); return false;
        }
        if (titre.length() > 50) {
            showAlert(Alert.AlertType.WARNING, "Titre trop long", "⚠ Le titre ne peut pas dépasser 50 caractères.");
            fieldTitre.requestFocus(); return false;
        }
        String desc = fieldDescription.getText().trim();
        if (desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ La description est obligatoire.");
            fieldDescription.requestFocus(); return false;
        }
        if (desc.length() > 255) {
            showAlert(Alert.AlertType.WARNING, "Trop long", "⚠ La description ne peut pas dépasser 255 caractères.");
            fieldDescription.requestFocus(); return false;
        }
        if (fieldDateDeb.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ La date de début est obligatoire.");
            fieldDateDeb.requestFocus(); return false;
        }
        if (fieldDateFin.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ La date de fin est obligatoire.");
            fieldDateFin.requestFocus(); return false;
        }
        if (!fieldDateFin.getValue().isAfter(fieldDateDeb.getValue())) {
            showAlert(Alert.AlertType.WARNING, "Dates invalides", "⚠ La date de fin doit être après la date de début.");
            fieldDateFin.requestFocus(); return false;
        }
        if (comboStatut.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ Veuillez sélectionner un statut.");
            comboStatut.requestFocus(); return false;
        }
        String idStr = fieldIdUser.getText().trim();
        if (idStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ L'ID utilisateur est obligatoire.");
            fieldIdUser.requestFocus(); return false;
        }
        try {
            int idUser = Integer.parseInt(idStr);
            if (idUser <= 0) {
                showAlert(Alert.AlertType.WARNING, "ID invalide", "⚠ L'ID utilisateur doit être un entier positif.");
                fieldIdUser.requestFocus(); return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "ID invalide", "⚠ L'ID utilisateur doit être un nombre entier.");
            fieldIdUser.requestFocus(); return false;
        }
        return true;
    }

    private void clearForm() {
        fieldTitre.clear();
        fieldDescription.clear();
        fieldDateDeb.setValue(LocalDate.now());
        fieldDateFin.setValue(null);
        comboStatut.setValue(null);
        fieldIdUser.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}