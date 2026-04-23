package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.AITaskGeneratorService;
import com.example.pijava_fluently.services.AITaskGeneratorService.AIGeneratedTask;
import com.example.pijava_fluently.services.AITaskGeneratorService.UserLearningProfile;
import com.example.pijava_fluently.services.TacheService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur du dialog de génération de tâches par IA.
 * Associé à : AITaskGenerator-dialog.fxml
 */
public class AITaskGeneratorController {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label     lblObjectifTitre;
    @FXML private Label     lblNiveau;
    @FXML private Label     lblStats;

    @FXML private Label     statTotal;
    @FXML private Label     statCompletion;
    @FXML private Label     statTerminees;
    @FXML private Label     statBloquees;

    @FXML private VBox      panelReady;         // état initial
    @FXML private VBox      panelLoading;       // pendant la génération
    @FXML private VBox      panelResults;       // résultats affichés
    @FXML private VBox      panelError;         // erreur

    @FXML private Label     lblLoadingMsg;
    @FXML private Label     lblErrorMsg;
    @FXML private VBox      cardsContainer;     // contiendra les cartes de tâches

    @FXML private Button    btnGenerer;
    @FXML private Button    btnRegenerer;
    @FXML private Button    btnRetour;
    @FXML private Button    btnFermer;

    // ── Dépendances ───────────────────────────────────────────────────────────
    private final AITaskGeneratorService aiService  = new AITaskGeneratorService();
    private final TacheService           tacheService = new TacheService();

    private Objectif          currentObjectif;
    private User              currentUser;
    private TacheController   tacheController;   // pour rafraîchir le Kanban après ajout

    // Statistiques calculées
    private int    nbTachesTotal     = 0;
    private int    nbTerminees       = 0;
    private int    nbBloquees        = 0;
    private double tauxCompletion    = 0;
    private double tauxEchec         = 0;

    // Tâches générées en cache
    private List<AIGeneratedTask> generatedTasks = new ArrayList<>();

    // ════════════════════════════════════════════════════════════════════════
    //  SETTERS
    // ════════════════════════════════════════════════════════════════════════

