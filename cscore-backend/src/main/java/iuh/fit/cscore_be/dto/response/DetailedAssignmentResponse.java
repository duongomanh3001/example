package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.AssignmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedAssignmentResponse {
    
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private AssignmentType type;
    private Double maxScore;
    private Integer timeLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private Boolean allowLateSubmission;
    private Boolean autoGrade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Course information
    private CourseInfo course;
    
    // Statistics
    private Integer testCaseCount;
    private Integer submissionCount;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfo {
        private Long id;
        private String name;
        private String code;
    }
}
