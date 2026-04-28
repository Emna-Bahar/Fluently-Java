package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.UserSession;
import com.example.pijava_fluently.entites.UserStats;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class UserSessionService {

    private static UserSessionService INSTANCE;
    private UserSession currentSession = null;
    private Connection cnx;

    public static UserSessionService getInstance() {
        if (INSTANCE == null) INSTANCE = new UserSessionService();
        return INSTANCE;
    }

    private UserSessionService() {
        cnx = MyDatabase.getInstance().getConnection();
        ensureTableExists();
    }

    private void ensureTableExists() {
        try {
            Statement st = cnx.createStatement();
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_session (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    session_date DATE NOT NULL,
                    login_time DATETIME NOT NULL,
                    logout_time DATETIME,
                    duree_minutes INT DEFAULT 0,
                    taches_completees INT DEFAULT 0,
                    taches_commencees INT DEFAULT 0,
                    objectifs_consultes INT DEFAULT 0,
                    points_gagnes INT DEFAULT 0,
                    INDEX idx_user_date (user_id, session_date)
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_stats_cache (
                    user_id INT PRIMARY KEY,
                    streak_actuel INT DEFAULT 0,
                    streak_max INT DEFAULT 0,
                    total_jours_actifs INT DEFAULT 0,
                    total_sessions INT DEFAULT 0,
                    total_points INT DEFAULT 0,
                    niveau INT DEFAULT 1,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            System.err.println("Erreur création tables: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SESSION LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    public void startSession(int userId) {
        try {
            LocalDate today = LocalDate.now();
            PreparedStatement check = cnx.prepareStatement(
                    "SELECT id FROM user_session WHERE user_id=? AND session_date=? ORDER BY id DESC LIMIT 1"
            );
            check.setInt(1, userId);
            check.setDate(2, Date.valueOf(today));
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // Session déjà créée aujourd'hui → on la reprend
                currentSession = new UserSession(userId, today, LocalDateTime.now());
                currentSession.setId(rs.getInt("id"));
            } else {
                // Nouvelle session
                PreparedStatement ps = cnx.prepareStatement(
                        "INSERT INTO user_session (user_id, session_date, login_time) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, userId);
                ps.setDate(2, Date.valueOf(today));
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                currentSession = new UserSession(userId, today, LocalDateTime.now());
                if (keys.next()) currentSession.setId(keys.getInt(1));
            }

            if (currentSession != null) {
                currentSession.setPointsGagnes(currentSession.getPointsGagnes() + 20);
                flushSessionIncrement("points_gagnes", 20);
            }
            recalculateStreak(userId);
        } catch (SQLException e) {
            System.err.println("startSession error: " + e.getMessage());
        }
    }

    public void endSession() {
        if (currentSession == null) return;
        try {
            LocalDateTime now = LocalDateTime.now();
            // ✅ FIX : calcul exact de la durée depuis le login
            long minutes = ChronoUnit.MINUTES.between(currentSession.getLoginTime(), now);
            minutes = Math.max(1, minutes); // minimum 1 minute

            PreparedStatement ps = cnx.prepareStatement("""
                UPDATE user_session
                SET logout_time          = ?,
                    duree_minutes        = duree_minutes + ?,
                    taches_completees    = ?,
                    taches_commencees    = ?,
                    objectifs_consultes  = ?,
                    points_gagnes        = points_gagnes + ?
                WHERE id = ?
            """);
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.setLong(2, minutes);
            ps.setInt(3, currentSession.getTachesCompletees());
            ps.setInt(4, currentSession.getTachesCommencees());
            ps.setInt(5, currentSession.getObjectifsConsultes());
            ps.setInt(6, currentSession.getPointsGagnes());
            ps.setInt(7, currentSession.getId());
            ps.executeUpdate();

            System.out.println("✅ Session fermée — durée : " + minutes + " min");
            currentSession = null;
        } catch (SQLException e) {
            System.err.println("endSession error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ENREGISTREMENT D'ÉVÉNEMENTS
    // ══════════════════════════════════════════════════════════════

    public void recordTaskCompleted() {
        if (currentSession == null) return;
        currentSession.setTachesCompletees(currentSession.getTachesCompletees() + 1);
        currentSession.setPointsGagnes(currentSession.getPointsGagnes() + 50);
        flushSessionIncrement("taches_completees", 1, "points_gagnes", 50);
    }

    public void recordTaskStarted() {
        if (currentSession == null) return;
        currentSession.setTachesCommencees(currentSession.getTachesCommencees() + 1);
        currentSession.setPointsGagnes(currentSession.getPointsGagnes() + 10);
        flushSessionIncrement("taches_commencees", 1, "points_gagnes", 10);
    }

    public void recordObjectifConsulted() {
        if (currentSession == null) return;
        currentSession.setObjectifsConsultes(currentSession.getObjectifsConsultes() + 1);
        currentSession.setPointsGagnes(currentSession.getPointsGagnes() + 5);
        flushSessionIncrement("objectifs_consultes", 1, "points_gagnes", 5);
    }

    public void recordTaskFailed() {
        if (currentSession == null) return;
        int deducted = Math.min(20, currentSession.getPointsGagnes());
        currentSession.setPointsGagnes(currentSession.getPointsGagnes() - deducted);
        if (deducted > 0) flushSessionIncrement("points_gagnes", -deducted);
    }

    // ══════════════════════════════════════════════════════════════
    //  ✅ FIX PRINCIPAL : getUserStats inclut la session ACTIVE
    // ══════════════════════════════════════════════════════════════

    public UserStats getUserStats(int userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);

        try {
            // ── Tâches de l'utilisateur ───────────────────────────
            PreparedStatement taskPs = cnx.prepareStatement(
                    "SELECT COUNT(*) as total, " +
                            "SUM(CASE WHEN statut = 'Terminée' THEN 1 ELSE 0 END) as terminees " +
                            "FROM tache WHERE id_objectif_id IN " +
                            "(SELECT id FROM objectif WHERE id_user_id = ?)"
            );
            taskPs.setInt(1, userId);
            ResultSet taskRs = taskPs.executeQuery();
            if (taskRs.next()) {
                int totalTaches     = taskRs.getInt("total");
                int tachesTerminees = taskRs.getInt("terminees");
                stats.setTotalTachesCompletees(tachesTerminees);
                stats.setTotalTachesCommencees(totalTaches);
                stats.setTauxCompletion(
                        totalTaches > 0 ? (double) tachesTerminees / totalTaches * 100 : 0
                );
            }

            // ── Stats sessions DB ─────────────────────────────────
            PreparedStatement ps = cnx.prepareStatement("""
                SELECT
                    COUNT(*)                     AS total_sessions,
                    COALESCE(SUM(duree_minutes), 0) AS total_minutes,
                    COALESCE(SUM(points_gagnes), 0) AS total_pts,
                    COUNT(DISTINCT session_date) AS total_jours
                FROM user_session
                WHERE user_id = ?
            """);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats.setTotalSessions(rs.getInt("total_sessions"));
                stats.setDureeMinutesTotale(rs.getInt("total_minutes"));
                stats.setTotalPoints(rs.getInt("total_pts"));
                stats.setTotalJoursActifs(rs.getInt("total_jours"));
            }

            // ✅ FIX SCREEN TIME : ajouter la durée de la session EN COURS
            // La session active n'a pas encore été flush dans duree_minutes
            if (currentSession != null && currentSession.getUserId() == userId) {
                long minutesActuelles = ChronoUnit.MINUTES.between(
                        currentSession.getLoginTime(), LocalDateTime.now()
                );
                long dureeActive = Math.max(1, minutesActuelles);

                // Ajouter le temps actif non sauvegardé
                stats.setDureeMinutesTotale(
                        stats.getDureeMinutesTotale() + (int) dureeActive
                );

                // Ajouter les points non encore flush (événements session active)
                // Note: les points d'événements SONT flush en temps réel via flushSessionIncrement
                // Seul le temps manque → on l'ajoute ici

                System.out.println("⏱ Session active : +" + dureeActive + " min (non encore flush)");
            }

            // ── Streak ────────────────────────────────────────────
            stats.setStreakActuel(calculateCurrentStreak(userId));
            stats.setStreakMax(calculateMaxStreak(userId));

            // ── Niveau ────────────────────────────────────────────
            int[] niveauData = calculateNiveau(stats.getTotalPoints());
            stats.setNiveau(niveauData[0]);
            stats.setPointsVersProchinNiveau(niveauData[1]);
            stats.setPointsProchinNiveau(niveauData[2]);
            stats.setNiveauLabel(getNiveauLabel(niveauData[0]));

            // ── Graphe hebdo + badges ─────────────────────────────
            fillActiviteHebdo(userId, stats);
            stats.setBadges(calculateBadges(stats));

        } catch (SQLException e) {
            System.err.println("getUserStats error: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    // ══════════════════════════════════════════════════════════════
    //  STREAK
    // ══════════════════════════════════════════════════════════════

    private int calculateCurrentStreak(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT DISTINCT session_date FROM user_session " +
                        "WHERE user_id = ? ORDER BY session_date DESC"
        );
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        List<LocalDate> dates = new ArrayList<>();
        while (rs.next()) dates.add(rs.getDate("session_date").toLocalDate());

        if (dates.isEmpty()) return 0;
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Le streak doit avoir une entrée aujourd'hui ou hier
        if (!dates.get(0).equals(today) && !dates.get(0).equals(yesterday)) return 0;

        int streak = 1;
        for (int i = 0; i < dates.size() - 1; i++) {
            long diff = ChronoUnit.DAYS.between(dates.get(i + 1), dates.get(i));
            if (diff == 1) streak++;
            else break;
        }
        return streak;
    }

    private int calculateMaxStreak(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT DISTINCT session_date FROM user_session " +
                        "WHERE user_id = ? ORDER BY session_date ASC"
        );
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        List<LocalDate> dates = new ArrayList<>();
        while (rs.next()) dates.add(rs.getDate("session_date").toLocalDate());

        if (dates.isEmpty()) return 0;
        int maxStreak = 1, currentStreak = 1;
        for (int i = 1; i < dates.size(); i++) {
            long diff = ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
            if (diff == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }

    private void recalculateStreak(int userId) {
        try {
            int streak    = calculateCurrentStreak(userId);
            int maxStreak = calculateMaxStreak(userId);
            PreparedStatement ps = cnx.prepareStatement("""
                INSERT INTO user_stats_cache (user_id, streak_actuel, streak_max)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    streak_actuel = VALUES(streak_actuel),
                    streak_max    = GREATEST(streak_max, VALUES(streak_max))
            """);
            ps.setInt(1, userId);
            ps.setInt(2, streak);
            ps.setInt(3, maxStreak);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("recalculateStreak error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GRAPHE HEBDOMADAIRE
    // ══════════════════════════════════════════════════════════════

    private void fillActiviteHebdo(int userId, UserStats stats) throws SQLException {
        int[]     points = new int[7];
        boolean[] actifs = new boolean[7];

        PreparedStatement ps = cnx.prepareStatement("""
            SELECT session_date, SUM(points_gagnes) AS pts
            FROM user_session
            WHERE user_id = ? AND session_date >= ?
            GROUP BY session_date
        """);
        ps.setInt(1, userId);
        ps.setDate(2, Date.valueOf(LocalDate.now().minusDays(6)));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            LocalDate d   = rs.getDate("session_date").toLocalDate();
            int       idx = (int) ChronoUnit.DAYS.between(LocalDate.now().minusDays(6), d);
            if (idx >= 0 && idx < 7) {
                points[idx] = rs.getInt("pts");
                actifs[idx] = true;
            }
        }

        // ✅ FIX : ajouter les points de la session active dans le graphe d'aujourd'hui
        if (currentSession != null && currentSession.getUserId() == userId) {
            points[6] += currentSession.getPointsGagnes();
            actifs[6]  = true;
        }

        stats.setActiviteHebdo(points);
        stats.setJoursActifs(actifs);
    }

    // ══════════════════════════════════════════════════════════════
    //  NIVEAU & BADGES
    // ══════════════════════════════════════════════════════════════

    private int[] calculateNiveau(int totalPoints) {
        int[] seuils = {0, 100, 300, 600, 1000, 1500, 2500, 4000, 6000, 10000};
        int niveau = 1;
        for (int i = seuils.length - 1; i >= 0; i--) {
            if (totalPoints >= seuils[i]) { niveau = i + 1; break; }
        }
        if (niveau >= seuils.length) {
            return new int[]{seuils.length, totalPoints - seuils[seuils.length - 1], 0};
        }
        int pointsVersProchain = totalPoints - seuils[niveau - 1];
        int pointsProchain     = seuils[niveau] - seuils[niveau - 1];
        return new int[]{niveau, pointsVersProchain, pointsProchain};
    }

    private String getNiveauLabel(int niveau) {
        return switch (niveau) {
            case 1  -> "🌱 Graine";
            case 2  -> "🌿 Pousse";
            case 3  -> "🍃 Apprenti";
            case 4  -> "🌳 Explorateur";
            case 5  -> "⭐ Érudit";
            case 6  -> "🔥 Maître";
            case 7  -> "💎 Expert";
            case 8  -> "🚀 Champion";
            case 9  -> "👑 Légende";
            default -> "🌌 Mythique";
        };
    }

    private List<String> calculateBadges(UserStats stats) {
        List<String> badges = new ArrayList<>();
        if (stats.getStreakActuel()         >= 3)  badges.add("🔥 Streak 3j");
        if (stats.getStreakActuel()         >= 7)  badges.add("💫 Semaine parfaite");
        if (stats.getTotalTachesCompletees() >= 10) badges.add("✅ 10 tâches");
        if (stats.getTotalTachesCompletees() >= 50) badges.add("🏆 50 tâches");
        if (stats.getTauxCompletion()        >= 80) badges.add("⚡ Efficace");
        if (stats.getDureeMinutesTotale()   >= 60)  badges.add("⏰ 1h d'étude");
        return badges;
    }

    // ══════════════════════════════════════════════════════════════
    //  SESSIONS RÉCENTES
    // ══════════════════════════════════════════════════════════════

    public List<UserSession> getRecentSessions(int userId, int limit) {
        List<UserSession> list = new ArrayList<>();
        try {
            PreparedStatement ps = cnx.prepareStatement("""
                SELECT * FROM user_session WHERE user_id = ?
                ORDER BY session_date DESC, login_time DESC LIMIT ?
            """);
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UserSession s = new UserSession();
                s.setId(rs.getInt("id"));
                s.setUserId(rs.getInt("user_id"));
                s.setSessionDate(rs.getDate("session_date").toLocalDate());
                s.setLoginTime(rs.getTimestamp("login_time").toLocalDateTime());
                if (rs.getTimestamp("logout_time") != null)
                    s.setLogoutTime(rs.getTimestamp("logout_time").toLocalDateTime());
                s.setDureeMinutes(rs.getInt("duree_minutes"));
                s.setTachesCompletees(rs.getInt("taches_completees"));
                s.setTachesCommencees(rs.getInt("taches_commencees"));
                s.setObjectifsConsultes(rs.getInt("objectifs_consultes"));
                s.setPointsGagnes(rs.getInt("points_gagnes"));
                list.add(s);
            }

            // ✅ FIX : si la session active appartient à cet utilisateur,
            // mettre à jour la première entrée avec la durée réelle en cours
            if (currentSession != null && currentSession.getUserId() == userId && !list.isEmpty()) {
                UserSession first = list.get(0);
                if (first.getId() == currentSession.getId()) {
                    long minutesActuelles = Math.max(1, ChronoUnit.MINUTES.between(
                            currentSession.getLoginTime(), LocalDateTime.now()
                    ));
                    first.setDureeMinutes((int) minutesActuelles);
                }
            }

        } catch (SQLException e) {
            System.err.println("getRecentSessions error: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════
    //  FLUSH HELPERS
    // ══════════════════════════════════════════════════════════════

    private void flushSessionIncrement(String col1, int val1, String col2, int val2) {
        if (currentSession == null || currentSession.getId() == 0) return;
        try {
            PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE user_session SET "
                            + col1 + " = " + col1 + " + ?, "
                            + col2 + " = " + col2 + " + ? WHERE id = ?"
            );
            ps.setInt(1, val1);
            ps.setInt(2, val2);
            ps.setInt(3, currentSession.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("flush error: " + e.getMessage());
        }
    }

    private void flushSessionIncrement(String col, int val) {
        if (currentSession == null || currentSession.getId() == 0) return;
        try {
            PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE user_session SET " + col + " = " + col + " + ? WHERE id = ?"
            );
            ps.setInt(1, val);
            ps.setInt(2, currentSession.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("flush error: " + e.getMessage());
        }
    }

    public UserSession getCurrentSession() { return currentSession; }
}