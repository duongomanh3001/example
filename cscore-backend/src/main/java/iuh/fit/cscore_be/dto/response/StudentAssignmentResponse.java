package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.AssignmentType;
import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private AssignmentType type;
    private Long courseId;
    private String courseName;
    private Long sectionId; // Add section reference for organizing assignments
    private Double maxScore;
    private Integer timeLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowLateSubmission;
    private Boolean isSubmitted;
    private Double currentScore;
    private LocalDateTime submissionTime;
    private String submissionStatus;
    private List<PublicTestCaseResponse> publicTestCases;
    private Integer totalQuestions;
    private Integer totalTestCases;
    private List<StudentQuestionResponse> questions; // Add questions field
    private Set<ProgrammingLanguage> programmingLanguages; // Add programming languages field
    private LocalDateTime createdAt;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private StudentAssignmentResponse response = new StudentAssignmentResponse();
        
        public Builder id(Long id) {
            response.id = id;
            return this;
        }
        
        public Builder title(String title) {
            response.title = title;
            return this;
        }
        
        public Builder description(String description) {
            response.description = description;
            return this;
        }
        
        public Builder requirements(String requirements) {
            response.requirements = requirements;
            return this;
        }
        
        public Builder type(AssignmentType type) {
            response.type = type;
            return this;
        }
        
        public Builder courseId(Long courseId) {
            response.courseId = courseId;
            return this;
        }
        
        public Builder courseName(String courseName) {
            response.courseName = courseName;
            return this;
        }
        
        public Builder courseTitle(String courseTitle) {
            response.courseName = courseTitle; // Map courseTitle to courseName
            return this;
        }
        
        public Builder sectionId(Long sectionId) {
            response.sectionId = sectionId;
            return this;
        }
        
        public Builder maxScore(Double maxScore) {
            response.maxScore = maxScore;
            return this;
        }
        
        public Builder timeLimit(Integer timeLimit) {
            response.timeLimit = timeLimit;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            response.startTime = startTime;
            return this;
        }
        
        public Builder endTime(LocalDateTime endTime) {
            response.endTime = endTime;
            return this;
        }
        
        public Builder allowLateSubmission(Boolean allowLateSubmission) {
            response.allowLateSubmission = allowLateSubmission;
            return this;
        }
        
        public Builder isSubmitted(Boolean isSubmitted) {
            response.isSubmitted = isSubmitted;
            return this;
        }
        
        public Builder currentScore(Double currentScore) {
            response.currentScore = currentScore;
            return this;
        }
        
        public Builder submissionTime(LocalDateTime submissionTime) {
            response.submissionTime = submissionTime;
            return this;
        }
        
        public Builder submissionStatus(String submissionStatus) {
            response.submissionStatus = submissionStatus;
            return this;
        }
        
        public Builder publicTestCases(List<PublicTestCaseResponse> publicTestCases) {
            response.publicTestCases = publicTestCases;
            return this;
        }
        
        public Builder totalQuestions(Integer totalQuestions) {
            response.totalQuestions = totalQuestions;
            return this;
        }
        
        public Builder totalTestCases(Integer totalTestCases) {
            response.totalTestCases = totalTestCases;
            return this;
        }
        
        public Builder questions(List<StudentQuestionResponse> questions) {
            response.questions = questions;
            return this;
        }
        
        public Builder programmingLanguages(Set<ProgrammingLanguage> programmingLanguages) {
            response.programmingLanguages = programmingLanguages;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            response.createdAt = createdAt;
            return this;
        }
        
        public StudentAssignmentResponse build() {
            return response;
        }
    }
}
