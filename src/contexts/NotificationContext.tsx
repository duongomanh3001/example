'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { Notification, NotificationStats } from '@/types/notification';
import { NotificationService } from '@/services/notification.service';
import { useAuth } from './AuthContext';

interface NotificationContextType {
  notifications: Notification[];
  stats: NotificationStats | null;
  loading: boolean;
  error: string | null;
  unreadCount: number;
  
  // Actions
  fetchNotifications: () => Promise<void>;
  fetchStats: () => Promise<void>;
  markAsRead: (notificationIds: number[]) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  deleteNotification: (notificationId: number) => Promise<void>;
  addNotification: (notification: Notification) => void;
  clearError: () => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

interface NotificationProviderProps {
  children: React.ReactNode;
}

export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
  const { state } = useAuth();
  const user = state.user;
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [stats, setStats] = useState<NotificationStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);

  // WebSocket connection
  const [ws, setWs] = useState<WebSocket | null>(null);

  const fetchNotifications = useCallback(async () => {
    if (!user) return;
    
    try {
      setLoading(true);
      setError(null);
      const response = await NotificationService.getNotifications({ 
        page: 0, 
        size: 50 
      });
      setNotifications(response.notifications);
      setUnreadCount(response.unreadCount);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi tải thông báo');
      console.error('Error fetching notifications:', err);
    } finally {
      setLoading(false);
    }
  }, [user]);

  const fetchStats = useCallback(async () => {
    if (!user) return;
    
    try {
      const notificationStats = await NotificationService.getNotificationStats();
      setStats(notificationStats);
      setUnreadCount(notificationStats.unreadCount);
    } catch (err) {
      console.error('Error fetching notification stats:', err);
    }
  }, [user]);

  const markAsRead = useCallback(async (notificationIds: number[]) => {
    try {
      await NotificationService.markNotifications({
        notificationIds,
        isRead: true
      });
      
      // Update local state
      setNotifications(prev => 
        prev.map(notification => 
          notificationIds.includes(notification.id)
            ? { ...notification, isRead: true }
            : notification
        )
      );
      
      // Update unread count
      const newUnreadCount = notifications.filter(n => 
        !notificationIds.includes(n.id) && !n.isRead
      ).length;
      setUnreadCount(newUnreadCount);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi đánh dấu đã đọc');
      console.error('Error marking notifications as read:', err);
    }
  }, [notifications]);

  const markAllAsRead = useCallback(async () => {
    try {
      await NotificationService.markAllAsRead();
      
      // Update local state
      setNotifications(prev => 
        prev.map(notification => ({ ...notification, isRead: true }))
      );
      setUnreadCount(0);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi đánh dấu tất cả đã đọc');
      console.error('Error marking all notifications as read:', err);
    }
  }, []);

  const deleteNotification = useCallback(async (notificationId: number) => {
    try {
      await NotificationService.deleteNotification(notificationId);
      
      // Update local state
      const deletedNotification = notifications.find(n => n.id === notificationId);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
      
      // Update unread count if deleted notification was unread
      if (deletedNotification && !deletedNotification.isRead) {
        setUnreadCount(prev => prev - 1);
      }
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi xóa thông báo');
      console.error('Error deleting notification:', err);
    }
  }, [notifications]);

  const addNotification = useCallback((notification: Notification) => {
    setNotifications(prev => [notification, ...prev]);
    if (!notification.isRead) {
      setUnreadCount(prev => prev + 1);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  // Setup WebSocket connection for real-time notifications
  useEffect(() => {
    if (!user?.id) return;

    // TODO: Implement WebSocket when backend WebSocket endpoint is ready
    // const websocket = NotificationService.subscribeToNotifications(
    //   user.id,
    //   (notification) => {
    //     addNotification(notification);
    //   }
    // );

    // if (websocket) {
    //   setWs(websocket);
    // }

    // return () => {
    //   if (websocket) {
    //     websocket.close();
    //   }
    // };
  }, [user?.id, addNotification]);

  // Initial data fetch
  useEffect(() => {
    if (user) {
      fetchNotifications();
      fetchStats();
    }
  }, [user, fetchNotifications, fetchStats]);

  // Cleanup WebSocket on unmount
  useEffect(() => {
    return () => {
      if (ws) {
        ws.close();
      }
    };
  }, [ws]);

  const value: NotificationContextType = {
    notifications,
    stats,
    loading,
    error,
    unreadCount,
    fetchNotifications,
    fetchStats,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    addNotification,
    clearError
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = (): NotificationContextType => {
  const context = useContext(NotificationContext);
  if (context === undefined) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};