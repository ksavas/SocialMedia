import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName: string;
}

export interface UpdateProfileRequest {
  username: string | null;
  password: string | null;
  email: string | null;
}

export interface AuthResponse {
  token: string;
  username: string;
  provider: string;
}

export interface UserSummary {
  id: string;
  username: string;
  displayName: string | null;
  isFollowing: boolean;
}

export interface UserProfile {
  id: string;
  username: string;
  displayName: string | null;
  followerCount: number;
  followingCount: number;
  isFollowing: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly authUrl = 'http://localhost:8081/api/auth';
  private readonly usersUrl = 'http://localhost:8081/api/users';

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/register`, request);
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, { provider: 'LOCAL', username, password });
  }

  oauthLogin(provider: 'GOOGLE' | 'MICROSOFT', idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, { provider, idToken });
  }

  updateProfile(request: UpdateProfileRequest): Observable<AuthResponse> {
    return this.http.patch<AuthResponse>(`${this.usersUrl}/me`, request);
  }

  getUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(this.usersUrl);
  }

  getProfile(id: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.usersUrl}/${id}`);
  }

  follow(id: string): Observable<void> {
    return this.http.post<void>(`${this.usersUrl}/${id}/follow`, null);
  }

  unfollow(id: string): Observable<void> {
    return this.http.delete<void>(`${this.usersUrl}/${id}/follow`);
  }
}
