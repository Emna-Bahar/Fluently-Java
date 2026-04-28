package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.entites.UserSession;
import com.example.pijava_fluently.entites.UserStats;
import com.example.pijava_fluently.services.UserSessionService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class StreakDashboardController {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Label       lblUserName;
    @FXML private Label       lblNiveauLabel;
    @FXML private Label       lblNiveauNum;
    @FXML private Label       lblTotalPoints;
    @FXML private Label       lblTauxCompletion;
    @FXML private Label       lblTachesTotal;
    @FXML private Label       lblDureeTotal;
    @FXML private Label       lblSessions;
    @FXML private Label       lblJoursActifs;

    // Streak
    @FXML private Label       lblStreakFire;
    @FXML private Label       lblStreakCount;
    @FXML private Label       lblStreakSub;
    @FXML private Label       lblStreakMax;
    @FXML private ProgressBar streakProgressBar;

    // Arc niveau
    @FXML private StackPane   arcContainer;
    @FXML private Label       lblArcPct;
    @FXML private Label       lblArcXp;

    // Graphe
    @FXML private HBox        hebdoChart;
    @FXML private Label       lblBestDay;

    // Badges
    @FXML private FlowPane    badgesContainer;
    @FXML private Label       lblBadgesCount;

    // Sessions
    @FXML private VBox        sessionsContainer;

    // ── State ─────────────────────────────────────────────────────
    private User             currentUser;
    private UserStats        stats;
    private final UserSessionService sessionService = UserSessionService.getInstance();

    // ── Couleurs (hex 6 chiffres uniquement — compatibles inline) ─
    private static final String FIRE_COLOR   = "#FF6B35";
    private static final String GOLD_COLOR   = "#FFD700";
    private static final String PURPLE_COLOR = "#6C63FF";
    private static final String GREEN_COLOR  = "#10B981";
    private static final String BLUE_COLOR   = "#3B82F6";
    private static final String DARK_BG      = "#1E293B";
    private static final String DARKER_BG    = "#0F172A";

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════════

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadAndRender();
    }

    public void refresh() {
        loadAndRender();
    }

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // rendu déclenché par setCurrentUser()
    }

    private void loadAndRender() {
        if (currentUser == null) return;
        new Thread(() -> {
            UserStats         s       = sessionService.getUserStats(currentUser.getId());
            List<UserSession> recent  = sessionService.getRecentSessions(currentUser.getId(), 5);
            Platform.runLater(() -> {
                this.stats = s;
                renderAll(s, recent);
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    //  RENDU PRINCIPAL
    // ══════════════════════════════════════════════════════════════

    private void renderAll(UserStats s, List<UserSession> sessions) {
        renderHeader(s);
        renderLevelArc(s);
        renderStreakHero(s);
        renderStats(s);
        renderHebdoChart(s);
        renderRecentSessions(sessions);
        renderBadges(s);
    }

    // ══════════════════════════════════════════════════════════════
    //  HEADER
    // ══════════════════════════════════════════════════════════════

    private void renderHeader(UserStats s) {
        if (lblUserName != null) {
            lblUserName.setText("Bonjour, " + currentUser.getPrenom() + " 👋");
            fadeIn(lblUserName, 400);
        }
        if (lblNiveauLabel != null) lblNiveauLabel.setText(s.getNiveauLabel());
        if (lblNiveauNum   != null) lblNiveauNum.setText("Niveau " + s.getNiveau());
        animateCountUp(lblTotalPoints, 0, s.getTotalPoints(), 1000);
    }

    // ══════════════════════════════════════════════════════════════
    //  ARC DE NIVEAU
    //  ✅ Utilise Color.web() — pas de hex 8 chiffres en inline
    // ══════════════════════════════════════════════════════════════

    private void renderLevelArc(UserStats s) {
        if (arcContainer == null) return;
        arcContainer.getChildren().clear();

        double pct = s.getPointsProchinNiveau() > 0
                ? (double) s.getPointsVersProchinNiveau() / s.getPointsProchinNiveau()
                : 1.0;
        pct = Math.min(1.0, Math.max(0.0, pct));

        int pointsRestants = Math.max(0,
                s.getPointsProchinNiveau() - s.getPointsVersProchinNiveau());

        final double SIZE   = 120;
        final double CX     = SIZE / 2;
        final double CY     = SIZE / 2;
        final double RADIUS = 44;
        final double STROKE = 10;

        // Arc fond
        Arc bgArc = new Arc(CX, CY, RADIUS, RADIUS, 225, -270);
        bgArc.setType(ArcType.OPEN);
        bgArc.setFill(null);
        bgArc.setStroke(Color.web("#2D3748"));
        bgArc.setStrokeWidth(STROKE);
        bgArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Arc progression
        Arc fgArc = new Arc(CX, CY, RADIUS, RADIUS, 225, 0);
        fgArc.setType(ArcType.OPEN);
        fgArc.setFill(null);
        fgArc.setStroke(Color.web(FIRE_COLOR));
        fgArc.setStrokeWidth(STROKE);
        fgArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Texte central
        VBox center = new VBox(2);
        center.setAlignment(Pos.CENTER);
        Label pctLbl = new Label(Math.round(pct * 100) + "%");
        pctLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#F8FAFC;");
        Label xpLbl = new Label(pointsRestants + " XP");
        xpLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#94A3B8;");
        center.getChildren().addAll(pctLbl, xpLbl);

        arcContainer.setMinSize(SIZE, SIZE);
        arcContainer.setMaxSize(SIZE, SIZE);
        arcContainer.setPrefSize(SIZE, SIZE);
        StackPane.setAlignment(bgArc,  Pos.TOP_LEFT);
        StackPane.setAlignment(fgArc,  Pos.TOP_LEFT);
        StackPane.setAlignment(center, Pos.CENTER);
        arcContainer.getChildren().addAll(bgArc, fgArc, center);

        // Animation
        double target = -(270.0 * pct);
        Timeline anim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(fgArc.lengthProperty(), 0.0)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(fgArc.lengthProperty(), target, Interpolator.EASE_OUT))
        );
        anim.play();

        if (lblArcPct != null) lblArcPct.setText(Math.round(pct * 100) + "%");
        if (lblArcXp  != null) lblArcXp.setText(pointsRestants + " XP restants");
    }

    // ══════════════════════════════════════════════════════════════
    //  STREAK HERO
    // ══════════════════════════════════════════════════════════════

    private void renderStreakHero(UserStats s) {
        int streak = s.getStreakActuel();

        if (lblStreakFire != null) {
            lblStreakFire.setText(streakEmoji(streak));
            ScaleTransition pulse = new ScaleTransition(Duration.millis(850), lblStreakFire);
            pulse.setFromX(1.0); pulse.setFromY(1.0);
            pulse.setToX(1.18);  pulse.setToY(1.18);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        }

        animateCountUp(lblStreakCount, 0, streak, 800);

        if (lblStreakSub != null) {
            if      (streak == 0)  lblStreakSub.setText("🌟 Commencez votre streak aujourd'hui !");
            else if (streak == 1)  lblStreakSub.setText("💪 Jour 1 — continuez demain !");
            else if (streak < 7)   lblStreakSub.setText("🔥 " + streak + " jours — continuez !");
            else if (streak < 30)  lblStreakSub.setText("🎉 " + streak + " jours — incroyable !");
            else                   lblStreakSub.setText("👑 " + streak + " jours — LÉGENDAIRE !");
        }

        if (lblStreakMax != null)
            lblStreakMax.setText("🏆 Record : " + s.getStreakMax() + " jours");

        if (streakProgressBar != null) {
            double progress = Math.min(1.0, (double) streak / 7);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(streakProgressBar.progressProperty(), 0)),
                    new KeyFrame(Duration.millis(800),
                            new KeyValue(streakProgressBar.progressProperty(), progress,
                                    Interpolator.EASE_OUT))
            );
            tl.play();
        }
    }

    private String streakEmoji(int streak) {
        if (streak >= 100) return "👑🔥🔥🔥";
        if (streak >= 50)  return "🏆🔥🔥";
        if (streak >= 30)  return "🌟🔥🔥";
        if (streak >= 14)  return "💪🔥";
        if (streak >= 7)   return "🔥";
        if (streak >= 3)   return "⚡";
        if (streak >= 1)   return "🌱";
        return "❄️";
    }

    // ══════════════════════════════════════════════════════════════
    //  STATS
    // ══════════════════════════════════════════════════════════════

    private void renderStats(UserStats s) {
        animateCountUp(lblTachesTotal, 0, s.getTotalTachesCompletees(), 700);
        animateCountUp(lblSessions,    0, s.getTotalSessions(),         500);
        animateCountUp(lblJoursActifs, 0, s.getTotalJoursActifs(),      600);

        if (lblTauxCompletion != null)
            lblTauxCompletion.setText(String.format("%.0f%%", s.getTauxCompletion()));

        if (lblDureeTotal != null) {
            int h = s.getDureeMinutesTotale() / 60;
            int m = s.getDureeMinutesTotale() % 60;
            lblDureeTotal.setText(h > 0 ? h + "h " + m + "m" : m + "m");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GRAPHE HEBDOMADAIRE
    //  ✅ Rectangle.setFill(Color.web(...)) — pas de hex inline
    // ══════════════════════════════════════════════════════════════

    private void renderHebdoChart(UserStats s) {
        if (hebdoChart == null) return;
        hebdoChart.getChildren().clear();
        hebdoChart.setAlignment(Pos.BOTTOM_CENTER);
        hebdoChart.setSpacing(8);

        String[]  labels = {"LUN","MAR","MER","JEU","VEN","SAM","DIM"};
        int[]     pts    = s.getActiviteHebdo();
        boolean[] actifs = s.getJoursActifs();
        LocalDate today  = LocalDate.now();

        int maxPts = 1;
        for (int p : pts) maxPts = Math.max(maxPts, p);

        // Meilleur jour
        int bestPts = 0; String bestDay = "";
        for (int i = 0; i < 7; i++) {
            if (pts[i] > bestPts) { bestPts = pts[i]; bestDay = labels[i]; }
        }
        if (lblBestDay != null && bestPts > 0)
            lblBestDay.setText("📈 Meilleur : " + bestDay + " (" + bestPts + " XP)");

        for (int i = 0; i < 7; i++) {
            LocalDate jour    = today.minusDays(6 - i);
            boolean   isToday = jour.equals(today);
            int       p       = pts[i];
            double    ratio   = (double) p / maxPts;

            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            col.setPrefWidth(40);

            // Valeur XP au-dessus
            Label ptsLbl = new Label(p > 0 ? String.valueOf(p) : " ");
            ptsLbl.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:"
                    + (isToday ? FIRE_COLOR : "#94A3B8") + ";");
            col.getChildren().add(ptsLbl);

            // Barre
            StackPane barWrap = new StackPane();
            barWrap.setPrefWidth(28);
            barWrap.setAlignment(Pos.BOTTOM_CENTER);

            Rectangle bg = new Rectangle(26, 65);
            bg.setArcWidth(6); bg.setArcHeight(6);
            bg.setFill(Color.web(DARK_BG));

            double fillH = Math.max(4, ratio * 60);
            Rectangle fill = new Rectangle(20, 0);
            fill.setArcWidth(6); fill.setArcHeight(6);
            // ✅ Color.web() — compatible, pas de hex 8 chiffres
            String barColor = isToday ? FIRE_COLOR : (actifs[i] ? PURPLE_COLOR : "#334155");
            fill.setFill(Color.web(barColor));

            barWrap.getChildren().addAll(bg, fill);
            StackPane.setAlignment(fill, Pos.BOTTOM_CENTER);

            // Animation barre
            int delay = i * 55;
            Timeline barAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(fill.heightProperty(), 0)),
                    new KeyFrame(Duration.millis(350 + delay),
                            new KeyValue(fill.heightProperty(), fillH, Interpolator.EASE_OUT))
            );
            barAnim.setDelay(Duration.millis(delay));
            barAnim.play();

            col.getChildren().add(barWrap);

            // Label jour
            String dayText = isToday ? "AUJ" : labels[jour.getDayOfWeek().getValue() - 1];
            Label dayLbl = new Label(dayText);
            dayLbl.setStyle("-fx-font-size:9px;-fx-font-weight:" + (isToday ? "bold" : "normal")
                    + ";-fx-text-fill:" + (isToday ? FIRE_COLOR : "#64748B") + ";");
            col.getChildren().add(dayLbl);

            hebdoChart.getChildren().add(col);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SESSIONS RÉCENTES
    // ══════════════════════════════════════════════════════════════

    private void renderRecentSessions(List<UserSession> sessions) {
        if (sessionsContainer == null) return;
        sessionsContainer.getChildren().clear();

        if (sessions.isEmpty()) {
            Label empty = new Label("📭  Aucune session — commencez aujourd'hui !");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;-fx-padding:16;");
            sessionsContainer.getChildren().add(empty);
            return;
        }

        for (int idx = 0; idx < sessions.size(); idx++) {
            UserSession sess = sessions.get(idx);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            // ✅ Pas de border-color avec alpha hex — on utilise une couleur opaque
            row.setStyle(
                    "-fx-background-color:" + DARK_BG + ";" +
                            "-fx-background-radius:12;" +
                            "-fx-border-color:#FF6B35;" +
                            "-fx-border-width:0 0 0 3;" +
                            "-fx-border-radius:12;"
            );

            // Date
            VBox dateBox = new VBox(2);
            dateBox.setAlignment(Pos.CENTER);
            dateBox.setMinWidth(42);
            Label dayNum = new Label(String.valueOf(sess.getSessionDate().getDayOfMonth()));
            dayNum.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#F8FAFC;");
            Label mon = new Label(sess.getSessionDate().getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase());
            mon.setStyle("-fx-font-size:9px;-fx-text-fill:#64748B;-fx-font-weight:bold;");
            dateBox.getChildren().addAll(dayNum, mon);

            // Stats
            VBox statsBox = new VBox(3);
            HBox.setHgrow(statsBox, Priority.ALWAYS);

            HBox topRow = new HBox(10);
            Label dur = new Label("⏱ " + sess.getDureeMinutes() + " min");
            dur.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#F8FAFC;");
            Label tach = new Label("✅ " + sess.getTachesCompletees() + " tâches");
            tach.setStyle("-fx-font-size:11px;-fx-text-fill:" + GREEN_COLOR + ";");
            topRow.getChildren().addAll(dur, tach);

            Label xp = new Label("+" + sess.getPointsGagnes() + " XP");
            xp.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + GOLD_COLOR + ";");
            statsBox.getChildren().addAll(topRow, xp);

            row.getChildren().addAll(dateBox, statsBox);

            // Animation slide depuis gauche
            row.setOpacity(0);
            row.setTranslateX(-20);
            Timeline enter = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(row.opacityProperty(), 0),
                            new KeyValue(row.translateXProperty(), -20)),
                    new KeyFrame(Duration.millis(260 + idx * 65),
                            new KeyValue(row.opacityProperty(), 1,  Interpolator.EASE_OUT),
                            new KeyValue(row.translateXProperty(), 0, Interpolator.EASE_OUT))
            );
            enter.play();

            sessionsContainer.getChildren().add(row);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  BADGES
    //  ✅ FIX PRINCIPAL :
    //    - Pas de linear-gradient(135deg,...) → non supporté inline
    //    - Pas de hex 8 chiffres (#FFD70055)  → non supporté inline
    //    - Pas de dropshadow avec couleur semi-transparente
    //    - Utilisation de couleurs solides hex 6 chiffres
    // ══════════════════════════════════════════════════════════════

    private void renderBadges(UserStats s) {
        if (badgesContainer == null) return;
        badgesContainer.getChildren().clear();

        List<String> unlocked = s.getBadges();
        if (lblBadgesCount != null)
            lblBadgesCount.setText(unlocked.size() + " badge(s) débloqué(s)");

        // ── Badges débloqués ──────────────────────────────────
        for (String badge : unlocked) {
            Label b = new Label(badge);
            b.setStyle(
                    // ✅ Couleur solide au lieu de linear-gradient(135deg,...)
                    "-fx-background-color:#1E1B3A;" +
                            "-fx-text-fill:" + GOLD_COLOR + ";" +
                            "-fx-font-size:11px;" +
                            "-fx-font-weight:bold;" +
                            "-fx-background-radius:25;" +
                            // ✅ Couleur de bordure opaque hex 6 chiffres
                            "-fx-border-color:#B8860B;" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:25;" +
                            "-fx-padding:8 16 8 16;"
            );
            b.setOnMouseEntered(e -> { b.setScaleX(1.06); b.setScaleY(1.06); });
            b.setOnMouseExited(e  -> { b.setScaleX(1.0);  b.setScaleY(1.0); });
            badgesContainer.getChildren().add(b);
        }

        // ── Badges verrouillés (preview) ──────────────────────
        String[][] locked = {
                {"🔒 3 jours",   "3 jours consécutifs"},
                {"🔒 7 jours",   "7 jours consécutifs"},
                {"🔒 10 tâches", "Compléter 10 tâches"},
                {"🔒 30 jours",  "30 jours consécutifs"},
                {"🔒 50 tâches", "Compléter 50 tâches"},
                {"🔒 100 XP",    "Gagner 100 XP"},
        };
        for (String[] lb : locked) {
            String  name = lb[0].replace("🔒 ", "");
            boolean done = unlocked.stream().anyMatch(u -> u.contains(name));
            if (!done) {
                Label b = new Label(lb[0]);
                b.setStyle(
                        "-fx-background-color:" + DARKER_BG + ";" +
                                "-fx-text-fill:#475569;" +
                                "-fx-font-size:11px;" +
                                "-fx-background-radius:25;" +
                                "-fx-border-color:#334155;" +
                                "-fx-border-width:1;" +
                                "-fx-border-radius:25;" +
                                "-fx-padding:8 16 8 16;"
                );
                Tooltip.install(b, new Tooltip(lb[1]));
                badgesContainer.getChildren().add(b);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRES ANIMATION
    // ══════════════════════════════════════════════════════════════

    private void animateCountUp(Label label, int from, int to, int durationMs) {
        if (label == null) return;
        int steps = Math.min(60, Math.max(1, Math.abs(to - from)));
        Timeline tl = new Timeline();
        for (int i = 0; i <= steps; i++) {
            int fi = i;
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis((double) durationMs * i / steps),
                    e -> label.setText(String.valueOf(
                            (int)(from + (to - from) * (double) fi / steps)))
            ));
        }
        tl.play();
    }

    private void fadeIn(javafx.scene.Node node, int ms) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}