import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OAuthButtons } from '../oauth-buttons/oauth-buttons';

@Component({
  selector: 'app-login',
  imports: [RouterLink, OAuthButtons],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {}
