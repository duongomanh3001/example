package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicTestCaseResponse {
    private Long id;
    private String input;
    private String expectedOutput;
    private Double weight;
}
