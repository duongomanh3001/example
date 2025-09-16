package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private String studentName;
    private String studentId;
    private String programmingLanguage;
    private SubmissionStatus status;
    private Double score;
    private Long executionTime;
    private Long memoryUsed;
    private String feedback;
    private LocalDateTime submissionTime;
    private LocalDateTime gradedTime;
    private Integer testCasesPassed;
    private Integer totalTestCases;
}
