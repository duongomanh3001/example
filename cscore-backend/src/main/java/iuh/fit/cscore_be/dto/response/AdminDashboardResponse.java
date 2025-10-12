package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    
    // Basic statistics
    private Long totalUsers;
    private Long totalCourses;
    private Long totalAssignments;
    private Long totalSubmissions;
    private Long activeUsers;
    
    // Recent activities
    private List<CourseResponse> recentCourses;
    private List<UserResponse> recentUsers;
    private List<SubmissionResponse> recentSubmissions;
    
    // System statistics
    private SystemStatistics systemStatistics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStatistics {
        private Long totalStudents;
        private Long totalTeachers;
        private Double averageScore;
        private Long submissionsToday;
        private Long coursesCreatedThisMonth;
        private Long activeUsersToday;
        private String systemVersion;
        private Long uptime;
    }
}