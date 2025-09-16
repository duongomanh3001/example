"use client";

import { useState, useEffect } from 'react';
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { CourseService } from "@/services/course.service";
import { AssignmentService } from "@/services/assignment.service";
import { CourseResponse, CreateAssignmentRequest, TestCaseRequest } from "@/types/api";
import MainLayout from "@/components/layouts/MainLayout";
import Link from "next/link";
import { useRouter } from "next/navigation";

interface Question {
  id?: number;
  title: string;
  description: string;
  questionType: 'PROGRAMMING' | 'MULTIPLE_CHOICE' | 'ESSAY' | 'TRUE_FALSE';
  points: number;
  orderIndex: number;
  testCases: EnhancedTestCase[];
  options: QuestionOption[];
  // New fields for enhanced programming questions
  answerCode?: string; // Lecturer's solution code
  starterCode?: string; // Template code for students with //TODO
  validateOnSave?: boolean; // Auto-generate expected outputs
}

interface EnhancedTestCase extends TestCaseRequest {
  testCode?: string; // Code segment to run/test the function
  useAsExample?: boolean; // Show to students as example
  isValidated?: boolean; // Whether output was auto-generated
}

interface QuestionOption {
  id?: number;
  optionText: string;
  isCorrect: boolean;
  orderIndex: number;
}

interface QuestionEditorProps {
  question: Question;
  onUpdate: (updatedQuestion: Partial<Question>) => void;
  onAddTestCase: () => void;
  onUpdateTestCase: (testCaseIndex: number, updatedTestCase: Partial<EnhancedTestCase>) => void;
  onDeleteTestCase: (testCaseIndex: number) => void;
  onAddOption: () => void;
  onUpdateOption: (optionIndex: number, updatedOption: Partial<QuestionOption>) => void;
  onDeleteOption: (optionIndex: number) => void;
  onValidateAnswer: () => Promise<void>; // New function for answer validation
}

