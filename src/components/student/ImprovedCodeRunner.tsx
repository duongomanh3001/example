"use client";

import { useState, useCallback, useRef } from 'react';
import { AssignmentService } from '@/services/assignment.service';
import { StudentQuestionResponse } from '@/types/api';

interface ImprovedCodeRunnerProps {
  question: StudentQuestionResponse;
  answer: string;
  onAnswerChange: (answer: string) => void;
  onTestResults: (results: any) => void;
  isChecking: boolean;
  setIsChecking: (checking: boolean) => void;
  testResults: any;
}

// Enhanced timeout controller with more robust error handling
class EnhancedTimeoutController {
  private controller: AbortController;
  private timeoutId: NodeJS.Timeout;
  private startTime: number;
  
  constructor(timeoutMs: number = 30000) {
    this.controller = new AbortController();
    this.startTime = Date.now();
    
    this.timeoutId = setTimeout(() => {
      console.warn(`Request timeout after ${timeoutMs}ms`);
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

export default function ImprovedCodeRunner({
  question,
  answer,
  onAnswerChange,
  onTestResults,
  isChecking,
  setIsChecking,
  testResults
}: ImprovedCodeRunnerProps) {
  const [showExampleTests, setShowExampleTests] = useState(true);
  const [codeOutput, setCodeOutput] = useState<string>('');
  const [executionLogs, setExecutionLogs] = useState<string[]>([]);
  const [currentTimeout, setCurrentTimeout] = useState<EnhancedTimeoutController | null>(null);
  const [executionStartTime, setExecutionStartTime] = useState<number | null>(null);
  
  const logRef = useRef<HTMLDivElement>(null);

  // Helper function to add execution logs
  const addLog = useCallback((message: string) => {
    const timestamp = new Date().toLocaleTimeString();
    const logMessage = `[${timestamp}] ${message}`;
    
    setExecutionLogs(prev => [...prev.slice(-9), logMessage]); // Keep last 10 logs
    console.log(logMessage);
    
    // Auto scroll to bottom
    setTimeout(() => {
      if (logRef.current) {
        logRef.current.scrollTop = logRef.current.scrollHeight;
      }
    }, 100);
  }, []);

  // Clear logs
  const clearLogs = useCallback(() => {
    setExecutionLogs([]);
  }, []);

  // Enhanced code validation
  const validateCode = useCallback((code: string, language: string): { valid: boolean; issues: string[] } => {
    const issues: string[] = [];
    
    if (!code.trim()) {
      issues.push('Code is empty');
      return { valid: false, issues };
    }

    // Basic syntax validation based on language
    switch (language.toLowerCase()) {
      case 'python':
        if (!code.includes('def ') && !code.includes('=') && !code.includes('print')) {
          issues.push('Python code might be incomplete - consider adding functions or statements');
        }
        break;
      case 'c':
      case 'cpp':
        if (!code.includes('{') || !code.includes('}')) {
          issues.push('C/C++ code might be missing function structure');
        }
        break;
      case 'java':
        if (!code.includes('{') || !code.includes('}')) {
          issues.push('Java code might be missing class/method structure');
        }
        break;
    }

    return { valid: issues.length === 0, issues };
  }, []);

  const handleRunCode = useCallback(async () => {
    if (!answer.trim()) {
      alert('Vui lòng nhập code trước khi chạy thử!');
      return;
    }

    // Validate code first
    const validation = validateCode(answer, question.language || 'c');
    if (!validation.valid) {
      const shouldContinue = confirm(
        `Phát hiện một số vấn đề với code:\n\n${validation.issues.join('\n')}\n\nBạn có muốn tiếp tục chạy thử không?`
      );
      if (!shouldContinue) return;
    }

    const timeoutController = new EnhancedTimeoutController(20000); // 20 second timeout for run
    setCurrentTimeout(timeoutController);
    setExecutionStartTime(Date.now());

    try {
      setIsChecking(true);
      setCodeOutput('');
      clearLogs();
      
      addLog(`Starting code execution...`);
      addLog(`Code length: ${answer.length} characters`);
      addLog(`Language: ${question.language || 'c'}`);

      setCodeOutput('Đang chạy code...');

      // Enhanced API call with better error context
      const result = await Promise.race([
        (async () => {
          try {
            addLog('Sending code to server...');
            const response = await AssignmentService.runQuestionCode(
              question.id, 
              question.id, 
              answer, 
              question.language || 'c'
            );
            addLog(`Server response received (${timeoutController.elapsedTime}ms)`);
            return response;
          } catch (error) {
            addLog(`Server request failed: ${error}`);
            throw error;
          }
        })(),
        new Promise<never>((_, reject) => {
          timeoutController.signal.addEventListener('abort', () => {
            reject(new Error(`Timeout: Code execution exceeded 20 seconds. This might indicate:\n• Infinite loop in your code\n• Server overload\n• Network connectivity issues\n\n💡 Try optimizing your code or checking your internet connection.`));
          });
        })
      ]);

      addLog(`Execution completed successfully`);

      if (result.success) {
        const output = result.output || 'Compilation successful - no output';
        setCodeOutput(`${output}`);
        addLog(`OUTPUT: ${output.substring(0, 50)}${output.length > 50 ? '...' : ''}`);
      } else {
        const errorMsg = result.error || result.compilationError || 'Unknown error occurred';
        setCodeOutput(`Error: ${errorMsg}`);
        addLog(`Error: ${errorMsg}`);
      }

    } catch (error) {
      const elapsedTime = timeoutController.elapsedTime;
      addLog(`Execution failed after ${elapsedTime}ms`);
      
      console.error('Code execution error:', error);
      
      if (error instanceof Error && error.message.includes('Timeout')) {
        setCodeOutput(`${error.message}`);
        addLog(`Timeout detected`);
      } else if (error instanceof Error && error.message.includes('fetch')) {
        setCodeOutput(`Network Error: Unable to connect to server. Please check your internet connection and try again.`);
        addLog(`Network connectivity issue`);
      } else {
        setCodeOutput(`Runtime Error: ${error}`);
        addLog(`Unexpected error: ${error}`);
      }
    } finally {
      timeoutController.clear();
      setCurrentTimeout(null);
      setExecutionStartTime(null);
      setIsChecking(false);
      addLog(`Execution finished`);
    }
  }, [answer, question.id, question.language, addLog, clearLogs, validateCode]);

  const handleTestCode = useCallback(async () => {
    if (!answer.trim()) {
      alert('Vui lòng nhập code trước khi test!');
      return;
    }

    // Enhanced validation for test cases
    const validation = validateCode(answer, question.language || 'c');
    if (!validation.valid) {
      const shouldContinue = confirm(
        `Code validation warnings:\n\n${validation.issues.join('\n')}\n\nContinue with testing?`
      );
      if (!shouldContinue) return;
    }

    // Validate question has test cases
    if (!question.exampleTestCases || question.exampleTestCases.length === 0) {
      addLog('No test cases available');
      onTestResults({
        success: false,
        error: 'This question has no test cases available. Please contact your instructor.',
        testCases: [],
        message: 'No test cases configured',
        passedTests: 0,
        totalTests: 0
      });
      return;
    }

    const timeoutController = new EnhancedTimeoutController(60000); // 60 second timeout for testing
    setCurrentTimeout(timeoutController);
    setExecutionStartTime(Date.now());

    try {
      setIsChecking(true);
      clearLogs();
      
      addLog(`Starting comprehensive testing...`);
      addLog(`Question ID: ${question.id}`);
      addLog(`Code length: ${answer.length} characters`);
      addLog(`Language: ${question.language || 'c'}`);
      addLog(`Test cases: ${question.exampleTestCases.length} available`);
      
      // Show test case details in logs
      question.exampleTestCases.forEach((testCase, index) => {
        addLog(`Test ${index + 1}: Input="${testCase.input || 'empty'}" | Expected="${testCase.expectedOutput}"`);
      });

      addLog('Submitting to auto-grading system...');

      // Enhanced API call with comprehensive error handling
      const result = await Promise.race([
        (async () => {
          try {
            const response = await AssignmentService.testQuestionCode(
              question.id, 
              question.id, 
              answer, 
              question.language || 'c'
            );
            addLog(`Auto-grading completed (${timeoutController.elapsedTime}ms)`);
            
            // Log detailed results
            if (response.testResults && response.testResults.length > 0) {
              addLog(`Results: ${response.passedTests || 0}/${response.totalTests || 0} tests passed`);
              response.testResults.forEach((test: any, index: number) => {
                const status = test.passed ? '✅' : '❌';
                addLog(`${status} Test ${index + 1}: ${test.passed ? 'PASS' : 'FAIL'} | Got: "${test.actualOutput || 'no output'}"`);
              });
            }
            
            return response;
          } catch (error) {
            addLog(`Auto-grading system error: ${error}`);
            throw error;
          }
        })(),
        new Promise<never>((_, reject) => {
          timeoutController.signal.addEventListener('abort', () => {
            reject(new Error(`Testing Timeout: The auto-grading process took longer than 60 seconds.\n\nPossible causes:\n• Complex code requiring extensive computation\n• Server under heavy load\n• Network connectivity issues\n• Infinite loops or performance problems\n\n💡 Suggestions:\n• Optimize your algorithm for better performance\n• Check for infinite loops or recursive calls\n• Try testing during off-peak hours\n• Contact technical support if the issue persists`));
          });
        })
      ]);

      addLog(`Testing process completed successfully`);
      console.log('Complete test results:', result);

      // Enhanced result processing
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
      const elapsedTime = timeoutController.elapsedTime;
      addLog(`Testing failed after ${elapsedTime}ms`);
      
      console.error('Code testing error:', error);
      
      let errorMessage = 'An error occurred during testing';
      let suggestions = '';
      
      if (error instanceof Error) {
        if (error.message.includes('Timeout') || error.message.includes('timeout')) {
          errorMessage = error.message;
          addLog(`Testing timeout detected`);
        } else if (error.message.includes('fetch') || error.message.includes('network') || error.message.includes('Network')) {
          errorMessage = 'Network connectivity issue prevented testing. Please check your internet connection and try again.';
          suggestions = '\n\nTroubleshooting:\n• Check internet connection\n• Refresh the page\n• Try again in a few minutes\n• Contact support if issue persists';
          addLog(`Network error during testing`);
        } else if (error.message.includes('401') || error.message.includes('Unauthorized')) {
          errorMessage = 'Authentication expired. Please log in again.';
          addLog(`Authentication issue`);
        } else if (error.message.includes('500') || error.message.includes('Internal Server Error')) {
          errorMessage = 'Server error occurred. The development team has been notified. Please try again later.';
          addLog(`Server-side error`);
        } else {
          errorMessage = error.message;
          addLog(`Unexpected error: ${error.message}`);
        }
      }
      
      onTestResults({
        success: false,
        error: errorMessage + suggestions,
        testCases: [],
        message: 'Testing failed',
        passedTests: 0,
        totalTests: question.exampleTestCases?.length || 0,
        executionTime: elapsedTime,
        timestamp: new Date().toISOString()
      });
      
    } finally {
      timeoutController.clear();
      setCurrentTimeout(null);
      setExecutionStartTime(null);
      setIsChecking(false);
      addLog(`Testing process finished`);
    }
  }, [answer, question.id, question.language, question.exampleTestCases, onTestResults, addLog, clearLogs, validateCode]);

  const handleStopExecution = useCallback(() => {
    if (currentTimeout) {
      addLog(`User stopped execution`);
      currentTimeout.abort();
      setCurrentTimeout(null);
      setExecutionStartTime(null);
      setIsChecking(false);
      setCodeOutput('Execution stopped by user');
      
      onTestResults({
        success: false,
        error: 'Execution was stopped by user',
        testCases: [],
        message: 'Stopped by user',
        passedTests: 0,
        totalTests: 0
      });
    }
  }, [currentTimeout, onTestResults, addLog]);

  // Calculate elapsed time for display
  const getElapsedTime = useCallback(() => {
    if (!executionStartTime) return 0;
    return Math.floor((Date.now() - executionStartTime) / 1000);
  }, [executionStartTime]);

  return (
    <div className="space-y-6">
      {/* Question Description */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="font-semibold text-blue-900 mb-2">Đề bài:</h3>
        <p className="text-blue-800 whitespace-pre-wrap">{question.description}</p>
        
        <div className="mt-3 flex gap-2 flex-wrap">
          {question.language && (
            <span className="inline-block bg-blue-600 text-white text-xs px-2 py-1 rounded">
              Language: {question.language.toUpperCase()}
            </span>
          )}
          <span className="inline-block bg-purple-600 text-white text-xs px-2 py-1 rounded">
            Points: {question.points}
          </span>
          {question.exampleTestCases && (
            <span className="inline-block bg-green-600 text-white text-xs px-2 py-1 rounded">
              Test Cases: {question.exampleTestCases.length}
            </span>
          )}
        </div>
      </div>

      {/* Example Test Cases */}
      {question.exampleTestCases && question.exampleTestCases.length > 0 && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-green-900">Test Cases ({question.exampleTestCases.length}):</h3>
            <button
              onClick={() => setShowExampleTests(!showExampleTests)}
              className="text-sm text-green-700 hover:text-green-900 px-2 py-1 rounded border border-green-300 hover:bg-green-100"
            >
              {showExampleTests ? 'Hide' : 'Show'}
            </button>
          </div>
          
          {showExampleTests && (
            <div className="space-y-3">
              {question.exampleTestCases.map((testCase, index) => (
                <div key={index} className="bg-white border border-green-200 rounded p-3 shadow-sm">
                  <div className="flex items-center mb-2">
                    <span className="font-medium text-green-800 text-sm">Test Case {index + 1}</span>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                    <div>
                      <span className="font-medium text-green-800">INPUT:</span>
                      <pre className="mt-1 bg-gray-100 p-2 rounded text-xs font-mono border">
                        {testCase.input || '(empty)'}
                      </pre>
                    </div>
                    <div>
                      <span className="font-medium text-green-800">EXPECTED OUTPUT:</span>
                      <pre className="mt-1 bg-gray-100 p-2 rounded text-xs font-mono border">
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
      <div className="bg-white border border-slate-300 rounded-lg shadow-sm">
        <div className="flex items-center justify-between p-3 border-b border-slate-200 bg-slate-50">
          <div className="flex gap-2 items-center">
            <button
              onClick={handleRunCode}
              disabled={isChecking}
              className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transition-colors"
              title="Execute code with custom input (no grading)"
            >

              {isChecking ? 'Running...' : 'RUN'}
            </button>
            
            <button
              onClick={handleTestCode}
              disabled={isChecking}
              className="px-4 py-2 text-sm bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transition-colors"
              title="Run with teacher's test cases for grading"
            >
         
              {isChecking ? 'Testing...' : 'TEST'}
            </button>
            
            {isChecking && (
              <button
                onClick={handleStopExecution}
                className="px-4 py-2 text-sm bg-red-600 text-white rounded hover:bg-red-700 flex items-center gap-2 transition-colors"
                title="Stop execution"
              >
                Stop
              </button>
            )}

            {executionLogs.length > 0 && (
              <button
                onClick={clearLogs}
                disabled={isChecking}
                className="px-3 py-2 text-sm bg-gray-500 text-white rounded hover:bg-gray-600 disabled:opacity-50"
                title="Clear execution logs"
              >
                Clear Logs
              </button>
            )}
          </div>
          
          {isChecking && (
            <div className="flex items-center gap-3 text-sm">
              <div className="flex items-center gap-2 text-amber-600">
                <div className="animate-spin h-4 w-4 border-2 border-amber-600 border-t-transparent rounded-full"></div>
                <span>Processing... ({getElapsedTime()}s)</span>
              </div>
            </div>
          )}
        </div>
        
        <div className="p-3">
          <textarea
            value={answer}
            onChange={(e) => onAnswerChange(e.target.value)}
            placeholder="Enter your code here..."
            className="w-full h-96 font-mono text-sm border border-slate-300 rounded p-3 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            style={{ 
              fontFamily: 'Monaco, Menlo, "Ubuntu Mono", Consolas, "Courier New", monospace',
              lineHeight: '1.6'
            }}
            disabled={isChecking}
          />
        </div>
      </div>

      {/* Execution Logs */}
      {executionLogs.length > 0 && (
        <div className="bg-slate-900 text-slate-100 border border-slate-700 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <h4 className="font-semibold text-slate-100">Execution Logs:</h4>
            <span className="text-xs text-slate-400">{executionLogs.length} entries</span>
          </div>
          <div 
            ref={logRef}
            className="text-xs font-mono bg-slate-800 border border-slate-600 rounded p-3 overflow-auto max-h-32"
          >
            {executionLogs.map((log, index) => (
              <div key={index} className="text-slate-300 leading-relaxed">
                {log}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Code Output */}
      {codeOutput && (
        <div className="bg-slate-50 border border-slate-300 rounded-lg p-4">
          <h4 className="font-semibold text-slate-900 mb-2">OUTPUT</h4>
          <pre className="text-sm font-mono bg-white border border-slate-200 rounded p-3 overflow-auto max-h-48 whitespace-pre-wrap">
            {codeOutput}
          </pre>
        </div>
      )}

      {/* Enhanced Test Results */}
      {testResults && (
        <div className={`border rounded-lg p-4 shadow-sm ${
          testResults.success ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
        }`}>
          <div className="flex items-center gap-3 mb-4">
            <div className={`w-6 h-6 rounded-full flex items-center justify-center ${
              testResults.success ? 'bg-green-500' : 'bg-red-500'
            }`}>
              <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {testResults.success ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                )}
              </svg>
            </div>
            <div>
              <h4 className={`font-bold text-lg ${
                testResults.success ? 'text-green-800' : 'text-red-800'
              }`}>
                Auto-Grading Results
              </h4>
              <p className={`text-sm ${
                testResults.success ? 'text-green-700' : 'text-red-700'
              }`}>
                {testResults.success 
                  ? `Perfect! ${testResults.passedTests || 0}/${testResults.totalTests || 0} test cases passed` 
                  : `Needs improvement: ${testResults.passedTests || 0}/${testResults.totalTests || 0} test cases passed`}
              </p>
            </div>
          </div>

          {/* Execution metadata */}
          {(testResults.executionTime || testResults.timestamp) && (
            <div className="mb-4 p-3 bg-slate-50 border border-slate-200 rounded text-sm">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-slate-600">
                {testResults.executionTime && (
                  <div>Time: {testResults.executionTime}ms</div>
                )}
                {testResults.timestamp && (
                  <div>{new Date(testResults.timestamp).toLocaleTimeString()}</div>
                )}
                {testResults.language && (
                  <div>{testResults.language.toUpperCase()}</div>
                )}
                {testResults.codeLength && (
                  <div>{testResults.codeLength} chars</div>
                )}
              </div>
            </div>
          )}
          
          {/* System message if available */}
          {testResults.message && (
            <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded">
              <p className="text-sm text-blue-700">ℹ️ {testResults.message}</p>
            </div>
          )}

          {/* Detailed test results */}
          {testResults.testResults && testResults.testResults.length > 0 && (
            <div className="space-y-3 mb-4">
              <h5 className="font-medium text-slate-800">Detailed Test Results:</h5>
              <div className="overflow-x-auto">
                <table className="w-full text-sm border-collapse bg-white rounded border">
                  <thead>
                    <tr className={`${testResults.success ? 'bg-green-100' : 'bg-red-100'}`}>
                      <th className="text-left px-4 py-3 border font-semibold">Test #</th>
                      <th className="text-left px-4 py-3 border font-semibold">Input</th>
                      <th className="text-left px-4 py-3 border font-semibold">Expected</th>
                      <th className="text-left px-4 py-3 border font-semibold">Your Output</th>
                      <th className="text-center px-4 py-3 border font-semibold">Result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {testResults.testResults.map((test: any, index: number) => (
                      <tr key={index} className={test.passed ? "bg-green-50" : "bg-red-50"}>
                        <td className="px-4 py-3 border font-medium">
                          #{index + 1}
                        </td>
                        <td className="px-4 py-3 border font-mono text-xs max-w-32 overflow-hidden">
                          <div className="truncate" title={test.input || '(empty)'}>
                            {test.input || '(empty)'}
                          </div>
                        </td>
                        <td className="px-4 py-3 border font-mono text-xs max-w-32 overflow-hidden">
                          <div className="truncate" title={test.expectedOutput}>
                            {test.expectedOutput}
                          </div>
                        </td>
                        <td className="px-4 py-3 border font-mono text-xs max-w-32 overflow-hidden">
                          <div className="truncate" title={test.actualOutput || '(no output)'}>
                            {test.actualOutput || '(no output)'}
                          </div>
                        </td>
                        <td className="px-4 py-3 border text-center">
                          <div className={`w-6 h-6 rounded-full flex items-center justify-center mx-auto ${
                            test.passed ? 'bg-green-500' : 'bg-red-500'
                          }`}>
                            <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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

          {/* Error details */}
          {testResults.error && (
            <div className="mt-4 p-4 bg-red-100 rounded border border-red-200">
              <h5 className="font-medium text-red-800 mb-2">Error Details:</h5>
              <pre className="text-sm text-red-700 whitespace-pre-wrap overflow-auto max-h-40 bg-white p-2 rounded border">
                {testResults.error}
              </pre>
            </div>
          )}

          {/* Score summary */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm bg-white p-4 rounded border">
            <div className="text-center">
              <span className={`inline-block px-4 py-2 rounded-lg font-bold text-white text-base ${
                testResults.success ? 'bg-green-600' : 'bg-red-600'
              }`}>
                {testResults.success ? 'PASSED' : 'TRY AGAIN'}
              </span>
            </div>
            <div className="text-center text-slate-700">
              <div className="font-semibold">Score</div>
              <div className="text-2xl font-bold text-blue-600">
                {testResults.score || 0} / {question.points}
              </div>
            </div>
            <div className="text-center text-slate-600">
              <div className="font-semibold">Test Cases</div>
              <div className="text-lg">
                {testResults.passedTests || 0} / {testResults.totalTests || 0}
                {question.exampleTestCases?.length && (
                  <div className="text-xs text-slate-500 mt-1">
                    ({question.exampleTestCases.length} public)
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}