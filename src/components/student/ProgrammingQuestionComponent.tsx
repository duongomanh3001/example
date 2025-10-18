"use client";

import { StudentQuestionResponse } from '@/types/api';
import CodeRunner from './CodeRunner';

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
  // Use the code runner component
  return (
    <CodeRunner
      question={question}
      answer={answer}
      onAnswerChange={onAnswerChange}
      onTestResults={onTestResults}
      isChecking={isChecking}
      setIsChecking={setIsChecking}
      testResults={testResults}
    />
  );
}