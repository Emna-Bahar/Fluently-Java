package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
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
import java.util.List;
import java.util.stream.Collectors;

public class NiveauController {

    // Formulaire
    @FXML private VBox formCard;
    @FXML private Label formTitle;
    @FXML private Label formTitleIcon;
    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private ComboBox<String> comboDifficulte;
    @FXML private TextField fieldOrdre;
    @FXML private TextField fieldImageCouverture;
    @FXML private ComboBox<Langue> comboLangue;

    // Image preview
    @FXML private ImageView imagePreview;
    @FXML private Label imagePlaceholder;
    @FXML private StackPane imagePreviewPane;

    // Table
    @FXML private TableView<Niveau> tableNiveaux;
    @FXML private TableColumn<Niveau, String> colTitre;
    @FXML private TableColumn<Niveau, String> colImage;
    @FXML private TableColumn<Niveau, String> colDifficulte;
    @FXML private TableColumn<Niveau, Integer> colOrdre;
    @FXML private TableColumn<Niveau, String> colLangue;
    @FXML private TableColumn<Niveau, Void> colActions;

    // Recherche
    @FXML private TextField searchField;
    @FXML private Label countLabel;

    // Services
    private final NiveauService niveauService = new NiveauService();
    private final LangueService langueService = new LangueService();
    private ObservableList<Niveau> allData = FXCollections.observableArrayList();
    private List<Langue> allLangues;
    private Niveau selectedNiveau = null;
    private File selectedImageFile = null;

    private static final String IMAGE_DIR = "src/main/resources/com/example/pijava_fluently/image/niveaux/";

    @FXML
    public void initialize() {
        setupComboBoxes();
        loadLangues();
        tableNiveaux.setFixedCellSize(52);
        setupColumns();
        loadData();
    }

    private void setupComboBoxes() {
        comboDifficulte.setItems(FXCollections.observableArrayList(
                "A1 - Débutant",
                "A2 - Élémentaire",
                "B1 - Intermédiaire",
                "B2 - Intermédiaire supérieur",
                "C1 - Avancé",
                "C2 - Maîtrise"
        ));
    }

