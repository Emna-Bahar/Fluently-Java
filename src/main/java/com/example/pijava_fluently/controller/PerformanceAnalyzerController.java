package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.PerformanceAnalyzerService;
import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PerformanceAnalyzerController {

    @FXML private ComboBox<Langue>          langueComboBox;
    @FXML private Label                     testsPassesLabel;
    @FXML private Label                     scoreMoyenLabel;
    @FXML private Label                     meilleurScoreLabel;
    @FXML private Label                     dernierScoreLabel;
    @FXML private Label                     progressionLabel;
    @FXML private LineChart<String, Number> progressionChart;
    @FXML private VBox                      competencesContainer;
    @FXML private TextArea                  recommendationsArea;
    @FXML private Label                     messageEncouragementLabel;
    @FXML private ProgressIndicator         loadingIndicator;

    private PerformanceAnalyzerService performanceService;
    private User                       currentUser;

    @FXML
    public void initialize() {
        performanceService = new PerformanceAnalyzerService();

        // Afficher le nom de la langue dans le ComboBox
        langueComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Langue item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNom());
            }
        });
        langueComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Langue item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNom());
            }
        });

        // Listener : charger les données quand une langue est sélectionnée
        langueComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && currentUser != null) {
                loadPerformanceData(newVal);
            }
        });
    }

    /**
     * IMPORTANT : appelé depuis MesTestsController après le chargement du FXML.
     * On charge les langues ICI, après avoir l'user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        LoggerUtil.info("PerformanceAnalyzer: user set", "userId", String.valueOf(user.getId()));
        // Charger les langues maintenant qu'on a l'user
        loadLanguages();
    }

    private void loadLanguages() {
        try {
            LangueService langueService = new LangueService();
            List<Langue> langues = langueService.recuperer();

            langueComboBox.getItems().clear();
            langueComboBox.getItems().addAll(langues);

            LoggerUtil.info("Languages loaded", "count", String.valueOf(langues.size()));

            // Sélectionner la première langue → déclenche le listener → charge les données
            if (!langues.isEmpty()) {
                langueComboBox.getSelectionModel().selectFirst();
            }

        } catch (SQLException e) {
            LoggerUtil.error("Error loading languages", e);
            showError("Erreur lors du chargement des langues");
        }
    }

    private void loadPerformanceData(Langue langue) {
        if (currentUser == null) {
            LoggerUtil.warning("loadPerformanceData: currentUser is null!");
            return;
        }

        loadingIndicator.setVisible(true);
        LoggerUtil.info("Loading performance data",
                "userId", String.valueOf(currentUser.getId()),
                "langue", langue.getNom());

        new Thread(() -> {
            try {
                Map<String, Object> analysis =
                        performanceService.analyzeUserPerformance(currentUser, langue);

                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    updateUI(analysis, langue);
                    generateAIRecommendations(langue, analysis);
                });

            } catch (SQLException e) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    LoggerUtil.error("Error loading performance data", e);
                    showError("Erreur lors du chargement des performances : " + e.getMessage());
                });
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void updateUI(Map<String, Object> analysis, Langue langue) {
        boolean hasData = (boolean) analysis.getOrDefault("has_data", false);

        if (!hasData) {
            testsPassesLabel.setText("0");
            scoreMoyenLabel.setText("0%");
            meilleurScoreLabel.setText("0%");
            dernierScoreLabel.setText("0%");
            progressionLabel.setText("Aucune donnée");
            competencesContainer.getChildren().clear();
            Label noData = new Label("Aucun test passé pour " + langue.getNom()
                    + ".\nPassez des tests pour voir vos statistiques ici !");
            noData.setStyle("-fx-font-size:14px;-fx-text-fill:#8A8FA8;-fx-text-alignment:center;");
            noData.setWrapText(true);
            competencesContainer.getChildren().add(noData);
            recommendationsArea.setText("Passez des tests pour recevoir des recommandations personnalisées.");
            return;
        }

        Map<String, Object> stats = (Map<String, Object>) analysis.get("stats_globales");

        testsPassesLabel.setText(String.valueOf(stats.get("tests_passes")));
        scoreMoyenLabel.setText(stats.get("score_moyen") + "%");
        meilleurScoreLabel.setText(stats.get("meilleur_score") + "%");
        dernierScoreLabel.setText(stats.get("dernier_score") + "%");

        String progression = (String) stats.getOrDefault("progression", "stable");
        switch (progression) {
            case "progression" -> {
                progressionLabel.setText("📈 En progression");
                progressionLabel.setStyle("-fx-text-fill:#10b981;-fx-font-weight:bold;");
            }
            case "regression" -> {
                progressionLabel.setText("📉 En régression");
                progressionLabel.setStyle("-fx-text-fill:#ef4444;-fx-font-weight:bold;");
            }
            default -> {
                progressionLabel.setText("➡️ Stable");
                progressionLabel.setStyle("-fx-text-fill:#6b7280;-fx-font-weight:bold;");
            }
        }

        updateCompetences((Map<String, Object>) analysis.get("competences"));
        updateProgressionChart((List<Map<String, Object>>) analysis.get("progression"));
    }

    @SuppressWarnings("unchecked")
    private void updateCompetences(Map<String, Object> competences) {
        competencesContainer.getChildren().clear();

        String[] keys   = {"grammaire", "vocabulaire", "comprehension", "oral"};
        String[] labels = {"📝 Grammaire", "📚 Vocabulaire", "📖 Compréhension", "🎤 Oral"};

        for (int i = 0; i < keys.length; i++) {
            Object raw = competences.get(keys[i]);
            if (!(raw instanceof Map)) continue;
            Map<String, Object> data = (Map<String, Object>) raw;

            double score  = ((Number) data.getOrDefault("score", 0)).doubleValue();
            String niveau = (String) data.getOrDefault("niveau", "Non évalué");

            VBox box = new VBox(5);
            box.setStyle("-fx-padding:10;-fx-background-color:#f3f4f6;-fx-background-radius:8;");

            Label titleLbl = new Label(labels[i]);
            titleLbl.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");

            ProgressBar pb = new ProgressBar(score / 100.0);
            pb.setPrefWidth(Double.MAX_VALUE);

            String color = score >= 70 ? "#10b981" : (score >= 50 ? "#f59e0b" : "#ef4444");
            pb.setStyle("-fx-accent:" + color + ";");

            Label scoreLbl = new Label(String.format("%.1f%% — %s", score, niveau));
            scoreLbl.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;");

            box.getChildren().addAll(titleLbl, pb, scoreLbl);
            competencesContainer.getChildren().add(box);
        }
    }

    private void updateProgressionChart(List<Map<String, Object>> progression) {
        progressionChart.getData().clear();
        if (progression == null || progression.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Score (%)");

        for (Map<String, Object> point : progression) {
            String date  = (String) point.getOrDefault("date", "");
            Number score = (Number) point.getOrDefault("score", 0);
            series.getData().add(new XYChart.Data<>(date, score.doubleValue()));
        }

        progressionChart.getData().add(series);
    }

    private void generateAIRecommendations(Langue langue, Map<String, Object> analysis) {
        boolean hasData = (boolean) analysis.getOrDefault("has_data", false);
        if (!hasData) {
            recommendationsArea.setText("Passez des tests pour recevoir des recommandations IA.");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> recommendations =
                        performanceService.generateAIRecommendations(currentUser, langue, analysis);

                Platform.runLater(() -> displayRecommendations(recommendations));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    recommendationsArea.setText(
                            "Recommandations temporairement indisponibles.\n" +
                                    "Continuez à pratiquer régulièrement !");
                    messageEncouragementLabel.setText("💪 Continuez vos efforts !");
                });
                LoggerUtil.error("Error generating AI recommendations", e);
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void displayRecommendations(Map<String, Object> recommendations) {
        Object recsObj = recommendations.get("recommandations");
        if (!(recsObj instanceof List)) {
            recommendationsArea.setText("Aucune recommandation disponible.");
            return;
        }

        List<Map<String, Object>> recs = (List<Map<String, Object>>) recsObj;
        StringBuilder sb = new StringBuilder();

        for (Map<String, Object> rec : recs) {
            sb.append("🎯 ").append(rec.getOrDefault("titre", "")).append("\n");
            sb.append("   ").append(rec.getOrDefault("description", "")).append("\n");

            Object actionsObj = rec.get("actions");
            if (actionsObj instanceof List) {
                for (Object action : (List<?>) actionsObj) {
                    sb.append("   • ").append(action).append("\n");
                }
            }
            sb.append("\n");
        }

        recommendationsArea.setText(sb.toString());

        String msg = (String) recommendations.getOrDefault(
                "message_encouragement", "💪 Continue comme ça !");
        messageEncouragementLabel.setText(msg);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}