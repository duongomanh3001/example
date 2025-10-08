package iuh.fit.cscore_be.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Individual question submission item within a multi-question assignment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSubmissionItem {
    
    @NotNull(message = "Question ID is required")
    private Long questionId;
    
    @NotBlank(message = "Answer is required")
    private String answer;
    
    // For programming questions
    private String language;
    
    // For multiple choice questions
    private List<Long> selectedOptionIds;
    
    // For any additional metadata
    private String questionType;
    
    /**
     * Check if this is a programming question
     */
    public boolean isProgrammingQuestion() {
        return language != null && !language.isEmpty();
    }
    
    /**
     * Check if this is a multiple choice question
     */
    public boolean isMultipleChoiceQuestion() {
        return selectedOptionIds != null && !selectedOptionIds.isEmpty();
    }
}