package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Cours;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.services.CoursService;
import com.example.pijava_fluently.services.NiveauService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class CoursController {

    // Formulaire
    @FXML private VBox formCard;
    @FXML private Label formTitle;
    @FXML private Label formTitleIcon;
    @FXML private TextField fieldNumero;
    @FXML private DatePicker fieldDate;
    @FXML private ComboBox<Niveau> comboNiveau;
    @FXML private ComboBox<Cours> comboPrecedent;
    @FXML private Label labelErreur;
    // Ressources
    @FXML private ListView<String> ressourcesList;

    // Table
    @FXML private TableView<Cours> tableCours;
    @FXML private TableColumn<Cours, Integer> colNumero;
    @FXML private TableColumn<Cours, String> colTitre;
    @FXML private TableColumn<Cours, String> colNiveau;
    @FXML private TableColumn<Cours, String> colDate;
    @FXML private TableColumn<Cours, String> colRessource;
    @FXML private TableColumn<Cours, Void> colActions;

    // Recherche
    @FXML private TextField searchField;
    @FXML private Label countLabel;

    // Services
    private final CoursService coursService = new CoursService();
    private final NiveauService niveauService = new NiveauService();
    private ObservableList<Cours> allData = FXCollections.observableArrayList();
    private List<Niveau> allNiveaux;
    private Cours selectedCours = null;
    private ObservableList<String> ressources = FXCollections.observableArrayList();

    private static final String RESSOURCES_DIR = "src/main/resources/com/example/pijava_fluently/image/ressources/";

    @FXML
    public void initialize() {
        loadNiveaux();
        tableCours.setFixedCellSize(52);
        setupColumns();
        loadData();
        ressourcesList.setItems(ressources);

        // Double-clic pour ouvrir une ressource
        ressourcesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = ressourcesList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    ouvrirRessource(selected);
                }
            }
        });
    }

    private void loadNiveaux() {
        try {
            allNiveaux = niveauService.recuperer();
            comboNiveau.setItems(FXCollections.observableArrayList(allNiveaux));

            comboNiveau.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Niveau n, boolean empty) {
                    super.updateItem(n, empty);
                    setText(empty || n == null ? null : n.getTitre() + " (" + n.getDifficulte() + ")");
                }
            });
            comboNiveau.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Niveau n, boolean empty) {
                    super.updateItem(n, empty);
                    setText(empty || n == null ? null : n.getTitre());
                }
            });

            comboNiveau.setOnAction(e -> {
                if (comboNiveau.getValue() != null) {
                    refreshComboPrecedent(comboNiveau.getValue().getId());
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les niveaux.");
        }
    }

    private void refreshComboPrecedent(int idNiveau) {
        ObservableList<Cours> coursDuNiveau = allData.stream()
                .filter(c -> c.getIdNiveauId() == idNiveau)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        Cours placeholder = new Cours();
        placeholder.setId(0);
        placeholder.setNumero(0);
        coursDuNiveau.add(0, placeholder);

        comboPrecedent.setItems(coursDuNiveau);

        comboPrecedent.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Cours c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else if (c.getId() == 0) {
                    setText("— Aucun —");
                } else {
                    setText("Cours N°" + c.getNumero());
                }
            }
        });
        comboPrecedent.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Cours c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText("Aucun");
                } else if (c.getId() == 0) {
                    setText("— Aucun —");
                } else {
                    setText("Cours N°" + c.getNumero());
                }
            }
        });
    }

    private void adjustTableHeight() {
        int count = tableCours.getItems().size();
        double h = count * 52 + 35;
        tableCours.setPrefHeight(h);
        tableCours.setMinHeight(h);
        tableCours.setMaxHeight(h);
    }

    private void setupColumns() {

        // Colonne Numéro
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(String.valueOf(item));
                badge.setStyle("-fx-background-color:#F3F0FF;-fx-text-fill:#6C63FF;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 10 4 10;");
                setGraphic(badge); setText(null);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        // Colonne Titre
        colTitre.setCellValueFactory(cellData -> {
            String titre = "Cours N°" + cellData.getValue().getNumero();
            return new SimpleStringProperty(titre);
        });
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1A1D2E;");
            }
        });

        // Colonne Niveau
        colNiveau.setCellValueFactory(cellData -> {
            int idNiveau = cellData.getValue().getIdNiveauId();
            String nom = allNiveaux == null ? String.valueOf(idNiveau) :
                    allNiveaux.stream()
                            .filter(n -> n.getId() == idNiveau)
                            .map(Niveau::getTitre)
                            .findFirst()
                            .orElse(String.valueOf(idNiveau));
            return new SimpleStringProperty(nom);
        });
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item.isBlank()) {
                    setText("—"); setGraphic(null); return;
                }
                Label badge = new Label(item);
                badge.setStyle(
                        "-fx-background-color:#F0FDF4;-fx-text-fill:#166534;-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-background-radius:20;-fx-padding:4 10 4 10;"
                );
                setGraphic(badge); setText(null);
            }
        });

        // Colonne Date
        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDateCreation() != null
                                ? cellData.getValue().getDateCreation().toString() : ""
                )
        );
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item.isEmpty()) {
                    setText(null); setGraphic(null); return;
                }
                Label badge = new Label(item);
                badge.setStyle(
                        "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-background-radius:20;-fx-padding:4 10 4 10;"
                );
                setGraphic(badge); setText(null);
            }
        });

        // Colonne Ressources
        colRessource.setCellValueFactory(cellData -> {
            String ress = cellData.getValue().getRessource();
            if (ress == null || ress.isEmpty()) {
                return new SimpleStringProperty("0");
            }
            String[] items = ress.split("\n");
            return new SimpleStringProperty(String.valueOf(items.length));
        });
        colRessource.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item.equals("0")) {
                    setText("—"); setGraphic(null); return;
                }
                Label badge = new Label(item + " ressource(s)");
                badge.setStyle(
                        "-fx-background-color:#EEF0FF;-fx-text-fill:#6C63FF;-fx-font-size:11px;-fx-font-weight:bold;" +
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

                btnView.setOnAction(e   -> showCoursDetails(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e   -> openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
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
            allData = FXCollections.observableArrayList(coursService.recuperer());
            tableCours.setItems(allData);
            updateCountLabel(allData.size());
            adjustTableHeight();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les cours : " + e.getMessage());
        }
    }

    private void updateCountLabel(int count) {
        countLabel.setText(count + " cours");
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableCours.setItems(allData);
            updateCountLabel(allData.size());
        } else {
            ObservableList<Cours> filtered = allData.stream()
                    .filter(c -> String.valueOf(c.getNumero()).contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableCours.setItems(filtered);
            updateCountLabel(filtered.size());
        }
        adjustTableHeight();
    }

    @FXML
    private void handleAddFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ajouter un fichier ressource");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Vidéo", "*.mp4", "*.webm", "*.mov"),
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) fieldNumero.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                Path dest = saveFileToResources(file);
                ressources.add(dest.toString());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de copier le fichier");
            }
        }
    }

    @FXML
    private void handleAddYoutubeLink() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Lien YouTube");
        dialog.setHeaderText("Ajouter un lien YouTube");
        dialog.setContentText("Entrez l'URL de la vidéo YouTube :");
        dialog.showAndWait().ifPresent(url -> {
            if (url != null && !url.trim().isEmpty()) {
                ressources.add(url.trim());
            }
        });
    }

    @FXML
    private void handleRemoveRessource() {
        String selected = ressourcesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ressources.remove(selected);
        }
    }

    private Path saveFileToResources(File source) throws IOException {
        Path dir = Paths.get(RESSOURCES_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String fileName = System.currentTimeMillis() + "_" + source.getName();
        Path dest = dir.resolve(fileName);
        Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    private void ouvrirRessource(String chemin) {
        try {
            // Vérifier si c'est un lien YouTube
            if (chemin.contains("youtube.com") || chemin.contains("youtu.be")) {
                Desktop.getDesktop().browse(new URI(chemin));
            }
            // Vérifier si c'est un fichier local
            else {
                File file = new File(chemin);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert(Alert.AlertType.WARNING, "Erreur", "Fichier introuvable : " + chemin);
                }
            }
        } catch (IOException | URISyntaxException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la ressource : " + e.getMessage());
        }
    }

    @FXML
    private void handleAjouter() {
        selectedCours = null;
        clearForm();
        formTitle.setText("Nouveau Cours");
        formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void openEditForm(Cours c) {
        selectedCours = c;
        fieldNumero.setText(String.valueOf(c.getNumero()));
        fieldDate.setValue(c.getDateCreation());

        ressources.clear();
        if (c.getRessource() != null && !c.getRessource().isEmpty()) {
            String[] items = c.getRessource().split("\n");
            ressources.addAll(items);
        }

        allNiveaux.stream()
                .filter(n -> n.getId() == c.getIdNiveauId())
                .findFirst().ifPresent(comboNiveau::setValue);

        if (c.getCoursPrecedentIdId() != 0) {
            allData.stream()
                    .filter(cc -> cc.getId() == c.getCoursPrecedentIdId())
                    .findFirst().ifPresent(comboPrecedent::setValue);
        }

        formTitle.setText("Modifier le Cours");
        formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void showCoursDetails(Cours c) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — Cours N°" + c.getNumero());
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setPadding(new Insets(25));
        content.setPrefWidth(480);

        // Header
        HBox header = new HBox(16);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#F5F3FF;-fx-background-radius:12;-fx-padding:16 20 16 20;");

        VBox headerText = new VBox(4);
        Label nameLabel = new Label("Cours N°" + c.getNumero());
        nameLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");
        headerText.getChildren().addAll(nameLabel);
        header.getChildren().add(headerText);

        // Grille infos
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints c1 = new ColumnConstraints(110);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        String ls = "-fx-font-size:12px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1A1D2E;";

        String niveauNom = allNiveaux.stream()
                .filter(n -> n.getId() == c.getIdNiveauId())
                .map(Niveau::getTitre)
                .findFirst().orElse("Non défini");

        String precedent = "Aucun";
        if (c.getCoursPrecedentIdId() != 0) {
            precedent = allData.stream()
                    .filter(cc -> cc.getId() == c.getCoursPrecedentIdId())
                    .map(cc -> "Cours N°" + cc.getNumero())
                    .findFirst().orElse("Non défini");
        }

        addGridRow(grid, 0, "ID", String.valueOf(c.getId()), ls, vs);
        addGridRow(grid, 1, "Date création", c.getDateCreation().toString(), ls, vs);
        addGridRow(grid, 2, "Niveau", niveauNom, ls, vs);
        addGridRow(grid, 3, "Cours précédent", precedent, ls, vs);

        // Ressources avec boutons
        Label ressourcesTitle = new Label("📎 Ressources du cours");
        ressourcesTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;-fx-padding:10 0 5 0;");

        VBox ressourcesContainer = new VBox(8);
        ressourcesContainer.setStyle("-fx-background-color:#F4F5FA;-fx-background-radius:10;-fx-padding:10;");

        if (c.getRessource() != null && !c.getRessource().isEmpty()) {
            String[] ressourcesArray = c.getRessource().split("\n");
            for (String res : ressourcesArray) {
                HBox resBox = new HBox(10);
                resBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label resLabel = new Label(res);
                resLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#4A4D6A;");
                resLabel.setMaxWidth(300);
                resLabel.setWrapText(true);

                Button ouvrirBtn = new Button("Ouvrir");
                ouvrirBtn.setStyle("-fx-background-color:#6C63FF;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:5 12 5 12;-fx-cursor:hand;");
                ouvrirBtn.setOnAction(e -> ouvrirRessource(res));

                resBox.getChildren().addAll(resLabel, ouvrirBtn);
                ressourcesContainer.getChildren().add(resBox);
            }
        } else {
            Label emptyLabel = new Label("📭 Aucune ressource disponible");
            emptyLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#8A8FA8;-fx-padding:10;");
            ressourcesContainer.getChildren().add(emptyLabel);
        }

        content.getChildren().addAll(header, grid, ressourcesTitle, ressourcesContainer);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:#6C63FF;-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 20 8 20;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private void addGridRow(GridPane grid, int row, String label, String value, String ls, String vs) {
        Label l = new Label(label); l.setStyle(ls);
        Label v = new Label(value); v.setStyle(vs);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        if (!validateRessources()) return;

        try {
            int numero = Integer.parseInt(fieldNumero.getText().trim());
            int idNiv = comboNiveau.getValue().getId();
            int idPrec = (comboPrecedent.getValue() != null && comboPrecedent.getValue().getId() != 0)
                    ? comboPrecedent.getValue().getId() : 0;

            String ressourcesStr = String.join("\n", ressources);

            if (selectedCours == null) {
                Cours newC = new Cours(
                        numero,
                        ressourcesStr,
                        fieldDate.getValue(),
                        idPrec,
                        idNiv
                );
                coursService.ajouter(newC);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Cours ajouté !");
            } else {
                selectedCours.setNumero(numero);
                selectedCours.setRessource(ressourcesStr);
                selectedCours.setDateCreation(fieldDate.getValue());
                selectedCours.setIdNiveauId(idNiv);
                selectedCours.setCoursPrecedentIdId(idPrec);
                coursService.modifier(selectedCours);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Cours modifié !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            afficherErreur("Le numéro doit être un entier.");
        } catch (SQLException e) {
            afficherErreur("Erreur base de données : " + e.getMessage());
        }
    }
    private void handleDelete(Cours c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le cours N°" + c.getNumero() + " ?\nCette action est irréversible.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    coursService.supprimer(c.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "🗑 Cours supprimé !");
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
        selectedCours = null;
    }

    // ── Validation complète du formulaire ─────────────────────────────
    private boolean validateForm() {
        cacherErreur();

        // 1. Validation du numéro
        String numeroStr = fieldNumero.getText().trim();
        if (numeroStr.isEmpty()) {
            afficherErreur("Le numéro du cours est obligatoire.");
            fieldNumero.requestFocus();
            return false;
        }
        try {
            int numero = Integer.parseInt(numeroStr);
            if (numero <= 0) {
                afficherErreur("Le numéro doit être un nombre positif.");
                fieldNumero.requestFocus();
                return false;
            }
            if (numero > 999) {
                afficherErreur("Le numéro ne peut pas dépasser 999.");
                fieldNumero.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            afficherErreur("Le numéro doit être un nombre entier valide.");
            fieldNumero.requestFocus();
            return false;
        }

        // 2. Validation du niveau
        if (comboNiveau.getValue() == null) {
            afficherErreur("Veuillez sélectionner un niveau.");
            comboNiveau.requestFocus();
            return false;
        }

        // 3. Validation de la date
        if (fieldDate.getValue() == null) {
            afficherErreur("Veuillez sélectionner une date de création.");
            fieldDate.requestFocus();
            return false;
        }
        LocalDate today = LocalDate.now();
        if (fieldDate.getValue().isAfter(today)) {
            afficherErreur("La date de création ne peut pas être dans le futur.");
            fieldDate.requestFocus();
            return false;
        }

        // 4. Validation du cours précédent (auto-référence)
        if (comboPrecedent.getValue() != null && selectedCours != null) {
            Cours precedent = comboPrecedent.getValue();
            if (precedent.getId() != 0 && precedent.getId() == selectedCours.getId()) {
                afficherErreur("Un cours ne peut pas être son propre prédécesseur.");
                comboPrecedent.requestFocus();
                return false;
            }
        }

        // 5. Vérification du numéro unique dans le niveau
        if (!isNumeroUniqueDansNiveau(fieldNumero.getText().trim(),
                comboNiveau.getValue().getId(),
                selectedCours != null ? selectedCours.getId() : null)) {
            afficherErreur("Un cours avec ce numéro existe déjà dans ce niveau.");
            fieldNumero.requestFocus();
            return false;
        }

        return true;
    }

    // ── Vérifier l'unicité du numéro dans le niveau ───────────────────
    private boolean isNumeroUniqueDansNiveau(String numeroStr, int niveauId, Integer excludeId) {
        try {
            int numero = Integer.parseInt(numeroStr);
            List<Cours> tousLesCours = coursService.recuperer();

            for (Cours c : tousLesCours) {
                if (c.getIdNiveauId() == niveauId && c.getNumero() == numero) {
                    if (excludeId == null || c.getId() != excludeId) {
                        return false;
                    }
                }
            }
        } catch (SQLException | NumberFormatException e) {
            return false;
        }
        return true;
    }

    // ── Validation des ressources ─────────────────────────────────────
    private boolean validateRessources() {
        if (ressources.isEmpty()) {
            afficherErreur("Veuillez ajouter au moins une ressource (fichier ou lien YouTube).");
            return false;
        }

        for (String res : ressources) {
            // Vérifier les liens YouTube
            if (res.contains("youtube.com") || res.contains("youtu.be")) {
                if (!res.matches(".*(youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9_-]+.*")) {
                    afficherErreur("Format de lien YouTube invalide : " + res);
                    return false;
                }
            }
            // Vérifier les fichiers
            else if (!res.startsWith("src/main/resources/")) {
                File file = new File(res);
                if (!file.exists()) {
                    afficherErreur("Fichier introuvable : " + res);
                    return false;
                }
            }
        }
        return true;
    }

    // ── Afficher erreur ───────────────────────────────────────────────
    private void afficherErreur(String message) {
        if (labelErreur != null) {
            labelErreur.setText("⚠ " + message);
            labelErreur.setVisible(true);
            labelErreur.setManaged(true);
        } else {
            showAlert(Alert.AlertType.WARNING, "Validation", message);
        }
    }

    private void cacherErreur() {
        if (labelErreur != null) {
            labelErreur.setVisible(false);
            labelErreur.setManaged(false);
        }
    }

    private void clearForm() {
        fieldNumero.clear();
        fieldDate.setValue(LocalDate.now());
        comboNiveau.setValue(null);
        comboPrecedent.setValue(null);
        ressources.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}