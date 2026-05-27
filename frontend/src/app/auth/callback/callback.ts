import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth';
import { OAuthProviderService } from '../oauth.service';

@Component({
  selector: 'app-oauth-callback',
  template: '<p style="padding:2rem">Signing in...</p>',
})
export class OAuthCallback implements OnInit {
  constructor(
    private oauthService: OAuthProviderService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const idToken = this.oauthService.parseCallbackToken();
    if (!idToken) {
      this.router.navigate(['/login']);
      return;
    }
    this.authService.oauthLogin('google', idToken).subscribe({
      next: (response) => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('username', response.username);
        this.router.navigate(['/feed']);
      },
      error: () => this.router.navigate(['/login']),
    });
  }
}