function QuestionEditor({
  question,
  onUpdate,
  onAddTestCase,
  onUpdateTestCase,
  onDeleteTestCase,
  onAddOption,
  onUpdateOption,
  onDeleteOption,
  onValidateAnswer
}: QuestionEditorProps) {
  return (
    <div className="space-y-6">
      {/* Question Basic Info */}
      <div className="bg-white border border-slate-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">Thông tin câu hỏi</h3>
        
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              Tiêu đề câu hỏi *
            </label>
            <input
              type="text"
              value={question.title}
              onChange={(e) => onUpdate({ title: e.target.value })}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
              placeholder="Nhập tiêu đề câu hỏi"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              Mô tả chi tiết *
            </label>
            <textarea
              value={question.description}
              onChange={(e) => onUpdate({ description: e.target.value })}
              rows={4}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
              placeholder="Mô tả yêu cầu, đề bài chi tiết"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Loại câu hỏi *
              </label>
              <select
                value={question.questionType}
                onChange={(e) => onUpdate({ questionType: e.target.value as Question['questionType'] })}
                className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
              >
                <option value="PROGRAMMING">Lập trình</option>
                <option value="MULTIPLE_CHOICE">Trắc nghiệm</option>
                <option value="ESSAY">Tự luận</option>
                <option value="TRUE_FALSE">Đúng/Sai</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Điểm *
              </label>
              <input
                type="number"
                min={1}
                max={100}
                value={question.points}
                onChange={(e) => onUpdate({ points: parseInt(e.target.value) })}
                className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Enhanced Programming Questions Section */}
      {question.questionType === 'PROGRAMMING' && (
        <div className="space-y-6">
          {/* Answer Code Section */}
          <div className="bg-white border border-slate-200 rounded-lg p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-slate-900">Đáp án (Answer)</h3>
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={question.validateOnSave || false}
                    onChange={(e) => onUpdate({ validateOnSave: e.target.checked })}
                    className="rounded border-slate-300 text-[#ff6a00] focus:ring-[#ff6a00]"
                  />
                  <span className="text-sm text-slate-700">Validate on save</span>
                </label>
                <button
                  type="button"
                  onClick={onValidateAnswer}
                  disabled={!question.answerCode || question.testCases.length === 0}
                  className="px-3 py-1.5 text-xs font-medium text-white bg-green-600 rounded-md hover:bg-green-700 disabled:bg-slate-300 disabled:cursor-not-allowed"
                >
                  Chạy thử nghiệm
                </button>
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Nhập function/code đáp án của giảng viên để kiểm tra với testcases
              </label>
              <textarea
                value={question.answerCode || ''}
                onChange={(e) => onUpdate({ answerCode: e.target.value })}
                rows={8}
                className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00] font-mono"
                placeholder="// Ví dụ: function countCharacter(str, key) {&#10;//   let count = 0;&#10;//   for(let i = 0; i < str.length; i++) {&#10;//     if(str[i] === key) count++;&#10;//   }&#10;//   return count;&#10;// }"
              />
            </div>
          </div>

          {/* Student Template Code Section */}
          <div className="bg-white border border-slate-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-slate-900 mb-4">Answer box preload</h3>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Mã mẫu hiển thị cho sinh viên (với //TODO để sinh viên hoàn thành)
              </label>
              <textarea
                value={question.starterCode || ''}
                onChange={(e) => onUpdate({ starterCode: e.target.value })}
                rows={6}
                className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00] font-mono"
                placeholder="// Mã mẫu cho sinh viên&#10;function countCharacter(str, key) {&#10;  // TODO: Implement this function&#10;}"
              />
            </div>
          </div>

          {/* Test Cases Section */}
          <div className="bg-white border border-slate-200 rounded-lg p-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-slate-900">Test Cases</h3>
              <button
                type="button"
                onClick={onAddTestCase}
                className="px-3 py-1.5 text-xs font-medium text-white bg-[#ff6a00] rounded-md hover:bg-[#e55a00]"
              >
                + Thêm Test Case
              </button>
            </div>
            
            <div className="space-y-4">
              {question.testCases.map((testCase, index) => (
                <div key={index} className="border border-slate-200 rounded-lg p-4">
                  <div className="flex justify-between items-center mb-3">
                    <h4 className="font-medium text-slate-900">Test Case {index + 1}</h4>
                    <div className="flex items-center gap-2">
                      {testCase.isValidated && (
                        <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                          ✓ Auto-generated
                        </span>
                      )}
                      <button
                        type="button"
                        onClick={() => onDeleteTestCase(index)}
                        className="text-red-500 hover:text-red-700 text-sm"
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                  
                  {/* Test Code Input */}
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Testcase (code để chạy/test code trong main function với function đã nhập trước)
                    </label>
                    <textarea
                      value={testCase.testCode || ''}
                      onChange={(e) => onUpdateTestCase(index, { testCode: e.target.value })}
                      rows={3}
                      className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00] font-mono"
                      placeholder="// Ví dụ:&#10;char data[] = &quot;Hello&quot;;&#10;char key = 'l';&#10;printf(&quot;%d&quot;, countCharacter(data, key));"
                    />
                  </div>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Standard Input
                      </label>
                      <textarea
                        value={testCase.input}
                        onChange={(e) => onUpdateTestCase(index, { input: e.target.value })}
                        rows={3}
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00] font-mono"
                        placeholder="Dữ liệu đầu vào (nếu có)"
                      />
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Expected Output
                        {question.validateOnSave && (
                          <span className="text-xs text-slate-500 ml-1">(sẽ được tự động tạo)</span>
                        )}
                      </label>
                      <textarea
                        value={testCase.expectedOutput}
                        onChange={(e) => onUpdateTestCase(index, { expectedOutput: e.target.value })}
                        rows={3}
                        className={`w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00] font-mono ${
                          testCase.isValidated ? 'bg-green-50' : ''
                        }`}
                        placeholder="Kết quả mong đợi"
                        readOnly={question.validateOnSave && testCase.isValidated}
                      />
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between mt-4">
                    <div className="flex items-center gap-4">
                      <label className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={testCase.useAsExample || false}
                          onChange={(e) => onUpdateTestCase(index, { useAsExample: e.target.checked })}
                          className="rounded border-slate-300 text-[#ff6a00] focus:ring-[#ff6a00]"
                        />
                        <span className="text-sm text-slate-700">Use as example</span>
                      </label>
                      
                      <label className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={testCase.isHidden}
                          onChange={(e) => onUpdateTestCase(index, { isHidden: e.target.checked })}
                          className="rounded border-slate-300 text-[#ff6a00] focus:ring-[#ff6a00]"
                        />
                        <span className="text-sm text-slate-700">Hide rest if fail</span>
                      </label>
                    </div>
                    
                    <div className="flex items-center gap-4">
                      <div className="flex items-center gap-2">
                        <label className="text-sm text-slate-700">Mark:</label>
                        <input
                          type="number"
                          min={0}
                          max={10}
                          value={testCase.points}
                          onChange={(e) => onUpdateTestCase(index, { points: parseInt(e.target.value) || 0 })}
                          className="w-16 border border-slate-300 rounded-md px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                        />
                      </div>
                      <div className="flex items-center gap-2">
                        <label className="text-sm text-slate-700">Ordering:</label>
                        <input
                          type="number"
                          min={0}
                          value={index}
                          className="w-16 border border-slate-300 rounded-md px-2 py-1 text-sm bg-slate-100"
                          readOnly
                        />
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              
              {question.testCases.length === 0 && (
                <div className="text-center text-slate-500 py-8">
                  Chưa có test case nào. Nhấn "Thêm Test Case" để thêm.
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Options for Multiple Choice/True False */}
      {(question.questionType === 'MULTIPLE_CHOICE' || question.questionType === 'TRUE_FALSE') && (
        <div className="bg-white border border-slate-200 rounded-lg p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold text-slate-900">
              {question.questionType === 'TRUE_FALSE' ? 'Lựa chọn Đúng/Sai' : 'Các lựa chọn'}
            </h3>
            {question.questionType === 'MULTIPLE_CHOICE' && (
              <button
                type="button"
                onClick={onAddOption}
                className="px-3 py-1.5 text-xs font-medium text-white bg-[#ff6a00] rounded-md hover:bg-[#e55a00]"
              >
                + Thêm lựa chọn
              </button>
            )}
          </div>
          
          <div className="space-y-3">
            {question.questionType === 'TRUE_FALSE' ? (
              // True/False options
              <>
                <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                  <input
                    type="radio"
                    name={`question-${question.orderIndex}`}
                    checked={question.options.length > 0 && question.options[0]?.isCorrect}
                    onChange={() => {
                      const newOptions = [
                        { optionText: 'Đúng', isCorrect: true, orderIndex: 1 },
                        { optionText: 'Sai', isCorrect: false, orderIndex: 2 }
                      ];
                      onUpdate({ options: newOptions });
                    }}
                    className="text-[#ff6a00] focus:ring-[#ff6a00]"
                  />
                  <span className="text-sm font-medium text-slate-700">Đúng</span>
                </label>
                
                <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                  <input
                    type="radio"
                    name={`question-${question.orderIndex}`}
                    checked={question.options.length > 1 && question.options[1]?.isCorrect}
                    onChange={() => {
                      const newOptions = [
                        { optionText: 'Đúng', isCorrect: false, orderIndex: 1 },
                        { optionText: 'Sai', isCorrect: true, orderIndex: 2 }
                      ];
                      onUpdate({ options: newOptions });
                    }}
                    className="text-[#ff6a00] focus:ring-[#ff6a00]"
                  />
                  <span className="text-sm font-medium text-slate-700">Sai</span>
                </label>
              </>
            ) : (
              // Multiple choice options
              <>
                {question.options.map((option, index) => (
                  <div key={index} className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg">
                    <input
                      type="radio"
                      name={`question-${question.orderIndex}`}
                      checked={option.isCorrect}
                      onChange={() => {
                        const updatedOptions = question.options.map((opt, i) => ({
                          ...opt,
                          isCorrect: i === index
                        }));
                        onUpdate({ options: updatedOptions });
                      }}
                      className="text-[#ff6a00] focus:ring-[#ff6a00]"
                    />
                    <input
                      type="text"
                      value={option.optionText}
                      onChange={(e) => onUpdateOption(index, { optionText: e.target.value })}
                      className="flex-1 border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                      placeholder={`Lựa chọn ${String.fromCharCode(65 + index)}`}
                    />
                    <button
                      type="button"
                      onClick={() => onDeleteOption(index)}
                      className="text-red-500 hover:text-red-700 text-sm"
                    >
                      Xóa
                    </button>
                  </div>
                ))}
                
                {question.options.length === 0 && (
                  <div className="text-center text-slate-500 py-4">
                    Chưa có lựa chọn nào. Nhấn "Thêm lựa chọn" để thêm.
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function CreateAssignmentPage() {
  const router = useRouter();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentStep, setCurrentStep] = useState(1); // 1: Basic Info, 2: Questions
  const [selectedQuestionIndex, setSelectedQuestionIndex] = useState<number | null>(null);
  
  const [formData, setFormData] = useState({
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
    fetchCourses();
  }, []);

  const fetchCourses = async () => {
    try {
      const teacherCourses = await CourseService.getTeacherCourses();
      setCourses(teacherCourses);
    } catch (error) {
      console.error('Failed to fetch courses:', error);
      alert('Không thể tải danh sách khóa học');
    }
  };

  // Question management functions
  const handleAddQuestion = () => {
    const newQuestion: Question = {
      title: `Câu hỏi ${questions.length + 1}`,
      description: '',
      questionType: 'PROGRAMMING',
      points: 10,
      orderIndex: questions.length + 1,
      testCases: [],
      options: [],
      answerCode: '',
      starterCode: '// TODO: Implement this function\nfunction yourFunction() {\n  // Your code here\n}',
      validateOnSave: false
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
    const reorderedQuestions = updatedQuestions.map((q, i) => ({
      ...q,
      orderIndex: i + 1,
      title: q.title.includes('Câu hỏi') ? `Câu hỏi ${i + 1}` : q.title
    }));
    setQuestions(reorderedQuestions);
    setSelectedQuestionIndex(null);
  };

  // Test case management functions
  const handleAddTestCase = (questionIndex: number) => {
    const newTestCase: EnhancedTestCase = {
      input: '',
      expectedOutput: '',
      isHidden: false,
      points: 1,
      testCode: '',
      useAsExample: false,
      isValidated: false
    };
    
    const updatedQuestions = [...questions];
    updatedQuestions[questionIndex].testCases.push(newTestCase);
    setQuestions(updatedQuestions);
  };

  const handleUpdateTestCase = (questionIndex: number, testCaseIndex: number, updatedTestCase: Partial<EnhancedTestCase>) => {
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

  // Answer validation function
  const handleValidateAnswer = async (questionIndex: number) => {
    const question = questions[questionIndex];
    if (!question.answerCode || question.testCases.length === 0) {
      alert('Vui lòng nhập code đáp án và ít nhất một test case trước khi validate.');
      return;
    }

    try {
      setLoading(true);
      
      const updatedQuestions = [...questions];
      let validatedCount = 0;
      
      for (let i = 0; i < question.testCases.length; i++) {
        const testCase = question.testCases[i];
        
        if (testCase.testCode && question.validateOnSave) {
          try {
            console.log(`Validating test case ${i + 1}:`, {
              answerCode: question.answerCode,
              testCode: testCase.testCode,
              input: testCase.input
            });
            
            // Call the real API service for code validation
            const result = await AssignmentService.validateAnswerCode(
              question.answerCode,
              testCase.testCode,
              'c', // Default to C language, can be made configurable
              testCase.input
            );
            
            console.log(`Test case ${i + 1} result:`, result);
            
            if (result.success && result.output) {
              // Update the test case with the actual output from code execution
              updatedQuestions[questionIndex].testCases[i] = {
                ...testCase,
                expectedOutput: result.output.trim(),
                isValidated: true
              };
              validatedCount++;
            } else {
              // If execution failed, keep existing output but mark as error
              console.warn(`Test case ${i + 1} validation failed:`, result.error);
              updatedQuestions[questionIndex].testCases[i] = {
                ...testCase,
                isValidated: false
              };
            }
          } catch (error) {
            console.error(`Error validating test case ${i + 1}:`, error);
            // Keep the test case unchanged on error
            updatedQuestions[questionIndex].testCases[i] = {
              ...testCase,
              isValidated: false
            };
          }
        }
      }
      
      setQuestions(updatedQuestions);
      
      if (validatedCount > 0) {
        alert(`Đã validate thành công ${validatedCount}/${question.testCases.length} test cases! Các Expected Output đã được tự động tạo.`);
      } else {
        alert('Không có test case nào được validate thành công. Vui lòng kiểm tra lại code và test cases.');
      }
      
    } catch (error) {
      console.error('Validation error:', error);
      alert('Có lỗi xảy ra khi validate code. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  // Option management functions
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
    updatedQuestions[questionIndex].options = updatedQuestions[questionIndex].options.map((opt, i) => ({
      ...opt,
      orderIndex: i + 1
    }));
    setQuestions(updatedQuestions);
  };

  const calculateTotalScore = () => {
    return questions.reduce((total, question) => total + question.points, 0);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.courseId) {
      alert('Vui lòng chọn khóa học');
      return;
    }

    if (currentStep === 1) {
      // Move to questions step
      setCurrentStep(2);
      return;
    }

    // Final submission
    setLoading(true);
    try {
      const totalScore = calculateTotalScore();
      
      const requestData: CreateAssignmentRequest = {
        ...formData,
        maxScore: totalScore || formData.maxScore,
        startTime: formData.startTime || undefined,
        endTime: formData.endTime || undefined,
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
      router.push('/teacher');
    } catch (error) {
      console.error('Failed to create assignment:', error);
      alert('Có lỗi xảy ra khi tạo bài tập. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : 
              type === 'number' ? parseInt(value) : value
    }));
  };

  return (
    <MainLayout>
      <div className="h-full flex flex-col">
        {/* Header */}
        <div className="bg-white border-b px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Link 
                href="/teacher" 
                className="text-slate-600 hover:text-slate-900"
              >
                ← Quay lại
              </Link>
              <div>
                <h1 className="text-[#ff6a00] font-semibold text-xl">Tạo bài tập mới</h1>
                <p className="text-slate-600 text-sm">
                  {currentStep === 1 ? 'Bước 1: Thông tin cơ bản' : 'Bước 2: Thêm câu hỏi và test case'}
                </p>
              </div>
            </div>
            
            {/* Step indicator */}
            <div className="flex items-center gap-2">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                currentStep >= 1 ? 'bg-[#ff6a00] text-white' : 'bg-slate-200 text-slate-600'
              }`}>
                1
              </div>
              <div className="w-8 h-0.5 bg-slate-200"></div>
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                currentStep >= 2 ? 'bg-[#ff6a00] text-white' : 'bg-slate-200 text-slate-600'
              }`}>
                2
              </div>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 flex">
          {currentStep === 1 ? (
            // Step 1: Basic Information
            <div className="flex-1 p-6">
              <div className="max-w-4xl mx-auto">
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* Basic Information */}
                  <div className="bg-white border border-slate-200 rounded-lg p-6">
                    <h2 className="text-lg font-semibold text-slate-900 mb-6">Thông tin bài tập</h2>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                          Tiêu đề bài tập *
                        </label>
                        <input
                          type="text"
                          name="title"
                          required
                          value={formData.title}
                          onChange={handleInputChange}
                          className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                          placeholder="Nhập tiêu đề bài tập"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                          Khóa học *
                        </label>
                        <select
                          name="courseId"
                          required
                          value={formData.courseId}
                          onChange={handleInputChange}
                          className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                        >
                          <option value={0}>Chọn khóa học</option>
                          {courses.map((course) => (
                            <option key={course.id} value={course.id}>
                              {course.name} ({course.code})
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>

                    <div className="mt-6">
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Mô tả
                      </label>
                      <textarea
                        name="description"
                        rows={4}
                        value={formData.description}
                        onChange={handleInputChange}
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                        placeholder="Mô tả chi tiết về bài tập"
                      />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                          Loại bài tập *
                        </label>
                        <select
                          name="type"
                          required
                          value={formData.type}
                          onChange={handleInputChange}
                          className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                        >
                          <option value="EXERCISE">Bài tập</option>
                          <option value="EXAM">Bài thi</option>
                          <option value="PROJECT">Dự án</option>
                          <option value="QUIZ">Kiểm tra nhanh</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                          Điểm tối đa *
                        </label>
                        <input
                          type="number"
                          name="maxScore"
                          required
                          min={1}
                          max={1000}
                          value={formData.maxScore}
                          onChange={handleInputChange}
                          className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                          placeholder="Sẽ tự động tính từ câu hỏi"
                        />
                        <p className="text-xs text-slate-500 mt-1">
                          Điểm này sẽ được cập nhật tự động dựa trên tổng điểm các câu hỏi
                        </p>
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                          Thời gian (phút) *
                        </label>
                        <input
                          type="number"
                          name="timeLimit"
                          required
                          min={1}
                          max={600}
                          value={formData.timeLimit}
                          onChange={handleInputChange}
                          className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                        />
                      </div>
                    </div>

                    {/* Time Settings */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                          Thời gian bắt đầu
                        </label>
                        <input
                          type="datetime-local"
                          name="startTime"
                          value={formData.startTime}
                          onChange={handleInputChange}
                          className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                          Thời gian kết thúc
                        </label>
                        <input
                          type="datetime-local"
                          name="endTime"
                          value={formData.endTime}
                          onChange={handleInputChange}
                          className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#ff6a00]"
                        />
                      </div>
                    </div>

                    {/* Options */}
                    <div className="space-y-3 mt-6">
                      <label className="flex items-center gap-3">
                        <input
                          type="checkbox"
                          name="allowLateSubmission"
                          checked={formData.allowLateSubmission}
                          onChange={handleInputChange}
                          className="rounded border-slate-300 text-[#ff6a00] focus:ring-[#ff6a00]"
                        />
                        <span className="text-sm font-medium text-slate-700">
                          Cho phép nộp bài muộn
                        </span>
                      </label>

                      <label className="flex items-center gap-3">
                        <input
                          type="checkbox"
                          name="autoGrade"
                          checked={formData.autoGrade}
                          onChange={handleInputChange}
                          className="rounded border-slate-300 text-[#ff6a00] focus:ring-[#ff6a00]"
                        />
                        <span className="text-sm font-medium text-slate-700">
                          Tự động chấm điểm
                        </span>
                      </label>
                    </div>
                  </div>

                  {/* Buttons */}
                  <div className="flex justify-end gap-3">
                    <Link
                      href="/teacher"
                      className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-md hover:bg-slate-50"
                    >
                      Hủy
                    </Link>
                    <button
                      type="submit"
                      className="px-6 py-2 text-sm font-medium text-white bg-[#ff6a00] border border-transparent rounded-md hover:bg-[#e55a00]"
                    >
                      Tiếp theo: Thêm câu hỏi →
                    </button>
                  </div>
                </form>
              </div>
            </div>
          ) : (
            // Step 2: Questions Management
            <>
              {/* Sidebar - Question List */}
              <div className="w-1/4 border-r bg-slate-50 p-4 overflow-y-auto">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="font-semibold text-slate-900">Danh sách câu hỏi</h3>
                  <button
                    type="button"
                    onClick={handleAddQuestion}
                    className="px-3 py-1.5 text-xs font-medium text-white bg-[#ff6a00] rounded-md hover:bg-[#e55a00]"
                  >
                    + Thêm câu hỏi
                  </button>
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
                          <h4 className="text-sm font-medium text-slate-900 truncate">
                            {question.title}
                          </h4>
                          <p className="text-xs text-slate-500 mt-1">
                            {question.questionType} • {question.points} điểm
                          </p>
                          {question.questionType === 'PROGRAMMING' && (
                            <p className="text-xs text-slate-400">
                              {question.testCases.length} test case(s)
                            </p>
                          )}
                        </div>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteQuestion(index);
                          }}
                          className="text-red-500 hover:text-red-700 text-xs ml-2"
                        >
                          ×
                        </button>
                      </div>
                    </div>
                  ))}
                  
                  {questions.length === 0 && (
                    <div className="text-center text-slate-500 text-sm py-8">
                      Chưa có câu hỏi nào.
                      <br />
                      Nhấn "Thêm câu hỏi" để bắt đầu.
                    </div>
                  )}
                </div>

                <div className="mt-6 p-3 bg-blue-50 rounded-lg">
                  <h4 className="text-sm font-medium text-blue-900">Tổng kết</h4>
                  <div className="text-xs text-blue-700 mt-2">
                    <p>Số câu hỏi: {questions.length}</p>
                    <p>Tổng điểm: {calculateTotalScore()}</p>
                  </div>
                </div>
              </div>

              {/* Main Content - Question Editor */}
              <div className="flex-1 p-6 overflow-y-auto pb-24">
                {selectedQuestionIndex !== null ? (
                  <QuestionEditor
                    question={questions[selectedQuestionIndex]}
                    onUpdate={(updatedQuestion) => handleUpdateQuestion(selectedQuestionIndex, updatedQuestion)}
                    onAddTestCase={() => handleAddTestCase(selectedQuestionIndex)}
                    onUpdateTestCase={(testCaseIndex, updatedTestCase) => handleUpdateTestCase(selectedQuestionIndex, testCaseIndex, updatedTestCase)}
                    onDeleteTestCase={(testCaseIndex) => handleDeleteTestCase(selectedQuestionIndex, testCaseIndex)}
                    onAddOption={() => handleAddOption(selectedQuestionIndex)}
                    onUpdateOption={(optionIndex, updatedOption) => handleUpdateOption(selectedQuestionIndex, optionIndex, updatedOption)}
                    onDeleteOption={(optionIndex) => handleDeleteOption(selectedQuestionIndex, optionIndex)}
                    onValidateAnswer={() => handleValidateAnswer(selectedQuestionIndex)}
                  />
                ) : (
                  <div className="text-center text-slate-500 mt-16">
                    <p className="text-lg mb-2">Chọn một câu hỏi để chỉnh sửa</p>
                    <p className="text-sm">hoặc thêm câu hỏi mới từ sidebar bên trái</p>
                  </div>
                )}
              </div>

              {/* Action Bar */}
              <div className="fixed bottom-0 left-0 right-0 bg-white border-t px-6 py-4 z-10">
                <div className="flex justify-between items-center">
                  <button
                    type="button"
                    onClick={() => setCurrentStep(1)}
                    className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-md hover:bg-slate-50"
                  >
                    ← Quay lại bước trước
                  </button>
                  
                  <div className="flex gap-3">
                    <Link
                      href="/teacher"
                      className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-md hover:bg-slate-50"
                    >
                      Hủy
                    </Link>
                    <button
                      type="button"
                      onClick={() => handleSubmit({ preventDefault: () => {} } as React.FormEvent)}
                      disabled={loading || questions.length === 0}
                      className="px-6 py-2 text-sm font-medium text-white bg-[#ff6a00] border border-transparent rounded-md hover:bg-[#e55a00] disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {loading ? 'Đang tạo...' : 'Tạo bài tập'}
                    </button>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </MainLayout>
  );
}

export default withAuth(CreateAssignmentPage, {
  requiredRoles: [Role.TEACHER],
});
