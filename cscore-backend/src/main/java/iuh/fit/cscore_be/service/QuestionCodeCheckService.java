package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.dto.response.TestResultResponse;
import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QuestionCodeCheckService {
    
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionSubmissionRepository questionSubmissionRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final HybridCodeExecutionService hybridCodeExecutionService;
    
    /**
     * Check student code against test cases for a specific question
     * This provides real-time feedback without final submission
     */
    public CodeExecutionResponse checkQuestionCode(Long questionId, String code, String language, String studentId) {
        return checkQuestionCode(questionId, code, language, studentId, null);
    }
    
    /**
     * Check student code with optional custom input
     * If input is provided, execute code with input instead of test cases
     */
    public CodeExecutionResponse checkQuestionCode(Long questionId, String code, String language, String studentId, String customInput) {
        // Get question and validate
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        // Try to find user by username first, then by ID if it fails
        User student = findUserByUsernameOrId(studentId);
        
        // Validate code and language compatibility
        String languageMismatchError = validateCodeLanguageCompatibility(code, language);
        if (languageMismatchError != null) {
            CodeExecutionResponse errorResponse = new CodeExecutionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError(languageMismatchError);
            errorResponse.setMessage("Vui lòng kiểm tra lại ngôn ngữ lập trình bạn đã chọn");
            return errorResponse;
        }
        
        CodeExecutionResponse result;
        
        if (customInput != null && !customInput.trim().isEmpty()) {
            // Execute code with custom input (for "Run" functionality)
            log.info("Running code for question {} with custom input", questionId);
            result = hybridCodeExecutionService.executeCodeWithInput(code, language, customInput);
            result.setMessage("Code được chạy với input tùy chỉnh. Kết quả chỉ mang tính tham khảo.");
        } else {
            // Execute code with test cases (for "Test" functionality)
            List<TestCase> testCases = question.getTestCases();
            if (testCases.isEmpty()) {
                CodeExecutionResponse response = new CodeExecutionResponse();
                response.setSuccess(false);
                response.setError("Câu hỏi này không có test case để kiểm tra");
                response.setMessage("Không thể chấm điểm vì không có bộ test case từ giảng viên");
                return response;
            }
            
            log.info("Testing code for question {} with {} test cases from teacher", questionId, testCases.size());
            result = hybridCodeExecutionService.executeCodeWithTestCases(code, language, testCases, null, question);
            
            // Add detailed message about grading process
            if (result.isSuccess()) {
                int passedTests = result.getPassedTests();
                int totalTests = result.getTotalTests();
                result.setMessage(String.format(
                    "Chấm điểm tự động: Code của bạn được chạy với %d test case(s) từ giảng viên. " +
                    "Kết quả: %d/%d test case(s) pass. " +
                    "Điểm số dựa trên số lượng test case pass và trọng số của từng test case.",
                    totalTests, passedTests, totalTests
                ));
            } else {
                result.setMessage("Chấm điểm tự động: Code của bạn có lỗi biên dịch hoặc runtime. Vui lòng kiểm tra lại code.");
            }
            
            // Save temporary result only for test runs (not custom input runs)
            saveTemporaryQuestionSubmission(question, student, code, language, result);
        }
        
        return result;
    }
    
    /**
     * Submit final answer for a question (this locks the score)
     */
    public CodeExecutionResponse submitQuestionAnswer(Long questionId, String code, String language, String studentId) {
        // First check the code
        CodeExecutionResponse result = checkQuestionCode(questionId, code, language, studentId);
        
        if (result.isSuccess()) {
            // Mark as final submission
            markQuestionSubmissionAsFinal(questionId, studentId, result);
        }
        
        return result;
    }
    
    /**
     * Get current best score for a question
     */
    public Double getQuestionScore(Long questionId, String studentId) {
        User student = findUserByUsernameOrId(studentId);
        
        Optional<QuestionSubmission> submission = questionSubmissionRepository
                .findByQuestionIdAndStudentId(questionId, student.getId());
        
        return submission.map(QuestionSubmission::getScore).orElse(0.0);
    }
    
    /**
     * Helper method to find user by username or ID
     * This handles both cases where studentId could be username or ID
     */
    private User findUserByUsernameOrId(String studentId) {
        // First try to find by username
        Optional<User> userByUsername = userRepository.findByUsername(studentId);
        if (userByUsername.isPresent()) {
            return userByUsername.get();
        }
        
        // If not found by username, try to parse as ID and find by ID
        try {
            Long userId = Long.parseLong(studentId);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
        } catch (NumberFormatException e) {
            // If it's not a valid number, it was supposed to be a username but not found
            throw new RuntimeException("Student not found with username: " + studentId);
        }
    }
    
    /**
     * Save temporary question submission (for checking, not final)
     */
    private void saveTemporaryQuestionSubmission(Question question, User student, String code, 
                                               String language, CodeExecutionResponse result) {
        
        // Find existing submission or create new one
        QuestionSubmission questionSubmission = questionSubmissionRepository
                .findByQuestionIdAndStudentId(question.getId(), student.getId())
                .orElse(new QuestionSubmission());
        
        // Calculate score from test results
        double score = calculateScoreFromResults(question.getTestCases(), result.getTestResults());
        
        // Only update if this is better than previous score (for temporary checks)
        if (questionSubmission.getId() == null || !questionSubmission.getIsFinalSubmission() || score > questionSubmission.getScore()) {
            
            // Create or find temporary submission if not exists
            if (questionSubmission.getSubmission() == null) {
                // Find or create temporary submission for this assignment and student
                Assignment assignment = question.getAssignment();
                Optional<Submission> existingSubmission = submissionRepository
                    .findByAssignmentAndStudent(assignment, student);
                
                Submission tempSubmission;
                if (existingSubmission.isPresent()) {
                    tempSubmission = existingSubmission.get();
                } else {
                    tempSubmission = new Submission();
                    tempSubmission.setAssignment(assignment);
                    tempSubmission.setStudent(student);
                    tempSubmission.setStatus(iuh.fit.cscore_be.enums.SubmissionStatus.SUBMITTED);
                    tempSubmission.setSubmissionTime(LocalDateTime.now());
                    tempSubmission = submissionRepository.save(tempSubmission);
                }
                questionSubmission.setSubmission(tempSubmission);
            }
            
            questionSubmission.setQuestion(question);
            questionSubmission.setStudent(student);
            questionSubmission.setSubmittedAnswer(code);
            questionSubmission.setCode(code);
            questionSubmission.setProgrammingLanguage(language);
            questionSubmission.setScore(score);
            questionSubmission.setIsCorrect(result.getPassedTests() == result.getTotalTests());
            questionSubmission.setSubmissionTime(LocalDateTime.now());
            questionSubmission.setIsFinalSubmission(false); // Mark as temporary
            
            questionSubmissionRepository.save(questionSubmission);
            log.info("Saved temporary submission for question {} with score {}", question.getId(), score);
        }
    }
    
    /**
     * Mark question submission as final
     */
    private void markQuestionSubmissionAsFinal(Long questionId, String studentId, CodeExecutionResponse result) {
        User student = findUserByUsernameOrId(studentId);
        
        Optional<QuestionSubmission> optionalSubmission = questionSubmissionRepository
                .findByQuestionIdAndStudentId(questionId, student.getId());
        
        if (optionalSubmission.isPresent()) {
            QuestionSubmission submission = optionalSubmission.get();
            submission.setIsFinalSubmission(true);
            submission.setSubmissionTime(LocalDateTime.now());
            
            questionSubmissionRepository.save(submission);
            log.info("Marked submission for question {} as final with score {}", questionId, submission.getScore());
        }
    }
    
    /**
     * Calculate score based on test results and weights
     */
    private double calculateScoreFromResults(List<TestCase> testCases, List<TestResultResponse> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return 0.0;
        }
        
        double totalScore = 0.0;
        
        for (TestResultResponse result : testResults) {
            if (result.isPassed()) {
                TestCase testCase = testCases.stream()
                        .filter(tc -> tc.getId().equals(result.getTestCaseId()))
                        .findFirst()
                        .orElse(null);
                
                if (testCase != null) {
                    totalScore += testCase.getWeight();
                }
            }
        }
        
        return totalScore;
    }
    
    /**
     * Validate if the submitted code matches the selected programming language
     * Returns error message if mismatch is detected, null if compatible
     */
    private String validateCodeLanguageCompatibility(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            return "Code không được để trống";
        }
        
        String trimmedCode = code.trim();
        language = language.toUpperCase();
        
        // Python language patterns
        if (language.equals("PYTHON") || language.equals("PYTHON3")) {
            if (trimmedCode.contains("def ") || trimmedCode.contains("import ") || 
                trimmedCode.contains("print(") || trimmedCode.contains("for ") && trimmedCode.contains(":")) {
                return null; // Valid Python code
            }
            // Check for C/Java patterns in Python submission
            if (trimmedCode.contains("int main(") || trimmedCode.contains("#include") ||
                trimmedCode.contains("System.out.print") || trimmedCode.contains("public class")) {
                return "Mã nguồn có vẻ không phải Python. Bạn có chắc đã chọn đúng ngôn ngữ?";
            }
        }
        
        // C language patterns
        else if (language.equals("C")) {
            if (trimmedCode.contains("#include") || trimmedCode.contains("int main(") ||
                trimmedCode.contains("printf(") || trimmedCode.contains("scanf(")) {
                return null; // Valid C code
            }
            // Check for Python/Java patterns in C submission
            if (trimmedCode.contains("def ") || trimmedCode.contains("print(") ||
                trimmedCode.contains("System.out.print") || trimmedCode.contains("public class")) {
                return "Mã nguồn có vẻ không phải C. Bạn có chắc đã chọn đúng ngôn ngữ?";
            }
        }
        
        // Java language patterns
        else if (language.equals("JAVA")) {
            if (trimmedCode.contains("public class") || trimmedCode.contains("System.out.print") ||
                trimmedCode.contains("public static void main")) {
                return null; // Valid Java code
            }
            // Check for Python/C patterns in Java submission
            if (trimmedCode.contains("def ") || trimmedCode.contains("print(") && !trimmedCode.contains("System.out.print") ||
                trimmedCode.contains("#include") || trimmedCode.contains("int main(")) {
                return "Mã nguồn có vẻ không phải Java. Bạn có chắc đã chọn đúng ngôn ngữ?";
            }
        }
        
        // JavaScript language patterns
        else if (language.equals("JAVASCRIPT") || language.equals("JS")) {
            if (trimmedCode.contains("console.log") || trimmedCode.contains("function ") ||
                trimmedCode.contains("var ") || trimmedCode.contains("let ") || trimmedCode.contains("const ")) {
                return null; // Valid JavaScript code
            }
            // Check for other language patterns in JavaScript submission
            if (trimmedCode.contains("def ") || trimmedCode.contains("print(") && !trimmedCode.contains("console.log") ||
                trimmedCode.contains("#include") || trimmedCode.contains("System.out.print")) {
                return "Mã nguồn có vẻ không phải JavaScript. Bạn có chắc đã chọn đúng ngôn ngữ?";
            }
        }
        
        return null; // No obvious mismatch detected
    }
}