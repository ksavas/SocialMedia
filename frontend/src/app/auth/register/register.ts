import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../auth';
import { AuthStateService } from '../auth-state.service';
import { OAuthButtons } from '../oauth-buttons/oauth-buttons';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, OAuthButtons],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly authService = inject(AuthService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  readonly mode = signal<'register' | 'login'>('register');

  registerForm: RegisterRequest = { username: '', email: '', password: '', displayName: '' };
  loginIdentifier = '';
  loginPassword = '';

  errorMessage = '';
  isLoading = false;

  toggleMode(): void {
    this.mode.update(m => m === 'register' ? 'login' : 'register');
    this.errorMessage = '';
  }

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const request$ = this.mode() === 'register'
      ? this.authService.register(this.registerForm)
      : this.authService.login(this.loginIdentifier, this.loginPassword);

    request$.subscribe({
      next: (response) => {
        this.authState.login(response.token, response.username, response.provider);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Something went wrong';
        this.isLoading = false;
      },
    });
  }
}
