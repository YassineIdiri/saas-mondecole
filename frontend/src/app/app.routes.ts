import { Routes } from '@angular/router';
import { authGuard, adminGuard, teacherGuard, redirectGuard } from './auth/auth.guard';
import { JoinOrganizationComponent } from './feature/auth/join-organization/join-organization.component';
import { LoginComponent } from './feature/auth/login/login.component';
import { HomepageComponent } from './feature/homepage/homepage.component';
import { RegisterOrganizationComponent } from './feature/organization/register-organization/register-organization.component';
import { CourseCatalogComponent } from './feature/student/course-catalog/course-catalog.component';
import { CourseDetailComponent } from './feature/student/course-detail/course-detail.component';
import { StudentDashboardComponent } from './feature/student/dashboard/dashboard.component';
import { LessonViewerComponent } from './feature/student/lesson-viewer/lesson-viewer.component';
import { MyCoursesComponent } from './feature/student/my-courses/my-courses.component';
import { CourseFormComponent } from './feature/teacher/courses-from/course-form.component';
import { CoursesListComponent } from './feature/teacher/courses-list/courses-list.component';
import { AdminDashboardComponent } from './feature/admin/dashboard/dashboard.component';
import { HomeComponent } from './feature/home/home.component';

// ✅ NOUVEAUX IMPORTS
import { CourseEditorComponent } from './feature/teacher/course-editor/course-editor.component';
import { LessonEditorComponent } from './feature/teacher/lesson-editor/lesson-editor.component';

export const routes: Routes = [
  // ════════════════════════════════════════════════════════
  // PUBLIC ROUTES
  // ════════════════════════════════════════════════════════
  { path: 'login', component: LoginComponent },
  { path: 'join', component: JoinOrganizationComponent },
  { path: 'organization/register', component: RegisterOrganizationComponent },

  // ════════════════════════════════════════════════════════
  // PROTECTED ROUTES (All authenticated users)
  // ════════════════════════════════════════════════════════
  {
    path: 'home',
    component: HomepageComponent,
    canActivate: [authGuard]
  },

  // ════════════════════════════════════════════════════════
  // ADMIN ROUTES
  // ════════════════════════════════════════════════════════
  {
    path: 'admin/dashboard',
    component: AdminDashboardComponent,
    canActivate: [adminGuard]
  },

  // ════════════════════════════════════════════════════════
  // TEACHER ROUTES
  // ════════════════════════════════════════════════════════
  {
    path: 'teacher/courses',
    component: CoursesListComponent,
    canActivate: [teacherGuard]
  },
  {
    path: 'teacher/courses/create',
    component: CourseFormComponent,
    canActivate: [teacherGuard]
  },
  {
    path: 'teacher/courses/edit/:id',
    component: CourseFormComponent,
    canActivate: [teacherGuard]
  },
  // ✅ NOUVEAU : Gérer le cours (sections + leçons)
  {
    path: 'teacher/courses/:id',
    component: CourseEditorComponent,
    canActivate: [teacherGuard]
  },
  // ✅ NOUVEAU : Gérer les leçons d'une section
  {
    path: 'teacher/courses/:courseId/sections/:sectionId/lessons',
    component: LessonEditorComponent,
    canActivate: [teacherGuard]
  },

  // ════════════════════════════════════════════════════════
  // STUDENT ROUTES (accessible by all roles)
  // ════════════════════════════════════════════════════════
  {
    path: 'student/dashboard',
    component: StudentDashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'student/courses/catalog',
    component: CourseCatalogComponent,
    canActivate: [authGuard]
  },
  {
    path: 'student/courses/my-courses',
    component: MyCoursesComponent,
    canActivate: [authGuard]
  },
  {
    path: 'student/courses/:id',
    component: CourseDetailComponent,
    canActivate: [authGuard]
  },
  {
    path: 'student/lessons/:id',
    component: LessonViewerComponent,
    canActivate: [authGuard]
  },

  // ════════════════════════════════════════════════════════
  // DEFAULT & WILDCARD
  // ════════════════════════════════════════════════════════
  {
    path: '',
    component: HomeComponent,
    pathMatch: 'full',
    canActivate: [redirectGuard]
  },
  { path: '**', redirectTo: '' }
];
