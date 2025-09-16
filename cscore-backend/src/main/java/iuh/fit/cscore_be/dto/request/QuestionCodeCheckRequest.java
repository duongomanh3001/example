package iuh.fit.cscore_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCodeCheckRequest {
    
    private Long questionId;
    private String code;
    private String language;
    private String input;  // Optional input for code execution
}