"use client";

import { useState, useEffect } from 'react';
import { AssignmentService } from '@/services/assignment.service';
import { CourseService } from '@/services/course.service';
import { AssignmentResponse, DetailedAssignmentResponse, CourseResponse } from '@/types/api';
import AssignmentCreationForm from './AssignmentCreationForm';

export default function TeacherAssignmentManagement() {
  const [assignments, setAssignments] = useState<AssignmentResponse[]>([]);
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number>(0);
  const [selectedAssignment, setSelectedAssignment] = useState<DetailedAssignmentResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showAssignmentDetail, setShowAssignmentDetail] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedCourse > 0) {
      loadAssignmentsByCourse(selectedCourse);
    } else {
      loadAllAssignments();
    }
  }, [selectedCourse]);

  const loadInitialData = async () => {
    try {
      setIsLoading(true);
      const [assignmentsData, coursesData] = await Promise.all([
        AssignmentService.getAllAssignments(),
        CourseService.getTeacherCourses()
      ]);
      setAssignments(assignmentsData);
      setCourses(coursesData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu');
      console.error('Failed to load data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadAllAssignments = async () => {
    try {
      const assignmentsData = await AssignmentService.getAllAssignments();
      setAssignments(assignmentsData);
    } catch (err) {
      console.error('Failed to load assignments:', err);
    }
  };

  const loadAssignmentsByCourse = async (courseId: number) => {
    try {
      const assignmentsData = await AssignmentService.getAssignmentsByCourse(courseId);
      setAssignments(assignmentsData);
    } catch (err) {
      console.error('Failed to load course assignments:', err);
    }
  };

  const handleViewAssignment = async (assignmentId: number) => {
    try {
      const assignment = await AssignmentService.getAssignmentById(assignmentId);
      setSelectedAssignment(assignment);
      setShowAssignmentDetail(true);
    } catch (err) {
      alert('Có lỗi xảy ra khi tải chi tiết bài tập');
      console.error('Failed to load assignment details:', err);
    }
  };

  const handleDeleteAssignment = async (assignmentId: number) => {
    if (!confirm('Bạn có chắc chắn muốn xóa bài tập này?')) return;
    
    try {
      await AssignmentService.deleteAssignment(assignmentId);
      // Reload assignments
      if (selectedCourse > 0) {
        loadAssignmentsByCourse(selectedCourse);
      } else {
        loadAllAssignments();
      }
      alert('Đã xóa bài tập thành công!');
    } catch (err) {
      alert('Có lỗi xảy ra khi xóa bài tập');
      console.error('Failed to delete assignment:', err);
    }
  };

  const handleToggleStatus = async (assignmentId: number) => {
    try {
      await AssignmentService.toggleAssignmentStatus(assignmentId);
      // Reload assignments
      if (selectedCourse > 0) {
        loadAssignmentsByCourse(selectedCourse);
      } else {
        loadAllAssignments();
      }
    } catch (err) {
      alert('Có lỗi xảy ra khi thay đổi trạng thái bài tập');
      console.error('Failed to toggle assignment status:', err);
    }
  };

  const handleAssignmentCreated = () => {
    setShowCreateForm(false);
    // Reload assignments after a brief delay to ensure backend processing
    setTimeout(() => {
      if (selectedCourse > 0) {
        loadAssignmentsByCourse(selectedCourse);
      } else {
        loadAllAssignments();
      }
    }, 1000);
  };

  const formatDate = (iso?: string) => {
    if (!iso) return '-';
    const date = new Date(iso);
    return date.toLocaleDateString('vi-VN') + ' ' + date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Quản lý bài tập</h2>
          <p className="text-slate-600 mt-1">Tạo và quản lý các bài tập cho sinh viên</p>
        </div>
        <button 
          onClick={() => setShowCreateForm(true)} 
          className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-md transition-colors"
        >
          <span className="mr-2">+</span>
          Tạo bài tập mới
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
          {error}
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-lg border border-slate-200 p-4">
        <div className="flex items-center space-x-4">
          <label className="text-sm font-medium text-slate-700">Lọc theo khóa học:</label>
          <select
            value={selectedCourse}
            onChange={(e) => setSelectedCourse(parseInt(e.target.value))}
            className="px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500"
          >
            <option value={0}>Tất cả khóa học</option>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.code} - {course.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Assignments List */}
      <div className="space-y-4">
        {assignments.length === 0 ? (
          <div className="bg-white rounded-lg border border-slate-200 p-12 text-center">
            <div className="text-4xl mb-4">📝</div>
            <h3 className="text-lg font-semibold text-slate-900 mb-2">Chưa có bài tập nào</h3>
            <p className="text-slate-600 mb-4">
              {selectedCourse > 0 
                ? 'Khóa học này chưa có bài tập nào. Hãy tạo bài tập đầu tiên!' 
                : 'Bạn chưa tạo bài tập nào. Hãy bắt đầu tạo bài tập cho sinh viên!'}
            </p>
            <button 
              onClick={() => setShowCreateForm(true)} 
              className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-md transition-colors"
            >
              <span className="mr-2">+</span>
              Tạo bài tập đầu tiên
            </button>
          </div>
        ) : (
          assignments.map((assignment) => (
            <div key={assignment.id} className="bg-white rounded-lg border border-slate-200 p-6">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-start space-x-4">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-slate-900 mb-2">
                        {assignment.title}
                      </h3>
                      
                      <div className="flex items-center space-x-4 text-sm text-slate-600 mb-3">
                        <span className="flex items-center">
                          <span className="w-2 h-2 bg-blue-500 rounded-full mr-2"></span>
                          {assignment.courseName}
                        </span>
                        <span>•</span>
                        <span>{assignment.totalQuestions > 0 ? assignment.totalQuestions : 'N/A'} câu hỏi</span>
                        <span>•</span>
                        <span>{assignment.maxScore} điểm</span>
                        <span>•</span>
                        <span>{assignment.timeLimit} phút</span>
                      </div>

                      <p className="text-slate-700 mb-4 line-clamp-2">
                        {assignment.description || 'Không có mô tả'}
                      </p>

                      <div className="flex items-center space-x-4 text-sm">
                        <div className="flex items-center space-x-2">
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                            assignment.type === 'EXERCISE' ? 'bg-blue-100 text-blue-800' :
                            assignment.type === 'EXAM' ? 'bg-red-100 text-red-800' :
                            assignment.type === 'PROJECT' ? 'bg-orange-100 text-orange-800' :
                            'bg-green-100 text-green-800'
                          }`}>
                            {assignment.type === 'EXERCISE' ? 'Bài tập' :
                             assignment.type === 'EXAM' ? 'Bài thi' :
                             assignment.type === 'PROJECT' ? 'Dự án' :
                             assignment.type === 'QUIZ' ? 'Quiz' : assignment.type}
                          </span>
                          
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                            assignment.isActive 
                              ? 'bg-green-100 text-green-800' 
                              : 'bg-gray-100 text-gray-800'
                          }`}>
                            {assignment.isActive ? 'Hoạt động' : 'Đã tắt'}
                          </span>
                        </div>
                      </div>

                      <div className="mt-3 text-xs text-slate-500 space-y-1">
                        <div>Bài nộp: {assignment.submissionCount || 0} | Chờ chấm: {assignment.pendingCount || 0}</div>
                        {assignment.startTime && (
                          <div>Bắt đầu: {formatDate(assignment.startTime)}</div>
                        )}
                        {assignment.endTime && (
                          <div>Kết thúc: {formatDate(assignment.endTime)}</div>
                        )}
                        <div>Tạo lúc: {formatDate(assignment.createdAt)}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-2 ml-4">
                  <button
                    onClick={() => handleViewAssignment(assignment.id)}
                    className="px-3 py-1 text-sm border border-slate-300 rounded-md hover:bg-slate-50 transition-colors"
                  >
                    Xem
                  </button>
                  
                  <button
                    onClick={() => handleToggleStatus(assignment.id)}
                    className={`px-3 py-1 text-sm border border-slate-300 rounded-md hover:bg-slate-50 transition-colors ${
                      assignment.isActive ? 'text-orange-600' : 'text-green-600'
                    }`}
                  >
                    {assignment.isActive ? 'Tắt' : 'Bật'}
                  </button>

                  <button
                    onClick={() => handleDeleteAssignment(assignment.id)}
                    className="px-3 py-1 text-sm border border-red-300 rounded-md text-red-600 hover:bg-red-50 transition-colors"
                  >
                    Xóa
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Assignment Creation Form */}
      <AssignmentCreationForm
        isOpen={showCreateForm}
        onClose={() => setShowCreateForm(false)}
        onSuccess={handleAssignmentCreated}
      />

      {/* Assignment Detail Modal */}
      {showAssignmentDetail && selectedAssignment && (
        <AssignmentDetailModal
          assignment={selectedAssignment}
          onClose={() => {
            setShowAssignmentDetail(false);
            setSelectedAssignment(null);
          }}
        />
      )}
    </div>
  );
}

// Component for showing assignment details
function AssignmentDetailModal({ 
  assignment, 
  onClose 
}: { 
  assignment: DetailedAssignmentResponse; 
  onClose: () => void; 
}) {
  const formatDate = (iso?: string) => {
    if (!iso) return '-';
    const date = new Date(iso);
    return date.toLocaleDateString('vi-VN') + ' ' + date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-xl font-bold text-slate-900">{assignment.title}</h2>
              <p className="text-slate-600">{assignment.courseName}</p>
            </div>
            <button onClick={onClose} className="text-slate-400 hover:text-slate-600 text-2xl">
              ×
            </button>
          </div>
        </div>

        <div className="p-6 space-y-6">
          {/* Basic Info */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h3 className="font-semibold text-slate-900 mb-3">Thông tin cơ bản</h3>
              <div className="space-y-2 text-sm">
                <div><span className="text-slate-600">Loại:</span> {
                  assignment.type === 'EXERCISE' ? 'Bài tập' :
                  assignment.type === 'EXAM' ? 'Bài thi' :
                  assignment.type === 'PROJECT' ? 'Dự án' :
                  assignment.type === 'QUIZ' ? 'Quiz' : assignment.type
                }</div>
                <div><span className="text-slate-600">Điểm tối đa:</span> {assignment.maxScore}</div>
                <div><span className="text-slate-600">Thời gian:</span> {assignment.timeLimit} phút</div>
                <div><span className="text-slate-600">Trạng thái:</span> 
                  <span className={`ml-1 ${assignment.isActive ? 'text-green-600' : 'text-gray-600'}`}>
                    {assignment.isActive ? 'Hoạt động' : 'Đã tắt'}
                  </span>
                </div>
                <div><span className="text-slate-600">Tự động chấm:</span> {assignment.autoGrade ? 'Có' : 'Không'}</div>
                <div><span className="text-slate-600">Cho phép nộp trễ:</span> {assignment.allowLateSubmission ? 'Có' : 'Không'}</div>
              </div>
            </div>

            <div>
              <h3 className="font-semibold text-slate-900 mb-3">Thời gian</h3>
              <div className="space-y-2 text-sm">
                <div><span className="text-slate-600">Bắt đầu:</span> {formatDate(assignment.startTime)}</div>
                <div><span className="text-slate-600">Kết thúc:</span> {formatDate(assignment.endTime)}</div>
                <div><span className="text-slate-600">Tạo lúc:</span> {formatDate(assignment.createdAt)}</div>
                <div><span className="text-slate-600">Cập nhật:</span> {formatDate(assignment.updatedAt)}</div>
              </div>
            </div>
          </div>

          {/* Description */}
          {assignment.description && (
            <div>
              <h3 className="font-semibold text-slate-900 mb-3">Mô tả</h3>
              <div className="bg-slate-50 p-4 rounded-lg text-sm">
                <p className="whitespace-pre-wrap">{assignment.description}</p>
              </div>
            </div>
          )}

          {/* Questions */}
          {assignment.questions && assignment.questions.length > 0 && (
            <div>
              <h3 className="font-semibold text-slate-900 mb-3">
                Câu hỏi ({assignment.questions.length})
              </h3>
              <div className="space-y-4">
                {assignment.questions.map((question, index) => (
                  <div key={question.id} className="border border-slate-200 rounded-lg p-4">
                    <div className="flex justify-between items-start mb-2">
                      <h4 className="font-medium text-slate-900">
                        Câu {index + 1}: {question.title}
                      </h4>
                      <div className="flex gap-2">
                        <span className={`text-xs px-2 py-1 rounded font-medium ${
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

          {/* Statistics */}
          <div>
            <h3 className="font-semibold text-slate-900 mb-3">Thống kê</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-blue-50 p-4 rounded-lg text-center">
                <div className="text-2xl font-bold text-blue-600">{assignment.submissionCount || 0}</div>
                <div className="text-sm text-blue-800">Bài nộp</div>
              </div>
              <div className="bg-orange-50 p-4 rounded-lg text-center">
                <div className="text-2xl font-bold text-orange-600">{assignment.pendingCount || 0}</div>
                <div className="text-sm text-orange-800">Chờ chấm</div>
              </div>
              <div className="bg-green-50 p-4 rounded-lg text-center">
                <div className="text-2xl font-bold text-green-600">{assignment.questions?.length || 1}</div>
                <div className="text-sm text-green-800">Câu hỏi</div>
              </div>
              <div className="bg-purple-50 p-4 rounded-lg text-center">
                <div className="text-2xl font-bold text-purple-600">{assignment.maxScore}</div>
                <div className="text-sm text-purple-800">Điểm tối đa</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
