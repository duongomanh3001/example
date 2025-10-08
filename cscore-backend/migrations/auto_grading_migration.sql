-- Auto-Grading System Database Migration Script
-- Updates for enhanced automatic code grading functionality
-- Run this script to add new columns and update existing tables

-- Update submissions table to add new status values and fields
ALTER TABLE submissions 
ADD COLUMN IF NOT EXISTS compilation_error TEXT,
ADD COLUMN IF NOT EXISTS auto_graded BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS max_execution_time BIGINT DEFAULT 30000,
ADD COLUMN IF NOT EXISTS max_memory_usage BIGINT DEFAULT 268435456;

-- Update test_results table to add more detailed execution information
ALTER TABLE test_results 
ADD COLUMN IF NOT EXISTS compilation_output TEXT,
ADD COLUMN IF NOT EXISTS runtime_error TEXT,
ADD COLUMN IF NOT EXISTS timeout BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS memory_limit_exceeded BOOLEAN DEFAULT FALSE;

-- Update assignments table to add auto-grading configuration
ALTER TABLE assignments 
ADD COLUMN IF NOT EXISTS auto_grade BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS allow_multiple_submissions BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS max_submissions INTEGER DEFAULT 10,
ADD COLUMN IF NOT EXISTS show_test_cases BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS immediate_feedback BOOLEAN DEFAULT TRUE;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions(status);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_student ON submissions(assignment_id, student_id);
CREATE INDEX IF NOT EXISTS idx_test_results_submission ON test_results(submission_id);
CREATE INDEX IF NOT EXISTS idx_test_results_passed ON test_results(is_passed);

-- Update existing submission statuses to new enum values
UPDATE submissions SET status = 'SUBMITTED' WHERE status = 'NOT_GRADED';
UPDATE submissions SET status = 'GRADED' WHERE status = 'GRADED' AND score IS NOT NULL;

-- Add comments to document the new columns
ALTER TABLE submissions 
MODIFY COLUMN status ENUM(
    'NOT_SUBMITTED',
    'SUBMITTED', 
    'GRADING',
    'GRADED',
    'PASSED',
    'PARTIAL',
    'FAILED',
    'COMPILATION_ERROR',
    'ERROR',
    'NO_TESTS',
    'LATE'
) DEFAULT 'NOT_SUBMITTED';

-- Create system_config table for storing auto-grading configuration
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default auto-grading configurations
INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('auto_grading_enabled', 'true', 'Enable automatic grading for all submissions'),
('max_execution_time', '30', 'Maximum execution time in seconds for code execution'),
('max_memory_limit', '256', 'Maximum memory limit in MB for code execution'),
('compilation_timeout', '60', 'Maximum compilation time in seconds'),
('max_output_length', '10000', 'Maximum length of program output in characters'),
('supported_languages', 'JAVA,PYTHON,C,CPP', 'List of supported programming languages'),
('concurrent_executions', '10', 'Maximum number of concurrent code executions'),
('cleanup_temp_files', 'true', 'Automatically cleanup temporary files after execution'),
('judge0_enabled', 'false', 'Use Judge0 API for code execution instead of local execution'),
('detailed_feedback', 'true', 'Provide detailed feedback for failed test cases');

-- Create compiler_status table to track available compilers
CREATE TABLE IF NOT EXISTS compiler_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    language ENUM('JAVA', 'PYTHON', 'C', 'CPP', 'JAVASCRIPT') NOT NULL,
    compiler_path VARCHAR(500),
    version VARCHAR(100),
    available BOOLEAN DEFAULT FALSE,
    last_checked TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    UNIQUE KEY unique_language (language)
);

-- Create grading_queue table for managing auto-grading tasks
CREATE TABLE IF NOT EXISTS grading_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    priority INTEGER DEFAULT 5,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    INDEX idx_grading_status (status),
    INDEX idx_grading_priority (priority),
    INDEX idx_grading_created (created_at)
);

-- Create execution_logs table for debugging and monitoring
CREATE TABLE IF NOT EXISTS execution_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    test_case_id BIGINT,
    language VARCHAR(20) NOT NULL,
    execution_time BIGINT,
    memory_used BIGINT,
    exit_code INTEGER,
    stdout_output TEXT,
    stderr_output TEXT,
    compiler_output TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE,
    INDEX idx_execution_submission (submission_id),
    INDEX idx_execution_language (language),
    INDEX idx_execution_created (created_at)
);

-- Update test_cases table to add more configuration options
ALTER TABLE test_cases 
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT 'BASIC',
ADD COLUMN IF NOT EXISTS points DOUBLE DEFAULT 1.0,
ADD COLUMN IF NOT EXISTS custom_time_limit INTEGER,
ADD COLUMN IF NOT EXISTS custom_memory_limit INTEGER;

-- Create performance statistics view
CREATE OR REPLACE VIEW submission_statistics AS
SELECT 
    a.id as assignment_id,
    a.title as assignment_title,
    COUNT(s.id) as total_submissions,
    COUNT(CASE WHEN s.status IN ('PASSED', 'GRADED') THEN 1 END) as passed_submissions,
    COUNT(CASE WHEN s.status = 'FAILED' THEN 1 END) as failed_submissions,
    COUNT(CASE WHEN s.status = 'COMPILATION_ERROR' THEN 1 END) as compilation_errors,
    COUNT(CASE WHEN s.status IN ('SUBMITTED', 'GRADING') THEN 1 END) as pending_submissions,
    AVG(CASE WHEN s.score IS NOT NULL THEN s.score END) as average_score,
    AVG(s.execution_time) as average_execution_time,
    MAX(s.execution_time) as max_execution_time,
    MIN(s.execution_time) as min_execution_time
FROM assignments a
LEFT JOIN submissions s ON a.id = s.assignment_id
GROUP BY a.id, a.title;

-- Add triggers for automatic timestamp updates
DELIMITER //

CREATE TRIGGER IF NOT EXISTS update_submission_timestamp
    BEFORE UPDATE ON submissions
    FOR EACH ROW
BEGIN
    IF NEW.status != OLD.status THEN
        SET NEW.updated_at = CURRENT_TIMESTAMP;
    END IF;
END //

CREATE TRIGGER IF NOT EXISTS update_grading_queue_timestamp
    BEFORE UPDATE ON grading_queue
    FOR EACH ROW
BEGIN
    IF NEW.status = 'PROCESSING' AND OLD.status = 'PENDING' THEN
        SET NEW.started_at = CURRENT_TIMESTAMP;
    ELSEIF NEW.status IN ('COMPLETED', 'FAILED') AND OLD.status = 'PROCESSING' THEN
        SET NEW.completed_at = CURRENT_TIMESTAMP;
    END IF;
END //

DELIMITER ;

-- Insert sample test data for development (optional)
-- INSERT INTO system_config (config_key, config_value, description) VALUES
-- ('dev_mode', 'true', 'Development mode with extended logging'),
-- ('test_data_enabled', 'true', 'Enable test data generation');

COMMIT;