    public void setObjectif(Objectif o) {
        this.currentObjectif = o;
        lblObjectifTitre.setText("📋  " + o.getTitre());
        calculateStats();
        refreshStatsDisplay();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setTacheController(TacheController tc) {
        this.tacheController = tc;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        showPanel("ready");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CALCUL DES STATISTIQUES
    // ════════════════════════════════════════════════════════════════════════

    private void calculateStats() {
        if (currentObjectif == null) return;
        try {
            List<Tache> taches = tacheService.recupererParObjectif(currentObjectif.getId());
            nbTachesTotal = taches.size();
            nbTerminees   = (int) taches.stream().filter(t -> "Terminée".equals(t.getStatut())).count();
            nbBloquees    = (int) taches.stream().filter(t -> "Annulée".equals(t.getStatut())).count();
            int nbEchecs  = (int) taches.stream().filter(t -> "Annulée".equals(t.getStatut())).count();

            tauxCompletion = nbTachesTotal > 0 ? (double) nbTerminees / nbTachesTotal * 100 : 0;
            tauxEchec      = nbTachesTotal > 0 ? (double) nbEchecs    / nbTachesTotal * 100 : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshStatsDisplay() {
        if (statTotal      != null) statTotal.setText(String.valueOf(nbTachesTotal));
        if (statCompletion != null) statCompletion.setText(String.format("%.0f%%", tauxCompletion));
        if (statTerminees  != null) statTerminees.setText(nbTerminees + "/" + nbTachesTotal);
        if (statBloquees   != null) statBloquees.setText(String.valueOf(nbBloquees));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GÉNÉRATION IA
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleGenerer() {
        startGeneration();
    }

    @FXML
    private void handleRegenerer() {
        startGeneration();
    }

    private void startGeneration() {
        showPanel("loading");
        generatedTasks.clear();

        UserLearningProfile profile = buildProfile();

        // Textes de chargement animés
        String[] loadingMessages = {
                "🤖 L'IA analyse votre profil...",
                "📊 Calcul de votre pattern d'apprentissage...",
                "🎯 Personnalisation des tâches...",
                "✨ Finalisation des recommandations..."
        };
        final int[] idx = {0};
        Timeline loadingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1.2), e -> {
                    if (lblLoadingMsg != null && idx[0] < loadingMessages.length) {
                        lblLoadingMsg.setText(loadingMessages[idx[0]++]);
                    }
                })
        );
        loadingTimeline.setCycleCount(loadingMessages.length);
        loadingTimeline.play();

        // Appel IA en thread séparé
        new Thread(() -> {
            try {
                List<AIGeneratedTask> tasks = aiService.generateTasks(currentObjectif, profile);
                Platform.runLater(() -> {
                    loadingTimeline.stop();
                    generatedTasks = tasks;
                    renderResults(tasks);
                    showPanel("results");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingTimeline.stop();
                    if (lblErrorMsg != null)
                        lblErrorMsg.setText("Erreur : " + e.getMessage() + "\n\nVérifiez votre clé API Groq.");
                    showPanel("error");
                });
            }
        }).start();
    }

    private UserLearningProfile buildProfile() {
        // Détecter le niveau et les intérêts depuis le titre/description de l'objectif
        String niveau   = detectNiveau();
        List<String> interests = detectInterests();
        String pattern  = detectPattern();

        return new UserLearningProfile(
                niveau,
                interests,
                tauxCompletion,
                tauxEchec,
                pattern,
                nbTachesTotal,
                nbTerminees,
                nbBloquees
        );
    }

    private String detectNiveau() {
        if (currentObjectif == null) return "B1";
        String titre = (currentObjectif.getTitre() + " " +
                (currentObjectif.getDescription() != null ? currentObjectif.getDescription() : ""))
                .toLowerCase();
        if (titre.contains("débutant") || titre.contains("a1") || titre.contains("a2")) return "A2";
        if (titre.contains("intermédiaire") || titre.contains("b1"))                    return "B1";
        if (titre.contains("avancé") || titre.contains("b2"))                           return "B2";
        if (titre.contains("expert") || titre.contains("c1") || titre.contains("c2"))  return "C1";
        // Déduire du taux de complétion
        if (tauxCompletion < 30) return "A2";
        if (tauxCompletion < 60) return "B1";
        return "B2";
    }

    private List<String> detectInterests() {
        List<String> interests = new ArrayList<>();
        if (currentObjectif == null) { interests.add("général"); return interests; }
        String text = (currentObjectif.getTitre() + " " +
                (currentObjectif.getDescription() != null ? currentObjectif.getDescription() : ""))
                .toLowerCase();

        if (text.contains("musique") || text.contains("chanson") || text.contains("music"))
            interests.add("musique");
        if (text.contains("lecture") || text.contains("livre") || text.contains("lire"))
            interests.add("lecture");
        if (text.contains("conversation") || text.contains("parler") || text.contains("oral"))
            interests.add("conversation");
        if (text.contains("conjugaison") || text.contains("grammaire"))
            interests.add("grammaire");
        if (text.contains("écriture") || text.contains("rédaction") || text.contains("écrit"))
            interests.add("écriture");
        if (text.contains("cinéma") || text.contains("film") || text.contains("vidéo"))
            interests.add("cinéma");
        if (text.contains("vocabulaire") || text.contains("mots"))
            interests.add("vocabulaire");

        if (interests.isEmpty()) interests.add("apprentissage général");
        return interests;
    }

    private String detectPattern() {
        if (tauxCompletion > 70) return "exercices structurés";
        if (tauxEchec > 40)     return "apprentissage progressif";
        List<String> interests = detectInterests();
        if (interests.contains("musique"))  return "écoute active";
        if (interests.contains("lecture"))  return "lecture analytique";
        return "pratique régulière";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDU DES RÉSULTATS
    // ════════════════════════════════════════════════════════════════════════

    private void renderResults(List<AIGeneratedTask> tasks) {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();

        String[] accentColors = {"#4F46E5", "#0891B2", "#059669"};
        String[] diffLabels   = {"🟢 Facile", "🟡 Moyen", "🔴 Défi"};

        for (int i = 0; i < tasks.size(); i++) {
            AIGeneratedTask t = tasks.get(i);
            VBox card = buildTaskCard(t, accentColors[i % accentColors.length],
                    diffLabels[i % diffLabels.length], i);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox buildTaskCard(AIGeneratedTask task, String accentColor,
                               String diffLabel, int index) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + accentColor + "22;" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);"
        );

        // ── Bande colorée gauche ──
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Region accent = new Region();
        accent.setPrefWidth(4);
        accent.setPrefHeight(40);
        accent.setStyle("-fx-background-color:" + accentColor + ";-fx-background-radius:4;");

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Badges
        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                makeBadge(diffLabel, "#F0F4FF", "#4F46E5"),
                makeBadge("⏱ " + task.dureeJours + "j", "#F0FDF4", "#059669"),
                makeBadge("🚩 " + task.priorite, getPrioColorBg(task.priorite), getPrioColorFg(task.priorite))
        );

        Label titreLabel = new Label(task.titre);
        titreLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1E293B;-fx-wrap-text:true;");
        titreLabel.setWrapText(true);

        titleBox.getChildren().addAll(badges, titreLabel);
        topRow.getChildren().addAll(accent, titleBox);
        card.getChildren().add(topRow);

        // ── Description ──
        Label descLabel = new Label(task.description);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;-fx-wrap-text:true;");
        descLabel.setWrapText(true);
        card.getChildren().add(descLabel);

        // ── Pourquoi ──
        if (task.pourquoi != null && !task.pourquoi.isBlank()) {
            VBox whyBox = new VBox(4);
            whyBox.setPadding(new Insets(10, 12, 10, 12));
            whyBox.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:8;");
            Label whyTitle = new Label("💡 Pourquoi cette tâche ?");
            whyTitle.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#374151;");
            Label whyText = new Label(task.pourquoi);
            whyText.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;-fx-wrap-text:true;");
            whyText.setWrapText(true);
            whyBox.getChildren().addAll(whyTitle, whyText);
            card.getChildren().add(whyBox);
        }

        // ── Ressource média ──
        if (task.mediaType != null && task.mediaTitre != null) {
            HBox mediaBox = new HBox(12);
            mediaBox.setPadding(new Insets(10, 12, 10, 12));
            mediaBox.setAlignment(Pos.CENTER_LEFT);
            mediaBox.setStyle("-fx-background-color:#EEF2FF;-fx-background-radius:8;");

            Label mediaIcon = new Label(getMediaIcon(task.mediaType));
            mediaIcon.setStyle("-fx-font-size:22px;");

            VBox mediaInfo = new VBox(3);
            HBox.setHgrow(mediaInfo, Priority.ALWAYS);
            Label mediaTitle = new Label(task.mediaTitre);
            mediaTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#3730A3;-fx-wrap-text:true;");
            mediaTitle.setWrapText(true);
            if (task.mediaAuteur != null) {
                Label mediaAuteur = new Label(task.mediaAuteur);
                mediaAuteur.setStyle("-fx-font-size:11px;-fx-text-fill:#6366F1;");
                mediaInfo.getChildren().addAll(mediaTitle, mediaAuteur);
            } else {
                mediaInfo.getChildren().add(mediaTitle);
            }
            mediaBox.getChildren().addAll(mediaIcon, mediaInfo);
            card.getChildren().add(mediaBox);
        }

        // ── Sous-tâches ──
        if (task.sousTaches != null && !task.sousTaches.isEmpty()) {
            VBox stBox = new VBox(5);
            Label stTitle = new Label("📝 Sous-tâches :");
            stTitle.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#475569;");
            stBox.getChildren().add(stTitle);
            for (String st : task.sousTaches) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                Label bullet = new Label("◉");
                bullet.setStyle("-fx-font-size:9px;-fx-text-fill:" + accentColor + ";");
                Label stLabel = new Label(st);
                stLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;-fx-wrap-text:true;");
                stLabel.setWrapText(true);
                HBox.setHgrow(stLabel, Priority.ALWAYS);
                row.getChildren().addAll(bullet, stLabel);
                stBox.getChildren().add(row);
            }
            card.getChildren().add(stBox);
        }

        // ── Bouton Ajouter ──
        Button btnAdd = new Button("✚  Ajouter à l'objectif");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setStyle(
                "-fx-background-color:" + accentColor + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:8;" +
                        "-fx-padding:9 0 9 0;" +
                        "-fx-cursor:hand;"
        );
        final int taskIdx = index;
        btnAdd.setOnAction(e -> handleAddTask(task, btnAdd, accentColor));
        card.getChildren().add(btnAdd);

        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  AJOUT D'UNE TÂCHE À L'OBJECTIF
    // ════════════════════════════════════════════════════════════════════════

    private void handleAddTask(AIGeneratedTask aiTask, Button btnAdd, String color) {
        if (currentObjectif == null) return;
        try {
            Tache tache = aiTask.toTache(currentObjectif.getId());
            tacheService.ajouter(tache);

            btnAdd.setText("✅  Ajoutée à l'objectif !");
            btnAdd.setStyle(
                    "-fx-background-color:#10B981;" +
                            "-fx-text-fill:white;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-background-radius:8;" +
                            "-fx-padding:9 0 9 0;" +
                            "-fx-cursor:default;"
            );
            btnAdd.setDisable(true);

            // Rafraîchir le Kanban si le contrôleur est disponible
            if (tacheController != null) {
                Platform.runLater(() -> {
                    try {
                        tacheController.refreshFromDialog();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            showSuccessToast("✅ Tâche \"" + aiTask.titre + "\" ajoutée avec succès !");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible d'ajouter la tâche : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NAVIGATION ENTRE PANNEAUX
    // ════════════════════════════════════════════════════════════════════════

    private void showPanel(String panel) {
        setVisible(panelReady,   "ready".equals(panel));
        setVisible(panelLoading, "loading".equals(panel));
        setVisible(panelResults, "results".equals(panel));
        setVisible(panelError,   "error".equals(panel));
    }

    private void setVisible(VBox panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
    }

    @FXML
    private void handleRetour() {
        showPanel("ready");
        generatedTasks.clear();
        if (cardsContainer != null) cardsContainer.getChildren().clear();
    }

    @FXML
    private void handleFermer() {
        // Ferme le dialog — géré par DialogPane en ButtonType.CLOSE
        if (btnFermer != null && btnFermer.getScene() != null) {
            btnFermer.getScene().getWindow().hide();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS VISUELS
    // ════════════════════════════════════════════════════════════════════════

    private Label makeBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;" +
                        "-fx-padding:3 9 3 9;"
        );
        return l;
    }

    private String getPrioColorBg(String priorite) {
        return switch (priorite != null ? priorite : "Normale") {
            case "Urgente" -> "#FEE2E2";
            case "Haute"   -> "#FEF3C7";
            case "Basse"   -> "#DCFCE7";
            default        -> "#DBEAFE";
        };
    }

    private String getPrioColorFg(String priorite) {
        return switch (priorite != null ? priorite : "Normale") {
            case "Urgente" -> "#991B1B";
            case "Haute"   -> "#92400E";
            case "Basse"   -> "#166534";
            default        -> "#1E40AF";
        };
    }

    private String getMediaIcon(String mediaType) {
        return switch (mediaType != null ? mediaType.toLowerCase() : "") {
            case "chanson", "musique" -> "🎵";
            case "livre", "lecture"   -> "📚";
            case "video", "film"      -> "🎬";
            default                   -> "📎";
        };
    }

    private void showSuccessToast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle("Succès"); a.setHeaderText(null);
        a.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null);
        a.showAndWait();
    }
}