import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService, UserSummary } from '../../auth/auth';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-people',
  imports: [RouterLink],
  templateUrl: './people.html',
  styleUrl: './people.scss',
})
export class People implements OnInit {
  private readonly authService = inject(AuthService);

  users = signal<UserSummary[]>([]);
  loading = signal(true);
  loadingMore = signal(false);
  hasMore = signal(false);
  error = signal('');
  private page = 0;

  ngOnInit(): void {
    this.authService.getUsers(0, PAGE_SIZE).subscribe({
      next: (users) => {
        this.users.set(users);
        this.hasMore.set(users.length === PAGE_SIZE);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load users.');
        this.loading.set(false);
      },
    });
  }

  loadMore(): void {
    this.loadingMore.set(true);
    this.authService.getUsers(this.page + 1, PAGE_SIZE).subscribe({
      next: (users) => {
        this.page++;
        this.users.update((list) => [...list, ...users]);
        this.hasMore.set(users.length === PAGE_SIZE);
        this.loadingMore.set(false);
      },
      error: () => this.loadingMore.set(false),
    });
  }

  toggleFollow(user: UserSummary): void {
    const action$ = user.isFollowing
      ? this.authService.unfollow(user.id)
      : this.authService.follow(user.id);

    action$.subscribe({
      next: () => {
        this.users.update((list) =>
          list.map((u) =>
            u.id === user.id ? { ...u, isFollowing: !u.isFollowing } : u
          )
        );
      },
    });
  }
}
