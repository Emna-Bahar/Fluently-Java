# 📝 Fluently — Module Test / Question / Réponse / Test Passage
> Application desktop JavaFX — Apprentissage des langues par les tests

---

## 📌 Présentation du module

Ce module est la partie **évaluation et progression** de l'application Fluently, une plateforme d'apprentissage des langues étrangères (Français, Anglais, Espagnol). Il permet à un étudiant de passer des tests, d'être évalué, de progresser de niveau en niveau, et de recevoir un certificat officiel CECRL.

Il a été développé en **JavaFX 17** (IntelliJ IDEA + SceneBuilder), connecté à la même base de données MySQL que l'application web Symfony existante.

---

## 🗂️ Structure du projet

```
com/example/pijava_fluently/
├── entites/
│   ├── Test.java               → un test (type, titre, durée, langue, niveau)
│   ├── Question.java           → une question (énoncé, type, scoreMax)
│   ├── Reponse.java            → une réponse QCM (contenu, isCorrect, score)
│   ├── TestPassage.java        → un passage de test (score, statut, temps)
│   ├── User.java               → un utilisateur (email, prénom, rôle)
│   ├── Langue.java             → une langue (nom, drapeau)
│   ├── Niveau.java             → un niveau CECRL (difficulté, ordre)
│   └── User_progress.java      → la progression par langue (niveauActuelId)
│
├── services/
│   ├── TestService.java
│   ├── QuestionService.java
│   ├── ReponseService.java
│   ├── TestPassageService.java
│   ├── LangueService.java
│   ├── NiveauService.java
│   ├── UserProgressService.java
│   ├── SpeechRecognitionService.java   → enregistrement micro + Whisper API
│   ├── SpeechEvaluationService.java    → algorithme Levenshtein
│   ├── AITextCorrectionService.java    → correction texte libre par Groq LLaMA
│   ├── AIQuizGeneratorService.java     → génération de questions par IA
│   ├── FraudeTrackerService.java       → suivi infractions mode examen (JSON)
│   ├── LeaderboardService.java         → classement des duels
│   ├── ExamModeService.java            → détection mode examen
│   ├── CertificatService.java          → génération PDF + QR code
│   ├── DuelServer.java                 → ServerSocket port 9090
│   └── DuelClient.java                 → connexion socket au serveur
│
├── controller/
│   ├── TestController.java             → CRUD tests (admin)
│   ├── QuestionController.java         → CRUD questions + génération IA
│   ├── ReponseController.java          → CRUD réponses
│   ├── TestPassageController.java      → lecture passages + export CSV (admin)
│   ├── TestPassageEtudiantController.java  → passer un test (étudiant)
│   ├── MesTestsController.java         → liste des tests par langue/niveau
│   ├── ModeHistoireController.java     → mode quiz gamifié avec personnage
│   ├── CertificatController.java       → popup + téléchargement PDF
│   ├── DuelLobbyController.java        → créer/rejoindre un duel
│   ├── DuelGameController.java         → jeu en duel temps réel
│   └── FraudeAdminController.java      → tableau de bord infractions (admin)
│
├── fxml/
│   ├── tests.fxml
│   ├── questions.fxml
│   ├── reponses.fxml
│   ├── passages.fxml
│   ├── mes-tests.fxml
│   ├── test-passage.fxml
│   ├── mode-histoire.fxml
│   ├── duel-lobby.fxml
│   ├── duel-game.fxml
│   └── fraude-admin.fxml
│
└── utils/
    ├── MyDatabase.java     → Singleton connexion MySQL
    ├── LoggerUtil.java     → logs formatés (INFO / WARN / ERROR)
    ├── ConfigLoader.java   → lecture config.properties
    └── HttpClientUtil.java → appels HTTP vers les APIs externes
```

---

## 🗄️ Base de données

**Nom :** `fluently`

