package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    
    List<QuestionOption> findByQuestionOrderByOptionOrderAsc(Question question);
    
    List<QuestionOption> findByQuestionIdOrderByOptionOrderAsc(Long questionId);
    
    void deleteByQuestionId(Long questionId);
}
