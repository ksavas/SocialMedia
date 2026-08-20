import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, firstValueFrom, from, shareReplay } from 'rxjs';
import { AuthService } from './auth';
import { AuthStateService } from './auth-state.service';

const REFRESH_LOCK = 'auth-token-refresh';

@Injectable({ providedIn: 'root' })
export class TokenRefreshService {
  private readonly authApi = inject(AuthService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  private inFlight: Observable<string> | null = null;

  refresh(rejectedToken: string): Observable<string> {
    this.inFlight ??= from(this.refreshOncePerBrowser(rejectedToken)).pipe(
      finalize(() => (this.inFlight = null)),
      shareReplay(1),
    );
    return this.inFlight;
  }

  private async refreshOncePerBrowser(rejectedToken: string): Promise<string> {
    return navigator.locks.request(REFRESH_LOCK, async () => {
      const current = localStorage.getItem('token');
      if (current && current !== rejectedToken) {
        return current;
      }
      try {
        const response = await firstValueFrom(this.authApi.refresh());
        this.authState.login(response.token, response.username, response.provider);
        return response.token;
      } catch (error) {
        this.endSession();
        throw error;
      }
    });
  }

  private endSession(): void {
    this.authState.logout();
    this.router.navigate(['/register']);
  }
}
