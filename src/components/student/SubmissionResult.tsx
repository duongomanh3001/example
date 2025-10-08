"use client";

import React, { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { AssignmentService } from '@/services';
import { SubmissionResponse, StudentAssignmentResponse, CourseResponse } from '@/types/api';

interface SubmissionResultProps {
  submissionId: number;
  assignmentId: number;
  courseId: number;
}

function SubmissionResult({ submissionId, assignmentId, courseId }: SubmissionResultProps) {
  const router = useRouter();
  const [submission, setSubmission] = useState<SubmissionResponse | null>(null);
  const [assignment, setAssignment] = useState<StudentAssignmentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchResults = async () => {
      try {
        setLoading(true);
        
        // Fetch submission details and assignment info
        const [submissionData, assignmentData] = await Promise.all([
          AssignmentService.getSubmissionDetails(submissionId),
          AssignmentService.getAssignmentForStudent(assignmentId),
        ]);

        setSubmission(submissionData);
        setAssignment(assignmentData);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải kết quả');
        console.error('Failed to fetch submission result:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchResults();
  }, [submissionId, assignmentId]);

  const formatScore = (score: number, maxScore: number): string => {
    return `${score.toFixed(2)}/${maxScore.toFixed(2)}`;
  };

  const getPercentage = (score: number, maxScore: number): number => {
    return maxScore > 0 ? (score / maxScore) * 100 : 0;
  };

  const getScoreColor = (percentage: number): string => {
    if (percentage >= 80) return 'text-green-600 bg-green-50 border-green-200';
    if (percentage >= 60) return 'text-yellow-600 bg-yellow-50 border-yellow-200';
    return 'text-red-600 bg-red-50 border-red-200';
  };

  const getStatusText = (status: string): string => {
    switch (status) {
      case 'GRADED': return 'Đã chấm điểm';
      case 'SUBMITTED': return 'Đã nộp';
      case 'GRADING': return 'Đang chấm điểm';
      case 'PASSED': return 'Đạt';
      case 'FAILED': return 'Không đạt';
      default: return status;
    }
  };

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-6">
        <div className="animate-pulse">
          <div className="h-6 bg-slate-200 rounded w-64 mb-2"></div>
          <div className="h-4 bg-slate-200 rounded w-48 mb-6"></div>
          <div className="text-slate-600">Đang tải kết quả...</div>
        </div>
      </div>
    );
  }

  if (error || !submission || !assignment) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
          <p className="text-red-600">{error || 'Không thể tải kết quả'}</p>
        </div>
        <button 
          onClick={() => router.back()} 
          className="text-blue-600 hover:underline"
        >
          ← Quay lại
        </button>
      </div>
    );
  }

  const totalScore = submission.score || 0;
  const maxScore = assignment.maxScore || 100;
  const percentage = getPercentage(totalScore, maxScore);
  const scoreColorClass = getScoreColor(percentage);

  return (
    <div className="mx-auto max-w-4xl px-4 py-6">
      {/* Header */}
      <div className="text-xs text-slate-500 flex items-center gap-2 mb-4">
        <button onClick={() => router.back()} className="hover:underline">
          ← Quay lại
        </button>
      </div>

      {/* Main Result Card */}
      <div className="bg-white rounded-lg border shadow-sm mb-6">
        <div className="p-6 border-b">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Kết quả: {assignment.title}
          </h1>
          <p className="text-gray-600">
            Nộp lúc: {new Date(submission.submissionTime).toLocaleString('vi-VN')}
          </p>
          {submission.gradedTime && (
            <p className="text-gray-600">
              Chấm điểm lúc: {new Date(submission.gradedTime).toLocaleString('vi-VN')}
            </p>
          )}
        </div>

        {/* Score Summary */}
        <div className="p-6">
          <div className={`rounded-lg border p-4 mb-6 ${scoreColorClass}`}>
            <div className="text-center">
              <div className="text-3xl font-bold mb-2">
                {formatScore(totalScore, maxScore)}
              </div>
              <div className="text-xl mb-2">
                {percentage.toFixed(1)}% - {getStatusText(submission.status)}
              </div>
              {submission.questionResults && (
                <div className="text-sm">
                  Đúng {submission.questionResults.filter(q => q.status === 'CORRECT').length}/{submission.questionResults.length} câu
                </div>
              )}
            </div>
          </div>

          {/* Question-wise Results */}
          {submission.questionResults && submission.questionResults.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Chi tiết từng câu hỏi</h3>
              {submission.questionResults.map((question, index) => {
                const questionPercentage = getPercentage(question.earnedScore, question.maxScore);
                const questionColorClass = getScoreColor(questionPercentage);
                
                return (
                  <div key={question.questionId} className="border rounded-lg p-4">
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <h4 className="font-semibold text-gray-900">
                          Câu {index + 1}: {question.questionTitle}
                        </h4>
                        <p className="text-sm text-gray-600">
                          Loại: {getQuestionTypeLabel(question.questionType)}
                        </p>
                      </div>
                      <div className={`px-3 py-1 rounded-full text-sm font-medium ${questionColorClass}`}>
                        {formatScore(question.earnedScore, question.maxScore)} ({questionPercentage.toFixed(1)}%)
                      </div>
                    </div>
                    
                    {question.feedback && (
                      <div className="bg-gray-50 rounded p-3 text-sm">
                        <strong>Phản hồi:</strong>
                        <pre className="mt-1 whitespace-pre-wrap">{question.feedback}</pre>
                      </div>
                    )}

                    {/* Test Results for Programming Questions */}
                    {question.testCaseResults && question.testCaseResults.length > 0 && (
                      <div className="mt-3">
                        <h5 className="font-medium text-gray-800 mb-2">Kết quả test cases:</h5>
                        <div className="space-y-2">
                          {question.testCaseResults.map((test, testIndex) => (
                            <div 
                              key={testIndex}
                              className={`p-2 rounded text-xs font-mono ${
                                test.passed 
                                  ? 'bg-green-50 text-green-800 border border-green-200' 
                                  : 'bg-red-50 text-red-800 border border-red-200'
                              }`}
                            >
                              <div className="flex justify-between items-center">
                                <span>Test case {testIndex + 1}</span>
                                <span className="font-semibold">
                                  {test.passed ? '✓ PASS' : '✗ FAIL'}
                                </span>
                              </div>
                              {test.input && (
                                <div className="mt-1">
                                  <strong>Input:</strong> {test.input}
                                </div>
                              )}
                              {test.expectedOutput && (
                                <div>
                                  <strong>Expected:</strong> {test.expectedOutput}
                                </div>
                              )}
                              {test.actualOutput && (
                                <div>
                                  <strong>Actual:</strong> {test.actualOutput}
                                </div>
                              )}
                              {test.error && (
                                <div className="text-red-600">
                                  <strong>Error:</strong> {test.error}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {/* Overall Feedback */}
          {submission.feedback && !submission.questionResults && (
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="font-semibold text-gray-900 mb-2">Phản hồi tổng quát</h3>
              <pre className="whitespace-pre-wrap text-sm">{submission.feedback}</pre>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex justify-center space-x-4 mt-6">
            <button
              onClick={() => router.push(`/student/course/${courseId}/assignment/${assignmentId}`)}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Xem bài tập
            </button>
            <button
              onClick={() => router.push(`/student/course/${courseId}`)}
              className="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
            >
              Về trang khóa học
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function getQuestionTypeLabel(type: string): string {
  switch (type) {
    case 'PROGRAMMING': return 'Lập trình';
    case 'MULTIPLE_CHOICE': return 'Trắc nghiệm';
    case 'ESSAY': return 'Tự luận';
    case 'TRUE_FALSE': return 'Đúng/Sai';
    default: return type;
  }
}

export default SubmissionResult;