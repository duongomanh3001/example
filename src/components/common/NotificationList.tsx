'use client';

import React, { useState, useEffect } from 'react';
import { 
  Bell, 
  Filter, 
  Search, 
  Check, 
  CheckCheck, 
  Trash2, 
  Eye,
  Calendar,
  Tag
} from 'lucide-react';
import { useNotifications } from '@/contexts/NotificationContext';
import { 
  Notification, 
  NotificationType, 
  NotificationCategory, 
  NotificationFilters 
} from '@/types/notification';
import { formatTimeAgo, formatNotificationDate } from '@/utils/notification';

interface NotificationListProps {
  className?: string;
}

export const NotificationList: React.FC<NotificationListProps> = ({ className }) => {
  const {
    notifications,
    stats,
    loading,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    fetchNotifications
  } = useNotifications();

  const [filters, setFilters] = useState<NotificationFilters>({
    page: 0,
    size: 20
  });
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedNotifications, setSelectedNotifications] = useState<number[]>([]);
  const [showFilters, setShowFilters] = useState(false);

  // Apply filters when they change
  useEffect(() => {
    fetchNotifications();
  }, [filters, fetchNotifications]);

  const getNotificationIcon = (type: NotificationType) => {
    switch (type) {
      case NotificationType.SUCCESS:
        return '✅';
      case NotificationType.WARNING:
        return '⚠️';
      case NotificationType.ERROR:
        return '❌';
      default:
        return '📝';
    }
  };

  const getCategoryColor = (category: NotificationCategory) => {
    switch (category) {
      case NotificationCategory.ASSIGNMENT:
        return 'bg-blue-100 text-blue-800';
      case NotificationCategory.COURSE:
        return 'bg-green-100 text-green-800';
      case NotificationCategory.SUBMISSION:
        return 'bg-yellow-100 text-yellow-800';
      case NotificationCategory.GRADING:
        return 'bg-purple-100 text-purple-800';
      case NotificationCategory.SYSTEM:
        return 'bg-red-100 text-red-800';
      case NotificationCategory.ANNOUNCEMENT:
        return 'bg-indigo-100 text-indigo-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getCategoryText = (category: NotificationCategory) => {
    switch (category) {
      case NotificationCategory.ASSIGNMENT:
        return 'Bài tập';
      case NotificationCategory.COURSE:
        return 'Khóa học';
      case NotificationCategory.SUBMISSION:
        return 'Nộp bài';
      case NotificationCategory.GRADING:
        return 'Chấm điểm';
      case NotificationCategory.SYSTEM:
        return 'Hệ thống';
      case NotificationCategory.ANNOUNCEMENT:
        return 'Thông báo';
      default:
        return category;
    }
  };

  const filteredNotifications = notifications.filter(notification => {
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      return (
        notification.title.toLowerCase().includes(searchLower) ||
        notification.message.toLowerCase().includes(searchLower)
      );
    }
    return true;
  });

  const handleSelectNotification = (notificationId: number, checked: boolean) => {
    if (checked) {
      setSelectedNotifications(prev => [...prev, notificationId]);
    } else {
      setSelectedNotifications(prev => prev.filter(id => id !== notificationId));
    }
  };

  const handleSelectAll = () => {
    const allIds = filteredNotifications.map(n => n.id);
    setSelectedNotifications(
      selectedNotifications.length === allIds.length ? [] : allIds
    );
  };

  const handleBulkMarkAsRead = async () => {
    if (selectedNotifications.length > 0) {
      await markAsRead(selectedNotifications);
      setSelectedNotifications([]);
    }
  };

  const handleBulkDelete = async () => {
    if (selectedNotifications.length > 0) {
      for (const id of selectedNotifications) {
        await deleteNotification(id);
      }
      setSelectedNotifications([]);
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    if (!notification.isRead) {
      await markAsRead([notification.id]);
    }
    
    if (notification.actionUrl) {
      window.location.href = notification.actionUrl;
    }
  };

  return (
    <div className={`bg-white rounded-lg shadow-sm border border-gray-200 ${className}`}>
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 flex items-center">
              <Bell className="mr-2" size={20} />
              Thông báo
            </h2>
            {stats && (
              <p className="text-sm text-gray-600 mt-1">
                Tổng: {stats.totalCount} • Chưa đọc: {stats.unreadCount}
              </p>
            )}
          </div>
          
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50 flex items-center space-x-1"
            >
              <Filter size={16} />
              <span>Lọc</span>
            </button>
            
            {(stats?.unreadCount || 0) > 0 && (
              <button
                onClick={markAllAsRead}
                className="px-3 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 flex items-center space-x-1"
              >
                <CheckCheck size={16} />
                <span>Đánh dấu tất cả</span>
              </button>
            )}
          </div>
        </div>

        {/* Search and Filters */}
        <div className="mt-4 space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={16} />
            <input
              type="text"
              placeholder="Tìm kiếm thông báo..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {showFilters && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 p-4 bg-gray-50 rounded-md">
              <select
                value={filters.isRead?.toString() || ''}
                onChange={(e) => setFilters(prev => ({
                  ...prev,
                  isRead: e.target.value === '' ? undefined : e.target.value === 'true'
                }))}
                className="border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Tất cả trạng thái</option>
                <option value="false">Chưa đọc</option>
                <option value="true">Đã đọc</option>
              </select>

              <select
                value={filters.type || ''}
                onChange={(e) => setFilters(prev => ({
                  ...prev,
                  type: e.target.value as NotificationType || undefined
                }))}
                className="border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Tất cả loại</option>
                <option value={NotificationType.INFO}>Thông tin</option>
                <option value={NotificationType.SUCCESS}>Thành công</option>
                <option value={NotificationType.WARNING}>Cảnh báo</option>
                <option value={NotificationType.ERROR}>Lỗi</option>
              </select>

              <select
                value={filters.category || ''}
                onChange={(e) => setFilters(prev => ({
                  ...prev,
                  category: e.target.value as NotificationCategory || undefined
                }))}
                className="border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Tất cả danh mục</option>
                <option value={NotificationCategory.ASSIGNMENT}>Bài tập</option>
                <option value={NotificationCategory.COURSE}>Khóa học</option>
                <option value={NotificationCategory.SUBMISSION}>Nộp bài</option>
                <option value={NotificationCategory.GRADING}>Chấm điểm</option>
                <option value={NotificationCategory.SYSTEM}>Hệ thống</option>
                <option value={NotificationCategory.ANNOUNCEMENT}>Thông báo</option>
              </select>

              <button
                onClick={() => setFilters({ page: 0, size: 20 })}
                className="px-4 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
              >
                Xóa bộ lọc
              </button>
            </div>
          )}
        </div>

        {/* Bulk Actions */}
        {selectedNotifications.length > 0 && (
          <div className="mt-4 flex items-center justify-between p-3 bg-blue-50 rounded-md">
            <span className="text-sm text-blue-700">
              Đã chọn {selectedNotifications.length} thông báo
            </span>
            <div className="flex items-center space-x-2">
              <button
                onClick={handleBulkMarkAsRead}
                className="px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center space-x-1"
              >
                <Check size={14} />
                <span>Đánh dấu đã đọc</span>
              </button>
              <button
                onClick={handleBulkDelete}
                className="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700 flex items-center space-x-1"
              >
                <Trash2 size={14} />
                <span>Xóa</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Notifications List */}
      <div className="max-h-96 overflow-y-auto">
        {loading ? (
          <div className="px-6 py-8 text-center text-gray-500">
            Đang tải thông báo...
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div className="px-6 py-8 text-center text-gray-500">
            <Bell size={48} className="mx-auto mb-4 text-gray-300" />
            <p className="text-lg font-medium">Không có thông báo nào</p>
            <p className="text-sm">
              {searchTerm ? 'Không tìm thấy thông báo phù hợp' : 'Bạn chưa có thông báo nào'}
            </p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {/* Select All */}
            <div className="px-6 py-3 bg-gray-50 flex items-center space-x-3">
              <input
                type="checkbox"
                checked={selectedNotifications.length === filteredNotifications.length}
                onChange={handleSelectAll}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700">Chọn tất cả</span>
            </div>

            {filteredNotifications.map((notification) => (
              <div
                key={notification.id}
                className={`px-6 py-4 hover:bg-gray-50 cursor-pointer transition-colors ${
                  !notification.isRead ? 'bg-blue-25 border-l-4 border-l-blue-500' : ''
                }`}
                onClick={() => handleNotificationClick(notification)}
              >
                <div className="flex items-start space-x-3">
                  <input
                    type="checkbox"
                    checked={selectedNotifications.includes(notification.id)}
                    onChange={(e) => {
                      e.stopPropagation();
                      handleSelectNotification(notification.id, e.target.checked);
                    }}
                    className="mt-1 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  
                  <span className="text-xl mt-1">
                    {getNotificationIcon(notification.type)}
                  </span>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <h4 className={`text-sm ${!notification.isRead ? 'font-semibold text-gray-900' : 'text-gray-800'}`}>
                          {notification.title}
                        </h4>
                        <p className="text-sm text-gray-600 mt-1 line-clamp-2">
                          {notification.message}
                        </p>
                        
                        <div className="flex items-center space-x-4 mt-2">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getCategoryColor(notification.category)}`}>
                            <Tag size={12} className="mr-1" />
                            {getCategoryText(notification.category)}
                          </span>
                          
                          <div className="flex items-center text-xs text-gray-500 space-x-1">
                            <Calendar size={12} />
                            <span>{formatNotificationDate(notification.createdAt)}</span>
                          </div>
                          
                          <span className="text-xs text-gray-400">
                            {formatTimeAgo(notification.createdAt)}
                          </span>
                        </div>
                      </div>
                      
                      <div className="flex items-center space-x-1 ml-4">
                        {!notification.isRead && (
                          <span className="w-2 h-2 bg-blue-500 rounded-full"></span>
                        )}
                        
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteNotification(notification.id);
                          }}
                          className="text-gray-400 hover:text-red-600 p-1"
                          title="Xóa thông báo"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Pagination placeholder */}
      {filteredNotifications.length > 0 && (
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-700">
              Hiển thị {filteredNotifications.length} thông báo
            </span>
            {notifications.length > 20 && (
              <button className="px-4 py-2 text-sm text-blue-600 hover:text-blue-800">
                Tải thêm
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};