package com.example.pijava_fluently.services;

import java.sql.Timestamp;

public class Langue {
    private int id;
    private String nom;
    private String drapeau;
    private String description;
    private String popularite;  // varchar in DB (e.g., "Haute", "Moyenne", "Basse")
    private Timestamp dateAjout;
    private boolean isActive;
    private Timestamp updateAt;

    public Langue() {
    }

    public Langue(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDrapeau() {
        return drapeau;
    }

    public void setDrapeau(String drapeau) {
        this.drapeau = drapeau;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPopularite() {
        return popularite;
    }

    public void setPopularite(String popularite) {
        this.popularite = popularite;
    }

    public Timestamp getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(Timestamp dateAjout) {
        this.dateAjout = dateAjout;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Timestamp getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Timestamp updateAt) {
        this.updateAt = updateAt;
    }

    @Override
    public String toString() {
        return nom;
    }
}
