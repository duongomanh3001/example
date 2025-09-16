package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    
    List<TestCase> findByQuestion(Question question);
    
    List<TestCase> findByQuestionAndIsHiddenFalse(Question question);
    
    List<TestCase> findByQuestionAndIsHiddenTrue(Question question);
    
    Long countByQuestion(Question question);
}
