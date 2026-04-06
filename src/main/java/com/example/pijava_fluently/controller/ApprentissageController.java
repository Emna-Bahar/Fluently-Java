package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Cours;
import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.services.CoursService;
import com.example.pijava_fluently.services.NiveauService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.scene.layout.Priority;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.layout.StackPane;
public class ApprentissageController {

    @FXML private Label langueNom;
    @FXML private Label langueDescription;
    @FXML private Label niveauActuelValue;

    // Niveaux
    @FXML private VBox niveauA1, niveauA2, niveauB1, niveauB2, niveauC1, niveauC2;
    @FXML private HBox coursA1, coursA2, coursB1, coursB2, coursC1, coursC2;
    @FXML private Label progressA1, progressA2, progressB1, progressB2, progressC1, progressC2;

    private Langue langue;
    private HomeController homeController;
    private Map<String, List<Cours>> coursParNiveau = new HashMap<>();
    private Map<String, Integer> progressionParNiveau = new HashMap<>();

    public void setLangue(Langue langue) {
        this.langue = langue;
        langueNom.setText(langue.getNom());
        langueDescription.setText(langue.getDescription());
        chargerCours();
    }

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void handleRetour() {
        if (homeController != null) {
            homeController.showLangues();
        }
    }

    @FXML
    private void handleTest() {
        showAlert("Test de niveau", "Cette fonctionnalité sera bientôt disponible !");
    }

    private void chargerCours() {
        new Thread(() -> {
            try {
                NiveauService niveauService = new NiveauService();
                List<Niveau> niveaux = niveauService.recuperer();

                // Filtrer les niveaux par langue
                List<Niveau> niveauxLangue = niveaux.stream()
                        .filter(n -> n.getIdLangueId() == langue.getId())
                        .collect(Collectors.toList());

                CoursService coursService = new CoursService();
                List<Cours> tousLesCours = coursService.recuperer();

                for (Niveau niveau : niveauxLangue) {
                    String difficulte = niveau.getDifficulte();
                    String niveauKey = extractNiveauKey(difficulte);

                    if (niveauKey != null) {
                        List<Cours> coursNiveau = tousLesCours.stream()
                                .filter(c -> c.getIdNiveauId() == niveau.getId())
                                .sorted((c1, c2) -> Integer.compare(c1.getNumero(), c2.getNumero()))
                                .collect(Collectors.toList());
                        coursParNiveau.put(niveauKey, coursNiveau);

                        // Compter les cours complétés (simulation pour l'instant)
                        int completed = 0;
                        progressionParNiveau.put(niveauKey, completed);
                    }
                }

                Platform.runLater(() -> afficherNiveaux());

            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de charger les cours"));
            }
        }).start();
    }

    private String extractNiveauKey(String difficulte) {
        if (difficulte == null) return null;
        if (difficulte.contains("A1")) return "A1";
        if (difficulte.contains("A2")) return "A2";
        if (difficulte.contains("B1")) return "B1";
        if (difficulte.contains("B2")) return "B2";
        if (difficulte.contains("C1")) return "C1";
        if (difficulte.contains("C2")) return "C2";
        return null;
    }

    private void afficherNiveaux() {
        afficherCoursPourNiveau("A1", coursA1, progressA1);
        afficherCoursPourNiveau("A2", coursA2, progressA2);
        afficherCoursPourNiveau("B1", coursB1, progressB1);
        afficherCoursPourNiveau("B2", coursB2, progressB2);
        afficherCoursPourNiveau("C1", coursC1, progressC1);
        afficherCoursPourNiveau("C2", coursC2, progressC2);
    }

    private void afficherCoursPourNiveau(String niveauKey, HBox container, Label progressLabel) {
        container.getChildren().clear();

        List<Cours> cours = coursParNiveau.get(niveauKey);
        if (cours == null || cours.isEmpty()) {
            Label emptyLabel = new Label("Aucun cours disponible");
            emptyLabel.setStyle("-fx-text-fill:#8A8FA8;-fx-font-size:12px;");
            container.getChildren().add(emptyLabel);
            if (progressLabel != null) progressLabel.setText("0/0 cours");
            return;
        }

        int completed = progressionParNiveau.getOrDefault(niveauKey, 0);
        if (progressLabel != null) {
            progressLabel.setText(completed + "/" + cours.size() + " cours");
        }

        // Créer les cercles pour chaque cours (max 3 pour l'affichage)
        int maxCours = Math.min(cours.size(), 6);
        for (int i = 0; i < maxCours; i++) {
            Cours c = cours.get(i);
            VBox cercle = createCercleCours(c, i + 1, i < completed);
            container.getChildren().add(cercle);
        }
    }

