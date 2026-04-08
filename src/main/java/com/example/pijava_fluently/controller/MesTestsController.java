package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
import com.example.pijava_fluently.services.TestService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MesTestsController {

    @FXML private VBox      vboxContenu;
    @FXML private TextField searchField;
    @FXML private Label     labelResultat;

    private final TestService   testService   = new TestService();
    private final LangueService langueService = new LangueService();
    private final NiveauService niveauService = new NiveauService();

    private List<Test>   tousTests;
    private List<Langue> langues;
    private List<Niveau> niveaux;
    private final int userId = 7;

    @FXML
    public void initialize() {
        chargerDonnees();
        if (searchField != null)
            searchField.textProperty().addListener((obs, old, nv) -> afficherHierarchie(nv.trim()));
    }

    private void chargerDonnees() {
        try { langues   = langueService.recuperer(); } catch (SQLException e) { langues  = List.of(); }
        try { niveaux   = niveauService.recuperer(); } catch (SQLException e) { niveaux  = List.of(); }
        try { tousTests = testService.recuperer();   } catch (SQLException e) { tousTests = List.of(); }
        afficherHierarchie("");
    }

    // ── Affichage principal ────────────────────────────────────────────
    private void afficherHierarchie(String recherche) {
        vboxContenu.getChildren().clear();
        String q = recherche.toLowerCase();

        int totalAffiches = 0;

        for (Langue langue : langues) {
            // Trouver les niveaux de cette langue
            List<Niveau> niveauxLangue = niveaux.stream()
                    .filter(n -> n.getIdLangueId() == langue.getId())
                    .sorted((a, b) -> Integer.compare(a.getOrdre(), b.getOrdre()))
                    .collect(Collectors.toList());

            // Trouver les tests de cette langue
            List<Test> testsLangue = tousTests.stream()
                    .filter(t -> t.getLangueId() == langue.getId())
                    .filter(t -> q.isEmpty()
                            || t.getTitre().toLowerCase().contains(q)
                            || t.getType().toLowerCase().contains(q))
                    .collect(Collectors.toList());

            if (testsLangue.isEmpty()) continue;

            // Bloc langue
            VBox blocLangue = creerBlocLangue(langue, niveauxLangue, testsLangue, q);
            vboxContenu.getChildren().add(blocLangue);
            totalAffiches += testsLangue.size();
        }

        // Tests sans langue assignée
        List<Test> sansLangue = tousTests.stream()
                .filter(t -> t.getLangueId() == 0
                        || langues.stream().noneMatch(l -> l.getId() == t.getLangueId()))
                .filter(t -> q.isEmpty()
                        || t.getTitre().toLowerCase().contains(q)
                        || t.getType().toLowerCase().contains(q))
                .collect(Collectors.toList());
        if (!sansLangue.isEmpty()) {
            vboxContenu.getChildren().add(creerBlocSansCategorie(sansLangue));
            totalAffiches += sansLangue.size();
        }

        if (totalAffiches == 0) afficherVide();

        if (labelResultat != null)
            labelResultat.setText(totalAffiches + " test(s)");
    }

    // ── Bloc d'une langue ──────────────────────────────────────────────
    private VBox creerBlocLangue(Langue langue, List<Niveau> niveauxLangue,
                                 List<Test> testsLangue, String q) {
        VBox bloc = new VBox(16);

        // ── Header langue ──
        HBox headerLangue = new HBox(14);
        headerLangue.setAlignment(Pos.CENTER_LEFT);
        headerLangue.setStyle(
                "-fx-background-color:linear-gradient(to right,#6C63FF,#8B7CF6);" +
                        "-fx-background-radius:16;-fx-padding:18 24;");

        Label nomLangue = new Label(langue.getNom());
        nomLangue.setStyle(
                "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;");

        Label nbTests = new Label(testsLangue.size() + " test(s)");
        nbTests.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:white;" +
                        "-fx-background-color:rgba(255,255,255,0.2);" +
                        "-fx-background-radius:20;-fx-padding:4 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerLangue.getChildren().addAll(new Label("🌍") {{
            setStyle("-fx-font-size:22px;");
        }}, nomLangue, spacer, nbTests);

        bloc.getChildren().add(headerLangue);

        // ── Tests groupés par niveau ──
        if (niveauxLangue.isEmpty()) {
            // Pas de niveaux définis → afficher les tests directement
            FlowPane fp = creerFlowPaneTests(testsLangue);
            bloc.getChildren().add(fp);
        } else {
            for (Niveau niveau : niveauxLangue) {
                List<Test> testsNiveau = testsLangue.stream()
                        .filter(t -> t.getNiveauId() == niveau.getId())
                        .collect(Collectors.toList());
                if (testsNiveau.isEmpty()) continue;

                VBox blocNiveau = creerBlocNiveau(niveau, testsNiveau);
                bloc.getChildren().add(blocNiveau);
            }
            // Tests du langage sans niveau assigné
            List<Test> sansNiveau = testsLangue.stream()
                    .filter(t -> t.getNiveauId() == 0
                            || niveauxLangue.stream().noneMatch(n -> n.getId() == t.getNiveauId()))
                    .collect(Collectors.toList());
            if (!sansNiveau.isEmpty()) {
                VBox blocAutres = creerBlocAutres(sansNiveau);
                bloc.getChildren().add(blocAutres);
            }
        }

        // Conteneur avec fond blanc
        VBox wrapper = new VBox(0);
        wrapper.setStyle(
                "-fx-background-color:white;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.10),18,0,0,5);");
        VBox inner = new VBox(20);
        inner.getChildren().add(headerLangue);

        // Reconstruire proprement
        VBox container = new VBox(0);
        container.setStyle(
                "-fx-background-color:white;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.10),18,0,0,5);");

        VBox content = new VBox(20);
        content.getChildren().add(headerLangue);

        if (niveauxLangue.isEmpty()) {
            VBox padded = new VBox(creerFlowPaneTests(testsLangue));
            padded.setPadding(new javafx.geometry.Insets(0, 20, 20, 20));
            content.getChildren().add(padded);
        } else {
            VBox niveauxBox = new VBox(12);
            niveauxBox.setPadding(new javafx.geometry.Insets(0, 20, 20, 20));
            for (Niveau niveau : niveauxLangue) {
                List<Test> testsNiveau = testsLangue.stream()
                        .filter(t -> t.getNiveauId() == niveau.getId())
                        .collect(Collectors.toList());
                if (testsNiveau.isEmpty()) continue;
                niveauxBox.getChildren().add(creerBlocNiveau(niveau, testsNiveau));
            }
            List<Test> sansNiveau = testsLangue.stream()
                    .filter(t -> t.getNiveauId() == 0
                            || niveauxLangue.stream().noneMatch(n -> n.getId() == t.getNiveauId()))
                    .collect(Collectors.toList());
            if (!sansNiveau.isEmpty())
                niveauxBox.getChildren().add(creerBlocAutres(sansNiveau));
            content.getChildren().add(niveauxBox);
        }

        container.getChildren().add(content);
        return container;
    }

    // ── Bloc d'un niveau ──────────────────────────────────────────────
    private VBox creerBlocNiveau(Niveau niveau, List<Test> tests) {
        VBox bloc = new VBox(12);

        // Badge niveau
        String couleur = switch (niveau.getDifficulte() != null
                ? niveau.getDifficulte().substring(0, Math.min(2, niveau.getDifficulte().length()))
                : "") {
            case "A1" -> "#6C63FF";
            case "A2" -> "#8B7CF6";
            case "B1" -> "#F59E0B";
            case "B2" -> "#EF4444";
            case "C1" -> "#EC4899";
            case "C2" -> "#14B8A6";
            default   -> "#6B7280";
        };

        HBox headerNiveau = new HBox(12);
        headerNiveau.setAlignment(Pos.CENTER_LEFT);
        headerNiveau.setStyle(
                "-fx-background-color:" + couleur + "15;" +
                        "-fx-background-radius:12;-fx-padding:12 16;");

        Label badgeNiveau = new Label(niveau.getDifficulte() != null
                ? niveau.getDifficulte().substring(0, Math.min(2, niveau.getDifficulte().length()))
                : "?");
        badgeNiveau.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";" +
                        "-fx-background-color:" + couleur + "20;" +
                        "-fx-background-radius:10;-fx-padding:4 12;");

        Label titrNiveau = new Label(niveau.getTitre());
        titrNiveau.setStyle(
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

        Label descNiveau = new Label(niveau.getDifficulte() != null
                ? " — " + niveau.getDifficulte() : "");
        descNiveau.setStyle("-fx-font-size:12px;-fx-text-fill:#8A8FA8;");

        Label nbTests = new Label(tests.size() + " test(s)");
        nbTests.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";" +
                        "-fx-background-color:" + couleur + "15;" +
                        "-fx-background-radius:20;-fx-padding:3 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerNiveau.getChildren().addAll(badgeNiveau, titrNiveau, descNiveau, spacer, nbTests);

        bloc.getChildren().add(headerNiveau);
        bloc.getChildren().add(creerFlowPaneTests(tests));
        return bloc;
    }

    // ── FlowPane de cartes de tests ────────────────────────────────────
    private FlowPane creerFlowPaneTests(List<Test> tests) {
        FlowPane fp = new FlowPane();
        fp.setHgap(16);
        fp.setVgap(16);
        fp.setPrefWrapLength(1000);
        for (Test t : tests)
            fp.getChildren().add(creerCarteTest(t));
        return fp;
    }

    // ── Bloc "Autres" (tests sans niveau) ─────────────────────────────
    private VBox creerBlocAutres(List<Test> tests) {
        VBox bloc = new VBox(12);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:#F4F5FA;-fx-background-radius:12;-fx-padding:10 16;");
        Label lbl = new Label("📌 Autres tests");
        lbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#6B7280;");
        header.getChildren().add(lbl);
        bloc.getChildren().addAll(header, creerFlowPaneTests(tests));
        return bloc;
    }

    // ── Bloc sans langue ──────────────────────────────────────────────
    private VBox creerBlocSansCategorie(List<Test> tests) {
        VBox container = new VBox(16);
        container.setStyle(
                "-fx-background-color:white;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),16,0,0,4);");
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:#F4F5FA;-fx-background-radius:18 18 0 0;-fx-padding:18 24;");
        Label lbl = new Label("📝 Tests généraux");
        lbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#4A4D6A;");
        header.getChildren().add(lbl);
        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(16, 20, 20, 20));
        content.getChildren().add(creerFlowPaneTests(tests));
        container.getChildren().addAll(header, content);
        return container;
    }

    // ── Carte d'un test ───────────────────────────────────────────────
    private VBox creerCarteTest(Test test) {
        String nomLangue = langues.stream()
                .filter(l -> l.getId() == test.getLangueId())
                .map(Langue::getNom).findFirst().orElse("");
        String nomNiveau = niveaux.stream()
                .filter(n -> n.getId() == test.getNiveauId())
                .map(n -> n.getDifficulte() != null
                        ? n.getDifficulte().substring(0, Math.min(2, n.getDifficulte().length()))
                        : n.getTitre())
                .findFirst().orElse("");

        VBox card = new VBox(10);
        card.setPrefWidth(260);
        card.setMinHeight(160);
        String cardNormal =
                "-fx-background-color:#FAFBFF;-fx-background-radius:14;" +
                        "-fx-border-color:#E8EAF0;-fx-border-radius:14;-fx-border-width:1.5;" +
                        "-fx-padding:18;-fx-cursor:hand;";
        String cardHover =
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#6C63FF;-fx-border-radius:14;-fx-border-width:1.5;" +
                        "-fx-padding:18;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(108,99,255,0.18),14,0,0,4);";
        card.setStyle(cardNormal);

        // Icône + badge
        String icone = switch (test.getType()) {
            case "Test de niveau"        -> "🎯";
            case "Test de fin de niveau" -> "🏆";
            case "quiz_debutant"         -> "🌱";
            default -> "📝";
        };
        String badgeC = switch (test.getType()) {
            case "Test de niveau"        ->
                    "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;";
            case "Test de fin de niveau" ->
                    "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;";
            default ->
                    "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;";
        };

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icone);
        ico.setStyle("-fx-font-size:18px;");
        Label badge = new Label(test.getType());
        badge.setStyle(badgeC +
                "-fx-font-size:9px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:3 8;");
        top.getChildren().addAll(ico, badge);

        // Titre
        Label titre = new Label(test.getTitre());
        titre.setWrapText(true);
        titre.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

        // Durée
        Label duree = new Label("⏱ " + (test.getDureeEstimee() > 0
                ? test.getDureeEstimee() + " min" : "Libre"));
        duree.setStyle("-fx-font-size:11px;-fx-text-fill:#8A8FA8;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bouton
        Button btn = new Button("▶  Commencer");
        btn.setMaxWidth(Double.MAX_VALUE);
        String btnN =
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 16;-fx-cursor:hand;";
        String btnH =
                "-fx-background-color:#5B52E0;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 16;-fx-cursor:hand;";
        btn.setStyle(btnN);
        btn.setOnMouseEntered(e -> btn.setStyle(btnH));
        btn.setOnMouseExited(e  -> btn.setStyle(btnN));
        btn.setOnAction(e -> lancerTest(test));

        card.getChildren().addAll(top, titre, duree, spacer, btn);
        card.setOnMouseEntered(e -> { card.setStyle(cardHover); card.setTranslateY(-2); });
        card.setOnMouseExited(e  -> { card.setStyle(cardNormal); card.setTranslateY(0); });
        card.setOnMouseClicked(e -> lancerTest(test));
        return card;
    }

    // ── Vide ──────────────────────────────────────────────────────────
    private void afficherVide() {
        VBox vide = new VBox(14);
        vide.setAlignment(Pos.CENTER);
        vide.setStyle("-fx-padding:60;");
        Label ico = new Label("📭");
        ico.setStyle("-fx-font-size:56px;");
        Label txt = new Label("Aucun test disponible");
        txt.setStyle(
                "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#4A4D6A;");
        Label sub = new Label("Aucun test ne correspond à votre recherche");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#C0C4D8;");
        vide.getChildren().addAll(ico, txt, sub);
        vboxContenu.getChildren().add(vide);
    }

    // ── Lancer le test ────────────────────────────────────────────────
    private void lancerTest(Test test) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/test-passage.fxml"));
            Node vue = loader.load();
            TestPassageEtudiantController ctrl = loader.getController();
            ctrl.initTest(test, userId);
            StackPane contentArea = (StackPane)
                    vboxContenu.getScene().lookup("#contentArea");
            if (contentArea != null)
                contentArea.getChildren().setAll(vue);
        } catch (IOException e) { e.printStackTrace(); }
    }
}