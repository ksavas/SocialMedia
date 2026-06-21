import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthStateService } from '../../auth/auth-state.service';
import { PostApiService, PostResponse } from '../../auth/auth';
import { NotificationService } from '../notifications/notification.service';

@Component({
  selector: 'app-dashboard-home',
  imports: [FormsModule, DatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class DashboardHome implements OnInit {
  readonly auth = inject(AuthStateService);
  private readonly postApi = inject(PostApiService);
  readonly notificationSvc = inject(NotificationService);

  posts = signal<PostResponse[]>([]);
  newContent = '';
  submitting = signal(false);

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.postApi.getFeed().subscribe({
      next: (posts) => this.posts.set(posts),
    });
  }

  showNewPosts(): void {
    this.notificationSvc.clearNewPosts();
    this.loadPosts();
  }

  submit(): void {
    if (!this.newContent.trim()) return;
    this.submitting.set(true);
    this.postApi.createPost(this.newContent.trim()).subscribe({
      next: (post) => {
        this.posts.update((list) => [post, ...list]);
        this.newContent = '';
        this.submitting.set(false);
      },
      error: () => this.submitting.set(false),
    });
  }

  toggleLike(post: PostResponse): void {
    const action$ = post.likedByMe
      ? this.postApi.unlikePost(post.id)
      : this.postApi.likePost(post.id);

    action$.subscribe({
      next: () => {
        this.posts.update((list) =>
          list.map((p) =>
            p.id === post.id
              ? { ...p, likedByMe: !p.likedByMe, likeCount: p.likedByMe ? p.likeCount - 1 : p.likeCount + 1 }
              : p
          )
        );
      },
    });
  }

  deletePost(post: PostResponse): void {
    this.postApi.deletePost(post.id).subscribe({
      next: () => this.posts.update((list) => list.filter((p) => p.id !== post.id)),
    });
  }
}
