package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.dto.response.TestResultResponse;
import iuh.fit.cscore_be.entity.*;
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
 * Enhanced Automatic Grading Service
 * 
 * This service implements an improved scoring algorithm that:
 * 1. Creates complete test code for both student and reference implementations
 * 2. Executes both implementations with identical test cases
 * 3. Compares outputs to determine PASS/FAIL
 * 4. Provides detailed feedback on differences
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EnhancedAutoGradingService {
    
    private final HybridCodeExecutionService hybridCodeExecutionService;
    private final SubmissionRepository submissionRepository;
    private final TestResultRepository testResultRepository;
    
    @Value("${grading.time-limit:30}")
    private int defaultTimeLimit;
    
    @Value("${grading.memory-limit:256}")
    private int defaultMemoryLimit;

    /**
     * Enhanced auto-grading process that compares student implementation 
     * with teacher's reference implementation
     */
    @Async
    public CompletableFuture<Double> gradeSubmissionEnhanced(Long submissionId) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));
            
            Double score = performEnhancedGrading(submission);
            return CompletableFuture.completedFuture(score);
        } catch (Exception e) {
            log.error("Error in enhanced async grading for submission {}", submissionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Main enhanced grading logic
     */
    public Double performEnhancedGrading(Submission submission) {
        log.info("Starting enhanced auto-grading for submission {} (Student: {}, Assignment: {})", 
                submission.getId(), submission.getStudent().getStudentId(), submission.getAssignment().getTitle());
        
        try {
            // Update status to GRADING
            submission.setStatus(SubmissionStatus.GRADING);
            submissionRepository.save(submission);
            
            // Get all questions with their test cases
            List<Question> questions = submission.getAssignment().getQuestions();
            
            if (questions.isEmpty()) {
                return handleNoQuestions(submission);
            }
            
            // Process each question separately
            List<QuestionGradingResult> questionResults = new ArrayList<>();
            double totalScore = 0.0;
            double totalPossibleScore = 0.0;
            
            for (Question question : questions) {
                QuestionGradingResult result = gradeQuestionEnhanced(submission, question);
                questionResults.add(result);
                
                totalScore += result.getScore();
                totalPossibleScore += question.getPoints();
            }
            
            // Calculate final percentage score
            Double finalScore = totalPossibleScore > 0 ? (totalScore / totalPossibleScore) * 100 : 0.0;
            
            // Update submission with results
            updateSubmissionWithEnhancedResults(submission, questionResults, finalScore);
            
            log.info("Enhanced auto-grading completed for submission {}. Final score: {}", 
                    submission.getId(), finalScore);
            
            return finalScore;
            
        } catch (Exception e) {
            log.error("Error during enhanced auto-grading for submission {}", submission.getId(), e);
            handleGradingError(submission, e);
            return 0.0;
        }
    }

    /**
     * Grade a single question using the enhanced algorithm
     */
    private QuestionGradingResult gradeQuestionEnhanced(Submission submission, Question question) {
        log.info("Grading question {} with enhanced algorithm", question.getId());
        
        QuestionGradingResult result = new QuestionGradingResult();
        result.setQuestion(question);
        result.setTestResults(new ArrayList<>());
        
        try {
            // Check if question has reference implementation
            if (question.getReferenceImplementation() == null || question.getReferenceImplementation().trim().isEmpty()) {
                log.warn("No reference implementation found for question {}. Using fallback scoring.", question.getId());
                return gradeFallback(submission, question);
            }
            
            // Extract student function for this question
            String studentFunction = extractStudentFunction(submission.getCode(), question);
            if (studentFunction == null) {
                log.error("Could not extract student function for question {}", question.getId());
                result.setScore(0.0);
                result.setFeedback("Không thể tìm thấy implementation của function trong code của bạn.");
                return result;
            }
            
            // Grade using reference comparison
            return gradeWithReferenceComparison(studentFunction, question);
            
        } catch (Exception e) {
            log.error("Error grading question {} with enhanced algorithm", question.getId(), e);
            result.setScore(0.0);
            result.setFeedback("Lỗi trong quá trình chấm điểm: " + e.getMessage());
            return result;
        }
    }

    /**
     * Grade question by comparing student function with reference implementation
     */
    private QuestionGradingResult gradeWithReferenceComparison(String studentFunction, Question question) {
        QuestionGradingResult result = new QuestionGradingResult();
        result.setQuestion(question);
        result.setTestResults(new ArrayList<>());
        
        List<TestCase> testCases = question.getTestCases();
        if (testCases.isEmpty()) {
            result.setScore(0.0);
            result.setFeedback("Câu hỏi này không có test case để chấm điểm.");
            return result;
        }
        
        double totalWeight = testCases.stream().mapToDouble(TestCase::getWeight).sum();
        double earnedScore = 0.0;
        int passedTests = 0;
        
        StringBuilder feedback = new StringBuilder();
        feedback.append("Kết quả so sánh với đáp án reference:\n");
        
        for (TestCase testCase : testCases) {
            TestCaseComparisonResult comparisonResult = compareWithReference(
                studentFunction, question.getReferenceImplementation(), testCase, question);
            
            TestResultResponse testResult = new TestResultResponse();
            testResult.setTestCaseId(testCase.getId());
            testResult.setPassed(comparisonResult.isPassed());
            testResult.setInput(testCase.getInput());
            testResult.setExpectedOutput(comparisonResult.getReferenceOutput());
            testResult.setActualOutput(comparisonResult.getStudentOutput());
            testResult.setErrorMessage(comparisonResult.getErrorMessage());
            testResult.setExecutionTime(comparisonResult.getExecutionTime());
            
            result.getTestResults().add(testResult);
            
            if (comparisonResult.isPassed()) {
                earnedScore += testCase.getWeight();
                passedTests++;
                feedback.append(String.format("✓ Test case %d: PASS\n", testCase.getId()));
            } else {
                feedback.append(String.format("✗ Test case %d: FAIL\n", testCase.getId()));
                feedback.append(String.format("  Input: %s\n", testCase.getInput()));
                feedback.append(String.format("  Reference output: %s\n", comparisonResult.getReferenceOutput()));
                feedback.append(String.format("  Your output: %s\n", comparisonResult.getStudentOutput()));
                
                if (comparisonResult.getErrorMessage() != null) {
                    feedback.append(String.format("  Error: %s\n", comparisonResult.getErrorMessage()));
                }
                feedback.append("\n");
            }
        }
        
        // Calculate score as percentage of total weight
        double scorePercentage = totalWeight > 0 ? (earnedScore / totalWeight) : 0.0;
        result.setScore(scorePercentage * question.getPoints());
        
        feedback.append(String.format("\nTổng kết: %d/%d test cases đạt yêu cầu", passedTests, testCases.size()));
        feedback.append(String.format("\nĐiểm: %.2f/%.2f", result.getScore(), question.getPoints()));
        
        result.setFeedback(feedback.toString());
        
        log.info("Question {} graded: {}/{} test cases passed, score: {}", 
                question.getId(), passedTests, testCases.size(), result.getScore());
        
        return result;
    }

    /**
     * Compare student function with reference implementation for a single test case
     */
    private TestCaseComparisonResult compareWithReference(String studentFunction, String referenceFunction, 
                                                        TestCase testCase, Question question) {
        TestCaseComparisonResult result = new TestCaseComparisonResult();
        
        try {
            // Generate complete test programs
            String studentTestProgram = generateCompleteTestProgram(studentFunction, testCase, question);
            String referenceTestProgram = generateCompleteTestProgram(referenceFunction, testCase, question);
            
            // Execute both programs
            long startTime = System.currentTimeMillis();
            
            CodeExecutionResponse studentResponse = hybridCodeExecutionService.executeCodeWithInput(
                studentTestProgram, question.getProgrammingLanguage(), testCase.getInput());
            
            CodeExecutionResponse referenceResponse = hybridCodeExecutionService.executeCodeWithInput(
                referenceTestProgram, question.getProgrammingLanguage(), testCase.getInput());
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTime(executionTime);
            
            // Check execution results
            if (!referenceResponse.isSuccess()) {
                result.setPassed(false);
                result.setErrorMessage("Reference implementation failed to execute");
                return result;
            }
            
            if (!studentResponse.isSuccess()) {
                result.setPassed(false);
                result.setStudentOutput("COMPILATION/RUNTIME ERROR");
                result.setReferenceOutput(referenceResponse.getOutput());
                result.setErrorMessage(studentResponse.getError());
                return result;
            }
            
            // Compare outputs
            String studentOutput = normalizeOutput(studentResponse.getOutput());
            String referenceOutput = normalizeOutput(referenceResponse.getOutput());
            
            result.setStudentOutput(studentOutput);
            result.setReferenceOutput(referenceOutput);
            result.setPassed(studentOutput.equals(referenceOutput));
            
            if (!result.isPassed()) {
                result.setErrorMessage("Output mismatch with reference implementation");
            }
            
        } catch (Exception e) {
            log.error("Error comparing with reference for test case {}", testCase.getId(), e);
            result.setPassed(false);
            result.setErrorMessage("Execution error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Generate complete executable test program
     */
    private String generateCompleteTestProgram(String functionCode, TestCase testCase, Question question) {
        String language = question.getProgrammingLanguage();
        String functionName = question.getFunctionName();
        
        if ("java".equalsIgnoreCase(language)) {
            return generateJavaTestProgram(functionCode, testCase, question);
        } else if ("python".equalsIgnoreCase(language)) {
            return generatePythonTestProgram(functionCode, testCase, question);
        } else if ("cpp".equalsIgnoreCase(language) || "c".equalsIgnoreCase(language)) {
            return generateCppTestProgram(functionCode, testCase, question);
        } else {
            // Generic template
            return generateGenericTestProgram(functionCode, testCase, question);
        }
    }

    /**
     * Generate Java test program
     */
    private String generateJavaTestProgram(String functionCode, TestCase testCase, Question question) {
        StringBuilder program = new StringBuilder();
        
        program.append("import java.util.*;\n");
        program.append("import java.io.*;\n\n");
        
        program.append("public class Solution {\n");
        program.append("    ").append(functionCode).append("\n\n");
        
        program.append("    public static void main(String[] args) {\n");
        program.append("        Scanner sc = new Scanner(System.in);\n");
        program.append("        Solution solution = new Solution();\n");
        
        // Parse input and call function based on signature
        String functionCall = generateFunctionCall(testCase, question);
        program.append("        ").append(functionCall).append("\n");
        
        program.append("    }\n");
        program.append("}\n");
        
        return program.toString();
    }

    /**
     * Generate Python test program
     */
    private String generatePythonTestProgram(String functionCode, TestCase testCase, Question question) {
        StringBuilder program = new StringBuilder();
        
        program.append(functionCode).append("\n\n");
        
        // Parse input and call function
        String functionCall = generateFunctionCall(testCase, question);
        program.append(functionCall).append("\n");
        
        return program.toString();
    }

    /**
     * Generate C++ test program
     */
    private String generateCppTestProgram(String functionCode, TestCase testCase, Question question) {
        StringBuilder program = new StringBuilder();
        
        program.append("#include <iostream>\n");
        program.append("#include <cstring>\n");
        program.append("#include <vector>\n");
        program.append("#include <string>\n");
        program.append("using namespace std;\n\n");
        
        program.append(functionCode).append("\n\n");
        
        program.append("int main() {\n");
        
        // For countCharacter function, we need to set up the test properly
        String input = testCase.getInput().trim();
        String[] params = parseInputParameters(input, question.getFunctionSignature());
        
        if (params.length >= 2) {
            // Handle string parameter (remove quotes and create char array)
            String stringParam = params[0];
            if (stringParam.startsWith("\"") && stringParam.endsWith("\"")) {
                stringParam = stringParam.substring(1, stringParam.length() - 1);
            }
            
            // Handle character parameter (remove quotes)
            String charParam = params[1];
            if (charParam.startsWith("'") && charParam.endsWith("'")) {
                charParam = charParam.substring(1, charParam.length() - 1);
            }
            
            program.append("    char data[] = \"").append(stringParam).append("\";\n");
            program.append("    char key = '").append(charParam).append("';\n");
            program.append("    cout << ").append(question.getFunctionName()).append("(data, key) << endl;\n");
        } else {
            // Fallback to original method
            String functionCall = generateFunctionCall(testCase, question);
            program.append("    ").append(functionCall).append("\n");
        }
        
        program.append("    return 0;\n");
        program.append("}\n");
        
        return program.toString();
    }

    /**
     * Generate generic test program (fallback)
     */
    private String generateGenericTestProgram(String functionCode, TestCase testCase, Question question) {
        if (question.getTestTemplate() != null && !question.getTestTemplate().trim().isEmpty()) {
            return question.getTestTemplate()
                .replace("{{FUNCTION_CODE}}", functionCode)
                .replace("{{INPUT}}", testCase.getInput())
                .replace("{{FUNCTION_NAME}}", question.getFunctionName());
        }
        
        // Default fallback
        return functionCode + "\n" + generateFunctionCall(testCase, question);
    }

    /**
     * Generate function call based on test case input and question signature
     */
    private String generateFunctionCall(TestCase testCase, Question question) {
        String functionName = question.getFunctionName();
        String input = testCase.getInput();
        String language = question.getProgrammingLanguage();
        
        // Parse input parameters intelligently
        String[] params = parseInputParameters(input, question.getFunctionSignature());
        
        StringBuilder call = new StringBuilder();
        
        if ("java".equalsIgnoreCase(language)) {
            call.append("System.out.println(solution.").append(functionName).append("(");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) call.append(", ");
                call.append(params[i]);
            }
            call.append("));");
        } else if ("python".equalsIgnoreCase(language)) {
            call.append("print(").append(functionName).append("(");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) call.append(", ");
                call.append(params[i]);
            }
            call.append("))");
        } else if ("cpp".equalsIgnoreCase(language) || "c".equalsIgnoreCase(language)) {
            call.append("cout << ").append(functionName).append("(");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) call.append(", ");
                call.append(params[i]);
            }
            call.append(") << endl;");
        }
        
        return call.toString();
    }

    /**
     * Parse input parameters based on function signature and input format
     */
    private String[] parseInputParameters(String input, String functionSignature) {
        // Handle special case for string and character parameters
        // Input format: "Hello l" should become ["Hello", 'l']
        // Input format: "HelloHelloHello o" should become ["HelloHelloHello", 'o'] 
        // Input format: "\"\" o" should become ["", 'o']
        
        if (input == null || input.trim().isEmpty()) {
            return new String[]{"\"\""};
        }
        
        input = input.trim();
        
        // Check if this looks like a string + character pattern
        if (input.matches(".*\\s+\\S$")) {
            // Find the last space to split string and character
            int lastSpaceIndex = input.lastIndexOf(' ');
            if (lastSpaceIndex > 0) {
                String stringPart = input.substring(0, lastSpaceIndex).trim();
                String charPart = input.substring(lastSpaceIndex + 1).trim();
                
                // Handle empty string case
                if (stringPart.equals("\"\"")) {
                    return new String[]{"\"\"", "'" + charPart + "'"};
                } else {
                    // Add quotes if not already present
                    if (!stringPart.startsWith("\"")) {
                        stringPart = "\"" + stringPart + "\"";
                    }
                    return new String[]{stringPart, "'" + charPart + "'"};
                }
            }
        }
        
        // Fallback: try comma-separated parsing
        String[] params = input.split(",");
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }
        return params;
    }

    /**
     * Extract student function from submission code for specific question
     */
    private String extractStudentFunction(String submissionCode, Question question) {
        // For multi-question submissions, try to extract the specific function
        String functionName = question.getFunctionName();
        
        if (functionName != null && !functionName.trim().isEmpty()) {
            return extractFunctionByName(submissionCode, functionName, question.getProgrammingLanguage());
        }
        
        // If no function name specified, return the whole code
        return submissionCode;
    }

    /**
     * Extract function by name from code
     */
    private String extractFunctionByName(String code, String functionName, String language) {
        if ("java".equalsIgnoreCase(language)) {
            return extractJavaFunction(code, functionName);
        } else if ("python".equalsIgnoreCase(language)) {
            return extractPythonFunction(code, functionName);
        } else if ("cpp".equalsIgnoreCase(language)) {
            return extractCppFunction(code, functionName);
        }
        
        // Fallback: return whole code
        return code;
    }

    private String extractJavaFunction(String code, String functionName) {
        // Simple regex to find Java method
        String pattern = "(?s)(public|private|protected)?\\s*(static)?\\s*\\w+\\s+" + functionName + "\\s*\\([^}]*\\}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(code);
        
        if (m.find()) {
            return m.group(0);
        }
        
        return code;
    }

    private String extractPythonFunction(String code, String functionName) {
        // Simple approach: find def functionName and extract until next def or end
        String[] lines = code.split("\n");
        StringBuilder function = new StringBuilder();
        boolean inFunction = false;
        int functionIndent = -1;
        
        for (String line : lines) {
            if (line.trim().startsWith("def " + functionName + "(")) {
                inFunction = true;
                functionIndent = getIndentLevel(line);
                function.append(line).append("\n");
            } else if (inFunction) {
                int currentIndent = getIndentLevel(line);
                if (line.trim().isEmpty() || currentIndent > functionIndent) {
                    function.append(line).append("\n");
                } else {
                    break;
                }
            }
        }
        
        return function.length() > 0 ? function.toString() : code;
    }

    private String extractCppFunction(String code, String functionName) {
        // Simple approach for C++ function extraction
        String pattern = "(?s)\\w+\\s+" + functionName + "\\s*\\([^{]*\\{[^}]*\\}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(code);
        
        if (m.find()) {
            return m.group(0);
        }
        
        return code;
    }

    private int getIndentLevel(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') indent++;
            else if (c == '\t') indent += 4;
            else break;
        }
        return indent;
    }

    /**
     * Normalize output for comparison
     */
    private String normalizeOutput(String output) {
        if (output == null) return "";
        return output.trim().replaceAll("\\r\\n", "\n");
    }

    /**
     * Fallback to original grading method if reference implementation is not available
     */
    private QuestionGradingResult gradeFallback(Submission submission, Question question) {
        log.info("Using fallback grading for question {}", question.getId());
        
        QuestionGradingResult result = new QuestionGradingResult();
        result.setQuestion(question);
        result.setTestResults(new ArrayList<>());
        
        List<TestCase> testCases = question.getTestCases();
        if (testCases.isEmpty()) {
            result.setScore(0.0);
            result.setFeedback("No test cases available for grading.");
            return result;
        }
        
        // Use original test case comparison method
        CodeExecutionResponse executionResult = hybridCodeExecutionService.executeCodeWithTestCases(
            submission.getCode(), question.getProgrammingLanguage(), testCases, submission);
        
        if (!executionResult.isSuccess()) {
            result.setScore(0.0);
            result.setFeedback("Code compilation or execution failed: " + executionResult.getError());
            return result;
        }
        
        int passedTests = executionResult.getPassedTests();
        int totalTests = executionResult.getTotalTests();
        
        double scorePercentage = totalTests > 0 ? (double) passedTests / totalTests : 0.0;
        result.setScore(scorePercentage * question.getPoints());
        result.setFeedback(String.format("Fallback grading: %d/%d test cases passed", passedTests, totalTests));
        
        return result;
    }

    private Double handleNoQuestions(Submission submission) {
        log.warn("No questions found for assignment {}", submission.getAssignment().getId());
        submission.setStatus(SubmissionStatus.NO_TESTS);
        submission.setScore(0.0);
        submission.setFeedback("Assignment has no questions to grade");
        submission.setGradedTime(LocalDateTime.now());
        submissionRepository.save(submission);
        return 0.0;
    }

    private void handleGradingError(Submission submission, Exception e) {
        submission.setStatus(SubmissionStatus.ERROR);
        submission.setFeedback("Enhanced grading error: " + e.getMessage());
        submission.setGradedTime(LocalDateTime.now());
        submissionRepository.save(submission);
    }

    private void updateSubmissionWithEnhancedResults(Submission submission, 
                                                   List<QuestionGradingResult> questionResults, 
                                                   Double finalScore) {
        // Clear existing test results
        testResultRepository.deleteBySubmissionId(submission.getId());
        
        // Create new test results
        for (QuestionGradingResult questionResult : questionResults) {
            for (TestResultResponse testResultResponse : questionResult.getTestResults()) {
                TestResult testResult = new TestResult();
                testResult.setSubmission(submission);
                testResult.setTestCase(getTestCaseById(testResultResponse.getTestCaseId()));
                testResult.setPassed(testResultResponse.isPassed());
                testResult.setActualOutput(testResultResponse.getActualOutput());
                testResult.setExecutionTime(testResultResponse.getExecutionTime());
                testResult.setMemoryUsed(0L); // Set default or from response if available
                testResult.setErrorMessage(testResultResponse.getErrorMessage());
                
                testResultRepository.save(testResult);
            }
        }
        
        // Update submission
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setScore(finalScore);
        submission.setGradedTime(LocalDateTime.now());
        
        // Create combined feedback
        StringBuilder combinedFeedback = new StringBuilder();
        combinedFeedback.append("Enhanced Auto-Grading Results:\n\n");
        
        for (QuestionGradingResult result : questionResults) {
            combinedFeedback.append("Question ").append(result.getQuestion().getTitle()).append(":\n");
            combinedFeedback.append(result.getFeedback()).append("\n\n");
        }
        
        submission.setFeedback(combinedFeedback.toString());
        submissionRepository.save(submission);
    }

    private TestCase getTestCaseById(Long testCaseId) {
        // This is a simple implementation - in practice you might want to cache these or pass them differently
        return new TestCase(); // Placeholder - implement proper lookup
    }

    // Inner classes for data structure
    private static class QuestionGradingResult {
        private Question question;
        private Double score;
        private String feedback;
        private List<TestResultResponse> testResults;

        // Getters and setters
        public Question getQuestion() { return question; }
        public void setQuestion(Question question) { this.question = question; }
        
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        
        public List<TestResultResponse> getTestResults() { return testResults; }
        public void setTestResults(List<TestResultResponse> testResults) { this.testResults = testResults; }
    }

    private static class TestCaseComparisonResult {
        private boolean passed;
        private String studentOutput;
        private String referenceOutput;
        private String errorMessage;
        private Long executionTime;

        // Getters and setters
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public String getStudentOutput() { return studentOutput; }
        public void setStudentOutput(String studentOutput) { this.studentOutput = studentOutput; }
        
        public String getReferenceOutput() { return referenceOutput; }
        public void setReferenceOutput(String referenceOutput) { this.referenceOutput = referenceOutput; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Long getExecutionTime() { return executionTime; }
        public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
    }
}