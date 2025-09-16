package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Teacher question response DTO that includes sensitive information like reference implementations
 * This should only be used for teacher/admin endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherQuestionResponse {
    private Long id;
    private String title;
    private String description;
    private QuestionType questionType;
    private Double points;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<TestCaseResponse> testCases;
    private List<QuestionOptionResponse> options;
    
    // Enhanced grading fields (full access for teachers)
    private String referenceImplementation;
    private String functionName;
    private String functionSignature;
    private String programmingLanguage;
    private String testTemplate;
    
    // Statistics
    private Integer totalSubmissions;
    private Double averageScore;
    private Integer passRate; // Percentage of students who passed
}