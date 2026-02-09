import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ui, Ui } from '../../shared/utils/ui.helper';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = '';
  loading: boolean = false;
  
  private ui: Ui;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    cdr: ChangeDetectorRef
  ) {
    this.ui = ui(cdr);
    
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      rememberMe: [false]
    });
  }

  onSubmit(): void {
    this.ui.set(() => {
      this.errorMessage = '';
    });

    if (this.loginForm.invalid) {
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.ui.set(() => {
      this.loading = true;
    });

    this.authService.login(this.loginForm.value)
      .pipe(this.ui.pipeRepaint())
      .subscribe({
        next: (response: any) => {
          console.log('Login success:', response);
          this.authService.saveToken(response.accessToken);
          this.authService.saveUsername(response.username);
          this.ui.set(() => {
            this.loading = false;
          });
          this.router.navigate(['/home']);
        },
        error: (error: HttpErrorResponse) => {
          console.log('Login error:', error);
          console.log('Error details:', error.error);
          
          this.ui.set(() => {
            this.loading = false;
            
            if (error.error?.message) {
              this.errorMessage = error.error.message;
            } else if (error.error?.code) {
              this.errorMessage = this.getErrorMessage(error.error.code);
            } else if (error.status === 401) {
              this.errorMessage = 'Invalid username or password';
            } else if (error.status === 0) {
              this.errorMessage = 'Cannot connect to server';
            } else {
              this.errorMessage = 'An error occurred. Please try again.';
            }
          });
          
          console.log('After error - loading:', this.loading, 'errorMessage:', this.errorMessage);
        }
      });
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

  get username() {
    return this.loginForm.get('username');
  }

  get password() {
    return this.loginForm.get('password');
  }
}