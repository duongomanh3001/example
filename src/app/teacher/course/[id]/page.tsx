"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { use } from "react";
import { CourseService } from "@/services/course.service";
import { AssignmentService } from "@/services/assignment.service";
import {
  CourseResponse,
  AssignmentResponse,
  SubmissionResponse,
} from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";
import ViewToggle from "@/components/common/ViewToggle";

type Props = { params: Promise<{ id: string }> };

function TeacherCourseDetails({ params }: Props) {
  const resolvedParams = use(params);
  const [currentView, setCurrentView] = useState<"teacher" | "student">(
    "teacher"
  );
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [assignments, setAssignments] = useState<AssignmentResponse[]>([]);
  const [submissions, setSubmissions] = useState<SubmissionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState("assignments");

  const fetchData = async () => {
    try {
      setLoading(true);

      const courseId = parseInt(resolvedParams.id);

      if (currentView === "teacher") {
        const [teacherCourses, allAssignments] = await Promise.all([
          CourseService.getTeacherCourses(),
          AssignmentService.getAllAssignments(),
        ]);

        const courseData = teacherCourses.find((c) => c.id === courseId);
        if (!courseData) {
          throw new Error(
            "Không tìm thấy khóa học hoặc bạn không có quyền truy cập khóa học này"
          );
        }

        const courseAssignments = allAssignments.filter(
          (assignment) => assignment.courseId === courseId
        );

        setCourse(courseData);
        setAssignments(courseAssignments);
      } else {
        window.location.href = `/student/course/${courseId}`;
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Có lỗi xảy ra khi tải dữ liệu"
      );
      console.error("Failed to fetch course data:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [resolvedParams.id, currentView]);

  if (loading) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-6">
          <div className="animate-pulse">
            <div className="h-6 bg-slate-200 rounded w-64 mb-6"></div>
            <div className="grid gap-6 md:grid-cols-[240px_1fr]">
              <div className="h-64 bg-slate-200 rounded-lg"></div>
              <div className="h-64 bg-slate-200 rounded-lg"></div>
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
            <p className="text-red-600">
              {error || "Không tìm thấy khóa học"}
            </p>
          </div>
          <Link
            className="text-blue-600 hover:underline"
            href="/teacher"
          >
            ← Quay lại danh sách khóa học
          </Link>
        </div>
      </MainLayout>
    );
  }

  const handleDeleteAssignment = async (assignmentId: number) => {
    if (!confirm("Bạn có chắc chắn muốn xóa bài tập này không?")) return;

    try {
      await AssignmentService.deleteAssignment(assignmentId);
      fetchData();
      alert("Đã xóa bài tập thành công!");
    } catch (err) {
      alert("Có lỗi xảy ra khi xóa bài tập");
      console.error("Failed to delete assignment:", err);
    }
  };

  const handleToggleStatus = async (assignmentId: number) => {
    try {
      await AssignmentService.toggleAssignmentStatus(assignmentId);
      fetchData();
    } catch (err) {
      alert("Có lỗi xảy ra khi thay đổi trạng thái bài tập");
      console.error("Failed to toggle assignment status:", err);
    }
  };

  // RENDER ICON
  const tabs = [
    { id: "assignments", label: "Bài tập", icon: "/icon/list-check-solid-full.svg" },
    { id: "students", label: "Sinh viên", icon: "/icon/users-solid-full.svg" },
    { id: "grades", label: "Điểm số", icon: "/icon/star-solid-full.svg" },
    { id: "submissions", label: "Bài nộp", icon: "/icon/address-book-solid-full.svg" },
  ];

  const renderAssignments = () => (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-slate-900">
          Bài tập trong khóa học
        </h3>
        <Link
          href="/teacher/assignment/create"
          className="bg-[#ff6a00] text-white px-4 py-2 rounded-md hover:bg-[#e55a00] transition-colors text-sm"
        >
          + Tạo bài tập mới
        </Link>
      </div>

      {assignments.length === 0 ? (
        <div className="bg-slate-50 border border-slate-200 rounded-lg p-8 text-center">
          <div className="mb-4 flex justify-center">
            <Image
              src="/icon/list-check-solid-full.svg"
              alt="empty"
              width={40}
              height={40}
            />
          </div>
          <h3 className="text-lg font-medium text-slate-900 mb-2">
            Chưa có bài tập nào
          </h3>
          <p className="text-slate-500 mb-4">
            Bạn chưa tạo bài tập nào cho khóa học này.
          </p>
          <Link
            href="/teacher/assignment/create"
            className="bg-[#ff6a00] text-white px-4 py-2 rounded-md hover:bg-[#e55a00] transition-colors"
          >
            Tạo bài tập đầu tiên
          </Link>
        </div>
      ) : (
        <div className="space-y-3">
          {assignments.map((assignment) => (
            <div
              key={assignment.id}
              className="bg-white border border-slate-200 rounded-lg p-4"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <h4 className="font-semibold text-slate-900">
                      {assignment.title}
                    </h4>
                    <span
                      className={`text-xs px-2 py-1 rounded-full ${
                        assignment.type === "EXERCISE"
                          ? "bg-blue-100 text-blue-800"
                          : assignment.type === "EXAM"
                          ? "bg-red-100 text-red-800"
                          : assignment.type === "PROJECT"
                          ? "bg-orange-100 text-orange-800"
                          : assignment.type === "QUIZ"
                          ? "bg-green-100 text-green-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      {assignment.type === "EXERCISE"
                        ? "Bài tập"
                        : assignment.type === "EXAM"
                        ? "Bài thi"
                        : assignment.type === "PROJECT"
                        ? "Dự án"
                        : assignment.type === "QUIZ"
                        ? "Kiểm tra nhanh"
                        : assignment.type}
                    </span>
                    <span
                      className={`text-xs px-2 py-1 rounded-full ${
                        assignment.isActive
                          ? "bg-green-100 text-green-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      {assignment.isActive ? "Hoạt động" : "Đã tắt"}
                    </span>
                  </div>

                  {assignment.description && (
                    <p className="text-sm text-slate-600 mb-2">
                      {assignment.description}
                    </p>
                  )}

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs text-slate-600">
                    <div>
                      <span className="font-medium">Điểm tối đa:</span>{" "}
                      {assignment.maxScore}
                    </div>
                    <div>
                      <span className="font-medium">Thời gian:</span>{" "}
                      {assignment.timeLimit} phút
                    </div>
                    <div>
                      <span className="font-medium">Câu hỏi:</span>{" "}
                      {assignment.totalQuestions}
                    </div>
                    <div>
                      <span className="font-medium">Bài nộp:</span>{" "}
                      {assignment.submissionCount || 0}
                    </div>
                  </div>

                  {(assignment.startTime || assignment.endTime) && (
                    <div className="mt-2 text-xs text-slate-600 space-y-1">
                      {assignment.startTime && (
                        <div>
                          <span className="font-medium">Bắt đầu:</span>{" "}
                          {new Date(
                            assignment.startTime
                          ).toLocaleString("vi-VN")}
                        </div>
                      )}
                      {assignment.endTime && (
                        <div>
                          <span className="font-medium">Kết thúc:</span>{" "}
                          {new Date(
                            assignment.endTime
                          ).toLocaleString("vi-VN")}
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <div className="flex flex-col gap-2 ml-4">
                  <button
                    onClick={() =>
                      window.open(`/teacher/assignment/${assignment.id}`, "_blank")
                    }
                    className="px-3 py-1 text-sm border border-slate-300 rounded-md hover:bg-slate-50 transition-colors"
                  >
                    Xem chi tiết
                  </button>

                  <button
                    onClick={() => handleToggleStatus(assignment.id)}
                    className={`px-3 py-1 text-sm border border-slate-300 rounded-md hover:bg-slate-50 transition-colors ${
                      assignment.isActive
                        ? "text-orange-600"
                        : "text-green-600"
                    }`}
                  >
                    {assignment.isActive ? "Tắt" : "Bật"}
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
          ))}
        </div>
      )}
    </div>
  );

  const renderStudents = () => (
    <div className="bg-white border border-slate-200 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">
        Danh sách sinh viên
      </h3>
      <div className="text-center py-8 text-slate-500">
        <div className="mb-4 flex justify-center">
          <Image src="/icon/users-solid-full.svg" alt="students" width={40} height={40} />
        </div>
        <p>Chức năng quản lý sinh viên đang được phát triển...</p>
        <div className="mt-4 text-sm text-slate-400">
          Hiện tại: {course.currentStudentCount || 0}/{course.maxStudents} sinh viên
        </div>
      </div>
    </div>
  );

  const renderGrades = () => (
    <div className="bg-white border border-slate-200 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">Bảng điểm</h3>
      <div className="text-center py-8 text-slate-500">
        <div className="mb-4 flex justify-center">
          <Image src="/icon/chart-bar-solid-full.svg" alt="grades" width={40} height={40} />
        </div>
        <p>Chức năng quản lý điểm số đang được phát triển...</p>
      </div>
    </div>
  );

  const renderSubmissions = () => (
    <div className="bg-white border border-slate-200 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">Bài nộp</h3>
      <div className="text-center py-8 text-slate-500">
        <div className="mb-4 flex justify-center">
          <Image src="/icon/file-arrow-up-solid-full.svg" alt="submissions" width={40} height={40} />
        </div>
        <p>Chức năng quản lý bài nộp đang được phát triển...</p>
      </div>
    </div>
  );

  return (
    <MainLayout>
      <div className="mx-auto max-w-7xl px-4 py-6">
        {/* Header */}
        <div className="mb-6 flex justify-between items-start">
          <div>
            <h1 className="text-[#ff6a00] font-semibold text-xl">
              {course.name}
            </h1>
            <p className="text-slate-600 text-sm mt-1">
              {course.code} - {course.description}
            </p>
            <p className="text-slate-500 text-xs mt-1">
              {course.semester}/{course.year} • {course.currentStudentCount || 0}/
              {course.maxStudents} sinh viên
            </p>
          </div>
          <ViewToggle currentView={currentView} onViewChange={setCurrentView} />
        </div>

        <div className="grid gap-6 grid-cols-1 md:grid-cols-[240px_1fr]">
          {/* Left sidebar */}
          <aside className="rounded-md border bg-white">
            <div className="p-4 border-b text-sm font-medium">{course.name}</div>
            <nav className="text-sm">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex items-center gap-2 w-full text-left px-4 py-2 hover:bg-slate-50 ${
                    activeTab === tab.id ? "bg-[#ff6a00] text-white" : ""
                  }`}
                >
                  <Image
                    src={tab.icon}
                    alt={tab.label}
                    width={16}
                    height={16}
                    className={`object-contain ${
                      activeTab === tab.id ? "invert" : ""
                    }`}
                  />
                  {tab.label}
                </button>
              ))}
            </nav>
          </aside>

          {/* Main content */}
          <section>
            <div className="rounded-md border bg-white">
              <div className="flex items-center gap-4 border-b px-4">
                {tabs.map((tab, i) => (
                  <button
                    key={i}
                    onClick={() => setActiveTab(tab.id)}
                    className={`flex items-center gap-2 h-10 px-3 text-sm ${
                      activeTab === tab.id
                        ? "border-b-2 border-[#ff6a00] text-[#ff6a00]"
                        : "text-slate-600"
                    }`}
                  >
                    <Image
                      src={tab.icon}
                      alt={tab.label}
                      width={16}
                      height={16}
                      className="object-contain"
                    />
                    {tab.label}
                  </button>
                ))}
              </div>

              <div className="p-4">
                {activeTab === "assignments" && renderAssignments()}
                {activeTab === "students" && renderStudents()}
                {activeTab === "grades" && renderGrades()}
                {activeTab === "submissions" && renderSubmissions()}
              </div>
            </div>
          </section>
        </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(TeacherCourseDetails, {
  requiredRoles: [Role.TEACHER],
});
