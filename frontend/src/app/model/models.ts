export interface LoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  username: string;
}

export interface RegisterResponse {
  username: string;
  email: string;
  createdAt: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  traceId: string;
  details?: {
    errors?: Record<string, string>;
  };
}
