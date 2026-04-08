package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
import com.example.pijava_fluently.services.TestService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MesTestsController {

    @FXML private FlowPane      flowTests;
    @FXML private ComboBox<Langue>  comboLangueFiltre;
    @FXML private ComboBox<Niveau>  comboNiveauFiltre;
    @FXML private Label         labelResultat;
    @FXML private TextField     searchField;

    private final TestService   testService   = new TestService();
    private final LangueService langueService = new LangueService();
    private final NiveauService niveauService = new NiveauService();

    private List<Test>   tousTests;
    private List<Langue> langues;
    private List<Niveau> niveaux;
    private final int userId = 7;

    @FXML
    public void initialize() {
        chargerFiltres();
        chargerTests();
    }

    private void chargerFiltres() {
        try {
            langues = langueService.recuperer();
            comboLangueFiltre.getItems().add(null); // "Toutes"
            comboLangueFiltre.getItems().addAll(langues);
            comboLangueFiltre.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Langue l, boolean empty) {
                    super.updateItem(l, empty);
                    setText(empty ? null : l == null ? "🌍 Toutes les langues" : l.getNom());
                }
            });
            comboLangueFiltre.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Langue l, boolean empty) {
                    super.updateItem(l, empty);
                    setText(empty ? null : l == null ? "🌍 Toutes les langues" : l.getNom());
                }
            });
            comboLangueFiltre.setValue(null);
            comboLangueFiltre.valueProperty().addListener((obs, old, newL) -> {
                filtrerNiveaux(newL);
                appliquerFiltres();
            });
        } catch (SQLException e) { e.printStackTrace(); }

        try {
            niveaux = niveauService.recuperer();
            configurerComboNiveau(niveaux);
            comboNiveauFiltre.valueProperty().addListener((obs, old, newN) ->
                    appliquerFiltres());
        } catch (SQLException e) { e.printStackTrace(); }

        if (searchField != null)
            searchField.textProperty().addListener((obs, old, newV) -> appliquerFiltres());
    }

    private void filtrerNiveaux(Langue langue) {
        comboNiveauFiltre.setValue(null);
        List<Niveau> liste = langue == null ? niveaux :
                niveaux.stream().filter(n -> n.getIdLangueId() == langue.getId())
                        .collect(Collectors.toList());
        configurerComboNiveau(liste);
    }

    private void configurerComboNiveau(List<Niveau> liste) {
        comboNiveauFiltre.getItems().clear();
        comboNiveauFiltre.getItems().add(null);
        comboNiveauFiltre.getItems().addAll(liste);
        comboNiveauFiltre.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Niveau n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty ? null : n == null ? "🎯 Tous les niveaux"
                        : n.getTitre() + " (" + n.getDifficulte() + ")");
            }
        });
        comboNiveauFiltre.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Niveau n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty ? null : n == null ? "🎯 Tous les niveaux"
                        : n.getTitre() + " (" + n.getDifficulte() + ")");
            }
        });
    }

    private void chargerTests() {
        try {
            tousTests = testService.recuperer();
            appliquerFiltres();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void appliquerFiltres() {
        if (tousTests == null) return;

        Langue langueFiltre  = comboLangueFiltre.getValue();
        Niveau niveauFiltre  = comboNiveauFiltre.getValue();
        String recherche     = searchField != null
                ? searchField.getText().toLowerCase().trim() : "";

        List<Test> filtres = tousTests.stream()
                .filter(t -> langueFiltre == null || t.getLangueId() == langueFiltre.getId())
                .filter(t -> niveauFiltre == null || t.getNiveauId() == niveauFiltre.getId())
                .filter(t -> recherche.isEmpty()
                        || t.getTitre().toLowerCase().contains(recherche)
                        || t.getType().toLowerCase().contains(recherche))
                .collect(Collectors.toList());

        afficherTests(filtres);
        if (labelResultat != null)
            labelResultat.setText(filtres.size() + " test(s) disponible(s)");
    }

    private void afficherTests(List<Test> tests) {
        flowTests.getChildren().clear();
        if (tests.isEmpty()) {
            VBox vide = new VBox(12);
            vide.setAlignment(javafx.geometry.Pos.CENTER);
            vide.setPrefWidth(flowTests.getPrefWrapLength());
            Label ico = new Label("📭");
            ico.setStyle("-fx-font-size:52px;");
            Label txt = new Label("Aucun test disponible pour ces filtres");
            txt.setStyle("-fx-font-size:15px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;");
            Label sub = new Label("Essayez de modifier vos filtres");
            sub.setStyle("-fx-font-size:12px;-fx-text-fill:#C0C4D8;");
            vide.getChildren().addAll(ico, txt, sub);
            flowTests.getChildren().add(vide);
            return;
        }
        for (Test test : tests)
            flowTests.getChildren().add(creerCarteTest(test));
    }

    private VBox creerCarteTest(Test test) {
        // Retrouver les noms
        String nomLangue = langues == null ? "" :
                langues.stream().filter(l -> l.getId() == test.getLangueId())
                        .map(Langue::getNom).findFirst().orElse("");
        String nomNiveau = niveaux == null ? "" :
                niveaux.stream().filter(n -> n.getId() == test.getNiveauId())
                        .map(n -> n.getTitre() + " · " + n.getDifficulte())
                        .findFirst().orElse("");

        VBox card = new VBox(14);
        card.setPrefWidth(290);
        card.setMinHeight(220);
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);" +
                        "-fx-padding:22;-fx-cursor:hand;");

        // Icône + badge type
        String icone = switch (test.getType()) {
            case "Test de niveau"        -> "🎯";
            case "Test de fin de niveau" -> "🏆";
            case "quiz_debutant"         -> "🌱";
            default                      -> "📝";
        };
        String badgeColor = switch (test.getType()) {
            case "Test de niveau"        ->
                    "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;";
            case "Test de fin de niveau" ->
                    "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;";
            default ->
                    "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;";
        };

        HBox topRow = new HBox(8);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconeLbl = new Label(icone);
        iconeLbl.setStyle("-fx-font-size:22px;");
        Label badgeType = new Label(test.getType());
        badgeType.setStyle(badgeColor +
                "-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 10;");
        topRow.getChildren().addAll(iconeLbl, badgeType);

        // Titre
        Label titre = new Label(test.getTitre());
        titre.setWrapText(true);
        titre.setStyle(
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

        // Langue + Niveau
        HBox tags = new HBox(8);
        tags.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (!nomLangue.isEmpty()) {
            Label lTag = new Label("🌍 " + nomLangue);
            lTag.setStyle(
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#3B82F6;" +
                            "-fx-background-color:#EFF6FF;-fx-background-radius:8;-fx-padding:3 8;");
            tags.getChildren().add(lTag);
        }
        if (!nomNiveau.isEmpty()) {
            Label nTag = new Label("📶 " + nomNiveau);
            nTag.setStyle(
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#7C3AED;" +
                            "-fx-background-color:#F5F3FF;-fx-background-radius:8;-fx-padding:3 8;");
            tags.getChildren().add(nTag);
        }

        // Durée
        Label duree = new Label("⏱ " + (test.getDureeEstimee() > 0
                ? test.getDureeEstimee() + " min" : "Sans limite"));
        duree.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bouton
        Button btnStart = new Button("Commencer le test  →");
        btnStart.setMaxWidth(Double.MAX_VALUE);
        String btnBase =
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:12;-fx-padding:11 20;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.3),8,0,0,2);";
        String btnHover =
                "-fx-background-color:#5B52E0;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:12;-fx-padding:11 20;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.45),10,0,0,3);";
        btnStart.setStyle(btnBase);
        btnStart.setOnMouseEntered(e -> btnStart.setStyle(btnHover));
        btnStart.setOnMouseExited(e  -> btnStart.setStyle(btnBase));
        btnStart.setOnAction(e -> lancerTest(test));

        card.getChildren().addAll(topRow, titre, tags, duree, spacer, btnStart);

        // Hover carte
        String cardNormal =
                "-fx-background-color:white;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);" +
                        "-fx-padding:22;-fx-cursor:hand;";
        String cardHover =
                "-fx-background-color:white;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.18),22,0,0,6);" +
                        "-fx-padding:22;-fx-cursor:hand;" +
                        "-fx-border-color:#DDD6FE;-fx-border-radius:18;-fx-border-width:2;";
        card.setOnMouseEntered(e -> { card.setStyle(cardHover); card.setTranslateY(-3); });
        card.setOnMouseExited(e  -> { card.setStyle(cardNormal); card.setTranslateY(0); });

        return card;
    }

    private void lancerTest(Test test) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/test-passage.fxml"));
            Node vue = loader.load();
            TestPassageEtudiantController ctrl = loader.getController();
            ctrl.initTest(test, userId);
            StackPane contentArea = (StackPane)
                    flowTests.getScene().lookup("#contentArea");
            if (contentArea != null)
                contentArea.getChildren().setAll(vue);
        } catch (IOException e) { e.printStackTrace(); }
    }
}