import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ReorderRequest } from './teacher-course-section.service';

export interface Lesson {
  id: number;
  title: string;
  type: string;
  orderIndex: number;
  durationSeconds?: number;
}

export interface LessonDetail {
  id: number;
  sectionId: number;
  title: string;
  description?: string;
  content: string;  // ✅ Le contenu texte de la leçon
  type: string;
  orderIndex: number;
  externalVideoUrl?: string;
  durationSeconds?: number;
  downloadable: boolean;
}

export interface CreateLessonRequest {
  title: string;
  description?: string;
  content: string;  // ✅ Le texte écrit par le prof
  type: string;     // TEXT, VIDEO, QUIZ
  orderIndex: number;
  externalVideoUrl?: string;
  durationSeconds?: number;
  downloadable?: boolean;
}

export interface UpdateLessonRequest {
  title?: string;
  description?: string;
  content?: string;
  type?: string;
  orderIndex?: number;
  externalVideoUrl?: string;
  durationSeconds?: number;
  downloadable?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TeacherLessonService {
  private apiUrl = `${environment.apiUrl}/api/teacher/sections`;

  constructor(private http: HttpClient) {}

  /**
   * Get all lessons in a section
   */
  getLessons(sectionId: number): Observable<Lesson[]> {
    return this.http.get<Lesson[]>(`${this.apiUrl}/${sectionId}/lessons`);
  }

  /**
   * Get lesson by ID (for editing)
   */
  getLessonById(sectionId: number, lessonId: number): Observable<LessonDetail> {
    return this.http.get<LessonDetail>(
      `${this.apiUrl}/${sectionId}/lessons/${lessonId}`
    );
  }

  /**
   * Create lesson
   */
  createLesson(sectionId: number, request: CreateLessonRequest): Observable<LessonDetail> {
    return this.http.post<LessonDetail>(
      `${this.apiUrl}/${sectionId}/lessons`,
      request
    );
  }

  /**
   * Update lesson
   */
  updateLesson(
    sectionId: number,
    lessonId: number,
    request: UpdateLessonRequest
  ): Observable<LessonDetail> {
    return this.http.put<LessonDetail>(
      `${this.apiUrl}/${sectionId}/lessons/${lessonId}`,
      request
    );
  }

  /**
   * Delete lesson
   */
  deleteLesson(sectionId: number, lessonId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${sectionId}/lessons/${lessonId}`
    );
  }

  /**
   * Reorder lessons
   */
  reorderLessons(sectionId: number, requests: ReorderRequest[]): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${sectionId}/lessons/reorder`,
      requests
    );
  }
}
