package com.example.pijava_fluently.services;

public class Niveau {
    private int id;
    private String titre;
    private String description;
    private String imageCouverture;
    private String difficulte;  // varchar in DB
    private int ordre;
    private int seuilScoreMax;
    private int seuilScoreMin;
    private int idLangueId;

    public Niveau() {
    }

    public Niveau(int id, String titre) {
        this.id = id;
        this.titre = titre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageCouverture() {
        return imageCouverture;
    }

    public void setImageCouverture(String imageCouverture) {
        this.imageCouverture = imageCouverture;
    }

    public String getDifficulte() {
        return difficulte;
    }

    public void setDifficulte(String difficulte) {
        this.difficulte = difficulte;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public int getSeuilScoreMax() {
        return seuilScoreMax;
    }

    public void setSeuilScoreMax(int seuilScoreMax) {
        this.seuilScoreMax = seuilScoreMax;
    }

    public int getSeuilScoreMin() {
        return seuilScoreMin;
    }

    public void setSeuilScoreMin(int seuilScoreMin) {
        this.seuilScoreMin = seuilScoreMin;
    }

    public int getIdLangueId() {
        return idLangueId;
    }

    public void setIdLangueId(int idLangueId) {
        this.idLangueId = idLangueId;
    }

    @Override
    public String toString() {
        return titre;
    }
}
