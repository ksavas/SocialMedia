import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AuthStateService } from '../auth/auth-state.service';
import { NotificationService, NotificationItem } from './notifications/notification.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit, OnDestroy {
  readonly auth = inject(AuthStateService);
  readonly notificationSvc = inject(NotificationService);
  private readonly router = inject(Router);

  readonly showPanel = signal(false);

  ngOnInit(): void {
    this.notificationSvc.connect();
  }

  ngOnDestroy(): void {
    this.notificationSvc.disconnect();
  }

  togglePanel(): void {
    this.showPanel.update((v) => !v);
  }

  markAsRead(n: NotificationItem, event: Event): void {
    event.stopPropagation();
    if (!n.read) {
      this.notificationSvc.markAsRead(n.id);
    }
  }

  logout(): void {
    this.notificationSvc.disconnect();
    this.auth.logout();
    this.router.navigate(['/register']);
  }
}
