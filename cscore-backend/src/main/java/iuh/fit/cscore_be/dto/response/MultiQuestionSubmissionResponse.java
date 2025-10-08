package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.SubmissionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for multi-question assignment submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiQuestionSubmissionResponse {
    
    private Long submissionId;
    private Long assignmentId;
    private String assignmentTitle;
    private String studentName;
    private String studentId;
    private SubmissionStatus status;
    
    // Scoring information
    private Double finalScore;        // Final score out of max score
    private Double maxScore;         // Maximum possible score
    private Double percentage;       // Percentage score (0-100)
    
    // Individual question results
    private List<QuestionScoreResult> questionResults;
    
    // Additional information
    private String feedback;
    private LocalDateTime submissionTime;
    private LocalDateTime gradedTime;
    
    /**
     * Get total questions count
     */
    public int getTotalQuestions() {
        return questionResults != null ? questionResults.size() : 0;
    }
    
    /**
     * Get count of correct questions
     */
    public long getCorrectQuestions() {
        return questionResults != null ? 
                questionResults.stream().mapToLong(q -> q.isCorrect() ? 1 : 0).sum() : 0;
    }
    
    /**
     * Check if all questions are correct
     */
    public boolean isAllQuestionsCorrect() {
        return questionResults != null && 
               questionResults.stream().allMatch(QuestionScoreResult::isCorrect);
    }
    
    /**
     * Get formatted score display
     */
    public String getFormattedScore() {
        return String.format("%.2f/%.2f (%.1f%%)", finalScore, maxScore, percentage);
    }
}