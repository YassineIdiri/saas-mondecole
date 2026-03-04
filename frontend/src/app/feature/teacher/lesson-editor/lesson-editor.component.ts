import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Lesson, CreateLessonRequest, TeacherLessonService } from '../../../service/teacher-lesson.service';

@Component({
  selector: 'app-lesson-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './lesson-editor.component.html',
})
export class LessonEditorComponent implements OnInit {
  courseId!: number;
  sectionId!: number;

  // ✅ Signals
  lessons = signal<Lesson[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showLessonForm = signal(false);
  editingLesson = signal<Lesson | undefined>(undefined);

  // ✅ Lesson form - PAS de signal car utilisé avec ngModel
  lessonForm: CreateLessonRequest = {
    title: '',
    content: '',
    type: 'TEXT',
    orderIndex: 0
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private lessonService: TeacherLessonService
  ) {}

  ngOnInit(): void {
    this.courseId = +this.route.snapshot.paramMap.get('courseId')!;
    this.sectionId = +this.route.snapshot.paramMap.get('sectionId')!;
    this.loadLessons();
  }

  loadLessons(): void {
    this.loading.set(true);
    this.lessonService.getLessons(this.sectionId).subscribe({
      next: (lessons) => {
        this.lessons.set(lessons);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des leçons');
        this.loading.set(false);
      }
    });
  }

  onAddLesson(): void {
    this.showLessonForm.set(true);
    this.editingLesson.set(undefined);
    this.lessonForm = {
      title: '',
      content: '',
      type: 'TEXT',
      orderIndex: this.lessons().length
    };
  }

  onEditLesson(lesson: Lesson): void {
    this.loading.set(true);
    this.lessonService.getLessonById(this.sectionId, lesson.id).subscribe({
      next: (detail) => {
        this.showLessonForm.set(true);
        this.editingLesson.set(lesson);
        this.lessonForm = {
          title: detail.title,
          description: detail.description,
          content: detail.content,
          type: detail.type,
          orderIndex: detail.orderIndex,
          externalVideoUrl: detail.externalVideoUrl,
          durationSeconds: detail.durationSeconds,
          downloadable: detail.downloadable
        };
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement de la leçon');
        this.loading.set(false);
      }
    });
  }

  onSaveLesson(): void {
    if (!this.lessonForm.title || !this.lessonForm.content) {
      this.error.set('Le titre et le contenu sont obligatoires');
      return;
    }

    this.loading.set(true);

    const currentEditingLesson = this.editingLesson();
    if (currentEditingLesson) {
      // Update
      this.lessonService.updateLesson(
        this.sectionId,
        currentEditingLesson.id,
        this.lessonForm
      ).subscribe({
        next: () => {
          this.loadLessons();
          this.showLessonForm.set(false);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Erreur lors de la mise à jour');
          this.loading.set(false);
        }
      });
    } else {
      // Create
      this.lessonService.createLesson(this.sectionId, this.lessonForm).subscribe({
        next: () => {
          this.loadLessons();
          this.showLessonForm.set(false);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Erreur lors de la création');
          this.loading.set(false);
        }
      });
    }
  }

  onDeleteLesson(lessonId: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette leçon ?')) {
      return;
    }

    this.lessonService.deleteLesson(this.sectionId, lessonId).subscribe({
      next: () => {
        this.lessons.update(currentLessons =>
          currentLessons.filter(l => l.id !== lessonId)
        );
      },
      error: (err) => {
        this.error.set('Erreur lors de la suppression');
      }
    });
  }

  /**
   * Format duration from seconds to readable format
   */
  formatDuration(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
      return `${hours}h ${minutes}min`;
    } else if (minutes > 0) {
      return `${minutes}min ${secs}s`;
    } else {
      return `${secs}s`;
    }
  }

  onCancel(): void {
    this.showLessonForm.set(false);
    this.editingLesson.set(undefined);
  }

  onBack(): void {
    this.router.navigate(['/teacher/courses', this.courseId]);
  }
}
