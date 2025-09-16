"use client";

import { useState, useCallback } from 'react';
import { AssignmentService } from '@/services/assignment.service';
import { StudentQuestionResponse, TestCaseResponse } from '@/types/api';

interface ProgrammingQuestionComponentProps {
  question: StudentQuestionResponse;
  answer: string;
  onAnswerChange: (answer: string) => void;
  onTestResults: (results: any) => void;
  isChecking: boolean;
  setIsChecking: (checking: boolean) => void;
  testResults: any;
}

export default function ProgrammingQuestionComponent({
  question,
  answer,
  onAnswerChange,
  onTestResults,
  isChecking,
  setIsChecking,
  testResults
}: ProgrammingQuestionComponentProps) {
  const [showExampleTests, setShowExampleTests] = useState(true);
  const [codeOutput, setCodeOutput] = useState<string>('');

  // Load starter code if answer is empty
  const displayCode = answer || question.starterCode || `// TODO: Implement your solution here\nfunction solution() {\n    // Your code here\n}`;

  const handleRunCode = useCallback(async () => {
    if (!answer.trim()) {
      alert('Vui lòng nhập code trước khi chạy thử!');
      return;
    }

    try {
      setIsChecking(true);
      setCodeOutput('Đang chạy code...');

      // Run code without test cases (just execution)
      const result = await AssignmentService.runQuestionCode(
        question.id, 
        question.id, 
        answer, 
        question.language || 'c'
      );

      if (result.success) {
        setCodeOutput(result.output || 'Biên dịch chương trình thành công');
      } else {
        setCodeOutput(`Lỗi: ${result.error || result.compilationError || 'Có lỗi xảy ra'}`);
      }
    } catch (error) {
      console.error('Error running code:', error);
      setCodeOutput(`Lỗi kết nối: ${error}`);
    } finally {
      setIsChecking(false);
    }
  }, [answer, question.id, question.language]);

  const handleTestCode = useCallback(async () => {
    if (!answer.trim()) {
      alert('Vui lòng nhập code trước khi test!');
      return;
    }

    try {
      setIsChecking(true);

      // Run code with test cases
      const result = await AssignmentService.testQuestionCode(
        question.id, 
        question.id, 
        answer, 
        question.language || 'c'
      );

      onTestResults(result);
    } catch (error) {
      console.error('Error testing code:', error);
      onTestResults({
        success: false,
        error: `Lỗi khi test code: ${error}`,
        testCases: []
      });
    } finally {
      setIsChecking(false);
    }
  }, [answer, question.id, question.language, onTestResults]);

  return (
    <div className="space-y-6">
      {/* Question Description */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="font-semibold text-blue-900 mb-2">Đề bài:</h3>
        <p className="text-blue-800 whitespace-pre-wrap">{question.description}</p>
        
        {question.language && (
          <div className="mt-3">
            <span className="inline-block bg-blue-600 text-white text-xs px-2 py-1 rounded">
              Ngôn ngữ: {question.language.toUpperCase()}
            </span>
          </div>
        )}
      </div>

      {/* Example Test Cases */}
      {question.exampleTestCases && question.exampleTestCases.length > 0 && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-green-900">Test Cases:</h3>
            <button
              onClick={() => setShowExampleTests(!showExampleTests)}
              className="text-sm text-green-700 hover:text-green-900"
            >
              {showExampleTests ? 'Ẩn' : 'Hiện'}
            </button>
          </div>
          
          {showExampleTests && (
            <div className="space-y-2">
              {question.exampleTestCases.map((testCase, index) => (
                <div key={index} className="bg-white border border-green-200 rounded p-3">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                    <div>
                      <span className="font-medium text-green-800">Input:</span>
                      <pre className="mt-1 bg-gray-100 p-2 rounded text-xs font-mono">
                        {testCase.input || '(empty)'}
                      </pre>
                    </div>
                    <div>
                      <span className="font-medium text-green-800">Expected Output:</span>
                      <pre className="mt-1 bg-gray-100 p-2 rounded text-xs font-mono">
                        {testCase.expectedOutput}
                      </pre>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Code Editor */}
      <div className="bg-white border border-slate-300 rounded-lg">
        <div className="flex items-center justify-between p-3 border-b border-slate-200 bg-slate-50">
          <div className="flex gap-2">
            <button
              onClick={handleRunCode}
              disabled={isChecking}
              className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 flex items-center gap-1"
              title="Chạy code với input tùy chỉnh (không chấm điểm)"
            >
               {isChecking ? 'Đang chạy...' : 'Chạy thử'}
            </button>
            <button
              onClick={handleTestCode}
              disabled={isChecking}
              className="px-3 py-1.5 text-sm bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 flex items-center gap-1"
              title="Chạy với test cases từ giảng viên để chấm điểm"
            >
               {isChecking ? 'Đang kiểm tra...' : 'Kiểm tra với Test Cases'}
            </button>
          </div>
        </div>
        
        <div className="p-3">
          <textarea
            value={answer}
            onChange={(e) => onAnswerChange(e.target.value)}
            placeholder={displayCode}
            className="w-full h-96 font-mono text-sm border border-slate-300 rounded p-3 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500"
            style={{ 
              fontFamily: 'Monaco, Menlo, "Ubuntu Mono", Consolas, source-code-pro, monospace',
              lineHeight: '1.5'
            }}
          />
        </div>
      </div>

      {/* Code Output */}
      {codeOutput && (
        <div className="bg-slate-50 border border-slate-300 rounded-lg p-4">
          <h4 className="font-semibold text-slate-900 mb-2">Output:</h4>
          <pre className="text-sm font-mono bg-white border border-slate-200 rounded p-3 overflow-auto max-h-40">
            {codeOutput}
          </pre>
        </div>
      )}

      {/* Test Results */}
      {testResults && (
        <div className={`border rounded-lg p-4 ${
          testResults.success ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
        }`}>
          <div className="flex items-center gap-2 mb-3">
            <div className={`w-5 h-5 rounded-full flex items-center justify-center ${
              testResults.success ? 'bg-green-500' : 'bg-red-500'
            }`}>
              <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {testResults.success ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                )}
              </svg>
            </div>
            <span className={`font-medium ${
              testResults.success ? 'text-green-800' : 'text-red-800'
            }`}>
              Kết quả chấm điểm tự động
            </span>
          </div>
          
          {/* Auto-grading explanation */}
          {/* <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <h5 className="font-medium text-blue-900 mb-2">📋 Quy trình chấm điểm:</h5>
            <div className="text-sm text-blue-800 space-y-1">
              <p>✅ Code của bạn được chạy với bộ test case từ giảng viên</p>
              <p>✅ So sánh kết quả output thực tế với expected output</p>
              <p>✅ Điểm số = (Số test cases pass × trọng số) / Tổng trọng số</p>
              <p>❌ <strong>KHÔNG</strong> so sánh trực tiếp code với đáp án giảng viên</p>
            </div>
          </div> */}
          
          {/* System message if available */}
          {testResults.message && (
            <div className="mb-4 p-3 bg-slate-50 border border-slate-200 rounded">
              <p className="text-sm text-slate-700">{testResults.message}</p>
            </div>
          )}
          
          <div className="mb-3">
            <span className={`font-medium ${
              testResults.success ? 'text-green-800' : 'text-red-800'
            }`}>
              {testResults.success 
                ? ` Hoàn thành! ${testResults.passedTests || 0}/${testResults.totalTests || 0} test cases đạt yêu cầu` 
                : ` Cần cải thiện! ${testResults.passedTests || 0}/${testResults.totalTests || 0} test cases đạt yêu cầu`}
            </span>
          </div>

          {testResults.testResults && testResults.testResults.length > 0 && (
            <div className="space-y-2 mb-4">
              <h5 className="font-medium text-slate-800">Chi tiết test cases:</h5>
              <div className="overflow-x-auto">
                <table className="w-full text-sm border-collapse">
                  <thead>
                    <tr className={`${testResults.success ? 'bg-green-100' : 'bg-red-100'}`}>
                      <th className="text-left px-3 py-2 border">Test</th>
                      <th className="text-left px-3 py-2 border">Expected</th>
                      <th className="text-left px-3 py-2 border">Got</th>
                      <th className="text-center px-3 py-2 border">Result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {testResults.testResults.map((test: any, index: number) => (
                      <tr key={index} className={test.passed ? "bg-green-50" : "bg-red-50"}>
                        <td className="px-3 py-2 border font-mono text-xs">{test.input || '(empty)'}</td>
                        <td className="px-3 py-2 border font-mono text-xs">{test.expectedOutput}</td>
                        <td className="px-3 py-2 border font-mono text-xs">{test.actualOutput || '(no output)'}</td>
                        <td className="px-3 py-2 border text-center">
                          <div className={`w-4 h-4 rounded-full flex items-center justify-center mx-auto ${
                            test.passed ? 'bg-green-500' : 'bg-red-500'
                          }`}>
                            <svg className="w-2 h-2 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              {test.passed ? (
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path>
                              ) : (
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M6 18L18 6M6 6l12 12"></path>
                              )}
                            </svg>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {testResults.error && (
            <div className="mt-3 p-3 bg-red-100 rounded border border-red-200">
              <h5 className="font-medium text-red-800 mb-1">Error:</h5>
              <pre className="text-xs text-red-700 whitespace-pre-wrap">{testResults.error}</pre>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div>
              <span className={`inline-block px-3 py-1.5 rounded-lg font-medium text-white ${
                testResults.success ? 'bg-green-600' : 'bg-red-600'
              }`}>
                {testResults.success ? 'Đạt yêu cầu' : ' Chưa đạt'}
              </span>
            </div>
            <div className="text-slate-700">
              <span className="font-medium">Điểm tạm thời:</span> 
              <span className="ml-1 font-bold text-blue-600">
                {testResults.score || 0}/{question.points}
              </span>
            </div>
            <div className="text-slate-600">
              <span className="font-medium">Test cases:</span> 
              <span className="ml-1">
                {question.totalTestCases || 0} 
                {question.totalTestCases && question.exampleTestCases?.length && (
                  <span className="text-xs text-slate-500">
                    ({question.totalTestCases - question.exampleTestCases.length} ẩn + {question.exampleTestCases.length} công khai)
                  </span>
                )}
              </span>
            </div>
          </div>
          
        </div>
      )}
    </div>
  );
}