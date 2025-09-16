/**
 * Application Configuration Constants
 */
export const APP_CONFIG = {
  // App info
  NAME: 'CScore',
  FULL_NAME: 'CScore - Hệ thống chấm điểm tự động',
  DESCRIPTION: 'Hệ thống chấm điểm tự động CScore - Đổi mới tư duy, làm giàu thêm tri thức',
  VERSION: '1.0.0',

  // API Configuration
  API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8086',
  API_TIMEOUT: 30000, // 30 seconds

  // Pagination
  DEFAULT_PAGE_SIZE: 10,
  MAX_PAGE_SIZE: 100,

  // File upload
  MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
  ALLOWED_FILE_TYPES: ['.csv', '.xlsx', '.xls'],

  // Local storage keys
  STORAGE_KEYS: {
    TOKEN: 'token',
    USER: 'user',
    THEME: 'theme',
    LANGUAGE: 'language',
  },

  // Routes
  ROUTES: {
    HOME: '/',
    LOGIN: '/login',
    ADMIN: '/admin',
    TEACHER: '/teacher',
    STUDENT: '/student',
    UNAUTHORIZED: '/unauthorized',
  },
} as const;

/**
 * UI Constants
 */
export const UI_CONFIG = {
  // Animation durations (ms)
  ANIMATION: {
    FAST: 150,
    NORMAL: 300,
    SLOW: 500,
  },

  // Breakpoints (matching Tailwind)
  BREAKPOINTS: {
    SM: 640,
    MD: 768,
    LG: 1024,
    XL: 1280,
    '2XL': 1536,
  },

  // Z-index levels
  Z_INDEX: {
    DROPDOWN: 1000,
    STICKY: 1020,
    FIXED: 1030,
    MODAL_BACKDROP: 1040,
    MODAL: 1050,
    POPOVER: 1060,
    TOOLTIP: 1070,
  },
} as const;