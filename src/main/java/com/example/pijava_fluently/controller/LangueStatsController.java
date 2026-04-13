package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.entites.User_progress;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.TestPassageService;
import com.example.pijava_fluently.services.TestService;
import com.example.pijava_fluently.services.UserProgressService;
import com.example.pijava_fluently.services.WikipediaService;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class LangueStatsController {

    @FXML private TableView<LangueStat> classementTable;
    @FXML private TextField searchField;
    @FXML private VBox statsCard;
    @FXML private Label langueFlag;
    @FXML private Label langueNom;
    @FXML private Label langueFamille;
    @FXML private Label tendanceBadge;
    @FXML private Label locuteursValue;
    @FXML private Label paysValue;
    @FXML private Label ecritureValue;
    @FXML private Label nbEtudiantsValue;
    @FXML private Label tauxCompletionValue;
    @FXML private Label nbTestsValue;
    @FXML private Label scoreMoyenValue;
    @FXML private Label descriptionValue;
    @FXML private Label funfactValue;
    @FXML private HBox difficulteStars;
    @FXML private ProgressIndicator loadingIndicator;

    private final LangueService langueService = new LangueService();
    private final UserProgressService userProgressService = new UserProgressService();
    private final TestPassageService testPassageService = new TestPassageService();
    private final TestService testService = new TestService();
    private final WikipediaService wikipediaService = new WikipediaService();

    @FXML
    public void initialize() {
        setupClassementTable();
        chargerClassement();
    }

    private void setupClassementTable() {
        // Configuration du tableau (comme avant)
        TableColumn<LangueStat, Integer> colPosition = new TableColumn<>("Position");
        colPosition.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(classementTable.getItems().indexOf(cellData.getValue()) + 1).asObject());
        colPosition.setStyle("-fx-alignment: CENTER;");
        colPosition.setPrefWidth(80);

        TableColumn<LangueStat, String> colLangue = new TableColumn<>("Langue");
        colLangue.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLangueComplet()));
        colLangue.setPrefWidth(200);

        TableColumn<LangueStat, Long> colEtudiants = new TableColumn<>("Étudiants");
        colEtudiants.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getNbEtudiants()));
        colEtudiants.setStyle("-fx-alignment: CENTER;");
        colEtudiants.setPrefWidth(120);

        TableColumn<LangueStat, Double> colCompletion = new TableColumn<>("Taux complétion");
        colCompletion.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTauxCompletion()));
        colCompletion.setPrefWidth(200);
        colCompletion.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar pb = new ProgressBar();
            private final Label label = new Label();
            private final HBox container = new HBox(8);
            {
                pb.setPrefWidth(100);
                pb.setPrefHeight(8);
                label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(pb, label);
            }
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    double progress = item / 3.0;
                    pb.setProgress(progress);
                    label.setText(String.format("%.1f/3 cours", item));
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        classementTable.getColumns().addAll(colPosition, colLangue, colEtudiants, colCompletion);
    }

    private void chargerClassement() {
        try {
            List<Langue> langues = langueService.recuperer();
            List<User_progress> progresses = userProgressService.recuperer();
            List<LangueStat> classement = new ArrayList<>();

            for (Langue langue : langues) {
                long nbEtudiants = progresses.stream()
                        .filter(p -> p.getLangueId() == langue.getId())
                        .map(User_progress::getUserId)
                        .distinct()
                        .count();

                double tauxCompletion = progresses.stream()
                        .filter(p -> p.getLangueId() == langue.getId())
                        .mapToInt(User_progress::getDernierNumeroCours)
                        .average()
                        .orElse(0);

                if (nbEtudiants > 0) {
                    classement.add(new LangueStat(getFlagEmoji(langue.getNom()), langue.getNom(), nbEtudiants, tauxCompletion));
                }
            }

            classement.sort((a, b) -> Long.compare(b.getNbEtudiants(), a.getNbEtudiants()));
            classementTable.getItems().setAll(classement);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void selectAnglais() { chargerStatsLangue("Anglais"); }
    @FXML
    private void selectFrancais() { chargerStatsLangue("Français"); }
    @FXML
    private void selectEspagnol() { chargerStatsLangue("Espagnol"); }
    @FXML
    private void selectAllemand() { chargerStatsLangue("Allemand"); }
    @FXML
    private void selectArabe() { chargerStatsLangue("Arabe"); }

    @FXML
    private void handleSearch() {
        String langue = searchField.getText().trim();
        if (!langue.isEmpty()) {
            chargerStatsLangue(langue);
        }
    }

    private void chargerStatsLangue(String nomLangue) {
        // Afficher un indicateur de chargement
        statsCard.setVisible(false);
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        // Récupérer les données en arrière-plan
        new Thread(() -> {
            try {
                // 1. Récupérer les données Wikipedia
                Map<String, Object> wikiData = wikipediaService.getLangueInfo(nomLangue);

                // 2. Récupérer les données de l'application
                Map<String, Object> appData = getAppData(nomLangue);

                // 3. Mettre à jour l'interface
                Platform.runLater(() -> {
                    updateUI(nomLangue, wikiData, appData);
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    statsCard.setVisible(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Erreur", "Impossible de charger les données pour " + nomLangue);
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                });
            }
        }).start();
    }

    private Map<String, Object> getAppData(String nomLangue) throws SQLException {
        Map<String, Object> data = new HashMap<>();

        // Récupérer l'ID de la langue
        List<Langue> langues = langueService.recuperer();
        int langueId = langues.stream()
                .filter(l -> l.getNom().equalsIgnoreCase(nomLangue))
                .map(Langue::getId)
                .findFirst()
                .orElse(0);

        if (langueId == 0) {
            data.put("nbEtudiants", 0);
            data.put("tauxCompletion", 0);
            data.put("nbTests", 0);
            data.put("scoreMoyen", 0);
            return data;
        }

        // Statistiques depuis la base
        List<User_progress> progresses = userProgressService.recuperer();
        List<TestPassage> passages = testPassageService.recuperer();
        List<Test> tests = testService.recuperer();

        List<Integer> testIds = tests.stream()
                .filter(t -> t.getLangueId() == langueId)
                .map(Test::getId)
                .collect(Collectors.toList());

        long nbEtudiants = progresses.stream()
                .filter(p -> p.getLangueId() == langueId)
                .map(User_progress::getUserId)
                .distinct()
                .count();
        data.put("nbEtudiants", nbEtudiants);

        double tauxCompletion = progresses.stream()
                .filter(p -> p.getLangueId() == langueId)
                .mapToInt(User_progress::getDernierNumeroCours)
                .average()
                .orElse(0);
        data.put("tauxCompletion", Math.round(tauxCompletion * 10.0) / 10.0);

        long nbTests = passages.stream()
                .filter(p -> testIds.contains(p.getTestId()))
                .count();
        data.put("nbTests", nbTests);

        double scoreMoyen = passages.stream()
                .filter(p -> testIds.contains(p.getTestId()))
                .filter(p -> p.getScoreMax() > 0)
                .mapToDouble(p -> (double) p.getScore() / p.getScoreMax() * 100)
                .average()
                .orElse(0);
        data.put("scoreMoyen", Math.round(scoreMoyen));

        String tendance;
        if (nbEtudiants > 10) tendance = "📈 Très populaire";
        else if (nbEtudiants > 5) tendance = "📈 Populaire";
        else if (nbEtudiants > 2) tendance = "📊 Moyenne";
        else tendance = "📉 Peu étudiée";
        data.put("tendance", tendance);

        return data;
    }

    private void updateUI(String nomLangue, Map<String, Object> wikiData, Map<String, Object> appData) {
        langueFlag.setText(getFlagEmoji(nomLangue));
        langueNom.setText(nomLangue);
        langueFamille.setText("Famille: " + wikiData.getOrDefault("famille", "Non classifiée"));
        tendanceBadge.setText(String.valueOf(appData.get("tendance")));

        locuteursValue.setText(String.valueOf(wikiData.getOrDefault("locuteurs", "?")));
        paysValue.setText(String.valueOf(wikiData.getOrDefault("pays", "Non spécifié")));
        ecritureValue.setText(String.valueOf(wikiData.getOrDefault("ecriture", "Variable")));

        nbEtudiantsValue.setText(appData.get("nbEtudiants") + " étudiant(s)");
        tauxCompletionValue.setText(appData.get("tauxCompletion") + "/3 cours");
        nbTestsValue.setText(appData.get("nbTests") + " test(s)");
        scoreMoyenValue.setText(appData.get("scoreMoyen") + "%");

        // Description depuis Wikipedia (ou fallback)
        String description = (String) wikiData.getOrDefault("description", getLocalDescription(nomLangue));
        descriptionValue.setText(description);

        funfactValue.setText(String.valueOf(wikiData.getOrDefault("funfact", "Découvrez cette langue fascinante !")));

        // Étoiles de difficulté
        int difficulte = (int) wikiData.getOrDefault("difficulte", 3);
        difficulteStars.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= difficulte ? "★" : "☆");
            star.setStyle(i <= difficulte ? "-fx-text-fill:#F59E0B;-fx-font-size:18px;"
                    : "-fx-text-fill:#D1D5DB;-fx-font-size:18px;");
            difficulteStars.getChildren().add(star);
        }
    }

    private String getFlagEmoji(String nom) {
        return switch (nom.toLowerCase()) {
            case "anglais" -> "🇬🇧";
            case "français" -> "🇫🇷";
            case "espagnol" -> "🇪🇸";
            case "allemand" -> "🇩🇪";
            case "arabe" -> "🇸🇦";
            case "italien" -> "🇮🇹";
            case "chinois" -> "🇨🇳";
            case "japonais" -> "🇯🇵";
            case "russe" -> "🇷🇺";
            default -> "🌍";
        };
    }

    private String getLocalDescription(String nomLangue) {
        return switch (nomLangue.toLowerCase()) {
            case "anglais" -> "L'anglais est une langue germanique occidentale originaire d'Angleterre.";
            case "français" -> "Le français est une langue romane parlée en France, Belgique, Suisse, Canada...";
            case "espagnol" -> "L'espagnol est une langue romane parlée en Espagne et en Amérique latine.";
            case "allemand" -> "L'allemand est une langue germanique parlée en Allemagne, Autriche, Suisse.";
            case "arabe" -> "L'arabe est une langue sémitique parlée dans le monde arabe.";
            default -> "Informations non disponibles pour cette langue.";
        };
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Classe interne pour le tableau
    public static class LangueStat {
        private final String drapeau;
        private final String nom;
        private final long nbEtudiants;
        private final double tauxCompletion;

        public LangueStat(String drapeau, String nom, long nbEtudiants, double tauxCompletion) {
            this.drapeau = drapeau;
            this.nom = nom;
            this.nbEtudiants = nbEtudiants;
            this.tauxCompletion = tauxCompletion;
        }

        public String getLangueComplet() { return drapeau + " " + nom; }
        public long getNbEtudiants() { return nbEtudiants; }
        public double getTauxCompletion() { return tauxCompletion; }
    }
}