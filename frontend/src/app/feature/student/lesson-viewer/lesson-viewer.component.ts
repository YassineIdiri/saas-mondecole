import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { LessonContent } from '../../../models/student-course.model';
import { StudentCourseService } from '../../../service/student-course.service';
import { SafePipe } from '../../../shared/pipes/safe.pipe';

@Component({
  selector: 'app-lesson-viewer',
  standalone: true,
  imports: [CommonModule, RouterModule, SafePipe],
  templateUrl: './lesson-viewer.component.html'
})
export class LessonViewerComponent implements OnInit, OnDestroy {

  // ✅ Signals
  lesson = signal<LessonContent | null>(null);
  loading = signal(true);

  // ✅ Computed signals pour les propriétés optionnelles
  videoUrl = computed(() => this.lesson()?.externalVideoUrl ?? '');
  hasVideo = computed(() => !!this.lesson()?.externalVideoUrl);

  fileUrl = computed(() => this.lesson()?.fileUrl ?? '');
  hasFile = computed(() => !!this.lesson()?.fileUrl);

  lessonId!: number;

  // Progress tracking
  private progressSubscription?: Subscription;
  private readonly autoSaveInterval = 10000;

  // ✅ Exposer au template
  readonly router: Router;
  readonly Math = Math;

  constructor(
    private studentCourseService: StudentCourseService,
    router: Router,
    private route: ActivatedRoute
  ) {
    this.router = router;
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.lessonId = +params['id'];
      this.loadLesson();
    });

    this.startProgressTracking();
  }

  ngOnDestroy(): void {
    this.stopProgressTracking();
  }

  loadLesson(): void {
    this.loading.set(true);

    this.studentCourseService.getLessonContent(this.lessonId).subscribe({
      next: (lesson) => {
        this.lesson.set(lesson);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading lesson:', error);
        this.loading.set(false);
        alert('Failed to load lesson. You may not be enrolled in this course.');
        this.router.navigate(['/student/courses/my-courses']);
      }
    });
  }

  startProgressTracking(): void {
    this.progressSubscription = interval(this.autoSaveInterval).subscribe(() => {
      const lessonData = this.lesson();
      if (lessonData && !lessonData.progress.completed) {
        if (lessonData.type === 'TEXT' || lessonData.type === 'DOCUMENT') {
          const currentProgress = lessonData.progress.progressPercent;
          if (currentProgress < 90) {
            this.updateProgress(Math.min(currentProgress + 10, 90));
          }
        }
      }
    });
  }

  stopProgressTracking(): void {
    if (this.progressSubscription) {
      this.progressSubscription.unsubscribe();
    }
  }

  updateProgress(progressPercent: number, lastPositionSeconds?: number): void {
    if (!this.lesson()) return;

    this.studentCourseService.updateLessonProgress(this.lessonId, {
      progressPercent,
      lastPositionSeconds,
      completed: progressPercent >= 100
    }).subscribe({
      next: (updatedLesson) => {
        this.lesson.set(updatedLesson);
      },
      error: (error) => {
        console.error('Error updating progress:', error);
      }
    });
  }

  markAsCompleted(): void {
    if (!this.lesson()) return;

    this.studentCourseService.markLessonCompleted(this.lessonId).subscribe({
      next: () => {
        this.loadLesson();
      },
      error: (error) => {
        console.error('Error marking as completed:', error);
        alert('Failed to mark lesson as completed');
      }
    });
  }

  goToPrevious(): void {
    const lessonData = this.lesson();
    if (lessonData?.navigation.previousLessonId) {
      this.router.navigate(['/student/lessons', lessonData.navigation.previousLessonId]);
    }
  }

  goToNext(): void {
    const lessonData = this.lesson();
    if (lessonData?.navigation.nextLessonId) {
      this.router.navigate(['/student/lessons', lessonData.navigation.nextLessonId]);
    }
  }

  downloadFile(): void {
    const lessonData = this.lesson();
    if (!lessonData?.fileUrl) return;
    window.open(lessonData.fileUrl, '_blank');
  }

  formatFileSize(bytes?: number): string {
    if (!bytes) return '';
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(2)} MB`;
  }

  getLessonTypeColor(type: string): string {
    switch (type) {
      case 'VIDEO': return 'bg-red-100 text-red-800';
      case 'DOCUMENT': return 'bg-blue-100 text-blue-800';
      case 'TEXT': return 'bg-green-100 text-green-800';
      case 'QUIZ': return 'bg-yellow-100 text-yellow-800';
      case 'ASSIGNMENT': return 'bg-purple-100 text-purple-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
}