    private VBox createCercleCours(Cours cours, int numero, boolean estComplete) {
        VBox cercle = new VBox();
        cercle.setAlignment(Pos.CENTER);
        cercle.setSpacing(8);
        cercle.setPrefWidth(80);
        cercle.setPrefHeight(100);
        cercle.setStyle("-fx-cursor: hand;");

        // Le cercle lui-même
        StackPane circle = new StackPane();
        circle.setPrefSize(60, 60);

        Label numberLabel = new Label(String.valueOf(numero));
        numberLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");

        if (estComplete) {
            circle.setStyle("-fx-background-color:#10B981;-fx-background-radius:30;-fx-effect:dropshadow(gaussian,rgba(16,185,129,0.3),8,0,0,2);");
            Label checkLabel = new Label("✓");
            checkLabel.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:white;");
            circle.getChildren().add(checkLabel);
        } else {
            circle.setStyle("-fx-background-color:#6C63FF;-fx-background-radius:30;-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.3),8,0,0,2);");
            circle.getChildren().add(numberLabel);
        }

        // Label du cours
        Label coursLabel = new Label("Cours " + numero);
        coursLabel.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#4A4D6A;");

        cercle.getChildren().addAll(circle, coursLabel);

        // Effet hover
        cercle.setOnMouseEntered(e -> {
            if (!estComplete) {
                circle.setStyle("-fx-background-color:#5849C4;-fx-background-radius:30;-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.5),12,0,0,4);");
                cercle.setTranslateY(-3);
            }
        });
        cercle.setOnMouseExited(e -> {
            if (!estComplete) {
                circle.setStyle("-fx-background-color:#6C63FF;-fx-background-radius:30;-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.3),8,0,0,2);");
                cercle.setTranslateY(0);
            }
        });

        // Action au clic
        cercle.setOnMouseClicked(e -> ouvrirCours(cours));

        return cercle;
    }

