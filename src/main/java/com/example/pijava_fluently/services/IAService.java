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
}