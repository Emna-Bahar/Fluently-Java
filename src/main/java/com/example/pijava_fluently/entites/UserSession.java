package com.example.pijava_fluently.entites;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Représente une session d'apprentissage de l'utilisateur.
 * Stockée en base pour calculer les streaks et la progression.
 */
public class UserSession {

    private int           id;
    private int           userId;
    private LocalDate     sessionDate;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private int           dureeMinutes;
    private int           tachesCompletees;
    private int           tachesCommencees;
    private int           objectifsConsultes;
    private int           pointsGagnes;

    public UserSession() {}

    public UserSession(int userId, LocalDate sessionDate, LocalDateTime loginTime) {
        this.userId      = userId;
        this.sessionDate = sessionDate;
        this.loginTime   = loginTime;
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getUserId()                          { return userId; }
    public void setUserId(int userId)               { this.userId = userId; }

    public LocalDate getSessionDate()               { return sessionDate; }
    public void setSessionDate(LocalDate d)         { this.sessionDate = d; }

    public LocalDateTime getLoginTime()             { return loginTime; }
    public void setLoginTime(LocalDateTime t)       { this.loginTime = t; }

    public LocalDateTime getLogoutTime()            { return logoutTime; }
    public void setLogoutTime(LocalDateTime t)      { this.logoutTime = t; }

    public int getDureeMinutes()                    { return dureeMinutes; }
    public void setDureeMinutes(int d)              { this.dureeMinutes = d; }

    public int getTachesCompletees()                { return tachesCompletees; }
    public void setTachesCompletees(int n)          { this.tachesCompletees = n; }

    public int getTachesCommencees()                { return tachesCommencees; }
    public void setTachesCommencees(int n)          { this.tachesCommencees = n; }

    public int getObjectifsConsultes()              { return objectifsConsultes; }
    public void setObjectifsConsultes(int n)        { this.objectifsConsultes = n; }

    public int getPointsGagnes()                    { return pointsGagnes; }
    public void setPointsGagnes(int p)              { this.pointsGagnes = p; }
}