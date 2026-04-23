package com.example.pijava_fluently.controller;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.entites.Reponse;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.services.*;
import com.example.pijava_fluently.utils.LoggerUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
    private List<List<Reponse>> reponsesParQuestion = new ArrayList<>();
    private List<Object> reponsesChoisies = new ArrayList<>(); // Reponse pour QCM, String pour oral/texte_libre
    private List<String> reponsesTextes = new ArrayList<>();    // Pour les réponses texte/oral
    private int                indexCourant     = 0;
    private int                secondesEcoulees = 0;
    private Timeline           timer;
    private LocalDateTime      dateDebut;
    private int                userId           = -1;
    private MesTestsController mesTestsController;

    private ExamModeService examService;
    private boolean isExamMode = false;
    private int tabSwitchCount = 0;
    private int copyPasteCount = 0;
    private int blurCount = 0;
    private TestPassage currentPassage; // Pour stocker le passage temporairement

    private final QuestionService    questionService    = new QuestionService();
    private final ReponseService     reponseService     = new ReponseService();
    private final TestPassageService testPassageService = new TestPassageService();
    private final FraudeTrackerService fraudeTracker = new FraudeTrackerService();

    // ── Initialisation avec le test ───────────────────────
    public void initTest(Test test, int userId, MesTestsController mesTestsController) {
        this.test = test;
        this.userId = userId;
        this.mesTestsController = mesTestsController;

        examService = new ExamModeService();
        isExamMode = examService.isExamMode(test);

        labelTitreTest.setText(test.getTitre());

        if (isExamMode) {
            MAX_INFRACTIONS = fraudeTracker.getMaxTentativesAutorisees(userId);
            LoggerUtil.info("Exam mode",
                    "maxInfractions", String.valueOf(MAX_INFRACTIONS),
                    "userId",         String.valueOf(userId));
            labelTitreTest.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            labelTitreTest.setText("🔒 MODE EXAMEN - " + test.getTitre());
            setupExamModeDetection();
        }

        dateDebut = LocalDateTime.now();
        chargerQuestions();
        demarrerTimer();
        afficherQuestion(0);
    }
    // Surcharge qui accepte un User directement
    public void initTest(Test test, User user) {
        initTest(test, user != null ? user.getId() : -1, null);
    }

    // Surcharge rétrocompatibilité int
    public void initTest(Test test, int userId) {
        initTest(test, userId, null);
    }

    private void chargerQuestions() {
        try {
            questions = questionService.recupererParTest(test.getId());



            // Initialiser les réponses choisies à null
            // Initialiser les structures pour tous les types
            for (Question q : questions) {
                reponsesChoisies.add(null);
                reponsesTextes.add("");
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
            showAlert("Info", "Ce test ne contient pas de questions.");
            return;
        }

        indexCourant = index;
        Question q = questions.get(index);

        labelNumeroQuestion.setText("Question " + (index + 1) + " / " + questions.size());
        labelProgression.setText((index + 1) + " / " + questions.size());
        progressBar.setProgress((double)(index + 1) / questions.size());
        labelEnonce.setText(q.getEnonce());
        vboxReponses.getChildren().clear();

        // Afficher selon le type
        switch (q.getType()) {
            case "qcm":
                afficherQuestionQCM(q, index);
                break;
            case "oral":
                afficherQuestionOral(q, index);
                break;
            case "texte_libre":
                afficherQuestionTexteLibre(q, index);
                break;
            default:
                afficherQuestionTexteLibre(q, index);
        }

        btnPrecedent.setDisable(index == 0);
        boolean derniere = index == questions.size() - 1;
        btnSuivant.setVisible(!derniere);
        btnSuivant.setManaged(!derniere);
        btnTerminer.setVisible(derniere);
        btnTerminer.setManaged(derniere);
    }
    private void afficherQuestionQCM(Question q, int index) {
        List<Reponse> reponses = reponsesParQuestion.get(index);
        Reponse reponseCourante = (Reponse) reponsesChoisies.get(index);
        ToggleGroup group = new ToggleGroup();

        for (Reponse r : reponses) {
            RadioButton rb = new RadioButton(r.getContenuRep());
            rb.setToggleGroup(group);
            rb.setUserData(r);
            rb.setWrapText(true);
            rb.setStyle("-fx-font-size:14px;-fx-text-fill:#1A1D2E;-fx-padding:14 18;-fx-cursor:hand;");

            if (reponseCourante != null && reponseCourante.getId() == r.getId()) {
                rb.setSelected(true);
            }

            HBox card = new HBox(12, rb);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#E8EAF0;-fx-border-radius:12;-fx-border-width:2;-fx-cursor:hand;");
            HBox.setHgrow(rb, Priority.ALWAYS);

            final Reponse reponse = r;
            rb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    reponsesChoisies.set(indexCourant, reponse);
                }
            });
            vboxReponses.getChildren().add(card);
        }
    }

    private void afficherQuestionOral(Question q, int index) {
        String reponseExistante = reponsesTextes.get(index);

        VBox oralBox = new VBox(15);
        oralBox.setStyle("-fx-padding: 20; -fx-background-color: #F8F7FF; -fx-background-radius: 15;");

        Label instructionLabel = new Label("🎤 Lisez la phrase ci-dessus à voix haute");
        instructionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Votre réponse apparaîtra ici après avoir parlé...");
        textArea.setWrapText(true);
        textArea.setPrefHeight(100);
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 10; -fx-background-color: #FFFFFF;");

        if (!reponseExistante.isEmpty()) {
            textArea.setText(reponseExistante);
        }

        // Champ de texte manuel pour test (optionnel)
        TextField manualInput = new TextField();
        manualInput.setPromptText("Ou écrivez votre réponse ici (mode test)");
        manualInput.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 10;");
        manualInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                reponsesTextes.set(index, newVal);
                reponsesChoisies.set(index, newVal);
                textArea.setText(newVal);
            }
        });

        if (!reponseExistante.isEmpty()) {
            manualInput.setText(reponseExistante);
        }

        // Bouton microphone
        Button micButton = new Button("🎙️ Cliquez et parlez");
        micButton.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 12 25; -fx-font-size: 14px; -fx-cursor: hand;");

        Label statusLabel = new Label("Prêt à enregistrer");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8A8FA8;");

        SpeechRecognitionService speechService = new SpeechRecognitionService();

        micButton.setOnAction(e -> {
            if (micButton.getText().equals("🎙️ Cliquez et parlez")) {
                // Démarrer l'enregistrement
                micButton.setText("⏹️ Arrêter l'enregistrement");
                micButton.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 12 25; -fx-font-size: 14px; -fx-cursor: hand;");
                statusLabel.setText("🎙️ Enregistrement en cours... Parlez maintenant !");
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                speechService.startRecording();
            } else {
                // Arrêter et reconnaître
                micButton.setText("🎙️ Traitement...");
                micButton.setDisable(true);
                statusLabel.setText("🔄 Analyse de votre enregistrement...");

                speechService.stopRecordingAndRecognize().thenAccept(recognizedText -> {
                    Platform.runLater(() -> {
                        String finalText = recognizedText;
                        if (finalText == null || finalText.isEmpty()) {
                            finalText = "";
                            statusLabel.setText("⚠️ Aucune parole détectée. Réessayez ou écrivez manuellement.");
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444;");
                        } else {
                            textArea.setText(finalText);
                            reponsesTextes.set(index, finalText);
                            reponsesChoisies.set(index, finalText);
                            manualInput.setText(finalText);
                            statusLabel.setText("✅ Enregistrement terminé !");
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669; -fx-font-weight: bold;");
                        }

                        micButton.setText("🎙️ Cliquez et parlez");
                        micButton.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 12 25; -fx-font-size: 14px; -fx-cursor: hand;");
                        micButton.setDisable(false);
                    });
                });
            }
        });

        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10 0;");

        oralBox.getChildren().addAll(instructionLabel, textArea, micButton, statusLabel, separator, manualInput);
        vboxReponses.getChildren().add(oralBox);
    }

    private void afficherQuestionTexteLibre(Question q, int index) {
        String reponseExistante = reponsesTextes.get(index);

        VBox textBox = new VBox(15);
        textBox.setStyle("-fx-padding: 20;");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Écrivez votre réponse ici...");
        textArea.setWrapText(true);
        textArea.setPrefHeight(150);
        if (!reponseExistante.isEmpty()) {
            textArea.setText(reponseExistante);
        }

        Label charCountLabel = new Label("0 caractères");
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            charCountLabel.setText(newVal.length() + " caractères");
            reponsesTextes.set(index, newVal);
            reponsesChoisies.set(index, newVal);
        });

        textBox.getChildren().addAll(textArea, charCountLabel);
        vboxReponses.getChildren().add(textBox);
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
        testTermine = true;

        double scoreTotal = 0.0;
        double scoreMaxTotal = 0.0;

        // Map pour stocker les scores réels par question (pour l'affichage)
        double[] scoresObtenus = new double[questions.size()];
        String[] statusParQuestion = new String[questions.size()];

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            scoreMaxTotal += q.getScoreMax();

            switch (q.getType()) {
                case "qcm": {
                    Reponse choix = (Reponse) reponsesChoisies.get(i);
                    if (choix != null && choix.isCorrect()) {
                        scoresObtenus[i] = q.getScoreMax();
                        statusParQuestion[i] = "correct";
                    } else {
                        scoresObtenus[i] = 0;
                        statusParQuestion[i] = choix != null ? "incorrect" : "non_repondu";
                    }
                    scoreTotal += scoresObtenus[i];
                    break;
                }

                case "oral": {
                    String spokenText = (String) reponsesChoisies.get(i);
                    if (spokenText != null && !spokenText.trim().isEmpty()) {
                        SpeechEvaluationService speechEval = new SpeechEvaluationService();
                        String status = speechEval.evaluateAnswer(spokenText, q.getEnonce());
                        double oralScore = speechEval.calculateScore(status, q.getScoreMax());
                        scoresObtenus[i] = oralScore;
                        statusParQuestion[i] = status;
                        scoreTotal += oralScore;
                        LoggerUtil.info("Oral eval",
                                "spoken", spokenText,
                                "expected", q.getEnonce(),
                                "status", status,
                                "score", oralScore);
                    } else {
                        scoresObtenus[i] = 0;
                        statusParQuestion[i] = "non_repondu";
                    }
                    break;
                }

                case "texte_libre": {
                    String texte = (String) reponsesChoisies.get(i);
                    if (texte != null && !texte.trim().isEmpty()) {
                        try {
                            AITextCorrectionService aiService = new AITextCorrectionService();
                            Map<String, Object> correction = aiService.correctFreeText(
                                    texte,
                                    q.getEnonce(),
                                    "Français",
                                    "B1"
                            );

                            // ── Correction du bug de casting ──
                            Object scoreObj = correction.get("score");
                            int iaScore;
                            if (scoreObj instanceof Integer) {
                                iaScore = (Integer) scoreObj;
                            } else if (scoreObj instanceof Double) {
                                iaScore = ((Double) scoreObj).intValue();
                            } else if (scoreObj instanceof Number) {
                                iaScore = ((Number) scoreObj).intValue();
                            } else {
                                iaScore = 50; // fallback
                            }

                            double pointsGagnes = (iaScore / 100.0) * q.getScoreMax();
                            scoresObtenus[i] = pointsGagnes;
                            statusParQuestion[i] = "ia_" + iaScore; // ex: "ia_75"
                            scoreTotal += pointsGagnes;

                            LoggerUtil.info("IA Correction",
                                    "iaScore", iaScore,
                                    "pointsGagnes", pointsGagnes,
                                    "scoreMax", q.getScoreMax(),
                                    "commentaire", correction.get("commentaire"));

                        } catch (Exception e) {
                            LoggerUtil.error("Error correcting text", e);
                            // Fallback : 20% pour avoir écrit quelque chose
                            double fallback = q.getScoreMax() * 0.2;
                            scoresObtenus[i] = fallback;
                            statusParQuestion[i] = "fallback";
                            scoreTotal += fallback;
                        }
                    } else {
                        scoresObtenus[i] = 0;
                        statusParQuestion[i] = "non_repondu";
                    }
                    break;
                }

                default: {
                    scoresObtenus[i] = 0;
                    statusParQuestion[i] = "non_repondu";
                }
            }
        }

        double pourcentage = scoreMaxTotal > 0
                ? (scoreTotal / scoreMaxTotal) * 100.0 : 0.0;

        LoggerUtil.info("Final score",
                "scoreTotal", scoreTotal,
                "scoreMax", scoreMaxTotal,
                "pourcentage", pourcentage);

        // Sauvegarder en BD
        LocalDateTime dateFin = LocalDateTime.now();
        try {
            TestPassage passage = new TestPassage(
                    dateDebut, dateFin, pourcentage,
                    (int) Math.round(scoreTotal),
                    (int) Math.round(scoreMaxTotal),
                    "termine",
                    secondesEcoulees,
                    test.getId(),
                    userId
            );
            testPassageService.ajouter(passage);
        } catch (SQLException e) {
            LoggerUtil.error("Error saving test passage", e);
        }

        afficherResultats(scoresObtenus, statusParQuestion,
                (int) Math.round(scoreTotal),
                (int) Math.round(scoreMaxTotal),
                pourcentage, parTimer);
    }
    private void showLoading(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Traitement");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void afficherResultats(double[] scoresObtenus, String[] statusParQuestion,
                                   int score, int scoreMaxInt,
                                   double pourcentage, boolean parTimer) {
        panelQuestion.setVisible(false);
        panelQuestion.setManaged(false);
        panelResultat.setVisible(true);
        panelResultat.setManaged(true);

        // Titre selon résultat
        if (parTimer) {
            labelResultatTitre.setText("⏰ Temps écoulé !");
            labelResultatTitre.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#CA8A04;");
        } else if (pourcentage >= 80) {
            labelResultatTitre.setText("🎉 Excellent !");
            labelResultatTitre.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#059669;");
        } else if (pourcentage >= 50) {
            labelResultatTitre.setText("👍 Bien joué !");
            labelResultatTitre.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");
        } else {
            labelResultatTitre.setText("📚 À améliorer");
            labelResultatTitre.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#E11D48;");
        }

        labelScoreFinal.setText(String.format("%.0f%%", pourcentage));

        String niveau;
        if      (pourcentage >= 90) niveau = "C2";
        else if (pourcentage >= 80) niveau = "C1";
        else if (pourcentage >= 70) niveau = "B2";
        else if (pourcentage >= 60) niveau = "B1";
        else if (pourcentage >= 50) niveau = "A2";
        else                        niveau = "A1";
        labelNiveauEstime.setText("Niveau estimé : " + niveau);

        int min = secondesEcoulees / 60;
        int sec = secondesEcoulees % 60;
        labelTempsTotal.setText(String.format("Temps : %02d min %02d s", min, sec));

        // Détail par question
        vboxDetailResultats.getChildren().clear();

        for (int i = 0; i < questions.size(); i++) {
            Question q    = questions.get(i);
            Object repObj = reponsesChoisies.get(i);
            double pts    = scoresObtenus[i];
            String status = statusParQuestion[i] != null ? statusParQuestion[i] : "";

            // ── Construire le texte de la réponse ──
            String repTxt;
            boolean ok;

            switch (q.getType()) {
                case "qcm": {
                    Reponse choix = (Reponse) repObj;
                    ok     = "correct".equals(status);
                    repTxt = choix != null ? choix.getContenuRep() : "— Non répondu —";
                    break;
                }
                case "oral": {
                    String t = (String) repObj;
                    ok     = "correct".equals(status) || "partial".equals(status);
                    repTxt = (t != null && !t.isEmpty())
                            ? (t.length() > 80 ? t.substring(0, 77) + "…" : t)
                            : "— Non répondu —";
                    break;
                }
                case "texte_libre": {
                    String t = (String) repObj;
                    ok     = pts > 0;
                    repTxt = (t != null && !t.isEmpty())
                            ? (t.length() > 80 ? t.substring(0, 77) + "…" : t)
                            : "— Non répondu —";
                    break;
                }
                default: {
                    ok     = false;
                    repTxt = "— Non répondu —";
                }
            }

            // ── Couleur selon le type et le score ──
            String bgColor;
            if      (pts >= q.getScoreMax())        bgColor = "#F0FDF4"; // vert
            else if (pts > 0)                        bgColor = "#FFFBEB"; // orange
            else                                     bgColor = "#FFF1F2"; // rouge

            HBox ligne = new HBox(12);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setStyle("-fx-background-color:" + bgColor
                    + ";-fx-background-radius:10;-fx-padding:12 16;");

            // Icône
            String icone;
            if      (pts >= q.getScoreMax()) icone = "✅";
            else if (pts > 0)                icone = "⚠️";
            else                             icone = "❌";

            Label iconeLabel = new Label(icone);
            iconeLabel.setStyle("-fx-font-size:18px;");

            VBox info = new VBox(3);

            Label typeLabel = new Label("[" + q.getType().toUpperCase() + "]");
            typeLabel.setStyle("-fx-font-size:10px;-fx-text-fill:#8A8FA8;");

            String enonceTxt = q.getEnonce().length() > 60
                    ? q.getEnonce().substring(0, 57) + "…" : q.getEnonce();
            Label enonceLabel = new Label("Q" + (i + 1) + " : " + enonceTxt);
            enonceLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

            Label repLabel = new Label("Votre réponse : " + repTxt);
            repLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
            repLabel.setWrapText(true);

            info.getChildren().addAll(typeLabel, enonceLabel, repLabel);

            // ── Infos supplémentaires selon le type ──
            if (q.getType().equals("qcm") && !ok) {
                reponsesParQuestion.get(i).stream()
                        .filter(Reponse::isCorrect).findFirst()
                        .ifPresent(bonne -> {
                            Label bonneRep = new Label("✓ Bonne réponse : " + bonne.getContenuRep());
                            bonneRep.setStyle("-fx-font-size:11px;-fx-text-fill:#059669;-fx-font-weight:bold;");
                            info.getChildren().add(bonneRep);
                        });
            }

            if (q.getType().equals("oral") && repObj != null) {
                SpeechEvaluationService se = new SpeechEvaluationService();
                double sim = se.calculateSimilarity((String) repObj, q.getEnonce());
                Label simLabel = new Label(String.format("Similarité : %.0f%%", sim * 100));
                simLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#6C63FF;");
                info.getChildren().add(simLabel);
            }

            if (q.getType().equals("texte_libre") && status.startsWith("ia_")) {
                int iaScore = Integer.parseInt(status.substring(3));
                Label iaLabel = new Label("Score IA : " + iaScore + "/100");
                iaLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#6C63FF;");
                info.getChildren().add(iaLabel);
            }

            HBox.setHgrow(info, Priority.ALWAYS);

            // Points affichés avec précision
            String ptsStr = pts == Math.floor(pts)
                    ? String.format("%.0f", pts)
                    : String.format("%.1f", pts);
            Label ptsLabel = new Label(ptsStr + "/" + q.getScoreMax() + " pts");
            ptsLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;"
                    + "-fx-text-fill:" + (pts > 0 ? "#059669" : "#9CA3AF") + ";");

            ligne.getChildren().addAll(iconeLabel, info, ptsLabel);
            vboxDetailResultats.getChildren().add(ligne);
        }
    }

    @FXML
    private void handleRecommencer() {
        // Réinitialiser toutes les structures
        reponsesChoisies.clear();
        reponsesTextes.clear();

        for (int i = 0; i < questions.size(); i++) {
            reponsesChoisies.add(null);
            reponsesTextes.add("");
        }

        secondesEcoulees = 0;
        dateDebut = LocalDateTime.now();

        // Arrêter l'ancien timer
        if (timer != null) {
            timer.stop();
        }

        // Cacher panel résultats
        panelResultat.setVisible(false);
        panelResultat.setManaged(false);
        panelQuestion.setVisible(true);
        panelQuestion.setManaged(true);

        // Redémarrer
        demarrerTimer();
        afficherQuestion(0);
    }

    @FXML private void handleRetourAccueil() {
        if (mesTestsController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/com/example/pijava_fluently/fxml/mes-tests.fxml"));
                Node vue = loader.load();
                MesTestsController ctrl = loader.getController();
                ctrl.setCurrentUser(mesTestsController.getCurrentUser());
                ctrl.setHomeController(mesTestsController.getHomeController());
                StackPane contentArea = (StackPane) panelResultat
                        .getScene().lookup("#contentArea");
                if (contentArea != null) contentArea.getChildren().setAll(vue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Retour depuis ApprentissageController
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/com/example/pijava_fluently/fxml/apprentissage.fxml"));
                Node vue = loader.load();
                StackPane contentArea = (StackPane) panelResultat
                        .getScene().lookup("#contentArea");
                if (contentArea != null) contentArea.getChildren().setAll(vue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
    // ── Compteur global d'infractions ────────────────────────────────

    // ── Flags pour éviter les boucles ────────────────────────────────
    private int     totalInfractions  = 0;
    private boolean testTermine       = false;  // stopper tout après fin
    private boolean alerteEnCours     = false;  // éviter les boucles focus
    private int MAX_INFRACTIONS = 3;

    private void setupExamModeDetection() {
        labelTitreTest.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            javafx.stage.Stage stage = (javafx.stage.Stage) newScene.getWindow();
            if (isExamMode) {
                stage.setFullScreen(true);
                stage.setFullScreenExitHint(
                        "⚠️ MODE EXAMEN — Rester en plein écran");
                // Empêcher la sortie du plein écran
                stage.fullScreenProperty().addListener((o, wasFS, isNowFS) -> {
                    if (!isNowFS && isExamMode && !testTermine && !alerteEnCours) {
                        // Forcer le retour en plein écran
                        Platform.runLater(() -> stage.setFullScreen(true));
                        logInfraction("Tentative de quitter le plein écran");
                    }
                });
            }
            // ── Perte de focus ────────────────────────────────────────
            stage.focusedProperty().addListener((obs2, wasActive, isNowActive) -> {
                if (!isNowActive && isExamMode && !testTermine && !alerteEnCours) {
                    blurCount++;
                    LoggerUtil.warning("Focus perdu", "count", String.valueOf(blurCount));

                    // Afficher un toast à CHAQUE changement (pas juste après 2)
                    Platform.runLater(() -> afficherToastAvertissement(
                            "⚠️ Changement de fenêtre détecté ! (" + blurCount + ")" +
                                    (blurCount > 2
                                            ? " — " + (MAX_INFRACTIONS - (blurCount - 2)) +
                                            " infraction(s) restante(s)"
                                            : " — Restez sur cet écran !")
                    ));

                    if (blurCount > 2) {
                        logInfraction("Changement de fenêtre/bureau (" + blurCount + " fois)");
                    }
                }
            });

            // ── Menu contextuel (clic droit) ──────────────────────────
            stage.addEventFilter(
                    javafx.scene.input.ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                    event -> {
                        if (!isExamMode || testTermine) return;
                        event.consume();
                        logInfraction("Clic droit détecté");
                    });

            // ── Touches interdites ────────────────────────────────────
            stage.addEventFilter(
                    javafx.scene.input.KeyEvent.KEY_PRESSED,
                    event -> {
                        if (!isExamMode || testTermine) return;
                        if (event.isControlDown()) {
                            javafx.scene.input.KeyCode kc = event.getCode();
                            if (kc == javafx.scene.input.KeyCode.C
                                    || kc == javafx.scene.input.KeyCode.V
                                    || kc == javafx.scene.input.KeyCode.X) {
                                event.consume();
                                copyPasteCount++;
                                logInfraction("Copier-coller (Ctrl+" + kc + ")");
                            }
                        }
                        if (event.getCode() == javafx.scene.input.KeyCode.F12) {
                            event.consume();
                            logInfraction("Tentative F12");
                        }
                    });
        });
    }
    /**
     * Affiche un bandeau d'avertissement NON BLOQUANT en haut de l'écran.
     * Disparaît automatiquement après 3 secondes.
     */
    private void afficherToastAvertissement(String message) {
        if (testTermine) return;

        // Trouver la scène
        javafx.scene.Scene scene = labelTitreTest.getScene();
        if (scene == null) return;

        // Créer le bandeau
        Label toast = new Label(message);
        toast.setStyle(
                "-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:0;-fx-padding:12 20;" +
                        "-fx-border-color:#FDE68A;-fx-border-width:0 0 2 0;");
        toast.setMaxWidth(Double.MAX_VALUE);
        toast.setAlignment(Pos.CENTER);

        // Chercher le conteneur principal (VBox ou BorderPane)
        if (scene.getRoot() instanceof VBox root) {
            root.getChildren().add(0, toast);
            // Supprimer après 3 secondes
            new Timeline(new KeyFrame(Duration.seconds(3),
                    e -> root.getChildren().remove(toast))).play();
        } else {
            // Fallback : StackPane overlay
            if (scene.getRoot() instanceof StackPane sp) {
                toast.setAlignment(Pos.CENTER);
                StackPane.setAlignment(toast, Pos.TOP_CENTER);
                sp.getChildren().add(toast);
                new Timeline(new KeyFrame(Duration.seconds(3),
                        e -> sp.getChildren().remove(toast))).play();
            }
        }
    }

    private void logInfraction(String raison) {
        if (testTermine || alerteEnCours) return;

        totalInfractions++;

        // ── Enregistrer dans le fichier de fraude ────────────────────
        fraudeTracker.enregistrer(userId, raison,
                test.getId(), test.getTitre());

        int restantes = MAX_INFRACTIONS - totalInfractions;
        LoggerUtil.warning("INFRACTION " + totalInfractions + "/" + MAX_INFRACTIONS,
                "raison", raison);

        Platform.runLater(() -> {
            if (testTermine) return;
            alerteEnCours = true;

            if (totalInfractions >= MAX_INFRACTIONS) {
                testTermine = true;
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("🔒 Test annulé");
                alert.setHeaderText("Trop d'infractions — test annulé");
                alert.setContentText(
                        "Infraction : " + raison + "\n\n" +
                                MAX_INFRACTIONS + " infractions atteintes.\n" +
                                "Le test est soumis avec un score de 0.");
                // Sortir du plein écran pour afficher l'alerte
                if (labelTitreTest.getScene() != null) {
                    javafx.stage.Stage s = (javafx.stage.Stage)
                            labelTitreTest.getScene().getWindow();
                    s.setFullScreen(false);
                }
                alert.showAndWait();
                alerteEnCours = false;
                terminerTestForce();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("⚠️ Avertissement");
                alert.setHeaderText("Infraction " + totalInfractions + "/" + MAX_INFRACTIONS);
                alert.setContentText("Infraction : " + raison + "\n\nEncore "
                        + restantes + " infraction(s) avant annulation.");
                alert.showAndWait();
                alerteEnCours = false;
            }
        });
    }

    private void terminerTestForce() {
        if (timer != null) timer.stop();

        try {
            TestPassage passage = new TestPassage(
                    dateDebut,
                    LocalDateTime.now(),
                    0.0, 0,
                    questions.stream().mapToInt(Question::getScoreMax).sum(),
                    "annule",
                    secondesEcoulees,
                    test.getId(),
                    userId
            );
            testPassageService.ajouter(passage);
        } catch (Exception e) {
            LoggerUtil.error("Erreur sauvegarde passage annulé", e);
        }

        // Afficher les résultats avec score 0
        int scoreMax = questions.stream().mapToInt(Question::getScoreMax).sum();
        double[] scoresObtenus  = new double[questions.size()];
        String[] statusParQuestion = new String[questions.size()];
        java.util.Arrays.fill(statusParQuestion, "non_repondu");

        afficherResultats(scoresObtenus, statusParQuestion, 0, scoreMax, 0.0, false);
    }

    /**
     * Termine le test avec score = 0 (cas d'infraction maximale).
     */


    /** Calcule le scoreMax sans passer les questions */
    private int calculerScoreMax() {
        return questions.stream().mapToInt(Question::getScoreMax).sum();
    }
    private void showWarning(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention - Mode Examen");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}