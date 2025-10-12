package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.SubmissionRequest;
import iuh.fit.cscore_be.dto.response.AutoGradingResponse;
import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.dto.response.SubmissionResponse;
import iuh.fit.cscore_be.dto.response.TestResultResponse;
import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.exception.ResourceNotFoundException;
import iuh.fit.cscore_be.repository.AssignmentRepository;
import iuh.fit.cscore_be.repository.QuestionRepository;
import iuh.fit.cscore_be.repository.SubmissionRepository;
import iuh.fit.cscore_be.repository.TestCaseRepository;
import iuh.fit.cscore_be.repository.TestResultRepository;
import iuh.fit.cscore_be.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Auto Grading Service
 * Unified service with multiple grading modes:
 * - BASIC: Simple pass/fail scoring
 * - ENHANCED: Advanced scoring with partial credit
 * - COMPARATIVE: Comparison with reference implementations
 * 
 * Features:
 * - Multi-question assignment support
 * - Advanced scoring algorithms
 * - Reference implementation comparison
 * - Detailed feedback generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AutoGradingService {
    
    private final CodeExecutionService codeExecutionService;
    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestResultRepository testResultRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    
    @Value("${grading.time-limit:30}")
    private int defaultTimeLimit;
    
    @Value("${grading.memory-limit:256}")
    private int defaultMemoryLimit;
    
    @Value("${grading.mode:enhanced}")
    private String defaultGradingMode; // basic, enhanced, comparative

    /**
     * Main entry point for asynchronous grading
     */
    @Async
    public CompletableFuture<Double> gradeSubmissionAsync(Long submissionId) {
        return gradeSubmissionAsync(submissionId, GradingMode.valueOf(defaultGradingMode.toUpperCase()));
    }
    
    @Async
    public CompletableFuture<Double> gradeSubmissionAsync(Long submissionId, GradingMode mode) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));
            
            Double score = gradeSubmission(submission, mode);
            return CompletableFuture.completedFuture(score);
        } catch (Exception e) {
            log.error("Error in async grading for submission {}", submissionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Main synchronous grading method
     */
    public Double gradeSubmission(Submission submission) {
        return gradeSubmission(submission, GradingMode.valueOf(defaultGradingMode.toUpperCase()));
    }
    
    public Double gradeSubmission(Submission submission, GradingMode mode) {
        log.info("Starting {} grading for submission {} (Student: {}, Assignment: {})", 
                mode, submission.getId(), submission.getStudent().getStudentId(), submission.getAssignment().getTitle());
        
        try {
            // Update status to GRADING
            submission.setStatus(SubmissionStatus.GRADING);
            submissionRepository.save(submission);
            
            // Choose grading strategy based on mode
            GradingResult result = switch (mode) {
                case BASIC -> performBasicGrading(submission);
                case ENHANCED -> performEnhancedGrading(submission);
                case COMPARATIVE -> performComparativeGrading(submission);
            };
            
            // Update submission with results
            updateSubmissionResults(submission, result);
            
            log.info("{} grading completed for submission {}. Final score: {}", 
                    mode, submission.getId(), result.getFinalScore());
            
            return result.getFinalScore();
            
        } catch (Exception e) {
            log.error("Error during {} grading for submission {}", mode, submission.getId(), e);
            handleGradingError(submission, e);
            return 0.0;
        }
    }

    // ========== BASIC GRADING ==========
    
    private GradingResult performBasicGrading(Submission submission) {
        log.info("Performing basic grading for submission {}", submission.getId());
        
        List<TestCase> allTestCases = getAllTestCases(submission.getAssignment());
        
        if (allTestCases.isEmpty()) {
            return handleNoTestCases(submission);
        }
        
        // Execute code with test cases
        CodeExecutionResponse executionResult = executeSubmissionCode(submission, allTestCases);
        
        // Calculate basic score
        double score = calculateBasicScore(executionResult, allTestCases);
        
        GradingResult result = new GradingResult();
        result.setMode(GradingMode.BASIC);
        result.setFinalScore(score);
        result.setExecutionResult(executionResult);
        result.setFeedback(generateBasicFeedback(executionResult));
        result.setDetailsJson(generateBasicDetails(executionResult));
        
        return result;
    }
    
    private double calculateBasicScore(CodeExecutionResponse executionResult, List<TestCase> testCases) {
        if (!executionResult.isSuccess() || executionResult.getTestResults() == null) {
            return 0.0;
        }
        
        double totalWeight = testCases.stream().mapToDouble(TestCase::getWeight).sum();
        double earnedWeight = 0.0;
        
        for (TestResultResponse testResult : executionResult.getTestResults()) {
            if (testResult.isPassed()) {
                TestCase testCase = testCases.stream()
                        .filter(tc -> tc.getId().equals(testResult.getTestCaseId()))
                        .findFirst()
                        .orElse(null);
                if (testCase != null) {
                    earnedWeight += testCase.getWeight();
                }
            }
        }
        
        return totalWeight > 0 ? (earnedWeight / totalWeight) * 100.0 : 0.0;
    }

    // ========== ENHANCED GRADING ==========
    
    private GradingResult performEnhancedGrading(Submission submission) {
        log.info("Performing enhanced grading for submission {}", submission.getId());
        
        Assignment assignment = submission.getAssignment();
        List<Question> questions = assignment.getQuestions();
        
        if (questions.isEmpty()) {
            return handleNoQuestions(submission);
        }
        
        // Process multi-question submissions
        if (isMultiQuestionCode(submission.getCode())) {
            return gradeMultiQuestionSubmission(submission, questions);
        } else {
            return gradeSingleQuestionSubmission(submission, questions);
        }
    }
    
    private GradingResult gradeMultiQuestionSubmission(Submission submission, List<Question> questions) {
        log.info("Grading multi-question submission with {} questions", questions.size());
        
        String fullCode = submission.getCode();
        List<String> questionCodes = splitMultiQuestionCode(fullCode);
        
        if (questionCodes.size() != questions.size()) {
            log.warn("Mismatch between question count ({}) and code sections ({})", 
                     questions.size(), questionCodes.size());
        }
        
        List<QuestionGradingResult> questionResults = new ArrayList<>();
        double totalScore = 0.0;
        double totalPossibleScore = 0.0;
        
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            String questionCode = i < questionCodes.size() ? questionCodes.get(i) : "";
            
            QuestionGradingResult questionResult = gradeIndividualQuestion(
                questionCode, question, submission.getProgrammingLanguage());
            
            questionResults.add(questionResult);
            totalScore += questionResult.getScore();
            totalPossibleScore += question.getPoints();
        }
        
        double finalPercentage = totalPossibleScore > 0 ? (totalScore / totalPossibleScore) * 100.0 : 0.0;
        
        GradingResult result = new GradingResult();
        result.setMode(GradingMode.ENHANCED);
        result.setFinalScore(finalPercentage);
        result.setQuestionResults(questionResults);
        result.setFeedback(generateEnhancedFeedback(questionResults, finalPercentage));
        result.setDetailsJson(generateEnhancedDetails(questionResults));
        
        return result;
    }
    
    private GradingResult gradeSingleQuestionSubmission(Submission submission, List<Question> questions) {
        Question primaryQuestion = questions.get(0); // Use first question as primary
        List<TestCase> allTestCases = getAllTestCases(submission.getAssignment());
        
        if (allTestCases.isEmpty()) {
            return handleNoTestCases(submission);
        }
        
        // Execute with question context for better wrapping
        CodeExecutionResponse executionResult = codeExecutionService.executeCodeWithTestCases(
            submission.getCode(), submission.getProgrammingLanguage(), allTestCases, submission, primaryQuestion);
        
        double score = calculateEnhancedScore(executionResult, allTestCases);
        
        GradingResult result = new GradingResult();
        result.setMode(GradingMode.ENHANCED);
        result.setFinalScore(score);
        result.setExecutionResult(executionResult);
        result.setFeedback(generateEnhancedFeedback(executionResult));
        result.setDetailsJson(generateEnhancedDetails(executionResult));
        
        return result;
    }
    
    private QuestionGradingResult gradeIndividualQuestion(String questionCode, Question question, String language) {
        QuestionGradingResult result = new QuestionGradingResult();
        result.setQuestionId(question.getId());
        result.setQuestionTitle(question.getTitle());
        result.setMaxScore(question.getPoints());
        
        try {
            List<TestCase> questionTestCases = question.getTestCases();
            
            if (questionTestCases.isEmpty()) {
                result.setScore(0.0);
                result.setFeedback("Câu hỏi này không có test case");
                return result;
            }
            
            // Execute question code with test cases
            CodeExecutionResponse executionResult = codeExecutionService.executeCodeWithTestCases(
                questionCode, language, questionTestCases, null, question);
            
            // Calculate score for this question
            double score = calculateQuestionScore(executionResult, questionTestCases, question.getPoints());
            
            result.setScore(score);
            result.setExecutionResult(executionResult);
            result.setFeedback(generateQuestionFeedback(executionResult, question));
            
        } catch (Exception e) {
            log.error("Error grading question {}: {}", question.getId(), e.getMessage());
            result.setScore(0.0);
            result.setFeedback("Lỗi khi chấm câu hỏi: " + e.getMessage());
        }
        
        return result;
    }

    // ========== COMPARATIVE GRADING ==========
    
    private GradingResult performComparativeGrading(Submission submission) {
        log.info("Performing comparative grading for submission {}", submission.getId());
        
        // First perform enhanced grading
        GradingResult enhancedResult = performEnhancedGrading(submission);
        
        // Then compare with reference implementations if available
        List<Question> questions = submission.getAssignment().getQuestions();
        List<ComparativeResult> comparativeResults = new ArrayList<>();
        
        for (Question question : questions) {
            if (question.getReferenceImplementation() != null && !question.getReferenceImplementation().trim().isEmpty()) {
                ComparativeResult compResult = compareWithReference(submission, question);
                comparativeResults.add(compResult);
            }
        }
        
        // Adjust score based on comparative analysis
        double adjustedScore = applyComparativeAdjustments(enhancedResult.getFinalScore(), comparativeResults);
        
        GradingResult result = new GradingResult();
        result.setMode(GradingMode.COMPARATIVE);
        result.setFinalScore(adjustedScore);
        result.setExecutionResult(enhancedResult.getExecutionResult());
        result.setQuestionResults(enhancedResult.getQuestionResults());
        result.setComparativeResults(comparativeResults);
        result.setFeedback(generateComparativeFeedback(enhancedResult, comparativeResults, adjustedScore));
        result.setDetailsJson(generateComparativeDetails(enhancedResult, comparativeResults));
        
        return result;
    }
    
    private ComparativeResult compareWithReference(Submission submission, Question question) {
        ComparativeResult result = new ComparativeResult();
        result.setQuestionId(question.getId());
        
        try {
            String studentCode = extractQuestionCode(submission.getCode(), question);
            String referenceCode = question.getReferenceImplementation();
            List<TestCase> testCases = question.getTestCases();
            
            // Execute both implementations
            CodeExecutionResponse studentResult = codeExecutionService.executeCodeWithTestCases(
                studentCode, submission.getProgrammingLanguage(), testCases, null, question);
            
            CodeExecutionResponse referenceResult = codeExecutionService.executeCodeWithTestCases(
                referenceCode, submission.getProgrammingLanguage(), testCases, null, question);
            
            // Compare results
            double similarity = calculateOutputSimilarity(studentResult, referenceResult);
            
            result.setStudentResult(studentResult);
            result.setReferenceResult(referenceResult);
            result.setSimilarityScore(similarity);
            result.setAnalysis(generateComparisonAnalysis(studentResult, referenceResult, similarity));
            
        } catch (Exception e) {
            log.error("Error in comparative analysis for question {}: {}", question.getId(), e.getMessage());
            result.setAnalysis("Lỗi khi so sánh với đáp án tham khảo: " + e.getMessage());
            result.setSimilarityScore(0.0);
        }
        
        return result;
    }

    // ========== HELPER METHODS ==========
    
    private CodeExecutionResponse executeSubmissionCode(Submission submission, List<TestCase> testCases) {
        if (isMultiQuestionCode(submission.getCode())) {
            return executeMultiQuestionCode(submission, testCases);
        } else {
            // For single question, try to use first question context if available
            Question firstQuestion = submission.getAssignment().getQuestions().stream()
                    .findFirst().orElse(null);
            
            return codeExecutionService.executeCodeWithTestCases(
                submission.getCode(), submission.getProgrammingLanguage(), testCases, submission, firstQuestion);
        }
    }
    
    private CodeExecutionResponse executeMultiQuestionCode(Submission submission, List<TestCase> testCases) {
        // This is a simplified version - the actual implementation would need to
        // properly split code and match test cases to questions
        return codeExecutionService.executeCodeWithTestCases(
            submission.getCode(), submission.getProgrammingLanguage(), testCases, submission);
    }
    
    private List<TestCase> getAllTestCases(Assignment assignment) {
        return assignment.getQuestions().stream()
                .flatMap(question -> question.getTestCases().stream())
                .collect(Collectors.toList());
    }
    
    private boolean isMultiQuestionCode(String code) {
        return code != null && (
            code.contains("// --- Next Question ---") ||
            code.contains("/* --- Next Question --- */") ||
            code.contains("# --- Next Question ---") ||
            code.contains("-- Next Question --")
        );
    }
    
    private List<String> splitMultiQuestionCode(String code) {
        List<String> parts = new ArrayList<>();
        
        String[] delimiters = {
            "// --- Next Question ---",
            "/* --- Next Question --- */",
            "# --- Next Question ---",
            "-- Next Question --"
        };
        
        String[] sections = code.split("(" + String.join("|", delimiters) + ")");
        
        for (String section : sections) {
            String trimmed = section.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        
        return parts;
    }
    
    private String extractQuestionCode(String fullCode, Question question) {
        if (isMultiQuestionCode(fullCode)) {
            List<String> codes = splitMultiQuestionCode(fullCode);
            int questionIndex = question.getOrderIndex();
            if (questionIndex >= 0 && questionIndex < codes.size()) {
                return codes.get(questionIndex);
            }
        }
        return fullCode;
    }

    // ========== SCORING METHODS ==========
    
    private double calculateEnhancedScore(CodeExecutionResponse executionResult, List<TestCase> testCases) {
        if (!executionResult.isSuccess() || executionResult.getTestResults() == null) {
            return 0.0;
        }
        
        double totalWeight = testCases.stream().mapToDouble(TestCase::getWeight).sum();
        double earnedWeight = 0.0;
        
        // Enhanced scoring with partial credit
        for (TestResultResponse testResult : executionResult.getTestResults()) {
            TestCase testCase = testCases.stream()
                    .filter(tc -> tc.getId().equals(testResult.getTestCaseId()))
                    .findFirst()
                    .orElse(null);
            
            if (testCase != null) {
                if (testResult.isPassed()) {
                    earnedWeight += testCase.getWeight();
                } else {
                    // Partial credit for close outputs
                    double partialCredit = calculatePartialCredit(testResult, testCase);
                    earnedWeight += testCase.getWeight() * partialCredit;
                }
            }
        }
        
        return totalWeight > 0 ? (earnedWeight / totalWeight) * 100.0 : 0.0;
    }
    
    private double calculateQuestionScore(CodeExecutionResponse executionResult, List<TestCase> testCases, double maxPoints) {
        if (!executionResult.isSuccess() || executionResult.getTestResults() == null) {
            return 0.0;
        }
        
        double totalWeight = testCases.stream().mapToDouble(TestCase::getWeight).sum();
        double earnedWeight = 0.0;
        
        for (TestResultResponse testResult : executionResult.getTestResults()) {
            if (testResult.isPassed()) {
                TestCase testCase = testCases.stream()
                        .filter(tc -> tc.getId().equals(testResult.getTestCaseId()))
                        .findFirst()
                        .orElse(null);
                if (testCase != null) {
                    earnedWeight += testCase.getWeight();
                }
            }
        }
        
        double percentage = totalWeight > 0 ? earnedWeight / totalWeight : 0.0;
        return percentage * maxPoints;
    }
    
    private double calculatePartialCredit(TestResultResponse testResult, TestCase testCase) {
        // Simple partial credit based on output similarity
        if (testResult.getExpectedOutput() == null || testResult.getActualOutput() == null) {
            return 0.0;
        }
        
        String expected = testResult.getExpectedOutput().trim();
        String actual = testResult.getActualOutput().trim();
        
        if (expected.equals(actual)) {
            return 1.0;
        }
        
        // Calculate similarity (simple Levenshtein-based approach)
        int maxLen = Math.max(expected.length(), actual.length());
        if (maxLen == 0) return 1.0;
        
        int editDistance = calculateEditDistance(expected, actual);
        double similarity = 1.0 - (double) editDistance / maxLen;
        
        // Give partial credit only if similarity is above threshold
        return similarity > 0.7 ? similarity * 0.3 : 0.0; // Max 30% partial credit
    }
    
    private int calculateEditDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(
                        dp[i-1][j] + 1,
                        dp[i][j-1] + 1),
                        dp[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    private double calculateOutputSimilarity(CodeExecutionResponse student, CodeExecutionResponse reference) {
        if (student.getTestResults() == null || reference.getTestResults() == null) {
            return 0.0;
        }
        
        int totalTests = Math.min(student.getTestResults().size(), reference.getTestResults().size());
        if (totalTests == 0) return 0.0;
        
        int matchingOutputs = 0;
        for (int i = 0; i < totalTests; i++) {
            TestResultResponse studentResult = student.getTestResults().get(i);
            TestResultResponse referenceResult = reference.getTestResults().get(i);
            
            if (studentResult.getActualOutput() != null && referenceResult.getActualOutput() != null) {
                if (studentResult.getActualOutput().trim().equals(referenceResult.getActualOutput().trim())) {
                    matchingOutputs++;
                }
            }
        }
        
        return (double) matchingOutputs / totalTests;
    }
    
    private double applyComparativeAdjustments(double baseScore, List<ComparativeResult> comparativeResults) {
        if (comparativeResults.isEmpty()) {
            return baseScore;
        }
        
        // Calculate average similarity with reference implementations
        double avgSimilarity = comparativeResults.stream()
                .mapToDouble(ComparativeResult::getSimilarityScore)
                .average()
                .orElse(0.0);
        
        // Apply small adjustment based on similarity
        double adjustment = (avgSimilarity - 0.5) * 5.0; // ±2.5% adjustment
        
        return Math.max(0.0, Math.min(100.0, baseScore + adjustment));
    }

    // ========== FEEDBACK GENERATION ==========
    
    private String generateBasicFeedback(CodeExecutionResponse executionResult) {
        if (!executionResult.isSuccess()) {
            return "Code có lỗi trong quá trình thực thi: " + executionResult.getError();
        }
        
        int passed = executionResult.getPassedTests();
        int total = executionResult.getTotalTests();
        
        return String.format("Kết quả chấm điểm: %d/%d test cases đạt yêu cầu.", passed, total);
    }
    
    private String generateEnhancedFeedback(CodeExecutionResponse executionResult) {
        if (!executionResult.isSuccess()) {
            return "Code có lỗi trong quá trình thực thi: " + executionResult.getError();
        }
        
        int passed = executionResult.getPassedTests();
        int total = executionResult.getTotalTests();
        
        StringBuilder feedback = new StringBuilder();
        feedback.append(String.format("Kết quả chấm điểm nâng cao: %d/%d test cases đạt yêu cầu.\n", passed, total));
        
        if (passed < total) {
            feedback.append("Một số test cases chưa đạt yêu cầu. Hãy kiểm tra lại logic và các trường hợp biên.\n");
        }
        
        if (executionResult.getExecutionTime() != null) {
            feedback.append(String.format("Thời gian thực thi: %d ms.\n", executionResult.getExecutionTime()));
        }
        
        return feedback.toString();
    }
    
    private String generateEnhancedFeedback(List<QuestionGradingResult> questionResults, double finalScore) {
        StringBuilder feedback = new StringBuilder();
        feedback.append(String.format("Kết quả chấm điểm tổng thể: %.1f/100\n\n", finalScore));
        
        for (int i = 0; i < questionResults.size(); i++) {
            QuestionGradingResult result = questionResults.get(i);
            feedback.append(String.format("Câu %d (%s): %.1f/%.1f điểm\n", 
                    i + 1, result.getQuestionTitle(), result.getScore(), result.getMaxScore()));
            
            if (result.getFeedback() != null && !result.getFeedback().isEmpty()) {
                feedback.append("  ").append(result.getFeedback()).append("\n");
            }
            feedback.append("\n");
        }
        
        return feedback.toString();
    }
    
    private String generateComparativeFeedback(GradingResult enhancedResult, 
                                             List<ComparativeResult> comparativeResults, 
                                             double adjustedScore) {
        StringBuilder feedback = new StringBuilder();
        feedback.append(enhancedResult.getFeedback());
        
        if (!comparativeResults.isEmpty()) {
            feedback.append("\n--- Phân tích so sánh với đáp án tham khảo ---\n");
            
            for (ComparativeResult compResult : comparativeResults) {
                if (compResult.getAnalysis() != null) {
                    feedback.append(compResult.getAnalysis()).append("\n");
                }
            }
            
            double avgSimilarity = comparativeResults.stream()
                    .mapToDouble(ComparativeResult::getSimilarityScore)
                    .average()
                    .orElse(0.0);
            
            feedback.append(String.format("\nĐộ tương đồng trung bình với đáp án: %.1f%%\n", avgSimilarity * 100));
            feedback.append(String.format("Điểm số sau điều chỉnh: %.1f/100\n", adjustedScore));
        }
        
        return feedback.toString();
    }
    
    private String generateQuestionFeedback(CodeExecutionResponse executionResult, Question question) {
        if (!executionResult.isSuccess()) {
            return "Lỗi thực thi: " + executionResult.getError();
        }
        
        int passed = executionResult.getPassedTests();
        int total = executionResult.getTotalTests();
        
        return String.format("%d/%d test cases đạt yêu cầu", passed, total);
    }
    
    private String generateComparisonAnalysis(CodeExecutionResponse student, 
                                            CodeExecutionResponse reference, 
                                            double similarity) {
        if (similarity >= 0.9) {
            return "Kết quả đầu ra rất giống với đáp án tham khảo";
        } else if (similarity >= 0.7) {
            return "Kết quả đầu ra tương đối giống với đáp án tham khảo";
        } else if (similarity >= 0.5) {
            return "Kết quả đầu ra có một số điểm giống với đáp án tham khảo";
        } else {
            return "Kết quả đầu ra khác biệt đáng kể so với đáp án tham khảo";
        }
    }

    // ========== UTILITY METHODS ==========
    
    private GradingResult handleNoTestCases(Submission submission) {
        log.warn("No test cases found for assignment {}", submission.getAssignment().getId());
        
        GradingResult result = new GradingResult();
        result.setMode(GradingMode.BASIC);
        result.setFinalScore(0.0);
        result.setFeedback("Bài tập này không có test case để chấm điểm");
        
        return result;
    }
    
    private GradingResult handleNoQuestions(Submission submission) {
        log.warn("No questions found for assignment {}", submission.getAssignment().getId());
        
        GradingResult result = new GradingResult();
        result.setMode(GradingMode.ENHANCED);
        result.setFinalScore(0.0);
        result.setFeedback("Bài tập này không có câu hỏi để chấm điểm");
        
        return result;
    }
    
    private void updateSubmissionResults(Submission submission, GradingResult result) {
        submission.setScore(result.getFinalScore());
        submission.setFeedback(result.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedTime(LocalDateTime.now());
        
        // Store detailed results as JSON if available
        if (result.getDetailsJson() != null) {
            submission.setGradingDetails(result.getDetailsJson());
        }
        
        submissionRepository.save(submission);
    }
    
    private void handleGradingError(Submission submission, Exception e) {
        submission.setStatus(SubmissionStatus.ERROR);
        submission.setScore(0.0);
        submission.setFeedback("Lỗi trong quá trình chấm điểm tự động: " + e.getMessage());
        submission.setGradedTime(LocalDateTime.now());
        submissionRepository.save(submission);
    }
    
    private String generateBasicDetails(CodeExecutionResponse executionResult) {
        // Return JSON representation of basic results
        return String.format("{\"mode\":\"basic\",\"passed\":%d,\"total\":%d,\"success\":%b}", 
                executionResult.getPassedTests(), 
                executionResult.getTotalTests(), 
                executionResult.isSuccess());
    }
    
    private String generateEnhancedDetails(CodeExecutionResponse executionResult) {
        // Return JSON representation of enhanced results
        return String.format("{\"mode\":\"enhanced\",\"passed\":%d,\"total\":%d,\"execution_time\":%d}", 
                executionResult.getPassedTests(), 
                executionResult.getTotalTests(), 
                executionResult.getExecutionTime() != null ? executionResult.getExecutionTime() : 0);
    }
    
    private String generateEnhancedDetails(List<QuestionGradingResult> questionResults) {
        // Return JSON representation of multi-question results
        StringBuilder json = new StringBuilder("{\"mode\":\"enhanced\",\"questions\":[");
        for (int i = 0; i < questionResults.size(); i++) {
            if (i > 0) json.append(",");
            QuestionGradingResult result = questionResults.get(i);
            json.append(String.format("{\"id\":%d,\"score\":%.1f,\"max_score\":%.1f}", 
                    result.getQuestionId(), result.getScore(), result.getMaxScore()));
        }
        json.append("]}");
        return json.toString();
    }
    
    private String generateComparativeDetails(GradingResult enhancedResult, List<ComparativeResult> comparativeResults) {
        // Return JSON representation of comparative results
        double avgSimilarity = comparativeResults.stream()
                .mapToDouble(ComparativeResult::getSimilarityScore)
                .average()
                .orElse(0.0);
        
        return String.format("{\"mode\":\"comparative\",\"base_score\":%.1f,\"similarity\":%.2f,\"questions\":%d}", 
                enhancedResult.getFinalScore(), avgSimilarity, comparativeResults.size());
    }

    // ========== DATA CLASSES ==========
    
    public enum GradingMode {
        BASIC,      // Simple pass/fail scoring
        ENHANCED,   // Advanced scoring with partial credit
        COMPARATIVE // Comparison with reference implementations
    }
    
    @Data
    public static class GradingResult {
        private GradingMode mode;
        private double finalScore;
        private String feedback;
        private String detailsJson;
        private CodeExecutionResponse executionResult;
        private List<QuestionGradingResult> questionResults;
        private List<ComparativeResult> comparativeResults;
    }
    
    @Data
    public static class QuestionGradingResult {
        private Long questionId;
        private String questionTitle;
        private double score;
        private double maxScore;
        private String feedback;
        private CodeExecutionResponse executionResult;
    }
    
    @Data
    public static class ComparativeResult {
        private Long questionId;
        private CodeExecutionResponse studentResult;
        private CodeExecutionResponse referenceResult;
        private double similarityScore;
        private String analysis;
    }
    
    // ========== ADDITIONAL METHODS FOR CONTROLLER COMPATIBILITY ==========
    
    /**
     * Enhanced grading method
     */
    @Async
    public CompletableFuture<Double> gradeSubmissionEnhanced(Long submissionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Submission submission = submissionRepository.findById(submissionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
                
                return gradeSubmission(submission);
            } catch (Exception e) {
                log.error("Error in enhanced grading for submission {}: {}", submissionId, e.getMessage());
                throw new RuntimeException("Enhanced grading failed", e);
            }
        });
    }
    
    /**
     * Check question code for real-time feedback
     */
    public CodeExecutionResponse checkQuestionCode(Long questionId, String code, String language, String input, String studentId) {
        try {
            // Find the question to get its test cases
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
            
            List<TestCase> testCases = question.getTestCases();
            
            if (testCases.isEmpty()) {
                log.warn("No test cases found for question {}", questionId);
                return CodeExecutionResponse.builder()
                    .success(false)
                    .error("No test cases available for this question")
                    .language(language)
                    .executionTime(0L)
                    .build();
            }
            
            log.info("Running question {} code check with {} test cases for student {}", 
                    questionId, testCases.size(), studentId);
            
            // Execute code with test cases
            CodeExecutionResponse result = codeExecutionService.executeCodeWithTestCases(
                code, language, testCases, null, question);
            
            log.info("Question code check completed for question {} by student {}: success={}, passed={}/{}", 
                    questionId, studentId, result.isSuccess(), result.getPassedTests(), result.getTotalTests());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error checking question code for question {} by student {}: {}", 
                    questionId, studentId, e.getMessage(), e);
            
            return CodeExecutionResponse.builder()
                .success(false)
                .error("Code check failed: " + e.getMessage())
                .language(language)
                .executionTime(0L)
                .build();
        }
    }
    
    /**
     * Get question score for a student
     */
    public Double getQuestionScore(Long questionId, String studentId) {
        try {
            // Find user first
            User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
            
            // Find assignment
            Assignment assignment = assignmentRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + questionId));
            
            // Find latest submission for this assignment by this student
            Optional<Submission> submissionOpt = submissionRepository.findByAssignmentAndStudent(assignment, student);
            
            if (submissionOpt.isEmpty()) {
                return 0.0;
            }
            
            // Get the score from the submission
            return submissionOpt.get().getScore() != null ? submissionOpt.get().getScore() : 0.0;
        } catch (Exception e) {
            log.error("Error getting question score for question {} by student {}: {}", 
                    questionId, studentId, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Submit question answer
     */
    public CodeExecutionResponse submitQuestionAnswer(Long questionId, String code, String language, String studentId) {
        try {
            // Execute and grade the submitted code
            CodeExecutionResponse result = codeExecutionService.executeCode(code, language);
            
            log.info("Question answer submitted for question {} by student {}: success={}", 
                    questionId, studentId, result.isSuccess());
            
            return result;
        } catch (Exception e) {
            log.error("Error submitting question answer for question {} by student {}: {}", 
                    questionId, studentId, e.getMessage());
            
            return CodeExecutionResponse.builder()
                .success(false)
                .error("Submission failed: " + e.getMessage())
                .language(language)
                .executionTime(0L)
                .build();
        }
    }
    
    /**
     * Submit and grade basic assignment
     */
    public SubmissionResponse submitAndGradeBasic(SubmissionRequest request, Assignment assignment, Long studentId) {
        try {
            // Create submission entity
            Submission submission = new Submission();
            submission.setStudent(userRepository.findById(studentId).orElse(null));
            submission.setAssignment(assignment);
            submission.setCode(request.getCode());
            submission.setProgrammingLanguage(request.getProgrammingLanguage());
            submission.setStatus(SubmissionStatus.SUBMITTED);
            submission.setSubmissionTime(LocalDateTime.now());
            
            submission = submissionRepository.save(submission);
            
            // Auto-grade if enabled
            if (assignment.getAutoGrade()) {
                gradeSubmission(submission);
                // Reload to get updated score
                submission = submissionRepository.findById(submission.getId()).orElse(submission);
            }
            
            return convertToSubmissionResponse(submission);
        } catch (Exception e) {
            log.error("Error in submit and grade basic for assignment {} by student {}: {}", 
                    assignment.getId(), studentId, e.getMessage());
            throw new RuntimeException("Submission failed", e);
        }
    }
    
    /**
     * Regrade assignment
     */
    public String regradeAssignment(Long assignmentId) {
        try {
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
            
            List<Submission> submissions = submissionRepository.findByAssignment(assignment);
            int regradedCount = 0;
            
            for (Submission submission : submissions) {
                try {
                    gradeSubmission(submission);
                    regradedCount++;
                } catch (Exception e) {
                    log.error("Error regrading submission {}: {}", submission.getId(), e.getMessage());
                }
            }
            
            return String.format("Regraded %d submissions for assignment %d", regradedCount, assignmentId);
        } catch (Exception e) {
            log.error("Error regrading assignment {}: {}", assignmentId, e.getMessage());
            throw new RuntimeException("Regrade failed", e);
        }
    }
    
    /**
     * Get grading statistics
     */
    public Map<String, Object> getGradingStats(Long assignmentId) {
        try {
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
            
            List<Submission> submissions = submissionRepository.findByAssignment(assignment);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSubmissions", submissions.size());
            stats.put("gradedSubmissions", submissions.stream().filter(s -> s.getScore() != null).count());
            stats.put("averageScore", submissions.stream().filter(s -> s.getScore() != null)
                    .mapToDouble(Submission::getScore).average().orElse(0.0));
            stats.put("maxScore", submissions.stream().filter(s -> s.getScore() != null)
                    .mapToDouble(Submission::getScore).max().orElse(0.0));
            stats.put("minScore", submissions.stream().filter(s -> s.getScore() != null)
                    .mapToDouble(Submission::getScore).min().orElse(0.0));
            
            return stats;
        } catch (Exception e) {
            log.error("Error getting grading stats for assignment {}: {}", assignmentId, e.getMessage());
            return Map.of("error", "Failed to get grading stats");
        }
    }
    
    /**
     * Convert submission to response
     */
    private SubmissionResponse convertToSubmissionResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentName(submission.getStudent() != null ? submission.getStudent().getFullName() : null)
                .studentId(submission.getStudent() != null ? submission.getStudent().getStudentId() : null)
                .programmingLanguage(submission.getProgrammingLanguage())
                .status(submission.getStatus())
                .score(submission.getScore())
                .feedback(submission.getFeedback())
                .submissionTime(submission.getSubmissionTime())
                .gradedTime(submission.getGradedTime())
                .build();
    }
}