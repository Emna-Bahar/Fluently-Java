package com.example.pijava_fluently.entites;

public class Niveau {
    private int id;
    private String titre;
    private String description;
    private String imageCouverture;
    private String difficulte;
    private int ordre;
    private double seuilScoreMax;
    private double seuilScoreMin;
    private int idLangueId; // FK → langue

    // Constructeur vide
    public Niveau() {}

    // Constructeur complet
    public Niveau(int id, String titre, String description, String imageCouverture,
                  String difficulte, int ordre, double seuilScoreMax, double seuilScoreMin, int idLangueId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.imageCouverture = imageCouverture;
        this.difficulte = difficulte;
        this.ordre = ordre;
        this.seuilScoreMax = seuilScoreMax;
        this.seuilScoreMin = seuilScoreMin;
        this.idLangueId = idLangueId;
    }

    // Constructeur sans id
    public Niveau(String titre, String description, String imageCouverture,
                  String difficulte, int ordre, double seuilScoreMax, double seuilScoreMin, int idLangueId) {
        this.titre = titre;
        this.description = description;
        this.imageCouverture = imageCouverture;
        this.difficulte = difficulte;
        this.ordre = ordre;
        this.seuilScoreMax = seuilScoreMax;
        this.seuilScoreMin = seuilScoreMin;
        this.idLangueId = idLangueId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageCouverture() { return imageCouverture; }
    public void setImageCouverture(String imageCouverture) { this.imageCouverture = imageCouverture; }

    public String getDifficulte() { return difficulte; }
    public void setDifficulte(String difficulte) { this.difficulte = difficulte; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public double getSeuilScoreMax() { return seuilScoreMax; }
    public void setSeuilScoreMax(double seuilScoreMax) { this.seuilScoreMax = seuilScoreMax; }

    public double getSeuilScoreMin() { return seuilScoreMin; }
    public void setSeuilScoreMin(double seuilScoreMin) { this.seuilScoreMin = seuilScoreMin; }

    public int getIdLangueId() { return idLangueId; }
    public void setIdLangueId(int idLangueId) { this.idLangueId = idLangueId; }

    @Override
    public String toString() {
        return "Niveau{id=" + id + ", titre='" + titre + "', difficulte='" + difficulte + "', ordre=" + ordre + "}";
    }
}