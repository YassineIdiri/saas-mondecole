import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { StudentCourseService } from '../../../service/student-course.service';
import { StudentCourseList } from '../../../models/student-course.model';

@Component({
  selector: 'app-my-courses',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-courses.component.html'
})
export class MyCoursesComponent implements OnInit {

  // ✅ Signals
  courses = signal<StudentCourseList[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalElements = signal(0);
  totalPages = signal(0);
  filter = signal<'all' | 'in-progress' | 'completed'>('all');

  readonly pageSize = 10;
  readonly Math = Math;

  constructor(private studentCourseService: StudentCourseService) {}

  ngOnInit(): void {
    this.loadCourses();
  }

  loadCourses(page: number = 0): void {
    this.loading.set(true);

    let completed: boolean | undefined;
    if (this.filter() === 'completed') {
      completed = true;
    } else if (this.filter() === 'in-progress') {
      completed = false;
    }

    this.studentCourseService.getMyEnrolledCourses(page, this.pageSize, completed).subscribe({
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

  setFilter(newFilter: 'all' | 'in-progress' | 'completed'): void {
    this.filter.set(newFilter);
    this.loadCourses(0);
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

  formatDate(date: string | undefined): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
