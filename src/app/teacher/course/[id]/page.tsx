"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { use } from "react";
import { useSearchParams } from "next/navigation";
import { CourseService } from "@/services/course.service";
import { AssignmentService } from "@/services/assignment.service";
import { SectionService } from "@/services/section.service";
import {
  CourseResponse,
  AssignmentResponse,
  SubmissionResponse,
  SectionResponse,
} from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";
import SectionItem from "@/components/teacher/SectionItem";

type Props = { params: Promise<{ id: string }> };

function TeacherCourseDetails({ params }: Props) {
  const resolvedParams = use(params);
  const courseId = parseInt(resolvedParams.id);
  const router = useRouter();
  const searchParams = useSearchParams();
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [assignments, setAssignments] = useState<AssignmentResponse[]>([]);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [submissions, setSubmissions] = useState<SubmissionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState("assignments");
  const [isCreatingSection, setIsCreatingSection] = useState(false);
  const [newSectionName, setNewSectionName] = useState("");
  const [newSectionDescription, setNewSectionDescription] = useState("");

  const fetchData = async () => {
    try {
      setLoading(true);

      const [teacherCourses, allAssignments, courseSections] = await Promise.all([
        CourseService.getTeacherCourses(),
        AssignmentService.getAllAssignments(),
        SectionService.getSectionsByCourse(courseId),
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

      console.log('📊 Course Assignments loaded:', courseAssignments);
      console.log('📊 Assignment section IDs:', courseAssignments.map(a => ({ id: a.id, title: a.title, sectionId: a.sectionId })));

      setCourse(courseData);
      setAssignments(courseAssignments);
      setSections(courseSections);
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
  }, [resolvedParams.id, searchParams]); // Add searchParams to trigger reload when URL changes

  if (loading) {
    return (
      <MainLayout>
        <div className="px-6 py-4">
          <div className="animate-pulse">
            <div className="h-6 bg-slate-200 rounded w-64 mb-6"></div>
            <div className="h-64 bg-slate-200 rounded-lg"></div>
          </div>
        </div>
      </MainLayout>
    );
  }

  if (error || !course) {
    return (
      <MainLayout>
        <div className="px-6 py-4">
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

  // Section handlers
  const handleCreateSection = async () => {
    if (!newSectionName.trim() || !course) return;

    try {
      await SectionService.createSection({
        courseId: course.id,
        name: newSectionName.trim(),
        description: newSectionDescription.trim() || undefined,
        orderIndex: sections.length,
      });
      setNewSectionName("");
      setNewSectionDescription("");
      setIsCreatingSection(false);
      fetchData();
    } catch (err) {
      alert("Có lỗi xảy ra khi tạo phân mục");
      console.error("Failed to create section:", err);
    }
  };

  const handleUpdateSection = async (
    sectionId: number,
    name: string,
    description?: string
  ) => {
    try {
      await SectionService.updateSection(sectionId, { name, description });
      fetchData();
    } catch (err) {
      alert("Có lỗi xảy ra khi cập nhật phân mục");
      console.error("Failed to update section:", err);
    }
  };

  const handleDeleteSection = async (sectionId: number) => {
    try {
      await SectionService.deleteSection(sectionId);
      fetchData();
    } catch (err) {
      alert("Có lỗi xảy ra khi xóa phân mục");
      console.error("Failed to delete section:", err);
    }
  };

  const handleToggleSectionCollapse = (sectionId: number) => {
    // Just update local state - no need to call backend for UI toggle
    setSections((prev) =>
      prev.map((s) =>
        s.id === sectionId ? { ...s, isCollapsed: !s.isCollapsed } : s
      )
    );
  };

  // RENDER ICON
  const tabs = [
    { id: "assignments", label: "Bài tập", icon: "/icon/list-check-solid-full.svg" },
    { id: "students", label: "Sinh viên", icon: "/icon/users-solid-full.svg" },
    { id: "grades", label: "Điểm số", icon: "/icon/star-solid-full.svg" },
    { id: "submissions", label: "Bài nộp", icon: "/icon/address-book-solid-full.svg" },
  ];

  // Render assignment card
  const renderAssignmentCard = (assignment: AssignmentResponse) => (
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
                  {new Date(assignment.startTime).toLocaleString("vi-VN")}
                </div>
              )}
              {assignment.endTime && (
                <div>
                  <span className="font-medium">Kết thúc:</span>{" "}
                  {new Date(assignment.endTime).toLocaleString("vi-VN")}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex flex-col gap-2 ml-4">
          <button
            onClick={() =>
              router.push(`/teacher/course/${courseId}/assignment/${assignment.id}`)
            }
            className="px-3 py-1 text-sm border border-slate-300 rounded-md hover:bg-slate-50 transition-colors"
          >
            Xem chi tiết
          </button>

          <button
            onClick={() => handleToggleStatus(assignment.id)}
            className={`px-3 py-1 text-sm border border-slate-300 rounded-md hover:bg-slate-50 transition-colors ${
              assignment.isActive ? "text-orange-600" : "text-green-600"
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
  );

  const renderAssignments = () => {
    // Group assignments by section
    const assignmentsBySection: { [key: string]: AssignmentResponse[] } = {
      uncategorized: assignments.filter((a) => !a.sectionId),
    };

    sections.forEach((section) => {
      assignmentsBySection[section.id] = assignments.filter(
        (a) => a.sectionId === section.id
      );
    });

    return (
      <div className="space-y-4">
        <div className="flex justify-between items-center">
          <h3 className="text-lg font-semibold text-slate-900">
            Bài tập trong khóa học
          </h3>
          <div className="flex gap-2">
            <button
              onClick={() => setIsCreatingSection(true)}
              className="bg-slate-600 text-white px-4 py-2 rounded-md hover:bg-slate-700 transition-colors text-sm"
            >
              + Tạo phân mục
            </button>
            <Link
              href="/teacher/assignment/create"
              className="bg-[#ff6a00] text-white px-4 py-2 rounded-md hover:bg-[#e55a00] transition-colors text-sm"
            >
              + Tạo bài tập mới
            </Link>
          </div>
        </div>

        {/* Create Section Form */}
        {isCreatingSection && (
          <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
            <h4 className="font-semibold text-slate-900 mb-3">
              Tạo phân mục mới
            </h4>
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Tên phân mục <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={newSectionName}
                  onChange={(e) => setNewSectionName(e.target.value)}
                  placeholder="Ví dụ: Bài Giảng, Hướng Dẫn Cài Đặt, Dữ Liệu..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  autoFocus
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Mô tả (tùy chọn)
                </label>
                <input
                  type="text"
                  value={newSectionDescription}
                  onChange={(e) => setNewSectionDescription(e.target.value)}
                  placeholder="Mô tả ngắn về phân mục này"
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                />
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleCreateSection}
                  disabled={!newSectionName.trim()}
                  className="px-4 py-2 bg-[#ff6a00] text-white rounded-md hover:bg-[#e55a00] transition-colors text-sm disabled:bg-slate-300 disabled:cursor-not-allowed"
                >
                  Tạo phân mục
                </button>
                <button
                  onClick={() => {
                    setIsCreatingSection(false);
                    setNewSectionName("");
                    setNewSectionDescription("");
                  }}
                  className="px-4 py-2 bg-slate-200 text-slate-700 rounded-md hover:bg-slate-300 transition-colors text-sm"
                >
                  Hủy
                </button>
              </div>
            </div>
          </div>
        )}

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
          <div className="space-y-4">
            {/* Render sections */}
            {sections.map((section) => (
              <SectionItem
                key={section.id}
                section={section}
                assignments={assignmentsBySection[section.id] || []}
                onUpdateSection={handleUpdateSection}
                onDeleteSection={handleDeleteSection}
                onToggleCollapse={handleToggleSectionCollapse}
                renderAssignmentCard={renderAssignmentCard}
              />
            ))}

            {/* Uncategorized assignments */}
            {assignmentsBySection.uncategorized.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-sm font-semibold text-slate-600 px-2">
                  Chưa phân loại ({assignmentsBySection.uncategorized.length})
                </h4>
                {assignmentsBySection.uncategorized.map((assignment) =>
                  renderAssignmentCard(assignment)
                )}
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

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
      <div className="px-6 py-4">
        {/* Header */}
        <div className="mb-4">
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

        <div>
          {/* Main content */}
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
        </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(TeacherCourseDetails, {
  requiredRoles: [Role.TEACHER],
});
