"use client";

import Link from 'next/link';
import { StudentAssignmentResponse } from '@/types/api';

interface UpcomingAssignmentsProps {
  assignments: StudentAssignmentResponse[];
  viewType: 'teacher' | 'student';
  maxItems?: number;
}

export default function UpcomingAssignments({ 
  assignments, 
  viewType,
  maxItems = 5 
}: UpcomingAssignmentsProps) {
  // Filter and sort assignments by deadline
  const upcomingAssignments = assignments
    .filter(a => {
      if (!a.endTime) return false;
      const deadline = new Date(a.endTime);
      const now = new Date();
      // Only show assignments that haven't passed deadline yet
      return deadline > now;
    })
    .sort((a, b) => {
      const dateA = new Date(a.endTime!);
      const dateB = new Date(b.endTime!);
      return dateA.getTime() - dateB.getTime();
    })
    .slice(0, maxItems);

  if (upcomingAssignments.length === 0) {
    return (
      <div className="bg-white rounded-lg border border-slate-200 p-6 text-center">
        <div className="text-slate-400 mb-2">
          <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <p className="text-slate-600 font-medium">Không có bài tập sắp tới hạn</p>
        <p className="text-slate-500 text-sm mt-1">Bạn đã hoàn thành tất cả!</p>
      </div>
    );
  }

  const getTimeUntilDeadline = (endTime: string) => {
    const now = new Date();
    const deadline = new Date(endTime);
    const diffMs = deadline.getTime() - now.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));

    if (diffDays > 7) {
      return {
        text: `${diffDays} ngày`,
        color: 'text-slate-600',
        bgColor: 'bg-slate-100'
      };
    } else if (diffDays > 2) {
      return {
        text: `${diffDays} ngày`,
        color: 'text-orange-600',
        bgColor: 'bg-orange-100'
      };
    } else if (diffDays > 0) {
      return {
        text: diffDays === 1 ? 'Ngày mai' : `${diffDays} ngày`,
        color: 'text-red-600',
        bgColor: 'bg-red-100'
      };
    } else if (diffHours > 0) {
      return {
        text: `${diffHours} giờ`,
        color: 'text-red-700',
        bgColor: 'bg-red-200'
      };
    } else {
      return {
        text: 'Sắp hết hạn',
        color: 'text-red-800',
        bgColor: 'bg-red-300'
      };
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'EXERCISE': return 'bg-blue-100 text-blue-800';
      case 'EXAM': return 'bg-red-100 text-red-800';
      case 'PROJECT': return 'bg-orange-100 text-orange-800';
      case 'QUIZ': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getTypeName = (type: string) => {
    switch (type) {
      case 'EXERCISE': return 'Bài tập';
      case 'EXAM': return 'Bài thi';
      case 'PROJECT': return 'Dự án';
      case 'QUIZ': return 'Quiz';
      default: return type;
    }
  };

  const formatDeadline = (endTime: string) => {
    const date = new Date(endTime);
    return date.toLocaleDateString('vi-VN', { 
      weekday: 'short',
      month: 'numeric',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="bg-white rounded-lg border border-slate-200 p-4">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-slate-900">Sắp tới hạn</h3>
        <span className="text-xs text-slate-500">{upcomingAssignments.length} bài tập</span>
      </div>

      <div className="space-y-3">
        {upcomingAssignments.map((assignment) => {
          const timeInfo = getTimeUntilDeadline(assignment.endTime!);
          
          return (
            <Link
              key={assignment.id}
              href={viewType === 'teacher' 
                ? `/teacher/course/${assignment.courseId}/assignment/${assignment.id}` 
                : `/student/assignment/${assignment.id}`
              }
              className="block p-3 rounded-lg border border-slate-200 hover:border-slate-300 hover:shadow-sm transition-all"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${getTypeColor(assignment.type)}`}>
                      {getTypeName(assignment.type)}
                    </span>
                    {assignment.isSubmitted && (
                      <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-green-100 text-green-800">
                        ✓ Đã nộp
                      </span>
                    )}
                  </div>
                  
                  <h4 className="font-medium text-slate-900 text-sm mb-1 truncate">
                    {assignment.title}
                  </h4>
                  
                  <p className="text-xs text-slate-600 mb-2">
                    {assignment.courseName}
                  </p>

                  <div className="flex items-center gap-3 text-xs text-slate-500">
                    <span className="flex items-center gap-1">
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      {formatDeadline(assignment.endTime!)}
                    </span>
                    <span>•</span>
                    <span>{assignment.maxScore} điểm</span>
                  </div>
                </div>

                <div className={`
                  ${timeInfo.bgColor} ${timeInfo.color}
                  px-3 py-1 rounded-md text-xs font-semibold whitespace-nowrap
                `}>
                  {timeInfo.text}
                </div>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
