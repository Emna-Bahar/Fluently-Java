package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
import com.example.pijava_fluently.services.TestService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class TestController {

    @FXML private VBox     formCard;
    @FXML private Label    formTitle;
    @FXML private TextField       fieldTitre;
    @FXML private ComboBox<String>  comboType;
    @FXML private TextField       fieldDuree;
    @FXML private ComboBox<Langue>  comboLangue;
    @FXML private ComboBox<Niveau>  comboNiveau;
    @FXML private Label    countLabel;
    @FXML private TextField       searchField;
    @FXML private Label    labelErreur;

    @FXML private TableView<Test>              tableTests;
    @FXML private TableColumn<Test, Integer>   colId;
    @FXML private TableColumn<Test, String>    colTitre;
    @FXML private TableColumn<Test, String>    colType;
    @FXML private TableColumn<Test, Integer>   colDuree;
    @FXML private TableColumn<Test, Integer>   colLangue;
    @FXML private TableColumn<Test, Integer>   colNiveau;
    @FXML private TableColumn<Test, Void>      colActions;

    private final TestService   service      = new TestService();
    private final LangueService langueService = new LangueService();
    private final NiveauService niveauService = new NiveauService();

    private ObservableList<Test>   allData  = FXCollections.observableArrayList();
    private List<Langue>           langues;
    private List<Niveau>           niveaux;
    private Test selectedTest = null;

    // ── Init ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList(
                "Test de niveau", "Test de fin de niveau", "quiz_debutant"
        ));
        chargerLanguesEtNiveaux();
        setupColumns();
        loadData();
    }

    private void chargerLanguesEtNiveaux() {
        try {
            langues = langueService.recuperer();
            comboLangue.setItems(FXCollections.observableArrayList(langues));
            comboLangue.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Langue l, boolean empty) {
                    super.updateItem(l, empty);
                    setText(empty || l == null ? null : l.getNom());
                }
            });
            comboLangue.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Langue l, boolean empty) {
                    super.updateItem(l, empty);
                    setText(empty || l == null ? "— Sélectionner —" : l.getNom());
                }
            });
            // Quand la langue change, filtrer les niveaux
            comboLangue.valueProperty().addListener((obs, old, newL) -> {
                filtrerNiveaux(newL);
            });
        } catch (SQLException e) { e.printStackTrace(); }

        try {
            niveaux = niveauService.recuperer();
            configurerComboNiveau(niveaux);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filtrerNiveaux(Langue langue) {
        comboNiveau.setValue(null);
        if (langue == null) {
            configurerComboNiveau(niveaux);
        } else {
            List<Niveau> filtrés = niveaux.stream()
                    .filter(n -> n.getIdLangueId() == langue.getId())
                    .collect(Collectors.toList());
            configurerComboNiveau(filtrés);
        }
    }

    private void configurerComboNiveau(List<Niveau> liste) {
        comboNiveau.setItems(FXCollections.observableArrayList(liste));
        comboNiveau.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Niveau n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null
                        : n.getTitre() + " (" + n.getDifficulte() + ")");
            }
        });
        comboNiveau.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Niveau n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "— Sélectionner —"
                        : n.getTitre() + " (" + n.getDifficulte() + ")");
            }
        });
    }

    // ── Colonnes ─────────────────────────────────────────────────────
    private void setupColumns() {

        // Masquer colonne ID (pas d'info utile pour l'admin)
        colId.setVisible(false);

        // Titre
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle(
                        "-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1A1D2E;");
                lbl.setWrapText(true);
                setGraphic(lbl); setText(null);
            }
        });

        // Type — badge coloré
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setGraphic(null); return; }
                String color = switch (item) {
                    case "Test de niveau" ->
                            "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;" +
                                    "-fx-border-color:#BFDBFE;";
                    case "Test de fin de niveau" ->
                            "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;" +
                                    "-fx-border-color:#BBF7D0;";
                    default ->
                            "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;" +
                                    "-fx-border-color:#FDE68A;";
                };
                Label badge = new Label(item);
                badge.setStyle(color +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-border-radius:20;" +
                        "-fx-border-width:1;-fx-padding:4 10;");
                setGraphic(badge); setText(null);
            }
        });

        // Durée
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeEstimee"));
        colDuree.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label("⏱ " + item + " min");
                lbl.setStyle(
                        "-fx-font-size:12px;-fx-text-fill:#6B7280;" +
                                "-fx-background-color:#F4F5FA;-fx-background-radius:8;-fx-padding:3 8;");
                setGraphic(lbl); setText(null);
            }
        });

        // Langue — affiche le NOM
        colLangue.setCellValueFactory(new PropertyValueFactory<>("langueId"));
        colLangue.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item == 0) {
                    setText("—"); setGraphic(null); return;
                }
                String nom = langues == null ? "#" + item :
                        langues.stream().filter(l -> l.getId() == item)
                                .map(Langue::getNom).findFirst().orElse("#" + item);
                Label lbl = new Label(nom);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-text-fill:#3B82F6;" +
                                "-fx-background-color:#EFF6FF;" +
                                "-fx-background-radius:8;-fx-padding:4 10;");
                setGraphic(lbl); setText(null);
            }
        });

        // Niveau — affiche le TITRE
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauId"));
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item == 0) {
                    setText("—"); setGraphic(null); return;
                }
                String titre = niveaux == null ? "#" + item :
                        niveaux.stream().filter(n -> n.getId() == item)
                                .map(n -> n.getTitre() + " · " + n.getDifficulte())
                                .findFirst().orElse("#" + item);
                Label lbl = new Label(titre);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-text-fill:#7C3AED;" +
                                "-fx-background-color:#F5F3FF;" +
                                "-fx-background-radius:8;-fx-padding:4 10;");
                setGraphic(lbl); setText(null);
            }
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✎ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                styleBtn(btnEdit,
                        "-fx-background-color:#F5F3FF;-fx-text-fill:#7C3AED;",
                        "-fx-background-color:#EDE9FE;-fx-text-fill:#5B21B6;");
                styleBtn(btnDelete,
                        "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;",
                        "-fx-background-color:#FFE4E6;-fx-text-fill:#BE123C;");
                btnEdit.setOnAction(e ->
                        openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        handleDelete(getTableView().getItems().get(getIndex())));
            }
            private void styleBtn(Button btn, String normal, String hover) {
                String base = "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:8;-fx-padding:6 14;-fx-cursor:hand;";
                btn.setStyle(normal + base);
                btn.setOnMouseEntered(e -> btn.setStyle(hover + base));
                btn.setOnMouseExited(e  -> btn.setStyle(normal + base));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Alternance lignes
        tableTests.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Test item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else if (getIndex() % 2 == 0) setStyle("-fx-background-color:#FAFBFF;");
                else setStyle("-fx-background-color:white;");
            }
        });
    }

    // ── CRUD ─────────────────────────────────────────────────────────
    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            tableTests.setItems(allData);
            if (countLabel != null)
                countLabel.setText(allData.size() + " test(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableTests.setItems(allData);
            countLabel.setText(allData.size() + " test(s)");
        } else {
            ObservableList<Test> filtered = allData.stream()
                    .filter(t -> t.getTitre().toLowerCase().contains(q)
                            || t.getType().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableTests.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    @FXML private void handleAjouter() {
        selectedTest = null;
        clearForm();
        formTitle.setText("Nouveau Test");
        cacherErreur();
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void openEditForm(Test t) {
        selectedTest = t;
        fieldTitre.setText(t.getTitre());
        comboType.setValue(t.getType());
        fieldDuree.setText(String.valueOf(t.getDureeEstimee()));
        // Sélectionner la langue
        if (langues != null) {
            langues.stream().filter(l -> l.getId() == t.getLangueId())
                    .findFirst().ifPresent(comboLangue::setValue);
        }
        // Sélectionner le niveau (après que la langue est sélectionnée)
        if (niveaux != null) {
            niveaux.stream().filter(n -> n.getId() == t.getNiveauId())
                    .findFirst().ifPresent(comboNiveau::setValue);
        }
        cacherErreur();
        formTitle.setText("Modifier le Test");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML private void handleSave() {
        String erreur = validateForm();
        if (erreur != null) { afficherErreur(erreur); return; }
        cacherErreur();
        try {
            int duree    = Integer.parseInt(fieldDuree.getText().trim());
            int langueId = comboLangue.getValue().getId();
            int niveauId = comboNiveau.getValue() != null
                    ? comboNiveau.getValue().getId() : 0;

            if (selectedTest == null) {
                service.ajouter(new Test(
                        comboType.getValue(), fieldTitre.getText().trim(),
                        duree, langueId, niveauId));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Test ajouté !");
            } else {
                selectedTest.setTitre(fieldTitre.getText().trim());
                selectedTest.setType(comboType.getValue());
                selectedTest.setDureeEstimee(duree);
                selectedTest.setLangueId(langueId);
                selectedTest.setNiveauId(niveauId);
                service.modifier(selectedTest);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Test modifié !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            afficherErreur("La durée doit être un nombre entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Test t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + t.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(t.getId()); loadData(); }
                catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
                }
            }
        });
    }

    @FXML private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        clearForm();
        selectedTest = null;
    }

    // ── Validation ───────────────────────────────────────────────────
    private String validateForm() {
        if (fieldTitre.getText().isBlank())
            return "Le titre est obligatoire.";
        if (fieldTitre.getText().trim().length() < 3)
            return "Le titre doit contenir au moins 3 caractères.";
        if (comboType.getValue() == null)
            return "Le type est obligatoire.";
        if (fieldDuree.getText().isBlank())
            return "La durée est obligatoire.";
        try {
            int d = Integer.parseInt(fieldDuree.getText().trim());
            if (d <= 0)  return "La durée doit être > 0.";
            if (d > 300) return "La durée ne peut pas dépasser 300 minutes.";
        } catch (NumberFormatException e) {
            return "La durée doit être un entier (ex: 30).";
        }
        if (comboLangue.getValue() == null)
            return "Veuillez sélectionner une langue.";
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private void clearForm() {
        fieldTitre.clear();
        comboType.setValue(null);
        fieldDuree.clear();
        comboLangue.setValue(null);
        comboNiveau.setValue(null);
    }

    private void afficherErreur(String msg) {
        if (labelErreur != null) {
            labelErreur.setText("⚠ " + msg);
            labelErreur.setVisible(true);
            labelErreur.setManaged(true);
        } else showAlert(Alert.AlertType.WARNING, "Validation", msg);
    }

    private void cacherErreur() {
        if (labelErreur != null) {
            labelErreur.setVisible(false);
            labelErreur.setManaged(false);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}