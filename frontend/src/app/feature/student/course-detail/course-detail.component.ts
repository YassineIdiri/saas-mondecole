import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { StudentCourseDetail } from '../../../models/student-course.model';
import { StudentCourseService } from '../../../service/student-course.service';

@Component({
  selector: 'app-course-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './course-detail.component.html'
})
export class CourseDetailComponent implements OnInit {

  // ✅ Signals
  course = signal<StudentCourseDetail | null>(null);
  loading = signal(true);
  enrolling = signal(false);

  courseId!: number;

  constructor(
    private studentCourseService: StudentCourseService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.courseId = +params['id'];
      this.loadCourse();
    });
  }

  loadCourse(): void {
    this.loading.set(true);

    this.studentCourseService.getCourseDetail(this.courseId).subscribe({
      next: (course) => {
        this.course.set(course);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading course:', error);
        this.loading.set(false);
        this.router.navigate(['/student/courses/catalog']);
      }
    });
  }

  enrollInCourse(): void {
    if (!this.course()) return;

    this.enrolling.set(true);

    this.studentCourseService.enrollInCourse(this.courseId).subscribe({
      next: () => {
        this.loadCourse();
        this.enrolling.set(false);
      },
      error: (error) => {
        console.error('Error enrolling:', error);
        alert('Failed to enroll in course');
        this.enrolling.set(false);
      }
    });
  }

  startLearning(): void {
    const courseData = this.course();
    if (!courseData || !courseData.sections.length) return;

    const firstSection = courseData.sections[0];
    if (firstSection.lessons.length > 0) {
      const firstLesson = firstSection.lessons[0];
      this.router.navigate(['/student/lessons', firstLesson.id]);
    }
  }

  continueFromLastLesson(): void {
    const courseData = this.course();
    if (!courseData) return;

    // Find first incomplete lesson
    for (const section of courseData.sections) {
      for (const lesson of section.lessons) {
        if (!lesson.completed) {
          this.router.navigate(['/student/lessons', lesson.id]);
          return;
        }
      }
    }

    // If all completed, start from first lesson
    this.startLearning();
  }

  getLevelColor(level?: string): string {
    switch (level) {
      case 'BEGINNER': return 'bg-green-100 text-green-800';
      case 'INTERMEDIATE': return 'bg-yellow-100 text-yellow-800';
      case 'ADVANCED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getLessonIcon(type: string): string {
    switch (type) {
      case 'VIDEO': return 'M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z';
      case 'DOCUMENT': return 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z';
      case 'TEXT': return 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
      case 'QUIZ': return 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01';
      case 'ASSIGNMENT': return 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
      default: return 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
    }
  }

  formatDuration(seconds?: number): string {
    if (!seconds) return '';
    const minutes = Math.floor(seconds / 60);
    return `${minutes} min`;
  }
}
