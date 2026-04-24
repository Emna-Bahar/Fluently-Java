package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.*;
import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Map;

public class PerformanceDashboardController {

    @FXML private ComboBox<Langue> langueComboBox;
    @FXML private ProgressIndicator loadingIndicator;

    // Cartes prédiction
    @FXML private Label labelPredictionScore;
    @FXML private Label labelPredictionConfiance;
    @FXML private Label labelPredictionTendance;

    // Carte fraude
    @FXML private Label labelRisqueFraude;
    @FXML private Label labelRisqueNiveau;
    @FXML private VBox facteursFraudeBox;

    // Carte horaire
    @FXML private Label labelMeilleurCreneau;
    @FXML private Label labelScoreCreneau;
    @FXML private HBox barresHoraires;

    // Graphiques
    @FXML private LineChart<String, Number> progressionChart;
    @FXML private BarChart<String, Number> competencesChart;

    // Jauge confiance
    @FXML private ProgressBar confianceBar;
    @FXML private Label labelConfianceNiveau;

    // Analyse IA
    @FXML private TextArea analyseIATextArea;

    private AdvancedAnalyticsService analyticsService;
    private User currentUser;
    private List<Langue> langues;

    @FXML
    public void initialize() {
        analyticsService = new AdvancedAnalyticsService();
        setupCharts();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadLanguages();
    }

    private void setupCharts() {
        // Configuration du graphique de progression
        progressionChart.setTitle("Évolution des scores");
        progressionChart.getXAxis().setLabel("Date");
        progressionChart.getYAxis().setLabel("Score (%)");
        progressionChart.setCreateSymbols(true);
        progressionChart.setLegendVisible(false);

        // Configuration du graphique des compétences
        competencesChart.setTitle("Compétences par type");
        competencesChart.getXAxis().setLabel("Compétence");
        competencesChart.getYAxis().setLabel("Score (%)");
        competencesChart.setLegendVisible(false);
        competencesChart.setCategoryGap(30);
        competencesChart.setBarGap(10);
    }

    private void loadLanguages() {
        try {
            LangueService langueService = new LangueService();
            langues = langueService.recuperer();
            langueComboBox.getItems().addAll(langues);

            if (!langues.isEmpty()) {
                langueComboBox.getSelectionModel().selectFirst();
            }

            langueComboBox.valueProperty().addListener((obs, old, newLangue) -> {
                if (newLangue != null) {
                    loadDashboardData(newLangue);
                }
            });

        } catch (Exception e) {
            LoggerUtil.error("Erreur chargement langues", e);
        }
    }

    private void loadDashboardData(Langue langue) {
        loadingIndicator.setVisible(true);

        new Thread(() -> {
            try {
                // 1. Prédiction de score
                AdvancedAnalyticsService.PredictionResult prediction =
                        analyticsService.predireScore(currentUser.getId(), langue.getId());

                // 2. Analyse de fraude
                AdvancedAnalyticsService.FraudAnalysis fraude =
                        analyticsService.analyserRisqueFraude(currentUser.getId());

                // 3. Progression
                List<AdvancedAnalyticsService.PointProgression> progression =
                        analyticsService.getProgression(currentUser.getId(), langue.getId());

                // 4. Heatmap compétences
                AdvancedAnalyticsService.CompetencesHeatmap competences =
                        analyticsService.getCompetencesHeatmap(currentUser.getId(), langue.getId());

                // 5. Meilleur créneau horaire
                AdvancedAnalyticsService.HoraireAnalysis horaire =
                        analyticsService.getMeilleurCreneau(currentUser.getId());

                // 6. Score de confiance
                AdvancedAnalyticsService.ConfianceAnalysis confiance =
                        analyticsService.getScoreConfiance(currentUser.getId());

                // 7. Analyse IA
                String analyseIA = analyticsService.genererAnalyseIA(
                        currentUser.getId(),
                        currentUser.getPrenom(),
                        langue.getNom()
                );

                Platform.runLater(() -> {
                    updatePredictionCard(prediction);
                    updateFraudeCard(fraude);
                    updateProgressionChart(progression);
                    updateCompetencesChart(competences);
                    updateHoraireCard(horaire);
                    updateConfianceCard(confiance);
                    analyseIATextArea.setText(analyseIA);
                    loadingIndicator.setVisible(false);
                });

            } catch (Exception e) {
                LoggerUtil.error("Erreur chargement dashboard", e);
                Platform.runLater(() -> loadingIndicator.setVisible(false));
            }
        }).start();
    }

