package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.MultiQuestionSubmissionRequest;
import iuh.fit.cscore_be.dto.request.QuestionSubmissionItem;
import iuh.fit.cscore_be.dto.response.MultiQuestionSubmissionResponse;
import iuh.fit.cscore_be.dto.response.QuestionScoreResult;
import iuh.fit.cscore_be.dto.response.SubmissionResponse;
import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.enums.QuestionType;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for handling multi-question assignment submissions with automatic grading
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiQuestionSubmissionService {
    
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuestionSubmissionRepository questionSubmissionRepository;
    private final UserRepository userRepository;
    private final QuestionCodeCheckService questionCodeCheckService;
    private final AutoGradingService autoGradingService;
    private final EnhancedAutoGradingService enhancedAutoGradingService;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * Submit assignment with multiple questions and calculate total score
     */
    @Transactional
    public MultiQuestionSubmissionResponse submitMultiQuestionAssignment(
            Long assignmentId, Long studentId, MultiQuestionSubmissionRequest request) {
        
        log.info("Processing multi-question assignment submission for assignmentId: {}, studentId: {}", 
                assignmentId, studentId);

        try {
            // Validate assignment and student
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));
                    
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

            // Validate assignment is active and not past deadline
            validateAssignmentSubmission(assignment);

            // Find or create main submission
            Submission submission = findOrCreateSubmission(assignment, student, request);

            // Process each question submission
            List<QuestionScoreResult> questionResults = new ArrayList<>();
            Map<Long, QuestionSubmission> questionSubmissions = new HashMap<>();
            
            double totalScore = 0.0;
            double totalPossiblePoints = 0.0;
            
            for (QuestionSubmissionItem item : request.getQuestions()) {
                Question question = assignment.getQuestions().stream()
                        .filter(q -> q.getId().equals(item.getQuestionId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Question not found: " + item.getQuestionId()));
                
                // Create or update question submission
                QuestionSubmission questionSubmission = createOrUpdateQuestionSubmission(
                        submission, question, item, student);
                questionSubmissions.put(question.getId(), questionSubmission);
                
                // Calculate score for this question
                QuestionScoreResult result = gradeQuestion(questionSubmission, question);
                questionResults.add(result);
                
                totalScore += result.getScore();
                totalPossiblePoints += question.getPoints();
                
                log.info("Question {} graded: {}/{} points", 
                        question.getId(), result.getScore(), question.getPoints());
            }
            
            // Calculate final score as percentage scaled to assignment max score
            double finalPercentage = totalPossiblePoints > 0 ? (totalScore / totalPossiblePoints) : 0.0;
            double finalScore = finalPercentage * assignment.getMaxScore();
            
            // Update main submission with final results
            submission.setScore(finalScore);
            submission.setStatus(SubmissionStatus.GRADED);
            submission.setGradedTime(LocalDateTime.now());
            
            // Generate feedback summary
            String feedbackSummary = generateFeedbackSummary(questionResults, finalScore, assignment.getMaxScore());
            submission.setFeedback(feedbackSummary);
            
            submission = submissionRepository.save(submission);
            
            log.info("Multi-question assignment graded. Final score: {}/{} ({:.1f}%)", 
                    finalScore, assignment.getMaxScore(), finalPercentage * 100);

            // Return comprehensive response
            return new MultiQuestionSubmissionResponse(
                    submission.getId(),
                    assignment.getId(),
                    assignment.getTitle(),
                    student.getFullName(),
                    student.getStudentId(),
                    submission.getStatus(),
                    finalScore,
                    assignment.getMaxScore(),
                    finalPercentage * 100, // percentage score
                    questionResults,
                    submission.getFeedback(),
                    submission.getSubmissionTime(),
                    submission.getGradedTime()
            );
            
        } catch (Exception e) {
            log.error("Error processing multi-question submission for assignmentId: {}, studentId: {}", 
                    assignmentId, studentId, e);
            throw new RuntimeException("Không thể xử lý bài nộp: " + e.getMessage());
        }
    }
    
    /**
     * Grade individual question based on type
     */
    private QuestionScoreResult gradeQuestion(QuestionSubmission questionSubmission, Question question) {
        try {
            switch (question.getQuestionType()) {
                case PROGRAMMING:
                    return gradeProgrammingQuestion(questionSubmission, question);
                case MULTIPLE_CHOICE:
                    return gradeMultipleChoiceQuestion(questionSubmission, question);
                case TRUE_FALSE:
                    return gradeTrueFalseQuestion(questionSubmission, question);
                case ESSAY:
                    return gradeEssayQuestion(questionSubmission, question);
                default:
                    return createErrorResult(question, "Unsupported question type: " + question.getQuestionType());
            }
        } catch (Exception e) {
            log.error("Error grading question {}: {}", question.getId(), e.getMessage());
            return createErrorResult(question, "Error grading question: " + e.getMessage());
        }
    }
    
    /**
     * Grade programming question using existing code check service
     */
    private QuestionScoreResult gradeProgrammingQuestion(QuestionSubmission questionSubmission, Question question) {
        try {
            // Use existing question code check service
            var testResult = questionCodeCheckService.checkQuestionCode(
                    question.getId(), 
                    questionSubmission.getCode(), 
                    questionSubmission.getProgrammingLanguage(),
                    questionSubmission.getStudent().getId().toString()
            );
            
            // Calculate score based on test results
            double score = testResult.getScore();
            boolean isCorrect = testResult.getPassedTests() == testResult.getTotalTests() && testResult.getTotalTests() > 0;
            
            // Update question submission with results
            questionSubmission.setScore(score);
            questionSubmission.setIsCorrect(isCorrect);
            questionSubmission.setExecutionTime(testResult.getExecutionTime());
            questionSubmission.setMemoryUsed(testResult.getMemoryUsed());
            questionSubmission.setFeedback(testResult.getOutput() != null ? testResult.getOutput() : "No feedback available");
            questionSubmission.setStatus(isCorrect ? SubmissionStatus.GRADED : SubmissionStatus.COMPILATION_ERROR);
            questionSubmission.setGradedTime(LocalDateTime.now());
            
            questionSubmissionRepository.save(questionSubmission);
            
            return new QuestionScoreResult(
                    question.getId(),
                    question.getTitle(),
                    question.getQuestionType().toString(),
                    score,
                    question.getPoints(),
                    isCorrect,
                    testResult.getOutput() != null ? testResult.getOutput() : "No feedback available",
                    testResult.getTestResults()
            );
            
        } catch (Exception e) {
            log.error("Error grading programming question {}: {}", question.getId(), e.getMessage(), e);
            
            // Determine appropriate error message and status
            String errorMessage;
            SubmissionStatus errorStatus;
            
            if (e.getMessage() != null && (e.getMessage().contains("compilation failed") || e.getMessage().contains("Compilation Error"))) {
                errorMessage = "Lỗi biên dịch: Code của bạn có lỗi cú pháp hoặc không phù hợp với ngôn ngữ đã chọn. " +
                              "Vui lòng kiểm tra lại code và đảm bảo bạn đã chọn đúng ngôn ngữ lập trình.";
                errorStatus = SubmissionStatus.COMPILATION_ERROR;
                log.warn("Compilation error for question {} - student may have selected wrong language", question.getId());
            } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                errorMessage = "Lỗi thời gian chạy: Code của bạn chạy quá lâu. Vui lòng tối ưu thuật toán.";
                errorStatus = SubmissionStatus.ERROR;
            } else {
                errorMessage = "Lỗi khi chấm điểm: " + e.getMessage();
                errorStatus = SubmissionStatus.ERROR;
            }
            
            // Create error result with 0 score and save it to avoid transaction rollback
            questionSubmission.setScore(0.0);
            questionSubmission.setIsCorrect(false);
            questionSubmission.setStatus(errorStatus);
            questionSubmission.setFeedback(errorMessage);
            questionSubmission.setGradedTime(LocalDateTime.now());
            
            // Save the error state to avoid rollback
            try {
                questionSubmissionRepository.save(questionSubmission);
                log.info("Question {} graded: 0.0/{} points (Error: {})", question.getId(), question.getPoints(), errorStatus);
            } catch (Exception saveException) {
                log.error("Error saving failed question submission: {}", saveException.getMessage());
            }
            
            return createErrorResult(question, errorMessage);
        }
    }
    
    /**
     * Grade multiple choice question
     */
    private QuestionScoreResult gradeMultipleChoiceQuestion(QuestionSubmission questionSubmission, Question question) {
        // Implementation for multiple choice grading
        // This would compare selected options with correct options
        // For now, return basic implementation
        
        double score = 0.0;
        boolean isCorrect = false;
        String feedback = "Multiple choice grading not fully implemented";
        
        // Update question submission
        questionSubmission.setScore(score);
        questionSubmission.setIsCorrect(isCorrect);
        questionSubmission.setFeedback(feedback);
        questionSubmission.setStatus(isCorrect ? SubmissionStatus.GRADED : SubmissionStatus.COMPILATION_ERROR);
        questionSubmission.setGradedTime(LocalDateTime.now());
        questionSubmissionRepository.save(questionSubmission);
        
        return new QuestionScoreResult(
                question.getId(),
                question.getTitle(),
                question.getQuestionType().toString(),
                score,
                question.getPoints(),
                isCorrect,
                feedback,
                null
        );
    }
    
    /**
     * Grade true/false question
     */
    private QuestionScoreResult gradeTrueFalseQuestion(QuestionSubmission questionSubmission, Question question) {
        // Similar to multiple choice but simpler
        double score = 0.0;
        boolean isCorrect = false;
        String feedback = "True/False grading not fully implemented";
        
        questionSubmission.setScore(score);
        questionSubmission.setIsCorrect(isCorrect);
        questionSubmission.setFeedback(feedback);
        questionSubmission.setStatus(isCorrect ? SubmissionStatus.GRADED : SubmissionStatus.COMPILATION_ERROR);
        questionSubmission.setGradedTime(LocalDateTime.now());
        questionSubmissionRepository.save(questionSubmission);
        
        return new QuestionScoreResult(
                question.getId(),
                question.getTitle(),
                question.getQuestionType().toString(),
                score,
                question.getPoints(),
                isCorrect,
                feedback,
                null
        );
    }
    
    /**
     * Grade essay question (manual grading required)
     */
    private QuestionScoreResult gradeEssayQuestion(QuestionSubmission questionSubmission, Question question) {
        // Essay questions require manual grading
        double score = 0.0;
        boolean isCorrect = false;
        String feedback = "Câu tự luận cần được chấm điểm thủ công bởi giáo viên";
        
        questionSubmission.setScore(score);
        questionSubmission.setIsCorrect(false);
        questionSubmission.setFeedback(feedback);
        questionSubmission.setStatus(SubmissionStatus.SUBMITTED);
        questionSubmission.setGradedTime(LocalDateTime.now());
        questionSubmissionRepository.save(questionSubmission);
        
        return new QuestionScoreResult(
                question.getId(),
                question.getTitle(),
                question.getQuestionType().toString(),
                score,
                question.getPoints(),
                isCorrect,
                feedback,
                null
        );
    }
    
    /**
     * Create error result for failed question grading
     */
    private QuestionScoreResult createErrorResult(Question question, String errorMessage) {
        return new QuestionScoreResult(
                question.getId(),
                question.getTitle(),
                question.getQuestionType().toString(),
                0.0,
                question.getPoints(),
                false,
                errorMessage,
                null
        );
    }
    
    /**
     * Validate assignment submission constraints
     */
    private void validateAssignmentSubmission(Assignment assignment) {
        if (!assignment.getIsActive()) {
            throw new RuntimeException("Assignment is not active");
        }

        if (LocalDateTime.now().isAfter(assignment.getEndTime())) {
            if (!assignment.getAllowLateSubmission()) {
                throw new RuntimeException("Assignment submission deadline has passed");
            }
        }
    }
    
    /**
     * Find existing submission or create new one
     */
    private Submission findOrCreateSubmission(Assignment assignment, User student, MultiQuestionSubmissionRequest request) {
        Optional<Submission> existingSubmission = submissionRepository
                .findByAssignmentAndStudent(assignment, student);

        Submission submission;
        if (existingSubmission.isPresent()) {
            submission = existingSubmission.get();
            log.info("Found existing submission with ID: {}", submission.getId());
        } else {
            submission = new Submission();
            submission.setAssignment(assignment);
            submission.setStudent(student);
            submission.setSubmissionTime(LocalDateTime.now());
            log.info("Creating new submission for student: {} and assignment: {}", 
                    student.getId(), assignment.getId());
        }

        // Update submission with new data
        submission.setCode(request.getCombinedCode());
        submission.setProgrammingLanguage(request.getPrimaryLanguage());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        
        return submissionRepository.save(submission);
    }
    
    /**
     * Create or update question submission
     */
    private QuestionSubmission createOrUpdateQuestionSubmission(
            Submission submission, Question question, QuestionSubmissionItem item, User student) {
        
        Optional<QuestionSubmission> existing = questionSubmissionRepository
                .findByQuestionAndStudent(question, student);
        
        QuestionSubmission questionSubmission;
        if (existing.isPresent()) {
            questionSubmission = existing.get();
        } else {
            questionSubmission = new QuestionSubmission();
            questionSubmission.setQuestion(question);
            questionSubmission.setStudent(student);
            questionSubmission.setSubmission(submission);
        }
        
        // Update with new submission data
        if (question.getQuestionType() == QuestionType.PROGRAMMING) {
            questionSubmission.setCode(item.getAnswer());
            questionSubmission.setProgrammingLanguage(item.getLanguage());
        } else {
            questionSubmission.setSubmittedAnswer(item.getAnswer());
        }
        
        questionSubmission.setSubmissionTime(LocalDateTime.now());
        questionSubmission.setIsFinalSubmission(true);
        
        return questionSubmissionRepository.save(questionSubmission);
    }
    
    /**
     * Generate comprehensive feedback summary
     */
    private String generateFeedbackSummary(List<QuestionScoreResult> questionResults, 
                                         double finalScore, double maxScore) {
        StringBuilder feedback = new StringBuilder();
        feedback.append("=== KẾT QUẢ CHẤM BÀI ===\n");
        feedback.append(String.format("Tổng điểm: %.2f/%.2f (%.1f%%)\n\n", 
                finalScore, maxScore, (finalScore / maxScore) * 100));
        
        feedback.append("Chi tiết từng câu hỏi:\n");
        for (int i = 0; i < questionResults.size(); i++) {
            QuestionScoreResult result = questionResults.get(i);
            feedback.append(String.format("%d. %s: %.2f/%.2f điểm %s\n", 
                    i + 1, 
                    result.getQuestionTitle(), 
                    result.getScore(), 
                    result.getMaxPoints(),
                    result.isCorrect() ? "✓" : "✗"));
                    
            if (result.getFeedback() != null && !result.getFeedback().isEmpty()) {
                feedback.append("   → ").append(result.getFeedback()).append("\n");
            }
        }
        
        return feedback.toString();
    }
    
    /**
     * Convert to legacy SubmissionResponse for backward compatibility
     */
    public SubmissionResponse convertToLegacyResponse(MultiQuestionSubmissionResponse response) {
        return new SubmissionResponse(
                response.getSubmissionId(),
                response.getAssignmentId(),
                response.getAssignmentTitle(),
                response.getStudentName(),
                response.getStudentId(),
                "mixed", // programming language
                response.getStatus(),
                response.getFinalScore(),
                null, // execution time
                null, // memory used
                response.getFeedback(),
                response.getSubmissionTime(),
                response.getGradedTime(),
                null, // test cases passed
                null  // total test cases
        );
    }
}