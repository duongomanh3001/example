package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class MarkNotificationRequest {
    
    @NotEmpty(message = "Notification IDs are required")
    private List<Long> notificationIds;
    
    @NotNull(message = "Read status is required")
    private Boolean isRead;

    // Constructors
    public MarkNotificationRequest() {}

    public MarkNotificationRequest(List<Long> notificationIds, Boolean isRead) {
        this.notificationIds = notificationIds;
        this.isRead = isRead;
    }

    // Getters and Setters
    public List<Long> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(List<Long> notificationIds) {
        this.notificationIds = notificationIds;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "MarkNotificationRequest{" +
                "notificationIds=" + notificationIds +
                ", isRead=" + isRead +
                '}';
    }
}