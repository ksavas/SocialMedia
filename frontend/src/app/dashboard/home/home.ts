import { Component, inject } from '@angular/core';
import { AuthStateService } from '../../auth/auth-state.service';

@Component({
  selector: 'app-dashboard-home',
  template: `
    <div class="home-content">
      <h2>Welcome, {{ auth.username() }}!</h2>
      <p>Your feed will appear here.</p>
    </div>
  `,
  styles: [`
    .home-content {
      padding: 2rem;
    }
    h2 {
      color: #2c2c2c;
      font-weight: 300;
      font-size: 1.75rem;
      margin-bottom: 0.5rem;
    }
    p {
      color: #888;
    }
  `],
})
export class DashboardHome {
  readonly auth = inject(AuthStateService);
}