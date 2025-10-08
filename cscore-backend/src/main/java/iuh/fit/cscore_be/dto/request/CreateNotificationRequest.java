package iuh.fit.cscore_be.dto.request;

import iuh.fit.cscore_be.enums.NotificationCategory;
import iuh.fit.cscore_be.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateNotificationRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;
    
    @NotNull(message = "Type is required")
    private NotificationType type;
    
    @NotNull(message = "Category is required")
    private NotificationCategory category;
    
    // For single user notification
    private Long userId;
    
    // For multiple users notification
    private List<Long> userIds;
    
    // For role-based notification
    private String roleType; // STUDENT, TEACHER, ADMIN
    
    // For course-based notification
    private Long courseId;
    
    // Optional fields
    private Long relatedEntityId;
    
    private String relatedEntityType;
    
    @Size(max = 500, message = "Action URL must not exceed 500 characters")
    private String actionUrl;

    // Constructors
    public CreateNotificationRequest() {}

    public CreateNotificationRequest(String title, String message, NotificationType type, NotificationCategory category) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.category = category;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public void setCategory(NotificationCategory category) {
        this.category = category;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    @Override
    public String toString() {
        return "CreateNotificationRequest{" +
                "title='" + title + '\'' +
                ", type=" + type +
                ", category=" + category +
                ", userId=" + userId +
                ", courseId=" + courseId +
                '}';
    }
}