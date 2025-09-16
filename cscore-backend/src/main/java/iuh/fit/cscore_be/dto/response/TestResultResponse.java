package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultResponse {
    private Long testCaseId;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private boolean passed;
    private Long executionTime; // milliseconds
    private Long memoryUsed;    // bytes
    private String errorMessage;
    private Double weight;
    private boolean isHidden;
}
