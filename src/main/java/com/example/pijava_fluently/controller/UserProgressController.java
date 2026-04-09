package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.entites.User_progress;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
import com.example.pijava_fluently.services.UserProgressService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class UserProgressController {

    @FXML private TableView<User_progress> tableProgress;
    @FXML private TableColumn<User_progress, Integer> colUserId;
    @FXML private TableColumn<User_progress, String> colLangue;
    @FXML private TableColumn<User_progress, String> colNiveauActuel;
    @FXML private TableColumn<User_progress, Integer> colDernierCours;
    @FXML private TableColumn<User_progress, String> colProgression;
    @FXML private TableColumn<User_progress, Boolean> colTestComplete;
    @FXML private TableColumn<User_progress, String> colDateActivite;
    @FXML private TableColumn<User_progress, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterLangue;
    @FXML private Button clearSearchBtn;
    @FXML private Label countLabel;

    private final UserProgressService progressService = new UserProgressService();
    private final LangueService langueService = new LangueService();
    private final NiveauService niveauService = new NiveauService();

    private ObservableList<User_progress> allData = FXCollections.observableArrayList();
    private List<Langue> allLangues;
    private List<Niveau> allNiveaux;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        loadReferences();
        setupColumns();
        setupTablePlaceholder();
        loadData();
        setupFilters();
        setupSearch();
    }

    private void loadReferences() {
        try {
            allLangues = langueService.recuperer();
            allNiveaux = niveauService.recuperer();

            filterLangue.setItems(FXCollections.observableArrayList(
                    allLangues.stream().map(Langue::getNom).collect(Collectors.toList())
            ));
            filterLangue.getItems().add(0, "Toutes");
            filterLangue.setValue("Toutes");

            filterNiveau.setItems(FXCollections.observableArrayList(
                    "A1", "A2", "B1", "B2", "C1", "C2"
            ));
            filterNiveau.getItems().add(0, "Tous");
            filterNiveau.setValue("Tous");

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les références");
        }
    }

    private void setupColumns() {
        // Utilisateur avec badge
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUserId.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label("" + item);
                    badge.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Langue
        colLangue.setCellValueFactory(cellData -> {
            int langueId = cellData.getValue().getLangueId();
            String nom = allLangues.stream()
                    .filter(l -> l.getId() == langueId)
                    .map(Langue::getNom)
                    .findFirst()
                    .orElse("Inconnue");
            return new javafx.beans.property.SimpleStringProperty(nom);
        });
        colLangue.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");
                }
            }
        });

        // Niveau actuel avec badge coloré
        colNiveauActuel.setCellValueFactory(cellData -> {
            int niveauId = cellData.getValue().getNiveauActuelId();
            String nom = allNiveaux.stream()
                    .filter(n -> n.getId() == niveauId)
                    .map(Niveau::getDifficulte)
                    .findFirst()
                    .orElse("Non défini");
            return new javafx.beans.property.SimpleStringProperty(nom);
        });
        colNiveauActuel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null || item.equals("Non défini")) {
                    setText(item);
                    setGraphic(null);
                } else {
                    String color = switch (item) {
                        case "A1" -> "#6C63FF";
                        case "A2" -> "#8B5CF6";
                        case "B1" -> "#F59E0B";
                        case "B2" -> "#EF4444";
                        case "C1" -> "#EC4899";
                        case "C2" -> "#14B8A6";
                        default -> "#8A8FA8";
                    };
                    Label badge = new Label(item);
                    badge.setStyle("-fx-background-color:" + color + "20;-fx-text-fill:" + color + ";-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Dernier cours
        colDernierCours.setCellValueFactory(new PropertyValueFactory<>("dernierNumeroCours"));
        colDernierCours.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Label badge = new Label("Cours N°" + item);
                    badge.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#166534;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Progression avec barre
        colProgression.setCellValueFactory(cellData -> {
            int dernierCours = cellData.getValue().getDernierNumeroCours();
            return new javafx.beans.property.SimpleStringProperty(dernierCours + "/3");
        });
        colProgression.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label label = new Label();
            private final HBox container = new HBox(8);
            {
                progressBar.setPrefWidth(100);
                progressBar.setPrefHeight(6);
                label.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(progressBar, label);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String[] parts = item.split("/");
                    int current = Integer.parseInt(parts[0]);
                    int total = Integer.parseInt(parts[1]);
                    double progress = (double) current / total;
                    progressBar.setProgress(progress);
                    label.setText(item);
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // Test niveau
        colTestComplete.setCellValueFactory(new PropertyValueFactory<>("testNiveauComplete"));
        colTestComplete.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item ? "✅ Complété" : "⏳ En attente");
                    badge.setStyle(item
                            ? "-fx-background-color:#ECFDF5;-fx-text-fill:#059669;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;"
                            : "-fx-background-color:#FEF3C7;-fx-text-fill:#D97706;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Date activité
        colDateActivite.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateDerniereActivite() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateDerniereActivite().format(dateFormatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
        colDateActivite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
                }
            }
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = new Button("👁 Voir ");
            private final HBox box = new HBox(btnDetails);
            {
                box.setAlignment(Pos.CENTER);
                btnDetails.setStyle("-fx-background-color:#EEF0FF;-fx-text-fill:#6C63FF;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 12 6 12;-fx-cursor:hand;");
                btnDetails.setOnMouseEntered(e -> btnDetails.setStyle("-fx-background-color:#DDDDFF;-fx-text-fill:#5B52CC;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 12 6 12;-fx-cursor:hand;"));
                btnDetails.setOnMouseExited(e -> btnDetails.setStyle("-fx-background-color:#EEF0FF;-fx-text-fill:#6C63FF;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 12 6 12;-fx-cursor:hand;"));
                btnDetails.setOnAction(e -> showDetails(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupTablePlaceholder() {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(60));

        Label iconLabel = new Label("📭");
        iconLabel.setStyle("-fx-font-size: 56px; -fx-opacity: 0.5;");

        Label titleLabel = new Label("Aucune progression trouvée");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #8A8FA8;");

        Label subLabel = new Label("Les étudiants n'ont pas encore commencé de cours");
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #C0C4D8;");

        placeholder.getChildren().addAll(iconLabel, titleLabel, subLabel);
        tableProgress.setPlaceholder(placeholder);
    }

    private void loadData() {
        try {
            List<User_progress> list = progressService.recuperer();

            if (list == null || list.isEmpty()) {
                allData = FXCollections.observableArrayList();
                tableProgress.setItems(allData);
                updateCountLabel(0);
                return;
            }

            allData = FXCollections.observableArrayList(list);
            tableProgress.setItems(allData);
            updateCountLabel(allData.size());
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les progressions: " + e.getMessage());
            allData = FXCollections.observableArrayList();
            tableProgress.setItems(allData);
            updateCountLabel(0);
        }
    }

    private void setupFilters() {
        filterLangue.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> applyFilters());
        filterNiveau.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, val) -> {
            clearSearchBtn.setVisible(!val.isEmpty());
            applyFilters();
        });
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase().trim();
        String selectedLangue = filterLangue.getValue();
        String selectedNiveau = filterNiveau.getValue();

        ObservableList<User_progress> filtered = allData.stream()
                .filter(p -> {
                    if (search.isEmpty()) return true;
                    String userId = String.valueOf(p.getUserId());
                    String langueNom = allLangues.stream()
                            .filter(l -> l.getId() == p.getLangueId())
                            .map(Langue::getNom)
                            .findFirst().orElse("");
                    return userId.contains(search) || langueNom.toLowerCase().contains(search);
                })
                .filter(p -> {
                    if (selectedLangue == null || selectedLangue.equals("Toutes")) return true;
                    String langueNom = allLangues.stream()
                            .filter(l -> l.getId() == p.getLangueId())
                            .map(Langue::getNom)
                            .findFirst().orElse("");
                    return langueNom.equals(selectedLangue);
                })
                .filter(p -> {
                    if (selectedNiveau == null || selectedNiveau.equals("Tous")) return true;
                    String niveauNom = allNiveaux.stream()
                            .filter(n -> n.getId() == p.getNiveauActuelId())
                            .map(Niveau::getDifficulte)
                            .findFirst().orElse("");
                    return niveauNom != null && niveauNom.contains(selectedNiveau);
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        tableProgress.setItems(filtered);
        updateCountLabel(filtered.size());
    }

    private void updateCountLabel(int count) {
        countLabel.setText(count + " progression(s)");
    }

    private void showDetails(User_progress progress) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("📊 Détails de la progression");
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setPrefWidth(420);
        content.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16;");

        String langueNom = allLangues.stream()
                .filter(l -> l.getId() == progress.getLangueId())
                .map(Langue::getNom)
                .findFirst().orElse("Inconnue");

        String niveauNom = allNiveaux.stream()
                .filter(n -> n.getId() == progress.getNiveauActuelId())
                .map(Niveau::getDifficulte)
                .findFirst().orElse("Non défini");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16;");

        String labelStyle = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #8A8FA8;";
        String valueStyle = "-fx-font-size: 14px; -fx-text-fill: #1A1D2E;";

        addGridRow(grid, 0, "👤 Utilisateur:", "#" + progress.getUserId(), labelStyle, valueStyle);
        addGridRow(grid, 1, "🌍 Langue:", langueNom, labelStyle, valueStyle);
        addGridRow(grid, 2, "🎯 Niveau actuel:", niveauNom, labelStyle, valueStyle);
        addGridRow(grid, 3, "📖 Dernier cours:", "Cours N°" + progress.getDernierNumeroCours(), labelStyle, valueStyle);
        addGridRow(grid, 4, "📊 Progression:", progress.getDernierNumeroCours() + "/3 cours", labelStyle, valueStyle);
        addGridRow(grid, 5, "✅ Test niveau:", progress.isTestNiveauComplete() ? "✅ Complété" : "⏳ En attente", labelStyle, valueStyle);
        addGridRow(grid, 6, "🕐 Dernière activité:", progress.getDateDerniereActivite() != null
                ? progress.getDateDerniereActivite().format(dateFormatter) : "—", labelStyle, valueStyle);

        content.getChildren().add(grid);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:#6C63FF;-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 20 8 20;-fx-cursor:hand;");

        dialog.showAndWait();
    }

    private void addGridRow(GridPane grid, int row, String label, String value, String labelStyle, String valueStyle) {
        Label l = new Label(label);
        l.setStyle(labelStyle);
        Label v = new Label(value);
        v.setStyle(valueStyle);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}