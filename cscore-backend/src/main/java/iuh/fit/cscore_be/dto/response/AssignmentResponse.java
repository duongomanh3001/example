package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.AssignmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private AssignmentType type;
    private String courseName;
    private Long courseId; // Added
    private String courseCode; // Added
    private Double maxScore;
    private Integer timeLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private Boolean allowLateSubmission;
    private Boolean autoGrade;
    private Long submissionCount;
    private Long pendingCount;
    private Long totalQuestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // Added
}
