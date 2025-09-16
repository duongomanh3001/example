-- Fix submission status column to support new enum values
-- This script will expand the status column to accommodate longer enum names

USE cscoredb;

-- Check current column definition
DESCRIBE submissions;

-- Alter the status column to accommodate longer enum values
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
) NOT NULL DEFAULT 'NOT_SUBMITTED';

-- Verify the change
DESCRIBE submissions;

-- Show current submissions
SELECT id, student_id, assignment_id, status, score, feedback 
FROM submissions 
ORDER BY submission_time DESC 
LIMIT 10;
