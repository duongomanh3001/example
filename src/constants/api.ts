/**
 * API Endpoints Constants
 */
export const API_ENDPOINTS = {
  // Auth endpoints
  AUTH: {
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    PROFILE: '/api/auth/profile',
  },

  // Admin endpoints
  ADMIN: {
    USERS: '/api/admin/users',
    COURSES: '/api/admin/courses',
    COURSES_PAGINATED: '/api/admin/courses/page',
    UPLOAD_CSV: '/api/admin/upload-csv',
  },

  // Teacher endpoints
  TEACHER: {
    COURSES: '/api/teacher/courses',
    ASSIGNMENTS: '/api/teacher/assignments',
    ASSIGNMENTS_PAGINATED: '/api/teacher/assignments/paginated',
  },

  // Student endpoints
  STUDENT: {
    COURSES: '/api/student/courses',
    ENROLLED_COURSES: '/api/student/courses/enrolled',
    ASSIGNMENTS: '/api/student/assignments',
  },

  // Common endpoints
  HEALTH: '/api/health',
  DASHBOARD: '/api/dashboard',
} as const;

/**
 * HTTP Status Codes
 */
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500,
} as const;