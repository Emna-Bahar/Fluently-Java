package com.example.pijava_fluently;

import com.example.pijava_fluently.services.Groupe;
import com.example.pijava_fluently.services.GroupService;

import java.sql.SQLException;
import java.util.List;

public class TestGroupCRUD {
    
    public static void main(String[] args) {
        GroupService groupService = new GroupService();
        
        System.out.println("=== TEST CRUD GROUPES ===\n");
        
        try {
            // CREATE - Ajouter un nouveau groupe
            System.out.println("1. Ajout d'un nouveau groupe...");
            Groupe newGroupe = new Groupe("Test English Group", 
                                         "A test group for English learners", 
                                         25, 
                                         "actif", 
                                         1, 
                                         1);
            groupService.ajouter(newGroupe);
            
            // READ - Récupérer tous les groupes
            System.out.println("\n2. Liste de tous les groupes :");
            List<Groupe> allGroupes = groupService.recuperer();
            for (Groupe g : allGroupes) {
                System.out.println("   - " + g);
            }
            
            // READ - Récupérer un groupe par ID
            if (!allGroupes.isEmpty()) {
                int testId = allGroupes.get(0).getId();
                System.out.println("\n3. Récupération du groupe ID " + testId + " :");
                Groupe groupe = groupService.recupererParId(testId);
                if (groupe != null) {
                    System.out.println("   - " + groupe);
                }
                
                // UPDATE - Modifier le groupe
                System.out.println("\n4. Modification du groupe...");
                groupe.setCapacite(35);
                groupe.setStatut("inactif");
                groupService.modifier(groupe);
                
                // Vérifier la modification
                Groupe updated = groupService.recupererParId(testId);
                System.out.println("   Groupe après modification : " + updated);
            }
            
            // SEARCH - Rechercher par nom
            System.out.println("\n5. Recherche par nom 'English' :");
            List<Groupe> searchResults = groupService.rechercherParNom("English");
            System.out.println("   Résultats trouvés : " + searchResults.size());
            
            // FILTER - Récupérer par statut
            System.out.println("\n6. Groupes avec statut 'actif' :");
            List<Groupe> activeGroups = groupService.recupererParStatut("actif");
            System.out.println("   Groupes actifs : " + activeGroups.size());
            
            System.out.println("\n=== TEST TERMINÉ AVEC SUCCÈS ===");
            
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
