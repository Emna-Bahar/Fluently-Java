package com.example.pijava_fluently.models;

public class User {

    private int    id;
    private String email;
    private String nom;      // this is the "username"
    private String prenom;
    private String statut;
    private String password;
    private String roles;
    private String faceDescriptor;

    public User() {}

    public int    getId()             { return id; }
    public String getEmail()          { return email; }
    public String getNom()            { return nom; }
    public String getPrenom()         { return prenom; }
    public String getStatut()         { return statut; }
    public String getPassword()       { return password; }
    public String getRoles()          { return roles; }
    public String getFaceDescriptor() { return faceDescriptor; }

    public void setId(int id)                  { this.id = id; }
    public void setEmail(String email)         { this.email = email; }
    public void setNom(String nom)             { this.nom = nom; }
    public void setPrenom(String prenom)       { this.prenom = prenom; }
    public void setStatut(String statut)       { this.statut = statut; }
    public void setPassword(String password)   { this.password = password; }
    public void setRoles(String roles)         { this.roles = roles; }
    public void setFaceDescriptor(String fd)   { this.faceDescriptor = fd; }

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    @Override
    public String toString() {
        return prenom + " " + nom + " <" + email + ">"; 
    }
}
