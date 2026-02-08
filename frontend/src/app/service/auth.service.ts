import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequest, RegisterRequest, AuthResponse, RegisterResponse } from '../model/models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/auth`;

  constructor(private http: HttpClient) {}

  register(data: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register`, data);
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials, {
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

  getToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  saveToken(token: string): void {
    localStorage.setItem('accessToken', token);
  }

  clearToken(): void {
    localStorage.removeItem('accessToken');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
