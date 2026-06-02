import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthStateService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/register']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthStateService);
  const router = inject(Router);
  return auth.isLoggedIn() ? router.createUrlTree(['/dashboard']) : true;
};
