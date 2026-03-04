import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { OrganizationService, OrganizationType } from '../../../service/organization.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-register-organization',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register-organization.component.html'
})
export class RegisterOrganizationComponent {
  organizationForm: FormGroup;

  errorMessage = signal<string>('');
  fieldErrors = signal<Record<string, string>>({});
  loading = signal<boolean>(false);
  currentStep = signal<number>(1);
  successData = signal<any>(null);

  readonly totalSteps = 3;

  organizationTypes: { value: OrganizationType; label: string }[] = [
    { value: 'TRAINING_CENTER', label: 'Training Center' },
    { value: 'UNIVERSITY', label: 'University' },
    { value: 'SCHOOL', label: 'School' },
    { value: 'COMPANY', label: 'Company' },
    { value: 'OTHER', label: 'Other' }
  ];

  constructor(
    private fb: FormBuilder,
    private organizationService: OrganizationService,
    private router: Router
  ) {
    this.organizationForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      type: ['TRAINING_CENTER', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      website: [''],
      description: [''],
      address: [''],
      city: [''],
      postalCode: [''],
      country: [''],
      adminFirstName: ['', [Validators.required]],
      adminLastName: ['', [Validators.required]],
      adminUsername: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(100),
        Validators.pattern(/^[a-zA-Z0-9_-]+$/)
      ]],
      adminEmail: ['', [Validators.required, Validators.email]],
      adminPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(128),
        this.passwordStrengthValidator
      ]],
      adminPasswordConfirm: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }


  nextStep(): void {
    if (this.currentStep() < this.totalSteps) {
      this.currentStep.update(step => step + 1);
    }
  }

  previousStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.update(step => step - 1);
    }
  }


  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.value;
  if (!password) return null;

  const hasUpperCase = /[A-Z]/.test(password);
  const hasLowerCase = /[a-z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecial = /[@$!%*?&#^()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);

  const valid = hasUpperCase && hasLowerCase && hasNumber && hasSpecial;

  return valid ? null : {
    weakPassword: {
      hasUpperCase,
      hasLowerCase,
      hasNumber,
      hasSpecial
    }
  };
}

passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('adminPassword')?.value;
  const confirmPassword = group.get('adminPasswordConfirm')?.value;

  // Ne valider que si les deux sont remplis
  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
}

isStepValid(step: number): boolean {
  const step1Fields = ['name', 'type', 'email'];
  const step3Fields = ['adminFirstName', 'adminLastName', 'adminUsername', 'adminEmail', 'adminPassword', 'adminPasswordConfirm'];

  if (step === 2) return true;

  const fields = step === 1 ? step1Fields : step3Fields;
  const fieldsValid = fields.every(field => this.organizationForm.get(field)?.valid);

  // Vérifier passwordMatch pour step 3
  if (step === 3) {
    return fieldsValid && !this.organizationForm.errors?.['passwordMismatch'];
  }

  return fieldsValid;
}

// ✅ Méthode de debug
debugForm(): void {
  console.log('=== FORM DEBUG ===');
  console.log('Form valid:', this.organizationForm.valid);
  console.log('Form errors:', this.organizationForm.errors);
  console.log('Step 3 valid:', this.isStepValid(3));

  Object.keys(this.organizationForm.controls).forEach(key => {
    const control = this.organizationForm.get(key);
    if (control?.invalid) {
      console.log(`❌ ${key}:`, control.errors, 'Value:', control.value);
    } else {
      console.log(`✅ ${key}:`, control?.value);
    }
  });
}

  onSubmit(): void {
    if (this.organizationForm.invalid) {
      Object.keys(this.organizationForm.controls).forEach(key => {
        this.organizationForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.fieldErrors.set({});

    const formValue = this.organizationForm.value;
    const request = {
      name: formValue.name,
      email: formValue.email,
      phone: formValue.phone || undefined,
      address: formValue.address || undefined,
      city: formValue.city || undefined,
      postalCode: formValue.postalCode || undefined,
      country: formValue.country || undefined,
      website: formValue.website || undefined,
      type: formValue.type,
      description: formValue.description || undefined,
      adminUsername: formValue.adminUsername,
      adminEmail: formValue.adminEmail,
      adminPassword: formValue.adminPassword,
      adminFirstName: formValue.adminFirstName,
      adminLastName: formValue.adminLastName
    };

    this.organizationService.registerOrganization(request).subscribe({
      next: (response) => {
        this.loading.set(false); // ✅ Reset loading
        this.successData.set(response);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false); // ✅ Reset loading même en cas d'erreur

        if (error.error?.details?.errors) {
          this.fieldErrors.set(error.error.details.errors);
        } else {
          this.errorMessage.set(error.error?.message || 'An error occurred during registration');
        }
      }
    });
  }

  getFieldError(fieldName: string): string | null {
    return this.fieldErrors()[fieldName] || null; // ✅ Appel signal avec ()
  }

  get name() { return this.organizationForm.get('name'); }
  get type() { return this.organizationForm.get('type'); }
  get email() { return this.organizationForm.get('email'); }
  get adminUsername() { return this.organizationForm.get('adminUsername'); }
  get adminEmail() { return this.organizationForm.get('adminEmail'); }
  get adminPassword() { return this.organizationForm.get('adminPassword'); }
  get adminPasswordConfirm() { return this.organizationForm.get('adminPasswordConfirm'); }
  get adminFirstName() { return this.organizationForm.get('adminFirstName'); }
  get adminLastName() { return this.organizationForm.get('adminLastName'); }

  copyInvitationCode(code: string): void {
    navigator.clipboard.writeText(code).then(() => {
      alert('Invitation code copied to clipboard!');
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
