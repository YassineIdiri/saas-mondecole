import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../service/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }
  return true;
};

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }
  if (!authService.isAdmin()) {
    router.navigate(['/home']);
    return false;
  }
  return true;
};

export const teacherGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }
  if (!authService.isTeacher() && !authService.isAdmin()) {
    router.navigate(['/home']);
    return false;
  }
  return true;
};

export const redirectGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    if (authService.isAdmin()) {
      router.navigate(['/admin/dashboard']);
    } else if (authService.isTeacher()) {
      router.navigate(['/teacher/courses']);
    } else {
      router.navigate(['/student/dashboard']);
    }
    return false;
  }
  return true;
};
