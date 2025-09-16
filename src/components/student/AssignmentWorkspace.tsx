import React, { useState, useEffect } from 'react';
import { CodeRunner } from './CodeRunner';

interface Question {
  id: number;
  title: string;
  content: string;
  type: string;
  testCases: TestCase[];
  maxScore: number;
}

interface TestCase {
  id: number;
  input: string;
  expectedOutput: string;
  isHidden: boolean;
  weight: number;
}

interface Assignment {
  id: number;
  title: string;
  description: string;
  questions: Question[];
  maxScore: number;
  timeLimit: number;
  endTime: string;
}

interface QuestionScore {
  questionId: number;
  score: number;
  isPassed: boolean;
  isSubmitted: boolean;
}

interface AssignmentWorkspaceProps {
  assignmentId: number;
  courseId: number;
}

export const AssignmentWorkspace: React.FC<AssignmentWorkspaceProps> = ({
  assignmentId,
  courseId
}) => {
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [questionScores, setQuestionScores] = useState<QuestionScore[]>([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmittingAll, setIsSubmittingAll] = useState(false);
  const [timeRemaining, setTimeRemaining] = useState<number>(0);
  const [currentCodes, setCurrentCodes] = useState<{ [key: number]: string }>({});

  useEffect(() => {
    if (assignmentId) {
      fetchAssignment();
    }
  }, [assignmentId]);

  // Timer effect
  useEffect(() => {
    if (timeRemaining > 0) {
      const timer = setInterval(() => {
        setTimeRemaining(prev => {
          if (prev <= 1) {
            handleAutoSubmit();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [timeRemaining]);

  const fetchAssignment = async () => {
    try {
      const response = await fetch(`/api/student/course/${courseId}/assignment/${assignmentId}`);
      const data = await response.json();
      
      if (data.success) {
        setAssignment(data.assignment);
        
        // Initialize question scores
        const initialScores = data.assignment.questions.map((q: Question) => ({
          questionId: q.id,
          score: 0,
          isPassed: false,
          isSubmitted: false
        }));
        setQuestionScores(initialScores);
        
        // Initialize code storage
        const initialCodes: { [key: number]: string } = {};
        data.assignment.questions.forEach((q: Question) => {
          initialCodes[q.id] = '';
        });
        setCurrentCodes(initialCodes);
        
        // Calculate time remaining
        if (data.assignment.endTime) {
          const endTime = new Date(data.assignment.endTime).getTime();
          const now = new Date().getTime();
          const remaining = Math.max(0, Math.floor((endTime - now) / 1000));
          setTimeRemaining(remaining);
        }
      }
    } catch (error) {
      console.error('Error fetching assignment:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleScoreUpdate = (questionId: number, score: number, passed: boolean) => {
    setQuestionScores(prev => 
      prev.map(qs => 
        qs.questionId === questionId 
          ? { ...qs, score, isPassed: passed }
          : qs
      )
    );
  };

  const handleCodeChange = (questionId: number, code: string) => {
    setCurrentCodes(prev => ({
      ...prev,
      [questionId]: code
    }));
  };

  const handleSubmitQuestion = async (questionId: number) => {
    try {
      const question = assignment?.questions.find(q => q.id === questionId);
      if (!question) return;

      const code = currentCodes[questionId] || '';
      
      const response = await fetch('/api/student/submit-question-answer', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          questionId: questionId,
          code: code,
          language: 'python' // This should be dynamic based on user selection
        })
      });

      const result = await response.json();
      
      if (result.success) {
        // Mark question as submitted
        setQuestionScores(prev => 
          prev.map(qs => 
            qs.questionId === questionId 
              ? { ...qs, isSubmitted: true }
              : qs
          )
        );
        
        alert('Question submitted successfully!');
      } else {
        alert('Error submitting question: ' + result.error);
      }
    } catch (error) {
      console.error('Error submitting question:', error);
      alert('Error submitting question');
    }
  };

  const handleSubmitAll = async () => {
    setIsSubmittingAll(true);
    
    try {
      // First submit all individual questions
      for (const question of assignment?.questions || []) {
        await handleSubmitQuestion(question.id);
      }
      
      // Then submit the assignment
      const response = await fetch('/api/student/submit-assignment', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          assignmentId: assignmentId
        })
      });

      const result = await response.json();
      
      if (result.success) {
        alert('Assignment submitted successfully!');
        // Could redirect or show success state
      } else {
        alert('Error submitting assignment: ' + result.error);
      }
    } catch (error) {
      console.error('Error submitting assignment:', error);
      alert('Error submitting assignment');
    } finally {
      setIsSubmittingAll(false);
    }
  };

  const handleAutoSubmit = async () => {
    alert('Time is up! Auto-submitting assignment...');
    await handleSubmitAll();
  };

  const formatTime = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const getTotalScore = (): number => {
    return questionScores.reduce((sum, qs) => sum + qs.score, 0);
  };

  const getMaxPossibleScore = (): number => {
    return assignment?.questions.reduce((sum, q) => sum + q.maxScore, 0) || 0;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (!assignment) {
    return (
      <div className="text-center py-8">
        <p className="text-red-600">Assignment not found</p>
      </div>
    );
  }

  const currentQuestion = assignment.questions[currentQuestionIndex];

  return (
    <div className="max-w-7xl mx-auto p-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{assignment.title}</h1>
            <p className="text-gray-600 mt-2">{assignment.description}</p>
          </div>
          
          <div className="text-right">
            {/* Timer */}
            {timeRemaining > 0 && (
              <div className={`text-lg font-mono ${timeRemaining < 300 ? 'text-red-600' : 'text-gray-700'}`}>
                Time: {formatTime(timeRemaining)}
              </div>
            )}
            
            {/* Score */}
            <div className="mt-2">
              <span className="text-sm text-gray-500">Score: </span>
              <span className="font-semibold">
                {getTotalScore()} / {getMaxPossibleScore()}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Questions Navigation */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-4 sticky top-6">
            <h3 className="font-semibold mb-4">Questions ({assignment.questions.length})</h3>
            <div className="space-y-2 max-h-96 overflow-y-auto">
              {assignment.questions.map((question, index) => {
                const questionScore = questionScores.find(qs => qs.questionId === question.id);
                return (
                  <button
                    key={question.id}
                    onClick={() => setCurrentQuestionIndex(index)}
                    className={`w-full text-left p-3 rounded border transition-colors ${
                      currentQuestionIndex === index
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium">Q{index + 1}</span>
                      <div className="flex items-center space-x-1">
                        {questionScore?.isPassed && (
                          <span className="text-green-600 text-sm">✓</span>
                        )}
                        {questionScore?.isSubmitted && (
                          <span className="text-blue-600 text-sm">📝</span>
                        )}
                      </div>
                    </div>
                    <div className="text-sm text-gray-500 mt-1 truncate">
                      {question.title}
                    </div>
                    <div className="text-xs text-gray-400 mt-1">
                      Score: {questionScore?.score || 0} / {question.maxScore}
                    </div>
                  </button>
                );
              })}
            </div>
            
            {/* Progress Bar */}
            <div className="mt-4 mb-4">
              <div className="text-xs text-gray-500 mb-1">
                Progress: {questionScores.filter(qs => qs.isSubmitted).length} / {assignment.questions.length}
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div 
                  className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                  style={{ 
                    width: `${(questionScores.filter(qs => qs.isSubmitted).length / assignment.questions.length) * 100}%` 
                  }}
                ></div>
              </div>
            </div>
            
            {/* Submit All Button */}
            <button
              onClick={handleSubmitAll}
              disabled={isSubmittingAll || questionScores.filter(qs => qs.isSubmitted).length === 0}
              className="w-full bg-green-600 text-white py-3 rounded font-semibold hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmittingAll ? 'Submitting...' : 'Submit Assignment'}
            </button>
          </div>
        </div>

        {/* Current Question */}
        <div className="lg:col-span-3">
          {currentQuestion && (
            <div className="space-y-6">
              {/* Question Content */}
              <div className="bg-white rounded-lg shadow-md p-6">
                <div className="flex justify-between items-start mb-4">
                  <h2 className="text-xl font-semibold">
                    Question {currentQuestionIndex + 1}: {currentQuestion.title}
                  </h2>
                  <div className="flex space-x-2">
                    <button
                      onClick={() => handleSubmitQuestion(currentQuestion.id)}
                      disabled={!currentCodes[currentQuestion.id]?.trim()}
                      className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Submit Question
                    </button>
                  </div>
                </div>
                
                <div className="prose max-w-none">
                  <div dangerouslySetInnerHTML={{ __html: currentQuestion.content }} />
                </div>

                {/* Show example test cases */}
                {currentQuestion.testCases.filter(tc => !tc.isHidden).length > 0 && (
                  <div className="mt-4 p-4 bg-gray-50 rounded">
                    <h4 className="font-medium mb-2">Example Test Cases:</h4>
                    <div className="space-y-2">
                      {currentQuestion.testCases
                        .filter(tc => !tc.isHidden)
                        .slice(0, 2) // Show only first 2 examples
                        .map((testCase, index) => (
                          <div key={testCase.id} className="text-sm">
                            <div><strong>Input:</strong> <code className="bg-white px-1">{testCase.input || '(no input)'}</code></div>
                            <div><strong>Expected Output:</strong> <code className="bg-white px-1">{testCase.expectedOutput}</code></div>
                          </div>
                        ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Code Runner */}
              {currentQuestion.type === 'PROGRAMMING' && (
                <CodeRunner
                  questionId={currentQuestion.id}
                  testCases={currentQuestion.testCases}
                  onScoreUpdate={handleScoreUpdate}
                  initialCode={currentCodes[currentQuestion.id] || ''}
                  language="python"
                  onCodeChange={(code) => handleCodeChange(currentQuestion.id, code)}
                />
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};