```sql
test         (id, type, titre, duree_estimee, langue_id, niveau_id)
question     (id, enonce, type, score_max, id_test_id)
reponse      (id, contenu_rep, is_correct, score, date_reponse, id_question_id)
test_passage (id, date_debut, date_fin, resultat, score, score_max,
              statut, temps_passe, test_id, user_id)
user         (id, email, nom, prenom, roles, statut, password)
langue       (id, nom, drapeau, description, popularite, is_active)
niveau       (id, titre, description, difficulte, ordre,
              seuil_score_max, seuil_score_min, id_langue_id)
user_progress(id, dernier_numero_cours, test_niveau_complete,
              date_derniere_activite, user_id, langue_id,
              niveau_actuel_id, dernier_cours_complete_id)
```

### Types de tests
| Type | Rôle |
|------|------|
| `"Test de niveau"` | Premier test pour déterminer le niveau CECRL de l'étudiant |
| `"quiz_debutant"` | Exercice d'entraînement du niveau actuel |
| `"Test de fin de niveau"` | Valide le passage au niveau supérieur |

### Types de questions
| Type | Description |
|------|-------------|
| `qcm` | Choix multiple avec RadioButton |
| `oral` | L'étudiant parle → transcrit par Whisper |
| `texte_libre` | L'étudiant écrit → noté par Groq LLaMA |

### Statuts de TestPassage
| Statut | Signification |
|--------|--------------|
| `termine` | Test complété normalement |
| `annule` | Annulé par infractions (mode examen) |
| `duel_gagne` | Victoire en duel |
| `duel_perdu` | Défaite en duel |
| `duel_egalite` | Égalité en duel |

---

## ⚙️ Configuration

Créer le fichier `src/main/resources/config.properties` :

```properties
groq.api.key=gsk_VOTRE_CLE_API_GROQ
ai.model=llama-3.3-70b-versatile
exam.maxInfractions=3
```

> ⚠️ Ne jamais committer ce fichier sur GitHub — ajouter à `.gitignore`

---

## 📦 Dépendances (pom.xml)

```xml
<!-- JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.2</version>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Jackson (JSON) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- iTextPDF (certificats) -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
    <version>5.5.13.3</version>
</dependency>

<!-- ZXing (QR code) -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.2</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
```

---

## 🚀 Fonctionnalités implémentées

### 1. 🎯 Passage de test (TestPassageEtudiantController)

Gestion complète du passage d'un test avec trois types de questions :

- **QCM** : RadioButton avec ToggleGroup, sélection unique, feedback immédiat
- **Oral** : Enregistrement micro via `javax.sound.sampled`, transcription par **Whisper API (Groq)**, évaluation par algorithme de Levenshtein (similarité ≥ 85% = correct, ≥ 60% = partiel)
- **Texte libre** : TextArea + correction par **Groq LLaMA** selon grille DELF (11 critères, score 0-100)

Timer configurable (`duree_estimee` en minutes), soumission automatique à expiration.

### 2. 📚 Système de progression (MesTestsController)

La progression de l'étudiant est lue directement depuis `user_progress` :

```
"Test de niveau"        → toujours accessible (porte d'entrée)
Pas de niveau encore    → tout bloqué dans cette langue
"quiz_debutant"         → accessible si niveauTest == niveauActuel
"Test de fin de niveau" → accessible si niveauTest == niveauActuel
```

Après un **"Test de niveau"** → le niveau CECRL est calculé depuis le score et inséré dans `user_progress`.

Après un **"Test de fin de niveau"** réussi (≥ 50%) → `user_progress.niveau_actuel_id` passe au niveau suivant (ordre + 1 dans la même langue).

### 3. 🏆 Certificats PDF (CertificatService + CertificatController)

Génération automatique d'un certificat PDF après réussite d'un "Test de fin de niveau" :

- Mise en page officielle (fond violet, double cadre doré, badge CECRL)
- **QR code auto-contenu** : encode nom, niveau, langue, score, date, UUID — lisible sans serveur
- Sauvegarde locale dans `certificats/`
- FileChooser pour choisir l'emplacement de sauvegarde
- Ouverture automatique dans le lecteur PDF

### 4. 🎮 Mode Histoire (ModeHistoireController + PersonnageSVG)

Mode activé automatiquement pour les `quiz_debutant` avec uniquement des questions QCM.

