package iuh.fit.cscore_be.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for multi-question assignment submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiQuestionSubmissionRequest {
    
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotEmpty(message = "At least one question must be answered")
    @Valid
    private List<QuestionSubmissionItem> questions;
    
    // Primary programming language for the submission (if mixed languages are used)
    private String primaryLanguage;
    
    // Combined code for legacy compatibility
    private String combinedCode;
    
    /**
     * Get the primary programming language from the questions
     */
    public String getPrimaryLanguage() {
        if (primaryLanguage != null && !primaryLanguage.isEmpty()) {
            return primaryLanguage;
        }
        
        // Find the first programming question's language
        return questions.stream()
                .filter(q -> q.getLanguage() != null && !q.getLanguage().isEmpty())
                .findFirst()
                .map(QuestionSubmissionItem::getLanguage)
                .orElse("javascript");
    }
    
    /**
     * Get combined code from all questions for legacy compatibility
     */
    public String getCombinedCode() {
        if (combinedCode != null && !combinedCode.isEmpty()) {
            return combinedCode;
        }
        
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            QuestionSubmissionItem question = questions.get(i);
            if (question.getAnswer() != null && !question.getAnswer().trim().isEmpty()) {
                if (i > 0) {
                    combined.append("\n\n// --- Next Question ---\n\n");
                }
                combined.append("// Question ").append(question.getQuestionId())
                        .append("\n").append(question.getAnswer());
            }
        }
        
        return combined.toString();
    }
}