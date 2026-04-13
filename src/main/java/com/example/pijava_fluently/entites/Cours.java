package com.example.pijava_fluently.entites;

import java.time.LocalDate;

public class Cours {
    private int id;
    private int numero;
    private String ressource;
    private LocalDate dateCreation;
    private int coursPrecedentIdId;
    private int idNiveauId;

    // Constructeur vide
    public Cours() {}

    // Constructeur complet
    public Cours(int id, int numero, String ressource, LocalDate dateCreation,
                 int coursPrecedentIdId, int idNiveauId) {
        this.id = id;
        this.numero = numero;
        this.ressource = ressource;
        this.dateCreation = dateCreation;
        this.coursPrecedentIdId = coursPrecedentIdId;
        this.idNiveauId = idNiveauId;
    }

    // Constructeur sans id
    public Cours(int numero, String ressource, LocalDate dateCreation,
                 int coursPrecedentIdId, int idNiveauId) {
        this.numero = numero;
        this.ressource = ressource;
        this.dateCreation = dateCreation;
        this.coursPrecedentIdId = coursPrecedentIdId;
        this.idNiveauId = idNiveauId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getRessource() { return ressource; }
    public void setRessource(String ressource) { this.ressource = ressource; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public int getCoursPrecedentIdId() { return coursPrecedentIdId; }
    public void setCoursPrecedentIdId(int coursPrecedentIdId) { this.coursPrecedentIdId = coursPrecedentIdId; }

    public int getIdNiveauId() { return idNiveauId; }
    public void setIdNiveauId(int idNiveauId) { this.idNiveauId = idNiveauId; }

    @Override
    public String toString() {
        return "Cours{id=" + id + ", numero=" + numero + ", dateCreation=" + dateCreation + ", idNiveauId=" + idNiveauId + "}";
    }
}