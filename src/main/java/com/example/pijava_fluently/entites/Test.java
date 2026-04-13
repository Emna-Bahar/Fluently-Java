package com.example.pijava_fluently.entites;

public class Test {
    private int id;
    private String type;
    private String titre;
    private int dureeEstimee;
    private int langueId;
    private int niveauId;

    public Test() {}

    public Test(String type, String titre, int dureeEstimee, int langueId, int niveauId) {
        this.type = type;
        this.titre = titre;
        this.dureeEstimee = dureeEstimee;
        this.langueId = langueId;
        this.niveauId = niveauId;
    }

    public Test(int id, String type, String titre, int dureeEstimee, int langueId, int niveauId) {
        this.id = id;
        this.type = type;
        this.titre = titre;
        this.dureeEstimee = dureeEstimee;
        this.langueId = langueId;
        this.niveauId = niveauId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public int getDureeEstimee() { return dureeEstimee; }
    public void setDureeEstimee(int dureeEstimee) { this.dureeEstimee = dureeEstimee; }

    public int getLangueId() { return langueId; }
    public void setLangueId(int langueId) { this.langueId = langueId; }

    public int getNiveauId() { return niveauId; }
    public void setNiveauId(int niveauId) { this.niveauId = niveauId; }

    @Override
    public String toString() {
        return titre;
    }
}