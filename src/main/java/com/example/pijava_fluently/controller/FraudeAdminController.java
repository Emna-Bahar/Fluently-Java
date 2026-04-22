package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.services.FraudeTrackerService;
import com.example.pijava_fluently.services.UserService;
import com.example.pijava_fluently.entites.User;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class FraudeAdminController {

    @FXML private VBox vboxInfractions;

    private final FraudeTrackerService tracker  = new FraudeTrackerService();
    private final UserService          userService = new UserService();

    @FXML
    public void initialize() {
        vboxInfractions.getChildren().clear();
        List<Integer> usersAvecFraudes = tracker.getUsersAvecInfractions();

        if (usersAvecFraudes.isEmpty()) {
            Label ok = new Label("✅ Aucune infraction détectée.");
            ok.setStyle("-fx-font-size:14px;-fx-text-fill:#059669;");
            vboxInfractions.getChildren().add(ok);
            return;
        }

        for (int userId : usersAvecFraudes) {
            FraudeTrackerService.ProfilFraude profil =
                    tracker.getProfilComplet(userId);

            // Carte par étudiant
            VBox carte = new VBox(10);
            carte.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                    "-fx-padding:18 22;" +
                    "-fx-border-color:" + (profil.totalInfractions() >= 6
                    ? "#FCA5A5" : profil.totalInfractions() >= 3
                    ? "#FDE68A" : "#E5E7EB") + ";" +
                    "-fx-border-radius:14;-fx-border-width:1.5;");

            // En-tête étudiant
            HBox entete = new HBox(12);
            entete.setAlignment(Pos.CENTER_LEFT);

            String niveau = profil.totalInfractions() >= 6 ? "🔴 HAUT RISQUE"
                    : profil.totalInfractions() >= 3 ? "🟡 SUSPECT"
                    : "🟢 FAIBLE";

            Label niveauLabel = new Label(niveau);
            niveauLabel.setStyle("-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-background-color:" + (profil.totalInfractions() >= 6
                    ? "#FEE2E2" : profil.totalInfractions() >= 3
                    ? "#FEF3C7" : "#F0FDF4") + ";" +
                    "-fx-text-fill:" + (profil.totalInfractions() >= 6
                    ? "#DC2626" : profil.totalInfractions() >= 3
                    ? "#D97706" : "#059669") + ";" +
                    "-fx-background-radius:20;-fx-padding:4 10;");

            Label nomLabel = new Label("Étudiant ID : " + userId);
            try {
                User u = userService.findById(userId);
                if (u != null)
                    nomLabel.setText(u.getPrenom() + " " + u.getNom());
            } catch (Exception ignored) {}
            nomLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;" +
                    "-fx-text-fill:#1A1D2E;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label statsLabel = new Label(
                    profil.totalInfractions() + " infraction(s)  •  " +
                            "Max par test : " + profil.maxInfractionsParTest() + "  •  " +
                            "Prochain seuil : " +
                            tracker.getMaxTentativesAutorisees(userId) + " tentative(s)");
            statsLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");

            entete.getChildren().addAll(nomLabel, spacer, niveauLabel);
            carte.getChildren().addAll(entete, statsLabel);

            // Détail des infractions
            TitledPane details = new TitledPane();
            details.setText("Voir le détail (" + profil.historique().size() + ")");
            details.setExpanded(false);
            details.setStyle("-fx-font-size:12px;");

            VBox listeInf = new VBox(6);
            listeInf.setStyle("-fx-padding:8 0 0 0;");
            for (FraudeTrackerService.Infraction inf : profil.historique()) {
                HBox ligne = new HBox(12);
                ligne.setAlignment(Pos.CENTER_LEFT);
                ligne.setStyle("-fx-background-color:#FFF7F7;" +
                        "-fx-background-radius:8;-fx-padding:8 12;");

                Label dateL = new Label(inf.date());
                dateL.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;" +
                        "-fx-min-width:130;");
                Label raisonL = new Label(inf.raison());
                raisonL.setStyle("-fx-font-size:12px;-fx-text-fill:#DC2626;");
                Label testL = new Label("— " + inf.testTitre());
                testL.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");

                ligne.getChildren().addAll(dateL, raisonL, testL);
                listeInf.getChildren().add(ligne);
            }
            details.setContent(listeInf);
            carte.getChildren().add(details);
            vboxInfractions.getChildren().add(carte);
        }
    }
}