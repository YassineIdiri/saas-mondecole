import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, take, tap } from 'rxjs/operators';
import { AuthApiService, AuthResponse, LoginRequest, UserOrganizationInfo } from './auth-api.service';
import { TokenStore } from './token-store.service';
import { JwtDecoderService, JwtPayload } from './jwt-decoder.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private isAuthenticatedSubject!: BehaviorSubject<boolean>;
  isAuthenticated$!: Observable<boolean>;

  private isReadySubject = new BehaviorSubject<boolean>(false);
  isReady$ = this.isReadySubject.asObservable();

  constructor(
    private authApi: AuthApiService,
    private tokenStore: TokenStore,
    private jwtDecoder: JwtDecoderService,
    private router: Router
  ) {
    this.isAuthenticatedSubject = new BehaviorSubject<boolean>(this.tokenStore.has());
    this.isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
    this.initAuth();
  }

  // ════════════════════════════════════════════════════════
  // INITIALISATION
  // ════════════════════════════════════════════════════════

  waitForReady(): Promise<void> {
    return new Promise(resolve => {
      this.isReady$.pipe(
        filter(ready => ready),
        take(1)
      ).subscribe(() => resolve());
    });
  }

  private initAuth(): void {

    const token = this.tokenStore.get();

    if (!token) {
      this.isAuthenticatedSubject.next(false);
      this.isReadySubject.next(true);
      return;
    }

    if (!this.jwtDecoder.isExpired(token)) {
      this.isAuthenticatedSubject.next(true);
      this.isReadySubject.next(true);
      return;
    }

    // Token expiré → refresh silencieux
    this.authApi.refresh().subscribe({
      next: (response) => {
        this.tokenStore.set(response.accessToken);
        this.saveUsername(response.username);
        this.isAuthenticatedSubject.next(true);
        this.isReadySubject.next(true);
      },
      error: () => {
        this.clearSession();
        this.isReadySubject.next(true);
      }
    });
  }

  // ════════════════════════════════════════════════════════
  // LOGIN & LOGOUT
  // ════════════════════════════════════════════════════════

  getUserOrganization(username: string): Observable<UserOrganizationInfo> {
    return this.authApi.getUserOrganization(username);
  }

  login(credentials: LoginRequest, organizationId: number): Observable<AuthResponse> {
    return this.authApi.login(credentials, organizationId).pipe(
      tap(response => {
        this.tokenStore.set(response.accessToken);
        this.saveUsername(response.username);
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  refreshToken(): Observable<AuthResponse> {
    return this.authApi.refresh().pipe(
      tap(response => {
        this.tokenStore.set(response.accessToken);
        this.saveUsername(response.username);
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  logout(): Observable<void> {
    return this.authApi.logout().pipe(
      tap(() => this.clearSession())
    );
  }

  logoutAndRedirect(reason: string = 'session_expired'): void {
    this.clearSession();
    this.router.navigate(['/'], { queryParams: { reason } });
  }

  // ════════════════════════════════════════════════════════
  // JWT PAYLOAD
  // ════════════════════════════════════════════════════════

  private getPayload(): JwtPayload | null {
    const token = this.tokenStore.get();
    if (!token) return null;
    return this.jwtDecoder.decode(token);
  }

  getToken(): string | null {
    return this.tokenStore.get();
  }

  getUserId(): number | null {
    return this.getPayload()?.userId || null;
  }

  getOrganizationId(): number | null {
    return this.getPayload()?.organizationId || null;
  }

  getCurrentUsername(): string | null {
    const payload = this.getPayload();
    if (payload?.sub) return payload.sub;
    return localStorage.getItem('username');
  }

  getUserRole(): string | null {
    return this.getPayload()?.role || null;
  }

  isAdmin(): boolean { return this.getUserRole() === 'ADMIN'; }
  isTeacher(): boolean { return this.getUserRole() === 'TEACHER'; }
  isStudent(): boolean { return this.getUserRole() === 'STUDENT'; }

  // ════════════════════════════════════════════════════════
  // SESSION MANAGEMENT
  // ════════════════════════════════════════════════════════

  clearSession(): void {
    this.tokenStore.clear();
    localStorage.removeItem('username');
    this.isAuthenticatedSubject.next(false);
  }

  saveUsername(username: string): void {
    localStorage.setItem('username', username);
  }

  isLoggedIn(): boolean {
    const token = this.tokenStore.get();
    if (!token) return false;
    return !this.jwtDecoder.isExpired(token);
  }
}
