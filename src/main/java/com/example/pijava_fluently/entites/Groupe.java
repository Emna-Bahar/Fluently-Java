package com.example.pijava_fluently.entites;

import java.sql.Timestamp;

public class Groupe {
    private int id;
    private String nom;
    private String description;
    private int capacite;
    private String statut;
    private Timestamp dateCreation;
    private int idLangueId;
    private int idNiveauId;

    public Groupe() {
    }

    public Groupe(int id, String nom, String description, int capacite, String statut,
                  Timestamp dateCreation, int idLangueId, int idNiveauId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.capacite = capacite;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.idLangueId = idLangueId;
        this.idNiveauId = idNiveauId;
    }

    public Groupe(String nom, String description, int capacite, String statut,
                  int idLangueId, int idNiveauId) {
        this.nom = nom;
        this.description = description;
        this.capacite = capacite;
        this.statut = statut;
        this.idLangueId = idLangueId;
        this.idNiveauId = idNiveauId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getIdLangueId() {
        return idLangueId;
    }

    public void setIdLangueId(int idLangueId) {
        this.idLangueId = idLangueId;
    }

    public int getIdNiveauId() {
        return idNiveauId;
    }

    public void setIdNiveauId(int idNiveauId) {
        this.idNiveauId = idNiveauId;
    }

    @Override
    public String toString() {
        return "Groupe{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", capacite=" + capacite +
                ", statut='" + statut + '\'' +
                ", dateCreation=" + dateCreation +
                ", idLangueId=" + idLangueId +
                ", idNiveauId=" + idNiveauId +
                '}';
    }
}