import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CourseService } from '../../../service/course.service';
import { CourseSection, TeacherCourseSectionService } from '../../../service/teacher-course-section.service';

@Component({
  selector: 'app-course-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './course-editor.component.html',
})
export class CourseEditorComponent implements OnInit {
  courseId!: number;

  // ✅ Signals
  course = signal<any | undefined>(undefined);
  sections = signal<CourseSection[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showSectionForm = signal(false);
  editingSection = signal<CourseSection | undefined>(undefined);

  // ✅ Section form - PAS de signal car utilisé avec ngModel
  sectionForm = {
    title: '',
    description: '',
    orderIndex: 0
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private sectionService: TeacherCourseSectionService
  ) {}

  ngOnInit(): void {
    this.courseId = +this.route.snapshot.paramMap.get('id')!;
    this.loadCourse();
    this.loadSections();
  }

  loadCourse(): void {
    this.loading.set(true);
    this.courseService.getCourseById(this.courseId).subscribe({
      next: (course) => {
        this.course.set(course);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement du cours');
        this.loading.set(false);
      }
    });
  }

  loadSections(): void {
    this.sectionService.getSections(this.courseId).subscribe({
      next: (sections) => {
        this.sections.set(sections);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des sections');
      }
    });
  }

  onAddSection(): void {
    this.showSectionForm.set(true);
    this.editingSection.set(undefined);
    this.sectionForm = {
      title: 'Nouvelle Section',
      description: '',
      orderIndex: this.sections().length
    };
  }

  onEditSection(section: CourseSection): void {
    this.showSectionForm.set(true);
    this.editingSection.set(section);
    this.sectionForm = {
      title: section.title,
      description: section.description || '',
      orderIndex: section.orderIndex
    };
  }

  onSaveSection(): void {
    if (!this.sectionForm.title) {
      this.error.set('Le titre est obligatoire');
      return;
    }

    this.loading.set(true);

    const currentEditingSection = this.editingSection();
    if (currentEditingSection) {
      // Update
      this.sectionService.updateSection(
        this.courseId,
        currentEditingSection.id,
        this.sectionForm
      ).subscribe({
        next: () => {
          this.loadSections();
          this.showSectionForm.set(false);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Erreur lors de la mise à jour');
          this.loading.set(false);
        }
      });
    } else {
      // Create
      this.sectionService.createSection(this.courseId, this.sectionForm).subscribe({
        next: () => {
          this.loadSections();
          this.showSectionForm.set(false);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Erreur lors de la création');
          this.loading.set(false);
        }
      });
    }
  }

  onDeleteSection(sectionId: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette section et toutes ses leçons ?')) {
      return;
    }

    this.sectionService.deleteSection(this.courseId, sectionId).subscribe({
      next: () => {
        this.sections.update(currentSections =>
          currentSections.filter(s => s.id !== sectionId)
        );
      },
      error: (err) => {
        this.error.set('Erreur lors de la suppression');
      }
    });
  }

  onManageLessons(sectionId: number): void {
    this.router.navigate([
      '/teacher/courses',
      this.courseId,
      'sections',
      sectionId,
      'lessons'
    ]);
  }

  onCancel(): void {
    this.showSectionForm.set(false);
    this.editingSection.set(undefined);
  }

  onBack(): void {
    this.router.navigate(['/teacher/courses']);
  }
}
