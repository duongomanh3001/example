package iuh.fit.cscore_be.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public class NotificationPageResponse {
    private List<NotificationResponse> notifications;
    private long totalCount;
    private long unreadCount;
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    // Constructors
    public NotificationPageResponse() {}

    public NotificationPageResponse(Page<NotificationResponse> page, long unreadCount) {
        this.notifications = page.getContent();
        this.totalCount = page.getTotalElements();
        this.unreadCount = unreadCount;
        this.currentPage = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.pageSize = page.getSize();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }

    // Static factory methods
    public static NotificationPageResponse from(Page<NotificationResponse> page, long unreadCount) {
        return new NotificationPageResponse(page, unreadCount);
    }

    // Getters and Setters
    public List<NotificationResponse> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationResponse> notifications) {
        this.notifications = notifications;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    @Override
    public String toString() {
        return "NotificationPageResponse{" +
                "totalCount=" + totalCount +
                ", unreadCount=" + unreadCount +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                '}';
    }
}