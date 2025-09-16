package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResponse {
    private Long id;
    private String input;
    private String expectedOutput;
    private Boolean isHidden;
    private Double weight;
    private Integer timeLimit;
    private Integer memoryLimit;
}
