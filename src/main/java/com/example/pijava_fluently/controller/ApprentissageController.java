package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.QuizQuestion;
import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.services.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import java.awt.Desktop;
import java.io.File;
import java.util.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class ApprentissageController {

    @FXML private Label langueNom;
    @FXML private Label langueDescription;
    @FXML private Label niveauActuelValue;

    // Niveaux
    @FXML private VBox niveauA1, niveauA2, niveauB1, niveauB2, niveauC1, niveauC2;
    @FXML private HBox coursA1, coursA2, coursB1, coursB2, coursC1, coursC2;
    @FXML private Label progressA1, progressA2, progressB1, progressB2, progressC1, progressC2;

    // Formulaire de génération de cours
    @FXML private VBox formCoursContainer;
    @FXML private ComboBox<String> comboTheme;
    @FXML private ComboBox<String> comboGrammaire;
    @FXML private TextField fieldVocabulaire;
    @FXML private Slider sliderDifficulte;
    @FXML private TextArea apercuCours;
    @FXML private Button btnExporterPDF;
    @FXML private TextField fieldTheme;
    @FXML private TextField fieldGrammaire;
    @FXML private Label niveauSelectionneLabel;

    // Cours PDF
    @FXML private HBox coursPdfContainer;
    @FXML private ScrollPane coursPdfScroll;
    @FXML private Label totalCoursCount;
    @FXML private VBox emptyCoursMessage;

    // YouTube
    @FXML private TextField youtubeSearchField;
    @FXML private HBox youtubeLoading;
    @FXML private ScrollPane youtubeScrollPane;
    @FXML private VBox youtubeResultsContainer;
    @FXML private VBox youtubeEmptyMessage;

    // Dictionnaire
    @FXML private TextField dictionarySearchField;
    @FXML private HBox dictionaryLoading;
    @FXML private VBox dictionaryResultContainer;
    @FXML private Label dictionaryWord;
    @FXML private Label dictionaryDefinition;
    @FXML private Label dictionaryExample;
    @FXML private Label dictionarySynonym;
    @FXML private VBox dictionaryEmptyMessage;

    // Jeu éducatif
    @FXML private ComboBox<String> gameTypeCombo;
    @FXML private ComboBox<String> gameLevelCombo;
    @FXML private Button startGameBtn;
    @FXML private HBox gameLoading;
    @FXML private VBox quizContainer;

    @FXML private VBox matchContainer;

    @FXML private VBox matchWordsContainer;
    @FXML private VBox matchDefsContainer;
    @FXML private Label matchScore;
    @FXML private Label quizScore;
    @FXML private ProgressBar quizProgress;
    @FXML private Label quizQuestion;
    @FXML private VBox quizAnswersContainer;
    @FXML private Button nextQuestionBtn;
    @FXML private VBox quizResultContainer;
    @FXML private Label quizFinalScore;
    @FXML private Label quizFeedback;
    @FXML private ProgressIndicator loadingIA;
    @FXML private Label loadingText;

    // Jeu "Compléter la phrase"
    @FXML private VBox fillGameContainer;
    @FXML private Label fillScore;
    @FXML private ProgressBar fillProgress;
    @FXML private Label fillQuestion;
    @FXML private VBox fillOptionsContainer;
    @FXML private Button fillNextBtn;
    @FXML private VBox fillResultContainer;
    @FXML private Label fillFinalScore;
    @FXML private Label fillFeedback;
    // Prononciation
    @FXML private TextField pronunciationTextField;
    @FXML private HBox pronunciationLoading;

    // Variables pour le jeu
    private List<FillQuestion> fillQuestionsList = new ArrayList<>();
    private int currentFillIndex = 0;
    private int currentFillScore = 0;
    // Variables

    private final PrononciationService prononciationService = new PrononciationService();
    private Map<String, Integer> selectedMatches;
    private List<QuizQuestion> questionsList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int currentScore = 0;
    private ToggleGroup currentToggleGroup;
    private int motSelectionneIndex = -1;
    private Button motSelectionneBtn = null;

    private final YouTubeService youTubeService = new YouTubeService();
    private Map<String, List<Cours>> coursPdfParNiveau = new HashMap<>();
    private String niveauSelectionne = null;
    private String dernierCoursGenere;
    private Langue langue;
    private HomeController homeController;
    private Map<String, List<Cours>> coursParNiveau = new HashMap<>();
    private Map<String, Integer> progressionParNiveau = new HashMap<>();
    private Map<String, Integer> coursCompleteParNiveau = new HashMap<>();
    private Map<Integer, Niveau> niveauParDifficulte = new HashMap<>();
    private Map<String, Integer> niveauIdParKey = new HashMap<>();
    private Map<String, Map<Integer, Boolean>> progressionCoursParNiveau = new HashMap<>();

    // Flashcards
    @FXML private VBox flashcardsFormContainer;
    @FXML private VBox flashcardsSessionContainer;
    @FXML private VBox flashcardResultContainer;
    @FXML private TextArea flashcardsPromptArea;
    @FXML private ComboBox<String> flashcardsLevelCombo;
    @FXML private HBox flashcardsLoading;
    @FXML private Label flashcardProgressLabel;
    @FXML private Label flashcardScoreLabel;
    @FXML private Label flashcardQuestionLabel;
    @FXML private VBox flashcardOptionsContainer;
    @FXML private Button flashcardValidateBtn;
    @FXML private Button flashcardNextBtn;
    @FXML private TextArea flashcardExplanationArea;
    @FXML private TextArea flashcardResultArea;

    // Variables pour la session flashcards
    private List<Flashcard> currentFlashcards = new ArrayList<>();
    private int currentFlashcardIndex = 0;
    private int currentFlashcardScore = 0;
    private ToggleGroup flashcardToggleGroup;
    private List<Boolean> flashcardUserAnswers = new ArrayList<>();
    private List<Integer> flashcardUserSelections = new ArrayList<>();
    private User currentUser;
    private UserProgressService userProgressService = new UserProgressService();
    private final IAService iaService = new IAService();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ Utilisateur connecté dans ApprentissageController : " + user.getEmail());
        chargerNiveauActuel();
    }

    public void setLangue(Langue langue) {
        this.langue = langue;
        langueNom.setText(langue.getNom());
        langueDescription.setText(langue.getDescription());
        chargerCours();
    }

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void handleRetour() {
        if (homeController != null) {
            homeController.showLangues();
        }
    }

    @FXML
    private void handleTest() {
        if (currentUser == null) {
            showAlert("Erreur", "Vous devez être connecté pour passer le test.");
            return;
        }

        try {
            TestService testService = new TestService();
            List<Test> tests = testService.recuperer();
            List<Test> testsNiveau = tests.stream()
                    .filter(t -> t.getLangueId() == langue.getId()
                            && t.getType().equals("Test de niveau"))
                    .collect(Collectors.toList());

            if (testsNiveau.isEmpty()) {
                showAlert("Information",
                        "Aucun test de niveau disponible pour " + langue.getNom() + ".");
                return;
            }

            Test testChoisi = testsNiveau.get(0);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/test-passage.fxml"));
            Node vue = loader.load();
            TestPassageEtudiantController ctrl = loader.getController();
            ctrl.initTest(testChoisi, currentUser); // ← utilise la surcharge User

            if (homeController != null) {
                homeController.setContent(vue);
            }
        } catch (SQLException | java.io.IOException e) {
            showAlert("Erreur", "Impossible de charger le test : " + e.getMessage());
        }
    }

    private void chargerNiveauActuel() {
        if (currentUser == null || langue == null) {
            niveauActuelValue.setText("Non défini");
            return;
        }

        try {
            TestService testService   = new TestService();
            TestPassageService tps    = new TestPassageService();

            // IDs des tests de niveau pour CETTE langue uniquement
            List<Integer> idsTestsNiveau = testService.recuperer().stream()
                    .filter(t -> t.getLangueId() == langue.getId()
                            && "Test de niveau".equals(t.getType()))
                    .map(Test::getId)
                    .collect(Collectors.toList());

            if (idsTestsNiveau.isEmpty()) {
                niveauActuelValue.setText("Non défini");
                return;
            }

            // Dernier passage terminé de CET utilisateur pour UN de ces tests
            Optional<TestPassage> meilleur = tps.recuperer().stream()
                    .filter(p -> p.getUserId() == currentUser.getId())
                    .filter(p -> "termine".equals(p.getStatut()))
                    .filter(p -> idsTestsNiveau.contains(p.getTestId()))
                    .max(Comparator.comparing(p ->
                            p.getDateDebut() != null ? p.getDateDebut() : LocalDateTime.MIN));

            if (meilleur.isPresent()) {
                double pct = meilleur.get().getScoreMax() > 0
                        ? (double) meilleur.get().getScore() / meilleur.get().getScoreMax() * 100
                        : 0;
                niveauActuelValue.setText(determinerNiveauParScore(pct));
            } else {
                niveauActuelValue.setText("Non défini");
            }

        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            niveauActuelValue.setText("Non défini");
        }
    }

    private String determinerNiveauParScore(double pourcentage) {
        if (pourcentage >= 90) return "C2";
        if (pourcentage >= 80) return "C1";
        if (pourcentage >= 70) return "B2";
        if (pourcentage >= 60) return "B1";
        if (pourcentage >= 50) return "A2";
        return "A1";
    }

    private void chargerCours() {
        new Thread(() -> {
            try {
                NiveauService niveauService = new NiveauService();
                List<Niveau> niveaux = niveauService.recuperer();

                List<Niveau> niveauxLangue = niveaux.stream()
                        .filter(n -> n.getIdLangueId() == langue.getId())
                        .collect(Collectors.toList());

                // Stocker les IDs des niveaux par clé
                for (Niveau niveau : niveauxLangue) {
                    String difficulte = niveau.getDifficulte();
                    String niveauKey = extractNiveauKey(difficulte);
                    if (niveauKey != null) {
                        niveauIdParKey.put(niveauKey, niveau.getId());
                        niveauParDifficulte.put(niveauKey.hashCode(), niveau);
                    }
                }

                CoursService coursService = new CoursService();
                List<Cours> tousLesCours = coursService.recuperer();

                for (Niveau niveau : niveauxLangue) {
                    String difficulte = niveau.getDifficulte();
                    String niveauKey = extractNiveauKey(difficulte);

                    if (niveauKey != null) {
                        List<Cours> coursNiveau = tousLesCours.stream()
                                .filter(c -> c.getIdNiveauId() == niveau.getId())
                                .sorted((c1, c2) -> Integer.compare(c1.getNumero(), c2.getNumero()))
                                .collect(Collectors.toList());
                        coursParNiveau.put(niveauKey, coursNiveau);
                    }
                }

                chargerProgressionUtilisateur();
                Platform.runLater(() -> afficherNiveaux());

            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de charger les cours"));
            }
        }).start();
    }


    private void chargerProgressionUtilisateur() {
        if (currentUser == null) return;

        try {
            List<User_progress> progresses = userProgressService.recuperer();

            // Réinitialiser les maps
            coursCompleteParNiveau.clear();
            progressionParNiveau.clear();
            progressionCoursParNiveau.clear();

            // Pour chaque niveau, charger sa propre progression
            for (Map.Entry<String, List<Cours>> entry : coursParNiveau.entrySet()) {
                String niveauKey = entry.getKey();
                List<Cours> cours = entry.getValue();

                // Récupérer l'ID du niveau pour cette clé
                Integer niveauId = niveauIdParKey.get(niveauKey);
                if (niveauId == null) {
                    progressionParNiveau.put(niveauKey, 0);
                    progressionCoursParNiveau.put(niveauKey, new HashMap<>());
                    continue;
                }

                // Chercher la progression pour CE niveau spécifique (via niveau_actuel_id)
                User_progress progress = progresses.stream()
                        .filter(p -> p.getUserId() == currentUser.getId()
                                && p.getLangueId() == langue.getId()
                                && p.getNiveauActuelId() == niveauId)
                        .findFirst()
                        .orElse(null);

                Map<Integer, Boolean> coursStatus = new HashMap<>();
                int completedCount = 0;

                if (progress != null) {
                    // Utiliser le dernierNumeroCours de CE niveau
                    int dernierCours = progress.getDernierNumeroCours();
                    for (Cours c : cours) {
                        boolean estComplete = c.getNumero() <= dernierCours;
                        if (estComplete) {
                            completedCount++;
                            coursCompleteParNiveau.put(niveauKey + "_" + c.getId(), c.getId());
                        }
                        coursStatus.put(c.getId(), estComplete);
                    }
                } else {
                    // Aucun cours complété pour ce niveau
                    for (Cours c : cours) {
                        coursStatus.put(c.getId(), false);
                    }
                }
                progressionCoursParNiveau.put(niveauKey, coursStatus);
                progressionParNiveau.put(niveauKey, completedCount);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de la progression: " + e.getMessage());
        }
    }

    private void marquerCoursComplete(Cours cours) {
        if (currentUser == null) {
            showAlert("Erreur", "Vous devez être connecté.");
            return;
        }

        try {
            // Déterminer le niveau du cours
            String niveauKey = null;
            for (Map.Entry<String, List<Cours>> entry : coursParNiveau.entrySet()) {
                if (entry.getValue().contains(cours)) {
                    niveauKey = entry.getKey();
                    break;
                }
            }

            if (niveauKey == null) {
                showAlert("Erreur", "Niveau non trouvé pour ce cours.");
                return;
            }

            // Vérifier si déjà complété dans ce niveau
            Map<Integer, Boolean> coursStatus = progressionCoursParNiveau.getOrDefault(niveauKey, new HashMap<>());
            if (coursStatus.getOrDefault(cours.getId(), false)) {
                showAlert("Information", "Ce cours a déjà été complété !");
                return;
            }

            // Mettre à jour la progression en mémoire
            coursStatus.put(cours.getId(), true);
            progressionCoursParNiveau.put(niveauKey, coursStatus);

            // Compter les cours complétés dans ce niveau
            int completedCount = 0;
            for (Boolean status : coursStatus.values()) {
                if (status) completedCount++;
            }
            progressionParNiveau.put(niveauKey, completedCount);
            coursCompleteParNiveau.put(niveauKey + "_" + cours.getId(), cours.getId());

            // Récupérer l'ID du niveau
            Integer niveauId = niveauIdParKey.get(niveauKey);
            if (niveauId == null) {
                showAlert("Erreur", "ID du niveau non trouvé.");
                return;
            }

            // Chercher par user_id, langue_id ET niveau_actuel_id
            List<User_progress> progresses = userProgressService.recuperer();
            User_progress existingProgress = progresses.stream()
                    .filter(p -> p.getUserId() == currentUser.getId()
                            && p.getLangueId() == langue.getId()
                            && p.getNiveauActuelId() == niveauId)
                    .findFirst()
                    .orElse(null);

            if (existingProgress == null) {
                // Créer une NOUVELLE progression pour CE niveau
                User_progress newProgress = new User_progress();
                newProgress.setUserId(currentUser.getId());
                newProgress.setLangueId(langue.getId());
                newProgress.setNiveauActuelId(niveauId);  // ID du niveau
                newProgress.setDernierNumeroCours(completedCount);
                newProgress.setDernierCoursCompleteId(cours.getId());
                newProgress.setTestNiveauComplete(false);
                newProgress.setDateDerniereActivite(LocalDateTime.now());

                userProgressService.ajouter(newProgress);
            } else {
                // Mettre à jour la progression EXISTANTE pour CE niveau
                existingProgress.setDernierNumeroCours(completedCount);
                existingProgress.setDernierCoursCompleteId(cours.getId());
                existingProgress.setDateDerniereActivite(LocalDateTime.now());
                userProgressService.modifier(existingProgress);
            }

            afficherNiveaux();
            showAlert("Succès", "Félicitations ! Vous avez complété le cours " + cours.getNumero() + " du niveau " + niveauKey);

        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour: " + e.getMessage());
            showAlert("Erreur", "Impossible d'enregistrer la progression.");
        }
    }

    private void afficherCoursPourNiveau(String niveauKey, HBox container, Label progressLabel) {
        container.getChildren().clear();

        List<Cours> cours = coursParNiveau.get(niveauKey);
        if (cours == null || cours.isEmpty()) {
            Label emptyLabel = new Label("Aucun cours disponible");
            emptyLabel.setStyle("-fx-text-fill:#8A8FA8;-fx-font-size:12px;");
            container.getChildren().add(emptyLabel);
            if (progressLabel != null) progressLabel.setText("0/0 cours");
            return;
        }

        int completed = progressionParNiveau.getOrDefault(niveauKey, 0);
        if (progressLabel != null) {
            progressLabel.setText(completed + "/" + cours.size() + " cours");
        }

        int maxCours = Math.min(cours.size(), 6);
        for (int i = 0; i < maxCours; i++) {
            Cours c = cours.get(i);
            // Vérifier si ce cours est complété dans SON niveau
            Map<Integer, Boolean> coursStatus = progressionCoursParNiveau.getOrDefault(niveauKey, new HashMap<>());
            boolean estComplete = coursStatus.getOrDefault(c.getId(), false);
            VBox cercle = createCercleCours(c, i + 1, estComplete);
            container.getChildren().add(cercle);
        }
    }

    private String extractNiveauKey(String difficulte) {
        if (difficulte == null) return null;
        if (difficulte.contains("A1")) return "A1";
        if (difficulte.contains("A2")) return "A2";
        if (difficulte.contains("B1")) return "B1";
        if (difficulte.contains("B2")) return "B2";
        if (difficulte.contains("C1")) return "C1";
        if (difficulte.contains("C2")) return "C2";
        return null;
    }

    private void afficherNiveaux() {
        afficherCoursPourNiveau("A1", coursA1, progressA1);
        afficherCoursPourNiveau("A2", coursA2, progressA2);
        afficherCoursPourNiveau("B1", coursB1, progressB1);
        afficherCoursPourNiveau("B2", coursB2, progressB2);
        afficherCoursPourNiveau("C1", coursC1, progressC1);
        afficherCoursPourNiveau("C2", coursC2, progressC2);
        // Afficher les cours PDF générés
        afficherCoursPdfGeneres();
    }


    private VBox createCercleCours(Cours cours, int numero, boolean estComplete) {
        VBox cercle = new VBox();
        cercle.setAlignment(Pos.CENTER);
        cercle.setSpacing(10);
        cercle.setPrefWidth(90);
        cercle.setPrefHeight(110);
        cercle.setStyle("-fx-cursor: hand;");

        StackPane circle = new StackPane();
        circle.setPrefSize(65, 65);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(8);
        shadow.setColor(Color.rgb(108, 99, 255, 0.3));
        shadow.setOffsetY(2);
        circle.setEffect(shadow);

        if (estComplete) {
            circle.setStyle("-fx-background-color: radial-gradient(radius 100%, #10B981, #059669); -fx-background-radius: 35;");
            Label checkLabel = new Label("✓");
            checkLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
            circle.getChildren().add(checkLabel);
        } else {
            circle.setStyle("-fx-background-color: radial-gradient(radius 100%, #6C63FF, #8B5CF6); -fx-background-radius: 35;");
            Label numberLabel = new Label(String.valueOf(numero));
            numberLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
            circle.getChildren().add(numberLabel);
        }

        Label coursLabel = new Label("Cours " + numero);
        coursLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4A4D6A;");

        if (estComplete) {
            Label completedBadge = new Label("✅ Complété");
            completedBadge.setStyle("-fx-font-size: 9px; -fx-text-fill: #059669; -fx-font-weight: bold;");
            cercle.getChildren().addAll(circle, coursLabel, completedBadge);
        } else {
            // Ajouter le bouton "Terminer" pour les cours non complétés
            Button terminerBtn = new Button("Terminer");
            terminerBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
            terminerBtn.setOnMouseEntered(e -> terminerBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 12 5 12; -fx-cursor: hand;"));
            terminerBtn.setOnMouseExited(e -> terminerBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 12 5 12; -fx-cursor: hand;"));
            terminerBtn.setOnAction(e -> marquerCoursComplete(cours));
            cercle.getChildren().addAll(circle, coursLabel, terminerBtn);
        }

        // Effet hover
        cercle.setOnMouseEntered(e -> {
            if (!estComplete) {
                circle.setEffect(new DropShadow(12, Color.rgb(108, 99, 255, 0.5)));
                circle.setScaleX(1.05);
                circle.setScaleY(1.05);
                cercle.setTranslateY(-4);
            } else {
                circle.setEffect(new DropShadow(12, Color.rgb(16, 185, 129, 0.4)));
                circle.setScaleX(1.03);
                circle.setScaleY(1.03);
            }
        });

        cercle.setOnMouseExited(e -> {
            if (!estComplete) {
                circle.setEffect(new DropShadow(8, Color.rgb(108, 99, 255, 0.3)));
                circle.setScaleX(1.0);
                circle.setScaleY(1.0);
                cercle.setTranslateY(0);
            } else {
                circle.setEffect(new DropShadow(8, Color.rgb(16, 185, 129, 0.2)));
                circle.setScaleX(1.0);
                circle.setScaleY(1.0);
            }
        });

        // Action au clic sur TOUT le cercle (ouvre le cours)
        cercle.setOnMouseClicked(e -> {
            ouvrirCours(cours);  // ← TOUJOURS ouvrir le cours, qu'il soit complété ou non
        });

        return cercle;
    }



    private int trouverNiveauId(String niveauKey) {
        // Chercher le niveau par sa difficulté
        for (Map.Entry<Integer, Niveau> entry : niveauParDifficulte.entrySet()) {
            if (entry.getValue().getDifficulte().contains(niveauKey)) {
                return entry.getValue().getId();
            }
        }
        return 1; // Valeur par défaut
    }

    private void ouvrirCours(Cours cours) {
        if (cours.getRessource() == null || cours.getRessource().isEmpty()) {
            showAlert("Information", "Aucune ressource disponible pour ce cours.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cours N°" + cours.getNumero());
        dialog.setHeaderText(null);

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setPrefWidth(600);
        content.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 20;");

        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setStyle("-fx-background-color: #6C63FF; -fx-background-radius: 15; -fx-padding: 15 20 15 20;");

        Label titleIcon = new Label("📚");
        titleIcon.setStyle("-fx-font-size: 24px;");

        Label titleLabel = new Label("Cours N°" + cours.getNumero());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        titleBox.getChildren().addAll(titleIcon, titleLabel);

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #E0E3F0;");

        Label categoriesTitle = new Label("📎 Ressources disponibles");
        categoriesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E; -fx-padding: 0 0 10 0;");

        VBox categoriesContainer = new VBox(15);

        String[] ressources = cours.getRessource().split("\n");

        List<String> youtubeLinks = new ArrayList<>();
        List<String> pdfFiles = new ArrayList<>();
        List<String> videoFiles = new ArrayList<>();
        List<String> audioFiles = new ArrayList<>();
        List<String> imageFiles = new ArrayList<>();
        List<String> otherFiles = new ArrayList<>();

        for (String res : ressources) {
            String lowerRes = res.toLowerCase();
            if (lowerRes.contains("youtube.com") || lowerRes.contains("youtu.be")) {
                youtubeLinks.add(res);
            } else if (lowerRes.endsWith(".pdf")) {
                pdfFiles.add(res);
            } else if (lowerRes.endsWith(".mp4") || lowerRes.endsWith(".webm") || lowerRes.endsWith(".mov")) {
                videoFiles.add(res);
            } else if (lowerRes.endsWith(".mp3") || lowerRes.endsWith(".wav") || lowerRes.endsWith(".ogg")) {
                audioFiles.add(res);
            } else if (lowerRes.endsWith(".png") || lowerRes.endsWith(".jpg") || lowerRes.endsWith(".jpeg") || lowerRes.endsWith(".gif") || lowerRes.endsWith(".webp")) {
                imageFiles.add(res);
            } else {
                otherFiles.add(res);
            }
        }

        if (!youtubeLinks.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🎬 Vidéos YouTube", youtubeLinks, "#FF6B6B"));
        }
        if (!pdfFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("📄 Documents PDF", pdfFiles, "#4ECDC4"));
        }
        if (!videoFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🎥 Vidéos", videoFiles, "#A8E6CF"));
        }
        if (!audioFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🎵 Audios", audioFiles, "#FFD93D"));
        }
        if (!imageFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("🖼️ Images", imageFiles, "#C5E8F7"));
        }
        if (!otherFiles.isEmpty()) {
            categoriesContainer.getChildren().add(createCategoryBox("📎 Autres fichiers", otherFiles, "#D4A5A5"));
        }

        if (categoriesContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("📭 Aucune ressource disponible");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8A8FA8; -fx-padding: 20;");
            emptyLabel.setAlignment(Pos.CENTER);
            categoriesContainer.getChildren().add(emptyLabel);
        }

        ScrollPane scrollRessources = new ScrollPane(categoriesContainer);
        scrollRessources.setFitToWidth(true);
        scrollRessources.setPrefHeight(400);
        scrollRessources.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        content.getChildren().addAll(titleBox, separator, categoriesTitle, scrollRessources);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 20;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setText("Fermer");
        closeButton.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 25 10 25; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    private VBox createCategoryBox(String categoryName, List<String> items, String color) {
        VBox categoryBox = new VBox(10);
        categoryBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2); -fx-padding: 15;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label categoryIcon = new Label(categoryName.substring(0, 2));
        categoryIcon.setStyle("-fx-font-size: 18px;");

        Label categoryLabel = new Label(categoryName);
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");

        Label countBadge = new Label(String.valueOf(items.size()));
        countBadge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 3 10 3 10;");

        header.getChildren().addAll(categoryIcon, categoryLabel, countBadge);
        HBox.setHgrow(categoryLabel, Priority.ALWAYS);

        VBox itemsBox = new VBox(8);

        for (String item : items) {
            HBox itemRow = new HBox(12);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 10; -fx-padding: 10 15 10 15;");
            itemRow.setEffect(new DropShadow(2, Color.rgb(0, 0, 0, 0.05)));

            Label itemIcon = new Label();
            if (item.contains("youtube.com") || item.contains("youtu.be")) {
                itemIcon.setText("🎬");
            } else if (item.toLowerCase().endsWith(".pdf")) {
                itemIcon.setText("📄");
            } else if (item.toLowerCase().endsWith(".mp4") || item.toLowerCase().endsWith(".webm")) {
                itemIcon.setText("🎥");
            } else if (item.toLowerCase().endsWith(".mp3") || item.toLowerCase().endsWith(".wav")) {
                itemIcon.setText("🎵");
            } else if (item.toLowerCase().endsWith(".png") || item.toLowerCase().endsWith(".jpg")) {
                itemIcon.setText("🖼️");
            } else {
                itemIcon.setText("📎");
            }
            itemIcon.setStyle("-fx-font-size: 18px;");

            String fileName = item;
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            if (fileName.length() > 40) {
                fileName = fileName.substring(0, 37) + "...";
            }

            Label itemLabel = new Label(fileName);
            itemLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A4D6A;");
            itemLabel.setMaxWidth(350);
            itemLabel.setWrapText(true);

            Button openBtn = new Button("Ouvrir");
            openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;");
            openBtn.setOnMouseEntered(e -> openBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;"));
            openBtn.setOnMouseExited(e -> openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;"));
            openBtn.setOnAction(e -> ouvrirRessource(item));

            itemRow.getChildren().addAll(itemIcon, itemLabel, openBtn);
            HBox.setHgrow(itemLabel, Priority.ALWAYS);

            itemsBox.getChildren().add(itemRow);
        }

        categoryBox.getChildren().addAll(header, itemsBox);
        return categoryBox;
    }

    private void ouvrirRessource(String chemin) {
        try {
            if (chemin.contains("youtube.com") || chemin.contains("youtu.be")) {
                Desktop.getDesktop().browse(new URI(chemin));
            } else {
                File file = new File(chemin);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("Erreur", "Fichier introuvable : " + chemin);
                }
            }
        } catch (IOException | URISyntaxException e) {
            showAlert("Erreur", "Impossible d'ouvrir la ressource");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    @FXML
    private void ouvrirFormulaireCours() {
        formCoursContainer.setVisible(true);
        formCoursContainer.setManaged(true);
    }

    @FXML
    private void fermerFormulaireCours() {
        formCoursContainer.setVisible(false);
        formCoursContainer.setManaged(false);
        effacerFormulaire();
    }
    @FXML
    private void effacerFormulaire() {
        fieldTheme.clear();           // Nouveau
        fieldGrammaire.clear();       // Nouveau
        fieldVocabulaire.clear();
        niveauSelectionne = null;
        niveauSelectionneLabel.setText("Niveau sélectionné : Aucun");
        resetNiveauStyles();          // Reset des styles
        apercuCours.clear();
        btnExporterPDF.setDisable(true);
        dernierCoursGenere = null;
    }

    @FXML
    private void genererCours() {
        // Validation des champs (nouveaux)
        if (fieldTheme.getText().trim().isEmpty()) {
            showAlert("Information", "Veuillez entrer un thème.");
            return;
        }
        if (fieldGrammaire.getText().trim().isEmpty()) {
            showAlert("Information", "Veuillez entrer un point de grammaire.");
            return;
        }
        if (niveauSelectionne == null) {
            showAlert("Information", "Veuillez sélectionner un niveau (A1 à C2).");
            return;
        }

        String theme = fieldTheme.getText().trim();
        String grammaire = fieldGrammaire.getText().trim();
        String vocabulaire = fieldVocabulaire.getText().trim();
        int difficulte = convertirNiveauEnDifficulte(niveauSelectionne);
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";

        // Désactiver le bouton pendant la génération
        btnExporterPDF.setDisable(true);
        showLoading(true);

        new Thread(() -> {
            try {
                String coursGenere = iaService.genererCours(
                        langueNom, theme, grammaire,
                        vocabulaire.isEmpty() ? "vocabulaire général du thème" : vocabulaire,
                        difficulte
                );

                Platform.runLater(() -> {
                    apercuCours.setText(coursGenere);
                    dernierCoursGenere = coursGenere;
                    btnExporterPDF.setDisable(false);
                    showLoading(false);
                    showAlert("Succès", "✅ Votre cours personnalisé a été généré par l'IA !");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    genererCoursLocal(theme, grammaire, vocabulaire, difficulte, langueNom);
                });
            }
        }).start();
    }

    private int convertirNiveauEnDifficulte(String niveau) {
        switch (niveau) {
            case "A1": return 1;
            case "A2": return 2;
            case "B1": return 3;
            case "B2": return 4;
            case "C1": return 4;
            case "C2": return 5;
            default: return 3;
        }
    }

    // Fallback local (votre ancienne méthode) en cas de panne IA
    private void genererCoursLocal(String theme, String grammaire, String vocabulaire,
                                   int difficulte, String langueNom) {
        StringBuilder cours = new StringBuilder();
        cours.append("⚠️ MODE DÉGRADÉ - GÉNÉRATION LOCALE ⚠️\n\n");
        cours.append("L'IA n'est pas disponible. Voici un cours basique.\n\n");

        cours.append("=".repeat(60)).append("\n");
        cours.append("📚 COURS PERSONNALISÉ - ").append(langueNom.toUpperCase()).append("\n");
        cours.append("=".repeat(60)).append("\n\n");

        cours.append("🎯 1. INTRODUCTION\n");
        cours.append("Thème : ").append(theme).append("\n");
        cours.append("Grammaire : ").append(grammaire).append("\n\n");

        cours.append("📝 2. VOCABULAIRE\n");
        if (vocabulaire.isEmpty()) {
            cours.append("- Mot 1\n- Mot 2\n- Mot 3\n");
        } else {
            String[] mots = vocabulaire.split(",");
            for (String mot : mots) {
                cours.append("- ").append(mot.trim()).append("\n");
            }
        }

        cours.append("\n📖 3. GRAMMAIRE : ").append(grammaire).append("\n");
        cours.append("Révisez la règle de grammaire dans votre manuel.\n\n");

        cours.append("✏️ 4. EXERCICES\n");
        cours.append("1. Créez 5 phrases avec le vocabulaire appris\n");
        cours.append("2. Conjuguez 3 verbes au ").append(grammaire).append("\n");

        apercuCours.setText(cours.toString());
        dernierCoursGenere = cours.toString();
        btnExporterPDF.setDisable(false);
        showAlert("Information", "Cours local généré (IA non disponible)");
    }

    private void showLoading(boolean show) {
        if (loadingIA != null) {
            loadingIA.setVisible(show);
            loadingIA.setManaged(show);
        }
        if (loadingText != null) {
            loadingText.setVisible(show);
            loadingText.setManaged(show);
            if (show) {
                loadingText.setText("🤖 L'IA génère votre cours personnalisé... (10-15 secondes)");
            }
        }
    }
    private String[] genererVocabulaireParTheme(String theme, String langue) {
        switch (theme) {
            case "Voyage et Tourisme":
                return new String[]{"L'aéroport", "Le billet d'avion", "La valise", "Le passeport",
                        "L'hôtel", "La réservation", "La chambre", "La destination",
                        "Le touriste", "La carte touristique"};
            case "Affaires et Travail":
                return new String[]{"La réunion", "Le collègue", "Le patron", "L'entreprise",
                        "Le contrat", "Le projet", "L'échéance", "La négociation",
                        "Le client", "Le rapport"};
            case "Culture et Arts":
                return new String[]{"Le musée", "L'exposition", "Le tableau", "Le peintre",
                        "La sculpture", "Le concert", "Le théâtre", "La littérature",
                        "Le poème", "L'œuvre d'art"};
            case "Vie quotidienne":
                return new String[]{"La maison", "La famille", "Les courses", "Le travail",
                        "Les loisirs", "La cuisine", "Le sommeil", "Les transports",
                        "Les amis", "Les activités"};
            case "Nourriture et Restaurant":
                return new String[]{"Le menu", "L'entrée", "Le plat principal", "Le dessert",
                        "La carte des vins", "Le serveur", "La réservation",
                        "L'addition", "Le pourboire", "Les spécialités"};
            default:
                return new String[]{"Le mot 1", "Le mot 2", "Le mot 3", "Le mot 4", "Le mot 5"};
        }
    }

    private String genererExplicationGrammaire(String grammaire, String langue) {
        switch (grammaire) {
            case "Présent de l'indicatif":
                return "Le présent de l'indicatif est utilisé pour exprimer des actions qui se déroulent au moment où l'on parle, des vérités générales ou des habitudes.\n\n" +
                        "Exemple avec le verbe 'parler' :\n" +
                        "- Je parle\n- Tu parles\n- Il/Elle parle\n- Nous parlons\n- Vous parlez\n- Ils/Elles parlent";
            case "Passé composé":
                return "Le passé composé est utilisé pour exprimer une action achevée dans le passé.\n\n" +
                        "Il se forme avec l'auxiliaire 'avoir' ou 'être' au présent + le participe passé du verbe.\n\n" +
                        "Exemple : 'J'ai mangé', 'Tu es allé(e)'";
            case "Futur simple":
                return "Le futur simple exprime une action qui se déroulera dans l'avenir.\n\n" +
                        "Il se forme en ajoutant les terminaisons -ai, -as, -a, -ons, -ez, -ont à l'infinitif du verbe.\n\n" +
                        "Exemple : 'Je mangerai', 'Tu finiras'";
            default:
                return "Explication détaillée du point de grammaire " + grammaire + " avec des exemples concrets pour bien comprendre son utilisation.";
        }
    }

    private String genererExercices(String theme, String grammaire, int difficulte) {
        StringBuilder exos = new StringBuilder();

        exos.append("Exercice 1 : Complétez les phrases avec le vocabulaire du thème '" + theme + "'\n");
        exos.append("------------------------------------------------------------\n");

        switch (theme) {
            case "Voyage et Tourisme":
                exos.append("1. Je dois prendre mon ________ pour partir en vacances.\n");
                exos.append("2. Nous avons réservé une ________ à l'hôtel.\n");
                exos.append("3. N'oubliez pas votre ________ pour passer la douane.\n");
                break;
            case "Affaires et Travail":
                exos.append("1. La ________ est prévue à 14h dans la salle de conférence.\n");
                exos.append("2. Je dois rendre mon ________ avant vendredi.\n");
                exos.append("3. Mon ________ est très satisfait de mon travail.\n");
                break;
            default:
                exos.append("1. Complétez avec un mot du vocabulaire : __________\n");
                exos.append("2. Complétez avec un mot du vocabulaire : __________\n");
                exos.append("3. Complétez avec un mot du vocabulaire : __________\n");
        }

        exos.append("\nExercice 2 : Conjuguez les verbes au " + grammaire + "\n");
        exos.append("------------------------------------------------------------\n");
        exos.append("1. Je (manger) ________ au restaurant ce soir.\n");
        exos.append("2. Nous (aller) ________ à la plage demain.\n");
        exos.append("3. Ils (finir) ________ leur travail à 17h.\n");

        if (difficulte >= 4) {
            exos.append("\nExercice 3 (Avancé) : Rédigez un petit paragraphe sur le thème '" + theme + "'\n");
            exos.append("en utilisant le " + grammaire + " et au moins 5 mots de vocabulaire.\n");
            exos.append("------------------------------------------------------------\n");
            exos.append("Rédigez ici : _____________________________________________\n");
        }

        return exos.toString();
    }

    private String genererCorrections(String theme, String grammaire) {
        StringBuilder corrections = new StringBuilder();

        corrections.append("Correction Exercice 1 :\n");
        switch (theme) {
            case "Voyage et Tourisme":
                corrections.append("1. avion / billet\n2. chambre\n3. passeport\n");
                break;
            case "Affaires et Travail":
                corrections.append("1. réunion\n2. rapport / projet\n3. patron\n");
                break;
            default:
                corrections.append("1. [réponse attendue]\n2. [réponse attendue]\n3. [réponse attendue]\n");
        }

        corrections.append("\nCorrection Exercice 2 :\n");
        if (grammaire.equals("Présent de l'indicatif")) {
            corrections.append("1. mange\n2. allons\n3. finissent\n");
        } else if (grammaire.equals("Passé composé")) {
            corrections.append("1. ai mangé\n2. sommes allé(e)s\n3. ont fini\n");
        } else if (grammaire.equals("Futur simple")) {
            corrections.append("1. mangerai\n2. irons\n3. finiront\n");
        } else {
            corrections.append("1. [conjugaison attendue]\n2. [conjugaison attendue]\n3. [conjugaison attendue]\n");
        }

        corrections.append("\nNotez-vous vous-même pour l'exercice 3 (10 points maximum).\n");
        corrections.append("Plus vous pratiquez, plus vous progressez !");

        return corrections.toString();
    }
    @FXML
    private void exporterPDF() {
        if (dernierCoursGenere == null || dernierCoursGenere.isEmpty()) {
            showAlert("Information", "Veuillez d'abord générer un cours.");
            return;
        }

        if (niveauSelectionne == null) {
            showAlert("Information", "Veuillez sélectionner un niveau pour le cours.");
            return;
        }

        try {
            String langueNom = this.langue != null ? this.langue.getNom() : "cours";
            String fileName = "cours_" + niveauSelectionne + "_" + langueNom + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

            // Chemin vers le dossier de la langue spécifique
            String baseDir = "src/main/resources/com/example/pijava_fluently/cours_pdf/";
            String langueDir = baseDir + langueNom + "/";
            File dir = new File(langueDir);
            if (!dir.exists()) dir.mkdirs();

            String filePath = langueDir + fileName;

            // Création du PDF
            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Configuration des marges
            document.setMargins(50, 50, 50, 50);

            // Définir les couleurs
            com.itextpdf.kernel.colors.DeviceRgb PRIMARY_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(108, 99, 255);
            com.itextpdf.kernel.colors.DeviceRgb SECONDARY_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(139, 92, 246);
            com.itextpdf.kernel.colors.DeviceRgb TITLE_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(26, 29, 46);
            com.itextpdf.kernel.colors.DeviceRgb TEXT_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(74, 77, 106);
            com.itextpdf.kernel.colors.DeviceRgb ACCENT_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(16, 185, 129);
            com.itextpdf.kernel.colors.DeviceRgb SECTION_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(245, 158, 11);

            // ========== EN-TÊTE ==========
            com.itextpdf.layout.element.Table headerTable = new com.itextpdf.layout.element.Table(2);
            headerTable.setWidth(com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 100);
            headerTable.setMarginBottom(20);

            // Cellule gauche
            com.itextpdf.layout.element.Cell titleCell = new com.itextpdf.layout.element.Cell();
            titleCell.setBackgroundColor(PRIMARY_COLOR);
            titleCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            titleCell.setPadding(20);
            titleCell.setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(10));

            Paragraph titlePara = new Paragraph("📚 " + langueNom.toUpperCase())
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
            titleCell.add(titlePara);

            // Cellule droite
            com.itextpdf.layout.element.Cell levelCell = new com.itextpdf.layout.element.Cell();
            levelCell.setBackgroundColor(SECONDARY_COLOR);
            levelCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            levelCell.setPadding(20);
            levelCell.setTextAlignment(TextAlignment.RIGHT);
            levelCell.setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(10));

            Paragraph levelPara = new Paragraph("Niveau " + niveauSelectionne)
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
            levelCell.add(levelPara);

            headerTable.addCell(titleCell);
            headerTable.addCell(levelCell);
            document.add(headerTable);

            // ========== TRAITEMENT DU CONTENU ==========
            String contenu = dernierCoursGenere;

            // Nettoyer le contenu (supprimer les caractères indésirables)
            contenu = contenu.replaceAll("\\*\\*", "");      // Supprimer **
            contenu = contenu.replaceAll("#", "");           // Supprimer #
            contenu = contenu.replaceAll("--", "");          // Supprimer --
            contenu = contenu.replaceAll("\\|\\|", "");      // Supprimer ||

            String[] lignes = contenu.split("\n");
            boolean inTable = false;

            for (String ligne : lignes) {
                if (ligne.trim().isEmpty()) {
                    document.add(new Paragraph(" "));
                    continue;
                }

                // Détection du début d'un tableau
                if (ligne.contains("|") && ligne.contains("---")) {
                    inTable = true;
                    continue;
                }

                // Traitement des tableaux
                if (inTable && ligne.contains("|")) {
                    String[] cells = ligne.split("\\|");
                    if (cells.length >= 2) {
                        com.itextpdf.layout.element.Table pdfTable = new com.itextpdf.layout.element.Table(cells.length - 1);
                        pdfTable.setWidth(com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 100);
                        pdfTable.setMarginTop(10);
                        pdfTable.setMarginBottom(10);

                        for (int i = 1; i < cells.length - 1; i++) {
                            String cellContent = cells[i].trim();
                            com.itextpdf.layout.element.Cell pdfCell = new com.itextpdf.layout.element.Cell();
                            pdfCell.add(new Paragraph(cellContent).setFontSize(10));
                            pdfCell.setPadding(8);

                            // En-tête du tableau (première ligne)
                            if (ligne.contains("Word") || ligne.contains("Mot") || ligne.contains("Définition")) {
                                pdfCell.setBackgroundColor(SECONDARY_COLOR);
                                pdfCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
                                pdfCell.setBold();
                            }
                            pdfTable.addCell(pdfCell);
                        }
                        document.add(pdfTable);
                    }
                    inTable = false;
                    continue;
                }

                // Titre principal (commence par 📚)
                if (ligne.startsWith("📚")) {
                    Paragraph p = new Paragraph(ligne)
                            .setFontSize(24)
                            .setBold()
                            .setFontColor(TITLE_COLOR)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(20)
                            .setMarginBottom(10);
                    document.add(p);
                }
                // Sections principales (🎯, 📝, 📖, 💬, ✏️, ✅, 💡, 🚀)
                else if (ligne.startsWith("🎯") || ligne.startsWith("📝") || ligne.startsWith("📖") ||
                        ligne.startsWith("💬") || ligne.startsWith("✏️") || ligne.startsWith("✅") ||
                        ligne.startsWith("💡") || ligne.startsWith("🚀")) {

                    Paragraph p = new Paragraph(ligne)
                            .setFontSize(16)
                            .setBold()
                            .setFontColor(PRIMARY_COLOR)
                            .setMarginTop(20)
                            .setMarginBottom(8);
                    document.add(p);
                }
                // Sous-titres (avec ---)
                else if (ligne.startsWith("---")) {
                    // Créer une ligne de séparation avec iText 7
                    com.itextpdf.layout.element.Paragraph linePara = new com.itextpdf.layout.element.Paragraph()
                            .add(new com.itextpdf.layout.element.Text("_________________________________________"))
                            .setFontColor(PRIMARY_COLOR)
                            .setMarginTop(5)
                            .setMarginBottom(5);
                    document.add(linePara);
                }
                // Liste à puces
                else if (ligne.trim().startsWith("•") || ligne.trim().startsWith("-") || ligne.trim().startsWith("*")) {
                    String cleanLine = ligne.trim().replaceFirst("[•\\-*] ", "• ");
                    Paragraph p = new Paragraph("    " + cleanLine)
                            .setFontSize(11)
                            .setFontColor(TEXT_COLOR)
                            .setMarginLeft(20);
                    document.add(p);
                }
                // Exercices numérotés
                else if (ligne.matches("\\d+\\..*") || ligne.matches("Exercice \\d+.*")) {
                    Paragraph p = new Paragraph(ligne)
                            .setFontSize(12)
                            .setBold()
                            .setFontColor(ACCENT_COLOR)
                            .setMarginTop(12)
                            .setMarginBottom(4);
                    document.add(p);
                }
                // Questions d'exercices (commencent par des lettres ou chiffres)
                else if (ligne.matches("^[a-z]\\) .*") || ligne.matches("^[0-9]\\. .*")) {
                    Paragraph p = new Paragraph("    " + ligne)
                            .setFontSize(11)
                            .setFontColor(TEXT_COLOR)
                            .setMarginLeft(15);
                    document.add(p);
                }
                // Texte normal
                else {
                    // Nettoyer la ligne
                    String cleanLine = ligne.trim();
                    if (!cleanLine.isEmpty()) {
                        Paragraph p = new Paragraph(cleanLine)
                                .setFontSize(11)
                                .setFontColor(TEXT_COLOR)
                                .setMarginBottom(4);
                        document.add(p);
                    }
                }
            }

            // ========== PIED DE PAGE ==========
            com.itextpdf.layout.element.Table footerTable = new com.itextpdf.layout.element.Table(1);
            footerTable.setWidth(com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 100);
            footerTable.setMarginTop(30);

            com.itextpdf.layout.element.Cell footerCell = new com.itextpdf.layout.element.Cell();
            footerCell.setBackgroundColor(PRIMARY_COLOR);
            footerCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            footerCell.setPadding(12);
            footerCell.setTextAlignment(TextAlignment.CENTER);
            footerCell.setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(10));

            Paragraph footerPara = new Paragraph("✨ Cours généré par Fluently - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .setFontSize(9)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
            footerCell.add(footerPara);

            footerTable.addCell(footerCell);
            document.add(footerTable);

            document.close();

            // Ajouter le cours dans le cercle du niveau choisi
            ajouterCoursDansNiveau(niveauSelectionne, fileName, filePath);

            showAlert("Succès", "✅ PDF exporté et ajouté au niveau " + niveauSelectionne + " !\n\nEmplacement : " + filePath);
            Desktop.getDesktop().open(dir);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'exporter le PDF : " + e.getMessage());
        }
    }

    private void ajouterCoursDansNiveau(String niveau, String fileName, String filePath) {
        // Créer un cours virtuel pour l'afficher dans le cercle
        Cours coursPerso = new Cours();
        // Correction : cast explicite de long vers int
        coursPerso.setId(-(int)(System.currentTimeMillis() % Integer.MAX_VALUE));
        coursPerso.setNumero(getNextCoursNumber(niveau));
        coursPerso.setRessource(filePath);
        coursPerso.setDateCreation(LocalDateTime.now().toLocalDate());

        // Ajouter à la map des cours par niveau
        List<Cours> coursList = coursParNiveau.getOrDefault(niveau, new ArrayList<>());
        coursList.add(coursPerso);
        coursParNiveau.put(niveau, coursList);

        // Mettre à jour l'affichage
        afficherNiveaux();
    }

    private int getNextCoursNumber(String niveau) {
        List<Cours> cours = coursParNiveau.getOrDefault(niveau, new ArrayList<>());
        int maxNumero = 0;
        for (Cours c : cours) {
            if (c.getNumero() > maxNumero) maxNumero = c.getNumero();
        }
        return maxNumero + 1;
    }
    @FXML private void selectionnerNiveauA1() { setNiveauSelectionne("A1"); }
    @FXML private void selectionnerNiveauA2() { setNiveauSelectionne("A2"); }
    @FXML private void selectionnerNiveauB1() { setNiveauSelectionne("B1"); }
    @FXML private void selectionnerNiveauB2() { setNiveauSelectionne("B2"); }
    @FXML private void selectionnerNiveauC1() { setNiveauSelectionne("C1"); }
    @FXML private void selectionnerNiveauC2() { setNiveauSelectionne("C2"); }

    private void setNiveauSelectionne(String niveau) {
        niveauSelectionne = niveau;
        niveauSelectionneLabel.setText("Niveau sélectionné : " + niveau);

        // Reset styles de tous les cercles
        resetNiveauStyles();

        // Appliquer le style actif sur le cercle choisi
        String styleActif = "-fx-background-color: #FFD700; -fx-background-radius: 40; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 12, 0, 0, 4);";

        switch (niveau) {
            case "A1": updateNiveauStyle(niveauA1Btn, styleActif); break;
            case "A2": updateNiveauStyle(niveauA2Btn, styleActif); break;
            case "B1": updateNiveauStyle(niveauB1Btn, styleActif); break;
            case "B2": updateNiveauStyle(niveauB2Btn, styleActif); break;
            case "C1": updateNiveauStyle(niveauC1Btn, styleActif); break;
            case "C2": updateNiveauStyle(niveauC2Btn, styleActif); break;
        }
    }

    private void resetNiveauStyles() {
        String styleParDefaut = "-fx-background-color: #6C63FF; -fx-background-radius: 40; -fx-padding: 15;";
        updateNiveauStyle(niveauA1Btn, styleParDefaut + " -fx-background-color: #6C63FF;");
        updateNiveauStyle(niveauA2Btn, styleParDefaut + " -fx-background-color: #8B5CF6;");
        updateNiveauStyle(niveauB1Btn, styleParDefaut + " -fx-background-color: #F59E0B;");
        updateNiveauStyle(niveauB2Btn, styleParDefaut + " -fx-background-color: #EF4444;");
        updateNiveauStyle(niveauC1Btn, styleParDefaut + " -fx-background-color: #EC4899;");
        updateNiveauStyle(niveauC2Btn, styleParDefaut + " -fx-background-color: #14B8A6;");
    }

    private void updateNiveauStyle(VBox container, String style) {
        if (container != null && container.getChildren().size() > 0) {
            Node circle = container.getChildren().get(0);
            if (circle instanceof StackPane) {
                circle.setStyle(style);
            }
        }
    }

    // Ajoutez ces attributs FXML pour les cercles
    @FXML private VBox niveauA1Btn, niveauA2Btn, niveauB1Btn, niveauB2Btn, niveauC1Btn, niveauC2Btn;



    /**
     * Crée une carte design pour un cours PDF
     */
    private VBox createPdfCard(Cours cours) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPrefWidth(180);
        card.setPrefHeight(160);
        card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1; -fx-cursor: hand;");

        // Effet hover
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F0F2FF; -fx-background-radius: 16; -fx-border-color: #6C63FF; -fx-border-radius: 16; -fx-border-width: 2;");
            card.setTranslateY(-4);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1;");
            card.setTranslateY(0);
        });

        // Icône PDF
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(60, 60);
        iconContainer.setStyle("-fx-background-color: #FFE4E6; -fx-background-radius: 30;");
        Label pdfIcon = new Label("📄");
        pdfIcon.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(pdfIcon);

        // Niveau du cours
        String niveau = trouverNiveauDuCours(cours);
        Label niveauBadge = new Label(niveau != null ? niveau : "Cours");
        niveauBadge.setStyle("-fx-background-color: " + getCouleurNiveau(niveau) + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 3 10 3 10;");

        // Numéro du cours
        Label numeroLabel = new Label("Cours N°" + cours.getNumero());
        numeroLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");

        // Date
        String dateStr = cours.getDateCreation() != null ? cours.getDateCreation().toString() : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #8A8FA8;");

        // Bouton ouvrir
        Button openBtn = new Button("📖 Ouvrir");
        openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;");
        openBtn.setOnMouseEntered(e -> openBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnMouseExited(e -> openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnAction(e -> ouvrirPdfCours(cours));

        card.getChildren().addAll(iconContainer, niveauBadge, numeroLabel, dateLabel, openBtn);
        VBox.setMargin(iconContainer, new Insets(10, 0, 0, 0));

        return card;
    }

    /**
     * Ouvre un cours PDF
     */
    private void ouvrirPdfCours(Cours cours) {
        if (cours.getRessource() != null) {
            File pdfFile = new File(cours.getRessource());
            if (pdfFile.exists()) {
                try {
                    Desktop.getDesktop().open(pdfFile);
                } catch (IOException e) {
                    showAlert("Erreur", "Impossible d'ouvrir le PDF : " + e.getMessage());
                }
            } else {
                showAlert("Erreur", "Fichier PDF introuvable : " + cours.getRessource());
            }
        }
    }

    /**
     * Retourne la couleur associée au niveau
     */
    private String getCouleurNiveau(String niveau) {
        if (niveau == null) return "#6C63FF";
        switch (niveau) {
            case "A1": return "#6C63FF";
            case "A2": return "#8B5CF6";
            case "B1": return "#F59E0B";
            case "B2": return "#EF4444";
            case "C1": return "#EC4899";
            case "C2": return "#14B8A6";
            default: return "#6C63FF";
        }
    }

    /**
     * Trouve le niveau d'un cours
     */
    private String trouverNiveauDuCours(Cours cours) {
        for (Map.Entry<String, List<Cours>> entry : coursParNiveau.entrySet()) {
            if (entry.getValue().contains(cours)) {
                return entry.getKey();
            }
        }
        return null;
    }
    /**
     * Affiche tous les cours PDF générés en lisant directement le dossier ressources
     */
    private void afficherCoursPdfGeneres() {
        if (coursPdfContainer == null) return;

        coursPdfContainer.getChildren().clear();

        // Récupérer le nom de la langue actuelle
        String langueActuelle = this.langue != null ? this.langue.getNom() : null;
        if (langueActuelle == null) {
            emptyCoursMessage.setVisible(true);
            emptyCoursMessage.setManaged(true);
            coursPdfScroll.setVisible(false);
            coursPdfScroll.setManaged(false);
            totalCoursCount.setText("0 cours");
            return;
        }

        // Chemin vers le dossier de la langue spécifique
        String langueDir = "src/main/resources/com/example/pijava_fluently/cours_pdf/" + langueActuelle + "/";
        File dossierLangue = new File(langueDir);

        List<File> fichiersPdf = new ArrayList<>();

        if (dossierLangue.exists() && dossierLangue.isDirectory()) {
            File[] fichiers = dossierLangue.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (fichiers != null) {
                fichiersPdf = Arrays.asList(fichiers);
                // Trier par date de modification (plus récent d'abord)
                fichiersPdf.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            }
        }

        int total = fichiersPdf.size();
        totalCoursCount.setText(total + " cours");

        if (total == 0) {
            emptyCoursMessage.setVisible(true);
            emptyCoursMessage.setManaged(true);
            coursPdfScroll.setVisible(false);
            coursPdfScroll.setManaged(false);
            return;
        }

        emptyCoursMessage.setVisible(false);
        emptyCoursMessage.setManaged(false);
        coursPdfScroll.setVisible(true);
        coursPdfScroll.setManaged(true);

        // Créer une carte pour chaque fichier PDF
        for (File pdfFile : fichiersPdf) {
            VBox card = createPdfCardFromFile(pdfFile);
            coursPdfContainer.getChildren().add(card);
        }
    }

    /**
     * Crée une carte design pour un fichier PDF
     */
    private VBox createPdfCardFromFile(File pdfFile) {
        String fileName = pdfFile.getName();
        // Extraire le niveau du nom du fichier (ex: cours_A1_Anglais_20250415_...pdf)
        String niveau = extraireNiveauFromFileName(fileName);
        // Extraire la date
        String dateStr = extraireDateFromFileName(fileName);
        // Extraire le numéro du cours
        int numero = extraireNumeroFromFileName(fileName);

        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPrefWidth(180);
        card.setPrefHeight(160);
        card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1; -fx-cursor: hand;");

        // Effet hover
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F0F2FF; -fx-background-radius: 16; -fx-border-color: #6C63FF; -fx-border-radius: 16; -fx-border-width: 2;");
            card.setTranslateY(-4);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1;");
            card.setTranslateY(0);
        });

        // Icône PDF
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(60, 60);
        iconContainer.setStyle("-fx-background-color: #FFE4E6; -fx-background-radius: 30;");
        Label pdfIcon = new Label("📄");
        pdfIcon.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(pdfIcon);

        // Niveau du cours
        Label niveauBadge = new Label(niveau != null ? niveau : "Cours");
        niveauBadge.setStyle("-fx-background-color: " + getCouleurNiveau(niveau) + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 3 10 3 10;");

        // Nom du fichier (tronqué)
        String displayName = fileName.length() > 25 ? fileName.substring(0, 22) + "..." : fileName;
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(160);

        // Date
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #8A8FA8;");

        // Bouton ouvrir
        Button openBtn = new Button("📖 Ouvrir");
        openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;");
        openBtn.setOnMouseEntered(e -> openBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnMouseExited(e -> openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(pdfFile);
            } catch (IOException ex) {
                showAlert("Erreur", "Impossible d'ouvrir le PDF : " + ex.getMessage());
            }
        });

        card.getChildren().addAll(iconContainer, niveauBadge, nameLabel, dateLabel, openBtn);
        VBox.setMargin(iconContainer, new Insets(10, 0, 0, 0));

        return card;
    }

    /**
     * Extrait le niveau du nom du fichier (ex: cours_A1_Anglais_... -> A1)
     */
    private String extraireNiveauFromFileName(String fileName) {
        if (fileName.startsWith("cours_")) {
            String[] parts = fileName.split("_");
            if (parts.length >= 2) {
                String niveau = parts[1];
                if (niveau.matches("A1|A2|B1|B2|C1|C2")) {
                    return niveau;
                }
            }
        }
        return "Cours";
    }

    /**
     * Extrait la date du nom du fichier
     */
    private String extraireDateFromFileName(String fileName) {
        try {
            // Format: cours_A1_Anglais_20250415_143022.pdf
            String[] parts = fileName.split("_");
            if (parts.length >= 4) {
                String datePart = parts[3];
                if (datePart.length() == 8) {
                    return datePart.substring(0, 4) + "-" + datePart.substring(4, 6) + "-" + datePart.substring(6, 8);
                }
            }
        } catch (Exception e) {}

        // Sinon, utiliser la date de modification du fichier
        return "";
    }

    /**
     * Extrait le numéro du cours (ordre)
     */
    private int extraireNumeroFromFileName(String fileName) {
        // Par défaut, retourner un numéro basé sur la position
        return 1;
    }
    @FXML
    private void rechercherVideosYouTube() {
        String query = youtubeSearchField.getText().trim();

        if (query.isEmpty()) {
            showAlert("Information", "Veuillez entrer un mot-clé pour la recherche.");
            return;
        }

        // Ajouter la langue à la recherche
        String langueNom = this.langue != null ? this.langue.getNom() : "";
        String rechercheComplet = query + " " + langueNom + " cours";

        // Afficher le chargement
        youtubeLoading.setVisible(true);
        youtubeLoading.setManaged(true);
        youtubeScrollPane.setVisible(false);
        youtubeScrollPane.setManaged(false);
        youtubeEmptyMessage.setVisible(false);
        youtubeEmptyMessage.setManaged(false);

        // Vider les résultats précédents
        youtubeResultsContainer.getChildren().clear();

        // Lancer la recherche dans un thread
        new Thread(() -> {
            try {
                List<YouTubeService.VideoInfo> videos = youTubeService.rechercherVideos(rechercheComplet, 10);

                Platform.runLater(() -> {
                    youtubeLoading.setVisible(false);
                    youtubeLoading.setManaged(false);

                    if (videos.isEmpty()) {
                        youtubeEmptyMessage.setVisible(true);
                        youtubeEmptyMessage.setManaged(true);
                        youtubeScrollPane.setVisible(false);
                        youtubeScrollPane.setManaged(false);
                    } else {
                        youtubeEmptyMessage.setVisible(false);
                        youtubeEmptyMessage.setManaged(false);
                        youtubeScrollPane.setVisible(true);
                        youtubeScrollPane.setManaged(true);

                        for (YouTubeService.VideoInfo video : videos) {
                            VBox videoCard = createYouTubeCard(video);
                            youtubeResultsContainer.getChildren().add(videoCard);
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    youtubeLoading.setVisible(false);
                    youtubeLoading.setManaged(false);
                    showAlert("Erreur", "Impossible de rechercher les vidéos : " + e.getMessage());
                });
            }
        }).start();
    }

    private VBox createYouTubeCard(YouTubeService.VideoInfo video) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 12; -fx-border-color: #E0E3F0; -fx-border-radius: 12; -fx-padding: 12; -fx-cursor: hand;");

        // Effet hover
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F0F2FF; -fx-background-radius: 12; -fx-border-color: #6C63FF; -fx-border-radius: 12; -fx-border-width: 2; -fx-padding: 12;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 12; -fx-border-color: #E0E3F0; -fx-border-radius: 12; -fx-padding: 12;");
        });

        // Titre
        Label titleLabel = new Label(video.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");
        titleLabel.setWrapText(true);

        // Description
        String desc = video.getDescription();
        if (desc.length() > 100) {
            desc = desc.substring(0, 100) + "...";
        }
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8FA8;");
        descLabel.setWrapText(true);

        // Bouton Regarder
        Button watchBtn = new Button("▶ Regarder");
        watchBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
        watchBtn.setOnAction(e -> ouvrirVideoYouTube(video.getVideoUrl()));

        card.getChildren().addAll(titleLabel, descLabel, watchBtn);

        return card;
    }

    private void ouvrirVideoYouTube(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            showAlert("Erreur", "Impossible d'ouvrir la vidéo");
        }
    }
    @FXML
    private void chercherMotDictionnaire() {
        String mot = dictionarySearchField.getText().trim();
        if (mot.isEmpty()) {
            showAlert("Information", "Veuillez entrer un mot à définir.");
            return;
        }

        String langueNom = this.langue != null ? this.langue.getNom() : "Français";

        dictionaryLoading.setVisible(true);
        dictionaryLoading.setManaged(true);
        dictionaryResultContainer.setVisible(false);
        dictionaryResultContainer.setManaged(false);
        dictionaryEmptyMessage.setVisible(false);
        dictionaryEmptyMessage.setManaged(false);

        new Thread(() -> {
            try {
                // Appel à la nouvelle méthode du dictionnaire
                String resultat = iaService.chercherDefinition(mot, langueNom);

                Platform.runLater(() -> {
                    dictionaryLoading.setVisible(false);
                    dictionaryLoading.setManaged(false);

                    if (resultat != null && !resultat.startsWith("❌")) {
                        afficherResultatDictionnaire(mot, resultat);
                    } else {
                        dictionaryEmptyMessage.setVisible(true);
                        dictionaryEmptyMessage.setManaged(true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dictionaryLoading.setVisible(false);
                    dictionaryLoading.setManaged(false);
                    dictionaryEmptyMessage.setVisible(true);
                    dictionaryEmptyMessage.setManaged(true);
                    showAlert("Erreur", "Impossible de trouver la définition : " + e.getMessage());
                });
            }
        }).start();
    }

    private void afficherResultatDictionnaire(String mot, String resultat) {
        // Parser le résultat formaté
        String definition = resultat;
        String exemple = "";
        String synonymes = "";

        // Extraction des différentes parties
        if (resultat.contains("📝 DÉFINITION:")) {
            String[] parts = resultat.split("📝 DÉFINITION:");
            if (parts.length > 1) {
                definition = parts[1].split("📌 EXEMPLE:")[0].trim();
            }
        }
        if (resultat.contains("📌 EXEMPLE:")) {
            String[] parts = resultat.split("📌 EXEMPLE:");
            if (parts.length > 1) {
                exemple = parts[1].split("🔗 SYNONYMES:")[0].trim();
            }
        }
        if (resultat.contains("🔗 SYNONYMES:")) {
            String[] parts = resultat.split("🔗 SYNONYMES:");
            if (parts.length > 1) {
                synonymes = parts[1].trim();
            }
        }

        dictionaryWord.setText("📖 " + mot.toUpperCase());
        dictionaryDefinition.setText(definition);

        if (!exemple.isEmpty()) {
            dictionaryExample.setText("📌 Exemple : " + exemple);
            dictionaryExample.setVisible(true);
            dictionaryExample.setManaged(true);
        } else {
            dictionaryExample.setVisible(false);
            dictionaryExample.setManaged(false);
        }

        if (!synonymes.isEmpty()) {
            dictionarySynonym.setText("🔗 Synonymes : " + synonymes);
            dictionarySynonym.setVisible(true);
            dictionarySynonym.setManaged(true);
        } else {
            dictionarySynonym.setVisible(false);
            dictionarySynonym.setManaged(false);
        }

        dictionaryResultContainer.setVisible(true);
        dictionaryResultContainer.setManaged(true);
    }
    @FXML
    private void genererQuiz() {
        String niveau = gameLevelCombo.getValue();
        if (niveau == null) {
            showAlert("Information", "Veuillez sélectionner un niveau pour le quiz.");
            return;
        }

        String langueNom = this.langue != null ? this.langue.getNom() : "Français";

        // Afficher le chargement
        gameLoading.setVisible(true);
        gameLoading.setManaged(true);
        quizContainer.setVisible(false);
        quizContainer.setManaged(false);

        new Thread(() -> {
            try {
                String prompt = "Génère un quiz de vocabulaire en " + langueNom + " pour un niveau " + niveau +
                        ". Format attendu (5 questions):\n" +
                        "Q1: [question]?|Option1|Option2|Option3|Option4|Réponse correcte (1-4)\n" +
                        "Q2: [question]?|Option1|Option2|Option3|Option4|Réponse correcte\n" +
                        "...";

                String resultat = iaService.genererCours(langueNom, "quiz", prompt, "", 3);

                Platform.runLater(() -> {
                    gameLoading.setVisible(false);
                    gameLoading.setManaged(false);
                    parserQuiz(resultat);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    gameLoading.setVisible(false);
                    gameLoading.setManaged(false);
                    showAlert("Erreur", "Impossible de générer le quiz : " + e.getMessage());
                });
            }
        }).start();
    }

    private void parserQuiz(String quizText) {
        questionsList.clear();
        String[] lignes = quizText.split("\n");

        for (String ligne : lignes) {
            if (ligne.trim().isEmpty()) continue;

            // Format: Q1: Quelle est la traduction de "chat"?|dog|cat|bird|fish|2
            if (ligne.contains("|")) {
                String[] parts = ligne.split("\\|");
                if (parts.length >= 6) {
                    String question = parts[0].replaceFirst("Q\\d+:", "").trim();
                    String[] options = Arrays.copyOfRange(parts, 1, 5);
                    int correctAnswer = Integer.parseInt(parts[5].trim());
                    questionsList.add(new QuizQuestion(question, options, correctAnswer));
                }
            }
        }

        if (questionsList.isEmpty()) {
            // Quiz de secours en cas d'échec du parsing
            creerQuizSecours();
        }

        currentQuestionIndex = 0;
        currentScore = 0;
        startQuiz();
    }

    private void creerQuizSecours() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        questionsList.clear();

        if (langueNom.equalsIgnoreCase("Anglais")) {
            questionsList.add(new QuizQuestion("What is the translation of 'Bonjour'?",
                    new String[]{"Goodbye", "Hello", "Thank you", "Please"}, 2));
            questionsList.add(new QuizQuestion("What does 'Chien' mean in English?",
                    new String[]{"Cat", "Bird", "Dog", "Fish"}, 3));
            questionsList.add(new QuizQuestion("Translate 'Rouge' to English",
                    new String[]{"Blue", "Green", "Yellow", "Red"}, 4));
            questionsList.add(new QuizQuestion("What is 'Maison' in English?",
                    new String[]{"Car", "House", "School", "Garden"}, 2));
            questionsList.add(new QuizQuestion("Translate 'Manger' to English",
                    new String[]{"To drink", "To sleep", "To eat", "To run"}, 3));
        } else {
            questionsList.add(new QuizQuestion("Quelle est la traduction de 'Hello' en français ?",
                    new String[]{"Au revoir", "Bonjour", "Merci", "S'il vous plaît"}, 2));
            questionsList.add(new QuizQuestion("Que signifie 'Dog' en français ?",
                    new String[]{"Chat", "Oiseau", "Chien", "Poisson"}, 3));
            questionsList.add(new QuizQuestion("Traduisez 'Blue' en français",
                    new String[]{"Rouge", "Vert", "Jaune", "Bleu"}, 4));
            questionsList.add(new QuizQuestion("Que veut dire 'Car' en français ?",
                    new String[]{"Maison", "Voiture", "École", "Jardin"}, 2));
            questionsList.add(new QuizQuestion("Traduisez 'To eat' en français",
                    new String[]{"Boire", "Dormir", "Manger", "Courir"}, 3));
        }
    }

    private void startQuiz() {
        currentToggleGroup = new ToggleGroup();
        quizAnswersContainer.getChildren().clear();
        quizResultContainer.setVisible(false);
        quizResultContainer.setManaged(false);
        nextQuestionBtn.setVisible(true);
        nextQuestionBtn.setManaged(true);

        afficherQuestion();

        quizContainer.setVisible(true);
        quizContainer.setManaged(true);
    }

    private void afficherQuestion() {
        if (currentQuestionIndex >= questionsList.size()) {
            terminerQuiz();
            return;
        }

        QuizQuestion q = questionsList.get(currentQuestionIndex);
        quizQuestion.setText((currentQuestionIndex + 1) + ". " + q.getQuestion());
        quizProgress.setProgress((double) currentQuestionIndex / questionsList.size());
        quizScore.setText("Score: " + currentScore);

        // Nettoyer les anciennes réponses
        quizAnswersContainer.getChildren().clear();
        currentToggleGroup = new ToggleGroup();

        // Ajouter les options
        for (int i = 0; i < q.getOptions().length; i++) {
            RadioButton rb = new RadioButton(q.getOptions()[i]);
            rb.setToggleGroup(currentToggleGroup);
            rb.setUserData(i + 1);
            rb.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8;");
            rb.setOnAction(e -> verifierReponse((int) rb.getUserData()));
            quizAnswersContainer.getChildren().add(rb);
        }
    }

    private void verifierReponse(int reponseChoisie) {
        QuizQuestion q = questionsList.get(currentQuestionIndex);

        if (reponseChoisie == q.getCorrectAnswer()) {
            currentScore += 10;
            showFeedback("✅ Bonne réponse ! +10 points");
        } else {
            String bonneReponse = q.getOptions()[q.getCorrectAnswer() - 1];
            showFeedback("❌ Mauvaise réponse. La bonne réponse était : " + bonneReponse);
        }

        // Désactiver les radios après réponse
        for (Node node : quizAnswersContainer.getChildren()) {
            if (node instanceof RadioButton) {
                ((RadioButton) node).setDisable(true);
            }
        }

        currentQuestionIndex++;
        quizScore.setText("Score: " + currentScore);

        if (currentQuestionIndex >= questionsList.size()) {
            terminerQuiz();
        }
    }

    private void showFeedback(String message) {
        Label feedback = new Label(message);
        feedback.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-padding: 5;");
        quizAnswersContainer.getChildren().add(feedback);

        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) {}
            Platform.runLater(() -> quizAnswersContainer.getChildren().remove(feedback));
        }).start();
    }

    @FXML
    private void nextQuestion() {
        afficherQuestion();
    }

    private void terminerQuiz() {
        nextQuestionBtn.setVisible(false);
        nextQuestionBtn.setManaged(false);

        int totalPoints = questionsList.size() * 10;
        double pourcentage = (double) currentScore / totalPoints * 100;

        quizFinalScore.setText("🎉 Score final : " + currentScore + "/" + totalPoints + " (" + (int)pourcentage + "%)");

        String appreciation;
        if (pourcentage >= 80) appreciation = "Excellent ! Vous maîtrisez très bien ce niveau ! 🏆";
        else if (pourcentage >= 60) appreciation = "Très bien ! Continuez à pratiquer ! 👍";
        else if (pourcentage >= 40) appreciation = "Pas mal ! Révisez un peu et réessayez ! 📚";
        else appreciation = "Continuez vos efforts ! Refaites le quiz après révision ! 💪";

        quizFeedback.setText(appreciation);
        quizResultContainer.setVisible(true);
        quizResultContainer.setManaged(true);
    }
    @FXML
    private void startGame() {
        String gameType = gameTypeCombo.getValue();
        String niveau = gameLevelCombo.getValue();

        if (gameType == null) {
            showAlert("Information", "Veuillez sélectionner un type de jeu.");
            return;
        }
        if (niveau == null) {
            showAlert("Information", "Veuillez sélectionner un niveau.");
            return;
        }

        String langueNom = this.langue != null ? this.langue.getNom() : "Français";

        // Cacher tous les conteneurs
        quizContainer.setVisible(false); quizContainer.setManaged(false);
        fillGameContainer.setVisible(false); fillGameContainer.setManaged(false);

        gameLoading.setVisible(true); gameLoading.setManaged(true);

        new Thread(() -> {
            try {
                String prompt = genererPromptJeu(gameType, langueNom, niveau);
                String resultat = iaService.genererCours(langueNom, "jeu", prompt, "", 3);

                Platform.runLater(() -> {
                    gameLoading.setVisible(false); gameLoading.setManaged(false);

                    if (gameType.contains("Quiz")) {
                        parserQuiz(resultat);
                    } else if (gameType.contains("Compléter")) {
                        parserFillGame(resultat);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    gameLoading.setVisible(false); gameLoading.setManaged(false);
                    showAlert("Erreur", "Impossible de générer le jeu : " + e.getMessage());
                });
            }
        }).start();
    }
    private String genererPromptJeu(String gameType, String langue, String niveau) {
        if (gameType.contains("Quiz")) {
            return "Génère un quiz de vocabulaire en " + langue + " pour niveau " + niveau +
                    ". Format: Q1: question?|opt1|opt2|opt3|opt4|numero_reponse(1-4)\n(5 questions)";
        } else {
            return "Génère 5 questions à trous en " + langue + " pour niveau " + niveau +
                    ". Format: PHRASE avec un ____|Option1|Option2|Option3|Option4|numero_reponse(1-4)\n" +
                    "Exemple: Je ____ à l'école.|vais|vont|allez|allons|1";
        }
    }

    private void parserFillGame(String data) {
        fillQuestionsList.clear();
        String[] lignes = data.split("\n");

        for (String ligne : lignes) {
            if (ligne.trim().isEmpty() || !ligne.contains("|")) continue;
            String[] parts = ligne.split("\\|");
            if (parts.length >= 6) {
                String phrase = parts[0].trim();
                String[] options = Arrays.copyOfRange(parts, 1, 5);

                // 🔥 NETTOYER LA RÉPONSE des caractères markdown
                String reponseRaw = parts[5].trim();
                String reponseClean = reponseRaw.replaceAll("\\*\\*", "").replaceAll("\\*", "").trim();

                try {
                    int correctAnswer = Integer.parseInt(reponseClean);
                    fillQuestionsList.add(new FillQuestion(phrase, options, correctAnswer));
                } catch (NumberFormatException e) {
                    System.err.println("Erreur parsing réponse: " + reponseRaw + " -> nettoyé: " + reponseClean);
                    // Fallback: essayer d'extraire le premier chiffre trouvé
                    String digits = reponseClean.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        int correctAnswer = Integer.parseInt(digits.substring(0, 1));
                        fillQuestionsList.add(new FillQuestion(phrase, options, correctAnswer));
                    }
                }
            }
        }

        if (fillQuestionsList.isEmpty()) {
            creerFillGameSecours();
        }

        currentFillIndex = 0;
        currentFillScore = 0;
        startFillGame();
    }

    private void creerFillGameSecours() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        fillQuestionsList.clear();

        if (langueNom.equalsIgnoreCase("Anglais")) {
            fillQuestionsList.add(new FillQuestion("I ____ to school every day.", new String[]{"go", "goes", "going", "went"}, 1));
            fillQuestionsList.add(new FillQuestion("She ____ eating an apple.", new String[]{"am", "are", "is", "be"}, 3));
            fillQuestionsList.add(new FillQuestion("They ____ playing football now.", new String[]{"am", "is", "are", "be"}, 3));
            fillQuestionsList.add(new FillQuestion("He ____ to the cinema yesterday.", new String[]{"go", "goes", "went", "going"}, 3));
            fillQuestionsList.add(new FillQuestion("We ____ happy.", new String[]{"am", "is", "are", "be"}, 3));
        } else {
            fillQuestionsList.add(new FillQuestion("Je ____ à l'école.", new String[]{"vais", "vont", "allez", "allons"}, 1));
            fillQuestionsList.add(new FillQuestion("Elle ____ une pomme.", new String[]{"mange", "manges", "mangent", "mangeons"}, 1));
            fillQuestionsList.add(new FillQuestion("Ils ____ au parc.", new String[]{"vais", "va", "vont", "allons"}, 3));
            fillQuestionsList.add(new FillQuestion("Nous ____ contents.", new String[]{"sommes", "êtes", "sont", "est"}, 1));
            fillQuestionsList.add(new FillQuestion("Tu ____ fatigué.", new String[]{"suis", "es", "est", "sommes"}, 2));
        }
    }

    private void startFillGame() {
        fillOptionsContainer.getChildren().clear();
        fillResultContainer.setVisible(false); fillResultContainer.setManaged(false);
        fillNextBtn.setVisible(true); fillNextBtn.setManaged(true);
        afficherFillQuestion();
        fillGameContainer.setVisible(true); fillGameContainer.setManaged(true);
    }

    private void afficherFillQuestion() {
        if (currentFillIndex >= fillQuestionsList.size()) {
            terminerFillGame();
            return;
        }

        FillQuestion q = fillQuestionsList.get(currentFillIndex);
        fillQuestion.setText((currentFillIndex + 1) + ". " + q.getPhrase());
        fillProgress.setProgress((double) currentFillIndex / fillQuestionsList.size());
        fillScore.setText("Score: " + currentFillScore);

        fillOptionsContainer.getChildren().clear();

        for (int i = 0; i < q.getOptions().length; i++) {
            Button optionBtn = new Button(q.getOptions()[i]);
            final int answerIndex = i + 1;
            optionBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15; -fx-cursor: hand;");
            optionBtn.setOnAction(e -> verifierFillReponse(answerIndex, optionBtn));
            fillOptionsContainer.getChildren().add(optionBtn);
        }
    }

    private void verifierFillReponse(int reponseChoisie, Button btn) {
        FillQuestion q = fillQuestionsList.get(currentFillIndex);

        // Désactiver tous les boutons
        for (Node node : fillOptionsContainer.getChildren()) {
            if (node instanceof Button) {
                ((Button) node).setDisable(true);
            }
        }

        if (reponseChoisie == q.getCorrectAnswer()) {
            currentFillScore += 10;
            btn.setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15;");
            showFillFeedback("✅ Bonne réponse ! +10 points", true);
        } else {
            btn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15;");
            String bonneReponse = q.getOptions()[q.getCorrectAnswer() - 1];
            showFillFeedback("❌ Mauvaise réponse. La bonne réponse était : " + bonneReponse, false);

            // Mettre en vert la bonne réponse
            for (int i = 0; i < fillOptionsContainer.getChildren().size(); i++) {
                Node node = fillOptionsContainer.getChildren().get(i);
                if (node instanceof Button && (i + 1) == q.getCorrectAnswer()) {
                    ((Button) node).setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15;");
                    break;
                }
            }
        }

        fillScore.setText("Score: " + currentFillScore);
        currentFillIndex++;

        if (currentFillIndex >= fillQuestionsList.size()) {
            terminerFillGame();
        }
    }

    private void showFillFeedback(String message, boolean isCorrect) {
        Label feedback = new Label(message);
        feedback.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-padding: 5;");
        fillOptionsContainer.getChildren().add(feedback);

        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) {}
            Platform.runLater(() -> fillOptionsContainer.getChildren().remove(feedback));
        }).start();
    }

    @FXML
    private void nextFillQuestion() {
        afficherFillQuestion();
    }

    private void terminerFillGame() {
        fillNextBtn.setVisible(false); fillNextBtn.setManaged(false);

        int totalPoints = fillQuestionsList.size() * 10;
        double pourcentage = (double) currentFillScore / totalPoints * 100;

        fillFinalScore.setText("🎉 Score final : " + currentFillScore + "/" + totalPoints + " (" + (int)pourcentage + "%)");

        String appreciation;
        if (pourcentage >= 80) appreciation = "Excellent ! Vous maîtrisez très bien ce niveau ! 🏆";
        else if (pourcentage >= 60) appreciation = "Très bien ! Continuez à pratiquer ! 👍";
        else if (pourcentage >= 40) appreciation = "Pas mal ! Révisez un peu et réessayez ! 📚";
        else appreciation = "Continuez vos efforts ! Refaites le jeu après révision ! 💪";

        fillFeedback.setText(appreciation);
        fillResultContainer.setVisible(true); fillResultContainer.setManaged(true);
    }
    @FXML
    private void prononcer() {
        String texte = pronunciationTextField.getText().trim();
        if (texte.isEmpty()) {
            showAlert("Information", "Veuillez entrer un mot ou une phrase à écouter.");
            return;
        }

        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        String langueCode = prononciationService.getLangueCode(langueNom);

        // Afficher l'indicateur de chargement
        pronunciationLoading.setVisible(true);
        pronunciationLoading.setManaged(true);

        // Désactiver le bouton pendant la lecture
        // (optionnel)

        new Thread(() -> {
            try {
                prononciationService.prononcer(texte, langueCode);
                Platform.runLater(() -> {
                    pronunciationLoading.setVisible(false);
                    pronunciationLoading.setManaged(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pronunciationLoading.setVisible(false);
                    pronunciationLoading.setManaged(false);
                    showAlert("Erreur", "Impossible de lire la prononciation : " + e.getMessage());
                });
            }
        }).start();
    }

    // Méthodes pour les suggestions
    @FXML
    private void suggestionBonjour() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) {
            pronunciationTextField.setText("Hello");
        } else if (langueNom.equalsIgnoreCase("Espagnol")) {
            pronunciationTextField.setText("Hola");
        } else if (langueNom.equalsIgnoreCase("Allemand")) {
            pronunciationTextField.setText("Hallo");
        } else if (langueNom.equalsIgnoreCase("Italien")) {
            pronunciationTextField.setText("Ciao");
        } else {
            pronunciationTextField.setText("Bonjour");
        }
        prononcer();
    }

    @FXML
    private void suggestionMerci() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) {
            pronunciationTextField.setText("Thank you");
        } else if (langueNom.equalsIgnoreCase("Espagnol")) {
            pronunciationTextField.setText("Gracias");
        } else if (langueNom.equalsIgnoreCase("Allemand")) {
            pronunciationTextField.setText("Danke");
        } else if (langueNom.equalsIgnoreCase("Italien")) {
            pronunciationTextField.setText("Grazie");
        } else {
            pronunciationTextField.setText("Merci");
        }
        prononcer();
    }

    @FXML
    private void suggestionComment() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) {
            pronunciationTextField.setText("How are you?");
        } else if (langueNom.equalsIgnoreCase("Espagnol")) {
            pronunciationTextField.setText("¿Cómo estás?");
        } else if (langueNom.equalsIgnoreCase("Allemand")) {
            pronunciationTextField.setText("Wie geht es dir?");
        } else if (langueNom.equalsIgnoreCase("Italien")) {
            pronunciationTextField.setText("Come stai?");
        } else {
            pronunciationTextField.setText("Comment allez-vous ?");
        }
        prononcer();
    }

    @FXML
    private void suggestionAmour() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) {
            pronunciationTextField.setText("I love you");
        } else if (langueNom.equalsIgnoreCase("Espagnol")) {
            pronunciationTextField.setText("Te quiero");
        } else if (langueNom.equalsIgnoreCase("Allemand")) {
            pronunciationTextField.setText("Ich liebe dich");
        } else if (langueNom.equalsIgnoreCase("Italien")) {
            pronunciationTextField.setText("Ti amo");
        } else {
            pronunciationTextField.setText("Je t'aime");
        }
        prononcer();
    }
    @FXML
    private void ouvrirFlashcards() {
        ouvrirFormulaireFlashcards();
    }
    @FXML
    private void ouvrirFormulaireFlashcards() {
        flashcardsFormContainer.setVisible(true);
        flashcardsFormContainer.setManaged(true);
        flashcardsPromptArea.clear();
        flashcardsLevelCombo.setValue(niveauActuelValue.getText() != null ? niveauActuelValue.getText() : "B1 - Intermédiaire");
    }

    @FXML
    private void fermerFormulaireFlashcards() {
        flashcardsFormContainer.setVisible(false);
        flashcardsFormContainer.setManaged(false);
    }

    @FXML
    private void genererFlashcardsIA() {
        String prompt = flashcardsPromptArea.getText().trim();
        if (prompt.isEmpty()) {
            showAlert("Information", "Veuillez décrire ce que vous souhaitez réviser.");
            return;
        }

        String selectedLevel = flashcardsLevelCombo.getValue();
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";

        flashcardsLoading.setVisible(true);
        flashcardsLoading.setManaged(true);

        new Thread(() -> {
            try {
                String resultat = iaService.genererFlashcards(langueNom, prompt, selectedLevel);
                List<Flashcard> flashcards = parserFlashcardsFromString(resultat);

                Platform.runLater(() -> {
                    flashcardsLoading.setVisible(false);
                    flashcardsLoading.setManaged(false);

                    if (!flashcards.isEmpty()) {
                        startFlashcardSession(flashcards);
                    } else {
                        showAlert("Erreur", "Impossible de générer les flashcards. Utilisation du mode secours.");
                        startFlashcardSession(getFallbackFlashcards(langueNom));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    flashcardsLoading.setVisible(false);
                    flashcardsLoading.setManaged(false);
                    showAlert("Erreur", "Erreur: " + e.getMessage() + "\nUtilisation du mode secours.");
                    startFlashcardSession(getFallbackFlashcards(langueNom));
                });
            }
        }).start();
    }

    private List<Flashcard> parserFlashcardsFromString(String data) {
        List<Flashcard> flashcards = new ArrayList<>();
        String[] sections = data.split("===");

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            String question = extractFlashcardField(section, "QUESTION:");
            String opt1 = extractFlashcardField(section, "OPTION1:");
            String opt2 = extractFlashcardField(section, "OPTION2:");
            String opt3 = extractFlashcardField(section, "OPTION3:");
            String opt4 = extractFlashcardField(section, "OPTION4:");
            String reponseStr = extractFlashcardField(section, "REPONSE:");
            String explication = extractFlashcardField(section, "EXPLICATION:");

            if (question != null && opt1 != null && reponseStr != null) {
                try {
                    int correctAnswer = Integer.parseInt(reponseStr.trim());
                    String[] options = {opt1, opt2, opt3, opt4};
                    flashcards.add(new Flashcard(question, options, correctAnswer,
                            explication != null ? explication : "Révisez ce concept."));
                } catch (NumberFormatException e) {}
            }
        }

        if (flashcards.isEmpty()) {
            flashcards = getFallbackFlashcards(langue != null ? langue.getNom() : "Français");
        }

        return flashcards;
    }

    private String extractFlashcardField(String text, String fieldName) {
        int start = text.indexOf(fieldName);
        if (start == -1) return null;
        start += fieldName.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    private List<Flashcard> getFallbackFlashcards(String langue) {
        List<Flashcard> flashcards = new ArrayList<>();

        if (langue.equalsIgnoreCase("anglais") || langue.equalsIgnoreCase("english")) {
            flashcards.add(new Flashcard("What is the past tense of 'to go'?",
                    new String[]{"Goed", "Went", "Gone", "Going"}, 2,
                    "'Went' is the correct past tense of 'to go'."));
            flashcards.add(new Flashcard("Choose the correct sentence:",
                    new String[]{"She don't like pizza", "She doesn't likes pizza", "She doesn't like pizza", "She not like pizza"}, 3,
                    "With 'she', use 'doesn't' + base verb."));
            flashcards.add(new Flashcard("What does 'big' mean?",
                    new String[]{"Small", "Large", "Fast", "Slow"}, 2,
                    "'Big' means large in size."));
            flashcards.add(new Flashcard("Complete: I ___ to the cinema yesterday.",
                    new String[]{"go", "went", "goes", "going"}, 2,
                    "'Yesterday' = past tense, use 'went'."));
            flashcards.add(new Flashcard("What is the opposite of 'hot'?",
                    new String[]{"Warm", "Cold", "Cool", "Freezing"}, 2,
                    "'Cold' is opposite of 'hot'."));
        } else {
            flashcards.add(new Flashcard("Quel est le passé composé du verbe 'aller' ?",
                    new String[]{"Je vais", "Je suis allé", "Je suis allée", "Je vais aller"}, 2,
                    "Le passé composé de 'aller' = auxiliaire 'être' + 'allé'."));
            flashcards.add(new Flashcard("Complétez : Elle ___ (manger) une pomme.",
                    new String[]{"mange", "manges", "mangent", "mangeons"}, 1,
                    "Avec 'elle', conjuguez au présent : 'elle mange'."));
            flashcards.add(new Flashcard("Que signifie 'big' en français ?",
                    new String[]{"Petit", "Grand", "Rapide", "Lent"}, 2,
                    "'Big' = 'grand' en français."));
            flashcards.add(new Flashcard("Complétez : Je ___ (être) fatigué.",
                    new String[]{"suis", "es", "est", "sommes"}, 1,
                    "Le verbe 'être' se conjugue 'je suis'."));
            flashcards.add(new Flashcard("Quel est le contraire de 'chaud' ?",
                    new String[]{"Tiède", "Froid", "Brûlant", "Glacé"}, 2,
                    "Le contraire de 'chaud' est 'froid'."));
        }
        return flashcards;
    }

    private void startFlashcardSession(List<Flashcard> flashcards) {
        currentFlashcards = flashcards;
        currentFlashcardIndex = 0;
        currentFlashcardScore = 0;
        flashcardUserAnswers.clear();
        flashcardUserSelections.clear();

        for (int i = 0; i < flashcards.size(); i++) {
            flashcardUserAnswers.add(false);
            flashcardUserSelections.add(-1);
        }

        fermerFormulaireFlashcards();
        loadCurrentFlashcard();
        flashcardsSessionContainer.setVisible(true);
        flashcardsSessionContainer.setManaged(true);
    }

    private void loadCurrentFlashcard() {
        if (currentFlashcardIndex >= currentFlashcards.size()) {
            terminerFlashcardSession();
            return;
        }

        Flashcard card = currentFlashcards.get(currentFlashcardIndex);
        // Nettoyer la question des caractères markdown
        flashcardQuestionLabel.setText(cleanMarkdown(card.getQuestion()));

        flashcardOptionsContainer.getChildren().clear();
        flashcardToggleGroup = new ToggleGroup();

        for (int i = 0; i < card.getOptions().length; i++) {
            RadioButton rb = new RadioButton(cleanMarkdown(card.getOptions()[i]));
            rb.setToggleGroup(flashcardToggleGroup);
            rb.setUserData(i + 1);
            rb.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-padding: 8; -fx-cursor: hand;");
            flashcardOptionsContainer.getChildren().add(rb);
        }

        flashcardExplanationArea.setVisible(false);
        flashcardExplanationArea.clear();
        flashcardValidateBtn.setDisable(false);
        flashcardValidateBtn.setVisible(true);
        flashcardNextBtn.setVisible(false);

        flashcardProgressLabel.setText("Carte " + (currentFlashcardIndex + 1) + "/" + currentFlashcards.size());
        flashcardScoreLabel.setText("Score: " + currentFlashcardScore);
    }

    @FXML
    private void validerReponseFlashcard() {
        if (flashcardToggleGroup == null) return;

        RadioButton selected = (RadioButton) flashcardToggleGroup.getSelectedToggle();
        if (selected == null) {
            showAlert("Information", "Veuillez sélectionner une réponse.");
            return;
        }

        int selectedOption = (int) selected.getUserData();
        Flashcard card = currentFlashcards.get(currentFlashcardIndex);
        boolean isCorrect = (selectedOption == card.getCorrectAnswer());

        // Nettoyer l'explication
        flashcardExplanationArea.setText(cleanMarkdown(card.getExplanation()));
        flashcardExplanationArea.setVisible(true);

        // Style de l'explication avec couleurs
        flashcardExplanationArea.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 10; -fx-border-color: #A5D6A7; -fx-border-radius: 10; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");

        // Désactiver et colorier
        for (int i = 0; i < flashcardOptionsContainer.getChildren().size(); i++) {
            javafx.scene.Node node = flashcardOptionsContainer.getChildren().get(i);
            if (node instanceof RadioButton) {
                RadioButton rb = (RadioButton) node;
                rb.setDisable(true);
                int optValue = (int) rb.getUserData();
                if (optValue == card.getCorrectAnswer()) {
                    rb.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #D1FAE5; -fx-background-radius: 8;");
                } else if (optValue == selectedOption && !isCorrect) {
                    rb.setStyle("-fx-text-fill: #EF4444; -fx-padding: 8; -fx-background-color: #FEE2E2; -fx-background-radius: 8;");
                }
            }
        }

        flashcardUserAnswers.set(currentFlashcardIndex, isCorrect);
        flashcardUserSelections.set(currentFlashcardIndex, selectedOption);
        if (isCorrect) currentFlashcardScore++;

        flashcardValidateBtn.setDisable(true);
        flashcardValidateBtn.setVisible(false);
        flashcardNextBtn.setVisible(true);
        flashcardScoreLabel.setText("Score: " + currentFlashcardScore);

        if (currentFlashcardIndex + 1 >= currentFlashcards.size()) {
            flashcardNextBtn.setText("✓ Terminer");
        }
    }

    @FXML
    private void flashcardSuivante() {
        currentFlashcardIndex++;
        if (currentFlashcardIndex >= currentFlashcards.size()) {
            terminerFlashcardSession();
        } else {
            loadCurrentFlashcard();
        }
    }

    private void terminerFlashcardSession() {
        flashcardsSessionContainer.setVisible(false);
        flashcardsSessionContainer.setManaged(false);

        String analysis = generateFlashcardAnalysis();
        flashcardResultArea.setText(analysis);
        styleFlashcardResult(); // Ajouter cette ligne
        flashcardResultContainer.setVisible(true);
        flashcardResultContainer.setManaged(true);
    }

    private String generateFlashcardAnalysis() {
        StringBuilder analysis = new StringBuilder();
        int total = currentFlashcards.size();
        int correct = currentFlashcardScore;
        int wrong = total - correct;
        double percentage = (double) correct / total * 100;

        analysis.append("📊 ANALYSE DE VOS RÉPONSES\n");
        analysis.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Section des statistiques avec couleurs via emojis
        analysis.append("✅ Réponses correctes : ").append(correct).append("/").append(total).append("\n");
        analysis.append("❌ Réponses incorrectes : ").append(wrong).append("/").append(total).append("\n");
        analysis.append("📈 Taux de réussite : ").append(String.format("%.1f", percentage)).append("%\n\n");

        // Message de félicitations ou encouragement
        if (percentage >= 80) {
            analysis.append("🏆 EXCELLENT ! Vous maîtrisez très bien ce sujet !\n");
            analysis.append("   Continuez comme ça, vous êtes sur la bonne voie !\n\n");
        } else if (percentage >= 60) {
            analysis.append("👍 TRÈS BIEN ! Quelques petites lacunes à combler.\n");
            analysis.append("   Révisez les cartes où vous avez fait des erreurs.\n\n");
        } else if (percentage >= 40) {
            analysis.append("📚 PAS MAL ! Mais il faut plus de pratique.\n");
            analysis.append("   Concentrez-vous sur les explications fournies.\n\n");
        } else {
            analysis.append("💪 CONTINUEZ VOS EFFORTS ! C'est en forgeant qu'on devient forgeron.\n");
            analysis.append("   Revoyez toutes les flashcards et réessayez.\n\n");
        }

        // Détail des erreurs
        analysis.append("📝 DÉTAIL DES ERREURS\n");
        analysis.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        boolean hasErrors = false;
        for (int i = 0; i < currentFlashcards.size(); i++) {
            if (!flashcardUserAnswers.get(i)) {
                hasErrors = true;
                Flashcard card = currentFlashcards.get(i);
                int userAnswer = flashcardUserSelections.get(i);
                analysis.append("\n❌ CARTE ").append(i + 1).append("\n");
                analysis.append("   📖 Question : ").append(cleanMarkdown(card.getQuestion())).append("\n");
                analysis.append("   ❌ Votre réponse : ").append(cleanMarkdown(card.getOptions()[userAnswer - 1])).append("\n");
                analysis.append("   ✅ Bonne réponse : ").append(cleanMarkdown(card.getOptions()[card.getCorrectAnswer() - 1])).append("\n");
                analysis.append("   💡 Explication : ").append(cleanMarkdown(card.getExplanation())).append("\n");
            }
        }

        if (!hasErrors) {
            analysis.append("\n🎉 Félicitations ! Aucune erreur ! Vous avez parfaitement maîtrisé ce sujet.\n");
        }

        // Conseils personnalisés
        analysis.append("\n\n🎯 CONSEILS POUR PROGRESSER\n");
        analysis.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // Analyser les types d'erreurs
        int grammarErrors = 0, vocabErrors = 0, conjugationErrors = 0;
        for (int i = 0; i < currentFlashcards.size(); i++) {
            if (!flashcardUserAnswers.get(i)) {
                String explanation = currentFlashcards.get(i).getExplanation().toLowerCase();
                if (explanation.contains("grammaire") || explanation.contains("règle")) grammarErrors++;
                if (explanation.contains("vocabulaire") || explanation.contains("mot")) vocabErrors++;
                if (explanation.contains("conjug") || explanation.contains("verbe")) conjugationErrors++;
            }
        }

        if (grammarErrors > 0) {
            analysis.append("📖 Grammaire : Révisez les règles de grammaire de ce thème.\n");
        }
        if (vocabErrors > 0) {
            analysis.append("📝 Vocabulaire : Apprenez plus de mots sur ce sujet.\n");
        }
        if (conjugationErrors > 0) {
            analysis.append("🔤 Conjugaison : Entraînez-vous avec des exercices de conjugaison.\n");
        }

        if (grammarErrors == 0 && vocabErrors == 0 && conjugationErrors == 0 && hasErrors) {
            analysis.append("📚 Révisez les explications des cartes où vous avez fait des erreurs.\n");
        }

        analysis.append("\n💪 N'oubliez pas : la pratique régulière est la clé du succès !\n");

        return analysis.toString();
    }

    @FXML
    private void fermerFlashcardsSession() {
        flashcardsSessionContainer.setVisible(false);
        flashcardsSessionContainer.setManaged(false);
    }

    @FXML
    private void fermerFlashcardsResult() {
        flashcardResultContainer.setVisible(false);
        flashcardResultContainer.setManaged(false);
        currentFlashcards.clear();
    }
    /**
     * Nettoie le texte en supprimant les caractères markdown (** et ##)
     */
    private String cleanMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*", "")      // Supprime **
                .replaceAll("##", "")          // Supprime ##
                .replaceAll("#", "")           // Supprime #
                .replaceAll("\\*", "");        // Supprime les * simples
    }


    // Ajoutez cette méthode pour styliser le résultat quand il s'affiche
    private void styleFlashcardResult() {
        flashcardResultArea.setStyle("-fx-background-color: #F8FAFC; -fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: #1E293B;");
    }
}