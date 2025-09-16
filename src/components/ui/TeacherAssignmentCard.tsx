"use client";

import Link from "next/link";
import { StudentAssignmentResponse } from "@/types/api";

interface TeacherAssignmentCardProps {
  assignment: StudentAssignmentResponse;
  isTeacherView?: boolean;
}

export default function TeacherAssignmentCard({ assignment, isTeacherView = false }: TeacherAssignmentCardProps) {
  const formatDate = (iso?: string) => {
    if (!iso) return '';
    const date = new Date(iso);
    return date.toLocaleDateString('vi-VN');
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
      case 'QUIZ': return 'Kiểm tra nhanh';
      default: return type;
    }
  };

  const href = isTeacherView 
    ? `/teacher/assignment/${assignment.id}`
    : `/student/assignment/${assignment.id}`;

  return (
    <Link 
      href={href}
      className="block bg-white border border-slate-200 rounded-lg p-4 hover:shadow-md hover:border-slate-300 transition-all duration-200"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-slate-900 truncate">{assignment.title}</h3>
          <p className="text-sm text-slate-600 mt-1">{assignment.courseName}</p>
        </div>
        <div className="flex items-center gap-2 ml-4">
          <span className={`text-xs px-2 py-1 rounded-full font-medium ${getTypeColor(assignment.type)}`}>
            {getTypeName(assignment.type)}
          </span>
          <span className="text-xs px-2 py-1 bg-emerald-100 text-emerald-800 rounded-full font-medium">
            {assignment.maxScore} điểm
          </span>
        </div>
      </div>

      {assignment.description && (
        <p className="text-sm text-slate-600 mb-3 line-clamp-2">{assignment.description}</p>
      )}

      <div className="flex items-center justify-between text-xs text-slate-500">
        <div className="flex items-center gap-4">
          <span> {assignment.timeLimit} phút</span>
          {assignment.totalQuestions > 0 && (
            <span> {assignment.totalQuestions} câu</span>
          )}
          {isTeacherView && (
            <span> 0 bài nộp</span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {assignment.endTime && (
            <span className="text-slate-600">
               {formatDate(assignment.endTime)}
            </span>
          )}
          {!isTeacherView && assignment.isSubmitted && (
            <span className="text-emerald-600 font-medium">Đã nộp</span>
          )}
        </div>
      </div>
    </Link>
  );
}
