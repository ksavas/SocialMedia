import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService, UserProfile } from '../../auth/auth';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  profile = signal<UserProfile | null>(null);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.authService.getProfile(id).subscribe({
      next: (p) => {
        this.profile.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('User not found.');
        this.loading.set(false);
      },
    });
  }

  toggleFollow(): void {
    const p = this.profile();
    if (!p) return;

    const action$ = p.isFollowing
      ? this.authService.unfollow(p.id)
      : this.authService.follow(p.id);

    action$.subscribe({
      next: () => {
        this.profile.update((prev) =>
          prev
            ? {
                ...prev,
                isFollowing: !prev.isFollowing,
                followerCount: prev.isFollowing
                  ? prev.followerCount - 1
                  : prev.followerCount + 1,
              }
            : prev
        );
      },
    });
  }
}
