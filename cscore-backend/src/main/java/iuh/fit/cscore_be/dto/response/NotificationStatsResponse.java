package iuh.fit.cscore_be.dto.response;

public class NotificationStatsResponse {
    private long totalCount;
    private long unreadCount;
    private long todayCount;
    private long weekCount;

    // Constructors
    public NotificationStatsResponse() {}

    public NotificationStatsResponse(long totalCount, long unreadCount, long todayCount, long weekCount) {
        this.totalCount = totalCount;
        this.unreadCount = unreadCount;
        this.todayCount = todayCount;
        this.weekCount = weekCount;
    }

    // Getters and Setters
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

    public long getTodayCount() {
        return todayCount;
    }

    public void setTodayCount(long todayCount) {
        this.todayCount = todayCount;
    }

    public long getWeekCount() {
        return weekCount;
    }

    public void setWeekCount(long weekCount) {
        this.weekCount = weekCount;
    }

    @Override
    public String toString() {
        return "NotificationStatsResponse{" +
                "totalCount=" + totalCount +
                ", unreadCount=" + unreadCount +
                ", todayCount=" + todayCount +
                ", weekCount=" + weekCount +
                '}';
    }
}