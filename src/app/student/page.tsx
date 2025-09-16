"use client";

import { useEffect, useState } from "react";
import CourseCard from "@/components/ui/CourseCard";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { useRoleAccess } from "@/hooks/useRoleAccess";
import { CourseService } from "@/services/course.service";
import { CourseResponse } from "@/types/api";
import Link from "next/link";
import MainLayout from "@/components/layouts/MainLayout";

function StudentDashboard() {
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const { getUserDisplayName, getRoleName } = useRoleAccess();

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Fetch enrolled courses only
        const enrolledCourses = await CourseService.getEnrolledCourses();
        setCourses(enrolledCourses);
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu';
        if (errorMessage.includes('không mong muốn') || errorMessage.includes('Network Error') || errorMessage.includes('failed to fetch')) {
          setError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng hoặc liên hệ quản trị viên.');
        } else {
          setError(errorMessage);
        }
        console.error('Failed to fetch student data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-6">
        <div className="animate-pulse">
          <div className="h-6 bg-primary-200 rounded w-64 mb-2"></div>
          <div className="h-4 bg-primary-200 rounded w-48 mb-6"></div>
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-48 bg-primary-200 rounded-lg"></div>
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

  return (
    <MainLayout>
      <div className="mx-auto max-w-7xl px-4 py-6">
      {/* Welcome Section */}
      <div className="mb-6">
        <h1 className="text-primary font-semibold text-xl">
          Chào mừng, {getUserDisplayName()}
        </h1>
        <p className="text-primary-400 text-sm mt-1">
          {getRoleName()} - Các khóa học đã đăng ký
        </p>
      </div>

      {/* Courses Section */}
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-primary mb-6">Các khóa học của tôi</h2>
        
        {courses.length === 0 ? (
          <div className="bg-primary-50 border border-primary-200 rounded-lg p-12 text-center">
            <div className="text-6xl mb-4"></div>
            <h3 className="text-lg font-medium text-primary mb-2">Chưa có khóa học nào</h3>
            <p className="text-primary-400 mb-4">Bạn chưa đăng ký khóa học nào</p>
            <button className="bg-primary text-white px-4 py-2 rounded-md hover:bg-primary-600 transition-colors">
              Xem khóa học có sẵn
            </button>
          </div>
        ) : (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {courses.map((course) => (
              <CourseCard 
                key={course.id}
                title={course.name}
                code={course.code}
                percent={0}
                gradient="from-primary to-primary-600"
                logoText={course.code.substring(0, 2).toUpperCase()}
                href={`/student/course/${course.id}`}
              />
            ))}
          </div>
        )}
      </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(StudentDashboard, {
  requiredRoles: [Role.STUDENT, Role.TEACHER, Role.ADMIN],
});