- **Personnages SVG animés** dessinés entièrement en Java Canvas (aucune image externe) :
    - 🇫🇷 **Pierre** — béret bleu marine, foulard rouge, moustache française
    - 🇬🇧 **James** — chapeau melon, monocle doré, favoris
    - 🇪🇸 **Carlos** — sombrero de matador, veste rouge brodée
- **Expressions réactives** : CONTENT (sourire + saut), TRISTE (arc inversé + frisson), NEUTRE, SURPRISE
- **Animations** : clignement toutes les 3 secondes, respiration continue
- **Bulle de dialogue** avec effet machine à écrire (lettre par lettre)
- Feedback immédiat après chaque réponse (boutons colorés vert/rouge)

### 5. ⚔️ Mode Duel (DuelServer + DuelClient + DuelGameController)

Duel en temps réel entre deux étudiants via **Java Sockets** :

```
Hôte  → DuelServer (ServerSocket port 9090)
Client → DuelClient (Socket → IP de l'hôte)
```

**Messages échangés (DuelMessage) :**
| Action | Contenu | Sens |
|--------|---------|------|
| `NAME` | prénom du joueur | bidirectionnel |
| `QUESTIONS` | liste complète des questions + scoreMax | hôte → client |
| `ANSWER` | index, isCorrect, scoreObtenu | chaque joueur → l'autre |
| `FINISHED` | scoreFinal | chaque joueur → l'autre |
| `END` | winnerName, scores | hôte → client |

**Particularité texte libre en duel :** le score IA est calculé UNE SEULE FOIS côté répondant et transmis via `scoreObtenu` — l'adversaire l'utilise directement sans recalculer, évitant les écarts dus à la non-déterminisme de l'IA.

**Anti-doublon leaderboard :** vérification SQL sur les 5 dernières secondes pour éviter la double insertion quand les deux joueurs sont sur le même PC.

**Détection IP fiable :**
```java
DatagramSocket socket = new DatagramSocket();
socket.connect(InetAddress.getByName("8.8.8.8"), 80);
return socket.getLocalAddress().getHostAddress();
```

### 6. 🏅 Leaderboard (LeaderboardService)

Classement calculé depuis `test_passage` avec statuts `duel_gagne/duel_perdu/duel_egalite` :

```sql
SELECT u.prenom, u.nom,
       COUNT(*) as duels_joues,
       SUM(CASE WHEN statut='duel_gagne' THEN 1 ELSE 0 END) as duels_gagnes,
       SUM(CASE WHEN statut='duel_gagne' THEN score ELSE 0 END) as total_points
FROM test_passage tp JOIN user u ON tp.user_id = u.id
WHERE tp.statut IN ('duel_gagne','duel_perdu','duel_egalite')
GROUP BY u.id ORDER BY duels_gagnes DESC, total_points DESC
```

Affichage : podium visuel TOP 3 + tableau complet avec ligne surlignée pour l'utilisateur courant.

### 7. 🔒 Mode Examen + Anti-fraude (ExamModeService + FraudeTrackerService)

Activé automatiquement pour les **"Test de fin de niveau"**.

**Détections :**
- Perte de focus (changement de fenêtre)
- Tentative de quitter le plein écran
- Ctrl+C / Ctrl+V / Ctrl+X
- F12
- Clic droit

**Système progressif :**
| Infractions passées | Tentatives autorisées |
|--------------------|-----------------------|
| 0 à 2 | 3 |
| 3 à 5 | 2 |
| 6 et + | 1 |

**Toast non-bloquant :** avertissement affiché 3 secondes sans interrompre le test, évitant la boucle infinie (alerte → perte focus → nouvelle alerte).

**Stockage :** fichiers JSON locaux `fraude_logs/user_{id}.json` (pas en base de données).

### 8. 🤖 Génération de questions par IA (AIQuizGeneratorService)

L'admin peut générer des questions automatiquement depuis `QuestionController` :

- **Thème** libre, **langue**, **niveau CECRL**, **nombre** (1-10), **type** (qcm / oral / texte_libre / mixte)
- Appel à **Groq LLaMA** avec prompt structuré
- Prévisualisation avant insertion en base
- Validation : au moins 1 bonne réponse par QCM
- Parsing robuste : Jackson en priorité, fallback regex si le JSON est malformé

