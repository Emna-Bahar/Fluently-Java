package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.entites.Reponse;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.services.QuestionService;
import com.example.pijava_fluently.services.ReponseService;
import com.example.pijava_fluently.services.TestPassageService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestPassageEtudiantController {

    // ── En-tête ──────────────────────────────────────────
    @FXML private Label labelTitreTest;
    @FXML private Label labelTimer;
    @FXML private Label labelProgression;
    @FXML private ProgressBar progressBar;

    // ── Zone question ─────────────────────────────────────
    @FXML private Label labelNumeroQuestion;
    @FXML private Label labelEnonce;
    @FXML private VBox  vboxReponses;

    // ── Navigation ────────────────────────────────────────
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;
    @FXML private Button btnTerminer;

    // ── Écran résultats ───────────────────────────────────
    @FXML private VBox  panelQuestion;
    @FXML private VBox  panelResultat;
    @FXML private Label labelScoreFinal;
    @FXML private Label labelNiveauEstime;
    @FXML private Label labelTempsTotal;
    @FXML private Label labelResultatTitre;
    @FXML private VBox  vboxDetailResultats;

    // ── State ─────────────────────────────────────────────
    private Test               test;
    private List<Question>     questions        = new ArrayList<>();
    private List<List<Reponse>>reponsesParQuestion = new ArrayList<>();
    private List<Reponse>      reponsesChoisies = new ArrayList<>(); // null = pas répondu
    private int                indexCourant     = 0;
    private int                secondesEcoulees = 0;
    private Timeline           timer;
    private LocalDateTime      dateDebut;
    private int                userId           = 7; // à remplacer par l'utilisateur connecté

    private final QuestionService    questionService    = new QuestionService();
    private final ReponseService     reponseService     = new ReponseService();
    private final TestPassageService testPassageService = new TestPassageService();

    // ── Initialisation avec le test ───────────────────────
    public void initTest(Test test, int userId) {
        this.test   = test;
        this.userId = userId;
        labelTitreTest.setText(test.getTitre());
        dateDebut = LocalDateTime.now();
        chargerQuestions();
        demarrerTimer();
        afficherQuestion(0);
    }

    private void chargerQuestions() {
        try {
            questions = questionService.recupererParTest(test.getId());
            // Filtrer : uniquement les QCM
            questions.removeIf(q -> !q.getType().equals("qcm"));

            // Initialiser les réponses choisies à null
            for (Question q : questions) {
                reponsesChoisies.add(null);
                List<Reponse> reps = reponseService.recupererParQuestion(q.getId());
                reponsesParQuestion.add(reps);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les questions : " + e.getMessage());
        }
    }

    private void demarrerTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondesEcoulees++;
            int min = secondesEcoulees / 60;
            int sec = secondesEcoulees % 60;
            labelTimer.setText(String.format("⏱ %02d:%02d", min, sec));

            // Timer limite (dureeEstimee en minutes)
            int limiteSecondes = test.getDureeEstimee() * 60;
            if (limiteSecondes > 0 && secondesEcoulees >= limiteSecondes) {
                timer.stop();
                terminerTest(true);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void afficherQuestion(int index) {
        if (questions.isEmpty()) {
            showAlert("Info", "Ce test ne contient pas de questions QCM.");
            return;
        }

        indexCourant = index;
        Question q   = questions.get(index);

        // Progression
        labelNumeroQuestion.setText("Question " + (index + 1) + " / " + questions.size());
        labelProgression.setText((index + 1) + " / " + questions.size());
        progressBar.setProgress((double)(index + 1) / questions.size());

        // Énoncé
        labelEnonce.setText(q.getEnonce());

        // Réponses
        vboxReponses.getChildren().clear();
        List<Reponse> reponses = reponsesParQuestion.get(index);
        Reponse reponseCourante = reponsesChoisies.get(index);
        ToggleGroup group = new ToggleGroup();

        for (Reponse r : reponses) {
            RadioButton rb = new RadioButton(r.getContenuRep());
            rb.setToggleGroup(group);
            rb.setUserData(r);
            rb.setWrapText(true);

            // Style de base
            rb.setStyle(
                    "-fx-font-size:14px;" +
                            "-fx-text-fill:#1A1D2E;" +
                            "-fx-padding:14 18;" +
                            "-fx-cursor:hand;"
            );

            // Restaurer la sélection précédente
            if (reponseCourante != null && reponseCourante.getId() == r.getId()) {
                rb.setSelected(true);
                rb.setStyle(
                        "-fx-font-size:14px;" +
                                "-fx-text-fill:#6C63FF;" +
                                "-fx-font-weight:bold;" +
                                "-fx-padding:14 18;" +
                                "-fx-cursor:hand;"
                );
            }

            // Conteneur carte
            HBox card = new HBox(12, rb);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle(
                    "-fx-background-color:" + (reponseCourante != null && reponseCourante.getId() == r.getId()
                            ? "#F0EEFF" : "white") + ";" +
                            "-fx-background-radius:12;" +
                            "-fx-border-color:" + (reponseCourante != null && reponseCourante.getId() == r.getId()
                            ? "#6C63FF" : "#E8EAF0") + ";" +
                            "-fx-border-radius:12;" +
                            "-fx-border-width:2;" +
                            "-fx-cursor:hand;"
            );
            HBox.setHgrow(rb, Priority.ALWAYS);

            // Hover + sélection
            final Reponse reponse = r;
            rb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    card.setStyle(
                            "-fx-background-color:#F0EEFF;" +
                                    "-fx-background-radius:12;" +
                                    "-fx-border-color:#6C63FF;" +
                                    "-fx-border-radius:12;" +
                                    "-fx-border-width:2;" +
                                    "-fx-cursor:hand;"
                    );
                    rb.setStyle(
                            "-fx-font-size:14px;" +
                                    "-fx-text-fill:#6C63FF;" +
                                    "-fx-font-weight:bold;" +
                                    "-fx-padding:14 18;" +
                                    "-fx-cursor:hand;"
                    );
                    reponsesChoisies.set(indexCourant, reponse);
                } else {
                    card.setStyle(
                            "-fx-background-color:white;" +
                                    "-fx-background-radius:12;" +
                                    "-fx-border-color:#E8EAF0;" +
                                    "-fx-border-radius:12;" +
                                    "-fx-border-width:2;" +
                                    "-fx-cursor:hand;"
                    );
                    rb.setStyle(
                            "-fx-font-size:14px;" +
                                    "-fx-text-fill:#1A1D2E;" +
                                    "-fx-padding:14 18;" +
                                    "-fx-cursor:hand;"
                    );
                }
            });

            card.setOnMouseClicked(e -> {
                rb.setSelected(true);
                reponsesChoisies.set(indexCourant, reponse);
            });

            card.setOnMouseEntered(e -> {
                if (!rb.isSelected()) {
                    card.setStyle(
                            "-fx-background-color:#F8F7FF;" +
                                    "-fx-background-radius:12;" +
                                    "-fx-border-color:#C4B5FD;" +
                                    "-fx-border-radius:12;" +
                                    "-fx-border-width:2;" +
                                    "-fx-cursor:hand;"
                    );
                }
            });

            card.setOnMouseExited(e -> {
                if (!rb.isSelected()) {
                    card.setStyle(
                            "-fx-background-color:white;" +
                                    "-fx-background-radius:12;" +
                                    "-fx-border-color:#E8EAF0;" +
                                    "-fx-border-radius:12;" +
                                    "-fx-border-width:2;" +
                                    "-fx-cursor:hand;"
                    );
                }
            });

            vboxReponses.getChildren().add(card);
        }

        // Navigation
        btnPrecedent.setDisable(index == 0);
        boolean derniere = index == questions.size() - 1;
        btnSuivant.setVisible(!derniere);
        btnSuivant.setManaged(!derniere);
        btnTerminer.setVisible(derniere);
        btnTerminer.setManaged(derniere);
    }

    @FXML private void handlePrecedent() {
        if (indexCourant > 0) afficherQuestion(indexCourant - 1);
    }

    @FXML private void handleSuivant() {
        if (indexCourant < questions.size() - 1) afficherQuestion(indexCourant + 1);
    }

    @FXML private void handleTerminer() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Êtes-vous sûr de vouloir terminer le test ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Terminer le test");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) terminerTest(false);
        });
    }

    private void terminerTest(boolean parTimer) {
        if (timer != null) timer.stop();

        // Calcul du score
        int scoreTotal = 0;
        int scoreMax   = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q     = questions.get(i);
            Reponse choix  = reponsesChoisies.get(i);
            scoreMax      += q.getScoreMax();
            if (choix != null && choix.isCorrect()) {
                scoreTotal += q.getScoreMax();
            }
        }

        double pourcentage = scoreMax > 0 ? (double) scoreTotal / scoreMax * 100 : 0;
        LocalDateTime dateFin = LocalDateTime.now();

        // Sauvegarder en BD
        try {
            TestPassage passage = new TestPassage(
                    dateDebut, dateFin, pourcentage,
                    scoreTotal, scoreMax, "termine",
                    secondesEcoulees, test.getId(), userId
            );
            testPassageService.ajouter(passage);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Afficher résultats
        afficherResultats(scoreTotal, scoreMax, pourcentage, parTimer);
    }

    private void afficherResultats(int score, int scoreMax,
                                   double pourcentage, boolean parTimer) {
        panelQuestion.setVisible(false);
        panelQuestion.setManaged(false);
        panelResultat.setVisible(true);
        panelResultat.setManaged(true);

        // Titre
        if (parTimer) {
            labelResultatTitre.setText("⏰ Temps écoulé !");
            labelResultatTitre.setStyle(
                    "-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#CA8A04;");
        } else if (pourcentage >= 80) {
            labelResultatTitre.setText("🎉 Excellent !");
            labelResultatTitre.setStyle(
                    "-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#059669;");
        } else if (pourcentage >= 50) {
            labelResultatTitre.setText("👍 Bien joué !");
            labelResultatTitre.setStyle(
                    "-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");
        } else {
            labelResultatTitre.setText("📚 À améliorer");
            labelResultatTitre.setStyle(
                    "-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#E11D48;");
        }

        labelScoreFinal.setText(String.format("%.0f%%", pourcentage));

        // Niveau estimé
        String niveau;
        if      (pourcentage >= 90) niveau = "C2";
        else if (pourcentage >= 80) niveau = "C1";
        else if (pourcentage >= 70) niveau = "B2";
        else if (pourcentage >= 60) niveau = "B1";
        else if (pourcentage >= 50) niveau = "A2";
        else                         niveau = "A1";
        labelNiveauEstime.setText("Niveau estimé : " + niveau);

        // Temps
        int min = secondesEcoulees / 60;
        int sec = secondesEcoulees % 60;
        labelTempsTotal.setText(String.format("Temps : %02d min %02d s", min, sec));

        // Détail par question
        vboxDetailResultats.getChildren().clear();
        for (int i = 0; i < questions.size(); i++) {
            Question q    = questions.get(i);
            Reponse choix = reponsesChoisies.get(i);
            boolean ok    = choix != null && choix.isCorrect();

            HBox ligne = new HBox(12);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setStyle(
                    "-fx-background-color:" + (ok ? "#F0FDF4" : "#FFF1F2") + ";" +
                            "-fx-background-radius:10;" +
                            "-fx-padding:12 16;"
            );

            Label icone = new Label(ok ? "✅" : "❌");
            icone.setStyle("-fx-font-size:18px;");

            VBox info = new VBox(3);
            Label enonce = new Label("Q" + (i + 1) + " : " +
                    (q.getEnonce().length() > 60
                            ? q.getEnonce().substring(0, 57) + "…"
                            : q.getEnonce()));
            enonce.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-text-fill:" + (ok ? "#059669" : "#E11D48") + ";");

            String repTxt = choix != null ? choix.getContenuRep() : "— Non répondu —";
            Label  rep    = new Label("Votre réponse : " + repTxt);
            rep.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");

            // Bonne réponse si mauvaise réponse
            if (!ok) {
                List<Reponse> reps = reponsesParQuestion.get(i);
                reps.stream().filter(Reponse::isCorrect).findFirst().ifPresent(bonne -> {
                    Label bonneRep = new Label("✓ Bonne réponse : " + bonne.getContenuRep());
                    bonneRep.setStyle(
                            "-fx-font-size:11px;-fx-text-fill:#059669;-fx-font-weight:bold;");
                    info.getChildren().add(bonneRep);
                });
            }

            info.getChildren().addAll(enonce, rep);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label pts = new Label((ok ? "+" + q.getScoreMax() : "0") + " pts");
            pts.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-text-fill:" + (ok ? "#059669" : "#9CA3AF") + ";");

            ligne.getChildren().addAll(icone, info, pts);
            vboxDetailResultats.getChildren().add(ligne);
        }
    }

    @FXML private void handleRecommencer() {
        // Réinitialiser et relancer
        reponsesChoisies.clear();
        for (Question ignored : questions) reponsesChoisies.add(null);
        secondesEcoulees = 0;
        dateDebut = LocalDateTime.now();
        panelResultat.setVisible(false);
        panelResultat.setManaged(false);
        panelQuestion.setVisible(true);
        panelQuestion.setManaged(true);
        demarrerTimer();
        afficherQuestion(0);
    }

    @FXML private void handleRetourAccueil() {
        // Retourner à la liste des tests
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/pijava_fluently/fxml/mes-tests.fxml")
            );
            Node vue = loader.load();
            // Remonter jusqu'au StackPane parent
            StackPane contentArea = (StackPane) panelResultat
                    .getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(vue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}