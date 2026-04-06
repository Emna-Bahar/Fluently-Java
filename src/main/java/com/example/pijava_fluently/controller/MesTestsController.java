package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.TestService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MesTestsController {

    @FXML private FlowPane flowTests;

    private final TestService service = new TestService();
    private final int userId = 7; // À remplacer par l'utilisateur connecté

    @FXML
    public void initialize() {
        chargerTests();
    }

    private void chargerTests() {
        try {
            List<Test> tests = service.recuperer();
            flowTests.getChildren().clear();

            for (Test test : tests) {
                flowTests.getChildren().add(creerCarteTest(test));
            }

            if (tests.isEmpty()) {
                Label vide = new Label("Aucun test disponible pour le moment.");
                vide.setStyle("-fx-font-size:14px;-fx-text-fill:#8A8FA8;");
                flowTests.getChildren().add(vide);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox creerCarteTest(Test test) {
        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);" +
                        "-fx-padding:24;" +
                        "-fx-cursor:hand;"
        );

        // Icône selon le type
        String icone = switch (test.getType()) {
            case "Test de niveau"        -> "🎯";
            case "Test de fin de niveau" -> "🏆";
            case "quiz_debutant"         -> "🌱";
            default                      -> "📝";
        };

        // Badge type
        Label badgeType = new Label(test.getType());
        String badgeColor = switch (test.getType()) {
            case "Test de niveau"        ->
                    "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;";
            case "Test de fin de niveau" ->
                    "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;";
            default                      ->
                    "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;";
        };
        badgeType.setStyle(badgeColor +
                "-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 10;");

        // Titre
        Label titre = new Label(icone + "  " + test.getTitre());
        titre.setWrapText(true);
        titre.setStyle(
                "-fx-font-size:16px;-fx-font-weight:bold;" +
                        "-fx-text-fill:#1A1D2E;");

        // Infos
        HBox infos = new HBox(16);
        Label duree = new Label("⏱ " + (test.getDureeEstimee() > 0
                ? test.getDureeEstimee() + " min" : "Sans limite"));
        duree.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");

        infos.getChildren().add(duree);

        // Séparateur
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bouton Commencer
        javafx.scene.control.Button btnCommencer =
                new javafx.scene.control.Button("Commencer →");
        btnCommencer.setMaxWidth(Double.MAX_VALUE);
        btnCommencer.setStyle(
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:10 20;-fx-cursor:hand;");

        btnCommencer.setOnAction(e -> lancerTest(test));

        btnCommencer.setOnMouseEntered(e ->
                btnCommencer.setStyle(
                        "-fx-background-color:#5B52E0;-fx-text-fill:white;" +
                                "-fx-font-size:13px;-fx-font-weight:bold;" +
                                "-fx-background-radius:10;-fx-padding:10 20;-fx-cursor:hand;"));
        btnCommencer.setOnMouseExited(e ->
                btnCommencer.setStyle(
                        "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                                "-fx-font-size:13px;-fx-font-weight:bold;" +
                                "-fx-background-radius:10;-fx-padding:10 20;-fx-cursor:hand;"));

        card.getChildren().addAll(badgeType, titre, infos, spacer, btnCommencer);

        // Hover sur la carte
        card.setOnMouseEntered(e ->
                card.setStyle(
                        "-fx-background-color:white;" +
                                "-fx-background-radius:16;" +
                                "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.18),20,0,0,6);" +
                                "-fx-padding:24;" +
                                "-fx-cursor:hand;" +
                                "-fx-border-color:#E0DDFF;-fx-border-radius:16;-fx-border-width:2;"));
        card.setOnMouseExited(e ->
                card.setStyle(
                        "-fx-background-color:white;" +
                                "-fx-background-radius:16;" +
                                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);" +
                                "-fx-padding:24;" +
                                "-fx-cursor:hand;"));

        return card;
    }

    private void lancerTest(Test test) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/pijava_fluently/fxml/test-passage.fxml")
            );
            Node vue = loader.load();

            // Injecter le test dans le controller
            TestPassageEtudiantController ctrl = loader.getController();
            ctrl.initTest(test, userId);

            // Charger dans le contentArea
            StackPane contentArea = (StackPane)
                    flowTests.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(vue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}