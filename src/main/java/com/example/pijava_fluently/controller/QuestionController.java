package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.QuestionService;
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

public class QuestionController {

    @FXML private VBox formCard;
    @FXML private Label formTitle;
    @FXML private TextArea fieldEnonce;   // ← TextArea, pas TextField
    @FXML private ComboBox<String> comboType;
    @FXML private TextField fieldScoreMax;
    @FXML private ComboBox<Test> comboTest;
    @FXML private Label countLabel;
    @FXML private TextField searchField;

    @FXML private TableView<Question> tableQuestions;
    @FXML private TableColumn<Question, Integer> colId;
    @FXML private TableColumn<Question, String>  colEnonce;
    @FXML private TableColumn<Question, String>  colType;
    @FXML private TableColumn<Question, Integer> colScore;
    @FXML private TableColumn<Question, Integer> colTest;
    @FXML private TableColumn<Question, Void>    colActions;

    private final QuestionService service      = new QuestionService();
    private final TestService     testService  = new TestService();
    private ObservableList<Question> allData   = FXCollections.observableArrayList();
    private Question selectedQuestion          = null;

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("qcm", "oral", "texte_libre"));
        try {
            List<Test> tests = testService.recuperer();
            comboTest.setItems(FXCollections.observableArrayList(tests));
        } catch (SQLException e) { e.printStackTrace(); }
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colEnonce.setCellValueFactory(new PropertyValueFactory<>("enonce"));
        colEnonce.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                String txt = item.length() > 60 ? item.substring(0, 57) + "…" : item;
                Label lbl = new Label(txt);
                lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#1A1D2E;");
                lbl.setTooltip(new Tooltip(item));
                setGraphic(lbl); setText(null);
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item.toUpperCase());
                String color = switch (item) {
                    case "qcm"         -> "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;";
                    case "oral"        -> "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;";
                    case "texte_libre" -> "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;";
                    default            -> "-fx-background-color:#F4F5FA;-fx-text-fill:#6B7280;";
                };
                badge.setStyle(color +
                        "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:3 8;");
                setGraphic(badge); setText(null);
            }
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("scoreMax"));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label(item + " pts");
                lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#6C63FF;-fx-font-size:12px;");
                setGraphic(lbl); setText(null);
            }
        });

        colTest.setCellValueFactory(new PropertyValueFactory<>("testId"));
        colTest.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText("—"); return; }
                Label lbl = new Label("Test #" + item);
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;" +
                        "-fx-background-color:#F4F5FA;-fx-background-radius:8;-fx-padding:3 8;");
                setGraphic(lbl); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✎ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnEdit.setStyle("-fx-background-color:#F5F3FF;-fx-text-fill:#7C3AED;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:7;-fx-padding:5 12;-fx-cursor:hand;");
                btnDelete.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:7;-fx-padding:5 12;-fx-cursor:hand;");
                btnEdit.setOnAction(e ->
                        openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            tableQuestions.setItems(allData);
            if (countLabel != null) countLabel.setText(allData.size() + " question(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableQuestions.setItems(allData);
            countLabel.setText(allData.size() + " question(s)");
        } else {
            ObservableList<Question> filtered = allData.stream()
                    .filter(qu -> qu.getEnonce().toLowerCase().contains(q)
                            || qu.getType().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableQuestions.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    @FXML private void handleAjouter() {
        selectedQuestion = null;
        clearForm();
        formTitle.setText("Nouvelle Question");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void openEditForm(Question q) {
        selectedQuestion = q;
        fieldEnonce.setText(q.getEnonce());
        comboType.setValue(q.getType());
        fieldScoreMax.setText(String.valueOf(q.getScoreMax()));
        comboTest.getItems().stream()
                .filter(t -> t.getId() == q.getTestId())
                .findFirst()
                .ifPresent(comboTest::setValue);
        formTitle.setText("Modifier la Question");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML private void handleSave() {
        if (!validateForm()) return;
        try {
            int score  = fieldScoreMax.getText().isBlank() ? 2
                    : Integer.parseInt(fieldScoreMax.getText().trim());
            int testId = comboTest.getValue().getId();

            if (selectedQuestion == null) {
                service.ajouter(new Question(
                        fieldEnonce.getText().trim(),
                        comboType.getValue(), score, testId));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Question ajoutée !");
            } else {
                selectedQuestion.setEnonce(fieldEnonce.getText().trim());
                selectedQuestion.setType(comboType.getValue());
                selectedQuestion.setScoreMax(score);
                selectedQuestion.setTestId(testId);
                service.modifier(selectedQuestion);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Question modifiée !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Format invalide",
                    "Le score doit être un nombre entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Question q) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette question ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(q.getId()); loadData(); }
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
        selectedQuestion = null;
    }

    private boolean validateForm() {
        if (fieldEnonce.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ L'énoncé est obligatoire.");
            return false;
        }
        if (comboType.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "⚠ Le type est obligatoire.");
            return false;
        }
        if (comboTest.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "⚠ Sélectionnez un test.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        fieldEnonce.clear();
        comboType.setValue(null);
        fieldScoreMax.clear();
        comboTest.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}