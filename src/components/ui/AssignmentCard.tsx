"use client";

import Link from "next/link";
import { StudentAssignmentResponse } from "@/types/api";

interface AssignmentCardProps {
  assignment: StudentAssignmentResponse;
}

export default function AssignmentCard({ assignment }: AssignmentCardProps) {
  const getTypeColor = (type: string) => {
    switch (type) {
      case 'EXERCISE': return 'bg-blue-100 text-blue-800';
      case 'EXAM': return 'bg-red-100 text-red-800';
      case 'PROJECT': return 'bg-orange-100 text-orange-800';
      case 'QUIZ': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getTypeText = (type: string) => {
    switch (type) {
      case 'EXERCISE': return 'Bài tập';
      case 'EXAM': return 'Bài thi';
      case 'PROJECT': return 'Dự án';
      case 'QUIZ': return 'Kiểm tra nhanh';
      default: return type;
    }
  };

  return (
    <div className="bg-white border border-primary-200 rounded-lg p-4 hover:shadow-sm transition-shadow hover:border-primary-300">
      <div className="flex items-start justify-between mb-2">
        <h3 className="font-semibold text-primary text-sm line-clamp-1">{assignment.title}</h3>
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getTypeColor(assignment.type)}`}>
          {getTypeText(assignment.type)}
        </span>
      </div>
      
      <p className="text-sm text-primary-600 mb-3 line-clamp-2">{assignment.description}</p>
      
      <div className="space-y-2">
        <div className="flex justify-between text-xs text-primary-400">
          <span>Khóa học: {assignment.courseName || 'N/A'}</span>
          <span>Điểm tối đa: {assignment.maxScore}</span>
        </div>
        
        <div className="flex justify-between text-xs text-primary-400">
          <span>Loại: {getTypeText(assignment.type)}</span>
          <span>Thời gian: {assignment.timeLimit} phút</span>
        </div>

        {assignment.totalQuestions > 0 && (
          <div className="text-xs text-slate-500">
            <span className="font-medium">Số câu hỏi:</span> {assignment.totalQuestions} câu
          </div>
        )}
        
        {assignment.endTime && (
          <div className="text-xs text-slate-500">
            <span className="font-medium">Hạn nộp:</span> {new Date(assignment.endTime).toLocaleDateString('vi-VN')}
          </div>
        )}
        
        {/* Submission Status */}
        {assignment.isSubmitted && (
          <div className="flex justify-between text-xs">
            <span className="text-emerald-600 font-medium">Đã nộp bài</span>
            {assignment.currentScore !== undefined && (
              <span className="text-slate-600">Điểm: {assignment.currentScore}/{assignment.maxScore}</span>
            )}
          </div>
        )}
        
        <div className="flex justify-between items-center pt-2 border-t border-slate-100">
          <div className="text-xs text-slate-500">
            {assignment.submissionStatus || 'Chưa nộp'}
          </div>
          <Link 
            href={`/student/assignment/${assignment.id}`}
            className="bg-emerald-600 text-white px-3 py-1 rounded text-xs hover:bg-emerald-700 transition-colors"
          >
            {assignment.isSubmitted ? 'Xem chi tiết' : 'Làm bài'}
          </Link>
        </div>
      </div>
    </div>
  );
}