### 9. 📊 Dashboard Admin (TestPassageController)

- 4 cartes statistiques : total passages, taux de réussite, score moyen, temps moyen
- Barre de progression du taux de réussite
- **Export CSV** UTF-8 avec BOM (compatible Excel) via FileChooser
- Colonnes : ID, Test, Étudiant, Score, ScoreMax, %, Statut, Temps, Dates

---

## 🔧 Patterns techniques

### Singleton MyDatabase
```java
// Une seule connexion MySQL pour toute l'application
MyDatabase.getInstance().getConnection()

// Reconnexion si la connexion a expiré (après 8h d'inactivité MySQL)
if (conn == null || conn.isClosed() || !conn.isValid(2)) {
    MyDatabase.getInstance().reconnect();
}
```

> ⚠️ Ne jamais utiliser `try-with-resources` sur la connexion Singleton — seulement sur le `PreparedStatement`. Fermer la connexion couperait tous les services suivants.

### INSERT OR UPDATE (ON DUPLICATE KEY)
```sql
INSERT INTO user_progress (user_id, langue_id, niveau_actuel_id, ...)
VALUES (?, ?, ?, ...)
ON DUPLICATE KEY UPDATE
    niveau_actuel_id = VALUES(niveau_actuel_id),
    date_derniere_activite = NOW()
```
Utilisé pour `user_progress` qui a une contrainte unique sur `(user_id, langue_id)`.

### Platform.runLater()
Toute mise à jour de l'interface depuis un thread secondaire (IA, réseau, audio) passe obligatoirement par :
```java
Platform.runLater(() -> { labelX.setText("..."); });
```

### Threads daemon pour les sockets
```java
Thread t = new Thread(...);
t.setDaemon(true); // s'arrête automatiquement quand l'app ferme
t.start();
```

---

## 🧪 Logique de scoring

### QCM
```
Bonne réponse → scoreMax de la question
Mauvaise réponse → 0
```

### Oral (Levenshtein)
```
Similarité ≥ 85% → correct  → 100% du scoreMax
Similarité ≥ 60% → partial  → 50%  du scoreMax
Similarité < 60% → incorrect → 0
```

### Texte libre (IA)
```
Score IA (0-100) → (iaScore / 100.0) × scoreMax
Ex: iaScore=75, scoreMax=5 → 3.75 points
```

### Score → Niveau CECRL
```
≥ 90% → C2 | ≥ 80% → C1 | ≥ 70% → B2
≥ 60% → B1 | ≥ 50% → A2 | < 50% → A1
```

---

## 📁 Fichiers générés à la racine du projet

```
certificats/          → PDFs des certificats générés
fraude_logs/          → fichiers JSON d'infractions par utilisateur
  └── user_9.json
```

---

## 🌐 APIs externes utilisées

| API | Usage | Endpoint |
|-----|-------|----------|
| **Groq Whisper** | Transcription audio → texte | `POST /openai/v1/audio/transcriptions` |
| **Groq LLaMA** | Correction texte libre + génération questions | `POST /openai/v1/chat/completions` |

---

## 🔑 Règles métier essentielles

1. **`user_progress` est la source de vérité du niveau** — jamais recalculé depuis les passages
2. **Le certificat affiche le niveau du test validé**, pas le niveau après promotion
3. **Mode histoire = quiz_debutant + toutes questions QCM uniquement**
4. **Mode examen = Test de fin de niveau uniquement** (plein écran forcé)
5. **Score texte libre en duel = calculé côté répondant, transmis via socket** (pas recalculé)
6. **Anti-doublon leaderboard = vérification sur les 5 dernières secondes**
7. **Un étudiant DOIT passer "Test de niveau" avant d'accéder au reste** dans chaque langue

---

## 👥 Équipe

| Membre | Module |
|--------|--------|
| **[Ton prénom]** | Test / Question / Réponse / TestPassage |
| Camarade | Langue / Cours / Niveau / UserProgress |
| Camarade | Groupe / Message / Session |
| Camarade | User / Objectif / Tâche |

