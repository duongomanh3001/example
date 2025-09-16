package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestCaseRequest {
    
    @Size(max = 5000, message = "Input không được vượt quá 5000 ký tự")
    private String input;
    
    @NotBlank(message = "Expected output không được trống")
    @Size(max = 5000, message = "Expected output không được vượt quá 5000 ký tự")
    private String expectedOutput;
    
    private Boolean isHidden = false;
    
    private Double weight = 1.0;
    
    private Integer timeLimit = 1000;
    
    private Integer memoryLimit = 128;
}
