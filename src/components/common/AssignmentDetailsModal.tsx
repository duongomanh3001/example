"use client";

import Link from 'next/link';
import { StudentAssignmentResponse } from '@/types/api';

interface AssignmentDetailsModalProps {
  date: Date;
  assignments: StudentAssignmentResponse[];
  onClose: () => void;
  viewType: 'teacher' | 'student';
}

export default function AssignmentDetailsModal({ 
  date, 
  assignments, 
  onClose,
  viewType 
}: AssignmentDetailsModalProps) {
  const formatDate = (iso: string) => {
    const d = new Date(iso);
    return d.toLocaleDateString('vi-VN', { 
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
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

  const getStatusBadge = (assignment: StudentAssignmentResponse) => {
    if (assignment.isSubmitted) {
      return (
        <span className="px-2 py-1 text-xs rounded-full bg-green-100 text-green-800 font-medium">
          ✓ Đã nộp
        </span>
      );
    }
    
    if (assignment.endTime) {
      const now = new Date();
      const deadline = new Date(assignment.endTime);
      const daysUntilDeadline = Math.floor((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
      
      if (daysUntilDeadline < 0) {
        return (
          <span className="px-2 py-1 text-xs rounded-full bg-gray-200 text-gray-700 font-medium">
            Quá hạn
          </span>
        );
      } else if (daysUntilDeadline <= 2) {
        return (
          <span className="px-2 py-1 text-xs rounded-full bg-red-100 text-red-800 font-medium">
            ⚠ Sắp đến hạn
          </span>
        );
      }
    }
    
    return (
      <span className="px-2 py-1 text-xs rounded-full bg-orange-100 text-orange-800 font-medium">
        Đang tiến hành
      </span>
    );
  };

  return (
    <div 
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <div 
        className="bg-white rounded-lg max-w-2xl w-full max-h-[80vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 bg-white border-b border-slate-200 p-6">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-xl font-bold text-slate-900">
                {date.toLocaleDateString('vi-VN', { 
                  weekday: 'long',
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric'
                })}
              </h2>
              <p className="text-sm text-slate-600 mt-1">
                {assignments.length} bài tập đến hạn
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-slate-100 rounded-md transition-colors"
            >
              <svg className="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* Assignments List */}
        <div className="p-6 space-y-4">
          {assignments.map((assignment) => (
            <Link
              key={assignment.id}
              href={viewType === 'teacher' 
                ? `/teacher/course/${assignment.courseId}/assignment/${assignment.id}` 
                : `/student/assignment/${assignment.id}`
              }
              className="block bg-white border border-slate-200 rounded-lg p-4 hover:shadow-md hover:border-slate-300 transition-all"
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-slate-900 mb-1">
                    {assignment.title}
                  </h3>
                  <p className="text-sm text-slate-600">
                    {assignment.courseName}
                  </p>
                </div>
                <div className="ml-4">
                  {getStatusBadge(assignment)}
                </div>
              </div>

              {assignment.description && (
                <p className="text-sm text-slate-600 mb-3 line-clamp-2">
                  {assignment.description}
                </p>
              )}

              <div className="flex flex-wrap items-center gap-2 mb-3">
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${getTypeColor(assignment.type)}`}>
                  {getTypeName(assignment.type)}
                </span>
                <span className="text-xs px-2 py-1 bg-slate-100 text-slate-700 rounded-full font-medium">
                  {assignment.maxScore} điểm
                </span>
                <span className="text-xs px-2 py-1 bg-slate-100 text-slate-700 rounded-full font-medium">
                  {assignment.timeLimit} phút
                </span>
                {assignment.totalQuestions > 0 && (
                  <span className="text-xs px-2 py-1 bg-slate-100 text-slate-700 rounded-full font-medium">
                    {assignment.totalQuestions} câu hỏi
                  </span>
                )}
              </div>

              <div className="flex items-center justify-between text-xs text-slate-500 pt-3 border-t border-slate-100">
                <div>
                  {assignment.endTime && (
                    <div className="flex items-center gap-1">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span>Hạn nộp: {formatDate(assignment.endTime)}</span>
                    </div>
                  )}
                </div>
                {assignment.isSubmitted && assignment.submissionTime && (
                  <div className="text-green-600 font-medium">
                    Đã nộp {new Date(assignment.submissionTime).toLocaleDateString('vi-VN')}
                  </div>
                )}
              </div>

              {assignment.currentScore !== undefined && (
                <div className="mt-2 pt-2 border-t border-slate-100">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-slate-600">Điểm hiện tại:</span>
                    <span className="text-sm font-semibold text-slate-900">
                      {assignment.currentScore} / {assignment.maxScore}
                    </span>
                  </div>
                </div>
              )}
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
