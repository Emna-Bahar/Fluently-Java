package com.example.pijava_fluently.entites;

import java.time.LocalDateTime;

public class TestPassage {
    private int id;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private double resultat;
    private int score;
    private int scoreMax;
    private String statut;
    private int tempsPasse;
    private int testId;
    private int userId;

    public TestPassage() {}

    public TestPassage(LocalDateTime dateDebut, LocalDateTime dateFin, double resultat,
                       int score, int scoreMax, String statut, int tempsPasse,
                       int testId, int userId) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.resultat = resultat;
        this.score = score;
        this.scoreMax = scoreMax;
        this.statut = statut;
        this.tempsPasse = tempsPasse;
        this.testId = testId;
        this.userId = userId;
    }

    public TestPassage(int id, LocalDateTime dateDebut, LocalDateTime dateFin, double resultat,
                       int score, int scoreMax, String statut, int tempsPasse,
                       int testId, int userId) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.resultat = resultat;
        this.score = score;
        this.scoreMax = scoreMax;
        this.statut = statut;
        this.tempsPasse = tempsPasse;
        this.testId = testId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public double getResultat() { return resultat; }
    public void setResultat(double resultat) { this.resultat = resultat; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getScoreMax() { return scoreMax; }
    public void setScoreMax(int scoreMax) { this.scoreMax = scoreMax; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getTempsPasse() { return tempsPasse; }
    public void setTempsPasse(int tempsPasse) { this.tempsPasse = tempsPasse; }
    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String toString() { return "Passage #" + id + " — " + statut; }
}