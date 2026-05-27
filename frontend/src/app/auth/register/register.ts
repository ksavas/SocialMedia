import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../auth';
import { OAuthButtons } from '../oauth-buttons/oauth-buttons';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, RouterLink, OAuthButtons],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  form: RegisterRequest = {
    username: '',
    email: '',
    password: '',
    displayName: '',
  };

  errorMessage = '';
  isLoading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  onSubmit() {
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.register(this.form).subscribe({
      next: (response) => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('username', response.username);
        alert('Registration succeeded! Welcome ' + response.username);
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Registration failed';
        this.isLoading = false;
      },
    });
  }
}
