import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  StudentCourseList,
  StudentCourseDetail,
  StudentDashboard,
  LessonContent,
  UpdateLessonProgressRequest,
  PageResponse
} from '../models/student-course.model';

@Injectable({
  providedIn: 'root'
})
export class StudentCourseService {
  private apiUrl = `${environment.apiUrl}/api/student`;

  constructor(private http: HttpClient) {}

  /**
   * Browse course catalog
   */
  browseCatalog(page: number = 0, size: number = 12): Observable<PageResponse<StudentCourseList>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<StudentCourseList>>(`${this.apiUrl}/courses/catalog`, { params });
  }

  /**
   * Get my enrolled courses
   */
  getMyEnrolledCourses(page: number = 0, size: number = 10, completed?: boolean): Observable<PageResponse<StudentCourseList>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (completed !== undefined) {
      params = params.set('completed', completed.toString());
    }

    return this.http.get<PageResponse<StudentCourseList>>(`${this.apiUrl}/courses/my-courses`, { params });
  }

  /**
   * Get course detail
   */
  getCourseDetail(courseId: number): Observable<StudentCourseDetail> {
    return this.http.get<StudentCourseDetail>(`${this.apiUrl}/courses/${courseId}`);
  }

  /**
   * Enroll in course
   */
  enrollInCourse(courseId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/courses/${courseId}/enroll`, {});
  }

  /**
   * Get student dashboard
   */
  getDashboard(): Observable<StudentDashboard> {
    return this.http.get<StudentDashboard>(`${this.apiUrl}/courses/dashboard`);
  }

  /**
   * Get lesson content
   */
  getLessonContent(lessonId: number): Observable<LessonContent> {
    return this.http.get<LessonContent>(`${this.apiUrl}/lessons/${lessonId}`);
  }

  /**
   * Update lesson progress
   */
  updateLessonProgress(lessonId: number, request: UpdateLessonProgressRequest): Observable<LessonContent> {
    return this.http.patch<LessonContent>(`${this.apiUrl}/lessons/${lessonId}/progress`, request);
  }

  /**
   * Mark lesson as completed
   */
  markLessonCompleted(lessonId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/lessons/${lessonId}/complete`, {});
  }
}
