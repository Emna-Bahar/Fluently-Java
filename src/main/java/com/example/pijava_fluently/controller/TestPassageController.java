package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.services.TestPassageService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TestPassageController {

    @FXML private Label countLabel;
    @FXML private TextField searchField;

    @FXML private TableView<TestPassage>                  tablePassages;
    @FXML private TableColumn<TestPassage, Integer>       colId;
    @FXML private TableColumn<TestPassage, Integer>       colTest;
    @FXML private TableColumn<TestPassage, Integer>       colUser;
    @FXML private TableColumn<TestPassage, Integer>       colScore;
    @FXML private TableColumn<TestPassage, Integer>       colScoreMax;
    @FXML private TableColumn<TestPassage, String>        colStatut;
    @FXML private TableColumn<TestPassage, Integer>       colTemps;
    @FXML private TableColumn<TestPassage, LocalDateTime> colDateDebut;

    private final TestPassageService service = new TestPassageService();
    private ObservableList<TestPassage> allData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label("#" + item);
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;");
                setGraphic(lbl); setText(null);
            }
        });

        colTest.setCellValueFactory(new PropertyValueFactory<>("testId"));
        colTest.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                Label lbl = new Label("📝 Test #" + item);
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#3B82F6;" +
                        "-fx-background-color:#EFF6FF;-fx-background-radius:8;-fx-padding:3 8;");
                setGraphic(lbl); setText(null);
            }
        });

        colUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                Label lbl = new Label("👤 #" + item);
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
                setGraphic(lbl); setText(null);
            }
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label(String.valueOf(item));
                lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#6C63FF;-fx-font-size:13px;");
                setGraphic(lbl); setText(null);
            }
        });

        colScoreMax.setCellValueFactory(new PropertyValueFactory<>("scoreMax"));
        colScoreMax.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText("—"); return; }
                setText("/ " + item);
                setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:12px;");
            }
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String icon  = switch (item) {
                    case "termine"   -> "✅ ";
                    case "en_cours"  -> "🔄 ";
                    case "abandonne" -> "❌ ";
                    default          -> "";
                };
                String color = switch (item) {
                    case "termine"   ->
                            "-fx-background-color:#ECFDF5;-fx-text-fill:#059669;-fx-border-color:#A7F3D0;";
                    case "en_cours"  ->
                            "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;-fx-border-color:#FDE68A;";
                    case "abandonne" ->
                            "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-border-color:#FECDD3;";
                    default          ->
                            "-fx-background-color:#F4F5FA;-fx-text-fill:#6B7280;-fx-border-color:#E0E3F0;";
                };
                Label badge = new Label(icon + item);
                badge.setStyle(color +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-border-radius:20;" +
                        "-fx-border-width:1;-fx-padding:4 10;");
                setGraphic(badge); setText(null);
            }
        });

        colTemps.setCellValueFactory(new PropertyValueFactory<>("tempsPasse"));
        colTemps.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText("—"); return; }
                // Convertir secondes en format lisible
                int min = item / 60;
                int sec = item % 60;
                String txt = min > 0 ? min + "m " + sec + "s" : sec + "s";
                Label lbl = new Label("⏱ " + txt);
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
                setGraphic(lbl); setText(null);
            }
        });

        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateDebut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                Label lbl = new Label(item.toLocalDate().toString());
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
                setGraphic(lbl); setText(null);
            }
        });

        // Alternance couleurs lignes
        tablePassages.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(TestPassage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color:#FAFBFF;");
                } else {
                    setStyle("-fx-background-color:#FFFFFF;");
                }
            }
        });
    }

    private void loadData() {
        try {
            List<TestPassage> list = service.recuperer();
            allData = FXCollections.observableArrayList(list);
            tablePassages.setItems(allData);
            if (countLabel != null) countLabel.setText(allData.size() + " passage(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tablePassages.setItems(allData);
            countLabel.setText(allData.size() + " passage(s)");
        } else {
            ObservableList<TestPassage> filtered = allData.stream()
                    .filter(p -> (p.getStatut() != null &&
                            p.getStatut().toLowerCase().contains(q))
                            || String.valueOf(p.getUserId()).contains(q)
                            || String.valueOf(p.getTestId()).contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tablePassages.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}