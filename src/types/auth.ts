export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface JwtResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  fullName: string;
  studentId?: string;
  role: Role;
}

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  studentId?: string;
  role: Role;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  studentId?: string;
  role: Role;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  fullName?: string;
  studentId?: string;
  role?: Role;
  isActive?: boolean;
}

export enum Role {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  ADMIN = 'ADMIN'
}

export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}

export interface MessageResponse {
  message: string;
}
