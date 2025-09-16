package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByAssignmentOrderByOrderIndexAsc(Assignment assignment);
    
    List<Question> findByAssignmentIdOrderByOrderIndexAsc(Long assignmentId);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.assignment.id = :assignmentId")
    Long countByAssignmentId(@Param("assignmentId") Long assignmentId);
    
    void deleteByAssignmentId(Long assignmentId);
}
