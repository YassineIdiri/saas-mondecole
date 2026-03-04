import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import {
  BehaviorSubject,
  catchError,
  filter,
  switchMap,
  take,
  throwError,
} from 'rxjs';
import { AuthApiService } from '../service/auth-api.service';
import { TokenStore } from '../service/token-store.service';
import { AuthService } from '../service/auth.service';

let refreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

/**
 * Check if URL is an auth route (should not have token intercepted)
 */
function isAuthRoute(url: string): boolean {
  return url.includes('/api/auth/') ||
         url.includes('/api/public/');
}

/**
 * Check if request already has Bearer token
 */
function hasBearer(req: HttpRequest<unknown>): boolean {
  const h = req.headers.get('Authorization');
  return !!h && h.startsWith('Bearer ');
}

/**
 * Clone request with Bearer token
 */
function withBearer(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * Add organizationId header if available
 */
function withOrganizationId(req: HttpRequest<unknown>, orgId: number): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      'X-Organization-Id': orgId.toString()
    }
  });
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStore = inject(TokenStore);
  const authApi = inject(AuthApiService);
  const auth = inject(AuthService);

  // Skip auth routes
  if (isAuthRoute(req.url)) {
    return next(req);
  }

  // Add token if available
  const token = tokenStore.get();
  let request = token ? withBearer(req, token) : req;

  // Add organization ID if available and not already present
  const orgId = auth.getOrganizationId();
  if (orgId && !req.headers.has('X-Organization-Id')) {
    request = withOrganizationId(request, orgId);
  }

  return next(request).pipe(
    catchError((err: unknown) => {
      // Only handle HTTP errors
      if (!(err instanceof HttpErrorResponse)) {
        return throwError(() => err);
      }

      // Only handle 401 errors
      if (err.status !== 401) {
        return throwError(() => err);
      }

      // Only refresh if request had a Bearer token
      if (!hasBearer(request)) {
        return throwError(() => err);
      }

      // Start refresh if not already refreshing
      if (!refreshing) {
        refreshing = true;
        refreshedToken$.next(null);

        return authApi.refresh().pipe(
          switchMap((res) => {
            refreshing = false;
            tokenStore.set(res.accessToken);
            refreshedToken$.next(res.accessToken);
            return next(withBearer(req, res.accessToken));
          }),
          catchError((refreshErr) => {
            refreshing = false;
            tokenStore.clear();
            auth.logoutAndRedirect('session_expired');
            return throwError(() => refreshErr);
          })
        );
      }

      // Wait for refresh to complete
      return refreshedToken$.pipe(
        filter((t): t is string => t !== null),
        take(1),
        switchMap((newToken) => next(withBearer(req, newToken)))
      );
    })
  );
};
