package iuh.fit.cscore_be.dto.request;

import iuh.fit.cscore_be.enums.AssignmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    private String description;
    
    private String requirements;
    
    @NotNull(message = "Loại bài tập không được để trống")
    private AssignmentType type;
    
    @NotNull(message = "ID khóa học không được để trống")
    private Long courseId;
    
    private Double maxScore = 100.0;
    
    private Integer timeLimit;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Boolean allowLateSubmission = false;
    
    private Boolean autoGrade = true;
}
