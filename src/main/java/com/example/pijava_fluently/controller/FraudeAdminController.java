package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.FraudeTrackerService;
import com.example.pijava_fluently.services.UserService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.stream.Collectors;

public class FraudeAdminController {

    @FXML private VBox  vboxStats;
    @FXML private VBox  vboxListe;
    @FXML private Label labelTotalInfractions;
    @FXML private Label labelEtudiantsSurveilles;
    @FXML private Label labelRisqueHaut;
    @FXML private BarChart<String, Number> barChart;

    private final FraudeTrackerService tracker     = new FraudeTrackerService();
    private final UserService          userService = new UserService();

    @FXML
    public void initialize() {
        chargerStats();
        chargerListe();
    }

    private void chargerStats() {
        List<Integer> ids = tracker.getUsersAvecInfractions();

        int total = 0;
        int risqueHaut = 0;
        Map<String, Integer> parType = new LinkedHashMap<>();

        for (int uid : ids) {
            List<FraudeTrackerService.Infraction> hist = tracker.charger(uid);
            total += hist.size();
            if (hist.size() >= 6) risqueHaut++;
            for (FraudeTrackerService.Infraction inf : hist) {
                // Catégoriser le type d'infraction
                String cat = categoriser(inf.raison());
                parType.merge(cat, 1, Integer::sum);
            }
        }

        // Mettre à jour les cartes stats
        labelTotalInfractions.setText(String.valueOf(total));
        labelEtudiantsSurveilles.setText(String.valueOf(ids.size()));
        labelRisqueHaut.setText(String.valueOf(risqueHaut));

        // Graphique en barres par type
        if (barChart != null && !parType.isEmpty()) {
            barChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Infractions");
            parType.forEach((type, count) ->
                    series.getData().add(new XYChart.Data<>(type, count)));
            barChart.getData().add(series);
            barChart.setLegendVisible(false);
        }
    }

    private void chargerListe() {
        vboxListe.getChildren().clear();
        List<Integer> ids = tracker.getUsersAvecInfractions();

        if (ids.isEmpty()) {
            Label ok = new Label("✅ Aucune infraction détectée.");
            ok.setStyle("-fx-font-size:14px;-fx-text-fill:#059669;-fx-padding:20;");
            vboxListe.getChildren().add(ok);
            return;
        }

        // Trier par nombre d'infractions décroissant
        ids.sort((a, b) ->
                tracker.charger(b).size() - tracker.charger(a).size());

        for (int userId : ids) {
            FraudeTrackerService.ProfilFraude profil =
                    tracker.getProfilComplet(userId);
            vboxListe.getChildren().add(creerCarteEtudiant(userId, profil));
        }
    }

    private VBox creerCarteEtudiant(int userId,
                                    FraudeTrackerService.ProfilFraude profil) {
        int total = profil.totalInfractions();
        String niveauRisque = total >= 6 ? "HAUT"
                : total >= 3 ? "MOYEN" : "FAIBLE";
        String couleurRisque = total >= 6 ? "#DC2626"
                : total >= 3 ? "#D97706" : "#059669";
        String bgRisque = total >= 6 ? "#FEF2F2"
                : total >= 3 ? "#FFFBEB" : "#F0FDF4";
        String borderRisque = total >= 6 ? "#FECACA"
                : total >= 3 ? "#FDE68A" : "#BBF7D0";

        VBox carte = new VBox(0);
        carte.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:" + borderRisque + ";" +
                        "-fx-border-radius:14;-fx-border-width:1.5;");

        // ── En-tête étudiant ──────────────────────────────────────
        HBox entete = new HBox(12);
        entete.setAlignment(Pos.CENTER_LEFT);
        entete.setPadding(new Insets(16, 20, 16, 20));
        entete.setStyle("-fx-background-color:" + bgRisque + ";" +
                "-fx-background-radius:14 14 0 0;");

        // Avatar initiales
        StackPane avatar = new StackPane();
        avatar.setPrefSize(44, 44);
        avatar.setStyle("-fx-background-color:" + couleurRisque + ";" +
                "-fx-background-radius:22;");
        Label initiales = new Label("?");
        initiales.setStyle(
                "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");

        // Nom
        String nomAffiche = "Étudiant #" + userId;
        try {
            User u = userService.findById(userId);
            if (u != null) {
                nomAffiche = u.getPrenom() + " " + u.getNom();
                initiales.setText(
                        String.valueOf(u.getPrenom().charAt(0)).toUpperCase() +
                                String.valueOf(u.getNom().charAt(0)).toUpperCase());
            }
        } catch (Exception ignored) {}
        avatar.getChildren().add(initiales);

        VBox infoEtudiant = new VBox(3);
        HBox.setHgrow(infoEtudiant, Priority.ALWAYS);
        Label nomLabel = new Label(nomAffiche);
        nomLabel.setStyle(
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

        // Stats rapides
        HBox statsRapides = new HBox(12);
        statsRapides.setAlignment(Pos.CENTER_LEFT);

        int maxAuto = tracker.getMaxTentativesAutorisees(userId);
        statsRapides.getChildren().addAll(
                badgeInfo("⚠️ " + total + " infraction(s)", "#6B7280", "#F4F5FA"),
                badgeInfo("🎯 Max " + profil.maxInfractionsParTest() + "/test",
                        "#6B7280", "#F4F5FA"),
                badgeInfo("🔒 Seuil : " + maxAuto + " tentative(s)",
                        couleurRisque, bgRisque)
        );
        infoEtudiant.getChildren().addAll(nomLabel, statsRapides);

        // Badge risque
        Label badgeRisque = new Label(niveauRisque);
        badgeRisque.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + couleurRisque + ";" +
                        "-fx-background-color:" + bgRisque + ";" +
                        "-fx-background-radius:20;-fx-padding:5 12;" +
                        "-fx-border-color:" + borderRisque + ";" +
                        "-fx-border-radius:20;-fx-border-width:1;");

