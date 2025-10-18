"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { use } from "react";
import { AssignmentService } from "@/services/assignment.service";
import { CourseService } from "@/services/course.service";
import { StudentAssignmentResponse, CourseResponse } from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";

type Props = { params: Promise<{ id: string; aid: string }> };

function formatDate(iso?: string) {
  if (!iso) return "-";
  const d = new Date(iso);
  const weekday = d.toLocaleDateString("vi-VN", { weekday: "long" });
  const date = d.toLocaleDateString("vi-VN");
  const time = d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
  return `${weekday}, ${date}, ${time}`;
}

function AssignmentDetails({ params }: Props) {
  const resolvedParams = use(params);
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [assignment, setAssignment] = useState<StudentAssignmentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        const courseId = parseInt(resolvedParams.id);
        const assignmentId = parseInt(resolvedParams.aid);
        
        const [courseData, assignmentData] = await Promise.all([
          CourseService.getStudentCourseById(courseId),
          AssignmentService.getAssignmentForStudent(assignmentId),
        ]);

        setCourse(courseData);
        setAssignment(assignmentData);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu');
        console.error('Failed to fetch assignment data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [resolvedParams.id, resolvedParams.aid]);

  if (loading) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-4xl px-4 py-4">
          <div className="animate-pulse">
            <div className="h-6 bg-primary-200 rounded w-64 mb-4"></div>
            <div className="h-48 bg-primary-200 rounded-lg"></div>
          </div>
        </div>
      </MainLayout>
    );
  }

  if (error || !course || !assignment) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-4xl px-4 py-4">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-600">{error || 'Không tìm thấy bài tập'}</p>
          </div>
          <Link href={`/student/course/${resolvedParams.id}`} className="text-primary hover:underline">← Quay lại khóa học</Link>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="mx-auto max-w-4xl px-4 py-4">
        {/* breadcrumb-like header */}
        <div className="flex items-center gap-2 text-xs text-primary-400 mb-4">
          <Link href="/student" className="hover:underline">Trang chủ</Link>
          <span>/</span>
          <Link href={`/student/course/${course.id}`} className="hover:underline">{course.name}</Link>
          <span className="ml-auto">
            <span className={`inline-block rounded px-2 py-0.5 text-[11px] font-medium ${
              assignment.type === 'EXERCISE' ? 'bg-blue-600/90 text-white' :
              assignment.type === 'EXAM' ? 'bg-red-600/90 text-white' :
              assignment.type === 'PROJECT' ? 'bg-orange-600/90 text-white' :
              assignment.type === 'QUIZ' ? 'bg-green-600/90 text-white' :
              'bg-gray-600/90 text-white'
            }`}>
              {assignment.type === 'EXERCISE' ? 'Bài tập' :
               assignment.type === 'EXAM' ? 'Bài thi' :
               assignment.type === 'PROJECT' ? 'Dự án' :
               assignment.type === 'QUIZ' ? 'Kiểm tra nhanh' : assignment.type}
            </span>
          </span>
        </div>

        <div className="rounded-md border border-primary-200 bg-white">
          <div className="p-4 border-b border-primary-100">
            <div className="text-sm text-primary-600 font-medium">
              Bài tập lập trình
            </div>
            <h1 className="text-xl font-semibold mt-2 text-primary">{assignment.title}</h1>
            <div className="mt-3 grid grid-cols-2 md:grid-cols-3 gap-3 text-sm text-primary-500">
              <div>
                <span className="font-semibold text-primary">Điểm tối đa:</span> {assignment.maxScore}
              </div>
              <div>
                <span className="font-semibold text-primary">Thời gian:</span> {assignment.timeLimit} phút
              </div>
              <div>
                <span className="font-semibold text-primary">Số câu hỏi:</span> {assignment.totalQuestions || 1}
              </div>
              {assignment.endTime && (
                <div className="col-span-2 md:col-span-3">
                  <span className="font-semibold text-primary">Hạn nộp:</span> {formatDate(assignment.endTime)}
                </div>
              )}
            </div>
          </div>

          <div className="p-4">
            <h2 className="text-primary font-semibold mb-3">Trạng thái bài nộp</h2>
            <div className="grid md:grid-cols-2 gap-4 text-sm">
              <div className="rounded-md border border-primary-200 bg-primary-50/50 p-4">
                <div className="space-y-2">
                  <div>
                    <div className="text-primary-400 text-xs">Trạng thái bài nộp</div>
                    <div className="font-medium text-primary mt-1">
                      {assignment.isSubmitted ? 'Đã nộp bài' : 'Chưa nộp bài'}
                    </div>
                  </div>
                  <div>
                    <div className="text-primary-400 text-xs">Điểm tốt nhất</div>
                    <div className="font-semibold text-primary-600 text-lg mt-1">
                      {assignment.currentScore !== undefined && assignment.currentScore !== null 
                        ? `${assignment.currentScore}/${assignment.maxScore}` 
                        : 'Chưa có điểm'}
                    </div>
                  </div>
                  {assignment.submissionTime && (
                    <div>
                      <div className="text-primary-400 text-xs">Thời gian nộp</div>
                      <div className="text-primary-600 text-sm mt-1">{formatDate(assignment.submissionTime)}</div>
                    </div>
                  )}
                </div>
              </div>
              
              <div className="rounded-md border border-primary-200 bg-primary-50/50 p-4">
                <div className="space-y-2">
                  <div>
                    <div className="text-primary-400 text-xs">Trạng thái chấm điểm</div>
                    <div className="font-medium text-primary mt-1">
                      {assignment.submissionStatus === 'GRADED' ? 'Đã chấm điểm' : 
                       assignment.submissionStatus === 'SUBMITTED' ? 'Chờ chấm điểm' : 'Chưa chấm điểm'}
                    </div>
                  </div>
                  <div>
                    <div className="text-primary-400 text-xs">Tạo lúc</div>
                    <div className="text-primary-600 text-sm mt-1">{formatDate(assignment.createdAt)}</div>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-6 flex items-center justify-center">
              <Link 
                href={`/student/course/${course.id}/assignment/${assignment.id}/attempt`} 
                className="h-11 inline-flex items-center justify-center rounded-md bg-primary px-6 text-white text-sm font-medium hover:bg-primary-600 transition-colors shadow-sm"
              >
                Bắt đầu làm bài
              </Link>
            </div>
          </div>
        </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(AssignmentDetails, {
  requiredRoles: [Role.STUDENT, Role.TEACHER, Role.ADMIN],
});
