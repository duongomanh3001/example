package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.AssignmentType;
import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private AssignmentType type;
    private String courseName;
    private Long courseId; // Added
    private String courseCode; // Added
    private Double maxScore;
    private Integer timeLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private Boolean allowLateSubmission;
    private Boolean autoGrade;
    private Set<ProgrammingLanguage> programmingLanguages;
    private Long submissionCount;
    private Long pendingCount;
    private Long totalQuestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // Added
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private AssignmentResponse response = new AssignmentResponse();
        
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
        
        public Builder type(AssignmentType type) {
            response.type = type;
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
        
        public Builder courseId(Long courseId) {
            response.courseId = courseId;
            return this;
        }
        
        public Builder courseCode(String courseCode) {
            response.courseCode = courseCode;
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
        
        public Builder isActive(Boolean isActive) {
            response.isActive = isActive;
            return this;
        }
        
        public Builder allowLateSubmission(Boolean allowLateSubmission) {
            response.allowLateSubmission = allowLateSubmission;
            return this;
        }
        
        public Builder autoGrade(Boolean autoGrade) {
            response.autoGrade = autoGrade;
            return this;
        }
        
        public Builder programmingLanguages(Set<ProgrammingLanguage> programmingLanguages) {
            response.programmingLanguages = programmingLanguages;
            return this;
        }
        
        public Builder submissionCount(Long submissionCount) {
            response.submissionCount = submissionCount;
            return this;
        }
        
        public Builder pendingCount(Long pendingCount) {
            response.pendingCount = pendingCount;
            return this;
        }
        
        public Builder totalQuestions(Long totalQuestions) {
            response.totalQuestions = totalQuestions;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            response.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(LocalDateTime updatedAt) {
            response.updatedAt = updatedAt;
            return this;
        }
        
        public AssignmentResponse build() {
            return response;
        }
    }
}