---

# 📝 Fluently — Module Langue / Niveau / Cours / CoursProgress
> Application desktop JavaFX — Apprentissage des langues par les cours et la progression

---

## 📌 Présentation du module

Ce module est la partie gestion de contenu pédagogique et progression de l'application Fluently. Il permet à un administrateur de gérer les langues, les niveaux CECRL et les cours, et à un étudiant de suivre son apprentissage, de consulter des cours, générer du contenu personnalisé par IA, et suivre sa progression.

Développé en JavaFX 17 (IntelliJ IDEA + SceneBuilder), connecté à la même base de données MySQL que l'application web Symfony existante.

---

## 🗂️ Structure du projet

```
com/example/pijava_fluently/
├── entites/
│   ├── Langue.java             → une langue (nom, drapeau, popularité)
│   ├── Niveau.java             → un niveau CECRL (A1 à C2, ordre, image)
│   ├── Cours.java              → un cours (numéro, ressources, date)
│   ├── User_progress.java      → progression par langue (cours complétés)
│   └── User.java               → utilisateur (email, prénom, rôle)
│
├── services/
│   ├── LangueService.java      → CRUD langues
│   ├── NiveauService.java      → CRUD niveaux
│   ├── CoursService.java       → CRUD cours
│   ├── UserProgressService.java → gestion progression
│   ├── WikipediaService.java   → données culturelles (locuteurs, pays...)
│   ├── MistralService.java     → IA : génération cours, flashcards, quiz, dictionnaire
│   ├── YouTubeService.java     → recherche vidéos YouTube
│   └── PrononciationService.java → synthèse vocale
│
├── controller/
│   ├── LangueController.java           → CRUD langues (admin)
│   ├── LanguesEtudiantController.java  → affichage langues (étudiant)
│   ├── LangueStatsController.java      → statistiques par langue
│   ├── NiveauController.java           → CRUD niveaux (admin)
│   ├── CoursController.java            → CRUD cours (admin)
│   ├── ApprentissageController.java    → cœur étudiant (cours, IA, quiz, flashcards)
│   └── UserProgressController.java     → tableau de bord progression (admin)
│
├── fxml/
│   ├── langues.fxml
│   ├── langues-etudiant.fxml
│   ├── langue-stats.fxml
│   ├── niveaux.fxml
│   ├── cours.fxml
│   ├── apprentissage.fxml
│   └── user-progress.fxml
│
└── utils/
    ├── MyDatabase.java     → Singleton connexion MySQL
    ├── ConfigLoader.java   → lecture config.properties (clés API)
```

---

## 🗄️ Base de données

**Nom :** `fluently`

```sql
langue       (id, nom, drapeau, description, popularite, is_active, date_ajout, updated_at)
niveau       (id, titre, description, image_couverture, difficulte, ordre,seuil_score_max, seuil_score_min, id_langue_id)
cours        (id, numero, ressource, date_creation, cours_precedent_id, id_niveau_id)
user_progress(id, dernier_numero_cours, test_niveau_complete, date_derniere_activite,
              user_id, langue_id, niveau_actuel_id, dernier_cours_complete_id)
user         (id, email, nom, prenom, roles, statut, password)
```

### Popularités de langue
| Type      | Rôle |
|------     |------|
|très haute |Langue très populaire (ex: Anglais)
|haute	    |Langue populaire (ex: Espagnol)
|moyenne	|Popularité moyenne
|faible	    |Peu étudiée

### Difficulté de niveau
| Type | Description |
|------|-------------|
|Difficulté| Ordre typique |Description
|------    |-------------  |
|A1	        1	            Débutant
|A2	        2	            Élémentaire
|B1	        3	            Intermédiaire
|B2	        4	            Intermédiaire avancé
|C1	        5	            Avancé
|C2	        6	            Maîtrise

---

## ⚙️ Configuration

Créer le fichier `src/main/resources/config.properties` :

