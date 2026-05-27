import { Component } from '@angular/core';

@Component({
  selector: 'app-feed',
  template: `
    <div style="padding: 2rem">
      <h2>Feed</h2>
      <p>Welcome, {{ username }}!</p>
    </div>
  `,
})
export class Feed {
  username = localStorage.getItem('username') ?? '';
}
