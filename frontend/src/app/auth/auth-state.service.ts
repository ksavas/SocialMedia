import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem('token'));
  private readonly usernameSignal = signal<string | null>(localStorage.getItem('username'));

  readonly isLoggedIn = computed(() => !!this.tokenSignal());
  readonly username = this.usernameSignal.asReadonly();

  login(token: string, username: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    this.tokenSignal.set(token);
    this.usernameSignal.set(username);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    this.tokenSignal.set(null);
    this.usernameSignal.set(null);
  }
}
