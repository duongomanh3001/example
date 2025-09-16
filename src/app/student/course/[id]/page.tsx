"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { use } from "react";
import { CourseService } from "@/services/course.service";
import { AssignmentService } from "@/services/assignment.service";
import { CourseResponse, StudentAssignmentResponse } from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";

type Props = { params: Promise<{ id: string }> };

function CourseDetails({ params }: Props) {
  const resolvedParams = use(params);
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [assignments, setAssignments] = useState<StudentAssignmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      // Fetch course details and assignments
      const courseId = parseInt(resolvedParams.id);
      
      // Fetch enrolled courses and assignments for students
      const [enrolledCourses, allAssignments] = await Promise.all([
        CourseService.getEnrolledCourses(),
        AssignmentService.getAssignmentsForStudent(),
      ]);

      // Find the specific course from enrolled courses
      const courseData = enrolledCourses.find(c => c.id === courseId);
      if (!courseData) {
        throw new Error('Không tìm thấy khóa học hoặc bạn chưa đăng ký khóa học này');
      }

      // Filter assignments for this specific course
      const courseAssignments = allAssignments.filter(assignment => 
        assignment.courseName === courseData.name
      );

      setCourse(courseData);
      setAssignments(courseAssignments);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu');
      console.error('Failed to fetch course data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [resolvedParams.id]);

  if (loading) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-6">
          <div className="animate-pulse">
            <div className="h-6 bg-primary-200 rounded w-64 mb-6"></div>
            <div className="grid gap-6 md:grid-cols-[240px_1fr]">
              <div className="h-64 bg-primary-200 rounded-lg"></div>
              <div className="h-64 bg-primary-200 rounded-lg"></div>
            </div>
          </div>
        </div>
      </MainLayout>
    );
  }

  if (error || !course) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-10">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-600">{error || 'Không tìm thấy khóa học'}</p>
          </div>
          <Link className="text-primary hover:underline" href="/student">← Quay lại danh sách khóa học</Link>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="mx-auto max-w-7xl px-4 py-6">
        <div className="mb-4">
          <h1 className="text-primary font-semibold text-xl">{course.name}</h1>
          <p className="text-primary-400 text-sm mt-1">{course.code} - {course.description}</p>
        </div>

      <div className="mt-4 grid gap-6 grid-cols-1 md:grid-cols-[240px_1fr]">
        {/* Left sidebar */}
        <aside className="rounded-md border border-primary-200 bg-white">
          <div className="p-4 border-b border-primary-100 text-sm font-medium text-primary">
            {course.name}
          </div>
          <nav className="text-sm">
            <a className="block px-4 py-2 bg-primary text-white" href="#">Khóa học</a>
            <a className="block px-4 py-2 hover:bg-primary-50 text-primary" href="#">Danh sách thành viên</a>
            <a className="block px-4 py-2 hover:bg-primary-50 text-primary" href="#">Điểm số</a>
            <a className="block px-4 py-2 hover:bg-primary-50 text-primary" href="#">Năng lực</a>
            <a className="block px-4 py-2 hover:bg-primary-50 text-rose-600" href="#">Thực Hành</a>
            <a className="block px-4 py-2 hover:bg-primary-50 text-primary" href="#">Mở rộng tất cả</a>
          </nav>
        </aside>

        {/* Main content */}
        <section>
          <div className="rounded-md border border-primary-200 bg-white">
            <div className="flex items-center gap-4 border-b border-primary-100 px-4">
              {(["Khóa học", "Danh sách thành viên", "Điểm số", "Năng lực"] as const).map((t, i) => (
                <button key={i} className={`h-10 px-3 text-sm ${i === 0 ? "border-b-2 border-primary text-primary-600" : "text-primary-400"}`}>{t}</button>
              ))}
            </div>
            <div className="p-3 space-y-3">
              {assignments.length === 0 ? (
                <div className="text-center py-8 text-primary-400">
                  Chưa có bài tập nào trong khóa học này
                </div>
              ) : (
                assignments.map((assignment) => (
                  <Link 
                    href={`/student/course/${course.id}/assignment/${assignment.id}`} 
                    key={assignment.id} 
                    className="assignment-link block rounded-md border border-primary-200 bg-white p-3 hover:shadow-md hover:bg-primary-50 hover:border-primary-300 transition-all"
                  > 
                    <div className="flex items-center gap-2 text-sm">
                      <div className="font-medium flex-1 text-primary">{assignment.title}</div>
                      <div className="flex items-center gap-2">
                        <span className={`text-[11px] rounded px-2 py-0.5 font-medium ${
                          assignment.type === 'EXERCISE' ? 'bg-primary-100 text-primary-800' :
                          assignment.type === 'EXAM' ? 'bg-red-100 text-red-800' :
                          assignment.type === 'PROJECT' ? 'bg-orange-100 text-orange-800' :
                          assignment.type === 'QUIZ' ? 'bg-green-100 text-green-800' :
                          'bg-primary-100 text-primary-800'
                        }`}>
                          {assignment.type === 'EXERCISE' ? 'Bài tập' :
                           assignment.type === 'EXAM' ? 'Bài thi' :
                           assignment.type === 'PROJECT' ? 'Dự án' :
                           assignment.type === 'QUIZ' ? 'Kiểm tra nhanh' : assignment.type}
                        </span>
                        <span className="text-[11px] rounded bg-primary-600 px-2 py-0.5 text-white font-medium">
                          {assignment.maxScore} điểm
                        </span>
                      </div>
                    </div>
                    {assignment.description && (
                      <p className="mt-2 text-sm text-primary font-medium line-clamp-2">{assignment.description}</p>
                    )}
                    <div className="mt-2 flex items-center justify-between text-xs text-primary font-medium">
                      <div className="flex items-center gap-4">
                        {assignment.totalQuestions > 0 && (
                          <span className="font-medium">
                            {assignment.totalQuestions} câu hỏi
                          </span>
                        )}
                        {assignment.endTime && (
                          <span className="font-medium">
                            Hạn nộp: {new Date(assignment.endTime).toLocaleDateString('vi-VN')}
                          </span>
                        )}
                      </div>
                      <div className="ml-auto flex items-center gap-2">
                        <span>Thời gian: {assignment.timeLimit} phút</span>
                        {assignment.isSubmitted && (
                          <span className="text-primary-600 font-medium">✓ Đã nộp</span>
                        )}
                      </div>
                    </div>
                  </Link>
                ))
              )}
            </div>
          </div>
        </section>
      </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(CourseDetails, {
  requiredRoles: [Role.STUDENT, Role.TEACHER, Role.ADMIN],
});
