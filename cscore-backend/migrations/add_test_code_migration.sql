-- Add testCode column to test_cases table
-- This migration supports the BubbleSort fix for combining student code with test execution logic

ALTER TABLE test_cases ADD COLUMN test_code TEXT;

-- Add comment explaining the purpose
COMMENT ON COLUMN test_cases.test_code IS 'Test code (main function) to execute and test the student function';