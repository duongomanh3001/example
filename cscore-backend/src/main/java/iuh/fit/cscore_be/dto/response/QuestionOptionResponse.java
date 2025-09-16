package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionResponse {
    private Long id;
    private String optionText;
    private Integer optionOrder;
    // Note: we don't include isCorrect for security reasons - students shouldn't see correct answers
}
