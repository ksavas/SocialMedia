import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';

interface OAuthInitResponse {
  state: string;
  nonce: string;
}

@Injectable({ providedIn: 'root' })
export class OAuthProviderService {
  private readonly http = inject(HttpClient);
  private readonly initUrl = 'http://localhost:8081/api/auth/oauth/init';

  loginWithGoogle(): Observable<void> {
    return this.http.get<OAuthInitResponse>(this.initUrl).pipe(
      tap(({ state, nonce }) => {
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
}
