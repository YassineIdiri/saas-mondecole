import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { StudentCourseList } from '../../../models/student-course.model';
import { StudentCourseService } from '../../../service/student-course.service';

@Component({
  selector: 'app-course-catalog',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './course-catalog.component.html'
})
export class CourseCatalogComponent implements OnInit {

  // ✅ Signals
  courses = signal<StudentCourseList[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalElements = signal(0);
  totalPages = signal(0);

  readonly pageSize = 12;  // ✅ Constante
  readonly Math = Math;    // ✅ Reste tel quel

  constructor(private studentCourseService: StudentCourseService) {}

  ngOnInit(): void {
    this.loadCourses();
  }

  loadCourses(page: number = 0): void {
    this.loading.set(true);

    this.studentCourseService.browseCatalog(page, this.pageSize).subscribe({
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

  getLevelColor(level?: string): string {
    switch (level) {
      case 'BEGINNER': return 'bg-green-100 text-green-800';
      case 'INTERMEDIATE': return 'bg-yellow-100 text-yellow-800';
      case 'ADVANCED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getLevelLabel(level?: string): string {
    return level || 'All Levels';
  }
}
