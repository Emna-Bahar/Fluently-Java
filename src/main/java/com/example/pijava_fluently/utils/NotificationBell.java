package com.example.pijava_fluently.utils;

import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Reservationservice;
import com.example.pijava_fluently.services.Sessionservice;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

public class NotificationBell {

    private final Sessionservice     sessionSvc     = new Sessionservice();
    private final Reservationservice reservationSvc = new Reservationservice();

    private final int     userId;
    private final boolean isProfesseur;
    private final Pane    rootOverlay;

    private final List<NotifItem>  notifications = new ArrayList<>();
    private final Set<String>      notifiedKeys  = new HashSet<>();

    // ── Suivi des réservations déjà vues ──────────────────────────
    // Pour le prof : IDs des réservations "en attente" déjà notifiées
    private final Set<Integer> resasEnAttenteNotifiees = new HashSet<>();
    // Pour l'étudiant : IDs des réservations "acceptée" déjà notifiées
    private final Set<Integer> resasAccepteesNotifiees = new HashSet<>();

    private int                    unreadCount   = 0;
    private VBox                   panneau       = null;
    private boolean                panneauOuvert = false;
    private Popup                  panneauPopup  = null;

    private final StackPane bellRoot;
    private final Label     badgeLabel;
    private final Label     bellIcon;

    private ScheduledExecutorService scheduler;

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // Fenêtre de détection : 60 jours
    private static final long FENETRE_MINUTES = 86400L;

    // ═════════════════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ═════════════════════════════════════════════════════════════

    public NotificationBell(int userId, boolean isProfesseur, Pane rootOverlay) {
        this.userId       = userId;
        this.isProfesseur = isProfesseur;
        this.rootOverlay  = rootOverlay;

        bellIcon = new Label();
        bellIcon.setGraphic(createBellShape());
        bellIcon.setStyle(
                "-fx-cursor:hand;" +
                        "-fx-padding:10;" +
                        "-fx-background-color:rgba(0,0,0,0.25);" +
                        "-fx-background-radius:50;"
        );

        badgeLabel = new Label("");
        badgeLabel.setVisible(false);
        badgeLabel.setManaged(false);
        badgeLabel.setMouseTransparent(true);
        badgeLabel.setStyle(
                "-fx-background-color:#E11D48;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:9px;-fx-font-weight:bold;" +
                        "-fx-background-radius:50;" +
                        "-fx-min-width:17;-fx-min-height:17;" +
                        "-fx-max-width:22;-fx-max-height:17;" +
                        "-fx-alignment:CENTER;-fx-padding:1 3 1 3;"
        );
        StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeLabel, new Insets(-5, -3, 0, 0));

        bellRoot = new StackPane(bellIcon, badgeLabel);
        bellRoot.setPrefSize(40, 40);
        bellRoot.setMaxSize(40, 40);
        bellRoot.setAlignment(Pos.CENTER);

        bellRoot.setOnMouseEntered(e -> bellIcon.setStyle(
                "-fx-font-size:20px;-fx-cursor:hand;-fx-padding:7 8 7 8;" +
                        "-fx-background-color:rgba(0,0,0,0.4);-fx-background-radius:50;" +
                        "-fx-text-fill:white;"
        ));
        bellRoot.setOnMouseExited(e -> bellIcon.setStyle(
                "-fx-font-size:20px;-fx-cursor:hand;-fx-padding:7 8 7 8;" +
                        "-fx-background-color:rgba(0,0,0,0.25);-fx-background-radius:50;" +
                        "-fx-text-fill:white;"
        ));
        bellRoot.setOnMouseClicked(e -> {
            e.consume();
            togglePanneau();
        });

