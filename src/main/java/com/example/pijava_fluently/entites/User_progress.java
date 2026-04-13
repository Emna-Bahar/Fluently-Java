package com.example.pijava_fluently.entites;

import java.time.LocalDateTime;

public class User_progress {
    private int id;
    private int dernierNumeroCours;
    private boolean testNiveauComplete;
    private LocalDateTime dateDerniereActivite;
    private int userId;
    private int langueId;
    private int niveauActuelId;
    private int dernierCoursCompleteId;

    // Constructeur vide
    public User_progress() {}

    // Constructeur complet
    public User_progress(int id, int dernierNumeroCours, boolean testNiveauComplete,
                         LocalDateTime dateDerniereActivite, int userId, int langueId,
                         int niveauActuelId, int dernierCoursCompleteId) {
        this.id = id;
        this.dernierNumeroCours = dernierNumeroCours;
        this.testNiveauComplete = testNiveauComplete;
        this.dateDerniereActivite = dateDerniereActivite;
        this.userId = userId;
        this.langueId = langueId;
        this.niveauActuelId = niveauActuelId;
        this.dernierCoursCompleteId = dernierCoursCompleteId;
    }

    // Constructeur sans id
    public User_progress(int dernierNumeroCours, boolean testNiveauComplete,
                         LocalDateTime dateDerniereActivite, int userId, int langueId,
                         int niveauActuelId, int dernierCoursCompleteId) {
        this.dernierNumeroCours = dernierNumeroCours;
        this.testNiveauComplete = testNiveauComplete;
        this.dateDerniereActivite = dateDerniereActivite;
        this.userId = userId;
        this.langueId = langueId;
        this.niveauActuelId = niveauActuelId;
        this.dernierCoursCompleteId = dernierCoursCompleteId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDernierNumeroCours() { return dernierNumeroCours; }
    public void setDernierNumeroCours(int dernierNumeroCours) { this.dernierNumeroCours = dernierNumeroCours; }

    public boolean isTestNiveauComplete() { return testNiveauComplete; }
    public void setTestNiveauComplete(boolean testNiveauComplete) { this.testNiveauComplete = testNiveauComplete; }

    public LocalDateTime getDateDerniereActivite() { return dateDerniereActivite; }
    public void setDateDerniereActivite(LocalDateTime dateDerniereActivite) { this.dateDerniereActivite = dateDerniereActivite; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getLangueId() { return langueId; }
    public void setLangueId(int langueId) { this.langueId = langueId; }

    public int getNiveauActuelId() { return niveauActuelId; }
    public void setNiveauActuelId(int niveauActuelId) { this.niveauActuelId = niveauActuelId; }

    public int getDernierCoursCompleteId() { return dernierCoursCompleteId; }
    public void setDernierCoursCompleteId(int dernierCoursCompleteId) { this.dernierCoursCompleteId = dernierCoursCompleteId; }

    @Override
    public String toString() {
        return "User_progress{id=" + id + ", userId=" + userId + ", langueId=" + langueId +
                ", niveauActuelId=" + niveauActuelId + ", dernierNumeroCours=" + dernierNumeroCours + "}";
    }
}