```properties
# Clé API Mistral 
mistral.api.key=VOTRE_CLE_API_MISTRAL
# Clé API YouTube 
youtube.api.key=VOTRE_CLE_API_YOUTUBE
```

> ⚠️ Ne jamais committer ce fichier sur GitHub — ajouter à `.gitignore`

---

## 📦 Dépendances (pom.xml)

```xml
<!-- JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.2</version>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Gson (JSON) -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- OkHttp (appels API) -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.11.0</version>
</dependency>

<!-- iTextPDF (export cours) -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>
```

---

## 🚀 Fonctionnalités implémentées

### 1.  Gestion des Langues (LangueController)

- **coté administrateur** :

CRUD complet avec validation métier

Upload d'image (drapeau) → sauvegarde dans C:/xampp/htdocs/fluently/public/uploads/images/langues/

Aperçu image avant validation

Recherche en temps réel

Badges de popularité avec couleurs :

🟠 très haute (orange)

🟢 haute (verte)

🔵 moyenne (bleue)

🟣 faible (violette)

Statut actif/inactif avec badge vert/rouge

- **Côté étudiant (LanguesEtudiantController)** :

Cartes stylisées avec drapeau, popularité, description tronquée

Effet hover avec ombre et translation

Bouton "Commencer" → navigation vers l'apprentissage
- **Statistiques avancées (LangueStatsController)** :

Classement des langues par nombre d'étudiants

Données enrichies via Wikipedia API :

Nombre de locuteurs

Famille linguistique

Système d'écriture

Pays principaux

"Fun fact" culturel

Étoiles de difficulté (1 à 5)

Tendance de popularité (📈 Très populaire / 📈 Populaire / 📊 Moyenne / 📉 Peu étudiée)


### 2. Gestion des Cours (CoursController + ApprentissageController)
- **Coté administrateur (CoursController)** :

