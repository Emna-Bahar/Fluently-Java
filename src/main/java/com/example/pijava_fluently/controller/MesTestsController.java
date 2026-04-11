package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.TestPassageService;
import com.example.pijava_fluently.services.TestService;
import com.example.pijava_fluently.entites.Langue;
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
import java.util.Comparator;

import java.util.Map;
import java.util.LinkedHashMap;

public class MesTestsController implements Initializable {

    @FXML private VBox      vboxContenu;
    @FXML private TextField searchField;
    @FXML private Label     labelResultat;

    private final TestService        testService        = new TestService();
    private final TestPassageService testPassageService = new TestPassageService();
    private final LangueService      langueService      = new LangueService();

    private List<Test>   allTests;
    private List<Langue> allLangues;
    private User         currentUser;
    private HomeController homeController;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadTests();
    }

    public void setHomeController(HomeController hc) {
        this.homeController = hc;
    }

    public User getCurrentUser()         { return currentUser; }
    public HomeController getHomeController() { return homeController; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> filtrer(val));
        }
    }

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
            allTests  = testService.recuperer();
            allLangues = langueService.recuperer();
            if (labelResultat != null)
                labelResultat.setText(allTests.size() + " test(s)");
            afficherParLangueEtNiveau(allTests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filtrer(String kw) {
        if (allTests == null) return;
        String q = kw == null ? "" : kw.toLowerCase().trim();
        List<Test> filtered = q.isEmpty() ? allTests :
                allTests.stream()
                        .filter(t -> t.getTitre().toLowerCase().contains(q)
                                || (t.getType() != null && t.getType().toLowerCase().contains(q)))
                        .collect(Collectors.toList());
        if (labelResultat != null) labelResultat.setText(filtered.size() + " test(s)");
        if (q.isEmpty()) afficherParLangueEtNiveau(filtered);
        else             afficherTestsPlat(filtered);
    }

    // ── Affichage hiérarchique : Langue → Niveau → Tests ─────────
    private void afficherParLangueEtNiveau(List<Test> tests) {
        vboxContenu.getChildren().clear();

        // Déterminer le niveau actuel de l'utilisateur
        String niveauActuel = determinerNiveauActuel();
        boolean aPasseTestNiveau = niveauActuel != null;

        Map<Integer, List<Test>> parLangue = new LinkedHashMap<>();
        for (Test t : tests) {
            parLangue.computeIfAbsent(t.getLangueId(), k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<Integer, List<Test>> entryLangue : parLangue.entrySet()) {
            int langueId = entryLangue.getKey();
            List<Test> testsLangue = entryLangue.getValue();

            String nomLangue = allLangues == null ? "Langue #" + langueId :
                    allLangues.stream().filter(l -> l.getId() == langueId)
                            .map(Langue::getNom).findFirst()
                            .orElse("Langue #" + langueId);

            VBox sectionLangue = new VBox(12);
            sectionLangue.setPadding(new Insets(0, 0, 8, 0));

            // Header langue
            HBox headerLangue = new HBox(12);
            headerLangue.setAlignment(Pos.CENTER_LEFT);
            headerLangue.setPadding(new Insets(14, 20, 14, 20));
            headerLangue.setStyle(
                    "-fx-background-color:linear-gradient(to right,#6C63FF,#8B7CF6);" +
                            "-fx-background-radius:14;");
            Label icoLangue = new Label(langueIcon(nomLangue));
            icoLangue.setStyle("-fx-font-size:22px;");
            Label lblLangue = new Label(nomLangue);
            lblLangue.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:white;");

            // Badge niveau actuel
            if (aPasseTestNiveau) {
                Label niveauBadge = new Label("Votre niveau : " + niveauActuel);
                niveauBadge.setStyle(
                        "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;" +
                                "-fx-background-color:white;-fx-background-radius:20;-fx-padding:4 12;");
                headerLangue.getChildren().addAll(icoLangue, lblLangue, niveauBadge);
            } else {
                Label infoLabel = new Label("Passez le test de niveau pour commencer !");
                infoLabel.setStyle(
                        "-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);" +
                                "-fx-background-color:rgba(255,255,255,0.2);" +
                                "-fx-background-radius:20;-fx-padding:4 12;");
                headerLangue.getChildren().addAll(icoLangue, lblLangue, infoLabel);
            }

            sectionLangue.getChildren().add(headerLangue);

            // Grouper par type
            String[] ordre = {"Test de niveau", "quiz_debutant", "Test de fin de niveau"};
            Map<String, List<Test>> parType = new LinkedHashMap<>();
            for (String type : ordre) {
                List<Test> gr = testsLangue.stream()
                        .filter(t -> type.equals(t.getType()))
                        .collect(Collectors.toList());
                if (!gr.isEmpty()) parType.put(type, gr);
            }

            for (Map.Entry<String, List<Test>> entryType : parType.entrySet()) {
                String type = entryType.getKey();
                List<Test> testsType = entryType.getValue();

                VBox sectionType = new VBox(8);
                sectionType.setPadding(new Insets(4, 0, 4, 16));

                HBox typeHeader = new HBox(8);
                typeHeader.setAlignment(Pos.CENTER_LEFT);
                typeHeader.setPadding(new Insets(8, 12, 8, 12));
                typeHeader.setStyle(
                        "-fx-background-color:" + typeBg(type) + ";" +
                                "-fx-background-radius:10;");
                Label icoType = new Label(typeIcon(type));
                icoType.setStyle("-fx-font-size:14px;");
                Label lblType = new Label(typeName(type));
                lblType.setStyle(
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                                "-fx-text-fill:" + typeColor(type) + ";");
                typeHeader.getChildren().addAll(icoType, lblType);
                sectionType.getChildren().add(typeHeader);

                for (Test t : testsType) {
                    // Déterminer si ce test est bloqué
                    boolean estBloque = estTestBloque(t, niveauActuel, aPasseTestNiveau);
                    sectionType.getChildren().add(buildTestCard(t, estBloque, niveauActuel));
                }

                sectionLangue.getChildren().add(sectionType);
            }

            vboxContenu.getChildren().add(sectionLangue);
        }

        if (vboxContenu.getChildren().isEmpty()) {
            Label empty = new Label("Aucun test disponible.");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#8A8FA8;");
            vboxContenu.getChildren().add(empty);
        }
    }

    // ── Affichage plat pour la recherche ─────────────────────────
    private void afficherTestsPlat(List<Test> tests) {
        vboxContenu.getChildren().clear();
        if (tests == null || tests.isEmpty()) {
            Label empty = new Label("Aucun test trouvé.");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#8A8FA8;");
            vboxContenu.getChildren().add(empty);
            return;
        }
        for (Test t : tests) vboxContenu.getChildren().add(buildTestCard(t));
    }

    private boolean estTestBloque(Test test, String niveauActuel, boolean aPasseTestNiveau) {
        String type = test.getType();
        if (type == null) return false;

        // Test de niveau : toujours accessible (point d'entrée)
        if ("Test de niveau".equals(type)) return false;

        // Si pas encore passé le test de niveau → tout le reste est bloqué
        if (!aPasseTestNiveau) return true;

        // Si niveau actuel connu
        if (niveauActuel != null) {
            String niveauTest = determinerNiveauDuTest(test);

            // Quiz débutant : accessible seulement pour le niveau actuel
            if ("quiz_debutant".equals(type)) {
                return niveauTest != null && !niveauTest.equals(niveauActuel);
            }

            // Test de fin de niveau : accessible seulement pour le niveau actuel
            if ("Test de fin de niveau".equals(type)) {
                return niveauTest != null && !niveauTest.equals(niveauActuel);
            }
        }
        return false;
    }

    // ── Récupère le niveau associé à un test ─────────────────────────
    private String determinerNiveauDuTest(Test test) {
        // Le test a un niveauId → chercher la difficulté dans la BD
        // On utilise le titre comme fallback
        String titre = test.getTitre() != null ? test.getTitre().toUpperCase() : "";
        for (String niv : new String[]{"C2","C1","B2","B1","A2","A1"}) {
            if (titre.contains(niv)) return niv;
        }
        return null;
    }

    // ── Détermine le niveau actuel de l'utilisateur ──────────────────
    private String determinerNiveauActuel() {
        if (currentUser == null) return null;
        try {
            List<TestPassage> passages = testPassageService.recuperer();
            // Chercher le dernier passage terminé d'un "Test de niveau"
            List<Test> testsNiveau = allTests == null ? List.of() :
                    allTests.stream()
                            .filter(t -> "Test de niveau".equals(t.getType()))
                            .collect(Collectors.toList());

            List<Integer> idsTestsNiveau = testsNiveau.stream()
                    .map(Test::getId).collect(Collectors.toList());

            return passages.stream()
                    .filter(p -> p.getUserId() == currentUser.getId())
                    .filter(p -> "termine".equals(p.getStatut()))
                    .filter(p -> idsTestsNiveau.contains(p.getTestId()))
                    .max(Comparator.comparing(p ->
                            p.getDateDebut() != null ? p.getDateDebut() : LocalDateTime.MIN))
                    .map(p -> {
                        double pct = p.getScoreMax() > 0
                                ? (double) p.getScore() / p.getScoreMax() * 100 : 0;
                        if (pct >= 90) return "C2";
                        if (pct >= 80) return "C1";
                        if (pct >= 70) return "B2";
                        if (pct >= 60) return "B1";
                        if (pct >= 50) return "A2";
                        return "A1";
                    })
                    .orElse(null);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    // ── Carte d'un test ──────────────────────────────────────────
    // ── Remplace buildTestCard(Test test) par ────────────────────────
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
            // ── CARTE BLOQUÉE ───────────────────────────────────────
            card.setStyle(
                    "-fx-background-color:#F8F9FD;" +
                            "-fx-background-radius:14;" +
                            "-fx-border-color:#E8EAF0;-fx-border-radius:14;-fx-border-width:1;" +
                            "-fx-opacity:0.7;");

            HBox header = new HBox(14);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(14, 20, 14, 20));

            Label iconeLabel = new Label("🔒");
            iconeLabel.setStyle("-fx-font-size:20px;-fx-opacity:0.5;");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label titre = new Label(test.getTitre());
            titre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");

            String raisonBlocage = "quiz_debutant".equals(test.getType()) || "Test de fin de niveau".equals(test.getType())
                    ? (niveauActuel == null
                    ? "Passez d'abord le test de niveau"
                    : "Réservé au niveau " + niveauActuel + " uniquement")
                    : "Passez d'abord le test de niveau";

            Label raison = new Label("🔒 " + raisonBlocage);
            raison.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#9CA3AF;" +
                            "-fx-background-color:#F3F4F6;-fx-background-radius:20;-fx-padding:3 10;");
            info.getChildren().addAll(titre, raison);

            Label lockIcon = new Label("🔒");
            lockIcon.setStyle(
                    "-fx-font-size:18px;" +
                            "-fx-background-color:#F3F4F6;-fx-background-radius:10;" +
                            "-fx-padding:8 12;");

            header.getChildren().addAll(iconeLabel, info, lockIcon);
            card.getChildren().add(header);

        } else {
            // ── CARTE ACCESSIBLE ────────────────────────────────────
            card.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:14;" +
                            "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.08),14,0,0,4);" +
                            "-fx-border-color:#F0F1F7;-fx-border-radius:14;-fx-border-width:1;");

            HBox header = new HBox(14);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(16, 20, 16, 20));

            Label iconeLabel = new Label(typeIcon(test.getType()));
            iconeLabel.setStyle("-fx-font-size:22px;");

            VBox info = new VBox(5);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label titre = new Label(test.getTitre());
            titre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

            HBox badges = new HBox(8);
            badges.setAlignment(Pos.CENTER_LEFT);
            badges.getChildren().add(
                    chip("⏱ " + test.getDureeEstimee() + " min", "#F0FDF4", "#059669"));
            if (!historique.isEmpty()) {
                double bestPct = historique.stream()
                        .mapToDouble(p -> p.getScoreMax() > 0
                                ? (double) p.getScore() / p.getScoreMax() * 100 : 0)
                        .max().orElse(0);
                badges.getChildren().add(
                        chip(String.format("✅ Meilleur: %.0f%%", bestPct), "#F0FDF4", "#059669"));
            }
            info.getChildren().addAll(titre, badges);

            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button btnPasser = new Button(historique.isEmpty() ? "▶ Passer" : "🔄 Repasser");
            btnPasser.setStyle(
                    "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-background-radius:10;-fx-padding:9 18;-fx-cursor:hand;");
            btnPasser.setOnMouseEntered(e -> btnPasser.setStyle(
                    "-fx-background-color:#5849C4;-fx-text-fill:white;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-background-radius:10;-fx-padding:9 18;-fx-cursor:hand;"));
            btnPasser.setOnMouseExited(e -> btnPasser.setStyle(
                    "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-background-radius:10;-fx-padding:9 18;-fx-cursor:hand;"));
            btnPasser.setOnAction(e -> lancerTest(test));
            actions.getChildren().add(btnPasser);

            if (!historique.isEmpty()) {
                Button btnHisto = new Button("📊 " + historique.size());
                btnHisto.setStyle(
                        "-fx-background-color:#EEF0FF;-fx-text-fill:#4C6EF5;" +
                                "-fx-font-size:12px;-fx-font-weight:bold;" +
                                "-fx-background-radius:10;-fx-padding:9 14;-fx-cursor:hand;" +
                                "-fx-border-color:#C7D2FE;-fx-border-radius:10;-fx-border-width:1;");
                VBox[] histoBox = {null};
                btnHisto.setOnAction(e -> {
                    if (histoBox[0] == null) {
                        histoBox[0] = buildHistoriquePanel(historique);
                        card.getChildren().add(histoBox[0]);
                        btnHisto.setText("🔼 Masquer");
                    } else {
                        card.getChildren().remove(histoBox[0]);
                        histoBox[0] = null;
                        btnHisto.setText("📊 " + historique.size());
                    }
                });
                actions.getChildren().add(btnHisto);
            }

            header.getChildren().addAll(iconeLabel, info, actions);
            card.getChildren().add(header);
        }

        return card;
    }

    // ── Mettre à jour l'ancienne signature pour compatibilité ─────────
