package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {
    
    @NotBlank(message = "Tên khóa học không được trống")
    @Size(min = 3, max = 100, message = "Tên khóa học phải từ 3 đến 100 ký tự")
    private String name;
    
    @NotBlank(message = "Mã khóa học không được trống")
    @Pattern(regexp = "^[A-Z]{2,4}[0-9]{2,4}$", message = "Mã khóa học phải có định dạng: CS01, ABC101, MATH1001")
    private String code;
    
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    @Min(value = 1, message = "Số tín chỉ phải lớn hơn 0")
    @Max(value = 10, message = "Số tín chỉ không được vượt quá 10")
    private Integer creditHours;
    
    @NotBlank(message = "Học kỳ không được trống")
    @Pattern(regexp = "^(1|2|3)$", message = "Học kỳ phải là 1, 2 hoặc 3")
    private String semester;
    
    @NotNull(message = "Năm học không được trống")
    @Min(value = 2020, message = "Năm học phải từ 2020 trở lên")
    @Max(value = 2030, message = "Năm học không được vượt quá 2030")
    private Integer year;
    
    @NotNull(message = "Số lượng sinh viên tối đa không được trống")
    @Min(value = 5, message = "Số lượng sinh viên tối đa phải ít nhất 5")
    @Max(value = 200, message = "Số lượng sinh viên tối đa không được vượt quá 200")
    private Integer maxStudents;
    
    @NotNull(message = "ID giáo viên không được trống")
    private Long teacherId;
}
