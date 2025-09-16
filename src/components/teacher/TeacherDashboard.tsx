"use client";

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import TeacherAssignmentManagement from '@/components/teacher/TeacherAssignmentManagement';
import AssignmentCreationForm from '@/components/teacher/AssignmentCreationForm';
import { CourseService } from '@/services/course.service';
import { AssignmentService } from '@/services/assignment.service';
import { CourseResponse, AssignmentResponse } from '@/types/api';
import { Role } from '@/types/auth';
import MainLayout from '@/components/layouts/MainLayout';

interface TeacherStats {
  totalCourses: number;
  totalStudents: number;
  totalAssignments: number;
  pendingSubmissions: number;
}

export default function TeacherDashboard() {
  const { state, hasRole } = useAuth();
  const user = state.user;
  const [activeTab, setActiveTab] = useState('overview');
  const [stats, setStats] = useState<TeacherStats>({
    totalCourses: 0,
    totalStudents: 0,
    totalAssignments: 0,
    pendingSubmissions: 0
  });
  const [recentCourses, setRecentCourses] = useState<CourseResponse[]>([]);
  const [recentAssignments, setRecentAssignments] = useState<AssignmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAssignmentForm, setShowAssignmentForm] = useState(false);

  useEffect(() => {
    if (user?.id) {
      loadDashboardData();
    }
  }, [user]);

  const loadDashboardData = async () => {
    if (!user?.id) return;

    try {
      setIsLoading(true);
      setError(null);
      
      const [courses, assignments] = await Promise.all([
        CourseService.getTeacherCourses(),
        AssignmentService.getAllAssignments()
      ]);

      // Calculate stats
      const totalStudents = courses.reduce((sum: number, course: CourseResponse) => sum + (course.currentStudentCount || 0), 0);

      setStats({
        totalCourses: courses.length,
        totalStudents,
        totalAssignments: assignments.length,
        pendingSubmissions: 0 // This would need to be calculated from submissions
      });

      setRecentCourses(courses.slice(0, 5));
      setRecentAssignments(assignments.slice(0, 5));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu dashboard');
    } finally {
      setIsLoading(false);
    }
  };

  // Redirect if not teacher
  if (!hasRole(Role.TEACHER)) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-600 mb-2">Truy cập bị từ chối</h1>
          <p className="text-slate-600">Bạn không có quyền truy cập trang này.</p>
        </div>
      </div>
    );
  }

  const handleAssignmentCreated = () => {
    setShowAssignmentForm(false);
    loadDashboardData(); // Reload data to show new assignment
  };

  const tabs = [
    { id: 'overview', label: 'Tổng quan', icon: '📊' },
    { id: 'courses', label: 'Khóa học của tôi', icon: '📚' },
    { id: 'assignments', label: 'Quản lý bài tập', icon: '📝' },
    { id: 'submissions', label: 'Bài nộp', icon: '📤' }
  ];

  const renderOverview = () => (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Khóa học</p>
              <p className="text-2xl font-bold text-slate-900">{stats.totalCourses}</p>
            </div>
            <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <span className="text-2xl">📚</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Sinh viên</p>
              <p className="text-2xl font-bold text-slate-900">{stats.totalStudents}</p>
            </div>
            <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
              <span className="text-2xl">🎓</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Bài tập</p>
              <p className="text-2xl font-bold text-slate-900">{stats.totalAssignments}</p>
            </div>
            <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
              <span className="text-2xl">📝</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Bài chờ chấm</p>
              <p className="text-2xl font-bold text-slate-900">{stats.pendingSubmissions}</p>
            </div>
            <div className="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center">
              <span className="text-2xl">⏳</span>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Courses */}
        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Khóa học gần đây</h3>
          {recentCourses.length > 0 ? (
            <div className="space-y-3">
              {recentCourses.map((course) => (
                <div key={course.id} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                  <div>
                    <h4 className="font-medium text-slate-900">{course.name || 'Khóa học không có tên'}</h4>
                    <p className="text-sm text-slate-500">
                      {course.code || 'N/A'} • {course.currentStudentCount || 0}/{course.maxStudents || 0} sinh viên
                    </p>
                  </div>
                  <div className="text-right">
                    <span className={`text-xs px-2 py-1 rounded-full ${
                      course.isActive 
                        ? 'bg-green-100 text-green-700' 
                        : 'bg-red-100 text-red-700'
                    }`}>
                      {course.isActive ? 'Hoạt động' : 'Tạm dừng'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-slate-500 text-center py-4">Chưa có khóa học nào</p>
          )}
        </div>

        {/* Recent Assignments */}
        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Bài tập gần đây</h3>
          {recentAssignments.length > 0 ? (
            <div className="space-y-3">
              {recentAssignments.map((assignment) => (
                <div key={assignment.id} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                  <div className="flex-1">
                    <h4 className="font-medium text-slate-900 truncate">{assignment.title || 'Bài tập không có tiêu đề'}</h4>
                    <p className="text-sm text-slate-500">
                      {assignment.courseName || 'Không có khóa học'} • {assignment.submissionCount || 0} bài nộp
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`text-xs px-2 py-1 rounded-full ${
                      assignment.type === 'EXERCISE' ? 'bg-blue-100 text-blue-700' :
                      assignment.type === 'EXAM' ? 'bg-red-100 text-red-700' :
                      assignment.type === 'PROJECT' ? 'bg-orange-100 text-orange-700' :
                      'bg-green-100 text-green-700'
                    }`}>
                      {assignment.type === 'EXERCISE' ? 'Bài tập' :
                       assignment.type === 'EXAM' ? 'Bài thi' :
                       assignment.type === 'PROJECT' ? 'Dự án' :
                       assignment.type === 'QUIZ' ? 'Quiz' : assignment.type}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-slate-500 text-center py-4">Chưa có bài tập nào</p>
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-lg border border-slate-200 p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">Thao tác nhanh</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <button
            onClick={() => setShowAssignmentForm(true)}
            className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg hover:bg-emerald-100 transition-colors"
          >
            <div className="text-2xl mb-2">➕</div>
            <div className="text-sm font-medium text-emerald-800">Tạo bài tập</div>
          </button>
          <button
            onClick={() => setActiveTab('courses')}
            className="p-4 bg-blue-50 border border-blue-200 rounded-lg hover:bg-blue-100 transition-colors"
          >
            <div className="text-2xl mb-2">📚</div>
            <div className="text-sm font-medium text-blue-800">Xem khóa học</div>
          </button>
          <button
            onClick={() => setActiveTab('assignments')}
            className="p-4 bg-purple-50 border border-purple-200 rounded-lg hover:bg-purple-100 transition-colors"
          >
            <div className="text-2xl mb-2">�</div>
            <div className="text-sm font-medium text-purple-800">Quản lý bài tập</div>
          </button>
          <button
            onClick={loadDashboardData}
            className="p-4 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <div className="text-2xl mb-2">🔄</div>
            <div className="text-sm font-medium text-gray-800">Làm mới</div>
          </button>
        </div>
      </div>
    </div>
  );

  const renderMyCourses = () => (
    <div className="bg-white rounded-lg border border-slate-200 p-6">
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-lg font-semibold text-slate-900">Khóa học của tôi</h3>
        <button
          onClick={loadDashboardData}
          className="text-sm text-emerald-600 hover:text-emerald-700"
        >
          Làm mới
        </button>
      </div>
      
      {recentCourses.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {recentCourses.map((course) => (
            <div key={course.id} className="border border-slate-200 rounded-lg p-4">
              <div className="flex justify-between items-start mb-3">
                <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                  <span className="text-blue-600 font-semibold text-xs">
                    {course.code?.substring(0, 2).toUpperCase() || 'CO'}
                  </span>
                </div>
                <span className={`text-xs px-2 py-1 rounded-full ${
                  course.isActive 
                    ? 'bg-green-100 text-green-700' 
                    : 'bg-red-100 text-red-700'
                }`}>
                  {course.isActive ? 'Hoạt động' : 'Tạm dừng'}
                </span>
              </div>
              
              <h4 className="font-semibold text-slate-900 mb-1">{course.name || 'Khóa học không có tên'}</h4>
              <p className="text-sm text-slate-500 mb-2">Mã: {course.code || 'N/A'}</p>
              <p className="text-xs text-slate-400 mb-2">{course.semester || 'N/A'}/{course.year || 'N/A'}</p>
              
              <div className="text-xs text-slate-500 mb-3">
                Sinh viên: {course.currentStudentCount || 0}/{course.maxStudents || 0}
              </div>
              
              <div className="text-xs text-slate-500">
                Tín chỉ: {course.creditHours || 'N/A'}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-12">
          <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="text-4xl">📚</span>
          </div>
          <h3 className="text-lg font-medium text-slate-900 mb-2">Chưa có khóa học nào</h3>
          <p className="text-slate-500">Bạn chưa được phân công giảng dạy khóa học nào.</p>
        </div>
      )}
    </div>
  );

  const renderSubmissions = () => (
    <div className="bg-white rounded-lg border border-slate-200 p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">Quản lý bài nộp</h3>
      <p className="text-slate-600">Chức năng quản lý bài nộp sẽ được triển khai sau...</p>
    </div>
  );

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <MainLayout>
      <div className="min-h-screen bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 mb-2">
            Bảng điều khiển Giáo viên
          </h1>
          <p className="text-slate-600">
            Chào mừng trở lại, {state.user?.fullName}! Quản lý khóa học và bài tập của bạn.
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md mb-6">
            {error}
            <button onClick={() => setError(null)} className="ml-4 text-red-900">×</button>
          </div>
        )}

        {/* Navigation Tabs */}
        <div className="border-b border-slate-200 mb-8">
          <nav className="-mb-px flex space-x-8">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === tab.id
                    ? 'border-emerald-500 text-emerald-600'
                    : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                }`}
              >
                <span className="mr-2">{tab.icon}</span>
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        <div>
          {activeTab === 'overview' && renderOverview()}
          {activeTab === 'courses' && renderMyCourses()}
          {activeTab === 'assignments' && <TeacherAssignmentManagement />}
          {activeTab === 'submissions' && renderSubmissions()}
        </div>
      </div>

      {/* Assignment Creation Form */}
      <AssignmentCreationForm
        isOpen={showAssignmentForm}
        onClose={() => setShowAssignmentForm(false)}
        onSuccess={handleAssignmentCreated}
      />
        </div>
    </MainLayout>
  );
}
