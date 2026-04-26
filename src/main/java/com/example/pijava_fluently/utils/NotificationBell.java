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
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 *  NotificationBell — Style Facebook / Instagram / Teams
 *  ▸ Icône cloche avec badge rouge animé
 *  ▸ Panneau déroulant élégant (liste des notifs)
 *  ▸ Toast slide-in à droite avec barre de progression
 *  ▸ Vérification au démarrage (immédiate) + toutes les 60s
 *  ▸ Rappels : sessions prochaines 24h, 1h, 15min, live
 * ╚══════════════════════════════════════════════════════════╝
 */
public class NotificationBell {

    // ── Services ──────────────────────────────────────────────────
    private final Sessionservice     sessionSvc     = new Sessionservice();
    private final Reservationservice reservationSvc = new Reservationservice();

    // ── Config ────────────────────────────────────────────────────
    private final int     userId;
    private final boolean isProfesseur;
    private final Pane    rootOverlay;

    // ── État ──────────────────────────────────────────────────────
    private final List<NotifItem>  notifications = new ArrayList<>();
    private final Set<String>      notifiedKeys  = new HashSet<>();
    private int                    unreadCount   = 0;
    private VBox                   panneau       = null;
    private boolean                panneauOuvert = false;

    // ── Widgets UI ────────────────────────────────────────────────
    private final StackPane bellRoot;
    private final Label     badgeLabel;
    private final Label     bellIcon;

    // ── Scheduler ────────────────────────────────────────────────
    private ScheduledExecutorService scheduler;

    private static final DateTimeFormatter FMT_HEURE = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATE  = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // ═════════════════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ═════════════════════════════════════════════════════════════

    public NotificationBell(int userId, boolean isProfesseur, Pane rootOverlay) {
        this.userId       = userId;
        this.isProfesseur = isProfesseur;
        this.rootOverlay  = rootOverlay;

        // ── Cloche ────────────────────────────────────────────────
        bellIcon = new Label();
        bellIcon.setGraphic(createBellShape());
        bellIcon.setStyle(
                "-fx-cursor:hand;" +
                        "-fx-padding:10;" +
                        "-fx-background-color:rgba(0,0,0,0.25);" +
                        "-fx-background-radius:50;"
        );

        // ── Badge rouge ────────────────────────────────────────────
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

        // ── Interactions ──────────────────────────────────────────
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
        bellRoot.setOnMouseClicked(e -> togglePanneau());

        // ── Démarrage ────────────────────────────────────────────
        demarrerScheduler();
    }

    // ── Getter ────────────────────────────────────────────────────
    public StackPane getBellRoot() { return bellRoot; }
    private Node createBellShape() {
        SVGPath bellShape = new SVGPath();
        bellShape.setContent("M12 22c1.1 0 2-.9 2-2h-4a2 2 0 002 2zm6-6V11c0-3.07-1.63-5.64-4.5-6.32V4a1.5 1.5 0 00-3 0v.68C7.63 5.36 6 7.92 6 11v5l-2 2h16l-2-2z");
        bellShape.setFill(Color.WHITE);
        bellShape.setScaleX(1.2);
        bellShape.setScaleY(1.2);
        bellShape.setTranslateX(4);
        bellShape.setTranslateY(4);
        return bellShape;
    }
    // ═════════════════════════════════════════════════════════════
    //  SCHEDULER
    // ═════════════════════════════════════════════════════════════

