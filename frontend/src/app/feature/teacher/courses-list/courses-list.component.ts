import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { CourseService } from '../../../service/course.service';
import { CourseListResponse } from '../../../models/course.model';

@Component({
  selector: 'app-courses-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './courses-list.component.html'
})
export class CoursesListComponent implements OnInit {
  courses = signal<CourseListResponse[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalElements = signal(0);
  totalPages = signal(0);
  filterPublished = signal<boolean | undefined>(undefined);

  readonly pageSize = 10;
  readonly Math = Math;

  constructor(
    private courseService: CourseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCourses();
  }

  loadCourses(page: number = 0): void {
    this.loading.set(true);
    this.courseService.getMyCourses(page, this.pageSize, this.filterPublished()).subscribe({
      next: (response) => {
        this.courses.set(response.content);
        this.currentPage.set(response.number);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading courses:', error);
        this.loading.set(false);
      }
    });
  }

  filterByPublished(published?: boolean): void {
    this.filterPublished.set(published);
    this.loadCourses(0);
  }

  createCourse(): void {
    this.router.navigate(['/teacher/courses/create']);
  }

  editCourse(courseId: number): void {
    this.router.navigate(['/teacher/courses/edit', courseId]);
  }

  // ✅ NOUVELLE MÉTHODE
  manageCourseContent(courseId: number): void {
    this.router.navigate(['/teacher/courses', courseId]);
  }

  deleteCourse(course: CourseListResponse): void {
    if (!confirm(`Are you sure you want to delete "${course.title}"? This action cannot be undone.`)) {
      return;
    }

    this.courseService.deleteCourse(course.id).subscribe({
      next: () => {
        this.loadCourses(this.currentPage());
      },
      error: (error) => {
        console.error('Error deleting course:', error);
        alert('Failed to delete course');
      }
    });
  }

  togglePublish(course: CourseListResponse): void {
    const action = course.published ? 'unpublish' : 'publish';
    const service = course.published
      ? this.courseService.unpublishCourse(course.id)
      : this.courseService.publishCourse(course.id);

    service.subscribe({
      next: () => {
        course.published = !course.published;
        this.courses.update(list => [...list]);
      },
      error: (error) => {
        console.error(`Error ${action}ing course:`, error);
        alert(`Failed to ${action} course`);
      }
    });
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.loadCourses(this.currentPage() + 1);
    }
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.loadCourses(this.currentPage() - 1);
    }
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
