package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionResponse {
    private Long id;
    private String title;
    private String description;
    private QuestionType questionType;
    private Double points;
    private Integer orderIndex;
    private List<PublicTestCaseResponse> publicTestCases;
    private List<QuestionOptionResponse> options;
    private Boolean isAnswered;
    private String userAnswer;
    private List<Long> selectedOptionIds;
    
    // Programming-specific fields
    private String starterCode;
    private List<PublicTestCaseResponse> exampleTestCases;
    private String language;
    private Integer totalTestCases;
    
    // Enhanced grading fields (public information only)
    private String functionName;
    private String functionSignature;
    private String programmingLanguage;
    private boolean hasReferenceImplementation;
}
