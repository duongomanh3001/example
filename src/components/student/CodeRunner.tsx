"use client";

import { useState, useCallback, useRef } from 'react';
import Image from 'next/image';
import { AssignmentService } from '@/services/assignment.service';
import { StudentQuestionResponse } from '@/types/api';

interface CodeRunnerProps {
  question: StudentQuestionResponse;
  answer: string;
  onAnswerChange: (answer: string) => void;
  onTestResults: (results: any) => void;
  isChecking: boolean;
  setIsChecking: (checking: boolean) => void;
  testResults: any;
}

// Simple code editor with line numbers
function CodeEditor({ value, onChange, disabled }: { value: string; onChange: (v: string) => void; disabled: boolean }) {
  const lineNumbers = value.split('\n').length;
  
  return (
    <div className="flex bg-white border border-gray-300 rounded font-mono text-sm">
      {/* Line numbers */}
      <div className="bg-gray-50 border-r border-gray-300 px-3 py-3 text-right text-gray-500 select-none" style={{ minWidth: '50px' }}>
        {Array.from({ length: lineNumbers }, (_, i) => (
          <div key={i} style={{ lineHeight: '1.5', height: '21px' }}>{i + 1}</div>
        ))}
      </div>
      
      {/* Code input */}
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className="flex-1 p-3 bg-white resize-none focus:outline-none"
        style={{ 
          fontFamily: 'Consolas, Monaco, "Courier New", monospace',
          lineHeight: '1.5',
          minHeight: '400px',
          color: '#000'
        }}
        spellCheck={false}
      />
    </div>
  );
}

// Enhanced timeout controller
class EnhancedTimeoutController {
  private controller: AbortController;
  private timeoutId: NodeJS.Timeout;
  private startTime: number;
  
  constructor(timeoutMs: number = 30000) {
    this.controller = new AbortController();
    this.startTime = Date.now();
    
    this.timeoutId = setTimeout(() => {
      this.controller.abort();
    }, timeoutMs);
  }

  get signal() {
    return this.controller.signal;
  }

  get elapsedTime() {
    return Date.now() - this.startTime;
  }

  clear() {
    clearTimeout(this.timeoutId);
  }

  abort() {
    this.clear();
    this.controller.abort();
  }
}

