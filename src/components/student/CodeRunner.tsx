import React, { useState, useEffect } from 'react';

interface TestCase {
  id: number;
  input: string;
  expectedOutput: string;
  isHidden: boolean;
  weight: number;
}

interface TestResult {
  testCaseId: number;
  input: string;
  expectedOutput: string;
  actualOutput: string;
  passed: boolean;
  executionTime?: number;
  errorMessage?: string;
}

interface CodeRunnerProps {
  questionId: number;
  testCases: TestCase[];
  onScoreUpdate: (questionId: number, score: number, passed: boolean) => void;
  initialCode?: string;
  language?: string;
  onCodeChange?: (code: string) => void;
}

export const CodeRunner: React.FC<CodeRunnerProps> = ({
  questionId,
  testCases,
  onScoreUpdate,
  initialCode = '',
  language = 'python',
  onCodeChange
}) => {
  const [code, setCode] = useState(initialCode);
  const [selectedLanguage, setSelectedLanguage] = useState(language);
  const [testResults, setTestResults] = useState<TestResult[]>([]);
  const [isRunning, setIsRunning] = useState(false);
  const [isChecking, setIsChecking] = useState(false);
  const [currentScore, setCurrentScore] = useState(0);
  const [maxScore, setMaxScore] = useState(0);
  const [lastSubmissionPassed, setLastSubmissionPassed] = useState(false);

  useEffect(() => {
    const totalWeight = testCases.reduce((sum, tc) => sum + tc.weight, 0);
    setMaxScore(totalWeight);
  }, [testCases]);

  const handleRunCode = async () => {
    if (!code.trim()) {
      return;
    }

    setIsRunning(true);
    
    try {
      const response = await fetch('/api/execution/test-with-input', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          code: code,
          language: selectedLanguage,
          input: ''
        })
      });

      const result = await response.json();
      console.log('Run result:', result);
      
    } catch (error) {
      console.error('Error running code:', error);
    } finally {
      setIsRunning(false);
    }
  };

  const handleCheckTestCases = async () => {
    if (!code.trim()) {
      return;
    }

    setIsChecking(true);
    setTestResults([]);
    
    try {
      const response = await fetch('/api/student/check-question-code', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          questionId: questionId,
          code: code,
          language: selectedLanguage
        })
      });

      const result = await response.json();
      
      if (result.success) {
        setTestResults(result.testResults || []);
        
        const passedWeight = result.testResults
          .filter((tr: TestResult) => tr.passed)
          .reduce((sum: number, tr: TestResult) => {
            const testCase = testCases.find(tc => tc.id === tr.testCaseId);
            return sum + (testCase?.weight || 0);
          }, 0);
        
        setCurrentScore(passedWeight);
        const allPassed = result.testResults.every((tr: TestResult) => tr.passed);
        setLastSubmissionPassed(allPassed);
        
        onScoreUpdate(questionId, passedWeight, allPassed);
      }
      
    } catch (error) {
      console.error('Error checking test cases:', error);
    } finally {
      setIsChecking(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-semibold mb-4">Question {questionId} - Code Runner</h3>
      
      {/* Language Selection */}
      <div className="mb-4">
        <label className="block text-sm font-medium mb-2">Programming Language:</label>
        <select
          value={selectedLanguage}
          onChange={(e) => setSelectedLanguage(e.target.value)}
          className="border border-gray-300 rounded px-3 py-2 w-48"
        >
          <option value="python">Python</option>
          <option value="java">Java</option>
          <option value="cpp">C++</option>
          <option value="c">C</option>
        </select>
      </div>

      {/* Code Editor */}
      <div className="mb-4">
        <label className="block text-sm font-medium mb-2">Your Code:</label>
        <textarea
          value={code}
          onChange={(e) => {
            setCode(e.target.value);
            onCodeChange?.(e.target.value);
          }}
          placeholder="Enter your code here..."
          rows={12}
          className="w-full border border-gray-300 rounded px-3 py-2 font-mono text-sm"
        />
      </div>

      {/* Action Buttons */}
      <div className="mb-4 space-x-2">
        <button
          onClick={handleRunCode}
          disabled={isRunning}
          className="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600 disabled:opacity-50"
        >
          {isRunning ? 'Running...' : '▶ Run Code'}
        </button>
        
        <button
          onClick={handleCheckTestCases}
          disabled={isChecking}
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:opacity-50"
        >
          {isChecking ? 'Checking...' : 'Check Test Cases'}
        </button>
      </div>

      {/* Score Display */}
      {testResults.length > 0 && (
        <div className={`p-3 rounded mb-4 ${lastSubmissionPassed ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}`}>
          <span className="font-medium">
            Score: {currentScore} / {maxScore} 
            {lastSubmissionPassed && <span className="ml-2">✓</span>}
          </span>
        </div>
      )}

      {/* Test Cases Results */}
      {testResults.length > 0 && (
        <div>
          <h4 className="font-medium mb-3">Test Results:</h4>
          <div className="overflow-x-auto">
            <table className="min-w-full border border-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Test</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Expected</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Got</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {testResults.map((result) => (
                  <tr key={result.testCaseId}>
                    <td className="px-3 py-2 text-xs">
                      <code className="bg-gray-100 px-1 py-0.5 rounded">
                        {result.input || '(no input)'}
                      </code>
                    </td>
                    <td className="px-3 py-2 text-xs">
                      <code className="bg-blue-100 text-blue-800 px-1 py-0.5 rounded">
                        {result.expectedOutput}
                      </code>
                    </td>
                    <td className="px-3 py-2 text-xs">
                      <code className={`px-1 py-0.5 rounded ${
                        result.passed 
                          ? 'bg-green-100 text-green-800' 
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {result.actualOutput || result.errorMessage || 'No output'}
                      </code>
                    </td>
                    <td className="px-3 py-2">
                      {result.passed ? (
                        <span className="text-green-600">✓</span>
                      ) : (
                        <span className="text-red-600">✗</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          {testResults.every(tr => tr.passed) && (
            <div className="mt-4 p-3 bg-green-100 text-green-800 rounded">
              <span className="font-medium">Passed all tests! ✓</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CodeRunner;