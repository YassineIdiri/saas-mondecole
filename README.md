# 🎓 Présentation Détaillée : Plateforme LMS Multi-tenant "Mon École Pocket"

---

## 📋 Table des Matières

1. [Vue d'ensemble du projet](#vue-densemble)
2. [Architecture technique](#architecture-technique)
3. [Sécurité & Multi-tenancy](#sécurité--multi-tenancy)
4. [Workflows utilisateurs](#workflows-utilisateurs)
5. [Fonctionnalités détaillées](#fonctionnalités-détaillées)
6. [Points techniques clés](#points-techniques-clés)
7. [Démonstration des flows](#démonstration-des-flows)

---

## 🎯 Vue d'ensemble

### Qu'est-ce que c'est ?

**Mon École Pocket** est une plateforme LMS (Learning Management System) complète permettant à des **organisations** (écoles, entreprises, centres de formation) de créer leur propre espace d'apprentissage isolé avec :

- **Gestion des utilisateurs** (Admin, Professeurs, Étudiants)
- **Création de cours** par les professeurs
- **Suivi de progression** pour les étudiants
- **Isolation totale** entre organisations (multi-tenant)

### Pourquoi "multi-tenant" ?

Imaginez une application unique qui héberge plusieurs écoles :
- École A → Leurs professeurs, leurs étudiants, leurs cours
- École B → Leurs professeurs, leurs étudiants, leurs cours
- École C → Leurs professeurs, leurs étudiants, leurs cours

**Chaque organisation est complètement isolée des autres**, mais toutes partagent la même infrastructure technique.

### Cas d'usage

1. **Entreprise de formation professionnelle** : Chaque client (entreprise) a son propre espace
2. **Réseau d'écoles** : Chaque établissement a son instance isolée
3. **Plateforme SaaS éducative** : Chaque abonné a son organisation privée

---

## 🏗️ Architecture Technique

### Stack Technologique

**Backend :**
- **Spring Boot 4.0.2** (Java)
- **PostgreSQL** (base de données)
- **JWT** pour l'authentification
- **Multi-tenant** avec isolation au niveau BDD

**Frontend :**
- **Angular 18** (zoneless)
- **Tailwind CSS** pour le design
- **Reactive programming** (RxJS)

### Architecture en couches

```
┌─────────────────────────────────────────────┐
│           FRONTEND (Angular 18)             │
│  ┌──────────┬──────────┬──────────┐        │
│  │  Admin   │ Teacher  │ Student  │        │
│  │   UI     │   UI     │   UI     │        │
│  └──────────┴──────────┴──────────┘        │
│           Services (HTTP calls)             │
└─────────────────┬───────────────────────────┘
                  │ REST API
┌─────────────────▼───────────────────────────┐
│          BACKEND (Spring Boot)              │
│  ┌─────────────────────────────────┐        │
│  │     Security Layer               │        │
│  │  - JWT Authentication            │        │
│  │  - Multi-tenant Filter           │        │
│  │  - Role-based Authorization      │        │
│  └─────────────────────────────────┘        │
│  ┌─────────────────────────────────┐        │
│  │     Business Logic               │        │
│  │  - CourseService                 │        │
│  │  - EnrollmentService             │        │
│  │  - UserService                   │        │
│  └─────────────────────────────────┘        │
│  ┌─────────────────────────────────┐        │
│  │     Data Access Layer            │        │
│  │  - Repositories (JPA)            │        │
│  │  - Entities                      │        │
│  └─────────────────────────────────┘        │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         DATABASE (PostgreSQL)               │
│  - organizations                            │
│  - users                                    │
│  - courses                                  │
│  - course_sections                          │
│  - lessons                                  │
│  - course_enrollments                       │
│  - lesson_progress                          │
└─────────────────────────────────────────────┘
```

---

## 🔒 Sécurité & Multi-tenancy

### Comment fonctionne l'isolation multi-tenant ?

#### 1. **Au niveau de la base de données**

Toutes les tables ont une colonne `organization_id` :

```sql
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,  -- ✅ Clé de tenant
    title VARCHAR(200) NOT NULL,
    -- ...
    CONSTRAINT fk_course_organization 
        FOREIGN KEY (organization_id) 
        REFERENCES organizations (id)
);
```

**Toutes les requêtes SQL incluent automatiquement le filtre d'organisation.**

#### 2. **Au niveau du backend (TenantFilter)**

Un filtre Spring intercepte **chaque requête HTTP** pour :

1. **Extraire l'organization_id** depuis :
   - Le JWT (pour les utilisateurs connectés)
   - Le body (pour le login)

2. **Stocker l'ID dans TenantContext** (ThreadLocal)

3. **Toutes les requêtes DB utilisent cet ID**

```java
// Exemple simplifié
@Component
public class TenantFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, ...) {
        // 1. Extraire organization_id du JWT
        Long orgId = jwtService.extractOrganizationId(token);
        
        // 2. Stocker dans le contexte
        TenantContext.setTenantId(orgId);
        
        // 3. Continuer la requête
        filterChain.doFilter(request, response);
        
        // 4. Nettoyer après
        TenantContext.clear();
    }
}
```

**Résultat :** Un professeur de l'École A ne peut **jamais** voir les cours de l'École B, même s'il essaie de forcer l'URL.

#### 3. **Au niveau de l'authentification (JWT)**

Le JWT contient :

```json
{
  "sub": "jean.dupont",           // username
  "userId": 42,
  "organizationId": 1,             // ✅ ID de l'organisation
  "role": "TEACHER",
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Le token lie l'utilisateur à son organisation** : impossible d'accéder aux données d'une autre organisation.

### Sécurité des rôles

3 rôles principaux :

1. **ADMIN** : Gestion complète de l'organisation
2. **TEACHER** : Création et gestion de cours
3. **STUDENT** : Inscription et suivi de cours

**Protection côté backend :**

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {
    // Seuls les admins peuvent supprimer des utilisateurs
}

@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public void createCourse(CreateCourseRequest request) {
    // Les profs et admins peuvent créer des cours
}
```

**Protection côté frontend :**

```typescript
// Guards Angular
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  
  if (!authService.isAdmin()) {
    router.navigate(['/home']);
    return false;
  }
  return true;
};
```

---

## 👥 Workflows Utilisateurs

### 1️⃣ Workflow ORGANISATION (Bootstrap)

**Création d'une nouvelle organisation :**

```
┌─────────────────────────────────────────────────┐
│ 1. Landing Page                                 │
│    └─> "Register Organization"                  │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 2. Formulaire d'inscription                     │
│    - Organization name: "École Strasbourg"      │
│    - Slug: "ecole-strasbourg"                   │
│    - Admin username: "admin"                    │
│    - Admin email: "admin@ecole.fr"              │
│    - Admin password: ********                   │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 3. Backend crée :                               │
│    ✅ Organization (id: 1)                      │
│    ✅ Admin user (organization_id: 1)           │
│    ✅ Hash du password                          │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 4. Redirection vers /login                      │
│    "Your organization is ready!"                │
└─────────────────────────────────────────────────┘
```

**L'organisation est maintenant créée et isolée.**

---

### 2️⃣ Workflow ADMIN

**Mission :** Gérer les utilisateurs (professeurs et étudiants)

#### A. Connexion Admin

```
┌─────────────────────────────────────────────────┐
│ 1. Page Login                                   │
│    - Username: "admin"                          │
│    - Password: ********                         │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 2. Backend vérifie :                            │
│    - Lookup organization_id pour "admin"        │
│    - Vérification password                      │
│    - Génération JWT avec organizationId: 1      │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 3. Frontend stocke JWT                          │
│    - Redirection vers /admin/dashboard          │
└─────────────────────────────────────────────────┘
```

#### B. Dashboard Admin

L'admin voit :

```
╔═══════════════════════════════════════════════╗
║          ADMIN DASHBOARD                      ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  📊 STATS                                     ║
║  ┌──────────┬──────────┬──────────┐          ║
║  │ 15 Users │ 5 Profs  │ 10 Élèves│          ║
║  └──────────┴──────────┴──────────┘          ║
║                                               ║
║  👥 TEACHERS                                  ║
║  ┌─────────────────────────────────────────┐ ║
║  │ Jean Dupont    | Active  | [🔒] [❌]   │ ║
║  │ Marie Martin   | Active  | [🔒] [❌]   │ ║
║  └─────────────────────────────────────────┘ ║
║                                               ║
║  🎓 STUDENTS                                  ║
║  ┌─────────────────────────────────────────┐ ║
║  │ Pierre Durand  | Active  | [🔒] [❌]   │ ║
║  │ Sophie Bernard | Locked  | [🔓] [❌]   │ ║
║  └─────────────────────────────────────────┘ ║
╚═══════════════════════════════════════════════╝
```

**Actions disponibles :**
- ✅ **Toggle Active/Inactive** : Désactiver un utilisateur
- ✅ **Toggle Lock/Unlock** : Verrouiller un compte
- ✅ **Delete User** : Supprimer définitivement
- ✅ **Pagination** : Naviguer entre les pages

#### C. Invitation d'utilisateurs

**Flow d'invitation :**

```
Admin génère un lien d'invitation
         │
         ▼
Envoie le lien à jean.dupont@ecole.fr
         │
         ▼
Jean clique sur le lien
         │
         ▼
Page "Join Organization"
   - Email pré-rempli
   - Choix de username
   - Choix de password
   - Sélection du rôle (TEACHER/STUDENT)
         │
         ▼
Backend crée le user avec organization_id = 1
         │
         ▼
Jean peut se connecter
```

---

### 3️⃣ Workflow TEACHER (Professeur)

**Mission :** Créer et gérer des cours

#### A. Connexion Professeur

Identique au login admin, mais avec `role: TEACHER` dans le JWT.

**Redirection automatique vers `/teacher/courses`**

#### B. Liste des cours

```
╔═══════════════════════════════════════════════╗
║          MY COURSES                           ║
╠═══════════════════════════════════════════════╣
║  [All] [Published] [Draft]                   ║
║                                  [+ Create]   ║
║                                               ║
║  ┌─────────────────────────────────────────┐ ║
║  │ 📘 Introduction to JavaScript            │ ║
║  │ Beginner | Programming                   │ ║
║  │ [Published] 25 students                  │ ║
║  │ Created Jan 15, 2026                     │ ║
║  │                                           │ ║
║  │ [Edit] [Unpublish] [Delete]              │ ║
║  └─────────────────────────────────────────┘ ║
║                                               ║
║  ┌─────────────────────────────────────────┐ ║
║  │ 📗 Advanced React Patterns               │ ║
║  │ Advanced | Programming                   │ ║
║  │ [Draft] 0 students                       │ ║
║  │ Created Feb 10, 2026                     │ ║
║  │                                           │ ║
║  │ [Edit] [Publish] [Delete]                │ ║
║  └─────────────────────────────────────────┘ ║
╚═══════════════════════════════════════════════╝
```

**Filtres disponibles :**
- **All** : Tous les cours
- **Published** : Cours visibles par les étudiants
- **Draft** : Cours en cours de création

#### C. Création d'un cours

**Formulaire en 2 sections :**

**Section 1 : Basic Information**
```
┌─────────────────────────────────────────┐
│ Course Title *                          │
│ ┌─────────────────────────────────────┐ │
│ │ Introduction to Python              │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Summary                                 │
│ ┌─────────────────────────────────────┐ │
│ │ Learn Python from scratch...        │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Category          Level                │
│ ┌──────────────┐ ┌──────────────────┐  │
│ │ Programming  │ │ BEGINNER         │  │
│ └──────────────┘ └──────────────────┘  │
│                                         │
│ Estimated Hours                         │
│ ┌─────────────────────────────────────┐ │
│ │ 20                                  │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Section 2 : Detailed Information**
```
┌─────────────────────────────────────────┐
│ Course Description                      │
│ ┌─────────────────────────────────────┐ │
│ │ This course covers...               │ │
│ │                                     │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Learning Objectives                     │
│ ┌─────────────────────────────────────┐ │
│ │ - Write Python programs             │ │
│ │ - Understand OOP concepts           │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Prerequisites                           │
│ ┌─────────────────────────────────────┐ │
│ │ Basic computer skills               │ │
│ └─────────────────────────────────────┘ │
│                                         │
│        [Cancel]  [Create Course]        │
└─────────────────────────────────────────┘
```

**Backend crée :**
- ✅ Course avec `organization_id = 1`
- ✅ `author_id = professor_id`
- ✅ `slug` auto-généré (ex: "introduction-to-python")
- ✅ `published = false` (draft par défaut)

#### D. Publication d'un cours

**Avant publication :** Le cours est invisible pour les étudiants

**Clic sur "Publish" :**
```
Course.published = true
Course.publishedAt = NOW()
```

**Après publication :** Le cours apparaît dans le catalogue étudiant

---

### 4️⃣ Workflow STUDENT (Étudiant)

**Mission :** Découvrir, s'inscrire, et suivre des cours

#### A. Dashboard Étudiant

```
╔═══════════════════════════════════════════════╗
║       MY LEARNING DASHBOARD                   ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  📊 STATS                                     ║
║  ┌──────────┬──────────┬──────────┬────────┐ ║
║  │ 5 Cours  │ 3 En     │ 2 Termi- │ 65%    │ ║
║  │ Inscrits │ Cours    │ nés      │ Moy.   │ ║
║  └──────────┴──────────┴──────────┴────────┘ ║
║                                               ║
║  🔥 CONTINUE LEARNING                         ║
║  ┌─────────────────────────────────────────┐ ║
║  │ Introduction to Python                  │ ║
║  │ Prof. Jean Dupont                       │ ║
║  │ ████████░░░░░░░░░░ 45%                  │ ║
║  │ Last accessed: 2 days ago               │ ║
║  └─────────────────────────────────────────┘ ║
║                                               ║
║  📚 QUICK ACCESS                              ║
║  [Browse Catalog] [My Courses] [Certificates]║
╚═══════════════════════════════════════════════╝
```

#### B. Catalogue de cours

```
╔═══════════════════════════════════════════════╗
║          COURSE CATALOG                       ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  ┌────────┐  ┌────────┐  ┌────────┐          ║
║  │ 📘     │  │ 📗     │  │ 📙     │          ║
║  │        │  │        │  │        │          ║
║  │ Python │  │ React  │  │ DevOps │          ║
║  │        │  │        │  │        │          ║
║  │ BEGIN. │  │ ADV.   │  │ INTER. │          ║
║  │ 20h    │  │ 15h    │  │ 30h    │          ║
║  │ 42 🎓  │  │ 18 🎓  │  │ 25 🎓  │          ║
║  │        │  │        │  │        │          ║
║  │[ENROLL]│  │███ 30% │  │[ENROLL]│          ║
║  └────────┘  └────────┘  └────────┘          ║
║                                               ║
║         [Previous] Page 1 of 5 [Next]         ║
╚═══════════════════════════════════════════════╝
```

**Légende :**
- 📘 Cours disponible
- ███ 30% = Cours déjà inscrit avec progression
- 🎓 Nombre d'étudiants inscrits

#### C. Détail d'un cours

**Page de détail :**

```
╔═══════════════════════════════════════════════╗
║    [← Back to Catalog]                        ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  [BEGINNER] [Programming]                     ║
║                                               ║
║  Introduction to Python                       ║
║  Learn Python from scratch with hands-on...   ║
║                                               ║
║  👨‍🏫 Jean Dupont  |  👥 42 students  |  ⏱️ 20h ║
║                                               ║
║  ┌──────────────────────────────────────────┐║
║  │  📸 [Course Thumbnail]                   │║
║  │                                          │║
║  │          [ENROLL NOW]                    │║
║  │                                          │║
║  │  Total Lessons: 25                       │║
║  │  Duration: 20 hours                      │║
║  │  Language: FR                            │║
║  └──────────────────────────────────────────┘║
║                                               ║
║  📖 ABOUT THIS COURSE                         ║
║  This comprehensive Python course...          ║
║                                               ║
║  🎯 WHAT YOU'LL LEARN                         ║
║  - Write Python programs                      ║
║  - Understand OOP concepts                    ║
║  - Build real projects                        ║
║                                               ║
║  📚 COURSE CURRICULUM                         ║
║  1. Introduction                              ║
║     ▢ What is Python?        [🎥 5min]       ║
║     ▢ Setup Environment      [📄 10min]      ║
║  2. Variables & Data Types                    ║
║     ▢ Numbers                [🎥 15min]      ║
║     ▢ Strings                [🎥 20min]      ║
║     ▢ Quiz                   [📝 10min]      ║
╚═══════════════════════════════════════════════╝
```

**Clic sur "Enroll Now" :**

Backend crée :
```java
CourseEnrollment enrollment = {
    organizationId: 1,
    studentId: 42,
    courseId: 5,
    progressPercent: 0,
    completed: false
}
```

**Page rafraîchie :**
- ❌ "Enroll Now" disparaît
- ✅ "Start Learning" apparaît
- ✅ Progress bar à 0%
- ✅ Leçons deviennent cliquables

#### D. Viewer de leçon

**Interface du player :**

```
╔═══════════════════════════════════════════════╗
║ [✕] Introduction | Lesson 1 of 25    [65% ✓] ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  ┌─────────────────────────────────────────┐ ║
║  │                                         │ ║
║  │         [VIDEO PLAYER]                  │ ║
║  │      ▶️ What is Python?                 │ ║
║  │                                         │ ║
║  │    ━━━━━━━━━━━━━━━━░░░░  2:30 / 5:00   │ ║
║  │                                         │ ║
║  └─────────────────────────────────────────┘ ║
║                                               ║
║  📖 ABOUT THIS LESSON                         ║
║  In this lesson, you'll discover what Python  ║
║  is and why it's one of the most popular...   ║
║                                               ║
║  ┌──────────────────────────┐                ║
║  │   [✓ Mark as Complete]   │                ║
║  └──────────────────────────┘                ║
║                                               ║
║  [← Previous Lesson]    [Next Lesson →]      ║
║                                               ║
╠═══════════════════════════════════════════════╣
║  YOUR PROGRESS                                ║
║  ┌──────────────────┐                        ║
║  │       65%        │ Status: In Progress    ║
║  │    ◐◐◐◐◑◑◑◑◑◑    │ Lesson: 1/25           ║
║  └──────────────────┘ Duration: 5 min         ║
╚═══════════════════════════════════════════════╝
```

**Types de leçons supportés :**

1. **VIDEO** 🎥
   - Player vidéo intégré (YouTube, Vimeo)
   - Sauvegarde automatique de la position
   - Durée trackée

2. **TEXT** 📝
   - Contenu textuel formaté
   - Auto-progression toutes les 10 secondes
   - Scroll tracking

3. **DOCUMENT** 📄
   - Fichiers PDF, DOCX téléchargeables
   - Bouton de téléchargement
   - Marquage manuel de complétion

4. **QUIZ** 📝
   - (Placeholder pour l'instant)
   - Questions à choix multiples
   - Scoring automatique

5. **ASSIGNMENT** 📋
   - (Placeholder pour l'instant)
   - Upload de devoirs
   - Correction par le prof

**Tracking de progression :**

```java
// À chaque interaction
LessonProgress progress = {
    studentId: 42,
    lessonId: 1,
    progressPercent: 45,        // Auto-incrémenté
    lastPositionSeconds: 150,   // Pour les vidéos
    completed: false,
    viewCount: 3
}

// Sauvegarde toutes les 10 secondes
```

**Calcul de la progression du cours :**

```
Progression = (Leçons complétées / Total leçons) × 100

Exemple :
- Total leçons : 25
- Complétées : 16
- Progression : 64%
```

**Complétion automatique du cours :**

```java
if (progressPercent == 100 && !enrollment.completed) {
    enrollment.completed = true;
    enrollment.completedAt = NOW();
    
    // 🎉 Génération du certificat (à venir)
}
```

---

## 🎯 Fonctionnalités Détaillées

### 1. Système d'authentification

#### A. Login multi-étapes

```
┌─────────────────────────────────────────┐
│ 1. User entre username "monprof"        │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 2. Frontend appelle:                    │
│    GET /api/auth/user-organization      │
│    ?username=monprof                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 3. Backend répond:                      │
│    {                                    │
│      organizationId: 1,                 │
│      organizationName: "École Stras",   │
│      organizationSlug: "ecole-stras",   │
│      userRole: "TEACHER"                │
│    }                                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 4. Frontend affiche:                    │
│    "Logging in to École Strasbourg"     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 5. User entre password                  │
│    Frontend envoie:                     │
│    POST /api/auth/login                 │
│    Headers: X-Organization-Id: 1        │
│    Body: {username, password}           │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 6. Backend:                             │
│    - Vérifie password                   │
│    - Génère JWT avec orgId: 1           │
│    - Set refresh_token cookie (30j)     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 7. Frontend:                            │
│    - Stocke access_token (1h)           │
│    - Redirige selon rôle                │
└─────────────────────────────────────────┘
```

**Pourquoi cette approche ?**

- ✅ Empêche l'énumération d'utilisateurs
- ✅ Affiche le nom de l'organisation (UX)
- ✅ Valide l'existence de l'utilisateur avant le password
- ✅ Multi-tenant transparent pour l'utilisateur

#### B. Refresh token automatique

**Problème :** Access token expire après 1h

**Solution :** Refresh token (HttpOnly cookie, 30 jours)

```
┌─────────────────────────────────────────┐
│ User navigue sur la plateforme          │
│ Access token expire après 1h            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ Frontend Interceptor détecte:           │
│ - Token expiré (avant envoi)            │
│ - Ou reçoit 401                         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ Interceptor appelle automatiquement:    │
│ POST /api/auth/refresh                  │
│ (Cookie refresh_token envoyé auto)      │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ Backend:                                │
│ - Valide refresh token                  │
│ - Génère nouveau access token           │
│ - Rotate refresh token (optionnel)      │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ Frontend:                               │
│ - Stocke nouveau token                  │
│ - Replay requête originale              │
│ - User ne voit rien (transparent)       │
└─────────────────────────────────────────┘
```

**Sécurité :**
- ✅ Refresh token dans HttpOnly cookie (pas accessible en JS)
- ✅ Rotation du refresh token à chaque usage
- ✅ Access token court (1h) limite l'exposition
- ✅ Refresh long (30j) évite les logins fréquents

#### C. Guards Angular

**Protection des routes :**

```typescript
// Route protégée
{
  path: 'admin/dashboard',
  component: AdminDashboardComponent,
  canActivate: [adminGuard]  // ✅ Seuls les admins
}

// Guard implementation
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  
  // Pas de token → /login
  if (!authService.getToken()) {
    router.navigate(['/login']);
    return false;
  }
  
  // Token expiré → Refresh puis retry
  if (!authService.isLoggedIn()) {
    return authService.refreshToken().pipe(
      map(() => {
        // Après refresh, vérifier admin
        if (!authService.isAdmin()) {
          router.navigate(['/home']);
          return false;
        }
        return true;
      })
    );
  }
  
  // Token valide mais pas admin → /home
  if (!authService.isAdmin()) {
    router.navigate(['/home']);
    return false;
  }
  
  return true;
};
```

---

### 2. Gestion des cours (Teacher)

#### A. CRUD Complet

**Create :**
```
POST /api/courses
Body: {
  title: "Python pour débutants",
  summary: "Apprenez Python...",
  description: "Ce cours complet...",
  category: "Programming",
  level: "BEGINNER",
  estimatedHours: 20,
  objectives: "- Écrire des programmes...",
  prerequisites: "Aucun"
}

Backend:
- Génère slug: "python-pour-debutants"
- Set organization_id depuis TenantContext
- Set author_id depuis JWT
- published = false (draft)
```

**Read :**
```
GET /api/courses/my-courses?page=0&size=10&published=true

Backend:
- WHERE organization_id = 1
- AND author_id = 42
- AND published = true
- ORDER BY created_at DESC
```

**Update :**
```
PUT /api/courses/5
Body: {
  title: "Python pour débutants (mise à jour)"
}

Backend:
- WHERE id = 5 
- AND organization_id = 1
- AND author_id = 42  // ✅ Vérification ownership
```

**Delete :**
```
DELETE /api/courses/5

Backend:
- WHERE id = 5
- AND organization_id = 1
- AND author_id = 42  // ✅ Only owner can delete
```

#### B. Publication

**Draft → Published :**

```java
@Transactional
public CourseDetailResponse publishCourse(Long courseId, Long authorId) {
    Course course = findCourseByAuthor(courseId, authorId);
    
    if (course.published) {
        throw new IllegalStateException("Already published");
    }
    
    course.published = true;
    course.publishedAt = LocalDateTime.now();
    
    return save(course);
}
```

**Impact :**
- ✅ Le cours apparaît dans le catalogue étudiant
- ✅ Les étudiants peuvent s'inscrire
- ✅ Les stats "Total students" commencent à compter

**Published → Unpublished :**

```java
@Transactional
public CourseDetailResponse unpublishCourse(Long courseId, Long authorId) {
    Course course = findCourseByAuthor(courseId, authorId);
    
    course.published = false;
    // publishedAt reste (historique)
    
    return save(course);
}
```

**Impact :**
- ❌ Le cours disparaît du catalogue
- ✅ Les étudiants déjà inscrits peuvent continuer
- ℹ️ Nouvelles inscriptions bloquées

---

### 3. Inscription & Progression (Student)

#### A. Enrollment (Inscription)

**Workflow complet :**

```
┌─────────────────────────────────────────┐
│ 1. Student browse catalog               │
│    GET /api/student/courses/catalog     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 2. Student clique sur un cours          │
│    GET /api/student/courses/5           │
│                                         │
│    Backend retourne:                    │
│    - Course details                     │
│    - Sections & Lessons                 │
│    - enrollment: null (pas inscrit)     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 3. Student clique "Enroll Now"          │
│    POST /api/student/courses/5/enroll   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 4. Backend crée:                        │
│    CourseEnrollment {                   │
│      organizationId: 1,                 │
│      studentId: 42,                     │
│      courseId: 5,                       │
│      progressPercent: 0,                │
│      completed: false,                  │
│      enrolledAt: NOW()                  │
│    }                                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 5. Frontend reload course detail        │
│    enrollment: {                        │
│      enrollmentId: 123,                 │
│      progressPercent: 0,                │
│      completed: false                   │
│    }                                    │
│                                         │
│    UI change:                           │
│    ❌ "Enroll Now"                      │
│    ✅ "Start Learning"                  │
│    ✅ Lessons clickable                 │
└─────────────────────────────────────────┘
```

#### B. Lesson Progress Tracking

**Auto-tracking (VIDEO) :**

```javascript
// Dans le player vidéo
videoPlayer.on('timeupdate', () => {
  const progress = (currentTime / duration) * 100;
  
  // Sauvegarder toutes les 10 secondes
  if (Date.now() - lastSave > 10000) {
    studentCourseService.updateLessonProgress(lessonId, {
      progressPercent: Math.floor(progress),
      lastPositionSeconds: currentTime,
      completed: progress >= 95  // 95% = completed
    });
    lastSave = Date.now();
  }
});
```

**Auto-tracking (TEXT) :**

```javascript
// Incrémentation progressive
interval(10000).subscribe(() => {
  if (currentProgress < 90) {
    updateProgress(currentProgress + 10);
  }
});

// Manual completion button
markAsCompleted() {
  updateProgress(100, completed: true);
}
```

**Backend met à jour :**

```java
@Transactional
public void updateLessonProgress(...) {
    // 1. Update lesson progress
    LessonProgress progress = findOrCreate(studentId, lessonId);
    progress.progressPercent = request.progressPercent;
    progress.lastPositionSeconds = request.lastPositionSeconds;
    
    if (request.completed && !progress.completed) {
        progress.completed = true;
        progress.completedAt = NOW();
    }
    
    save(progress);
    
    // 2. Recalculate course progress
    updateCourseProgress(studentId, courseId);
}

private void updateCourseProgress(Long studentId, Long courseId) {
    // Count total lessons
    int totalLessons = lessonRepository.countByCourse(courseId);
    
    // Count completed lessons
    int completedLessons = progressRepository
        .countCompletedByCourse(studentId, courseId);
    
    // Calculate percentage
    int progressPercent = (completedLessons * 100) / totalLessons;
    
    // Update enrollment
    CourseEnrollment enrollment = findEnrollment(studentId, courseId);
    enrollment.progressPercent = progressPercent;
    
    if (progressPercent == 100 && !enrollment.completed) {
        enrollment.completed = true;
        enrollment.completedAt = NOW();
        // 🎉 Trigger certificate generation
    }
    
    save(enrollment);
}
```

#### C. Navigation entre leçons

**Backend calcule automatiquement :**

```java
public LessonContent getLessonContent(Long lessonId, Long studentId) {
    // ... get lesson ...
    
    // Get all lessons in course (ordered)
    List<Lesson> allLessons = findAllByCourse(courseId);
    
    // Find current lesson index
    int currentIndex = findIndex(allLessons, lessonId);
    
    // Calculate navigation
    Long previousLessonId = currentIndex > 0 
        ? allLessons.get(currentIndex - 1).id 
        : null;
    
    Long nextLessonId = currentIndex < allLessons.size() - 1
        ? allLessons.get(currentIndex + 1).id
        : null;
    
    return LessonContent {
        // ... lesson data ...
        navigation: {
            previousLessonId,
            nextLessonId,
            lessonNumber: currentIndex + 1,
            totalLessons: allLessons.size()
        }
    };
}
```

**Frontend utilise :**

```html
<!-- Previous button -->
<button *ngIf="lesson.navigation.previousLessonId"
        (click)="goToPrevious()">
  ← Previous Lesson
</button>

<!-- Next button -->
<button *ngIf="lesson.navigation.nextLessonId"
        (click)="goToNext()">
  Next Lesson →
</button>

<!-- End of course -->
<div *ngIf="!lesson.navigation.nextLessonId">
  <a routerLink="/student/courses/my-courses">
    🎉 Course Complete! Back to My Courses
  </a>
</div>
```

---

## 🔧 Points Techniques Clés

### 1. Isolation Multi-tenant

**Niveau 1 : Base de données**

Chaque table a `organization_id` :

```sql
-- TOUTES les requêtes incluent organization_id
SELECT * FROM courses 
WHERE organization_id = 1 
AND published = true;

-- Les Foreign Keys garantissent l'intégrité
CONSTRAINT fk_course_organization 
    FOREIGN KEY (organization_id) 
    REFERENCES organizations (id)
```

**Niveau 2 : Application (TenantFilter)**

```java
@Component
public class TenantFilter extends OncePerRequestFilter {
    
    protected void doFilterInternal(...) {
        try {
            // Extract organization_id from JWT or body
            Long orgId = resolveOrganizationId(request);
            
            // Store in thread-local
            TenantContext.setTenantId(orgId);
            
            // Continue request
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clean up
            TenantContext.clear();
        }
    }
}

// Thread-local storage
public class TenantContext {
    private static ThreadLocal<Long> tenantId = new ThreadLocal<>();
    
    public static void setTenantId(Long id) {
        tenantId.set(id);
    }
    
    public static Long getTenantId() {
        return tenantId.get();
    }
    
    public static void clear() {
        tenantId.remove();
    }
}
```

**Niveau 3 : Service Layer**

```java
@Service
public class CourseService {
    
    public Page<Course> getMyCourses(...) {
        // ✅ Toujours récupérer orgId depuis TenantContext
        Long organizationId = TenantContext.getTenantId();
        
        // ✅ TOUTES les requêtes incluent organization_id
        return courseRepository.findByOrganizationIdAndAuthorId(
            organizationId, authorId, pageable
        );
    }
}
```

**Résultat :**
- ✅ Impossible d'accéder aux données d'une autre organisation
- ✅ Même si on force l'ID dans l'URL
- ✅ Même si on modifie le JWT (signature invalide)

---

### 2. Architecture Frontend (Zoneless Angular)

**Pourquoi Zoneless ?**

Angular classique utilise Zone.js pour détecter les changements :

```typescript
// Avec Zone.js (classique)
button.addEventListener('click', () => {
  this.counter++;  // Zone.js détecte et déclenche change detection
});
```

**Problème :** Performance overhead, bugs subtils

**Solution : Zoneless (Angular 18)**

```typescript
// Sans Zone.js (zoneless)
import { ChangeDetectorRef } from '@angular/core';

constructor(private cdr: ChangeDetectorRef) {}

button.addEventListener('click', () => {
  this.counter++;
  this.cdr.markForCheck();  // ✅ Explicite
});
```

**Notre helper `ui(cdr)` :**

```typescript
// ui.helper.ts
export function ui(cdr: ChangeDetectorRef): Ui {
  return {
    set: (fn: () => void) => {
      fn();
      cdr.markForCheck();
    },
    pipeRepaint: () => tap(() => cdr.markForCheck())
  };
}

// Usage
private ui: Ui;

constructor(cdr: ChangeDetectorRef) {
  this.ui = ui(cdr);
}

loadCourses() {
  this.ui.set(() => {
    this.loading = true;  // Change state
  });  // ✅ Auto mark for check
  
  this.service.getCourses()
    .pipe(this.ui.pipeRepaint())  // ✅ Auto mark on response
    .subscribe(courses => {
      this.ui.set(() => {
        this.courses = courses;
        this.loading = false;
      });
    });
}
```

**Avantages :**
- ✅ Performance accrue (pas de Zone.js)
- ✅ Change detection explicite et contrôlée
- ✅ Pas de bugs liés à Zone.js

---

### 3. SSR (Server-Side Rendering) Compatibility

**Problème :** Angular peut faire du rendu côté serveur (SSR) où `localStorage` n'existe pas.

**Solution :**

```typescript
@Injectable()
export class TokenStore {
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);
  
  get(): string | null {
    if (!this.isBrowser) return null;  // ✅ SSR-safe
    return localStorage.getItem('accessToken');
  }
  
  set(token: string): void {
    if (!this.isBrowser) return;  // ✅ SSR-safe
    localStorage.setItem('accessToken', token);
  }
}
```

**Guards SSR-safe :**

```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const platformId = inject(PLATFORM_ID);
  
  // ✅ SSR : Skip auth check, let client handle it
  if (!isPlatformBrowser(platformId)) {
    return true;
  }
  
  // Client-side auth check
  const authService = inject(AuthService);
  return authService.isLoggedIn();
};
```

---

### 4. HTTP Interceptor (Token Management)

**Rôles de l'intercepteur :**

1. **Ajouter automatiquement le JWT**
2. **Ajouter automatiquement X-Organization-Id**
3. **Gérer le refresh automatique**
4. **Retry sur 401**

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStore = inject(TokenStore);
  const authService = inject(AuthService);
  
  // 1. Skip auth routes
  if (req.url.includes('/api/auth/')) {
    return next(req);
  }
  
  // 2. Add Bearer token
  const token = tokenStore.get();
  let request = token 
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` }})
    : req;
  
  // 3. Add organization ID
  const orgId = authService.getOrganizationId();
  if (orgId) {
    request = request.clone({
      setHeaders: { 'X-Organization-Id': orgId.toString() }
    });
  }
  
  // 4. Handle response
  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      // Only handle 401
      if (err.status !== 401) {
        return throwError(() => err);
      }
      
      // Refresh token
      return authService.refreshToken().pipe(
        switchMap(newToken => {
          // Retry with new token
          const retryRequest = request.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` }
          });
          return next(retryRequest);
        }),
        catchError(refreshErr => {
          // Refresh failed → logout
          authService.logoutAndRedirect();
          return throwError(() => refreshErr);
        })
      );
    })
  );
};
```

---

### 5. Slugification

**Pourquoi slugifier ?**

Les titres de cours contiennent souvent des caractères spéciaux :

- "Introduction à Python" → URL illisible
- Espaces, accents, caractères spéciaux

**Solution :**

```java
private String slugify(String text) {
    return text.toLowerCase()
        .replaceAll("[àáâãäå]", "a")
        .replaceAll("[èéêë]", "e")
        .replaceAll("[ìíîï]", "i")
        .replaceAll("[òóôõö]", "o")
        .replaceAll("[ùúûü]", "u")
        .replaceAll("[ç]", "c")
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
}

// "Introduction à Python" → "introduction-a-python"
```

**Unicité garantie :**

```java
private String generateUniqueSlug(String title, Long orgId) {
    String baseSlug = slugify(title);  // "python"
    String slug = baseSlug;
    int counter = 1;
    
    while (courseRepository.existsByOrganizationIdAndSlug(orgId, slug)) {
        slug = baseSlug + "-" + counter;  // "python-1", "python-2"
        counter++;
    }
    
    return slug;
}
```

---

## 🎬 Démonstration des Flows

### Scénario Complet : De la création d'organisation à la certification

#### Jour 1 : Setup

**09:00 - Admin crée l'organisation**

```
1. Visite /organization/register
2. Remplit :
   - Name: "École de Code Strasbourg"
   - Slug: "ecole-code-67"
   - Admin: "admin@ecole67.fr" / "admin" / "password123"
3. Soumission
4. Backend crée :
   ✅ Organization (id: 1)
   ✅ Admin user (id: 1, organization_id: 1, role: ADMIN)
5. Redirect /login
```

**09:05 - Admin se connecte**

```
1. Login avec "admin" / "password123"
2. Backend :
   - Lookup organization_id pour "admin" → 1
   - Génère JWT avec organizationId: 1, role: ADMIN
3. Redirect /admin/dashboard
```

**09:10 - Admin invite un professeur**

```
1. Dashboard admin → Génère lien d'invitation
2. Copie lien : https://app.com/join?token=abc123
3. Envoie email à jean.dupont@ecole67.fr
```

**09:30 - Jean rejoint l'organisation**

```
1. Jean clique sur le lien
2. Page /join affiche :
   "Join École de Code Strasbourg"
   Email: jean.dupont@ecole67.fr (pré-rempli)
3. Jean choisit :
   Username: "jdupont"
   Password: "******"
   Role: TEACHER
4. Backend crée :
   ✅ User (id: 2, organization_id: 1, role: TEACHER)
5. Redirect /login
```

#### Jour 2 : Création de cours

**10:00 - Jean crée un cours**

```
1. Login "jdupont" → Redirect /teacher/courses
2. Clique "Create Course"
3. Remplit formulaire :
   - Title: "Python pour débutants"
   - Summary: "Apprenez Python de zéro"
   - Category: "Programming"
   - Level: "BEGINNER"
   - Estimated Hours: 20
   - Description: "Ce cours complet vous apprendra..."
   - Objectives: "- Écrire des programmes Python..."
   - Prerequisites: "Aucun prérequis"
4. Soumission
5. Backend crée :
   ✅ Course (id: 1, organization_id: 1, author_id: 2, published: false)
   ✅ Slug: "python-pour-debutants"
6. Jean voit son cours en "Draft"
```

**10:30 - Jean publie le cours**

```
1. Jean clique "Publish"
2. Backend :
   Course.published = true
   Course.publishedAt = NOW()
3. ✅ Cours maintenant visible dans le catalogue étudiant
```

#### Jour 3 : Étudiant s'inscrit

**14:00 - Admin invite un étudiant**

```
1. Admin génère lien → marie.martin@gmail.com
2. Marie rejoint :
   Username: "mmartin"
   Password: "******"
   Role: STUDENT
3. Backend crée :
   ✅ User (id: 3, organization_id: 1, role: STUDENT)
```

**14:15 - Marie découvre le catalogue**

```
1. Login "mmartin" → Redirect /student/dashboard
2. Dashboard vide (pas encore de cours)
3. Clique "Browse Catalog"
4. Voit :
   ┌────────────────────────────┐
   │ 📘 Python pour débutants   │
   │ BEGINNER | Programming     │
   │ 20h | 0 students            │
   │ [ENROLL NOW]               │
   └────────────────────────────┘
```

**14:20 - Marie s'inscrit au cours**

```
1. Clique sur le cours → Page détail
2. Voit curriculum :
   1. Introduction (0/3 lessons)
   2. Variables (0/5 lessons)
   ...
3. Clique "Enroll Now"
4. Backend crée :
   ✅ CourseEnrollment (id: 1, student_id: 3, course_id: 1, progress: 0%)
5. Page rafraîchie :
   ❌ "Enroll Now" disparaît
   ✅ "Start Learning" apparaît
   ✅ Progress bar 0%
```

**14:25 - Marie commence le cours**

```
1. Clique "Start Learning"
2. Redirect /student/lessons/1 (première leçon)
3. Voit :
   ┌─────────────────────────────────┐
   │ 🎥 What is Python?              │
   │ [VIDEO PLAYER]                  │
   │ Duration: 5:00                  │
   │                                 │
   │ Progress: 0% | Lesson 1/25      │
   │                                 │
   │ [Mark as Complete]              │
   │ [Next Lesson →]                 │
   └─────────────────────────────────┘
```

**14:30 - Marie regarde la vidéo**

```
1. Play vidéo
2. Pendant la lecture :
   - Toutes les 10 secondes :
     Backend reçoit :
     progressPercent: 20 (puis 40, 60, 80)
     lastPositionSeconds: 60 (puis 120, 180, 240)
3. À 95% de la vidéo :
   Backend marque :
   LessonProgress.completed = true
   LessonProgress.completedAt = NOW()
4. UI affiche :
   ✅ "Lesson Completed!"
   Progress: 100%
```

**14:35 - Marie navigue vers la leçon suivante**

```
1. Clique "Next Lesson"
2. Backend met à jour :
   CourseEnrollment.progressPercent = 4%  (1/25 leçons)
3. Marie sur leçon 2 : "Setup Environment"
```

#### Semaine suivante : Progression

**Marie continue le cours sur plusieurs jours :**

```
Jour 4 : 5 leçons complétées → 20% progress
Jour 5 : 3 leçons complétées → 32% progress
Jour 8 : 7 leçons complétées → 60% progress
Jour 10 : 5 leçons complétées → 80% progress
Jour 12 : 5 leçons complétées → 100% progress
```

**Jour 12 - Complétion du cours**

```
1. Marie complète la dernière leçon
2. Backend détecte :
   completedLessons = 25
   totalLessons = 25
   progressPercent = 100
3. Backend met à jour :
   CourseEnrollment.completed = true
   CourseEnrollment.completedAt = NOW()
   
   🎉 Génération du certificat (à venir)
4. Dashboard Marie :
   📊 Stats mises à jour :
   - Enrolled: 1
   - Completed: 1 ✅
   - In Progress: 0
   - Avg Progress: 100%
```

---

## 💡 Idées d'Améliorations Futures

### Court terme (1-2 semaines)

1. **Certificates PDF**
   - Génération automatique à 100%
   - Template personnalisable par organisation
   - QR code de vérification

2. **Quiz interactif**
   - Questions à choix multiples
   - Scoring automatique
   - Minimum 70% pour valider

3. **Assignments (devoirs)**
   - Upload de fichiers par les étudiants
   - Correction par le professeur
   - Notes et commentaires

4. **Search & Filters avancés**
   - Recherche full-text dans le catalogue
   - Filtres : catégorie, niveau, durée
   - Tri : popularité, date, note

### Moyen terme (1-2 mois)

5. **Discussions par cours**
   - Forum Q&A par cours
   - Réponses du professeur
   - Upvote des meilleures réponses

6. **Notifications**
   - Email : nouveau cours disponible
   - Push : rappel de cours non terminés
   - Digest hebdomadaire de progression

7. **Analytics avancés**
   - Dashboard professeur : taux de complétion, temps moyen
   - Dashboard admin : engagement, retention
   - Heatmaps de progression

8. **Video hosting**
   - Upload direct de vidéos
   - Conversion automatique
   - Streaming optimisé

### Long terme (3-6 mois)

9. **Live classes**
   - Visioconférence intégrée
   - Scheduling de sessions live
   - Recording automatique

10. **Gamification**
    - Points par leçon complétée
    - Badges et achievements
    - Leaderboards

11. **Mobile apps**
    - iOS et Android natifs
    - Offline mode
    - Push notifications

12. **API publique**
    - Webhooks pour intégrations
    - OAuth pour apps tierces
    - Documentation Swagger

---

## ✅ Conclusion

### Ce qui a été accompli

**Backend (Spring Boot) :**
- ✅ Architecture multi-tenant complète
- ✅ Authentification JWT sécurisée
- ✅ Gestion des utilisateurs (CRUD)
- ✅ Gestion des cours (CRUD)
- ✅ Système d'enrollment
- ✅ Tracking de progression
- ✅ 10 migrations de base de données
- ✅ Isolation totale entre organisations

**Frontend (Angular) :**
- ✅ Interface admin (dashboard, users management)
- ✅ Interface professeur (course creation, management)
- ✅ Interface étudiant (catalog, enrollment, learning)
- ✅ Player de contenu (video, text, document)
- ✅ Tracking de progression en temps réel
- ✅ Navigation fluide entre leçons
- ✅ Design moderne avec Tailwind CSS

**Sécurité :**
- ✅ Multi-tenant isolation (impossible de voir les données d'autres organisations)
- ✅ Role-based access control (ADMIN, TEACHER, STUDENT)
- ✅ JWT avec refresh token
- ✅ HttpOnly cookies pour refresh token
- ✅ Guards Angular pour protection des routes

### Métriques du projet

**Lignes de code :**
- Backend : ~5,000 lignes (Java)
- Frontend : ~3,500 lignes (TypeScript/HTML)
- Total : ~8,500 lignes

**Fichiers créés :**
- Backend : ~40 fichiers
- Frontend : ~25 fichiers
- Migrations SQL : 10 fichiers

**Endpoints API :**
- Auth : 4 endpoints
- Admin : 6 endpoints
- Teacher : 7 endpoints
- Student : 7 endpoints
- Total : 24 endpoints REST
