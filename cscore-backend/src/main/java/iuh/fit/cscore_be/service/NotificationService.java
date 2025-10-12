package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateNotificationRequest;
import iuh.fit.cscore_be.dto.request.MarkNotificationRequest;
import iuh.fit.cscore_be.dto.response.NotificationPageResponse;
import iuh.fit.cscore_be.dto.response.NotificationResponse;
import iuh.fit.cscore_be.dto.response.NotificationStatsResponse;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Enrollment;
import iuh.fit.cscore_be.entity.Notification;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.NotificationCategory;
import iuh.fit.cscore_be.enums.NotificationType;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.repository.AssignmentRepository;
import iuh.fit.cscore_be.repository.CourseRepository;
import iuh.fit.cscore_be.repository.EnrollmentRepository;
import iuh.fit.cscore_be.repository.NotificationRepository;
import iuh.fit.cscore_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Get notifications for current user with filters
     */
    public NotificationPageResponse getNotifications(
            User currentUser,
            Boolean isRead,
            NotificationType type,
            NotificationCategory category,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            int page,
            int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository.findByUserWithFilters(
                currentUser, isRead, type, category, dateFrom, dateTo, pageable
        );

        // Convert to response DTOs
        Page<NotificationResponse> responsePage = notificationPage.map(NotificationResponse::from);
        
        // Get unread count
        long unreadCount = notificationRepository.countByUserAndIsReadFalse(currentUser);
        
        return NotificationPageResponse.from(responsePage, unreadCount);
    }

    /**
     * Get notification statistics for current user
     */
    public NotificationStatsResponse getNotificationStats(User currentUser) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        
        Object[] stats = notificationRepository.getNotificationStats(currentUser, todayStart);
        
        if (stats != null && stats.length >= 3) {
            long totalCount = ((Number) stats[0]).longValue();
            long unreadCount = ((Number) stats[1]).longValue();
            long todayCount = ((Number) stats[2]).longValue();
            
            // Calculate week count
            long weekCount = notificationRepository.countByUserAndDateRange(
                    currentUser, weekStart, LocalDateTime.now()
            );
            
            return new NotificationStatsResponse(totalCount, unreadCount, todayCount, weekCount);
        }
        
        return new NotificationStatsResponse(0, 0, 0, 0);
    }

    /**
     * Get unread notification count
     */
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countByUserAndIsReadFalse(currentUser);
    }

    /**
     * Mark notifications as read/unread
     */
    public void markNotifications(User currentUser, MarkNotificationRequest request) {
        notificationRepository.updateReadStatusByIds(
                request.getNotificationIds(),
                request.getIsRead(),
                currentUser
        );
    }

    /**
     * Mark all notifications as read for current user
     */
    public void markAllAsRead(User currentUser) {
        notificationRepository.markAllAsReadForUser(currentUser);
    }

    /**
     * Delete a notification (only if it belongs to current user)
     */
    public void deleteNotification(User currentUser, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to delete this notification");
        }
        
        notificationRepository.delete(notification);
    }

    /**
     * Delete multiple notifications
     */
    public void deleteNotifications(User currentUser, List<Long> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        
        // Verify all notifications belong to current user
        for (Notification notification : notifications) {
            if (!notification.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You don't have permission to delete some notifications");
            }
        }
        
        notificationRepository.deleteAll(notifications);
    }

    /**
     * Get notification by ID (only if it belongs to current user)
     */
    public NotificationResponse getNotificationById(User currentUser, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to view this notification");
        }
        
        // Mark as read if not already read
        if (!notification.getIsRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
        
        return NotificationResponse.from(notification);
    }

    /**
     * Create a single notification for a specific user
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Notification notification = new Notification(
                request.getTitle(),
                request.getMessage(),
                request.getType(),
                request.getCategory(),
                targetUser
        );
        
        notification.setRelatedEntityId(request.getRelatedEntityId());
        notification.setRelatedEntityType(request.getRelatedEntityType());
        notification.setActionUrl(request.getActionUrl());
        
        notification = notificationRepository.save(notification);
        
        // TODO: Send real-time notification via WebSocket
        
        return NotificationResponse.from(notification);
    }

    /**
     * Send notification to multiple users
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public void createNotificationsForUsers(CreateNotificationRequest request) {
        List<User> targetUsers = userRepository.findAllById(request.getUserIds());
        
        List<Notification> notifications = new ArrayList<>();
        for (User user : targetUsers) {
            Notification notification = new Notification(
                    request.getTitle(),
                    request.getMessage(),
                    request.getType(),
                    request.getCategory(),
                    user
            );
            
            notification.setRelatedEntityId(request.getRelatedEntityId());
            notification.setRelatedEntityType(request.getRelatedEntityType());
            notification.setActionUrl(request.getActionUrl());
            
            notifications.add(notification);
        }
        
        notificationRepository.saveAll(notifications);
        
        // TODO: Send real-time notifications via WebSocket
    }

    /**
     * Send notification to all students in a course
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public void sendNotificationToCourse(Long courseId, CreateNotificationRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        List<User> students = course.getEnrollments().stream()
                .filter(enrollment -> enrollment.getIsActive())
                .map(enrollment -> enrollment.getStudent())
                .collect(Collectors.toList());
        
        List<Notification> notifications = new ArrayList<>();
        for (User student : students) {
            Notification notification = new Notification(
                    request.getTitle(),
                    request.getMessage(),
                    request.getType(),
                    request.getCategory(),
                    student
            );
            
            notification.setRelatedEntityId(courseId);
            notification.setRelatedEntityType("COURSE");
            notification.setActionUrl(request.getActionUrl());
            
            notifications.add(notification);
        }
        
        notificationRepository.saveAll(notifications);
        
        // TODO: Send real-time notifications via WebSocket
    }

    /**
     * Send notification to all users with a specific role
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void sendNotificationByRole(String roleType, CreateNotificationRequest request) {
        Role role = Role.valueOf(roleType);
        List<User> users = userRepository.findByRole(role);
        
        List<Notification> notifications = new ArrayList<>();
        for (User user : users) {
            Notification notification = new Notification(
                    request.getTitle(),
                    request.getMessage(),
                    request.getType(),
                    request.getCategory(),
                    user
            );
            
            notification.setRelatedEntityId(request.getRelatedEntityId());
            notification.setRelatedEntityType(request.getRelatedEntityType());
            notification.setActionUrl(request.getActionUrl());
            
            notifications.add(notification);
        }
        
        notificationRepository.saveAll(notifications);
        
        // TODO: Send real-time notifications via WebSocket
    }

    /**
     * Create notification for teacher when they create a new assignment
     */
    public void createTeacherAssignmentNotification(User teacher, Long assignmentId, String assignmentTitle, Course course) {
        Notification notification = new Notification(
                "Bài tập đã tạo thành công: " + assignmentTitle,
                "Bạn vừa tạo bài tập mới \"" + assignmentTitle + "\" trong khóa học " + course.getName() + ". Sinh viên sẽ nhận được thông báo.",
                NotificationType.SUCCESS,
                NotificationCategory.ASSIGNMENT,
                teacher
        );
        
        notification.setRelatedEntityId(assignmentId);
        notification.setRelatedEntityType("ASSIGNMENT");
        notification.setActionUrl("/teacher/assignment/" + assignmentId);
        
        notificationRepository.save(notification);
        
        // TODO: Send real-time notifications via WebSocket
    }
    
    /**
     * Create notification for students when new assignment is created
     */
    public void createStudentAssignmentNotification(Long assignmentId, String assignmentTitle, Course course) {
        List<User> students = course.getEnrollments().stream()
                .filter(enrollment -> enrollment.getIsActive())
                .map(enrollment -> enrollment.getStudent())
                .filter(student -> student.getRole() == Role.STUDENT) // Only include actual students, not teachers
                .collect(Collectors.toList());
        
        List<Notification> notifications = new ArrayList<>();
        for (User student : students) {
            Notification notification = new Notification(
                    "Bài tập mới: " + assignmentTitle,
                    "Giảng viên vừa tạo bài tập mới trong khóa học " + course.getName() + ". Hãy kiểm tra và hoàn thành đúng thời hạn.",
                    NotificationType.INFO,
                    NotificationCategory.ASSIGNMENT,
                    student
            );
            
            notification.setRelatedEntityId(assignmentId);
            notification.setRelatedEntityType("ASSIGNMENT");
            notification.setActionUrl("/student/course/" + course.getId() + "/assignment/" + assignmentId);
            
            notifications.add(notification);
        }
        
        notificationRepository.saveAll(notifications);
        
        // TODO: Send real-time notifications via WebSocket
    }

    /**
     * Create notification when assignment is graded
     */
    public void createGradingNotification(User student, Long assignmentId, String assignmentTitle, Double score) {
        Notification notification = new Notification(
                "Bài tập đã được chấm điểm: " + assignmentTitle,
                String.format("Bài tập của bạn đã được chấm điểm. Điểm số: %.2f", score),
                NotificationType.SUCCESS,
                NotificationCategory.GRADING,
                student
        );
        
        notification.setRelatedEntityId(assignmentId);
        notification.setRelatedEntityType("ASSIGNMENT");
        notification.setActionUrl("/student/course/" + getCourseIdFromAssignmentId(assignmentId) + "/assignment/" + assignmentId);
        
        notificationRepository.save(notification);
        
        // TODO: Send real-time notification via WebSocket
    }

    /**
     * Clean up old notifications (can be scheduled)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void cleanupOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        notificationRepository.deleteOldNotifications(cutoffDate);
    }

    /**
     * Send notification about new assignment to all enrolled students
     */
    public void notifyNewAssignment(Assignment assignment) {
        try {
            Course course = assignment.getCourse();
            
            // Get all enrolled students using EnrollmentRepository
            List<Enrollment> enrollments = enrollmentRepository.findByCourseAndIsActiveTrue(course);
            
            List<Notification> notifications = new ArrayList<>();
            for (Enrollment enrollment : enrollments) {
                User student = enrollment.getStudent();
                Notification notification = new Notification(
                        "Bài tập mới: " + assignment.getTitle(),
                        "Bạn có bài tập mới trong khóa học " + course.getName() + 
                        ". Hạn nộp: " + assignment.getEndTime().toString(),
                        NotificationType.INFO,
                        NotificationCategory.ASSIGNMENT,
                        student
                );
                
                notification.setRelatedEntityId(assignment.getId());
                notification.setRelatedEntityType("Assignment");
                notification.setActionUrl("/student/assignments/" + assignment.getId());
                
                notifications.add(notification);
            }
            
            notificationRepository.saveAll(notifications);
            
            // TODO: Send real-time notifications via WebSocket
        } catch (Exception e) {
            // Log the error but don't throw to avoid breaking assignment creation
            System.err.println("Failed to send assignment notifications: " + e.getMessage());
        }
    }

    /**
     * Helper method to get courseId from assignmentId
     */
    private Long getCourseIdFromAssignmentId(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        return assignment.getCourse().getId();
    }
}