import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Course, CourseListResponse, CreateCourseRequest, UpdateCourseRequest, PageResponse } from '../models/course.model';

@Injectable({
  providedIn: 'root'
})
export class CourseService {
  private apiUrl = `${environment.apiUrl}/api/courses`;

  constructor(private http: HttpClient) {}

  /**
   * Get all courses for the current teacher
   */
  getMyCourses(page: number = 0, size: number = 10, published?: boolean): Observable<PageResponse<CourseListResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (published !== undefined) {
      params = params.set('published', published.toString());
    }

    return this.http.get<PageResponse<CourseListResponse>>(`${this.apiUrl}/my-courses`, { params });
  }

  /**
   * Get course by ID
   */
  getCourseById(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.apiUrl}/${id}`);
  }

  /**
   * Create new course
   */
  createCourse(request: CreateCourseRequest): Observable<Course> {
    return this.http.post<Course>(this.apiUrl, request);
  }

  /**
   * Update course
   */
  updateCourse(id: number, request: UpdateCourseRequest): Observable<Course> {
    return this.http.put<Course>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete course
   */
  deleteCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Publish course
   */
  publishCourse(id: number): Observable<Course> {
    return this.http.patch<Course>(`${this.apiUrl}/${id}/publish`, {});
  }

  /**
   * Unpublish course
   */
  unpublishCourse(id: number): Observable<Course> {
    return this.http.patch<Course>(`${this.apiUrl}/${id}/unpublish`, {});
  }
}
