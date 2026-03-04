import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  username: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface UserOrganizationInfo {
  organizationId: number;
  organizationName: string;
  organizationSlug: string;
  userRole: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthApiService {
  private apiUrl = `${environment.apiUrl}/api/auth`;

  constructor(private http: HttpClient) {}

  getUserOrganization(username: string): Observable<UserOrganizationInfo> {
    return this.http.get<UserOrganizationInfo>(`${this.apiUrl}/user-organization`, {
      params: { username }
    });
  }

  login(credentials: LoginRequest, organizationId: number): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials, {
      headers: {
        'X-Organization-Id': organizationId.toString()
      },
      withCredentials: true
    });
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, {
      withCredentials: true
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}, {
      withCredentials: true
    });
  }
}
