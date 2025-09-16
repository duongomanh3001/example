"use client";

import { useEffect, useState } from "react";
import CourseCard from "@/components/ui/CourseCard";
import ViewToggle from "@/components/common/ViewToggle";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { useRoleAccess } from "@/hooks/useRoleAccess";
import { CourseService } from "@/services/course.service";
import { CourseResponse } from "@/types/api";
import Link from "next/link";
import MainLayout from "@/components/layouts/MainLayout";

function TeacherPage() {
  const [currentView, setCurrentView] = useState<'teacher' | 'student'>('teacher');
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const { getUserDisplayName, getRoleName } = useRoleAccess();

  useEffect(() => {
    fetchData();
  }, [currentView]);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      if (currentView === 'teacher') {
        // Fetch teacher courses data
        const teacherCourses = await CourseService.getTeacherCourses();
        setCourses(teacherCourses);
      } else {
        // Fetch student data when in student view
        const enrolledCourses = await CourseService.getEnrolledCourses();
        setCourses(enrolledCourses);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu';
      if (errorMessage.includes('không mong muốn') || errorMessage.includes('Network Error') || errorMessage.includes('failed to fetch')) {
        setError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng hoặc liên hệ quản trị viên.');
      } else {
        setError(errorMessage);
      }
      console.error('Failed to fetch data:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-6">
        <div className="animate-pulse">
          <div className="h-6 bg-slate-200 rounded w-64 mb-2"></div>
          <div className="h-4 bg-slate-200 rounded w-48 mb-6"></div>
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-48 bg-slate-200 rounded-lg"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-600">{error}</p>
        </div>
      </div>
    );
  }

  const getViewLabel = () => {
    if (currentView === 'teacher') {
      return 'Giáo viên - Khóa học đang giảng dạy';
    }
    return 'Sinh viên - Khóa học đã đăng ký';
  };

  const getCourseHref = (courseId: number) => {
    if (currentView === 'teacher') {
      return `/teacher/course/${courseId}`;
    }
    return `/student/course/${courseId}`;
  };

  return (
    <MainLayout>
      <div className="mx-auto max-w-7xl px-4 py-6">
        {/* Header with View Toggle */}
        <div className="mb-6 flex justify-between items-start">
          <div>
            <h1 className="text-[#ff6a00] font-semibold text-xl">
              Chào mừng, {getUserDisplayName()}
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              {getViewLabel()}
            </p>
          </div>
          <ViewToggle 
            currentView={currentView} 
            onViewChange={setCurrentView}
          />
        </div>



        {/* Courses Section */}
        <div className="mb-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-lg font-semibold text-slate-900">
              {currentView === 'teacher' ? 'Khóa học tôi đang dạy' : 'Các khóa học của tôi'}
            </h2>
            {currentView === 'teacher' && (
              <Link 
                href="/teacher/assignment/create"
                className="bg-[#ff6a00] text-white px-4 py-2 rounded-md hover:bg-[#e55a00] transition-colors text-sm"
              >
                + Tạo bài tập mới
              </Link>
            )}
          </div>
          
          {courses.length === 0 ? (
            <div className="bg-slate-50 border border-slate-200 rounded-lg p-12 text-center">
              <h3 className="text-lg font-medium text-slate-900 mb-2">
                {currentView === 'teacher' ? 'Chưa có khóa học nào' : 'Chưa đăng ký khóa học'}
              </h3>
              <p className="text-slate-500 mb-4">
                {currentView === 'teacher' 
                  ? 'Bạn chưa được phân công giảng dạy khóa học nào' 
                  : 'Bạn chưa đăng ký khóa học nào'
                }
              </p>
              {currentView === 'student' && (
                <button className="bg-emerald-600 text-white px-4 py-2 rounded-md hover:bg-emerald-700 transition-colors">
                  Xem khóa học có sẵn
                </button>
              )}
            </div>
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {courses.map((course) => (
                <CourseCard 
                  key={course.id}
                  title={course.name}
                  code={course.code}
                  percent={currentView === 'teacher' ? 100 : 0}
                  gradient={currentView === 'teacher' ? "from-orange-500 to-orange-600" : "from-emerald-500 to-emerald-600"}
                  logoText={course.code.substring(0, 2).toUpperCase()}
                  href={getCourseHref(course.id)}
                />
              ))}
            </div>
          )}
        </div>


      </div>
    </MainLayout>
  );
}

export default withAuth(TeacherPage, {
  requiredRoles: [Role.TEACHER],
});
