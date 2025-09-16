package iuh.fit.cscore_be.dto.request;

import iuh.fit.cscore_be.enums.AssignmentType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentRequest {
    
    @NotBlank(message = "Tiêu đề bài tập không được trống")
    @Size(min = 3, max = 200, message = "Tiêu đề bài tập phải từ 3 đến 200 ký tự")
    private String title;
    
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;
    
    @Size(max = 3000, message = "Yêu cầu không được vượt quá 3000 ký tự")
    private String requirements;
    
    @NotNull(message = "Loại bài tập không được trống")
    private AssignmentType type;
    
    @NotNull(message = "ID khóa học không được trống")
    private Long courseId;
    
    @DecimalMin(value = "0.0", message = "Điểm tối đa phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "100.0", message = "Điểm tối đa không được vượt quá 100")
    private Double maxScore = 100.0;
    
    @Min(value = 1, message = "Thời gian làm bài phải ít nhất 1 phút")
    @Max(value = 300, message = "Thời gian làm bài không được vượt quá 300 phút")
    private Integer timeLimit;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Boolean allowLateSubmission = false;
    
    private Boolean autoGrade = true;
    
    private List<CreateQuestionRequest> questions;
}
