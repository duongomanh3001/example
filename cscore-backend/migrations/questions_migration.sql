-- Migration to add Questions and QuestionOptions tables
-- Run this script to update your database schema

-- Create questions table
CREATE TABLE questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    question_type VARCHAR(20) NOT NULL,
    points DOUBLE NOT NULL,
    order_index INT,
    assignment_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE
);

-- Create question_options table for multiple choice questions
CREATE TABLE question_options (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    option_order INT,
    question_id BIGINT NOT NULL,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Update test_cases table to reference questions instead of assignments
ALTER TABLE test_cases 
DROP FOREIGN KEY test_cases_ibfk_1;

ALTER TABLE test_cases 
DROP COLUMN assignment_id,
ADD COLUMN question_id BIGINT NOT NULL;

ALTER TABLE test_cases 
ADD FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

-- Create indexes for better performance
CREATE INDEX idx_questions_assignment_id ON questions(assignment_id);
CREATE INDEX idx_questions_order_index ON questions(order_index);
CREATE INDEX idx_question_options_question_id ON question_options(question_id);
CREATE INDEX idx_test_cases_question_id ON test_cases(question_id);
