package com.example.pijava_fluently.entites;

import java.time.LocalDateTime;

public class Session {

    private int id;
    private LocalDateTime dateHeure;
    private String statut;
    private String lienReunion;
    private int idGroupId;
    private int idUserId;          // professeur
    private Integer rating;
    private Integer duree;         // en minutes
    private Double prix;
    private String description;
    private Integer capaciteMax;
    private String nom;
    // ── Constructeur vide ──────────────────────────────────────────
    public Session() {}

    // ── Constructeur sans id ───────────────────────────────────────
    public Session(LocalDateTime dateHeure, String statut, String lienReunion,
                   int idGroupId, int idUserId, Integer duree, Double prix,
                   String description, Integer capaciteMax, String nom) {
        this.dateHeure    = dateHeure;
        this.statut       = statut;
        this.lienReunion  = lienReunion;
        this.idGroupId    = idGroupId;
        this.idUserId     = idUserId;
        this.duree        = duree;
        this.prix         = prix;
        this.description  = description;
        this.capaciteMax  = capaciteMax;
        this.nom          = nom;
    }

    // ── Constructeur complet ───────────────────────────────────────
    public Session(int id, LocalDateTime dateHeure, String statut, String lienReunion,
                   int idGroupId, int idUserId, Integer rating, Integer duree,
                   Double prix, String description, Integer capaciteMax) {
        this.id          = id;
        this.dateHeure   = dateHeure;
        this.statut      = statut;
        this.lienReunion = lienReunion;
        this.idGroupId   = idGroupId;
        this.idUserId    = idUserId;
        this.rating      = rating;
        this.duree       = duree;
        this.prix        = prix;
        this.description = description;
        this.capaciteMax = capaciteMax;
    }

    // ── Getters & Setters ──────────────────────────────────────────
    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public LocalDateTime getDateHeure()             { return dateHeure; }
    public void setDateHeure(LocalDateTime d)       { this.dateHeure = d; }
    public String getStatut()                       { return statut; }
    public void setStatut(String statut)            { this.statut = statut; }
    public String getLienReunion()                  { return lienReunion; }
    public void setLienReunion(String l)            { this.lienReunion = l; }
    public int getIdGroupId()                       { return idGroupId; }
    public void setIdGroupId(int id)                { this.idGroupId = id; }
    public int getIdUserId()                        { return idUserId; }
    public void setIdUserId(int id)                 { this.idUserId = id; }
    public Integer getRating()                      { return rating; }
    public void setRating(Integer rating)           { this.rating = rating; }
    public Integer getDuree()                       { return duree; }
    public void setDuree(Integer duree)             { this.duree = duree; }
    public Double getPrix()                         { return prix; }
    public void setPrix(Double prix)                { this.prix = prix; }
    public String getDescription()                  { return description; }
    public void setDescription(String d)            { this.description = d; }
    public Integer getCapaciteMax()                 { return capaciteMax; }
    public void setCapaciteMax(Integer c)           { this.capaciteMax = c; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    @Override
    public String toString() {
        return "Session{id=" + id + ", statut='" + statut + "', dateHeure=" + dateHeure + "}";
    }
}