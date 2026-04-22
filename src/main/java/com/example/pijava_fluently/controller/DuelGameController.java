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

    private final QuestionService questionService = new QuestionService();
    private final ReponseService reponseService = new ReponseService();

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

        labelNomMoi.setText("🏆 " + user.getPrenom());

        if (isHost) server.onMessageReceived = this::handleMessage;
        else client.onMessageReceived = this::handleMessage;

        if (isHost) {
            chargerEtEnvoyerQuestions();
        }
        // Client attendra le message QUESTIONS
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
        aRepondu     = false;
        DuelMessage.QuestionDto q = questions.get(idx);

        labelProgression.setText("Question " + (idx+1) + "/" + questions.size());
        labelQuestion.setText(q.enonce);
        vboxReponses.getChildren().clear();

        switch (q.type) {
            case "qcm"         -> afficherQCM(q, idx);
            case "oral"        -> afficherOral(q, idx);
            case "texte_libre" -> afficherTexteLibre(q, idx);
            default            -> afficherQCM(q, idx);
        }
    }

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
                if (correct) scoreMoi += q.scoreMax;
                card.setStyle(
                        "-fx-background-color:" + (correct ? "#F0FDF4" : "#FFF1F2") + ";" +
                                "-fx-background-radius:12;-fx-border-color:" +
                                (correct ? "#16A34A" : "#DC2626") + ";" +
                                "-fx-border-radius:12;-fx-border-width:2;");
                group.getToggles().forEach(t -> ((RadioButton)t).setDisable(true));
                stopTimer();
                envoyerReponse(idx, r.id, correct);
                mettreAJourScores();
                passerQuestionSuivante(idx);
            });
            vboxReponses.getChildren().add(card);
        }
    }

    private void afficherOral(DuelMessage.QuestionDto q, int idx) {
        VBox box = new VBox(12);
        box.setStyle("-fx-padding:16;-fx-background-color:#F8F7FF;" +
                "-fx-background-radius:12;");

        Label inst = new Label("🎤 Lisez à voix haute, puis saisissez ce que vous avez dit");
        inst.setStyle("-fx-font-size:13px;-fx-text-fill:#6C63FF;-fx-font-weight:bold;");
        inst.setWrapText(true);

        TextField champOral = new TextField();
        champOral.setPromptText("Tapez votre réponse orale ici...");
        champOral.setStyle("-fx-font-size:14px;-fx-padding:10;-fx-background-radius:8;");

        Button btnValider = new Button("✅ Valider");
        btnValider.setStyle(
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:10 20;");

        btnValider.setOnAction(e -> {
            if (aRepondu) return;
            String texte = champOral.getText().trim();
            if (texte.isEmpty()) return;
            aRepondu = true;

            // Évaluation Levenshtein
            SpeechEvaluationService eval = new SpeechEvaluationService();
            String status = eval.evaluateAnswer(texte, q.enonce);
            double pts    = eval.calculateScore(status, q.scoreMax);
            scoreMoi     += (int) pts;

            boolean correct = "correct".equals(status) || "partial".equals(status);
            champOral.setDisable(true);
            btnValider.setDisable(true);
            stopTimer();
            envoyerReponse(idx, -1, correct);
            mettreAJourScores();
            passerQuestionSuivante(idx);
        });

        box.getChildren().addAll(inst, champOral, btnValider);
        vboxReponses.getChildren().add(box);
    }

    private void afficherTexteLibre(DuelMessage.QuestionDto q, int idx) {
        VBox box = new VBox(12);
        box.setStyle("-fx-padding:16;-fx-background-color:#F8F9FF;" +
                "-fx-background-radius:12;");

        Label inst = new Label("✍️ Rédigez votre réponse (sera corrigée par l'IA après le duel)");
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

        Button btnValider = new Button("✅ Valider");
        btnValider.setStyle(
                "-fx-background-color:#059669;-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:10 20;");

        btnValider.setOnAction(e -> {
            if (aRepondu) return;
            String texte = textArea.getText().trim();
            if (texte.isEmpty()) return;
            aRepondu = true;

            // Score basique longueur (correction IA différée, pas en duel)
            int pts = texte.length() >= 30 ? q.scoreMax : q.scoreMax / 2;
            scoreMoi += pts;

            textArea.setDisable(true);
            btnValider.setDisable(true);
            stopTimer();
            envoyerReponse(idx, -1, pts > 0);
            mettreAJourScores();
            passerQuestionSuivante(idx);
        });

        box.getChildren().addAll(inst, textArea, compteur, btnValider);
        vboxReponses.getChildren().add(box);
    }

    // Méthodes utilitaires extraites
    private void envoyerReponse(int idx, int reponseId, boolean correct) {
        DuelMessage msg = new DuelMessage(DuelMessage.Action.ANSWER);
        msg.questionIndex = idx;
        msg.reponseId     = reponseId;
        msg.isCorrect     = correct;
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
                labelTitre.setText("⚔️ Duel — " + msg.testTitre);
                afficherQuestion(0);
                demarrerTimer();
            }
            case NAME -> labelNomAdv.setText("⚔️ " + msg.playerName);
            case ANSWER -> {
                if (msg.isCorrect) scoreAdv += getScoreMaxQuestion(msg.questionIndex);
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
            String gagnant = scoreMoi > scoreAdv ? currentUser.getPrenom() + " 🏆"
                    : scoreAdv > scoreMoi ? "Adversaire 🏆" : "Égalité ! 🤝";

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
        new Thread(() -> {
            try {
                LeaderboardService lb = new LeaderboardService();
                boolean jaGagne  = gagnant.contains(currentUser.getPrenom());
                boolean egalite  = gagnant.contains("Égalité");
                int     testId   = (test != null) ? test.getId() : 0;
                lb.sauvegarderResultatDuel(
                        currentUser.getId(), testId,
                        scoreMoi, scoreMaxTotal, jaGagne, egalite);
            } catch (Exception e) {
                LoggerUtil.error("Erreur sauvegarde leaderboard", e);
            }
        }).start();

        vboxReponses.setVisible(false);
        vboxReponses.setManaged(false);
        panelResultat.setVisible(true);
        panelResultat.setManaged(true);

        labelGagnant.setText(gagnant);
        boolean jaGagne = gagnant.contains(currentUser.getPrenom());
        labelGagnant.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:" +
                (gagnant.contains("Égalité") ? "#D97706" : jaGagne ? "#059669" : "#EF4444") + ";");

        labelScoreFinMoi.setText(scoreMoi + " pts");
        labelScoreFinAdv.setText(scoreAdv + " pts");
        mettreAJourScores();
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
}