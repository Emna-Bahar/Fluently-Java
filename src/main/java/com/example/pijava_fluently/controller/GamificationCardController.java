package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.GamificationService;
import com.example.pijava_fluently.services.GamificationService.Badge;
import com.example.pijava_fluently.services.GamificationService.GamificationResult;
import com.example.pijava_fluently.services.ObjectifService;
import com.example.pijava_fluently.services.TacheService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class GamificationCardController {

    // ── FXML injections ──────────────────────────────────────────────────────
    @FXML private VBox     niveauCard;
    @FXML private Label    niveauIcone;
    @FXML private Label    niveauLabel;
    @FXML private ProgressBar niveauProgressBar;
    @FXML private Label    niveauProgressLabel;

    @FXML private Label    totalPointsLabel;
    @FXML private Label    pointsTachesLabel;
    @FXML private Label    nbTachesLabel;
    @FXML private Label    pointsObjectifsLabel;
    @FXML private Label    nbObjectifsLabel;

    @FXML private FlowPane badgesRow;
    @FXML private Label    badgesCountLabel;

    // ── Services ─────────────────────────────────────────────────────────────
    private final GamificationService gamifService   = new GamificationService();
    private final ObjectifService     objectifService = new ObjectifService();
    private final TacheService        tacheService    = new TacheService();

    // ── État ─────────────────────────────────────────────────────────────────
    private User currentUser;
    private GamificationResult lastResult;

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Les données seront chargées via setCurrentUser()
    }

    /**
     * Appelé par ObjectifController (et TacheController) pour injecter l'utilisateur
     * et déclencher le calcul.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            refresh();
        }
    }

    /**
     * Recalcule et rafraîchit l'affichage.
     * À appeler après chaque ajout/modification/suppression de tâche ou objectif.
     */
    public void refresh() {
        if (currentUser == null) return;

        // Calcul en arrière-plan pour ne pas bloquer l'UI
        Thread thread = new Thread(() -> {
            GamificationResult result = gamifService.calculer(
                    currentUser.getId(), objectifService, tacheService);
            Platform.runLater(() -> applyResult(result));
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    private void applyResult(GamificationResult r) {
        this.lastResult = r;

        // ── Carte Niveau ────────────────────────────────────────────────────
        niveauIcone.setText(r.niveau().icone());
        niveauLabel.setText(r.niveau().label());

        // Couleur de fond de la carte niveau
        niveauCard.setStyle(
                "-fx-background-color:" + r.niveau().couleur() + ";" +
                        "-fx-background-radius:18;" +
                        "-fx-padding:22 18 18 18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),18,0,0,6);"
        );

        double prog = r.progression() / 100.0;
        niveauProgressBar.setProgress(prog);
        niveauProgressLabel.setText(r.totalPoints() + " / " + r.niveau().prochaineEtape() + " pts");

        // ── Carte Points ────────────────────────────────────────────────────
        totalPointsLabel.setText(String.valueOf(r.totalPoints()));
        pointsTachesLabel.setText("+" + r.pointsTaches() + " pts");
        nbTachesLabel.setText(r.nbTachesTerminees() + " tâche(s)");
        pointsObjectifsLabel.setText("+" + r.pointsObjectifs() + " pts");
        nbObjectifsLabel.setText(r.nbObjectifsCompletes() + " obj.");

        // ── Carte Badges ────────────────────────────────────────────────────
        badgesRow.getChildren().clear();

        for (Badge badge : r.badgesObtenus()) {
            Label icon = new Label(badge.icone());
            icon.setStyle(
                    "-fx-font-size:22px;" +
                            "-fx-background-color:" + badge.couleur() + "22;" +
                            "-fx-background-radius:50;" +
                            "-fx-padding:6 8 6 8;" +
                            "-fx-cursor:hand;"
            );
            // Tooltip avec nom + description
            Tooltip tip = new Tooltip(badge.nom() + "\n" + badge.description());
            tip.setStyle("-fx-font-size:12px;");
            Tooltip.install(icon, tip);
            badgesRow.getChildren().add(icon);
        }

        badgesCountLabel.setText(r.badgesObtenus().size() + " badge(s) débloqué(s)");
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    private void handleVoirBadges() {
        if (lastResult == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🏆 Tous les badges");
        dialog.setHeaderText(null);

        VBox content = new VBox(0);
        content.setPrefWidth(480);

        // En-tête dégradé
        VBox header = new VBox(6);
        header.setPadding(new Insets(24, 28, 20, 28));
        header.setStyle(
                "-fx-background-color:linear-gradient(to right,#6C63FF,#8B5CF6);" +
                        "-fx-background-radius:12 12 0 0;"
        );
        Label title = new Label("🏅  Tous les badges");
        title.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label subtitle = new Label(lastResult.badgesObtenus().size() + " / " +
                lastResult.tousBadges().size() + " débloqué(s)");
        subtitle.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.80);");
        header.getChildren().addAll(title, subtitle);

        // Corps : grille de badges
        FlowPane grid = new FlowPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 24, 24));
        grid.setPrefWrapLength(440);

        for (Badge badge : lastResult.tousBadges()) {
            VBox card = new VBox(6);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(130);
            card.setPadding(new Insets(14, 10, 14, 10));

            String bg, textColor;
            if (badge.obtenu()) {
                bg = badge.couleur() + "22";
                textColor = badge.couleur();
            } else {
                bg = "#F1F5F9";
                textColor = "#CBD5E1";
            }
            card.setStyle(
                    "-fx-background-color:" + bg + ";" +
                            "-fx-background-radius:14;" +
                            "-fx-border-color:" + (badge.obtenu() ? badge.couleur() : "#E2E8F0") + ";" +
                            "-fx-border-radius:14;-fx-border-width:1.5;"
            );

            String iconeText = badge.obtenu() ? badge.icone() : "🔒";
            Label icone = new Label(iconeText);
            icone.setStyle("-fx-font-size:28px;" + (badge.obtenu() ? "" : "-fx-opacity:0.5;"));

            Label nom = new Label(badge.nom());
            nom.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + textColor + ";-fx-wrap-text:true;");
            nom.setWrapText(true);
            nom.setAlignment(Pos.CENTER);

            Label desc = new Label(badge.description());
            desc.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;-fx-wrap-text:true;");
            desc.setWrapText(true);
            desc.setAlignment(Pos.CENTER);

            if (badge.obtenu()) {
                Label obtenu = new Label("✓ Obtenu");
                obtenu.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + badge.couleur() + ";");
                card.getChildren().addAll(icone, nom, desc, obtenu);
            } else {
                card.getChildren().addAll(icone, nom, desc);
            }
            grid.getChildren().add(card);
        }

        content.getChildren().addAll(header, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("Fermer");
        closeBtn.setStyle(
                "-fx-background-color:linear-gradient(to right,#6C63FF,#8B5CF6);" +
                        "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;"
        );

        dialog.showAndWait();
    }
}