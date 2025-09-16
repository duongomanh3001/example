package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for validating teacher's answer code against test cases
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeValidationRequest {
    
    @NotBlank(message = "Code cannot be blank")
    private String code;
    
    private String language = "c"; // Default to C language
    
    private String input = ""; // Optional input for the program
    
    // Additional constructors for convenience
    public CodeValidationRequest(String code, String language) {
        this.code = code;
        this.language = language;
        this.input = "";
    }
    
    public CodeValidationRequest(String code) {
        this.code = code;
        this.language = "c";
        this.input = "";
    }
}