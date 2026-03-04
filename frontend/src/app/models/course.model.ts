export interface Course {
  id: number;
  organizationId: number;
  authorId: number;
  title: string;
  slug: string;
  summary?: string;
  description?: string;
  category?: string;
  tags?: string[];
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  estimatedHours?: number;
  thumbnailUrl?: string;
  language: string;
  published: boolean;
  publishedAt?: string;
  objectives?: string;
  prerequisites?: string;
  active: boolean;
  createdAt: string;
  updatedAt?: string;

  // Relations
  author?: {
    id: number;
    username: string;
    fullName: string;
  };
}

export interface CourseListResponse {
  id: number;
  title: string;
  summary?: string;
  category?: string;
  level?: string;
  published: boolean;
  createdAt: string;
  estimatedHours?: number;
}

export interface CreateCourseRequest {
  title: string;
  summary?: string;
  description?: string;
  category?: string;
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  estimatedHours?: number;
  objectives?: string;
  prerequisites?: string;
}

export interface UpdateCourseRequest {
  title?: string;
  summary?: string;
  description?: string;
  category?: string;
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  estimatedHours?: number;
  objectives?: string;
  prerequisites?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
