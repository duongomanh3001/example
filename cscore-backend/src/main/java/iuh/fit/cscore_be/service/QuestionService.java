package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateQuestionOptionRequest;
import iuh.fit.cscore_be.dto.request.CreateQuestionRequest;
import iuh.fit.cscore_be.dto.request.CreateTestCaseRequest;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.QuestionOption;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.repository.QuestionOptionRepository;
import iuh.fit.cscore_be.repository.QuestionRepository;
import iuh.fit.cscore_be.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final TestCaseRepository testCaseRepository;
    
    public Question createQuestion(CreateQuestionRequest request, Assignment assignment) {
        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setDescription(request.getDescription());
        question.setQuestionType(request.getQuestionType());
        question.setPoints(request.getPoints());
        question.setOrderIndex(request.getOrderIndex());
        question.setAssignment(assignment);
        
        Question savedQuestion = questionRepository.save(question);
        
        // Create test cases for PROGRAMMING questions
        if (request.getTestCases() != null && !request.getTestCases().isEmpty()) {
            for (CreateTestCaseRequest testCaseRequest : request.getTestCases()) {
                createTestCase(testCaseRequest, savedQuestion);
            }
        }
        
        // Create options for MULTIPLE_CHOICE questions
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            for (CreateQuestionOptionRequest optionRequest : request.getOptions()) {
                createQuestionOption(optionRequest, savedQuestion);
            }
        }
        
        return savedQuestion;
    }
    
    private TestCase createTestCase(CreateTestCaseRequest request, Question question) {
        TestCase testCase = new TestCase();
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setIsHidden(request.getIsHidden());
        testCase.setWeight(request.getWeight());
        testCase.setTimeLimit(request.getTimeLimit());
        testCase.setMemoryLimit(request.getMemoryLimit());
        testCase.setQuestion(question);
        
        return testCaseRepository.save(testCase);
    }
    
    private QuestionOption createQuestionOption(CreateQuestionOptionRequest request, Question question) {
        QuestionOption option = new QuestionOption();
        option.setOptionText(request.getOptionText());
        option.setIsCorrect(request.getIsCorrect());
        option.setOptionOrder(request.getOptionOrder());
        option.setQuestion(question);
        
        return questionOptionRepository.save(option);
    }
    
    public List<Question> getQuestionsByAssignment(Long assignmentId) {
        return questionRepository.findByAssignmentIdOrderByOrderIndexAsc(assignmentId);
    }
    
    public Question getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));
    }
    
    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }
    
    public Long countQuestionsByAssignment(Long assignmentId) {
        return questionRepository.countByAssignmentId(assignmentId);
    }
}
