package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.services.CoursService;
import com.example.pijava_fluently.services.NiveauService;
import com.example.pijava_fluently.services.TestPassageService;
import com.example.pijava_fluently.services.TestService;
import com.example.pijava_fluently.services.UserProgressService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApprentissageController {

    @FXML private Label langueNom;
    @FXML private Label langueDescription;
    @FXML private Label niveauActuelValue;

    // Niveaux
    @FXML private VBox niveauA1, niveauA2, niveauB1, niveauB2, niveauC1, niveauC2;
    @FXML private HBox coursA1, coursA2, coursB1, coursB2, coursC1, coursC2;
    @FXML private Label progressA1, progressA2, progressB1, progressB2, progressC1, progressC2;

    private Langue langue;
    private HomeController homeController;
    private Map<String, List<Cours>> coursParNiveau = new HashMap<>();
    private Map<String, Integer> progressionParNiveau = new HashMap<>();
    private Map<String, Integer> coursCompleteParNiveau = new HashMap<>(); // Stocke les IDs des cours complétés
    private Map<Integer, Niveau> niveauParDifficulte = new HashMap<>(); // Map niveauKey -> Niveau object

    private User currentUser;
    private UserProgressService userProgressService = new UserProgressService();

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
        if (currentUser == null) {
            niveauActuelValue.setText("Non défini");
            return;
        }

        try {
            TestPassageService testPassageService = new TestPassageService();
            List<TestPassage> passages = testPassageService.recuperer();

            List<TestPassage> passagesUtilisateur = passages.stream()
                    .filter(p -> p.getUserId() == currentUser.getId())
                    .filter(p -> "termine".equals(p.getStatut()))
                    .collect(Collectors.toList());

            if (passagesUtilisateur.isEmpty()) {
                niveauActuelValue.setText("Non défini");
                return;
            }

            TestPassage dernierPassage = passagesUtilisateur.stream()
                    .max((p1, p2) -> {
                        if (p1.getDateFin() == null && p2.getDateFin() == null) return 0;
                        if (p1.getDateFin() == null) return -1;
                        if (p2.getDateFin() == null) return 1;
                        return p1.getDateFin().compareTo(p2.getDateFin());
                    })
                    .orElse(null);

            if (dernierPassage != null && dernierPassage.getResultat() > 0) {
                double pourcentage = dernierPassage.getResultat();
                String niveau = determinerNiveauParScore(pourcentage);
                niveauActuelValue.setText(niveau);
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

                // Stocker les niveaux par difficulté
                for (Niveau niveau : niveauxLangue) {
                    String difficulte = niveau.getDifficulte();
                    String niveauKey = extractNiveauKey(difficulte);
                    if (niveauKey != null) {
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

                // Charger la progression depuis la base de données
                chargerProgressionUtilisateur();

                Platform.runLater(() -> afficherNiveaux());

            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de charger les cours"));
            }
        }).start();
    }

    // Ajoutez ces maps pour stocker la progression par niveau
    private Map<String, Map<Integer, Boolean>> progressionCoursParNiveau = new HashMap<>();

    private void chargerProgressionUtilisateur() {
        if (currentUser == null) return;

        try {
            List<User_progress> progresses = userProgressService.recuperer();

            // Chercher la progression pour cet utilisateur et cette langue
            User_progress progress = progresses.stream()
                    .filter(p -> p.getUserId() == currentUser.getId() && p.getLangueId() == langue.getId())
                    .findFirst()
                    .orElse(null);

            // Réinitialiser les maps
            coursCompleteParNiveau.clear();
            progressionParNiveau.clear();
            progressionCoursParNiveau.clear();

            if (progress != null) {
                // Pour chaque niveau, récupérer les cours complétés
                for (Map.Entry<String, List<Cours>> entry : coursParNiveau.entrySet()) {
                    String niveauKey = entry.getKey();
                    List<Cours> cours = entry.getValue();

                    Map<Integer, Boolean> coursStatus = new HashMap<>();
                    int completedCount = 0;

                    for (Cours c : cours) {
                        boolean estComplete = false;
                        // Vérifier si ce cours est complété (stocké dans dernierNumeroCours)
                        // Note: Ceci est une solution temporaire. Idéalement, chaque niveau devrait avoir son propre compteur
                        if (c.getNumero() <= progress.getDernierNumeroCours()) {
                            estComplete = true;
                            completedCount++;
                            coursCompleteParNiveau.put(niveauKey + "_" + c.getId(), c.getId());
                        }
                        coursStatus.put(c.getId(), estComplete);
                    }
                    progressionCoursParNiveau.put(niveauKey, coursStatus);
                    progressionParNiveau.put(niveauKey, completedCount);
                }
            } else {
                // Aucune progression, initialiser à 0
                for (String niveauKey : coursParNiveau.keySet()) {
                    progressionParNiveau.put(niveauKey, 0);
                    progressionCoursParNiveau.put(niveauKey, new HashMap<>());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de la progression: " + e.getMessage());
            for (String niveauKey : coursParNiveau.keySet()) {
                progressionParNiveau.put(niveauKey, 0);
                progressionCoursParNiveau.put(niveauKey, new HashMap<>());
            }
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

            // Mettre à jour la clé coursCompleteParNiveau
            coursCompleteParNiveau.put(niveauKey + "_" + cours.getId(), cours.getId());

            // Mettre à jour la base de données (stockage global)
            List<User_progress> progresses = userProgressService.recuperer();
            User_progress existingProgress = progresses.stream()
                    .filter(p -> p.getUserId() == currentUser.getId() && p.getLangueId() == langue.getId())
                    .findFirst()
                    .orElse(null);

            if (existingProgress == null) {
                // Créer une nouvelle progression
                User_progress newProgress = new User_progress();
                newProgress.setUserId(currentUser.getId());
                newProgress.setLangueId(langue.getId());
                newProgress.setDernierNumeroCours(cours.getNumero()); // Stocke le dernier cours
                newProgress.setDernierCoursCompleteId(cours.getId());
                newProgress.setTestNiveauComplete(false);
                newProgress.setDateDerniereActivite(LocalDateTime.now());

                int niveauId = trouverNiveauId(niveauKey);
                newProgress.setNiveauActuelId(niveauId);

                userProgressService.ajouter(newProgress);
            } else {
                // Mettre à jour
                existingProgress.setDernierCoursCompleteId(cours.getId());
                existingProgress.setDateDerniereActivite(LocalDateTime.now());
                userProgressService.modifier(existingProgress);
            }

            // Recharger l'affichage pour ce niveau uniquement
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
}