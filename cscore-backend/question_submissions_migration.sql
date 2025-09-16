-- Migration script to add question-level submissions support
-- This allows grading individual programming questions separately

-- Create question_submissions table for individual question responses
CREATE TABLE IF NOT EXISTS question_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    code TEXT,
    programming_language VARCHAR(50),
    score DOUBLE DEFAULT 0,
    status ENUM('NOT_SUBMITTED', 'SUBMITTED', 'GRADING', 'GRADED', 'PASSED', 'PARTIAL', 'FAILED', 'COMPILATION_ERROR', 'ERROR', 'NO_TESTS') DEFAULT 'NOT_SUBMITTED',
    execution_time BIGINT,
    memory_used BIGINT,
    feedback TEXT,
    graded_time DATETIME(6),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_submission_question (submission_id, question_id),
    INDEX idx_submission_question (submission_id, question_id)
);

-- Add question_id column to test_results for better tracking
ALTER TABLE test_results ADD COLUMN question_submission_id BIGINT NULL AFTER submission_id;
ALTER TABLE test_results ADD FOREIGN KEY (question_submission_id) REFERENCES question_submissions(id) ON DELETE CASCADE;

-- Update submissions table to support mixed question types
ALTER TABLE submissions 
MODIFY COLUMN code TEXT NULL COMMENT 'Legacy field for backward compatibility',
ADD COLUMN total_questions INT DEFAULT 0,
ADD COLUMN completed_questions INT DEFAULT 0,
ADD COLUMN has_programming_questions BOOLEAN DEFAULT FALSE;

-- Create indexes for better performance
CREATE INDEX idx_question_submissions_question ON question_submissions(question_id);
CREATE INDEX idx_question_submissions_status ON question_submissions(status);
CREATE INDEX idx_question_submissions_score ON question_submissions(score);
