'use client';

import React, { useState, useEffect } from 'react';
import { Settings, Bell, Mail, Smartphone, Monitor, Save } from 'lucide-react';
import { NotificationService } from '@/services/notification.service';
import { 
  NotificationPreferences, 
  NotificationCategory 
} from '@/types/notification';

interface NotificationSettingsProps {
  className?: string;
}

export const NotificationSettings: React.FC<NotificationSettingsProps> = ({ className }) => {
  const [preferences, setPreferences] = useState<NotificationPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadPreferences();
  }, []);

  const loadPreferences = async () => {
    try {
      setLoading(true);
      const prefs = await NotificationService.getNotificationPreferences();
      setPreferences(prefs);
    } catch (err) {
      setError('Lỗi khi tải cài đặt thông báo');
      console.error('Error loading notification preferences:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!preferences) return;

    try {
      setSaving(true);
      setError('');
      setSuccess('');
      
      await NotificationService.updateNotificationPreferences(preferences);
      setSuccess('Đã lưu cài đặt thành công');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi lưu cài đặt');
    } finally {
      setSaving(false);
    }
  };

  const handleGlobalToggle = (type: 'enableEmail' | 'enablePush' | 'enableInApp') => {
    if (!preferences) return;
    
    setPreferences({
      ...preferences,
      [type]: !preferences[type]
    });
  };

  const handleCategoryToggle = (
    category: NotificationCategory,
    type: 'email' | 'push' | 'inApp'
  ) => {
    if (!preferences) return;

    setPreferences({
      ...preferences,
      categories: {
        ...preferences.categories,
        [category]: {
          ...preferences.categories[category],
          [type]: !preferences.categories[category][type]
        }
      }
    });
  };

  const getCategoryName = (category: NotificationCategory): string => {
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

  const getCategoryDescription = (category: NotificationCategory): string => {
    switch (category) {
      case NotificationCategory.ASSIGNMENT:
        return 'Thông báo về bài tập mới, thay đổi bài tập';
      case NotificationCategory.COURSE:
        return 'Thông báo về khóa học, thay đổi lịch học';
      case NotificationCategory.SUBMISSION:
        return 'Thông báo về trạng thái nộp bài';
      case NotificationCategory.GRADING:
        return 'Thông báo về kết quả chấm điểm';
      case NotificationCategory.SYSTEM:
        return 'Thông báo hệ thống, bảo trì';
      case NotificationCategory.ANNOUNCEMENT:
        return 'Thông báo chung, tin tức';
      default:
        return '';
    }
  };

  if (loading) {
    return (
      <div className={`bg-white rounded-lg shadow-sm border border-gray-200 p-6 ${className}`}>
        <div className="flex items-center space-x-2 mb-6">
          <Settings size={20} />
          <h2 className="text-xl font-semibold text-gray-900">Cài đặt thông báo</h2>
        </div>
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="text-gray-500 mt-2">Đang tải cài đặt...</p>
        </div>
      </div>
    );
  }

  if (!preferences) {
    return (
      <div className={`bg-white rounded-lg shadow-sm border border-gray-200 p-6 ${className}`}>
        <div className="flex items-center space-x-2 mb-6">
          <Settings size={20} />
          <h2 className="text-xl font-semibold text-gray-900">Cài đặt thông báo</h2>
        </div>
        <div className="text-center py-8 text-red-500">
          Không thể tải cài đặt thông báo
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-lg shadow-sm border border-gray-200 ${className}`}>
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Settings size={20} />
            <h2 className="text-xl font-semibold text-gray-900">Cài đặt thông báo</h2>
          </div>
          
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
          >
            {saving ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
            ) : (
              <Save size={16} />
            )}
            <span>{saving ? 'Đang lưu...' : 'Lưu cài đặt'}</span>
          </button>
        </div>

        {error && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
            {error}
          </div>
        )}
        
        {success && (
          <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-md text-green-700 text-sm">
            {success}
          </div>
        )}
      </div>

      <div className="p-6">
        {/* Global Settings */}
        <div className="mb-8">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Cài đặt chung</h3>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <Bell size={20} className="text-gray-500" />
                <div>
                  <p className="font-medium text-gray-900">Thông báo trong ứng dụng</p>
                  <p className="text-sm text-gray-500">Hiển thị thông báo khi sử dụng ứng dụng</p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={preferences.enableInApp}
                  onChange={() => handleGlobalToggle('enableInApp')}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <Mail size={20} className="text-gray-500" />
                <div>
                  <p className="font-medium text-gray-900">Thông báo qua email</p>
                  <p className="text-sm text-gray-500">Gửi thông báo đến địa chỉ email của bạn</p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={preferences.enableEmail}
                  onChange={() => handleGlobalToggle('enableEmail')}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <Smartphone size={20} className="text-gray-500" />
                <div>
                  <p className="font-medium text-gray-900">Thông báo đẩy (Push)</p>
                  <p className="text-sm text-gray-500">Thông báo trên thiết bị di động</p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={preferences.enablePush}
                  onChange={() => handleGlobalToggle('enablePush')}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
          </div>
        </div>

        {/* Category Settings */}
        <div>
          <h3 className="text-lg font-medium text-gray-900 mb-4">Cài đặt theo danh mục</h3>
          
          <div className="space-y-6">
            {Object.entries(preferences.categories).map(([category, settings]) => {
              const categoryEnum = category as NotificationCategory;
              return (
                <div key={category} className="border border-gray-200 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <h4 className="font-medium text-gray-900">
                        {getCategoryName(categoryEnum)}
                      </h4>
                      <p className="text-sm text-gray-500">
                        {getCategoryDescription(categoryEnum)}
                      </p>
                    </div>
                  </div>
                  
                  <div className="grid grid-cols-3 gap-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <Monitor size={16} className="text-gray-400" />
                        <span className="text-sm">Trong app</span>
                      </div>
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input
                          type="checkbox"
                          checked={settings.inApp}
                          onChange={() => handleCategoryToggle(categoryEnum, 'inApp')}
                          disabled={!preferences.enableInApp}
                          className="sr-only peer"
                        />
                        <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-blue-300 rounded-full peer peer-disabled:opacity-50 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
                      </label>
                    </div>

                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <Mail size={16} className="text-gray-400" />
                        <span className="text-sm">Email</span>
                      </div>
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input
                          type="checkbox"
                          checked={settings.email}
                          onChange={() => handleCategoryToggle(categoryEnum, 'email')}
                          disabled={!preferences.enableEmail}
                          className="sr-only peer"
                        />
                        <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-blue-300 rounded-full peer peer-disabled:opacity-50 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
                      </label>
                    </div>

                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <Smartphone size={16} className="text-gray-400" />
                        <span className="text-sm">Push</span>
                      </div>
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input
                          type="checkbox"
                          checked={settings.push}
                          onChange={() => handleCategoryToggle(categoryEnum, 'push')}
                          disabled={!preferences.enablePush}
                          className="sr-only peer"
                        />
                        <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-blue-300 rounded-full peer peer-disabled:opacity-50 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
                      </label>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};