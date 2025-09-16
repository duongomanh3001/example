package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.TestResult;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    
    List<TestResult> findBySubmission(Submission submission);
    
    List<TestResult> findByTestCase(TestCase testCase);
    
    List<TestResult> findBySubmissionAndTestCase(Submission submission, TestCase testCase);
    
    List<TestResult> findBySubmissionAndIsPassedTrue(Submission submission);
    
    List<TestResult> findBySubmissionAndIsPassedFalse(Submission submission);
    
    Long countBySubmissionAndIsPassed(Submission submission, Boolean isPassed);
}
