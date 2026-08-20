import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { TokenRefreshService } from './token-refresh.service';

const AUTH_ENDPOINTS = '/api/auth/';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.includes(AUTH_ENDPOINTS)) {
    return next(req);
  }

  const tokenRefresh = inject(TokenRefreshService);
  const token = localStorage.getItem('token');

  return next(withBearer(req, token)).pipe(
    catchError((error: unknown) => {
      if (!token || !(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return tokenRefresh.refresh(token).pipe(
        switchMap((freshToken) => next(withBearer(req, freshToken))),
      );
    }),
  );
};

function withBearer<T>(req: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}
