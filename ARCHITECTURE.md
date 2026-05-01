# 🏗️ Architecture du Projet - Skill Bridge

## Vue d'ensemble
**Skill Bridge** est une plateforme de gestion de cours et de quizz basée sur **JavaFX** avec une architecture **MVC** (Model-View-Controller) et une base de données **MySQL**.

---

## 📁 Arborescence du Projet

```
gestionCours/
│
├── src/main/
│   ├── java/com/pidev/
│   │   ├── 📋 Main.java                          [Point d'entrée]
│   │   │
│   │   ├── 👥 Controllers/
│   │   │   ├── admin/                            [Panneau d'administration]
│   │   │   │   ├── AdminDialogStyler.java       [Utilitaire UI pour dialogs]
│   │   │   │   ├── BaseController.java          [Base du backoffice]
│   │   │   │   ├── DashboardController.java     [Vue dashboard]
│   │   │   │   ├── CourseManagementController.java
│   │   │   │   ├── ChapterManagementController.java
│   │   │   │   ├── QuestionManagementController.java
│   │   │   │   ├── AnswerManagementController.java
│   │   │   │   ├── QuizManagementController.java
│   │   │   │   ├── QuizResultsController.java
│   │   │   │   ├── GenerateQuizAIController.java [Génération IA de quiz]
│   │   │   │   └── LearningIntelligenceController.java [Analytics IA]
│   │   │   │
│   │   │   └── client/                           [Interface étudiant]
│   │   │       ├── BaseController.java          [Base du client]
│   │   │       ├── HomeController.java          [Accueil]
│   │   │       ├── CoursesController.java       [Liste des cours]
│   │   │       ├── CourseDetailController.java  [Détails d'un cours]
│   │   │       ├── QuizSessionController.java   [Session de quiz - AVEC TIMER MAGNIFIQUE]
│   │   │       └── User/
│   │   │           └── login_Controller.java    [Connexion/Authentification]
│   │   │
│   │   ├── 📊 models/                            [Modèles de données]
│   │   │   ├── User.java
│   │   │   ├── Course.java
│   │   │   ├── Chapter.java
│   │   │   ├── Quiz.java
│   │   │   ├── Question.java
│   │   │   ├── Answer.java
│   │   │   ├── Challenge.java
│   │   │   ├── QuestionStatistic.java
│   │   │   ├── QuizAttemptDetail.java
│   │   │   ├── QuizStatisticsSummary.java
│   │   │   └── StudentRiskInsight.java          [Modèle pour l'analyse de risque]
│   │   │
│   │   ├── ⚙️  Services/                         [Logique métier & APIs]
│   │   │   ├── ICrud.java                       [Interface CRUD générique]
│   │   │   │
│   │   │   ├── 📚 Gestion de contenu
│   │   │   │   ├── CourseService.java
│   │   │   │   ├── ChapterService.java
│   │   │   │   ├── QuestionService.java
│   │   │   │   ├── AnswerService.java
│   │   │   │   ├── QuizService.java
│   │   │   │   └── CourseAdvancedBusinessService.java
│   │   │   │
│   │   │   ├── 🤖 Intelligence Artificielle
│   │   │   │   ├── GeminiQuizService.java       [Génération quiz via Gemini API]
│   │   │   │   ├── AiFeedbackService.java       [Feedback adapté IA]
│   │   │   │   └── LearningIntelligenceService.java
│   │   │   │
│   │   │   ├── 📈 Analytics & Statistiques
│   │   │   │   ├── QuizStatisticsService.java
│   │   │   │   └── AdminLookupService.java
│   │   │   │
│   │   │   ├── 🌐 Services d'intégration
│   │   │   │   ├── TranslationService.java      [Traduction local avec dictionnaire]
│   │   │   │   ├── SupervisorMailService.java   [Envoi d'alertes par email]
│   │   │   │   ├── CertificateGeneratorService.java [Génération PDF de certificats]
│   │   │   │   └── CoursePdfExportService.java
│   │   │   │
│   │   │   └── 🔧 Utilitaires
│   │   │       └── AdminLookupService.java
│   │   │
│   │   └── 🔌 utils/                            [Utilitaires généraux]
│   │       └── DataSource.java                  [Connexion à la base de données]
│   │
│   └── resources/
│       ├── 🎨 styles/
│       │   ├── home.css                         [Styles principaux]
│       │   ├── dashboard.css
│       │   ├── backoffice.css
│       │   ├── login-style.css
│       │   ├── navbarstyle.css
│       │   └── quiz-timer.css                   [NOUVEAU: Animations timer magnifiques]
│       │
│       ├── 🖼️  images/
│       │   ├── logo.png
│       │   ├── home_pic.jpg
│       │   ├── softskills.jpg
│       │   └── members/                         [Photos des équipes]
│       │
│       └── 📑 Fxml/
│           ├── admin/                           [Interfaces backoffice]
│           │   ├── base_back.fxml
│           │   ├── dashboard.fxml
│           │   ├── course_management.fxml
│           │   ├── chapter_management.fxml
│           │   ├── question_management.fxml
│           │   ├── answer_management.fxml
│           │   ├── quiz_management.fxml
│           │   ├── quiz_results.fxml
│           │   ├── generate_quiz_ai.fxml        [Interface IA]
│           │   └── learning_intelligence.fxml   [Analytics]
│           │
│           └── client/                          [Interfaces étudiant]
│               ├── base.fxml
│               ├── home.fxml
│               ├── CoursesView.fxml
│               ├── CourseDetailView.fxml
│               ├── QuizSessionView.fxml         [AVEC animation timer 🎨]
│               ├── Challenge.fxml
│               ├── HackathonView.fxml
│               ├── GroupsView.fxml
│               ├── JobsView.fxml
│               ├── MyCVView.fxml
│               └── User/
│                   └── (fichiers connexion)
│
├── pom.xml                                      [Dépendances Maven]
├── mvnw / mvnw.cmd                             [Maven Wrapper]
├── build_executable.bat                         [Script de build]
└── README.md
```

