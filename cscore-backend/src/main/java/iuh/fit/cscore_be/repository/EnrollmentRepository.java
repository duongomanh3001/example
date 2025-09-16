package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Enrollment;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    List<Enrollment> findByCourseAndIsActiveTrue(Course course);
    
    List<Enrollment> findByStudentAndIsActiveTrue(User student);
    
    List<Enrollment> findByCourseOrderByEnrollmentDateDesc(Course course);
    
    Page<Enrollment> findByCourseAndIsActiveTrue(Course course, Pageable pageable);
    
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);
    
    Optional<Enrollment> findByCourseAndStudent(Course course, User student);
    
    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);
    
    Boolean existsByStudentAndCourse(User student, Course course);
    
    Boolean existsByCourseAndStudent(Course course, User student);
    
    @Query("SELECT e FROM Enrollment e WHERE e.course.teacher = :teacher AND e.isActive = true")
    List<Enrollment> findAllByTeacher(@Param("teacher") User teacher);
    
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course = :course AND e.isActive = true")
    Long countByCourseAndIsActiveTrue(@Param("course") Course course);
    
    // New methods for admin course management
    Long countByCourse(Course course);
}
