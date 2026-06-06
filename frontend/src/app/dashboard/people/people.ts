import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService, UserSummary } from '../../auth/auth';

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
  error = signal('');

  ngOnInit(): void {
    this.authService.getUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load users.');
        this.loading.set(false);
      },
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
