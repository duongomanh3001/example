package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.QuestionSubmission;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionSubmissionRepository extends JpaRepository<QuestionSubmission, Long> {
    
    List<QuestionSubmission> findBySubmissionId(Long submissionId);
    
    List<QuestionSubmission> findBySubmissionIdOrderByQuestionOrderIndexAsc(Long submissionId);
    
    Optional<QuestionSubmission> findBySubmissionAndQuestion(Submission submission, Question question);
    
    Optional<QuestionSubmission> findByQuestionIdAndStudentId(Long questionId, Long studentId);
    
    List<QuestionSubmission> findBySubmissionAndStatus(Submission submission, SubmissionStatus status);
    
    @Query("SELECT qs FROM QuestionSubmission qs WHERE qs.submission.id = :submissionId AND qs.question.questionType = 'PROGRAMMING'")
    List<QuestionSubmission> findProgrammingQuestionsBySubmissionId(@Param("submissionId") Long submissionId);
    
    @Query("SELECT COUNT(qs) FROM QuestionSubmission qs WHERE qs.submission.id = :submissionId")
    long countBySubmissionId(@Param("submissionId") Long submissionId);
    
    @Query("SELECT COUNT(qs) FROM QuestionSubmission qs WHERE qs.submission.id = :submissionId AND qs.status IN ('PASSED', 'GRADED', 'PARTIAL')")
    long countCompletedBySubmissionId(@Param("submissionId") Long submissionId);
}
