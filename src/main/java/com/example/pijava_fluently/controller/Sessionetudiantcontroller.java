package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Reservationservice;
import com.example.pijava_fluently.services.Sessionservice;
import com.example.pijava_fluently.services.RecommandationService;
import com.example.pijava_fluently.utils.NotificationBell;
import com.example.pijava_fluently.utils.ReminderScheduler;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.stream.Collectors;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.Group;
import java.util.Random;
import javafx.scene.Parent;
import com.example.pijava_fluently.utils.JitsiUtil;
import javafx.fxml.FXMLLoader;

public class Sessionetudiantcontroller {

    // ── Champs FXML ───────────────────────────────────────────────
    @FXML private StackPane            bellContainer;   // ← ÉTAPE 3A
    @FXML private Button               btnRecommander;
    @FXML private VBox                 rootPane;
    @FXML private Label                countLabel;
    @FXML private TextField            searchField;
    @FXML private ComboBox<String>     filterStatut;
    @FXML private FlowPane             sessionsContainer;
    @FXML private FlowPane             mesResasContainer;
    @FXML private Label                mesResasCount;
    @FXML private TabPane              tabPane;
    @FXML private VBox                 recoContainer;
    @FXML private FlowPane             recoCardsPane;
    @FXML private Label                recoCount;
    @FXML private VBox                 calendrierEtudiantContainer;

    // ── NotificationBell ──────────────────────────────────────────
    private NotificationBell notifBell;              // ← ÉTAPE 3A

    // ── Services ──────────────────────────────────────────────────
    private final Sessionservice        sessionservice     = new Sessionservice();
    private final Reservationservice    reservationservice = new Reservationservice();
    private final RecommandationService recoService        = new RecommandationService();
    private final Map<Integer, String[]> recoRaisons       = new HashMap<>();

    private ObservableList<Session>     allSessions = FXCollections.observableArrayList();
    private ObservableList<Reservation> mesResas    = FXCollections.observableArrayList();
    private int     currentUserId = -1;
    private boolean initialized   = false;

    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[][] CARD_COLORS = {
            {"#3B82F6","#2563EB"}, {"#6C63FF","#8B5CF6"}, {"#10B981","#059669"},
            {"#F59E0B","#D97706"}, {"#06B6D4","#0891B2"}, {"#EC4899","#DB2777"},
    };

    // ══════════════════════════════════════════════════════════════
    // INITIALISATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        filterStatut.setItems(FXCollections.observableArrayList("Toutes", "planifiée", "en cours"));
        filterStatut.setValue("Toutes");
        filterStatut.setOnAction(e -> applyFilter());

