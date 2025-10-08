export interface Notification {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  category: NotificationCategory;
  isRead: boolean;
  createdAt: string;
  updatedAt: string;
  userId: number;
  relatedEntityId?: number;
  relatedEntityType?: string;
  actionUrl?: string;
}

export enum NotificationType {
  INFO = 'INFO',
  SUCCESS = 'SUCCESS',
  WARNING = 'WARNING',
  ERROR = 'ERROR'
}

export enum NotificationCategory {
  ASSIGNMENT = 'ASSIGNMENT',
  COURSE = 'COURSE',
  SUBMISSION = 'SUBMISSION',
  GRADING = 'GRADING',
  SYSTEM = 'SYSTEM',
  ANNOUNCEMENT = 'ANNOUNCEMENT'
}

export interface CreateNotificationRequest {
  title: string;
  message: string;
  type: NotificationType;
  category: NotificationCategory;
  userId?: number;
  userIds?: number[];
  roleType?: 'STUDENT' | 'TEACHER' | 'ADMIN';
  courseId?: number;
  relatedEntityId?: number;
  relatedEntityType?: string;
  actionUrl?: string;
}

export interface NotificationPreferences {
  id: number;
  userId: number;
  enableEmail: boolean;
  enablePush: boolean;
  enableInApp: boolean;
  categories: {
    [key in NotificationCategory]: {
      email: boolean;
      push: boolean;
      inApp: boolean;
    }
  };
}

export interface NotificationResponse {
  notifications: Notification[];
  totalCount: number;
  unreadCount: number;
  currentPage: number;
  totalPages: number;
}

export interface NotificationStats {
  totalCount: number;
  unreadCount: number;
  todayCount: number;
  weekCount: number;
}

export interface MarkNotificationRequest {
  notificationIds: number[];
  isRead: boolean;
}

export interface NotificationFilters {
  isRead?: boolean;
  type?: NotificationType;
  category?: NotificationCategory;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}