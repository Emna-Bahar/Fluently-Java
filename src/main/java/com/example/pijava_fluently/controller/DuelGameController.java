package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.services.*;
import com.example.pijava_fluently.utils.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.util.HashMap;
import java.util.Map;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DuelGameController {

    @FXML private Label labelTitre;
    @FXML private Label labelQuestion;
    @FXML private Label labelProgression;
    @FXML private Label labelTimer;
    @FXML private VBox vboxReponses;
    @FXML private Label labelScoreMoi;
    @FXML private Label labelScoreAdv;
    @FXML private Label labelNomMoi;
    @FXML private Label labelNomAdv;
    @FXML private ProgressBar barMoi;
    @FXML private ProgressBar barAdv;
    @FXML private VBox panelResultat;
    @FXML private Label labelGagnant;
    @FXML private Label labelScoreFinMoi;
    @FXML private Label labelScoreFinAdv;

    private User currentUser;
    private Test test;
    private DuelServer server;
    private DuelClient client;
    private boolean isHost;

    private List<DuelMessage.QuestionDto> questions;
    private int indexCourant = 0;
    private int scoreMoi = 0;
    private int scoreAdv = 0;
    private int scoreMaxTotal = 0;

    private Timeline timerQuestion;
    private int secondesRestantes;
    private boolean aRepondu = false;
    private boolean duelTermine = false;
    private boolean moiATermine = false;
    private boolean advATermine = false;

    // Stockage des réponses pour le détail
    private List<String> mesReponsesTextes = new ArrayList<>();
    private List<Integer> mesScoresParQuestion = new ArrayList<>();
    private List<Boolean> mesReponsesCorrectes = new ArrayList<>();

    private final QuestionService questionService = new QuestionService();
    private final ReponseService reponseService = new ReponseService();
    private int testIdClient = 0;
    private boolean testTermine = false;

    private HomeController homeController;

    public void setHomeController(HomeController hc) {
        this.homeController = hc;
    }

    public void init(User user, Test test, DuelServer server, DuelClient client) {
        this.currentUser = user;
        this.test = test;
        this.server = server;
        this.client = client;
        this.isHost = (server != null);

        // Réinitialiser les listes
        mesReponsesTextes.clear();
        mesScoresParQuestion.clear();
        mesReponsesCorrectes.clear();
        testTermine = false;

        labelNomMoi.setText("🏆 " + user.getPrenom());

        if (isHost) server.onMessageReceived = this::handleMessage;
        else client.onMessageReceived = this::handleMessage;

        if (isHost) {
            chargerEtEnvoyerQuestions();
        }
    }

    private void chargerEtEnvoyerQuestions() {
        try {
            List<Question> rawQuestions = questionService.recupererParTest(test.getId());
            rawQuestions.removeIf(q -> q.getType() == null);

            questions = new ArrayList<>();
            scoreMaxTotal = 0;

            for (Question q : rawQuestions) {
                List<Reponse> rawRep = reponseService.recupererParQuestion(q.getId());

                List<DuelMessage.ReponseDto> repDtos = rawRep.stream()
                        .map(r -> new DuelMessage.ReponseDto(
                                r.getId(),
                                r.getContenuRep(),
                                r.isCorrect(),
                                r.getScore()))
                        .toList();

                questions.add(new DuelMessage.QuestionDto(
                        q.getId(), q.getEnonce(), q.getType(), q.getScoreMax(), repDtos));

                scoreMaxTotal += q.getScoreMax();
            }

            labelTitre.setText("⚔️ Duel — " + test.getTitre());

            DuelMessage msg = new DuelMessage(DuelMessage.Action.QUESTIONS);
            msg.questions = questions;
            msg.scoreMaxTotal = scoreMaxTotal;
            msg.testTitre = test.getTitre();
            msg.testId = test.getId();  // ← Envoyer l'ID du test
            sendMessage(msg);

            afficherQuestion(0);
            demarrerTimer();

        } catch (SQLException e) {
            LoggerUtil.error("Erreur chargement questions duel", e);
        }
    }

    private void afficherQuestion(int idx) {
        if (questions == null || idx >= questions.size()) {
            finirMonCote();
            return;
        }

        indexCourant = idx;
        aRepondu = false;
        DuelMessage.QuestionDto q = questions.get(idx);

        labelProgression.setText("Question " + (idx + 1) + "/" + questions.size());
        labelQuestion.setText(q.enonce);
        vboxReponses.getChildren().clear();

        switch (q.type) {
            case "qcm" -> afficherQCM(q, idx);
            case "oral" -> afficherOral(q, idx);
            case "texte_libre" -> afficherTexteLibre(q, idx);
            default -> afficherQCM(q, idx);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  QCM
    // ═══════════════════════════════════════════════════════════════
    private void afficherQCM(DuelMessage.QuestionDto q, int idx) {
        ToggleGroup group = new ToggleGroup();
        for (DuelMessage.ReponseDto r : q.reponses) {
            RadioButton rb = new RadioButton(r.contenuRep);
            rb.setToggleGroup(group);
            rb.setStyle("-fx-font-size:14px;");

            HBox card = new HBox(12, rb);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12, 16, 12, 16));
            card.setStyle(
                    "-fx-background-color:white;-fx-background-radius:12;" +
                            "-fx-border-color:#E8EAF0;-fx-border-radius:12;" +
                            "-fx-border-width:1.5;-fx-cursor:hand;");

            rb.selectedProperty().addListener((obs, o, selected) -> {
                if (!selected || aRepondu) return;
                aRepondu = true;
                boolean correct = r.isCorrect;
                int pts = correct ? q.scoreMax : 0;
                if (correct) scoreMoi += q.scoreMax;

                // Stocker pour le détail
                mesReponsesTextes.add(r.contenuRep);
                mesScoresParQuestion.add(pts);
                mesReponsesCorrectes.add(correct);

                card.setStyle(
                        "-fx-background-color:" + (correct ? "#F0FDF4" : "#FFF1F2") + ";" +
                                "-fx-background-radius:12;-fx-border-color:" +
                                (correct ? "#16A34A" : "#DC2626") + ";" +
                                "-fx-border-radius:12;-fx-border-width:2;");
                group.getToggles().forEach(t -> ((RadioButton) t).setDisable(true));
                stopTimer();
                envoyerReponse(idx, r.id, correct);
                mettreAJourScores();
                passerQuestionSuivante(idx);
            });
            vboxReponses.getChildren().add(card);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ORAL avec microphone + saisie manuelle
    // ═══════════════════════════════════════════════════════════════
    private void afficherOral(DuelMessage.QuestionDto q, int idx) {
        VBox box = new VBox(16);
        box.setStyle(
                "-fx-background-color:#F8F7FF;-fx-background-radius:14;" +
                        "-fx-border-color:#DDD6FE;-fx-border-radius:14;" +
                        "-fx-border-width:1;-fx-padding:20;");

        Label inst = new Label("🎤 Lisez la phrase à voix haute");
        inst.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");

        Label phraseALire = new Label("\"" + q.enonce + "\"");
        phraseALire.setWrapText(true);
        phraseALire.setStyle(
                "-fx-font-size:16px;-fx-font-style:italic;-fx-text-fill:#1A1D2E;" +
                        "-fx-font-weight:bold;-fx-background-color:white;-fx-background-radius:10;" +
                        "-fx-border-color:#E8EAF0;-fx-border-radius:10;-fx-border-width:1;-fx-padding:14 18;");

        // Zone de texte pour le résultat de la reconnaissance
        TextArea textArea = new TextArea();
        textArea.setPromptText("Le texte reconnu apparaîtra ici...");
        textArea.setWrapText(true);
        textArea.setPrefHeight(80);
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 10; -fx-background-color: #FFFFFF;");

        Label labelResultat = new Label();
        labelResultat.setVisible(false);
        labelResultat.setManaged(false);
        labelResultat.setWrapText(true);
        labelResultat.setStyle("-fx-font-size:13px;-fx-background-radius:10;-fx-padding:10 14;");

        Button btnMicro = new Button("🎤 Enregistrer");
        btnMicro.setStyle(
                "-fx-background-color:#6C63FF;-fx-text-fill:white;-fx-font-size:14px;" +
                        "-fx-font-weight:bold;-fx-background-radius:12;-fx-padding:12 28;-fx-cursor:hand;");

        Label statusLabel = new Label("Prêt à enregistrer");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8A8FA8;");

        // Champ de texte manuel
        TextField champManuel = new TextField();
        champManuel.setPromptText("Ou écrivez votre réponse manuellement...");
        champManuel.setStyle(
                "-fx-font-size:14px;-fx-padding:10 14;-fx-background-radius:10;" +
                        "-fx-border-color:#E8EAF0;-fx-border-radius:10;-fx-border-width:1.5;");

        Button btnValider = new Button("✅ Valider");
        btnValider.setStyle(
                "-fx-background-color:#059669;-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:10 20;-fx-cursor:hand;");

        boolean[] enregistrement = {false};
        SpeechRecognitionService speechService = new SpeechRecognitionService("fr");

        btnMicro.setOnAction(e -> {
            if (aRepondu) return;

            if (!enregistrement[0]) {
                enregistrement[0] = true;
                btnMicro.setText("⏹ Arrêter");
                btnMicro.setStyle(
                        "-fx-background-color:#EF4444;-fx-text-fill:white;-fx-font-size:14px;" +
                                "-fx-font-weight:bold;-fx-background-radius:12;-fx-padding:12 28;-fx-cursor:hand;");
                statusLabel.setText("🎙️ Enregistrement en cours... Parlez !");
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                speechService.startRecording();

            } else {
                enregistrement[0] = false;
                btnMicro.setText("⏳ Analyse...");
                btnMicro.setDisable(true);
                statusLabel.setText("🔄 Analyse de votre enregistrement...");

                speechService.stopRecordingAndRecognize().thenAccept(transcription -> {
                    Platform.runLater(() -> {
                        if (aRepondu) return;

                        String finalText = transcription;
                        if (finalText == null || finalText.isEmpty()) {
                            statusLabel.setText("⚠️ Aucune parole détectée. Réessayez ou écrivez manuellement.");
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444;");
                        } else {
                            textArea.setText(finalText);
                            champManuel.setText(finalText);
                            statusLabel.setText("✅ Enregistrement terminé !");
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669; -fx-font-weight: bold;");
                        }

                        btnMicro.setText("🎤 Enregistrer");
                        btnMicro.setStyle(
                                "-fx-background-color:#6C63FF;-fx-text-fill:white;-fx-font-size:14px;" +
                                        "-fx-font-weight:bold;-fx-background-radius:12;-fx-padding:12 28;-fx-cursor:hand;");
                        btnMicro.setDisable(false);
                    });
                });
            }
        });

        // Validation manuelle + envoi au serveur
        btnValider.setOnAction(ev -> {
            if (aRepondu) return;
            String texte = champManuel.getText().trim();
            if (texte.isEmpty()) {
                statusLabel.setText("⚠️ Veuillez écrire une réponse !");
                return;
            }
            traiterReponseOrale(texte, q, idx, labelResultat, btnMicro, champManuel, btnValider, textArea, statusLabel);
        });

        // Séparateur
        HBox separateur = new HBox(10);
        separateur.setAlignment(Pos.CENTER);
        Separator sep1 = new Separator();
        sep1.setPrefWidth(80);
        HBox.setHgrow(sep1, Priority.ALWAYS);
        Label ouLabel = new Label("OU");
        ouLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#C0C7D0;-fx-font-weight:bold;");
        Separator sep2 = new Separator();
        HBox.setHgrow(sep2, Priority.ALWAYS);
        separateur.getChildren().addAll(sep1, ouLabel, sep2);

        VBox manuelBox = new VBox(8);
        Label manuelLbl = new Label("⌨️ Ou tapez votre réponse directement :");
        manuelLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#8A8FA8;");
        manuelBox.getChildren().addAll(manuelLbl, champManuel, btnValider);

        box.getChildren().addAll(inst, phraseALire, textArea, btnMicro, statusLabel, labelResultat, separateur, manuelBox);
        vboxReponses.getChildren().add(box);
    }

    private void traiterReponseOrale(
            String texte,
            DuelMessage.QuestionDto q,
            int idx,
            Label labelResultat,
            Button btnMicro,
            TextField champManuel,
            Button btnValider,
            TextArea textArea,
            Label statusLabel) {

        if (aRepondu) return;
        aRepondu = true;

        // Désactiver les contrôles
        btnMicro.setDisable(true);
        champManuel.setDisable(true);
        btnValider.setDisable(true);
        if (textArea != null) textArea.setEditable(false);
        stopTimer();

        // Évaluation Levenshtein
        SpeechEvaluationService eval = new SpeechEvaluationService();
        String status = eval.evaluateAnswer(texte, q.enonce);
        double pts = eval.calculateScore(status, q.scoreMax);
        int scoreObtenu = (int) pts;
        scoreMoi += scoreObtenu;

        // Stocker pour le détail
        mesReponsesTextes.add(texte);
        mesScoresParQuestion.add(scoreObtenu);
        mesReponsesCorrectes.add("correct".equals(status));

        // Afficher le résultat avec la similarité
        double similarity = eval.calculateSimilarity(texte, q.enonce);
        boolean correct = "correct".equals(status);
        boolean partial = "partial".equals(status);

        labelResultat.setVisible(true);
        labelResultat.setManaged(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        String expectedText = q.enonce;

        if (correct) {
            labelResultat.setText("✅ Excellent !\n" +
                    "▶ Votre réponse : \"" + texte + "\"\n" +
                    "🎯 Attendu : \"" + expectedText + "\"\n" +
                    "📊 Similarité : " + String.format("%.1f", similarity * 100) + "%\n" +
                    "⭐ Score : " + scoreObtenu + "/" + q.scoreMax + " pts");
            labelResultat.setStyle(
                    "-fx-font-size:12px;-fx-text-fill:#059669;" +
                            "-fx-background-color:#F0FDF4;-fx-background-radius:10;" +
                            "-fx-border-color:#BBF7D0;-fx-border-radius:10;" +
                            "-fx-border-width:1;-fx-padding:10 14;");
        } else if (partial) {
            labelResultat.setText("🟡 Partiellement correct\n" +
                    "▶ Votre réponse : \"" + texte + "\"\n" +
                    "🎯 Attendu : \"" + expectedText + "\"\n" +
                    "📊 Similarité : " + String.format("%.1f", similarity * 100) + "%\n" +
                    "⭐ Score : " + scoreObtenu + "/" + q.scoreMax + " pts");
            labelResultat.setStyle(
                    "-fx-font-size:12px;-fx-text-fill:#D97706;" +
                            "-fx-background-color:#FFFBEB;-fx-background-radius:10;" +
                            "-fx-border-color:#FDE68A;-fx-border-radius:10;" +
                            "-fx-border-width:1;-fx-padding:10 14;");
        } else {
            labelResultat.setText("❌ Incorrect\n" +
                    "▶ Votre réponse : \"" + texte + "\"\n" +
                    "🎯 Attendu : \"" + expectedText + "\"\n" +
                    "📊 Similarité : " + String.format("%.1f", similarity * 100) + "%\n" +
                    "⭐ Score : 0/" + q.scoreMax + " pts");
            labelResultat.setStyle(
                    "-fx-font-size:12px;-fx-text-fill:#DC2626;" +
                            "-fx-background-color:#FEF2F2;-fx-background-radius:10;" +
                            "-fx-border-color:#FECACA;-fx-border-radius:10;" +
                            "-fx-border-width:1;-fx-padding:10 14;");
        }

        // Envoyer au serveur et passer à la suite
        envoyerReponse(idx, -1, correct || partial);
        mettreAJourScores();
        passerQuestionSuivante(idx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TEXTE LIBRE
    // ═══════════════════════════════════════════════════════════════
    private void afficherTexteLibre(DuelMessage.QuestionDto q, int idx) {
        VBox box = new VBox(12);
        box.setStyle("-fx-padding:16;-fx-background-color:#F8F9FF;" +
                "-fx-background-radius:12;");

        Label inst = new Label("✍️ Rédigez votre réponse (sera corrigée par l'IA)");
        inst.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-font-weight:bold;");
        inst.setWrapText(true);

        TextArea textArea = new TextArea();
        textArea.setPromptText("Écrivez votre réponse ici...");
        textArea.setPrefHeight(120);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-size:14px;-fx-background-radius:8;");

        Label compteur = new Label("0 caractères");
        compteur.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
        textArea.textProperty().addListener((obs, o, n) ->
                compteur.setText(n.length() + " caractères"));

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size:12px;-fx-padding:8 0;");
        statusLabel.setVisible(false);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(30, 30);
        progress.setVisible(false);

        Button btnValider = new Button("✅ Valider");
        btnValider.setStyle(
                "-fx-background-color:#059669;-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:10 20;");

        HBox progressBox = new HBox(10, progress, statusLabel);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.setVisible(false);

        btnValider.setOnAction(e -> {
            if (aRepondu) return;
            String texte = textArea.getText().trim();
            if (texte.isEmpty()) {
                statusLabel.setText("⚠️ Veuillez écrire une réponse");
                statusLabel.setVisible(true);
                return;
            }

            aRepondu = true;
            textArea.setDisable(true);
            btnValider.setDisable(true);
            progressBox.setVisible(true);
            progress.setVisible(true);
            statusLabel.setVisible(true);
            statusLabel.setText("🤖 L'IA analyse votre réponse...");
            stopTimer();

            new Thread(() -> {
                int pts;
                String commentaire;

                try {
                    AITextCorrectionService aiService = new AITextCorrectionService();
                    Map<String, Object> correction = aiService.correctFreeText(
                            texte, q.enonce, "Français", "B1");

                    Object scoreObj = correction.get("score");
                    int iaScore;
                    if      (scoreObj instanceof Integer) iaScore = (Integer) scoreObj;
                    else if (scoreObj instanceof Double)  iaScore = ((Double) scoreObj).intValue();
                    else if (scoreObj instanceof Number)  iaScore = ((Number) scoreObj).intValue();
                    else                                  iaScore = 50;

                    pts = (int) Math.round((iaScore / 100.0) * q.scoreMax);
                    commentaire = (String) correction.getOrDefault("commentaire", "");

                } catch (Exception ex) {
                    // Fallback si Groq indisponible
                    pts = 0;
                    commentaire = "Correction IA indisponible";
                }

                final int finalPts = pts;
                final String finalComm = commentaire;

                scoreMoi += finalPts;
                mesReponsesTextes.add(texte);
                mesScoresParQuestion.add(finalPts);
                mesReponsesCorrectes.add(finalPts > 0);

                // ── Envoyer le score RÉEL via le socket ──
                // L'adversaire recevra ce score et n'aura pas besoin de le recalculer
                DuelMessage msg = new DuelMessage(DuelMessage.Action.ANSWER);
                msg.questionIndex = idx;
                msg.reponseId     = -1;
                msg.isCorrect     = finalPts > 0;
                msg.scoreObtenu   = finalPts; // ← score IA transmis
                sendMessage(msg);

                Platform.runLater(() -> {
                    progressBox.setVisible(false);
                    Label feedback = new Label(
                            "📝 Score IA : " + finalPts + "/" + q.scoreMax + " pts\n"
                                    + (finalComm.isEmpty() ? "" : "💬 " + finalComm));
                    feedback.setWrapText(true);
                    feedback.setStyle("-fx-font-size:12px;-fx-text-fill:#6C63FF;-fx-padding:8 0;");
                    box.getChildren().add(feedback);

                    mettreAJourScores();
                    passerQuestionSuivante(idx);
                });
            }).start();
        });

        box.getChildren().addAll(inst, textArea, compteur, btnValider, progressBox);
        vboxReponses.getChildren().add(box);
    }

    // ═══════════════════════════════════════════════════════════════
    //  MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    private void envoyerReponse(int idx, int reponseId, boolean correct) {
        DuelMessage msg = new DuelMessage(DuelMessage.Action.ANSWER);
        msg.questionIndex = idx;
        msg.reponseId = reponseId;
        msg.isCorrect = correct;
        sendMessage(msg);
    }

    private void passerQuestionSuivante(int idx) {
        new Timeline(new KeyFrame(Duration.millis(1500), e -> {
            afficherQuestion(idx + 1);
            demarrerTimer();
        })).play();
    }

    private void demarrerTimer() {
        secondesRestantes = 15;
        labelTimer.setText("⏱ 15s");
        labelTimer.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#EF4444;" +
                "-fx-background-color:#FEF2F2;-fx-background-radius:20;-fx-padding:6 14;");

        if (timerQuestion != null) timerQuestion.stop();
        timerQuestion = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondesRestantes--;
            labelTimer.setText("⏱ " + secondesRestantes + "s");
            if (secondesRestantes <= 5) {
                labelTimer.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#D97706;" +
                        "-fx-background-color:#FFFBEB;-fx-background-radius:20;-fx-padding:6 14;");
            }
            if (secondesRestantes <= 0) {
                timerQuestion.stop();
                if (!aRepondu) {
                    aRepondu = true;
                    new Timeline(new KeyFrame(Duration.millis(500), ev -> {
                        afficherQuestion(indexCourant + 1);
                        demarrerTimer();
                    })).play();
                }
            }
        }));
        timerQuestion.setCycleCount(Timeline.INDEFINITE);
        timerQuestion.play();
    }

    private void stopTimer() {
        if (timerQuestion != null) timerQuestion.stop();
    }

    private void handleMessage(DuelMessage msg) {
        switch (msg.action) {
            case QUESTIONS -> {
                this.questions = msg.questions;
                this.scoreMaxTotal = msg.scoreMaxTotal;
                this.testIdClient = msg.testId;
                labelTitre.setText("⚔️ Duel — " + msg.testTitre);
                afficherQuestion(0);
                demarrerTimer();
            }
            case NAME -> labelNomAdv.setText("⚔️ " + msg.playerName);
            case ANSWER -> {
                if (msg.scoreObtenu > 0) {
                    // Score texte libre transmis directement — pas de recalcul
                    scoreAdv += msg.scoreObtenu;
                } else if (msg.isCorrect) {
                    // QCM ou oral — score basé sur le scoreMax de la question
                    scoreAdv += getScoreMaxQuestion(msg.questionIndex);
                }
                mettreAJourScores();
            }
            case FINISHED -> {
                advATermine = true;
                checkFinDuel();
            }
            case END -> afficherResultatFinal(msg.winnerName);
        }
    }

    private void finirMonCote() {
        stopTimer();
        moiATermine = true;
        DuelMessage fin = new DuelMessage(DuelMessage.Action.FINISHED);
        fin.scoreFinal = scoreMoi;
        sendMessage(fin);
        checkFinDuel();
    }

    private void checkFinDuel() {
        if (!moiATermine || !advATermine) {
            if (moiATermine && !advATermine) {
                labelProgression.setText("✅ Terminé ! En attente de l'adversaire...");
                vboxReponses.getChildren().clear();
            }
            return;
        }

        if (isHost) {
            String gagnant;
            if (scoreMoi > scoreAdv) {
                gagnant = "🏆 " + currentUser.getPrenom() + " a gagné !";
            } else if (scoreAdv > scoreMoi) {
                gagnant = "🏆 L'adversaire a gagné !";
            } else {
                gagnant = "🤝 Égalité !";
            }

            DuelMessage end = new DuelMessage(DuelMessage.Action.END);
            end.winnerName = gagnant;
            sendMessage(end);
            afficherResultatFinal(gagnant);
        }
    }

    private void afficherResultatFinal(String gagnant) {
        if (duelTermine) return;
        duelTermine = true;
        stopTimer();

        // Sauvegarder dans leaderboard
        int finalTestId = (test != null) ? test.getId() : testIdClient;
        if (finalTestId > 0) {
            new Thread(() -> {
                try {
                    LeaderboardService lb = new LeaderboardService();
                    boolean jaGagne = gagnant.contains(currentUser.getPrenom());
                    boolean egalite = gagnant.contains("Égalité");
                    lb.sauvegarderResultatDuel(
                            currentUser.getId(), finalTestId,
                            scoreMoi, scoreMaxTotal, jaGagne, egalite);
                } catch (Exception e) {
                    LoggerUtil.error("Erreur sauvegarde leaderboard", e);
                }
            }).start();
        }

        vboxReponses.setVisible(false);
        vboxReponses.setManaged(false);
        panelResultat.setVisible(true);
        panelResultat.setManaged(true);

        labelGagnant.setText(gagnant);
        boolean jaGagne = gagnant.contains(currentUser.getPrenom());
        labelGagnant.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:" +
                (gagnant.contains("Égalité") ? "#D97706" : jaGagne ? "#059669" : "#EF4444") + ";");

        labelScoreFinMoi.setText(scoreMoi + " / " + scoreMaxTotal + " pts");
        labelScoreFinAdv.setText(scoreAdv + " / " + scoreMaxTotal + " pts");

        // Afficher le détail des réponses
        afficherDetailReponses();
        mettreAJourScores();
    }

    /**
     * Affiche le détail des réponses de l'utilisateur (questions, réponses données, scores)
     */
    private void afficherDetailReponses() {
        if (questions == null || questions.isEmpty()) return;

        VBox detailBox = new VBox(10);
        detailBox.setStyle("-fx-padding: 20; -fx-background-color: #F8F9FD; -fx-background-radius: 12;");
        detailBox.setMaxHeight(400);
        detailBox.setPrefHeight(350);

        ScrollPane scrollPane = new ScrollPane(detailBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(350);

        Label titreDetail = new Label("📋 Détail de vos réponses");
        titreDetail.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");
        detailBox.getChildren().add(titreDetail);

        for (int i = 0; i < questions.size(); i++) {
            DuelMessage.QuestionDto q = questions.get(i);

            String maReponse = (i < mesReponsesTextes.size()) ? mesReponsesTextes.get(i) : "— Non répondu —";
            int monScore = (i < mesScoresParQuestion.size()) ? mesScoresParQuestion.get(i) : 0;
            boolean correct = (i < mesReponsesCorrectes.size()) && mesReponsesCorrectes.get(i);

            // Badge type de question
            String typeBadge = "";
            String typeColor = "";
            switch (q.type) {
                case "qcm":
                    typeBadge = "📝 QCM";
                    typeColor = "#6C63FF";
                    break;
                case "oral":
                    typeBadge = "🎤 Oral";
                    typeColor = "#EC4899";
                    break;
                case "texte_libre":
                    typeBadge = "✍️ Texte libre";
                    typeColor = "#059669";
                    break;
            }

            VBox card = new VBox(8);
            card.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-background-radius: 10;" +
                    "-fx-border-color: #E8EAF0; -fx-border-radius: 10; -fx-border-width: 1;");

            // En-tête
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            Label numLabel = new Label("Q" + (i + 1));
            numLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6C63FF; -fx-min-width: 35;");

            Label typeLabel = new Label(typeBadge);
            typeLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;" +
                    "-fx-background-color:" + typeColor + "; -fx-background-radius: 10; -fx-padding: 3 8;");

            String enonceText = q.enonce;
            if (enonceText == null) enonceText = "";
            String shortEnonce = enonceText.length() > 50 ? enonceText.substring(0, 47) + "..." : enonceText;
            Label enonceLabel = new Label(shortEnonce);
            enonceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
            enonceLabel.setWrapText(true);
            HBox.setHgrow(enonceLabel, Priority.ALWAYS);

            Label scoreLabel = new Label(monScore + "/" + q.scoreMax + " pts");
            scoreLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (correct ? "#059669" : "#DC2626") + ";");

            header.getChildren().addAll(numLabel, typeLabel, enonceLabel, scoreLabel);
            card.getChildren().add(header);

            // Votre réponse
            Label reponseTitle = new Label("Votre réponse :");
            reponseTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8A8FA8;");

            Label reponseLabel = new Label(maReponse);
            reponseLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (correct ? "#059669" : "#DC2626") + ";");
            reponseLabel.setWrapText(true);

            card.getChildren().addAll(reponseTitle, reponseLabel);

            // Si incorrect ou partiel, afficher la bonne réponse attendue
            if (!correct && monScore < q.scoreMax) {
                Label expectedTitle = new Label("✓ Réponse attendue :");
                expectedTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #059669;");

                Label expectedLabel = new Label(q.enonce);
                expectedLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669;");
                expectedLabel.setWrapText(true);

                card.getChildren().addAll(expectedTitle, expectedLabel);
            }

            detailBox.getChildren().add(card);
        }

        // Ajouter le détail avant le bouton retour
        Node retourButton = null;
        for (Node child : panelResultat.getChildren()) {
            if (child instanceof Button && ((Button) child).getText().contains("Retour")) {
                retourButton = child;
                break;
            }
        }

        if (retourButton != null) {
            int index = panelResultat.getChildren().indexOf(retourButton);
            panelResultat.getChildren().add(index, scrollPane);
        } else {
            panelResultat.getChildren().add(scrollPane);
        }
    }

    @FXML
    private void handleRetour() {
        if (server != null) server.stop();
        if (client != null) client.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pijava_fluently/fxml/mes-tests.fxml"));
            Node vue = loader.load();
            MesTestsController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);

            if (homeController != null) {
                homeController.setContent(vue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mettreAJourScores() {
        labelScoreMoi.setText(scoreMoi + " pts");
        labelScoreAdv.setText(scoreAdv + " pts");
        if (scoreMaxTotal > 0) {
            barMoi.setProgress((double) scoreMoi / scoreMaxTotal);
            barAdv.setProgress((double) scoreAdv / scoreMaxTotal);
        }
    }

    private int getScoreMaxQuestion(int idx) {
        if (questions == null || idx >= questions.size()) return 0;
        return questions.get(idx).scoreMax;
    }

    private void sendMessage(DuelMessage msg) {
        if (isHost) server.send(msg);
        else client.send(msg);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}