package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private Long assignmentId;
    private String title;
    private String description;
    private QuestionType questionType;
    private Double points;
    private Integer orderIndex;
    private List<TestCaseResponse> testCases;
    private List<QuestionOptionResponse> options;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}