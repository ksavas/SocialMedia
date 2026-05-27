import { Routes } from '@angular/router';
import { Register } from './auth/register/register';
import { Login } from './auth/login/login';
import { OAuthCallback } from './auth/callback/callback';
import { Feed } from './feed/feed';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'auth/callback', component: OAuthCallback },
  { path: 'feed', component: Feed },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
