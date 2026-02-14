# 🎓 Detailed Presentation: Multi-tenant LMS Platform "Mondecole"

---

## 📋 Table of Contents

1. [Project overview](#overview)
2. [Technical Architecture](#architecture-technology)
3. [Security & Multi-tenancy](#security--multi-tenancy)
4. [User workflows](#user workflows)
5. [Detailed features](#detailed features)
6. [Key technical points](#key-technical points)
7. [Flow demonstration](#flow demonstration)

---

## 🎯 Overview

### What is it?

**Mondecole** is a complete Learning Management System (LMS) platform that allows **organizations** (schools, companies, training centers) to create their own isolated learning space with:

- **User management** (Admins, Teachers, Students)
- **Course creation** by teachers
- **Progress tracking** for students
- **Complete isolation** between organizations (multi-tenant)

### Why "multi-tenant"?

Imagine a single application that hosts several schools:
- School A → Their teachers, their students, their courses
- School B → Their teachers, their students, their courses
- School C → Their teachers, their students, their courses

**Each organization is completely isolated from the others**, but all share the same technical infrastructure.

### Use Cases

1. **Professional Training Company**: Each client (company) has their own space
2. **School Network**: Each institution has its own isolated instance
3. **Educational SaaS Platform**: Each subscriber has their own private organization

---

## 🏗️ Technical Architecture

### Technology Stack

**Backend:**
- **Spring Boot 4.0.2** (Java)
- **PostgreSQL** (database)
- **JWT** for authentication
- **Multi-tenant** with database isolation

**Frontend:**
- **Angular 18** (zoneless)
- **Tailwind CSS** for design
- **Reactive programming** (RxJS)

### Layered Architecture

```
┌─────────────────────────────────────────────┐
│           FRONTEND (Angular 18)             │
│  ┌──────────┬──────────┬──────────┐         │
│  │  Admin   │ Teacher  │ Student  │         │
│  │   UI     │   UI     │   UI     │         │
│  └──────────┴──────────┴──────────┘         │
│           Services (HTTP calls)             │
└─────────────────┬───────────────────────────┘
                  │ REST API
┌─────────────────▼───────────────────────────┐
│          BACKEND (Spring Boot)              │
│  ┌─────────────────────────────────┐        │
│  │     Security Layer              │        │
│  │  - JWT Authentication           │        │
│  │  - Multi-tenant Filter          │        │
│  │  - Role-based Authorization     │        │
│  └─────────────────────────────────┘        │
│  ┌─────────────────────────────────┐        │
│  │     Business Logic              │        │
│  │  - CourseService                │        │
│  │  - EnrollmentService            │        │
│  │  - UserService                  │        │
│  └─────────────────────────────────┘        │
│  ┌─────────────────────────────────┐        │
│  │     Data Access Layer           │        │
│  │  - Repositories (JPA)           │        │
│  │  - Entities                     │        │
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

## 🔒 Security & Multi-tenant

### How does multi-tenant isolation work?

#### 1. **At the database level**

All tables have an `organization_id` column:

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

**All SQL queries automatically include the organization filter.**

#### 2. **At the backend level (TenantFilter)**

A Spring filter intercepts **each HTTP request** to:

1. **Extract the organization_id** from:

- The JWT (for logged-in users)

- The body (for login)

2. **Store the ID in TenantContext** (ThreadLocal)

3. **All DB queries use this ID**
4. 
```java
// Exemple simplifié
@Component
public class TenantFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, ...) {
        // 1. Extract organization_id from the JWT
        Long orgId = jwtService.extractOrganizationId(token);
        
        // 2. Store in context
        TenantContext.setTenantId(orgId);
        
        // 3. Continue the request
        filterChain.doFilter(request, response);
        
        // 4. Clean after
        TenantContext.clear();
    }
}
```

**Result:** A teacher at School A can **never** see the courses at School B, even if they try to force the URL.

#### 3. **At the authentication level (JWT)**

The JWT contains:

```json
{
  "sub": "jean.dupont",           // username
  "userId": 42,
  "organizationId": 1,            // ✅ Organization ID
  "role": "TEACHER",
  "iat": 1234567890,
  "exp": 1234571490
}
```

**The token links the user to their organization:** It is impossible to access data from another organization.

### Role Security

3 main roles:

1. **ADMIN**: Full management of the organization
2. **TEACHER**: Creation and management of courses
3. **STUDENT**: Course registration and tracking

**Backend protection:**

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {
   // Only admins can delete users
}

@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public void createCourse(CreateCourseRequest request) {
    // Teachers and admins can create courses
}
```

**Frontend protection:**

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

## User Workflows

### 1️⃣ Workflow ORGANISATION

**Creation of a new organization:**

```
┌─────────────────────────────────────────────────┐
│ 1. Landing Page                                 │
│    └─> "Register Organization"                  │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 2. Registration form                            │
│    - Organization name: "École Strasbourg"      │
│    - Slug: "ecole-strasbourg"                   │
│    - Admin username: "admin"                    │
│    - Admin email: "admin@ecole.fr"              │
│    - Admin password: ********                   │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 3. Backend createde :                           │
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

**The organization is now created and isolated.**

---

### 2️⃣ Workflow ADMIN

**Mission:** Manage users (professors and students)

#### A. Admin Login

```
┌─────────────────────────────────────────────────┐
│ 1. Login Page                                   │
│    - Username: "admin"                          │
│    - Password: ********                         │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 2. Backend checks:                              │
│    - Lookup organization_id for "admin"         │
│    - Password verification                      │
│    - JWT generation with organizationId: 1      │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│ 3. Frontend stores JWT                          │
│    - Redirect to /admin/dashboard               │
└─────────────────────────────────────────────────┘
```

#### B. Dashboard Admin

The admin sees:

```
╔═══════════════════════════════════════════════╗
║          ADMIN DASHBOARD                      ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  📊 STATS                                     ║
║  ┌──────────┬─────────---─┬────────--──┐      ║
║  │ 15 Users │ 5 Teachers  │ 10 Students│      ║
║  └──────────┴────────---──┴────────--──┘      ║
║                                               ║
║  👥 TEACHERS                                  ║
║  ┌─────────────────────────────────────────┐  ║
║  │ Jean Dupont    | Active  | [🔒] [❌]    │  ║
║  │ Marie Martin   | Active  | [🔒] [❌]    │  ║
║  └─────────────────────────────────────────┘  ║
║                                               ║
║  🎓 STUDENTS                                  ║
║  ┌─────────────────────────────────────────┐  ║
║  │ Pierre Durand  | Active  | [🔒] [❌]    │  ║
║  │ Sophie Bernard | Locked  | [🔓] [❌]    │  ║
║  └─────────────────────────────────────────┘  ║
╚═══════════════════════════════════════════════╝
```

**Available Actions:**
- ✅ **Toggle Active/Inactive**: Deactivate a user
- ✅ **Toggle Lock/Unlock**: Lock an account
- ✅ **Delete User**: Permanently delete a user
- ✅ **Pagination**: Navigate between pages
- 
#### C. User Invitation

**Invitation flow:**

```
Admin generates an invitation link
         │
         ▼
Send the link to jean.dupont@ecole.fr
         │
         ▼
Jean clicks on the link
         │
         ▼
"Join Organization" page
- Pre-filled email address
- Username selection
- Password selection
- Role selection (TEACHER/STUDENT)
         │
         ▼
The backend creates the user with organization_id = 1
         │
         ▼
Jean can log in
```

---

### 3️⃣ Workflow TEACHER

**Mission:** Create and manage courses

#### A. Teacher Login

Same as admin login, but with `role: TEACHER` in the JWT.

**Automatic redirection to `/teacher/courses`**

#### B. Course List

```
╔═══════════════════════════════════════════════╗
║          MY COURSES                           ║
╠═══════════════════════════════════════════════╣
║  [All] [Published] [Draft]                    ║
║                                  [+ Create]   ║
║                                               ║
║  ┌─────────────────────────────────────────┐  ║
║  │ 📘 Introduction to JavaScript           │  ║
║  │ Beginner | Programming                  │  ║
║  │ [Published] 25 students                 │  ║
║  │ Created Jan 15, 2026                    │  ║
║  │                                         │  ║
║  │ [Edit] [Unpublish] [Delete]             │  ║
║  └─────────────────────────────────────────┘  ║
║                                               ║
║  ┌─────────────────────────────────────────┐  ║
║  │ 📗 Advanced React Patterns              │  ║
║  │ Advanced | Programming                  │  ║
║  │ [Draft] 0 students                      │  ║
║  │ Created Feb 10, 2026                    │  ║
║  │                                         │  ║
║  │ [Edit] [Publish] [Delete]               │  ║
║  └─────────────────────────────────────────┘  ║
╚═══════════════════════════════════════════════╝
```

**Available Filters:**
- **All:** All courses
- **Published:** Courses visible to students
- **Draft:** Courses currently being created

#### C. Creating a Course

**Form in 2 sections:**

**Section 1: Basic Information**

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
│ Category          Level                 │
│ ┌──────────────┐ ┌──────────────────┐   │
│ │ Programming  │ │ BEGINNER         │   │
│ └──────────────┘ └──────────────────┘   │
│                                         │
│ Estimated Hours                         │
│ ┌─────────────────────────────────────┐ │
│ │ 20                                  │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Section 2: Detailed Information**
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

**Backend created:**
- ✅ Course with `organization_id = 1`
- ✅ `author_id = professor_id`
- ✅ Auto-generated `slug` (e.g., "introduction-to-python")
- ✅ `published = false` (default draft)

#### D. Publishing a Course

**Before publication:** The course is invisible to students

**Click on "Publish":**
```
Course.published = true
Course.publishedAt = NOW()
```

**After publication:** The course appears in the student catalog

---

### 4️⃣ Student Workflow

**Mission:** Discover, enroll in, and take courses

#### A. Student Dashboard

```
╔═══════════════════════════════════════════════╗
║       MY LEARNING DASHBOARD                   ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  📊 STATS                                     ║
║  ┌──────────┬──────────┬──────────┬────────┐  ║
║  │ 5 Cours  │ 3 En     │ 2 Termi- │ 65%    │  ║
║  │ Inscrits │ Cours    │ nés      │ Moy.   │  ║
║  └──────────┴──────────┴──────────┴────────┘  ║
║                                               ║
║  🔥 CONTINUE LEARNING                         ║
║  ┌─────────────────────────────────────────┐  ║
║  │ Introduction to Python                  │  ║
║  │ Prof. Jean Dupont                       │  ║
║  │ ████████░░░░░░░░░░ 45%                  │  ║
║  │ Last accessed: 2 days ago               │  ║
║  └─────────────────────────────────────────┘  ║
║                                               ║
║  📚 QUICK ACCESS                              ║
║  [Browse Catalog] [My Courses] [Certificates] ║
╚═══════════════════════════════════════════════╝
```

#### B. Course Catalog

```
╔═══════════════════════════════════════════════╗
║          COURSE CATALOG                       ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  ┌────────┐  ┌────────┐  ┌────────┐           ║
║  │ 📘     │  │ 📗     │  │ 📙     │           ║
║  │        │  │        │  │        │           ║
║  │ Python │  │ React  │  │ DevOps │           ║
║  │        │  │        │  │        │           ║
║  │ BEGIN. │  │ ADV.   │  │ INTER. │           ║
║  │ 20h    │  │ 15h    │  │ 30h    │           ║
║  │ 42 🎓  │  │ 18 🎓  │  │ 25 🎓  │           ║
║  │        │  │        │  │        │           ║
║  │[ENROLL]│  │███ 30% │  │[ENROLL]│           ║
║  └────────┘  └────────┘  └────────┘           ║
║                                               ║
║         [Previous] Page 1 of 5 [Next]         ║
╚═══════════════════════════════════════════════╝
```

**Legend:**
- 📘 Course available
- ███ 30% = Course already enrolled with progress
- 🎓 Number of enrolled students

#### C. Course Details

**Detail Page:**

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
║  ┌──────────────────────────────────────────┐ ║
║  │  📸 [Course Thumbnail]                   │ ║
║  │                                          │ ║
║  │          [ENROLL NOW]                    │ ║
║  │                                          │ ║
║  │  Total Lessons: 25                       │ ║
║  │  Duration: 20 hours                      │ ║
║  │  Language: FR                            │ ║
║  └──────────────────────────────────────────┘ ║
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
║     ▢ What is Python?        [🎥 5min]        ║
║     ▢ Setup Environment      [📄 10min]       ║
║  2. Variables & Data Types                    ║
║     ▢ Numbers                [🎥 15min]       ║
║     ▢ Strings                [🎥 20min]       ║
║     ▢ Quiz                   [📝 10min]       ║
╚═══════════════════════════════════════════════╝
```

**Click on "Enroll Now":**

Backend created:
```java
CourseEnrollment enrollment = {
organizationId: 1,
studentId: 42,
courseId: 5,
progressPercent: 0,
completed: false
}
```

**Page refreshed:**
- ❌ "Enroll Now" disappears
- ✅ "Start Learning" appears
- ✅ Progress bar at 0%
- ✅ Lessons become clickable

#### D. Lesson Viewer

**Player Interface:**

```
╔═══════════════════════════════════════════════╗
║ [✕] Introduction | Lesson 1 of 25    [65% ✓]  ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  ┌─────────────────────────────────────────┐  ║
║  │                                         │  ║
║  │         [VIDEO PLAYER]                  │  ║
║  │      ▶️ What is Python?                 │  ║
║  │                                         │  ║
║  │    ━━━━━━━━━━━━━━━━░░░░  2:30 / 5:00    │  ║
║  │                                         │  ║
║  └─────────────────────────────────────────┘  ║
║                                               ║
║  📖 ABOUT THIS LESSON                         ║
║  In this lesson, you'll discover what Python  ║
║  is and why it's one of the most popular...   ║
║                                               ║
║  ┌──────────────────────────┐                 ║
║  │   [✓ Mark as Complete]   │                 ║
║  └──────────────────────────┘                 ║
║                                               ║
║  [← Previous Lesson]    [Next Lesson →]       ║
║                                               ║
╠═══════════════════════════════════════════════╣
║  YOUR PROGRESS                                ║
║  ┌──────────────────┐                         ║
║  │       65%        │ Status: In Progress     ║
║  │    ◐◐◐◐◑◑◑◑◑◑    │ Lesson: 1/25            ║
║  └──────────────────┘ Duration: 5 min         ║
╚═══════════════════════════════════════════════╝
```

**Supported Lesson Types:**

1. **VIDEO** 🎥
- Integrated video player (YouTube, Vimeo)
- Automatic position saving
- Tracked time

2. **TEXT** 📝
- Formatted text content
- Auto-progress every 10 seconds
- Scroll tracking

3. **DOCUMENT** 📄
- Downloadable PDF and DOCX files
- Download button
- Manual completion marking

4. **QUIZ** 📝
- (Currently a placeholder)
- Multiple-choice questions
- Automatic scoring

5. **ASSIGNMENT** 📋
- (Currently a placeholder)
- Assignment upload
- Teacher grading

**Progress Tracking:**

```java
// With each interaction
LessonProgress progress = {
    studentId: 42,
    lessonId: 1,
    progressPercent: 45,        // Auto-incrementing
    lastPositionSeconds: 150,   // For the videos
    completed: false,
    viewCount: 3
}

// Saves every 10 seconds

**Course progress calculation:**

**Progress = (Completed lessons / Total lessons) × 100

Example:
- Total lessons: 25
- Completed: 16
- Progress: 64%

**Automatic course completion:**

```java
if (progressPercent == 100 && !enrollment.completed) {
    enrollment.completed = true;
    enrollment.completedAt = NOW();
    
  // 🎉 Certificate generation (coming soon)
}
```

---

## 🎯 Detailed Features

### 1. Authentication System

#### A. Multi-step Login
```
┌─────────────────────────────────────────┐
│ 1. User between username "myteacher"    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 2. Frontend call:                    │
│    GET /api/auth/user-organization      │
│    ?username=monprof                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 3. Backend responds:                    │
│    {                                    │
│      organizationId: 1,                 │
│      organizationName: "École Stras",   │
│      organizationSlug: "ecole-stras",   │
│      userRole: "TEACHER"                │
│    }                                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 4. Frontend display:                    │
│    "Logging in to École Strasbourg"     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 5. User enter password                  │
│    Frontend send:                       │
│    POST /api/auth/login                 │
│    Headers: X-Organization-Id: 1        │
│    Body: {username, password}           │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 6. Backend:                             │
│    - Verify password                    │
│    - Generates JWT with orgId:          │
│    - Set refresh_token cookie (30j)     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 7. Frontend:                            │
│    - Store access_token (1h)            │
│    - Redirects according to role        │
└─────────────────────────────────────────┘
```

**Why this approach?**

- ✅ Prevents user enumeration
- ✅ Displays the organization name (UX)
- ✅ Validates user existence before the password
- ✅ Transparent multi-tenant for the user

#### B. Automatic token refresh

**Problem:** Access token expires after 1 hour

**Solution:** Refresh token (HttpOnly cookie, 30 days)

```
┌─────────────────────────────────────────┐
│ User navigates the platform             │
│ Access token expires afte 1h            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ Frontend Interceptor detects:           │
│ - Token expired (before sending)        │
│ - Or receives 401                       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────-┐
│ The interceptor automatically calls:     │
│ POST /api/auth/refresh                   │
│ (Cookie refresh_token sent automatically)│
└──────────────┬─────────────────────────-─┘
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

**Security:**
- ✅ Refresh token in an HttpOnly cookie (not accessible via JavaScript)
- ✅ Refresh token rotates with each use
- ✅ Short access token (1 hour) limits exposure
- ✅ Long refresh token (30 days) prevents frequent logins
- 
#### C. Guards Angular

**Road protection:**

```typescript
// Protected route
{
  path: 'admin/dashboard',
  component: AdminDashboardComponent,
  canActivate: [adminGuard]  // ✅ Only admins

}

// Guard implementation
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  
  // No token → /login
  if (!authService.getToken()) {
    router.navigate(['/login']);
    return false;
  }
  
  // Token expired → Refresh then retry
  if (!authService.isLoggedIn()) {
    return authService.refreshToken().pipe(
      map(() => {
        // After refreshing, check admin
        if (!authService.isAdmin()) {
          router.navigate(['/home']);
          return false;
        }
        return true;
      })
    );
  }
  
// Valid token but not admin → /home
  if (!authService.isAdmin()) {
    router.navigate(['/home']);
    return false;
  }
  
  return true;
};
```

---

### 2. Course Management (Teacher)

#### A. Complete CRUD

**Create :**
```
Body: {
  title: "Python for Beginners",
  summary: "Learn Python...",
  description: "This comprehensive course...",
  category: "Programming",
  level: "BEGINNER",
  estimated Hours: 20,
  objectives: "- Write programs...",
  prerequisites: "None"
}
Backend:
- Generates slug: "python-for-beginners"
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
  title: "Python for Beginners (Update)"
}

Backend:
- WHERE id = 5 
- AND organization_id = 1
- AND author_id = 42  // ✅ Ownership verification
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

**Impact:**
- ✅ The course appears in the student catalog
- ✅ Students can enroll
- ✅ The "Total students" statistics start being counted

**Published → Unpublished:**

```java
@Transactional
public CourseDetailResponse unpublishCourse(Long courseId, Long authorId) {
    Course course = findCourseByAuthor(courseId, authorId);
    
    course.published = false;
    // publishedAt reste (historique)
    
    return save(course);
}
```

**Impact:**
- ❌ The course is removed from the catalog
- ✅ Students already enrolled can continue
- ℹ️ New enrollments are blocked

---

### 3. Registration & Progress (Student)

#### A. Enrollment (Registration)

**Workflow complet :**

```
┌─────────────────────────────────────────┐
│ 1. Student browse catalog               │
│    GET /api/student/courses/catalog     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 2. Student clicks on a course           │
│    GET /api/student/courses/5           │
│                                         │
│    Backend returns::                    │
│    - Course details                     │
│    - Sections & Lessons                 │
│    - enrollment: null (not registered)  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 3. Student click "Enroll Now"           │
│    POST /api/student/courses/5/enroll   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│ 4. Backend created:                     │
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
// In the video player
videoPlayer.on('timeupdate', () => {
  const progress = (currentTime / duration) * 100;
  
// Save every 10 seconds
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
// Progressive increment
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
#### C. Navigation between lessons

**Backend automatically calculates:**

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

**Frontend uses:**

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

## 🔧 Key Technical Points

### 1. Multi-tenant Isolation

**Level 1: Database**

Each table has an `organization_id`:

```sql
-- ALL queries include organization_id
SELECT * FROM courses 
WHERE organization_id = 1 
AND published = true;

-- The Foreign Keys guarantee the integrity
CONSTRAINT fk_course_organization 
    FOREIGN KEY (organization_id) 
    REFERENCES organizations (id)
```

**Level 2: Application (TenantFilter)**

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

**Level 3: Service Layer**

```java
@Service
public class CourseService {
    
    public Page<Course> getMyCourses(...) {
        // ✅ Always retrieve orgId from TenantContext
        Long organizationId = TenantContext.getTenantId();
        
        // ✅ ALL requests include organization_id
        return courseRepository.findByOrganizationIdAndAuthorId(
            organizationId, authorId, pageable
        );
    }
}
```

**Result:**
- ✅ Unable to access another organization's data
- ✅ Even if the ID is forced in the URL
- ✅ Even if the JWT is modified (invalid signature)

---

### 2. Frontend Architecture (Zoneless Angular)

**Why Zoneless?**

Classic Angular uses Zone.js to detect changes:

```typescript
// With Zone.js (classic)
button.addEventListener('click', () => {
  this.counter++;  // Zone.js detects and triggers change detection
});
```

**Problem:** Performance overhead, subtle bugs

**Solution: Zoneless (Angular 18)**

```typescript
// Without Zone.js (zoneless)
import { ChangeDetectorRef } from '@angular/core';

constructor(private cdr: ChangeDetectorRef) {}

button.addEventListener('click', () => {
  this.counter++;
  this.cdr.markForCheck();  // ✅ Explicit
});
```

**Our `ui(cdr)` helper:**

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

**Advantages:**
- ✅ Increased performance (no Zone.js)
- ✅ Explicit and controlled change detection
- ✅ No Zone.js-related bugs

---

### 3. Server-Side Rendering (SSR) Compatibility

**Problem:** Angular can perform server-side rendering (SSR) where `localStorage` does not exist.

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

**Interceptor Roles:**

1. **Automatically add the JWT**
2. **Automatically add the X-Organization-Id**
3. **Manage automatic refresh**
4. **Retry on 401**
   
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

**Why slugify?**

Course titles often contain special characters:

- "Introduction to Python" → Unreadable URL
- Spaces, accents, special characters

**Solution:**

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

// "Introduction to Python" → "introduction-a-python"
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

## 🎬 Flows Demonstration

### Complete Scenario: From Organization Creation to Certification

#### Day 1: Setup

**9:00 AM - Admin Creates Organization**

```
1. Visite /organization/register
2. Remplit :
   - Name: "École de Code Strasbourg"
   - Slug: "ecole-code-67"
   - Admin: "admin@ecole67.fr" / "admin" / "password123"
3. Submission
4. Backend created:
   ✅ Organization (id: 1)
   ✅ Admin user (id: 1, organization_id: 1, role: ADMIN)
5. Redirect /login
```

**09:05 - Admin logs in**

```
1. Log in with "admin" / "password123"
2. Backend :
  - Lookup organization_id for "admin" → 1
  - Generates JWT with organizationId: 1, role: ADMIN
3. Redirect /admin/dashboard
```

**09:10 - Admin invites a teacher**

```
1. Admin dashboard → Generate invitation link
2. Copy link: https://app.com/join?token=abc123
3. Send email to jean.dupont@ecole67.fr
```

**09:30 - Jean rejoint l'organisation**

```
1. Jean clicks on the link.
2. The /join page displays:
   "Join École de Code Strasbourg"
   Email: jean.dupont@ecole67.fr (pre-filled)
3. Jean chooses:
   Username: "jdupont"
   Password: "******"
   Role: TEACHER
4. Backend crée :
   ✅ User (id: 2, organization_id: 1, role: TEACHER)
5. Redirect /login
```

#### Day 2: Creating a Course

**10:00 - Jean creates a course**

```
1. Login “jdupont” → Redirect /teacher/courses
2. Click “Create Course”
3. Complete the form:
   - Title: "Python for Beginners"
   - Summary: "Learn Python from scratch"
   - Category: "Programming"
   - Level: "BEGINNER"
   - Estimated Hours: 20
   - Description: "This comprehensive course will teach you..."
   - Objectives: "- Write Python programs..."
   - Prerequisites: "None"
4. Submission
5. Backend created:
   ✅ Course (id: 1, organization_id: 1, author_id: 2, published: false)
   ✅ Slug: "python-for-beginners"
6. Jean sees his course as a "Draft"
```

**10:30 - Jean publishes the course**

```
1. Jean clicks "Publish"
2. Backend:
   Course.published = true
   Course.publishedAt = NOW()
3. ✅ Course now visible in the student catalog
```

#### Day 3: Student registers

**2:00 PM - Admin invites a student**

```
1. Admin generates link → marie.martin@gmail.com
2. Marie joins:
   Username: "mmartin"
   Password: "******"
   Role: STUDENT
3. Backend creates:

✅ User (id: 3, organization_id: 1, role: STUDENT)
```

**14:15 - Marie discovers the catalog**

```
1. Log in "mmartin" → Redirect /student/dashboard
2. Empty dashboard (no courses yet)
3. Click "Browse Catalog"
4. View:
   ┌────────────────────────────┐
   │ 📘 Python for beginners    │
   │ BEGINNER | Programming     │
   │ 20h | 0 students           │
   │ [ENROLL NOW]               │
   └────────────────────────────┘
```

**2:20 PM - Marie signs up for the course**

```
1. Click on the course → Details page
2. View curriculum:
  1. Introduction (0-3 lessons)
  2. Variables (0-5 lessons)
  ...
3. Click "Enroll Now"
4. Backend created:
  ✅ CourseEnrollment (id: 1, student_id: 3, course_id: 1, progress: 0%)
5. Page refreshed:
  ❌ "Enroll Now" disappears
  ✅ "Start Learning" appears
  ✅ Progress bar 0%
```

**2:25 PM - Marie begins the lesson**

```
1. Click "Start Learning"
2. Redirect /student/lessons/1 (first lesson)
3. View:
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
1. Play video
2. During playback:
  - Every 10 seconds:
    Backend receives:
    progressPercent: 20 (then 40, 60, 80)
    lastPositionSeconds: 60 (then 120, 180, 240)
3. At 95% of the video:
    Backend sets:
    LessonProgress.completed = true
    LessonProgress.completedAt = NOW()
4. UI displays:
    ✅ "Lesson Completed!"
    Progress: 100%
```

**14:35 - Marie navigates to the next lesson**

```
1. Click "Next Lesson"
2. Backend updates:
   CourseEnrollment.progressPercent = 4% (1/25 lessons)
3. Marie on Lesson 2: "Setup Environment"
```

#### Next Week: Progress

**Marie continues the course over several days:**

```
Day 4: 5 lessons completed → 20% progress
Day 5: 3 lessons completed → 32% progress
Day 8: 7 lessons completed → 60% progress
Day 10: 5 lessons completed → 80% progress
Day 12: 5 lessons completed → 100% progress
```

**Day 12 - Course Completion**

```
1. Marie completes the last lesson.
2. Backend detects:
    completedLessons = 25
    totalLessons = 25
    progressPercent = 100
3. Backend updates:
   CourseEnrollment.completed = true
   CourseEnrollment.completedAt = NOW()
   🎉 Génération du certificat (à venir)
4. Dashboard Marie :
   📊 Updated stats:
   - Enrolled: 1
   - Completed: 1 ✅
   - In Progress: 0
   - Avg Progress: 100%
```

---
## 💡 Ideas for Future Improvements

### Short Term (1-2 weeks)

1. **PDF Certificates**
- 100% Automatic Generation
- Customizable Template for Organizations
- QR Verification Code

2. **Interactive Quiz**
- Multiple Choice Questions
- Automatic Scoring
- Minimum 70% Passing Score

3. **Assignments**
- Student File Uploads
- Instructor Grading
- Grades and Comments

4. **Advanced Search & Filters**
- Full-Text Search in the Catalog
- Filters: Category, Level, Duration
- Sorting: Popularity, Date, Grade

### Medium Term (1-2 months)

5. **Course Discussions**
- Course Q&A Forum
- Instructor Answers
- Upvoting of Top Answers

6. **Notifications**
- Email: New Course Available
- Push Notifications: Reminders for Incomplete Courses
- Weekly Digest Progress

7. **Advanced Analytics**
- Teacher Dashboard: Completion Rate, Average Time
- Admin Dashboard: Engagement, Retention
- Progress Heatmaps

8. **Video Hosting**
- Direct Video Upload
- Automatic Conversion
- Optimized Streaming

### Long Term (3-6 months)

9. **Live Classes**
- Integrated Video Conferencing
- Live Session Scheduling
- Automatic Recording

10. **Gamification**
- Points per Completed Lesson
- Badges and Achievements
- Leaderboards

11. **Mobile Apps**
- Native iOS and Android
- Offline Mode
- Push Notifications

12. **Public API**
- Webhooks for Integrations
- OAuth for Third-Party Apps
- Swagger Documentation

---

### What Has Been Accomplished

**Backend (Spring Boot):**

- ✅ Complete multi-tenant architecture
- ✅ Secure JWT authentication
- ✅ User management (CRUD)
- ✅ Course management (CRUD)
- ✅ Enrollment system
- ✅ Progress tracking
- ✅ 10 database migrations
- ✅ Complete isolation between organizations

**Frontend (Angular):**
- ✅ Admin interface (dashboard, user management)
- ✅ Instructor interface (course creation, management)
- ✅ Student interface (catalog, enrollment, learning)
- ✅ Content player (video, text, document)
- ✅ Real-time progress tracking
- ✅ Seamless navigation between lessons
- ✅ Modern design with Tailwind CSS

**Security:**
- ✅ Multi-tenant isolation (unable to view data from other organizations)
- ✅ Role-based access control (ADMIN, TEACHER, STUDENT)
- ✅ JWT with refresh token
- ✅ HttpOnly cookies for refresh token
- ✅ Angular Guards for route protection

### Project Metrics

**Lines of Code:**

- Backend: ~5,000 lines (Java)
- Frontend: ~3,500 lines (TypeScript/HTML)
- Total: ~8,500 lines

**Files Created:**
- Backend: ~40 files
- Frontend: ~25 files
- SQL Migrations: 10 files

**API Endpoints:**
- Auth: 4 endpoints
- Admin: 6 endpoints
- Teacher: 7 endpoints
- Student: 7 endpoints
- Total: 24 REST endpoints
