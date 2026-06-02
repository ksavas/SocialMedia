import { Routes } from '@angular/router';
import { Register } from './auth/register/register';
import { OAuthCallback } from './auth/callback/callback';
import { Dashboard } from './dashboard/dashboard';
import { DashboardHome } from './dashboard/home/home';
import { UserSettings } from './dashboard/user-settings/user-settings';
import { authGuard, guestGuard } from './auth/guards';

export const routes: Routes = [
  { path: 'register', component: Register, canActivate: [guestGuard] },
  { path: 'auth/callback', component: OAuthCallback },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [authGuard],
    children: [
      { path: '', component: DashboardHome },
      { path: 'settings', component: UserSettings },
    ],
  },
  { path: '', redirectTo: 'register', pathMatch: 'full' },
];