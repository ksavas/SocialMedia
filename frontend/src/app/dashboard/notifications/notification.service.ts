import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

export interface NotificationItem {
  id: string;
  recipientId: string;
  type: 'FOLLOWED' | 'POST_LIKED' | 'COMMENT_CREATED' | 'NEW_POST';
  actorId: string;
  postId: string | null;
  read: boolean;
  createdAt: string;
}

interface StreamTicket {
  ticket: string;
}

const INITIAL_RECONNECT_DELAY_MS = 1000;
const MAX_RECONNECT_DELAY_MS = 30000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8085/api/notifications';

  readonly notifications = signal<NotificationItem[]>([]);
  readonly unreadCount = computed(() => this.notifications().filter(n => !n.read).length);
  readonly newPostCount = signal<number>(0);

  private eventSource?: EventSource;
  private reconnectTimer?: ReturnType<typeof setTimeout>;
  private reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
  private active = false;

  connect(): void {
    if (!localStorage.getItem('token')) return;
    this.active = true;
    this.openStream();
  }

  disconnect(): void {
    this.active = false;
    clearTimeout(this.reconnectTimer);
    this.closeStream();
    this.reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
    this.notifications.set([]);
    this.newPostCount.set(0);
  }

  private openStream(): void {
    if (!this.active || !localStorage.getItem('token')) return;

    this.http.post<StreamTicket>(`${this.baseUrl}/stream-ticket`, null).subscribe({
      next: ({ ticket }) => {
        if (!this.active) return;
        const source = new EventSource(`${this.baseUrl}/stream?ticket=${encodeURIComponent(ticket)}`);
        source.onopen = () => {
          this.reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
          this.loadNotifications();
        };
        source.onmessage = (event) => this.handleMessage(JSON.parse(event.data));
        source.onerror = () => this.scheduleReconnect();
        this.eventSource = source;
      },
      error: () => this.scheduleReconnect(),
    });
  }

  private scheduleReconnect(): void {
    this.closeStream();
    if (!this.active) return;
    clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => this.openStream(), this.reconnectDelayMs);
    this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
  }

  private closeStream(): void {
    this.eventSource?.close();
    this.eventSource = undefined;
  }

  private loadNotifications(): void {
    this.http.get<NotificationItem[]>(this.baseUrl).subscribe({
      next: (list) => this.notifications.set(list),
    });
  }

  private handleMessage(notification: NotificationItem): void {
    if (notification.type === 'NEW_POST') {
      this.newPostCount.update((n) => n + 1);
    } else {
      this.notifications.update((list) => [notification, ...list]);
    }
  }

  clearNewPosts(): void {
    this.newPostCount.set(0);
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
      case 'NEW_POST': return 'New post available';
    }
  }
}
