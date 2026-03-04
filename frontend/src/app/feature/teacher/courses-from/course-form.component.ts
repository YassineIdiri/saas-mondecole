import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { CourseService } from '../../../service/course.service';

@Component({
  selector: 'app-course-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './course-form.component.html'
})
export class CourseFormComponent implements OnInit {
  courseForm: FormGroup;

  // ✅ Signals
  isEditMode = signal(false);
  courseId = signal<number | null>(null);
  loading = signal(false);
  saving = signal(false);
  errorMessage = signal('');

  readonly levels = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
  readonly categories = [
    'Programming',
    'Design',
    'Business',
    'Marketing',
    'Data Science',
    'Languages',
    'Health & Fitness',
    'Music',
    'Photography',
    'Other'
  ];

  constructor(
    private fb: FormBuilder,
    private courseService: CourseService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.courseForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(200)]],
      summary: ['', [Validators.maxLength(500)]],
      description: [''],
      category: [''],
      level: [''],
      estimatedHours: [null, [Validators.min(1), Validators.max(1000)]],
      objectives: [''],
      prerequisites: ['']
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode.set(true);
        this.courseId.set(+params['id']);
        this.loadCourse(+params['id']);
      }
    });
  }

  loadCourse(id: number): void {
    this.loading.set(true);

    this.courseService.getCourseById(id).subscribe({
      next: (course) => {
        this.courseForm.patchValue({
          title: course.title,
          summary: course.summary || '',
          description: course.description || '',
          category: course.category || '',
          level: course.level || '',
          estimatedHours: course.estimatedHours,
          objectives: course.objectives || '',
          prerequisites: course.prerequisites || ''
        });
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading course:', error);
        this.errorMessage.set('Failed to load course');
        this.loading.set(false);
      }
    });
  }

  onSubmit(): void {
    if (this.courseForm.invalid) {
      Object.keys(this.courseForm.controls).forEach(key => {
        this.courseForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.saving.set(true);
    this.errorMessage.set('');

    const formValue = this.courseForm.value;

    // Clean up empty values
    const request = {
      title: formValue.title,
      summary: formValue.summary || undefined,
      description: formValue.description || undefined,
      category: formValue.category || undefined,
      level: formValue.level || undefined,
      estimatedHours: formValue.estimatedHours || undefined,
      objectives: formValue.objectives || undefined,
      prerequisites: formValue.prerequisites || undefined
    };

    const operation = this.isEditMode()
      ? this.courseService.updateCourse(this.courseId()!, request)
      : this.courseService.createCourse(request);

    operation.subscribe({
      next: (course) => {
        this.saving.set(false);
        this.router.navigate(['/teacher/courses']);
      },
      error: (error) => {
        console.error('Error saving course:', error);
        this.saving.set(false);
        this.errorMessage.set(error.error?.message || 'Failed to save course');
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/teacher/courses']);
  }

  get title() { return this.courseForm.get('title'); }
  get summary() { return this.courseForm.get('summary'); }
  get description() { return this.courseForm.get('description'); }
  get estimatedHours() { return this.courseForm.get('estimatedHours'); }
}
