import { apiClient } from '@/lib/api-client';
import { 
  User, 
  CreateUserRequest, 
  UpdateUserRequest, 
  MessageResponse,
  Role 
} from '@/types/auth';
import { UserStatsResponse } from '@/types/api';

export class UserService {
  /**
   * Get all users (Admin only)
   */
  static async getAllUsers(): Promise<User[]> {
    return apiClient.get<User[]>('/api/admin/users');
  }

  /**
   * Get user by ID (Admin only)
   */
  static async getUserById(id: number): Promise<User> {
    return apiClient.get<User>(`/api/admin/users/${id}`);
  }

  /**
   * Create user (Admin only)
   */
  static async createUser(userData: CreateUserRequest): Promise<MessageResponse> {
    return apiClient.post<MessageResponse>('/api/admin/users', userData);
  }

  /**
   * Update user (Admin only)
   */
  static async updateUser(id: number, userData: UpdateUserRequest): Promise<MessageResponse> {
    return apiClient.put<MessageResponse>(`/api/admin/users/${id}`, userData);
  }

  /**
   * Delete user (Admin only)
   */
  static async deleteUser(id: number): Promise<MessageResponse> {
    return apiClient.delete<MessageResponse>(`/api/admin/users/${id}`);
  }

  /**
   * Toggle user status (Admin only)
   */
  static async toggleUserStatus(id: number): Promise<MessageResponse> {
    return apiClient.patch<MessageResponse>(`/api/admin/users/${id}/toggle-status`);
  }

  /**
   * Get all students (Admin only)
   */
  static async getAllStudents(): Promise<User[]> {
    return apiClient.get<User[]>('/api/admin/students');
  }

  /**
   * Get all teachers (Admin only)
   */
  static async getAllTeachers(): Promise<User[]> {
    return apiClient.get<User[]>('/api/admin/teachers');
  }

  /**
   * Search users (Admin only)
   */
  static async searchUsers(keyword: string): Promise<User[]> {
    return apiClient.get<User[]>(`/api/admin/users/search?keyword=${encodeURIComponent(keyword)}`);
  }

  /**
   * Get user statistics (Admin only)
   */
  static async getUserStats(): Promise<UserStatsResponse> {
    return apiClient.get<UserStatsResponse>('/api/admin/stats');
  }
}

export default UserService;
