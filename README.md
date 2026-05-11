# 🌍 Fluently — Application Desktop JavaFX
> Plateforme d'apprentissage des langues étrangères — ESPRIT School of Engineering

---

## 🧭 Présentation générale

**Fluently** est une application desktop développée en **JavaFX 17** dans le cadre d'un projet académique à ESPRIT School of Engineering, Tunis. Elle constitue le pendant desktop d'une application web **Symfony** existante, avec laquelle elle partage la même base de données MySQL `fluently`.

L'application couvre l'intégralité du parcours d'apprentissage d'une langue étrangère :

| Module | Description |
|--------|-------------|
| 👤 **Utilisateurs** | Inscription, connexion (classique / Google OAuth / Face ID), profil, avatar IA |
| 🌐 **Langues & Cours** | Gestion des langues, niveaux CECRL, cours, progression, IA pédagogique |
| 📝 **Tests & Évaluation** | Tests QCM / oral / texte libre, duels, certificats, anti-fraude |
| 🎯 **Objectifs & Gamification** | Kanban, streaks, XP, badges, recommandations IA |
| 💬 **Groupes & Chat** | Groupes de conversation, chat en temps réel, modération IA, logs admin |
| 📅 **Sessions & Réservations** | Sessions planifiées, réservation, QR code, Google Calendar, Jitsi/Meet, notifications |

### Stack technique

