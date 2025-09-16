package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for enhanced auto-grading results
 * Contains detailed comparison information between student and reference implementations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedAutoGradingResponse {
    
    private Long submissionId;
    private Double finalScore;
    private Double maxScore;
    private String overallFeedback;
    private String gradingMethod; // "enhanced" or "fallback"
    private Long totalExecutionTime;
    
    private List<QuestionGradingDetail> questionDetails;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGradingDetail {
        private Long questionId;
        private String questionTitle;
        private Double earnedScore;
        private Double maxScore;
        private String feedback;
        private boolean hasReferenceImplementation;
        private String gradingMethod; // "reference_comparison" or "fallback"
        
        private int totalTestCases;
        private int passedTestCases;
        
        private List<TestCaseComparison> testCaseResults;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseComparison {
        private Long testCaseId;
        private boolean passed;
        private String input;
        private String expectedOutput; // From reference implementation
        private String studentOutput;
        private String referenceOutput; // May differ from expectedOutput if dynamic
        private String errorMessage;
        private Long executionTime;
        private Double weight;
        
        // Additional comparison details
        private boolean compilationSuccess;
        private boolean runtimeSuccess;
        private String comparisonResult; // "PASS", "FAIL", "ERROR", "TIMEOUT"
    }
}