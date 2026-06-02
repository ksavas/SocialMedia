import { Component, inject } from '@angular/core';
import { AuthStateService } from '../../auth/auth-state.service';

@Component({
  selector: 'app-user-settings',
  templateUrl: './user-settings.html',
  styleUrl: './user-settings.scss',
})
export class UserSettings {
  readonly auth = inject(AuthStateService);
}
