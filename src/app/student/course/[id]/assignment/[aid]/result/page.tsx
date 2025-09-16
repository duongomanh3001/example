"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { use } from "react";
import { AssignmentService } from "@/services/assignment.service";
import { CourseService } from "@/services/course.service";
import { StudentAssignmentResponse, CourseResponse, SubmissionResponse, QuestionResultResponse } from "@/types/api";
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

export default function ResultPage({ params }: Props) {
  const resolvedParams = use(params);
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [assignment, setAssignment] = useState<StudentAssignmentResponse | null>(null);
  const [submissions, setSubmissions] = useState<SubmissionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        const courseId = parseInt(resolvedParams.id);
        const assignmentId = parseInt(resolvedParams.aid);
        
        const [courseData, assignmentData, submissionsData] = await Promise.all([
          CourseService.getStudentCourseById(courseId),
          AssignmentService.getAssignmentForStudent(assignmentId),
          AssignmentService.getMySubmissions(),
        ]);

        setCourse(courseData);
        setAssignment(assignmentData);
        
        // Filter submissions for this assignment
        const assignmentSubmissions = submissionsData.filter(s => s.assignmentId === assignmentId);
        setSubmissions(assignmentSubmissions);
        
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu');
        console.error('Failed to fetch result data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    
    // Auto refresh every 5 seconds to get updated grading results
    const intervalId = setInterval(async () => {
      try {
        const submissionsData = await AssignmentService.getMySubmissions();
        const assignmentId = parseInt(resolvedParams.aid);
        const assignmentSubmissions = submissionsData.filter(s => s.assignmentId === assignmentId);
        
        // Check if any submission is still being graded
        const hasProcessing = assignmentSubmissions.some(s => 
          s.status === 'SUBMITTED' || 
          s.status === 'GRADING' || 
          (s.score === null || s.score === undefined)
        );
        
        setSubmissions(assignmentSubmissions);
        
        // Stop polling if no submissions are being processed
        if (!hasProcessing) {
          clearInterval(intervalId);
        }
      } catch (err) {
        console.error('Failed to refresh submission data:', err);
      }
    }, 3000); // Check every 3 seconds

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [resolvedParams.id, resolvedParams.aid]);

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-6">
        <div className="animate-pulse">
          <div className="h-6 bg-slate-200 rounded w-64 mb-4"></div>
          <div className="h-64 bg-slate-200 rounded-lg"></div>
        </div>
      </div>
    );
  }

  if (error || !course || !assignment) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-10">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
          <p className="text-red-600">{error || 'Không tìm thấy kết quả bài tập'}</p>
        </div>
        <Link href={`/student/course/${resolvedParams.id}`} className="text-blue-600 hover:underline">← Quay lại khóa học</Link>
      </div>
    );
  }

  const latestSubmission = submissions.length > 0 ? submissions[submissions.length - 1] : null;

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      <div className="rounded-md border bg-white">
        <div className="p-4 border-b">
          <div className="text-sm text-rose-600 font-medium flex items-center gap-2">
            <span className="inline-flex h-6 w-6 items-center justify-center rounded bg-rose-600 text-white">📄</span>
            Bài tập
          </div>
          <h1 className="text-lg font-semibold mt-2">{assignment.title}</h1>
          <div className="mt-2 text-sm space-y-1 text-slate-700">
            <div><span className="font-semibold">Khóa học:</span> {assignment.courseName}</div>
            <div><span className="font-semibold">Loại:</span> {
              assignment.type === 'EXERCISE' ? 'Bài tập' :
              assignment.type === 'EXAM' ? 'Bài thi' :
              assignment.type === 'PROJECT' ? 'Dự án' :
              assignment.type === 'QUIZ' ? 'Kiểm tra nhanh' : assignment.type
            }</div>
            {assignment.startTime && (
              <div><span className="font-semibold">Bắt đầu:</span> {formatDate(assignment.startTime)}</div>
            )}
            {assignment.endTime && (
              <div><span className="font-semibold">Hạn nộp:</span> {formatDate(assignment.endTime)}</div>
            )}
          </div>
        </div>

        <div className="p-4">
          <h2 className="text-rose-600 font-semibold mb-4">Kết quả bài nộp</h2>
          
          {submissions.length === 0 ? (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <p className="text-yellow-800">Chưa có bài nộp nào.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Latest Submission Summary */}
              {latestSubmission && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <h3 className="font-semibold text-blue-900 mb-2 flex items-center gap-2">
                    Bài nộp mới nhất
                    {(latestSubmission.status === 'SUBMITTED' || latestSubmission.status === 'GRADING') && (
                      <div className="animate-spin h-4 w-4 border-2 border-blue-600 border-t-transparent rounded-full"></div>
                    )}
                  </h3>
                  <div className="grid md:grid-cols-2 gap-4 text-sm">
                    <div className="space-y-2">
                      <div>
                        <div className="text-slate-600">Trạng thái</div>
                        <div className={`font-medium ${
                          latestSubmission.status === 'PASSED' ? 'text-green-600' :
                          latestSubmission.status === 'FAILED' ? 'text-red-600' :
                          latestSubmission.status === 'PARTIAL' ? 'text-yellow-600' :
                          latestSubmission.status === 'SUBMITTED' || latestSubmission.status === 'GRADING' ? 'text-blue-600' :
                          latestSubmission.status === 'NO_TESTS' ? 'text-gray-600' :
                          latestSubmission.status === 'COMPILATION_ERROR' ? 'text-red-600' :
                          latestSubmission.status === 'ERROR' ? 'text-red-600' :
                          'text-gray-600'
                        }`}>
                          {latestSubmission.status === 'PASSED' ? 'Đạt' :
                           latestSubmission.status === 'FAILED' ? 'Không đạt' :
                           latestSubmission.status === 'PARTIAL' ? 'Đạt một phần' :
                           latestSubmission.status === 'SUBMITTED' ? 'Đang chấm' :
                           latestSubmission.status === 'GRADING' ? 'Đang chấm' :
                           latestSubmission.status === 'NO_TESTS' ? 'Chưa có test case' :
                           latestSubmission.status === 'COMPILATION_ERROR' ? 'Lỗi biên dịch' :
                           latestSubmission.status === 'ERROR' ? 'Lỗi hệ thống' :
                           latestSubmission.status}
                        </div>
                      </div>
                      <div>
                        <div className="text-slate-600">Điểm số</div>
                        <div className="font-medium">
                          {latestSubmission.status === 'SUBMITTED' || latestSubmission.status === 'GRADING' ? (
                            <span className="text-blue-600">Đang chấm điểm...</span>
                          ) : latestSubmission.score !== undefined && latestSubmission.score !== null ? 
                            `${latestSubmission.score.toFixed(1)} / ${assignment.maxScore}` : 
                            'Chưa có điểm'
                          }
                        </div>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <div>
                        <div className="text-slate-600">Ngôn ngữ</div>
                        <div className="font-medium">{latestSubmission.programmingLanguage}</div>
                      </div>
                      <div>
                        <div className="text-slate-600">Thời gian nộp</div>
                        <div className="font-medium">{formatDate(latestSubmission.submissionTime)}</div>
                      </div>
                    </div>
                  </div>
                  
                  {/* Detailed Score Breakdown */}
                  {latestSubmission.questionResults && latestSubmission.questionResults.length > 0 && (
                    <div className="mt-4">
                      <h4 className="text-slate-900 font-medium mb-3">Chi tiết điểm từng câu:</h4>
                      <div className="space-y-3">
                        {latestSubmission.questionResults.map((questionResult) => (
                          <div key={questionResult.questionId} className="border border-slate-200 rounded-lg p-3">
                            <div className="flex items-center justify-between mb-2">
                              <h5 className="font-medium text-slate-800">{questionResult.questionTitle}</h5>
                              <div className="flex items-center gap-2">
                                <span className={`px-2 py-1 rounded text-xs font-medium ${
                                  questionResult.status === 'CORRECT' ? 'bg-green-100 text-green-800' :
                                  questionResult.status === 'PARTIAL' ? 'bg-yellow-100 text-yellow-800' :
                                  questionResult.status === 'INCORRECT' ? 'bg-red-100 text-red-800' :
                                  'bg-gray-100 text-gray-800'
                                }`}>
                                  {questionResult.status === 'CORRECT' ? 'Đúng' :
                                   questionResult.status === 'PARTIAL' ? 'Đúng một phần' :
                                   questionResult.status === 'INCORRECT' ? 'Sai' :
                                   'Chưa trả lời'}
                                </span>
                                <span className="text-sm font-medium">
                                  {questionResult.earnedScore.toFixed(1)}/{questionResult.maxScore} điểm
                                </span>
                              </div>
                            </div>
                            
                            {questionResult.questionType === 'PROGRAMMING' && questionResult.testCaseResults && (
                              <div className="mt-2">
                                {/* Auto-grading explanation */}
                                <div className="mb-3 p-2 bg-blue-50 border border-blue-200 rounded text-xs">
                                  <span className="font-medium text-blue-900">📋 Quy trình chấm điểm tự động:</span>
                                  <span className="text-blue-800 ml-1">
                                    Code của bạn được chạy với test cases từ giảng viên và so sánh output thực tế với expected output.
                                  </span>
                                </div>
                                <div className="text-sm text-slate-600 mb-2">
                                  Kết quả test cases: {questionResult.testCaseResults.filter(t => t.passed).length}/{questionResult.testCaseResults.length} đạt yêu cầu
                                </div>
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                                  {questionResult.testCaseResults.map((testCase, index) => (
                                    <div key={testCase.id} className={`p-2 rounded text-xs border ${
                                      testCase.passed ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
                                    }`}>
                                      <div className="flex items-center justify-between mb-1">
                                        <span className="font-medium">Test {index + 1}</span>
                                        <div className={`w-3 h-3 rounded-full ${
                                          testCase.passed ? 'bg-green-500' : 'bg-red-500'
                                        }`}></div>
                                      </div>
                                      <div className="space-y-1">
                                        <div>
                                          <span className="text-slate-600">Input:</span>
                                          <div className="font-mono bg-white p-1 rounded border">
                                            {testCase.input || '(empty)'}
                                          </div>
                                        </div>
                                        <div>
                                          <span className="text-slate-600">Expected:</span>
                                          <div className="font-mono bg-white p-1 rounded border">
                                            {testCase.expectedOutput}
                                          </div>
                                        </div>
                                        {testCase.actualOutput !== undefined && (
                                          <div>
                                            <span className="text-slate-600">Got:</span>
                                            <div className={`font-mono p-1 rounded border ${
                                              testCase.passed ? 'bg-green-50' : 'bg-red-50'
                                            }`}>
                                              {testCase.actualOutput || '(no output)'}
                                            </div>
                                          </div>
                                        )}
                                        {testCase.error && (
                                          <div>
                                            <span className="text-red-600">Error:</span>
                                            <div className="font-mono bg-red-50 p-1 rounded border border-red-200 text-red-700">
                                              {testCase.error}
                                            </div>
                                          </div>
                                        )}
                                      </div>
                                    </div>
                                  ))}
                                </div>
                              </div>
                            )}
                            
                            {questionResult.feedback && (
                              <div className="mt-2 p-2 bg-slate-50 rounded border">
                                <div className="text-xs text-slate-600 mb-1">Feedback:</div>
                                <div className="text-sm text-slate-800">{questionResult.feedback}</div>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {latestSubmission.feedback && (
                    <div className="mt-4">
                      <div className="text-slate-600 text-sm">Feedback tổng:</div>
                      <div className="mt-1 p-3 bg-white border rounded text-sm">
                        {latestSubmission.feedback}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* All Submissions */}
              <div className="bg-white border rounded-lg">
                <div className="p-4 border-b">
                  <h3 className="font-semibold">Tất cả bài nộp ({submissions.length})</h3>
                </div>
                <div className="divide-y">
                  {submissions.map((submission, index) => (
                    <div key={submission.id} className="p-4">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <span className="text-sm text-slate-500">#{submissions.length - index}</span>
                          <span className={`px-2 py-1 rounded text-xs font-medium ${
                            submission.status === 'PASSED' ? 'bg-green-100 text-green-800' :
                            submission.status === 'FAILED' ? 'bg-red-100 text-red-800' :
                            submission.status === 'PARTIAL' ? 'bg-yellow-100 text-yellow-800' :
                            submission.status === 'SUBMITTED' || submission.status === 'GRADING' ? 'bg-blue-100 text-blue-800' :
                            submission.status === 'NO_TESTS' ? 'bg-gray-100 text-gray-800' :
                            submission.status === 'COMPILATION_ERROR' || submission.status === 'ERROR' ? 'bg-red-100 text-red-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {submission.status === 'PASSED' ? 'Đạt' :
                             submission.status === 'FAILED' ? 'Không đạt' :
                             submission.status === 'PARTIAL' ? 'Đạt một phần' :
                             submission.status === 'SUBMITTED' ? 'Đang chấm' :
                             submission.status === 'GRADING' ? 'Đang chấm' :
                             submission.status === 'NO_TESTS' ? 'Chưa có test case' :
                             submission.status === 'COMPILATION_ERROR' ? 'Lỗi biên dịch' :
                             submission.status === 'ERROR' ? 'Lỗi hệ thống' :
                             submission.status === 'COMPILE_ERROR' ? 'Lỗi biên dịch' :
                             submission.status === 'RUNTIME_ERROR' ? 'Lỗi runtime' :
                             submission.status}
                          </span>
                        </div>
                        <div className="flex items-center gap-4 text-sm text-slate-600">
                          <span>{submission.score !== undefined ? `${submission.score.toFixed(1)}/${assignment.maxScore}` : 'N/A'}</span>
                          <span>{formatDate(submission.submissionTime)}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          <div className="mt-6 flex gap-3">
            <Link 
              href={`/student/course/${course.id}/assignment/${assignment.id}/attempt`}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            >
              Làm lại
            </Link>
            <Link 
              href={`/student/course/${course.id}`}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded hover:bg-gray-50 transition-colors"
            >
              Quay lại khóa học
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
