package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.services.CertificatService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.*;

public class CertificatController {

    private final CertificatService certificatService = new CertificatService();

    /**
     * Appelé après une réussite de "Test de fin de niveau".
     * Affiche une popup de félicitations avec bouton de téléchargement.
     */
    public void afficherPopupCertificat(TestPassage passage, Test test,
                                        User user, String niveau, String langue) {
        Platform.runLater(() -> {
            double scorePct = passage.getScoreMax() > 0
                    ? (passage.getScore() * 100.0 / passage.getScoreMax())
                    : 0;

            // Layout principal de la popup
            VBox root = new VBox(16);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(32));
            root.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #6C63FF;
                -fx-border-width: 2px;
                -fx-border-radius: 12px;
                -fx-background-radius: 12px;
                """);

            // Icône trophée (SVG dessiné en JavaFX avec Label)
            Label icone = new Label("🏆");
            icone.setStyle("-fx-font-size: 48px;");

            // Titre
            Label titre = new Label("Félicitations !");
            titre.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

            // Niveau obtenu
            Label niveauLabel = new Label("Niveau " + niveau + " — " + langue);
            niveauLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

            // Score
            Label scoreLabel = new Label(String.format("Score : %.0f%%", scorePct));
            scoreLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

            // Message
            Label message = new Label(
                    user.getPrenom() + ", vous avez validé ce niveau !\n"
                            + "Votre certificat officiel CECRL est prêt.");
            message.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-text-alignment: center;");
            message.setWrapText(true);
            message.setAlignment(Pos.CENTER);

            // Barre de progression du score
            ProgressBar progressBar = new ProgressBar(scorePct / 100.0);
            progressBar.setPrefWidth(250);
            progressBar.setStyle("-fx-accent: #6C63FF;");

            // Bouton télécharger
            Button btnTelecharger = new Button("Télécharger mon certificat PDF");
            btnTelecharger.setStyle("""
                -fx-background-color: #6C63FF;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-padding: 10 20;
                -fx-background-radius: 8px;
                -fx-cursor: hand;
                """);

            // Bouton fermer
            Button btnFermer = new Button("Fermer");
            btnFermer.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #999;
                -fx-font-size: 12px;
                -fx-cursor: hand;
                """);

            // Action : générer et sauvegarder le PDF
            btnTelecharger.setOnAction(e -> {
                btnTelecharger.setDisable(true);
                btnTelecharger.setText("Génération en cours...");

                new Thread(() -> {
                    try {
                        String prenomNom = user.getPrenom() + " " + user.getNom();

                        // ← 5 paramètres seulement, sans test.getId()
                        String cheminPdf = certificatService.genererCertificat(
                                prenomNom, niveau, langue, scorePct, user.getId());

                        Platform.runLater(() -> {
                            FileChooser fc = new FileChooser();
                            fc.setTitle("Enregistrer le certificat");
                            fc.setInitialFileName("Certificat_" + niveau + "_"
                                    + langue.replace(" ", "_") + ".pdf");
                            fc.getExtensionFilters().add(
                                    new FileChooser.ExtensionFilter("PDF", "*.pdf"));

                            Stage stage = (Stage) btnTelecharger.getScene().getWindow();
                            File destination = fc.showSaveDialog(stage);

                            if (destination != null) {
                                try {
                                    Files.copy(Paths.get(cheminPdf),
                                            destination.toPath(),
                                            StandardCopyOption.REPLACE_EXISTING);

                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(destination);
                                    }

                                    btnTelecharger.setText("Certificat téléchargé !");
                                    btnTelecharger.setStyle("""
                            -fx-background-color: #059669;
                            -fx-text-fill: white;
                            -fx-font-size: 14px;
                            -fx-font-weight: bold;
                            -fx-padding: 10 20;
                            -fx-background-radius: 8px;
                            """);
                                } catch (Exception ex) {
                                    afficherErreur("Impossible de copier : " + ex.getMessage());
                                }
                            } else {
                                btnTelecharger.setDisable(false);
                                btnTelecharger.setText("Télécharger mon certificat PDF");
                            }
                        });

                    } catch (Exception ex) {
                        Platform.runLater(() ->
                                afficherErreur("Erreur génération PDF : " + ex.getMessage()));
                    }
                }, "thread-certificat").start();
            });

            // Fermer la popup
            btnFermer.setOnAction(e ->
                    ((Stage) btnFermer.getScene().getWindow()).close());

            root.getChildren().addAll(
                    icone, titre, niveauLabel, scoreLabel,
                    progressBar, message, btnTelecharger, btnFermer);

            // Créer et afficher la fenêtre popup
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 450);
            Stage popup = new Stage();
            popup.setTitle("Certificat CECRL — Fluently");
            popup.setScene(scene);
            popup.setResizable(false);
            popup.show();
        });
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Erreur");
        alert.showAndWait();
    }
}