package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String semester;
    private String academicYear;
    private String teacherName;
    private LocalDateTime enrollmentDate;
    private Double finalGrade;
    private Integer totalAssignments;
    private Integer completedAssignments;
}
