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
