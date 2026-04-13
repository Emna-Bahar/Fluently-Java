package com.example.pijava_fluently.entites;

public class Tache {

    private int id;
    private String titre;
    private String description;
    private java.time.LocalDate dateLimite;
    private String statut;
    private String priorite;
    private int idObjectifId;

    public Tache() {}

    public Tache(String titre, String description, java.time.LocalDate dateLimite,
                 String statut, String priorite, int idObjectifId) {
        this.titre        = titre;
        this.description  = description;
        this.dateLimite   = dateLimite;
        this.statut       = statut;
        this.priorite     = priorite;
        this.idObjectifId = idObjectifId;
    }

    public Tache(int id, String titre, String description, java.time.LocalDate dateLimite,
                 String statut, String priorite, int idObjectifId) {
        this.id           = id;
        this.titre        = titre;
        this.description  = description;
        this.dateLimite   = dateLimite;
        this.statut       = statut;
        this.priorite     = priorite;
        this.idObjectifId = idObjectifId;
    }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getTitre()                        { return titre; }
    public void setTitre(String titre)              { this.titre = titre; }
    public String getDescription()                  { return description; }
    public void setDescription(String d)            { this.description = d; }
    public java.time.LocalDate getDateLimite()      { return dateLimite; }
    public void setDateLimite(java.time.LocalDate d){ this.dateLimite = d; }
    public String getStatut()                       { return statut; }
    public void setStatut(String statut)            { this.statut = statut; }
    public String getPriorite()                     { return priorite; }
    public void setPriorite(String priorite)        { this.priorite = priorite; }
    public int getIdObjectifId()                    { return idObjectifId; }
    public void setIdObjectifId(int id)             { this.idObjectifId = id; }

    @Override
    public String toString() {
        return "Tache{id=" + id + ", titre='" + titre + "', statut='" + statut + "', priorite='" + priorite + "'}";
    }
}