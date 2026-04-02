package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
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

public class NiveauController {

    // ── Formulaire ─────────────────────────────────────────────────
    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private TextField fieldTitre;
    @FXML private TextField fieldDescription;
    @FXML private TextField fieldDifficulte;
    @FXML private TextField fieldOrdre;
    @FXML private TextField fieldScoreMin;
    @FXML private TextField fieldScoreMax;
    @FXML private TextField fieldImageCouverture;
    @FXML private ComboBox<Langue> comboLangue;

    // ── Table ──────────────────────────────────────────────────────
    @FXML private TableView<Niveau>           tableNiveaux;
    @FXML private TableColumn<Niveau, Integer> colId;
    @FXML private TableColumn<Niveau, String>  colTitre;
    @FXML private TableColumn<Niveau, String>  colDifficulte;
    @FXML private TableColumn<Niveau, Integer> colOrdre;
    @FXML private TableColumn<Niveau, Double>  colScoreMin;
    @FXML private TableColumn<Niveau, Double>  colScoreMax;
    @FXML private TableColumn<Niveau, String>  colLangue;
    @FXML private TableColumn<Niveau, Void>    colActions;

    // ── Recherche ──────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── State ──────────────────────────────────────────────────────
    private final NiveauService  niveauService  = new NiveauService();
    private final LangueService  langueService  = new LangueService();
    private ObservableList<Niveau> allData      = FXCollections.observableArrayList();
    private List<Langue>           allLangues;
    private Niveau selectedNiveau = null;

    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        loadLangues();
        setupColumns();
        loadData();
    }

    // ── Charger les langues pour le ComboBox ───────────────────────
    private void loadLangues() {
        try {
            allLangues = langueService.recuperer();
            comboLangue.setItems(FXCollections.observableArrayList(allLangues));
            // Afficher le nom dans le ComboBox
            comboLangue.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Langue l, boolean empty) {
                    super.updateItem(l, empty);
                    setText(empty || l == null ? null : l.getNom());
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

    // ── Configuration colonnes ─────────────────────────────────────
    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDifficulte.setCellValueFactory(new PropertyValueFactory<>("difficulte"));
        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        colScoreMin.setCellValueFactory(new PropertyValueFactory<>("seuilScoreMin"));
        colScoreMax.setCellValueFactory(new PropertyValueFactory<>("seuilScoreMax"));

        // Colonne Langue — afficher le nom au lieu de l'id
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

        // Colonne Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color:#6c63ff;-fx-text-fill:white;-fx-font-size:11px;-fx-cursor:hand;");
                btnDelete.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:11px;-fx-cursor:hand;");

                btnEdit.setOnAction(e -> openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
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
            allData = FXCollections.observableArrayList(niveauService.recuperer());
            tableNiveaux.setItems(allData);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les niveaux : " + e.getMessage());
        }
    }

    // ── Recherche ──────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableNiveaux.setItems(allData);
        } else {
            ObservableList<Niveau> filtered = allData.stream()
                    .filter(n -> n.getTitre().toLowerCase().contains(q)
                            || n.getDifficulte().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableNiveaux.setItems(filtered);
        }
    }

    // ── Ouvrir formulaire AJOUT ────────────────────────────────────
    @FXML
    private void handleAjouter() {
        selectedNiveau = null;
        clearForm();
        formTitle.setText("Nouveau Niveau");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    // ── Ouvrir formulaire MODIFICATION ────────────────────────────
    private void openEditForm(Niveau n) {
        selectedNiveau = n;
        fieldTitre.setText(n.getTitre());
        fieldDescription.setText(n.getDescription());
        fieldDifficulte.setText(n.getDifficulte());
        fieldOrdre.setText(String.valueOf(n.getOrdre()));
        fieldScoreMin.setText(String.valueOf(n.getSeuilScoreMin()));
        fieldScoreMax.setText(String.valueOf(n.getSeuilScoreMax()));
        fieldImageCouverture.setText(n.getImageCouverture());
        // Sélectionner la langue correspondante
        allLangues.stream()
                .filter(l -> l.getId() == n.getIdLangueId())
                .findFirst()
                .ifPresent(comboLangue::setValue);
        formTitle.setText("Modifier le Niveau");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    // ── Enregistrer ────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        try {
            int    ordre    = Integer.parseInt(fieldOrdre.getText().trim());
            double scoreMin = Double.parseDouble(fieldScoreMin.getText().trim());
            double scoreMax = Double.parseDouble(fieldScoreMax.getText().trim());
            int    idLangue = comboLangue.getValue().getId();

            if (selectedNiveau == null) {
                Niveau newN = new Niveau(
                        fieldTitre.getText().trim(),
                        fieldDescription.getText().trim(),
                        fieldImageCouverture.getText().trim(),
                        fieldDifficulte.getText().trim(),
                        ordre, scoreMax, scoreMin, idLangue
                );
                niveauService.ajouter(newN);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Niveau ajouté !");
            } else {
                selectedNiveau.setTitre(fieldTitre.getText().trim());
                selectedNiveau.setDescription(fieldDescription.getText().trim());
                selectedNiveau.setDifficulte(fieldDifficulte.getText().trim());
                selectedNiveau.setOrdre(ordre);
                selectedNiveau.setSeuilScoreMin(scoreMin);
                selectedNiveau.setSeuilScoreMax(scoreMax);
                selectedNiveau.setImageCouverture(fieldImageCouverture.getText().trim());
                selectedNiveau.setIdLangueId(idLangue);
                niveauService.modifier(selectedNiveau);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Niveau modifié !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Ordre et scores doivent être des nombres.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Supprimer ──────────────────────────────────────────────────
    private void handleDelete(Niveau n) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le niveau \"" + n.getTitre() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    niveauService.supprimer(n.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Niveau supprimé !");
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
    }

    private boolean validateForm() {
        if (fieldTitre.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le titre est obligatoire.");
            return false;
        }
        if (comboLangue.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez sélectionner une langue.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        fieldTitre.clear(); fieldDescription.clear(); fieldDifficulte.clear();
        fieldOrdre.clear(); fieldScoreMin.clear(); fieldScoreMax.clear();
        fieldImageCouverture.clear(); comboLangue.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}