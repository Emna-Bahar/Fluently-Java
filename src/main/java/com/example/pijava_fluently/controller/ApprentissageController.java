package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.QuizQuestion;
import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.services.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

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
    @FXML private ComboBox<String> comboVocabulaire;

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

    // Flashcards de révision (cartes mémoire)
    @FXML private VBox flashcardsListContainer;
    @FXML private Label flashcardsEmptyLabel;
    @FXML private VBox revisionSessionContainer;
    @FXML private StackPane flashcardCard;
    @FXML private VBox flashcardFront;
    @FXML private VBox flashcardBack;
    @FXML private Label frontQuestion;
    @FXML private Label backAnswer;
    @FXML private Label revisionProgressLabel;
    @FXML private Label revisionScoreLabel;
    @FXML private Button btnVoirFlashcards;

    // Flashcards (ancien Quiz IA)
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

    // Formulaire de génération de cours - suggestions
    @FXML private FlowPane themeSuggestionsContainer;
    @FXML private FlowPane grammaireSuggestionsContainer;
    @FXML private FlowPane vocabulaireSuggestionsContainer;
    @FXML private Label grammaireSelectedLabel;

    // Variables
    private List<FillQuestion> fillQuestionsList = new ArrayList<>();
    private int currentFillIndex = 0;
    private int currentFillScore = 0;
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

    // Variables pour la session flashcards (Quiz IA)
    private List<Flashcard> currentFlashcards = new ArrayList<>();
    private int currentFlashcardIndex = 0;
    private int currentFlashcardScore = 0;
    private ToggleGroup flashcardToggleGroup;
    private List<Boolean> flashcardUserAnswers = new ArrayList<>();
    private List<Integer> flashcardUserSelections = new ArrayList<>();

    private User currentUser;
    private UserProgressService userProgressService = new UserProgressService();
    private final IAService iaService = new IAService();

    // Variables pour la sélection multiple de grammaire
    private Set<String> selectedGrammaireItems = new HashSet<>();

    // Structure pour stocker les flashcards de révision
    private static class RevisionFlashcard {
        String question;
        String answer;
        RevisionFlashcard(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    private List<RevisionFlashcard> revisionFlashcards = new ArrayList<>();
    private int currentRevisionIndex = 0;
    private boolean isCardFlipped = false;
    private Set<Integer> reviewedCards = new HashSet<>();
    private List<CoursInfo> coursGeneresList = new ArrayList<>();

    private static class CoursInfo {
        String theme;
        String grammaire;
        String niveau;
        String contenu;
        LocalDateTime dateCreation;
        CoursInfo(String theme, String grammaire, String niveau, String contenu) {
            this.theme = theme;
            this.grammaire = grammaire;
            this.niveau = niveau;
            this.contenu = contenu;
            this.dateCreation = LocalDateTime.now();
        }
    }

    @FXML private Button btnGenererCours;

    @FXML
    public void initialize() {
        ajouterSuggestionsTheme();
        ajouterSuggestionsGrammaire();
        fieldTheme.textProperty().addListener((obs, oldVal, newVal) -> {
            mettreAJourSuggestionsVocabulaire(newVal);
        });
    }

    private void ajouterSuggestionsTheme() {
        String[] themes = {
                "Voyage et Tourisme", "Affaires et Travail", "Culture et Arts",
                "Vie quotidienne", "Nourriture et Restaurant", "Technologie et Internet",
                "Santé et Bien-être", "Éducation et Carrière", "Sports et Loisirs",
                "Météo et Saisons", "Famille et Amis", "Vêtements et Mode"
        };
        for (String theme : themes) {
            Button btn = createSuggestionButton(theme, "#6C63FF");
            btn.setOnAction(e -> fieldTheme.setText(theme));
            themeSuggestionsContainer.getChildren().add(btn);
        }
    }

    private void ajouterSuggestionsGrammaire() {
        String[] grammaires = {
                "Présent de l'indicatif", "Passé composé", "Imparfait",
                "Futur simple", "Conditionnel présent", "Subjonctif présent",
                "Impératif", "Plus-que-parfait", "Voix passive",
                "Discours indirect", "Comparatifs et superlatifs", "Pronoms relatifs"
        };
        for (String grammaire : grammaires) {
            Button btn = createSuggestionButton(grammaire, "#8B5CF6");
            btn.setOnAction(e -> toggleGrammaireSelection(grammaire, btn));
            grammaireSuggestionsContainer.getChildren().add(btn);
        }
    }

    private Button createSuggestionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 16; " +
                        "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);",
                color
        ));
        btn.setOnMouseEntered(e -> btn.setStyle(String.format(
                "-fx-background-color: derive(%s, -20%%); -fx-text-fill: white; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;",
                color
        )));
        btn.setOnMouseExited(e -> btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;",
                color
        )));
        return btn;
    }

    private void toggleGrammaireSelection(String grammaire, Button btn) {
        if (selectedGrammaireItems.contains(grammaire)) {
            selectedGrammaireItems.remove(grammaire);
            btn.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
        } else {
            selectedGrammaireItems.add(grammaire);
            btn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #4A4D6A; -fx-font-size: 11px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
        }
        mettreAJourChampGrammaire();
    }

    private void mettreAJourChampGrammaire() {
        if (selectedGrammaireItems.isEmpty()) {
            fieldGrammaire.clear();
            grammaireSelectedLabel.setText("Sélectionnés : aucun");
        } else {
            String grammaireText = String.join(", ", selectedGrammaireItems);
            fieldGrammaire.setText(grammaireText);
            grammaireSelectedLabel.setText("Sélectionnés : " + grammaireText);
        }
    }

    private void mettreAJourSuggestionsVocabulaire(String theme) {
        vocabulaireSuggestionsContainer.getChildren().clear();
        if (theme == null || theme.trim().isEmpty()) return;

        Map<String, List<String>> vocabMap = new HashMap<>();
        vocabMap.put("voyage", Arrays.asList("aéroport", "billet", "valise", "passeport", "hôtel", "avion", "destination"));
        vocabMap.put("affaires", Arrays.asList("réunion", "contrat", "client", "projet", "négociation", "rapport"));
        vocabMap.put("culture", Arrays.asList("musée", "tableau", "concert", "théâtre", "exposition", "artiste"));
        vocabMap.put("vie quotidienne", Arrays.asList("maison", "famille", "courses", "repas", "loisirs", "jardin"));
        vocabMap.put("nourriture", Arrays.asList("restaurant", "menu", "plat", "dessert", "serveur", "cuisine"));
        vocabMap.put("technologie", Arrays.asList("ordinateur", "internet", "application", "smartphone", "wifi"));
        vocabMap.put("santé", Arrays.asList("médecin", "hôpital", "médicament", "sport", "bien-être"));
        vocabMap.put("éducation", Arrays.asList("école", "professeur", "cours", "diplôme", "étudiant"));
        vocabMap.put("sport", Arrays.asList("football", "tennis", "équipe", "match", "entraînement"));
        vocabMap.put("météo", Arrays.asList("soleil", "pluie", "température", "vent", "neige"));

        List<String> suggestions = new ArrayList<>();
        String themeLower = theme.toLowerCase();
        for (Map.Entry<String, List<String>> entry : vocabMap.entrySet()) {
            if (themeLower.contains(entry.getKey())) {
                suggestions.addAll(entry.getValue());
                break;
            }
        }
        if (suggestions.isEmpty() && themeLower.contains("voyage")) suggestions = vocabMap.get("voyage");
        else if (suggestions.isEmpty()) suggestions = Arrays.asList("mot1", "mot2", "mot3", "mot4", "mot5");

        for (String mot : suggestions) {
            Button btn = createSuggestionButton(mot, "#10B981");
            btn.setOnAction(e -> {
                String currentText = fieldVocabulaire.getText();
                if (currentText.isEmpty()) {
                    fieldVocabulaire.setText(mot);
                } else if (!currentText.contains(mot)) {
                    fieldVocabulaire.setText(currentText + ", " + mot);
                }
            });
            vocabulaireSuggestionsContainer.getChildren().add(btn);
        }
    }

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

    @FXML private void handleRetour() {
        if (homeController != null) homeController.showLangues();
    }

    @FXML private void handleTest() {
        if (currentUser == null) {
            showAlert("Erreur", "Vous devez être connecté pour passer le test.");
            return;
        }
        try {
            TestService testService = new TestService();
            List<Test> tests = testService.recuperer();
            List<Test> testsNiveau = tests.stream()
                    .filter(t -> t.getLangueId() == langue.getId() && t.getType().equals("Test de niveau"))
                    .collect(Collectors.toList());
            if (testsNiveau.isEmpty()) {
                showAlert("Information", "Aucun test de niveau disponible pour " + langue.getNom() + ".");
                return;
            }
            Test testChoisi = testsNiveau.get(0);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pijava_fluently/fxml/test-passage.fxml"));
            Node vue = loader.load();
            TestPassageEtudiantController ctrl = loader.getController();
            ctrl.initTest(testChoisi, currentUser);
            if (homeController != null) homeController.setContent(vue);
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
            TestService testService = new TestService();
            TestPassageService tps = new TestPassageService();
            List<Integer> idsTestsNiveau = testService.recuperer().stream()
                    .filter(t -> t.getLangueId() == langue.getId() && "Test de niveau".equals(t.getType()))
                    .map(Test::getId)
                    .collect(Collectors.toList());
            if (idsTestsNiveau.isEmpty()) {
                niveauActuelValue.setText("Non défini");
                return;
            }
            Optional<TestPassage> meilleur = tps.recuperer().stream()
                    .filter(p -> p.getUserId() == currentUser.getId())
                    .filter(p -> "termine".equals(p.getStatut()))
                    .filter(p -> idsTestsNiveau.contains(p.getTestId()))
                    .max(Comparator.comparing(p -> p.getDateDebut() != null ? p.getDateDebut() : LocalDateTime.MIN));
            if (meilleur.isPresent()) {
                double pct = meilleur.get().getScoreMax() > 0 ? (double) meilleur.get().getScore() / meilleur.get().getScoreMax() * 100 : 0;
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
                List<Niveau> niveauxLangue = niveaux.stream().filter(n -> n.getIdLangueId() == langue.getId()).collect(Collectors.toList());
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
            coursCompleteParNiveau.clear();
            progressionParNiveau.clear();
            progressionCoursParNiveau.clear();
            for (Map.Entry<String, List<Cours>> entry : coursParNiveau.entrySet()) {
                String niveauKey = entry.getKey();
                List<Cours> cours = entry.getValue();
                Integer niveauId = niveauIdParKey.get(niveauKey);
                if (niveauId == null) {
                    progressionParNiveau.put(niveauKey, 0);
                    progressionCoursParNiveau.put(niveauKey, new HashMap<>());
                    continue;
                }
                User_progress progress = progresses.stream()
                        .filter(p -> p.getUserId() == currentUser.getId() && p.getLangueId() == langue.getId() && p.getNiveauActuelId() == niveauId)
                        .findFirst().orElse(null);
                Map<Integer, Boolean> coursStatus = new HashMap<>();
                int completedCount = 0;
                if (progress != null) {
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
                    for (Cours c : cours) coursStatus.put(c.getId(), false);
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
            Map<Integer, Boolean> coursStatus = progressionCoursParNiveau.getOrDefault(niveauKey, new HashMap<>());
            if (coursStatus.getOrDefault(cours.getId(), false)) {
                showAlert("Information", "Ce cours a déjà été complété !");
                return;
            }
            coursStatus.put(cours.getId(), true);
            progressionCoursParNiveau.put(niveauKey, coursStatus);
            int completedCount = 0;
            for (Boolean status : coursStatus.values()) if (status) completedCount++;
            progressionParNiveau.put(niveauKey, completedCount);
            coursCompleteParNiveau.put(niveauKey + "_" + cours.getId(), cours.getId());
            Integer niveauId = niveauIdParKey.get(niveauKey);
            if (niveauId == null) {
                showAlert("Erreur", "ID du niveau non trouvé.");
                return;
            }
            List<User_progress> progresses = userProgressService.recuperer();
            User_progress existingProgress = progresses.stream()
                    .filter(p -> p.getUserId() == currentUser.getId() && p.getLangueId() == langue.getId() && p.getNiveauActuelId() == niveauId)
                    .findFirst().orElse(null);
            if (existingProgress == null) {
                User_progress newProgress = new User_progress();
                newProgress.setUserId(currentUser.getId());
                newProgress.setLangueId(langue.getId());
                newProgress.setNiveauActuelId(niveauId);
                newProgress.setDernierNumeroCours(completedCount);
                newProgress.setDernierCoursCompleteId(cours.getId());
                newProgress.setTestNiveauComplete(false);
                newProgress.setDateDerniereActivite(LocalDateTime.now());
                userProgressService.ajouter(newProgress);
            } else {
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
        if (progressLabel != null) progressLabel.setText(completed + "/" + cours.size() + " cours");
        int maxCours = Math.min(cours.size(), 6);
        for (int i = 0; i < maxCours; i++) {
            Cours c = cours.get(i);
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
            Button terminerBtn = new Button("Terminer");
            terminerBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
            terminerBtn.setOnMouseEntered(e -> terminerBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 12 5 12; -fx-cursor: hand;"));
            terminerBtn.setOnMouseExited(e -> terminerBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 12 5 12; -fx-cursor: hand;"));
            terminerBtn.setOnAction(e -> marquerCoursComplete(cours));
            cercle.getChildren().addAll(circle, coursLabel, terminerBtn);
        }
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
        cercle.setOnMouseClicked(e -> ouvrirCours(cours));
        return cercle;
    }

    private int trouverNiveauId(String niveauKey) {
        for (Map.Entry<Integer, Niveau> entry : niveauParDifficulte.entrySet()) {
            if (entry.getValue().getDifficulte().contains(niveauKey)) return entry.getValue().getId();
        }
        return 1;
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

            // POUR LES LIENS YOUTUBE : les garder tels quels
            if (lowerRes.contains("youtube.com") || lowerRes.contains("youtu.be")) {
                youtubeLinks.add(res);
            }
            // POUR LES FICHIERS LOCAUX : ne PAS convertir ici, laisser le chemin original
            // La conversion se fera dans ouvrirRessource()
            else {
                // Ajouter le chemin original, sans conversion
                if (lowerRes.endsWith(".pdf")) {
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
        }

        if (!youtubeLinks.isEmpty()) categoriesContainer.getChildren().add(createCategoryBox("🎬 Vidéos YouTube", youtubeLinks, "#FF6B6B"));
        if (!pdfFiles.isEmpty()) categoriesContainer.getChildren().add(createCategoryBox("📄 Documents PDF", pdfFiles, "#4ECDC4"));
        if (!videoFiles.isEmpty()) categoriesContainer.getChildren().add(createCategoryBox("🎥 Vidéos", videoFiles, "#A8E6CF"));
        if (!audioFiles.isEmpty()) categoriesContainer.getChildren().add(createCategoryBox("🎵 Audios", audioFiles, "#FFD93D"));
        if (!imageFiles.isEmpty()) categoriesContainer.getChildren().add(createCategoryBox("🖼️ Images", imageFiles, "#C5E8F7"));
        if (!otherFiles.isEmpty()) categoriesContainer.getChildren().add(createCategoryBox("📎 Autres fichiers", otherFiles, "#D4A5A5"));
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
            if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            if (fileName.length() > 40) fileName = fileName.substring(0, 37) + "...";
            Label itemLabel = new Label(fileName);
            itemLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A4D6A;");
            itemLabel.setMaxWidth(350);
            itemLabel.setWrapText(true);
            Button openBtn = new Button("Ouvrir");
            openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;");
            openBtn.setOnMouseEntered(e -> openBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;"));
            openBtn.setOnMouseExited(e -> openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15; -fx-cursor: hand;"));

            // Pour les fichiers locaux, utiliser le chemin absolu déjà converti
            final String finalPath = item;
            openBtn.setOnAction(e -> ouvrirRessource(finalPath));

            itemRow.getChildren().addAll(itemIcon, itemLabel, openBtn);
            HBox.setHgrow(itemLabel, Priority.ALWAYS);
            itemsBox.getChildren().add(itemRow);
        }
        categoryBox.getChildren().addAll(header, itemsBox);
        return categoryBox;
    }

    private void ouvrirRessource(String chemin) {
        try {
            // Vérifier si c'est un lien YouTube
            if (chemin.contains("youtube.com") || chemin.contains("youtu.be")) {
                Desktop.getDesktop().browse(new URI(chemin));
            }
            // Vérifier si c'est un fichier local
            else {
                File file;
                // Si le chemin est déjà absolu
                if (chemin.startsWith("C:/") || chemin.startsWith("file:/")) {
                    file = new File(chemin);
                }
                // Si le chemin est relatif (commence par /uploads/)
                else if (chemin.startsWith("/uploads/")) {
                    file = new File("C:/xampp/htdocs/fluently/public" + chemin);
                }
                // Sinon, essayer directement
                else {
                    file = new File(chemin);
                }

                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("Erreur", "Fichier introuvable : " + chemin);
                }
            }
        } catch (IOException | URISyntaxException e) {
            showAlert("Erreur", "Impossible d'ouvrir la ressource : " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML private void ouvrirFormulaireCours() {
        formCoursContainer.setVisible(true);
        formCoursContainer.setManaged(true);
    }

    @FXML private void fermerFormulaireCours() {
        formCoursContainer.setVisible(false);
        formCoursContainer.setManaged(false);
        effacerFormulaire();
    }

    @FXML private void effacerFormulaire() {
        fieldTheme.clear();
        fieldGrammaire.clear();
        fieldVocabulaire.clear();
        selectedGrammaireItems.clear();
        for (javafx.scene.Node node : grammaireSuggestionsContainer.getChildren()) {
            if (node instanceof Button) {
                node.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
            }
        }
        grammaireSelectedLabel.setText("Sélectionnés : aucun");
        niveauSelectionne = null;
        niveauSelectionneLabel.setText("Niveau sélectionné : Aucun");
        resetNiveauStyles();
        apercuCours.clear();
        btnExporterPDF.setDisable(true);
        dernierCoursGenere = null;
    }

    @FXML private void genererCours() {
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
        btnExporterPDF.setDisable(true);
        btnGenererCours.setDisable(true);
        showLoading(true);
        apercuCours.clear();
        new Thread(() -> {
            try {
                String coursGenere = iaService.genererCours(langueNom, theme, grammaire,
                        vocabulaire.isEmpty() ? "vocabulaire général du thème" : vocabulaire, difficulte);
                Platform.runLater(() -> {
                    apercuCours.setText(coursGenere);
                    dernierCoursGenere = coursGenere;
                    btnExporterPDF.setDisable(false);
                    btnGenererCours.setDisable(false);
                    showLoading(false);
                    showAlert("Succès", "✅ Votre cours personnalisé a été généré par l'IA !");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    btnGenererCours.setDisable(false);
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

    private void genererCoursLocal(String theme, String grammaire, String vocabulaire, int difficulte, String langueNom) {
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
        if (vocabulaire.isEmpty()) cours.append("- Mot 1\n- Mot 2\n- Mot 3\n");
        else {
            String[] mots = vocabulaire.split(",");
            for (String mot : mots) cours.append("- ").append(mot.trim()).append("\n");
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
            if (show) loadingText.setText("🤖 Génération du cours en cours...");
        }
    }

    @FXML private void exporterPDF() {
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
            String fileName = "cours_" + niveauSelectionne + "_" + langueNom + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            // Chemin vers le dossier partagé
            String baseDir = "C:/xampp/htdocs/fluently/public/uploads/cours_pdf/";
            String langueDir = baseDir + langueNom + "/";
            File dir = new File(langueDir);
            if (!dir.exists()) dir.mkdirs();
            String filePath = langueDir + fileName;

            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(60, 60, 60, 60);

            com.itextpdf.kernel.colors.DeviceRgb PRIMARY_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(79, 70, 229);
            com.itextpdf.kernel.colors.DeviceRgb SECONDARY_COLOR = new com.itextpdf.kernel.colors.DeviceRgb(139, 92, 246);

            // Titre
            Paragraph titlePara = new Paragraph("📚 " + langueNom.toUpperCase())
                    .setFontSize(32).setBold().setFontColor(PRIMARY_COLOR).setTextAlignment(TextAlignment.CENTER).setMarginTop(100);
            document.add(titlePara);

            // Sous-titre
            Paragraph subtitlePara = new Paragraph("Cours personnalisé - Niveau " + niveauSelectionne)
                    .setFontSize(18).setFontColor(SECONDARY_COLOR).setTextAlignment(TextAlignment.CENTER).setMarginTop(10).setMarginBottom(50);
            document.add(subtitlePara);

            // Date
            Paragraph datePara = new Paragraph("Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                    .setFontSize(11).setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(100, 116, 139)).setTextAlignment(TextAlignment.CENTER).setMarginBottom(100);
            document.add(datePara);

            // Ligne de séparation
            com.itextpdf.layout.element.Table lineTable = new com.itextpdf.layout.element.Table(1);
            lineTable.setWidth(com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 120);
            lineTable.setMarginTop(20);
            lineTable.setMarginBottom(20);
            com.itextpdf.layout.element.Cell lineCell = new com.itextpdf.layout.element.Cell();
            lineCell.setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(PRIMARY_COLOR, 2));
            lineCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            lineCell.setPadding(0);
            lineCell.setHeight(5);
            lineTable.addCell(lineCell);
            document.add(lineTable);

            // Contenu du cours
            String contenu = dernierCoursGenere;
            contenu = contenu.replaceAll("\\*\\*", "").replaceAll("#", "").replaceAll("--", "—").replaceAll("\\|\\|", "");
            String[] lignes = contenu.split("\n");

            for (String ligne : lignes) {
                if (ligne.trim().isEmpty()) {
                    document.add(new Paragraph(" "));
                    continue;
                }
                String cleanLine = ligne.trim();

                if (cleanLine.contains("EXERCICES") || cleanLine.startsWith("✏️")) {
                    Paragraph p = new Paragraph("✏️ " + cleanLine.replaceAll("✏️", "").trim())
                            .setFontSize(20).setBold().setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(16, 185, 129)).setMarginTop(30).setMarginBottom(15);
                    document.add(p);
                } else if (cleanLine.contains("CORRECTION") || cleanLine.startsWith("✅")) {
                    Paragraph p = new Paragraph("✅ " + cleanLine.replaceAll("✅", "").trim())
                            .setFontSize(20).setBold().setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(16, 185, 129)).setMarginTop(30).setMarginBottom(15);
                    document.add(p);
                } else if (cleanLine.contains("INTRODUCTION") || cleanLine.startsWith("🎯")) {
                    Paragraph p = new Paragraph("🎯 " + cleanLine.replaceAll("🎯", "").trim())
                            .setFontSize(20).setBold().setFontColor(PRIMARY_COLOR).setMarginTop(25).setMarginBottom(10);
                    document.add(p);
                } else if (cleanLine.contains("VOCABULAIRE") || cleanLine.startsWith("📝")) {
                    Paragraph p = new Paragraph("📝 " + cleanLine.replaceAll("📝", "").trim())
                            .setFontSize(18).setBold().setFontColor(SECONDARY_COLOR).setMarginTop(20).setMarginBottom(10);
                    document.add(p);
                } else if (cleanLine.contains("GRAMMAIRE") || cleanLine.startsWith("📖")) {
                    Paragraph p = new Paragraph("📖 " + cleanLine.replaceAll("📖", "").trim())
                            .setFontSize(18).setBold().setFontColor(SECONDARY_COLOR).setMarginTop(20).setMarginBottom(10);
                    document.add(p);
                } else if (cleanLine.startsWith("•") || cleanLine.startsWith("-") || cleanLine.startsWith("*")) {
                    String bulletLine = cleanLine.replaceFirst("[•\\-*] ", "• ");
                    Paragraph p = new Paragraph("    " + bulletLine).setFontSize(11).setMarginLeft(20).setMarginBottom(3);
                    document.add(p);
                } else if (!cleanLine.isEmpty()) {
                    Paragraph p = new Paragraph(cleanLine).setFontSize(11).setMarginBottom(6);
                    document.add(p);
                }
            }

            // Pied de page
            com.itextpdf.layout.element.Table footerTable = new com.itextpdf.layout.element.Table(1);
            footerTable.setWidth(com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 120);
            footerTable.setMarginTop(40);
            com.itextpdf.layout.element.Cell footerCell = new com.itextpdf.layout.element.Cell();
            footerCell.setBackgroundColor(PRIMARY_COLOR);
            footerCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            footerCell.setPadding(15);
            footerCell.setTextAlignment(TextAlignment.CENTER);
            footerCell.setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(10));
            Paragraph footerPara = new Paragraph("✨ Cours généré par Fluently - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
            footerCell.add(footerPara);
            footerTable.addCell(footerCell);
            document.add(footerTable);

            document.close();

            // Ajouter le cours dans la liste
            ajouterCoursDansNiveau(niveauSelectionne, fileName, filePath);


            showAlert("Succès", "✅ PDF exporté avec succès !\n\nEmplacement : " + filePath);
            Desktop.getDesktop().open(dir);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'exporter le PDF : " + e.getMessage());
        }
    }
    // Nouvelles variables pour le Quiz IA amélioré
    @FXML private ToggleButton levelA1Btn, levelA2Btn, levelB1Btn, levelB2Btn, levelC1Btn;
    @FXML private Label selectedLevelLabel;
    @FXML private RadioButton nbQuestions5, nbQuestions10, nbQuestions15;
    @FXML private CheckBox typeGrammar, typeVocabulary, typeConjugation;
    @FXML private ProgressBar quizProgressBar;
    @FXML private Label quizNiveauLabel;
    @FXML private StackPane resultIconContainer;
    @FXML private Label resultIcon, resultTitle, resultScore, resultMessage;
    @FXML private ProgressBar resultProgressBar;
    @FXML private ScrollPane resultDetailsScroll;
    @FXML private VBox resultDetailsContainer;

    // Variable pour stocker le niveau sélectionné
    private String selectedQuizLevel = null;
    // Gestion du niveau
    @FXML private void setLevelA1() { setQuizLevel("A1", levelA1Btn); }
    @FXML private void setLevelA2() { setQuizLevel("A2", levelA2Btn); }
    @FXML private void setLevelB1() { setQuizLevel("B1", levelB1Btn); }
    @FXML private void setLevelB2() { setQuizLevel("B2", levelB2Btn); }
    @FXML private void setLevelC1() { setQuizLevel("C1", levelC1Btn); }

    private void setQuizLevel(String level, ToggleButton selectedBtn) {
        selectedQuizLevel = level;
        selectedLevelLabel.setText("Niveau sélectionné : " + level);
        quizNiveauLabel.setText(level);

        ToggleButton[] btns = {levelA1Btn, levelA2Btn, levelB1Btn, levelB2Btn, levelC1Btn};
        for (ToggleButton btn : btns) {
            if (btn == selectedBtn) {
                btn.setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");
                btn.setSelected(true);
            } else {
                String originalStyle = btn.getText().startsWith("A") ? "-fx-background-color: #F3E8FF; -fx-text-fill: #6B21A5;" :
                        btn.getText().startsWith("B") ? "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309;" :
                                "-fx-background-color: #EFF6FF; -fx-text-fill: #1E40AF;";
                btn.setStyle(originalStyle + " -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");
                btn.setSelected(false);
            }
        }
    }

    private void ajouterCoursDansNiveau(String niveau, String fileName, String filePath) {
        Cours coursPerso = new Cours();
        coursPerso.setId(-(int)(System.currentTimeMillis() % Integer.MAX_VALUE));
        coursPerso.setNumero(getNextCoursNumber(niveau));
        coursPerso.setRessource(filePath);
        coursPerso.setDateCreation(LocalDateTime.now().toLocalDate());
        List<Cours> coursList = coursParNiveau.getOrDefault(niveau, new ArrayList<>());
        coursList.add(coursPerso);
        coursParNiveau.put(niveau, coursList);
        afficherNiveaux();
    }

    private int getNextCoursNumber(String niveau) {
        List<Cours> cours = coursParNiveau.getOrDefault(niveau, new ArrayList<>());
        int maxNumero = 0;
        for (Cours c : cours) if (c.getNumero() > maxNumero) maxNumero = c.getNumero();
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
        resetNiveauStyles();
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
            if (circle instanceof StackPane) circle.setStyle(style);
        }
    }

    @FXML private VBox niveauA1Btn, niveauA2Btn, niveauB1Btn, niveauB2Btn, niveauC1Btn, niveauC2Btn;

    private VBox createPdfCard(Cours cours) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPrefWidth(180);
        card.setPrefHeight(160);
        card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F0F2FF; -fx-background-radius: 16; -fx-border-color: #6C63FF; -fx-border-radius: 16; -fx-border-width: 2;");
            card.setTranslateY(-4);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1;");
            card.setTranslateY(0);
        });
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(60, 60);
        iconContainer.setStyle("-fx-background-color: #FFE4E6; -fx-background-radius: 30;");
        Label pdfIcon = new Label("📄");
        pdfIcon.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(pdfIcon);
        String niveau = trouverNiveauDuCours(cours);
        Label niveauBadge = new Label(niveau != null ? niveau : "Cours");
        niveauBadge.setStyle("-fx-background-color: " + getCouleurNiveau(niveau) + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 3 10 3 10;");
        Label numeroLabel = new Label("Cours N°" + cours.getNumero());
        numeroLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");
        String dateStr = cours.getDateCreation() != null ? cours.getDateCreation().toString() : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #8A8FA8;");
        Button openBtn = new Button("📖 Ouvrir");
        openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;");
        openBtn.setOnMouseEntered(e -> openBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnMouseExited(e -> openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnAction(e -> ouvrirPdfCours(cours));
        card.getChildren().addAll(iconContainer, niveauBadge, numeroLabel, dateLabel, openBtn);
        VBox.setMargin(iconContainer, new Insets(10, 0, 0, 0));
        return card;
    }

    private void ouvrirPdfCours(Cours cours) {
        if (cours.getRessource() != null) {
            String absolutePath = getAbsolutePath(cours.getRessource());
            File pdfFile = new File(absolutePath);
            if (pdfFile.exists()) {
                try { Desktop.getDesktop().open(pdfFile); }
                catch (IOException e) { showAlert("Erreur", "Impossible d'ouvrir le PDF : " + e.getMessage()); }
            } else showAlert("Erreur", "Fichier PDF introuvable : " + cours.getRessource());
        }
    }

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

    private String trouverNiveauDuCours(Cours cours) {
        for (Map.Entry<String, List<Cours>> entry : coursParNiveau.entrySet()) {
            if (entry.getValue().contains(cours)) return entry.getKey();
        }
        return null;
    }

    private void afficherCoursPdfGeneres() {
        if (coursPdfContainer == null) return;
        coursPdfContainer.getChildren().clear();
        String langueActuelle = this.langue != null ? this.langue.getNom() : null;
        if (langueActuelle == null) {
            emptyCoursMessage.setVisible(true);
            emptyCoursMessage.setManaged(true);
            coursPdfScroll.setVisible(false);
            coursPdfScroll.setManaged(false);
            totalCoursCount.setText("0 cours");
            return;
        }
        // NOUVEAU CHEMIN VERS LE DOSSIER PARTAGÉ
        String langueDir = "C:/xampp/htdocs/fluently/public/uploads/cours_pdf/" + langueActuelle + "/";
        File dossierLangue = new File(langueDir);
        List<File> fichiersPdf = new ArrayList<>();
        if (dossierLangue.exists() && dossierLangue.isDirectory()) {
            File[] fichiers = dossierLangue.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (fichiers != null) {
                fichiersPdf = Arrays.asList(fichiers);
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
        for (File pdfFile : fichiersPdf) {
            VBox card = createPdfCardFromFile(pdfFile);
            coursPdfContainer.getChildren().add(card);
        }
    }

    private VBox createPdfCardFromFile(File pdfFile) {
        String fileName = pdfFile.getName();
        String niveau = extraireNiveauFromFileName(fileName);
        String dateStr = extraireDateFromFileName(fileName);
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPrefWidth(180);
        card.setPrefHeight(160);
        card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F0F2FF; -fx-background-radius: 16; -fx-border-color: #6C63FF; -fx-border-radius: 16; -fx-border-width: 2;");
            card.setTranslateY(-4);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 16; -fx-border-color: #E0E3F0; -fx-border-radius: 16; -fx-border-width: 1;");
            card.setTranslateY(0);
        });
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(60, 60);
        iconContainer.setStyle("-fx-background-color: #FFE4E6; -fx-background-radius: 30;");
        Label pdfIcon = new Label("📄");
        pdfIcon.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(pdfIcon);
        Label niveauBadge = new Label(niveau != null ? niveau : "Cours");
        niveauBadge.setStyle("-fx-background-color: " + getCouleurNiveau(niveau) + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 3 10 3 10;");
        String displayName = fileName.length() > 25 ? fileName.substring(0, 22) + "..." : fileName;
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(160);
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #8A8FA8;");
        Button openBtn = new Button("📖 Ouvrir");
        openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;");
        openBtn.setOnMouseEntered(e -> openBtn.setStyle("-fx-background-color: #5849C4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnMouseExited(e -> openBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 5 15 5 15; -fx-cursor: hand;"));
        openBtn.setOnAction(e -> {
            try { Desktop.getDesktop().open(pdfFile); }
            catch (IOException ex) { showAlert("Erreur", "Impossible d'ouvrir le PDF : " + ex.getMessage()); }
        });
        card.getChildren().addAll(iconContainer, niveauBadge, nameLabel, dateLabel, openBtn);
        VBox.setMargin(iconContainer, new Insets(10, 0, 0, 0));
        return card;
    }

    private String extraireNiveauFromFileName(String fileName) {
        if (fileName.startsWith("cours_")) {
            String[] parts = fileName.split("_");
            if (parts.length >= 2) {
                String niveau = parts[1];
                if (niveau.matches("A1|A2|B1|B2|C1|C2")) return niveau;
            }
        }
        return "Cours";
    }

    private String extraireDateFromFileName(String fileName) {
        try {
            String[] parts = fileName.split("_");
            if (parts.length >= 4) {
                String datePart = parts[3];
                if (datePart.length() == 8) {
                    return datePart.substring(0, 4) + "-" + datePart.substring(4, 6) + "-" + datePart.substring(6, 8);
                }
            }
        } catch (Exception e) {}
        return "";
    }

    @FXML private void rechercherVideosYouTube() {
        String query = youtubeSearchField.getText().trim();
        if (query.isEmpty()) {
            showAlert("Information", "Veuillez entrer un mot-clé pour la recherche.");
            return;
        }
        String langueNom = this.langue != null ? this.langue.getNom() : "";
        String rechercheComplet = query + " " + langueNom + " cours";
        youtubeLoading.setVisible(true);
        youtubeLoading.setManaged(true);
        youtubeScrollPane.setVisible(false);
        youtubeScrollPane.setManaged(false);
        youtubeEmptyMessage.setVisible(false);
        youtubeEmptyMessage.setManaged(false);
        youtubeResultsContainer.getChildren().clear();
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
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F0F2FF; -fx-background-radius: 12; -fx-border-color: #6C63FF; -fx-border-radius: 12; -fx-border-width: 2; -fx-padding: 12;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #F8F9FD; -fx-background-radius: 12; -fx-border-color: #E0E3F0; -fx-border-radius: 12; -fx-padding: 12;");
        });
        Label titleLabel = new Label(video.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1D2E;");
        titleLabel.setWrapText(true);
        String desc = video.getDescription();
        if (desc.length() > 100) desc = desc.substring(0, 100) + "...";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8FA8;");
        descLabel.setWrapText(true);
        Button watchBtn = new Button("▶ Regarder");
        watchBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
        watchBtn.setOnAction(e -> ouvrirVideoYouTube(video.getVideoUrl()));
        card.getChildren().addAll(titleLabel, descLabel, watchBtn);
        return card;
    }

    private void ouvrirVideoYouTube(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); }
        catch (IOException | URISyntaxException e) { showAlert("Erreur", "Impossible d'ouvrir la vidéo"); }
    }

    @FXML private void chercherMotDictionnaire() {
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
        String cleanResultat = cleanMarkdown(resultat);
        String definition = cleanResultat;
        String exemple = "";
        String synonymes = "";
        if (cleanResultat.contains("DÉFINITION:")) {
            String[] parts = cleanResultat.split("DÉFINITION:");
            if (parts.length > 1) definition = parts[1].split("EXEMPLE:")[0].trim();
        }
        if (cleanResultat.contains("EXEMPLE:")) {
            String[] parts = cleanResultat.split("EXEMPLE:");
            if (parts.length > 1) exemple = parts[1].split("SYNONYMES:")[0].trim();
        }
        if (cleanResultat.contains("SYNONYMES:")) {
            String[] parts = cleanResultat.split("SYNONYMES:");
            if (parts.length > 1) synonymes = parts[1].trim();
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

    @FXML private void genererQuiz() {
        String niveau = gameLevelCombo.getValue();
        if (niveau == null) {
            showAlert("Information", "Veuillez sélectionner un niveau pour le quiz.");
            return;
        }
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        gameLoading.setVisible(true);
        gameLoading.setManaged(true);
        quizContainer.setVisible(false);
        quizContainer.setManaged(false);
        new Thread(() -> {
            try {
                String prompt = "Génère un quiz de vocabulaire en " + langueNom + " pour un niveau " + niveau +
                        ". Format attendu (5 questions):\n" +
                        "Q1: [question]?|Option1|Option2|Option3|Option4|Réponse correcte (1-4)\n" +
                        "Q2: [question]?|Option1|Option2|Option3|Option4|Réponse correcte\n...";
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
        if (questionsList.isEmpty()) creerQuizSecours();
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
        quizAnswersContainer.getChildren().clear();
        currentToggleGroup = new ToggleGroup();

        // Style amélioré pour les options - texte visible
        for (int i = 0; i < q.getOptions().length; i++) {
            RadioButton rb = new RadioButton(q.getOptions()[i]);
            rb.setToggleGroup(currentToggleGroup);
            rb.setUserData(i + 1);
            // Style avec texte foncé sur fond clair
            rb.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E293B; -fx-padding: 10 15; " +
                    "-fx-background-color: #F1F5F9; -fx-background-radius: 10; -fx-cursor: hand;");
            rb.setPrefWidth(Double.MAX_VALUE);

            // Effet hover
            rb.setOnMouseEntered(e -> rb.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E293B; -fx-padding: 10 15; " +
                    "-fx-background-color: #E2E8F0; -fx-background-radius: 10; -fx-cursor: hand;"));
            rb.setOnMouseExited(e -> rb.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E293B; -fx-padding: 10 15; " +
                    "-fx-background-color: #F1F5F9; -fx-background-radius: 10; -fx-cursor: hand;"));

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
        for (Node node : quizAnswersContainer.getChildren()) {
            if (node instanceof RadioButton) ((RadioButton) node).setDisable(true);
        }
        currentQuestionIndex++;
        quizScore.setText("Score: " + currentScore);
        if (currentQuestionIndex >= questionsList.size()) terminerQuiz();
    }

    private void showFeedback(String message) {
        HBox feedbackBox = new HBox(10);
        feedbackBox.setAlignment(Pos.CENTER);
        feedbackBox.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 10; -fx-padding: 10 15;");

        Label icon = new Label(message.contains("✅") ? "✅" : "❌");
        icon.setStyle("-fx-font-size: 16px;");

        Label feedback = new Label(message);
        feedback.setStyle("-fx-text-fill: #92400E; -fx-font-size: 13px; -fx-font-weight: bold;");

        feedbackBox.getChildren().addAll(icon, feedback);
        quizAnswersContainer.getChildren().add(feedbackBox);

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            Platform.runLater(() -> quizAnswersContainer.getChildren().remove(feedbackBox));
        }).start();
    }

    @FXML private void nextQuestion() { afficherQuestion(); }

    private void terminerQuiz() {
        nextQuestionBtn.setVisible(false);
        nextQuestionBtn.setManaged(false);
        int totalPoints = questionsList.size() * 10;
        double pourcentage = (double) currentScore / totalPoints * 100;

        // Créer un conteneur stylisé pour les résultats
        VBox resultContent = new VBox(15);
        resultContent.setAlignment(Pos.CENTER);
        resultContent.setPadding(new Insets(20));
        resultContent.setStyle("-fx-background-color: linear-gradient(to bottom, #F8FAFC, #F1F5F9); -fx-background-radius: 20;");

        // Couleur selon le score
        String scoreColor;
        String titleIcon;
        if (pourcentage >= 80) {
            scoreColor = "#10B981";
            titleIcon = "🏆";
        } else if (pourcentage >= 60) {
            scoreColor = "#F59E0B";
            titleIcon = "👍";
        } else if (pourcentage >= 40) {
            scoreColor = "#3B82F6";
            titleIcon = "📚";
        } else {
            scoreColor = "#EF4444";
            titleIcon = "💪";
        }

        // Titre
        Label titleLabel = new Label(titleIcon + " RÉSULTATS DU QUIZ " + titleIcon);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        titleLabel.setAlignment(Pos.CENTER);

        // Score
        Label scoreLabel = new Label(currentScore + " / " + totalPoints);
        scoreLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        scoreLabel.setAlignment(Pos.CENTER);

        // Pourcentage avec barre
        Label percentageLabel = new Label(String.format("%.1f%%", pourcentage));
        percentageLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        ProgressBar progressBar = new ProgressBar(pourcentage / 100);
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: " + scoreColor + ";");

        // Message d'appréciation
        String appreciation;
        if (pourcentage >= 80) {
            appreciation = "🏆 EXCELLENT ! Vous maîtrisez très bien ce niveau !";
        } else if (pourcentage >= 60) {
            appreciation = "👍 TRÈS BIEN ! Continuez à pratiquer !";
        } else if (pourcentage >= 40) {
            appreciation = "📚 PAS MAL ! Révisez un peu et réessayez !";
        } else {
            appreciation = "💪 CONTINUEZ VOS EFFORTS ! Refaites le quiz après révision !";
        }

        Label appreciationLabel = new Label(appreciation);
        appreciationLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + "; -fx-wrap-text: true;");
        appreciationLabel.setAlignment(Pos.CENTER);
        appreciationLabel.setMaxWidth(400);

        // Séparateur
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #E2E8F0;");

        // Détail des réponses
        Label detailsTitle = new Label("📝 DÉTAIL DE VOS RÉPONSES");
        detailsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        VBox detailsContainer = new VBox(10);
        detailsContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 15;");
        detailsContainer.setMaxHeight(300);

        ScrollPane detailsScroll = new ScrollPane(detailsContainer);
        detailsScroll.setFitToWidth(true);
        detailsScroll.setPrefHeight(250);
        detailsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        // Afficher chaque question avec la réponse
        for (int i = 0; i < questionsList.size(); i++) {
            QuizQuestion q = questionsList.get(i);
            boolean estCorrecte = (i < currentScore / 10);

            HBox questionRow = new HBox(10);
            questionRow.setAlignment(Pos.CENTER_LEFT);
            questionRow.setStyle("-fx-padding: 10; -fx-background-color: " + (estCorrecte ? "#D1FAE5" : "#FEE2E2") + "; -fx-background-radius: 8;");

            Label statusIcon = new Label(estCorrecte ? "✅" : "❌");
            statusIcon.setStyle("-fx-font-size: 16px;");

            VBox questionContent = new VBox(5);

            Label questionText = new Label("Q" + (i+1) + ": " + q.getQuestion());
            questionText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-wrap-text: true;");

            String bonneReponse = q.getOptions()[q.getCorrectAnswer() - 1];
            Label answerText = new Label("✓ Bonne réponse : " + bonneReponse);
            answerText.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981;");

            questionContent.getChildren().addAll(questionText, answerText);
            questionRow.getChildren().addAll(statusIcon, questionContent);

            detailsContainer.getChildren().add(questionRow);
        }

        // Boutons d'action
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button replayBtn = new Button("🔄 Rejouer");
        replayBtn.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 24; -fx-cursor: hand;");
        replayBtn.setOnAction(e -> startGame());

        Button closeBtn = new Button("✕ Fermer");
        closeBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 24; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            quizResultContainer.setVisible(false);
            quizContainer.setVisible(false);
        });

        buttonBox.getChildren().addAll(replayBtn, closeBtn);

        // Assembler tout
        resultContent.getChildren().addAll(
                titleLabel, scoreLabel, percentageLabel, progressBar,
                appreciationLabel, separator, detailsTitle, detailsScroll, buttonBox
        );

        // Mettre à jour le conteneur existant
        quizResultContainer.getChildren().clear();
        quizResultContainer.getChildren().add(resultContent);
        quizResultContainer.setVisible(true);
        quizResultContainer.setManaged(true);
    }

    @FXML private void startGame() {
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
        quizContainer.setVisible(false); quizContainer.setManaged(false);
        fillGameContainer.setVisible(false); fillGameContainer.setManaged(false);
        gameLoading.setVisible(true); gameLoading.setManaged(true);
        new Thread(() -> {
            try {
                String prompt = genererPromptJeu(gameType, langueNom, niveau);
                String resultat = iaService.genererCours(langueNom, "jeu", prompt, "", 3);
                Platform.runLater(() -> {
                    gameLoading.setVisible(false); gameLoading.setManaged(false);
                    if (gameType.contains("Quiz")) parserQuiz(resultat);
                    else if (gameType.contains("Compléter")) parserFillGame(resultat);
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
                String reponseRaw = parts[5].trim();
                String reponseClean = reponseRaw.replaceAll("\\*\\*", "").replaceAll("\\*", "").trim();
                try {
                    int correctAnswer = Integer.parseInt(reponseClean);
                    fillQuestionsList.add(new FillQuestion(phrase, options, correctAnswer));
                } catch (NumberFormatException e) {
                    String digits = reponseClean.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        int correctAnswer = Integer.parseInt(digits.substring(0, 1));
                        fillQuestionsList.add(new FillQuestion(phrase, options, correctAnswer));
                    }
                }
            }
        }
        if (fillQuestionsList.isEmpty()) creerFillGameSecours();
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
        for (Node node : fillOptionsContainer.getChildren()) {
            if (node instanceof Button) ((Button) node).setDisable(true);
        }
        if (reponseChoisie == q.getCorrectAnswer()) {
            currentFillScore += 10;
            btn.setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15;");
            showFillFeedback("✅ Bonne réponse ! +10 points", true);
        } else {
            btn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15;");
            String bonneReponse = q.getOptions()[q.getCorrectAnswer() - 1];
            showFillFeedback("❌ Mauvaise réponse. La bonne réponse était : " + bonneReponse, false);
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
        if (currentFillIndex >= fillQuestionsList.size()) terminerFillGame();
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

    @FXML private void nextFillQuestion() { afficherFillQuestion(); }

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

    @FXML private void prononcer() {
        String texte = pronunciationTextField.getText().trim();
        if (texte.isEmpty()) {
            showAlert("Information", "Veuillez entrer un mot ou une phrase à écouter.");
            return;
        }
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        String langueCode = prononciationService.getLangueCode(langueNom);
        pronunciationLoading.setVisible(true);
        pronunciationLoading.setManaged(true);
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

    @FXML private void suggestionBonjour() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) pronunciationTextField.setText("Hello");
        else if (langueNom.equalsIgnoreCase("Espagnol")) pronunciationTextField.setText("Hola");
        else if (langueNom.equalsIgnoreCase("Allemand")) pronunciationTextField.setText("Hallo");
        else if (langueNom.equalsIgnoreCase("Italien")) pronunciationTextField.setText("Ciao");
        else pronunciationTextField.setText("Bonjour");
        prononcer();
    }

    @FXML private void suggestionMerci() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) pronunciationTextField.setText("Thank you");
        else if (langueNom.equalsIgnoreCase("Espagnol")) pronunciationTextField.setText("Gracias");
        else if (langueNom.equalsIgnoreCase("Allemand")) pronunciationTextField.setText("Danke");
        else if (langueNom.equalsIgnoreCase("Italien")) pronunciationTextField.setText("Grazie");
        else pronunciationTextField.setText("Merci");
        prononcer();
    }

    @FXML private void suggestionComment() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) pronunciationTextField.setText("How are you?");
        else if (langueNom.equalsIgnoreCase("Espagnol")) pronunciationTextField.setText("¿Cómo estás?");
        else if (langueNom.equalsIgnoreCase("Allemand")) pronunciationTextField.setText("Wie geht es dir?");
        else if (langueNom.equalsIgnoreCase("Italien")) pronunciationTextField.setText("Come stai?");
        else pronunciationTextField.setText("Comment allez-vous ?");
        prononcer();
    }

    @FXML private void suggestionAmour() {
        String langueNom = this.langue != null ? this.langue.getNom() : "Français";
        if (langueNom.equalsIgnoreCase("Anglais")) pronunciationTextField.setText("I love you");
        else if (langueNom.equalsIgnoreCase("Espagnol")) pronunciationTextField.setText("Te quiero");
        else if (langueNom.equalsIgnoreCase("Allemand")) pronunciationTextField.setText("Ich liebe dich");
        else if (langueNom.equalsIgnoreCase("Italien")) pronunciationTextField.setText("Ti amo");
        else pronunciationTextField.setText("Je t'aime");
        prononcer();
    }

    @FXML private void ouvrirFlashcards() { ouvrirFormulaireFlashcards(); }
    @FXML private void ouvrirFormulaireFlashcards() {
        flashcardsFormContainer.setVisible(true);
        flashcardsFormContainer.setManaged(true);
        flashcardsPromptArea.clear();
        flashcardsLevelCombo.setValue(niveauActuelValue.getText() != null ? niveauActuelValue.getText() : "B1 - Intermédiaire");
    }
    @FXML private void fermerFormulaireFlashcards() {
        flashcardsFormContainer.setVisible(false);
        flashcardsFormContainer.setManaged(false);
    }
    @FXML private void genererFlashcardsIA() {
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
                    if (!flashcards.isEmpty()) startFlashcardSession(flashcards);
                    else {
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
        if (flashcards.isEmpty()) flashcards = getFallbackFlashcards(langue != null ? langue.getNom() : "Français");
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
                    new String[]{"Goed", "Went", "Gone", "Going"}, 2, "'Went' is correct."));
            flashcards.add(new Flashcard("Choose the correct sentence:",
                    new String[]{"She don't like pizza", "She doesn't likes pizza", "She doesn't like pizza", "She not like pizza"}, 3,
                    "With 'she', use 'doesn't' + base verb."));
            flashcards.add(new Flashcard("What does 'big' mean?",
                    new String[]{"Small", "Large", "Fast", "Slow"}, 2, "'Big' means large."));
            flashcards.add(new Flashcard("Complete: I ___ to the cinema yesterday.",
                    new String[]{"go", "went", "goes", "going"}, 2, "Use 'went' for past tense."));
            flashcards.add(new Flashcard("What is the opposite of 'hot'?",
                    new String[]{"Warm", "Cold", "Cool", "Freezing"}, 2, "'Cold' is opposite."));
        } else {
            flashcards.add(new Flashcard("Quel est le passé composé du verbe 'aller' ?",
                    new String[]{"Je vais", "Je suis allé", "Je suis allée", "Je vais aller"}, 2,
                    "Auxiliaire 'être' + participe passé 'allé'."));
            flashcards.add(new Flashcard("Complétez : Elle ___ (manger) une pomme.",
                    new String[]{"mange", "manges", "mangent", "mangeons"}, 1, "Avec 'elle', conjuguez au présent."));
            flashcards.add(new Flashcard("Que signifie 'big' en français ?",
                    new String[]{"Petit", "Grand", "Rapide", "Lent"}, 2, "'Big' = 'grand'."));
            flashcards.add(new Flashcard("Complétez : Je ___ (être) fatigué.",
                    new String[]{"suis", "es", "est", "sommes"}, 1, "Le verbe 'être' se conjugue 'je suis'."));
            flashcards.add(new Flashcard("Quel est le contraire de 'chaud' ?",
                    new String[]{"Tiède", "Froid", "Brûlant", "Glacé"}, 2, "Le contraire est 'froid'."));
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

        // Styliser le conteneur de session
        flashcardsSessionContainer.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 20; -fx-padding: 20; -fx-border-color: #BBF7D0; -fx-border-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");

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

    @FXML private void validerReponseFlashcard() {
        if (flashcardToggleGroup == null) return;
        RadioButton selected = (RadioButton) flashcardToggleGroup.getSelectedToggle();
        if (selected == null) {
            showAlert("Information", "Veuillez sélectionner une réponse.");
            return;
        }
        int selectedOption = (int) selected.getUserData();
        Flashcard card = currentFlashcards.get(currentFlashcardIndex);
        boolean isCorrect = (selectedOption == card.getCorrectAnswer());
        flashcardExplanationArea.setText(cleanMarkdown(card.getExplanation()));
        flashcardExplanationArea.setVisible(true);
        flashcardExplanationArea.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 10; -fx-border-color: #A5D6A7; -fx-border-radius: 10; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");
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
        if (currentFlashcardIndex + 1 >= currentFlashcards.size()) flashcardNextBtn.setText("✓ Terminer");
    }

    @FXML private void flashcardSuivante() {
        currentFlashcardIndex++;
        if (currentFlashcardIndex >= currentFlashcards.size()) terminerFlashcardSession();
        else loadCurrentFlashcard();
    }

    private void terminerFlashcardSession() {
        flashcardsSessionContainer.setVisible(false);
        flashcardsSessionContainer.setManaged(false);

        // Créer un conteneur stylisé pour les résultats
        VBox resultContent = new VBox(15);
        resultContent.setAlignment(Pos.CENTER);
        resultContent.setPadding(new Insets(20));
        resultContent.setStyle("-fx-background-color: linear-gradient(to bottom, #F8FAFC, #F1F5F9); -fx-background-radius: 20;");

        int total = currentFlashcards.size();
        int correct = currentFlashcardScore;
        int wrong = total - correct;
        double percentage = (double) correct / total * 100;

        // Couleur selon le score
        String scoreColor;
        String titleIcon;
        if (percentage >= 80) {
            scoreColor = "#10B981";
            titleIcon = "🏆";
        } else if (percentage >= 60) {
            scoreColor = "#F59E0B";
            titleIcon = "👍";
        } else if (percentage >= 40) {
            scoreColor = "#3B82F6";
            titleIcon = "📚";
        } else {
            scoreColor = "#EF4444";
            titleIcon = "💪";
        }

        // Titre
        Label titleLabel = new Label(titleIcon + " RÉSULTATS DU QUIZ " + titleIcon);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        titleLabel.setAlignment(Pos.CENTER);

        // Score
        Label scoreLabel = new Label(correct + " / " + total);
        scoreLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        scoreLabel.setAlignment(Pos.CENTER);

        // Pourcentage avec barre
        Label percentageLabel = new Label(String.format("%.1f%%", percentage));
        percentageLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        ProgressBar progressBar = new ProgressBar(percentage / 100);
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: " + scoreColor + ";");

        // Message d'appréciation
        String appreciation;
        if (percentage >= 80) {
            appreciation = "🏆 EXCELLENT ! Vous maîtrisez très bien ce sujet !";
        } else if (percentage >= 60) {
            appreciation = "👍 TRÈS BIEN ! Continuez à pratiquer !";
        } else if (percentage >= 40) {
            appreciation = "📚 PAS MAL ! Révisez un peu et réessayez !";
        } else {
            appreciation = "💪 CONTINUEZ VOS EFFORTS ! Refaites le quiz après révision !";
        }

        Label appreciationLabel = new Label(appreciation);
        appreciationLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + "; -fx-wrap-text: true;");
        appreciationLabel.setAlignment(Pos.CENTER);
        appreciationLabel.setMaxWidth(400);

        // Séparateur
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #E2E8F0;");

        // Détail des erreurs
        Label detailsTitle = new Label("📝 DÉTAIL DES RÉPONSES");
        detailsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        VBox detailsContainer = new VBox(10);
        detailsContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 15;");
        detailsContainer.setMaxHeight(350);

        ScrollPane detailsScroll = new ScrollPane(detailsContainer);
        detailsScroll.setFitToWidth(true);
        detailsScroll.setPrefHeight(300);
        detailsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        // Afficher chaque question avec résultat
        for (int i = 0; i < currentFlashcards.size(); i++) {
            Flashcard card = currentFlashcards.get(i);
            boolean estCorrecte = flashcardUserAnswers.get(i);
            int userAnswer = flashcardUserSelections.get(i);

            HBox questionRow = new HBox(10);
            questionRow.setAlignment(Pos.TOP_LEFT);
            questionRow.setStyle("-fx-padding: 12; -fx-background-color: " + (estCorrecte ? "#D1FAE5" : "#FEE2E2") + "; -fx-background-radius: 10;");

            Label statusIcon = new Label(estCorrecte ? "✅" : "❌");
            statusIcon.setStyle("-fx-font-size: 18px;");

            VBox questionContent = new VBox(8);

            Label questionText = new Label("Q" + (i+1) + ": " + card.getQuestion());
            questionText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-wrap-text: true;");
            questionText.setWrapText(true);

            String userAnswerText = (userAnswer != -1 && userAnswer <= card.getOptions().length) ? card.getOptions()[userAnswer - 1] : "Non répondue";
            String correctAnswerText = card.getOptions()[card.getCorrectAnswer() - 1];

            Label userAnswerLabel = new Label("❌ Votre réponse : " + userAnswerText);
            userAnswerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
            userAnswerLabel.setWrapText(true);

            Label correctAnswerLabel = new Label("✓ Bonne réponse : " + correctAnswerText);
            correctAnswerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981;");
            correctAnswerLabel.setWrapText(true);

            Label explanationLabel = new Label("💡 " + card.getExplanation());
            explanationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-wrap-text: true;");
            explanationLabel.setWrapText(true);

            questionContent.getChildren().addAll(questionText, userAnswerLabel, correctAnswerLabel, explanationLabel);
            questionRow.getChildren().addAll(statusIcon, questionContent);

            detailsContainer.getChildren().add(questionRow);
        }

        // Conseils pour progresser
        VBox conseilsBox = new VBox(10);
        conseilsBox.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 12; -fx-padding: 15;");

        Label conseilsTitle = new Label("🎯 CONSEILS POUR PROGRESSER");
        conseilsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E40AF;");

        VBox conseilsList = new VBox(5);

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
            Label conseil = new Label("📖 Grammaire : Révisez les règles de grammaire (" + grammarErrors + " erreur(s))");
            conseil.setStyle("-fx-font-size: 12px; -fx-text-fill: #1E40AF;");
            conseilsList.getChildren().add(conseil);
        }
        if (vocabErrors > 0) {
            Label conseil = new Label("📝 Vocabulaire : Apprenez plus de mots (" + vocabErrors + " erreur(s))");
            conseil.setStyle("-fx-font-size: 12px; -fx-text-fill: #1E40AF;");
            conseilsList.getChildren().add(conseil);
        }
        if (conjugationErrors > 0) {
            Label conseil = new Label("🔤 Conjugaison : Entraînez-vous (" + conjugationErrors + " erreur(s))");
            conseil.setStyle("-fx-font-size: 12px; -fx-text-fill: #1E40AF;");
            conseilsList.getChildren().add(conseil);
        }
        if (grammarErrors == 0 && vocabErrors == 0 && conjugationErrors == 0 && wrong > 0) {
            Label conseil = new Label("📚 Révisez les explications des erreurs ci-dessus");
            conseil.setStyle("-fx-font-size: 12px; -fx-text-fill: #1E40AF;");
            conseilsList.getChildren().add(conseil);
        }
        if (wrong == 0) {
            Label conseil = new Label("🎉 Félicitations ! Continuez comme ça !");
            conseil.setStyle("-fx-font-size: 12px; -fx-text-fill: #10B981; -fx-font-weight: bold;");
            conseilsList.getChildren().add(conseil);
        }

        conseilsBox.getChildren().addAll(conseilsTitle, conseilsList);

        // Boutons d'action
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button replayBtn = new Button("🔄 Recommencer");
        replayBtn.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 24; -fx-cursor: hand;");
        replayBtn.setOnAction(e -> {
            flashcardResultContainer.setVisible(false);
            ouvrirFormulaireFlashcards();
        });

        Button closeBtn = new Button("✕ Fermer");
        closeBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 24; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> flashcardResultContainer.setVisible(false));

        buttonBox.getChildren().addAll(replayBtn, closeBtn);

        // Assembler tout
        resultContent.getChildren().addAll(
                titleLabel, scoreLabel, percentageLabel, progressBar,
                appreciationLabel, separator, detailsTitle, detailsScroll,
                conseilsBox, buttonBox
        );

        // Mettre à jour le conteneur existant
        flashcardResultArea.setVisible(false);
        flashcardResultArea.setManaged(false);
        flashcardResultContainer.getChildren().clear();
        flashcardResultContainer.getChildren().add(resultContent);
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
        analysis.append("✅ Réponses correctes : ").append(correct).append("/").append(total).append("\n");
        analysis.append("❌ Réponses incorrectes : ").append(wrong).append("/").append(total).append("\n");
        analysis.append("📈 Taux de réussite : ").append(String.format("%.1f", percentage)).append("%\n\n");
        if (percentage >= 80) {
            analysis.append("🏆 EXCELLENT ! Continuez comme ça !\n\n");
        } else if (percentage >= 60) {
            analysis.append("👍 TRÈS BIEN ! Révisez les erreurs.\n\n");
        } else if (percentage >= 40) {
            analysis.append("📚 PAS MAL ! Plus de pratique nécessaire.\n\n");
        } else {
            analysis.append("💪 CONTINUEZ VOS EFFORTS !\n\n");
        }
        analysis.append("📝 DÉTAIL DES ERREURS\n");
        analysis.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        boolean hasErrors = false;
        for (int i = 0; i < currentFlashcards.size(); i++) {
            if (!flashcardUserAnswers.get(i)) {
                hasErrors = true;
                Flashcard card = currentFlashcards.get(i);
                int userAnswer = flashcardUserSelections.get(i);
                analysis.append("\n❌ CARTE ").append(i + 1).append("\n");
                analysis.append("   Question : ").append(cleanMarkdown(card.getQuestion())).append("\n");
                analysis.append("   Votre réponse : ").append(cleanMarkdown(card.getOptions()[userAnswer - 1])).append("\n");
                analysis.append("   Bonne réponse : ").append(cleanMarkdown(card.getOptions()[card.getCorrectAnswer() - 1])).append("\n");
                analysis.append("   💡 ").append(cleanMarkdown(card.getExplanation())).append("\n");
            }
        }
        if (!hasErrors) analysis.append("\n🎉 Aucune erreur ! Félicitations !\n");
        analysis.append("\n\n🎯 CONSEILS POUR PROGRESSER\n");
        analysis.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        int grammarErrors = 0, vocabErrors = 0, conjugationErrors = 0;
        for (int i = 0; i < currentFlashcards.size(); i++) {
            if (!flashcardUserAnswers.get(i)) {
                String explanation = cleanMarkdown(currentFlashcards.get(i).getExplanation()).toLowerCase();
                if (explanation.contains("grammaire") || explanation.contains("règle")) grammarErrors++;
                if (explanation.contains("vocabulaire") || explanation.contains("mot")) vocabErrors++;
                if (explanation.contains("conjug") || explanation.contains("verbe")) conjugationErrors++;
            }
        }
        if (grammarErrors > 0) analysis.append("📖 Grammaire : Révisez les règles de grammaire.\n");
        if (vocabErrors > 0) analysis.append("📝 Vocabulaire : Apprenez plus de mots.\n");
        if (conjugationErrors > 0) analysis.append("🔤 Conjugaison : Entraînez-vous.\n");
        if (grammarErrors == 0 && vocabErrors == 0 && conjugationErrors == 0 && hasErrors) {
            analysis.append("📚 Révisez les explications des erreurs.\n");
        }
        analysis.append("\n💪 La pratique régulière est la clé du succès !\n");
        return analysis.toString();
    }

    @FXML private void fermerFlashcardsSession() {
        flashcardsSessionContainer.setVisible(false);
        flashcardsSessionContainer.setManaged(false);
    }
    @FXML private void fermerFlashcardsResult() {
        flashcardResultContainer.setVisible(false);
        flashcardResultContainer.setManaged(false);
        currentFlashcards.clear();
    }
    private void styleFlashcardResult() {
        flashcardResultArea.setStyle("-fx-background-color: #F8FAFC; -fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: #1E293B;");
    }

    private String cleanMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*", "").replaceAll("##", "").replaceAll("#", "").replaceAll("\\*", "").replaceAll("\\|", "");
    }

    // ==================== FLASHCARDS DE RÉVISION (CARTES MÉMOIRE À PARTIR DES PDF) ====================

    private void afficherListeFlashcards() {
        flashcardsListContainer.getChildren().clear();
        if (revisionFlashcards.isEmpty()) {
            flashcardsEmptyLabel.setVisible(true);
            return;
        }
        flashcardsEmptyLabel.setVisible(false);
        for (int i = 0; i < revisionFlashcards.size(); i++) {
            RevisionFlashcard card = revisionFlashcards.get(i);
            HBox cardItem = new HBox(15);
            cardItem.setAlignment(Pos.CENTER_LEFT);
            cardItem.setStyle("-fx-background-color: #FEFCE8; -fx-background-radius: 12; -fx-padding: 12 15; -fx-border-color: #FDE047; -fx-border-radius: 12; -fx-border-width: 1; -fx-cursor: hand;");
            cardItem.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.05)));
            Label numLabel = new Label(String.valueOf(i + 1));
            numLabel.setStyle("-fx-background-color: #EAB308; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 10;");
            Label questionLabel = new Label(card.question.length() > 50 ? card.question.substring(0, 50) + "..." : card.question);
            questionLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #713F12;");
            questionLabel.setWrapText(true);
            HBox.setHgrow(questionLabel, Priority.ALWAYS);
            Label statusLabel = new Label(reviewedCards.contains(i) ? "✅ Vu" : "📌 À réviser");
            statusLabel.setStyle(reviewedCards.contains(i) ? "-fx-text-fill: #10B981; -fx-font-size: 11px;" : "-fx-text-fill: #F59E0B; -fx-font-size: 11px;");
            cardItem.getChildren().addAll(numLabel, questionLabel, statusLabel);
            final int index = i;
            cardItem.setOnMouseClicked(e -> demarrerRevision(index));
            flashcardsListContainer.getChildren().add(cardItem);
        }
    }

    private void demarrerRevision(int startIndex) {
        currentRevisionIndex = startIndex;
        reviewedCards.clear();
        isCardFlipped = false;
        afficherCarteCourante();
        revisionSessionContainer.setVisible(true);
        revisionSessionContainer.setManaged(true);
    }


    @FXML private void retournerCarte() {
        isCardFlipped = !isCardFlipped;
        if (isCardFlipped) {
            flashcardFront.setVisible(false);
            flashcardFront.setManaged(false);
            flashcardBack.setVisible(true);
            flashcardBack.setManaged(true);
            reviewedCards.add(currentRevisionIndex);
        } else {
            flashcardFront.setVisible(true);
            flashcardFront.setManaged(true);
            flashcardBack.setVisible(false);
            flashcardBack.setManaged(false);
        }
        int reviewedCount = reviewedCards.size();
        revisionScoreLabel.setText("Progression: " + reviewedCount + "/" + revisionFlashcards.size());
    }

    @FXML private void carteSuivante() {
        if (currentRevisionIndex + 1 < revisionFlashcards.size()) {
            currentRevisionIndex++;
            isCardFlipped = false;
            afficherCarteCourante();
        } else {
            terminerRevision();
        }
    }

    @FXML private void cartePrecedente() {
        if (currentRevisionIndex - 1 >= 0) {
            currentRevisionIndex--;
            isCardFlipped = false;
            afficherCarteCourante();
        }
    }

    private void terminerRevision() {
        revisionSessionContainer.setVisible(false);
        revisionSessionContainer.setManaged(false);
        afficherListeFlashcards();
        int total = revisionFlashcards.size();
        int reviewed = reviewedCards.size();
        double pourcentage = (double) reviewed / total * 100;
        showAlert("🎉 Révision terminée !",
                "Vous avez révisé " + reviewed + "/" + total + " flashcards (" + String.format("%.0f", pourcentage) + "%)\n\n" +
                        "Continuez à pratiquer pour mieux mémoriser !");
    }

    @FXML private void fermerRevisionSession() {
        revisionSessionContainer.setVisible(false);
        revisionSessionContainer.setManaged(false);
    }

    // ==================== FLASHCARDS À PARTIR DES FICHIERS PDF ====================

    private void genererFlashcardsDepuisPDF(File pdfFile) {
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(50, 50);
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Génération en cours");
        loadingAlert.setHeaderText(null);
        loadingAlert.setGraphic(loadingIndicator);
        loadingAlert.setContentText("📖 Lecture du PDF et génération des flashcards...\n\nFichier: " + pdfFile.getName());
        loadingAlert.show();

        new Thread(() -> {
            try {
                String contenuPDF = lireContenuPDF(pdfFile);
                String langueNom = this.langue != null ? this.langue.getNom() : "Français";
                String niveau = extraireNiveauFromFileName(pdfFile.getName());
                String theme = pdfFile.getName().replace(".pdf", "");
                if (niveau == null) niveau = "B1";

                // PROMPT PLUS PRÉCIS pour forcer 5 cartes avec question/réponse
                String prompt = String.format("""
            À partir du cours suivant, génère EXACTEMENT 5 flashcards de révision.
            
            CONTENU DU COURS:
            %s
            
            INSTRUCTIONS STRICTES:
            1. Tu dois générer 5 flashcards différentes
            2. Chaque flashcard doit avoir une QUESTION (recto) et une RÉPONSE (verso)
            3. Ne pose JAMAIS de questions sur "le thème" ou "le niveau" du cours
            4. Pose des VRAIES questions de grammaire et de vocabulaire
            
            FORMAT OBLIGATOIRE POUR CHAQUE FLASHCARD:
            CARTE 1:
            QUESTION: [ta question ici ?]
            REPONSE: [ta réponse ici]
            
            CARTE 2:
            QUESTION: [ta question ici ?]
            REPONSE: [ta réponse ici]
            
            (continue jusqu'à CARTE 5)
            
            EXEMPLES DE BONNES QUESTIONS:
            - QUESTION: Comment se conjugue le verbe "aller" au présent ?
              REPONSE: Je vais, tu vas, il/elle/on va, nous allons, vous allez, ils/elles vont
            
            - QUESTION: Que signifie le mot "aéroport" en anglais ?
              REPONSE: Airport
            
            - QUESTION: Quelle est la différence entre "savoir" et "connaître" ?
              REPONSE: "Savoir" s'utilise avec une connaissance factuelle, "connaître" avec une personne ou un lieu
            
            IMPORTANT: GÉNÈRE EXACTEMENT 5 CARTES AVEC QUESTION ET REPONSE !
            """, contenuPDF);

                String resultat = iaService.genererCours(langueNom, theme, prompt, "", 3);

                System.out.println("=== RÉPONSE DE L'IA ===");
                System.out.println(resultat);
                System.out.println("======================");

                List<RevisionFlashcard> flashcards = parserFlashcardsFromIARevision(resultat);

                // FORCER 5 CARTES MINIMUM
                if (flashcards.size() < 5) {
                    flashcards.addAll(genererFlashcardsComplementaires(flashcards.size(), contenuPDF, theme, niveau, langueNom));
                }

                // Limiter à 5 cartes
                List<RevisionFlashcard> finalList;
                if (flashcards.size() > 5) {
                    finalList = flashcards.subList(0, 5);
                } else {
                    finalList = new ArrayList<>(flashcards);
                }

                Platform.runLater(() -> {
                    loadingAlert.close();
                    revisionFlashcards = finalList;
                    afficherListeFlashcards();
                    demarrerRevision(0);
                    showAlert("Succès", "✅ " + finalList.size() + " flashcards générées !");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingAlert.close();
                    // Fallback garanti avec 5 cartes
                    List<RevisionFlashcard> fallbackFlashcards = genererFlashcardsGarantis(pdfFile.getName(), this.langue != null ? this.langue.getNom() : "Français");
                    revisionFlashcards = fallbackFlashcards;
                    afficherListeFlashcards();
                    demarrerRevision(0);
                    showAlert("Information", "⚠️ " + fallbackFlashcards.size() + " flashcards générées en mode secours.");
                });
            }
        }).start();
    }


    private List<RevisionFlashcard> genererFlashcardsComplementaires(int nbManquant, String contenu, String theme, String niveau, String langue) {
        List<RevisionFlashcard> complement = new ArrayList<>();

        // Extraire des mots du contenu pour créer des questions
        String[] mots = extraireMotsVocabulaire(contenu);

        for (int i = 0; i < nbManquant && i < 5; i++) {
            if (i < mots.length && mots[i].length() > 3) {
                complement.add(new RevisionFlashcard(
                        "Que signifie le mot \"" + mots[i] + "\" en " + langue + " ?",
                        "La définition de \"" + mots[i] + "\" se trouve dans votre cours."
                ));
            } else {
                // Questions de grammaire par défaut
                String[] questionsDefaut = {
                        "Comment se forme le passé composé ?",
                        "Quelle est la différence entre 'un' et 'une' ?",
                        "Comment conjugue-t-on le verbe 'être' au présent ?",
                        "Qu'est-ce qu'un adjectif qualificatif ?",
                        "Comment former le pluriel des noms en " + langue + " ?"
                };
                String[] reponsesDefaut = {
                        "Le passé composé se forme avec l'auxiliaire 'avoir' ou 'être' + participe passé.",
                        "'Un' est pour les mots masculins, 'une' pour les mots féminins.",
                        "Je suis, tu es, il/elle/on est, nous sommes, vous êtes, ils/elles sont.",
                        "Un adjectif qualificatif donne une qualité au nom (ex: grand, petit, beau).",
                        "En général, on ajoute un 's' à la fin du nom."
                };
                complement.add(new RevisionFlashcard(questionsDefaut[i % questionsDefaut.length], reponsesDefaut[i % reponsesDefaut.length]));
            }
        }
        return complement;
    }

    private List<RevisionFlashcard> genererFlashcardsGarantis(String nomFichier, String langue) {
        List<RevisionFlashcard> flashcards = new ArrayList<>();

        // 5 flashcards garanties
        flashcards.add(new RevisionFlashcard(
                "Quel est le verbe principal dans la phrase : 'Je mange une pomme' ?",
                "Le verbe principal est 'mange'."
        ));

        flashcards.add(new RevisionFlashcard(
                "Comment dit-on 'Bonjour' en " + (langue != null ? langue : "français") + " ?",
                "La réponse se trouve dans votre cours."
        ));

        flashcards.add(new RevisionFlashcard(
                "Quelle est la forme négative de 'Je parle' ?",
                "Je ne parle pas."
        ));

        flashcards.add(new RevisionFlashcard(
                "À quelle personne correspond le pronom 'ils' ?",
                "Le pronom 'ils' correspond à la 3ème personne du pluriel (masculin)."
        ));

        flashcards.add(new RevisionFlashcard(
                "Que signifie l'expression 'avoir faim' ?",
                "Avoir faim signifie ressentir le besoin de manger."
        ));

        return flashcards;
    }

    private String lireContenuPDF(File pdfFile) throws IOException {
        StringBuilder contenu = new StringBuilder();
        try {
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(new com.itextpdf.kernel.pdf.PdfReader(pdfFile));
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                String pageText = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                contenu.append(pageText).append("\n");
            }
            pdfDoc.close();
        } catch (Exception e) {
            contenu.append("Cours: ").append(pdfFile.getName());
        }
        return contenu.toString();
    }

    private List<RevisionFlashcard> parserFlashcardsFromIARevision(String data) {
        List<RevisionFlashcard> flashcards = new ArrayList<>();

        System.out.println("=== DEBUG - Données reçues de l'IA ===");
        System.out.println(data);
        System.out.println("=====================================");

        // Séparer par "CARTE X:" ou "**CARTE X:**"
        String[] cartes = data.split("(?=\\*\\*CARTE\\s+\\d+\\*\\*|CARTE\\s+\\d+)");

        for (String carte : cartes) {
            if (carte.trim().isEmpty()) continue;

            // Extraire la question : tout ce qui est entre "QUESTION:" et "REPONSE:"
            String question = "";
            String reponse = "";

            int qStart = carte.toUpperCase().indexOf("QUESTION:");
            int rStart = carte.toUpperCase().indexOf("REPONSE:");

            if (qStart != -1 && rStart != -1 && rStart > qStart) {
                // Extraire la question
                qStart += "QUESTION:".length();
                question = carte.substring(qStart, rStart).trim();

                // Extraire la réponse (jusqu'à la fin ou jusqu'à la prochaine carte)
                rStart += "REPONSE:".length();
                int nextCarte = carte.toUpperCase().indexOf("CARTE", rStart);
                if (nextCarte == -1) {
                    reponse = carte.substring(rStart).trim();
                } else {
                    reponse = carte.substring(rStart, nextCarte).trim();
                }

                // Nettoyer le texte
                question = nettoyerTexte(question);
                reponse = nettoyerTexte(reponse);

                // Nettoyer les caractères restants comme "**" qui pourraient être collés
                question = question.replaceAll("^\\*+", "").replaceAll("\\*+$", "").trim();
                reponse = reponse.replaceAll("^\\*+", "").replaceAll("\\*+$", "").trim();

                if (!question.isEmpty() && !reponse.isEmpty()) {
                    flashcards.add(new RevisionFlashcard(question, reponse));
                }
            }
        }

        // Si on n'a rien trouvé, essayer avec un autre pattern
        if (flashcards.isEmpty()) {
            Pattern pattern = Pattern.compile("QUESTION:\\s*(.+?)\\s*REPONSE:\\s*(.+?)(?=(?:QUESTION|CARTE|$))", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(data);
            while (matcher.find()) {
                String question = nettoyerTexte(matcher.group(1));
                String reponse = nettoyerTexte(matcher.group(2));
                if (!question.isEmpty() && !reponse.isEmpty()) {
                    flashcards.add(new RevisionFlashcard(question, reponse));
                }
            }
        }

        // Supprimer les doublons
        Set<String> seenQuestions = new HashSet<>();
        flashcards.removeIf(f -> !seenQuestions.add(f.question.toLowerCase()));

        // Limiter à 5 cartes
        if (flashcards.size() > 5) {
            flashcards = flashcards.subList(0, 5);
        }

        System.out.println("=== DEBUG - Flashcards parsées: " + flashcards.size() + " ===");
        for (int i = 0; i < flashcards.size(); i++) {
            System.out.println((i+1) + ". Q: " + flashcards.get(i).question);
            System.out.println("   R: " + flashcards.get(i).answer);
        }

        return flashcards;
    }

    // Nouvelle méthode pour nettoyer le texte
    private String nettoyerTexte(String text) {
        if (text == null) return "";
        // Supprimer les astérisques ** mais garder le texte entre eux
        text = text.replaceAll("\\*\\*", "");
        // Supprimer les tirets ---
        text = text.replaceAll("---", "");
        // Supprimer les underscores
        text = text.replaceAll("_", "");
        // Remplacer les multiples espaces par un seul
        text = text.replaceAll("\\s+", " ");
        // Supprimer les espaces au début et à la fin
        text = text.trim();
        return text;
    }

    // Méthode simple pour extraire un champ
    private String extractSimpleField(String text, String fieldName) {
        // Chercher le champ (ex: "QUESTION:" ou "REPONSE:")
        String pattern = fieldName + "\\s*(.*?)(?=\\n\\s*(?:QUESTION|REPONSE|CARTE|\\*\\*CARTE|$))";
        Pattern p = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String result = m.group(1).trim();
            // Nettoyer les retours à la ligne
            result = result.replaceAll("\\n\\s*", " ");
            return result;
        }
        return null;
    }

    private void afficherCarteCourante() {
        if (currentRevisionIndex >= revisionFlashcards.size()) {
            terminerRevision();
            return;
        }
        RevisionFlashcard card = revisionFlashcards.get(currentRevisionIndex);

        // IMPORTANT: Ne pas mélanger question et réponse
        // Le recto (front) doit contenir UNIQUEMENT la question
        // Le verso (back) doit contenir UNIQUEMENT la réponse
        frontQuestion.setText(card.question);
        backAnswer.setText(card.answer);

        // S'assurer que le recto est visible et le verso caché
        isCardFlipped = false;
        flashcardFront.setVisible(true);
        flashcardFront.setManaged(true);
        flashcardBack.setVisible(false);
        flashcardBack.setManaged(false);

        // Permettre le wrapping du texte
        frontQuestion.setWrapText(true);
        backAnswer.setWrapText(true);

        // Agrandir la carte
        flashcardCard.setPrefWidth(600);
        flashcardCard.setPrefHeight(400);
        flashcardFront.setPrefWidth(600);
        flashcardFront.setPrefHeight(400);
        flashcardBack.setPrefWidth(600);
        flashcardBack.setPrefHeight(400);

        revisionProgressLabel.setText("Carte " + (currentRevisionIndex + 1) + "/" + revisionFlashcards.size());
        int reviewedCount = reviewedCards.size();
        revisionScoreLabel.setText("Progression: " + reviewedCount + "/" + revisionFlashcards.size());
    }

    // Nouvelle méthode d'extraction plus générique
    private String extractFieldFromIAGeneral(String text, String fieldName) {
        // Chercher le fieldName de différentes manières
        String[] patterns = {
                fieldName + "\\s*:\\s*(.+?)(?=\\n\\s*[A-Z]+\\s*:|$)",
                fieldName + "\\s*\\n\\s*(.+?)(?=\\n\\s*[A-Z]+\\s*:|$)",
                "(?i)" + fieldName + "\\s*:\\s*(.+?)(?=\\n\\s*[A-Z]|$)"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                String result = m.group(1).trim();
                // Nettoyer le résultat
                result = result.replaceAll("\\n\\s*", " ").trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        // Méthode simple par indexOf
        int start = text.toUpperCase().indexOf(fieldName.toUpperCase());
        if (start != -1) {
            start = text.indexOf(":", start);
            if (start != -1) {
                start++;
                int end = text.indexOf("\n", start);
                if (end == -1) end = text.length();

                // Chercher le prochain champ
                String[] nextFields = {"QUESTION", "REPONSE", "CARTE", "Q:", "R:"};
                for (String nextField : nextFields) {
                    int nextIndex = text.toUpperCase().indexOf(nextField.toUpperCase(), start);
                    if (nextIndex != -1 && nextIndex < end) {
                        end = nextIndex;
                        break;
                    }
                }

                String result = text.substring(start, end).trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        return null;
    }

    private String extractFieldFromIA(String text, String fieldName) {
        int start = text.indexOf(fieldName);
        if (start == -1) return null;
        start += fieldName.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    private void genererFlashcardsSecoursPDF(String contenu, String theme, String niveau, String langue) {
        revisionFlashcards.clear();

        // Extraire des mots de vocabulaire du contenu
        String[] motsVocabulaire = extraireMotsVocabulaire(contenu);

        // Générer des questions de grammaire basées sur le contenu
        if (contenu.toLowerCase().contains("verbe") || contenu.toLowerCase().contains("conjug")) {
            revisionFlashcards.add(new RevisionFlashcard(
                    "Comment conjugue-t-on les verbes du 1er groupe au présent ?",
                    "Les verbes du 1er groupe (en -er) se conjuguent : je -e, tu -es, il/elle -e, nous -ons, vous -ez, ils/elles -ent."
            ));
        }

        if (contenu.toLowerCase().contains("passé") || contenu.toLowerCase().contains("passe")) {
            revisionFlashcards.add(new RevisionFlashcard(
                    "Comment forme-t-on le passé composé ?",
                    "Le passé composé se forme avec l'auxiliaire 'avoir' ou 'être' au présent + le participe passé du verbe."
            ));
        }

        // Générer des questions de vocabulaire
        if (motsVocabulaire.length > 0) {
            for (int i = 0; i < Math.min(motsVocabulaire.length, 3); i++) {
                String mot = motsVocabulaire[i].trim();
                if (mot.length() > 2 && mot.length() < 30) {
                    revisionFlashcards.add(new RevisionFlashcard(
                            "Que signifie le mot '" + mot + "' ?",
                            "Révisez la définition de '" + mot + "' dans votre cours."
                    ));
                }
            }
        }

        // Ajouter des questions par défaut si nécessaire
        if (revisionFlashcards.isEmpty()) {
            revisionFlashcards.add(new RevisionFlashcard(
                    "Comment dit-on 'Bonjour' en " + langue + " ?",
                    "La réponse se trouve dans votre cours."
            ));
            revisionFlashcards.add(new RevisionFlashcard(
                    "Quelle est la différence entre 'un' et 'une' en français ?",
                    "'Un' est pour les mots masculins, 'une' pour les mots féminins."
            ));
        }

        // Compléter jusqu'à 5 flashcards
        while (revisionFlashcards.size() < 5) {
            revisionFlashcards.add(new RevisionFlashcard(
                    "Question de révision " + (revisionFlashcards.size() + 1),
                    "Révisez le contenu de votre cours pour trouver la réponse."
            ));
        }

        afficherListeFlashcards();
    }

    // Méthode utilitaire pour extraire des mots du texte
    private String[] extraireMotsVocabulaire(String texte) {
        // Nettoyer le texte
        texte = texte.replaceAll("[^a-zA-ZÀ-ÿ\\s]", " ");
        String[] mots = texte.split("\\s+");

        // Filtrer les mots pertinents (longueur > 3, pas des mots vides)
        List<String> motsPertinents = new ArrayList<>();
        Set<String> motsVides = new HashSet<>(Arrays.asList(
                "le", "la", "les", "un", "une", "des", "et", "est", "sont", "dans",
                "pour", "avec", "sans", "par", "the", "and", "for", "with", "this", "that"
        ));

        for (String mot : mots) {
            mot = mot.toLowerCase();
            if (mot.length() > 3 && !motsVides.contains(mot) && !motsPertinents.contains(mot)) {
                motsPertinents.add(mot);
            }
        }

        return motsPertinents.toArray(new String[0]);
    }
    @FXML
    private void afficherListeCoursPourFlashcards() {
        String langueActuelle = this.langue != null ? this.langue.getNom() : "Français";
        // NOUVEAU CHEMIN VERS LE DOSSIER PARTAGÉ
        String langueDir = "C:/xampp/htdocs/fluently/public/uploads/cours_pdf/" + langueActuelle + "/";
        File dossierLangue = new File(langueDir);
        List<File> fichiersPdf = new ArrayList<>();
        if (dossierLangue.exists() && dossierLangue.isDirectory()) {
            File[] fichiers = dossierLangue.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (fichiers != null) {
                fichiersPdf = Arrays.asList(fichiers);
                fichiersPdf.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            }
        }

        if (fichiersPdf.isEmpty()) {
            showAlert("Information", "Aucun cours PDF disponible. Générez d'abord un cours et exportez-le en PDF !");
            return;
        }
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("📚 Choisir un cours");
        dialog.setHeaderText("Sélectionnez le cours PDF pour lequel vous voulez générer des flashcards");
        dialog.setResizable(true);
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        Label titleLabel = new Label("📖 Vos cours PDF (" + langueActuelle + ") :");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        ListView<File> listView = new ListView<>();
        listView.setPrefHeight(300);
        listView.setCellFactory(lv -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cellContent = new VBox(5);
                    cellContent.setPadding(new Insets(10));
                    String fileName = item.getName();
                    String niveau = extraireNiveauFromFileName(fileName);
                    String dateStr = extraireDateFromFileName(fileName);
                    Label themeLabel = new Label("🎯 " + fileName.replace(".pdf", ""));
                    themeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
                    Label infoLabel = new Label("⭐ Niveau " + (niveau != null ? niveau : "?") + " | 📅 " + dateStr);
                    infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
                    Label fileLabel = new Label("📄 " + fileName);
                    fileLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
                    fileLabel.setWrapText(true);
                    cellContent.getChildren().addAll(themeLabel, infoLabel, fileLabel);
                    setGraphic(cellContent);
                }
            }
        });
        listView.getItems().addAll(fichiersPdf);
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 20; -fx-cursor: hand;");
        Button generateBtn = new Button("🚀 Générer les flashcards");
        generateBtn.setStyle("-fx-background-color: #EAB308; -fx-text-fill: #713F12; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 20; -fx-cursor: hand;");
        generateBtn.setDisable(true);
        buttonBox.getChildren().addAll(cancelBtn, generateBtn);
        content.getChildren().addAll(titleLabel, listView, buttonBox);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> generateBtn.setDisable(newVal == null));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        cancelBtn.setOnAction(e -> dialog.close());
        generateBtn.setOnAction(e -> {
            File selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                dialog.close();
                genererFlashcardsDepuisPDF(selected);
            }
        });
        dialog.showAndWait();
    }
    private String getAbsoluteImagePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        if (relativePath.startsWith("C:/") || relativePath.startsWith("file:/")) {
            return relativePath;
        }
        if (relativePath.startsWith("/uploads/")) {
            return "C:/xampp/htdocs/fluently/public" + relativePath;
        }
        return relativePath;
    }

    private String getAbsolutePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;

        // Si c'est déjà un chemin absolu Windows, le retourner directement
        if (relativePath.startsWith("C:/") || relativePath.startsWith("file:/")) {
            return relativePath;
        }

        // Si c'est un chemin relatif commençant par /uploads/
        if (relativePath.startsWith("/uploads/")) {
            return "C:/xampp/htdocs/fluently/public" + relativePath;
        }

        // Si le chemin contient déjà "fluently/public", ne pas ajouter à nouveau
        if (relativePath.contains("fluently/public")) {
            return relativePath;
        }

        return relativePath;
    }

}