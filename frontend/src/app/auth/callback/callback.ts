import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth';
import { AuthStateService } from '../auth-state.service';
import { OAuthProviderService } from '../oauth.service';

@Component({
  selector: 'app-oauth-callback',
  template: '<p style="padding:2rem">Signing in...</p>',
})
export class OAuthCallback implements OnInit {
  private readonly oauthService = inject(OAuthProviderService);
  private readonly authService = inject(AuthService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const idToken = this.oauthService.parseCallbackToken();
    if (!idToken) {
      this.router.navigate(['/register']);
      return;
    }
    this.authService.oauthLogin('GOOGLE', idToken).subscribe({
      next: (response) => {
        this.authState.login(response.token, response.username);
        this.router.navigate(['/dashboard']);
      },
      error: () => this.router.navigate(['/register']),
    });
  }
}
