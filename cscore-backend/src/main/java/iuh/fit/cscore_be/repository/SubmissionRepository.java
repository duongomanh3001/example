package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    
    List<Submission> findByAssignment(Assignment assignment);
    
    Page<Submission> findByAssignment(Assignment assignment, Pageable pageable);
    
    List<Submission> findByStudent(User student);
    
    List<Submission> findByStudentOrderBySubmissionTimeDesc(User student);
    
    Page<Submission> findByStudent(User student, Pageable pageable);
    
    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);
    
    List<Submission> findByStatus(SubmissionStatus status);
    
    List<Submission> findByAssignmentId(Long assignmentId);
    
    Page<Submission> findByAssignmentId(Long assignmentId, Pageable pageable);
    
    List<Submission> findByStatusIn(List<SubmissionStatus> statuses);
    
    Page<Submission> findByStatusIn(List<SubmissionStatus> statuses, Pageable pageable);
    
    @Query("SELECT s FROM Submission s WHERE s.assignment.course.teacher = :teacher")
    List<Submission> findByTeacher(@Param("teacher") User teacher);
    
    @Query("SELECT s FROM Submission s WHERE s.assignment.course.teacher = :teacher")
    Page<Submission> findByTeacher(@Param("teacher") User teacher, Pageable pageable);
    
    @Query("SELECT s FROM Submission s WHERE s.assignment.course.teacher = :teacher AND s.status = :status")
    List<Submission> findByTeacherAndStatus(@Param("teacher") User teacher, @Param("status") SubmissionStatus status);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment = :assignment")
    Long countByAssignment(@Param("assignment") Assignment assignment);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.course.teacher = :teacher AND s.status = 'SUBMITTED'")
    Long countPendingSubmissionsByTeacher(@Param("teacher") User teacher);
    
    @Query("SELECT AVG(s.score) FROM Submission s WHERE s.assignment = :assignment AND s.score IS NOT NULL")
    Double getAverageScoreByAssignment(@Param("assignment") Assignment assignment);
    
    // Additional methods for enhanced functionality
    Optional<Submission> findByIdAndStudent(Long id, User student);
    
    @Query("SELECT s FROM Submission s WHERE s.student = :student AND s.assignment = :assignment ORDER BY s.submissionTime DESC")
    Page<Submission> findByStudentAndAssignment(@Param("student") User student, @Param("assignment") Assignment assignment, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.student = :student AND s.assignment = :assignment")
    Long countByStudentAndAssignment(@Param("student") User student, @Param("assignment") Assignment assignment);
    
    @Query("SELECT s FROM Submission s WHERE s.assignment.id = :assignmentId AND s.student.id = :studentId ORDER BY s.submissionTime DESC")
    Page<Submission> findByAssignmentIdAndStudentId(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.id = :assignmentId AND s.status = :status")
    Long countByAssignmentIdAndStatus(@Param("assignmentId") Long assignmentId, @Param("status") SubmissionStatus status);
    
    // Additional methods for admin functionality  
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.submissionTime >= :date")
    Long countSubmissionsAfter(@Param("date") java.time.LocalDateTime date);
    
    // Missing methods for assignment management
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment = :assignment AND s.status = 'SUBMITTED'")
    Long countByAssignmentAndStatusSubmitted(@Param("assignment") Assignment assignment);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment = :assignment AND s.status = 'GRADED'")
    Long countByAssignmentAndStatusGraded(@Param("assignment") Assignment assignment);
    
    @Query("SELECT AVG(s.score) FROM Submission s WHERE s.assignment = :assignment AND s.score IS NOT NULL")
    Double calculateAverageScoreByAssignment(@Param("assignment") Assignment assignment);
    
    @Query("SELECT AVG(s.score) FROM Submission s WHERE s.assignment.course.teacher = :teacher AND s.score IS NOT NULL")
    Double calculateAverageScoreByTeacher(@Param("teacher") User teacher);
}
