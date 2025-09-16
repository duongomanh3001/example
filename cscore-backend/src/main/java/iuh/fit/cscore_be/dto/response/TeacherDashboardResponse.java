package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardResponse {
    private Long totalCourses;
    private Long totalStudents;
    private Long totalAssignments;
    private Long pendingSubmissions;
    private List<CourseResponse> recentCourses;
    private List<AssignmentResponse> recentAssignments;
    private List<SubmissionResponse> pendingGrades;
    private TeacherStatistics statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherStatistics {
        private Double averageScore;
        private Long totalSubmissions;
        private Long gradedSubmissions;
        private Integer activeStudents;
    }
}
