-- Notification System Migration for Hibernate
-- This will be automatically executed by Hibernate ddl-auto=update

-- Note: Hibernate will create these tables automatically based on @Entity annotations
-- But we need to ensure proper indexes and constraints

-- If tables already exist, these statements will be ignored
-- If you need to run manually:
-- USE cscoredb;

-- CREATE TABLE IF NOT EXISTS notifications (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     title VARCHAR(255) NOT NULL,
--     message TEXT NOT NULL,
--     type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR') NOT NULL,
--     category ENUM('ASSIGNMENT', 'COURSE', 'SUBMISSION', 'GRADING', 'SYSTEM', 'ANNOUNCEMENT') NOT NULL,
--     is_read BOOLEAN NOT NULL DEFAULT FALSE,
--     user_id BIGINT NOT NULL,
--     related_entity_id BIGINT NULL,
--     related_entity_type VARCHAR(100) NULL,
--     action_url VARCHAR(500) NULL,
--     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
--     
--     INDEX idx_notifications_user_id (user_id),
--     INDEX idx_notifications_is_read (is_read),
--     INDEX idx_notifications_type (type),
--     INDEX idx_notifications_category (category),
--     INDEX idx_notifications_created_at (created_at),
--     INDEX idx_notifications_user_read (user_id, is_read),
--     INDEX idx_notifications_user_created (user_id, created_at DESC)
-- );

-- Tables will be created automatically by Hibernate based on @Entity annotations
-- This is just documentation of the expected structure