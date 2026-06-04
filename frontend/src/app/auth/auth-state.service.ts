import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem('token'));
  private readonly usernameSignal = signal<string | null>(localStorage.getItem('username'));
  private readonly providerSignal = signal<string | null>(localStorage.getItem('provider'));

  readonly isLoggedIn = computed(() => !!this.tokenSignal());
  readonly username = this.usernameSignal.asReadonly();
  readonly provider = this.providerSignal.asReadonly();
  readonly isLocalProvider = computed(() => this.providerSignal() === 'LOCAL');

  login(token: string, username: string, provider: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('provider', provider);
    this.tokenSignal.set(token);
    this.usernameSignal.set(username);
    this.providerSignal.set(provider);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('provider');
    this.tokenSignal.set(null);
    this.usernameSignal.set(null);
    this.providerSignal.set(null);
  }
}
