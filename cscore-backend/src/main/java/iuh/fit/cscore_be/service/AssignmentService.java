package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.AssignmentRequest;
import iuh.fit.cscore_be.dto.request.CreateAssignmentRequest;
import iuh.fit.cscore_be.dto.request.TestCaseRequest;
import iuh.fit.cscore_be.dto.response.AssignmentResponse;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.repository.AssignmentRepository;
import iuh.fit.cscore_be.repository.CourseRepository;
import iuh.fit.cscore_be.repository.SubmissionRepository;
import iuh.fit.cscore_be.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {
    
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final QuestionService questionService;
    
    public AssignmentResponse createAssignment(AssignmentRequest request, User teacher) {
        Course course = courseRepository.findById(request.getCourseId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền tạo bài tập cho khóa học này");
        }
        
        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setRequirements(request.getRequirements());
        assignment.setType(request.getType());
        assignment.setCourse(course);
        assignment.setMaxScore(request.getMaxScore());
        assignment.setTimeLimit(request.getTimeLimit());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        assignment.setAutoGrade(request.getAutoGrade());
        assignment.setIsActive(true); // Explicitly set to true to ensure visibility
        
        Assignment savedAssignment = assignmentRepository.save(assignment);
        return convertToResponse(savedAssignment);
    }
    
    @Transactional
    public AssignmentResponse createAssignmentWithQuestions(CreateAssignmentRequest request, User teacher) {
        Course course = courseRepository.findById(request.getCourseId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền tạo bài tập cho khóa học này");
        }
        
        // Create assignment
        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setRequirements(request.getRequirements());
        assignment.setType(request.getType());
        assignment.setCourse(course);
        assignment.setMaxScore(request.getMaxScore());
        assignment.setTimeLimit(request.getTimeLimit());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        assignment.setAutoGrade(request.getAutoGrade());
        assignment.setIsActive(true);
        
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        // Create questions if provided
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (int i = 0; i < request.getQuestions().size(); i++) {
                var questionRequest = request.getQuestions().get(i);
                questionRequest.setOrderIndex(i); // Ensure proper ordering
                questionService.createQuestion(questionRequest, savedAssignment);
            }
        }
        
        // Force reload to get questions
        savedAssignment = assignmentRepository.findById(savedAssignment.getId()).orElse(savedAssignment);
        
        return convertToResponse(savedAssignment);
    }
    
    public AssignmentResponse updateAssignment(Long assignmentId, AssignmentRequest request, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bài tập này");
        }
        
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setRequirements(request.getRequirements());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setTimeLimit(request.getTimeLimit());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        assignment.setAutoGrade(request.getAutoGrade());
        
        Assignment savedAssignment = assignmentRepository.save(assignment);
        return convertToResponse(savedAssignment);
    }
    
    public void deleteAssignment(Long assignmentId, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa bài tập này");
        }
        
        assignment.setIsActive(false);
        assignmentRepository.save(assignment);
    }
    
    public List<AssignmentResponse> getAssignmentsByTeacher(User teacher) {
        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacher.getId());
        return assignments.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public Page<AssignmentResponse> getAssignmentsByTeacher(User teacher, Pageable pageable) {
        Page<Assignment> assignments = assignmentRepository.findByTeacherId(teacher.getId(), pageable);
        return assignments.map(this::convertToResponse);
    }
    
    public List<AssignmentResponse> getAssignmentsByCourse(Long courseId, User teacher) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem bài tập của khóa học này");
        }
        
        List<Assignment> assignments = assignmentRepository.findByCourseAndIsActiveTrue(course);
        return assignments.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public AssignmentResponse getAssignmentById(Long assignmentId, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem bài tập này");
        }
        
        return convertToResponse(assignment);
    }
    
    // TODO: Refactor TestCase methods to work with Questions
    public void addTestCase(Long assignmentId, TestCaseRequest request, User teacher) {
        throw new RuntimeException("TestCase operations temporarily disabled. Use Questions instead.");
    }
    
    public void updateTestCase(Long testCaseId, TestCaseRequest request, User teacher) {
        throw new RuntimeException("TestCase operations temporarily disabled. Use Questions instead.");  
    }
    
    public void deleteTestCase(Long testCaseId, User teacher) {
        throw new RuntimeException("TestCase operations temporarily disabled. Use Questions instead.");
    }
    
    public List<TestCase> getTestCases(Long assignmentId, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem test case của bài tập này");
        }
        
        // Return all test cases from all questions
        return assignment.getQuestions().stream()
            .flatMap(q -> q.getTestCases().stream())
            .collect(Collectors.toList());
    }
    
    private AssignmentResponse convertToResponse(Assignment assignment) {
        Long submissionCount = submissionRepository.countByAssignment(assignment);
        Long pendingCount = assignmentRepository.countPendingSubmissionsByAssignment(assignment);
        
        Long totalQuestions = (long) assignment.getQuestions().size();
        
        return new AssignmentResponse(
            assignment.getId(),
            assignment.getTitle(),
            assignment.getDescription(),
            assignment.getType(),
            assignment.getCourse().getName(),
            assignment.getCourse().getId(),
            assignment.getCourse().getCode(),
            assignment.getMaxScore(),
            assignment.getTimeLimit(),
            assignment.getStartTime(),
            assignment.getEndTime(),
            assignment.getIsActive(),
            assignment.getAllowLateSubmission(),
            assignment.getAutoGrade(),
            submissionCount,
            pendingCount,
            totalQuestions,
            assignment.getCreatedAt(),
            assignment.getUpdatedAt()
        );
    }
}
