package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    
    List<Section> findByCourseOrderByOrderIndexAsc(Course course);
    
    List<Section> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    
    void deleteByCourseId(Long courseId);
}
