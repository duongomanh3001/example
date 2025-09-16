import { apiClient } from '@/lib/api-client';
import { LoginRequest, JwtResponse, CreateUserRequest, MessageResponse } from '@/types/auth';

export class AuthService {
  /**
   * Sign in user
   */
  static async signIn(credentials: LoginRequest): Promise<JwtResponse> {
    const response = await apiClient.post<JwtResponse>('/api/auth/signin', credentials);
    
    // Store token in localStorage
    if (response.token) {
      localStorage.setItem('token', response.token);
      localStorage.setItem('user', JSON.stringify({
        id: response.id,
        username: response.username,
        email: response.email,
        fullName: response.fullName,
        studentId: response.studentId,
        role: response.role
      }));
    }
    
    return response;
  }

  /**
   * Sign out user
   */
  static signOut(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  /**
   * Get current user from localStorage
   */
  static getCurrentUser() {
    if (typeof window !== 'undefined') {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    }
    return null;
  }

  /**
   * Get current token from localStorage
   */
  static getToken(): string | null {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('token');
    }
    return null;
  }

  /**
   * Check if user is authenticated
   */
  static isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /**
   * Check if user has specific role
   */
  static hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user?.role === role;
  }

  /**
   * Check if user has any of the specified roles
   */
  static hasAnyRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    return roles.includes(user?.role);
  }

  /**
   * Create user (Admin only)
   */
  static async createUser(userData: CreateUserRequest): Promise<MessageResponse> {
    return apiClient.post<MessageResponse>('/api/admin/users', userData);
  }
}

export default AuthService;
