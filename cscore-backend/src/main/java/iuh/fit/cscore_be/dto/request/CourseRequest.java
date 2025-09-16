package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CourseRequest {
    
    @NotBlank(message = "Tên khóa học không được để trống")
    private String name;
    
    @NotBlank(message = "Mã khóa học không được để trống")
    private String code;
    
    private String description;
    
    private Integer creditHours;
    
    @NotBlank(message = "Học kỳ không được để trống")
    private String semester;
    
    @NotBlank(message = "Năm học không được để trống")
    private String academicYear;
    
    private Integer maxStudents;
    
    // Constructors
    public CourseRequest() {}
    
    public CourseRequest(String name, String code, String description, Integer creditHours, 
                        String semester, String academicYear, Integer maxStudents) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.creditHours = creditHours;
        this.semester = semester;
        this.academicYear = academicYear;
        this.maxStudents = maxStudents;
    }
    
    // Getters and Setters
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
    
    public Integer getMaxStudents() {
        return maxStudents;
    }
    
    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }
}
