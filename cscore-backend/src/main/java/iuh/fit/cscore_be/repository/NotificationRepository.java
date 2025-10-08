package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Notification;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.NotificationCategory;
import iuh.fit.cscore_be.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find notifications by user
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // Find unread notifications by user
    Page<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // Count unread notifications by user
    long countByUserAndIsReadFalse(User user);
    
    // Find notifications by user with filters
    @Query("SELECT n FROM Notification n WHERE n.user = :user " +
           "AND (:isRead IS NULL OR n.isRead = :isRead) " +
           "AND (:type IS NULL OR n.type = :type) " +
           "AND (:category IS NULL OR n.category = :category) " +
           "AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR n.createdAt <= :dateTo) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserWithFilters(
            @Param("user") User user,
            @Param("isRead") Boolean isRead,
            @Param("type") NotificationType type,
            @Param("category") NotificationCategory category,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );
    
    // Count notifications by user and date range
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user " +
           "AND n.createdAt >= :startDate AND n.createdAt <= :endDate")
    long countByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // Find recent notifications (last 30 days)
    @Query("SELECT n FROM Notification n WHERE n.user = :user " +
           "AND n.createdAt >= :thirtyDaysAgo " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(
            @Param("user") User user,
            @Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo
    );
    
    // Mark multiple notifications as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = :isRead WHERE n.id IN :notificationIds AND n.user = :user")
    int updateReadStatusByIds(
            @Param("notificationIds") List<Long> notificationIds,
            @Param("isRead") Boolean isRead,
            @Param("user") User user
    );
    
    // Mark all notifications as read for user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadForUser(@Param("user") User user);
    
    // Delete old notifications (older than specified days)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Find notifications by related entity
    List<Notification> findByRelatedEntityIdAndRelatedEntityType(Long entityId, String entityType);
    
    // Get notification statistics for user
    @Query("SELECT " +
           "COUNT(n) as totalCount, " +
           "SUM(CASE WHEN n.isRead = false THEN 1 ELSE 0 END) as unreadCount, " +
           "SUM(CASE WHEN n.createdAt >= :todayStart THEN 1 ELSE 0 END) as todayCount " +
           "FROM Notification n WHERE n.user = :user")
    Object[] getNotificationStats(
            @Param("user") User user,
            @Param("todayStart") LocalDateTime todayStart
    );
}