package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.services.ObjectifService;
import com.example.pijava_fluently.services.TacheService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectifController {

    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private Label     formTitleIcon;
    @FXML private TextField fieldTitre;
    @FXML private TextArea  fieldDescription;
    @FXML private DatePicker fieldDateDeb;
    @FXML private DatePicker fieldDateFin;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextField fieldIdUser;
    @FXML private FlowPane  cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    private final ObjectifService service     = new ObjectifService();
    private final TacheService    tacheService = new TacheService();
    private ObservableList<Objectif> allData  = FXCollections.observableArrayList();
    private Objectif selectedObjectif         = null;

    // Référence au HomeController pour la navigation
    private HomeController homeController;

    private static final String[] STATUTS = {"En cours", "Terminé", "En pause", "Annulé"};
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

    public void setHomeController(HomeController hc) {
        this.homeController = hc;
    }

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList(STATUTS));
        loadData();
    }

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            renderCards(allData);
            updateCountLabel(allData.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
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
            empty.setStyle("-fx-font-size:16px;-fx-text-fill:#9CA3AF;-fx-padding:60;");
            cardsContainer.getChildren().add(empty);
        }
    }

    private VBox buildCard(Objectif o, int colorIdx) {
        String c1 = CARD_COLORS[colorIdx][0];
        String c2 = CARD_COLORS[colorIdx][1];

        // Compter les tâches
        int nbTaches = 0;
        try { nbTaches = tacheService.recupererParObjectif(o.getId()).size(); } catch (SQLException ignored) {}

        VBox card = new VBox(0);
        card.setPrefWidth(295);
        card.setMaxWidth(295);
        card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);"
        );

        // ── Header gradient ────────────────────────────────────────
        VBox header = new VBox(8);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle(
                "-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");" +
                        "-fx-background-radius:18 18 0 0;"
        );

        HBox headerTop = new HBox(10);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        // Icône
        Label iconLbl = new Label(getStatutIcon(o.getStatut()));
        iconLbl.setStyle(
                "-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);" +
                        "-fx-background-radius:50;-fx-padding:6 8 6 8;"
        );

        // Compteur tâches
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label tachesCount = new Label("📋 " + nbTaches + " tâche" + (nbTaches > 1 ? "s" : ""));
        tachesCount.setStyle(
                "-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                        "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;"
        );
        headerTop.getChildren().addAll(iconLbl, spacer, tachesCount);

        Label titreLabel = new Label(o.getTitre() != null ? o.getTitre() : "Sans titre");
        titreLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titreLabel.setWrapText(true);

        header.getChildren().addAll(headerTop, titreLabel);

        // ── Corps ──────────────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 18, 10, 18));

        // Description
        String desc = o.getDescription() != null && !o.getDescription().isBlank()
                ? (o.getDescription().length() > 75 ? o.getDescription().substring(0, 72) + "…" : o.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);

        // Dates
        HBox datesBox = new HBox(10);
        datesBox.setAlignment(Pos.CENTER_LEFT);
        datesBox.getChildren().addAll(
                dateBadge("📅 Début", o.getDateDeb(), "#EFF6FF", "#3B82F6"),
                dateBadge("🏁 Fin",   o.getDateFin(), "#FFF7ED", "#EA580C")
        );

        // Statut
        Label statutBadge = buildStatutBadge(o.getStatut());

        body.getChildren().addAll(descLabel, datesBox, statutBadge);

        // ── Séparateur ─────────────────────────────────────────────
        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        // ── Boutons ────────────────────────────────────────────────
        VBox actionsBox = new VBox(8);
        actionsBox.setPadding(new Insets(12, 16, 14, 16));

        // Ligne 1 : Détails + Modifier + Supprimer
        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER);
        Button btnVoir = makeBtn("👁 Détails",   "#EFF6FF", "#3B82F6");
        Button btnEdit = makeBtn("✏ Modifier",   c1 + "22", c1);
        Button btnDel  = makeBtn("🗑 Supprimer", "#FFF1F2", "#E11D48");
        btnVoir.setStyle(btnVoir.getStyle() + "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;");
        btnEdit.setStyle("-fx-background-color:" + c1 + "22;-fx-text-fill:" + c1 + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;");
        btnDel.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;");
        HBox.setHgrow(btnVoir, Priority.ALWAYS); btnVoir.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEdit, Priority.ALWAYS); btnEdit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDel,  Priority.ALWAYS); btnDel.setMaxWidth(Double.MAX_VALUE);
        row1.getChildren().addAll(btnVoir, btnEdit, btnDel);

        // Ligne 2 : Voir les tâches (pleine largeur)
        Button btnTaches = new Button("📋  Voir les tâches (" + nbTaches + ")");
        btnTaches.setMaxWidth(Double.MAX_VALUE);
        btnTaches.setStyle(
                "-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");" +
                        "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;"
        );

        btnVoir.setOnAction(e   -> showDetails(o));
        btnEdit.setOnAction(e   -> openEditForm(o));
        btnDel.setOnAction(e    -> handleDelete(o));
        btnTaches.setOnAction(e -> ouvrirTaches(o));

        actionsBox.getChildren().addAll(row1, btnTaches);
        card.getChildren().addAll(header, body, sep, actionsBox);
        return card;
    }

    /** Ouvre la vue des tâches de cet objectif */
    private void ouvrirTaches(Objectif o) {
        if (homeController == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "HomeController non initialisé.");
            return;
        }
        try {
            // ✅ Vérifier d'abord que la ressource existe
            var resource = getClass().getResource("/com/example/pijava_fluently/fxml/Tache-view.fxml");
            if (resource == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Fichier introuvable : tache-view.fxml\n" +
                                "Vérifiez le nom et l'emplacement dans src/main/resources/");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            TacheController ctrl = loader.getController();
            ctrl.setObjectif(o);
            ctrl.setObjectifController(this);
            homeController.setContent(view);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la vue des tâches : " + e.getMessage());
        }
    }
    /** Appelé par TacheController pour revenir à la liste des objectifs */
    public void retourObjectifs() {
        if (homeController != null) {
            homeController.showObjectifs();
        }
    }

    // ── Helpers visuels ────────────────────────────────────────────
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

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;" +
                "-fx-padding:6 10 6 10;-fx-cursor:hand;");
        return btn;
    }

    private void updateCountLabel(int count) {
        countLabel.setText(count + " objectif(s)");
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) { renderCards(allData); updateCountLabel(allData.size()); return; }
        List<Objectif> filtered = allData.stream()
                .filter(o -> (o.getTitre() != null && o.getTitre().toLowerCase().contains(q)) ||
                        (o.getDescription() != null && o.getDescription().toLowerCase().contains(q)) ||
                        (o.getStatut() != null && o.getStatut().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderCards(filtered);
        updateCountLabel(filtered.size());
    }

    @FXML
    private void handleAjouter() {
        selectedObjectif = null; clearForm();
        formTitle.setText("Nouvel Objectif"); formTitleIcon.setText("✚");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    private void openEditForm(Objectif o) {
        selectedObjectif = o;
        fieldTitre.setText(o.getTitre() != null ? o.getTitre() : "");
        fieldDescription.setText(o.getDescription() != null ? o.getDescription() : "");
        fieldDateDeb.setValue(o.getDateDeb()); fieldDateFin.setValue(o.getDateFin());
        comboStatut.setValue(o.getStatut());
        fieldIdUser.setText(String.valueOf(o.getIdUserId()));
        formTitle.setText("Modifier l'Objectif"); formTitleIcon.setText("✎");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    private void showDetails(Objectif o) {
        int colorIdx = (int)(o.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + o.getTitre()); dialog.setHeaderText(null);

        VBox content = new VBox(0); content.setPrefWidth(500);

        VBox header = new VBox(6);
        header.setPadding(new Insets(24, 28, 20, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");
        Label titleLbl = new Label(o.getTitre() != null ? o.getTitre() : "—");
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Label statLbl = new Label(getStatutIcon(o.getStatut()) + "  " + (o.getStatut() != null ? o.getStatut() : "—"));
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(titleLbl, statLbl);

        VBox body = new VBox(14); body.setPadding(new Insets(20, 28, 24, 28));
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(110);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);
        String ls = "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";
        addRow(grid, 0, "ID",          String.valueOf(o.getId()), ls, vs);
        addRow(grid, 1, "Date début",  o.getDateDeb() != null ? o.getDateDeb().format(FMT) : "—", ls, vs);
        addRow(grid, 2, "Date fin",    o.getDateFin() != null ? o.getDateFin().format(FMT)  : "—", ls, vs);
        addRow(grid, 3, "Utilisateur", "User #" + o.getIdUserId(), ls, vs);

        Label descTitle = new Label("📝  Description");
        descTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        TextArea descArea = new TextArea(o.getDescription() != null ? o.getDescription() : "Aucune description.");
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

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        try {
            String titre      = fieldTitre.getText().trim();
            String desc       = fieldDescription.getText().trim();
            LocalDate dateDeb = fieldDateDeb.getValue();
            LocalDate dateFin = fieldDateFin.getValue();
            String statut     = comboStatut.getValue();
            int idUser        = Integer.parseInt(fieldIdUser.getText().trim());

            if (selectedObjectif == null) {
                service.ajouter(new Objectif(titre, desc, dateDeb, dateFin, statut, idUser));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Objectif ajouté !");
            } else {
                selectedObjectif.setTitre(titre); selectedObjectif.setDescription(desc);
                selectedObjectif.setDateDeb(dateDeb); selectedObjectif.setDateFin(dateFin);
                selectedObjectif.setStatut(statut); selectedObjectif.setIdUserId(idUser);
                service.modifier(selectedObjectif);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Objectif modifié !");
            }
            handleCancel(); loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "⚠ L'ID utilisateur doit être un entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Objectif o) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + o.getTitre() + "\" ?\nSes tâches seront aussi supprimées.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation"); confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(o.getId()); loadData(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleCancel() {
        formCard.setVisible(false); formCard.setManaged(false);
        clearForm(); selectedObjectif = null;
    }

    private boolean validateForm() {
        String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ Le titre est obligatoire."); fieldTitre.requestFocus(); return false; }
        if (titre.length() < 3) { showAlert(Alert.AlertType.WARNING, "Trop court", "⚠ Minimum 3 caractères."); fieldTitre.requestFocus(); return false; }
        if (titre.length() > 50) { showAlert(Alert.AlertType.WARNING, "Trop long", "⚠ Maximum 50 caractères."); fieldTitre.requestFocus(); return false; }
        String desc = fieldDescription.getText().trim();
        if (desc.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ La description est obligatoire."); fieldDescription.requestFocus(); return false; }
        if (desc.length() > 255) { showAlert(Alert.AlertType.WARNING, "Trop long", "⚠ Maximum 255 caractères."); fieldDescription.requestFocus(); return false; }
        if (fieldDateDeb.getValue() == null) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ Date de début obligatoire."); return false; }
        if (fieldDateFin.getValue() == null) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ Date de fin obligatoire."); return false; }
        if (!fieldDateFin.getValue().isAfter(fieldDateDeb.getValue())) { showAlert(Alert.AlertType.WARNING, "Dates invalides", "⚠ La date de fin doit être après la date de début."); return false; }
        if (comboStatut.getValue() == null) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ Sélectionnez un statut."); return false; }
        String idStr = fieldIdUser.getText().trim();
        if (idStr.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Requis", "⚠ L'ID utilisateur est obligatoire."); fieldIdUser.requestFocus(); return false; }
        try { if (Integer.parseInt(idStr) <= 0) { showAlert(Alert.AlertType.WARNING, "Invalide", "⚠ ID positif requis."); return false; } }
        catch (NumberFormatException e) { showAlert(Alert.AlertType.WARNING, "Invalide", "⚠ ID doit être un entier."); return false; }
        return true;
    }

    private void clearForm() {
        fieldTitre.clear(); fieldDescription.clear();
        fieldDateDeb.setValue(LocalDate.now()); fieldDateFin.setValue(null);
        comboStatut.setValue(null); fieldIdUser.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}