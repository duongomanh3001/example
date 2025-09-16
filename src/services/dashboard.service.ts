import { apiClient } from '@/lib/api-client';
import { 
  TeacherDashboardResponse,
  StudentDashboardResponse,
  UserStatsResponse 
} from '@/types/api';

export class DashboardService {
  /**
   * Get admin dashboard data
   */
  static async getAdminDashboard(): Promise<UserStatsResponse> {
    return apiClient.get<UserStatsResponse>('/api/admin/dashboard');
  }

  /**
   * Get teacher dashboard data
   */
  static async getTeacherDashboard(): Promise<TeacherDashboardResponse> {
    return apiClient.get<TeacherDashboardResponse>('/api/teacher/dashboard');
  }

  /**
   * Get student dashboard data
   */
  static async getStudentDashboard(): Promise<StudentDashboardResponse> {
    return apiClient.get<StudentDashboardResponse>('/api/student/dashboard');
  }
}

export default DashboardService;
