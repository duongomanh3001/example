'use client';

import React from 'react';
import { NotificationSettings } from '@/components/common/NotificationSettings';
import { useRoleAccess } from '@/hooks/useRoleAccess';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function SettingsPage() {
  const { isAuthenticated } = useRoleAccess();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500">Đang chuyển hướng...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-4xl px-4 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Cài đặt</h1>
          <p className="text-gray-600 mt-2">
            Quản lý các tùy chọn và cài đặt cá nhân của bạn
          </p>
        </div>

        <NotificationSettings />
      </div>
    </div>
  );
}