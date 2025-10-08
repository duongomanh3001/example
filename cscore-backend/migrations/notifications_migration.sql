-- Migration for Notification System
-- Create notifications table

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR') NOT NULL,
    category ENUM('ASSIGNMENT', 'COURSE', 'SUBMISSION', 'GRADING', 'SYSTEM', 'ANNOUNCEMENT') NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT NOT NULL,
    related_entity_id BIGINT NULL,
    related_entity_type VARCHAR(100) NULL,
    action_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_notifications_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes for better performance
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_is_read (is_read),
    INDEX idx_notifications_type (type),
    INDEX idx_notifications_category (category),
    INDEX idx_notifications_created_at (created_at),
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_user_created (user_id, created_at DESC),
    INDEX idx_notifications_related_entity (related_entity_id, related_entity_type)
);

-- Create notification_preferences table for user notification settings
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    enable_email BOOLEAN NOT NULL DEFAULT TRUE,
    enable_push BOOLEAN NOT NULL DEFAULT TRUE,
    enable_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Category-specific preferences (JSON format for flexibility)
    assignment_email BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_push BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    
    course_email BOOLEAN NOT NULL DEFAULT TRUE,
    course_push BOOLEAN NOT NULL DEFAULT TRUE,
    course_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    
    submission_email BOOLEAN NOT NULL DEFAULT TRUE,
    submission_push BOOLEAN NOT NULL DEFAULT TRUE,
    submission_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    
    grading_email BOOLEAN NOT NULL DEFAULT TRUE,
    grading_push BOOLEAN NOT NULL DEFAULT TRUE,
    grading_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    
    system_email BOOLEAN NOT NULL DEFAULT TRUE,
    system_push BOOLEAN NOT NULL DEFAULT FALSE,
    system_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    
    announcement_email BOOLEAN NOT NULL DEFAULT TRUE,
    announcement_push BOOLEAN NOT NULL DEFAULT TRUE,
    announcement_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_notification_preferences_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert default notification preferences for existing users
INSERT INTO notification_preferences (user_id, enable_email, enable_push, enable_in_app)
SELECT id, TRUE, TRUE, TRUE 
FROM users 
WHERE id NOT IN (SELECT user_id FROM notification_preferences);

-- Add some sample notifications for testing (optional)
INSERT INTO notifications (title, message, type, category, user_id, action_url) 
SELECT 
    'Chào mừng bạn đến với CScore!',
    'Hệ thống chấm điểm tự động CScore sẽ giúp bạn quản lý và theo dõi kết quả học tập một cách hiệu quả.',
    'INFO',
    'SYSTEM',
    id,
    '/dashboard'
FROM users 
WHERE role IN ('STUDENT', 'TEACHER')
LIMIT 5;