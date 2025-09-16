package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    List<Course> findByTeacherAndIsActiveTrue(User teacher);
    
    Page<Course> findByTeacherAndIsActiveTrue(User teacher, Pageable pageable);
    
    List<Course> findByIsActiveTrueOrderByCreatedAtDesc();
    
    @Query("SELECT c FROM Course c WHERE c.teacher = :teacher AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Course> findRecentCoursesByTeacher(@Param("teacher") User teacher);
    
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course = :course AND e.isActive = true")
    Long countActiveStudentsInCourse(@Param("course") Course course);
    
    Boolean existsByCodeAndTeacher(String code, User teacher);
    
    Boolean existsByCode(String code);
    
    // New methods for admin course management
    Boolean existsByCodeAndIsActiveTrue(String code);
    
    @Query("SELECT c FROM Course c WHERE c.isActive = true ORDER BY c.createdAt DESC")
    List<Course> findAllActiveCourses();
}