    private void ouvrirCours(Cours cours) {
        if (cours.getRessource() == null || cours.getRessource().isEmpty()) {
            showAlert("Information", "Aucune ressource disponible pour ce cours.");
            return;
        }

        // Créer un dialogue moderne
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cours N°" + cours.getNumero());
        dialog.setHeaderText(null);

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setPrefWidth(600);
        content.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 20;");

        // Titre avec icône
        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setStyle("-fx-background-color: #6C63FF; -fx-background-radius: 15; -fx-padding: 15 20 15 20;");

        Label titleIcon = new Label("📚");
        titleIcon.setStyle("-fx-font-size: 24px;");

        Label titleLabel = new Label("Cours N°" + cours.getNumero());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        titleBox.getChildren().addAll(titleIcon, titleLabel);

        // Séparateur
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #E0E3F0;");

        // Catégories de ressources
        Label categoriesTitle = new Label("📎 Ressources disponibles");
        categoriesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E; -fx-padding: 0 0 10 0;");

        // Conteneur principal pour les catégories
        VBox categoriesContainer = new VBox(15);

        // Séparer les ressources par type
        String[] ressources = cours.getRessource().split("\n");

        // Listes par catégorie
        List<String> youtubeLinks = new ArrayList<>();
        List<String> pdfFiles = new ArrayList<>();
        List<String> videoFiles = new ArrayList<>();
        List<String> audioFiles = new ArrayList<>();
        List<String> imageFiles = new ArrayList<>();
        List<String> otherFiles = new ArrayList<>();

        for (String res : ressources) {
            String lowerRes = res.toLowerCase();
            if (lowerRes.contains("youtube.com") || lowerRes.contains("youtu.be")) {
                youtubeLinks.add(res);
            } else if (lowerRes.endsWith(".pdf")) {
                pdfFiles.add(res);
            } else if (lowerRes.endsWith(".mp4") || lowerRes.endsWith(".webm") || lowerRes.endsWith(".mov")) {
                videoFiles.add(res);
            } else if (lowerRes.endsWith(".mp3") || lowerRes.endsWith(".wav") || lowerRes.endsWith(".ogg")) {
                audioFiles.add(res);
            } else if (lowerRes.endsWith(".png") || lowerRes.endsWith(".jpg") || lowerRes.endsWith(".jpeg") || lowerRes.endsWith(".gif") || lowerRes.endsWith(".webp")) {
                imageFiles.add(res);
            } else {
                otherFiles.add(res);
            }
        }

        // Ajouter les catégories non vides
        if (!youtubeLinks.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🎬 Vidéos YouTube", youtubeLinks, "#FF6B6B"));
        }
        if (!pdfFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("📄 Documents PDF", pdfFiles, "#4ECDC4"));
        }
        if (!videoFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🎥 Vidéos", videoFiles, "#A8E6CF"));
        }
        if (!audioFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🎵 Audios", audioFiles, "#FFD93D"));
        }
        if (!imageFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🖼️ Images", imageFiles, "#C5E8F7"));
        }
        if (!otherFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("📎 Autres fichiers", otherFiles, "#D4A5A5"));
        }

        // Si aucune catégorie
        if (categoriesContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("📭 Aucune ressource disponible");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8A8FA8; -fx-padding: 20;");
            emptyLabel.setAlignment(Pos.CENTER);
            categoriesContainer.getChildren().add(emptyLabel);
        }

        // ScrollPane pour les ressources (si beaucoup)
        ScrollPane scrollRessources = new ScrollPane(categoriesContainer);
        scrollRessources.setFitToWidth(true);
        scrollRessources.setPrefHeight(400);
        scrollRessources.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        content.getChildren().addAll(titleBox, separator, categoriesTitle, scrollRessources);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 20;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setText("Fermer");
        closeButton.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 25 10 25; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    // Méthode pour créer une boîte de catégorie
    private VBox createCategoryBox(String categoryName, List<String> items, String color) {
        VBox categoryBox = new VBox(10);
        categoryBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2); -fx-padding: 15;");

        // En-tête de catégorie
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label categoryIcon = new Label(categoryName.substring(0, 2));
        categoryIcon.setStyle("-fx-font-size: 18px;");

        Label categoryLabel = new Label(categoryName);
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");

        Label countBadge = new Label(String.valueOf(items.size()));
        countBadge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 3 10 3 10;");

        header.getChildren().addAll(categoryIcon, categoryLabel, countBadge);
        HBox.setHgrow(categoryLabel, Priority.ALWAYS);

        // Liste des ressources
        VBox itemsBox = new VBox(8);

        for (String item : items) {
            HBox itemRow = new HBox(12);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 10; -fx-padding: 10 15 10 15;");
            itemRow.setEffect(new DropShadow(2, Color.rgb(0, 0, 0, 0.05)));

            // Icône selon le type
            Label itemIcon = new Label();
            if (item.contains("youtube.com") || item.contains("youtu.be")) {
                itemIcon.setText("🎬");
            } else if (item.toLowerCase().endsWith(".pdf")) {
                itemIcon.setText("📄");
            } else if (item.toLowerCase().endsWith(".mp4") || item.toLowerCase().endsWith(".webm")) {
                itemIcon.setText("🎥");
            } else if (item.toLowerCase().endsWith(".mp3") || item.toLowerCase().endsWith(".wav")) {
                itemIcon.setText("🎵");
            } else if (item.toLowerCase().endsWith(".png") || item.toLowerCase().endsWith(".jpg")) {
                itemIcon.setText("🖼️");
            } else {
                itemIcon.setText("📎");
            }
            itemIcon.setStyle("-fx-font-size: 18px;");

            // Nom du fichier (tronqué)
            String fileName = item;
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            if (fileName.length() > 40) {
                fileName = fileName.substring(0, 37) + "...";
            }

            Label itemLabel = new Label(fileName);
            itemLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A4D6A;");
            itemLabel.setMaxWidth(350);
            itemLabel.setWrapText(true);

            Button openBtn = new Button("Ouvrir");
            openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;");
            openBtn.setOnMouseEntered(e -> openBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;"));
            openBtn.setOnMouseExited(e -> openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;"));
            openBtn.setOnAction(e -> ouvrirRessource(item));

            itemRow.getChildren().addAll(itemIcon, itemLabel, openBtn);
            HBox.setHgrow(itemLabel, Priority.ALWAYS);

            itemsBox.getChildren().add(itemRow);
        }

        categoryBox.getChildren().addAll(header, itemsBox);
        return categoryBox;
    }

    private void ouvrirRessource(String chemin) {
        try {
            if (chemin.contains("youtube.com") || chemin.contains("youtu.be")) {
                Desktop.getDesktop().browse(new URI(chemin));
            } else {
                File file = new File(chemin);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("Erreur", "Fichier introuvable : " + chemin);
                }
            }
        } catch (IOException | URISyntaxException e) {
            showAlert("Erreur", "Impossible d'ouvrir la ressource");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}