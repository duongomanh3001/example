package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.MultiQuestionSubmissionRequest;
import iuh.fit.cscore_be.dto.request.QuestionSubmissionItem;
import iuh.fit.cscore_be.dto.request.SubmissionRequest;
import iuh.fit.cscore_be.dto.response.MultiQuestionSubmissionResponse;
import iuh.fit.cscore_be.dto.response.SubmissionResponse;
import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service  
@RequiredArgsConstructor
public class StudentService {
    
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AutoGradingService autoGradingService;
    private final EnhancedAutoGradingService enhancedAutoGradingService;
    private final UserRepository userRepository;
    private final MultiQuestionSubmissionService multiQuestionSubmissionService;
    
    // Enhanced submit assignment method with multi-question support
    @Transactional
    public SubmissionResponse submitAssignment(Long assignmentId, Long studentId, SubmissionRequest request) {
        log.info("Processing assignment submission for assignmentId: {}, studentId: {}", 
                assignmentId, studentId);

        try {
            // Validate assignment exists
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));
                    
            // Get student user
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

            // Check if this is a multi-question assignment
            if (assignment.getQuestions() != null && assignment.getQuestions().size() > 1) {
                log.info("Detected multi-question assignment with {} questions", assignment.getQuestions().size());
                return handleMultiQuestionSubmission(assignmentId, studentId, request, assignment);
            }

