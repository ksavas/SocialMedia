import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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
  private readonly authUrl = `${environment.apiUrl}/api/auth`;
  private readonly usersUrl = `${environment.apiUrl}/api/users`;

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/register`, request, { withCredentials: true });
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.authUrl}/login`,
      { provider: 'LOCAL', username, password },
      { withCredentials: true },
    );
  }

  oauthLogin(provider: 'GOOGLE' | 'MICROSOFT', idToken: string, state: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.authUrl}/login`,
      { provider, idToken, state },
      { withCredentials: true },
    );
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/refresh`, null, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.authUrl}/logout`, null, { withCredentials: true });
  }

  updateProfile(request: UpdateProfileRequest): Observable<AuthResponse> {
    return this.http.patch<AuthResponse>(`${this.usersUrl}/me`, request);
  }

  getUsers(page = 0, size = 20): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(this.usersUrl, { params: { page, size } });
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

export interface PostResponse {
  id: string;
  authorId: string;
  authorUsername: string;
  content: string;
  likeCount: number;
  commentCount: number;
  likedByMe: boolean;
  createdAt: string;
}

export interface CommentResponse {
  id: string;
  postId: string;
  authorId: string;
  authorUsername: string;
  content: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class PostApiService {
  private readonly postsUrl = `${environment.apiUrl}/api/posts`;
  private readonly feedUrl = `${environment.apiUrl}/api/feed`;

  constructor(private http: HttpClient) {}

  getFeed(): Observable<PostResponse[]> {
    return this.http.get<PostResponse[]>(this.feedUrl);
  }

  createPost(content: string): Observable<PostResponse> {
    return this.http.post<PostResponse>(this.postsUrl, { content });
  }

  getPost(id: string): Observable<PostResponse> {
    return this.http.get<PostResponse>(`${this.postsUrl}/${id}`);
  }

  deletePost(id: string): Observable<void> {
    return this.http.delete<void>(`${this.postsUrl}/${id}`);
  }

  likePost(id: string): Observable<void> {
    return this.http.post<void>(`${this.postsUrl}/${id}/like`, null);
  }

  unlikePost(id: string): Observable<void> {
    return this.http.delete<void>(`${this.postsUrl}/${id}/like`);
  }

  addComment(postId: string, content: string): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(`${this.postsUrl}/${postId}/comments`, { content });
  }

  getComments(postId: string): Observable<CommentResponse[]> {
    return this.http.get<CommentResponse[]>(`${this.postsUrl}/${postId}/comments`);
  }
}
