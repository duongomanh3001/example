package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedCourseResponse {
    
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer creditHours;
    private String semester;
    private Integer year;
    private Integer maxStudents;
    private Integer currentStudentCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Teacher information
    private TeacherInfo teacher;
    
    // Assignment count
    private Integer assignmentCount;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long id;
        private String username;
        private String fullName;
        private String email;
    }
}
