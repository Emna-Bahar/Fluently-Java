# Groupe CRUD Implementation

## Files Created

1. **Entity**: `src/main/java/com/example/pijava_fluently/services/Groupe.java`
2. **Service**: `src/main/java/com/example/pijava_fluently/services/GroupService.java`
3. **Langue Entity & Service**: For loading language options
4. **Niveau Entity & Service**: For loading level options
5. **Message Entity & Service**: For loading and sending group messages

## Recent Updates

### 🔧 Foreign Key Handling
The `GroupService.supprimer()` method now automatically deletes related messages before deleting a group, preventing foreign key constraint errors.

### 📅 Auto Date Creation
Groups now automatically set `date_creation` to current timestamp when created (using SQL `NOW()` function).

### 🌍 Dynamic Language & Level Dropdowns
- Language dropdown loads from `langue` table (only active languages)
- Level dropdown loads from `niveau` table based on selected language
- Displays actual names instead of IDs
- Automatically updates when language selection changes

### 💬 Group Conversations
- Conversation window opens per group from `groupes.fxml`
- Messages come from `message` table sorted by newest first
- `emoji_react` and `is_epingle` are not used
- Group capacity preview uses distinct message participants

## Usage Example

```java
import com.example.pijava_fluently.services.*;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        GroupService groupService = new GroupService();
        LangueService langueService = new LangueService();
        NiveauService niveauService = new NiveauService();
        
        try {
            // Get available languages
            List<Langue> langues = langueService.recupererToutesLanguesActives();
            for (Langue langue : langues) {
                System.out.println("Langue: " + langue.getNom());
            }
            
            // Get levels for a specific language
            List<Niveau> niveaux = niveauService.recupererNiveauxParLangue(1);
            for (Niveau niveau : niveaux) {
                System.out.println("Niveau: " + niveau.getTitre());
            }
            
            // CREATE - Add a new group (date_creation set automatically)
            Groupe newGroupe = new Groupe("English Beginners", "Beginners English group", 30, "actif", 1, 1);
            groupService.ajouter(newGroupe);
            
            // READ - Get all groups
            List<Groupe> allGroupes = groupService.recuperer();
            for (Groupe groupe : allGroupes) {
                System.out.println(groupe);
            }
            
            // UPDATE - Modify a group
            Groupe groupe = groupService.recupererParId(1);
            if (groupe != null) {
                groupe.setCapacite(40);
                groupService.modifier(groupe);
            }
            
            // DELETE - Remove a group (messages deleted automatically)
            groupService.supprimer(1);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

## Available Methods

### GroupService Methods:
- `ajouter(Groupe groupe)` - Add a new group (date set automatically)
- `modifier(Groupe groupe)` - Update an existing group
- `supprimer(int id)` - Delete a group (handles foreign keys)
- `recuperer()` - Get all groups
- `recupererParId(int id)` - Get a specific group by ID
- `rechercherParNom(String nom)` - Search groups by name
- `recupererParStatut(String statut)` - Filter by status
- `recupererParLangue(int idLangue)` - Filter by language
- `recupererParNiveau(int idNiveau)` - Filter by level

### LangueService Methods:
- `recupererToutesLanguesActives()` - Get all active languages
- `recupererParId(int id)` - Get language by ID

### NiveauService Methods:
- `recupererNiveauxParLangue(int idLangue)` - Get levels for a language
- `recupererParId(int id)` - Get level by ID

### MessageService Methods:
- `recupererParGroupe(int idGroupe)` - Get group messages (latest first)
- `ajouter(Message message)` - Send a new message to a group
- `compterParticipantsParGroupe(int idGroupe)` - Count distinct group participants
- `estParticipant(int idGroupe, int idUser)` - Check whether user already participated

## Database Schema

### `groupe` table:
- `id` - Auto-increment primary key
- `nom` - Group name
- `description` - Group description
- `capacite` - Group capacity (max members)
- `statut` - Group status (actif, inactif, complet)
- `date_creation` - Auto-set to NOW() on creation
- `id_langue_id` - Foreign key to langue table
- `id_niveau_id` - Foreign key to niveau table

### `langue` table:
- `id`, `nom`, `drapeau`, `description`, `popularite`, `date_ajout`, `is_active`, `update_at`

### `niveau` table:
- `id`, `titre`, `description`, `image_couverture`, `difficulte`, `ordre`, `seuil_score_max`, `seuil_score_min`, `id_langue_id`

### `message` table:
- `id`, `contenu`, `type_message`, `emoji_react`, `is_epingle`, `date_creation`, `date_modif`, `statut_message`, `id_groupe_id`, `id_user_id`
