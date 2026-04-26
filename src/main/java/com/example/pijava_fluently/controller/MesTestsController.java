package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.services.*;
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
    @FXML private StackPane contentArea;

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
        if (currentUser == null) return;

        // ── Lire directement depuis user_progress ──────────────────────
        // C'est la source de vérité après passerAuNiveauSuivant()
        String sql = """
        SELECT up.langue_id, n.difficulte
        FROM user_progress up
        JOIN niveau n ON up.niveau_actuel_id = n.id
        WHERE up.user_id = ?
        """;

        try {
            java.sql.Connection conn = com.example.pijava_fluently.utils.MyDatabase
                    .getInstance().getConnection();

            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                com.example.pijava_fluently.utils.MyDatabase.getInstance().reconnect();
                conn = com.example.pijava_fluently.utils.MyDatabase
                        .getInstance().getConnection();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                java.sql.ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int langueId      = rs.getInt("langue_id");
                    String difficulte = rs.getString("difficulte");

                    // Extraire le code CECRL depuis la difficulté
                    // (ex: "B2 - Intermédiaire supérieur" → "B2")
                    String cecrl = extractCecrl(difficulte);
                    if (cecrl != null) {
                        niveauParLangue.put(langueId, cecrl);
                        System.out.println("✅ Langue ID : " + langueId
                                + " | Niveau actuel : " + cecrl);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[MesTests] Erreur chargement niveaux : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extrait le code CECRL depuis le champ difficulte de la table niveau.
     * Ex: "B2 - Intermédiaire supérieur" → "B2"
     *     "A1" → "A1"
     *     "C1 pour English" → "C1"
     */
    private String extractCecrl(String difficulte) {
        if (difficulte == null) return null;
        // Chercher dans cet ordre (C2 avant C1, B2 avant B1, etc.)
        for (String code : new String[]{"C2","C1","B2","B1","A2","A1"}) {
            if (difficulte.contains(code)) return code;
        }
        return null;
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
        card.setStyle("-fx-background-radius:14;-fx-cursor:"
                + (estBloque ? "default" : "hand") + ";");

        if (estBloque) {
            // ── CARTE BLOQUÉE — sobre et claire ──
            card.setStyle(card.getStyle() +
                    "-fx-background-color:#FAFBFF;" +
                    "-fx-border-color:#EEF0F8;-fx-border-radius:14;-fx-border-width:1.5;");
            card.setOpacity(0.65);

            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 18, 14, 18));

            // Cadenas dans cercle gris
            StackPane lockCircle = new StackPane();
            lockCircle.setPrefSize(38, 38);
            lockCircle.setMinSize(38, 38);
            lockCircle.setStyle("-fx-background-color:#F3F4F6;-fx-background-radius:19;");
            Label lockLbl = new Label("🔒");
            lockLbl.setStyle("-fx-font-size:15px;");
            lockCircle.getChildren().add(lockLbl);

            VBox info = new VBox(3);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label titre = new Label(test.getTitre());
            titre.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
            Label raison = new Label(niveauActuel == null
                    ? "Passez d'abord le test de niveau"
                    : "Disponible au niveau " + trouverNiveauCecrlDuTest(test));
            raison.setStyle("-fx-font-size:11px;-fx-text-fill:#C0C7D0;");
            info.getChildren().addAll(titre, raison);

            Label duree = new Label("⏱ " + test.getDureeEstimee() + " min");
            duree.setStyle("-fx-font-size:11px;-fx-text-fill:#C0C7D0;");

            row.getChildren().addAll(lockCircle, info, duree);
            card.getChildren().add(row);

        } else {
            // ── CARTE ACCESSIBLE — riche et accueillante ──
            boolean dejaPassé = !historique.isEmpty();
            double bestPct = historique.stream()
                    .mapToDouble(p -> p.getScoreMax() > 0
                            ? (double) p.getScore() / p.getScoreMax() * 100 : 0)
                    .max().orElse(-1);

            // Couleur accent selon le type
            String accent = switch (test.getType() == null ? "" : test.getType()) {
                case "Test de niveau"        -> "#6366F1";
                case "Test de fin de niveau" -> "#10B981";
                case "quiz_debutant"         -> "#F59E0B";
                default -> "#6366F1";
            };
            String accentLight = switch (test.getType() == null ? "" : test.getType()) {
                case "Test de niveau"        -> "#EEF2FF";
                case "Test de fin de niveau" -> "#ECFDF5";
                case "quiz_debutant"         -> "#FFFBEB";
                default -> "#EEF2FF";
            };

            card.setStyle(card.getStyle() +
                    "-fx-background-color:white;" +
                    "-fx-border-color:" + accent + "22;" +
                    "-fx-border-radius:14;-fx-border-width:1.5;" +
                    "-fx-effect:dropshadow(gaussian," + accent + "18,8,0,0,2);");

            // Hover effect
            card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                    .replace("white", accentLight)
                    .replace("dropshadow(gaussian," + accent + "18,8,0,0,2)",
                            "dropshadow(gaussian," + accent + "30,12,0,0,4)")));
            card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                    .replace(accentLight, "white")
                    .replace("dropshadow(gaussian," + accent + "30,12,0,0,4)",
                            "dropshadow(gaussian," + accent + "18,8,0,0,2)")));

            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(18, 20, 18, 20));

            // Icône dans cercle coloré
            StackPane iconCircle = new StackPane();
            iconCircle.setPrefSize(46, 46);
            iconCircle.setMinSize(46, 46);
            iconCircle.setStyle("-fx-background-color:" + accentLight
                    + ";-fx-background-radius:23;");
            Label icoLbl = new Label(typeIcon(test.getType()));
            icoLbl.setStyle("-fx-font-size:20px;");
            iconCircle.getChildren().add(icoLbl);

            // Infos centre
            VBox info = new VBox(6);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label titre = new Label(test.getTitre());
            titre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1E1B4B;");

            HBox meta = new HBox(8);
            meta.setAlignment(Pos.CENTER_LEFT);

            // Badge durée
            Label dureeLbl = chip("⏱ " + test.getDureeEstimee() + " min",
                    "#F1F5F9", "#64748B");
            meta.getChildren().add(dureeLbl);

            // Badge meilleur score si déjà passé
            if (dejaPassé && bestPct >= 0) {
                String scoreColor = bestPct >= 70 ? "#059669"
                        : bestPct >= 50 ? "#D97706" : "#DC2626";
                String scoreBg = bestPct >= 70 ? "#ECFDF5"
                        : bestPct >= 50 ? "#FFFBEB" : "#FEF2F2";
                meta.getChildren().add(
                        chip(String.format("Meilleur : %.0f%%", bestPct), scoreBg, scoreColor));
                // Nombre de passages
                meta.getChildren().add(
                        chip(historique.size() + " passage(s)", "#F8FAFF", "#818CF8"));
            }

            info.getChildren().addAll(titre, meta);

            // Actions droite
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            // Bouton historique (si déjà passé)
            VBox[] histoBox = {null};
            if (dejaPassé) {
                Button btnHisto = new Button("📋 Historique");
                btnHisto.setStyle(
                        "-fx-background-color:#F8FAFF;-fx-text-fill:#818CF8;" +
                                "-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-background-radius:20;-fx-padding:6 12;-fx-cursor:hand;" +
                                "-fx-border-color:#E0E7FF;-fx-border-radius:20;-fx-border-width:1;");
                btnHisto.setOnAction(e -> {
                    if (histoBox[0] == null) {
                        histoBox[0] = buildHistoriquePanel(historique);
                        card.getChildren().add(histoBox[0]);
                        btnHisto.setText("🔼 Masquer");
                    } else {
                        card.getChildren().remove(histoBox[0]);
                        histoBox[0] = null;
                        btnHisto.setText("📋 Historique");
                    }
                });
                actions.getChildren().add(btnHisto);
            }

            // Bouton principal lancer/repasser
            Button btnPasser = new Button(dejaPassé ? "🔄 Repasser" : "▶  Commencer");
            btnPasser.setStyle(
                    "-fx-background-color:" + accent + ";-fx-text-fill:white;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-background-radius:22;-fx-padding:9 22;-fx-cursor:hand;");
            btnPasser.setOnMouseEntered(e ->
                    btnPasser.setStyle(btnPasser.getStyle()
                            .replace(accent, darken(accent))));
            btnPasser.setOnMouseExited(e ->
                    btnPasser.setStyle(btnPasser.getStyle()
                            .replace(darken(accent), accent)));
            btnPasser.setOnAction(e -> lancerTest(test));
            actions.getChildren().add(btnPasser);

            row.getChildren().addAll(iconCircle, info, actions);
            card.getChildren().add(row);
        }

        return card;
    }

    // Helper pour assombrir la couleur au hover
    private String darken(String hex) {
        return switch (hex) {
            case "#6366F1" -> "#4F46E5";
            case "#10B981" -> "#059669";
            case "#F59E0B" -> "#D97706";
            default -> hex;
        };
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
    /**
     * Lance le test en Mode Histoire si le test est entièrement composé de QCM,
     * sinon lance le mode classique.
     */
    private void lancerTest(Test test) {
        try {
            // Vérifier si toutes les questions sont des QCM
            boolean modeHistoire = "quiz_debutant".equals(test.getType())
                    && verifierTousQCM(test);

            String fxmlPath = modeHistoire
                    ? "/com/example/pijava_fluently/fxml/mode-histoire.fxml"
                    : "/com/example/pijava_fluently/fxml/test-passage.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node vue = loader.load();

            if (modeHistoire) {
                ModeHistoireController ctrl = loader.getController();
                ctrl.initTest(test, currentUser.getId(), this);
            } else {
                TestPassageEtudiantController ctrl = loader.getController();
                ctrl.initTest(test, currentUser.getId(), this);
            }

            // Utiliser contentArea directement (déjà déclaré dans la classe)
            if (contentArea != null) {
                contentArea.getChildren().setAll(vue);
            } else {
                // Fallback si contentArea est null
                if (homeController != null) {
                    homeController.setContent(vue);
                } else {
                    vboxContenu.getChildren().clear();
                    vboxContenu.getChildren().add(vue);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de lancer le test : " + e.getMessage());
        }
    }

    /**
     * Vérifie si toutes les questions du test sont de type QCM.
     */
    private boolean verifierTousQCM(Test test) {
        try {
            QuestionService qs = new QuestionService();
            List<Question> questions = qs.recupererParTest(test.getId());
            return !questions.isEmpty()
                    && questions.stream().allMatch(q -> "qcm".equals(q.getType()));
        } catch (Exception e) {
            return false;
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

    @FXML
    private void handlePerformances() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/performance-analyzer.fxml"));
            Node vue = loader.load();

            PerformanceAnalyzerController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);

            // Remplacer par vboxContenu au lieu de contentArea
            if (vboxContenu != null) {
                vboxContenu.getChildren().clear();
                vboxContenu.getChildren().add(vue);
            } else {
                System.err.println("vboxContenu is null!");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'analyseur de performances");
        }
    }
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    @FXML
    private void handleDuel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/duel-lobby.fxml"));
            Node vue = loader.load();
            DuelLobbyController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);

            if (homeController != null) {
                homeController.setContent(vue);
            } else {
                // Fallback : remplacer dans vboxContenu
                vboxContenu.getChildren().clear();
                vboxContenu.getChildren().add(vue);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le lobby duel.");
        }
    }
    @FXML
    private void handleMesInfractions() {
        try {
            FraudeTrackerService tracker = new FraudeTrackerService();
            List<FraudeTrackerService.Infraction> infractions =
                    tracker.charger(currentUser.getId());
            int maxAutorise = tracker.getMaxTentativesAutorisees(currentUser.getId());

            // Construire la vue inline dans vboxContenu
            vboxContenu.getChildren().clear();

            // En-tête
            VBox header = new VBox(4);
            header.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                    "-fx-padding:20 24;");
            Label titre = new Label("🔍 Mon historique de comportement en examen");
            titre.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

            String niveauTexte = switch (maxAutorise) {
                case 1 -> "⚠️ Surveillance renforcée — 1 seule tentative autorisée";
                case 2 -> "🟡 Surveillance modérée — 2 tentatives autorisées";
                default -> "🟢 Normal — 3 tentatives autorisées";
            };
            Label niveau = new Label(niveauTexte);
            niveau.setStyle("-fx-font-size:13px;-fx-text-fill:#6B7280;");
            header.getChildren().addAll(titre, niveau);
            vboxContenu.getChildren().add(header);

            if (infractions.isEmpty()) {
                Label aucune = new Label("✅ Aucune infraction enregistrée. Continuez comme ça !");
                aucune.setStyle("-fx-font-size:14px;-fx-text-fill:#059669;-fx-padding:20;");
                vboxContenu.getChildren().add(aucune);
                return;
            }

            // Liste des infractions
            for (FraudeTrackerService.Infraction inf : infractions) {
                HBox ligne = new HBox(16);
                ligne.setAlignment(Pos.CENTER_LEFT);
                ligne.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
                        "-fx-padding:12 16;-fx-border-color:#FEE2E2;" +
                        "-fx-border-radius:10;-fx-border-width:1;");

                Label icone = new Label("⚠️");
                icone.setStyle("-fx-font-size:16px;");

                VBox info = new VBox(3);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label raison = new Label(inf.raison());
                raison.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#DC2626;");
                Label details = new Label("Test : " + inf.testTitre() + "  •  " + inf.date());
                details.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
                info.getChildren().addAll(raison, details);

                ligne.getChildren().addAll(icone, info);
                vboxContenu.getChildren().add(ligne);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleLeaderboard() {
        try {
            LeaderboardService lb = new LeaderboardService();
            List<LeaderboardService.EntreeLeaderboard> classement =
                    lb.getLeaderboard();

            vboxContenu.getChildren().clear();

            // ── En-tête ──────────────────────────────────────────────
            VBox header = new VBox(4);
            header.setStyle(
                    "-fx-background-color:linear-gradient(to right,#F59E0B,#D97706);" +
                            "-fx-background-radius:16;-fx-padding:24 28;");
            Label titre = new Label("🏆 Leaderboard — Tournoi de Duels");
            titre.setStyle(
                    "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;");
            Label sous = new Label("Classement des meilleurs joueurs");
            sous.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(255,255,255,0.8);");
            header.getChildren().addAll(titre, sous);
            vboxContenu.getChildren().add(header);

            // ── Aucun résultat ────────────────────────────────────────
            if (classement.isEmpty()) {
                VBox empty = new VBox(12);
                empty.setAlignment(javafx.geometry.Pos.CENTER);
                empty.setStyle(
                        "-fx-background-color:white;-fx-background-radius:14;" +
                                "-fx-padding:40;");
                Label ico  = new Label("🎮");
                ico.setStyle("-fx-font-size:48px;");
                Label msg  = new Label("Aucun duel joué pour l'instant.");
                msg.setStyle("-fx-font-size:15px;-fx-text-fill:#8A8FA8;");
                Label msg2 = new Label("Défiez un camarade via le bouton ⚔️ Duel !");
                msg2.setStyle("-fx-font-size:13px;-fx-text-fill:#C0C7D0;");
                empty.getChildren().addAll(ico, msg, msg2);
                vboxContenu.getChildren().add(empty);
                return;
            }

            // ── Podium TOP 3 ──────────────────────────────────────────
            if (classement.size() >= 1) {
                HBox podium = new HBox(16);
                podium.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
                podium.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

                // Ordre visuel : 2ème - 1er - 3ème
                int[] ordreVisuel = classement.size() >= 3
                        ? new int[]{1, 0, 2}
                        : classement.size() == 2
                        ? new int[]{1, 0}
                        : new int[]{0};

                String[] medailles  = {"🥇", "🥈", "🥉"};
                String[] hauteurs   = {"160", "200", "130"};
                String[] couleurs   = {"#F59E0B", "#94A3B8", "#CD7C2F"};
                String[] textColors = {"#92400E", "#1E293B", "#7C3109"};
                String[] bgLight    = {"#FFFBEB", "#F8FAFC", "#FFF7ED"};

                for (int vi = 0; vi < ordreVisuel.length; vi++) {
                    int rankIdx = ordreVisuel[vi];
                    if (rankIdx >= classement.size()) continue;
                    LeaderboardService.EntreeLeaderboard e = classement.get(rankIdx);
                    int rang = rankIdx + 1;

                    VBox marche = new VBox(10);
                    marche.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
                    marche.setPrefWidth(160);

                    // Carte joueur
                    VBox carte = new VBox(6);
                    carte.setAlignment(javafx.geometry.Pos.CENTER);
                    carte.setStyle(
                            "-fx-background-color:" + bgLight[rang - 1] + ";" +
                                    "-fx-background-radius:14;" +
                                    "-fx-border-color:" + couleurs[rang - 1] + ";" +
                                    "-fx-border-radius:14;-fx-border-width:2;" +
                                    "-fx-padding:14 10;");
                    Label medalLabel = new Label(medailles[rang - 1]);
                    medalLabel.setStyle("-fx-font-size:32px;");
                    Label nomLabel = new Label(e.prenom() + "\n" + e.nom());
                    nomLabel.setStyle(
                            "-fx-font-size:13px;-fx-font-weight:bold;" +
                                    "-fx-text-fill:" + textColors[rang - 1] + ";" +
                                    "-fx-text-alignment:center;");
                    nomLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    Label winsLabel = new Label(e.duelsGagnes() + " victoires");
                    winsLabel.setStyle(
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                                    "-fx-text-fill:" + couleurs[rang - 1] + ";");
                    Label ptsLabel = new Label(e.totalPoints() + " pts");
                    ptsLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
                    carte.getChildren().addAll(medalLabel, nomLabel, winsLabel, ptsLabel);

                    // Socle de la marche
                    VBox socle = new VBox();
                    socle.setPrefWidth(140);
                    socle.setPrefHeight(Double.parseDouble(hauteurs[rang - 1]) / 3);
                    socle.setStyle(
                            "-fx-background-color:" + couleurs[rang - 1] + ";" +
                                    "-fx-background-radius:8 8 0 0;");

                    Label rangSocle = new Label("#" + rang);
                    rangSocle.setStyle(
                            "-fx-font-size:18px;-fx-font-weight:bold;" +
                                    "-fx-text-fill:white;");
                    rangSocle.setAlignment(javafx.geometry.Pos.CENTER);
                    socle.setAlignment(javafx.geometry.Pos.CENTER);
                    socle.getChildren().add(rangSocle);

                    marche.getChildren().addAll(carte, socle);
                    podium.getChildren().add(marche);
                }
                vboxContenu.getChildren().add(podium);
            }

            // ── Tableau complet ───────────────────────────────────────
            VBox tableau = new VBox(0);
            tableau.setStyle(
                    "-fx-background-color:white;-fx-background-radius:14;" +
                            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

            // Ligne d'en-tête tableau
            HBox ligneHeader = new HBox();
            ligneHeader.setStyle(
                    "-fx-background-color:#1A1D2E;-fx-background-radius:14 14 0 0;" +
                            "-fx-padding:12 16;");
            String[] cols    = {"#", "Joueur", "Duels", "Victoires", "Taux", "Points"};
            double[] largeurs = {40, 200, 80, 90, 80, 80};
            for (int i = 0; i < cols.length; i++) {
                Label h = new Label(cols[i]);
                h.setStyle(
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                                "-fx-text-fill:rgba(255,255,255,0.7);");
                h.setPrefWidth(largeurs[i]);
                h.setAlignment(i == 1
                        ? javafx.geometry.Pos.CENTER_LEFT
                        : javafx.geometry.Pos.CENTER);
                ligneHeader.getChildren().add(h);
            }
            tableau.getChildren().add(ligneHeader);

            // Lignes joueurs
            for (int i = 0; i < classement.size(); i++) {
                LeaderboardService.EntreeLeaderboard e = classement.get(i);
                int rang = i + 1;

                // Mettre en évidence l'utilisateur courant
                boolean estMoi = e.prenom().equals(currentUser.getPrenom())
                        && e.nom().equals(currentUser.getNom());

                HBox ligne = new HBox();
                ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                ligne.setStyle(
                        "-fx-padding:12 16;" +
                                "-fx-background-color:" + (estMoi
                                ? "#F0EEFF"
                                : i % 2 == 0 ? "white" : "#FAFBFF") + ";" +
                                (i == classement.size() - 1
                                        ? "-fx-background-radius:0 0 14 14;"
                                        : "") +
                                "-fx-border-color:#F0F1F7;-fx-border-width:0 0 1 0;");

                // Rang avec médaille
                String rangTxt = rang == 1 ? "🥇"
                        : rang == 2 ? "🥈"
                        : rang == 3 ? "🥉"
                        : "#" + rang;
                Label lblRang = new Label(rangTxt);
                lblRang.setStyle(
                        "-fx-font-size:" + (rang <= 3 ? "16" : "13") + "px;" +
                                "-fx-font-weight:bold;-fx-text-fill:#6B7280;");
                lblRang.setPrefWidth(largeurs[0]);
                lblRang.setAlignment(javafx.geometry.Pos.CENTER);

                // Nom
                Label lblNom = new Label(e.prenom() + " " + e.nom()
                        + (estMoi ? "  ← vous" : ""));
                lblNom.setPrefWidth(largeurs[1]);
                lblNom.setStyle(
                        "-fx-font-size:13px;-fx-font-weight:" +
                                (estMoi ? "bold" : "normal") + ";" +
                                "-fx-text-fill:" + (estMoi ? "#6C63FF" : "#1A1D2E") + ";");

                // Duels joués
                Label lblDuels = new Label(String.valueOf(e.duelsJoues()));
                lblDuels.setPrefWidth(largeurs[2]);
                lblDuels.setAlignment(javafx.geometry.Pos.CENTER);
                lblDuels.setStyle("-fx-font-size:13px;-fx-text-fill:#6B7280;");

                // Victoires
                Label lblVic = new Label(String.valueOf(e.duelsGagnes()));
                lblVic.setPrefWidth(largeurs[3]);
                lblVic.setAlignment(javafx.geometry.Pos.CENTER);
                lblVic.setStyle(
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                                "-fx-text-fill:" + (e.duelsGagnes() > 0 ? "#059669" : "#9CA3AF") + ";");

                // Taux victoire
                Label lblTaux = new Label(
                        String.format("%.0f%%", e.tauxVictoire()));
                lblTaux.setPrefWidth(largeurs[4]);
                lblTaux.setAlignment(javafx.geometry.Pos.CENTER);
                String couleurTaux = e.tauxVictoire() >= 70 ? "#059669"
                        : e.tauxVictoire() >= 40 ? "#D97706" : "#EF4444";
                lblTaux.setStyle(
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                                "-fx-text-fill:" + couleurTaux + ";" +
                                "-fx-background-color:" + (e.tauxVictoire() >= 70
                                ? "#F0FDF4" : e.tauxVictoire() >= 40 ? "#FFFBEB" : "#FEF2F2") + ";" +
                                "-fx-background-radius:20;-fx-padding:3 10;");

                // Points
                Label lblPts = new Label(e.totalPoints() + " pts");
                lblPts.setPrefWidth(largeurs[5]);
                lblPts.setAlignment(javafx.geometry.Pos.CENTER);
                lblPts.setStyle(
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                                "-fx-text-fill:#F59E0B;");

                ligne.getChildren().addAll(
                        lblRang, lblNom, lblDuels, lblVic, lblTaux, lblPts);
                tableau.getChildren().add(ligne);
            }

            vboxContenu.getChildren().add(tableau);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le leaderboard : " + e.getMessage());
        }
    }
    @FXML
    private void handleDashboardIA() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/performance-dashboard.fxml"));
            Node vue = loader.load();
            PerformanceDashboardController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);

            if (homeController != null) {
                homeController.setContent(vue);
            } else {
                StackPane contentArea = (StackPane) vboxContenu.getScene().lookup("#contentArea");
                if (contentArea != null) contentArea.getChildren().setAll(vue);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le tableau de bord");
        }
    }
    // Ajoutez ces méthodes pour les effets hover des boutons

    @FXML
    private void onButtonHover(javafx.scene.input.MouseEvent event) {
        Button btn = (Button) event.getSource();
        if (btn.getText().equals("⚔️ Duel")) {
            btn.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand;");
        } else if (btn.getText().equals("📊 Performances")) {
            btn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand;");
        } else if (btn.getText().equals("🔍 Mon comportement")) {
            btn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand; -fx-border-color: #FECACA; -fx-border-radius: 32; -fx-border-width: 1;");
        } else if (btn.getText().equals("🏆 Leaderboard")) {
            btn.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand; -fx-border-color: #FDE68A; -fx-border-radius: 32; -fx-border-width: 1;");
        }
    }

    @FXML
    private void onButtonExit(javafx.scene.input.MouseEvent event) {
        Button btn = (Button) event.getSource();
        if (btn.getText().equals("⚔️ Duel")) {
            btn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand;");
        } else if (btn.getText().equals("📊 Performances")) {
            btn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand;");
        } else if (btn.getText().equals("🔍 Mon comportement")) {
            btn.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand; -fx-border-color: #FEE2E2; -fx-border-radius: 32; -fx-border-width: 1;");
        } else if (btn.getText().equals("🏆 Leaderboard")) {
            btn.setStyle("-fx-background-color: #FFFBEB; -fx-text-fill: #D97706; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand; -fx-border-color: #FDE68A; -fx-border-radius: 32; -fx-border-width: 1;");
        }
    }

    @FXML
    private void onButtonHoverGradient(javafx.scene.input.MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: linear-gradient(to right, #7C3AED, #4F46E5); -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand;");
    }

    @FXML
    private void onButtonExitGradient(javafx.scene.input.MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: linear-gradient(to right, #8B5CF6, #6366F1); -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-padding: 8 20; -fx-cursor: hand;");
    }
}