export default function CodeRunner({
  question,
  answer,
  onAnswerChange,
  onTestResults,
  isChecking,
  setIsChecking,
  testResults
}: CodeRunnerProps) {
  const [showExampleTests, setShowExampleTests] = useState(true);
  const [codeOutput, setCodeOutput] = useState<string>('');
  const [currentTimeout, setCurrentTimeout] = useState<EnhancedTimeoutController | null>(null);
  const [executionStartTime, setExecutionStartTime] = useState<number | null>(null);

  const handleRunCode = useCallback(async () => {
    if (!answer.trim()) {
      alert('Vui lòng nhập code trước khi chạy thử!');
      return;
    }

    const timeoutController = new EnhancedTimeoutController(20000);
    setCurrentTimeout(timeoutController);
    setExecutionStartTime(Date.now());

    try {
      setIsChecking(true);
      setCodeOutput('Đang chạy code...');

      const result = await Promise.race([
        AssignmentService.runQuestionCode(
          question.id, 
          question.id, 
          answer, 
          question.language || 'c'
        ),
        new Promise<never>((_, reject) => {
          timeoutController.signal.addEventListener('abort', () => {
            reject(new Error('Timeout: Quá thời gian chờ (20s)'));
          });
        })
      ]);

      if (result.success) {
        setCodeOutput(result.output || 'Chạy thành công - không có output');
      } else {
        setCodeOutput(`Lỗi: ${result.error || result.compilationError || 'Lỗi không xác định'}`);
      }

    } catch (error) {
      console.error('Code execution error:', error);
      
      if (error instanceof Error && error.message.includes('Timeout')) {
        setCodeOutput(error.message);
      } else if (error instanceof Error && error.message.includes('fetch')) {
        setCodeOutput('Lỗi kết nối. Vui lòng kiểm tra internet và thử lại.');
      } else {
        setCodeOutput(`Lỗi: ${error}`);
      }
    } finally {
      timeoutController.clear();
      setCurrentTimeout(null);
      setExecutionStartTime(null);
      setIsChecking(false);
    }
  }, [answer, question.id, question.language]);

  const handleTestCode = useCallback(async () => {
    if (!answer.trim()) {
      alert('Vui lòng nhập code trước khi test!');
      return;
    }

    if (!question.exampleTestCases || question.exampleTestCases.length === 0) {
      onTestResults({
        success: false,
        error: 'Câu hỏi này không có test case. Vui lòng liên hệ giảng viên.',
        testCases: [],
        passedTests: 0,
        totalTests: 0
      });
      return;
    }

    const timeoutController = new EnhancedTimeoutController(60000);
    setCurrentTimeout(timeoutController);
    setExecutionStartTime(Date.now());

    try {
      setIsChecking(true);

      const result = await Promise.race([
        AssignmentService.testQuestionCode(
          question.id, 
          question.id, 
          answer, 
          question.language || 'c'
        ),
        new Promise<never>((_, reject) => {
          timeoutController.signal.addEventListener('abort', () => {
            reject(new Error('Timeout: Quá thời gian chờ chấm điểm (60s). Code có thể chạy quá chậm hoặc vô hạn.'));
          });
        })
      ]);

      const enhancedResult = {
        ...result,
        executionTime: timeoutController.elapsedTime,
        timestamp: new Date().toISOString(),
        questionId: question.id,
        language: question.language || 'c',
        codeLength: answer.length
      };

      onTestResults(enhancedResult);
      
    } catch (error) {
      console.error('Code testing error:', error);
      
      let errorMessage = 'Có lỗi xảy ra khi chấm điểm';
      
      if (error instanceof Error) {
        if (error.message.includes('Timeout') || error.message.includes('timeout')) {
          errorMessage = error.message;
        } else if (error.message.includes('fetch') || error.message.includes('network')) {
          errorMessage = 'Lỗi kết nối. Vui lòng kiểm tra internet và thử lại.';
        } else if (error.message.includes('401') || error.message.includes('Unauthorized')) {
          errorMessage = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
        } else if (error.message.includes('500')) {
          errorMessage = 'Lỗi server. Vui lòng thử lại sau.';
        } else {
          errorMessage = error.message;
        }
      }
      
      onTestResults({
        success: false,
        error: errorMessage,
        testCases: [],
        passedTests: 0,
        totalTests: question.exampleTestCases?.length || 0,
        executionTime: timeoutController.elapsedTime,
        timestamp: new Date().toISOString()
      });
      
    } finally {
      timeoutController.clear();
      setCurrentTimeout(null);
      setExecutionStartTime(null);
      setIsChecking(false);
    }
  }, [answer, question.id, question.language, question.exampleTestCases, onTestResults]);


  const getElapsedTime = useCallback(() => {
    if (!executionStartTime) return 0;
    return Math.floor((Date.now() - executionStartTime) / 1000);
  }, [executionStartTime]);

  return (
    <div className="w-full">
      {/* Question Description */}
      <div className="border-b border-gray-300">
        <div className="px-4 py-3 bg-gray-100">
          <h3 className="font-semibold text-gray-900">Đề Bài</h3>
        </div>
        <div className="px-4 py-4 bg-white">
          <p className="text-gray-800 whitespace-pre-wrap">{question.description}</p>
        </div>
      </div>

      {/* Example Test Cases */}
      {question.exampleTestCases && question.exampleTestCases.length > 0 && (
        <div className="border-b border-gray-300">
          <div className="flex items-center justify-between px-4 py-3 bg-gray-100">
            <h3 className="font-semibold text-gray-900">Test Case</h3>
            <button
              onClick={() => setShowExampleTests(!showExampleTests)}
              className="text-sm text-gray-600 hover:text-gray-900 px-3 py-1 border border-gray-300 rounded bg-white"
            >
              {showExampleTests ? 'Ẩn' : 'Hiện'}
            </button>
          </div>
          
          {showExampleTests && (
            <div className="px-4 py-4 bg-white space-y-3">
              {question.exampleTestCases.map((testCase, index) => (
                <div key={index} className="border border-gray-200 rounded p-3 bg-gray-50">
                  <div className="font-medium text-gray-700 text-sm mb-3">Test Case {index + 1}</div>
                  <div className="grid grid-cols-2 gap-3 text-sm">
                    <div>
                      <span className="font-medium text-gray-700">INPUT</span>
                      <pre className="mt-1 bg-white p-2 rounded text-xs font-mono border border-gray-200">
                        {testCase.input || '(empty)'}
                      </pre>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">EXPECTED OUTPUT</span>
                      <pre className="mt-1 bg-white p-2 rounded text-xs font-mono border border-gray-200">
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

      {/* Giải pháp của bạn */}
      <div className="border-b border-gray-300">
        <div className="flex items-center justify-between px-4 py-3 bg-gray-100">
          <h3 className="font-semibold text-gray-900">Đáp Án</h3>
          <div className="flex gap-2">
            <button
              onClick={handleRunCode}
              disabled={isChecking}
              className="p-2 bg-white border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
              title={isChecking ? 'Đang chạy...' : 'RUN'}
            >
              <Image 
                src="/icon/running.png" 
                alt="Run" 
                width={24} 
                height={24}
              />
            </button>
            
            <button
              onClick={handleTestCode}
              disabled={isChecking}
              className="p-2 bg-white border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
              title={isChecking ? 'Đang test...' : 'TEST'}
            >
              <Image 
                src="/icon/testing.png" 
                alt="Test" 
                width={24} 
                height={24}
              />
            </button>
            
          </div>
        </div>
        
        <div className="px-4 py-4 bg-white">
          <CodeEditor 
            value={answer}
            onChange={onAnswerChange}
            disabled={isChecking}
          />
        </div>
      </div>

      {/* RUN Output - Only show output */}
      {codeOutput && (
        <div className="border-b border-gray-300">
          <div className="px-4 py-3 bg-gray-100">
            <h4 className="font-semibold text-gray-900">OUTPUT</h4>
          </div>
          <div className="px-4 py-4 bg-white">
            <pre className="text-sm font-mono bg-gray-50 border border-gray-200 rounded p-3 overflow-auto whitespace-pre-wrap">
              {codeOutput}
            </pre>
          </div>
        </div>
      )}

      {/* TEST Results - Only show table and score */}
      {testResults && (
        <div className="border-b border-gray-300">
          <div className={`px-4 py-3 ${
            testResults.passedTests === testResults.totalTests && testResults.totalTests > 0 ? 'bg-green-50' : 'bg-red-50'
          }`}>
            <div className="flex items-center justify-between">
              <h4 className="font-semibold text-gray-900">Kết Quả</h4>
              <span className={`text-sm font-medium ${
                testResults.passedTests === testResults.totalTests && testResults.totalTests > 0 ? 'text-green-700' : 'text-red-700'
              }`}>
                {testResults.passedTests || 0}/{testResults.totalTests || 0} PASSED
              </span>
            </div>
          </div>

          <div className="px-4 py-4 bg-white">
            {/* Error message if any */}
            {testResults.error && (
              <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded">
                <p className="text-sm text-red-700">{testResults.error}</p>
              </div>
            )}

            {/* Detailed test results table */}
            {testResults.testResults && testResults.testResults.length > 0 && (
              <div className="mb-4 overflow-x-auto">
                <table className="w-full text-sm border-collapse border border-gray-300">
                  <thead>
                    <tr className="bg-gray-100">
                      <th className="text-left px-3 py-2 border border-gray-300 font-semibold">Test Case</th>
                      <th className="text-left px-3 py-2 border border-gray-300 font-semibold">Input</th>
                      <th className="text-left px-3 py-2 border border-gray-300 font-semibold">Expected</th>
                      <th className="text-left px-3 py-2 border border-gray-300 font-semibold">Your output</th>
                      <th className="text-center px-3 py-2 border border-gray-300 font-semibold">Kết quả</th>
                    </tr>
                  </thead>
                  <tbody>
                    {testResults.testResults.map((test: any, index: number) => (
                      <tr key={index} className={test.passed ? "bg-green-50" : "bg-red-50"}>
                        <td className="px-3 py-2 border border-gray-300">#{index + 1}</td>
                        <td className="px-3 py-2 border border-gray-300 font-mono text-xs">
                          {test.input || '(empty)'}
                        </td>
                        <td className="px-3 py-2 border border-gray-300 font-mono text-xs">
                          {test.expectedOutput}
                        </td>
                        <td className="px-3 py-2 border border-gray-300 font-mono text-xs">
                          {test.actualOutput || '(no output)'}
                        </td>
                        <td className="px-3 py-2 border border-gray-300 text-center">
                          <span className={`inline-flex items-center justify-center w-6 h-6 rounded-full ${
                            test.passed ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
                          }`}>
                            {test.passed ? '✓' : '✗'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Score summary */}
            <div className="flex items-center justify-between p-4 bg-gray-50 border border-gray-300 rounded">
              <div>
                <span className={`inline-block px-4 py-2 rounded font-semibold text-white ${
                  testResults.passedTests === testResults.totalTests && testResults.totalTests > 0 ? 'bg-green-600' : 'bg-red-600'
                }`}>
                  {testResults.passedTests === testResults.totalTests && testResults.totalTests > 0 ? 'CORRECT' : 'INCORRECT'}
                </span>
              </div>
              <div className="text-right">
                <div className="text-sm text-gray-600">Điểm</div>
                <div className="text-2xl font-bold text-gray-900">
                  {testResults.passedTests || 0} / {testResults.totalTests || 0}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
