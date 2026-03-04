import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './homepage.component.html'
})
export class HomepageComponent implements OnInit {

  // ✅ Signals
  username = signal<string | null>(null);
  isAdmin = signal(false);
  isTeacher = signal(false);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.username.set(this.authService.getCurrentUsername());
    this.isAdmin.set(this.authService.isAdmin());
    this.isTeacher.set(this.authService.isTeacher());
  }
}
