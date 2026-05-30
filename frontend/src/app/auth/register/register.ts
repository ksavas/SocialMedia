import { Component, inject } from '@angular/core';
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

  form: RegisterRequest = { username: '', email: '', password: '', displayName: '' };
  errorMessage = '';
  isLoading = false;

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.register(this.form).subscribe({
      next: (response) => {
        this.authState.login(response.token, response.username);
        this.router.navigate(['/feed']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Registration failed';
        this.isLoading = false;
      },
    });
  }
}
