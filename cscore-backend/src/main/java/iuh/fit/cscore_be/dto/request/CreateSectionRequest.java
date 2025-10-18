package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSectionRequest {
    
    @NotNull(message = "Course ID không được trống")
    private Long courseId;
    
    @NotBlank(message = "Tên phân mục không được trống")
    @Size(min = 1, max = 200, message = "Tên phân mục phải từ 1 đến 200 ký tự")
    private String name;
    
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    @Min(value = 0, message = "Order index phải lớn hơn hoặc bằng 0")
    private Integer orderIndex = 0;
}
