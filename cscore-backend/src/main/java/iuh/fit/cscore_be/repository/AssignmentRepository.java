package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.enums.AssignmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    List<Assignment> findByCourseAndIsActiveTrue(Course course);
    
    List<Assignment> findByCourseAndIsActiveTrueOrderByCreatedAtDesc(Course course);
    
    Page<Assignment> findByCourseAndIsActiveTrue(Course course, Pageable pageable);
    
    @Query("SELECT a FROM Assignment a WHERE a.course.teacher.id = :teacherId AND a.isActive = true")
    List<Assignment> findByTeacherId(@Param("teacherId") Long teacherId);
    
    @Query("SELECT a FROM Assignment a WHERE a.course.teacher.id = :teacherId AND a.isActive = true")
    Page<Assignment> findByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);
    
    @Query("SELECT a FROM Assignment a WHERE a.course.teacher.id = :teacherId AND a.type = :type AND a.isActive = true")
    List<Assignment> findByTeacherIdAndType(@Param("teacherId") Long teacherId, @Param("type") AssignmentType type);
    
    @Query("SELECT a FROM Assignment a WHERE a.endTime < :now AND a.isActive = true")
    List<Assignment> findOverdueAssignments(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment = :assignment")
    Long countSubmissionsByAssignment(@Param("assignment") Assignment assignment);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment = :assignment AND s.status = 'SUBMITTED'")
    Long countPendingSubmissionsByAssignment(@Param("assignment") Assignment assignment);
    
    // New methods for teacher assignment management
    List<Assignment> findByCourseInAndIsActiveTrueOrderByCreatedAtDesc(List<Course> courses);
    
    Page<Assignment> findByCourseInAndIsActiveTrue(List<Course> courses, Pageable pageable);
}
