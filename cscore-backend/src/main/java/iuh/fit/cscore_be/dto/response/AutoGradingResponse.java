package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoGradingResponse {
    private boolean success;
    private Double score;
    private Double maxScore;
    private Double totalScore; // Add this field
    private String status;
    private String message;
    private String feedback;
    private String executionResult;
    private Long executionTime;
    private Long totalExecutionTime; // Add this field
    private Long maxMemoryUsed; // Add this field
}
