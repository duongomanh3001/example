import { apiClient } from '@/lib/api-client';
import { 
  CourseResponse,
  DetailedCourseResponse,
  CreateCourseRequest,
  UpdateCourseRequest,
  CreateAssignmentRequest,
  DetailedAssignmentResponse,
  StudentResponse, 
  TeacherDashboardResponse,
  StudentDashboardResponse 
} from '@/types/api';
import { MessageResponse } from '@/types/auth';

export class CourseService {
  /**
   * Get available courses for enrollment
   */
  static async getAvailableCourses(): Promise<CourseResponse[]> {
    return apiClient.get<CourseResponse[]>('/api/courses/available');
  }

  /**
   * Get enrolled courses for current user
   */
  static async getEnrolledCourses(): Promise<CourseResponse[]> {
    return apiClient.get<CourseResponse[]>('/api/courses/enrolled');
  }

  /**
   * Get course details for student (uses enrolled courses approach)
   */
  static async getStudentCourseById(courseId: number): Promise<CourseResponse> {
    const enrolledCourses = await this.getEnrolledCourses();
    const course = enrolledCourses.find(c => c.id === courseId);
    if (!course) {
      throw new Error('Course not found or you are not enrolled');
    }
    return course;
  }

  /**
   * Enroll in a course
   */
  static async enrollInCourse(courseId: number): Promise<void> {
    return apiClient.post<void>(`/api/courses/${courseId}/enroll`);
  }

  /**
   * Unenroll from a course
   */
  static async unenrollFromCourse(courseId: number): Promise<void> {
    return apiClient.delete<void>(`/api/courses/${courseId}/unenroll`);
  }

  // ============ ADMIN COURSE MANAGEMENT ============
  
  /**
   * Get all courses (Admin only)
   */
  static async getAllCourses(): Promise<CourseResponse[]> {
    return apiClient.get<CourseResponse[]>('/api/admin/courses');
  }

  /**
   * Get courses with pagination (Admin only)
   */
  static async getAllCoursesPaginated(page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc') {
    return apiClient.get<any>(`/api/admin/courses/page?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
  }

  /**
   * Get course by ID (Admin only)
   */
  static async getCourseById(courseId: number): Promise<DetailedCourseResponse> {
    return apiClient.get<DetailedCourseResponse>(`/api/admin/courses/${courseId}`);
  }

  /**
   * Create course (Admin only)
   */
  static async createCourse(courseData: CreateCourseRequest): Promise<DetailedCourseResponse> {
    return apiClient.post<DetailedCourseResponse>('/api/admin/courses', courseData);
  }

  /**
   * Update course (Admin only)
   */
  static async updateCourse(courseId: number, courseData: UpdateCourseRequest): Promise<DetailedCourseResponse> {
    return apiClient.put<DetailedCourseResponse>(`/api/admin/courses/${courseId}`, courseData);
  }

  /**
   * Delete course (Admin only)
   */
  static async deleteCourse(courseId: number): Promise<MessageResponse> {
    return apiClient.delete<MessageResponse>(`/api/admin/courses/${courseId}`);
  }

  /**
   * Assign teacher to course (Admin only)
   */
  static async assignTeacherToCourse(courseId: number, teacherId: number): Promise<MessageResponse> {
    return apiClient.post<MessageResponse>(`/api/admin/courses/${courseId}/teachers/${teacherId}`);
  }

  /**
   * Enroll student to course (Admin only)
   */
  static async enrollStudentToCourse(courseId: number, studentId: number): Promise<MessageResponse> {
    return apiClient.post<MessageResponse>(`/api/admin/courses/${courseId}/students/${studentId}`);
  }

  /**
   * Remove student from course (Admin only)
   */
  static async removeStudentFromCourse(courseId: number, studentId: number): Promise<MessageResponse> {
    return apiClient.delete<MessageResponse>(`/api/admin/courses/${courseId}/students/${studentId}`);
  }

  /**
   * Get students in course (Admin only)
   */
  static async getStudentsInCourse(courseId: number): Promise<StudentResponse[]> {
    return apiClient.get<StudentResponse[]>(`/api/admin/courses/${courseId}/students`);
  }

  // ============ TEACHER COURSE MANAGEMENT ============
  
  /**
   * Get courses for teacher
   */
  static async getTeacherCourses(): Promise<CourseResponse[]> {
    return apiClient.get<CourseResponse[]>('/api/teacher/courses');
  }

  /**
   * Get teacher courses with pagination
   */
  static async getTeacherCoursesPaginated(page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc') {
    return apiClient.get<any>(`/api/teacher/courses/paginated?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
  }

  /**
   * Get course details for teacher
   */
  static async getTeacherCourseById(courseId: number): Promise<CourseResponse> {
    return apiClient.get<CourseResponse>(`/api/teacher/courses/${courseId}`);
  }

  /**
   * Get students in course for teacher
   */
  static async getTeacherCourseStudents(courseId: number): Promise<StudentResponse[]> {
    return apiClient.get<StudentResponse[]>(`/api/teacher/courses/${courseId}/students`);
  }

  /**
   * Get courses by teacher ID
   */
  static async getCoursesByTeacher(teacherId: number): Promise<CourseResponse[]> {
    return apiClient.get<CourseResponse[]>(`/api/teacher/teachers/${teacherId}/courses`);
  }
}

export default CourseService;
