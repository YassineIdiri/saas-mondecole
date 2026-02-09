import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './homepage.component.html'
})
export class HomepageComponent {
  username: string | null = null;
  isLoggingOut = false;

  constructor(private authService: AuthService) {
    this.username = this.authService.getCurrentUsername();
  }

  onLogout(): void {
    this.isLoggingOut = true;
    this.authService.logout().subscribe({
      error: (error) => {
        console.error('Logout error:', error);
        this.isLoggingOut = false;
        this.authService.clearToken();
      }
    });
  }
}
