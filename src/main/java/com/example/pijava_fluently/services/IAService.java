package com.example.pijava_fluently.services;

public class IAService {

    // Choisissez votre API ici
    private final MistralService mistralService = new MistralService();

    // Ou gardez OpenAI comme backup
    // private final OpenAIService openAIService = new OpenAIService();

    public String genererCours(String langue, String theme, String grammaire,
                               String vocabulaire, int niveauDifficulte) {

        // Utiliser Mistral
        return mistralService.genererCours(langue, theme, grammaire, vocabulaire, niveauDifficulte);

        // Ou fallback : si Mistral échoue, utiliser OpenAI
        // try {
        //     return mistralService.genererCours(...);
        // } catch (Exception e) {
        //     return openAIService.genererCours(...);
        // }
    }
    public String chercherDefinition(String mot, String langue) {
        return mistralService.chercherDefinition(mot, langue);
    }
    // Ajoutez cette méthode dans IAService.java
    public String genererFlashcards(String langue, String promptUtilisateur, String niveau) {
        return mistralService.genererFlashcards(langue, promptUtilisateur, niveau);
    }
    // Ajoutez cette méthode
    public String explorerCulture(String pays, String categorie, String langue) {
        return mistralService.explorerCulture(pays, categorie, langue);
    }
    public String genererPuzzleEtymologique(String langue, String niveau) {
        return mistralService.genererPuzzleEtymologique(langue, niveau);
    }
}