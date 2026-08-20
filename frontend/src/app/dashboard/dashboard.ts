import { Component, HostListener, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AuthService } from '../auth/auth';
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
  private readonly authApi = inject(AuthService);
  private readonly router = inject(Router);

  readonly showPanel = signal(false);

  ngOnInit(): void {
    this.notificationSvc.connect();
  }

  ngOnDestroy(): void {
    this.notificationSvc.disconnect();
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showPanel.set(false);
  }

  togglePanel(event: Event): void {
    event.stopPropagation();
    this.showPanel.update((v) => !v);
  }

  markAsRead(n: NotificationItem, event: Event): void {
    event.stopPropagation();
    if (!n.read) {
      this.notificationSvc.markAsRead(n.id);
    }
  }

  logout(): void {
    this.authApi.logout().subscribe({ error: () => undefined });
    this.notificationSvc.disconnect();
    this.auth.logout();
    this.router.navigate(['/register']);
  }
}
