package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionResponse {
    private boolean success;
    
    // Explicit getter for boolean field
    public boolean isSuccess() {
        return success;
    }
    private String output;
    private String error;
    private String language;
    private Long executionTime;
    private Long memoryUsed;
    
    // For legacy compatibility
    private String stdout;
    private String stderr;
    private String compile_output;
    private String message;
    private Integer exit_code;
    private String status;
    private Double time;
    private Double memory;
    private String token;
    
    // For test case execution
    private List<TestResultResponse> testResults;
    private int passedTests;
    private int totalTests;
    private double score;
    private String compilationError;
    private boolean isCompiled = true;
    
    // Legacy test results
    private List<TestCaseResult> legacyTestResults;
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private CodeExecutionResponse response = new CodeExecutionResponse();
        
        public Builder success(boolean success) {
            response.success = success;
            return this;
        }
        
        public Builder output(String output) {
            response.output = output;
            return this;
        }
        
        public Builder error(String error) {
            response.error = error;
            return this;
        }
        
        public Builder language(String language) {
            response.language = language;
            return this;
        }
        
        public Builder executionTime(Long executionTime) {
            response.executionTime = executionTime;
            return this;
        }
        
        public Builder memoryUsed(Long memoryUsed) {
            response.memoryUsed = memoryUsed;
            return this;
        }
        
        public Builder stdout(String stdout) {
            response.stdout = stdout;
            return this;
        }
        
        public Builder stderr(String stderr) {
            response.stderr = stderr;
            return this;
        }
        
        public Builder compile_output(String compile_output) {
            response.compile_output = compile_output;
            return this;
        }
        
        public Builder message(String message) {
            response.message = message;
            return this;
        }
        
        public Builder exit_code(Integer exit_code) {
            response.exit_code = exit_code;
            return this;
        }
        
        public Builder status(String status) {
            response.status = status;
            return this;
        }
        
        public Builder time(Double time) {
            response.time = time;
            return this;
        }
        
        public Builder memory(Double memory) {
            response.memory = memory;
            return this;
        }
        
        public Builder token(String token) {
            response.token = token;
            return this;
        }
        
        public Builder testResults(List<TestResultResponse> testResults) {
            response.testResults = testResults;
            return this;
        }
        
        public Builder passedTests(int passedTests) {
            response.passedTests = passedTests;
            return this;
        }
        
        public Builder totalTests(int totalTests) {
            response.totalTests = totalTests;
            return this;
        }
        
        public Builder score(double score) {
            response.score = score;
            return this;
        }
        
        public Builder compilationError(String compilationError) {
            response.compilationError = compilationError;
            return this;
        }
        
        public Builder isCompiled(boolean isCompiled) {
            response.isCompiled = isCompiled;
            return this;
        }
        
        public Builder legacyTestResults(List<TestCaseResult> legacyTestResults) {
            response.legacyTestResults = legacyTestResults;
            return this;
        }
        
        public CodeExecutionResponse build() {
            return response;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private Long testCaseId;
        private Boolean passed;
        private String actualOutput;
        private String expectedOutput;
        private String errorMessage;
        private Double executionTime;
        private Double weight;
    }
}
