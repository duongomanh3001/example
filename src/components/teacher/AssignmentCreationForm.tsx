"use client";

import { useState, useEffect } from 'react';
import { CourseService } from '@/services/course.service';
import { AssignmentService } from '@/services/assignment.service';
import { CourseResponse, CreateAssignmentRequest, TestCaseRequest } from '@/types/api';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Modal } from '@/components/ui/Modal';

interface Question {
  id?: number;
  title: string;
  description: string;
  questionType: 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE';
  points: number;
  orderIndex: number;
  testCases: TestCaseRequest[];
  options: QuestionOption[];
}

interface QuestionOption {
  id?: number;
  optionText: string;
  isCorrect: boolean;
  orderIndex: number;
}

interface AssignmentCreationFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export default function AssignmentCreationForm({ isOpen, onClose, onSuccess }: AssignmentCreationFormProps) {
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [currentStep, setCurrentStep] = useState(1); // 1: Basic Info, 2: Questions
  const [selectedQuestionIndex, setSelectedQuestionIndex] = useState<number | null>(null);
  
  const [assignmentData, setAssignmentData] = useState({
    title: '',
    description: '',
    type: 'EXERCISE' as 'EXERCISE' | 'EXAM' | 'PROJECT' | 'QUIZ',
    courseId: 0,
    maxScore: 100,
    timeLimit: 60,
    startTime: '',
    endTime: '',
    allowLateSubmission: false,
    autoGrade: true,
  });

  const [questions, setQuestions] = useState<Question[]>([]);

  useEffect(() => {
    if (isOpen) {
      loadCourses();
    }
  }, [isOpen]);

  const loadCourses = async () => {
    try {
      const coursesData = await CourseService.getTeacherCourses();
      setCourses(coursesData);
    } catch (error) {
      console.error('Failed to load courses:', error);
    }
  };

  const handleAddQuestion = () => {
    const newQuestion: Question = {
      title: `Câu hỏi ${questions.length + 1}`,
      description: '',
      questionType: 'PROGRAMMING',
      points: 10,
      orderIndex: questions.length + 1,
      testCases: [],
      options: []
    };
    setQuestions([...questions, newQuestion]);
    setSelectedQuestionIndex(questions.length);
  };

  const handleUpdateQuestion = (index: number, updatedQuestion: Partial<Question>) => {
    const updatedQuestions = [...questions];
    updatedQuestions[index] = { ...updatedQuestions[index], ...updatedQuestion };
    setQuestions(updatedQuestions);
  };

  const handleDeleteQuestion = (index: number) => {
    const updatedQuestions = questions.filter((_, i) => i !== index);
    // Reorder questions
    const reorderedQuestions = updatedQuestions.map((q, i) => ({
      ...q,
      orderIndex: i + 1,
      title: q.title.includes('Câu hỏi') ? `Câu hỏi ${i + 1}` : q.title
    }));
    setQuestions(reorderedQuestions);
    setSelectedQuestionIndex(null);
  };

  const handleAddTestCase = (questionIndex: number) => {
    const newTestCase: TestCaseRequest = {
      input: '',
      expectedOutput: '',
      isHidden: false,
      points: 1
    };
    
    const updatedQuestions = [...questions];
    updatedQuestions[questionIndex].testCases.push(newTestCase);
    setQuestions(updatedQuestions);
  };

  const handleUpdateTestCase = (questionIndex: number, testCaseIndex: number, updatedTestCase: Partial<TestCaseRequest>) => {
    const updatedQuestions = [...questions];
    updatedQuestions[questionIndex].testCases[testCaseIndex] = {
      ...updatedQuestions[questionIndex].testCases[testCaseIndex],
      ...updatedTestCase
    };
    setQuestions(updatedQuestions);
  };

  const handleDeleteTestCase = (questionIndex: number, testCaseIndex: number) => {
    const updatedQuestions = [...questions];
    updatedQuestions[questionIndex].testCases.splice(testCaseIndex, 1);
    setQuestions(updatedQuestions);
  };

  const handleAddOption = (questionIndex: number) => {
    const question = questions[questionIndex];
    const newOption: QuestionOption = {
      optionText: '',
      isCorrect: false,
      orderIndex: question.options.length + 1
    };
    
    const updatedQuestions = [...questions];
    updatedQuestions[questionIndex].options.push(newOption);
    setQuestions(updatedQuestions);
  };

