package com.example.pijava_fluently.entites;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class Langue {
    private int id;
    private String nom;
    private String drapeau;
    private LocalDateTime updatedAt;
    private String description;
    private String popularite;
    private LocalDate dateAjout;
    private boolean isActive;

    // Constructeur vide
    public Langue() {}

    // Constructeur complet
    public Langue(int id, String nom, String drapeau, LocalDateTime updatedAt,
                  String description, String popularite, LocalDate dateAjout, boolean isActive) {
        this.id = id;
        this.nom = nom;
        this.drapeau = drapeau;
        this.updatedAt = updatedAt;
        this.description = description;
        this.popularite = popularite;
        this.dateAjout = dateAjout;
        this.isActive = isActive;
    }

    // Constructeur sans id (pour insertion)
    public Langue(String nom, String drapeau, LocalDateTime updatedAt,
                  String description, String popularite, LocalDate dateAjout, boolean isActive) {
        this.nom = nom;
        this.drapeau = drapeau;
        this.updatedAt = updatedAt;
        this.description = description;
        this.popularite = popularite;
        this.dateAjout = dateAjout;
        this.isActive = isActive;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDrapeau() { return drapeau; }
    public void setDrapeau(String drapeau) { this.drapeau = drapeau; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPopularite() { return popularite; }
    public void setPopularite(String popularite) { this.popularite = popularite; }

    public LocalDate getDateAjout() { return dateAjout; }
    public void setDateAjout(LocalDate dateAjout) { this.dateAjout = dateAjout; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "Langue{id=" + id + ", nom='" + nom + "', popularite='" + popularite + "', isActive=" + isActive + "}";
    }
}