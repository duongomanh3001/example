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
public class DetailedAssignmentResponse {
    
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private AssignmentType type;
    private Double maxScore;
    private Integer timeLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private Boolean allowLateSubmission;
    private Boolean autoGrade;
    private Set<ProgrammingLanguage> programmingLanguages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Course information
    private CourseInfo course;
    
    // Statistics
    private Integer testCaseCount;
    private Integer submissionCount;
    
    // Questions
    private List<QuestionResponse> questions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfo {
        private Long id;
        private String name;
        private String code;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private DetailedAssignmentResponse response = new DetailedAssignmentResponse();
        
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
        
        public Builder createdAt(LocalDateTime createdAt) {
            response.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(LocalDateTime updatedAt) {
            response.updatedAt = updatedAt;
            return this;
        }
        
        public Builder course(CourseInfo course) {
            response.course = course;
            return this;
        }
        
        public Builder courseId(Long courseId) {
            if (response.course == null) {
                response.course = new CourseInfo();
            }
            response.course.id = courseId;
            return this;
        }
        
        public Builder courseTitle(String courseTitle) {
            if (response.course == null) {
                response.course = new CourseInfo();
            }
            response.course.name = courseTitle;
            return this;
        }
        
        public Builder teacherName(String teacherName) {
            // This field doesn't exist in the response, so we'll ignore it for now
            return this;
        }
        
        public Builder testCaseCount(Integer testCaseCount) {
            response.testCaseCount = testCaseCount;
            return this;
        }
        
        public Builder submissionCount(Integer submissionCount) {
            response.submissionCount = submissionCount;
            return this;
        }
        
        public Builder totalQuestions(Long totalQuestions) {
            // Map totalQuestions to testCaseCount for now, or add this field to the response class
            response.testCaseCount = totalQuestions.intValue();
            return this;
        }
        
        public Builder questions(List<QuestionResponse> questions) {
            response.questions = questions;
            return this;
        }
        
        public DetailedAssignmentResponse build() {
            return response;
        }
    }
}
