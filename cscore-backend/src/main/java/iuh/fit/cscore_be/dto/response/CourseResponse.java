package iuh.fit.cscore_be.dto.response;

import java.time.LocalDateTime;

public class CourseResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer creditHours;
    private String semester;
    private Integer year;  // Changed from academicYear to year
    private String academicYear;
    private String teacherName;
    private Long studentCount;
    private Long assignmentCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;  // Added
    private Integer maxStudents;
    private Integer currentStudentCount;  // Added
    private Boolean enrollmentOpen;
    
    // Teacher info nested class
    public static class TeacherInfo {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        
        public TeacherInfo() {}
        
        public TeacherInfo(Long id, String username, String fullName, String email) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    private TeacherInfo teacher;  // Added nested teacher info
    
    // Constructors
    public CourseResponse() {}
    
    public CourseResponse(Long id, String name, String code, String description, 
                         Integer creditHours, String semester, String academicYear, 
                         String teacherName, Long studentCount, Long assignmentCount, 
                         Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.creditHours = creditHours;
        this.semester = semester;
        this.academicYear = academicYear;
        this.teacherName = teacherName;
        this.studentCount = studentCount;
        this.assignmentCount = assignmentCount;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getCreditHours() {
        return creditHours;
    }
    
    public void setCreditHours(Integer creditHours) {
        this.creditHours = creditHours;
    }
    
    public String getSemester() {
        return semester;
    }
    
    public void setSemester(String semester) {
        this.semester = semester;
    }
    
    public String getAcademicYear() {
        return academicYear;
    }
    
    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }
    
    public String getTeacherName() {
        return teacherName;
    }
    
    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
    
    public Long getStudentCount() {
        return studentCount;
    }
    
    public void setStudentCount(Long studentCount) {
        this.studentCount = studentCount;
    }
    
    public Long getAssignmentCount() {
        return assignmentCount;
    }
    
    public void setAssignmentCount(Long assignmentCount) {
        this.assignmentCount = assignmentCount;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public Integer getCurrentStudentCount() {
        return currentStudentCount;
    }
    
    public void setCurrentStudentCount(Integer currentStudentCount) {
        this.currentStudentCount = currentStudentCount;
    }
    
    public TeacherInfo getTeacher() {
        return teacher;
    }
    
    public void setTeacher(TeacherInfo teacher) {
        this.teacher = teacher;
    }
    
    public Integer getMaxStudents() {
        return maxStudents;
    }
    
    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }
    
    public Boolean getEnrollmentOpen() {
        return enrollmentOpen;
    }
    
    public void setEnrollmentOpen(Boolean enrollmentOpen) {
        this.enrollmentOpen = enrollmentOpen;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private CourseResponse response = new CourseResponse();
        
        public Builder id(Long id) {
            response.id = id;
            return this;
        }
        
        public Builder name(String name) {
            response.name = name;
            return this;
        }
        
        public Builder title(String title) {
            response.name = title; // Map title to name
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
        
        public Builder creditHours(Integer creditHours) {
            response.creditHours = creditHours;
            return this;
        }
        
        public Builder semester(String semester) {
            response.semester = semester;
            return this;
        }
        
        public Builder year(Integer year) {
            response.year = year;
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
        
        public Builder studentCount(Long studentCount) {
            response.studentCount = studentCount;
            return this;
        }
        
        public Builder assignmentCount(Long assignmentCount) {
            response.assignmentCount = assignmentCount;
            return this;
        }
        
        public Builder isActive(Boolean isActive) {
            response.isActive = isActive;
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
        
        public Builder maxStudents(Integer maxStudents) {
            response.maxStudents = maxStudents;
            return this;
        }
        
        public Builder currentStudentCount(Integer currentStudentCount) {
            response.currentStudentCount = currentStudentCount;
            return this;
        }
        
        public Builder enrollmentOpen(Boolean enrollmentOpen) {
            response.enrollmentOpen = enrollmentOpen;
            return this;
        }
        
        public Builder teacher(TeacherInfo teacher) {
            response.teacher = teacher;
            return this;
        }
        
        public CourseResponse build() {
            return response;
        }
    }
}
