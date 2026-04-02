package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Cours;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.services.CoursService;
import com.example.pijava_fluently.services.NiveauService;
import javafx.beans.property.SimpleStringProperty;
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

public class CoursController {

    // ── Formulaire ─────────────────────────────────────────────────
    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private TextField fieldNumero;
    @FXML private TextField fieldRessource;
    @FXML private DatePicker fieldDate;
    @FXML private ComboBox<Niveau> comboNiveau;
    @FXML private ComboBox<Cours>  comboPrecedent;

    // ── Table ──────────────────────────────────────────────────────
    @FXML private TableView<Cours>           tableCours;
    @FXML private TableColumn<Cours, Integer> colId;
    @FXML private TableColumn<Cours, Integer> colNumero;
    @FXML private TableColumn<Cours, String>  colRessource;
    @FXML private TableColumn<Cours, String>  colDate;
    @FXML private TableColumn<Cours, String>  colNiveau;
    @FXML private TableColumn<Cours, String>  colPrecedent;
    @FXML private TableColumn<Cours, Void>    colActions;

    // ── Recherche ──────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── State ──────────────────────────────────────────────────────
    private final CoursService  coursService  = new CoursService();
    private final NiveauService niveauService = new NiveauService();
    private ObservableList<Cours>  allData    = FXCollections.observableArrayList();
    private List<Niveau>           allNiveaux;
    private Cours selectedCours = null;

    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        loadNiveaux();
        setupColumns();
        loadData();
    }

    // ── Charger les niveaux pour les ComboBox ──────────────────────
    private void loadNiveaux() {
        try {
            allNiveaux = niveauService.recuperer();
            comboNiveau.setItems(FXCollections.observableArrayList(allNiveaux));

            // Afficher titre dans ComboBox Niveau
            comboNiveau.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Niveau n, boolean empty) {
                    super.updateItem(n, empty);
                    setText(empty || n == null ? null : n.getTitre());
                }
            });
            comboNiveau.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Niveau n, boolean empty) {
                    super.updateItem(n, empty);
                    setText(empty || n == null ? null : n.getTitre());
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les niveaux.");
        }
    }

    // ── Remplir comboPrecedent avec les cours du niveau sélectionné ─
    private void refreshComboPrecedent(int idNiveau) {
        ObservableList<Cours> coursDuNiveau = allData.stream()
                .filter(c -> c.getIdNiveauId() == idNiveau)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        comboPrecedent.setItems(coursDuNiveau);

        comboPrecedent.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Cours c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : "Cours N°" + c.getNumero());
            }
        });
        comboPrecedent.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Cours c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : "Cours N°" + c.getNumero());
            }
        });
    }

    // ── Configuration colonnes ─────────────────────────────────────
    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colRessource.setCellValueFactory(new PropertyValueFactory<>("ressource"));

        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDateCreation() != null
                                ? cellData.getValue().getDateCreation().toString() : ""
                )
        );

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

        colPrecedent.setCellValueFactory(cellData -> {
            int idPrec = cellData.getValue().getCoursPrecedentIdId();
            if (idPrec == 0) return new SimpleStringProperty("—");
            String label = allData.stream()
                    .filter(c -> c.getId() == idPrec)
                    .map(c -> "N°" + c.getNumero())
                    .findFirst().orElse(String.valueOf(idPrec));
            return new SimpleStringProperty(label);
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
            allData = FXCollections.observableArrayList(coursService.recuperer());
            tableCours.setItems(allData);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les cours : " + e.getMessage());
        }
    }

    // ── Recherche ──────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableCours.setItems(allData);
        } else {
            ObservableList<Cours> filtered = allData.stream()
                    .filter(c -> c.getRessource().toLowerCase().contains(q)
                            || String.valueOf(c.getNumero()).contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableCours.setItems(filtered);
        }
    }

    // ── Ouvrir formulaire AJOUT ────────────────────────────────────
    @FXML
    private void handleAjouter() {
        selectedCours = null;
        clearForm();
        formTitle.setText("Nouveau Cours");
        formCard.setVisible(true);
        formCard.setManaged(true);
        // Quand on change de niveau → recharger comboPrecedent
        comboNiveau.setOnAction(e -> {
            if (comboNiveau.getValue() != null)
                refreshComboPrecedent(comboNiveau.getValue().getId());
        });
    }

    // ── Ouvrir formulaire MODIFICATION ────────────────────────────
    private void openEditForm(Cours c) {
        selectedCours = c;
        fieldNumero.setText(String.valueOf(c.getNumero()));
        fieldRessource.setText(c.getRessource());
        fieldDate.setValue(c.getDateCreation());
        // Sélectionner niveau
        allNiveaux.stream()
                .filter(n -> n.getId() == c.getIdNiveauId())
                .findFirst().ifPresent(n -> {
                    comboNiveau.setValue(n);
                    refreshComboPrecedent(n.getId());
                });
        // Sélectionner cours précédent
        if (c.getCoursPrecedentIdId() != 0) {
            allData.stream()
                    .filter(cc -> cc.getId() == c.getCoursPrecedentIdId())
                    .findFirst().ifPresent(comboPrecedent::setValue);
        }
        formTitle.setText("Modifier le Cours");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    // ── Enregistrer ────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        try {
            int numero  = Integer.parseInt(fieldNumero.getText().trim());
            int idNiv   = comboNiveau.getValue().getId();
            int idPrec  = comboPrecedent.getValue() != null ? comboPrecedent.getValue().getId() : 0;

            if (selectedCours == null) {
                Cours newC = new Cours(
                        numero,
                        fieldRessource.getText().trim(),
                        fieldDate.getValue(),
                        idPrec,
                        idNiv
                );
                coursService.ajouter(newC);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Cours ajouté !");
            } else {
                selectedCours.setNumero(numero);
                selectedCours.setRessource(fieldRessource.getText().trim());
                selectedCours.setDateCreation(fieldDate.getValue());
                selectedCours.setIdNiveauId(idNiv);
                selectedCours.setCoursPrecedentIdId(idPrec);
                coursService.modifier(selectedCours);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Cours modifié !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le numéro doit être un entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Supprimer ──────────────────────────────────────────────────
    private void handleDelete(Cours c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le cours N°" + c.getNumero() + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    coursService.supprimer(c.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Cours supprimé !");
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

    private boolean validateForm() {
        if (fieldNumero.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le numéro est obligatoire.");
            return false;
        }
        if (comboNiveau.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez sélectionner un niveau.");
            return false;
        }
        if (fieldDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez sélectionner une date.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        fieldNumero.clear(); fieldRessource.clear();
        fieldDate.setValue(null); comboNiveau.setValue(null);
        comboPrecedent.setValue(null); comboPrecedent.setItems(FXCollections.emptyObservableList());
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
