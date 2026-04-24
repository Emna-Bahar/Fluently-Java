package com.example.pijava_fluently.entites;

/**
 * Statistiques agrégées d'un utilisateur calculées depuis ses sessions.
 * Contient tout ce qu'il faut pour afficher les streaks et la progression.
 */
public class UserStats {

    private int    userId;

    // ── Streaks ───────────────────────────────────────────────────
    private int    streakActuel;          // jours consécutifs actuels
    private int    streakMax;             // meilleur streak historique
    private int    totalJoursActifs;      // total de jours d'activité

    // ── Sessions ──────────────────────────────────────────────────
    private int    totalSessions;
    private int    dureeMinutesTotale;
    private int    sessionMoyenneMinutes;
    private int    sessionMaxMinutes;

    // ── Tâches ────────────────────────────────────────────────────
    private int    totalTachesCompletees;
    private int    totalTachesCommencees;
    private double tauxCompletion;        // 0-100

    // ── Objectifs ─────────────────────────────────────────────────
    private int    totalObjectifsConsultes;

    // ── Points & Niveau ──────────────────────────────────────────
    private int    totalPoints;
    private int    niveau;
    private String niveauLabel;           // "Débutant", "Explorateur", etc.
    private int    pointsVersProchinNiveau;
    private int    pointsProchinNiveau;

    // ── Activité hebdomadaire (7 derniers jours) ─────────────────
    private int[]  activiteHebdo = new int[7];  // points par jour
    private boolean[] joursActifs = new boolean[7]; // true si actif ce jour

    // ── Badges débloqués ─────────────────────────────────────────
    private java.util.List<String> badges = new java.util.ArrayList<>();

    // ════ Getters & Setters ══════════════════════════════════════

    public int getUserId()                              { return userId; }
    public void setUserId(int v)                        { this.userId = v; }

    public int getStreakActuel()                        { return streakActuel; }
    public void setStreakActuel(int v)                  { this.streakActuel = v; }

    public int getStreakMax()                           { return streakMax; }
    public void setStreakMax(int v)                     { this.streakMax = v; }

    public int getTotalJoursActifs()                    { return totalJoursActifs; }
    public void setTotalJoursActifs(int v)              { this.totalJoursActifs = v; }

    public int getTotalSessions()                       { return totalSessions; }
    public void setTotalSessions(int v)                 { this.totalSessions = v; }

    public int getDureeMinutesTotale()                  { return dureeMinutesTotale; }
    public void setDureeMinutesTotale(int v)            { this.dureeMinutesTotale = v; }

    public int getSessionMoyenneMinutes()               { return sessionMoyenneMinutes; }
    public void setSessionMoyenneMinutes(int v)         { this.sessionMoyenneMinutes = v; }

    public int getSessionMaxMinutes()                   { return sessionMaxMinutes; }
    public void setSessionMaxMinutes(int v)             { this.sessionMaxMinutes = v; }

    public int getTotalTachesCompletees()               { return totalTachesCompletees; }
    public void setTotalTachesCompletees(int v)         { this.totalTachesCompletees = v; }

    public int getTotalTachesCommencees()               { return totalTachesCommencees; }
    public void setTotalTachesCommencees(int v)         { this.totalTachesCommencees = v; }

    public double getTauxCompletion()                   { return tauxCompletion; }
    public void setTauxCompletion(double v)             { this.tauxCompletion = v; }

    public int getTotalObjectifsConsultes()             { return totalObjectifsConsultes; }
    public void setTotalObjectifsConsultes(int v)       { this.totalObjectifsConsultes = v; }

    public int getTotalPoints()                         { return totalPoints; }
    public void setTotalPoints(int v)                   { this.totalPoints = v; }

    public int getNiveau()                              { return niveau; }
    public void setNiveau(int v)                        { this.niveau = v; }

    public String getNiveauLabel()                      { return niveauLabel; }
    public void setNiveauLabel(String v)                { this.niveauLabel = v; }

    public int getPointsVersProchinNiveau()             { return pointsVersProchinNiveau; }
    public void setPointsVersProchinNiveau(int v)       { this.pointsVersProchinNiveau = v; }

    public int getPointsProchinNiveau()                 { return pointsProchinNiveau; }
    public void setPointsProchinNiveau(int v)           { this.pointsProchinNiveau = v; }

    public int[] getActiviteHebdo()                     { return activiteHebdo; }
    public void setActiviteHebdo(int[] v)               { this.activiteHebdo = v; }

    public boolean[] getJoursActifs()                   { return joursActifs; }
    public void setJoursActifs(boolean[] v)             { this.joursActifs = v; }

    public java.util.List<String> getBadges()           { return badges; }
    public void setBadges(java.util.List<String> v)     { this.badges = v; }
}