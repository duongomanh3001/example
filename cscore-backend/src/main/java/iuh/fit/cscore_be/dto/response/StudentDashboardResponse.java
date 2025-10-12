package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardResponse {
    private Long totalCourses;
    private Long totalAssignments;
    private Long submittedAssignments;
    private Double averageScore;
    private List<StudentCourseResponse> enrolledCourses;
    private List<StudentAssignmentResponse> recentAssignments;
    private List<SubmissionResponse> recentSubmissions;
    private StudentStatistics statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentStatistics {
        private Long totalSubmissions;
        private Long pendingSubmissions;
        private Long gradedSubmissions;
        private Double bestScore;
        private Integer coursesEnrolled;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private StudentStatistics statistics = new StudentStatistics();
            
            public Builder totalSubmissions(Long totalSubmissions) {
                statistics.totalSubmissions = totalSubmissions;
                return this;
            }
            
            public Builder pendingSubmissions(Long pendingSubmissions) {
                statistics.pendingSubmissions = pendingSubmissions;
                return this;
            }
            
            public Builder gradedSubmissions(Long gradedSubmissions) {
                statistics.gradedSubmissions = gradedSubmissions;
                return this;
            }
            
            public Builder bestScore(Double bestScore) {
                statistics.bestScore = bestScore;
                return this;
            }
            
            public Builder coursesEnrolled(Integer coursesEnrolled) {
                statistics.coursesEnrolled = coursesEnrolled;
                return this;
            }
            
            public StudentStatistics build() {
                return statistics;
            }
        }
    }
}
