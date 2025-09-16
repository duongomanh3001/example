"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { use } from "react";
import { AssignmentService } from "@/services/assignment.service";
import { DetailedAssignmentResponse } from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";
import ViewToggle from "@/components/common/ViewToggle";

type Props = { params: Promise<{ id: string }> };

function AssignmentDetails({ params }: Props) {
  const resolvedParams = use(params);
  const [currentView, setCurrentView] = useState<'teacher' | 'student'>('teacher');
  const [assignment, setAssignment] = useState<DetailedAssignmentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      const assignmentId = parseInt(resolvedParams.id);
      
      if (currentView === 'teacher') {
        const assignmentData = await AssignmentService.getAssignmentById(assignmentId);
        setAssignment(assignmentData);
      } else {
        // Simulate student view - redirect to student assignment page
        window.location.href = `/student/assignment/${assignmentId}`;
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu');
      console.error('Failed to fetch assignment data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [resolvedParams.id, currentView]);

  const handleToggleStatus = async () => {
    if (!assignment) return;
    
    try {
      await AssignmentService.toggleAssignmentStatus(assignment.id);
      // Refresh assignment data
      fetchData();
    } catch (err) {
      alert('Có lỗi xảy ra khi thay đổi trạng thái bài tập');
      console.error('Failed to toggle assignment status:', err);
    }
  };

  const handleDeleteAssignment = async () => {
    if (!assignment) return;
    if (!confirm('Bạn có chắc chắn muốn xóa bài tập này không?')) return;
    
    try {
      await AssignmentService.deleteAssignment(assignment.id);
      alert('Đã xóa bài tập thành công!');
      window.location.href = '/teacher';
    } catch (err) {
      alert('Có lỗi xảy ra khi xóa bài tập');
      console.error('Failed to delete assignment:', err);
    }
  };

  const formatDate = (iso?: string) => {
    if (!iso) return '-';
    const date = new Date(iso);
    return date.toLocaleDateString('vi-VN') + ' ' + date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  if (loading) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-6">
          <div className="animate-pulse">
            <div className="h-6 bg-slate-200 rounded w-64 mb-6"></div>
            <div className="h-64 bg-slate-200 rounded-lg"></div>
          </div>
        </div>
      </MainLayout>
    );
  }

  if (error || !assignment) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-10">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-600">{error || 'Không tìm thấy bài tập'}</p>
          </div>
          <Link className="text-blue-600 hover:underline" href="/teacher">← Quay lại danh sách bài tập</Link>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="mx-auto max-w-7xl px-4 py-6">
        {/* Header */}
        <div className="mb-6 flex justify-between items-start">
          <div>
            <div className="flex items-center gap-4 mb-2">
              <Link 
                href="/teacher" 
                className="text-slate-600 hover:text-slate-900"
              >
                ← Quay lại
              </Link>
            </div>
            <h1 className="text-[#ff6a00] font-semibold text-xl">{assignment.title}</h1>
            <p className="text-slate-600 text-sm mt-1">{assignment.courseName}</p>
          </div>
          <ViewToggle 
            currentView={currentView} 
            onViewChange={setCurrentView}
          />
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            {/* Basic Info */}
            <div className="bg-white border border-slate-200 rounded-lg p-6">
              <h2 className="text-lg font-semibold text-slate-900 mb-4">Thông tin cơ bản</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <div className="space-y-3 text-sm">
                    <div className="flex items-center gap-2">
                      <span className="text-slate-600">Loại:</span> 
                      <span className={`px-2 py-1 text-xs rounded-full ${
                        assignment.type === 'EXERCISE' ? 'bg-blue-100 text-blue-800' :
                        assignment.type === 'EXAM' ? 'bg-red-100 text-red-800' :
                        assignment.type === 'PROJECT' ? 'bg-orange-100 text-orange-800' :
                        assignment.type === 'QUIZ' ? 'bg-green-100 text-green-800' :
                        'bg-gray-100 text-gray-800'
                      }`}>
                        {assignment.type === 'EXERCISE' ? 'Bài tập' :
                         assignment.type === 'EXAM' ? 'Bài thi' :
                         assignment.type === 'PROJECT' ? 'Dự án' :
                         assignment.type === 'QUIZ' ? 'Kiểm tra nhanh' : assignment.type}
                      </span>
                    </div>
                    <div><span className="text-slate-600">Điểm tối đa:</span> {assignment.maxScore}</div>
                    <div><span className="text-slate-600">Thời gian:</span> {assignment.timeLimit} phút</div>
                    <div><span className="text-slate-600">Trạng thái:</span> 
                      <span className={`ml-1 ${assignment.isActive ? 'text-green-600' : 'text-gray-600'}`}>
                        {assignment.isActive ? 'Hoạt động' : 'Đã tắt'}
                      </span>
                    </div>
                  </div>
                </div>
                
                <div>
                  <div className="space-y-3 text-sm">
                    <div><span className="text-slate-600">Tự động chấm:</span> {assignment.autoGrade ? 'Có' : 'Không'}</div>
                    <div><span className="text-slate-600">Cho phép nộp muộn:</span> {assignment.allowLateSubmission ? 'Có' : 'Không'}</div>
                    {assignment.startTime && (
                      <div><span className="text-slate-600">Bắt đầu:</span> {formatDate(assignment.startTime)}</div>
                    )}
                    {assignment.endTime && (
                      <div><span className="text-slate-600">Kết thúc:</span> {formatDate(assignment.endTime)}</div>
                    )}
                  </div>
                </div>
              </div>

              {assignment.description && (
                <div className="mt-6">
                  <h3 className="font-medium text-slate-900 mb-2">Mô tả</h3>
                  <p className="text-sm text-slate-600">{assignment.description}</p>
                </div>
              )}
            </div>

            {/* Questions */}
            {assignment.questions && assignment.questions.length > 0 && (
              <div className="bg-white border border-slate-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-slate-900 mb-4">
                  Câu hỏi ({assignment.questions.length})
                </h3>
                <div className="space-y-4">
                  {assignment.questions.map((question, index) => (
                    <div key={question.id} className="border border-slate-200 rounded-lg p-4">
                      <div className="flex items-start justify-between mb-3">
                        <h4 className="font-medium text-slate-900">
                          Câu {index + 1}: {question.title}
                        </h4>
                        <div className="flex gap-2">
                          <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                            question.questionType === 'PROGRAMMING' ? 'bg-blue-100 text-blue-800' :
                            question.questionType === 'MULTIPLE_CHOICE' ? 'bg-green-100 text-green-800' :
                            question.questionType === 'ESSAY' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-purple-100 text-purple-800'
                          }`}>
                            {question.questionType === 'PROGRAMMING' ? 'Lập trình' :
                             question.questionType === 'MULTIPLE_CHOICE' ? 'Trắc nghiệm' :
                             question.questionType === 'ESSAY' ? 'Tự luận' :
                             'Đúng/Sai'}
                          </span>
                          <span className="text-xs px-2 py-1 bg-slate-100 text-slate-800 rounded font-medium">
                            {question.points} điểm
                          </span>
                        </div>
                      </div>
                      
                      <p className="text-sm text-slate-600 mb-3">{question.description}</p>
                      
                      {question.testCases && question.testCases.length > 0 && (
                        <div className="text-xs text-slate-500">
                          Test cases: {question.testCases.length}
                          {question.testCases.some(tc => tc.isHidden) && ' (có test case ẩn)'}
                        </div>
                      )}
                      
                      {question.options && question.options.length > 0 && (
                        <div className="mt-2">
                          <div className="text-xs text-slate-500 mb-1">Lựa chọn:</div>
                          <div className="space-y-1">
                            {question.options.map((option, optIndex) => (
                              <div key={option.id || optIndex} className="text-xs flex items-center">
                                <span className={`w-4 h-4 rounded-full mr-2 flex items-center justify-center ${
                                  option.isCorrect ? 'bg-green-100 text-green-600' : 'bg-slate-100 text-slate-400'
                                }`}>
                                  {option.isCorrect ? '✓' : '○'}
                                </span>
                                {option.optionText}
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Statistics */}
            <div className="bg-white border border-slate-200 rounded-lg p-6">
              <h3 className="font-semibold text-slate-900 mb-4">Thống kê</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-blue-50 p-4 rounded-lg text-center">
                  <div className="text-2xl font-bold text-blue-600">{assignment.submissionCount || 0}</div>
                  <div className="text-sm text-blue-800">Bài nộp</div>
                </div>
                <div className="bg-orange-50 p-4 rounded-lg text-center">
                  <div className="text-2xl font-bold text-orange-600">{assignment.pendingCount || 0}</div>
                  <div className="text-sm text-orange-800">Chờ chấm</div>
                </div>
                <div className="bg-green-50 p-4 rounded-lg text-center">
                  <div className="text-2xl font-bold text-green-600">{assignment.questions?.length || 0}</div>
                  <div className="text-sm text-green-800">Câu hỏi</div>
                </div>
                <div className="bg-purple-50 p-4 rounded-lg text-center">
                  <div className="text-2xl font-bold text-purple-600">
                    {assignment.testCases?.length || assignment.questions?.reduce((acc, q) => acc + (q.testCases?.length || 0), 0) || 0}
                  </div>
                  <div className="text-sm text-purple-800">Test cases</div>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="bg-white border border-slate-200 rounded-lg p-6">
              <h3 className="font-semibold text-slate-900 mb-4">Thao tác</h3>
              <div className="space-y-3">
                <button
                  onClick={handleToggleStatus}
                  className={`w-full px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                    assignment.isActive 
                      ? 'bg-orange-100 text-orange-800 hover:bg-orange-200' 
                      : 'bg-green-100 text-green-800 hover:bg-green-200'
                  }`}
                >
                  {assignment.isActive ? '🔒 Tắt bài tập' : '🔓 Bật bài tập'}
                </button>
                
                <button className="w-full px-4 py-2 text-sm font-medium text-blue-800 bg-blue-100 hover:bg-blue-200 rounded-md transition-colors">
                  Xem báo cáo chi tiết
                </button>
                
                <button className="w-full px-4 py-2 text-sm font-medium text-purple-800 bg-purple-100 hover:bg-purple-200 rounded-md transition-colors">
                  Chỉnh sửa bài tập
                </button>
                
                <button
                  onClick={handleDeleteAssignment}
                  className="w-full px-4 py-2 text-sm font-medium text-red-800 bg-red-100 hover:bg-red-200 rounded-md transition-colors"
                >
                  Xóa bài tập
                </button>
              </div>
            </div>

            {/* Info */}
            <div className="bg-white border border-slate-200 rounded-lg p-6">
              <h3 className="font-semibold text-slate-900 mb-4">Thông tin</h3>
              <div className="text-sm text-slate-600 space-y-2">
                <div><span className="font-medium">Tạo lúc:</span><br/>{formatDate(assignment.createdAt)}</div>
                <div><span className="font-medium">Cập nhật:</span><br/>{formatDate(assignment.updatedAt)}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(AssignmentDetails, {
  requiredRoles: [Role.TEACHER],
});
