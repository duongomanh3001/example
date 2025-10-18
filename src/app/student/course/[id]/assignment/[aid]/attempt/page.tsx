"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { use } from "react";
import { AssignmentService } from "@/services/assignment.service";
import { CourseService } from "@/services/course.service";
import { HealthService } from "@/services/health.service";
import { StudentAssignmentResponse, StudentQuestionResponse, CourseResponse } from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";
import ProgrammingQuestionComponent from "@/components/student/ProgrammingQuestionComponent";

type Props = { params: Promise<{ id: string; aid: string }> };

function AssignmentAttempt({ params }: Props) {
  const resolvedParams = use(params);
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [assignment, setAssignment] = useState<StudentAssignmentResponse | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [answers, setAnswers] = useState<{ [questionId: number]: { answer: string; selectedOptions?: number[] } }>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timeLeft, setTimeLeft] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [checkingQuestions, setCheckingQuestions] = useState<{ [questionId: number]: boolean }>({});
  const [testResults, setTestResults] = useState<{ [questionId: number]: any }>({});

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
        
        // Initialize time left if timeLimit is set
        if (assignmentData.timeLimit) {
          const storageKey = `assignment_${assignmentId}_start_time`;
          const storedStartTime = localStorage.getItem(storageKey);
          
          if (storedStartTime) {
            // Calculate remaining time based on stored start time
            const startTime = parseInt(storedStartTime);
            const elapsed = Math.floor((Date.now() - startTime) / 1000);
            const totalTime = assignmentData.timeLimit * 60;
            const remaining = Math.max(0, totalTime - elapsed);
            setTimeLeft(remaining);
            
            // Auto-submit if time is up
            if (remaining === 0) {
              handleAutoSubmit();
            }
          } else {
            // First time starting the assignment - save start time
            localStorage.setItem(storageKey, Date.now().toString());
            setTimeLeft(assignmentData.timeLimit * 60);
          }
        }

        // Initialize answers for all questions
        if (assignmentData.questions) {
          const initialAnswers: { [questionId: number]: { answer: string; selectedOptions?: number[] } } = {};
          assignmentData.questions.forEach(question => {
            initialAnswers[question.id] = {
              answer: question.userAnswer || '',
              selectedOptions: question.selectedOptionIds || []
            };
          });
          setAnswers(initialAnswers);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu');
        console.error('Failed to fetch assignment data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [resolvedParams.id, resolvedParams.aid]);

  // Timer effect
  useEffect(() => {
    if (timeLeft === null || timeLeft <= 0) return;

    const timer = setInterval(() => {
      setTimeLeft(prev => {
        if (prev && prev <= 1) {
          handleAutoSubmit();
          return 0;
        }
        return prev ? prev - 1 : 0;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [timeLeft]);

  const handleAutoSubmit = async () => {
    if (assignment) {
      // Clear the stored start time
      const storageKey = `assignment_${assignment.id}_start_time`;
      localStorage.removeItem(storageKey);
      await handleSubmit(true);
    }
  };

  const handleAnswerChange = (questionId: number, answer: string) => {
    setAnswers(prev => ({
      ...prev,
      [questionId]: { ...prev[questionId], answer }
    }));
    
    // Clear test results when code changes
    if (testResults[questionId]) {
      setTestResults(prev => {
        const newResults = { ...prev };
        delete newResults[questionId];
        return newResults;
      });
    }
  };

  const handleOptionSelect = (questionId: number, optionId: number, isMultiple = false) => {
    setAnswers(prev => {
      const currentAnswers = prev[questionId]?.selectedOptions || [];
      let newOptions: number[];
      
      if (isMultiple) {
        if (currentAnswers.includes(optionId)) {
          newOptions = currentAnswers.filter(id => id !== optionId);
        } else {
          newOptions = [...currentAnswers, optionId];
        }
      } else {
        newOptions = [optionId];
      }

      return {
        ...prev,
        [questionId]: { ...prev[questionId], selectedOptions: newOptions }
      };
    });
  };

  const handleSubmit = async (autoSubmit = false) => {
    if (!assignment) return;

    const confirmMessage = autoSubmit 
      ? 'Thời gian đã hết. Bài tập sẽ được nộp tự động.' 
      : 'Bạn có chắc chắn muốn nộp bài?';
      
    if (!autoSubmit && !confirm(confirmMessage)) return;

    try {
      setIsSubmitting(true);
      
      // Clear the stored start time when submitting
      const storageKey = `assignment_${assignment.id}_start_time`;
      localStorage.removeItem(storageKey);

      // Check if this is a programming assignment (has at least one PROGRAMMING question)
      const programmingQuestions = assignment.questions?.filter(q => q.questionType === 'PROGRAMMING') || [];
      const isProgrammingAssignment = programmingQuestions.length > 0;

      if (isProgrammingAssignment && assignment.questions) {
        // Programming assignment - validate and submit each question separately
        const programmingAnswers = programmingQuestions.map(q => ({
          questionId: q.id,
          answer: answers[q.id]?.answer || '',
          language: 'C' // Will be determined by content analysis
        })).filter(ans => ans.answer.trim());
        
        // Validate programming language consistency
        const detectedLanguages = new Set<string>();
        programmingAnswers.forEach(ans => {
          const code = ans.answer;
          
          // Enhanced language detection logic
          if (code.includes('public class') || code.includes('System.out.println') || code.includes('import java') || code.includes('String[]') || code.includes('public static void main')) {
            detectedLanguages.add('JAVA');
            ans.language = 'JAVA';
          } else if (code.includes('def ') || code.includes('print(') || code.includes('import ') && !code.includes('#include') || code.includes('->') || code.includes('str:') || code.includes('int:')) {
            detectedLanguages.add('PYTHON');
            ans.language = 'PYTHON';
          } else if (code.includes('#include') && (code.includes('iostream') || code.includes('vector') || code.includes('string>') || code.includes('cout') || code.includes('cin'))) {
            detectedLanguages.add('CPP');
            ans.language = 'CPP';
          } else if (code.includes('#include') && (code.includes('stdio.h') || code.includes('printf') || code.includes('scanf'))) {
            detectedLanguages.add('C');
            ans.language = 'C';
          } else {
            // Enhanced fallback detection
            if (/def\s+\w+\s*\(/.test(code) || /:\s*int/.test(code) || /:\s*str/.test(code)) {
              detectedLanguages.add('PYTHON');
              ans.language = 'PYTHON';
            } else {
              detectedLanguages.add('C');
              ans.language = 'C';
            }
          }
        });

        // Allow mixed languages for different questions - each question can have its own language
        if (programmingAnswers.length === 0) {
          setError('Vui lòng nhập code cho ít nhất một câu hỏi trước khi nộp bài');
          setIsSubmitting(false);
          return;
        }

        // For programming assignment, send as legacy format (single code submission)
        // Use the first programming answer or combine all codes
        const primaryAnswer = programmingAnswers[0];
        const allCodes = programmingAnswers.map(ans => ans.answer).join('\n\n// --- Next Question ---\n\n');
        
        const submissionData = {
          assignmentId: assignment.id,
          code: allCodes,
          programmingLanguage: primaryAnswer.language || 'C'
        };

        const response = await AssignmentService.submitAssignment(submissionData);
        
        // Enhanced success message with score information for programming assignment
        if (response && response.score !== undefined) {
          const percentage = assignment.maxScore > 0 ? (response.score / assignment.maxScore) * 100 : 0;
          alert(`Nộp bài thành công!\n\nKết quả chấm điểm:\nĐiểm số: ${response.score.toFixed(2)}/${assignment.maxScore} (${percentage.toFixed(1)}%)\nTrạng thái: ${response.status === 'GRADED' ? 'Đã chấm điểm' : 'Đang xử lý'}`);
        } else {
          alert('Nộp bài thành công!');
        }
        
        // Redirect to results page after successful programming submission
        const submissionId = response?.id || '';
        window.location.href = `/student/course/${course?.id}/assignment/${assignment.id}/result${submissionId ? `?submissionId=${submissionId}` : ''}`;
      } else {
        // Traditional multi-question assignment - convert to legacy format
        const allAnswers = assignment.questions?.map(question => {
          const userAnswer = answers[question.id];
          if (userAnswer?.answer) {
            return `// Question ${question.id}: ${question.title || 'Question'}\n${userAnswer.answer}`;
          }
          return '';
        }).filter(ans => ans.trim()).join('\n\n// --- Next Question ---\n\n') || '';

        if (!allAnswers.trim()) {
          setError('Vui lòng nhập câu trả lời cho ít nhất một câu hỏi');
          setIsSubmitting(false);
          return;
        }
        
        const submissionData = {
          assignmentId: assignment.id,
          code: allAnswers,
          programmingLanguage: 'javascript' // Default language, can be made configurable
        };

        const response = await AssignmentService.submitAssignment(submissionData);
        
        // Enhanced success message with score information
        if (response && response.score !== undefined) {
          const percentage = assignment.maxScore > 0 ? (response.score / assignment.maxScore) * 100 : 0;
          alert(`Nộp bài thành công!\n\nKết quả chấm điểm:\nĐiểm số: ${response.score.toFixed(2)}/${assignment.maxScore} (${percentage.toFixed(1)}%)\nTrạng thái: ${response.status === 'GRADED' ? 'Đã chấm điểm' : 'Đang xử lý'}`);
        } else {
          alert('Nộp bài thành công!');
        }
      
        // Redirect to results page after successful submission
        const submissionId = response?.id || '';
        window.location.href = `/student/course/${course?.id}/assignment/${assignment.id}/result${submissionId ? `?submissionId=${submissionId}` : ''}`;
      }
    } catch (err) {
      console.error('Submission error:', err);
      
      let errorMessage = 'Có lỗi xảy ra khi nộp bài';
      
      if (err instanceof Error) {
        errorMessage = err.message;
      } else if (typeof err === 'string') {
        errorMessage = err;
      }
      
      // Add additional context for common issues
      if (errorMessage.includes('fetch') || errorMessage.includes('network') || errorMessage.includes('kết nối')) {
        errorMessage += '\n\nVui lòng kiểm tra:\n- Kết nối internet\n- Server có đang hoạt động không\n- Thử tải lại trang và nộp lại';
      }
      
      setError(errorMessage);
      setIsSubmitting(false);
    }
  };

  const handleCheckQuestion = async (questionId: number) => {
    const question = assignment?.questions?.find(q => q.id === questionId);
    if (!question || !answers[questionId]?.answer) {
      setError('Vui lòng nhập code trước khi kiểm tra');
      return;
    }

    try {
      setCheckingQuestions(prev => ({ ...prev, [questionId]: true }));
      
      const code = answers[questionId].answer;
      let language = 'C';
      
      // Detect programming language
      if (code.includes('public class') || code.includes('System.out.println') || code.includes('import java')) {
        language = 'JAVA';
      } else if (code.includes('print(') || code.includes('def ') || (code.includes('import ') && !code.includes('#include'))) {
        language = 'PYTHON';
      } else if (code.includes('#include') && (code.includes('iostream') || code.includes('vector') || code.includes('string>'))) {
        language = 'CPP';
      } else if (code.includes('#include') && (code.includes('stdio.h') || code.includes('printf') || code.includes('scanf'))) {
        language = 'C';
      }

      // Call API to check code with test cases
      const testResult = await AssignmentService.checkQuestionCode({
        questionId,
        code,
        language
      });
      
      setTestResults(prev => ({ ...prev, [questionId]: testResult }));
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi kiểm tra code');
      console.error('Error checking question:', err);
    } finally {
      setCheckingQuestions(prev => ({ ...prev, [questionId]: false }));
    }
  };

  const formatTime = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  const getQuestionTypeLabel = (type: string): string => {
    switch (type) {
      case 'PROGRAMMING': return 'Lập trình';
      case 'MULTIPLE_CHOICE': return 'Trắc nghiệm';
      case 'ESSAY': return 'Tự luận';
      case 'TRUE_FALSE': return 'Đúng/Sai';
      default: return type;
    }
  };

  if (loading) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-6">
          <div className="animate-pulse">
            <div className="h-6 bg-primary-200 rounded w-64 mb-2"></div>
            <div className="h-4 bg-primary-200 rounded w-48 mb-6"></div>
            <div className="text-primary-600">Đang tải bài tập...</div>
          </div>
        </div>
      </MainLayout>
    );
  }

  if (error || !course || !assignment) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-600">{error || 'Không tìm thấy bài tập'}</p>
          </div>
          <Link href={`/student/course/${resolvedParams.id}/assignment/${resolvedParams.aid}`} className="text-primary hover:underline">← Quay lại bài tập</Link>
        </div>
      </MainLayout>
    );
  }

  const questions = assignment.questions || [];
  const currentQuestion = questions[currentQuestionIndex];

  // If no questions are available, show a helpful message
  if (questions.length === 0) {
    return (
      <MainLayout>
        <div className="mx-auto max-w-7xl px-4 py-6">
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-8 text-center">
            <h3 className="text-lg font-semibold text-yellow-800 mb-2">Bài tập đang được xử lý</h3>
            <p className="text-yellow-700 mb-4">
              Bài tập này đang được chuyển đổi sang định dạng mới với nhiều câu hỏi.
            </p>
            <div className="text-sm text-yellow-600 space-y-2">
              <p><strong>Thông tin bài tập:</strong></p>
              <p>• Tiêu đề: {assignment.title}</p>
              <p>• Loại: {assignment.type}</p>
              <p>• Điểm tối đa: {assignment.maxScore}</p>
              <p>• Thời gian: {assignment.timeLimit} phút</p>
              {assignment.publicTestCases && assignment.publicTestCases.length > 0 && (
                <p>• Test cases: {assignment.publicTestCases.length} test cases công khai</p>
              )}
            </div>
            <div className="mt-6 space-x-4">
              <Link 
                href={`/student/course/${resolvedParams.id}/assignment/${resolvedParams.aid}`} 
                className="px-4 py-2 bg-primary text-white rounded hover:bg-primary-600 transition-colors"
              >
                ← Quay lại chi tiết bài tập
              </Link>
              <button 
                onClick={() => window.location.reload()} 
                className="px-4 py-2 bg-primary-600 text-white rounded hover:bg-primary-700 transition-colors"
              >
                Tải lại trang
              </button>
            </div>
          </div>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="mx-auto max-w-7xl px-4 py-6">
        {/* Header */}
        <div className="text-xs text-primary-400 flex items-center gap-2 mb-4">
          <Link href="/student" className="hover:underline hover:text-primary">Trang chủ</Link>
          <span>/</span>
          <Link href={`/student/course/${course.id}`} className="hover:underline hover:text-primary">{course.name}</Link>
          <span>/</span>
          <Link href={`/student/course/${course.id}/assignment/${assignment.id}`} className="hover:underline hover:text-primary">{assignment.title}</Link>
        </div>

        <div className="grid gap-6 grid-cols-1 lg:grid-cols-[1fr_300px]">
        {/* Left: Question and Answer Area */}
        <section className="rounded-md border bg-white">
          <div className="border-b bg-primary-50 text-primary rounded-t-md flex items-center justify-between p-3 text-sm">
            <div className="flex items-center gap-2">
              <span className="font-medium">Câu hỏi {currentQuestionIndex + 1}</span>
              <span className="text-primary-600">({getQuestionTypeLabel(currentQuestion.questionType)})</span>
              <span className={`text-xs px-2 py-1 rounded ${
                answers[currentQuestion.id]?.answer || (answers[currentQuestion.id]?.selectedOptions && answers[currentQuestion.id]?.selectedOptions!.length > 0)
                  ? 'bg-green-100 text-green-700' 
                  : 'bg-slate-100 text-slate-500'
              }`}>
                {answers[currentQuestion.id]?.answer || (answers[currentQuestion.id]?.selectedOptions && answers[currentQuestion.id]?.selectedOptions!.length > 0)
                  ? 'Đã trả lời' 
                  : 'Chưa trả lời'
                }
              </span>
            </div>
            <div className="text-sm text-primary-600">
              Điểm: {currentQuestion.points}
            </div>
          </div>

          <div className="p-4">
            <h3 className="font-semibold text-primary mb-3">{currentQuestion.title}</h3>
            <div className="text-sm text-primary-600 mb-4 whitespace-pre-wrap">
              {currentQuestion.description}
            </div>
          </div>

            {/* Answer Input based on question type */}
            {currentQuestion.questionType === 'PROGRAMMING' && (
              <div className="border-t border-gray-200">
                <ProgrammingQuestionComponent
                  question={currentQuestion}
                  answer={answers[currentQuestion.id]?.answer || ''}
                  onAnswerChange={(code: string) => handleAnswerChange(currentQuestion.id, code)}
                  onTestResults={(result: any) => setTestResults(prev => ({
                    ...prev,
                    [currentQuestion.id]: result
                  }))}
                  isChecking={checkingQuestions[currentQuestion.id] || false}
                  setIsChecking={(checking: boolean) => setCheckingQuestions(prev => ({
                    ...prev,
                    [currentQuestion.id]: checking
                  }))}
                  testResults={testResults[currentQuestion.id]}
                />
              </div>
            )}

            {currentQuestion.questionType === 'ESSAY' && (
              <div className="px-4 pb-4">
                <label className="block text-sm font-medium text-primary mb-2">
                  Câu trả lời của bạn:
                </label>
                <textarea
                  value={answers[currentQuestion.id]?.answer || ''}
                  onChange={(e) => handleAnswerChange(currentQuestion.id, e.target.value)}
                  placeholder="Nhập câu trả lời của bạn..."
                  className="w-full min-h-[150px] rounded border border-primary-200 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
                />
              </div>
            )}

            {(currentQuestion.questionType === 'MULTIPLE_CHOICE' || currentQuestion.questionType === 'TRUE_FALSE') && (
              <div className="px-4 pb-4">
                <label className="block text-sm font-medium text-primary mb-2">
                  Chọn đáp án:
                </label>
                <div className="space-y-2">
                  {currentQuestion.options.map((option, index) => (
                    <label key={option.id} className="flex items-center p-2 border border-primary-200 rounded hover:bg-primary-50 cursor-pointer">
                      <input
                        type={currentQuestion.questionType === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'}
                        name={`question_${currentQuestion.id}`}
                        checked={answers[currentQuestion.id]?.selectedOptions?.includes(option.id) || false}
                        onChange={() => handleOptionSelect(currentQuestion.id, option.id, currentQuestion.questionType === 'MULTIPLE_CHOICE')}
                        className="mr-3 text-primary focus:ring-primary"
                      />
                      <span className="text-sm text-primary-700">{option.optionText}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Navigation */}
            <div className="flex justify-between items-center px-4 pb-4 pt-4 border-t border-primary-200">
              <button
                onClick={() => setCurrentQuestionIndex(prev => Math.max(0, prev - 1))}
                disabled={currentQuestionIndex === 0}
                className="px-4 py-2 text-sm bg-primary-100 text-primary rounded hover:bg-primary-200 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                ← Câu trước
              </button>
              
              <span className="text-sm text-primary-600">
                {currentQuestionIndex + 1} / {questions.length}
              </span>
              
              <button
                onClick={() => setCurrentQuestionIndex(prev => Math.min(questions.length - 1, prev + 1))}
                disabled={currentQuestionIndex === questions.length - 1}
                className="px-4 py-2 text-sm bg-primary-100 text-primary rounded hover:bg-primary-200 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Câu sau →
              </button>
            </div>
        </section>

        {/* Right: Question Navigator */}
        <aside className="rounded-md border border-primary-200 bg-white p-4 h-min">
          {/* Timer */}
          {timeLeft !== null && (
            <div className="pb-4 mb-4 border-b border-primary-200">
              <div className="flex items-center justify-center gap-3">
                <div className="relative w-8 h-8">
                  <Image
                    src="/icon/hourglass.png"
                    alt="Timer"
                    width={32}
                    height={32}
                    className={`${timeLeft < 300 ? 'animate-pulse' : ''}`}
                  />
                </div>
                <span className={`text-2xl font-bold ${timeLeft < 300 ? 'text-red-600' : 'text-primary'}`}>
                  {formatTime(timeLeft)}
                </span>
              </div>
            </div>
          )}
          
          <div className="grid grid-cols-4 gap-2 mb-4">
            {questions.map((question, index) => {
              const isAnswered = answers[question.id]?.answer || 
                (answers[question.id]?.selectedOptions && answers[question.id]?.selectedOptions!.length > 0);
              const hasTestResult = testResults[question.id];
              const testPassed = hasTestResult?.success;
              
              return (
                <button
                  key={question.id}
                  onClick={() => setCurrentQuestionIndex(index)}
                  className={`h-8 w-8 rounded border text-sm font-medium relative ${
                    index === currentQuestionIndex
                      ? 'bg-primary text-white'
                      : testPassed
                      ? 'bg-green-200 text-green-800 border-green-400'
                      : hasTestResult && !testPassed
                      ? 'bg-red-200 text-red-800 border-red-400'
                      : isAnswered
                      ? 'bg-primary-100 text-primary border-primary-300'
                      : 'bg-white text-primary-600 border-primary-300 hover:bg-primary-50'
                  }`}
                >
                  {index + 1}
                  {/* Test result indicator */}
                  {hasTestResult && (
                    <div className={`absolute -top-1 -right-1 w-3 h-3 rounded-full ${
                      testPassed ? 'bg-green-500' : 'bg-red-500'
                    }`}>
                      <div className="w-full h-full flex items-center justify-center">
                        {testPassed ? (
                          <svg className="w-2 h-2 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path>
                          </svg>
                        ) : (
                          <svg className="w-2 h-2 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M6 18L18 6M6 6l12 12"></path>
                          </svg>
                        )}
                      </div>
                    </div>
                  )}
                </button>
              );
            })}
          </div>


          <div className="border-t border-primary-200 pt-4">
            
            <button
              onClick={() => handleSubmit()}
              disabled={isSubmitting}
              className="w-full bg-primary text-white px-4 py-2 rounded hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isSubmitting ? 'Đang nộp...' : 'Nộp bài'}
            </button>
          </div>
        </aside>
        </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(AssignmentAttempt, {
  requiredRoles: [Role.STUDENT, Role.TEACHER, Role.ADMIN],
});
