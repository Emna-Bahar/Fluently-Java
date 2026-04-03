package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.TestService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class TestController {

    @FXML private VBox formCard;
    @FXML private Label formTitle;
    @FXML private TextField fieldTitre;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField fieldDuree;
    @FXML private TextField fieldLangueId;
    @FXML private TextField fieldNiveauId;
    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Test> tableTests;
    @FXML private TableColumn<Test, Integer> colId;
    @FXML private TableColumn<Test, String> colTitre;
    @FXML private TableColumn<Test, String> colType;
    @FXML private TableColumn<Test, Integer> colDuree;
    @FXML private TableColumn<Test, Integer> colLangue;
    @FXML private TableColumn<Test, Integer> colNiveau;
    @FXML private TableColumn<Test, Void> colActions;

    private final TestService service = new TestService();
    private ObservableList<Test> allData = FXCollections.observableArrayList();
    private Test selectedTest = null;

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("Niveau", "Fin de niveau", "Placement", "Examen"));
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeEstimee"));
        colLangue.setCellValueFactory(new PropertyValueFactory<>("langueId"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauId"));

        // Colonne Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✎ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnEdit.setStyle("-fx-background-color:#F5F3FF;-fx-text-fill:#7C3AED;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12;-fx-cursor:hand;");
                btnDelete.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12;-fx-cursor:hand;");

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

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            tableTests.setItems(allData);
            countLabel.setText(allData.size() + " test(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableTests.setItems(allData);
            countLabel.setText(allData.size() + " test(s)");
        } else {
            ObservableList<Test> filtered = allData.stream()
                    .filter(t -> t.getTitre().toLowerCase().contains(q) ||
                            t.getType().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableTests.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    @FXML
    private void handleAjouter() {
        selectedTest = null;
        clearForm();
        formTitle.setText("Nouveau Test");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void openEditForm(Test t) {
        selectedTest = t;
        fieldTitre.setText(t.getTitre());
        comboType.setValue(t.getType());
        fieldDuree.setText(String.valueOf(t.getDureeEstimee()));
        fieldLangueId.setText(String.valueOf(t.getLangueId()));
        fieldNiveauId.setText(String.valueOf(t.getNiveauId()));
        formTitle.setText("Modifier le Test");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        try {
            String type = comboType.getValue();
            String titre = fieldTitre.getText().trim();
            int duree = Integer.parseInt(fieldDuree.getText().trim());
            int langueId = Integer.parseInt(fieldLangueId.getText().trim());
            int niveauId = Integer.parseInt(fieldNiveauId.getText().trim());

            if (selectedTest == null) {
                service.ajouter(new Test(type, titre, duree, langueId, niveauId));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Test ajouté !");
            } else {
                selectedTest.setType(type);
                selectedTest.setTitre(titre);
                selectedTest.setDureeEstimee(duree);
                selectedTest.setLangueId(langueId);
                selectedTest.setNiveauId(niveauId);
                service.modifier(selectedTest);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Test modifié !");
            }

            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Format invalide", "Durée et IDs doivent être des nombres.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Test t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce test ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(t.getId());
                    loadData();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        clearForm();
        selectedTest = null;
    }

    private boolean validateForm() {
        if (fieldTitre.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Le titre est obligatoire.");
            return false;
        }
        if (comboType.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Le type est obligatoire.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        fieldTitre.clear();
        fieldDuree.clear();
        fieldLangueId.clear();
        fieldNiveauId.clear();
        comboType.setValue("Niveau");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}