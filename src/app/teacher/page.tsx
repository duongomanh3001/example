"use client";

import { useEffect, useState } from "react";
import CourseCard from "@/components/ui/CourseCard";
import Calendar from "@/components/common/Calendar";
import AssignmentDetailsModal from "@/components/common/AssignmentDetailsModal";
import UpcomingAssignments from "@/components/common/UpcomingAssignments";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { useRoleAccess } from "@/hooks/useRoleAccess";
import { CourseService } from "@/services/course.service";
import { AssignmentService } from "@/services/assignment.service";
import { CourseResponse, StudentAssignmentResponse } from "@/types/api";
import Link from "next/link";
import TeacherLayout from "@/components/layouts/TeacherLayout";

function TeacherPage() {
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [assignments, setAssignments] = useState<StudentAssignmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedAssignments, setSelectedAssignments] = useState<StudentAssignmentResponse[]>([]);
  
  const { getUserDisplayName } = useRoleAccess();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      // Fetch teacher courses and assignments data
      const [teacherCourses, teacherAssignments] = await Promise.all([
        CourseService.getTeacherCourses(),
        AssignmentService.getAssignmentsForStudent() // This will get all assignments accessible
      ]);
      setCourses(teacherCourses);
      setAssignments(teacherAssignments);
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

  const handleDateClick = (date: Date, dayAssignments: StudentAssignmentResponse[]) => {
    setSelectedDate(date);
    setSelectedAssignments(dayAssignments);
  };

  const handleCloseModal = () => {
    setSelectedDate(null);
    setSelectedAssignments([]);
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

  return (
    <TeacherLayout>
      <div>
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-[#ff6a00] font-semibold text-xl">
            Chào mừng, {getUserDisplayName()}
          </h1>
          <p className="text-slate-500 text-sm mt-1">
            Giáo viên - Khóa học đang giảng dạy
          </p>
        </div>

        {/* Calendar and Upcoming Assignments Section */}
        <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <h2 className="text-lg font-semibold text-slate-900 mb-4">Lịch bài tập</h2>
            <Calendar 
              assignments={assignments}
              onDateClick={handleDateClick}
            />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-slate-900 mb-4">Sắp tới hạn</h2>
            <UpcomingAssignments 
              assignments={assignments}
              viewType="teacher"
              maxItems={8}
            />
          </div>
        </div>

        {/* Courses Section */}
        <div className="mb-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-lg font-semibold text-slate-900">
              Khóa học tôi đang dạy
            </h2>
            <Link 
              href="/teacher/assignment/create"
              className="bg-[#ff6a00] text-white px-4 py-2 rounded-md hover:bg-[#e55a00] transition-colors text-sm"
            >
              + Tạo bài tập mới
            </Link>
          </div>
          
          {courses.length === 0 ? (
            <div className="bg-slate-50 border border-slate-200 rounded-lg p-12 text-center">
              <h3 className="text-lg font-medium text-slate-900 mb-2">
                Chưa có khóa học nào
              </h3>
              <p className="text-slate-500 mb-4">
                Bạn chưa được phân công giảng dạy khóa học nào
              </p>
            </div>
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {courses.map((course) => (
                <CourseCard 
                  key={course.id}
                  title={course.name}
                  code={course.code}
                  percent={100}
                  gradient="from-orange-500 to-orange-600"
                  logoText={course.code.substring(0, 2).toUpperCase()}
                  href={`/teacher/course/${course.id}`}
                />
              ))}
            </div>
          )}
        </div>

        {/* Assignment Details Modal */}
        {selectedDate && selectedAssignments.length > 0 && (
          <AssignmentDetailsModal
            date={selectedDate}
            assignments={selectedAssignments}
            onClose={handleCloseModal}
            viewType="teacher"
          />
        )}
      </div>
    </TeacherLayout>
  );
}

export default withAuth(TeacherPage, {
  requiredRoles: [Role.TEACHER],
});
