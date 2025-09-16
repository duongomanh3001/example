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
    }
}
