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

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label   lblUserName;
    @FXML private Label   lblNiveauLabel;
    @FXML private Label   lblNiveauNum;
    @FXML private Label   lblTotalPoints;
    @FXML private Label   lblTauxCompletion;
    @FXML private Label   lblTachesTotal;
    @FXML private Label   lblDureeTotal;
    @FXML private Label   lblSessions;
    @FXML private Label   lblJoursActifs;

    // Streak principal
    @FXML private Label   lblStreakFire;
    @FXML private Label   lblStreakCount;
    @FXML private Label   lblStreakSub;
    @FXML private Label   lblStreakMax;
    @FXML private ProgressBar streakProgressBar;

    // Arc de progression niveau
    @FXML private StackPane arcContainer;
    @FXML private Label     lblArcPct;
    @FXML private Label     lblArcXp;

    // Graphe hebdo
    @FXML private HBox    hebdoChart;
    @FXML private Label   lblHebdoTitle;
    @FXML private Label   lblBestDay;

    // Badges
    @FXML private FlowPane badgesContainer;
    @FXML private Label   lblBadgesCount;

    // Sessions récentes
    @FXML private VBox    sessionsContainer;

    // ── Données ──────────────────────────────────────────────────────────────
    private User             currentUser;
    private UserStats        stats;
    private final UserSessionService sessionService = UserSessionService.getInstance();

    // ── Couleurs du theme ────────────────────────────────────────────────────
    private static final String FIRE_COLOR    = "#FF6B35";
    private static final String GOLD_COLOR    = "#FFD700";
    private static final String PURPLE_COLOR  = "#6C63FF";
    private static final String GREEN_COLOR   = "#10B981";
    private static final String BLUE_COLOR    = "#3B82F6";

    // ════════════════════════════════════════════════════════════════════════
    //  SETTERS
    // ════════════════════════════════════════════════════════════════════════

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadAndRender();
    }

    public void refresh() {
        loadAndRender();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Animation d'entrée
        if (arcContainer != null) {
            arcContainer.setOpacity(0);
            ScaleTransition st = new ScaleTransition(Duration.millis(400), arcContainer);
            st.setFromX(0.5);
            st.setFromY(0.5);
            st.setToX(1);
            st.setToY(1);
            st.play();
        }
    }

    private void loadAndRender() {
        if (currentUser == null) return;

        new Thread(() -> {
            UserStats s = sessionService.getUserStats(currentUser.getId());
            List<UserSession> recent = sessionService.getRecentSessions(currentUser.getId(), 5);
            Platform.runLater(() -> {
                this.stats = s;
                renderAll(s, recent);
            });
        }).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDU PRINCIPAL
    // ════════════════════════════════════════════════════════════════════════

    private void renderAll(UserStats s, List<UserSession> sessions) {
        renderHeader(s);
        renderLevelArc(s);
        renderStreakHero(s);
        renderStats(s);
        renderRecentSessions(sessions);
        renderHebdoChart(s);
        renderBadges(s);
        animateAll();
    }

    // ── Header ───────────────────────────────────────────────────────────────
    private void renderHeader(UserStats s) {
        if (lblUserName != null) {
            lblUserName.setText("Bonjour, " + currentUser.getPrenom() + " 👋");
            animateText(lblUserName);
        }
        if (lblNiveauLabel != null) {
            lblNiveauLabel.setText(s.getNiveauLabel());
        }
        if (lblNiveauNum != null) {
            lblNiveauNum.setText("Niveau " + s.getNiveau());
        }
        if (lblTotalPoints != null) {
            animateCountUp(lblTotalPoints, 0, s.getTotalPoints(), 1000);
        }
    }

    // ── Arc de Niveau ────────────────────────────────────────────────────────
    private void renderLevelArc(UserStats s) {
        if (arcContainer == null) return;
        arcContainer.getChildren().clear();

        double pct = s.getPointsProchinNiveau() > 0
                ? (double) s.getPointsVersProchinNiveau() / s.getPointsProchinNiveau()
                : 1.0;
        pct = Math.min(1.0, Math.max(0.0, pct));

        int pointsRestants = s.getPointsProchinNiveau() - s.getPointsVersProchinNiveau();

        // Arc de fond
        Arc bgArc = new Arc(60, 60, 52, 52, 220, -260);
        bgArc.setType(ArcType.OPEN);
        bgArc.setFill(null);
        bgArc.setStroke(Color.web("#1E293B"));
        bgArc.setStrokeWidth(12);

        // Arc de progression
        Arc fgArc = new Arc(60, 60, 52, 52, 220, -(260 * pct));
        fgArc.setType(ArcType.OPEN);
        fgArc.setFill(null);
        fgArc.setStroke(Color.web(FIRE_COLOR));
        fgArc.setStrokeWidth(12);
        fgArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Texte central
        VBox center = new VBox(3);
        center.setAlignment(Pos.CENTER);
        Label pctLbl = new Label(Math.round(pct * 100) + "%");
        pctLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#F8FAFC;");
        Label xpLbl = new Label(pointsRestants + " XP");
        xpLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#94A3B8;");
        center.getChildren().addAll(pctLbl, xpLbl);

        if (lblArcPct != null) lblArcPct.setText(Math.round(pct * 100) + "%");
        if (lblArcXp != null) lblArcXp.setText(pointsRestants + " XP restants");

        arcContainer.setMinSize(130, 130);
        arcContainer.setMaxSize(130, 130);
        arcContainer.getChildren().addAll(bgArc, fgArc, center);

        // Animation de l'arc
        Timeline arcAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(fgArc.lengthProperty(), 0)),
                new KeyFrame(Duration.millis(1000), new KeyValue(fgArc.lengthProperty(), -(260 * pct), Interpolator.EASE_OUT))
        );
        arcAnim.play();
    }

    // ── Streak Hero ──────────────────────────────────────────────────────────
    private void renderStreakHero(UserStats s) {
        int streak = s.getStreakActuel();

        if (lblStreakFire != null) {
            lblStreakFire.setText(getStreakEmoji(streak));
            ScaleTransition pulse = new ScaleTransition(Duration.millis(800), lblStreakFire);
            pulse.setFromX(1.0); pulse.setFromY(1.0);
            pulse.setToX(1.15);  pulse.setToY(1.15);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        }

        if (lblStreakCount != null) {
            animateCountUp(lblStreakCount, 0, streak, 800);
        }

        if (lblStreakSub != null) {
            String[] messages = {
                    "🌟 Commencez votre streak aujourd'hui !",
                    "💪 Jour 1 — continuez demain !",
                    "🔥 " + streak + " jours — vous êtes sur une bonne lancée !",
                    "🎉 " + streak + " jours — incroyable progression !",
                    "🏆 " + streak + " jours — vous êtes une légende !"
            };
            int idx = Math.min(4, streak / 7);
            lblStreakSub.setText(streak == 0 ? messages[0] : (streak == 1 ? messages[1] : (streak < 7 ? messages[2] : (streak < 30 ? messages[3] : messages[4]))));
        }

        if (lblStreakMax != null) {
            lblStreakMax.setText("🏆 Record : " + s.getStreakMax() + " jours");
        }

        if (streakProgressBar != null) {
            double progress = Math.min(1.0, (double) streak / 7);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(streakProgressBar.progressProperty(), 0)),
                    new KeyFrame(Duration.millis(800), new KeyValue(streakProgressBar.progressProperty(), progress))
            );
            timeline.play();
        }
    }

    private String getStreakEmoji(int streak) {
        if (streak >= 100) return "👑🔥🔥🔥";
        if (streak >= 50) return "🏆🔥🔥";
        if (streak >= 30) return "🌟🔥🔥";
        if (streak >= 14) return "💪🔥";
        if (streak >= 7) return "🔥";
        if (streak >= 3) return "⚡";
        if (streak >= 1) return "🌱";
        return "❄️";
    }

    // ── Statistiques ─────────────────────────────────────────────────────────
    private void renderStats(UserStats s) {
        animateCountUp(lblTachesTotal, 0, s.getTotalTachesCompletees(), 700);
        animateCountUp(lblSessions, 0, s.getTotalSessions(), 500);
        animateCountUp(lblJoursActifs, 0, s.getTotalJoursActifs(), 600);

        if (lblTauxCompletion != null) {
            animateCountUp(lblTauxCompletion, 0, (int) Math.round(s.getTauxCompletion()), 700);
            lblTauxCompletion.setText(String.format("%.0f%%", s.getTauxCompletion()));
        }

        if (lblDureeTotal != null) {
            int h = s.getDureeMinutesTotale() / 60;
            int m = s.getDureeMinutesTotale() % 60;
            lblDureeTotal.setText(h > 0 ? h + "h " + m + "m" : m + "m");
        }
    }

    // ── Graphe Hebdomadaire ──────────────────────────────────────────────────
    private void renderHebdoChart(UserStats s) {
        if (hebdoChart == null) return;
        hebdoChart.getChildren().clear();
        hebdoChart.setSpacing(10);
        hebdoChart.setAlignment(Pos.BOTTOM_CENTER);

        String[] joursLabels = {"LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM"};
        int[]    points      = s.getActiviteHebdo();
        boolean[] actifs     = s.getJoursActifs();
        LocalDate today      = LocalDate.now();

        int maxPts = 1;
        for (int p : points) maxPts = Math.max(maxPts, p);

        // Meilleur jour
        int bestPoints = 0;
        String bestDay = "";
        for (int i = 0; i < 7; i++) {
            if (points[i] > bestPoints) {
                bestPoints = points[i];
                bestDay = joursLabels[i];
            }
        }
        if (lblBestDay != null && bestPoints > 0) {
            lblBestDay.setText("📈 Meilleur jour : " + bestDay + " (" + bestPoints + " XP)");
        }

        for (int i = 0; i < 7; i++) {
            LocalDate jour = today.minusDays(6 - i);
            boolean isToday = jour.equals(today);
            boolean actif   = actifs[i];
            int     pts     = points[i];
            double  ratio   = maxPts > 0 ? (double) pts / maxPts : 0;

            VBox col = new VBox(5);
            col.setAlignment(Pos.BOTTOM_CENTER);
            col.setPrefWidth(45);

            // Points label
            if (pts > 0) {
                Label ptsLbl = new Label(String.valueOf(pts));
                ptsLbl.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + (isToday ? FIRE_COLOR : "#94A3B8") + ";");
                col.getChildren().add(ptsLbl);
            } else {
                Label empty = new Label(" ");
                col.getChildren().add(empty);
            }

            // Barre
            StackPane barWrap = new StackPane();
            barWrap.setPrefWidth(32);
            barWrap.setAlignment(Pos.BOTTOM_CENTER);

            Rectangle bgBar = new Rectangle(28, 70);
            bgBar.setArcWidth(6); bgBar.setArcHeight(6);
            bgBar.setFill(Color.web("#1E293B"));

            double fillHeight = Math.max(4, ratio * 65);
            Rectangle fillBar = new Rectangle(24, 0);
            fillBar.setArcWidth(6); fillBar.setArcHeight(6);
            String barColor = isToday ? FIRE_COLOR : (actif ? PURPLE_COLOR : "#334155");
            fillBar.setFill(Color.web(barColor));

            barWrap.getChildren().addAll(bgBar, fillBar);
            StackPane.setAlignment(fillBar, Pos.BOTTOM_CENTER);

            Timeline barAnim = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(fillBar.heightProperty(), 0)),
                    new KeyFrame(Duration.millis(400 + i * 60),
                            new KeyValue(fillBar.heightProperty(), fillHeight, Interpolator.EASE_OUT))
            );
            barAnim.setDelay(Duration.millis(i * 50));
            barAnim.play();

            col.getChildren().add(barWrap);

            // Label jour
            Label dayLbl = new Label(isToday ? "AUJ" : joursLabels[i]);
            dayLbl.setStyle("-fx-font-size:9px;-fx-font-weight:" + (isToday ? "bold" : "normal") +
                    ";-fx-text-fill:" + (isToday ? FIRE_COLOR : "#64748B") + ";");
            col.getChildren().add(dayLbl);

            hebdoChart.getChildren().add(col);
        }
    }

    // ── Badges ───────────────────────────────────────────────────────────────
    private void renderBadges(UserStats s) {
        if (badgesContainer == null) return;
        badgesContainer.getChildren().clear();

        List<String> unlockedBadges = s.getBadges();

        if (lblBadgesCount != null) {
            lblBadgesCount.setText(unlockedBadges.size() + " badge(s) débloqué(s)");
        }

        // Badges débloqués avec style amélioré
        for (String badge : unlockedBadges) {
            Label b = new Label(badge);
            b.setStyle(
                    "-fx-background-color:linear-gradient(135deg,#2D2B4E,#1E1B3A);" +
                            "-fx-text-fill:#FFD700;" +
                            "-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-background-radius:25;" +
                            "-fx-border-color:#FFD70044;-fx-border-width:1;-fx-border-radius:25;" +
                            "-fx-padding:8 16 8 16;" +
                            "-fx-effect:dropshadow(gaussian,#FFD70030,8,0,0,0);"
            );

            // Animation au survol
            b.setOnMouseEntered(e -> {
                b.setScaleX(1.05);
                b.setScaleY(1.05);
                b.setStyle(b.getStyle().replace("-fx-effect:dropshadow(gaussian,#FFD70030,8,0,0,0);",
                        "-fx-effect:dropshadow(gaussian,#FFD700,12,0,0,0);"));
            });
            b.setOnMouseExited(e -> {
                b.setScaleX(1.0);
                b.setScaleY(1.0);
                b.setStyle(b.getStyle().replace("-fx-effect:dropshadow(gaussian,#FFD700,12,0,0,0);",
                        "-fx-effect:dropshadow(gaussian,#FFD70030,8,0,0,0);"));
            });

            badgesContainer.getChildren().add(b);
        }

        // Badges verrouillés (preview)
        String[][] lockedBadges = {
                {"🔒 3 jours", "3 jours consécutifs", "3"},
                {"🔒 7 jours", "7 jours consécutifs", "7"},
                {"🔒 10 tâches", "Compléter 10 tâches", "10"},
                {"🔒 30 jours", "30 jours consécutifs", "30"},
                {"🔒 50 tâches", "Compléter 50 tâches", "50"},
                {"🔒 100 XP", "Gagner 100 XP", "100"},
        };

        for (String[] lb : lockedBadges) {
            boolean isUnlocked = unlockedBadges.stream().anyMatch(b -> b.contains(lb[0].replace("🔒 ", "")));
            if (!isUnlocked) {
                Label b = new Label(lb[0]);
                b.setStyle(
                        "-fx-background-color:#0F172A;" +
                                "-fx-text-fill:#475569;" +
                                "-fx-font-size:11px;" +
                                "-fx-background-radius:25;" +
                                "-fx-border-color:#334155;-fx-border-width:1;-fx-border-radius:25;" +
                                "-fx-padding:8 16 8 16;"
                );
                Tooltip.install(b, new Tooltip(lb[1]));
                badgesContainer.getChildren().add(b);
            }
        }
    }

    // ── Sessions Récentes ────────────────────────────────────────────────────
    private void renderRecentSessions(List<UserSession> sessions) {
        if (sessionsContainer == null) return;
        sessionsContainer.getChildren().clear();

        if (sessions.isEmpty()) {
            Label empty = new Label("📭 Aucune session enregistrée\nCommencez à apprendre dès aujourd'hui !");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;-fx-padding:30;-fx-alignment:center;");
            empty.setAlignment(Pos.CENTER);
            sessionsContainer.getChildren().add(empty);
            return;
        }

        for (UserSession s : sessions) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12, 16, 12, 16));
            row.setStyle(
                    "-fx-background-color:#1E293B;" +
                            "-fx-background-radius:14;" +
                            "-fx-border-color:#334155;-fx-border-width:0 0 0 3;-fx-border-radius:14;" +
                            "-fx-cursor:hand;"
            );

            // Icône date
            VBox dateBox = new VBox(3);
            dateBox.setAlignment(Pos.CENTER);
            dateBox.setMinWidth(50);
            Label dayNum = new Label(String.valueOf(s.getSessionDate().getDayOfMonth()));
            dayNum.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#F8FAFC;");
            Label monthLbl = new Label(s.getSessionDate().getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase());
            monthLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#64748B;-fx-font-weight:bold;");
            dateBox.getChildren().addAll(dayNum, monthLbl);

            // Stats de la session
            VBox statsBox = new VBox(5);
            HBox.setHgrow(statsBox, Priority.ALWAYS);

            HBox topRow = new HBox(12);
            Label duree = new Label("⏱ " + s.getDureeMinutes() + " min");
            duree.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#F8FAFC;");
            Label taches = new Label("✅ " + s.getTachesCompletees() + " tâches");
            taches.setStyle("-fx-font-size:11px;-fx-text-fill:" + GREEN_COLOR + ";");
            topRow.getChildren().addAll(duree, taches);

            Label pts = new Label("+" + s.getPointsGagnes() + " XP");
            pts.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + GOLD_COLOR + ";");
            statsBox.getChildren().addAll(topRow, pts);

            row.getChildren().addAll(dateBox, statsBox);

            // Animation au survol
            row.setOnMouseEntered(e -> row.setStyle(row.getStyle() + "-fx-background-color:#2A2A3E;"));
            row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("-fx-background-color:#2A2A3E;", "-fx-background-color:#1E293B;")));

            // Animation d'entrée
            row.setOpacity(0);
            row.setTranslateX(-20);
            Timeline enterAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(row.opacityProperty(), 0),
                            new KeyValue(row.translateXProperty(), -20)),
                    new KeyFrame(Duration.millis(300 + sessions.indexOf(s) * 80),
                            new KeyValue(row.opacityProperty(), 1),
                            new KeyValue(row.translateXProperty(), 0))
            );
            enterAnim.play();

            sessionsContainer.getChildren().add(row);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ════════════════════════════════════════════════════════════════════════

    private void animateAll() {
        // Animation d'apparition du container principal
        if (arcContainer != null) {
            arcContainer.setOpacity(0);
            ScaleTransition st = new ScaleTransition(Duration.millis(400), arcContainer);
            st.setFromX(0.8);
            st.setFromY(0.8);
            st.setToX(1);
            st.setToY(1);
            st.setOnFinished(e -> arcContainer.setOpacity(1));
            st.play();
        }
    }

    private void animateText(Label label) {
        if (label == null) return;
        label.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), label);
        tt.setFromY(-20);
        tt.setToY(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        tt.play();
        ft.play();
    }

    private void animateCountUp(Label label, int from, int to, int durationMs) {
        if (label == null) return;
        Timeline tl = new Timeline();
        int steps = Math.min(50, Math.max(10, Math.abs(to - from)));
        for (int i = 0; i <= steps; i++) {
            int finalI = i;
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis((double) durationMs * i / steps),
                    e -> {
                        int value = (int)(from + (to - from) * (double) finalI / steps);
                        if (label.getId() != null && label.getId().contains("Taux")) {
                            label.setText(value + "%");
                        } else {
                            label.setText(String.valueOf(value));
                        }
                    }
            ));
        }
        tl.play();
    }
}