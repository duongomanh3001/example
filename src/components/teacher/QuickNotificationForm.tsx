'use client';

import React, { useState } from 'react';
import { Send, X, Users, BookOpen } from 'lucide-react';
import { NotificationService } from '@/services/notification.service';
import { 
  NotificationType, 
  NotificationCategory, 
  CreateNotificationRequest 
} from '@/types/notification';

interface QuickNotificationFormProps {
  isOpen: boolean;
  onClose: () => void;
  courseId?: number;
  courseName?: string;
  onSuccess?: () => void;
}

export const QuickNotificationForm: React.FC<QuickNotificationFormProps> = ({
  isOpen,
  onClose,
  courseId,
  courseName,
  onSuccess
}) => {
  const [formData, setFormData] = useState({
    title: '',
    message: '',
    type: NotificationType.INFO,
    category: NotificationCategory.ANNOUNCEMENT,
    sendToCourse: !!courseId,
    sendToAll: false
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    try {
      const request: CreateNotificationRequest = {
        title: formData.title,
        message: formData.message,
        type: formData.type,
        category: formData.category
      };

      if (formData.sendToCourse && courseId) {
        // Send to all students in course
        await NotificationService.sendNotificationToCourse(courseId, request);
      } else if (formData.sendToAll) {
        // Send to all students
        await NotificationService.sendNotificationByRole('STUDENT', request);
      } else {
        setError('Vui lòng chọn đối tượng nhận thông báo');
        return;
      }

      // Reset form
      setFormData({
        title: '',
        message: '',
        type: NotificationType.INFO,
        category: NotificationCategory.ANNOUNCEMENT,
        sendToCourse: !!courseId,
        sendToAll: false
      });

      onSuccess?.();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi gửi thông báo');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4 max-h-screen overflow-y-auto">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 bg-blue-50">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900 flex items-center">
              <Send className="mr-2" size={20} />
              Gửi thông báo
            </h3>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
              disabled={isSubmitting}
            >
              <X size={20} />
            </button>
          </div>
          {courseName && (
            <p className="text-sm text-gray-600 mt-1">
              Khóa học: {courseName}
            </p>
          )}
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
              {error}
            </div>
          )}

          {/* Title */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Tiêu đề <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
              placeholder="Nhập tiêu đề thông báo"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
              disabled={isSubmitting}
            />
          </div>

          {/* Message */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nội dung <span className="text-red-500">*</span>
            </label>
            <textarea
              value={formData.message}
              onChange={(e) => setFormData(prev => ({ ...prev, message: e.target.value }))}
              placeholder="Nhập nội dung thông báo"
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              required
              disabled={isSubmitting}
            />
          </div>

          {/* Type and Category */}
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Loại thông báo
              </label>
              <select
                value={formData.type}
                onChange={(e) => setFormData(prev => ({ 
                  ...prev, 
                  type: e.target.value as NotificationType 
                }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
                disabled={isSubmitting}
              >
                <option value={NotificationType.INFO}>Thông tin</option>
                <option value={NotificationType.SUCCESS}>Thành công</option>
                <option value={NotificationType.WARNING}>Cảnh báo</option>
                <option value={NotificationType.ERROR}>Lỗi</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Danh mục
              </label>
              <select
                value={formData.category}
                onChange={(e) => setFormData(prev => ({ 
                  ...prev, 
                  category: e.target.value as NotificationCategory 
                }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
                disabled={isSubmitting}
              >
                <option value={NotificationCategory.ANNOUNCEMENT}>Thông báo</option>
                <option value={NotificationCategory.ASSIGNMENT}>Bài tập</option>
                <option value={NotificationCategory.COURSE}>Khóa học</option>
                <option value={NotificationCategory.GRADING}>Chấm điểm</option>
                <option value={NotificationCategory.SYSTEM}>Hệ thống</option>
              </select>
            </div>
          </div>

          {/* Recipients */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Đối tượng nhận thông báo
            </label>
            <div className="space-y-2">
              {courseId && (
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="recipients"
                    checked={formData.sendToCourse}
                    onChange={(e) => setFormData(prev => ({
                      ...prev,
                      sendToCourse: e.target.checked,
                      sendToAll: false
                    }))}
                    className="mr-2 text-blue-600 focus:ring-blue-500"
                    disabled={isSubmitting}
                  />
                  <BookOpen size={16} className="mr-1" />
                  <span className="text-sm">
                    Tất cả sinh viên trong khóa học này
                  </span>
                </label>
              )}
              
              <label className="flex items-center">
                <input
                  type="radio"
                  name="recipients"
                  checked={formData.sendToAll}
                  onChange={(e) => setFormData(prev => ({
                    ...prev,
                    sendToAll: e.target.checked,
                    sendToCourse: false
                  }))}
                  className="mr-2 text-blue-600 focus:ring-blue-500"
                  disabled={isSubmitting}
                />
                <Users size={16} className="mr-1" />
                <span className="text-sm">
                  Tất cả sinh viên trong hệ thống
                </span>
              </label>
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50 focus:ring-2 focus:ring-blue-500"
              disabled={isSubmitting}
            >
              Hủy
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              disabled={isSubmitting || !formData.title || !formData.message}
            >
              {isSubmitting && (
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
              )}
              <Send size={16} className="mr-1" />
              {isSubmitting ? 'Đang gửi...' : 'Gửi thông báo'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};