        entete.getChildren().addAll(avatar, infoEtudiant, badgeRisque);

        // ── Timeline des infractions ──────────────────────────────
        VBox timeline = new VBox(0);
        timeline.setStyle("-fx-padding:16 20 16 20;");

        // Répartition par type (mini-graphique textuel)
        Map<String, Long> parType = profil.historique().stream()
                .collect(Collectors.groupingBy(
                        i -> categoriser(i.raison()), Collectors.counting()));

        if (!parType.isEmpty()) {
            HBox repartition = new HBox(8);
            repartition.setAlignment(Pos.CENTER_LEFT);
            repartition.setPadding(new Insets(0, 0, 12, 0));

            Label repLbl = new Label("Répartition : ");
            repLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
            repartition.getChildren().add(repLbl);

            parType.forEach((type, count) -> {
                String[] info = getTypeInfo(type);
                repartition.getChildren().add(
                        badgeInfo(info[0] + " " + type + " (" + count + ")",
                                info[1], info[2]));
            });
            timeline.getChildren().add(repartition);
        }

        // Liste des 5 dernières infractions
        Label dernieres = new Label("📋 Dernières infractions :");
        dernieres.setStyle(
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#374151;" +
                        "-fx-padding:0 0 8 0;");
        timeline.getChildren().add(dernieres);

        List<FraudeTrackerService.Infraction> recent =
                profil.historique().subList(
                        Math.max(0, profil.historique().size() - 5),
                        profil.historique().size());
        // Inverser pour avoir le plus récent en premier
        Collections.reverse(new ArrayList<>(recent));

        for (FraudeTrackerService.Infraction inf : recent) {
            timeline.getChildren().add(creerLigneInfraction(inf));
        }

        if (profil.historique().size() > 5) {
            Label plusLabel = new Label(
                    "... et " + (profil.historique().size() - 5) +
                            " infraction(s) plus ancienne(s)");
            plusLabel.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-padding:6 0 0 0;");
            timeline.getChildren().add(plusLabel);
        }

        carte.getChildren().addAll(entete, timeline);
        return carte;
    }

    private HBox creerLigneInfraction(FraudeTrackerService.Infraction inf) {
        HBox ligne = new HBox(10);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(7, 10, 7, 10));
        ligne.setStyle(
                "-fx-background-color:#FAFBFF;-fx-background-radius:8;" +
                        "-fx-border-color:#F0F1F7;-fx-border-radius:8;" +
                        "-fx-border-width:1;");

        String[] info = getTypeInfo(categoriser(inf.raison()));
        Label icoLabel = new Label(info[0]);
        icoLabel.setStyle("-fx-font-size:14px;");

        VBox detail = new VBox(2);
        HBox.setHgrow(detail, Priority.ALWAYS);
        Label raisonLbl = new Label(inf.raison());
        raisonLbl.setStyle(
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");
        Label testDateLbl = new Label(
                inf.testTitre() + "  •  " + inf.date());
        testDateLbl.setStyle(
                "-fx-font-size:10px;-fx-text-fill:#9CA3AF;");
        detail.getChildren().addAll(raisonLbl, testDateLbl);

        ligne.getChildren().addAll(icoLabel, detail);
        return ligne;
    }

    private Label badgeInfo(String text, String textColor, String bg) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + textColor + ";" +
                        "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:20;-fx-padding:3 8;");
        return l;
    }

    private String categoriser(String raison) {
        if (raison == null) return "Autre";
        String r = raison.toLowerCase();
        if (r.contains("copier") || r.contains("ctrl"))
            return "Copier-coller";
        if (r.contains("fenêtre") || r.contains("focus") || r.contains("bureau"))
            return "Changement fenêtre";
        if (r.contains("plein écran") || r.contains("fullscreen"))
            return "Plein écran";
        if (r.contains("clic droit") || r.contains("contextuel"))
            return "Clic droit";
        if (r.contains("f12") || r.contains("devtools"))
            return "DevTools";
        return "Autre";
    }

    private String[] getTypeInfo(String cat) {
        return switch (cat) {
            case "Copier-coller"       -> new String[]{"📋", "#D97706", "#FFFBEB"};
            case "Changement fenêtre"  -> new String[]{"🪟", "#7C3AED", "#F5F3FF"};
            case "Plein écran"         -> new String[]{"📺", "#3B82F6", "#EFF6FF"};
            case "Clic droit"          -> new String[]{"🖱️", "#059669", "#F0FDF4"};
            case "DevTools"            -> new String[]{"🔧", "#DC2626", "#FEF2F2"};
            default                    -> new String[]{"⚠️", "#6B7280", "#F4F5FA"};
        };
    }
}