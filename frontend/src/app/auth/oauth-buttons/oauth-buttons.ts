import { Component } from '@angular/core';
import { OAuthProviderService } from '../oauth.service';

@Component({
  selector: 'app-oauth-buttons',
  templateUrl: './oauth-buttons.html',
  styleUrl: './oauth-buttons.scss',
})
export class OAuthButtons {
  constructor(private oauthProviderService: OAuthProviderService) {}

  loginWithGoogle(): void {
    this.oauthProviderService.loginWithGoogle();
  }
}
