import { 
  AssignmentResponse, 
  DetailedAssignmentResponse, 
  CreateAssignmentRequest, 
  UpdateAssignmentRequest,
  StudentAssignmentResponse,
  SubmissionRequest,
  SubmissionResponse,
  CodeExecutionResponse,
  QuestionResponse,
  CreateQuestionRequest,
  UpdateQuestionRequest,
  StudentQuestionResponse,
  TestCaseResponse
} from '@/types/api';
import { apiClient } from '@/lib/api-client';

class AssignmentServiceClass {
  async getAllAssignments(): Promise<AssignmentResponse[]> {
    const assignments = await apiClient.get<AssignmentResponse[]>('/api/teacher/assignments');
    return assignments.map(assignment => this.transformTeacherAssignment(assignment));
  }

  async getAssignmentsByTeacher(teacherId: number): Promise<AssignmentResponse[]> {
    const assignments = await apiClient.get<AssignmentResponse[]>('/api/teacher/assignments');
    return assignments.map(assignment => this.transformTeacherAssignment(assignment));
  }

  async getAssignmentsByCourse(courseId: number): Promise<AssignmentResponse[]> {
    const assignments = await apiClient.get<AssignmentResponse[]>(`/api/teacher/courses/${courseId}/assignments`);
    return assignments.map(assignment => this.transformTeacherAssignment(assignment));
  }

  async getAssignmentById(id: number): Promise<DetailedAssignmentResponse> {
    return apiClient.get<DetailedAssignmentResponse>(`/api/teacher/assignments/${id}`);
  }

  async createAssignment(assignmentData: CreateAssignmentRequest): Promise<DetailedAssignmentResponse> {
    return apiClient.post<DetailedAssignmentResponse>('/api/teacher/assignments', assignmentData);
  }

  async updateAssignment(id: number, assignmentData: UpdateAssignmentRequest): Promise<DetailedAssignmentResponse> {
    return apiClient.put<DetailedAssignmentResponse>(`/api/teacher/assignments/${id}`, assignmentData);
  }

  async deleteAssignment(id: number): Promise<void> {
    return apiClient.delete<void>(`/api/teacher/assignments/${id}`);
  }

  async toggleAssignmentStatus(id: number): Promise<DetailedAssignmentResponse> {
    return apiClient.patch<DetailedAssignmentResponse>(`/api/teacher/assignments/${id}/toggle-status`);
  }

  // Student-specific methods
  async getAssignmentsForStudent(): Promise<StudentAssignmentResponse[]> {
    const assignments = await apiClient.get<StudentAssignmentResponse[]>('/api/student/assignments');
    
    // Transform assignments to include simulated questions for existing single-question assignments
    return assignments.map(assignment => this.transformToMultiQuestionFormat(assignment));
  }

  async getAssignmentForStudent(assignmentId: number): Promise<StudentAssignmentResponse> {
    const assignment = await apiClient.get<StudentAssignmentResponse>(`/api/student/assignments/${assignmentId}`);
    
    // Transform to multi-question format
    return this.transformToMultiQuestionFormat(assignment);
  }

  // Helper method to transform single-question assignments to multi-question format
  private transformToMultiQuestionFormat(assignment: StudentAssignmentResponse): StudentAssignmentResponse {
    // If already has questions, return as-is
    if (assignment.questions && assignment.questions.length > 0) {
      return {
        ...assignment,
        totalQuestions: assignment.questions.length
      };
    }

    // Use the totalQuestions from backend instead of hardcoding to 1
    const backendTotalQuestions = assignment.totalQuestions || 1;
    
    // For backward compatibility, create a single question from the assignment data
    const singleQuestion: StudentQuestionResponse = {
      id: assignment.id, // Use assignment ID as question ID
      title: assignment.title || 'Bài tập chính',
      description: this.getQuestionDescription(assignment),
      questionType: this.determineQuestionType(assignment),
      points: assignment.maxScore,
      orderIndex: 1,
      publicTestCases: (assignment.publicTestCases as TestCaseResponse[]) || [],
      options: [],
      isAnswered: assignment.isSubmitted,
      userAnswer: assignment.isSubmitted ? 'Đã nộp bài' : undefined,
      selectedOptionIds: []
    };

    return {
      ...assignment,
      totalQuestions: backendTotalQuestions, // Use backend value, not hardcoded 1
      questions: [singleQuestion],
      publicTestCases: (assignment.publicTestCases as TestCaseResponse[]) || []
    };
  }

