"use client";

import { useState, useCallback } from 'react';
import { AssignmentService } from '@/services/assignment.service';
import { StudentQuestionResponse, TestCaseResponse } from '@/types/api';
import ImprovedCodeRunner from './ImprovedCodeRunner';

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
  // Use the new improved code runner
  return (
    <ImprovedCodeRunner
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