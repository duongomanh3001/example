package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.AssignmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private AssignmentType type;
    private Long courseId;
    private String courseName;
    private Double maxScore;
    private Integer timeLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowLateSubmission;
    private Boolean isSubmitted;
    private Double currentScore;
    private LocalDateTime submissionTime;
    private String submissionStatus;
    private List<PublicTestCaseResponse> publicTestCases;
    private Integer totalQuestions;
    private Integer totalTestCases;
    private List<StudentQuestionResponse> questions; // Add questions field
    private LocalDateTime createdAt;
}
