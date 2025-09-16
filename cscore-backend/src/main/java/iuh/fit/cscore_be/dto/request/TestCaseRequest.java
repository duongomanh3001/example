package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestCaseRequest {
    
    private String input;
    
    @NotNull(message = "Kết quả mong đợi không được để trống")
    private String expectedOutput;
    
    private Boolean isHidden = false;
    
    private Double weight = 1.0;
    
    private Integer timeLimit = 1000;
    
    private Integer memoryLimit = 128;
}
