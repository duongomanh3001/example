package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionOptionRequest {
    
    @NotBlank(message = "Nội dung lựa chọn không được trống")
    @Size(min = 1, max = 1000, message = "Nội dung lựa chọn phải từ 1 đến 1000 ký tự")
    private String optionText;
    
    private Boolean isCorrect = false;
    
    private Integer optionOrder;
}
