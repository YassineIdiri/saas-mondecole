import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserListResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: 'ADMIN' | 'TEACHER' | 'STUDENT';
  active: boolean;
  locked: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface DashboardStatsResponse {
  totalUsers: number;
  totalTeachers: number;
  totalStudents: number;
  activeUsers: number;
  lockedUsers: number;
  organization: {
    id: number;
    name: string;
    slug: string;
    logoUrl: string | null;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiUrl}/api/users`;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<DashboardStatsResponse> {
    return this.http.get<DashboardStatsResponse>(`${this.apiUrl}/dashboard/stats`);
  }

  listUsers(page: number = 0, size: number = 20, role?: string, active?: boolean): Observable<PageResponse<UserListResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (role) params = params.set('role', role);
    if (active !== undefined) params = params.set('active', active.toString());

    return this.http.get<PageResponse<UserListResponse>>(`${this.apiUrl}`, { params });
  }

  listTeachers(page: number = 0, size: number = 20, active?: boolean): Observable<PageResponse<UserListResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (active !== undefined) params = params.set('active', active.toString());

    return this.http.get<PageResponse<UserListResponse>>(`${this.apiUrl}/teachers`, { params });
  }

  listStudents(page: number = 0, size: number = 20, active?: boolean): Observable<PageResponse<UserListResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (active !== undefined) params = params.set('active', active.toString());

    return this.http.get<PageResponse<UserListResponse>>(`${this.apiUrl}/students`, { params });
  }

  toggleUserStatus(userId: number): Observable<UserListResponse> {
    return this.http.patch<UserListResponse>(`${this.apiUrl}/${userId}/toggle-status`, {});
  }

  toggleUserLock(userId: number): Observable<UserListResponse> {
    return this.http.patch<UserListResponse>(`${this.apiUrl}/${userId}/toggle-lock`, {});
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}`);
  }
}