        demarrerScheduler();
        // Vérification initiale immédiate en arrière-plan
        new Thread(this::verifierSessionsSilencieux).start();
    }

    public StackPane getBellRoot() { return bellRoot; }

    private Node createBellShape() {
        SVGPath s = new SVGPath();
        s.setContent("M12 22c1.1 0 2-.9 2-2h-4a2 2 0 002 2zm6-6V11c0-3.07-1.63-5.64-4.5-6.32V4a1.5 1.5 0 00-3 0v.68C7.63 5.36 6 7.92 6 11v5l-2 2h16l-2-2z");
        s.setFill(Color.WHITE);
        s.setScaleX(1.2); s.setScaleY(1.2);
        s.setTranslateX(4); s.setTranslateY(4);
        return s;
    }

    // ═════════════════════════════════════════════════════════════
    //  SCHEDULER — vérification toutes les 30 secondes
    // ═════════════════════════════════════════════════════════════

    private void demarrerScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NotifBell");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::verifierSessionsSilencieux, 0, 30, TimeUnit.SECONDS);  // ← ICI
    }

    // ═════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE VÉRIFICATION
    // ═════════════════════════════════════════════════════════════

    private void verifierSessionsSilencieux() {
        verifierSessionsInternal(false);
    }

    private void verifierSessionsAvecToast() {
        verifierSessionsInternal(true);
    }

    private void verifierSessionsInternal(boolean afficherToasts) {
        System.out.println("🔍 verifierSessions userId=" + userId + " isProfesseur=" + isProfesseur);
        if (userId == -1) return;

        List<NotifItem> nouvelles = new ArrayList<>();

        try {
            if (isProfesseur) {
                // ── PROFESSEUR : nouvelles demandes de réservation ──
                verifierDemandesReservationProfesseur(nouvelles);
                // ── PROFESSEUR : rappels sessions à venir ──────────
                verifierRappelsSessionsProfesseur(nouvelles);
            } else {
                // ── ÉTUDIANT : réservations acceptées/refusées ─────
                verifierStatutReservationsEtudiant(nouvelles);
                // ── ÉTUDIANT : rappels sessions à venir ────────────
                verifierRappelsSessionsEtudiant(nouvelles);
            }
        } catch (SQLException e) {
            System.err.println("❌ NotifBell erreur BD : " + e.getMessage());
        }

        System.out.println("🔔 " + nouvelles.size() + " nouvelles notifs");

        Platform.runLater(() -> {
            for (NotifItem item : nouvelles) {
                notifications.add(0, item);
                unreadCount++;
                if (afficherToasts) afficherToast(item);
            }
            if (!nouvelles.isEmpty()) {
                majBadge();
                animerCloche();
            }
            if (panneauOuvert) rebuildPanneau();
        });
    }

    // ═════════════════════════════════════════════════════════════
    //  PROFESSEUR — Nouvelles demandes de réservation en attente
    // ═════════════════════════════════════════════════════════════

    private void verifierDemandesReservationProfesseur(List<NotifItem> nouvelles)
            throws SQLException {
        // Récupérer toutes les réservations en attente pour les sessions du prof
        List<Reservation> toutesResas = reservationSvc.recuperer();

        for (Reservation r : toutesResas) {
            if (r.getStatut() == null) continue;

            String statut = normaliserStatut(r.getStatut());

            // Notifier uniquement les "en attente" pas encore vues
            if (statut.contains("attente") && !resasEnAttenteNotifiees.contains(r.getId())) {
                // Vérifier que la session appartient bien à ce professeur
                Session session = sessionSvc.recupererParId(r.getIdSessionId());
                if (session != null && session.getIdUserId() == userId) {
                    resasEnAttenteNotifiees.add(r.getId());

                    String nomSession = nomSession(session);
                    nouvelles.add(new NotifItem(
                            "📩",
                            "Nouvelle demande de réservation",
                            "Un étudiant demande à rejoindre : " + nomSession,
                            "#7C3AED",
                            LocalDateTime.now(),
                            session
                    ));
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  PROFESSEUR — Rappels sessions à venir
    // ═════════════════════════════════════════════════════════════

    private void verifierRappelsSessionsProfesseur(List<NotifItem> nouvelles)
            throws SQLException {
        List<Session> sessions = sessionSvc.recuperer();
        LocalDateTime now = LocalDateTime.now();

        for (Session s : sessions) {
            if (s.getIdUserId() != userId) continue;
            if (s.getDateHeure() == null) continue;
            if ("annulée".equals(s.getStatut())) continue;

            ajouterRappelsTemporels(s, now, nouvelles);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  ÉTUDIANT — Statut réservations (acceptée / refusée)
    // ═════════════════════════════════════════════════════════════

    private void verifierStatutReservationsEtudiant(List<NotifItem> nouvelles)
            throws SQLException {
        List<Reservation> resas = reservationSvc.recupererParEtudiant(userId);

        for (Reservation r : resas) {
            if (r.getStatut() == null) continue;

            String statut = normaliserStatut(r.getStatut());

            // Réservation ACCEPTÉE — notifier l'étudiant
            if ((statut.contains("accept") || statut.contains("confirmee") || statut.contains("validee"))
                    && !resasAccepteesNotifiees.contains(r.getId())) {

                resasAccepteesNotifiees.add(r.getId());

                Session session = sessionSvc.recupererParId(r.getIdSessionId());
                String nomSession = session != null ? nomSession(session) : "Session #" + r.getIdSessionId();
                String dateInfo = (session != null && session.getDateHeure() != null)
                        ? " · Le " + session.getDateHeure().format(FMT_DATE) : "";

                nouvelles.add(new NotifItem(
                        "✅",
                        "Réservation acceptée !",
                        "Ta demande pour « " + nomSession + " » a été acceptée" + dateInfo,
                        "#10B981",
                        LocalDateTime.now(),
                        session
                ));
            }

            // Réservation REFUSÉE — notifier l'étudiant
            String keyRefus = "refus_" + r.getId();
            if ((statut.contains("refus") || statut.contains("rejet") || statut.contains("annul"))
                    && !notifiedKeys.contains(keyRefus)) {

                notifiedKeys.add(keyRefus);

                Session session = sessionSvc.recupererParId(r.getIdSessionId());
                String nomSession = session != null ? nomSession(session) : "Session #" + r.getIdSessionId();

                nouvelles.add(new NotifItem(
                        "❌",
                        "Réservation refusée",
                        "Ta demande pour « " + nomSession + " » n'a pas été acceptée",
                        "#EF4444",
                        LocalDateTime.now(),
                        session
                ));
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  ÉTUDIANT — Rappels sessions à venir
    // ═════════════════════════════════════════════════════════════

    private void verifierRappelsSessionsEtudiant(List<NotifItem> nouvelles)
            throws SQLException {
        // Récupérer les sessions pour lesquelles l'étudiant a une réservation acceptée
        List<Session> sessions = chargerSessionsEtudiant();
        LocalDateTime now = LocalDateTime.now();

        for (Session s : sessions) {
            if (s.getDateHeure() == null) continue;
            if ("annulée".equals(s.getStatut())) continue;
            ajouterRappelsTemporels(s, now, nouvelles);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  RAPPELS TEMPORELS COMMUNS (prof + étudiant)
    // ═════════════════════════════════════════════════════════════

    private void ajouterRappelsTemporels(Session s, LocalDateTime now,
                                         List<NotifItem> nouvelles) {
        long min = ChronoUnit.MINUTES.between(now, s.getDateHeure());
        String nom   = nomSession(s);
        String heure = s.getDateHeure().format(FMT_DATE);

        // Dans les 60 jours
        String kSoon = "soon_" + s.getId();
        if (min > 0 && min <= FENETRE_MINUTES && !notifiedKeys.contains(kSoon)) {
            notifiedKeys.add(kSoon);
            String msg, icon, couleur;
            if (min <= 60) {
                icon = "⚡"; couleur = "#EF4444";
                msg = "Session dans " + min + " min  ·  " + heure;
            } else if (min <= 1440) {
                long h = min / 60;
                icon = "🔔"; couleur = "#F59E0B";
                msg = "Session dans " + h + "h"
                        + (min % 60 > 0 ? " " + min % 60 + "min" : "")
                        + "  ·  " + heure;
            } else {
                long jours = min / 1440;
                icon = "📅"; couleur = "#3B82F6";
                msg = "Session dans " + jours + " jour"
                        + (jours > 1 ? "s" : "") + "  ·  " + heure;
            }
            nouvelles.add(new NotifItem(icon, "À venir : " + nom, msg, couleur,
                    LocalDateTime.now(), s));
        }

        // 1h avant
        String k1h = "1h_" + s.getId();
        if (min >= 55 && min <= 65 && !notifiedKeys.contains(k1h)) {
            notifiedKeys.add(k1h);
            nouvelles.add(new NotifItem("🔔", "Dans 1 heure : " + nom,
                    "⏰ " + heure + "  ·  Préparez-vous !",
                    "#F59E0B", LocalDateTime.now(), s));
        }

        // 15 min avant
        String k15 = "15m_" + s.getId();
        if (min >= 12 && min <= 17 && !notifiedKeys.contains(k15)) {
            notifiedKeys.add(k15);
            nouvelles.add(new NotifItem("🚨", "Dans 15 minutes : " + nom,
                    "🔴 " + heure + "  ·  Rejoignez maintenant !",
                    "#EF4444", LocalDateTime.now(), s));
        }

        // En live
        String kNow = "live_" + s.getId();
        if (min >= -3 && min <= 3 && !notifiedKeys.contains(kNow)) {
            notifiedKeys.add(kNow);
            nouvelles.add(new NotifItem("🎯", "En cours maintenant : " + nom,
                    "🟢 LIVE  ·  " + heure,
                    "#10B981", LocalDateTime.now(), s));
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  CHARGEMENT SESSIONS ÉTUDIANT
    // ═════════════════════════════════════════════════════════════

    private List<Session> chargerSessionsEtudiant() throws SQLException {
        List<Session> result = new ArrayList<>();
        List<Reservation> resas = reservationSvc.recupererParEtudiant(userId);
        Set<Integer> ids = new HashSet<>();

        for (Reservation r : resas) {
            if (r.getStatut() == null) continue;
            String st = normaliserStatut(r.getStatut());
            if (st.contains("accept") || st.contains("attente"))
                ids.add(r.getIdSessionId());
        }

        if (!ids.isEmpty()) {
            for (Session s : sessionSvc.recuperer())
                if (ids.contains(s.getId())) result.add(s);
        }

        System.out.println("  → " + result.size() + " sessions pour userId=" + userId);
        return result;
    }

    // ═════════════════════════════════════════════════════════════
    //  HELPER — normaliser le statut (accents, casse)
    // ═════════════════════════════════════════════════════════════

    private String normaliserStatut(String statut) {
        return statut.trim().toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("ï", "i").replace("û", "u").replace("ô", "o");
    }

    // ═════════════════════════════════════════════════════════════
    //  AJOUT MANUEL (depuis controllers)
    // ═════════════════════════════════════════════════════════════

    public void ajouterNotification(String icon, String titre, String message,
                                    String couleur, Session session, boolean afficherToast) {
        Platform.runLater(() -> {
            NotifItem item = new NotifItem(icon, titre, message, couleur,
                    LocalDateTime.now(), session);
            notifications.add(0, item);
            unreadCount++;
            majBadge();
            animerCloche();
            if (afficherToast) afficherToast(item);
            if (panneauOuvert) rebuildPanneau();
        });
    }

    // ═════════════════════════════════════════════════════════════
    //  BADGE
    // ═════════════════════════════════════════════════════════════

    private void majBadge() {
        if (unreadCount <= 0) {
            badgeLabel.setVisible(false);
            badgeLabel.setManaged(false);
        } else {
            badgeLabel.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            badgeLabel.setVisible(true);
            badgeLabel.setManaged(true);
            ScaleTransition pop = new ScaleTransition(Duration.millis(180), badgeLabel);
            pop.setFromX(0.4); pop.setToX(1.25);
            pop.setFromY(0.4); pop.setToY(1.25);
            ScaleTransition back = new ScaleTransition(Duration.millis(100), badgeLabel);
            back.setFromX(1.25); back.setToX(1.0);
            back.setFromY(1.25); back.setToY(1.0);
            back.setInterpolator(Interpolator.EASE_OUT);
            new SequentialTransition(pop, back).play();
        }
    }

    private void animerCloche() {
        RotateTransition rt = new RotateTransition(Duration.millis(70), bellIcon);
        rt.setByAngle(18); rt.setCycleCount(6); rt.setAutoReverse(true);
        rt.play();
    }

    // ═════════════════════════════════════════════════════════════
    //  PANNEAU
    // ═════════════════════════════════════════════════════════════

    private void togglePanneau() {
        if (panneauOuvert) fermerPanneau();
        else ouvrirPanneau();
    }

    private void ouvrirPanneau() {
        System.out.println("📂 Ouvrir panneau, notifications: " + notifications.size());
        panneauOuvert = true;
        unreadCount   = 0;
        majBadge();

        if (panneauPopup != null && panneauPopup.isShowing()) {
            panneauPopup.hide();
            panneauPopup = null;
        }

        // Délai pour laisser Platform.runLater() finir
        javafx.animation.PauseTransition delay =
                new javafx.animation.PauseTransition(Duration.millis(150));
        delay.setOnFinished(ev -> {

            VBox p = buildPanneau();

            javafx.scene.Scene sc = new javafx.scene.Scene(p);
            sc.setFill(null); // transparent

            javafx.stage.Stage popStage = new javafx.stage.Stage();
            popStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popStage.initOwner(rootOverlay.getScene().getWindow());
            popStage.setScene(sc);
            popStage.setAlwaysOnTop(true);

            // Position sous la cloche
            javafx.geometry.Point2D pt = bellRoot.localToScreen(0, 0);
            double x = Math.max(8, pt.getX() - 360 + 40);
            double y = pt.getY() + 50;
            popStage.setX(x);
            popStage.setY(y);

            // Fermer si on clique ailleurs
            popStage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    popStage.close();
                    panneauOuvert = false;
                    panneau       = null;
                    panneauPopup  = null;
                }
            });

            popStage.setOnHidden(e -> {
                panneauOuvert = false;
                panneau       = null;
                panneauPopup  = null;
            });

            // Garder référence dans panneauPopup via un wrapper
            panneauPopup = new Popup();
            panneauPopup.setOnHidden(e -> popStage.close());

            panneau = p;
            popStage.show();

            // Animation d'ouverture
            p.setOpacity(0);
            FadeTransition fd = new FadeTransition(Duration.millis(200), p);
            fd.setFromValue(0); fd.setToValue(1);
            fd.play();
        });
        delay.play();
    }
    private void fermerPanneau() {
        panneauOuvert = false;
        panneau = null;
        if (panneauPopup != null) {
            panneauPopup.hide();
            panneauPopup = null;
        }
    }

    private void rebuildPanneau() {
        // Le panneau se rebuildera automatiquement à la prochaine ouverture
    }

    // ═════════════════════════════════════════════════════════════
    //  BUILD PANNEAU
    // ═════════════════════════════════════════════════════════════

    private VBox buildPanneau() {
        VBox root = new VBox(0);
        root.setPrefWidth(400);
        root.setMaxWidth(400);
        root.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#7C3AED;" +
                        "-fx-border-radius:16;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.25),32,0,0,12);"
        );

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 12, 18));

        Label titre = new Label("Notifications");
        titre.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#111827;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        if (!notifications.isEmpty()) {
            Label countLbl = new Label(String.valueOf(notifications.size()));
            countLbl.setStyle(
                    "-fx-background-color:#7C3AED;-fx-text-fill:white;" +
                            "-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:2 8 2 8;"
            );
            Button btnAll = new Button("Tout effacer");
            btnAll.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#7C3AED;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;" +
                            "-fx-padding:4 8 4 8;-fx-background-radius:8;"
            );
            btnAll.setOnAction(e -> {
                notifications.clear();
                notifiedKeys.clear();
                resasEnAttenteNotifiees.clear();
                resasAccepteesNotifiees.clear();
                unreadCount = 0;
                majBadge();
                rebuildPanneau();
            });
            header.getChildren().addAll(titre, countLbl, sp, btnAll);
        } else {
            header.getChildren().addAll(titre, sp);
        }

        root.getChildren().add(header);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#EDE9FE;");
        root.getChildren().add(sep);

        // ── Liste ────────────────────────────────────────────────
        VBox liste = new VBox(0);
        ScrollPane scroll = new ScrollPane(liste);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background-color:transparent;-fx-background:#FFFFFF;" +
                        "-fx-border-color:transparent;"
        );
        scroll.setMaxHeight(460);
        scroll.setPrefHeight(notifications.isEmpty() ? 200
                : Math.min(notifications.size() * 95 + 20, 460));

        if (notifications.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40, 20, 40, 20));

            Label ic  = new Label("🔔");
            ic.setStyle("-fx-font-size:44px;");

            Label msg = new Label("Aucune notification pour le moment");
            msg.setStyle(
                    "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#374151;" +
                            "-fx-wrap-text:true;-fx-text-alignment:center;"
            );
            msg.setWrapText(true);
            msg.setMaxWidth(320);

            String descNotifs = isProfesseur
                    ? "Vous serez alerté automatiquement :\n" +
                    "• Quand un étudiant fait une demande de réservation\n" +
                    "• 1h avant une session\n" +
                    "• 15 min avant une session\n" +
                    "• Quand une session commence"
                    : "Vous serez alerté automatiquement :\n" +
                    "• Quand votre réservation est acceptée\n" +
                    "• Quand votre réservation est refusée\n" +
                    "• Dans les 60 jours avant une session\n" +
                    "• 1h avant une session\n" +
                    "• 15 min avant une session\n" +
                    "• Quand une session commence";

            Label sub = new Label(descNotifs);
            sub.setStyle(
                    "-fx-font-size:12px;-fx-text-fill:#6B7280;" +
                            "-fx-wrap-text:true;-fx-text-alignment:left;"
            );
            sub.setWrapText(true);
            sub.setMaxWidth(320);

            empty.getChildren().addAll(ic, msg, sub);
            liste.getChildren().add(empty);

        } else {
            LocalDateTime now = LocalDateTime.now();
            List<NotifItem> nouv  = new ArrayList<>();
            List<NotifItem> today = new ArrayList<>();
            List<NotifItem> older = new ArrayList<>();

            for (NotifItem item : notifications) {
                long minAgo = ChronoUnit.MINUTES.between(item.dateHeure, now);
                if      (minAgo < 60)   nouv.add(item);
                else if (minAgo < 1440) today.add(item);
                else                    older.add(item);
            }

            if (!nouv.isEmpty()) {
                liste.getChildren().add(buildSectionLabel("🆕  Nouvelles"));
                for (NotifItem item : nouv)  liste.getChildren().add(buildNotifRow(item));
            }
            if (!today.isEmpty()) {
                liste.getChildren().add(buildSectionLabel("📅  Aujourd'hui"));
                for (NotifItem item : today) liste.getChildren().add(buildNotifRow(item));
            }
            if (!older.isEmpty()) {
                liste.getChildren().add(buildSectionLabel("🗂  Précédentes"));
                for (NotifItem item : older) liste.getChildren().add(buildNotifRow(item));
            }
        }

        root.getChildren().add(scroll);

        // ── Footer ───────────────────────────────────────────────
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color:#EDE9FE;");
        Button btnAct = new Button(notifications.isEmpty()
                ? "🔄 Vérifier maintenant" : "🔄 Actualiser");
        btnAct.setMaxWidth(Double.MAX_VALUE);
        btnAct.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#7C3AED;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;" +
                        "-fx-padding:12 0 12 0;-fx-background-radius:0 0 16 16;"
        );
        btnAct.setOnMouseEntered(e -> btnAct.setStyle(
                "-fx-background-color:#F5F3FF;-fx-text-fill:#7C3AED;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;" +
                        "-fx-padding:12 0 12 0;-fx-background-radius:0 0 16 16;"));
        btnAct.setOnMouseExited(e -> btnAct.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#7C3AED;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;" +
                        "-fx-padding:12 0 12 0;-fx-background-radius:0 0 16 16;"));
        btnAct.setOnAction(e -> {
            // Forcer une nouvelle vérification complète
            notifiedKeys.clear();
            resasEnAttenteNotifiees.clear();
            resasAccepteesNotifiees.clear();
            new Thread(this::verifierSessionsAvecToast).start();
        });
        root.getChildren().addAll(sep2, btnAct);

        panneau = root;
        return root;
    }

    // ═════════════════════════════════════════════════════════════
    //  HELPERS PANNEAU
    // ═════════════════════════════════════════════════════════════

    private Label buildSectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-text-fill:#6B7280;-fx-padding:12 18 4 18;"
        );
        return l;
    }

    private HBox buildNotifRow(NotifItem item) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 18, 10, 18));
        row.setStyle("-fx-background-color:transparent;-fx-cursor:hand;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(46, 46); iconBox.setMinSize(46, 46);
        Circle bg = new Circle(23);
        bg.setFill(Color.web(item.couleur + "22"));
        bg.setStroke(Color.web(item.couleur)); bg.setStrokeWidth(1.5);
        Label iconL = new Label(item.icon);
        iconL.setStyle("-fx-font-size:20px;");
        iconBox.getChildren().addAll(bg, iconL);

        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titreL = new Label(item.titre);
        titreL.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-text-fill:#111827;-fx-wrap-text:true;"
        );
        titreL.setWrapText(true);

        Label msgL = new Label(item.message);
        msgL.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        msgL.setWrapText(true);

        long minAgo = ChronoUnit.MINUTES.between(item.dateHeure, LocalDateTime.now());
        String temps = minAgo < 1    ? "À l'instant"
                : minAgo < 60  ? "Il y a " + minAgo + " min"
                : minAgo < 1440 ? "Il y a " + (minAgo / 60) + "h"
                :                "Il y a " + (minAgo / 1440) + "j";
        Label dateL = new Label(temps);
        dateL.setStyle(
                "-fx-font-size:11px;-fx-text-fill:" + item.couleur + ";-fx-font-weight:bold;"
        );
        content.getChildren().addAll(titreL, msgL, dateL);

        // Bouton Rejoindre si lien disponible
        if (item.session != null
                && item.session.getLienReunion() != null
                && !item.session.getLienReunion().isBlank()) {
            Button btnJoin = new Button("🎥 Rejoindre");
            btnJoin.setStyle(
                    "-fx-background-color:" + item.couleur + ";-fx-text-fill:white;" +
                            "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-radius:8;-fx-padding:3 10 3 10;-fx-cursor:hand;"
            );
            btnJoin.setOnAction(e -> {
                JitsiUtil.ouvrirDansAppDesktop(item.session.getLienReunion());
                fermerPanneau();
            });
            content.getChildren().add(btnJoin);
        }

        Circle dot = new Circle(5);
        dot.setFill(Color.web("#7C3AED"));

        row.getChildren().addAll(iconBox, content, dot);
        row.setOnMouseEntered(e ->
                row.setStyle("-fx-background-color:#F5F3FF;-fx-cursor:hand;"));
        row.setOnMouseExited(e ->
                row.setStyle("-fx-background-color:transparent;-fx-cursor:hand;"));
        return row;
    }

    // ═════════════════════════════════════════════════════════════
    //  TOAST
    // ═════════════════════════════════════════════════════════════

    private void afficherToast(NotifItem item) {
        VBox toastBox = new VBox(0);

        HBox toast = new HBox(12);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(14, 16, 14, 14));
        toast.setMaxWidth(370);
        toast.setStyle(
                "-fx-background-color:#1C1C1E;" +
                        "-fx-background-radius:14 14 0 0;" +
                        "-fx-border-color:" + item.couleur + ";" +
                        "-fx-border-radius:14 14 0 0;-fx-border-width:0 0 0 4;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.40),24,0,0,8);"
        );

        Label iconL = new Label(item.icon);
        iconL.setStyle("-fx-font-size:26px;");
        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), iconL);
        pulse.setFromX(1.0); pulse.setToX(1.2);
        pulse.setFromY(1.0); pulse.setToY(1.2);
        pulse.setAutoReverse(true); pulse.setCycleCount(4); pulse.play();

        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);
        Label appLbl = new Label("Fluently · Notification");
        appLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#8A8D91;-fx-font-weight:bold;");
        Label titreL = new Label(item.titre);
        titreL.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" +
                        item.couleur + ";-fx-wrap-text:true;"
        );
        titreL.setWrapText(true);
        Label msgL = new Label(item.message);
        msgL.setStyle("-fx-font-size:11px;-fx-text-fill:#E5E7EB;-fx-wrap-text:true;");
        msgL.setWrapText(true);
        content.getChildren().addAll(appLbl, titreL, msgL);

        if (item.session != null
                && item.session.getLienReunion() != null
                && !item.session.getLienReunion().isBlank()) {
            Button btnJoin = new Button("🎥 Rejoindre");
            btnJoin.setStyle(
                    "-fx-background-color:" + item.couleur + ";-fx-text-fill:white;" +
                            "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-radius:8;-fx-padding:4 10 4 10;-fx-cursor:hand;"
            );
            btnJoin.setOnAction(e ->
                    JitsiUtil.ouvrirDansAppDesktop(item.session.getLienReunion()));
            content.getChildren().add(btnJoin);
        }

        Label closeBtn = new Label("✕");
        closeBtn.setStyle(
                "-fx-font-size:13px;-fx-text-fill:#6B7280;-fx-cursor:hand;-fx-padding:0 0 0 4;"
        );
        closeBtn.setOnMouseClicked(e -> fermerToast(toastBox));
        toast.getChildren().addAll(iconL, content, closeBtn);

        Rectangle progBg  = new Rectangle(370, 4);
        progBg.setFill(Color.web("#2D2D30"));
        progBg.setArcWidth(4); progBg.setArcHeight(4);
        Rectangle progBar = new Rectangle(0, 4);
        progBar.setFill(Color.web(item.couleur));
        progBar.setArcWidth(4); progBar.setArcHeight(4);
        StackPane prog = new StackPane(progBg, progBar);
        prog.setAlignment(Pos.CENTER_LEFT);

        toastBox.getChildren().addAll(toast, prog);
        toastBox.setMaxWidth(370);
        rootOverlay.getChildren().add(toastBox);

        Platform.runLater(() -> {
            double rw = rootOverlay.getWidth();
            double rh = rootOverlay.getHeight();
            if (rw < 100 && rootOverlay.getScene() != null) {
                rw = rootOverlay.getScene().getWidth();
                rh = rootOverlay.getScene().getHeight();
            }
            if (rw < 100) { rw = 1200; rh = 700; }
            double h = toastBox.prefHeight(370);
            toastBox.setLayoutX(Math.max(8, rw - 386));
            toastBox.setLayoutY(Math.max(8, rh - 20 - h - calculerOffsetToasts(toastBox)));
        });

        toastBox.setOpacity(0);
        toastBox.setTranslateX(80);
        FadeTransition fd = new FadeTransition(Duration.millis(300), toastBox);
        fd.setFromValue(0); fd.setToValue(1);
        TranslateTransition td = new TranslateTransition(Duration.millis(300), toastBox);
        td.setFromX(80); td.setToX(0); td.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fd, td).play();

        Timeline prog7s = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progBar.widthProperty(), 0)),
                new KeyFrame(Duration.seconds(7),
                        new KeyValue(progBar.widthProperty(), 370, Interpolator.LINEAR))
        );
        prog7s.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(7));
        pause.setOnFinished(e -> fermerToast(toastBox));
        pause.play();
    }

    private double calculerOffsetToasts(VBox current) {
        double offset = 0;
        for (Node n : rootOverlay.getChildren())
            if (n instanceof VBox && n != current && n.isVisible()) {
                double h = n.getBoundsInLocal().getHeight();
                if (h > 40) offset += h + 10;
            }
        return offset;
    }

    private void fermerToast(VBox toast) {
        FadeTransition fd = new FadeTransition(Duration.millis(220), toast);
        fd.setFromValue(1); fd.setToValue(0);
        TranslateTransition td = new TranslateTransition(Duration.millis(220), toast);
        td.setFromX(0); td.setToX(80);
        fd.setOnFinished(e -> rootOverlay.getChildren().remove(toast));
        new ParallelTransition(fd, td).play();
    }

    // ═════════════════════════════════════════════════════════════
    //  ARRÊT
    // ═════════════════════════════════════════════════════════════

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown())
            scheduler.shutdownNow();
    }

    private String nomSession(Session s) {
        return (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();
    }

    // ═════════════════════════════════════════════════════════════
    //  INNER CLASS
    // ═════════════════════════════════════════════════════════════

    private static class NotifItem {
        final String icon, titre, message, couleur;
        final LocalDateTime dateHeure;
        final Session session;

        NotifItem(String icon, String titre, String message, String couleur,
                  LocalDateTime dateHeure, Session session) {
            this.icon      = icon;      this.titre   = titre;
            this.message   = message;   this.couleur = couleur;
            this.dateHeure = dateHeure; this.session = session;
        }
    }
}