---

## 🔄 Flux de Données & Architecture

### 1. **Authentification & Navigation**
```
login_Controller.java
        ↓
User.java (Model)
        ↓
     ↙    ↘
Admin       Client
  ↓           ↓
BaseController (Admin)    BaseController (Client)
  ↓           ↓
Dashboard    Home
```

### 2. **Gestion des Cours**
```
CoursesController (Client)
        ↓
CourseService.java
        ↓
[SELECT * FROM courses]
        ↓
Course.java (Model) ←→ MySQL DB
```

### 3. **Session Quiz en Temps Réel**
```
QuizSessionController.java
        ↓
    ┌---┴───┬────────────────┐
    ↓       ↓                ↓
Timer    Questions      Traduction
(Magnifique UI)  ↓           ↓
   ↓         QuestionService TranslationService
   ↓             ↓           (Dictionnaire local)
Animation    Quiz.java    French/English
  Badge      Answer.java
(Pulse/Flash)
```

### 4. **Génération de Quiz avec IA (Gemini)**
```
GenerateQuizAIController.java
        ↓
GeminiQuizService.java
        ↓
📄 PDF Upload
        ↓
Extraction texte (PDFBox)
        ↓
Appel API Gemini 2.0 Flash
        ↓
JSON Quiz généré
        ↓
Base de données
```

### 5. **Analytics & Feedback IA**
```
QuizAttemptDetail.java
        ↓
QuizStatisticsService.java
        ↓
AiFeedbackService.java
        ↓
StudentRiskInsight.java
        ↓
LearningIntelligenceController.java
        ↓
Dashboard Analytics
```

### 6. **Certificats PDF**
```
QuizSessionController (Après réussite)
        ↓
CertificateGeneratorService.java
        ↓
iText 7 (OpenPDF)
        ↓
QR Code (ZXing)
        ↓
PDF Certifié généré
```

---

## 🗄️ Modèle de Données (MCD Relationnel)

```
User (id, nom, email, role)
  ├── 1:N ──→ Course (id, titre, description, idProfesseur)
  │             ├── 1:N ──→ Chapter (id, titre, contenu, idCours)
  │             │             ├── 1:N ──→ Question (id, contenu, idChapitre)
  │             │             │             ├── 1:N ──→ Answer (id, contenu, estCorrect)
  │             │             │             └── 1:N ──→ QuestionStatistic
  │             │             │
  │             └── 1:N ──→ Quiz (id, titre, scoreMax)
  │                           ├── 1:N ──→ Question
  │                           ├── 1:N ──→ QuizAttemptDetail
  │                           └── 1:N ──→ QuizStatisticsSummary
  │
  └── 1:N ──→ StudentRiskInsight (analyse IA)
```

---

## 🧩 Architecture Techniques

### **Patterns Utilisés**
1. ✅ **MVC** - Model-View-Controller séparation claire
2. ✅ **Service Layer** - Logique métier isolée
3. ✅ **DAO Pattern** - Accès données unifié (ICrud)
4. ✅ **Singleton** - DataSource, Services
5. ✅ **Observer** - JavaFX Events

### **Technologies**
- **UI Framework**: JavaFX 21
- **Database**: MySQL + JDBC
- **APIs**:
  - 🤖 **Gemini 2.0 Flash** (Génération IA de quiz)
  - 🌐 **MyMemory Translate** (Traduction locale)
  - 📧 **Gmail SMTP** (Alertes email)
- **Libraries**:
  - iText 7 (Génération PDF)
  - ZXing (QR Codes)
  - PDFBox (Extraction PDF)
  - JSON (org.json)

### **Modules JavaFX**
```
BuildModule {
    requires java.base
    requires java.sql
    requires java.mail
    requires java.desktop
    requires com.github.librepdf.openpdf
    requires com.google.zxing
    requires com.google.zxing.javase
}
```

---