    private void demarrerScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NotifBell");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::verifierSessions, 3, 60, TimeUnit.SECONDS);
    }

    // ═════════════════════════════════════════════════════════════
    //  VÉRIFICATION SESSIONS
    // ═════════════════════════════════════════════════════════════

    private void verifierSessions() {
        if (userId == -1) return;
        try {
            List<Session> sessions = chargerSessions();
            LocalDateTime now = LocalDateTime.now();

            for (Session s : sessions) {
                if (s.getDateHeure() == null) continue;
                if ("annulée".equals(s.getStatut())) continue;

                long min = ChronoUnit.MINUTES.between(now, s.getDateHeure());
                String nom   = nomSession(s);
                String heure = s.getDateHeure().format(FMT_DATE);

                // Dans les 24h prochaines
                String kSoon = "soon_" + s.getId();
                if (min > 0 && min <= 1440 && !notifiedKeys.contains(kSoon)) {
                    notifiedKeys.add(kSoon);
                    String msg, icon, couleur;
                    if (min <= 60) {
                        icon    = "⚡";
                        couleur = "#EF4444";
                        msg     = "Session dans " + min + " min  ·  " + heure;
                    } else {
                        long h = min / 60;
                        icon    = "📅";
                        couleur = "#3B82F6";
                        msg     = "Session dans " + h + "h" + (min % 60 > 0 ? " " + min % 60 + "min" : "") + "  ·  " + heure;
                    }
                    ajouterNotification(icon, "À venir : " + nom, msg, couleur, s, true);
                }

                // 1h avant
                String k1h = "1h_" + s.getId();
                if (min >= 55 && min <= 65 && !notifiedKeys.contains(k1h)) {
                    notifiedKeys.add(k1h);
                    ajouterNotification("🔔", "Dans 1 heure : " + nom,
                            "⏰ " + heure + "  ·  Préparez-vous !", "#F59E0B", s, true);
                }

                // 15 min avant
                String k15 = "15m_" + s.getId();
                if (min >= 12 && min <= 17 && !notifiedKeys.contains(k15)) {
                    notifiedKeys.add(k15);
                    ajouterNotification("🚨", "Dans 15 minutes : " + nom,
                            "🔴 " + heure + "  ·  Rejoignez maintenant !", "#EF4444", s, true);
                }

                // Live maintenant
                String kNow = "live_" + s.getId();
                if (min >= -3 && min <= 3 && !notifiedKeys.contains(kNow)) {
                    notifiedKeys.add(kNow);
                    ajouterNotification("🎯", "En cours maintenant : " + nom,
                            "🟢 LIVE  ·  " + heure, "#10B981", s, true);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ NotifBell erreur : " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  CHARGEMENT SESSIONS
    // ═════════════════════════════════════════════════════════════

    private List<Session> chargerSessions() throws SQLException {
        if (isProfesseur) {
            return sessionSvc.recuperer();
        }

        List<Session> result = new ArrayList<>();
        List<Reservation> resas = reservationSvc.recupererParEtudiant(userId);

        Set<Integer> ids = new HashSet<>();
        for (Reservation r : resas) {
            if (r.getStatut() == null) continue;
            String st = r.getStatut().trim().toLowerCase()
                    .replace("é", "e").replace("è", "e").replace("ê", "e");
            if (st.contains("accept") || st.contains("attente")) {
                ids.add(r.getIdSessionId());
            }
        }

        if (!ids.isEmpty()) {
            for (Session s : sessionSvc.recuperer()) {
                if (ids.contains(s.getId())) result.add(s);
            }
        }

        return result;
    }

    // ═════════════════════════════════════════════════════════════
    //  AJOUT NOTIFICATION
    // ═════════════════════════════════════════════════════════════

    public void ajouterNotification(String icon, String titre, String message,
                                    String couleur, Session session,
                                    boolean afficherToast) {
        Platform.runLater(() -> {
            NotifItem item = new NotifItem(icon, titre, message, couleur,
                    LocalDateTime.now(), session);
            notifications.add(0, item);
            unreadCount++;
            majBadge();
            animerCloche();
            if (afficherToast) afficherToast(item);
            if (panneauOuvert && panneau != null) rebuildPanneau();
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

    // ═════════════════════════════════════════════════════════════
    //  ANIMATION CLOCHE
    // ═════════════════════════════════════════════════════════════

    private void animerCloche() {
        RotateTransition rt = new RotateTransition(Duration.millis(70), bellIcon);
        rt.setByAngle(18);
        rt.setCycleCount(6);
        rt.setAutoReverse(true);
        rt.play();
    }

    // ═════════════════════════════════════════════════════════════
    //  PANNEAU DÉROULANT
    // ═════════════════════════════════════════════════════════════

    private void togglePanneau() {
        if (panneauOuvert) fermerPanneau();
        else ouvrirPanneau();
    }

    private void ouvrirPanneau() {
        unreadCount = 0;
        majBadge();
        panneauOuvert = true;

        panneau = buildPanneau();

        // ✅ FIX : On remonte jusqu'au Pane racine de la scène
        // pour positionner le panneau en absolu par rapport à la fenêtre
        javafx.geometry.Point2D ptScene = bellRoot.localToScene(0, 0);

        // Chercher le vrai Pane racine (pas le ScrollPane ni le VBox)
        javafx.scene.Parent sceneRoot = rootOverlay.getScene().getRoot();

        Pane targetOverlay;
        if (sceneRoot instanceof Pane) {
            targetOverlay = (Pane) sceneRoot;
        } else {
            targetOverlay = rootOverlay; // fallback
        }

        javafx.geometry.Point2D ptLocal = targetOverlay.sceneToLocal(ptScene.getX(), ptScene.getY());

        double panneauWidth = 380;
        double px = ptLocal.getX() - panneauWidth + 40;
        // S'assurer que le panneau ne sort pas à gauche
        px = Math.max(8, px);
        // S'assurer que le panneau ne sort pas à droite
        double sceneWidth = rootOverlay.getScene().getWidth();
        if (px + panneauWidth > sceneWidth - 8) {
            px = sceneWidth - panneauWidth - 8;
        }
        double py = ptLocal.getY() + 48;

        panneau.setLayoutX(px);
        panneau.setLayoutY(py);
        panneau.setOpacity(0);
        panneau.setScaleY(0.88);
        panneau.setTranslateY(-8);

        // Ajouter au bon overlay (la racine de la scène)
        targetOverlay.getChildren().add(panneau);

        // Fermer si on clique en dehors
        targetOverlay.setOnMouseClicked(e -> {
            if (panneauOuvert && panneau != null) {
                javafx.geometry.Bounds bounds = panneau.getBoundsInParent();
                if (!bounds.contains(e.getX(), e.getY())) {
                    fermerPanneau();
                    targetOverlay.setOnMouseClicked(null);
                }
            }
        });

        FadeTransition fd = new FadeTransition(javafx.util.Duration.millis(180), panneau);
        fd.setFromValue(0); fd.setToValue(1);
        ScaleTransition sc = new ScaleTransition(javafx.util.Duration.millis(180), panneau);
        sc.setFromY(0.88); sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition td = new TranslateTransition(javafx.util.Duration.millis(180), panneau);
        td.setFromY(-8); td.setToY(0);
        td.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fd, sc, td).play();
    }

    // ═════════════════════════════════════════════════════════════
    //  REMPLACE aussi fermerPanneau() — pour retirer du bon parent
    // ═════════════════════════════════════════════════════════════

    private void fermerPanneau() {
        panneauOuvert = false;
        if (panneau == null) return;
        VBox p = panneau;
        panneau = null;

        FadeTransition fd = new FadeTransition(javafx.util.Duration.millis(140), p);
        fd.setFromValue(1); fd.setToValue(0);
        ScaleTransition sc = new ScaleTransition(javafx.util.Duration.millis(140), p);
        sc.setFromY(1.0); sc.setToY(0.9);
        fd.setOnFinished(e -> {
            if (p.getParent() instanceof Pane parent) {
                parent.getChildren().remove(p);
                parent.setOnMouseClicked(null);
            }
        });
        new ParallelTransition(fd, sc).play();
    }

    private void rebuildPanneau() {
        if (panneau == null) return;
        double lx = panneau.getLayoutX();
        double ly = panneau.getLayoutY();
        rootOverlay.getChildren().remove(panneau);
        panneau = buildPanneau();
        panneau.setLayoutX(lx);
        panneau.setLayoutY(ly);
        rootOverlay.getChildren().add(panneau);
    }

    private VBox buildPanneau() {
        VBox root = new VBox(0);
        root.setPrefWidth(380);
        root.setMaxWidth(380);
        root.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#7C3AED;" +
                        "-fx-border-radius:16;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.25),32,0,0,12);"
        );

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 12, 18));
        header.setStyle("-fx-background-radius:16 16 0 0;");
        Label titre = new Label("Notifications");
        titre.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#050505;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        if (!notifications.isEmpty()) {
            Button btnAll = new Button("Tout effacer");
            btnAll.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#7C3AED;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;" +
                            "-fx-padding:4 8 4 8;-fx-background-radius:8;"
            );
            btnAll.setOnAction(e -> {
                notifications.clear();
                unreadCount = 0;
                majBadge();
                rebuildPanneau();
            });
            header.getChildren().addAll(titre, sp, btnAll);
        } else {
            header.getChildren().addAll(titre, sp);
        }

        HBox tabs = new HBox(4);
        tabs.setPadding(new Insets(0, 18, 8, 18));
        tabs.getChildren().addAll(buildTab("Toutes", true), buildTab("Non lues", false));

        root.getChildren().addAll(header, tabs);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#7C3AED;");
        root.getChildren().add(sep);

        VBox liste = new VBox(0);
        ScrollPane scroll = new ScrollPane(liste);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:#FFFFFF;-fx-border-color:transparent;");
        scroll.setMaxHeight(420);
        scroll.setPrefHeight(Math.min(notifications.size() * 82 + 20, 420));

        if (notifications.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label ic  = new Label("!");
            ic.setStyle("-fx-font-size:40px;-fx-font-weight:bold;-fx-text-fill:#7C3AED;");
            Label msg = new Label("Pas de notifications");
            msg.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#65676B;");
            Label sub = new Label("Les rappels de vos sessions apparaîtront ici");
            sub.setStyle("-fx-font-size:12px;-fx-text-fill:#8A8D91;-fx-wrap-text:true;");
            sub.setWrapText(true);
            sub.setMaxWidth(260);
            empty.getChildren().addAll(ic, msg, sub);
            liste.getChildren().add(empty);
        } else {
            LocalDateTime now     = LocalDateTime.now();
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
                liste.getChildren().add(buildSectionLabel("Nouvelles"));
                nouv.forEach(item -> liste.getChildren().add(buildNotifRow(item)));
            }
            if (!today.isEmpty()) {
                liste.getChildren().add(buildSectionLabel("Aujourd'hui"));
                today.forEach(item -> liste.getChildren().add(buildNotifRow(item)));
            }
            if (!older.isEmpty()) {
                liste.getChildren().add(buildSectionLabel("Précédentes"));
                older.forEach(item -> liste.getChildren().add(buildNotifRow(item)));
            }
        }

        root.getChildren().add(scroll);

        if (!notifications.isEmpty()) {
            Separator sep2 = new Separator();
            sep2.setStyle("-fx-background-color:#7C3AED;");
            Button btnVoir = new Button("Voir toutes les notifications");
            btnVoir.setMaxWidth(Double.MAX_VALUE);
            btnVoir.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#7C3AED;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;" +
                            "-fx-padding:12 0 12 0;-fx-background-radius:0 0 16 16;"
            );
            btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(
                    "-fx-background-color:#F0F2F5;-fx-text-fill:#7C3AED;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;" +
                            "-fx-padding:12 0 12 0;-fx-background-radius:0 0 16 16;"));
            btnVoir.setOnMouseExited(e -> btnVoir.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#7C3AED;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;" +
                            "-fx-padding:12 0 12 0;-fx-background-radius:0 0 16 16;"));
            root.getChildren().addAll(sep2, btnVoir);
        }

        panneau = root;
        return root;
    }

    private Label buildSectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#050505;-fx-padding:10 18 4 18;");
        return l;
    }

    private Button buildTab(String text, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:" + (active ? "#F0E6FF" : "transparent") + ";" +
                        "-fx-text-fill:" + (active ? "#7C3AED" : "#65676B") + ";" +
                        "-fx-font-size:13px;-fx-font-weight:" + (active ? "bold" : "normal") + ";" +
                        "-fx-background-radius:20;-fx-padding:6 14 6 14;-fx-cursor:hand;"
        );
        return btn;
    }

    private HBox buildNotifRow(NotifItem item) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 18, 8, 18));
        row.setStyle("-fx-background-color:transparent;-fx-cursor:hand;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(46, 46);
        iconBox.setMinSize(46, 46);
        Circle bg = new Circle(23);
        bg.setFill(Color.web(item.couleur + "22"));
        bg.setStroke(Color.web(item.couleur));
        bg.setStrokeWidth(1.5);
        Label iconL = new Label(item.icon);
        iconL.setStyle("-fx-font-size:20px;");
        iconBox.getChildren().addAll(bg, iconL);

        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);
        Label titreL = new Label(item.titre);
        titreL.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#050505;-fx-wrap-text:true;");
        titreL.setWrapText(true);
        Label msgL = new Label(item.message);
        msgL.setStyle("-fx-font-size:12px;-fx-text-fill:#65676B;-fx-wrap-text:true;");
        msgL.setWrapText(true);

        long minAgo = ChronoUnit.MINUTES.between(item.dateHeure, LocalDateTime.now());
        String tempsRelatif = minAgo < 1    ? "À l'instant"
                : minAgo < 60   ? "Il y a " + minAgo + " min"
                : minAgo < 1440 ? "Il y a " + (minAgo / 60) + "h"
                :                 "Il y a " + (minAgo / 1440) + "j";
        Label dateL = new Label(tempsRelatif);
        dateL.setStyle("-fx-font-size:11px;-fx-text-fill:" + item.couleur + ";-fx-font-weight:bold;");
        content.getChildren().addAll(titreL, msgL, dateL);

        Circle dot = new Circle(5);
        dot.setFill(Color.web("#1877F2"));

        row.getChildren().addAll(iconBox, content, dot);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#F0F2F5;-fx-cursor:hand;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:transparent;-fx-cursor:hand;"));
        return row;
    }

    // ═════════════════════════════════════════════════════════════
    //  TOAST
    // ═════════════════════════════════════════════════════════════

    private void afficherToast(NotifItem item) {
        VBox toastBox = new VBox(0);
        toastBox.setMouseTransparent(false);

        HBox toast = new HBox(12);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(14, 16, 14, 14));
        toast.setMaxWidth(360);
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
        pulse.setAutoReverse(true);
        pulse.setCycleCount(4);
        pulse.play();

        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label appLabel = new Label("Fluently · Session");
        appLabel.setStyle("-fx-font-size:10px;-fx-text-fill:#8A8D91;-fx-font-weight:bold;");
        Label titreL = new Label(item.titre);
        titreL.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" +
                item.couleur + ";-fx-wrap-text:true;");
        titreL.setWrapText(true);
        Label msgL = new Label(item.message);
        msgL.setStyle("-fx-font-size:11px;-fx-text-fill:#E5E7EB;-fx-wrap-text:true;");
        msgL.setWrapText(true);
        content.getChildren().addAll(appLabel, titreL, msgL);

        if (item.session != null
                && item.session.getLienReunion() != null
                && !item.session.getLienReunion().isBlank()) {
            Button btnJoin = new Button("🎥 Rejoindre");
            btnJoin.setStyle(
                    "-fx-background-color:" + item.couleur + ";-fx-text-fill:white;" +
                            "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-radius:8;-fx-padding:4 10 4 10;-fx-cursor:hand;"
            );
            btnJoin.setOnAction(e -> JitsiUtil.ouvrirDansNavigateur(item.session.getLienReunion()));
            content.getChildren().add(btnJoin);
        }

        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-font-size:13px;-fx-text-fill:#6B7280;-fx-cursor:hand;-fx-padding:0 0 0 4;");
        closeBtn.setOnMouseClicked(e -> fermerToast(toastBox));

        toast.getChildren().addAll(iconL, content, closeBtn);

        // Barre de progression
        Rectangle progBg = new Rectangle(360, 4);
        progBg.setFill(Color.web("#2D2D30"));
        progBg.setArcWidth(4);
        progBg.setArcHeight(4);
        Rectangle progBar = new Rectangle(0, 4);
        progBar.setFill(Color.web(item.couleur));
        progBar.setArcWidth(4);
        progBar.setArcHeight(4);
        StackPane prog = new StackPane(progBg, progBar);
        prog.setAlignment(Pos.CENTER_LEFT);
        prog.setStyle("-fx-background-radius:0 0 14 14;");

        toastBox.getChildren().addAll(toast, prog);
        toastBox.setMaxWidth(360);

        rootOverlay.getChildren().add(toastBox);

        Platform.runLater(() -> {
            double rw, rh;
            rw = rootOverlay.getWidth();
            rh = rootOverlay.getHeight();
            if (rw < 100 && rootOverlay.getScene() != null) {
                rw = rootOverlay.getScene().getWidth();
                rh = rootOverlay.getScene().getHeight();
            }
            if (rw < 100) { rw = 1200; rh = 700; }

            double toastH = toastBox.prefHeight(360);
            double x = rw - 376;
            double y = rh - 20 - toastH - calculerOffsetToasts(toastBox);
            toastBox.setLayoutX(Math.max(8, x));
            toastBox.setLayoutY(Math.max(8, y));
        });

        // Animation entrée
        toastBox.setOpacity(0);
        toastBox.setTranslateX(80);
        FadeTransition fd = new FadeTransition(Duration.millis(300), toastBox);
        fd.setFromValue(0); fd.setToValue(1);
        TranslateTransition td = new TranslateTransition(Duration.millis(300), toastBox);
        td.setFromX(80); td.setToX(0);
        td.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fd, td).play();

        // Barre progression 7s
        Timeline prog7s = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(progBar.widthProperty(), 0)),
                new KeyFrame(Duration.seconds(7), new KeyValue(progBar.widthProperty(), 360, Interpolator.LINEAR))
        );
        prog7s.play();

        // Auto-fermeture
        PauseTransition pause = new PauseTransition(Duration.seconds(7));
        pause.setOnFinished(e -> fermerToast(toastBox));
        pause.play();
    }

    private double calculerOffsetToasts(VBox current) {
        double offset = 0;
        for (Node n : rootOverlay.getChildren()) {
            if (n instanceof VBox && n != current && n.isVisible()) {
                double h = n.getBoundsInLocal().getHeight();
                if (h > 40) offset += h + 10;
            }
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

    // ═════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════

    private String nomSession(Session s) {
        return (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();
    }

    // ═════════════════════════════════════════════════════════════
    //  INNER CLASS
    // ═════════════════════════════════════════════════════════════

    private static class NotifItem {
        final String        icon;
        final String        titre;
        final String        message;
        final String        couleur;
        final LocalDateTime dateHeure;
        final Session       session;

        NotifItem(String icon, String titre, String message, String couleur,
                  LocalDateTime dateHeure, Session session) {
            this.icon      = icon;
            this.titre     = titre;
            this.message   = message;
            this.couleur   = couleur;
            this.dateHeure = dateHeure;
            this.session   = session;
        }
    }
}