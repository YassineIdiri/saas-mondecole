import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { OrganizationService, JoinOrganizationRequest } from '../../../service/organization.service';
import { HttpErrorResponse } from '@angular/common/http';

type UserRole = 'TEACHER' | 'STUDENT';

@Component({
  selector: 'app-join-organization',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './join-organization.component.html'
})
export class JoinOrganizationComponent implements OnInit {
  joinForm: FormGroup;

  // ✅ Signals
  errorMessage = signal('');
  fieldErrors = signal<Record<string, string>>({});
  loading = signal(false);
  verifyingCode = signal(false);
  organizationInfo = signal<any>(null);
  codeVerified = signal(false);
  selectedRole = signal<UserRole>('STUDENT');
  successMessage = signal('');

  constructor(
    private fb: FormBuilder,
    private organizationService: OrganizationService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.joinForm = this.fb.group({
      invitationCode: ['', [Validators.required]],
      role: ['STUDENT', [Validators.required]],
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(100),
        Validators.pattern(/^[a-zA-Z0-9_-]+$/)
      ]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(128),
        this.passwordStrengthValidator
      ]],
      confirmPassword: ['', [Validators.required]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['code']) {
        this.joinForm.patchValue({ invitationCode: params['code'] });
        this.verifyCode();
      }
    });
  }

  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.value;
    if (!password) return null;
    const valid = /[A-Z]/.test(password) && /[a-z]/.test(password)
               && /[0-9]/.test(password) && /[@$!%*?&]/.test(password);
    return valid ? null : { weakPassword: true };
  }

  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  selectRole(role: UserRole): void {
    this.selectedRole.set(role);
    this.joinForm.patchValue({ role });
  }

  verifyCode(): void {
    const code = this.joinForm.get('invitationCode')?.value;

    if (!code || code.trim() === '') {
      this.errorMessage.set('Please enter an invitation code');
      return;
    }

    this.verifyingCode.set(true);
    this.errorMessage.set('');
    this.organizationInfo.set(null);
    this.codeVerified.set(false);

    this.organizationService.verifyInvitationCode(code.trim()).subscribe({
      next: (response) => {
        this.verifyingCode.set(false);
        if (response.valid && response.organization) {
          this.codeVerified.set(true);
          this.organizationInfo.set(response.organization);
        } else {
          this.errorMessage.set(response.message || 'Invalid invitation code');
        }
      },
      error: (error: HttpErrorResponse) => {
        this.verifyingCode.set(false);
        this.errorMessage.set(error.error?.message || 'Error verifying invitation code');
      }
    });
  }

  onSubmit(): void {
    if (this.joinForm.invalid) {
      Object.keys(this.joinForm.controls).forEach(key => {
        this.joinForm.get(key)?.markAsTouched();
      });
      return;
    }

    if (!this.codeVerified()) {
      this.errorMessage.set('Please verify the invitation code first');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.fieldErrors.set({});

    const formValue = this.joinForm.value;
    const request: JoinOrganizationRequest = {
      invitationCode: formValue.invitationCode.trim(),
      username: formValue.username,
      email: formValue.email,
      password: formValue.password,
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      role: formValue.role
    };

    this.organizationService.joinOrganization(request).subscribe({
      next: (response) => {
        this.successMessage.set(response.message);
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.error?.details?.errors) {
          this.fieldErrors.set(error.error.details.errors);
        } else {
          this.errorMessage.set(error.error?.message || 'An error occurred during registration');
        }
      }
    });
  }

  getFieldError(fieldName: string): string | null {
    return this.fieldErrors()[fieldName] || null;
  }

  get invitationCode() { return this.joinForm.get('invitationCode'); }
  get username() { return this.joinForm.get('username'); }
  get email() { return this.joinForm.get('email'); }
  get password() { return this.joinForm.get('password'); }
  get confirmPassword() { return this.joinForm.get('confirmPassword'); }
  get firstName() { return this.joinForm.get('firstName'); }
  get lastName() { return this.joinForm.get('lastName'); }
}
