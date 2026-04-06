package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.services.LangueService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LangueController {

    // ── Formulaire ─────────────────────────────────────────────────
    @FXML private VBox formCard;
    @FXML private Label formTitle;
    @FXML private Label formTitleIcon;
    @FXML private TextField fieldNom;
    @FXML private TextArea fieldDescription;
    @FXML private TextField fieldDrapeau;
    @FXML private ComboBox<String> comboPopularite;
    @FXML private CheckBox checkActive;
    @FXML private ImageView imagePreview;
    @FXML private Label imagePlaceholder;
    @FXML private Label countLabel;

    // ── Table ──────────────────────────────────────────────────────
    @FXML private TableView<Langue> tableLangues;
    @FXML private TableColumn<Langue, String>  colNom;
    @FXML private TableColumn<Langue, String>  colDrapeau;
    @FXML private TableColumn<Langue, String>  colDescription;
    @FXML private TableColumn<Langue, String>  colPopularite;
    @FXML private TableColumn<Langue, Boolean> colActive;
    @FXML private TableColumn<Langue, Void>    colActions;

    // ── Recherche ──────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── State ──────────────────────────────────────────────────────
    private final LangueService service = new LangueService();
    private ObservableList<Langue> allData = FXCollections.observableArrayList();
    private Langue selectedLangue    = null;
    private File   selectedImageFile = null;

    private static final String IMAGE_DIR =
            "src/main/resources/com/example/pijava_fluently/image/";

    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        comboPopularite.setItems(FXCollections.observableArrayList(
                "très haute", "haute", "moyenne", "faible"
        ));

        tableLangues.setFixedCellSize(52);
        setupColumns();
        loadData();
    }

    // ── Ajuster hauteur table selon nb lignes ──────────────────────
    private void adjustTableHeight() {
        int count = tableLangues.getItems().size();
        double h = count * 52 + 35; // 52px/ligne + header
        tableLangues.setPrefHeight(h);
        tableLangues.setMinHeight(h);
        tableLangues.setMaxHeight(h);
    }

    // ── Configuration colonnes ─────────────────────────────────────
    private void setupColumns() {

        // Nom
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1A1D2E;");
            }
        });

        // Drapeau
        colDrapeau.setCellValueFactory(new PropertyValueFactory<>("drapeau"));
        colDrapeau.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            { iv.setFitWidth(46); iv.setFitHeight(30); iv.setPreserveRatio(true); }
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || path == null || path.isBlank()) {
                    setText("—"); setGraphic(null); return;
                }
                try {
                    File f = new File(path);
                    if (f.exists()) {
                        iv.setImage(new Image(f.toURI().toString()));
                        setGraphic(iv); setText(null);
                    } else { setText("🖼"); setGraphic(null); }
                } catch (Exception e) { setText("🖼"); setGraphic(null); }
            }
        });

        // Description
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                String txt = item.length() > 50 ? item.substring(0, 47) + "…" : item;
                setText(txt);
                setTooltip(new Tooltip(item));
                setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");
            }
        });

        // Popularité
        colPopularite.setCellValueFactory(new PropertyValueFactory<>("popularite"));
        colPopularite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item.isBlank()) {
                    setText("—"); setGraphic(null); return;
                }
                Label badge = new Label(item);
                badge.setStyle(
                        "-fx-background-color:#F3F0FF;-fx-text-fill:#6C63FF;" +
                                "-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-background-radius:20;-fx-padding:4 10 4 10;"
                );
                setGraphic(badge); setText(null);
            }
        });

        // Statut
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || val == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(val ? "● Active" : "● Inactive");
                badge.setStyle(val
                        ? "-fx-background-color:#ECFDF5;-fx-text-fill:#059669;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:4 10 4 10;"
                        : "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:4 10 4 10;"
                );
                setGraphic(badge); setText(null);
            }
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView   = new Button("Détails");
            private final Button btnEdit   = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox   box       = new HBox(6, btnView, btnEdit, btnDelete);

            private final String sView   = "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;-fx-border-color:#BFDBFE;-fx-border-radius:7;-fx-border-width:1;";
            private final String sEdit   = "-fx-background-color:#F5F3FF;-fx-text-fill:#7C3AED;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;-fx-border-color:#DDD6FE;-fx-border-radius:7;-fx-border-width:1;";
            private final String sDelete = "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;-fx-border-color:#FECDD3;-fx-border-radius:7;-fx-border-width:1;";
            private final String hView   = "-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;-fx-border-color:#93C5FD;-fx-border-radius:7;-fx-border-width:1;";
            private final String hEdit   = "-fx-background-color:#EDE9FE;-fx-text-fill:#5B21B6;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;-fx-border-color:#C4B5FD;-fx-border-radius:7;-fx-border-width:1;";
            private final String hDelete = "-fx-background-color:#FFE4E6;-fx-text-fill:#BE123C;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;-fx-border-color:#FECDD3;-fx-border-radius:7;-fx-border-width:1;";

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnView.setStyle(sView);
                btnEdit.setStyle(sEdit);
                btnDelete.setStyle(sDelete);

                btnView.setOnMouseEntered(e -> btnView.setStyle(hView));
                btnView.setOnMouseExited(e  -> btnView.setStyle(sView));
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(hEdit));
                btnEdit.setOnMouseExited(e  -> btnEdit.setStyle(sEdit));
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(hDelete));
                btnDelete.setOnMouseExited(e  -> btnDelete.setStyle(sDelete));

                btnView.setOnAction(e   ->
                        showLangueDetails(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e   ->
                        openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ── Charger données ────────────────────────────────────────────
    private void loadData() {
        try {
            List<Langue> list = service.recuperer();
            allData = FXCollections.observableArrayList(list);
            tableLangues.setItems(allData);
            countLabel.setText(allData.size() + " langue(s)");
            adjustTableHeight();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible de charger les langues :\n" + e.getMessage());
        }
    }

    // ── Recherche ──────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableLangues.setItems(allData);
            countLabel.setText(allData.size() + " langue(s)");
        } else {
            ObservableList<Langue> filtered = allData.stream()
                    .filter(l ->
                            l.getNom().toLowerCase().contains(q) ||
                                    (l.getDescription() != null &&
                                            l.getDescription().toLowerCase().contains(q)) ||
                                    (l.getPopularite() != null &&
                                            l.getPopularite().toLowerCase().contains(q))
                    )
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableLangues.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
        adjustTableHeight();
    }

    // ── Dialog Détails ─────────────────────────────────────────────
    private void showLangueDetails(Langue l) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + l.getNom());
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setPadding(new Insets(25));
        content.setPrefWidth(440);

        // Header drapeau + nom + statut
        HBox header = new HBox(16);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:#F5F3FF;-fx-background-radius:12;" +
                        "-fx-padding:16 20 16 20;"
        );
        if (l.getDrapeau() != null && !l.getDrapeau().isBlank()) {
            try {
                File f = new File(l.getDrapeau());
                if (f.exists()) {
                    ImageView flag = new ImageView(new Image(f.toURI().toString()));
                    flag.setFitWidth(70);
                    flag.setFitHeight(50);
                    flag.setPreserveRatio(true);
                    header.getChildren().add(flag);
                }
            } catch (Exception ignored) {}
        }
        VBox headerText = new VBox(4);
        Label nameLabel = new Label(l.getNom());
        nameLabel.setStyle(
                "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;"
        );
        Label statusLabel = new Label(l.isActive() ? "● Active" : "● Inactive");
        statusLabel.setStyle(l.isActive()
                ? "-fx-font-size:12px;-fx-text-fill:#059669;-fx-font-weight:bold;"
                : "-fx-font-size:12px;-fx-text-fill:#E11D48;-fx-font-weight:bold;"
        );
        headerText.getChildren().addAll(nameLabel, statusLabel);
        header.getChildren().add(headerText);

        // Grille infos
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(12);
        grid.setStyle(
                "-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;"
        );
        ColumnConstraints c1 = new ColumnConstraints(110);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        String ls = "-fx-font-size:12px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1A1D2E;";

        addGridRow(grid, 0, "ID",           String.valueOf(l.getId()), ls, vs);
        addGridRow(grid, 1, "Popularité",
                l.getPopularite() != null ? l.getPopularite() : "—", ls, vs);
        addGridRow(grid, 2, "Date d'ajout",
                l.getDateAjout() != null ? l.getDateAjout().toString() : "—", ls, vs);
        addGridRow(grid, 3, "Mise à jour",
                l.getUpdatedAt() != null
                        ? l.getUpdatedAt().toLocalDate().toString() : "—", ls, vs);

        // Description
        Label descTitle = new Label("Description");
        descTitle.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#4A4D6A;"
        );
        TextArea descArea = new TextArea(
                l.getDescription() != null && !l.getDescription().isBlank()
                        ? l.getDescription() : "Aucune description."
        );
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefHeight(80);
        descArea.setStyle(
                "-fx-background-color:#F4F5FA;-fx-background-radius:10;" +
                        "-fx-border-color:#E0E3F0;-fx-border-radius:10;-fx-font-size:13px;"
        );

        content.getChildren().addAll(header, grid, descTitle, descArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle(
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:8;-fx-padding:8 20 8 20;-fx-cursor:hand;"
        );
        dialog.showAndWait();
    }

    private void addGridRow(GridPane grid, int row,
                            String label, String value,
                            String ls, String vs) {
        Label l = new Label(label); l.setStyle(ls);
        Label v = new Label(value); v.setStyle(vs);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    // ── Choisir image ──────────────────────────────────────────────
    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un drapeau");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        Stage stage = (Stage) fieldNom.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedImageFile = file;
            fieldDrapeau.setText(file.getName());
            try {
                imagePreview.setImage(new Image(file.toURI().toString()));
                imagePlaceholder.setVisible(false);
            } catch (Exception ex) {
                imagePlaceholder.setVisible(true);
            }
        }
    }

    private String saveImageToResources(File source) throws IOException {
        Path dir = Paths.get(IMAGE_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String fileName = System.currentTimeMillis() + "_" + source.getName();
        Path dest = dir.resolve(fileName);
        Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString();
    }

    // ── Ajouter ────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() {
        selectedLangue    = null;
        selectedImageFile = null;
        clearForm();
        formTitle.setText("Nouvelle Langue");
        formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    // ── Modifier ───────────────────────────────────────────────────
    private void openEditForm(Langue l) {
        selectedLangue    = l;
        selectedImageFile = null;
        fieldNom.setText(l.getNom());
        fieldDescription.setText(l.getDescription() != null ? l.getDescription() : "");
        fieldDrapeau.setText(l.getDrapeau() != null ? l.getDrapeau() : "");
        comboPopularite.setValue(l.getPopularite());
        checkActive.setSelected(l.isActive());
        if (l.getDrapeau() != null && !l.getDrapeau().isBlank()) {
            try {
                File f = new File(l.getDrapeau());
                if (f.exists()) {
                    imagePreview.setImage(new Image(f.toURI().toString()));
                    imagePlaceholder.setVisible(false);
                }
            } catch (Exception ignored) {}
        }
        formTitle.setText("Modifier la Langue");
        formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    // ── Enregistrer ────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        try {
            String imagePath = selectedLangue != null
                    ? (selectedLangue.getDrapeau() != null ? selectedLangue.getDrapeau() : "")
                    : "";
            if (selectedImageFile != null)
                imagePath = saveImageToResources(selectedImageFile);

            if (selectedLangue == null) {
                Langue newL = new Langue(
                        fieldNom.getText().trim(),
                        imagePath,
                        LocalDateTime.now(),
                        fieldDescription.getText().trim(),
                        comboPopularite.getValue() != null ? comboPopularite.getValue() : "",
                        LocalDate.now(),
                        checkActive.isSelected()
                );
                service.ajouter(newL);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Langue ajoutée avec succès !");
            } else {
                selectedLangue.setNom(fieldNom.getText().trim());
                selectedLangue.setDescription(fieldDescription.getText().trim());
                selectedLangue.setPopularite(comboPopularite.getValue());
                selectedLangue.setActive(checkActive.isSelected());
                selectedLangue.setUpdatedAt(LocalDateTime.now());
                if (!imagePath.isBlank()) selectedLangue.setDrapeau(imagePath);
                service.modifier(selectedLangue);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Langue modifiée avec succès !");
            }
            handleCancel();
            loadData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur image",
                    "Impossible de copier l'image : " + e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    // ── Supprimer ──────────────────────────────────────────────────
    private void handleDelete(Langue l) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer \"" + l.getNom() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(l.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "🗑 Langue supprimée.");
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
        selectedLangue    = null;
        selectedImageFile = null;
    }

    // ── Validation ─────────────────────────────────────────────────
    private boolean validateForm() {
        if (fieldNom.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "⚠ Le nom de la langue est obligatoire.");
            fieldNom.requestFocus();
            return false;
        }
        return true;
    }

    // ── Clear formulaire ───────────────────────────────────────────
    private void clearForm() {
        fieldNom.clear();
        fieldDescription.clear();
        fieldDrapeau.clear();
        comboPopularite.setValue(null);
        checkActive.setSelected(false);
        imagePreview.setImage(null);
        imagePlaceholder.setVisible(true);
    }

    // ── Alert ──────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}