            // Handle single question or legacy assignment
            return handleLegacySubmission(assignmentId, studentId, request, assignment, student);
        } catch (Exception e) {
            log.error("Unexpected error in submitAssignment for assignmentId: {}, studentId: {}, error: {}", 
                    assignmentId, studentId, e.getMessage(), e);
            
            // Return a user-friendly error message
            throw new RuntimeException("Không thể xử lý bài nộp. Vui lòng kiểm tra lại thông tin và thử lại sau. Chi tiết: " + e.getMessage());
        }
    }
    
    /**
     * Handle multi-question assignment submission
     */
    private SubmissionResponse handleMultiQuestionSubmission(Long assignmentId, Long studentId, 
                                                           SubmissionRequest request, Assignment assignment) {
        // Convert legacy request to multi-question format
        MultiQuestionSubmissionRequest multiRequest = convertToMultiQuestionRequest(request, assignment);
        
        // Use multi-question submission service
        MultiQuestionSubmissionResponse multiResponse = multiQuestionSubmissionService
                .submitMultiQuestionAssignment(assignmentId, studentId, multiRequest);
        
        // Convert back to legacy response for API compatibility
        return multiQuestionSubmissionService.convertToLegacyResponse(multiResponse);
    }
    
    /**
     * Handle legacy single-question submission
     */
    private SubmissionResponse handleLegacySubmission(Long assignmentId, Long studentId, 
                                                    SubmissionRequest request, Assignment assignment, User student) {
        // Check if assignment is active
        if (!assignment.getIsActive()) {
            throw new RuntimeException("Assignment is not active");
        }

        // Check deadline
        if (LocalDateTime.now().isAfter(assignment.getEndTime())) {
            if (!assignment.getAllowLateSubmission()) {
                throw new RuntimeException("Assignment submission deadline has passed");
            }
        }

        // Find existing submission by assignment and student
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
                    studentId, assignmentId);
        }

        // Update submission with new data
        submission.setCode(request.getCode());
        submission.setProgrammingLanguage(request.getProgrammingLanguage());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        
        // Set multi-question metadata
        updateSubmissionMetadata(submission, request.getCode(), assignment);

        // Save submission first
        submission = submissionRepository.save(submission);
        log.info("Saved submission with ID: {}", submission.getId());

            // Auto-grade if enabled
            if (assignment.getAutoGrade()) {
                try {
                    log.info("Starting auto-grading for submission: {} (assignment: {})", submission.getId(), assignmentId);
                    
                    // Validate assignment has questions
                    if (assignment.getQuestions() == null || assignment.getQuestions().isEmpty()) {
                        log.warn("Assignment {} has no questions, skipping auto-grading", assignmentId);
                        submission.setStatus(SubmissionStatus.SUBMITTED);
                        submission.setFeedback("Bài tập chưa được cấu hình câu hỏi. Vui lòng liên hệ giáo viên.");
                        submission = submissionRepository.save(submission);
                    } else {
                        // Check if any question has reference implementation for enhanced grading
                        boolean hasReferenceImplementation = assignment.getQuestions().stream()
                            .anyMatch(q -> q.getReferenceImplementation() != null && 
                                          !q.getReferenceImplementation().trim().isEmpty());
                        
                        if (hasReferenceImplementation) {
                            log.info("Using enhanced grading (reference comparison) for submission: {}", submission.getId());
                            enhancedAutoGradingService.performEnhancedGrading(submission);
                        } else {
                            log.info("Using traditional grading (test case comparison) for submission: {}", submission.getId());
                            autoGradingService.gradeSubmission(submission);
                        }
                        
                        // Reload submission to get updated score
                        submission = submissionRepository.findById(submission.getId())
                                .orElse(submission);
                        
                        log.info("Auto-grading completed for submission: {}, final score: {}", 
                                submission.getId(), submission.getScore());
                    }
                } catch (Exception e) {
                    log.error("Auto-grading failed for submission: {}, error: {}", 
                            submission.getId(), e.getMessage(), e);
                    submission.setStatus(SubmissionStatus.COMPILATION_ERROR);
                    submission.setFeedback("Lỗi chấm điểm tự động: " + e.getMessage());
                    submission = submissionRepository.save(submission);
                }
            }

            return new SubmissionResponse(
                    submission.getId(),
                    assignment.getId(),
                    assignment.getTitle(), // assignment title
                    student.getFullName(), // student name  
                    studentId.toString(), // student ID as string
                    submission.getProgrammingLanguage(), // programming language
                    submission.getStatus(),
                    submission.getScore(),
                    submission.getExecutionTime(), // execution time
                    submission.getMemoryUsed(), // memory used  
                    submission.getFeedback(), // feedback
                    submission.getSubmissionTime(), // submission time
                    submission.getGradedTime(), // graded time
                    null, // test cases passed
                    null  // total test cases
            );
    }
    
    /**
     * Update submission metadata for multi-question support
     */
    private void updateSubmissionMetadata(Submission submission, String code, Assignment assignment) {
        // Check if this is multi-question code
        boolean isMultiQuestion = isMultiQuestionCode(code);
        
        if (isMultiQuestion) {
            List<String> questions = splitMultiQuestionCode(code);
            submission.setTotalQuestions(questions.size());
            submission.setCompletedQuestions(questions.size()); // All questions have code submitted
            submission.setHasProgrammingQuestions(true);
        } else {
            // Single question
            submission.setTotalQuestions(1);
            submission.setCompletedQuestions(1);
            submission.setHasProgrammingQuestions(true);
        }
    }
    
    /**
     * Check if code contains multiple questions
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
     * Split multi-question code into individual questions
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
     * Convert legacy SubmissionRequest to MultiQuestionSubmissionRequest
     */
    private MultiQuestionSubmissionRequest convertToMultiQuestionRequest(SubmissionRequest request, Assignment assignment) {
        List<QuestionSubmissionItem> questionItems = new ArrayList<>();
        
        // Check if code contains multi-question format
        if (isMultiQuestionCode(request.getCode())) {
            // Split the code by questions
            List<String> questionCodes = splitMultiQuestionCode(request.getCode());
            List<Question> questions = assignment.getQuestions();
            
            for (int i = 0; i < Math.min(questionCodes.size(), questions.size()); i++) {
                Question question = questions.get(i);
                String code = questionCodes.get(i).trim();
                
                if (!code.isEmpty()) {
                    QuestionSubmissionItem item = new QuestionSubmissionItem();
                    item.setQuestionId(question.getId());
                    item.setAnswer(code);
                    item.setLanguage(request.getProgrammingLanguage());
                    item.setQuestionType(question.getQuestionType().toString());
                    questionItems.add(item);
                }
            }
        } else {
            // Single question - map to first question in assignment
            if (!assignment.getQuestions().isEmpty()) {
                Question firstQuestion = assignment.getQuestions().get(0);
                QuestionSubmissionItem item = new QuestionSubmissionItem();
                item.setQuestionId(firstQuestion.getId());
                item.setAnswer(request.getCode());
                item.setLanguage(request.getProgrammingLanguage());
                item.setQuestionType(firstQuestion.getQuestionType().toString());
                questionItems.add(item);
            }
        }
        
        MultiQuestionSubmissionRequest multiRequest = new MultiQuestionSubmissionRequest();
        multiRequest.setAssignmentId(request.getAssignmentId());
        multiRequest.setQuestions(questionItems);
        multiRequest.setPrimaryLanguage(request.getProgrammingLanguage());
        multiRequest.setCombinedCode(request.getCode());
        
        return multiRequest;
    }
    
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getMySubmissions(Long studentId) {
        log.info("Getting submissions for student: {}", studentId);
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        
        List<Submission> submissions = submissionRepository.findByStudentOrderBySubmissionTimeDesc(student);
        
        return submissions.stream().map(submission -> {
            Assignment assignment = submission.getAssignment();
            return new SubmissionResponse(
                    submission.getId(),
                    assignment.getId(),
                    assignment.getTitle(),
                    student.getFullName(),
                    studentId.toString(),
                    submission.getProgrammingLanguage(),
                    submission.getStatus(),
                    submission.getScore(),
                    submission.getExecutionTime(),
                    submission.getMemoryUsed(),
                    submission.getFeedback(),
                    submission.getSubmissionTime(),
                    submission.getGradedTime(),
                    null, // test cases passed
                    null  // total test cases
            );
        }).collect(toList());
    }
    
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionDetails(Long submissionId, Long studentId) {
        log.info("Getting submission details for id: {}, student: {}", submissionId, studentId);
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
                
        // Verify that this submission belongs to the student
        if (!submission.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Access denied: Submission does not belong to this student");
        }
        
        Assignment assignment = submission.getAssignment();
        return new SubmissionResponse(
                submission.getId(),
                assignment.getId(),
                assignment.getTitle(),
                student.getFullName(),
                studentId.toString(),
                submission.getProgrammingLanguage(),
                submission.getStatus(),
                submission.getScore(),
                submission.getExecutionTime(),
                submission.getMemoryUsed(),
                submission.getFeedback(),
                submission.getSubmissionTime(),
                submission.getGradedTime(),
                null, // test cases passed
                null  // total test cases
        );
    }
}
