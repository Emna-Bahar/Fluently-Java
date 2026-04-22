package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.LangueService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
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
    private HomeController homeController;

    // Couleurs par popularité
    private static final String COLOR_TRES_HAUTE = "#F59E0B";
    private static final String COLOR_HAUTE = "#10B981";
    private static final String COLOR_MOYENNE = "#3B82F6";
    private static final String COLOR_FAIBLE = "#8B5CF6";
    private static final String COLOR_DEFAULT = "#6C63FF";

    @FXML
    public void initialize() {
        setupFilters();
        setupSearch();
        loadLangues();
    }

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    private void setupFilters() {
        filterPopularite.setItems(javafx.collections.FXCollections.observableArrayList(
                " Toutes", " Très haute", " Haute", " Moyenne", " Faible"
        ));
        filterPopularite.setValue(" Toutes");
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
                .filter(l -> {
                    if (selectedPopularite == null || selectedPopularite.equals(" Toutes")) return true;
                    String pop = l.getPopularite() != null ? l.getPopularite().toLowerCase() : "";
                    if (selectedPopularite.equals(" Très haute")) return pop.contains("très haute");
                    if (selectedPopularite.equals(" Haute")) return pop.contains("haute");
                    if (selectedPopularite.equals(" Moyenne")) return pop.contains("moyenne");
                    if (selectedPopularite.equals(" Faible")) return pop.contains("faible");
                    return true;
                })
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

    private String getPopulariteColor(String popularite) {
        if (popularite == null) return COLOR_DEFAULT;
        if (popularite.toLowerCase().contains("très haute")) return COLOR_TRES_HAUTE;
        if (popularite.toLowerCase().contains("haute")) return COLOR_HAUTE;
        if (popularite.toLowerCase().contains("moyenne")) return COLOR_MOYENNE;
        if (popularite.toLowerCase().contains("faible")) return COLOR_FAIBLE;
        return COLOR_DEFAULT;
    }
    // Dans LanguesEtudiantController.java
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private VBox createLanguageCard(Langue langue) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(14);
        card.setPrefWidth(300);
        card.setPrefHeight(380);

        String popColor = getPopulariteColor(langue.getPopularite());

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: rgba(108,99,255,0.1);" +
                        "-fx-border-radius: 24;"
        );

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 24;" +
                            "-fx-effect: dropshadow(gaussian, rgba(108,99,255,0.3), 20, 0, 0, 10);" +
                            "-fx-cursor: hand;" +
                            "-fx-border-color: " + popColor + ";" +
                            "-fx-border-radius: 24;" +
                            "-fx-border-width: 2;"
            );
            card.setTranslateY(-8);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 24;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);" +
                            "-fx-cursor: hand;" +
                            "-fx-border-color: rgba(108,99,255,0.1);" +
                            "-fx-border-radius: 24;"
            );
            card.setTranslateY(0);
        });

        // Bandeau coloré en haut
        HBox topBar = new HBox();
        topBar.setPrefHeight(120);
        topBar.setStyle("-fx-background-color: " + popColor + "; -fx-background-radius: 24 24 0 0;");
        topBar.setAlignment(Pos.CENTER);

        // Conteneur image drapeau
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(90, 90);
        imageContainer.setStyle("-fx-background-color: white; -fx-background-radius: 45; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);");

        ImageView flagView = new ImageView();
        flagView.setFitWidth(65);
        flagView.setFitHeight(50);
        flagView.setPreserveRatio(true);

        String imagePath = langue.getDrapeau();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                // CONVERSION DU CHEMIN RELATIF EN CHEMIN ABSOLU
                String absolutePath = imagePath;
                if (imagePath.startsWith("/uploads/")) {
                    absolutePath = "C:/xampp/htdocs/fluently/public" + imagePath;
                }
                File file = new File(absolutePath);
                if (file.exists()) {
                    flagView.setImage(new Image(file.toURI().toString()));
                } else {
                    System.out.println("Fichier drapeau introuvable: " + absolutePath);
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement drapeau: " + e.getMessage());
            }
        }

        if (flagView.getImage() == null) {
            Label flagPlaceholder = new Label("🏳️");
            flagPlaceholder.setStyle("-fx-font-size: 42px;");
            imageContainer.getChildren().add(flagPlaceholder);
        } else {
            imageContainer.getChildren().add(flagView);
        }

        topBar.getChildren().add(imageContainer);

        // Contenu
        VBox contentBox = new VBox(10);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(18, 16, 16, 16));

        // Nom
        Label nameLabel = new Label(langue.getNom());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");
        nameLabel.setMaxWidth(260);

        // Description tronquée
        String desc = langue.getDescription();
        if (desc != null && desc.length() > 80) {
            desc = desc.substring(0, 77) + "...";
        }
        Label descLabel = new Label(desc != null ? desc : "");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8A8FA8; -fx-wrap-text: true;");
        descLabel.setMaxWidth(260);
        descLabel.setAlignment(Pos.CENTER);

        // Badge popularité
        String popText = langue.getPopularite() != null ? langue.getPopularite() : "";
        Label popBadge = new Label(" " + popText);
        popBadge.setStyle(
                "-fx-background-color: " + popColor + "20;" +
                        "-fx-text-fill: " + popColor + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 6 16 6 16;"
        );

        // Bouton Commencer
        Button startBtn = new Button("Commencer →");
        startBtn.setStyle(
                "-fx-background-color: " + popColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 12 28 12 28;" +
                        "-fx-cursor: hand;"
        );
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(
                "-fx-background-color: " + popColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 12 32 12 32;" +
                        "-fx-cursor: hand;"
        ));
        startBtn.setOnMouseExited(e -> startBtn.setStyle(
                "-fx-background-color: " + popColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 12 28 12 28;" +
                        "-fx-cursor: hand;"
        ));

        startBtn.setOnAction(e -> ouvrirApprentissage(langue));

        contentBox.getChildren().addAll(nameLabel, descLabel, popBadge);
        card.getChildren().addAll(topBar, contentBox, startBtn);

        VBox.setMargin(startBtn, new Insets(0, 16, 20, 16));

        return card;
    }

    private void ouvrirApprentissage(Langue langue) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pijava_fluently/fxml/apprentissage.fxml"));
            Node apprentissageView = loader.load();

            ApprentissageController controller = loader.getController();
            controller.setLangue(langue);
            controller.setHomeController(homeController);
            controller.setCurrentUser(currentUser);

            if (homeController != null) {
                homeController.setContent(apprentissageView);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page d'apprentissage");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}