## 🎯 Fonctionnalités Principales

### 📚 **Pour les Professeurs (Admin)**
| Feature | Controller | Service | Status |
|---------|-----------|---------|--------|
| Gestion Cours | CourseManagement | CourseService | ✅ |
| Gestion Chapitres | ChapterManagement | ChapterService | ✅ |
| Gestion Questions | QuestionManagement | QuestionService | ✅ |
| CRUD Réponses | AnswerManagement | AnswerService | ✅ |
| Création Quiz | QuizManagement | QuizService | ✅ |
| Quiz IA (Gemini) | GenerateQuizAI | GeminiQuizService | ✅ |
| Résultats Quiz | QuizResults | QuizStatisticsService | ✅ |
| Analytics IA | LearningIntelligence | LearningIntelligenceService | ✅ |
| Dashboard | Dashboard | AdminLookupService | ✅ |

### 👨‍🎓 **Pour les Étudiants (Client)**
| Feature | Controller | Service | Status |
|---------|-----------|---------|--------|
| Affichage Cours | CoursesController | CourseService | ✅ |
| Détails Cours | CourseDetailController | CourseService | ✅ |
| Session Quiz | QuizSessionController | QuizService | ✅ |
| Timer Magnifique | QuizSessionController | AnimationFX | ✨ |
| **Traduction** | QuizSessionController | TranslationService | ✅ |
| Certificat PDF | QuizSessionController | CertificateGeneratorService | ✅ |
| Authentification | login_Controller | UserService | ✅ |

---

## 🚀 Points Clés de l'Architecture

### **1. Séparation des Préoccupations**
- Controllers = Gestion UI + User Events
- Services = Logique métier + Transactions DB
- Models = Représentation des données
- Utils = Outils partagés

### **2. Réutilisabilité**
```java
public interface ICrud<T> {
    void create(T entity);
    T read(int id);
    void update(T entity);
    void delete(int id);
}
// Implémenté par: CourseService, QuestionService, etc.
```

### **3. Gestion des Erreurs**
- Try-catch pour DB operations
- Platform.runLater() pour les opérations longues
- Validation des inputs UI

### **4. Performance**
- Cache des traductions
- Lazy loading des questions
- Connexion BD persistent

### **5. Sécurité**
- 🔐 Authentification utilisateur
- 🔑 Variables d'env pour API keys
- ✅ Validation des inputs

---

## 📦 Dépendances Maven Principales

```xml
<!-- JavaFX -->
<dependency>org.openjfx:javafx-controls:21.0.6</dependency>

<!-- Database -->
<dependency>mysql:mysql-connector-java:8.0.33</dependency>

<!-- PDF & QR Code -->
<dependency>com.github.librepdf:openpdf:1.3.30</dependency>
<dependency>com.google.zxing:core:3.5.0</dependency>
<dependency>com.google.zxing:javase:3.5.0</dependency>

<!-- APIs -->
<dependency>org.json:json:20231013</dependency>

<!-- PDF Box -->
<dependency>org.apache.pdfbox:pdfbox:3.0.0</dependency>
```

---

## 🔐 Configuration Required

### Fichier `DataSource.java`
```properties
DB_URL = jdbc:mysql://localhost:3306/skill_bridge
DB_USER = root
DB_PASSWORD = (votre mot de passe)
```

### Variables d'environnement
```
GEMINI_API_KEY = votre_clé_api
GMAIL_SENDER = votre_email@gmail.com
GMAIL_PASSWORD = votre_mot_de_passe_app
```

---

## 🎨 Améliorations Récentes

✨ **v1.2.0 - Premium UI Enhancements**
- ✅ Timer Quiz magnifique (Pulse + Flash animations)
- ✅ Service de traduction local avec dictionnaire
- ✅ Génération de certificats PDF avec QR Code
- ✅ Alertes email superviseur
- ✅ Modèle Gemini 2.0 Flash

✨ **v1.1.0 - IA Features**
- ✅ Génération IA de quiz (Gemini API)
- ✅ Feedback adapté IA
- ✅ Analytics intelligentes

✨ **v1.0.0 - Core Features**
- ✅ CRUD complet (Cours, Quiz, Questions)
- ✅ Session Quiz interactive
- ✅ Authentification utilisateur

---

## 📊 Statistiques du Projet

- **Controllers**: 12 fichiers
- **Services**: 16 fichiers
- **Models**: 11 fichiers
- **Fichiers FXML**: 20+
- **Feuilles CSS**: 6
- **Lignes de code Java**: 3000+
- **API Intégrations**: 3 (Gemini, MyMemory, Gmail)

---

## 🏃‍♂️ Comment Démarrer

```bash
# 1. Compiler
./mvnw clean compile

# 2. Empaqueter
./mvnw package

# 3. Exécuter
java -jar target/gestionCours.jar

# OU avec l'exécutable
./build_executable.bat
```

---

**Architecture conçue pour la scalabilité, maintenabilité et performance!** 🚀

