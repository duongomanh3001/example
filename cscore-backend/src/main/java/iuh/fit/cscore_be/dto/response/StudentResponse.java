package iuh.fit.cscore_be.dto.response;

import java.time.LocalDateTime;

public class StudentResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String studentId;
    private Boolean isActive;
    private LocalDateTime enrollmentDate;
    private String enrolledAt; // Added for enrollment date as string
    private Double finalGrade;
    private Integer totalSubmissions;
    private Double averageScore;
    
    // Constructors
    public StudentResponse() {}
    
    public StudentResponse(Long id, String username, String email, String fullName, 
                          String studentId, Boolean isActive, LocalDateTime enrollmentDate, 
                          Double finalGrade, Integer totalSubmissions, Double averageScore) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.studentId = studentId;
        this.isActive = isActive;
        this.enrollmentDate = enrollmentDate;
        this.finalGrade = finalGrade;
        this.totalSubmissions = totalSubmissions;
        this.averageScore = averageScore;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public String getEnrolledAt() {
        return enrolledAt;
    }
    
    public void setEnrolledAt(String enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
    
    public Double getFinalGrade() {
        return finalGrade;
    }
    
    public void setFinalGrade(Double finalGrade) {
        this.finalGrade = finalGrade;
    }
    
    public Integer getTotalSubmissions() {
        return totalSubmissions;
    }
    
    public void setTotalSubmissions(Integer totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }
    
    public Double getAverageScore() {
        return averageScore;
    }
    
    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }
}
