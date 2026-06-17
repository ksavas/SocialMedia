import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

export interface NotificationItem {
  id: string;
  recipientId: string;
  type: 'FOLLOWED' | 'POST_LIKED' | 'COMMENT_CREATED';
  actorId: string;
  postId: string | null;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8085/api/notifications';

  readonly notifications = signal<NotificationItem[]>([]);
  readonly unreadCount = computed(() => this.notifications().filter(n => !n.read).length);

  private eventSource?: EventSource;

  connect(): void {
    const token = localStorage.getItem('token');
    if (!token) return;

    this.http.get<NotificationItem[]>(this.baseUrl).subscribe({
      next: (list) => this.notifications.set(list),
    });

    this.eventSource = new EventSource(`${this.baseUrl}/stream?token=${token}`);
    this.eventSource.onmessage = (event) => {
      const notification: NotificationItem = JSON.parse(event.data);
      this.notifications.update((list) => [notification, ...list]);
    };
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = undefined;
    this.notifications.set([]);
  }

  markAsRead(id: string): void {
    this.http.put<void>(`${this.baseUrl}/${id}/read`, null).pipe(
      tap(() => this.notifications.update((list) =>
        list.map((n) => (n.id === id ? { ...n, read: true } : n))
      ))
    ).subscribe();
  }

  labelFor(n: NotificationItem): string {
    switch (n.type) {
      case 'FOLLOWED': return 'You have a new follower';
      case 'POST_LIKED': return 'Someone liked your post';
      case 'COMMENT_CREATED': return 'Someone commented on your post';
    }
  }
}
