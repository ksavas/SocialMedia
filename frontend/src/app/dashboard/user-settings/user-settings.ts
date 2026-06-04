import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth/auth';
import { AuthStateService } from '../../auth/auth-state.service';

@Component({
  selector: 'app-user-settings',
  imports: [FormsModule],
  templateUrl: './user-settings.html',
  styleUrl: './user-settings.scss',
})
export class UserSettings {
  readonly auth = inject(AuthStateService);
  private readonly authService = inject(AuthService);

  username = this.auth.username() ?? '';
  email = '';
  password = '';
  showPassword = false;

  isSaving = false;
  successMessage = '';
  usernameError = '';
  emailError = '';
  generalError = '';

  save(): void {
    this.isSaving = true;
    this.successMessage = '';
    this.usernameError = '';
    this.emailError = '';
    this.generalError = '';

    this.authService.updateProfile({
      username: this.username || null,
      password: this.password || null,
      email: this.email || null,
    }).subscribe({
      next: (response) => {
        this.auth.login(response.token, response.username, response.provider);
        this.username = response.username;
        this.password = '';
        this.email = '';
        this.successMessage = 'Settings saved successfully.';
        this.isSaving = false;
      },
      error: (err) => {
        const message: string = err.error?.message ?? 'Something went wrong';
        if (message.toLowerCase().includes('username')) {
          this.usernameError = message;
        } else if (message.toLowerCase().includes('email')) {
          this.emailError = message;
        } else {
          this.generalError = message;
        }
        this.isSaving = false;
      },
    });
  }
}
