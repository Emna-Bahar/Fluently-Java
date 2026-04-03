package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.services.LangueService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class LanguesEtudiantController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterPopularite;
    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyMessage;
    @FXML private VBox loadingOverlay;
    @FXML private Button clearSearchBtn;

    private final LangueService langueService = new LangueService();
    private List<Langue> allLangues;

    @FXML
    public void initialize() {
        setupFilters();
        setupSearch();
        loadLangues();
    }

    private void setupFilters() {
        filterPopularite.setItems(javafx.collections.FXCollections.observableArrayList(
                "⭐ Très populaire", "🔥 Populaire", "📈 En croissance", "🌱 Émergente", "💤 Peu demandée"
        ));
        filterPopularite.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearSearchBtn.setVisible(!newVal.isEmpty());
            applyFilters();
        });
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
    }

    private void loadLangues() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);

        new Thread(() -> {
            try {
                allLangues = langueService.recuperer();
                Platform.runLater(() -> {
                    applyFilters();
                    loadingOverlay.setVisible(false);
                    loadingOverlay.setManaged(false);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    loadingOverlay.setManaged(false);
                    showAlert("Erreur", "Impossible de charger les langues");
                });
            }
        }).start();
    }

    private void applyFilters() {
        if (allLangues == null) return;

        String searchText = searchField.getText().toLowerCase().trim();
        String selectedPopularite = filterPopularite.getValue();

        List<Langue> filtered = allLangues.stream()
                .filter(Langue::isActive)
                .filter(l -> searchText.isEmpty() || l.getNom().toLowerCase().contains(searchText))
                .filter(l -> selectedPopularite == null || selectedPopularite.equals(l.getPopularite()))
                .collect(Collectors.toList());

        displayCards(filtered);
    }

    private void displayCards(List<Langue> langues) {
        cardsContainer.getChildren().clear();

        if (langues.isEmpty()) {
            emptyMessage.setVisible(true);
            emptyMessage.setManaged(true);
            return;
        }

        emptyMessage.setVisible(false);
        emptyMessage.setManaged(false);

        for (Langue langue : langues) {
            VBox card = createLanguageCard(langue);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createLanguageCard(Langue langue) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(12);
        card.setPrefWidth(280);
        card.setPrefHeight(320);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: #F0F1F7;" +
                        "-fx-border-radius: 20;"
        );

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(108,99,255,0.25), 20, 0, 0, 8);" +
                            "-fx-cursor: hand;" +
                            "-fx-border-color: #6C63FF;" +
                            "-fx-border-radius: 20;"
            );
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);" +
                            "-fx-cursor: hand;" +
                            "-fx-border-color: #F0F1F7;" +
                            "-fx-border-radius: 20;"
            );
            card.setTranslateY(0);
        });

        // Image container
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        imageContainer.setStyle("-fx-background-color: #F8F9FD;-fx-background-radius: 20 20 0 0;");

        ImageView flagView = new ImageView();
        flagView.setFitWidth(100);
        flagView.setFitHeight(70);
        flagView.setPreserveRatio(true);

        String imagePath = langue.getDrapeau();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    flagView.setImage(new Image(file.toURI().toString()));
                }
            } catch (Exception ignored) {}
        }

        if (flagView.getImage() == null) {
            Label flagPlaceholder = new Label("🏳️");
            flagPlaceholder.setStyle("-fx-font-size: 48px;");
            imageContainer.getChildren().add(flagPlaceholder);
        } else {
            imageContainer.getChildren().add(flagView);
        }

        // Nom
        Label nameLabel = new Label(langue.getNom());
        nameLabel.setStyle("-fx-font-size: 18px;-fx-font-weight: bold;-fx-text-fill: #1A1D2E;");
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(260);

        // Description tronquée
        String desc = langue.getDescription();
        if (desc != null && desc.length() > 80) {
            desc = desc.substring(0, 77) + "...";
        }
        Label descLabel = new Label(desc != null ? desc : "");
        descLabel.setStyle("-fx-font-size: 12px;-fx-text-fill: #8A8FA8;-fx-wrap-text: true;");
        descLabel.setMaxWidth(250);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setTextAlignment(TextAlignment.CENTER);

        // Badge popularité
        Label popBadge = new Label(langue.getPopularite() != null ? langue.getPopularite() : "");
        popBadge.setStyle(
                "-fx-background-color: #EEF0FF;" +
                        "-fx-text-fill: #6C63FF;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 4 12 4 12;"
        );

        // Bouton Commencer
        Button startBtn = new Button("Commencer →");
        startBtn.setStyle(
                "-fx-background-color: #6C63FF;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-cursor: hand;"
        );
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(
                "-fx-background-color: #5849C4;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-cursor: hand;"
        ));
        startBtn.setOnMouseExited(e -> startBtn.setStyle(
                "-fx-background-color: #6C63FF;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-cursor: hand;"
        ));

        startBtn.setOnAction(e -> {
            // À implémenter : ouvrir la page d'apprentissage
            showAlert("Information", "Démarrage du cours pour " + langue.getNom());
        });

        card.getChildren().addAll(imageContainer, nameLabel, descLabel, popBadge, startBtn);
        VBox.setMargin(startBtn, new Insets(0, 0, 16, 0));
        VBox.setMargin(nameLabel, new Insets(12, 0, 0, 0));
        VBox.setMargin(descLabel, new Insets(0, 12, 0, 12));
        VBox.setMargin(popBadge, new Insets(4, 0, 8, 0));

        return card;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}