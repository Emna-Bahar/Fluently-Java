package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.services.TestPassageService;
import com.example.pijava_fluently.services.TestService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TestPassageController {

    @FXML private Label     countLabel;
    @FXML private TextField searchField;

    @FXML private TableView<TestPassage>                  tablePassages;
    @FXML private TableColumn<TestPassage, Integer>       colId;
    @FXML private TableColumn<TestPassage, Integer>       colTest;
    @FXML private TableColumn<TestPassage, Integer>       colUser;
    @FXML private TableColumn<TestPassage, Integer>       colScore;
    @FXML private TableColumn<TestPassage, Integer>       colScoreMax;
    @FXML private TableColumn<TestPassage, String>        colStatut;
    @FXML private TableColumn<TestPassage, Integer>       colTemps;
    @FXML private TableColumn<TestPassage, LocalDateTime> colDateDebut;
    @FXML private TableColumn<TestPassage, Void>          colActions;

    @FXML private Label statTotalLabel;
    @FXML private Label statReussiteLabel;
    @FXML private Label statScoreMoyenLabel;
    @FXML private Label statTempsMoyenLabel;
    @FXML private ProgressBar barReussite;

    private final TestPassageService service     = new TestPassageService();
    private final TestService        testService = new TestService();
    private ObservableList<TestPassage> allData  = FXCollections.observableArrayList();
    private List<Test> tests;

    @FXML
    public void initialize() {
        try { tests = testService.recuperer(); }
        catch (SQLException e) { tests = List.of(); }
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label("#" + item);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;");
                setGraphic(lbl); setText(null);
            }
        });

        // Test — affiche le TITRE du test
        colTest.setCellValueFactory(new PropertyValueFactory<>("testId"));
        colTest.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                String titre = tests.stream()
                        .filter(t -> t.getId() == item)
                        .map(Test::getTitre)
                        .findFirst().orElse("Test #" + item);
                Label lbl = new Label("📝 " + titre);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-text-fill:#3B82F6;" +
                                "-fx-background-color:#EFF6FF;" +
                                "-fx-background-radius:8;-fx-padding:3 8;");
                setGraphic(lbl); setText(null);
            }
        });

        colUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                Label lbl = new Label("👤 #" + item);
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
                setGraphic(lbl); setText(null);
            }
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label(String.valueOf(item));
                lbl.setStyle(
                        "-fx-font-weight:bold;-fx-text-fill:#6C63FF;-fx-font-size:13px;");
                setGraphic(lbl); setText(null);
            }
        });

        colScoreMax.setCellValueFactory(new PropertyValueFactory<>("scoreMax"));
        colScoreMax.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText("—"); return; }
                setText("/ " + item);
                setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:12px;");
            }
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String icon = switch (item) {
                    case "termine"   -> "✅ ";
                    case "en_cours"  -> "🔄 ";
                    case "abandonne" -> "❌ ";
                    default          -> "";
                };
                String color = switch (item) {
                    case "termine"   ->
                            "-fx-background-color:#ECFDF5;-fx-text-fill:#059669;" +
                                    "-fx-border-color:#A7F3D0;";
                    case "en_cours"  ->
                            "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;" +
                                    "-fx-border-color:#FDE68A;";
                    case "abandonne" ->
                            "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;" +
                                    "-fx-border-color:#FECDD3;";
                    default ->
                            "-fx-background-color:#F4F5FA;-fx-text-fill:#6B7280;" +
                                    "-fx-border-color:#E0E3F0;";
                };
                Label badge = new Label(icon + item);
                badge.setStyle(color +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-border-radius:20;" +
                        "-fx-border-width:1;-fx-padding:4 10;");
                setGraphic(badge); setText(null);
            }
        });

        colTemps.setCellValueFactory(new PropertyValueFactory<>("tempsPasse"));
        colTemps.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText("—"); return; }
                int min = item / 60, sec = item % 60;
                String txt = min > 0 ? min + "m " + sec + "s" : sec + "s";
                Label lbl = new Label("⏱ " + txt);
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
                setGraphic(lbl); setText(null);
            }
        });

        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateDebut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                Label lbl = new Label(item.toLocalDate().toString());
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
                setGraphic(lbl); setText(null);
            }
        });

        // ── Colonne Actions — bouton Détails uniquement (lecture seule) ──
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = bouton(
                    "🔍 Voir détails",
                    "#EFF6FF", "#3B82F6", "#DBEAFE", "#1D4ED8", "#BFDBFE");
            private final HBox box = new HBox(btnDetails);
            {
                box.setAlignment(Pos.CENTER);
                btnDetails.setOnAction(e ->
                        afficherDetails(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Alternance couleurs lignes
        tablePassages.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(TestPassage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else if (getIndex() % 2 == 0) setStyle("-fx-background-color:#FAFBFF;");
                else setStyle("-fx-background-color:#FFFFFF;");
            }
        });
    }

    // ── Détails du passage ────────────────────────────────────────────
    private void afficherDetails(TestPassage tp) {
        String titreTest = tests.stream()
                .filter(t -> t.getId() == tp.getTestId())
                .map(Test::getTitre)
                .findFirst().orElse("Test #" + tp.getTestId());

        double pct = tp.getScoreMax() > 0
                ? (double) tp.getScore() / tp.getScoreMax() * 100 : 0;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — Passage #" + tp.getId());
        dialog.setHeaderText(null);

        VBox root = new VBox(0);
        root.setPrefWidth(540);

        // ── Header coloré selon statut ──
        String gradColor = switch (tp.getStatut() != null ? tp.getStatut() : "") {
            case "termine"   -> "linear-gradient(to right,#059669,#10B981)";
            case "en_cours"  -> "linear-gradient(to right,#D97706,#F59E0B)";
            case "abandonne" -> "linear-gradient(to right,#DC2626,#EF4444)";
            default          -> "linear-gradient(to right,#6C63FF,#8B7CF6)";
        };
        String statutIco = switch (tp.getStatut() != null ? tp.getStatut() : "") {
            case "termine"   -> "✅";
            case "en_cours"  -> "🔄";
            case "abandonne" -> "❌";
            default          -> "📊";
        };

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:" + gradColor + ";-fx-padding:20 24;");
        Label ico = new Label(statutIco);
        ico.setStyle("-fx-font-size:30px;");
        VBox hInfo = new VBox(5);
        Label hTitre = new Label("Passage #" + tp.getId());
        hTitre.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hTest = new Label("📝 " + titreTest);
        hTest.setStyle(
                "-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);" +
                        "-fx-background-color:rgba(255,255,255,0.15);" +
                        "-fx-background-radius:20;-fx-padding:3 10;");
        hInfo.getChildren().addAll(hTitre, hTest);
        header.getChildren().addAll(ico, hInfo);

        // ── Body ──
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:#F8F9FD;");

        // Score visuel
        VBox scoreCard = new VBox(10);
        scoreCard.setAlignment(Pos.CENTER);
        scoreCard.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);" +
                        "-fx-padding:20;");

        Label scorePct = new Label(String.format("%.0f%%", pct));
        scorePct.setStyle(
                "-fx-font-size:42px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");

        // Barre de progression du score
        ProgressBar pb = new ProgressBar(pct / 100.0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(10);
        pb.setStyle("-fx-accent:" + (pct >= 80 ? "#059669" :
                pct >= 50 ? "#6C63FF" : "#E11D48") + ";" +
                "-fx-background-color:#F0F2FF;" +
                "-fx-background-radius:5;");

        Label scoreDet = new Label(tp.getScore() + " pts / " + tp.getScoreMax() + " pts");
        scoreDet.setStyle("-fx-font-size:13px;-fx-text-fill:#6B7280;");

        scoreCard.getChildren().addAll(scorePct, pb, scoreDet);

        // Grille d'infos
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");
        ColumnConstraints c1 = new ColumnConstraints(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        String ls = "-fx-font-size:12px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1A1D2E;-fx-font-weight:bold;";

        // Statut avec couleur
        Label statutVal = new Label(
                (tp.getStatut() != null ? tp.getStatut() : "—"));
        statutVal.setStyle(vs + "-fx-text-fill:" + switch (
                tp.getStatut() != null ? tp.getStatut() : "") {
            case "termine"   -> "#059669";
            case "en_cours"  -> "#CA8A04";
            case "abandonne" -> "#E11D48";
            default          -> "#6B7280";
        } + ";");

        ajouterLigne(grid, 0, "Test",         titreTest, ls, vs);
        ajouterLigne(grid, 1, "Étudiant",     "👤 Utilisateur #" + tp.getUserId(), ls, vs);
        grid.add(new Label("Statut") {{ setStyle(ls); }}, 0, 2);
        grid.add(statutVal, 1, 2);

        // Temps passé
        int min = tp.getTempsPasse() / 60, sec = tp.getTempsPasse() % 60;
        String temps = min > 0 ? min + " min " + sec + " s" : sec + " s";
        ajouterLigne(grid, 3, "Temps passé",  "⏱ " + temps, ls, vs);
        ajouterLigne(grid, 4, "Date début",
                tp.getDateDebut() != null ? tp.getDateDebut().toString().replace("T", " à ") : "—",
                ls, vs);
        ajouterLigne(grid, 5, "Date fin",
                tp.getDateFin() != null ? tp.getDateFin().toString().replace("T", " à ") : "—",
                ls, vs);

        body.getChildren().addAll(scoreCard, grid);
        root.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color:#F8F9FD;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle(
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 22;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private void ajouterLigne(GridPane g, int row,
                              String label, String val,
                              String ls, String vs) {
        Label l = new Label(label); l.setStyle(ls);
        Label v = new Label(val);   v.setStyle(vs);
        g.add(l, 0, row); g.add(v, 1, row);
    }

    // ── Bouton helper style "Langue" ──────────────────────────────────
    private Button bouton(String texte, String bgNormal, String textNormal,
                          String bgHover, String textHover, String borderColor) {
        Button b = new Button(texte);
        String base =
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;" +
                        "-fx-border-radius:7;-fx-border-width:1;";
        String normal = "-fx-background-color:" + bgNormal + ";" +
                "-fx-text-fill:" + textNormal + ";" +
                "-fx-border-color:" + borderColor + ";";
        String hover  = "-fx-background-color:" + bgHover + ";" +
                "-fx-text-fill:" + textHover + ";" +
                "-fx-border-color:" + borderColor + ";";
        b.setStyle(normal + base);
        b.setOnMouseEntered(e -> b.setStyle(hover + base));
        b.setOnMouseExited(e  -> b.setStyle(normal + base));
        return b;
    }

    // ── CRUD ──────────────────────────────────────────────────────────
    private void loadData() {
        try {
            List<TestPassage> list = service.recuperer();
            allData = FXCollections.observableArrayList(list);
            tablePassages.setItems(allData);
            if (countLabel != null) countLabel.setText(allData.size() + " passage(s)");
            mettreAJourStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }
    private void mettreAJourStats() {
        if (allData == null || allData.isEmpty()) return;

        int total = allData.size();
        long termines = allData.stream()
                .filter(p -> "termine".equals(p.getStatut())).count();
        long reussis = allData.stream()
                .filter(p -> "termine".equals(p.getStatut()))
                .filter(p -> p.getScoreMax() > 0
                        && (double) p.getScore() / p.getScoreMax() >= 0.5)
                .count();

        double scoreMoyen = allData.stream()
                .filter(p -> p.getScoreMax() > 0)
                .mapToDouble(p -> (double) p.getScore() / p.getScoreMax() * 100)
                .average().orElse(0);

        double tempsMoyen = allData.stream()
                .mapToInt(TestPassage::getTempsPasse)
                .average().orElse(0);

        double tauxReussite = termines > 0 ? (double) reussis / termines * 100 : 0;

        if (statTotalLabel != null)
            statTotalLabel.setText(String.valueOf(total));
        if (statReussiteLabel != null)
            statReussiteLabel.setText(String.format("%.0f%%", tauxReussite));
        if (statScoreMoyenLabel != null)
            statScoreMoyenLabel.setText(String.format("%.1f%%", scoreMoyen));
        if (statTempsMoyenLabel != null) {
            int min = (int)(tempsMoyen / 60), sec = (int)(tempsMoyen % 60);
            statTempsMoyenLabel.setText(min + "m " + sec + "s");
        }
        if (barReussite != null)
            barReussite.setProgress(tauxReussite / 100.0);
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tablePassages.setItems(allData);
            countLabel.setText(allData.size() + " passage(s)");
        } else {
            ObservableList<TestPassage> filtered = allData.stream()
                    .filter(p -> (p.getStatut() != null &&
                            p.getStatut().toLowerCase().contains(q))
                            || String.valueOf(p.getUserId()).contains(q)
                            || String.valueOf(p.getTestId()).contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tablePassages.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    @FXML
    private void handleExportCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les passages en CSV");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        fc.setInitialFileName("passages_tests.csv");

        Stage stage = (Stage) tablePassages.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // En-tête BOM pour Excel
            pw.print('\uFEFF');
            pw.println("ID;Test;Étudiant;Score;Score Max;Pourcentage;Statut;Temps (s);Date Début;Date Fin");

            ObservableList<TestPassage> items = tablePassages.getItems();
            for (TestPassage p : items) {
                String titreTest = tests.stream()
                        .filter(t -> t.getId() == p.getTestId())
                        .map(Test::getTitre).findFirst().orElse("Test #" + p.getTestId());
                double pct = p.getScoreMax() > 0
                        ? (double) p.getScore() / p.getScoreMax() * 100 : 0;
                String dateDeb = p.getDateDebut() != null
                        ? p.getDateDebut().toString().replace("T", " ") : "—";
                String dateFin = p.getDateFin() != null
                        ? p.getDateFin().toString().replace("T", " ") : "—";

                pw.println(String.join(";",
                        String.valueOf(p.getId()),
                        "\"" + titreTest + "\"",
                        "Utilisateur #" + p.getUserId(),
                        String.valueOf(p.getScore()),
                        String.valueOf(p.getScoreMax()),
                        String.format("%.1f%%", pct),
                        p.getStatut() != null ? p.getStatut() : "—",
                        String.valueOf(p.getTempsPasse()),
                        dateDeb,
                        dateFin
                ));
            }

            showAlert(Alert.AlertType.INFORMATION, "Export réussi",
                    "✅ " + items.size() + " passage(s) exporté(s) vers :\n" + file.getAbsolutePath());

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur export", e.getMessage());
        }
    }
}