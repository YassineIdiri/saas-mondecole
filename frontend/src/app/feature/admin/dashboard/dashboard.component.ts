import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, UserListResponse, DashboardStatsResponse } from '../../../service/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit {

  stats = signal<DashboardStatsResponse | null>(null);
  teachers = signal<UserListResponse[]>([]);
  students = signal<UserListResponse[]>([]);
  activeTab = signal<'teachers' | 'students'>('teachers');

  loadingStats = signal(true);
  loadingTeachers = signal(true);
  loadingStudents = signal(true);

  teachersPage = signal(0);
  teachersTotal = signal(0);
  teachersTotalPages = signal(0);

  studentsPage = signal(0);
  studentsTotal = signal(0);
  studentsTotalPages = signal(0);

  readonly Math = Math;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadTeachers();
    this.loadStudents();
  }

  loadStats(): void {
    this.loadingStats.set(true);

    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.loadingStats.set(false);
      },
      error: (error) => {
        console.error('Error loading stats:', error);
        this.loadingStats.set(false);
      }
    });
  }

  loadTeachers(page: number = 0): void {
    this.loadingTeachers.set(true);

    this.adminService.listTeachers(page, 10).subscribe({
      next: (response) => {
        this.teachers.set(response.content);
        this.teachersPage.set(response.number);
        this.teachersTotal.set(response.totalElements);
        this.teachersTotalPages.set(response.totalPages);
        this.loadingTeachers.set(false);
      },
      error: (error) => {
        console.error('Error loading teachers:', error);
        this.loadingTeachers.set(false);
      }
    });
  }

  loadStudents(page: number = 0): void {
    this.loadingStudents.set(true);

    this.adminService.listStudents(page, 10).subscribe({
      next: (response) => {
        this.students.set(response.content);
        this.studentsPage.set(response.number);
        this.studentsTotal.set(response.totalElements);
        this.studentsTotalPages.set(response.totalPages);
        this.loadingStudents.set(false);
      },
      error: (error) => {
        console.error('Error loading students:', error);
        this.loadingStudents.set(false);
      }
    });
  }

  switchTab(tab: 'teachers' | 'students'): void {
    this.activeTab.set(tab);
  }

  toggleUserStatus(user: UserListResponse): void {
    if (!confirm(`Are you sure you want to ${user.active ? 'deactivate' : 'activate'} ${user.fullName}?`)) return;

    this.adminService.toggleUserStatus(user.id).subscribe({
      next: () => {
        user.active = !user.active;
        this.teachers.update(list => [...list]);
        this.students.update(list => [...list]);
      },
      error: (error) => {
        console.error('Error toggling user status:', error);
        alert('Failed to update user status');
      }
    });
  }

  toggleUserLock(user: UserListResponse): void {
    if (!confirm(`Are you sure you want to ${user.locked ? 'unlock' : 'lock'} ${user.fullName}?`)) return;

    this.adminService.toggleUserLock(user.id).subscribe({
      next: () => {
        user.locked = !user.locked;
        this.teachers.update(list => [...list]);
        this.students.update(list => [...list]);
      },
      error: (error) => {
        console.error('Error toggling user lock:', error);
        alert('Failed to update user lock status');
      }
    });
  }

  deleteUser(user: UserListResponse): void {
    if (!confirm(`Are you sure you want to delete ${user.fullName}? This action cannot be undone.`)) return;

    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        if (user.role === 'TEACHER') {
          this.loadTeachers(this.teachersPage()); // ✅ signal()
        } else {
          this.loadStudents(this.studentsPage()); // ✅ signal()
        }
      },
      error: (error) => {
        console.error('Error deleting user:', error);
        alert('Failed to delete user');
      }
    });
  }

  nextTeachersPage(): void {
    if (this.teachersPage() < this.teachersTotalPages() - 1) {
      this.loadTeachers(this.teachersPage() + 1);
    }
  }

  previousTeachersPage(): void {
    if (this.teachersPage() > 0) {
      this.loadTeachers(this.teachersPage() - 1);
    }
  }

  nextStudentsPage(): void {
    if (this.studentsPage() < this.studentsTotalPages() - 1) {
      this.loadStudents(this.studentsPage() + 1);
    }
  }

  previousStudentsPage(): void {
    if (this.studentsPage() > 0) {
      this.loadStudents(this.studentsPage() - 1);
    }
  }

  formatDate(date: string | null): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  }
}
