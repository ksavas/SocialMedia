import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';

interface OAuthInitResponse {
  state: string;
  nonce: string;
}

const PENDING_STATE_KEY = 'oauth_pending_state';

@Injectable({ providedIn: 'root' })
export class OAuthProviderService {
  private readonly http = inject(HttpClient);
  private readonly initUrl = `${environment.api.user}/api/auth/oauth/init`;

  loginWithGoogle(): Observable<void> {
    return this.http.get<OAuthInitResponse>(this.initUrl).pipe(
      tap(({ state, nonce }) => {
        sessionStorage.setItem(PENDING_STATE_KEY, state);
        const params = new URLSearchParams({
          client_id: environment.googleClientId,
          redirect_uri: `${window.location.origin}/auth/callback`,
          response_type: 'id_token',
          scope: 'openid email profile',
          nonce,
          state,
          prompt: 'select_account',
        });
        window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?${params}`;
      }),
      map(() => void 0),
    );
  }

  parseCallbackParams(): { idToken: string | null; state: string | null } {
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);
    return { idToken: params.get('id_token'), state: params.get('state') };
  }

  consumePendingState(returnedState: string): boolean {
    const pendingState = sessionStorage.getItem(PENDING_STATE_KEY);
    sessionStorage.removeItem(PENDING_STATE_KEY);
    return pendingState !== null && pendingState === returnedState;
  }
}
