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
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SubmissionResponse response = new SubmissionResponse();
        
        public Builder id(Long id) {
            response.id = id;
            return this;
        }
        
        public Builder assignmentId(Long assignmentId) {
            response.assignmentId = assignmentId;
            return this;
        }
        
        public Builder assignmentTitle(String assignmentTitle) {
            response.assignmentTitle = assignmentTitle;
            return this;
        }
        
        public Builder studentName(String studentName) {
            response.studentName = studentName;
            return this;
        }
        
        public Builder studentId(String studentId) {
            response.studentId = studentId;
            return this;
        }
        
        public Builder programmingLanguage(String programmingLanguage) {
            response.programmingLanguage = programmingLanguage;
            return this;
        }
        
        public Builder status(SubmissionStatus status) {
            response.status = status;
            return this;
        }
        
        public Builder score(Double score) {
            response.score = score;
            return this;
        }
        
        public Builder executionTime(Long executionTime) {
            response.executionTime = executionTime;
            return this;
        }
        
        public Builder memoryUsed(Long memoryUsed) {
            response.memoryUsed = memoryUsed;
            return this;
        }
        
        public Builder feedback(String feedback) {
            response.feedback = feedback;
            return this;
        }
        
        public Builder submissionTime(LocalDateTime submissionTime) {
            response.submissionTime = submissionTime;
            return this;
        }
        
        public Builder gradedTime(LocalDateTime gradedTime) {
            response.gradedTime = gradedTime;
            return this;
        }
        
        public Builder testCasesPassed(Integer testCasesPassed) {
            response.testCasesPassed = testCasesPassed;
            return this;
        }
        
        public Builder totalTestCases(Integer totalTestCases) {
            response.totalTestCases = totalTestCases;
            return this;
        }
        
        public SubmissionResponse build() {
            return response;
        }
    }
}