        if (btnRecommander != null) {
            btnRecommander.setOnMouseEntered(e -> btnRecommander.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.32);" +
                            "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-background-radius:40;-fx-padding:10 22 10 22;-fx-cursor:hand;" +
                            "-fx-border-color:rgba(255,255,255,0.65);-fx-border-radius:40;-fx-border-width:1.5;"
            ));
            btnRecommander.setOnMouseExited(e -> btnRecommander.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.18);" +
                            "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-background-radius:40;-fx-padding:10 22 10 22;-fx-cursor:hand;" +
                            "-fx-border-color:rgba(255,255,255,0.45);-fx-border-radius:40;-fx-border-width:1.5;"
            ));
        }

        if (tabPane != null) {
            tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, old, nw) -> {
                if (nw.intValue() == 2) {
                    chargerCalendrierEtudiant();
                }
            });
        }

        initialized = true;
    }

    // ══════════════════════════════════════════════════════════════
    // SETUID — INTÉGRATION NotificationBell (ÉTAPE 3B)
    // ══════════════════════════════════════════════════════════════

    public void setUserId(int id) {
        this.currentUserId = id;
        if (initialized) {
            loadSessions();
            loadMesReservations();
            loadRecommandationsAuto();

            javafx.application.Platform.runLater(() -> {
                if (rootPane != null) {
                    // Créer la cloche
                    notifBell = new NotificationBell(id, false, rootPane);
                    // L'insérer dans le conteneur FXML
                    if (bellContainer != null)
                        bellContainer.getChildren().setAll(notifBell.getBellRoot());
                    // Enregistrer dans le scheduler
                    ReminderScheduler.getInstance().start(id, false, rootPane, notifBell);
                }
            });
        }
    }

    // ══════════════════════════════════════════════════════════════
    // REFRESH SCROLL
    // ══════════════════════════════════════════════════════════════

    private void forceScrollPaneRefresh() {
        javafx.application.Platform.runLater(() -> {
            if (recoCardsPane != null) {
                recoCardsPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
                recoCardsPane.setMinHeight(Region.USE_COMPUTED_SIZE);
                recoCardsPane.layout();
            }
            if (recoContainer != null) {
                recoContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
                recoContainer.setMinHeight(Region.USE_COMPUTED_SIZE);
                recoContainer.layout();
            }
            if (rootPane != null) {
                rootPane.layout();
                javafx.scene.Parent parent = rootPane.getParent();
                while (parent != null) {
                    parent.layout();
                    if (parent instanceof ScrollPane
                            || parent instanceof VBox
                            || parent instanceof HBox) {
                        parent.layout();
                    }
                    parent = parent.getParent();
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // CHARGEMENT DONNÉES
    // ══════════════════════════════════════════════════════════════

    private void loadAll() {
        loadSessions();
        loadMesReservations();
        if (!recoContainer.isVisible()) {
            loadRecommandationsAuto();
        }
    }

    private void loadSessions() {
        try {
            List<Session> sessions = sessionservice.recupererToutesEtudiant();
            List<Session> filtrees = sessions.stream()
                    .filter(s -> !"terminée".equals(s.getStatut()))
                    .collect(Collectors.toList());
            allSessions = FXCollections.observableArrayList(filtrees);
            applyFilter();
        } catch (SQLException e) {
            showStyledAlert("error", "Erreur de chargement", e.getMessage());
        }
    }

    private void loadMesReservations() {
        if (currentUserId == -1) return;
        try {
            List<Reservation> toutes = reservationservice.recupererParEtudiant(currentUserId);
            List<Reservation> visibles = toutes.stream()
                    .filter(r -> !"annulée".equals(r.getStatut()))
                    .collect(Collectors.toList());
            mesResas = FXCollections.observableArrayList(visibles);
            renderMesResas(mesResas);
            if (mesResasCount != null)
                mesResasCount.setText(mesResas.size() + " réservation(s)");
        } catch (SQLException e) {
            showStyledAlert("error", "Erreur", e.getMessage());
        }
    }

    private void loadRecommandationsAuto() {
        if (currentUserId == -1) return;

        if (recoCount != null) recoCount.setText("Analyse en cours…");
        recoCardsPane.getChildren().clear();

        Label spinner = new Label("✨  L'IA analyse ton profil…");
        spinner.setStyle(
                "-fx-font-size:13px;-fx-text-fill:#7C3AED;-fx-font-weight:bold;" +
                        "-fx-padding:20 0 20 0;"
        );
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(28, 28);
        pi.setStyle("-fx-accent:#7C3AED;");
        HBox loading = new HBox(12, pi, spinner);
        loading.setAlignment(Pos.CENTER_LEFT);
        loading.setPadding(new Insets(12));
        recoCardsPane.getChildren().add(loading);

        recoContainer.setVisible(true);
        recoContainer.setManaged(true);

        if (btnRecommander != null) {
            btnRecommander.setText("✖  Masquer");
            btnRecommander.setDisable(true);
        }

        forceScrollPaneRefresh();

        new Thread(() -> {
            try {
                List<Session> recos = recoService.getRecommandations(currentUserId);
                recoRaisons.clear();
                recoRaisons.putAll(recoService.getLastRaisons());

                javafx.application.Platform.runLater(() -> {
                    recoCardsPane.getChildren().clear();

                    if (recos.isEmpty()) {
                        recoContainer.setVisible(false);
                        recoContainer.setManaged(false);
                        if (btnRecommander != null) {
                            btnRecommander.setDisable(false);
                            btnRecommander.setText("✨  Recommandation IA");
                        }
                        forceScrollPaneRefresh();
                        return;
                    }

                    if (recoCount != null) recoCount.setText(recos.size() + " session(s) pour toi");
                    int i = 0;
                    for (Session s : recos) {
                        VBox card = buildSessionCard(s, i++ % CARD_COLORS.length);
                        ajouterBadgeReco(card);
                        ajouterBoutonPourquoi(card, s);
                        recoCardsPane.getChildren().add(card);
                    }
                    animerCartes(recoCardsPane);
                    animerEntree(recoContainer);
                    showSimpleCelebration();

                    if (btnRecommander != null) {
                        btnRecommander.setDisable(false);
                        btnRecommander.setText("✖  Masquer");
                    }
                    forceScrollPaneRefresh();
                });

            } catch (SQLException e) {
                javafx.application.Platform.runLater(() -> {
                    recoCardsPane.getChildren().clear();
                    recoContainer.setVisible(false);
                    recoContainer.setManaged(false);
                    if (btnRecommander != null) {
                        btnRecommander.setDisable(false);
                        btnRecommander.setText("✨  Recommandation IA");
                    }
                    forceScrollPaneRefresh();
                });
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    // FILTRE + RECHERCHE
    // ══════════════════════════════════════════════════════════════

    @FXML private void handleSearch() { applyFilter(); }

    private void applyFilter() {
        String q      = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String statut = filterStatut != null ? filterStatut.getValue() : "Toutes";

        List<Session> filtered = allSessions.stream()
                .filter(s -> {
                    boolean mq = q.isEmpty()
                            || (s.getDescription() != null && s.getDescription().toLowerCase().contains(q))
                            || (s.getStatut() != null && s.getStatut().toLowerCase().contains(q))
                            || (s.getNom() != null && s.getNom().toLowerCase().contains(q));
                    boolean ms = "Toutes".equals(statut) || statut == null || statut.equals(s.getStatut());
                    return mq && ms;
                }).collect(Collectors.toList());

        renderSessions(filtered);
        if (countLabel != null) countLabel.setText(filtered.size() + " session(s)");
    }

    // ══════════════════════════════════════════════════════════════
    // RECOMMANDATIONS IA
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void handleRecommandations() {
        animerBouton(btnRecommander);

        if (recoContainer.isVisible()) {
            animerSortie(recoContainer, () -> {
                recoContainer.setVisible(false);
                recoContainer.setManaged(false);
                btnRecommander.setText("✨  Recommandation IA");
                btnRecommander.setStyle(
                        "-fx-background-color:rgba(255,255,255,0.18);" +
                                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                                "-fx-background-radius:40;-fx-padding:10 22 10 22;-fx-cursor:hand;" +
                                "-fx-border-color:rgba(255,255,255,0.45);-fx-border-radius:40;-fx-border-width:1.5;"
                );
                forceScrollPaneRefresh();
            });
        } else {
            loadRecommandationsAuto();
        }
    }

    private void ajouterBadgeReco(VBox card) {
        if (card.getChildren().isEmpty()) return;
        if (card.getChildren().get(0) instanceof VBox header) {
            Label badge = new Label("✨  Recommandée pour toi");
            badge.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.25);" +
                            "-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:3 10 3 10;"
            );
            header.getChildren().add(0, badge);
        }
    }

    private void ajouterBoutonPourquoi(VBox card, Session s) {
        String[] raisons = recoRaisons.get(s.getId());
        if (raisons == null) return;
        javafx.scene.Node last = card.getChildren().get(card.getChildren().size() - 1);
        if (!(last instanceof HBox actions)) return;

        Button btnWhy = new Button("💡");
        btnWhy.setStyle(
                "-fx-background-color:#EEF2FF;-fx-text-fill:#534AB7;" +
                        "-fx-font-size:13px;-fx-background-radius:8;" +
                        "-fx-padding:6 10 6 10;-fx-cursor:hand;"
        );
        btnWhy.setTooltip(new Tooltip("Pourquoi cette recommandation ?"));
        btnWhy.setOnMouseEntered(e -> {
            animerBouton(btnWhy);
            btnWhy.setStyle("-fx-background-color:#DDD6FE;-fx-text-fill:#4C1D95;" +
                    "-fx-font-size:13px;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;");
        });
        btnWhy.setOnMouseExited(e -> btnWhy.setStyle(
                "-fx-background-color:#EEF2FF;-fx-text-fill:#534AB7;" +
                        "-fx-font-size:13px;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;"
        ));
        btnWhy.setOnAction(e -> showPourquoiDialog(s, raisons));
        actions.getChildren().add(btnWhy);
    }

    // ══════════════════════════════════════════════════════════════
    // DIALOG POURQUOI
    // ══════════════════════════════════════════════════════════════

    private void showPourquoiDialog(Session s, String[] raisons) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Pourquoi cette recommandation ?");
        dialog.setHeaderText(null);

        VBox mainContainer = new VBox(0);
        mainContainer.setMaxWidth(400);
        mainContainer.setPrefWidth(380);
        mainContainer.setMinWidth(320);

        boolean estIA     = "IA — Personnalisée".equals(raisons[0]);
        String  typeLabel = raisons[0];

        String[] couleurs;
        String   icon;

        if (estIA) {
            couleurs = new String[]{"#7C3AED", "#5B21B6"};
            icon     = "✨";
        } else if ("Niveau 1 — Groupe".equals(typeLabel)) {
            couleurs = new String[]{"#7C3AED", "#5B21B6"};
            icon     = "👥";
        } else if ("Niveau 2 — Langue".equals(typeLabel)) {
            couleurs = new String[]{"#2563EB", "#1D4ED8"};
            icon     = "🌐";
        } else {
            couleurs = new String[]{"#059669", "#047857"};
            icon     = "⭐";
        }

        VBox header = new VBox(10);
        header.setPadding(new Insets(22, 24, 18, 24));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,"
                + couleurs[0] + "," + couleurs[1] + ");");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:22px;-fx-background-color:rgba(255,255,255,0.2);"
                + "-fx-background-radius:50;-fx-padding:8 10 8 10;");

        VBox titleBox = new VBox(3);
        Label titleLbl = new Label("Pourquoi cette session ?");
        titleLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");

        String nomSession = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();
        Label sessionLbl = new Label(nomSession + "  (ID #" + s.getId() + ")");
        sessionLbl.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);");
        titleBox.getChildren().addAll(titleLbl, sessionLbl);
        titleRow.getChildren().addAll(iconLbl, titleBox);

        Label niveauBadge = new Label(typeLabel);
        niveauBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;"
                + "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;"
                + "-fx-padding:4 14 4 14;");
        header.getChildren().addAll(titleRow, niveauBadge);

        VBox body = new VBox(10);
        body.setPadding(new Insets(20, 24, 8, 24));
        body.setMaxHeight(450);

        if (raisons[1] != null && !raisons[1].isBlank()) {
            VBox explBox = new VBox(6);
            explBox.setStyle("-fx-background-color:#F5F3FF;-fx-background-radius:12;"
                    + "-fx-padding:14 16 14 16;-fx-border-color:#EDE9FE;"
                    + "-fx-border-radius:12;-fx-border-width:1;");
            Label explTitle = new Label(estIA ? "💬  Analyse de l'IA" : "💡  Raison principale");
            explTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6D28D9;");
            Label explText = new Label(raisons[1]);
            explText.setStyle("-fx-font-size:12px;-fx-text-fill:#4C1D95;-fx-wrap-text:true;");
            explText.setWrapText(true);
            explBox.getChildren().addAll(explTitle, explText);
            body.getChildren().add(explBox);
        }

        Label critLabel = new Label("Critères de sélection");
        critLabel.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6B7280;"
                + "-fx-padding:8 0 4 0;");
        body.getChildren().add(critLabel);

        String[][] criteres;
        if (estIA) {
            criteres = new String[][]{
                    {"✨","Sélection intelligente par l'IA",
                            "L'IA Groq (Llama3) a analysé ton profil complet pour choisir cette session."},
                    {"👤","Adapté à ton profil",
                            raisons[3] != null && !raisons[3].isBlank()
                                    ? raisons[3] : "Basé sur tes langues, niveaux et historique de réservations."},
                    {"✅","Session disponible",
                            "Cette session a des places libres et n'est pas encore dans tes réservations."},
                    {"📊","Moteur de recommandation",
                            raisons[2] != null && !raisons[2].isBlank()
                                    ? raisons[2] : "Recommandation générée par le modèle Llama-3.3-70b."}
            };
        } else if ("Niveau 1 — Groupe".equals(typeLabel)) {
            criteres = new String[][]{
                    {"👥","Tu es dans ce groupe","Tu es membre du groupe auquel cette session est rattachée."},
                    {"✅","Non encore réservée","Tu n'as pas encore réservé cette session."},
                    {"🪑","Places disponibles","La session n'est pas complète — il reste de la place pour toi."},
                    {"📈","Meilleure note du groupe","Parmi toutes les sessions de ton groupe, celle-ci est la mieux notée."}
            };
        } else if ("Niveau 2 — Langue".equals(typeLabel)) {
            criteres = new String[][]{
                    {"🌐","Même langue pratiquée","Tu pratiques déjà cette langue dans au moins un de tes groupes."},
                    {"✅","Non encore réservée","Tu n'as pas encore réservé cette session."},
                    {"🪑","Places disponibles","La session n'est pas complète — il reste de la place pour toi."},
                    {"🔀","Groupe différent","Elle appartient à un autre groupe, ce qui t'offre une nouvelle perspective."}
            };
        } else {
            criteres = new String[][]{
                    {"⭐","Meilleure note disponible","Cette session figure parmi les mieux évaluées de la plateforme."},
                    {"✅","Non encore réservée","Tu n'as pas encore réservé cette session."},
                    {"🪑","Places disponibles","La session n'est pas complète — il reste de la place pour toi."},
                    {"🔍","Découverte","C'est l'occasion d'explorer un domaine que tu ne connais pas encore."}
            };
        }

        for (String[] c : criteres) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.TOP_LEFT);
            row.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:10;"
                    + "-fx-padding:10 14 10 14;-fx-border-color:#E2E8F0;"
                    + "-fx-border-radius:10;-fx-border-width:1;");
            Label iconC = new Label(c[0]); iconC.setStyle("-fx-font-size:16px;-fx-min-width:24;");
            VBox textBox = new VBox(3);
            Label titreC = new Label(c[1]);
            titreC.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
            Label detailC = new Label(c[2]);
            detailC.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;-fx-wrap-text:true;");
            detailC.setWrapText(true);
            textBox.getChildren().addAll(titreC, detailC);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            row.getChildren().addAll(iconC, textBox);
            body.getChildren().add(row);
        }

        VBox sessionInfo = new VBox(6);
        sessionInfo.setStyle("-fx-background-color:#EFF6FF;-fx-background-radius:10;"
                + "-fx-padding:12 16 12 16;-fx-border-color:#BFDBFE;"
                + "-fx-border-radius:10;-fx-border-width:1;");
        Label siTitle = new Label("📚  Session concernée");
        siTitle.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#1D4ED8;");
        Label siNom = new Label(nomSession);
        siNom.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1E3A8A;");
        HBox siChips = new HBox(6); siChips.setAlignment(Pos.CENTER_LEFT);
        if (s.getDuree() != null) {
            Label dl = new Label("⏱ " + s.getDuree() + " min");
            dl.setStyle("-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;"
                    + "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:15;"
                    + "-fx-padding:2 8 2 8;");
            siChips.getChildren().add(dl);
        }
        if (s.getPrix() != null) {
            Label pl = new Label(String.format("%.0f TND", s.getPrix()));
            pl.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#065F46;"
                    + "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:15;"
                    + "-fx-padding:2 8 2 8;");
            siChips.getChildren().add(pl);
        }
        if (s.getStatut() != null) {
            Label sl = new Label(s.getStatut());
            sl.setStyle("-fx-background-color:#EDE9FE;-fx-text-fill:#6D28D9;"
                    + "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:15;"
                    + "-fx-padding:2 8 2 8;");
            siChips.getChildren().add(sl);
        }
        sessionInfo.getChildren().addAll(siTitle, siNom, siChips);
        body.getChildren().add(sessionInfo);

        VBox footer = new VBox(0);
        footer.setPadding(new Insets(12, 24, 20, 24));
        Label footerLbl = new Label(
                "ℹ️  Les recommandations sont recalculées à chaque clic selon ton activité.");
        footerLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;-fx-wrap-text:true;");
        footerLbl.setWrapText(true);
        footer.getChildren().add(footerLbl);

        mainContainer.getChildren().addAll(header, body, footer);

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:#FFFFFF;" +
                "-fx-border-color:transparent;");
        scrollPane.setMaxHeight(550);
        scrollPane.setPrefHeight(480);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(420);
        dialog.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialog.getDialogPane().setMaxHeight(580);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("Fermer");
        closeBtn.setStyle("-fx-background-color:" + couleurs[0] + ";-fx-text-fill:white;"
                + "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;"
                + "-fx-padding:10 28 10 28;-fx-cursor:hand;");
        closeBtn.setMinWidth(120);

        dialog.setOnShown(evt -> {
            dialog.getDialogPane().setOpacity(0);
            dialog.getDialogPane().setScaleX(0.92);
            dialog.getDialogPane().setScaleY(0.92);
            dialog.getDialogPane().setTranslateY(16);
            FadeTransition fade = new FadeTransition(Duration.millis(340), dialog.getDialogPane());
            fade.setFromValue(0); fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(340), dialog.getDialogPane());
            scale.setFromX(0.92); scale.setToX(1.0);
            scale.setFromY(0.92); scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);
            TranslateTransition slide = new TranslateTransition(Duration.millis(340), dialog.getDialogPane());
            slide.setFromY(16); slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, scale, slide).play();

            int delay = 220;
            for (javafx.scene.Node node : body.getChildren()) {
                node.setOpacity(0); node.setTranslateX(-14);
                FadeTransition f = new FadeTransition(Duration.millis(260), node);
                f.setFromValue(0); f.setToValue(1);
                TranslateTransition t = new TranslateTransition(Duration.millis(260), node);
                t.setFromX(-14); t.setToX(0);
                t.setInterpolator(Interpolator.EASE_OUT);
                ParallelTransition pt = new ParallelTransition(f, t);
                pt.setDelay(Duration.millis(delay));
                pt.play();
                delay += 65;
            }
        });
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // RENDU SESSIONS
    // ══════════════════════════════════════════════════════════════

    private void renderSessions(List<Session> list) {
        sessionsContainer.getChildren().clear();
        int i = 0;
        for (Session s : list)
            sessionsContainer.getChildren().add(buildSessionCard(s, i++ % CARD_COLORS.length));

        if (list.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("📭"); icon.setStyle("-fx-font-size:48px;");
            Label msg  = new Label("Aucune session disponible");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            empty.getChildren().addAll(icon, msg);
            sessionsContainer.getChildren().add(empty);
        }
    }

    private VBox buildSessionCard(Session s, int ci) {
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        boolean dejaResa = false;
        int nbPlaces = 0;
        try {
            dejaResa = reservationservice.dejaReserve(currentUserId, s.getId());
            int nbAcc = sessionservice.compterReservationsAcceptees(s.getId());
            if (s.getCapaciteMax() != null) nbPlaces = s.getCapaciteMax() - nbAcc;
        } catch (SQLException ignored) {}

        final boolean alreadyBooked  = dejaResa;
        final int    placesRestantes = nbPlaces;
        final boolean estTerminee    = "terminée".equals(s.getStatut());

        VBox card = new VBox(0);
        card.setPrefWidth(290); card.setMaxWidth(290);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),26,0,0,8);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;"));

        VBox header = new VBox(6);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");" +
                "-fx-background-radius:18 18 0 0;");

        HBox htop = new HBox(8); htop.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getStatutIcon(s.getStatut()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);" +
                "-fx-background-radius:50;-fx-padding:6 8 6 8;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        if (estTerminee) {
            Label t = new Label("✅ Terminée");
            t.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                    "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
            htop.getChildren().addAll(iconLbl, sp, t);
        } else if (alreadyBooked) {
            Label t = new Label("✅ Déjà réservé");
            t.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                    "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
            htop.getChildren().addAll(iconLbl, sp, t);
        } else if (s.getCapaciteMax() != null) {
            Label t = new Label(placesRestantes + " place(s)");
            t.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                    "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
            htop.getChildren().addAll(iconLbl, sp, t);
        } else {
            htop.getChildren().addAll(iconLbl, sp);
        }

        if (s.getNom() != null && !s.getNom().isBlank()) {
            Label nomLabel = new Label(s.getNom());
            nomLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
            nomLabel.setWrapText(true);
            Label dateLabel = new Label(s.getDateHeure() != null
                    ? s.getDateHeure().format(FMT_DT) : "Date à définir");
            dateLabel.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);");
            header.getChildren().addAll(htop, nomLabel, dateLabel);
        } else {
            Label dateLabel = new Label(s.getDateHeure() != null
                    ? s.getDateHeure().format(FMT_DT) : "Date à définir");
            dateLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
            header.getChildren().addAll(htop, dateLabel);
        }

        VBox body = new VBox(10); body.setPadding(new Insets(14, 18, 10, 18));
        String desc = s.getDescription() != null && !s.getDescription().isBlank()
                ? (s.getDescription().length() > 75
                ? s.getDescription().substring(0, 72) + "…"
                : s.getDescription())
                : "Aucune description.";
        Label descL = new Label(desc);
        descL.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descL.setWrapText(true);
        Label statutBadge = buildStatutBadge(s.getStatut());
        HBox infoRow = new HBox(8);
        if (s.getDuree() != null) {
            Label dl = new Label("⏱ " + s.getDuree() + " min");
            dl.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;" +
                    "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(dl);
        }
        if (s.getPrix() != null) {
            Label pl = new Label("💰 " + String.format("%.0f", s.getPrix()) + " TND");
            pl.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;-fx-font-size:11px;" +
                    "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(pl);
        }
        body.getChildren().addAll(descL, statutBadge, infoRow);

        if (estTerminee) {
            try {
                Object[] stats = sessionservice.getStatistiquesSession(s.getId());
                double scorePondere = (double) stats[0];
                String[] classif   = (String[]) stats[1];
                String etoiles     = (String)   stats[3];
                if (scorePondere > 0) {
                    Label classifBadge = new Label(classif[1] + "  " + classif[0]
                            + String.format("  (%.1f/5)", scorePondere));
                    classifBadge.setStyle("-fx-background-color:" + classif[2] +
                            ";-fx-text-fill:" + classif[3] +
                            ";-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:4 12 4 12;");
                    Label etoilesL = new Label(etoiles);
                    etoilesL.setStyle("-fx-font-size:16px;-fx-text-fill:#F59E0B;");
                    body.getChildren().addAll(etoilesL, classifBadge);
                } else {
                    Label nonNotee = new Label("☆☆☆☆☆  Pas encore notée");
                    nonNotee.setStyle("-fx-background-color:#F8FAFC;-fx-text-fill:#94A3B8;" +
                            "-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:4 12 4 12;");
                    body.getChildren().add(nonNotee);
                }
            } catch (SQLException ignored) {}
        }

        if (s.getLienReunion() != null && !s.getLienReunion().isBlank() && !estTerminee) {
            Label lienL = new Label("🔗 " + (s.getLienReunion().length() > 30
                    ? s.getLienReunion().substring(0, 27) + "…" : s.getLienReunion()));
            lienL.setStyle("-fx-font-size:11px;-fx-text-fill:#6C63FF;");
            body.getChildren().add(lienL);
        }

        if (!estTerminee && s.getCapaciteMax() != null
                && placesRestantes <= 2 && placesRestantes >= 0 && !alreadyBooked) {
            Label alertL = new Label("⚠ " + (placesRestantes == 0
                    ? "Session complète !" : "Dernières places !"));
            alertL.setStyle("-fx-background-color:#FFF7ED;-fx-text-fill:#EA580C;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-background-radius:8;-fx-padding:4 10 4 10;");
            body.getChildren().add(alertL);
        }

        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(10, 14, 14, 14));
        actions.setAlignment(Pos.CENTER);

        Button btnDetails = makeBtn("👁 Détails", "#EFF6FF", "#3B82F6");
        HBox.setHgrow(btnDetails, Priority.ALWAYS);
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> showSessionDetails(s));

        if (estTerminee) {
            actions.getChildren().add(btnDetails);
        } else if (alreadyBooked) {
            Label done = new Label("✅ Réservation effectuée");
            done.setStyle("-fx-font-size:12px;-fx-text-fill:#059669;-fx-font-weight:bold;");
            actions.getChildren().addAll(btnDetails, done);
        } else if (s.getCapaciteMax() != null && placesRestantes <= 0) {
            Label full = new Label("🚫 Session complète");
            full.setStyle("-fx-font-size:12px;-fx-text-fill:#E11D48;-fx-font-weight:bold;");
            actions.getChildren().addAll(btnDetails, full);
        } else {
            Button btnResa = new Button("📋  Réserver");
            btnResa.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnResa, Priority.ALWAYS);
            btnResa.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");" +
                    "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                    "-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;");
            btnResa.setOnAction(e -> handleReserver(s));
            actions.getChildren().addAll(btnDetails, btnResa);
        }

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    // ══════════════════════════════════════════════════════════════
    // DÉTAILS SESSION
    // ══════════════════════════════════════════════════════════════

    private void showSessionDetails(Session s) {
        int ci = (int)(s.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + (s.getNom() != null && !s.getNom().isBlank()
                ? s.getNom() : "Session #" + s.getId()));
        dialog.setHeaderText(null);
        VBox content = new VBox(0); content.setPrefWidth(480);

        VBox header = new VBox(6); header.setPadding(new Insets(22, 26, 18, 26));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");

        if (s.getNom() != null && !s.getNom().isBlank()) {
            Label nomLbl = new Label(s.getNom());
            nomLbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
            nomLbl.setWrapText(true);
            Label dateLbl = new Label("📅  " + (s.getDateHeure() != null
                    ? s.getDateHeure().format(FMT_DT) : "—"));
            dateLbl.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(255,255,255,0.88);");
            Label statutLbl = new Label(s.getStatut() != null ? s.getStatut().toUpperCase() : "");
            statutLbl.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 12 3 12;");
            header.getChildren().addAll(nomLbl, dateLbl, statutLbl);
        } else {
            Label tl = new Label("📅  Session du " + (s.getDateHeure() != null
                    ? s.getDateHeure().format(FMT_DT) : "—"));
            tl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
            Label sl = new Label(s.getStatut() != null ? s.getStatut().toUpperCase() : "");
            sl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
            header.getChildren().addAll(tl, sl);
        }

        VBox body = new VBox(14); body.setPadding(new Insets(18, 26, 22, 26));
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(130);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);
        String ls = "-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";

        int rowIdx = 0;
        if (s.getNom() != null && !s.getNom().isBlank())
            addGridRow(grid, rowIdx++, "Nom", s.getNom(), ls,
                    "-fx-font-size:14px;-fx-text-fill:#1F2937;-fx-font-weight:bold;");
        addGridRow(grid, rowIdx++, "Statut",   s.getStatut() != null ? s.getStatut() : "—", ls, vs);
        addGridRow(grid, rowIdx++, "Date",     s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "—", ls, vs);
        addGridRow(grid, rowIdx++, "Durée",    s.getDuree() != null ? s.getDuree() + " min" : "—", ls, vs);
        addGridRow(grid, rowIdx++, "Prix",     s.getPrix() != null ? String.format("%.2f TND", s.getPrix()) : "Gratuit", ls, vs);
        addGridRow(grid, rowIdx++, "Capacité", s.getCapaciteMax() != null ? String.valueOf(s.getCapaciteMax()) : "Illimitée", ls, vs);
        if (!"terminée".equals(s.getStatut()))
            addGridRow(grid, rowIdx++, "Lien réunion", s.getLienReunion() != null ? s.getLienReunion() : "—", ls, vs);

        try {
            Object[] stats = sessionservice.getStatistiquesSession(s.getId());
            double scorePondere = (double) stats[0];
            String[] classif   = (String[]) stats[1];
            double[] rep       = (double[]) stats[2];
            String etoiles     = (String)   stats[3];
            if (scorePondere > 0) {
                addGridRow(grid, rowIdx++, "Score pondéré",
                        String.format("%.2f/5 — %s %s", scorePondere, classif[1], classif[0]), ls, vs);
                addGridRow(grid, rowIdx++, "Étoiles", etoiles, ls,
                        "-fx-font-size:16px;-fx-text-fill:#F59E0B;");
            } else {
                addGridRow(grid, rowIdx++, "Note", "Pas encore notée", ls, vs);
            }
            if (rep[0] > 0) {
                int nbPRep = (int) Math.round(rep[0]);
                String etoilesRep = "★".repeat(Math.max(0, Math.min(5, nbPRep)))
                        + "☆".repeat(Math.max(0, 5 - nbPRep));
                addGridRow(grid, rowIdx, "Réputation groupe",
                        String.format("%s %.2f/5 (%d/%d sessions notées)",
                                etoilesRep, rep[0], (int) rep[2], (int) rep[1]), ls, vs);
            }
        } catch (SQLException ignored) {}

        body.getChildren().add(grid);
        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            TextArea da = new TextArea(s.getDescription());
            da.setEditable(false); da.setWrapText(true); da.setPrefHeight(65);
            da.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;" +
                    "-fx-border-radius:10;-fx-font-size:13px;");
            body.getChildren().add(da);
        }
        content.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // MES RÉSERVATIONS
    // ══════════════════════════════════════════════════════════════

    private void renderMesResas(List<Reservation> list) {
        if (mesResasContainer == null) return;
        mesResasContainer.getChildren().clear();
        int i = 0;
        for (Reservation r : list)
            mesResasContainer.getChildren().add(buildResaCard(r, i++ % CARD_COLORS.length));

        if (list.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label icon = new Label("📭"); icon.setStyle("-fx-font-size:40px;");
            Label msg  = new Label("Vous n'avez aucune réservation");
            msg.setStyle("-fx-font-size:14px;-fx-text-fill:#9CA3AF;");
            empty.getChildren().addAll(icon, msg);
            mesResasContainer.getChildren().add(empty);
        }
    }

    private VBox buildResaCard(Reservation r, int ci) {
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        DateTimeFormatter fmtD = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        VBox card = new VBox(0); card.setPrefWidth(270); card.setMaxWidth(270);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),22,0,0,7);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);"));

        VBox header = new VBox(6); header.setPadding(new Insets(16, 18, 12, 18));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");" +
                "-fx-background-radius:16 16 0 0;");
        Label statusBadge = new Label(getResaStatutIcon(r.getStatut()) + "  " + r.getStatut().toUpperCase());
        statusBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        Label idLbl = new Label("Réservation #" + r.getId());
        idLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        header.getChildren().addAll(statusBadge, idLbl);

        VBox body = new VBox(8); body.setPadding(new Insets(12, 16, 10, 16));
        Label dateLbl = new Label("📅 Réservé le " + r.getDateReservation().format(fmtD));
        dateLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");

        String nomSession = "";
        try {
            Session sessionLiee = sessionservice.recupererParId(r.getIdSessionId());
            if (sessionLiee != null && sessionLiee.getNom() != null && !sessionLiee.getNom().isBlank())
                nomSession = sessionLiee.getNom();
        } catch (SQLException ignored) {}

        Label sessLbl = new Label("🎓 " + (nomSession.isBlank()
                ? "Session #" + r.getIdSessionId() : nomSession));
        sessLbl.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;" +
                "-fx-wrap-text:true;");
        sessLbl.setWrapText(true);
        body.getChildren().addAll(dateLbl, sessLbl);

        if (r.getCommentaire() != null && !r.getCommentaire().isBlank()) {
            Label cl = new Label("💬 " + r.getCommentaire());
            cl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
            cl.setWrapText(true);
            body.getChildren().add(cl);
        }
        if (r.getDateConfirmation() != null) {
            Label confL = new Label("✔ Confirmé le " + r.getDateConfirmation()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            confL.setStyle("-fx-font-size:11px;-fx-text-fill:#059669;-fx-font-weight:bold;");
            body.getChildren().add(confL);
        }

        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(10, 14, 12, 14));
        actions.setAlignment(Pos.CENTER);

        if ("en attente".equals(r.getStatut())) {
            Button btnAnn = new Button("🚫  Annuler ma réservation");
            btnAnn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnAnn, Priority.ALWAYS);
            btnAnn.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;" +
                    "-fx-font-size:12px;-fx-font-weight:bold;" +
                    "-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;");
            btnAnn.setOnAction(e -> handleAnnulerResa(r));
            actions.getChildren().add(btnAnn);
        } else if ("acceptée".equals(r.getStatut())) {
            try {
                Session sessionLiee = sessionservice.recupererParId(r.getIdSessionId());
                if (sessionLiee != null && !"terminée".equals(sessionLiee.getStatut())
                        && sessionLiee.getLienReunion() != null
                        && !sessionLiee.getLienReunion().isBlank()) {
                    Button btnRejoindre = new Button("🎥  Rejoindre la réunion");
                    btnRejoindre.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(btnRejoindre, Priority.ALWAYS);
                    btnRejoindre.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:10;" +
                            "-fx-padding:9 0 9 0;-fx-cursor:hand;");
                    btnRejoindre.setOnAction(ev ->
                            JitsiUtil.ouvrirDansAppDesktop(sessionLiee.getLienReunion()));
                    actions.getChildren().add(btnRejoindre);
                } else {
                    Label info = new Label("✅ Acceptée");
                    info.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748B;");
                    actions.getChildren().add(info);
                }
            } catch (SQLException ex) {
                Label info = new Label("✅ Acceptée");
                info.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748B;");
                actions.getChildren().add(info);
            }
        } else {
            Label info = new Label(getResaStatutIcon(r.getStatut()) + " " + r.getStatut());
            info.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748B;");
            actions.getChildren().add(info);
        }

        try {
            if (reservationservice.peutNoter(r.getIdSessionId(), currentUserId)) {
                Button btnNoter = new Button("⭐  Évaluer cette session");
                btnNoter.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(btnNoter, Priority.ALWAYS);
                btnNoter.setStyle("-fx-background-color:#FFFBEB;-fx-text-fill:#D97706;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;");
                btnNoter.setOnAction(e -> showRatingDialogAvance(r.getIdSessionId()));
                actions.getChildren().add(btnNoter);
            }
        } catch (SQLException ignored) {}

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    // ══════════════════════════════════════════════════════════════
    // NOTATION
    // ══════════════════════════════════════════════════════════════

    private void showRatingDialogAvance(int sessionId) {
        try {
            Session s = sessionservice.recupererParId(sessionId);
            if (s == null) return;
            int nbAcc = sessionservice.compterReservationsAcceptees(sessionId);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Évaluer la session");
            dialog.setHeaderText(null);

            VBox content = new VBox(0); content.setPrefWidth(440);

            VBox header = new VBox(8); header.setPadding(new Insets(20, 24, 16, 24));
            header.setStyle("-fx-background-color:linear-gradient(to right,#F59E0B,#D97706);");
            String titreSession = (s.getNom() != null && !s.getNom().isBlank())
                    ? s.getNom() : "Session #" + sessionId;
            Label tl = new Label("⭐  Évaluer — " + titreSession);
            tl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
            tl.setWrapText(true);
            Label sub = new Label("Votre avis aide les autres étudiants à choisir leurs sessions");
            sub.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);");
            header.getChildren().addAll(tl, sub);

            VBox body = new VBox(14); body.setPadding(new Insets(18, 24, 22, 24));
            Label instruc = new Label("Sélectionne ta note (1 à 5 étoiles) :");
            instruc.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-font-weight:bold;");

            StarRatingComponent starComp = new StarRatingComponent();
            if (s.getRating() != null) starComp.setRating(s.getRating());

            Label feedbackLabel = new Label("Sélectionne une note pour voir l'impact...");
            feedbackLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#94A3B8;-fx-font-style:italic;-fx-wrap-text:true;");
            feedbackLabel.setWrapText(true);

            double taux = s.getCapaciteMax() != null && s.getCapaciteMax() > 0
                    ? Math.min(1.0, (double) nbAcc / s.getCapaciteMax()) : 1.0;
            String pctRemplissage = String.format("%.0f%%", taux * 100);

            VBox infoBox = new VBox(6);
            infoBox.setStyle("-fx-background-color:#EEF2FF;-fx-background-radius:10;" +
                    "-fx-padding:12 14 12 14;-fx-border-color:#C7D2FE;" +
                    "-fx-border-radius:10;-fx-border-width:1;");
            Label infoTitle = new Label("ℹ️  Comment fonctionne la notation ?");
            infoTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#3730A3;");
            Label infoText = new Label(
                    "Ton score est pondéré selon le taux de participation (" + pctRemplissage + ").\n" +
                            "Formule : note × (0.7 + 0.3 × taux_remplissage)\n" +
                            "Plus la session était remplie, plus ton avis a de poids.");
            infoText.setStyle("-fx-font-size:11px;-fx-text-fill:#4338CA;-fx-wrap-text:true;");
            infoText.setWrapText(true);
            infoBox.getChildren().addAll(infoTitle, infoText);

            for (int i = 0; i < 5; i++) {
                final int noteVal = i + 1;
                starComp.getChildren().get(i).setOnMouseClicked(e -> {
                    starComp.setRating(noteVal);
                    double scorePreview = sessionservice.calculerScorePondere(
                            noteVal, nbAcc, s.getCapaciteMax());
                    String[] classif = sessionservice.getClassificationQualite(scorePreview);
                    int nbP = (int) Math.round(scorePreview);
                    String etoilesP = "★".repeat(Math.max(0, Math.min(5, nbP)))
                            + "☆".repeat(Math.max(0, 5 - nbP));
                    feedbackLabel.setText(classif[1] + " Score pondéré : "
                            + String.format("%.2f/5", scorePreview)
                            + "  →  " + etoilesP + "  →  Classif. : " + classif[0]);
                    feedbackLabel.setStyle("-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-text-fill:" + classif[3] +
                            ";-fx-background-color:" + classif[2] +
                            ";-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-wrap-text:true;");
                });
            }

            body.getChildren().addAll(instruc, starComp, feedbackLabel, infoBox);
            content.getChildren().addAll(header, body);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");

            ButtonType btnValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnValider, ButtonType.CANCEL);
            Button valBtn = (Button) dialog.getDialogPane().lookupButton(btnValider);
            valBtn.setStyle("-fx-background-color:#F59E0B;-fx-text-fill:white;" +
                    "-fx-font-size:13px;-fx-font-weight:bold;" +
                    "-fx-background-radius:8;-fx-padding:8 20 8 20;");

            dialog.showAndWait().ifPresent(btn -> {
                if (btn == btnValider) {
                    int rating = starComp.getRating();
                    if (rating == 0) {
                        showStyledAlert("warning", "Note manquante", "Sélectionne au moins 1 étoile !");
                        return;
                    }
                    try {
                        Object[] anomalie = sessionservice.verifierAnomalieNote(s.getIdGroupId(), rating);
                        boolean estAberrante = (boolean) anomalie[0];
                        String msgAnomalies  = (String)  anomalie[1];
                        if (estAberrante) {
                            Alert avert = new Alert(Alert.AlertType.WARNING);
                            avert.setTitle("Note inhabituelle détectée");
                            avert.setHeaderText("⚠️  Anomalie statistique");
                            avert.setContentText(msgAnomalies +
                                    "\n\nVoulez-vous quand même valider cette note ?");
                            avert.getButtonTypes().setAll(
                                    new ButtonType("Oui, valider quand même", ButtonBar.ButtonData.OK_DONE),
                                    ButtonType.CANCEL);
                            avert.showAndWait().ifPresent(resp -> {
                                if (resp.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                                    enregistrerEtAfficherConfirmation(sessionId, rating, s, nbAcc);
                            });
                        } else {
                            enregistrerEtAfficherConfirmation(sessionId, rating, s, nbAcc);
                        }
                    } catch (SQLException e) {
                        showStyledAlert("error", "Erreur", e.getMessage());
                    }
                }
            });
        } catch (SQLException e) {
            showStyledAlert("error", "Erreur", e.getMessage());
        }
    }

    private void enregistrerEtAfficherConfirmation(int sessionId, int rating,
                                                   Session s, int nbAcc) {
        try {
            double scorePondere = sessionservice.calculerScorePondere(
                    rating, nbAcc, s.getCapaciteMax());
            String[] classif = sessionservice.getClassificationQualite(scorePondere);
            int nbP = (int) Math.round(scorePondere);
            String etoiles = "★".repeat(Math.max(0, Math.min(5, nbP)))
                    + "☆".repeat(Math.max(0, 5 - nbP));
            sessionservice.enregistrerRatingAvance(sessionId, rating);
            String msg = String.format(
                    "Note brute : %d★\nScore pondéré : %.2f/5\nClassification : %s %s\n" +
                            "Représentation : %s\n\nMerci pour ton retour !",
                    rating, scorePondere, classif[1], classif[0], etoiles);
            showStyledAlert("success", "Note enregistrée ! ⭐", msg);
            loadAll();
        } catch (SQLException ex) {
            showStyledAlert("error", "Erreur lors de l'enregistrement", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // RÉSERVATION
    // ══════════════════════════════════════════════════════════════

    private void handleReserver(Session s) {
        Stage ownerStage = rootPane.getScene() != null
                ? (Stage) rootPane.getScene().getWindow() : null;
        new SessionQRManager(currentUserId).showReservationDialog(s, ownerStage, () -> loadAll());
    }

    @FXML
    private void handleScannerQR() {
        Stage ownerStage = rootPane.getScene() != null
                ? (Stage) rootPane.getScene().getWindow() : null;
        Stage scanStage = new Stage();
        scanStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        if (ownerStage != null) scanStage.initOwner(ownerStage);
        scanStage.setTitle("Scanner un QR Code");
        scanStage.setResizable(false);
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:#F8FAFC;");
        Label info = new Label("📷  Fonctionnalité de scan QR\nnon disponible sur bureau.\n\n" +
                "Utilisez le QR affiché sur le bouton\n\"Réserver\" pour scanner avec votre téléphone.");
        info.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-text-alignment:center;-fx-wrap-text:true;");
        info.setWrapText(true); info.setMaxWidth(300);
        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color:#3B82F6;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:10;-fx-padding:10 24 10 24;-fx-cursor:hand;");
        btnClose.setOnAction(e -> scanStage.close());
        root.getChildren().addAll(info, btnClose);
        scanStage.setScene(new javafx.scene.Scene(root, 340, 220));
        scanStage.show();
    }

    private void handleAnnulerResa(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la réservation");
        confirm.setContentText("Êtes-vous sûr de vouloir annuler la réservation #" + r.getId() + " ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    reservationservice.annuler(r.getId());
                    loadAll();
                    showStyledAlert("warning", "Réservation annulée",
                            "Réservation #" + r.getId() + " annulée.");
                } catch (SQLException e) {
                    showStyledAlert("error", "Erreur BD", e.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // CHARGEMENT CALENDRIER ÉTUDIANT (onglet 2)
    // ══════════════════════════════════════════════════════════════

    private void chargerCalendrierEtudiant() {
        if (calendrierEtudiantContainer == null) return;
        if (!calendrierEtudiantContainer.getChildren().isEmpty()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/calendrier.fxml"));
            javafx.scene.Node vue = loader.load();
            CalendrierController ctrl = loader.getController();
            ctrl.setUserId(currentUserId, false);
            VBox.setVgrow(vue, Priority.ALWAYS);
            calendrierEtudiantContainer.getChildren().add(vue);
        } catch (Exception e) {
            Label errLbl = new Label("❌ Impossible de charger le calendrier : " + e.getMessage());
            errLbl.setStyle("-fx-font-size:14px;-fx-text-fill:#E11D48;-fx-padding:20;-fx-wrap-text:true;");
            errLbl.setWrapText(true);
            calendrierEtudiantContainer.getChildren().add(errLbl);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ANIMATIONS
    // ══════════════════════════════════════════════════════════════

    private void animerEntree(VBox node) {
        node.setOpacity(0); node.setTranslateY(-24);
        FadeTransition fade = new FadeTransition(Duration.millis(450), node);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(450), node);
        slide.setFromY(-24); slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private void animerSortie(VBox node, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(1); fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
        slide.setFromY(0); slide.setToY(-16);
        slide.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setOnFinished(e -> onFinished.run());
        pt.play();
    }

    private void animerCartes(FlowPane pane) {
        int delay = 0;
        for (javafx.scene.Node card : pane.getChildren()) {
            card.setOpacity(0); card.setTranslateY(30);
            card.setScaleX(0.93); card.setScaleY(0.93);
            FadeTransition fade = new FadeTransition(Duration.millis(380), card);
            fade.setFromValue(0); fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(380), card);
            slide.setFromY(30); slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition scale = new ScaleTransition(Duration.millis(380), card);
            scale.setFromX(0.93); scale.setToX(1.0);
            scale.setFromY(0.93); scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition pt = new ParallelTransition(fade, slide, scale);
            pt.setDelay(Duration.millis(delay));
            pt.play();
            delay += 130;
        }
    }

    private void animerBouton(Button btn) {
        ScaleTransition down = new ScaleTransition(Duration.millis(90), btn);
        down.setFromX(1.0); down.setToX(0.92);
        down.setFromY(1.0); down.setToY(0.92);
        ScaleTransition up = new ScaleTransition(Duration.millis(160), btn);
        up.setFromX(0.92); up.setToX(1.0);
        up.setFromY(0.92); up.setToY(1.0);
        up.setInterpolator(Interpolator.EASE_OUT);
        new SequentialTransition(down, up).play();
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private Label buildStatutBadge(String statut) {
        String bg, fg, icon;
        switch (statut != null ? statut : "") {
            case "planifiée" -> { bg = "#EFF6FF"; fg = "#3B82F6"; icon = "📅"; }
            case "en cours"  -> { bg = "#EEF2FF"; fg = "#6C63FF"; icon = "🔄"; }
            case "terminée"  -> { bg = "#ECFDF5"; fg = "#059669"; icon = "✅"; }
            case "annulée"   -> { bg = "#FFF1F2"; fg = "#E11D48"; icon = "❌"; }
            default          -> { bg = "#F8FAFC"; fg = "#64748B"; icon = "📌"; }
        }
        Label l = new Label(icon + "  " + (statut != null ? statut : "—"));
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return l;
    }

    private String getStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "planifiée" -> "📅";
            case "en cours"  -> "🔄";
            case "terminée"  -> "✅";
            case "annulée"   -> "❌";
            default          -> "📌";
        };
    }

    private String getResaStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "en attente" -> "⏳";
            case "acceptée"   -> "✅";
            case "refusée"    -> "❌";
            case "annulée"    -> "🚫";
            default           -> "📋";
        };
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:7 10 7 10;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void addGridRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll = new Label(l); ll.setStyle(ls);
        Label vv = new Label(v); vv.setStyle(vs);
        vv.setWrapText(true);
        g.add(ll, 0, row); g.add(vv, 1, row);
    }

    private void showStyledAlert(String type, String title, String message) {
        Alert.AlertType t = switch (type) {
            case "success" -> Alert.AlertType.INFORMATION;
            case "error"   -> Alert.AlertType.ERROR;
            case "warning" -> Alert.AlertType.WARNING;
            default        -> Alert.AlertType.INFORMATION;
        };
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // CÉLÉBRATION IA
    // ══════════════════════════════════════════════════════════════

    private void showSimpleCelebration() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (recoCardsPane == null || recoCardsPane.getChildren().isEmpty()) return;
                Parent parent = recoCardsPane.getParent();
                if (parent == null) return;

                double x = recoCardsPane.getLayoutX();
                double y = recoCardsPane.getLayoutY();

                Pane overlay = new Pane();
                overlay.setMouseTransparent(true);
                overlay.setPrefSize(recoCardsPane.getWidth(), recoCardsPane.getHeight());
                overlay.setStyle("-fx-background-color:transparent;");
                overlay.setLayoutX(x); overlay.setLayoutY(y);

                if (parent instanceof Pane) {
                    ((Pane) parent).getChildren().add(overlay);
                } else {
                    recoCardsPane.getChildren().add(overlay);
                    overlay.toFront();
                }

                String[] colors = {"#7C3AED","#3B82F6","#10B981","#F59E0B",
                        "#EF4444","#EC4899","#06B6D4"};
                Random random = new Random();

                for (int i = 0; i < 150; i++) {
                    Rectangle confetti = new Rectangle(6, 10);
                    confetti.setFill(Color.web(colors[random.nextInt(colors.length)]));
                    confetti.setArcWidth(3); confetti.setArcHeight(3);
                    double startX = random.nextDouble() * overlay.getPrefWidth();
                    double startY = -50 - random.nextDouble() * 100;
                    confetti.setTranslateX(startX); confetti.setTranslateY(startY);
                    confetti.setRotate(random.nextDouble() * 360);
                    confetti.setOpacity(0.9);
                    overlay.getChildren().add(confetti);
                    double duration = 2 + random.nextDouble() * 2;
                    TranslateTransition tt = new TranslateTransition(Duration.seconds(duration), confetti);
                    tt.setFromY(startY); tt.setToY(overlay.getPrefHeight() + 100);
                    RotateTransition rt = new RotateTransition(Duration.seconds(duration), confetti);
                    rt.setByAngle(360 * (random.nextBoolean() ? 1 : -1));
                    FadeTransition ft = new FadeTransition(Duration.seconds(duration), confetti);
                    ft.setFromValue(0.9); ft.setToValue(0);
                    new ParallelTransition(tt, rt, ft).play();
                }

                for (int i = 0; i < 60; i++) {
                    Label heart = new Label("❤️");
                    heart.setStyle("-fx-font-size:14px;"); heart.setOpacity(0.8);
                    double startX = random.nextDouble() * overlay.getPrefWidth();
                    double startY = overlay.getPrefHeight();
                    heart.setTranslateX(startX); heart.setTranslateY(startY);
                    overlay.getChildren().add(heart);
                    double duration = 2.5 + random.nextDouble() * 2;
                    TranslateTransition tt = new TranslateTransition(Duration.seconds(duration), heart);
                    tt.setFromY(startY); tt.setToY(-100);
                    FadeTransition ft = new FadeTransition(Duration.seconds(duration), heart);
                    ft.setFromValue(0.8); ft.setToValue(0);
                    ParallelTransition pt = new ParallelTransition(tt, ft);
                    pt.setOnFinished(e -> overlay.getChildren().remove(heart));
                    pt.play();
                }

                Timeline globalCleanup = new Timeline(new KeyFrame(Duration.seconds(6), e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), overlay);
                    fadeOut.setFromValue(1); fadeOut.setToValue(0);
                    fadeOut.setOnFinished(ev -> {
                        if (parent instanceof Pane)
                            ((Pane) parent).getChildren().remove(overlay);
                        else
                            recoCardsPane.getChildren().remove(overlay);
                    });
                    fadeOut.play();
                }));
                globalCleanup.setCycleCount(1);
                globalCleanup.play();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}