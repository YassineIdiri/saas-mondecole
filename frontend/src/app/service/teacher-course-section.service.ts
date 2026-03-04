import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CourseSection {
  id: number;
  courseId: number;
  title: string;
  description?: string;
  orderIndex: number;
  lessonCount: number;
}

export interface CourseSectionDetail {
  id: number;
  courseId: number;
  title: string;
  description?: string;
  orderIndex: number;
  lessons: LessonSummary[];
}

export interface LessonSummary {
  id: number;
  title: string;
  type: string;
  orderIndex: number;
  durationSeconds?: number;
}

export interface CreateSectionRequest {
  title: string;
  description?: string;
  orderIndex: number;
}

export interface UpdateSectionRequest {
  title?: string;
  description?: string;
  orderIndex?: number;
}

export interface ReorderRequest {
  id: number;
  orderIndex: number;
}

@Injectable({
  providedIn: 'root'
})
export class TeacherCourseSectionService {
  private apiUrl = `${environment.apiUrl}/api/teacher/courses`;

  constructor(private http: HttpClient) {}

  /**
   * Get all sections for a course
   */
  getSections(courseId: number): Observable<CourseSection[]> {
    return this.http.get<CourseSection[]>(`${this.apiUrl}/${courseId}/sections`);
  }

  /**
   * Get section by ID (with lessons)
   */
  getSectionById(courseId: number, sectionId: number): Observable<CourseSectionDetail> {
    return this.http.get<CourseSectionDetail>(
      `${this.apiUrl}/${courseId}/sections/${sectionId}`
    );
  }

  /**
   * Create section
   */
  createSection(courseId: number, request: CreateSectionRequest): Observable<CourseSection> {
    return this.http.post<CourseSection>(
      `${this.apiUrl}/${courseId}/sections`,
      request
    );
  }

  /**
   * Update section
   */
  updateSection(
    courseId: number,
    sectionId: number,
    request: UpdateSectionRequest
  ): Observable<CourseSection> {
    return this.http.put<CourseSection>(
      `${this.apiUrl}/${courseId}/sections/${sectionId}`,
      request
    );
  }

  /**
   * Delete section
   */
  deleteSection(courseId: number, sectionId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${courseId}/sections/${sectionId}`
    );
  }

  /**
   * Reorder sections
   */
  reorderSections(courseId: number, requests: ReorderRequest[]): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${courseId}/sections/reorder`,
      requests
    );
  }
}