-CRUD complet des cours
-Numéro de cours (validation unicité par niveau)
-Date de création (non future)
-Cours précédent (évite l'auto-référence)
-Gestion avancée des ressources :

-Ajout fichiers (PDF, vidéos, audio, images)
-Ajout liens YouTube
-Suppression de ressources
-Double-clic pour ouvrir
-Sauvegarde dans C:/xampp/htdocs/fluently/public/uploads/ressources/

- **Côté étudiant (ApprentissageController)** :

**a. Affichage des cours par niveau**

Cercles numérotés avec effet 3D
Badge "✅ Complété" ou bouton "Terminer"
Progression affichée (ex: "2/5 cours")
Marquage automatique dans user_progress

**b. Ouverture d'un cours**

Dialog moderne avec catégories :
🎬 YouTube
📄 PDF
🎥 Vidéos
🎵 Audios
🖼️ Images
Ouverture native des fichiers/liens

**c. Génération de cours personnalisé par IA (Mistral)**

Thème libre
Points de grammaire (sélection multiple)
Vocabulaire souhaité
Niveau (A1 à C2)
Prompt structuré avec sections :
INTRODUCTION, VOCABULAIRE, GRAMMAIRE, DIALOGUE, EXERCICES, CORRECTION, ASTUCES, POUR ALLER PLUS LOIN
Loading overlay avec animation
Fallback local si API indisponible

**d. Export PDF du cours généré**

Mise en page professionnelle
En-tête violet, sections structurées
Sauvegarde dans C:/xampp/htdocs/fluently/public/uploads/cours_pdf/{langue}/
Affichage en grille des PDFs générés
Suppression via menu contextuel

**e. Génération de Flashcards IA**

Prompt personnalisable
Niveau sélectionnable
5 flashcards générées (question + 4 options + réponse + explication)
Session interactive avec score
Résultats détaillés avec analyse des erreurs

**f. Quiz interactif**

Généré par IA selon niveau
5 questions QCM
Score en temps réel
Feedback immédiat
Résultats avec pourcentage et conseils

**g. Jeu "Compléter la phrase"**

Questions à trous générées par IA
4 options par question
Score progressif
Correction automatique

**h. Puzzle étymologique**

Découpage de mots en morphèmes (préfixe/racine/suffixe)
Niveaux A1 à C1
Indices disponibles
Score dynamique

**i. Dictionnaire intégré**

Recherche de définitions via Mistral
Exemples d'utilisation
Synonymes
Format structuré

**j. Recherche YouTube**

Intégration YouTube API
Cartes de résultats avec titre, description
Lecture dans navigateur

**k. Exploration culturelle**

Lieux emblématiques, cuisine, traditions, art
Données enrichies par IA
Cartes avec images générées par couleur (hash du nom)
Adresses précises, horaires
Carte interactive intégrée (Leaflet + OpenStreetMap)
Écoute du nom et de la description (synthèse vocale)

**l. Prononciation**

Synthèse vocale des mots/phrases
Suggestions : Bonjour, Merci, Comment allez-vous ?, Je t'aime
Support multilingue

**m. Flashcards PDF (cartes mémoire)**

Extraction de contenu depuis les PDFs exportés
Génération automatique de 5 flashcards
Session de révision avec cartes recto/verso
Progression sauvegardée

### 3.📈 Suivi de progression (UserProgressController)

**Interface administrateur :**

Tableau complet des progressions
Colonnes : Utilisateur, Langue, Niveau actuel, Dernier cours, Progression (barre), Test niveau, Date activité
Filtres par langue et niveau
Recherche par ID utilisateur
Détails popup avec toutes les infos
Mécanique de progression :
Cours N°1 → bouton "Terminer" → marqué complété
Cours N°2 → accessible après N°1
...
Dernier cours complété → `dernier_numero_cours` mis à jour

---

## 🔧 Patterns techniques

### Singleton MyDatabase
```java
MyDatabase.getInstance().getConnection()
// Reconnexion automatique si connexion expirée
if (conn == null || conn.isClosed() || !conn.isValid(2)) {
    MyDatabase.getInstance().reconnect();
}
```

### Gestion des chemins d'images
```java
// Conversion chemin relatif Symfony → absolu Windows
if (path.startsWith("/uploads/")) {
    absolutePath = "C:/xampp/htdocs/fluently/public" + path;
}
```


## 📁 Fichiers générés à la racine du projet

```
└── cours_pdf/
    ├── Français/         → PDFs générés pour le français
    ├── Anglais/          → PDFs générés pour l'anglais
    └── Espagnol/         → PDFs générés pour l'espagnol
```

---

## 🌐 APIs externes utilisées

| API               | Usage                                                             | Endpoint |
|-----              |-------                                                            |----------|
**Mistral AI**	    Génération cours, flashcards, quiz, dictionnaire, puzzle, culture	`https://api.mistral.ai/v1/chat/completions`
**Wikipedia**	    Données culturelles (locuteurs, pays, fun facts)	                `https://fr.wikipedia.org/api/rest_v1/page/summary/`
**YouTube Data**	Recherche de vidéos éducatives	                                    `https://www.googleapis.com/youtube/v3/search`
**OpenStreetMap**	Cartes interactives (intégrée via Leaflet)	                        `https://nominatim.openstreetmap.org/search`

---

## 🔑 Règles métier essentielles

1. Une langue doit avoir un nom unique (insensible à la casse)
2. Un niveau doit avoir un titre unique par langue
3. Un niveau doit avoir un ordre unique par langue
4. Un cours doit avoir un numéro unique par niveau
5. Un cours ne peut pas être son propre prédécesseur
6. La date de création d'un cours ne peut pas être dans le futur
7. La progression est stockée dans user_progress avec clé composite (user_id, langue_id)
8. Les chemins des images/ressources sont stockés en relatif pour Symfony

---

## 👥 Équipe

| Membre        | Module |
|--------       |--------|
[Ton prénom]	Langue / Niveau / Cours / Progression
Camarade	    Test / Question / Réponse / TestPassage
Camarade	    Groupe / Message / Session
Camarade	    User / Objectif / Tâche




## 🏫 Contexte académique

Projet réalisé dans le cadre du cours de **Programmation Avancée Java** — ESPRIT School of Engineering, Tunis.

Application web Symfony : `fluently` (base de données partagée)  
Application desktop JavaFX : ce module

---

*README généré le 30/04/2026*
