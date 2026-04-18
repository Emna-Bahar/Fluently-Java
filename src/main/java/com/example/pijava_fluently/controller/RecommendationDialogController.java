package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.GroqRecommendationService;
import com.example.pijava_fluently.services.ObjectifService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecommendationDialogController {

    @FXML private TextField fieldInterests;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private ComboBox<String> comboLangue;
    @FXML private Button btnGenerer;
    @FXML private VBox recommendationsContainer;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private Button btnFermer;
    @FXML private Button btnAjouterSelection;

    private GroqRecommendationService recommendationService;
    private ObjectifService objectifService;
    private User currentUser;
    private ObjectifController objectifController;

    private List<GroqRecommendationService.ObjectifRecommendation> currentRecommendations = new ArrayList<>();
    private List<CheckBox> checkBoxes = new ArrayList<>();

    @FXML
    public void initialize() {
        recommendationService = new GroqRecommendationService();
        objectifService = new ObjectifService();

        // Initialiser les ComboBox
        comboNiveau.setItems(javafx.collections.FXCollections.observableArrayList(
                "Débutant (A1)", "Élémentaire (A2)", "Intermédiaire (B1)",
                "Intermédiaire avancé (B2)", "Avancé (C1)", "Expert (C2)"
        ));
        comboNiveau.setValue("Intermédiaire (B1)");

        comboLangue.setItems(javafx.collections.FXCollections.observableArrayList(
                "Anglais", "Français", "Espagnol", "Allemand", "Italien",
                "Portugais", "Japonais", "Chinois", "Coréen", "Arabe"
        ));
        comboLangue.setValue("Français");

        // Désactiver le bouton d'ajout au départ
        btnAjouterSelection.setDisable(true);

        showPlaceholder();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && fieldInterests != null) {
            fieldInterests.setPromptText("Ex: voyages, cuisine, technologie, littérature...");
        }
    }

    public void setObjectifController(ObjectifController controller) {
        this.objectifController = controller;
    }

    @FXML
    private void genererRecommendations() {
        String interests = fieldInterests.getText().trim();
        if (interests.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Veuillez saisir vos centres d'intérêt");
            return;
        }

        // Afficher le chargement
        loadingIndicator.setVisible(true);
        btnGenerer.setDisable(true);
        btnAjouterSelection.setDisable(true);
        statusLabel.setText("🤖 Génération des recommandations personnalisées...");
        recommendationsContainer.getChildren().clear();
        checkBoxes.clear();

        // Lancer la requête dans un thread séparé
        new Thread(() -> {
            try {
                String niveau = comboNiveau.getValue();
                String langue = comboLangue.getValue();

                List<GroqRecommendationService.ObjectifRecommendation> recos =
                        recommendationService.getRecommendations(interests, niveau, langue);

                Platform.runLater(() -> {
                    if (recos != null && !recos.isEmpty()) {
                        currentRecommendations = recos;
                        displayRecommendations(recos);
                        statusLabel.setText("✨ " + recos.size() + " objectifs recommandés");
                    } else {
                        statusLabel.setText("⚠️ Aucune recommandation générée");
                        showPlaceholder();
                    }
                    loadingIndicator.setVisible(false);
                    btnGenerer.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    btnGenerer.setDisable(false);
                    statusLabel.setText("❌ Erreur");
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                    showPlaceholder();
                });
            }
        }).start();
    }

    private void displayRecommendations(List<GroqRecommendationService.ObjectifRecommendation> recommendations) {
        recommendationsContainer.getChildren().clear();
        checkBoxes.clear();

        if (recommendations == null || recommendations.isEmpty()) {
            showPlaceholder();
            return;
        }

        for (int i = 0; i < recommendations.size(); i++) {
            GroqRecommendationService.ObjectifRecommendation rec = recommendations.get(i);
            VBox card = createRecommendationCard(rec, i);
            recommendationsContainer.getChildren().add(card);
        }

        // Activer le bouton d'ajout
        btnAjouterSelection.setDisable(false);
    }

    private VBox createRecommendationCard(GroqRecommendationService.ObjectifRecommendation rec, int index) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-border-color: #E5E7EB; -fx-border-radius: 20; -fx-border-width: 1.5; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);");
        card.setPadding(new Insets(18));
        card.setMaxWidth(Double.MAX_VALUE);

        // Animation au survol
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; " +
                        "-fx-border-color: #667eea; -fx-border-radius: 20; -fx-border-width: 2; " +
                        "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.25), 15, 0, 0, 4);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; " +
                        "-fx-border-color: #E5E7EB; -fx-border-radius: 20; -fx-border-width: 1.5; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);"));

        // Ligne 1 : Checkbox + Titre + Badge durée
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox checkBox = new CheckBox();
        checkBox.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
        checkBox.selectedProperty().addListener((obs, old, val) -> updateAjouterButtonState());
        checkBoxes.add(checkBox);

        Label indexLabel = new Label(String.format("%02d", index + 1));
        indexLabel.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 12px;" +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 12 4 12;");

        Label titreLabel = new Label(rec.getTitre());
        titreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        titreLabel.setWrapText(true);
        HBox.setHgrow(titreLabel, Priority.ALWAYS);

        Label dureeLabel = new Label("⏱️ " + rec.getDureeEstimeeJours() + " jours");
        dureeLabel.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-font-size: 11px;" +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 12 4 12;");

        headerRow.getChildren().addAll(checkBox, indexLabel, titreLabel, dureeLabel);

        // Ligne 2 : Description
        Text descriptionText = new Text(rec.getDescription());
        descriptionText.setStyle("-fx-font-size: 13px; -fx-fill: #6B7280;");
        TextFlow descriptionFlow = new TextFlow(descriptionText);
        descriptionFlow.setStyle("-fx-padding: 4 0 4 28;"); // Indentation pour aligner avec le titre

        // Ligne 3 : Tags supplémentaires
        HBox tagsRow = new HBox(10);
        tagsRow.setStyle("-fx-padding: 8 0 0 28;");

        // Tag niveau
        Label niveauTag = new Label("📚 " + comboNiveau.getValue());
        niveauTag.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4338CA; -fx-font-size: 10px;" +
                "-fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 3 12 3 12;");

        // Tag langue
        Label langueTag = new Label("🌍 " + comboLangue.getValue());
        langueTag.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #059669; -fx-font-size: 10px;" +
                "-fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 3 12 3 12;");

        tagsRow.getChildren().addAll(niveauTag, langueTag);

        card.getChildren().addAll(headerRow, descriptionFlow, tagsRow);
        return card;
    }

    private void updateAjouterButtonState() {
        boolean hasSelection = checkBoxes.stream().anyMatch(CheckBox::isSelected);
        btnAjouterSelection.setDisable(!hasSelection);
    }

    @FXML
    private void ajouterObjectifsSelectionnes() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté");
            return;
        }

        List<GroqRecommendationService.ObjectifRecommendation> selected = new ArrayList<>();

        for (int i = 0; i < checkBoxes.size() && i < currentRecommendations.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selected.add(currentRecommendations.get(i));
            }
        }

        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    "Veuillez sélectionner au moins un objectif à ajouter");
            return;
        }

        try {
            int addedCount = 0;
            for (GroqRecommendationService.ObjectifRecommendation rec : selected) {
                Objectif objectif = rec.toObjectif(currentUser.getId());
                objectifService.ajouter(objectif);
                addedCount++;
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès !",
                    addedCount + " objectif(s) ajouté(s) avec succès !\n\n" +
                            "✅ Titre: " + selected.get(0).getTitre() +
                            (selected.size() > 1 ? "\n✅ Et " + (selected.size() - 1) + " autre(s) objectif(s)" : ""));

            // Rafraîchir la liste des objectifs
            if (objectifController != null) {
                objectifController.refreshObjectifs();
            }

            // Fermer le dialogue
            btnFermer.getScene().getWindow().hide();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible d'ajouter les objectifs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showPlaceholder() {
        recommendationsContainer.getChildren().clear();
        checkBoxes.clear();
        btnAjouterSelection.setDisable(true);

        VBox placeholder = new VBox(20);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(60));
        placeholder.setStyle("-fx-background-color: white; -fx-background-radius: 24; " +
                "-fx-border-color: #E5E7EB; -fx-border-radius: 24; -fx-border-width: 2; " +
                "-fx-border-style: dashed;");

        Label icon = new Label("🤖✨");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("Prêt à découvrir vos objectifs ?");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1F293B;");

        Label subtitle = new Label("Décrivez vos centres d'intérêt ci-dessus\n" +
                "et laissez l'IA vous proposer des objectifs personnalisés.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B7280;");
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        placeholder.getChildren().addAll(icon, title, subtitle);
        recommendationsContainer.getChildren().add(placeholder);
    }

    @FXML
    private void fermer() {
        btnFermer.getScene().getWindow().hide();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);

        // Style personnalisé pour l'alerte
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 16;");
        dialogPane.lookup(".content.label").setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937;");

        alert.showAndWait();
    }
}