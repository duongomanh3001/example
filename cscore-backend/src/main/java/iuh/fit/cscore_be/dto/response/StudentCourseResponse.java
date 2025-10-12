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
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private StudentCourseResponse response = new StudentCourseResponse();
        
        public Builder id(Long id) {
            response.id = id;
            return this;
        }
        
        public Builder name(String name) {
            response.name = name;
            return this;
        }
        
        public Builder code(String code) {
            response.code = code;
            return this;
        }
        
        public Builder description(String description) {
            response.description = description;
            return this;
        }
        
        public Builder semester(String semester) {
            response.semester = semester;
            return this;
        }
        
        public Builder academicYear(String academicYear) {
            response.academicYear = academicYear;
            return this;
        }
        
        public Builder teacherName(String teacherName) {
            response.teacherName = teacherName;
            return this;
        }
        
        public Builder enrollmentDate(LocalDateTime enrollmentDate) {
            response.enrollmentDate = enrollmentDate;
            return this;
        }
        
        public Builder finalGrade(Double finalGrade) {
            response.finalGrade = finalGrade;
            return this;
        }
        
        public Builder totalAssignments(Integer totalAssignments) {
            response.totalAssignments = totalAssignments;
            return this;
        }
        
        public Builder completedAssignments(Integer completedAssignments) {
            response.completedAssignments = completedAssignments;
            return this;
        }
        
        public StudentCourseResponse build() {
            return response;
        }
    }
}
