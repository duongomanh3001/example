export interface CourseResponse {
  id: number;
  name: string;
  description: string;
  code: string;
  creditHours: number;
  semester: string;
  year: number;
  isActive: boolean;
  maxStudents: number;
  currentStudentCount: number;
  createdAt: string;
  updatedAt: string;
  teacher?: {
    id: number;
    username: string;
    fullName: string;
    email: string;
  };
}

export interface DetailedCourseResponse {
  id: number;
  name: string;
  code: string;
  description: string;
  creditHours: number;
  semester: string;
  year: number;
  maxStudents: number;
  currentStudentCount: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  teacher: {
    id: number;
    username: string;
    fullName: string;
    email: string;
  };
  assignmentCount: number;
}

export interface CreateCourseRequest {
  name: string;
  code: string;
  description?: string;
  creditHours: number;
  semester: string;
  year: number;
  maxStudents: number;
  teacherId: number;
}

export interface UpdateCourseRequest {
  name?: string;
  description?: string;
  creditHours?: number;
  semester?: string;
  year?: number;
  maxStudents?: number;
  teacherId?: number;
  isActive?: boolean;
}

export interface CreateAssignmentRequest {
  title: string;
  description?: string;
  requirements?: string;
  type: 'EXERCISE' | 'EXAM' | 'PROJECT' | 'QUIZ';
  courseId: number;
  maxScore: number;
  timeLimit: number;
  startTime?: string;
  endTime?: string;
  allowLateSubmission?: boolean;
  autoGrade?: boolean;
  questions?: CreateQuestionRequest[];
}

export interface CreateQuestionRequest {
  title: string;
  description: string;
  questionType: 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE';
  points: number;
  orderIndex: number;
  testCases?: TestCaseRequest[];
  options?: QuestionOptionRequest[];
}

export interface UpdateAssignmentRequest {
  title?: string;
  description?: string;
  requirements?: string;
  type?: 'EXERCISE' | 'EXAM' | 'PROJECT' | 'QUIZ';
  courseId?: number;
  maxScore?: number;
  timeLimit?: number;
  startTime?: string;
  endTime?: string;
  allowLateSubmission?: boolean;
  autoGrade?: boolean;
  questions?: UpdateQuestionRequest[];
}

export interface UpdateQuestionRequest {
  id?: number;
  title?: string;
  description?: string;
  questionType?: 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE';
  points?: number;
  orderIndex?: number;
  testCases?: TestCaseRequest[];
  options?: QuestionOptionRequest[];
}

export interface QuestionOptionRequest {
  id?: number;
  optionText: string;
  isCorrect: boolean;
  orderIndex: number;
}

export interface TestCaseRequest {
  input: string;
  expectedOutput: string;
  isHidden: boolean;
  points: number;
}

export interface AssignmentResponse {
  id: number;
  title: string;
  description: string;
  type: 'EXERCISE' | 'EXAM' | 'PROJECT' | 'QUIZ';
  courseName: string;
  courseId: number;
  courseCode: string;
  maxScore: number;
  timeLimit: number;
  startTime?: string;
  endTime?: string;
  isActive: boolean;
  allowLateSubmission: boolean;
  autoGrade: boolean;
  submissionCount: number;
  pendingCount: number;
  totalQuestions: number;
  createdAt: string;
  updatedAt: string;
}

export interface DetailedAssignmentResponse extends AssignmentResponse {
  testCases: TestCaseResponse[];
  questions: QuestionResponse[];
}

