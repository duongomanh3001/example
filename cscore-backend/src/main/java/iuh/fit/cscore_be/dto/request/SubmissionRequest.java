package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmissionRequest {
    
    @NotNull(message = "ID bài tập không được để trống")
    private Long assignmentId;
    
    @NotBlank(message = "Code không được để trống")
    private String code;
    
    @NotBlank(message = "Ngôn ngữ lập trình không được để trống")
    private String programmingLanguage;

    public SubmissionRequest() {}

    public SubmissionRequest(Long assignmentId, String code, String programmingLanguage) {
        this.assignmentId = assignmentId;
        this.code = code;
        this.programmingLanguage = programmingLanguage;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(String programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }
}
