package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.AutoGradingResponse;
import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.dto.response.TestResultResponse;
import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.SubmissionRepository;
import iuh.fit.cscore_be.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for automatic grading of programming assignments
 * Supports multiple programming languages and provides detailed feedback
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AutoGradingService {
    
    private final HybridCodeExecutionService hybridCodeExecutionService;
    private final SubmissionRepository submissionRepository;
    private final TestResultRepository testResultRepository;
    
    @Value("${grading.time-limit:30}")
    private int defaultTimeLimit;
    
    @Value("${grading.memory-limit:256}")
    private int defaultMemoryLimit;

    /**
     * Automatically grade a submission asynchronously
     */
    @Async
    public CompletableFuture<Double> gradeSubmissionAsync(Long submissionId) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));
            
            Double score = gradeSubmission(submission);
            return CompletableFuture.completedFuture(score);
        } catch (Exception e) {
            log.error("Error in async grading for submission {}", submissionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Grade a submission synchronously
     */
    public Double gradeSubmission(Submission submission) {
        log.info("Starting auto-grading for submission {} (Student: {}, Assignment: {})", 
                submission.getId(), submission.getStudent().getStudentId(), submission.getAssignment().getTitle());
        
        try {
            // Update status to GRADING
            submission.setStatus(SubmissionStatus.GRADING);
            submissionRepository.save(submission);
            
            // Get all test cases for the assignment
            List<TestCase> allTestCases = getAllTestCases(submission.getAssignment());
            
            if (allTestCases.isEmpty()) {
                log.warn("No test cases found for assignment {}", submission.getAssignment().getId());
                submission.setStatus(SubmissionStatus.NO_TESTS);
                submission.setScore(0.0);
                submission.setFeedback("Bài tập này không có test case để chấm điểm");
                submission.setGradedTime(LocalDateTime.now());
                submissionRepository.save(submission);
                return 0.0;
            }
            
            // Execute code with all test cases
            CodeExecutionResponse executionResult = executeSubmission(submission, allTestCases);
            
            // Process results and calculate score
            Double finalScore = processExecutionResults(submission, executionResult, allTestCases);
            
            // Update submission with final results
            updateSubmissionResults(submission, executionResult, finalScore);
            
            log.info("Auto-grading completed for submission {}. Final score: {}", 
                    submission.getId(), finalScore);
            
            return finalScore;
            
        } catch (Exception e) {
            log.error("Error during auto-grading for submission {}", submission.getId(), e);
            
            // Update submission status to error
            submission.setStatus(SubmissionStatus.ERROR);
            submission.setFeedback("Lỗi trong quá trình chấm điểm tự động: " + e.getMessage());
            submission.setGradedTime(LocalDateTime.now());
            submissionRepository.save(submission);
            
            return 0.0;
        }
    }

    /**
     * Get all test cases for an assignment from all questions
     */
    private List<TestCase> getAllTestCases(Assignment assignment) {
        return assignment.getQuestions().stream()
                .flatMap(question -> question.getTestCases().stream())
                .toList();
    }

    /**
     * Execute submission code with test cases using appropriate service
     */
    private CodeExecutionResponse executeSubmission(Submission submission, List<TestCase> testCases) {
        String code = submission.getCode();
        String language = submission.getProgrammingLanguage();
        
        // Check if this is multi-question code
        if (isMultiQuestionCode(code)) {
            return executeMultiQuestionSubmission(submission, testCases);
        }
        
        // Use hybrid execution service (JOBE + Local fallback)
        return hybridCodeExecutionService.executeCodeWithTestCases(code, language, testCases, submission);
    }
    
    /**
     * Check if code contains multiple questions separated by delimiter
     */
    private boolean isMultiQuestionCode(String code) {
        return code != null && (
            code.contains("// --- Next Question ---") ||
            code.contains("/* --- Next Question --- */") ||
            code.contains("# --- Next Question ---") ||
            code.contains("-- Next Question --")
        );
    }
    
    /**
     * Execute multi-question submission by splitting code and running each question
     */
    private CodeExecutionResponse executeMultiQuestionSubmission(Submission submission, List<TestCase> testCases) {
        log.info("Processing multi-question submission for submission {}", submission.getId());
        
        String fullCode = submission.getCode();
        String language = submission.getProgrammingLanguage();
        
        // Split code into individual questions
        List<String> questionCodes = splitMultiQuestionCode(fullCode);
        
        if (questionCodes.isEmpty()) {
            log.error("Failed to split multi-question code for submission {}", submission.getId());
            CodeExecutionResponse errorResponse = new CodeExecutionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Không thể tách các câu hỏi từ code đã submit");
            return errorResponse;
        }
        
        log.info("Found {} questions in submission {}", questionCodes.size(), submission.getId());
        
        // Group test cases by question (assuming test cases are ordered by question)
        List<List<TestCase>> testCasesByQuestion = groupTestCasesByQuestion(testCases, questionCodes.size());
        
        // Execute each question separately
        CodeExecutionResponse combinedResponse = new CodeExecutionResponse();
        combinedResponse.setLanguage(language);
        combinedResponse.setSuccess(true);
        combinedResponse.setTestResults(new ArrayList<>());
        
        long totalExecutionTime = 0L;
        long maxMemoryUsed = 0L;
        int totalPassedTests = 0;
        
        for (int questionIndex = 0; questionIndex < questionCodes.size(); questionIndex++) {
            String questionCode = questionCodes.get(questionIndex);
            List<TestCase> questionTestCases = questionIndex < testCasesByQuestion.size() 
                ? testCasesByQuestion.get(questionIndex) 
                : new ArrayList<>();
            
            log.debug("Executing question {} with {} test cases", questionIndex + 1, questionTestCases.size());
            
            if (questionTestCases.isEmpty()) {
                log.warn("No test cases found for question {} in submission {}", questionIndex + 1, submission.getId());
                continue;
            }
            
            // Execute this question's code with its test cases
            CodeExecutionResponse questionResponse = hybridCodeExecutionService.executeCodeWithTestCases(
                questionCode, language, questionTestCases, null);
            
            if (!questionResponse.isSuccess()) {
                // If any question fails compilation, mark entire submission as failed
                combinedResponse.setSuccess(false);
                combinedResponse.setError("Lỗi trong câu hỏi " + (questionIndex + 1) + ": " + questionResponse.getError());
                combinedResponse.setCompilationError(questionResponse.getError());
                return combinedResponse;
            }
            
            // Merge results from this question
            if (questionResponse.getTestResults() != null) {
                combinedResponse.getTestResults().addAll(questionResponse.getTestResults());
                totalPassedTests += questionResponse.getTestResults().stream()
                    .mapToInt(result -> result.isPassed() ? 1 : 0)
                    .sum();
            }
            
            if (questionResponse.getExecutionTime() != null) {
                totalExecutionTime += questionResponse.getExecutionTime();
            }
            
            if (questionResponse.getMemoryUsed() != null) {
                maxMemoryUsed = Math.max(maxMemoryUsed, questionResponse.getMemoryUsed());
            }
        }
        
        combinedResponse.setExecutionTime(totalExecutionTime);
        combinedResponse.setMemoryUsed(maxMemoryUsed);
        combinedResponse.setPassedTests(totalPassedTests);
        combinedResponse.setTotalTests(testCases.size());
        
        log.info("Multi-question execution completed for submission {}. Passed: {}/{}", 
            submission.getId(), totalPassedTests, testCases.size());
        
        return combinedResponse;
    }
    
    /**
     * Split multi-question code into individual question codes
     */
    private List<String> splitMultiQuestionCode(String fullCode) {
        List<String> questions = new ArrayList<>();
        
        String[] delimiters = {
            "// --- Next Question ---",
            "/* --- Next Question --- */", 
            "# --- Next Question ---",
            "-- Next Question --"
        };
        
        String currentCode = fullCode;
        
        // Find which delimiter is used
        String usedDelimiter = null;
        for (String delimiter : delimiters) {
            if (currentCode.contains(delimiter)) {
                usedDelimiter = delimiter;
                break;
            }
        }
        
        if (usedDelimiter == null) {
            // No delimiter found, treat as single question
            questions.add(fullCode.trim());
            return questions;
        }
        
        // Split by the found delimiter
        String[] parts = currentCode.split(java.util.regex.Pattern.quote(usedDelimiter));
        
        for (String part : parts) {
            String trimmedPart = part.trim();
            if (!trimmedPart.isEmpty()) {
                questions.add(trimmedPart);
            }
        }
        
        return questions;
    }
    
    /**
     * Group test cases by question (assumes test cases are ordered by question)
     */
    private List<List<TestCase>> groupTestCasesByQuestion(List<TestCase> allTestCases, int numQuestions) {
        List<List<TestCase>> grouped = new ArrayList<>();
        
        if (numQuestions <= 0 || allTestCases.isEmpty()) {
            return grouped;
        }
        
        // Simple approach: divide test cases equally among questions
        int testCasesPerQuestion = allTestCases.size() / numQuestions;
        int remainder = allTestCases.size() % numQuestions;
        
        int currentIndex = 0;
        for (int i = 0; i < numQuestions; i++) {
            int numTestCasesForThisQuestion = testCasesPerQuestion;
            if (i < remainder) {
                numTestCasesForThisQuestion++; // Add extra test case to first 'remainder' questions
            }
            
            List<TestCase> questionTestCases = new ArrayList<>();
            for (int j = 0; j < numTestCasesForThisQuestion && currentIndex < allTestCases.size(); j++) {
                questionTestCases.add(allTestCases.get(currentIndex++));
            }
            
            grouped.add(questionTestCases);
        }
        
        return grouped;
    }

    /**
     * Process execution results and calculate weighted score
     */
    private Double processExecutionResults(Submission submission, CodeExecutionResponse executionResult, List<TestCase> testCases) {
        double totalScore = 0.0;
        double maxPossibleScore = testCases.stream().mapToDouble(TestCase::getWeight).sum();
        
        if (executionResult.getTestResults() != null) {
            for (TestResultResponse result : executionResult.getTestResults()) {
                if (result.isPassed()) {
                    // Find corresponding test case to get weight
                    TestCase testCase = testCases.stream()
                            .filter(tc -> tc.getId().equals(result.getTestCaseId()))
                            .findFirst()
                            .orElse(null);
                    
                    if (testCase != null) {
                        totalScore += testCase.getWeight();
                    }
                }
                
                // Save individual test result to database
                saveTestResult(submission, result, testCases);
            }
        }
        
        // Calculate percentage score
        double percentageScore = maxPossibleScore > 0 ? (totalScore / maxPossibleScore) * 100.0 : 0.0;
        
        // Apply assignment's max score if specified
        if (submission.getAssignment().getMaxScore() != null) {
            return Math.min(percentageScore, submission.getAssignment().getMaxScore());
        }
        
        return percentageScore;
    }

    /**
     * Save individual test result to database
     */
    private void saveTestResult(Submission submission, TestResultResponse testResult, List<TestCase> testCases) {
        try {
            TestCase testCase = testCases.stream()
                    .filter(tc -> tc.getId().equals(testResult.getTestCaseId()))
                    .findFirst()
                    .orElse(null);
            
            if (testCase != null) {
                TestResult result = new TestResult();
                result.setSubmission(submission);
                result.setTestCase(testCase);
                result.setActualOutput(testResult.getActualOutput());
                result.setIsPassed(testResult.isPassed());
                result.setExecutionTime(testResult.getExecutionTime());
                result.setMemoryUsed(testResult.getMemoryUsed());
                result.setErrorMessage(testResult.getErrorMessage());
                
                testResultRepository.save(result);
            }
        } catch (Exception e) {
            log.warn("Failed to save test result for submission {} and test case {}", 
                    submission.getId(), testResult.getTestCaseId(), e);
        }
    }

    /**
     * Update submission with final results
     */
    private void updateSubmissionResults(Submission submission, CodeExecutionResponse executionResult, Double finalScore) {
        submission.setScore(finalScore);
        submission.setExecutionTime(executionResult.getExecutionTime());
        submission.setMemoryUsed(executionResult.getMemoryUsed());
        submission.setGradedTime(LocalDateTime.now());
        
        // Generate feedback based on results
        String feedback = generateFeedback(executionResult, finalScore);
        submission.setFeedback(feedback);
        
        // Determine final status
        if (executionResult.isSuccess()) {
            if (finalScore >= 80.0) {
                submission.setStatus(SubmissionStatus.PASSED);
            } else if (finalScore >= 50.0) {
                submission.setStatus(SubmissionStatus.PARTIAL);
            } else {
                submission.setStatus(SubmissionStatus.FAILED);
            }
        } else {
            submission.setStatus(SubmissionStatus.COMPILATION_ERROR);
            submission.setFeedback("Lỗi biên dịch: " + executionResult.getError());
        }
        
        submissionRepository.save(submission);
    }

    /**
     * Generate detailed feedback based on execution results
     */
    private String generateFeedback(CodeExecutionResponse executionResult, Double finalScore) {
        StringBuilder feedback = new StringBuilder();
        
        feedback.append("=== KẾT QUẢ CHẤM ĐIỂM TỰ ĐỘNG ===\n");
        feedback.append(String.format("Điểm số: %.2f/100\n", finalScore));
        
        if (executionResult.getTestResults() != null) {
            int passed = (int) executionResult.getTestResults().stream().mapToLong(r -> r.isPassed() ? 1 : 0).sum();
            int total = executionResult.getTestResults().size();
            
            feedback.append(String.format("Test cases: %d/%d passed\n", passed, total));
            
            // Execution statistics
            if (executionResult.getExecutionTime() != null) {
                feedback.append(String.format("Thời gian thực thi: %d ms\n", executionResult.getExecutionTime()));
            }
            if (executionResult.getMemoryUsed() != null) {
                feedback.append(String.format("Bộ nhớ sử dụng: %.2f MB\n", executionResult.getMemoryUsed() / (1024.0 * 1024.0)));
            }
            
            // Detailed test results
            feedback.append("\n=== CHI TIẾT TEST CASES ===\n");
            for (int i = 0; i < executionResult.getTestResults().size(); i++) {
                TestResultResponse result = executionResult.getTestResults().get(i);
                feedback.append(String.format("Test %d: %s\n", i + 1, result.isPassed() ? "✓ PASS" : "✗ FAIL"));
                
                if (!result.isPassed()) {
                    if (result.getErrorMessage() != null && !result.getErrorMessage().trim().isEmpty()) {
                        feedback.append(String.format("  Lỗi: %s\n", result.getErrorMessage()));
                    }
                    if (result.getExpectedOutput() != null && result.getActualOutput() != null) {
                        feedback.append(String.format("  Kết quả mong đợi: %s\n", result.getExpectedOutput().trim()));
                        feedback.append(String.format("  Kết quả thực tế: %s\n", result.getActualOutput().trim()));
                    }
                }
            }
        }
        
        // Add compilation error if any
        if (executionResult.getError() != null && !executionResult.getError().trim().isEmpty()) {
            feedback.append("\n=== LỖI BIÊN DỊCH ===\n");
            feedback.append(executionResult.getError());
        }
        
        // Performance recommendations
        if (finalScore < 100.0) {
            feedback.append("\n=== GỢI Ý CẢI THIỆN ===\n");
            if (finalScore < 50.0) {
                feedback.append("- Kiểm tra lại logic thuật toán\n");
                feedback.append("- Đảm bảo xử lý đúng các trường hợp đầu vào\n");
            } else if (finalScore < 80.0) {
                feedback.append("- Kiểm tra các trường hợp biên (edge cases)\n");
                feedback.append("- Tối ưu hóa hiệu suất thuật toán\n");
            }
        }
        
        return feedback.toString();
    }

    /**
     * Re-grade all submissions for an assignment
     */
    public void regradeAssignment(Long assignmentId) {
        log.info("Starting re-grading for assignment {}", assignmentId);
        
        List<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId);
        
        for (Submission submission : submissions) {
            try {
                gradeSubmission(submission);
                log.info("Re-graded submission {} successfully", submission.getId());
            } catch (Exception e) {
                log.error("Failed to re-grade submission {}", submission.getId(), e);
            }
        }
        
        log.info("Completed re-grading for assignment {}. Total submissions: {}", assignmentId, submissions.size());
    }

    /**
     * Get grading statistics for an assignment
     */
    public GradingStats getGradingStats(Long assignmentId) {
        List<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId);
        
        if (submissions.isEmpty()) {
            return new GradingStats(0, 0, 0, 0, 0, 0.0);
        }
        
        int total = submissions.size();
        int passed = 0;
        int failed = 0;
        int pending = 0;
        int errors = 0;
        double averageScore = 0.0;
        
        for (Submission submission : submissions) {
            switch (submission.getStatus()) {
                case PASSED -> passed++;
                case FAILED -> failed++;
                case SUBMITTED, GRADING -> pending++;
                case ERROR, COMPILATION_ERROR -> errors++;
            }
            
            if (submission.getScore() != null) {
                averageScore += submission.getScore();
            }
        }
        
        averageScore = total > 0 ? averageScore / total : 0.0;
        
        return new GradingStats(total, passed, failed, pending, errors, averageScore);
    }

    /**
     * Grade a specific question submission (for multi-question assignments)
     */
    public AutoGradingResponse gradeQuestionSubmission(QuestionSubmission questionSubmission, List<TestCase> testCases) {
        try {
            log.info("Starting auto-grading for question submission {}", questionSubmission.getId());
            
            String code = questionSubmission.getCode();
            String language = questionSubmission.getProgrammingLanguage();
            
            if (code == null || code.trim().isEmpty()) {
                log.warn("No code provided for question submission {}", questionSubmission.getId());
                return createErrorResponse("Không có code để chấm điểm");
            }
            
            if (testCases == null || testCases.isEmpty()) {
                log.warn("No test cases provided for question submission {}", questionSubmission.getId());
                return createErrorResponse("Không có test cases để chấm điểm");
            }
            
            // Validate programming language
            ProgrammingLanguage programmingLanguage;
            try {
                programmingLanguage = ProgrammingLanguage.valueOf(language.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid programming language: {}", language);
                return createErrorResponse("Ngôn ngữ lập trình không hợp lệ: " + language);
            }
            
            // Test compilation first
            log.debug("Testing compilation for question submission {}", questionSubmission.getId());
            CodeExecutionResponse compileTest = hybridCodeExecutionService.executeCode(code, language.toLowerCase());
            
            if (!compileTest.isSuccess()) {
                log.warn("Compilation failed for question submission {}: {}", questionSubmission.getId(), compileTest.getError());
                
                AutoGradingResponse response = new AutoGradingResponse();
                response.setStatus(SubmissionStatus.COMPILATION_ERROR.toString());
                response.setTotalScore(0.0);
                response.setFeedback("Lỗi biên dịch: " + compileTest.getError());
                response.setTotalExecutionTime(0L);
                response.setMaxMemoryUsed(0L);
                return response;
            }
            
            // Run test cases
            log.debug("Running {} test cases for question submission {}", testCases.size(), questionSubmission.getId());
            
            int passed = 0;
            int total = testCases.size();
            double totalScore = 0.0;
            long totalExecutionTime = 0L;
            long maxMemoryUsed = 0L;
            StringBuilder feedbackBuilder = new StringBuilder();
            
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                
                try {
                    TestResultResponse testResult = executeTestCase(
                        code, language, testCase, questionSubmission.getQuestion()
                    );
                    
                    // Save test result (linked to question submission)
                    saveQuestionTestResult(questionSubmission, testCase, testResult);
                    
                    if (testResult.isPassed()) {
                        passed++;
                        totalScore += testCase.getWeight();
                        feedbackBuilder.append("Test ").append(i + 1).append(": ✓ PASS\n");
                    } else {
                        feedbackBuilder.append("Test ").append(i + 1).append(": ✗ FAIL\n");
                        if (testResult.getErrorMessage() != null && !testResult.getErrorMessage().trim().isEmpty()) {
                            feedbackBuilder.append("  Lỗi: ").append(testResult.getErrorMessage()).append("\n");
                        }
                        if (testResult.getActualOutput() != null && testResult.getExpectedOutput() != null) {
                            feedbackBuilder.append("  Kết quả mong đợi: ").append(truncateOutput(testResult.getExpectedOutput())).append("\n");
                            feedbackBuilder.append("  Kết quả thực tế: ").append(truncateOutput(testResult.getActualOutput())).append("\n");
                        }
                    }
                    
                    totalExecutionTime += testResult.getExecutionTime();
                    maxMemoryUsed = Math.max(maxMemoryUsed, testResult.getMemoryUsed() != null ? testResult.getMemoryUsed() : 0L);
                    
                } catch (Exception e) {
                    log.error("Error executing test case {} for question submission {}: ", i, questionSubmission.getId(), e);
                    feedbackBuilder.append("Test ").append(i + 1).append(": ✗ ERROR\n");
                    feedbackBuilder.append("  Lỗi hệ thống: ").append(e.getMessage()).append("\n");
                }
            }
            
            // Calculate final score and status
            double scorePercentage = total > 0 ? (totalScore / total) * 100.0 : 0.0;
            SubmissionStatus status = determineSubmissionStatus(passed, total, scorePercentage);
            
            // Build comprehensive feedback
            String feedback = buildQuestionFeedback(passed, total, scorePercentage, feedbackBuilder.toString());
            
            log.info("Question submission {} graded: {}/{} tests passed, score: {:.2f}%, status: {}", 
                questionSubmission.getId(), passed, total, scorePercentage, status);
            
            AutoGradingResponse response = new AutoGradingResponse();
            response.setStatus(status.toString());
            response.setTotalScore(scorePercentage);
            response.setFeedback(feedback);
            response.setTotalExecutionTime(totalExecutionTime);
            response.setMaxMemoryUsed(maxMemoryUsed);
            
            return response;
            
        } catch (Exception e) {
            log.error("Unexpected error during question submission grading: ", e);
            return createErrorResponse("Lỗi hệ thống khi chấm điểm: " + e.getMessage());
        }
    }

    /**
     * Save test result for a question submission
     */
    private void saveQuestionTestResult(QuestionSubmission questionSubmission, TestCase testCase, TestResultResponse testResult) {
        TestResult entity = new TestResult();
        entity.setSubmission(questionSubmission.getSubmission());
        entity.setQuestionSubmission(questionSubmission);
        entity.setTestCase(testCase);
        entity.setActualOutput(testResult.getActualOutput());
        entity.setIsPassed(testResult.isPassed());
        entity.setExecutionTime(testResult.getExecutionTime());
        entity.setMemoryUsed(testResult.getMemoryUsed());
        entity.setErrorMessage(testResult.getErrorMessage());
        
        testResultRepository.save(entity);
    }

    /**
     * Build feedback message for question submission
     */
    private String buildQuestionFeedback(int passed, int total, double scorePercentage, String testDetails) {
        StringBuilder feedback = new StringBuilder();
        
        feedback.append("=== KẾT QUẢ CHẤM ĐIỂM TỰ ĐỘNG ===\n");
        feedback.append("Điểm số: ").append(String.format("%.2f", scorePercentage)).append("/100\n");
        feedback.append("Test cases: ").append(passed).append("/").append(total).append(" passed\n\n");
        
        feedback.append("=== CHI TIẾT TEST CASES ===\n");
        feedback.append(testDetails);
        
        if (passed == total) {
            feedback.append("\n🎉 Xuất sắc! Tất cả test cases đều passed!");
        } else if (passed > 0) {
            feedback.append("\n⚠️  Một số test cases chưa pass. Hãy kiểm tra lại logic của bạn.");
        } else {
            feedback.append("\n❌ Không có test case nào pass. Vui lòng kiểm tra lại code.");
        }
        
        return feedback.toString();
    }

    /**
     * Statistics class for grading results
     */
    public record GradingStats(
            int totalSubmissions,
            int passedSubmissions,
            int failedSubmissions,
            int pendingSubmissions,
            int errorSubmissions,
            double averageScore
    ) {}
    
    // Helper methods
    private TestResultResponse executeTestCase(String code, String language, TestCase testCase, Question question) {
        try {
            // Basic test execution - can be enhanced later
            return TestResultResponse.builder()
                    .passed(true)
                    .expectedOutput(testCase.getExpectedOutput())
                    .actualOutput("Mock output") // TODO: implement actual code execution
                    .executionTime(100L)
                    .memoryUsed(1024L)
                    .build();
        } catch (Exception e) {
            return TestResultResponse.builder()
                    .passed(false)
                    .expectedOutput(testCase.getExpectedOutput())
                    .actualOutput("Error: " + e.getMessage())
                    .executionTime(0L)
                    .memoryUsed(0L)
                    .build();
        }
    }
    
    private String truncateOutput(String output) {
        if (output == null) return "";
        return output.length() > 200 ? output.substring(0, 200) + "..." : output;
    }
    
    private SubmissionStatus determineSubmissionStatus(int passed, int total, double scorePercentage) {
        if (passed == total) return SubmissionStatus.PASSED;
        if (passed > 0) return SubmissionStatus.PARTIAL;
        return SubmissionStatus.FAILED;
    }
    
    // Helper method to create error response
    private AutoGradingResponse createErrorResponse(String message) {
        return AutoGradingResponse.builder()
                .success(false)
                .score(0.0)
                .maxScore(0.0)
                .status("ERROR")
                .message(message)
                .feedback(message)
                .build();
    }
}