export interface QuestionResponse {
  id: number;
  assignmentId: number;
  title: string;
  description: string;
  questionType: 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE';
  points: number;
  orderIndex: number;
  testCases: TestCaseResponse[];
  options: QuestionOptionResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface QuestionOptionResponse {
  id: number;
  questionId: number;
  optionText: string;
  isCorrect: boolean;
  orderIndex: number;
}

export interface TestCaseResponse {
  id: number;
  input: string;
  expectedOutput: string;
  isHidden: boolean;
  points: number;
}

export enum AssignmentType {
  PROGRAMMING = 'PROGRAMMING',
  ESSAY = 'ESSAY',
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE',
  PROJECT = 'PROJECT'
}

export interface StudentResponse {
  id: number;
  username: string;
  email: string;
  fullName: string;
  studentId: string;
  isActive: boolean;
  enrolledAt?: string;
}

export interface StudentAssignmentResponse {
  id: number;
  title: string;
  description: string;
  requirements?: string;
  type: 'EXERCISE' | 'EXAM' | 'PROJECT' | 'QUIZ';
  courseId: number;
  courseName: string;
  maxScore: number;
  timeLimit: number;
  startTime?: string;
  endTime?: string;
  allowLateSubmission: boolean;
  isSubmitted: boolean;
  currentScore?: number;
  submissionTime?: string;
  submissionStatus?: string;
  publicTestCases?: TestCaseResponse[];
  totalTestCases: number;
  totalQuestions: number;
  questions?: StudentQuestionResponse[];
  createdAt: string;
}

export interface StudentQuestionResponse {
  id: number;
  title: string;
  description: string;
  questionType: 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE';
  points: number;
  orderIndex: number;
  publicTestCases: TestCaseResponse[];
  options: QuestionOptionResponse[];
  isAnswered: boolean;
  userAnswer?: string;
  selectedOptionIds?: number[];
  // New fields for enhanced programming questions
  starterCode?: string;        // Template code for students
  exampleTestCases?: TestCaseResponse[];  // Test cases marked as examples
  language?: string;           // Programming language for this question
  totalTestCases?: number;     // Total number of test cases (including hidden)
}

export interface SubmissionRequest {
  assignmentId: number;
  code: string;
  programmingLanguage: string;
}

export interface QuestionAnswerRequest {
  questionId: number;
  answer: string;
  selectedOptionIds?: number[];
  language?: string;
}

export interface SubmissionResponse {
  id: number;
  assignmentId: number;
  assignmentTitle: string;
  studentName: string;
  studentId: string;
  programmingLanguage: string;
  status: 'NOT_SUBMITTED' | 'SUBMITTED' | 'GRADING' | 'GRADED' | 'PASSED' | 'PARTIAL' | 'FAILED' | 'COMPILATION_ERROR' | 'ERROR' | 'NO_TESTS' | 'LATE' | 'PENDING' | 'COMPILE_ERROR' | 'RUNTIME_ERROR';
  score?: number;
  executionTime?: number;
  memoryUsed?: number;
  feedback?: string;
  submissionTime: string;
  gradedTime?: string;
  testCasesPassed?: number;
  totalTestCases?: number;
  // Enhanced details
  questionResults?: QuestionResultResponse[];
}

export interface QuestionResultResponse {
  questionId: number;
  questionTitle: string;
  questionType: 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE';
  maxScore: number;
  earnedScore: number;
  status: 'CORRECT' | 'INCORRECT' | 'PARTIAL' | 'NOT_ANSWERED';
  testCaseResults?: TestResultResponse[];
  feedback?: string;
}

export interface TestResultResponse {
  id: number;
  input: string;
  expectedOutput: string;
  actualOutput?: string;
  passed: boolean;
  executionTime?: number;
  memoryUsage?: number;
  error?: string;
}

export interface CodeExecutionResponse {
  success: boolean;
  output?: string;
  error?: string;
  executionTime?: number;
  memoryUsage?: number;
  testResults?: TestResultResponse[];
  compilationError?: string;
  message?: string; // System message explaining the grading process
  passedTests?: number;
  totalTests?: number;
  score?: number;
}

export interface TeacherDashboardResponse {
  totalCourses: number;
  totalStudents: number;
  totalAssignments: number;
  pendingSubmissions: number;
  recentCourses: CourseResponse[];
  recentSubmissions: any[];
}

export interface StudentDashboardResponse {
  totalCourses: number;
  totalAssignments: number;
  completedAssignments: number;
  pendingAssignments: number;
  recentCourses: CourseResponse[];
  recentAssignments: any[];
}

export interface AdminCourseRequest {
  name: string;
  description: string;
  code: string;
  creditHours: number;
  semester: string;
  year: number;
  maxStudents: number;
  teacherId?: number;
}

export interface UserStatsResponse {
  totalUsers: number;
  totalStudents: number;
  totalTeachers: number;
  totalAdmins: number;
  activeUsers: number;
  inactiveUsers: number;
}