- **Frontend :** JavaFX 17 + FXML + SceneBuilder
- **Backend :** JDBC brut sur MySQL 8 (pas d'ORM)
- **IA :** Groq (LLaMA), Mistral, Whisper
- **APIs externes :** API Ninjas, detectlanguage.com, Google OAuth, YouTube, Wikipedia, Google Calendar, Jitsi Meet
- **Build :** Maven + `module-info.java`

### Démarrage rapide

```bash
# Compiler
mvn -q -DskipTests compile

# Lancer l'application
mvn javafx:run
```

Créer `src/main/resources/config.properties` avec toutes les clés API (voir section ⚙️ Configuration de chaque module). Ce fichier est dans `.gitignore` — ne jamais le committer.

---

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
| **Emna Bahar** | Test / Question / Réponse / TestPassage |
| **Oumaima Ben Hammou** | Langue / Cours / Niveau / UserProgress |
| **Jihed Ramedi** | Groupe / Message / Session |
| **Azer Aissaoui** | User / Objectif / Tâche |
| **Sarra Ben Boubaker** | Objectif / Tâche / Gamification / IA |
| **Yosr Ben Hamouda** | Session / Réservation / Google Calendar / Jitsi |

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
niveau       (id, titre, description, image_couverture, difficulte, ordre, seuil_score_max, seuil_score_min, id_langue_id)
cours        (id, numero, ressource, date_creation, cours_precedent_id, id_niveau_id)
user_progress(id, dernier_numero_cours, test_niveau_complete, date_derniere_activite,
              user_id, langue_id, niveau_actuel_id, dernier_cours_complete_id)
user         (id, email, nom, prenom, roles, statut, password)
```

### Popularités de langue
| Type | Rôle |
|------|------|
| très haute | Langue très populaire (ex: Anglais) |
| haute | Langue populaire (ex: Espagnol) |
| moyenne | Popularité moyenne |
| faible | Peu étudiée |

### Difficulté de niveau
| Difficulté | Ordre | Description |
|------------|-------|-------------|
| A1 | 1 | Débutant |
| A2 | 2 | Élémentaire |
| B1 | 3 | Intermédiaire |
| B2 | 4 | Intermédiaire avancé |
| C1 | 5 | Avancé |
| C2 | 6 | Maîtrise |

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

### 1. Gestion des Langues (LangueController)

- **Côté administrateur** :
    - CRUD complet avec validation métier
    - Upload d'image (drapeau) → sauvegarde dans `C:/xampp/htdocs/fluently/public/uploads/images/langues/`
    - Aperçu image avant validation
    - Recherche en temps réel
    - Badges de popularité avec couleurs : 🟠 très haute, 🟢 haute, 🔵 moyenne, 🟣 faible
    - Statut actif/inactif avec badge vert/rouge

- **Côté étudiant (LanguesEtudiantController)** :
    - Cartes stylisées avec drapeau, popularité, description tronquée
    - Effet hover avec ombre et translation
    - Bouton "Commencer" → navigation vers l'apprentissage

- **Statistiques avancées (LangueStatsController)** :
    - Classement des langues par nombre d'étudiants
    - Données enrichies via Wikipedia API : nombre de locuteurs, famille linguistique, système d'écriture, pays principaux, "Fun fact" culturel
    - Étoiles de difficulté (1 à 5)
    - Tendance de popularité (📈 Très populaire / 📈 Populaire / 📊 Moyenne / 📉 Peu étudiée)

### 2. Gestion des Cours (CoursController + ApprentissageController)

- **Côté administrateur (CoursController)** :
    - CRUD complet des cours
    - Numéro de cours (validation unicité par niveau)
    - Date de création (non future)
    - Cours précédent (évite l'auto-référence)
    - Gestion avancée des ressources : ajout fichiers (PDF, vidéos, audio, images), ajout liens YouTube, suppression de ressources, double-clic pour ouvrir
    - Sauvegarde dans `C:/xampp/htdocs/fluently/public/uploads/ressources/`

- **Côté étudiant (ApprentissageController)** :
    - **a. Affichage des cours par niveau** — cercles numérotés avec effet 3D, badge "✅ Complété" ou bouton "Terminer", progression (ex: "2/5 cours"), marquage automatique dans user_progress
    - **b. Ouverture d'un cours** — dialog moderne avec catégories (🎬 YouTube, 📄 PDF, 🎥 Vidéos, 🎵 Audios, 🖼️ Images), ouverture native des fichiers/liens
    - **c. Génération de cours personnalisé par IA (Mistral)** — thème libre, points de grammaire, vocabulaire, niveau A1 à C2, prompt structuré avec 8 sections, loading overlay animé
    - **d. Export PDF du cours généré** — mise en page professionnelle, en-tête violet, sauvegarde dans `cours_pdf/{langue}/`
    - **e. Génération de Flashcards IA** — 5 flashcards (question + 4 options + réponse + explication), session interactive avec score
    - **f. Quiz interactif** — 5 questions QCM générées par IA selon niveau, score en temps réel, feedback immédiat
    - **g. Jeu "Compléter la phrase"** — questions à trous générées par IA, 4 options, correction automatique
    - **h. Puzzle étymologique** — découpage en morphèmes (préfixe/racine/suffixe), niveaux A1 à C1
    - **i. Dictionnaire intégré** — définitions + exemples + synonymes via Mistral
    - **j. Recherche YouTube** — intégration YouTube API, cartes de résultats, lecture dans navigateur
    - **k. Exploration culturelle** — lieux emblématiques, cuisine, traditions via IA + OpenStreetMap
    - **l. Prononciation** — synthèse vocale des mots/phrases, support multilingue
    - **m. Flashcards PDF** — extraction depuis PDFs exportés, session recto/verso

### 3. 📈 Suivi de progression (UserProgressController)

- Tableau complet des progressions
- Colonnes : Utilisateur, Langue, Niveau actuel, Dernier cours, Progression (barre), Test niveau, Date activité
- Filtres par langue et niveau
- Recherche par ID utilisateur
- Détails popup avec toutes les infos

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

| API | Usage | Endpoint |
|-----|-------|----------|
| **Mistral AI** | Génération cours, flashcards, quiz, dictionnaire, puzzle, culture | `https://api.mistral.ai/v1/chat/completions` |
| **Wikipedia** | Données culturelles (locuteurs, pays, fun facts) | `https://fr.wikipedia.org/api/rest_v1/page/summary/` |
| **YouTube Data** | Recherche de vidéos éducatives | `https://www.googleapis.com/youtube/v3/search` |
| **OpenStreetMap** | Cartes interactives (intégrée via Leaflet) | `https://nominatim.openstreetmap.org/search` |

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

| Membre | Module |
|--------|--------|
| **Oumaima Ben Hammou** | Langue / Niveau / Cours / Progression |
| **Emna Bahar** | Test / Question / Réponse / TestPassage |
| **Jihed Ramedi** | Groupe / Message / Session |
| **Azer Aissaoui** | User / Objectif / Tâche |
| **Sarra Ben Boubaker** | Objectif / Tâche / Gamification / IA |
| **Yosr Ben Hamouda** | Session / Réservation / Google Calendar / Jitsi |

---

# 🎯 Fluently — Module Objectif / Tâche / Gamification / IA
> Application desktop JavaFX — Gestion des objectifs d'apprentissage avec IA et suivi de progression

---

## 📌 Présentation du module

Ce module est la partie **planification, suivi et gamification** de l'application Fluently. Il permet à un utilisateur de créer et gérer ses objectifs d'apprentissage, d'organiser ses tâches dans un tableau Kanban, de recevoir des recommandations personnalisées par IA, et de suivre sa progression via un système de streaks, de points XP et de badges — inspiré de Duolingo.

Développé en **JavaFX 17** (IntelliJ IDEA + SceneBuilder), connecté à la même base de données MySQL partagée. L'IA est propulsée par **Groq AI (LLaMA 3, 100% gratuit)**.

---

## 🗂️ Structure du projet

```
com/example/pijava_fluently/
├── entites/
│   ├── Objectif.java           → objectif d'apprentissage (titre, desc, dates, statut, userId)
│   ├── Tache.java              → tâche liée à un objectif (titre, desc, dateLimite, statut, priorité)
│   ├── UserSession.java        → session de connexion (login/logout, durée, points)
│   └── UserStats.java          → statistiques agrégées (streaks, XP, niveau, badges, graphe hebdo)
│
├── services/
│   ├── ObjectifService.java         → CRUD objectifs + filtre par utilisateur
│   ├── TacheService.java            → CRUD tâches + filtre par objectif
│   ├── AITaskGeneratorService.java  → génération de tâches par Groq LLaMA 3
│   ├── UserSessionService.java      → tracking sessions, calcul streaks, XP, badges (Singleton)
│   └── NotificationService.java     → notifications deadlines avec badge animé
│
├── controller/
│   ├── ObjectifController.java           → CRUD objectifs, cartes colorées, pagination
│   ├── TacheController.java              → Kanban drag & drop, correcteur orthographe
│   ├── AITaskGeneratorController.java    → dialog génération tâches IA
│   ├── StreakDashboardController.java    → dashboard streaks, arc niveau, graphe hebdo, badges
│   ├── RecommendationDialogController.java → recommandations IA d'objectifs
│   └── GamificationCardController.java  → carte gamification intégrée dans l'écran objectifs
│
├── fxml/
│   ├── Objectif-view.fxml            → liste des objectifs (cartes + formulaire + pagination)
│   ├── Tache-view.fxml               → kanban 4 colonnes (À faire / En cours / Terminée / Annulée)
│   ├── AITaskGenerator-dialog.fxml   → dialog génération tâches IA
│   ├── StreakDashboard.fxml          → dashboard dark-mode (streaks, arc, graphe, badges)
│   ├── RecommendationDialog.fxml     → dialog recommandations objectifs IA
│   └── Gamificationcard.fxml         → carte gamification intégrée
│
└── utils/
    └── MyDatabase.java     → Singleton connexion MySQL
```

---

## 🗄️ Base de données

**Nom :** `fluently`

```sql
objectif      (id, titre, description, date_deb, date_fin, statut, id_user_id)
tache         (id, titre, description, date_limite, statut, priorite, id_objectif_id)
user          (id, email, nom, prenom, roles, statut, password)

-- Tables créées automatiquement au démarrage (UserSessionService)
user_session  (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    user_id             INT NOT NULL,
    session_date        DATE NOT NULL,
    login_time          DATETIME NOT NULL,
    logout_time         DATETIME,
    duree_minutes       INT DEFAULT 0,
    taches_completees   INT DEFAULT 0,
    taches_commencees   INT DEFAULT 0,
    objectifs_consultes INT DEFAULT 0,
    points_gagnes       INT DEFAULT 0,
    INDEX idx_user_date (user_id, session_date)
)

user_stats_cache (
    user_id             INT PRIMARY KEY,
    streak_actuel       INT DEFAULT 0,
    streak_max          INT DEFAULT 0,
    total_jours_actifs  INT DEFAULT 0,
    total_sessions      INT DEFAULT 0,
    total_points        INT DEFAULT 0,
    niveau              INT DEFAULT 1,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)
```

### Statuts d'Objectif
| Statut | Description |
|--------|-------------|
| `En cours` | Objectif actif |
| `Terminé` | Objectif accompli |
| `En pause` | Mis en attente |
| `Annulé` | Abandonné |

### Statuts de Tâche
| Statut | Colonne Kanban |
|--------|----------------|
| `À faire` | Colonne 1 — tâches non commencées |
| `En cours` | Colonne 2 — tâches en progression |
| `Terminée` | Colonne 3 — tâches accomplies (+50 XP) |
| `Annulée` | Colonne 4 — tâches abandonnées (-20 XP) |

### Priorités de Tâche
| Priorité | Couleur | Icône |
|----------|---------|-------|
| `Basse` | 🟢 Vert | Priorité faible |
| `Normale` | 🔵 Bleu | Priorité standard |
| `Haute` | 🟠 Orange | Attention requise |
| `Urgente` | 🔴 Rouge | Action immédiate |

---

## ⚙️ Configuration

Ajouter dans `src/main/resources/config.properties` :

```properties
# Clé Groq gratuite → https://console.groq.com/keys
groq.api.key=gsk_VOTRE_CLE_GROQ
```

> ⚠️ Ne jamais committer ce fichier — ajouter à `.gitignore`

---

## 📦 Dépendances (pom.xml)

```xml
<!-- Jackson (parsing JSON réponses Groq) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

Les autres dépendances (JavaFX, MySQL) sont partagées avec les modules précédents.

---

## 🚀 Fonctionnalités implémentées

### 1. 🎯 Gestion des Objectifs (ObjectifController)

**Interface en cartes colorées** avec 8 dégradés distincts selon l'index :

- **CRUD complet** avec formulaire inline (apparaît/disparaît sans changer de page)
- **Pagination** — 4 objectifs par page avec boutons Précédent / Suivant et indicateur "Page X / Y (N-M sur Total)"
- **Barre de progression** calculée en temps réel depuis les tâches (tâches terminées / total)
- **Recherche en temps réel** — filtre par titre, description et statut simultanément
- **Contrôle de propriété** — seul le créateur peut modifier/supprimer ses objectifs, les autres voient uniquement les détails
- **Dialog de détails** — popup stylisé avec gradient, grille de métadonnées (ID, dates, utilisateur, progression)
- **Navigation directe vers les tâches** depuis chaque carte
- **Validation en temps réel** des champs du formulaire avec messages d'erreur colorés (rouge = erreur, vert = valide)

### 2. ✅ Gestion des Tâches — Kanban (TacheController)

**Tableau Kanban à 4 colonnes** avec glisser-déposer natif JavaFX :

- **Drag & Drop** — une carte glissée d'une colonne à l'autre met à jour le statut en base immédiatement
- **Highlight visuel** au drag-over (bordure bleue + fond semi-transparent sur la colonne cible)
- **Compteurs** par colonne mis à jour en temps réel
- **Badge "Retard"** (🟥 rouge) affiché automatiquement si la date limite est dépassée et la tâche non terminée
- **Couleurs de priorité** : fond coloré de la bande supérieure de chaque carte selon la priorité
- **Correcteur orthographique en temps réel** avec debounce 500ms, suggestions cliquables, support multilingue

### 3. 🤖 Génération de Tâches par IA (AITaskGeneratorService)

L'IA **Groq LLaMA 3** analyse l'objectif et génère 3 tâches personnalisées avec niveau de difficulté progressif.

### 4. 🔥 Système de Streaks & Gamification

Système inspiré de **Duolingo** avec 10 niveaux (🌱 → 🌌), badges débloquables, graphe hebdomadaire animé et dashboard dark-mode.

### 5. 🔔 Notifications de Deadlines

Badge rouge animé avec compteur, rafraîchissement toutes les 5 secondes.

### 6. 🤖 Recommandations IA d'Objectifs

3 objectifs recommandés personnalisés générés par Groq LLaMA avec bouton "Régénérer".

---

## 🌐 APIs externes utilisées

| API | Usage | Endpoint | Coût |
|-----|-------|----------|------|
| **Groq LLaMA 3** | Génération tâches + recommandations objectifs | `POST https://api.groq.com/openai/v1/chat/completions` | 🆓 Gratuit |

---

# 👤 Fluently — Module User / Gestion des Utilisateurs
> Application desktop JavaFX — Authentification, profil, avatar IA et administration des utilisateurs

---

## 📌 Présentation du module

Ce module est la partie **gestion des utilisateurs** de l'application Fluently. Il gère l'inscription, la connexion (classique, Google OAuth, Face ID), le profil utilisateur avec avatar généré par IA, et l'administration complète des comptes depuis un tableau de bord admin.

Développé en **JavaFX 17** (IntelliJ IDEA + SceneBuilder), connecté à la même base de données MySQL partagée.

---

## 🗂️ Structure du projet

```
com/example/pijava_fluently/
├── entites/
│   └── User.java               → utilisateur (email, nom, prénom, rôle, statut,
│                                  password, faceDescriptor, chosenLanguage, avatarSvg)
│
├── services/
│   ├── UserService.java        → CRUD utilisateurs + authentification BCrypt
│   └── AvatarService.java      → génération avatar SVG via Groq LLaMA (IA)
│
├── controller/
│   ├── LoginController.java            → connexion + inscription + validation
│   ├── GoogleAuthController.java       → OAuth2 Google (login sans mot de passe)
│   ├── FaceLoginController.java        → reconnaissance faciale via Python
│   ├── LanguagePickerController.java   → choix de langue + génération avatar IA
│   ├── FrontProfileController.java     → profil utilisateur avec avatar SVG
│   └── AdminDashboardController.java   → tableau de bord admin + export Google Sheets
│
├── fxml/
│   ├── login.fxml
│   ├── language-picker.fxml
│   ├── front-profile.fxml
│   ├── face-login.fxml
│   └── admin-dashboard.fxml
│
└── utils/
    ├── MyDatabase.java     → Singleton connexion MySQL
    └── ConfigLoader.java   → lecture config.properties (clés API)
```

---

## 🗄️ Base de données

**Nom :** `fluently`

```sql
user (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(180) UNIQUE NOT NULL,
    nom              VARCHAR(100),
    prenom           VARCHAR(100),
    roles            JSON,
    statut           VARCHAR(20) DEFAULT 'actif',
    password         VARCHAR(255),
    face_descriptor  TEXT,
    chosen_language  VARCHAR(50),
    avatar_svg       MEDIUMTEXT
)
```

**Migration nécessaire :**
```sql
ALTER TABLE user ADD COLUMN chosen_language VARCHAR(50) NULL;
ALTER TABLE user ADD COLUMN avatar_svg MEDIUMTEXT NULL;
```

### Rôles utilisateur
| Rôle | Accès |
|------|-------|
| `ROLE_ETUDIANT` | Interface étudiant (home, profil, cours, tests) |
| `ROLE_PROF` | Interface professeur |
| `ROLE_ADMIN` | Tableau de bord admin complet |

### Statuts utilisateur
| Statut | Description |
|--------|-------------|
| `actif` | Compte actif |
| `online` | Connecté en ce moment |
| `inactif` | Compte désactivé |

---

## ⚙️ Configuration

Créer `src/main/resources/config.properties` :

```properties
# Groq AI (avatar generation) — gratuit sur console.groq.com
groq.api.key=gsk_VOTRE_CLE_GROQ

# Google OAuth (login + sheets export)
google.auth.client.id=VOTRE_GOOGLE_AUTH_CLIENT_ID
google.auth.client.secret=VOTRE_GOOGLE_AUTH_CLIENT_SECRET
google.sheets.client.id=VOTRE_GOOGLE_SHEETS_CLIENT_ID
google.sheets.client.secret=VOTRE_GOOGLE_SHEETS_CLIENT_SECRET
```

> ⚠️ Ne jamais committer ce fichier — ajouté à `.gitignore`

---

## 📦 Dépendances ajoutées (pom.xml)

```xml
<!-- JavaFX Web (rendu SVG avatar via WebView) -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-web</artifactId>
    <version>17.0.6</version>
</dependency>
```

**module-info.java :**
```java
requires javafx.web;    // WebView pour afficher les avatars SVG
requires java.desktop;  // Desktop.browse() pour Google OAuth
requires jdk.httpserver; // HttpServer pour le callback OAuth local
```

---

## 🚀 Fonctionnalités implémentées

### 1. 🔐 Authentification (LoginController)

Trois modes de connexion depuis un seul écran :

- **Classique** : email + mot de passe hashé en **BCrypt** (`jbcrypt`)
- **Google OAuth 2.0** : connexion sans mot de passe via compte Google
- **Face ID** : reconnaissance faciale via caméra

**Inscription :**
- Validation en temps réel (regex nom/prénom, format email, longueur mot de passe, confirmation)
- Vérification unicité email en base
- Hash BCrypt automatique du mot de passe
- Redirection vers `LanguagePickerController` après inscription (choix langue + génération avatar)

### 2. 🔵 Google Login (GoogleAuthController)

Implémenté en **pur Java** sans librairie externe :

```
1. Démarrage d'un HttpServer local sur un port aléatoire
2. Ouverture du navigateur → écran de consentement Google
3. L'utilisateur approuve → Google redirige vers http://localhost:PORT/callback?code=...
4. Le serveur local capture le code d'autorisation
5. Échange du code contre un ID token (JWT) via POST à oauth2.googleapis.com/token
6. Décodage du payload JWT (base64) → extraction email, prénom, nom
7. Création ou mise à jour du compte en base de données
8. Navigation vers home ou admin selon le rôle
```

### 3. 😐 Face ID (FaceLoginController + LoginController)

Implémenté via **Python** lancé depuis Java avec `ProcessBuilder` :

**Librairies Python requises :**
```bash
pip install face_recognition opencv-python numpy
```

### 4. 🌍 Sélection de langue + Avatar IA (LanguagePickerController)

- 10 langues proposées avec drapeaux emoji
- Avatar généré par Groq LLaMA sauvegardé en base (`avatar_svg`)
- Fallback avatar (cercle coloré avec initiale) si l'API échoue

### 5. 🤖 Génération d'Avatar par IA (AvatarService)

Visage humain cartoon avec le **motif du drapeau peint sur la peau** selon la langue choisie.

### 6. 👤 Profil Utilisateur (FrontProfileController)

- Avatar SVG via WebView, badges langue/rôle/statut
- Formulaire d'édition avec indicateur force du mot de passe

### 7. 🏛️ Tableau de Bord Admin + Export Google Sheets

- Tableau des utilisateurs avec avatars, recherche, modification, export vers Google Sheets v4

---

## 🌐 APIs externes utilisées

| API | Usage | Endpoint | Coût |
|-----|-------|----------|------|
| **Groq LLaMA** | Génération avatar SVG | `POST https://api.groq.com/openai/v1/chat/completions` | 🆓 Gratuit |
| **Google OAuth 2.0** | Login Google + autorisation Sheets | `https://accounts.google.com/o/oauth2/v2/auth` | 🆓 Gratuit |
| **Google Sheets API v4** | Création et écriture de feuilles | `https://sheets.googleapis.com/v4/spreadsheets` | 🆓 Gratuit |

---

## 🔑 Règles métier essentielles

1. **L'email est unique** — vérification avant inscription ET avant modification du profil
2. **Le mot de passe est toujours hashé en BCrypt** — jamais stocké en clair
3. **L'avatar est généré une seule fois** à l'inscription — stocké en base, pas recalculé
4. **Google Login crée le compte automatiquement** si l'email n'existe pas encore en base

---

## 👥 Équipe

| Membre | Module |
|--------|--------|
| **Azer Aissaoui** | User / Authentification / Avatar IA / Google Sheets |
| **Emna Bahar** | Test / Question / Réponse / TestPassage |
| **Oumaima Ben Hammou** | Langue / Cours / Niveau / UserProgress |
| **Sarra Ben Boubaker** | Objectif / Tâche / Gamification / IA |
| **Jihed Ramedi** | Groupe / Message / Chat / Modération IA |
| **Yosr Ben Hamouda** | Session / Réservation / Google Calendar / Jitsi |

---

# 💬 Fluently — Module Groupe / Message / Modération / Chat
> Application desktop JavaFX — Groupes de conversation avec modération IA et suivi admin

---

## 📌 Présentation du module

Ce module est la partie **communication et collaboration** de l'application Fluently. Il permet aux étudiants de rejoindre des groupes de conversation selon leur langue et leur niveau CECRL, d'échanger des messages en temps réel, et de répondre aux messages des autres membres. Chaque message est automatiquement analysé par trois APIs : modération du contenu, détection de langue, et analyse de sentiment.

Développé en **JavaFX 17** (IntelliJ IDEA + SceneBuilder), connecté à la même base de données MySQL partagée.

---

## 🗂️ Structure du projet

```
com/example/pijava_fluently/
├── entites/
│   ├── Groupe.java         → groupe de conversation (nom, description, capacité, statut, langue, niveau)
│   ├── Message.java        → message (contenu, type, statut, dates, sentiment, modération)
│   └── MessageLog.java     → entrée d'audit (action, contenu original, nouveau contenu, auteur)
│
├── services/
│   ├── GroupService.java               → CRUD groupes
│   ├── MessageService.java             → envoi, lecture, suppression, rejoindre groupe, tables auto-créées
│   ├── MessageLogService.java          → journal d'audit (INSERT dans message_log)
│   ├── ModerationService.java          → filtre de contenu via API Ninjas (profanityfilter)
│   ├── SentimentService.java           → analyse de sentiment via API Ninjas (sentiment)
│   └── LanguageDetectionService.java   → détection de langue via detectlanguage.com
│
├── controller/
│   ├── GroupesController.java          → navigation et filtrage des groupes
│   ├── GroupChatController.java        → interface de chat, envoi, réponses, mentions
│   ├── GroupFormController.java        → formulaire création/édition de groupe (admin)
│   └── AdminGroupMessagesController.java → tableau de bord admin : messages + logs
│
├── fxml/
│   ├── groupes.fxml
│   ├── group-chat.fxml
│   ├── group-form.fxml
│   └── admin-group-messages.fxml
│
└── utils/
    ├── MyDatabase.java     → Singleton connexion MySQL
    └── AppConfig.java      → lecture config.properties + fallback variable d'environnement
```

---

## 🗄️ Base de données

**Nom :** `fluently`

```sql
groupe       (id, nom, description, capacite, statut, date_creation, id_langue_id, id_niveau_id)
message      (id, contenu, type_message, emoji_react, is_epingle, date_creation,
              date_modif, statut_message, id_groupe_id, id_user_id)
message_log  (id, action, message_id, groupe_id, user_id, user_name,
              original_content, new_content, created_at, updated_at,
              created_by_id, updated_by_id)

-- Tables créées automatiquement au démarrage
groupe_membre     (id, id_groupe_id, id_user_id, date_joined)
message_metadata  (id, message_id, parent_message_id, mentions)
message_moderation(id, message_id, provider, is_flagged, top_category, top_score,
                   api_available, error_message, raw_response, checked_at)
message_sentiment (id, message_id, sentiment, checked_at)
```

### Statuts de groupe
| Statut | Description |
|--------|-------------|
| `actif` | Groupe ouvert aux membres |
| `inactif` | Groupe fermé |
| `complet` | Capacité maximale atteinte |

### Valeurs de sentiment
| Valeur | Badge affiché |
|--------|---------------|
| `positive` | 😊 Positif (fond vert) |
| `negative` | 😠 Négatif (fond rouge) |
| `neutral` | 😐 Neutre (fond jaune) |
| `null` | ○ Sentiment inconnu (fond gris) |

---

## ⚙️ Configuration

Ajouter dans `src/main/resources/config.properties` :

```properties
API_NINJAS_KEY=VOTRE_CLE_API_NINJAS
DETECTLANGUAGE_KEY=VOTRE_CLE_DETECTLANGUAGE
```

---

## 🚀 Fonctionnalités implémentées

### 1. 🏘️ Navigation des groupes (GroupesController)

- Recherche par nom/description, filtres langue/niveau/statut
- Filtre "Groupes que je peux rejoindre" — vérifie langue ET niveau dans `user_progress`
- Cartes avec barre de progression membres/capacité

### 2. 🔑 Rejoindre un groupe — 4 vérifications dans l'ordre

```
1. capacite <= 0              → GROUP_FULL
2. estParticipant()           → ALREADY_PARTICIPANT
3. belongsToLangueAndNiveau() → LANGUAGE_LEVEL_MISMATCH
4. participantsActuels >= capacite → GROUP_FULL
→ sinon : INSERT dans groupe_membre → JOINED
```

### 3. 💬 Chat — Pipeline d'envoi

```
1. Validation → 2. Modération → 3. Détection langue → 4. INSERT message
→ 5. INSERT metadata → 6. INSERT moderation → 7. INSERT sentiment → 8. Rechargement
```

### 4. 🔞 Modération (fail-safe)

API Ninjas `profanityfilter` — bloque uniquement si `apiAvailable == true` ET `flagged == true`.

### 5. 🌐 Détection de langue (fail-safe)

detectlanguage.com — ignorée si l'API retourne `null` (panne, quota épuisé).

### 6. 📋 Journal d'audit

Tout modifier/supprimer insère dans `message_log` avant d'effectuer l'action.

### 7. 🏛️ Backoffice admin

Onglet Messages + Onglet Logs — noms résolus via cache en mémoire.

---

## 🌐 APIs externes utilisées

| API | Usage | Endpoint | Coût |
|-----|-------|----------|------|
| **API Ninjas Profanity** | Détection contenu inapproprié | `GET https://api.api-ninjas.com/v1/profanityfilter` | 🆓 Gratuit |
| **API Ninjas Sentiment** | Analyse ton des messages | `GET https://api.api-ninjas.com/v1/sentiment` | 🆓 Gratuit |
| **detectlanguage.com** | Vérification langue d'un message | `POST https://ws.detectlanguage.com/0.2/detect` | 🆓 1000/jour |

---

## 🔑 Règles métier essentielles

1. **Même langue ET même niveau** dans `user_progress` pour rejoindre un groupe
2. **Modération et détection langue sont fail-safe** — en cas de panne le message passe
3. **Tout modifier/supprimer est journalisé** avant que l'action soit effectuée
4. **Le sentiment est purement décoratif** — aucun impact sur l'envoi

---

## 👥 Équipe

| Membre | Module |
|--------|--------|
| **Jihed Ramedi** | Groupe / Message / Chat / Modération IA |
| **Azer Aissaoui** | User / Authentification / Avatar IA / Google Sheets |
| **Emna Bahar** | Test / Question / Réponse / TestPassage |
| **Oumaima Ben Hammou** | Langue / Cours / Niveau / UserProgress |
| **Sarra Ben Boubaker** | Objectif / Tâche / Gamification / IA |
| **Yosr Ben Hamouda** | Session / Réservation / Google Calendar / Jitsi |

---

# 📅 Fluently — Module Session / Réservation
> Application desktop JavaFX — Sessions de conversation avec réservation, Google Calendar, Jitsi/Meet et notifications

---

## 📌 Présentation du module

Ce module est la partie **planification et réservation de sessions** de l'application Fluently. Il permet à un étudiant de consulter les sessions disponibles, de réserver une place, de recevoir une confirmation par QR code, et de rejoindre la session via **Google Meet** ou **Jitsi** intégré dans l'application. Un système de suggestions personnalisées par IA (**Groq LLaMA 3**) recommande les sessions les plus adaptées au profil de l'étudiant. Les sessions sont synchronisées automatiquement avec **Google Calendar**.

Développé en **JavaFX 17** (IntelliJ IDEA + SceneBuilder), connecté à la même base de données MySQL partagée.

---

## 🗂️ Structure du projet

```
com/example/pijava_fluently/
├── entites/
│   ├── Session.java        → session (titre, date, durée, lien Meet/Jitsi, statut, langue, niveau)
│   └── Reservation.java    → réservation (statut, date, QR code, user, session)
│
├── services/
│   ├── SessionService.java             → CRUD sessions
│   ├── ReservationService.java         → CRUD réservations + génération QR code
│   ├── AISessionSuggestionService.java → suggestions personnalisées via Groq LLaMA 3
│   ├── GoogleCalendarService.java      → synchronisation Google Calendar
│   ├── NotificationService.java        → notifications in-app + rappels de sessions
│   └── JitsiService.java               → intégration Jitsi Meet dans l'application
│
├── controller/
│   ├── SessionController.java              → CRUD sessions (admin)
│   ├── SessionEtudiantController.java      → liste sessions + suggestions IA (étudiant)
│   ├── ReservationController.java          → réserver, annuler, QR code (étudiant)
│   └── AdminReservationController.java     → tableau de bord réservations (admin)
│
├── fxml/
│   ├── sessions.fxml
│   ├── sessions-etudiant.fxml
│   ├── reservations.fxml
│   └── admin-reservations.fxml
│
└── utils/
    ├── MyDatabase.java     → Singleton connexion MySQL
    └── ConfigLoader.java   → lecture config.properties (clés API)
```

---

## 🗄️ Base de données

**Nom :** `fluently`

```sql
session     (id, titre, description, date_session, duree_minutes, lien_meet,
             type_session, statut, capacite, id_langue_id, id_niveau_id, id_user_id)

reservation (id, date_reservation, statut, qr_code_data,
             id_session_id, id_user_id)
```

### Types de session
| Type | Description |
|------|-------------|
| `google_meet` | Session via lien Google Meet externe |
| `jitsi` | Session via Jitsi intégré dans l'application |

### Statuts de session
| Statut | Description |
|--------|-------------|
| `planifiee` | Session à venir, ouverte aux réservations |
| `en_cours` | Session active en ce moment |
| `terminee` | Session passée |
| `annulee` | Session annulée par l'admin |

### Statuts de réservation
| Statut | Description |
|--------|-------------|
| `confirmee` | Réservation validée, QR code généré |
| `annulee` | Annulée par l'étudiant |
| `en_attente` | En attente de confirmation admin |

---

## ⚙️ Configuration

Ajouter dans `src/main/resources/config.properties` :

```properties
# Groq AI (suggestions de sessions)
groq.api.key=gsk_VOTRE_CLE_GROQ

# Google Calendar + Meet
google.calendar.client.id=VOTRE_CLIENT_ID
google.calendar.client.secret=VOTRE_CLIENT_SECRET
```

> ⚠️ Ne jamais committer ce fichier — ajouté à `.gitignore`

---

## 📦 Dépendances (pom.xml)

```xml
<!-- ZXing (génération QR code) — partagé avec module Test -->
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

<!-- JavaFX Web (intégration Jitsi via WebView) — partagé avec module User -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-web</artifactId>
    <version>17.0.6</version>
</dependency>

<!-- Jackson (parsing JSON Groq) — partagé avec module Test -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

---

## 🚀 Fonctionnalités implémentées

### 1. 📋 Gestion des Sessions (SessionController)

**Côté administrateur :**
- CRUD complet avec validation métier (date non passée, capacité > 0)
- Choix du type de session : Google Meet (lien externe) ou Jitsi (intégré dans l'application)
- Synchronisation automatique avec **Google Calendar** à chaque création ou modification
- Filtres par langue, niveau, statut et date
- Compteur de places restantes en temps réel

**Côté étudiant (SessionEtudiantController) :**
- Liste des sessions disponibles filtrables par langue et niveau
- **Suggestions IA** — Groq LLaMA 3 analyse le profil de l'étudiant (langue, niveau actuel, historique) et recommande les 3 sessions les plus pertinentes avec justification
- Bouton "Rejoindre" actif uniquement si une réservation confirmée existe
- Accès direct à Jitsi intégré ou ouverture du lien Google Meet dans le navigateur

### 2. 🎟️ Réservation (ReservationController)

**Pipeline de réservation :**
```
1. Vérification places disponibles (capacite - réservations confirmées)
2. Vérification non-doublon (l'étudiant n'a pas déjà réservé cette session)
3. INSERT dans reservation (statut = "confirmee")
4. Génération du QR code (ZXing)
5. Notification in-app de confirmation
6. Rechargement de la vue
```

**QR code de confirmation :**
- Encodé avec ZXing : nom de l'étudiant, titre de la session, date, ID de réservation
- Affiché dans un dialog popup avec option de sauvegarde en PNG
- Auto-contenu — lisible sans serveur

### 3. 🤖 Suggestions IA (AISessionSuggestionService)

Groq LLaMA 3 reçoit en contexte le niveau actuel, la langue étudiée, et les sessions disponibles, puis retourne les 3 sessions les plus adaptées avec une explication courte pour chacune.

```java
// Prompt structuré envoyé à Groq
"Voici le profil de l'étudiant : niveau {niveau}, langue {langue}.
Voici les sessions disponibles : {sessionsJSON}.
Recommande les 3 sessions les plus adaptées.
Réponds uniquement en JSON : [{id, titre, raison}]"
```

Affichage : cartes surlignées avec badge 🤖 et texte de justification.

### 4. 📅 Google Calendar (GoogleCalendarService)

Synchronisation automatique via l'API Google Calendar v3 (même pattern OAuth local que le module User) :

```
Création session  → INSERT dans Calendar (titre, date, durée, lien Meet/Jitsi)
Modification      → PATCH dans Calendar (via eventId stocké dans session)
Annulation        → DELETE dans Calendar
```

### 5. 📹 Intégration Jitsi (JitsiService)

Les sessions de type `jitsi` s'ouvrent directement dans l'application via **WebView** :

```java
String roomUrl = "https://meet.jit.si/" + sessionId + "-fluently";
webView.getEngine().load(roomUrl);
```

Les sessions Google Meet ouvrent le lien dans le navigateur par défaut via `Desktop.getDesktop().browse()`.

### 6. 🔔 Notifications in-app (NotificationService)

Toasts non-bloquants affichés 3 secondes via `Platform.runLater()` :

| Déclencheur | Message affiché |
|-------------|----------------|
| Réservation confirmée | ✅ "Réservation confirmée — [titre session]" |
| Nouvelle session ajoutée (langue correspondante) | 📅 "Nouvelle session disponible : [titre]" |
| Session dans moins de 30 minutes | ⏰ "Rappel : [titre] commence dans 30 min" |
| Annulation par l'admin | ❌ "Session annulée : [titre]" |

**Rappels :** un `ScheduledExecutorService` tourne en arrière-plan et vérifie toutes les 5 minutes si une session réservée approche des 30 minutes.

### 7. 🏛️ Backoffice Admin (AdminReservationController)

- Tableau de toutes les réservations (ID, Session, Étudiant, Date, Statut, QR code)
- Aperçu du QR code directement depuis la ligne du tableau
- Confirmation ou annulation manuelle d'une réservation
- Statistiques : taux de remplissage par session, nombre de réservations par langue

---

## 🔧 Patterns techniques

### Scheduler pour les rappels
```java
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
scheduler.scheduleAtFixedRate(() -> {
    checkUpcomingSessions();
}, 0, 5, TimeUnit.MINUTES);

Platform.runLater(() -> showToastNotification("⏰ Rappel : " + session.getTitre()));
```

### Génération QR code (ZXing)
```java
QRCodeWriter writer = new QRCodeWriter();
BitMatrix matrix  = writer.encode(qrData, BarcodeFormat.QR_CODE, 200, 200);
BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
```

### Intégration Jitsi via WebView
```java
WebView webView = new WebView();
webView.setPrefSize(900, 600);
String roomUrl = "https://meet.jit.si/" + sessionId + "-fluently";
webView.getEngine().load(roomUrl);
jitsiContainer.getChildren().add(webView);
```

### Singleton MyDatabase
```java
MyDatabase.getInstance().getConnection()
if (conn == null || conn.isClosed() || !conn.isValid(2)) {
    MyDatabase.getInstance().reconnect();
}
```

---

## 🌐 APIs externes utilisées

| API | Usage | Endpoint | Coût |
|-----|-------|----------|------|
| **Groq LLaMA 3** | Suggestions de sessions personnalisées | `POST https://api.groq.com/openai/v1/chat/completions` | 🆓 Gratuit |
| **Google Calendar API v3** | Synchronisation des sessions | `https://www.googleapis.com/calendar/v3/calendars` | 🆓 Gratuit |
| **Jitsi Meet** | Visioconférence intégrée (WebView) | `https://meet.jit.si/` | 🆓 Gratuit |
| **Google Meet** | Visioconférence externe (lien) | Via lien généré | 🆓 Gratuit |

---

## 🔑 Règles métier essentielles

1. **Un étudiant ne peut pas réserver deux fois la même session** — vérification avant INSERT
2. **Une session pleine n'accepte plus de réservations** — vérifiée en temps réel
3. **Le QR code est généré à la confirmation uniquement** — pas en statut `en_attente`
4. **Les rappels ne se déclenchent que pour les réservations confirmées** — statut vérifié avant notification
5. **Jitsi s'ouvre dans l'application (WebView)**, Google Meet s'ouvre dans le navigateur
6. **Toute création ou modification de session sync Google Calendar automatiquement** — sans action manuelle de l'admin
7. **Les notifications sont non-bloquantes** — toast 3 secondes, `Platform.runLater()` obligatoire depuis les threads secondaires

---

## 👥 Équipe

| Membre | Module |
|--------|--------|
| **Yosr Ben Hamouda** | Session / Réservation / Google Calendar / Jitsi |
| **Azer Aissaoui** | User / Authentification / Avatar IA / Google Sheets |
| **Emna Bahar** | Test / Question / Réponse / TestPassage |
| **Oumaima Ben Hammou** | Langue / Cours / Niveau / UserProgress |
| **Sarra Ben Boubaker** | Objectif / Tâche / Gamification / IA |
| **Jihed Ramedi** | Groupe / Message / Chat / Modération IA |

---

## 🏫 Contexte académique

Projet réalisé dans le cadre du cours de **Programmation Avancée Java** — ESPRIT School of Engineering, Tunis.

Application web Symfony : `fluently` (base de données partagée)
Application desktop JavaFX : ce module

---

*README mis à jour le 11/05/2026*
