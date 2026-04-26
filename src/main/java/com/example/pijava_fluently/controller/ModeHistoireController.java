package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.services.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class ModeHistoireController {

    // ── FXML ──────────────────────────────────────────────
    @FXML private Label   labelTitreTest;
    @FXML private Label   labelTimer;
    @FXML private Label   labelProgression;
    @FXML private ProgressBar progressBar;
    @FXML private Label   labelNumeroQuestion;
    @FXML private Label   labelEnonce;
    @FXML private VBox    vboxReponses;
    @FXML private Label   labelDialogue;
    @FXML private Label   labelLanguePersonnage;
    @FXML private Pane    panelPersonnage;
    @FXML private Pane    panelPersonnageResultat;
    @FXML private VBox    panelFeedback;
    @FXML private Label   labelFeedback;
    @FXML private Label   labelExplication;
    @FXML private Button  btnSuivant;
    @FXML private Button  btnTerminer;
    @FXML private VBox    panelResultat;
    @FXML private Label   labelResultatTitre;
    @FXML private Label   labelScoreFinal;
    @FXML private Label   labelNiveauEstime;
    @FXML private Label   labelTempsTotal;

    // ── State ─────────────────────────────────────────────
    private Test                  test;
    private List<Question>        questions            = new ArrayList<>();
    private List<List<Reponse>>   reponsesParQuestion  = new ArrayList<>();
    private List<Object>          reponsesChoisies     = new ArrayList<>();
    private int                   indexCourant         = 0;
    private int                   secondesEcoulees     = 0;
    private Timeline              timer;
    private LocalDateTime         dateDebut;
    private int                   userId               = -1;
    private MesTestsController    mesTestsController;

    // Résultats intermédiaires
    private double[] scoresObtenus;
    private String[] statusParQuestion;
    private boolean  reponduCourant = false; // a-t-on répondu à la question courante ?

    // Personnage
    private PersonnageSVG         personnageSVG        = new PersonnageSVG();
    private String                nomLangue            = "Français";

    // Dialogues par langue et contexte
    private static final Map<String, String[]> DIALOGUES_INTRO = Map.of(
            "Français",  new String[]{
                    "Bonjour ! Je suis Pierre. On va pratiquer le français ensemble !",
                    "Ah, une nouvelle question ! Réfléchis bien...",
                    "Tu progresses bien ! Continuons.",
                    "Presque fini ! Courage !"
            },
            "English", new String[]{
                    "Hello! I'm James. Let's practice English together!",
                    "Here's another one! Take your time...",
                    "You're doing great! Keep going.",
                    "Almost there! Keep it up!"
            },
            "Espagnol", new String[]{
                    "¡Hola! Soy Carlos. ¡Vamos a practicar español juntos!",
                    "¡Otra pregunta! Piénsalo bien...",
                    "¡Muy bien! Sigamos adelante.",
                    "¡Casi terminamos! ¡Ánimo!"
            }
    );

    private static final Map<String, String[]> DIALOGUES_CORRECT = Map.of(
            "Français",  new String[]{
                    "Magnifique ! C'est exactement ça !",
                    "Bravo ! Tu maîtrises bien le français !",
                    "Excellent ! Quelle belle réponse !"
            },
            "English", new String[]{
                    "Brilliant! That's exactly right!",
                    "Well done! Your English is great!",
                    "Excellent! Perfect answer!"
            },
            "Espagnol", new String[]{
                    "¡Perfecto! ¡Exactamente eso!",
                    "¡Muy bien! ¡Tu español es estupendo!",
                    "¡Excelente! ¡Respuesta perfecta!"
            }
    );

    private static final Map<String, String[]> DIALOGUES_FAUX = Map.of(
            "Français",  new String[]{
                    "Oh non ! Pas tout à fait... regarde la bonne réponse.",
                    "Ce n'est pas ça, mais tu apprendras !",
                    "Dommage ! La prochaine sera la bonne !"
            },
            "English", new String[]{
                    "Oh no! Not quite... check the right answer.",
                    "That's not it, but you'll learn!",
                    "Too bad! The next one will be right!"
            },
            "Espagnol", new String[]{
                    "¡Ay! No es correcto... mira la respuesta correcta.",
                    "¡No es eso! ¡Pero aprenderás!",
                    "¡Lástima! ¡La próxima será la correcta!"
            }
    );

    private final QuestionService    questionService    = new QuestionService();
    private final ReponseService     reponseService     = new ReponseService();
    private final TestPassageService testPassageService = new TestPassageService();

    // ── Init ──────────────────────────────────────────────

    public void initTest(Test test, int userId, MesTestsController mesTestsCtrl) {
        this.test               = test;
        this.userId             = userId;
        this.mesTestsController = mesTestsCtrl;
        this.dateDebut          = LocalDateTime.now();

        labelTitreTest.setText(test.getTitre());

        // Détecter la langue depuis le titre
        nomLangue = detecterLangue(test.getTitre());
        labelLanguePersonnage.setText(nomLangue.equals("English") ? "James — London"
                : nomLangue.equals("Espagnol") ? "Carlos — Madrid"
                : "Pierre — Paris");

        // Créer le personnage SVG
        Node personnage = personnageSVG.creerPersonnage(nomLangue);
        panelPersonnage.getChildren().setAll(personnage);

        chargerQuestions();
        demarrerTimer();
        afficherQuestion(0);
    }

    public void initTest(Test test, int userId) {
        initTest(test, userId, null);
    }

    private String detecterLangue(String titre) {
        if (titre == null) return "Français";
        String t = titre.toLowerCase();
        if (t.contains("english") || t.contains("anglais")) return "English";
        if (t.contains("espagnol") || t.contains("spanish")) return "Espagnol";
        return "Français";
    }

    private void chargerQuestions() {
        try {
            questions = questionService.recupererParTest(test.getId());
            scoresObtenus      = new double[questions.size()];
            statusParQuestion  = new String[questions.size()];
            for (Question q : questions) {
                reponsesChoisies.add(null);
                reponsesParQuestion.add(
                        reponseService.recupererParQuestion(q.getId()));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les questions : " + e.getMessage());
        }
    }

    private void demarrerTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondesEcoulees++;
            int m = secondesEcoulees / 60, s = secondesEcoulees % 60;
            labelTimer.setText(String.format("⏱ %02d:%02d", m, s));
            int limite = test.getDureeEstimee() * 60;
            if (limite > 0 && secondesEcoulees >= limite) {
                timer.stop();
                terminerTest(true);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    // ── Affichage question ────────────────────────────────

    private void afficherQuestion(int index) {
        if (questions.isEmpty()) {
            showAlert("Info", "Ce test ne contient pas de questions.");
            return;
        }

        indexCourant   = index;
        reponduCourant = false;
        Question q     = questions.get(index);

        // Mise à jour progression
        labelNumeroQuestion.setText("Question " + (index + 1) + " / " + questions.size());
        labelProgression.setText((index + 1) + " / " + questions.size());
        progressBar.setProgress((double)(index + 1) / questions.size());
        labelEnonce.setText(q.getEnonce());

        // Dialogue intro du personnage
        String[] intros = DIALOGUES_INTRO.getOrDefault(nomLangue,
                DIALOGUES_INTRO.get("Français"));
        int idxDialogue = Math.min(index, intros.length - 1);
        animerDialogue(intros[idxDialogue]);
        personnageSVG.changerExpression(PersonnageSVG.Expression.NEUTRE);

        // Cacher le feedback de la question précédente
        panelFeedback.setVisible(false);
        panelFeedback.setManaged(false);

        // Navigation
        btnSuivant.setVisible(false);
        btnSuivant.setManaged(false);
        btnTerminer.setVisible(false);
        btnTerminer.setManaged(false);

        // Afficher les réponses
        vboxReponses.getChildren().clear();
        afficherReponsesQCM(q, index);
    }

    private void afficherReponsesQCM(Question q, int index) {
        List<Reponse> reponses = reponsesParQuestion.get(index);

        for (Reponse r : reponses) {
            Button btnReponse = new Button(r.getContenuRep());
            btnReponse.setMaxWidth(Double.MAX_VALUE);
            btnReponse.setWrapText(true);
            btnReponse.setAlignment(Pos.CENTER_LEFT);
            btnReponse.setStyle(styleReponseNormal());

            // Hover effect
            btnReponse.setOnMouseEntered(e ->
                    btnReponse.setStyle(styleReponseHover()));
            btnReponse.setOnMouseExited(e ->
                    btnReponse.setStyle(styleReponseNormal()));

            btnReponse.setOnAction(e -> {
                if (reponduCourant) return; // bloquer si déjà répondu
                traiterReponse(r, index);
            });

            vboxReponses.getChildren().add(btnReponse);
        }
    }

    // ── Traitement réponse ────────────────────────────────

    private void traiterReponse(Reponse reponseChoisie, int index) {
        reponduCourant = true;
        reponsesChoisies.set(index, reponseChoisie);
        Question q = questions.get(index);

        boolean correct = reponseChoisie.isCorrect();

        // Calculer le score
        if (correct) {
            scoresObtenus[index]    = q.getScoreMax();
            statusParQuestion[index] = "correct";
        } else {
            scoresObtenus[index]    = 0;
            statusParQuestion[index] = "incorrect";
        }

        // Colorer les boutons
        colorerBoutonsReponse(index, reponseChoisie);

        // Expression + dialogue du personnage
        if (correct) {
            personnageSVG.changerExpression(PersonnageSVG.Expression.CONTENT);
            String[] msgs = DIALOGUES_CORRECT.getOrDefault(nomLangue,
                    DIALOGUES_CORRECT.get("Français"));
            animerDialogue(msgs[new Random().nextInt(msgs.length)]);
        } else {
            personnageSVG.changerExpression(PersonnageSVG.Expression.TRISTE);
            String[] msgs = DIALOGUES_FAUX.getOrDefault(nomLangue,
                    DIALOGUES_FAUX.get("Français"));
            animerDialogue(msgs[new Random().nextInt(msgs.length)]);
        }

        // Afficher le feedback
        afficherFeedback(correct, q, reponseChoisie);

        // Afficher bouton continuer (avec délai pour laisser voir le feedback)
        new Timeline(new KeyFrame(Duration.millis(600), e ->
                Platform.runLater(() -> {
                    boolean derniere = index == questions.size() - 1;
                    btnSuivant.setVisible(!derniere);
                    btnSuivant.setManaged(!derniere);
                    btnTerminer.setVisible(derniere);
                    btnTerminer.setManaged(derniere);
                })
        )).play();
    }

    private void colorerBoutonsReponse(int index, Reponse choisie) {
        List<Reponse> reponses = reponsesParQuestion.get(index);
        List<Node>    boutons  = vboxReponses.getChildren();

        for (int i = 0; i < boutons.size() && i < reponses.size(); i++) {
            if (!(boutons.get(i) instanceof Button btn)) continue;
            Reponse r = reponses.get(i);

            if (r.isCorrect()) {
                // Verte : bonne réponse
                btn.setStyle("""
                    -fx-background-color:#D1FAE5;
                    -fx-border-color:#059669;
                    -fx-border-width:2.5;
                    -fx-border-radius:14;
                    -fx-background-radius:14;
                    -fx-font-size:14px;
                    -fx-font-weight:bold;
                    -fx-text-fill:#065F46;
                    -fx-padding:14 20;
                    -fx-cursor:default;
                    """);
            } else if (r.getId() == choisie.getId()) {
                // Rouge : celle choisie (mauvaise)
                btn.setStyle("""
                    -fx-background-color:#FEE2E2;
                    -fx-border-color:#DC2626;
                    -fx-border-width:2.5;
                    -fx-border-radius:14;
                    -fx-background-radius:14;
                    -fx-font-size:14px;
                    -fx-font-weight:bold;
                    -fx-text-fill:#991B1B;
                    -fx-padding:14 20;
                    -fx-cursor:default;
                    """);
            } else {
                // Grisée : autres
                btn.setStyle("""
                    -fx-background-color:#F3F4F6;
                    -fx-border-color:#D1D5DB;
                    -fx-border-width:2;
                    -fx-border-radius:14;
                    -fx-background-radius:14;
                    -fx-font-size:14px;
                    -fx-text-fill:#9CA3AF;
                    -fx-padding:14 20;
                    -fx-cursor:default;
                    """);
            }
            btn.setDisable(true);
        }
    }

    private void afficherFeedback(boolean correct, Question q, Reponse choisie) {
        panelFeedback.setVisible(true);
        panelFeedback.setManaged(true);

        if (correct) {
            panelFeedback.setStyle("""
                -fx-background-color:#D1FAE5;
                -fx-background-radius:12;
                -fx-padding:14 18;
                -fx-border-color:#059669;
                -fx-border-radius:12;
                -fx-border-width:2;
                """);
            labelFeedback.setText("✅  Bonne réponse !");
            labelFeedback.setStyle("-fx-font-size:15px;-fx-font-weight:bold;"
                    + "-fx-text-fill:#065F46;");
            labelExplication.setText("Tu as gagné " + q.getScoreMax() + " points !");
            labelExplication.setStyle("-fx-font-size:12px;-fx-text-fill:#047857;");
        } else {
            // Trouver la bonne réponse pour l'afficher
            String bonneRep = reponsesParQuestion.get(indexCourant).stream()
                    .filter(Reponse::isCorrect)
                    .map(Reponse::getContenuRep)
                    .findFirst().orElse("?");

            panelFeedback.setStyle("""
                -fx-background-color:#FEE2E2;
                -fx-background-radius:12;
                -fx-padding:14 18;
                -fx-border-color:#DC2626;
                -fx-border-radius:12;
                -fx-border-width:2;
                """);
            labelFeedback.setText("❌  Mauvaise réponse");
            labelFeedback.setStyle("-fx-font-size:15px;-fx-font-weight:bold;"
                    + "-fx-text-fill:#991B1B;");
            labelExplication.setText("La bonne réponse était : " + bonneRep);
            labelExplication.setStyle("-fx-font-size:12px;-fx-text-fill:#B91C1C;");
        }

        // Animation d'apparition du feedback
        FadeTransition ft = new FadeTransition(Duration.millis(300), panelFeedback);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ── Animation dialogue bulle ──────────────────────────

    /**
     * Anime le texte dans la bulle lettre par lettre (effet machine à écrire).
     */
    private void animerDialogue(String texte) {
        labelDialogue.setText("");
        final int[] i = {0};
        Timeline typewriter = new Timeline(new KeyFrame(Duration.millis(30), e -> {
            if (i[0] < texte.length()) {
                labelDialogue.setText(texte.substring(0, ++i[0]));
            }
        }));
        typewriter.setCycleCount(texte.length());
        typewriter.play();
    }

    // ── Navigation ────────────────────────────────────────

    @FXML private void handleSuivant() {
        if (indexCourant < questions.size() - 1) {
            afficherQuestion(indexCourant + 1);
        }
    }

    @FXML private void handleTerminer() {
        terminerTest(false);
    }

    // ── Fin du test ───────────────────────────────────────

    private void terminerTest(boolean parTimer) {
        if (timer != null) timer.stop();
        personnageSVG.arreterAnimations();

        // Calculer totaux
        double scoreTotal    = Arrays.stream(scoresObtenus).sum();
        double scoreMaxTotal = questions.stream()
                .mapToDouble(Question::getScoreMax).sum();
        double pourcentage   = scoreMaxTotal > 0
                ? (scoreTotal / scoreMaxTotal) * 100.0 : 0.0;

        // Sauvegarder en BD
        try {
            TestPassage passage = new TestPassage(
                    dateDebut, LocalDateTime.now(), pourcentage,
                    (int) Math.round(scoreTotal),
                    (int) Math.round(scoreMaxTotal),
                    "termine", secondesEcoulees,
                    test.getId(), userId);
            testPassageService.ajouter(passage);
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde passage : " + e.getMessage());
        }

        afficherResultats(pourcentage, parTimer);
    }

    private void afficherResultats(double pourcentage, boolean parTimer) {
        // Cacher la zone principale, montrer les résultats
        panelResultat.setVisible(true);
        panelResultat.setManaged(true);

        // Personnage dans l'écran résultats
        PersonnageSVG persoResultat = new PersonnageSVG();
        Node pNode = persoResultat.creerPersonnage(nomLangue);
        panelPersonnageResultat.getChildren().setAll(pNode);

        // Expression finale
        if (pourcentage >= 80) {
            persoResultat.changerExpression(PersonnageSVG.Expression.CONTENT);
            labelResultatTitre.setText("🎉 Excellent !");
            labelResultatTitre.setStyle("-fx-font-size:32px;-fx-font-weight:bold;"
                    + "-fx-text-fill:#059669;");
        } else if (pourcentage >= 50) {
            persoResultat.changerExpression(PersonnageSVG.Expression.NEUTRE);
            labelResultatTitre.setText("👍 Bien joué !");
            labelResultatTitre.setStyle("-fx-font-size:32px;-fx-font-weight:bold;"
                    + "-fx-text-fill:#6C63FF;");
        } else {
            persoResultat.changerExpression(PersonnageSVG.Expression.TRISTE);
            labelResultatTitre.setText("📚 À améliorer");
            labelResultatTitre.setStyle("-fx-font-size:32px;-fx-font-weight:bold;"
                    + "-fx-text-fill:#DC2626;");
        }

        // Animer le score (compte de 0 à la valeur finale)
        final int scoreFinal = (int) Math.round(pourcentage);
        final int[] compteur = {0};
        Timeline scoreAnim = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            if (compteur[0] < scoreFinal) {
                labelScoreFinal.setText(++compteur[0] + "%");
            }
        }));
        scoreAnim.setCycleCount(scoreFinal);
        scoreAnim.play();

        String niveau = pourcentage >= 90 ? "C2"
                : pourcentage >= 80 ? "C1"
                : pourcentage >= 70 ? "B2"
                : pourcentage >= 60 ? "B1"
                : pourcentage >= 50 ? "A2" : "A1";
        labelNiveauEstime.setText("Niveau estimé : " + niveau);

        int m = secondesEcoulees / 60, s = secondesEcoulees % 60;
        labelTempsTotal.setText(String.format("Temps : %02d min %02d s", m, s));
    }

    @FXML private void handleRecommencer() {
        reponsesChoisies.clear();
        scoresObtenus    = new double[questions.size()];
        statusParQuestion = new String[questions.size()];
        secondesEcoulees = 0;
        dateDebut        = LocalDateTime.now();

        panelResultat.setVisible(false);
        panelResultat.setManaged(false);

        // Recréer le personnage
        PersonnageSVG nouveauPerso = new PersonnageSVG();
        personnageSVG = nouveauPerso;
        Node pNode = personnageSVG.creerPersonnage(nomLangue);
        panelPersonnage.getChildren().setAll(pNode);

        for (Question q : questions) reponsesChoisies.add(null);

        demarrerTimer();
        afficherQuestion(0);
    }

    @FXML private void handleRetourAccueil() {
        personnageSVG.arreterAnimations();
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
        }
    }

    // ── Styles boutons réponse ────────────────────────────

    private String styleReponseNormal() {
        return """
            -fx-background-color:white;
            -fx-border-color:#D0CBFF;
            -fx-border-width:2;
            -fx-border-radius:14;
            -fx-background-radius:14;
            -fx-font-size:14px;
            -fx-text-fill:#1A1D2E;
            -fx-padding:14 20;
            -fx-cursor:hand;
            -fx-alignment:CENTER_LEFT;
            """;
    }

    private String styleReponseHover() {
        return """
            -fx-background-color:#EEF0FF;
            -fx-border-color:#6C63FF;
            -fx-border-width:2.5;
            -fx-border-radius:14;
            -fx-background-radius:14;
            -fx-font-size:14px;
            -fx-font-weight:bold;
            -fx-text-fill:#3730A3;
            -fx-padding:14 20;
            -fx-cursor:hand;
            -fx-alignment:CENTER_LEFT;
            """;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}