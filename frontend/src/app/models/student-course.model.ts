export interface StudentCourseList {
  id: number;
  title: string;
  summary?: string;
  category?: string;
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  estimatedHours?: number;
  thumbnailUrl?: string;
  teacher: TeacherInfo;
  enrollment?: EnrollmentInfo;
  totalStudents: number;
  publishedAt: string;
}

export interface TeacherInfo {
  id: number;
  username: string;
  fullName: string;
}

export interface EnrollmentInfo {
  enrollmentId: number;
  progressPercent: number;
  completed: boolean;
  enrolledAt: string;
  lastAccessedAt?: string;
}

export interface StudentCourseDetail {
  id: number;
  title: string;
  slug: string;
  summary?: string;
  description?: string;
  category?: string;
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  estimatedHours?: number;
  thumbnailUrl?: string;
  language: string;
  objectives?: string;
  prerequisites?: string;
  teacher: TeacherInfo;
  sections: SectionInfo[];
  enrollment?: EnrollmentInfo;
  totalLessons: number;
  totalStudents: number;
  publishedAt: string;
}

export interface SectionInfo {
  id: number;
  title: string;
  description?: string;
  orderIndex: number;
  lessons: LessonInfo[];
}

export interface LessonInfo {
  id: number;
  title: string;
  type: 'VIDEO' | 'DOCUMENT' | 'TEXT' | 'QUIZ' | 'ASSIGNMENT';
  orderIndex: number;
  durationSeconds?: number;
  completed: boolean;
  progressPercent: number;
}

export interface LessonContent {
  id: number;
  title: string;
  type: string;
  description?: string;
  content?: string;
  fileUrl?: string;
  fileName?: string;
  mimeType?: string;
  fileSizeBytes?: number;
  durationSeconds?: number;
  externalVideoUrl?: string;
  downloadable: boolean;
  progress: ProgressInfo;
  navigation: NavigationInfo;
}

export interface ProgressInfo {
  completed: boolean;
  progressPercent: number;
  lastPositionSeconds?: number;
}

export interface NavigationInfo {
  previousLessonId?: number;
  nextLessonId?: number;
  sectionTitle: string;
  lessonNumber: number;
  totalLessons: number;
}

export interface UpdateLessonProgressRequest {
  progressPercent?: number;
  lastPositionSeconds?: number;
  completed?: boolean;
}

export interface StudentDashboard {
  stats: DashboardStats;
  recentCourses: EnrolledCourseInfo[];
  inProgressCourses: EnrolledCourseInfo[];
}

export interface DashboardStats {
  totalEnrolledCourses: number;
  completedCourses: number;
  inProgressCourses: number;
  averageProgress: number;
}

export interface EnrolledCourseInfo {
  courseId: number;
  courseTitle: string;
  courseThumbnail?: string;
  progressPercent: number;
  teacherName: string;
  lastAccessedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
