-- Database Cleanup Migration Script
-- Removes unused columns and optimizes database structure
-- Run this after updating the entity classes

-- Backup current data first (uncomment if needed)
-- CREATE TABLE submissions_backup AS SELECT * FROM submissions;
-- CREATE TABLE test_results_backup AS SELECT * FROM test_results;

-- 1. Remove unused columns from submissions table
ALTER TABLE submissions 
DROP COLUMN IF EXISTS code,
DROP COLUMN IF EXISTS programming_language,
DROP COLUMN IF EXISTS memory_used,
DROP COLUMN IF EXISTS total_questions,
DROP COLUMN IF EXISTS completed_questions,
DROP COLUMN IF EXISTS has_programming_questions;

-- 2. Remove unused columns from test_results table
ALTER TABLE test_results 
DROP COLUMN IF EXISTS submission_id,
DROP COLUMN IF EXISTS memory_used;

-- 3. Update test_results foreign key constraint
-- Make question_submission_id NOT NULL since we removed submission_id
ALTER TABLE test_results 
MODIFY COLUMN question_submission_id BIGINT NOT NULL;

-- 4. Add indexes for better performance on frequently queried columns
CREATE INDEX IF NOT EXISTS idx_submissions_student_assignment 
ON submissions(student_id, assignment_id);

CREATE INDEX IF NOT EXISTS idx_submissions_status 
ON submissions(status);

CREATE INDEX IF NOT EXISTS idx_question_submissions_student_question 
ON question_submissions(student_id, question_id);

CREATE INDEX IF NOT EXISTS idx_question_submissions_final 
ON question_submissions(is_final_submission);

CREATE INDEX IF NOT EXISTS idx_test_results_question_submission 
ON test_results(question_submission_id);

CREATE INDEX IF NOT EXISTS idx_test_cases_question 
ON test_cases(question_id);

-- 5. Update any existing data constraints
-- Ensure question_submission_id is properly linked for existing test_results
UPDATE test_results tr 
SET question_submission_id = (
    SELECT qs.id 
    FROM question_submissions qs 
    WHERE qs.question_id = (
        SELECT tc.question_id 
        FROM test_cases tc 
        WHERE tc.id = tr.test_case_id
    )
    AND qs.student_id = (
        SELECT s.student_id 
        FROM submissions s 
        WHERE s.id = tr.submission_id
    )
    LIMIT 1
)
WHERE question_submission_id IS NULL;

-- 6. Clean up orphaned records
-- Remove test results that don't have valid question submissions
DELETE FROM test_results 
WHERE question_submission_id IS NULL;

-- 7. Optimize table storage
OPTIMIZE TABLE submissions;
OPTIMIZE TABLE test_results;
OPTIMIZE TABLE question_submissions;

-- 8. Update application.properties cleanup configuration
-- Add these settings to application.properties:
-- 
-- # Database optimization
-- spring.jpa.hibernate.ddl-auto=validate
-- spring.jpa.properties.hibernate.jdbc.batch_size=25
-- spring.jpa.properties.hibernate.order_inserts=true
-- spring.jpa.properties.hibernate.order_updates=true
-- 
-- # Remove unused Judge0 configuration
-- # grading.use-judge0=false (can be removed)
-- # judge0.url= (can be removed)
-- # judge0.api.key= (can be removed)