    private void loadLangues() {
        try {
            allLangues = langueService.recuperer();
            comboLangue.setItems(FXCollections.observableArrayList(allLangues));
            comboLangue.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Langue l, boolean empty) {
                    super.updateItem(l, empty);
                    setText(empty || l == null ? null : l.getNom() + " (" + l.getPopularite() + ")");
                }
            });
            comboLangue.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Langue l, boolean empty) {
                    super.updateItem(l, empty);
                    setText(empty || l == null ? null : l.getNom());
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les langues.");
        }
    }

    private void adjustTableHeight() {
        int count = tableNiveaux.getItems().size();
        double h = count * 52 + 35;
        tableNiveaux.setPrefHeight(h);
        tableNiveaux.setMinHeight(h);
        tableNiveaux.setMaxHeight(h);
    }

    private void setupColumns() {

        // Colonne Titre
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1A1D2E;");
            }
        });

        // Colonne Image avec preview
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageCouverture"));
        colImage.setCellFactory(col -> new TableCell<>() {
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

        // Colonne Difficulté
        colDifficulte.setCellValueFactory(new PropertyValueFactory<>("difficulte"));
        colDifficulte.setCellFactory(col -> new TableCell<>() {
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

        // Colonne Ordre
        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        colOrdre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(String.valueOf(item));
                badge.setStyle(
                        "-fx-background-color:#F0FDF4;-fx-text-fill:#059669;" +
                                "-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-background-radius:20;-fx-padding:4 10 4 10;"
                );
                setGraphic(badge); setText(null);
            }
        });

        // Colonne Langue
        colLangue.setCellValueFactory(cellData -> {
            int idLangue = cellData.getValue().getIdLangueId();
            String nomLangue = allLangues == null ? String.valueOf(idLangue) :
                    allLangues.stream()
                            .filter(l -> l.getId() == idLangue)
                            .map(Langue::getNom)
                            .findFirst()
                            .orElse(String.valueOf(idLangue));
            return new javafx.beans.property.SimpleStringProperty(nomLangue);
        });
        colLangue.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item.isBlank()) {
                    setText("—"); setGraphic(null); return;
                }
                Label badge = new Label(item);
                badge.setStyle(
                        "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;" +
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
                        showNiveauDetails(getTableView().getItems().get(getIndex())));
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

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(niveauService.recuperer());
            tableNiveaux.setItems(allData);
            countLabel.setText(allData.size() + " niveau(x)");
            adjustTableHeight();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les niveaux : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableNiveaux.setItems(allData);
            countLabel.setText(allData.size() + " niveau(x)");
        } else {
            ObservableList<Niveau> filtered = allData.stream()
                    .filter(n -> n.getTitre().toLowerCase().contains(q)
                            || (n.getDifficulte() != null && n.getDifficulte().toLowerCase().contains(q)))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableNiveaux.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
        adjustTableHeight();
    }

    private void showNiveauDetails(Niveau n) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + n.getTitre());
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setPadding(new Insets(25));
        content.setPrefWidth(440);

        // Header
        HBox header = new HBox(16);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:#F5F3FF;-fx-background-radius:12;" +
                        "-fx-padding:16 20 16 20;"
        );
        if (n.getImageCouverture() != null && !n.getImageCouverture().isBlank()) {
            try {
                File f = new File(n.getImageCouverture());
                if (f.exists()) {
                    ImageView img = new ImageView(new Image(f.toURI().toString()));
                    img.setFitWidth(70);
                    img.setFitHeight(50);
                    img.setPreserveRatio(true);
                    header.getChildren().add(img);
                }
            } catch (Exception ignored) {}
        }
        VBox headerText = new VBox(4);
        Label nameLabel = new Label(n.getTitre());
        nameLabel.setStyle(
                "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;"
        );
        headerText.getChildren().addAll(nameLabel);
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

        // Trouver la langue
        String langueNom = allLangues.stream()
                .filter(l -> l.getId() == n.getIdLangueId())
                .map(Langue::getNom)
                .findFirst()
                .orElse("Non définie");

        addGridRow(grid, 0, "ID",           String.valueOf(n.getId()), ls, vs);
        addGridRow(grid, 1, "Difficulté",   n.getDifficulte(), ls, vs);
        addGridRow(grid, 2, "Ordre",        String.valueOf(n.getOrdre()), ls, vs);
        addGridRow(grid, 3, "Langue",       langueNom, ls, vs);

        // Description
        Label descTitle = new Label("Description");
        descTitle.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#4A4D6A;"
        );
        TextArea descArea = new TextArea(
                n.getDescription() != null && !n.getDescription().isBlank()
                        ? n.getDescription() : "Aucune description."
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

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image de couverture");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        Stage stage = (Stage) fieldTitre.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedImageFile = file;
            fieldImageCouverture.setText(file.getName());
            try {
                Image img = new Image(file.toURI().toString());
                imagePreview.setImage(img);
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

    @FXML
    private void handleAjouter() {
        selectedNiveau = null;
        selectedImageFile = null;
        clearForm();
        formTitle.setText("Nouveau Niveau");
        formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void openEditForm(Niveau n) {
        selectedNiveau = n;
        selectedImageFile = null;
        fieldTitre.setText(n.getTitre());
        fieldDescription.setText(n.getDescription());
        comboDifficulte.setValue(n.getDifficulte());
        fieldOrdre.setText(String.valueOf(n.getOrdre()));
        fieldImageCouverture.setText(n.getImageCouverture());

        if (n.getImageCouverture() != null && !n.getImageCouverture().isBlank()) {
            try {
                File f = new File(n.getImageCouverture());
                if (f.exists()) {
                    imagePreview.setImage(new Image(f.toURI().toString()));
                    imagePlaceholder.setVisible(false);
                }
            } catch (Exception ignored) {}
        }

        allLangues.stream()
                .filter(l -> l.getId() == n.getIdLangueId())
                .findFirst()
                .ifPresent(comboLangue::setValue);

        formTitle.setText("Modifier le Niveau");
        formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        try {
            int ordre = Integer.parseInt(fieldOrdre.getText().trim());
            int idLangue = comboLangue.getValue().getId();

            String imagePath = selectedNiveau != null ? selectedNiveau.getImageCouverture() : "";
            if (selectedImageFile != null) {
                imagePath = saveImageToResources(selectedImageFile);
            }

            if (selectedNiveau == null) {
                Niveau newN = new Niveau(
                        fieldTitre.getText().trim(),
                        fieldDescription.getText().trim(),
                        imagePath,
                        comboDifficulte.getValue(),
                        ordre, 0, 0, idLangue
                );
                niveauService.ajouter(newN);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Niveau ajouté avec succès !");
            } else {
                selectedNiveau.setTitre(fieldTitre.getText().trim());
                selectedNiveau.setDescription(fieldDescription.getText().trim());
                selectedNiveau.setDifficulte(comboDifficulte.getValue());
                selectedNiveau.setOrdre(ordre);
                selectedNiveau.setImageCouverture(imagePath);
                selectedNiveau.setIdLangueId(idLangue);
                niveauService.modifier(selectedNiveau);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Niveau modifié avec succès !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "⚠ L'ordre doit être un nombre.");
        } catch (SQLException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void handleDelete(Niveau n) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le niveau \"" + n.getTitre() + "\" ?\nCette action est irréversible.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    niveauService.supprimer(n.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "🗑 Niveau supprimé !");
                    loadData();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        clearForm();
        selectedNiveau = null;
        selectedImageFile = null;
    }

    private boolean validateForm() {
        if (fieldTitre.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "⚠ Le titre est obligatoire.");
            fieldTitre.requestFocus();
            return false;
        }
        if (comboDifficulte.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "⚠ Veuillez sélectionner une difficulté.");
            comboDifficulte.requestFocus();
            return false;
        }
        if (comboLangue.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "⚠ Veuillez sélectionner une langue.");
            comboLangue.requestFocus();
            return false;
        }
        if (fieldOrdre.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "⚠ L'ordre est obligatoire.");
            fieldOrdre.requestFocus();
            return false;
        }
        return true;
    }

    private void clearForm() {
        fieldTitre.clear();
        fieldDescription.clear();
        comboDifficulte.setValue(null);
        fieldOrdre.clear();
        fieldImageCouverture.clear();
        comboLangue.setValue(null);
        imagePreview.setImage(null);
        imagePlaceholder.setVisible(true);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}