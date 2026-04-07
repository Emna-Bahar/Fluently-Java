package com.example.pijava_fluently.entites;

import java.time.LocalDate;

public class Objectif {

    private int id;
    private String titre;
    private String description;
    private LocalDate dateDeb;
    private LocalDate dateFin;
    private String statut;
    private int idUserId;

    // ── Constructeur vide ──────────────────────────────────────────
    public Objectif() {}

    // ── Constructeur complet ───────────────────────────────────────
    public Objectif(int id, String titre, String description,
                    LocalDate dateDeb, LocalDate dateFin,
                    String statut, int idUserId) {
        this.id          = id;
        this.titre       = titre;
        this.description = description;
        this.dateDeb     = dateDeb;
        this.dateFin     = dateFin;
        this.statut      = statut;
        this.idUserId    = idUserId;
    }

    // ── Constructeur sans id ───────────────────────────────────────
    public Objectif(String titre, String description,
                    LocalDate dateDeb, LocalDate dateFin,
                    String statut, int idUserId) {
        this.titre       = titre;
        this.description = description;
        this.dateDeb     = dateDeb;
        this.dateFin     = dateFin;
        this.statut      = statut;
        this.idUserId    = idUserId;
    }

    // ── Getters & Setters ──────────────────────────────────────────
    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getTitre()                { return titre; }
    public void setTitre(String titre)      { this.titre = titre; }

    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }

    public LocalDate getDateDeb()                    { return dateDeb; }
    public void setDateDeb(LocalDate dateDeb)        { this.dateDeb = dateDeb; }

    public LocalDate getDateFin()                    { return dateFin; }
    public void setDateFin(LocalDate dateFin)        { this.dateFin = dateFin; }

    public String getStatut()               { return statut; }
    public void setStatut(String statut)    { this.statut = statut; }

    public int getIdUserId()                { return idUserId; }
    public void setIdUserId(int idUserId)   { this.idUserId = idUserId; }

    @Override
    public String toString() {
        return "Objectif{id=" + id + ", titre='" + titre + "', statut='" + statut
                + "', dateDeb=" + dateDeb + ", dateFin=" + dateFin + "}";
    }
}