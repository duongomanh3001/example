import { apiClient } from '@/lib/api-client';
import { 
  Notification, 
  NotificationResponse, 
  NotificationStats, 
  NotificationPreferences,
  CreateNotificationRequest,
  MarkNotificationRequest,
  NotificationFilters
} from '@/types/notification';

export class NotificationService {
  private static readonly BASE_URL = '/notifications';

  // Lấy danh sách thông báo của user hiện tại
  static async getNotifications(filters?: NotificationFilters): Promise<NotificationResponse> {
    const params = new URLSearchParams();
    
    if (filters?.isRead !== undefined) {
      params.append('isRead', filters.isRead.toString());
    }
    if (filters?.type) {
      params.append('type', filters.type);
    }
    if (filters?.category) {
      params.append('category', filters.category);
    }
    if (filters?.dateFrom) {
      params.append('dateFrom', filters.dateFrom);
    }
    if (filters?.dateTo) {
      params.append('dateTo', filters.dateTo);
    }
    if (filters?.page) {
      params.append('page', filters.page.toString());
    }
    if (filters?.size) {
      params.append('size', filters.size.toString());
    }

    const queryString = params.toString();
    const url = queryString ? `${this.BASE_URL}?${queryString}` : this.BASE_URL;
    
    return apiClient.get<NotificationResponse>(url);
  }

  // Lấy thống kê thông báo
  static async getNotificationStats(): Promise<NotificationStats> {
    return apiClient.get<NotificationStats>(`${this.BASE_URL}/stats`);
  }

  // Lấy số lượng thông báo chưa đọc
  static async getUnreadCount(): Promise<{ count: number }> {
    return apiClient.get<{ count: number }>(`${this.BASE_URL}/unread-count`);
  }

  // Đánh dấu thông báo đã đọc/chưa đọc
  static async markNotifications(request: MarkNotificationRequest): Promise<void> {
    return apiClient.put<void>(`${this.BASE_URL}/mark`, request);
  }

  // Đánh dấu tất cả thông báo là đã đọc
  static async markAllAsRead(): Promise<void> {
    return apiClient.put<void>(`${this.BASE_URL}/mark-all-read`);
  }

  // Xóa thông báo
  static async deleteNotification(notificationId: number): Promise<void> {
    return apiClient.delete<void>(`${this.BASE_URL}/${notificationId}`);
  }

  // Xóa nhiều thông báo
  static async deleteNotifications(notificationIds: number[]): Promise<void> {
    return apiClient.request<void>(`${this.BASE_URL}/bulk`, {
      method: 'DELETE',
      body: JSON.stringify({ notificationIds }),
    });
  }

  // Lấy cài đặt thông báo
  static async getNotificationPreferences(): Promise<NotificationPreferences> {
    return apiClient.get<NotificationPreferences>(`${this.BASE_URL}/preferences`);
  }

  // Cập nhật cài đặt thông báo
  static async updateNotificationPreferences(
    preferences: Partial<NotificationPreferences>
  ): Promise<NotificationPreferences> {
    return apiClient.put<NotificationPreferences>(`${this.BASE_URL}/preferences`, preferences);
  }

  // Tạo thông báo (dành cho admin/teacher)
  static async createNotification(request: CreateNotificationRequest): Promise<Notification> {
    return apiClient.post<Notification>(`${this.BASE_URL}`, request);
  }

  // Gửi thông báo đến tất cả sinh viên trong khóa học
  static async sendNotificationToCourse(
    courseId: number, 
    request: CreateNotificationRequest
  ): Promise<void> {
    return apiClient.post<void>(`${this.BASE_URL}/course/${courseId}`, request);
  }

  // Gửi thông báo theo role
  static async sendNotificationByRole(
    role: 'STUDENT' | 'TEACHER' | 'ADMIN',
    request: CreateNotificationRequest
  ): Promise<void> {
    return apiClient.post<void>(`${this.BASE_URL}/role/${role}`, request);
  }

  // Lấy thông báo theo ID
  static async getNotificationById(notificationId: number): Promise<Notification> {
    return apiClient.get<Notification>(`${this.BASE_URL}/${notificationId}`);
  }

  // Subscribe to real-time notifications (WebSocket)
  static subscribeToNotifications(
    userId: number, 
    onNotification: (notification: Notification) => void
  ): WebSocket | null {
    // TODO: Implement WebSocket when backend WebSocket endpoint is ready
    console.log('WebSocket notifications will be implemented later', { userId });
    return null;
    
    // try {
    //   // Sử dụng WebSocket để nhận thông báo real-time
    //   const wsUrl = `${process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8086'}/ws/notifications?userId=${userId}`;
    //   const ws = new WebSocket(wsUrl);

    //   ws.onmessage = (event) => {
    //     try {
    //       const notification: Notification = JSON.parse(event.data);
    //       onNotification(notification);
    //     } catch (error) {
    //       console.error('Error parsing notification:', error);
    //     }
    //   };

    //   ws.onerror = (error) => {
    //     console.error('WebSocket error:', error);
    //   };

    //   return ws;
    // } catch (error) {
    //   console.error('Error setting up WebSocket:', error);
    //   return null;
    // }
  }
}

export const notificationService = NotificationService;