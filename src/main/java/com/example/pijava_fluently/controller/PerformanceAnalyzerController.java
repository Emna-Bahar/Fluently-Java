package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.PerformanceAnalyzerService;
import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
    @FXML private VBox                      recommendationsContainer;
    @FXML private Label                     messageEncouragementLabel;
    @FXML private ProgressIndicator         loadingIndicator;

    private PerformanceAnalyzerService performanceService;
    private User                       currentUser;

    @FXML
    public void initialize() {
        performanceService = new PerformanceAnalyzerService();

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

        langueComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && currentUser != null) {
                loadPerformanceData(newVal);
            }
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        LoggerUtil.info("PerformanceAnalyzer: user set",
                "userId", String.valueOf(user.getId()));
        loadLanguages();
    }

    private void loadLanguages() {
        try {
            LangueService ls = new LangueService();
            List<Langue> langues = ls.recuperer();
            langueComboBox.getItems().clear();
            langueComboBox.getItems().addAll(langues);
            if (!langues.isEmpty())
                langueComboBox.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            LoggerUtil.error("Error loading languages", e);
        }
    }

    private void loadPerformanceData(Langue langue) {
        if (currentUser == null) return;
        loadingIndicator.setVisible(true);

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
                    LoggerUtil.error("Error loading performance", e);
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
            progressionLabel.setText("—");

            competencesContainer.getChildren().clear();
            Label noData = new Label(
                    "Aucun test passé pour " + langue.getNom() + ".\n" +
                            "Passez des tests pour voir vos statistiques.");
            noData.setWrapText(true);
            noData.setStyle("-fx-font-size:13px;-fx-text-fill:#8A8FA8;");
            competencesContainer.getChildren().add(noData);
            return;
        }

        Map<String, Object> stats =
                (Map<String, Object>) analysis.get("stats_globales");

        testsPassesLabel.setText(String.valueOf(stats.get("tests_passes")));
        scoreMoyenLabel.setText(stats.get("score_moyen") + "%");
        meilleurScoreLabel.setText(stats.get("meilleur_score") + "%");
        dernierScoreLabel.setText(stats.get("dernier_score") + "%");

        String trend = (String) stats.getOrDefault("progression", "stable");
        switch (trend) {
            case "progression" -> {
                progressionLabel.setText("📈 En hausse");
                progressionLabel.setStyle(
                        "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#059669;");
            }
            case "regression" -> {
                progressionLabel.setText("📉 En baisse");
                progressionLabel.setStyle(
                        "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#EF4444;");
            }
            default -> {
                progressionLabel.setText("➡️ Stable");
                progressionLabel.setStyle(
                        "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#6B7280;");
            }
        }

        updateCompetences((Map<String, Object>) analysis.get("competences"));
        updateChart((List<Map<String, Object>>) analysis.get("progression"));
    }

    @SuppressWarnings("unchecked")
    private void updateCompetences(Map<String, Object> competences) {
        competencesContainer.getChildren().clear();

        String[] keys   = {"grammaire", "vocabulaire", "comprehension", "oral"};
        String[] labels = {"📝 Grammaire", "📚 Vocabulaire",
                "📖 Compréhension", "🎤 Oral"};
        String[] colors = {"#6C63FF", "#059669", "#3B82F6", "#F59E0B"};

        for (int i = 0; i < keys.length; i++) {
            Object raw = competences.get(keys[i]);
            if (!(raw instanceof Map)) continue;
            Map<String, Object> d = (Map<String, Object>) raw;

            double score  = ((Number) d.getOrDefault("score", 0)).doubleValue();
            String niveau = (String) d.getOrDefault("niveau", "—");
            String color  = colors[i];

            // Barre de compétence
            VBox box = new VBox(6);
            box.setStyle(
                    "-fx-background-color:#F8F9FF;" +
                            "-fx-background-radius:12;-fx-padding:12 14;");

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);

            Label lbl = new Label(labels[i]);
            lbl.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
            HBox.setHgrow(lbl, Priority.ALWAYS);

            Label pct = new Label(String.format("%.0f%%", score));
            pct.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

            row.getChildren().addAll(lbl, pct);

            // ProgressBar
            ProgressBar pb = new ProgressBar(score / 100.0);
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.setPrefHeight(8);

            String barColor = score >= 70 ? "#10b981"
                    : score >= 50 ? "#F59E0B" : "#EF4444";
            pb.setStyle("-fx-accent:" + barColor + ";");

            Label niveauLbl = new Label(niveau);
            niveauLbl.setStyle(
                    "-fx-font-size:10px;-fx-text-fill:#9CA3AF;");

            box.getChildren().addAll(row, pb, niveauLbl);
            competencesContainer.getChildren().add(box);
        }
    }

    private void updateChart(List<Map<String, Object>> progression) {
        progressionChart.getData().clear();
        if (progression == null || progression.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Score");

        for (Map<String, Object> pt : progression) {
            String date  = (String) pt.getOrDefault("date", "");
            Number score = (Number) pt.getOrDefault("score", 0);
            series.getData().add(
                    new XYChart.Data<>(date, score.doubleValue()));
        }
        progressionChart.getData().add(series);
    }

    private void generateAIRecommendations(Langue langue,
                                           Map<String, Object> analysis) {
        boolean hasData = (boolean) analysis.getOrDefault("has_data", false);
        if (!hasData) {
            showNoDataRecommendations();
            return;
        }

        // Spinner de chargement
        recommendationsContainer.getChildren().clear();
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);
        Label loading = new Label("Génération des recommandations IA...");
        loading.setStyle("-fx-font-size:13px;-fx-text-fill:#8A8FA8;");
        HBox loadBox = new HBox(12, pi, loading);
        loadBox.setAlignment(Pos.CENTER_LEFT);
        recommendationsContainer.getChildren().add(loadBox);

        new Thread(() -> {
            try {
                Map<String, Object> recs =
                        performanceService.generateAIRecommendations(
                                currentUser, langue, analysis);
                Platform.runLater(() -> displayRecommendations(recs));
            } catch (Exception e) {
                Platform.runLater(this::showNoDataRecommendations);
                LoggerUtil.error("Error generating AI recommendations", e);
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void displayRecommendations(Map<String, Object> recs) {
        recommendationsContainer.getChildren().clear();

        String msg = (String) recs.getOrDefault(
                "message_encouragement", "💪 Continue comme ça !");
        messageEncouragementLabel.setText(msg);

        Object listObj = recs.get("recommandations");
        if (!(listObj instanceof List)) {
            showNoDataRecommendations();
            return;
        }

        List<Map<String, Object>> list = (List<Map<String, Object>>) listObj;

        String[] cardColors = {"#EEF2FF", "#F0FDF4", "#FFFBEB", "#EFF6FF"};
        String[] borderColors = {"#C7D2FE", "#BBF7D0", "#FDE68A", "#BFDBFE"};
        String[] iconColors   = {"#6C63FF", "#059669", "#D97706", "#3B82F6"};

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> rec = list.get(i);
            String titre       = (String) rec.getOrDefault("titre", "");
            String description = (String) rec.getOrDefault("description", "");
            String priorite    = (String) rec.getOrDefault("priorite", "moyenne");
            Object actionsObj  = rec.get("actions");

            int ci = i % cardColors.length;

            VBox card = new VBox(8);
            card.setStyle(
                    "-fx-background-color:" + cardColors[ci] + ";" +
                            "-fx-background-radius:14;-fx-padding:16 18;" +
                            "-fx-border-color:" + borderColors[ci] + ";" +
                            "-fx-border-radius:14;-fx-border-width:1.5;");

            // Titre + badge priorité
            HBox titleRow = new HBox(10);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label titreLbl = new Label("🎯 " + titre);
            titreLbl.setStyle(
                    "-fx-font-size:14px;-fx-font-weight:bold;" +
                            "-fx-text-fill:" + iconColors[ci] + ";");
            HBox.setHgrow(titreLbl, Priority.ALWAYS);

            String badgeBg = switch(priorite) {
                case "haute"   -> "#FEE2E2";
                case "basse"   -> "#F0FDF4";
                default        -> "#F3F4F6";
            };
            String badgeFg = switch(priorite) {
                case "haute"   -> "#DC2626";
                case "basse"   -> "#16A34A";
                default        -> "#6B7280";
            };
            Label badgeLbl = new Label(priorite.toUpperCase());
            badgeLbl.setStyle(
                    "-fx-background-color:" + badgeBg + ";" +
                            "-fx-text-fill:" + badgeFg + ";" +
                            "-fx-font-size:9px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:3 8;");

            titleRow.getChildren().addAll(titreLbl, badgeLbl);

            // Description
            Label descLbl = new Label(description);
            descLbl.setWrapText(true);
            descLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#4B5563;");

            card.getChildren().addAll(titleRow, descLbl);

            // Actions
            if (actionsObj instanceof List) {
                VBox actionsBox = new VBox(4);
                actionsBox.setStyle("-fx-padding:4 0 0 0;");
                for (Object action : (List<?>) actionsObj) {
                    HBox actionRow = new HBox(8);
                    actionRow.setAlignment(Pos.CENTER_LEFT);
                    Label dot = new Label("•");
                    dot.setStyle("-fx-text-fill:" + iconColors[ci]
                            + ";-fx-font-weight:bold;");
                    Label actionLbl = new Label(action.toString());
                    actionLbl.setWrapText(true);
                    actionLbl.setStyle(
                            "-fx-font-size:12px;-fx-text-fill:#374151;");
                    actionRow.getChildren().addAll(dot, actionLbl);
                    actionsBox.getChildren().add(actionRow);
                }
                card.getChildren().add(actionsBox);
            }

            recommendationsContainer.getChildren().add(card);
        }
    }

    private void showNoDataRecommendations() {
        recommendationsContainer.getChildren().clear();
        Label msg = new Label(
                "💡 Passez des tests pour recevoir des recommandations personnalisées par l'IA.");
        msg.setWrapText(true);
        msg.setStyle(
                "-fx-font-size:13px;-fx-text-fill:#8A8FA8;" +
                        "-fx-background-color:#F8F9FF;-fx-background-radius:12;" +
                        "-fx-padding:16;");
        recommendationsContainer.getChildren().add(msg);
    }
}