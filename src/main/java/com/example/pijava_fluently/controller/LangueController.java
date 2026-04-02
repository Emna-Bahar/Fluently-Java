package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.services.LangueService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private Label     formTitleIcon;
    @FXML private TextField fieldNom;
    @FXML private TextArea  fieldDescription;
    @FXML private TextField fieldDrapeau;
    @FXML private ComboBox<String> comboPopularite;
    @FXML private CheckBox  checkActive;
    @FXML private ImageView imagePreview;
    @FXML private Label     imagePlaceholder;
    @FXML private Label     countLabel;

    // ── Table ──────────────────────────────────────────────────────
    @FXML private TableView<Langue>            tableLangues;
    @FXML private TableColumn<Langue, Integer> colId;
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
    private Langue  selectedLangue   = null;
    private File    selectedImageFile = null;

    // Dossier où les images sont copiées
    private static final String IMAGE_DIR = "src/main/resources/com/example/pijava_fluently/image/";

    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        comboPopularite.setItems(FXCollections.observableArrayList(
                "⭐ Très populaire", "🔥 Populaire", "📈 En croissance", "🌱 Émergente", "💤 Peu demandée"
        ));
        setupColumns();
        loadData();
    }

    // ── Configuration colonnes ─────────────────────────────────────
    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPopularite.setCellValueFactory(new PropertyValueFactory<>("popularite"));

        // Colonne Drapeau — afficher l'image
        colDrapeau.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(40);
                iv.setFitHeight(30);
                iv.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isBlank()) {
                    setGraphic(new Label("—"));
                } else {
                    try {
                        File f = new File(path);
                        if (f.exists()) {
                            iv.setImage(new Image(f.toURI().toString()));
                            setGraphic(iv);
                        } else {
                            setGraphic(new Label("🖼"));
                        }
                    } catch (Exception e) {
                        setGraphic(new Label("🖼"));
                    }
                }
                setText(null);
            }
        });
        colDrapeau.setCellValueFactory(new PropertyValueFactory<>("drapeau"));

        // Colonne Statut — badge coloré
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setGraphic(null); return; }
                Label badge = new Label(val ? "✓ Active" : "✗ Inactive");
                badge.setStyle(val
                        ? "-fx-background-color:#E8F5E9;-fx-text-fill:#2E7D32;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 10 4 10;"
                        : "-fx-background-color:#FFEBEE;-fx-text-fill:#C62828;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 10 4 10;"
                );
                setGraphic(badge);
                setText(null);
            }
        });
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Colonne Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏ Modifier");
            private final Button btnDelete = new Button("🗑");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);

            {
                box.setStyle("-fx-alignment:CENTER;");
                btnEdit.setStyle(
                        "-fx-background-color:#EEF0FF;-fx-text-fill:#6C63FF;" +
                                "-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-background-radius:8;-fx-padding:6 12 6 12;-fx-cursor:hand;"
                );
                btnDelete.setStyle(
                        "-fx-background-color:#FFEBEE;-fx-text-fill:#E53935;" +
                                "-fx-font-size:13px;-fx-background-radius:8;" +
                                "-fx-padding:6 10 6 10;-fx-cursor:hand;"
                );
                btnEdit.setOnAction(e ->
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

    // ── Chargement données ─────────────────────────────────────────
    private void loadData() {
        try {
            List<Langue> list = service.recuperer();
            allData = FXCollections.observableArrayList(list);
            tableLangues.setItems(allData);
            countLabel.setText(allData.size() + " langue(s)");
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
                                    (l.getDescription() != null && l.getDescription().toLowerCase().contains(q)) ||
                                    (l.getPopularite() != null && l.getPopularite().toLowerCase().contains(q))
                    )
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableLangues.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    // ── Choisir image ──────────────────────────────────────────────
    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un drapeau");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.svg")
        );
        Stage stage = (Stage) fieldNom.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedImageFile = file;
            fieldDrapeau.setText(file.getName());
            // Prévisualiser
            try {
                Image img = new Image(file.toURI().toString());
                imagePreview.setImage(img);
                imagePlaceholder.setVisible(false);
            } catch (Exception ex) {
                imagePlaceholder.setVisible(true);
            }
        }
    }

    // ── Copier l'image dans le dossier resources/image ─────────────
    private String saveImageToResources(File source) throws IOException {
        Path dir = Paths.get(IMAGE_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String fileName = System.currentTimeMillis() + "_" + source.getName();
        Path dest = dir.resolve(fileName);
        Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString(); // chemin absolu stocké en BD
    }

    // ── Ouvrir formulaire AJOUT ────────────────────────────────────
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

    // ── Ouvrir formulaire MODIFICATION ────────────────────────────
    private void openEditForm(Langue l) {
        selectedLangue    = l;
        selectedImageFile = null;
        fieldNom.setText(l.getNom());
        fieldDescription.setText(l.getDescription());
        fieldDrapeau.setText(l.getDrapeau() != null ? l.getDrapeau() : "");
        comboPopularite.setValue(l.getPopularite());
        checkActive.setSelected(l.isActive());
        // Charger preview si image existe
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
            // Copier l'image si une nouvelle a été choisie
            String imagePath = selectedLangue != null ? selectedLangue.getDrapeau() : "";
            if (selectedImageFile != null) {
                imagePath = saveImageToResources(selectedImageFile);
            }

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
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Langue ajoutée avec succès !");
            } else {
                selectedLangue.setNom(fieldNom.getText().trim());
                selectedLangue.setDescription(fieldDescription.getText().trim());
                selectedLangue.setPopularite(comboPopularite.getValue());
                selectedLangue.setActive(checkActive.isSelected());
                selectedLangue.setUpdatedAt(LocalDateTime.now());
                if (!imagePath.isBlank()) selectedLangue.setDrapeau(imagePath);
                service.modifier(selectedLangue);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Langue modifiée avec succès !");
            }
            handleCancel();
            loadData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur image", "Impossible de copier l'image : " + e.getMessage());
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
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ Le nom de la langue est obligatoire.");
            fieldNom.requestFocus();
            return false;
        }
        return true;
    }

    // ── Réinitialiser formulaire ───────────────────────────────────
    private void clearForm() {
        fieldNom.clear();
        fieldDescription.clear();
        fieldDrapeau.clear();
        comboPopularite.setValue(null);
        checkActive.setSelected(false);
        imagePreview.setImage(null);
        imagePlaceholder.setVisible(true);
    }

    // ── Alerte ─────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}