    private void updatePredictionCard(AdvancedAnalyticsService.PredictionResult pred) {
        if (pred == null) {
            labelPredictionScore.setText("?");
            labelPredictionConfiance.setText("Assez de tests");
            labelPredictionTendance.setText("?");
            return;
        }

        labelPredictionScore.setText(String.format("%.0f%% ± %.0f%%", pred.prediction, pred.marge));
        labelPredictionConfiance.setText("Basé sur " + pred.nbTests + " test(s)");

        String tendanceStyle;
        String tendanceIcone;
        switch (pred.tendance) {
            case "progression":
                tendanceIcone = "📈";
                tendanceStyle = "#059669";
                break;
            case "régression":
                tendanceIcone = "📉";
                tendanceStyle = "#DC2626";
                break;
            default:
                tendanceIcone = "➡️";
                tendanceStyle = "#6B7280";
        }
        labelPredictionTendance.setText(tendanceIcone + " " + pred.tendance);
        labelPredictionTendance.setStyle("-fx-text-fill: " + tendanceStyle);
    }

    private void updateFraudeCard(AdvancedAnalyticsService.FraudAnalysis fraude) {
        labelRisqueFraude.setText(fraude.couleur + " " + fraude.niveau);
        labelRisqueNiveau.setText("Score: " + fraude.score + "/100");

        String couleur;
        if (fraude.score >= 70) couleur = "#DC2626";
        else if (fraude.score >= 40) couleur = "#D97706";
        else couleur = "#059669";
        labelRisqueFraude.setStyle("-fx-text-fill: " + couleur + "; -fx-font-weight: bold;");

        facteursFraudeBox.getChildren().clear();
        for (String facteur : fraude.facteurs) {
            Label f = new Label("• " + facteur);
            f.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
            facteursFraudeBox.getChildren().add(f);
        }
    }

    private void updateProgressionChart(List<AdvancedAnalyticsService.PointProgression> points) {
        progressionChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Score");

        for (AdvancedAnalyticsService.PointProgression p : points) {
            series.getData().add(new XYChart.Data<>(p.date, p.score));
        }
        progressionChart.getData().add(series);
    }

    private void updateCompetencesChart(AdvancedAnalyticsService.CompetencesHeatmap comp) {
        competencesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Score");

        series.getData().add(new XYChart.Data<>("Grammaire", comp.grammaire));
        series.getData().add(new XYChart.Data<>("Vocabulaire", comp.vocabulaire));
        series.getData().add(new XYChart.Data<>("Compréhension", comp.comprehension));
        series.getData().add(new XYChart.Data<>("Oral", comp.oral));

        competencesChart.getData().add(series);
    }

    private void updateHoraireCard(AdvancedAnalyticsService.HoraireAnalysis horaire) {
        labelMeilleurCreneau.setText(horaire.meilleurCreneau.isEmpty() ? "Non disponible" : horaire.meilleurCreneau);
        labelScoreCreneau.setText(String.format("Score moyen: %.0f%%", horaire.meilleurScore));

        // Afficher les barres pour chaque créneau
        barresHoraires.getChildren().clear();
        String[] ordre = {"Matin (6h-12h)", "Après-midi (12h-18h)", "Soirée (18h-23h)", "Nuit (23h-6h)"};
        double max = 0;
        for (String c : ordre) {
            double moy = horaire.scoresParCreneau.getOrDefault(c, List.of()).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            max = Math.max(max, moy);
        }

        for (String creneau : ordre) {
            List<Double> scores = horaire.scoresParCreneau.get(creneau);
            double moyenne = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            VBox barreBox = new VBox(5);
            barreBox.setAlignment(Pos.CENTER);
            barreBox.setPrefWidth(80);

            Label nom = new Label(creneau.split(" ")[0]);
            nom.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");

            double hauteur = max > 0 ? (moyenne / max) * 60 : 0;
            Rectangle barre = new Rectangle(30, hauteur);
            if (creneau.equals(horaire.meilleurCreneau)) {
                barre.setFill(Color.web("#059669"));
            } else {
                barre.setFill(Color.web("#C0C7D0"));
            }
            barre.setArcWidth(5);
            barre.setArcHeight(5);

            Label valeur = new Label(String.format("%.0f%%", moyenne));
            valeur.setStyle("-fx-font-size: 9px; -fx-text-fill: #4A4D6A;");

            barreBox.getChildren().addAll(nom, barre, valeur);
            barresHoraires.getChildren().add(barreBox);
        }
    }

    private void updateConfianceCard(AdvancedAnalyticsService.ConfianceAnalysis confiance) {
        confianceBar.setProgress(confiance.score / 100);
        labelConfianceNiveau.setText(confiance.niveau);

        String couleur;
        if (confiance.score >= 70) couleur = "#059669";
        else if (confiance.score >= 50) couleur = "#D97706";
        else couleur = "#DC2626";
        confianceBar.setStyle("-fx-accent: " + couleur + ";");
        labelConfianceNiveau.setStyle("-fx-text-fill: " + couleur + "; -fx-font-weight: bold;");
    }
}