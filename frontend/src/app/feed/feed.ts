import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStateService } from '../auth/auth-state.service';

@Component({
  selector: 'app-feed',
  template: `
    <div style="padding: 2rem">
      <h2>Feed</h2>
      <p>Welcome, {{ auth.username() }}!</p>
      <button (click)="logout()">Logout</button>
    </div>
  `,
})
export class Feed {
  readonly auth = inject(AuthStateService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/register']);
  }
}
