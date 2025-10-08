package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.request.CreateNotificationRequest;
import iuh.fit.cscore_be.dto.request.DeleteNotificationRequest;
import iuh.fit.cscore_be.dto.request.MarkNotificationRequest;
import iuh.fit.cscore_be.dto.response.NotificationPageResponse;
import iuh.fit.cscore_be.dto.response.NotificationResponse;
import iuh.fit.cscore_be.dto.response.NotificationStatsResponse;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.NotificationCategory;
import iuh.fit.cscore_be.enums.NotificationType;
import iuh.fit.cscore_be.repository.UserRepository;
import iuh.fit.cscore_be.security.UserPrincipal;
import iuh.fit.cscore_be.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;
    
    private User getCurrentUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get notifications for current user with optional filters
     */
    @GetMapping
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "isRead", required = false) Boolean isRead,
            @RequestParam(value = "type", required = false) NotificationType type,
            @RequestParam(value = "category", required = false) NotificationCategory category,
            @RequestParam(value = "dateFrom", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        User currentUser = getCurrentUser(userPrincipal);
        NotificationPageResponse response = notificationService.getNotifications(
                currentUser, isRead, type, category, dateFrom, dateTo, page, size
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get notification statistics for current user
     */
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsResponse> getNotificationStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = getCurrentUser(userPrincipal);
        NotificationStatsResponse stats = notificationService.getNotificationStats(currentUser);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = getCurrentUser(userPrincipal);
        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get notification by ID
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        
        User currentUser = getCurrentUser(userPrincipal);
        NotificationResponse response = notificationService.getNotificationById(currentUser, notificationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark notifications as read/unread
     */
    @PutMapping("/mark")
    public ResponseEntity<Void> markNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MarkNotificationRequest request) {
        
        User currentUser = getCurrentUser(userPrincipal);
        notificationService.markNotifications(currentUser, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = getCurrentUser(userPrincipal);
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a single notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        
        User currentUser = getCurrentUser(userPrincipal);
        notificationService.deleteNotification(currentUser, notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete multiple notifications
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DeleteNotificationRequest request) {
        
        User currentUser = getCurrentUser(userPrincipal);
        notificationService.deleteNotifications(currentUser, request.getNotificationIds());
        return ResponseEntity.ok().build();
    }

    // Admin/Teacher endpoints

    /**
     * Create a notification for a specific user
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Send notification to all students in a course
     */
    @PostMapping("/course/{courseId}")
    public ResponseEntity<Void> sendNotificationToCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateNotificationRequest request) {
        
        notificationService.sendNotificationToCourse(courseId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Send notification to all users with a specific role
     */
    @PostMapping("/role/{roleType}")
    public ResponseEntity<Void> sendNotificationByRole(
            @PathVariable String roleType,
            @Valid @RequestBody CreateNotificationRequest request) {
        
        notificationService.sendNotificationByRole(roleType, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Send notification to multiple users
     */
    @PostMapping("/bulk")
    public ResponseEntity<Void> createNotificationsForUsers(
            @Valid @RequestBody CreateNotificationRequest request) {
        
        notificationService.createNotificationsForUsers(request);
        return ResponseEntity.ok().build();
    }

    // Admin endpoints

    /**
     * Clean up old notifications
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldNotifications(
            @RequestParam(value = "daysToKeep", defaultValue = "90") int daysToKeep) {
        
        notificationService.cleanupOldNotifications(daysToKeep);
        return ResponseEntity.ok().build();
    }
}