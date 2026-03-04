import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  loginForm: FormGroup;

  // ✅ Signals
  errorMessage = signal('');
  loading = signal(false);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      rememberMe: [false]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    const username = this.loginForm.value.username;

    // Step 1: Get user's organization
    this.authService.getUserOrganization(username)
      .pipe(
        // Step 2: Login with organizationId
        switchMap(orgInfo => {
          const credentials = {
            username: this.loginForm.value.username,
            password: this.loginForm.value.password,
            rememberMe: this.loginForm.value.rememberMe
          };
          return this.authService.login(credentials, orgInfo.organizationId);
        })
      )
      .subscribe({
        next: (response) => {
          console.log('✅ Login success:', response);
          this.loading.set(false);

          // Redirection intelligente basée sur le rôle
          const role = this.authService.getUserRole();

          if (role === 'ADMIN') {
            this.router.navigate(['/admin/dashboard']);
          } else if (role === 'TEACHER') {
            this.router.navigate(['/teacher/courses']);
          } else if (role === 'STUDENT') {
            this.router.navigate(['/student/dashboard']);
          } else {
            this.router.navigate(['/home']);
          }
        },
        error: (error: HttpErrorResponse) => {
          console.log('❌ Login error:', error);
          this.loading.set(false);

          if (error.status === 404) {
            this.errorMessage.set('Username not found');
          } else if (error.error?.message) {
            this.errorMessage.set(this.translateErrorMessage(error.error.message));
          } else if (error.error?.code) {
            this.errorMessage.set(this.getErrorMessage(error.error.code));
          } else if (error.status === 401) {
            this.errorMessage.set('Invalid username or password');
          } else if (error.status === 0) {
            this.errorMessage.set('Cannot connect to server');
          } else {
            this.errorMessage.set('An error occurred. Please try again.');
          }
        }
      });
  }

  private translateErrorMessage(message: string): string {
    const translations: Record<string, string> = {
      'Invalid token': 'Invalid credentials',
      'Token invalide': 'Invalid credentials',
      'Account deactivated': 'Your account has been deactivated',
      'Account locked': 'Your account has been locked',
      'User not found': 'Invalid username or password',
      'Unauthenticated': 'Invalid username or password'
    };
    return translations[message] || message;
  }

  private getErrorMessage(code: string): string {
    const messages: Record<string, string> = {
      'INVALID_CREDENTIALS': 'Invalid username or password',
      'ACCOUNT_DISABLED': 'Your account has been deactivated',
      'ACCOUNT_LOCKED': 'Your account has been locked',
      'USER_NOT_FOUND': 'Invalid username or password',
      'UNAUTHORIZED': 'Invalid username or password'
    };
    return messages[code] || 'Authentication failed';
  }

  get username() { return this.loginForm.get('username'); }
  get password() { return this.loginForm.get('password'); }
}
