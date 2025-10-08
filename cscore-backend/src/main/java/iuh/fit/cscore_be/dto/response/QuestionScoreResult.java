package iuh.fit.cscore_be.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Score result for individual question in multi-question assignment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionScoreResult {
    
    private Long questionId;
    private String questionTitle;
    private String questionType;
    
    // Scoring information
    private Double score;           // Actual score achieved
    private Double maxPoints;       // Maximum points for this question
    private boolean isCorrect;      // Whether the answer is completely correct
    
    // Feedback and details
    private String feedback;
    private List<TestResultResponse> testResults; // For programming questions
    
    /**
     * Get percentage score for this question
     */
    public double getPercentage() {
        return maxPoints > 0 ? (score / maxPoints) * 100 : 0.0;
    }
    
    /**
     * Get formatted score display
     */
    public String getFormattedScore() {
        return String.format("%.2f/%.2f", score, maxPoints);
    }
    
    /**
     * Check if this is a programming question
     */
    public boolean isProgrammingQuestion() {
        return "PROGRAMMING".equals(questionType);
    }
    
    /**
     * Get pass/fail status
     */
    public String getStatus() {
        return isCorrect ? "PASS" : "FAIL";
    }
}