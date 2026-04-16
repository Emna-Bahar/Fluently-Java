package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
import com.example.pijava_fluently.services.TestPassageService;
import com.example.pijava_fluently.services.TestService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MesTestsController implements Initializable { //initializable

    @FXML private VBox      vboxContenu;
    @FXML private TextField searchField;
    @FXML private Label     labelResultat;

    private final TestService        testService        = new TestService();
    private final TestPassageService testPassageService = new TestPassageService();
    private final LangueService      langueService      = new LangueService();
    private final NiveauService      niveauService      = new NiveauService();

    private List<Test>   allTests;
    private List<Langue> allLangues;
    private List<Niveau> allNiveaux;
    private User         currentUser;
    private HomeController homeController;

    // Cache : langueId → niveau actuel (ex: 3 → "A1")
    private final Map<Integer, String> niveauParLangue = new HashMap<>();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadTests();
    }

    public void setHomeController(HomeController hc) {
        this.homeController = hc;
    }

    public User getCurrentUser()              { return currentUser; }
    public HomeController getHomeController() { return homeController; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> filtrer(val));
        }
    }

    // ── Chargement ────────────────────────────────────────────────
    private void loadTests() {
        if (currentUser == null) {
            if (vboxContenu != null) {
                Label msg = new Label("Veuillez vous connecter pour voir les tests.");
                msg.setStyle("-fx-font-size:14px;-fx-text-fill:#8A8FA8;-fx-padding:20;");
                vboxContenu.getChildren().setAll(msg);
            }
            return;
        }
        try {
            allTests   = testService.recuperer();
            allLangues = langueService.recuperer();
            allNiveaux = niveauService.recuperer();

            // Calculer le niveau actuel PAR LANGUE
            chargerNiveauxParLangue();

            if (labelResultat != null)
                labelResultat.setText(allTests.size() + " test(s)");
            afficherParLangue(allTests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Pour chaque langue, cherche le dernier passage terminé d'un "Test de niveau"
     * de CETTE langue et calcule le niveau CECRL correspondant.
     */
    private void chargerNiveauxParLangue() {
        niveauParLangue.clear();
        if (allTests == null || currentUser == null) return;

        try {
            List<TestPassage> tousPassages = testPassageService.recuperer();

            // Grouper les tests de niveau par langue
            Map<Integer, List<Test>> testsNiveauParLangue = allTests.stream()
                    .filter(t -> "Test de niveau".equals(t.getType()))
                    .collect(Collectors.groupingBy(Test::getLangueId));

            for (Map.Entry<Integer, List<Test>> entry : testsNiveauParLangue.entrySet()) {
                int langueId = entry.getKey();
                List<Integer> idsTestsNiveau = entry.getValue().stream()
                        .map(Test::getId).collect(Collectors.toList());

                // Dernier passage terminé pour CET utilisateur dans CETTE langue
                Optional<TestPassage> meilleur = tousPassages.stream()
                        .filter(p -> p.getUserId() == currentUser.getId())
                        .filter(p -> "termine".equals(p.getStatut()))
                        .filter(p -> idsTestsNiveau.contains(p.getTestId()))
                        .max(Comparator.comparing(p ->
                                p.getDateDebut() != null ? p.getDateDebut() : LocalDateTime.MIN));

                meilleur.ifPresent(p -> {
                    double pct = p.getScoreMax() > 0
                            ? (double) p.getScore() / p.getScoreMax() * 100 : 0;
                    niveauParLangue.put(langueId, scoreToNiveau(pct));
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String scoreToNiveau(double pct) {
        if (pct >= 90) return "C2";
        if (pct >= 80) return "C1";
        if (pct >= 70) return "B2";
        if (pct >= 60) return "B1";
        if (pct >= 50) return "A2";
        return "A1";
    }

    // ── Filtrage ──────────────────────────────────────────────────
    private void filtrer(String kw) {
        if (allTests == null) return;
        String q = kw == null ? "" : kw.toLowerCase().trim();
        List<Test> filtered = q.isEmpty() ? allTests :
                allTests.stream()
                        .filter(t -> t.getTitre().toLowerCase().contains(q)
                                || (t.getType() != null && t.getType().toLowerCase().contains(q)))
                        .collect(Collectors.toList());
        if (labelResultat != null) labelResultat.setText(filtered.size() + " test(s)");
        if (q.isEmpty()) afficherParLangue(filtered);
        else             afficherTestsPlat(filtered);
    }

    private void afficherRecommandation() {
        if (currentUser == null || allTests == null) return;

        // Trouver si l'utilisateur a un niveau dans au moins une langue
        if (niveauParLangue.isEmpty()) {
            System.out.println("❌ niveauParLangue est vide");
            return;
        }

        // Chercher le test recommandé : quiz_debutant du niveau actuel
        // dans la première langue où l'étudiant a un niveau
        int langueId = niveauParLangue.keySet().iterator().next();
        String niveau = niveauParLangue.get(langueId);
        System.out.println("✅ Langue ID : " + langueId + " | Niveau actuel : " + niveau);
        //DEBUGGAGE------------------------------------------------------
        // Affiche tous les quiz_debutant disponibles
        allTests.stream()
                .filter(t -> "quiz_debutant".equals(t.getType()))
                .forEach(t -> {
                    String niveauTest = trouverNiveauCecrlDuTest(t);
                    System.out.println("📝 Test : " + t.getTitre()
                            + " | langueId=" + t.getLangueId()
                            + " | niveauTest=" + niveauTest
                            + " | match niveau=" + niveau.equals(niveauTest));
                });

        // Vérifie les passages
        allTests.stream()
                .filter(t -> "quiz_debutant".equals(t.getType()))
                .filter(t -> t.getLangueId() == langueId)
                .forEach(t -> {
                    try {
                        int nbPassages = testPassageService
                                .recupererParTestEtUser(t.getId(), currentUser.getId()).size();
                        System.out.println("🔍 Test '" + t.getTitre()
                                + "' → " + nbPassages + " passage(s) existant(s)");
                    } catch (SQLException e) { e.printStackTrace(); }
                });
        //--------------------------------------------------------------
        String nomLangue = allLangues == null ? "?" :
                allLangues.stream().filter(l -> l.getId() == langueId)
                        .map(Langue::getNom).findFirst().orElse("?");

        // Chercher un quiz_debutant non encore tenté du bon niveau
        Optional<Test> testReco = allTests.stream()
                .filter(t -> t.getLangueId() == langueId)
                .filter(t -> "quiz_debutant".equals(t.getType()))
                .filter(t -> niveau.equals(trouverNiveauCecrlDuTest(t)))
                .filter(t -> {
                    try {
                        return testPassageService
                                .recupererParTestEtUser(t.getId(), currentUser.getId())
                                .isEmpty();
                    } catch (SQLException e) { return true; }
                })
                .findFirst();
        System.out.println("🎯 testReco trouvé : " + testReco.isPresent());
        if (testReco.isEmpty()) return;
        Test reco = testReco.get();

        // Construire la bannière
        HBox banniere = new HBox(16);
        banniere.setAlignment(Pos.CENTER_LEFT);
        banniere.setPadding(new Insets(18, 24, 18, 24));
        banniere.setStyle(
                "-fx-background-color:linear-gradient(to right,#EFF6FF,#F0FDF4);" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#BFDBFE;-fx-border-radius:16;-fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(59,130,246,0.12),12,0,0,4);");

        // Icône animée
        StackPane ico = new StackPane();
        ico.setPrefSize(50, 50);
        ico.setStyle("-fx-background-color:#DBEAFE;-fx-background-radius:25;");
        Label icoLbl = new Label("💡");
        icoLbl.setStyle("-fx-font-size:22px;");
        ico.getChildren().add(icoLbl);

        VBox texte = new VBox(4);
        HBox.setHgrow(texte, Priority.ALWAYS);
        Label titre = new Label("Test recommandé pour vous !");
        titre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1E40AF;");
        Label detail = new Label(
                "Niveau " + niveau + " en " + nomLangue + " · " + reco.getTitre() +
                        "  (" + reco.getDureeEstimee() + " min)");
        detail.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        texte.getChildren().addAll(titre, detail);

        Button btnLancer = new Button("▶  Commencer");
        btnLancer.setStyle(
                "-fx-background-color:#3B82F6;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:10 20;-fx-cursor:hand;" +
                        "-fx-border-color:transparent;");
        btnLancer.setOnAction(e -> lancerTest(reco));

        banniere.getChildren().addAll(ico, texte, btnLancer);

        // Insérer EN PREMIER dans vboxContenu
        vboxContenu.getChildren().add(0, banniere);
    }
    // ── Affichage principal : Langue → Type → Tests ───────────────

    private void afficherParLangue(List<Test> tests) {

        vboxContenu.getChildren().clear();
        afficherRecommandation();

        // Grouper par langue
        Map<Integer, List<Test>> parLangue = new LinkedHashMap<>();
        for (Test t : tests) {
            parLangue.computeIfAbsent(t.getLangueId(), k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<Integer, List<Test>> entry : parLangue.entrySet()) {
            int langueId    = entry.getKey();
            List<Test> testsLangue = entry.getValue();

            String nomLangue = allLangues == null ? "Langue #" + langueId :
                    allLangues.stream().filter(l -> l.getId() == langueId)
                            .map(Langue::getNom).findFirst()
                            .orElse("Langue #" + langueId);

            // Niveau actuel pour CETTE langue
            String niveauActuel = niveauParLangue.get(langueId);
            boolean aPasseTestNiveau = niveauActuel != null;

            // ── Section langue ──────────────────────────────────
            VBox sectionLangue = new VBox(0);
            sectionLangue.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:18;" +
                            "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.10),16,0,0,5);");

            // Header langue avec dégradé
            HBox headerLangue = new HBox(14);
            headerLangue.setAlignment(Pos.CENTER_LEFT);
            headerLangue.setPadding(new Insets(18, 24, 18, 24));
            headerLangue.setStyle(
                    "-fx-background-color:linear-gradient(to right,#6C63FF,#8B7CF6);" +
                            "-fx-background-radius:18 18 0 0;");

            // Icône drapeau
            Label icoLangue = new Label(langueIcon(nomLangue));
            icoLangue.setStyle("-fx-font-size:24px;");

            VBox infoLangue = new VBox(3);
            HBox.setHgrow(infoLangue, Priority.ALWAYS);
            Label lblLangue = new Label(nomLangue);
            lblLangue.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");
            Label subLangue = new Label(testsLangue.size() + " test(s) disponible(s)");
            subLangue.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.75);");
            infoLangue.getChildren().addAll(lblLangue, subLangue);

            // Badge niveau actuel ou invitation
            if (aPasseTestNiveau) {
                VBox badgeBox = new VBox(2);
                badgeBox.setAlignment(Pos.CENTER);
                badgeBox.setStyle(
                        "-fx-background-color:rgba(255,255,255,0.20);" +
                                "-fx-background-radius:12;-fx-padding:8 16;");
                Label badgeLbl = new Label("Votre niveau");
                badgeLbl.setStyle("-fx-font-size:9px;-fx-text-fill:rgba(255,255,255,0.75);-fx-font-weight:bold;");
                Label badgeVal = new Label(niveauActuel);
                badgeVal.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;");
                badgeBox.getChildren().addAll(badgeLbl, badgeVal);
                headerLangue.getChildren().addAll(icoLangue, infoLangue, badgeBox);
            } else {
                Label infoLabel = new Label("🎯 Passez le test de niveau pour commencer !");
                infoLabel.setStyle(
                        "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:white;" +
                                "-fx-background-color:rgba(255,255,255,0.18);" +
                                "-fx-background-radius:20;-fx-padding:6 14;");
                headerLangue.getChildren().addAll(icoLangue, infoLangue, infoLabel);
            }

            sectionLangue.getChildren().add(headerLangue);

            // ── Grouper par type dans l'ordre logique ───────────
            String[] ordreTypes = {"Test de niveau", "quiz_debutant", "Test de fin de niveau"};
            Map<String, List<Test>> parType = new LinkedHashMap<>();
            for (String type : ordreTypes) {
                List<Test> gr = testsLangue.stream()
                        .filter(t -> type.equals(t.getType()))
                        .collect(Collectors.toList());
                if (!gr.isEmpty()) parType.put(type, gr);
            }

            VBox corps = new VBox(0);
            corps.setPadding(new Insets(16, 20, 20, 20));
            corps.setSpacing(16);

            for (Map.Entry<String, List<Test>> entryType : parType.entrySet()) {
                String type = entryType.getKey();
                List<Test> testsType = entryType.getValue();

                // Sous-section type
                VBox sectionType = new VBox(8);

                // Label de section type
                HBox typeHeader = new HBox(10);
                typeHeader.setAlignment(Pos.CENTER_LEFT);
                typeHeader.setPadding(new Insets(6, 12, 6, 12));
                typeHeader.setStyle(
                        "-fx-background-color:" + typeBg(type) + ";" +
                                "-fx-background-radius:8;");
                Label icoType = new Label(typeIcon(type));
                icoType.setStyle("-fx-font-size:14px;");
                Label lblType = new Label(typeName(type));
                lblType.setStyle(
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                                "-fx-text-fill:" + typeColor(type) + ";");
                typeHeader.getChildren().addAll(icoType, lblType);
                sectionType.getChildren().add(typeHeader);

                // Cards des tests
                for (Test t : testsType) {
                    boolean estBloque = estTestBloquePourLangue(t, langueId, niveauActuel, aPasseTestNiveau);
                    sectionType.getChildren().add(buildTestCard(t, estBloque, niveauActuel));
                }

                corps.getChildren().add(sectionType);
            }

            sectionLangue.getChildren().add(corps);
            vboxContenu.getChildren().add(sectionLangue);
        }

        if (vboxContenu.getChildren().isEmpty()) {
            Label empty = new Label("Aucun test disponible.");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#8A8FA8;");
            vboxContenu.getChildren().add(empty);
        }
    }

    // ── Affichage plat (recherche) ────────────────────────────────
    private void afficherTestsPlat(List<Test> tests) {
        vboxContenu.getChildren().clear();
        if (tests == null || tests.isEmpty()) {
            Label empty = new Label("Aucun test trouvé.");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#8A8FA8;");
            vboxContenu.getChildren().add(empty);
            return;
        }
        for (Test t : tests) {
            String niveauActuel = niveauParLangue.get(t.getLangueId());
            boolean bloque = estTestBloquePourLangue(t, t.getLangueId(), niveauActuel, niveauActuel != null);
            vboxContenu.getChildren().add(buildTestCard(t, bloque, niveauActuel));
        }
    }

    // ── Logique de blocage PAR LANGUE ────────────────────────────
    /**
     * Règles :
     * - "Test de niveau" → toujours accessible (point d'entrée)
     * - "quiz_debutant"  → bloqué si pas encore passé le test de niveau de CETTE langue
     *                      ou si le test appartient à un autre niveau que le niveau actuel
     * - "Test de fin de niveau" → idem quiz_debutant
     */
    private boolean estTestBloquePourLangue(Test test, int langueId,
                                            String niveauActuel, boolean aPasseTestNiveau) {
        String type = test.getType();
        if (type == null) return false;

        // Test de niveau : toujours accessible
        if ("Test de niveau".equals(type)) return false;

        // Pas encore passé le test de niveau → tout bloqué
        if (!aPasseTestNiveau) return true;

        // Trouver le niveau CECRL du test via la BD (niveauId du test → difficulté)
        String niveauDuTest = trouverNiveauCecrlDuTest(test);

        if ("quiz_debutant".equals(type) || "Test de fin de niveau".equals(type)) {
            if (niveauDuTest == null) return false; // pas de niveau précis → accessible
            return !niveauDuTest.equals(niveauActuel);
        }
        return false;
    }

    /**
     * Trouve le niveau CECRL (A1, A2, B1...) d'un test en cherchant
     * dans la liste des niveaux de la BD via le niveauId du test.
     * Fallback : cherche dans le titre.
     */
    private String trouverNiveauCecrlDuTest(Test test) {
        // Via niveauId du test → chercher dans allNiveaux
        if (allNiveaux != null && test.getNiveauId() > 0) {
            return allNiveaux.stream()
                    .filter(n -> n.getId() == test.getNiveauId())
                    .map(n -> extractCecrl(n.getDifficulte()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> fallbackNiveauDuTitre(test.getTitre()));
        }
        return fallbackNiveauDuTitre(test.getTitre());
    }

    private String extractCecrl(String difficulte) {
        if (difficulte == null) return null;
        for (String niv : new String[]{"C2","C1","B2","B1","A2","A1"}) {
            if (difficulte.contains(niv)) return niv;
        }
        return null;
    }

    private String fallbackNiveauDuTitre(String titre) {
        if (titre == null) return null;
        String up = titre.toUpperCase();
        for (String niv : new String[]{"C2","C1","B2","B1","A2","A1"}) {
            if (up.contains(niv)) return niv;
        }
        return null;
    }

    // ── Construction de la carte test ────────────────────────────
    private VBox buildTestCard(Test test, boolean estBloque, String niveauActuel) {
        List<TestPassage> historiqueTemp = new ArrayList<>();
        if (currentUser != null) {
            try {
                historiqueTemp = testPassageService.recupererParTestEtUser(
                        test.getId(), currentUser.getId());
            } catch (SQLException e) { e.printStackTrace(); }
        }
        final List<TestPassage> historique = historiqueTemp;

        VBox card = new VBox(0);

        if (estBloque) {
            // ── CARTE BLOQUÉE ─────────────────────────────────
            card.setStyle(
                    "-fx-background-color:#FAFBFF;" +
                            "-fx-background-radius:12;" +
                            "-fx-border-color:#E8EAF0;-fx-border-radius:12;-fx-border-width:1;");

            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 18, 14, 18));

            // Icône cadenas dans cercle grisé
            StackPane lockCircle = new StackPane();
            lockCircle.setPrefSize(40, 40);
            lockCircle.setStyle(
                    "-fx-background-color:#F3F4F6;-fx-background-radius:20;");
            Label lockLbl = new Label("🔒");
            lockLbl.setStyle("-fx-font-size:16px;");
            lockCircle.getChildren().add(lockLbl);

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label titre = new Label(test.getTitre());
            titre.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#B0B7C3;");

            String raison = niveauActuel == null
                    ? "Passez d'abord le test de niveau"
                    : "Pas Disponible au cours de votre Niveau";
            Label raisonLbl = new Label(raison);
            raisonLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#C0C7D0;");

            info.getChildren().addAll(titre, raisonLbl);

            // Badge durée grisé
            Label dureeLbl = new Label("⏱ " + test.getDureeEstimee() + " min");
            dureeLbl.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#C0C7D0;" +
                            "-fx-background-color:#F3F4F6;-fx-background-radius:20;-fx-padding:4 10;");

            row.getChildren().addAll(lockCircle, info, dureeLbl);
            card.getChildren().add(row);

        } else {
            // ── CARTE ACCESSIBLE ──────────────────────────────
            card.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:12;" +
                            "-fx-border-color:#EAECF5;-fx-border-radius:12;-fx-border-width:1;" +
                            "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.06),10,0,0,3);");

            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(16, 18, 16, 18));

            // Icône type dans cercle coloré
            StackPane iconCircle = new StackPane();
            iconCircle.setPrefSize(44, 44);
            iconCircle.setStyle(
                    "-fx-background-color:" + typeCircleBg(test.getType()) + ";" +
                            "-fx-background-radius:22;");
            Label icoLbl = new Label(typeIcon(test.getType()));
            icoLbl.setStyle("-fx-font-size:18px;");
            iconCircle.getChildren().add(icoLbl);

            VBox info = new VBox(5);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label titre = new Label(test.getTitre());
            titre.setStyle(
                    "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

            HBox badges = new HBox(8);
            badges.setAlignment(Pos.CENTER_LEFT);
            badges.getChildren().add(
                    chip("⏱ " + test.getDureeEstimee() + " min", "#EFF6FF", "#3B82F6"));

            if (!historique.isEmpty()) {
                double bestPct = historique.stream()
                        .mapToDouble(p -> p.getScoreMax() > 0
                                ? (double) p.getScore() / p.getScoreMax() * 100 : 0)
                        .max().orElse(0);
                badges.getChildren().add(
                        chip(String.format("✅ %.0f%%", bestPct), "#F0FDF4", "#16A34A"));
            }

            info.getChildren().addAll(titre, badges);

            // Boutons
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            boolean dejaPassé = !historique.isEmpty();
            Button btnPasser = new Button(dejaPassé ? "🔄 Repasser" : "▶  Commencer");
            String btnStyle = dejaPassé
                    ? "-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;"
                    : "-fx-background-color:#6C63FF;-fx-text-fill:white;";
            btnPasser.setStyle(btnStyle +
                    "-fx-font-size:12px;-fx-font-weight:bold;" +
                    "-fx-background-radius:10;-fx-padding:9 18;-fx-cursor:hand;" +
                    "-fx-border-color:transparent;");
            btnPasser.setOnMouseEntered(e -> btnPasser.setStyle(
                    (dejaPassé
                            ? "-fx-background-color:#DDE4FF;-fx-text-fill:#5849C4;"
                            : "-fx-background-color:#5849C4;-fx-text-fill:white;") +
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-background-radius:10;-fx-padding:9 18;-fx-cursor:hand;" +
                            "-fx-border-color:transparent;"));
            btnPasser.setOnMouseExited(e -> btnPasser.setStyle(btnStyle +
                    "-fx-font-size:12px;-fx-font-weight:bold;" +
                    "-fx-background-radius:10;-fx-padding:9 18;-fx-cursor:hand;" +
                    "-fx-border-color:transparent;"));
            btnPasser.setOnAction(e -> lancerTest(test));
            actions.getChildren().add(btnPasser);

            if (dejaPassé) {
                Button btnHisto = new Button("📊");
                btnHisto.setStyle(
                        "-fx-background-color:#F4F5FA;-fx-text-fill:#6B7280;" +
                                "-fx-font-size:13px;-fx-background-radius:10;" +
                                "-fx-padding:8 12;-fx-cursor:hand;-fx-border-color:#E5E7EB;" +
                                "-fx-border-radius:10;-fx-border-width:1;");
                btnHisto.setTooltip(new Tooltip("Voir l'historique (" + historique.size() + " passage(s))"));
                VBox[] histoBox = {null};
                btnHisto.setOnAction(e -> {
                    if (histoBox[0] == null) {
                        histoBox[0] = buildHistoriquePanel(historique);
                        card.getChildren().add(histoBox[0]);
                        btnHisto.setText("🔼");
                    } else {
                        card.getChildren().remove(histoBox[0]);
                        histoBox[0] = null;
                        btnHisto.setText("📊");
                    }
                });
                actions.getChildren().add(btnHisto);
            }

            row.getChildren().addAll(iconCircle, info, actions);
            card.getChildren().add(row);
        }

        return card;
    }

    private VBox buildTestCard(Test test) {
        return buildTestCard(test, false, null);
    }

    // ── Panel historique ─────────────────────────────────────────
    private VBox buildHistoriquePanel(List<TestPassage> passages) {
        VBox panel = new VBox(6);
        panel.setStyle(
                "-fx-background-color:#F8F9FD;" +
                        "-fx-border-color:#EAECF5;-fx-border-width:1 0 0 0;");
        panel.setPadding(new Insets(12, 18, 14, 18));

        Label titre = new Label("Historique des passages");
        titre.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;" +
                "-fx-padding:0 0 4 0;");
        panel.getChildren().add(titre);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (TestPassage p : passages) {
            double pct = p.getScoreMax() > 0
                    ? (double) p.getScore() / p.getScoreMax() * 100 : 0;
            String scoreColor = pct >= 70 ? "#16A34A" : pct >= 50 ? "#CA8A04" : "#DC2626";

            HBox ligne = new HBox(14);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setPadding(new Insets(8, 12, 8, 12));
            ligne.setStyle(
                    "-fx-background-color:white;-fx-background-radius:8;" +
                            "-fx-border-color:#EAECF5;-fx-border-radius:8;-fx-border-width:1;");
            VBox.setMargin(ligne, new Insets(0, 0, 4, 0));

            Label statut = new Label(
                    "termine".equals(p.getStatut()) ? "✅" :
                            "en_cours".equals(p.getStatut()) ? "🔄" : "❌");
            statut.setStyle("-fx-font-size:13px;");

            String dateStr = p.getDateDebut() != null ? p.getDateDebut().format(fmt) : "—";
            Label date = new Label(dateStr);
            date.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
            HBox.setHgrow(date, Priority.ALWAYS);

            Label score = new Label(String.format("%.0f%%", pct));
            score.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + scoreColor + ";");

            int min = p.getTempsPasse() / 60, sec = p.getTempsPasse() % 60;
            Label temps = new Label("⏱" + (min > 0 ? min + "m" : "") + sec + "s");
            temps.setStyle("-fx-font-size:11px;-fx-text-fill:#C0C4D8;");

            ligne.getChildren().addAll(statut, date, score, temps);
            panel.getChildren().add(ligne);
        }
        return panel;
    }

    // ── Lancer le test ────────────────────────────────────────────
    private void lancerTest(Test test) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/test-passage.fxml"));
            if (loader.getLocation() == null) {
                System.err.println("❌ test-passage.fxml introuvable");
                return;
            }
            Node vue = loader.load();
            TestPassageEtudiantController ctrl = loader.getController();
            int uid = (currentUser != null) ? currentUser.getId() : -1;
            ctrl.initTest(test, uid, this);
            if (homeController != null) homeController.setContent(vue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Helpers visuels ──────────────────────────────────────────
    private Label chip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:3 10;");
        return l;
    }

    private String langueIcon(String nom) {
        if (nom == null) return "🌍";
        return switch (nom.toLowerCase()) {
            case "english"  -> "🇬🇧";
            case "français" -> "🇫🇷";
            case "espagnol" -> "🇪🇸";
            case "allemand" -> "🇩🇪";
            case "arabe"    -> "🇹🇳";
            default         -> "🌍";
        };
    }

    private String typeIcon(String type) {
        if (type == null) return "📝";
        return switch (type) {
            case "Test de niveau"        -> "🎯";
            case "Test de fin de niveau" -> "🏆";
            case "quiz_debutant"         -> "📖";
            default -> "📝";
        };
    }

    private String typeCircleBg(String type) {
        if (type == null) return "#F4F5FA";
        return switch (type) {
            case "Test de niveau"        -> "#EFF6FF";
            case "Test de fin de niveau" -> "#F0FDF4";
            case "quiz_debutant"         -> "#FFFBEB";
            default -> "#F4F5FA";
        };
    }

    private String typeName(String type) {
        if (type == null) return "Test";
        return switch (type) {
            case "Test de niveau"        -> "Test de Niveau";
            case "Test de fin de niveau" -> "Test de Fin de Niveau";
            case "quiz_debutant"         -> "Quiz Débutant";
            default -> type;
        };
    }

    private String typeBg(String type) {
        if (type == null) return "#F4F5FA";
        return switch (type) {
            case "Test de niveau"        -> "#EFF6FF";
            case "Test de fin de niveau" -> "#F0FDF4";
            case "quiz_debutant"         -> "#FFFBEB";
            default -> "#F4F5FA";
        };
    }

    private String typeColor(String type) {
        if (type == null) return "#6B7280";
        return switch (type) {
            case "Test de niveau"        -> "#3B82F6";
            case "Test de fin de niveau" -> "#16A34A";
            case "quiz_debutant"         -> "#D97706";
            default -> "#6B7280";
        };
    }

}