  const handleUpdateOption = (questionIndex: number, optionIndex: number, updatedOption: Partial<QuestionOption>) => {
    const updatedQuestions = [...questions];
    updatedQuestions[questionIndex].options[optionIndex] = {
      ...updatedQuestions[questionIndex].options[optionIndex],
      ...updatedOption
    };
    setQuestions(updatedQuestions);
  };

  const handleDeleteOption = (questionIndex: number, optionIndex: number) => {
    const updatedQuestions = [...questions];
    updatedQuestions[questionIndex].options.splice(optionIndex, 1);
    // Reorder options
    updatedQuestions[questionIndex].options = updatedQuestions[questionIndex].options.map((opt, i) => ({
      ...opt,
      orderIndex: i + 1
    }));
    setQuestions(updatedQuestions);
  };

  const calculateTotalScore = () => {
    return questions.reduce((total, question) => total + question.points, 0);
  };

  const handleSubmit = async () => {
    try {
      setIsLoading(true);
      
      const totalScore = calculateTotalScore();
      
      const requestData: CreateAssignmentRequest = {
        ...assignmentData,
        maxScore: totalScore,
        questions: questions.map(q => ({
          title: q.title,
          description: q.description,
          questionType: q.questionType,
          points: q.points,
          orderIndex: q.orderIndex,
          testCases: q.testCases,
          options: q.options.map(opt => ({
            optionText: opt.optionText,
            isCorrect: opt.isCorrect,
            orderIndex: opt.orderIndex
          }))
        }))
      };

      await AssignmentService.createAssignment(requestData);
      alert('Bài tập đã được tạo thành công với ' + questions.length + ' câu hỏi!');
      onSuccess();
      handleClose();
    } catch (error) {
      console.error('Failed to create assignment:', error);
      alert('Có lỗi xảy ra khi tạo bài tập. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setCurrentStep(1);
    setSelectedQuestionIndex(null);
    setAssignmentData({
      title: '',
      description: '',
      type: 'EXERCISE',
      courseId: 0,
      maxScore: 100,
      timeLimit: 60,
      startTime: '',
      endTime: '',
      allowLateSubmission: false,
      autoGrade: true,
    });
    setQuestions([]);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={handleClose} size="full">
      <div className="flex h-full">
        {/* Sidebar - Question List */}
        {currentStep === 2 && (
          <div className="w-1/4 border-r bg-slate-50 p-4 overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-semibold text-slate-900">Danh sách câu hỏi</h3>
              <Button onClick={handleAddQuestion} size="sm">
                + Thêm câu hỏi
              </Button>
            </div>
            
            <div className="space-y-2">
              {questions.map((question, index) => (
                <div
                  key={index}
                  className={`p-3 rounded-lg cursor-pointer border transition-colors ${
                    selectedQuestionIndex === index
                      ? 'bg-blue-100 border-blue-300'
                      : 'bg-white border-slate-200 hover:bg-slate-50'
                  }`}
                  onClick={() => setSelectedQuestionIndex(index)}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-slate-900 truncate">
                        {question.title || `Câu hỏi ${index + 1}`}
                      </div>
                      <div className="text-xs text-slate-500 mt-1">
                        {question.questionType === 'PROGRAMMING' ? 'Lập trình' :
                         question.questionType === 'MULTIPLE_CHOICE' ? 'Trắc nghiệm' :
                         question.questionType === 'ESSAY' ? 'Tự luận' :
                         'Đúng/Sai'}
                      </div>
                      <div className="text-xs font-medium text-blue-600 mt-1">
                        {question.points} điểm
                      </div>
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteQuestion(index);
                      }}
                      className="text-red-500 hover:text-red-700 text-xs ml-2"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
              
              {questions.length === 0 && (
                <div className="text-center text-slate-500 text-sm py-8">
                  Chưa có câu hỏi nào.<br/>
                  Nhấn "Thêm câu hỏi" để bắt đầu.
                </div>
              )}
            </div>
            
            {questions.length > 0 && (
              <div className="mt-4 pt-4 border-t">
                <div className="text-sm text-slate-600">
                  <div>Tổng số câu: <span className="font-medium">{questions.length}</span></div>
                  <div>Tổng điểm: <span className="font-medium text-blue-600">{calculateTotalScore()}</span></div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Main Content */}
        <div className="flex-1 flex flex-col">
          {/* Header */}
          <div className="px-6 py-4 border-b bg-white">
            <div className="flex justify-between items-center">
              <div>
                <h2 className="text-xl font-bold text-slate-900">
                  {currentStep === 1 ? 'Tạo Bài Tập Mới' : 'Thiết lập Câu hỏi'}
                </h2>
                <div className="flex items-center mt-2 space-x-4">
                  <div className={`flex items-center ${currentStep >= 1 ? 'text-blue-600' : 'text-slate-400'}`}>
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium mr-2 ${
                      currentStep >= 1 ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-500'
                    }`}>1</div>
                    Thông tin cơ bản
                  </div>
                  <div className={`w-8 h-px ${currentStep >= 2 ? 'bg-blue-600' : 'bg-slate-300'}`}></div>
                  <div className={`flex items-center ${currentStep >= 2 ? 'text-blue-600' : 'text-slate-400'}`}>
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium mr-2 ${
                      currentStep >= 2 ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-500'
                    }`}>2</div>
                    Câu hỏi
                  </div>
                </div>
              </div>
              <button
                onClick={handleClose}
                className="text-slate-400 hover:text-slate-600 text-xl font-bold"
              >
                ✕
              </button>
            </div>
          </div>

          {/* Content */}
          <div className="flex-1 p-6 overflow-y-auto">
            {currentStep === 1 && (
              <BasicInfoStep
                assignmentData={assignmentData}
                courses={courses}
                onUpdate={setAssignmentData}
              />
            )}
            
            {currentStep === 2 && (
              <QuestionStep
                questions={questions}
                selectedQuestionIndex={selectedQuestionIndex}
                onUpdateQuestion={handleUpdateQuestion}
                onAddTestCase={handleAddTestCase}
                onUpdateTestCase={handleUpdateTestCase}
                onDeleteTestCase={handleDeleteTestCase}
                onAddOption={handleAddOption}
                onUpdateOption={handleUpdateOption}
                onDeleteOption={handleDeleteOption}
              />
            )}
          </div>

          {/* Footer */}
          <div className="px-6 py-4 border-t bg-white flex justify-between">
            <div>
              {currentStep === 2 && (
                <Button
                  onClick={() => setCurrentStep(1)}
                  variant="outline"
                >
                  ← Quay lại
                </Button>
              )}
            </div>
            
            <div className="flex space-x-3">
              <Button onClick={handleClose} variant="outline">
                Hủy
              </Button>
              
              {currentStep === 1 ? (
                <Button
                  onClick={() => setCurrentStep(2)}
                  disabled={!assignmentData.title || !assignmentData.courseId}
                >
                  Tiếp theo →
                </Button>
              ) : (
                <Button
                  onClick={handleSubmit}
                  disabled={isLoading || questions.length === 0}
                  className="bg-green-600 hover:bg-green-700"
                >
                  {isLoading ? 'Đang tạo...' : 'Tạo bài tập'}
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
}

// Component for Step 1 - Basic Info
function BasicInfoStep({ 
  assignmentData, 
  courses, 
  onUpdate 
}: {
  assignmentData: any;
  courses: CourseResponse[];
  onUpdate: (data: any) => void;
}) {
  return (
    <div className="max-w-2xl space-y-6">
      <Card className="p-6">
        <h3 className="text-lg font-semibold mb-4">Thông tin bài tập</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Tiêu đề bài tập *
            </label>
            <input
              type="text"
              value={assignmentData.title}
              onChange={(e) => onUpdate({ ...assignmentData, title: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Nhập tiêu đề bài tập"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Mô tả bài tập
            </label>
            <textarea
              value={assignmentData.description}
              onChange={(e) => onUpdate({ ...assignmentData, description: e.target.value })}
              rows={4}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Mô tả chi tiết về bài tập"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Khóa học *
            </label>
            <select
              value={assignmentData.courseId}
              onChange={(e) => onUpdate({ ...assignmentData, courseId: parseInt(e.target.value) })}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value={0}>Chọn khóa học</option>
              {courses.map((course) => (
                <option key={course.id} value={course.id}>
                  {course.code} - {course.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Loại bài tập
            </label>
            <select
              value={assignmentData.type}
              onChange={(e) => onUpdate({ ...assignmentData, type: e.target.value as any })}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="EXERCISE">Bài tập</option>
              <option value="EXAM">Bài thi</option>
              <option value="PROJECT">Dự án</option>
              <option value="QUIZ">Kiểm tra nhanh</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Thời gian làm bài (phút)
            </label>
            <input
              type="number"
              value={assignmentData.timeLimit}
              onChange={(e) => onUpdate({ ...assignmentData, timeLimit: parseInt(e.target.value) })}
              min="1"
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Thời gian bắt đầu
            </label>
            <input
              type="datetime-local"
              value={assignmentData.startTime}
              onChange={(e) => onUpdate({ ...assignmentData, startTime: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Hạn nộp bài
            </label>
            <input
              type="datetime-local"
              value={assignmentData.endTime}
              onChange={(e) => onUpdate({ ...assignmentData, endTime: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="md:col-span-2 space-y-3">
            <label className="flex items-center">
              <input
                type="checkbox"
                checked={assignmentData.allowLateSubmission}
                onChange={(e) => onUpdate({ ...assignmentData, allowLateSubmission: e.target.checked })}
                className="mr-2"
              />
              <span className="text-sm text-slate-700">Cho phép nộp trễ</span>
            </label>
            
            <label className="flex items-center">
              <input
                type="checkbox"
                checked={assignmentData.autoGrade}
                onChange={(e) => onUpdate({ ...assignmentData, autoGrade: e.target.checked })}
                className="mr-2"
              />
              <span className="text-sm text-slate-700">Tự động chấm điểm</span>
            </label>
          </div>
        </div>
      </Card>
    </div>
  );
}

// Component for Step 2 - Questions
function QuestionStep({
  questions,
  selectedQuestionIndex,
  onUpdateQuestion,
  onAddTestCase,
  onUpdateTestCase,
  onDeleteTestCase,
  onAddOption,
  onUpdateOption,
  onDeleteOption,
}: {
  questions: Question[];
  selectedQuestionIndex: number | null;
  onUpdateQuestion: (index: number, updatedQuestion: Partial<Question>) => void;
  onAddTestCase: (questionIndex: number) => void;
  onUpdateTestCase: (questionIndex: number, testCaseIndex: number, updatedTestCase: Partial<TestCaseRequest>) => void;
  onDeleteTestCase: (questionIndex: number, testCaseIndex: number) => void;
  onAddOption: (questionIndex: number) => void;
  onUpdateOption: (questionIndex: number, optionIndex: number, updatedOption: Partial<QuestionOption>) => void;
  onDeleteOption: (questionIndex: number, optionIndex: number) => void;
}) {
  if (selectedQuestionIndex === null || !questions[selectedQuestionIndex]) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center text-slate-500">
          <div className="text-lg font-medium mb-2">Chọn một câu hỏi để chỉnh sửa</div>
          <div className="text-sm">Hoặc thêm câu hỏi mới từ sidebar</div>
        </div>
      </div>
    );
  }

  const question = questions[selectedQuestionIndex];

  return (
    <div className="space-y-6">
      <Card className="p-6">
        <h3 className="text-lg font-semibold mb-4">Câu hỏi {selectedQuestionIndex + 1}</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Tiêu đề câu hỏi
            </label>
            <input
              type="text"
              value={question.title}
              onChange={(e) => onUpdateQuestion(selectedQuestionIndex, { title: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Nhập tiêu đề câu hỏi"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Mô tả câu hỏi
            </label>
            <textarea
              value={question.description}
              onChange={(e) => onUpdateQuestion(selectedQuestionIndex, { description: e.target.value })}
              rows={4}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Mô tả chi tiết câu hỏi, yêu cầu, ví dụ..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Loại câu hỏi
            </label>
            <select
              value={question.questionType}
              onChange={(e) => onUpdateQuestion(selectedQuestionIndex, { questionType: e.target.value as any })}
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="PROGRAMMING">Lập trình</option>
              <option value="MULTIPLE_CHOICE">Trắc nghiệm</option>
              <option value="ESSAY">Tự luận</option>
              <option value="TRUE_FALSE">Đúng/Sai</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Điểm số
            </label>
            <input
              type="number"
              value={question.points}
              onChange={(e) => onUpdateQuestion(selectedQuestionIndex, { points: parseFloat(e.target.value) || 0 })}
              min="0"
              step="0.5"
              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
      </Card>

      {/* Test Cases for PROGRAMMING questions */}
      {question.questionType === 'PROGRAMMING' && (
        <Card className="p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold">Test Cases</h3>
            <Button onClick={() => onAddTestCase(selectedQuestionIndex)} size="sm">
              + Thêm Test Case
            </Button>
          </div>

          {question.testCases.length === 0 ? (
            <div className="text-center text-slate-500 py-8">
              Chưa có test case nào. Nhấn "Thêm Test Case" để bắt đầu.
            </div>
          ) : (
            <div className="space-y-4">
              {question.testCases.map((testCase, tcIndex) => (
                <div key={tcIndex} className="border border-slate-200 rounded-lg p-4">
                  <div className="flex justify-between items-start mb-3">
                    <h4 className="font-medium text-slate-900">Test Case {tcIndex + 1}</h4>
                    <button
                      onClick={() => onDeleteTestCase(selectedQuestionIndex, tcIndex)}
                      className="text-red-500 hover:text-red-700 text-sm"
                    >
                      Xóa
                    </button>
                  </div>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">
                        Input
                      </label>
                      <textarea
                        value={testCase.input}
                        onChange={(e) => onUpdateTestCase(selectedQuestionIndex, tcIndex, { input: e.target.value })}
                        rows={3}
                        className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                        placeholder="Dữ liệu đầu vào"
                      />
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">
                        Expected Output
                      </label>
                      <textarea
                        value={testCase.expectedOutput}
                        onChange={(e) => onUpdateTestCase(selectedQuestionIndex, tcIndex, { expectedOutput: e.target.value })}
                        rows={3}
                        className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                        placeholder="Kết quả mong đợi"
                      />
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between mt-4">
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={testCase.isHidden}
                        onChange={(e) => onUpdateTestCase(selectedQuestionIndex, tcIndex, { isHidden: e.target.checked })}
                        className="mr-2"
                      />
                      <span className="text-sm text-slate-700">Test case ẩn</span>
                    </label>
                    
                    <div className="flex items-center space-x-2">
                      <label className="text-sm text-slate-700">Điểm:</label>
                      <input
                        type="number"
                        value={testCase.points}
                        onChange={(e) => onUpdateTestCase(selectedQuestionIndex, tcIndex, { points: parseFloat(e.target.value) || 0 })}
                        min="0"
                        step="0.1"
                        className="w-20 px-2 py-1 border border-slate-300 rounded text-sm"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      )}

      {/* Options for MULTIPLE_CHOICE and TRUE_FALSE questions */}
      {(question.questionType === 'MULTIPLE_CHOICE' || question.questionType === 'TRUE_FALSE') && (
        <Card className="p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold">
              {question.questionType === 'MULTIPLE_CHOICE' ? 'Lựa chọn' : 'Đáp án'}
            </h3>
            {question.questionType === 'MULTIPLE_CHOICE' && (
              <Button onClick={() => onAddOption(selectedQuestionIndex)} size="sm">
                + Thêm lựa chọn
              </Button>
            )}
          </div>

          {question.questionType === 'TRUE_FALSE' ? (
            <div className="space-y-3">
              {['Đúng', 'Sai'].map((optionText, index) => (
                <label key={index} className="flex items-center p-3 border rounded-lg">
                  <input
                    type="radio"
                    name={`question-${selectedQuestionIndex}-answer`}
                    checked={question.options[index]?.isCorrect || false}
                    onChange={() => {
                      // Ensure we have both options
                      const newOptions = [
                        { optionText: 'Đúng', isCorrect: index === 0, orderIndex: 1 },
                        { optionText: 'Sai', isCorrect: index === 1, orderIndex: 2 }
                      ];
                      onUpdateQuestion(selectedQuestionIndex, { options: newOptions });
                    }}
                    className="mr-3"
                  />
                  <span className="text-sm font-medium">{optionText}</span>
                </label>
              ))}
            </div>
          ) : (
            <>
              {question.options.length === 0 ? (
                <div className="text-center text-slate-500 py-8">
                  Chưa có lựa chọn nào. Nhấn "Thêm lựa chọn" để bắt đầu.
                </div>
              ) : (
                <div className="space-y-3">
                  {question.options.map((option, optIndex) => (
                    <div key={optIndex} className="flex items-center space-x-3 p-3 border rounded-lg">
                      <input
                        type="checkbox"
                        checked={option.isCorrect}
                        onChange={(e) => onUpdateOption(selectedQuestionIndex, optIndex, { isCorrect: e.target.checked })}
                        className="flex-shrink-0"
                      />
                      <input
                        type="text"
                        value={option.optionText}
                        onChange={(e) => onUpdateOption(selectedQuestionIndex, optIndex, { optionText: e.target.value })}
                        className="flex-1 px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder={`Lựa chọn ${optIndex + 1}`}
                      />
                      <button
                        onClick={() => onDeleteOption(selectedQuestionIndex, optIndex)}
                        className="text-red-500 hover:text-red-700 flex-shrink-0"
                      >
                        Xóa
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </Card>
      )}
    </div>
  );
}