  private getQuestionDescription(assignment: StudentAssignmentResponse): string {
    let description = '';
    
    if (assignment.description) {
      description += assignment.description;
    }
    
    if (assignment.requirements) {
      if (description) description += '\n\n';
      description += '**Yêu cầu chi tiết:**\n' + assignment.requirements;
    }
    
    if (!description) {
      description = `Bài tập ${assignment.type.toLowerCase()} - ${assignment.title}`;
    }
    
    return description;
  }

  private determineQuestionType(assignment: StudentAssignmentResponse): 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE' {
    // If has test cases, it's likely a programming question
    if (assignment.totalTestCases > 0 || (assignment.publicTestCases && assignment.publicTestCases.length > 0)) {
      return 'PROGRAMMING';
    }
    
    // Based on assignment type, make a best guess
    switch (assignment.type) {
      case 'EXERCISE':
        return 'PROGRAMMING';
      case 'QUIZ':
        return 'MULTIPLE_CHOICE';
      case 'EXAM':
        return 'ESSAY';
      case 'PROJECT':
        return 'ESSAY';
      default:
        return 'PROGRAMMING';
    }
  }

  async submitAssignment(request: SubmissionRequest): Promise<SubmissionResponse> {
    return apiClient.post<SubmissionResponse>(`/api/student/assignments/${request.assignmentId}/submit`, request);
  }

  async getMySubmissions(): Promise<SubmissionResponse[]> {
    return apiClient.get<SubmissionResponse[]>('/api/student/submissions');
  }

  async getSubmissionDetails(submissionId: number): Promise<SubmissionResponse> {
    return apiClient.get<SubmissionResponse>(`/api/student/submissions/${submissionId}`);
  }

  async runCode(assignmentId: number, code: string, language: string, input?: string): Promise<CodeExecutionResponse> {
    return apiClient.post<CodeExecutionResponse>(`/api/student/assignments/${assignmentId}/run`, {
      code,
      language,
      input
    });
  }

  async testCode(assignmentId: number, code: string, language: string): Promise<CodeExecutionResponse> {
    return apiClient.post<CodeExecutionResponse>(`/api/student/assignments/${assignmentId}/test`, {
      code,
      language
    });
  }

  async compileCode(code: string, language: string): Promise<CodeExecutionResponse> {
    return apiClient.post<CodeExecutionResponse>('/api/student/code/compile', {
      code,
      language
    });
  }

  async getSubmissionsByAssignment(assignmentId: number): Promise<any[]> {
    return apiClient.get<any[]>(`/api/teacher/assignments/${assignmentId}/submissions`);
  }

  async gradeSubmission(submissionId: number, score: number, feedback?: string): Promise<any> {
    const params = new URLSearchParams();
    params.append('score', score.toString());
    if (feedback) {
      params.append('feedback', feedback);
    }
    return apiClient.post<any>(`/api/teacher/submissions/${submissionId}/grade?${params.toString()}`);
  }

  // Question management methods
  async addQuestionToAssignment(assignmentId: number, question: CreateQuestionRequest): Promise<QuestionResponse> {
    return apiClient.post<QuestionResponse>(`/api/teacher/assignments/${assignmentId}/questions`, question);
  }

  async updateQuestion(assignmentId: number, questionId: number, question: UpdateQuestionRequest): Promise<QuestionResponse> {
    return apiClient.put<QuestionResponse>(`/api/teacher/assignments/${assignmentId}/questions/${questionId}`, question);
  }

  async deleteQuestion(assignmentId: number, questionId: number): Promise<void> {
    return apiClient.delete<void>(`/api/teacher/assignments/${assignmentId}/questions/${questionId}`);
  }

  async reorderQuestions(assignmentId: number, questionOrders: { questionId: number; orderIndex: number }[]): Promise<void> {
    return apiClient.put<void>(`/api/teacher/assignments/${assignmentId}/questions/reorder`, { questionOrders });
  }

  // Student question-specific methods
  async getQuestionForStudent(assignmentId: number, questionId: number): Promise<StudentQuestionResponse> {
    return apiClient.get<StudentQuestionResponse>(`/api/student/assignments/${assignmentId}/questions/${questionId}`);
  }

  async runQuestionCode(assignmentId: number, questionId: number, code: string, language: string, input?: string): Promise<CodeExecutionResponse> {
    // Run code with custom input (for testing, no grading)
    const result = await apiClient.post<CodeExecutionResponse>(`/api/student/check-question-code`, {
      questionId,
      code,
      language,
      input
    });
    
    // Add context message for run mode
    if (result && input) {
      result.message = "Chạy code với input tùy chỉnh - Kết quả chỉ để tham khảo, không tính điểm.";
    }
    
    return result;
  }

  async testQuestionCode(assignmentId: number, questionId: number, code: string, language: string): Promise<CodeExecutionResponse> {
    // Test code with teacher's test cases (with grading)
    const result = await apiClient.post<CodeExecutionResponse>(`/api/student/check-question-code`, {
      questionId,
      code,
      language
    });
    
    return result;
  }

  // Check question code with test cases (similar to Moodle CodeRunner)
  async checkQuestionCode(params: { questionId: number; code: string; language: string }): Promise<any> {
    try {
      // Mock implementation for now - replace with actual API call
      // return apiClient.post<any>(`/api/student/questions/${params.questionId}/check`, {
      //   code: params.code,
      //   language: params.language
      // });
      
      // Mock test results based on the provided image
      if (params.code.includes('def sqr(n):') && params.code.includes('return n * n')) {
        return {
          success: true,
          score: 1.0,
          testCases: [
            { input: 'print(sqr(-3))', expected: '9', actual: '9', passed: true },
            { input: 'print(sqr(11))', expected: '121', actual: '121', passed: true },
            { input: 'print(sqr(-4))', expected: '16', actual: '16', passed: true },
            { input: 'print(sqr(0))', expected: '0', actual: '0', passed: true }
          ]
        };
      } else {
        return {
          success: false,
          score: 0,
          testCases: [
            { input: 'print(sqr(-3))', expected: '9', actual: 'undefined', passed: false },
            { input: 'print(sqr(11))', expected: '121', actual: 'undefined', passed: false }
          ],
          error: 'Function sqr() is not defined or incorrect implementation'
        };
      }
    } catch (error) {
      throw new Error('Failed to check question code');
    }
  }

  // Teacher-specific code validation for answer checking
  async validateAnswerCode(answerCode: string, testCode: string, language: string = 'c', input?: string): Promise<CodeExecutionResponse> {
    // Combine the answer code with the test code for execution
    const combinedCode = `#include <stdio.h>\n#include <string.h>\n\n${answerCode}\n\nint main() {\n    ${testCode}\n    return 0;\n}`;
    
    console.log('Sending code to backend:', {
      combinedCode,
      language,
      input: input || ''
    });
    
    return apiClient.post<CodeExecutionResponse>('/api/teacher/validate-code', {
      code: combinedCode,
      language: language,
      input: input || ''
    });
  }

  // Batch validation for multiple test cases
  async validateMultipleTestCases(answerCode: string, testCases: Array<{testCode: string, input: string}>, language: string = 'c'): Promise<Array<{output: string, error?: string}>> {
    const results = [];
    
    for (const testCase of testCases) {
      try {
        const result = await this.validateAnswerCode(answerCode, testCase.testCode, language, testCase.input);
        results.push({
          output: result.output || '',
          error: result.error
        });
      } catch (error) {
        results.push({
          output: '',
          error: `Execution failed: ${error}`
        });
      }
    }
    
    return results;
  }

  // Helper method to transform teacher assignment
  private transformTeacherAssignment(assignment: any): AssignmentResponse {
    const transformed: AssignmentResponse = {
      id: assignment.id,
      title: assignment.title,
      description: assignment.description,
      type: assignment.type || 'EXERCISE',
      courseName: assignment.courseName || assignment.course?.name || '',
      courseId: assignment.courseId || assignment.course?.id || 0,
      courseCode: assignment.courseCode || assignment.course?.code || '',
      maxScore: assignment.maxScore || 100,
      timeLimit: assignment.timeLimit || 60,
      startTime: assignment.startTime,
      endTime: assignment.endTime,
      isActive: assignment.isActive !== undefined ? assignment.isActive : true,
      allowLateSubmission: assignment.allowLateSubmission || false,
      autoGrade: assignment.autoGrade || true,
      submissionCount: assignment.submissionCount || 0,
      pendingCount: assignment.pendingCount || 0,
      totalQuestions: assignment.totalQuestions || 0, // Chỉ dùng totalQuestions từ API
      createdAt: assignment.createdAt,
      updatedAt: assignment.updatedAt
    };
    
    return transformed;
  }
}

export const AssignmentService = new AssignmentServiceClass();
