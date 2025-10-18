"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { use } from "react";
import { CourseService } from "@/services/course.service";
import { AssignmentService } from "@/services/assignment.service";
import { SectionService } from "@/services/section.service";
import { CourseResponse, StudentAssignmentResponse, SectionResponse } from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";

type Props = { params: Promise<{ id: string }> };

function CourseDetails({ params }: Props) {
  const resolvedParams = use(params);
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [assignments, setAssignments] = useState<StudentAssignmentResponse[]>([]);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [collapsedSections, setCollapsedSections] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null); // Clear previous errors
      
      // Fetch course details and assignments
      const courseId = parseInt(resolvedParams.id);
      
      // Fetch enrolled courses first
      const enrolledCourses = await CourseService.getEnrolledCourses();

      // Find the specific course from enrolled courses
      const courseData = enrolledCourses.find(c => c.id === courseId);
      if (!courseData) {
        throw new Error('Không tìm thấy khóa học hoặc bạn chưa đăng ký khóa học này');
      }

      setCourse(courseData);

      // Fetch sections for this course using student endpoint
      try {
        const courseSections = await fetch(`http://localhost:8086/api/student/courses/${courseId}/sections`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
          }
        }).then(res => {
          if (!res.ok) throw new Error('Failed to fetch sections');
          return res.json();
        });
        setSections(courseSections);
        
        // Initialize collapsed state from section data
        const initialCollapsed = new Set<number>();
        courseSections.forEach((section: SectionResponse) => {
          if (section.isCollapsed) {
            initialCollapsed.add(section.id);
          }
        });
        setCollapsedSections(initialCollapsed);
      } catch (sectionError) {
        console.error('Failed to fetch sections:', sectionError);
        setSections([]);
      }

      // Fetch assignments for this specific course (separate to handle errors better)
      try {
        const allAssignments = await AssignmentService.getAssignmentsForStudent();
        const courseAssignments = allAssignments.filter(assignment => 
          assignment.courseName === courseData.name
        );
        setAssignments(courseAssignments);
      } catch (assignmentError) {
        console.error('Failed to fetch assignments:', assignmentError);
        // Don't fail the whole page if assignments fail to load
        setAssignments([]);
      }
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

  const toggleSectionCollapse = (sectionId: number) => {
    setCollapsedSections(prev => {
      const newSet = new Set(prev);
      if (newSet.has(sectionId)) {
        newSet.delete(sectionId);
      } else {
        newSet.add(sectionId);
      }
      return newSet;
    });
  };

    const renderAssignmentCard = (assignment: StudentAssignmentResponse) => (
    <Link
      href={`/student/assignment/${assignment.id}`}
      key={assignment.id}
      className="block bg-white border border-slate-200 rounded-lg p-3 hover:shadow-md transition-all hover:border-slate-300"
    >
      <div className="flex justify-between items-start">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <h3 className="font-semibold text-slate-900 text-sm">{assignment.title}</h3>
            {assignment.isSubmitted && (
              <span className="px-2 py-0.5 bg-green-100 text-green-700 rounded text-xs font-medium">
                Đã nộp
              </span>
            )}
          </div>

          {assignment.description && (
            <p className="text-xs text-slate-600 mb-2">
              {assignment.description}
            </p>
          )}

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs text-slate-600">
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
            {assignment.currentScore !== undefined && (
              <div>
                <span className="font-medium">Điểm của bạn:</span>{" "}
                <span className="text-green-600 font-semibold">{assignment.currentScore}</span>
              </div>
            )}
          </div>

          {(assignment.startTime || assignment.endTime) && (
            <div className="mt-2 text-xs text-slate-600 space-y-0.5">
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
      </div>
    </Link>
  );

  if (loading) {
    return (
      <MainLayout>
        <div className="px-4 py-4">
          <div className="animate-pulse">
            <div className="h-6 bg-slate-200 rounded w-64 mb-4"></div>
            <div className="h-64 bg-slate-200 rounded-lg"></div>
          </div>
        </div>
      </MainLayout>
    );
  }

  if (error || !course) {
    return (
      <MainLayout>
        <div className="px-4 py-4">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-600">{error || 'Không tìm thấy khóa học'}</p>
          </div>
          <Link className="text-[#ff6a00] hover:underline" href="/student">← Quay lại danh sách khóa học</Link>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="px-4 py-4">
        <div className="mb-4">
          <h1 className="text-slate-900 font-semibold text-xl">{course.name}</h1>
          <p className="text-slate-600 text-sm mt-1">{course.code} - {course.description}</p>
        </div>

      <div>
        {/* Main content */}
        <div className="rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center gap-3 border-b border-slate-200 px-4 bg-slate-50">
            {(["Khóa học", "Danh sách thành viên", "Điểm số", "Năng lực"] as const).map((t, i) => (
              <button 
                key={i} 
                className={`h-11 px-3 text-sm font-medium transition-colors ${
                  i === 0 
                    ? "border-b-2 border-[#ff6a00] text-[#ff6a00]" 
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                {t}
              </button>
            ))}
          </div>
          <div className="p-3 space-y-3">
            {assignments.length === 0 ? (
              <div className="text-center py-8 text-slate-500">
                <p className="text-base">Chưa có bài tập nào trong khóa học này</p>
              </div>
            ) : (
              <>
                {/* Render sections with assignments */}
                {sections.map((section) => {
                  const sectionAssignments = assignments.filter(
                    (a: any) => a.sectionId === section.id
                  );
                  
                  if (sectionAssignments.length === 0) return null;
                  
                  const isCollapsed = collapsedSections.has(section.id);
                  
                  return (
                    <div key={section.id} className="border border-slate-200 rounded-lg overflow-hidden bg-white">
                      {/* Section Header */}
                      <div className="bg-slate-50 border-b border-slate-200">
                        <div
                          onClick={() => toggleSectionCollapse(section.id)}
                          className="flex items-center justify-between p-2.5 cursor-pointer hover:bg-slate-100 transition-colors"
                        >
                          <div className="flex items-center gap-2">
                            {/* Toggle Collapse Button */}
                            <button
                              className="p-1 hover:bg-slate-200 rounded transition-colors"
                              aria-label={isCollapsed ? "Mở rộng" : "Thu gọn"}
                            >
                              <svg
                                width="16"
                                height="16"
                                viewBox="0 0 16 16"
                                fill="currentColor"
                                className={`transition-transform duration-200 ${
                                  isCollapsed ? "-rotate-90" : ""
                                }`}
                              >
                                <path d="M4 6l4 4 4-4H4z" />
                              </svg>
                            </button>
                            
                            <div>
                              <h3 className="font-semibold text-slate-900 text-sm">
                                {section.name}
                                <span className="ml-2 text-xs text-slate-500 font-normal">
                                  ({sectionAssignments.length} bài tập)
                                </span>
                              </h3>
                              {section.description && (
                                <p className="text-xs text-slate-600 mt-0.5">
                                  {section.description}
                                </p>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* Section Content - Assignments */}
                      {!isCollapsed && (
                        <div className="p-2.5 space-y-2.5 bg-white">
                          {sectionAssignments.map((assignment) =>
                            renderAssignmentCard(assignment)
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}

                {/* Uncategorized assignments */}
                {(() => {
                  const uncategorizedAssignments = assignments.filter(
                    (a: any) => !a.sectionId
                  );
                  
                  if (uncategorizedAssignments.length === 0) return null;
                  
                  return (
                    <div className="space-y-2.5">
                      <h4 className="text-sm font-semibold text-slate-700 px-2 py-1 bg-slate-100 rounded">
                        Chưa phân loại ({uncategorizedAssignments.length})
                      </h4>
                      {uncategorizedAssignments.map((assignment) =>
                        renderAssignmentCard(assignment)
                      )}
                    </div>
                  );
                })()}
              </>
            )}
          </div>
        </div>
      </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(CourseDetails, {
  requiredRoles: [Role.STUDENT, Role.TEACHER, Role.ADMIN],
});
