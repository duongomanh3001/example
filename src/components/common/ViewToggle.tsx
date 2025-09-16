"use client";

import { useState } from 'react';
import { Role } from '@/types/auth';
import { useAuth } from '@/contexts/AuthContext';

interface ViewToggleProps {
  currentView: 'teacher' | 'student';
  onViewChange: (view: 'teacher' | 'student') => void;
}

export default function ViewToggle({ currentView, onViewChange }: ViewToggleProps) {
  const { hasRole } = useAuth();

  // Only show toggle if user has teacher role
  if (!hasRole(Role.TEACHER)) {
    return null;
  }

  return (
    <div className="flex items-center bg-white border border-slate-200 rounded-lg p-1 shadow-sm">
      <button
        onClick={() => onViewChange('teacher')}
        className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
          currentView === 'teacher'
            ? 'bg-[#ff6a00] text-white shadow-sm'
            : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
        }`}
      >
        Chế độ giáo viên
      </button>
      <button
        onClick={() => onViewChange('student')}
        className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
          currentView === 'student'
            ? 'bg-[#ff6a00] text-white shadow-sm'
            : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
        }`}
      >
        Chế độ sinh viên
      </button>
    </div>
  );
}
