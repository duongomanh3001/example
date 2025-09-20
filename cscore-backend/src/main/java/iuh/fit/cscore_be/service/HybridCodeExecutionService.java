package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridCodeExecutionService {

    private final JobeExecutionService jobeExecutionService;
    private final CodeExecutionService localCodeExecutionService;
    
    @Value("${execution.strategy:hybrid}")
    private String executionStrategy; // hybrid, jobe, local
    
    @Value("${jobe.server.enabled:false}")
    private boolean jobeEnabled;

    /**
     * Execute code using the configured strategy
     */
    public CodeExecutionResponse executeCode(String code, String language) {
        ExecutionStrategy strategy = determineExecutionStrategy();
        
        log.info("Executing code using strategy: {} for language: {}", strategy, language);
        
        switch (strategy) {
            case JOBE:
                return executeWithJobe(code, language);
            case LOCAL:
                return executeWithLocal(code, language);
            case HYBRID:
            default:
                return executeWithHybrid(code, language);
        }
    }

    /**
     * Execute code with input using the configured strategy
     */
    public CodeExecutionResponse executeCodeWithInput(String code, String language, String input) {
        ExecutionStrategy strategy = determineExecutionStrategy();
        
        log.info("Executing code with input using strategy: {} for language: {}", strategy, language);
        
        switch (strategy) {
            case JOBE:
                return executeWithInputJobe(code, language, input);
            case LOCAL:
                return executeWithInputLocal(code, language, input);
            case HYBRID:
            default:
                return executeWithInputHybrid(code, language, input);
        }
    }

    /**
     * Execute code with test cases using the configured strategy
     */
    public CodeExecutionResponse executeCodeWithTestCases(String code, String language, List<TestCase> testCases, Submission submission) {
        return executeCodeWithTestCases(code, language, testCases, submission, null);
    }

    /**
     * Execute code with test cases using the configured strategy with question metadata for code wrapping
     */
    public CodeExecutionResponse executeCodeWithTestCases(String code, String language, List<TestCase> testCases, Submission submission, Question question) {
        ExecutionStrategy strategy = determineExecutionStrategy();
        
        log.info("Executing code with {} test cases using strategy: {} for language: {}", 
                testCases.size(), strategy, language);
        
        CodeExecutionResponse response;
        switch (strategy) {
            case JOBE:
                response = executeWithTestCasesJobe(code, language, testCases, submission, question);
                break;
            case LOCAL:
                response = executeWithTestCasesLocal(code, language, testCases, submission, question);
                break;
            case HYBRID:
            default:
                response = executeWithTestCasesHybrid(code, language, testCases, submission, question);
                break;
        }
        
        // Add detailed message about auto-grading process if not already set
        if (response != null && (response.getMessage() == null || response.getMessage().isEmpty())) {
            if (response.isSuccess()) {
                int passedTests = response.getPassedTests();
                int totalTests = response.getTotalTests();
                response.setMessage(String.format(
                    "Chấm điểm tự động hoàn thành: Code của bạn được thực thi với %d test case(s) từ giảng viên. " +
                    "Kết quả: %d/%d test case(s) đạt yêu cầu. " +
                    "Điểm số được tính dựa trên output thực tế so với expected output và trọng số của từng test case. " +
                    "Không có so sánh trực tiếp code với đáp án của giảng viên.",
                    totalTests, passedTests, totalTests
                ));
            } else {
                response.setMessage("Chấm điểm tự động: Code có lỗi trong quá trình biên dịch hoặc thực thi. " +
                    "Hệ thống không thể chạy test cases. Vui lòng kiểm tra lại code và thử lại.");
            }
        }
        
        return response;
    }

    /**
     * Determine execution strategy based on configuration and availability
     */
    private ExecutionStrategy determineExecutionStrategy() {
        switch (executionStrategy.toLowerCase()) {
            case "jobe":
                return jobeEnabled && jobeExecutionService.isJobeServerAvailable() ? 
                       ExecutionStrategy.JOBE : ExecutionStrategy.LOCAL;
            case "local":
                return ExecutionStrategy.LOCAL;
            case "hybrid":
            default:
                return jobeEnabled && jobeExecutionService.isJobeServerAvailable() ? 
                       ExecutionStrategy.JOBE : ExecutionStrategy.LOCAL;
        }
    }

    // JOBE execution methods
    private CodeExecutionResponse executeWithJobe(String code, String language) {
        try {
            // Preprocess C code to add necessary includes
            String processedCode = code;
            if ("c".equalsIgnoreCase(language)) {
                processedCode = preprocessCCode(code);
                log.debug("C code preprocessed for Jobe execution");
            }
            
            return jobeExecutionService.executeCode(processedCode, language);
        } catch (Exception e) {
            log.warn("Jobe execution failed, falling back to local: {}", e.getMessage());
            return localCodeExecutionService.executeCode(code, language);
        }
    }

    private CodeExecutionResponse executeWithInputJobe(String code, String language, String input) {
        try {
            // Preprocess C code to add necessary includes
            String processedCode = code;
            if ("c".equalsIgnoreCase(language)) {
                processedCode = preprocessCCode(code);
                log.debug("C code preprocessed. Original length: {}, Processed length: {}", 
                         code.length(), processedCode.length());
            }
            
            return jobeExecutionService.executeCodeWithInput(processedCode, language, input);
        } catch (Exception e) {
            log.warn("Jobe execution with input failed, falling back to local: {}", e.getMessage());
            return localCodeExecutionService.executeCodeWithInput(code, language, input);
        }
    }

    private CodeExecutionResponse executeWithTestCasesJobe(String code, String language, List<TestCase> testCases, Submission submission) {
        return executeWithTestCasesJobe(code, language, testCases, submission, null);
    }

    private CodeExecutionResponse executeWithTestCasesJobe(String code, String language, List<TestCase> testCases, Submission submission, Question question) {
        // If question parameter is provided, we need code wrapping capabilities which JOBE doesn't support
        // Always fall back to local execution for programming assignments with function-only code
        if (question != null) {
            log.info("Question parameter provided, falling back to local execution for code wrapping support");
            return localCodeExecutionService.executeCodeWithTestCases(code, language, testCases, submission, question);
        }
        
        try {
            // For simple code execution without wrapping needs
            return jobeExecutionService.executeCodeWithTestCases(code, language, testCases, submission);
        } catch (Exception e) {
            log.warn("Jobe execution with test cases failed, falling back to local: {}", e.getMessage());
            return localCodeExecutionService.executeCodeWithTestCases(code, language, testCases, submission, question);
        }
    }

    // LOCAL execution methods
    private CodeExecutionResponse executeWithLocal(String code, String language) {
        return localCodeExecutionService.executeCode(code, language);
    }

    private CodeExecutionResponse executeWithInputLocal(String code, String language, String input) {
        return localCodeExecutionService.executeCodeWithInput(code, language, input);
    }

    private CodeExecutionResponse executeWithTestCasesLocal(String code, String language, List<TestCase> testCases, Submission submission) {
        return executeWithTestCasesLocal(code, language, testCases, submission, null);
    }

    private CodeExecutionResponse executeWithTestCasesLocal(String code, String language, List<TestCase> testCases, Submission submission, Question question) {
        return localCodeExecutionService.executeCodeWithTestCases(code, language, testCases, submission, question);
    }

    // HYBRID execution methods (with fallback)
    private CodeExecutionResponse executeWithHybrid(String code, String language) {
        if (jobeEnabled && jobeExecutionService.isJobeServerAvailable()) {
            try {
                // Preprocess C code to add necessary includes
                String processedCode = code;
                if ("c".equalsIgnoreCase(language)) {
                    processedCode = preprocessCCode(code);
                    log.debug("C code preprocessed for hybrid Jobe execution");
                }
                
                CodeExecutionResponse result = jobeExecutionService.executeCode(processedCode, language);
                if (result.isSuccess()) {
                    log.debug("Hybrid execution: Jobe succeeded for language: {}", language);
                    return result;
                }
            } catch (Exception e) {
                log.warn("Hybrid execution: Jobe failed for {}, falling back to local: {}", language, e.getMessage());
            }
        }
        
        log.debug("Hybrid execution: Using local execution for language: {}", language);
        return localCodeExecutionService.executeCode(code, language);
    }

    private CodeExecutionResponse executeWithInputHybrid(String code, String language, String input) {
        if (jobeEnabled && jobeExecutionService.isJobeServerAvailable()) {
            try {
                // Preprocess C code to add necessary includes
                String processedCode = code;
                if ("c".equalsIgnoreCase(language)) {
                    processedCode = preprocessCCode(code);
                    log.debug("C code preprocessed for hybrid Jobe execution with input");
                }
                
                CodeExecutionResponse result = jobeExecutionService.executeCodeWithInput(processedCode, language, input);
                if (result.isSuccess()) {
                    return result;
                }
                log.warn("Jobe execution with input unsuccessful, falling back to local");
            } catch (Exception e) {
                log.warn("Jobe execution with input failed, falling back to local: {}", e.getMessage());
            }
        }
        
        return localCodeExecutionService.executeCodeWithInput(code, language, input);
    }

    private CodeExecutionResponse executeWithTestCasesHybrid(String code, String language, List<TestCase> testCases, Submission submission) {
        return executeWithTestCasesHybrid(code, language, testCases, submission, null);
    }

    private CodeExecutionResponse executeWithTestCasesHybrid(String code, String language, List<TestCase> testCases, Submission submission, Question question) {
        if (jobeEnabled && jobeExecutionService.isJobeServerAvailable()) {
            try {
                // For now, use existing service without question parameter
                // TODO: Update JobeExecutionService to support question parameter  
                CodeExecutionResponse result = jobeExecutionService.executeCodeWithTestCases(code, language, testCases, submission);
                if (result.isSuccess()) {
                    return result;
                }
                log.warn("Jobe execution with test cases unsuccessful, falling back to local");
            } catch (Exception e) {
                log.warn("Jobe execution with test cases failed, falling back to local: {}", e.getMessage());
            }
        }
        
        return localCodeExecutionService.executeCodeWithTestCases(code, language, testCases, submission, question);
    }

    /**
     * Get execution status and strategy info
     */
    public ExecutionInfo getExecutionInfo() {
        ExecutionInfo info = new ExecutionInfo();
        info.setCurrentStrategy(determineExecutionStrategy());
        info.setJobeEnabled(jobeEnabled);
        info.setJobeAvailable(jobeEnabled && jobeExecutionService.isJobeServerAvailable());
        info.setConfiguredStrategy(executionStrategy);
        
        if (info.isJobeAvailable()) {
            info.setSupportedLanguages(jobeExecutionService.getSupportedLanguages());
        }
        
        return info;
    }

    /**
     * Preprocess C code to automatically add necessary includes
     * This is particularly important for teacher code validation
     */
    private String preprocessCCode(String code) {
        if (code != null && !code.trim().isEmpty()) {
            // Check if code uses math functions
            boolean needsMathLib = containsMathFunctions(code);
            boolean hasStdioInclude = code.contains("#include <stdio.h>");
            boolean hasMathInclude = code.contains("#include <math.h>");
            
            StringBuilder preprocessed = new StringBuilder();
            
            // Add standard includes at the beginning if not present
            if (!hasStdioInclude) {
                preprocessed.append("#include <stdio.h>\n");
            }
            
            // Add math.h if needed and not present
            if (needsMathLib && !hasMathInclude) {
                preprocessed.append("#include <math.h>\n");
            }
            
            // Add original code
            preprocessed.append(code);
            
            return preprocessed.toString();
        }
        
        return code;
    }
    
    /**
     * Check if C code contains math functions that require math.h
     */
    private boolean containsMathFunctions(String code) {
        String[] mathFunctions = {
            "sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
            "exp", "log", "log10", "pow", "ceil", "floor", "fabs", "abs",
            "sinh", "cosh", "tanh", "fmod", "ldexp", "frexp", "modf"
        };
        
        for (String func : mathFunctions) {
            if (code.matches(".*\\b" + func + "\\s*\\(.*")) {
                return true;
            }
        }
        
        return false;
    }

    // Enums and DTOs
    public enum ExecutionStrategy {
        JOBE, LOCAL, HYBRID
    }

    public static class ExecutionInfo {
        private ExecutionStrategy currentStrategy;
        private boolean jobeEnabled;
        private boolean jobeAvailable;
        private String configuredStrategy;
        private List<String> supportedLanguages;

        // Getters and setters
        public ExecutionStrategy getCurrentStrategy() { return currentStrategy; }
        public void setCurrentStrategy(ExecutionStrategy currentStrategy) { this.currentStrategy = currentStrategy; }
        
        public boolean isJobeEnabled() { return jobeEnabled; }
        public void setJobeEnabled(boolean jobeEnabled) { this.jobeEnabled = jobeEnabled; }
        
        public boolean isJobeAvailable() { return jobeAvailable; }
        public void setJobeAvailable(boolean jobeAvailable) { this.jobeAvailable = jobeAvailable; }
        
        public String getConfiguredStrategy() { return configuredStrategy; }
        public void setConfiguredStrategy(String configuredStrategy) { this.configuredStrategy = configuredStrategy; }
        
        public List<String> getSupportedLanguages() { return supportedLanguages; }
        public void setSupportedLanguages(List<String> supportedLanguages) { this.supportedLanguages = supportedLanguages; }
    }
}