// (si buildTestCard(Test) est encore appelée ailleurs)
    private VBox buildTestCard(Test test) {
        return buildTestCard(test, false, null);
    }

    // ── Panel historique inline ───────────────────────────────────
    private VBox buildHistoriquePanel(List<TestPassage> passages) {
        VBox panel = new VBox(6);
        panel.setStyle(
                "-fx-background-color:#F8F9FD;" +
                        "-fx-border-color:#E8EAF0;-fx-border-width:1 0 0 0;");
        panel.setPadding(new Insets(12, 20, 16, 20));

        Label titre = new Label("Historique de vos passages");
        titre.setStyle(
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6B7280;" +
                        "-fx-padding:0 0 6 0;");
        panel.getChildren().add(titre);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (TestPassage p : passages) {
            double pct = p.getScoreMax() > 0
                    ? (double) p.getScore() / p.getScoreMax() * 100 : 0;
            String scoreColor = pct >= 70 ? "#059669" : pct >= 50 ? "#CA8A04" : "#E11D48";

            HBox ligne = new HBox(14);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setPadding(new Insets(9, 12, 9, 12));
            ligne.setStyle(
                    "-fx-background-color:white;-fx-background-radius:10;" +
                            "-fx-border-color:#E8EAF0;-fx-border-radius:10;-fx-border-width:1;");
            VBox.setMargin(ligne, new Insets(0, 0, 4, 0));

            Label statut = new Label(
                    "termine".equals(p.getStatut()) ? "✅" :
                            "en_cours".equals(p.getStatut()) ? "🔄" : "❌");
            statut.setStyle("-fx-font-size:14px;");

            String dateStr = p.getDateDebut() != null ? p.getDateDebut().format(fmt) : "—";
            Label date = new Label("📅 " + dateStr);
            date.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
            HBox.setHgrow(date, Priority.ALWAYS);

            Label score = new Label(String.format("%.0f%%  (%d/%d pts)",
                    pct, p.getScore(), p.getScoreMax()));
            score.setStyle("-fx-font-size:12px;-fx-font-weight:bold;"
                    + "-fx-text-fill:" + scoreColor + ";");

            int min = p.getTempsPasse() / 60, sec = p.getTempsPasse() % 60;
            Label temps = new Label("⏱ " + (min > 0 ? min + "m " : "") + sec + "s");
            temps.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");

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
            case "english"   -> "🇬🇧";
            case "français"  -> "🇫🇷";
            case "espagnol"  -> "🇪🇸";
            case "allemand"  -> "🇩🇪";
            case "arabe"     -> "🇹🇳";
            default          -> "🌍";
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
            case "quiz_debutant"         -> "#FEF9C3";
            default -> "#F4F5FA";
        };
    }

    private String typeColor(String type) {
        if (type == null) return "#6B7280";
        return switch (type) {
            case "Test de niveau"        -> "#3B82F6";
            case "Test de fin de niveau" -> "#16A34A";
            case "quiz_debutant"         -> "#CA8A04";
            default -> "#6B7280";
        };
    }
}