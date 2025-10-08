package iuh.fit.cscore_be.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class DeleteNotificationRequest {
    
    @NotEmpty(message = "Notification IDs are required")
    private List<Long> notificationIds;

    // Constructors
    public DeleteNotificationRequest() {}

    public DeleteNotificationRequest(List<Long> notificationIds) {
        this.notificationIds = notificationIds;
    }

    // Getters and Setters
    public List<Long> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(List<Long> notificationIds) {
        this.notificationIds = notificationIds;
    }

    @Override
    public String toString() {
        return "DeleteNotificationRequest{" +
                "notificationIds=" + notificationIds +
                '}';
    }
}