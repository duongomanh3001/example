package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateAssignmentRequest;
import iuh.fit.cscore_be.dto.request.AssignmentRequest;
import iuh.fit.cscore_be.dto.request.TestCaseRequest;
import iuh.fit.cscore_be.dto.response.DetailedAssignmentResponse;
import iuh.fit.cscore_be.dto.response.AssignmentResponse;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.exception.ResourceNotFoundException;
import iuh.fit.cscore_be.exception.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import iuh.fit.cscore_be.repository.AssignmentRepository;
import iuh.fit.cscore_be.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TeacherAssignmentManagementService {
    
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final AssignmentService assignmentService;
    
    public DetailedAssignmentResponse createAssignment(CreateAssignmentRequest request, User teacher) {
        log.info("Creating new assignment '{}' by teacher {}", request.getTitle(), teacher.getUsername());
        
        Course course = validateCourseAccess(request.getCourseId(), teacher);
        validateAssignmentRequest(request);
        
        // Use AssignmentService to create assignment with questions
        AssignmentResponse assignmentResponse = assignmentService.createAssignmentWithQuestions(request, teacher);
        
        // Fetch the saved assignment to return detailed response
        Assignment savedAssignment = assignmentRepository.findById(assignmentResponse.getId())
            .orElseThrow(() -> new RuntimeException("Không thể tìm thấy assignment vừa tạo"));
        
        log.info("Successfully created assignment with ID: {} with {} questions", 
                savedAssignment.getId(), assignmentResponse.getTotalQuestions());
        return mapToDetailedAssignmentResponse(savedAssignment);
    }
    
    public DetailedAssignmentResponse updateAssignment(Long assignmentId, AssignmentRequest request, User teacher) {
        log.info("Updating assignment {} by teacher {}", assignmentId, teacher.getUsername());
        
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        validateAssignmentUpdateRequest(request);
        
        updateAssignmentFromRequest(assignment, request);
        Assignment updatedAssignment = assignmentRepository.save(assignment);
        
        log.info("Successfully updated assignment with ID: {}", assignmentId);
        return mapToDetailedAssignmentResponse(updatedAssignment);
    }
    
    public void deleteAssignment(Long assignmentId, User teacher) {
        log.info("Deleting assignment {} by teacher {}", assignmentId, teacher.getUsername());
        
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        validateAssignmentDeletion(assignment);
        
        assignment.setIsActive(false);
        assignmentRepository.save(assignment);
        
        log.info("Successfully deactivated assignment with ID: {}", assignmentId);
    }
    
    public DetailedAssignmentResponse getAssignmentById(Long assignmentId, User teacher) {
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        return mapToDetailedAssignmentResponse(assignment);
    }
    
    public List<AssignmentResponse> getAssignmentsByTeacher(User teacher) {
        List<Course> teacherCourses = courseRepository.findByTeacherAndIsActiveTrue(teacher);
        List<Assignment> assignments = assignmentRepository.findByCourseInAndIsActiveTrueOrderByCreatedAtDesc(teacherCourses);
        
        return assignments.stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }
    
    public Page<AssignmentResponse> getAssignmentsByTeacher(User teacher, Pageable pageable) {
        List<Course> teacherCourses = courseRepository.findByTeacherAndIsActiveTrue(teacher);
        Page<Assignment> assignments = assignmentRepository.findByCourseInAndIsActiveTrue(teacherCourses, pageable);
        
        return assignments.map(this::mapToAssignmentResponse);
    }
    
    public List<AssignmentResponse> getAssignmentsByCourse(Long courseId, User teacher) {
        Course course = validateCourseAccess(courseId, teacher);
        List<Assignment> assignments = assignmentRepository.findByCourseAndIsActiveTrueOrderByCreatedAtDesc(course);
        
        return assignments.stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }
    
    public DetailedAssignmentResponse toggleAssignmentStatus(Long assignmentId, User teacher) {
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        assignment.setIsActive(!assignment.getIsActive());
        
        Assignment updatedAssignment = assignmentRepository.save(assignment);
        log.info("Toggled assignment {} status to {}", assignmentId, updatedAssignment.getIsActive());
        
        return mapToDetailedAssignmentResponse(updatedAssignment);
    }
    
    // Private helper methods
    
    private Course validateCourseAccess(Long courseId, User teacher) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId));
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new BadRequestException("Bạn không có quyền truy cập khóa học này");
        }
        
        if (!course.getIsActive()) {
            throw new BadRequestException("Khóa học không còn hoạt động");
        }
        
        return course;
    }
    
    private Assignment findAssignmentByIdAndTeacher(Long assignmentId, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new BadRequestException("Bạn không có quyền truy cập bài tập này");
        }
        
        return assignment;
    }
    
    private void validateAssignmentRequest(CreateAssignmentRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new BadRequestException("Thời gian bắt đầu không thể sau thời gian kết thúc");
            }
            
            if (request.getStartTime().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Thời gian bắt đầu phải trong tương lai");
            }
        }
    }
    
    private void validateAssignmentUpdateRequest(AssignmentRequest request) {
        // Add validation logic for update requests if needed
    }
    
    private void validateAssignmentDeletion(Assignment assignment) {
        long submissionCount = assignment.getSubmissions().size();
        if (submissionCount > 0) {
            throw new BadRequestException(
                    "Không thể xóa bài tập đã có sinh viên nộp bài. Hiện có " + submissionCount + " bài nộp.");
        }
    }
    
    private Assignment buildAssignmentFromRequest(CreateAssignmentRequest request, Course course) {
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
        return assignment;
    }
    
    private void updateAssignmentFromRequest(Assignment assignment, AssignmentRequest request) {
        if (request.getTitle() != null) {
            assignment.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            assignment.setDescription(request.getDescription());
        }
        if (request.getRequirements() != null) {
            assignment.setRequirements(request.getRequirements());
        }
        if (request.getMaxScore() != null) {
            assignment.setMaxScore(request.getMaxScore());
        }
        if (request.getTimeLimit() != null) {
            assignment.setTimeLimit(request.getTimeLimit());
        }
        if (request.getStartTime() != null) {
            assignment.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            assignment.setEndTime(request.getEndTime());
        }
        if (request.getAllowLateSubmission() != null) {
            assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        }
        if (request.getAutoGrade() != null) {
            assignment.setAutoGrade(request.getAutoGrade());
        }
    }
    
    private DetailedAssignmentResponse mapToDetailedAssignmentResponse(Assignment assignment) {
        DetailedAssignmentResponse response = new DetailedAssignmentResponse();
        response.setId(assignment.getId());
        response.setTitle(assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setRequirements(assignment.getRequirements());
        response.setType(assignment.getType());
        response.setMaxScore(assignment.getMaxScore());
        response.setTimeLimit(assignment.getTimeLimit());
        response.setStartTime(assignment.getStartTime());
        response.setEndTime(assignment.getEndTime());
        response.setIsActive(assignment.getIsActive());
        response.setAllowLateSubmission(assignment.getAllowLateSubmission());
        response.setAutoGrade(assignment.getAutoGrade());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());
        
        // Set course information
        DetailedAssignmentResponse.CourseInfo courseInfo = new DetailedAssignmentResponse.CourseInfo();
        courseInfo.setId(assignment.getCourse().getId());
        courseInfo.setName(assignment.getCourse().getName());
        courseInfo.setCode(assignment.getCourse().getCode());
        response.setCourse(courseInfo);
        
        // Set statistics
        response.setTestCaseCount(assignment.getQuestions().stream()
            .mapToInt(q -> q.getTestCases().size()).sum());
        response.setSubmissionCount(assignment.getSubmissions().size());
        
        return response;
    }
    
    private AssignmentResponse mapToAssignmentResponse(Assignment assignment) {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        response.setTitle(assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setType(assignment.getType());
        response.setMaxScore(assignment.getMaxScore());
        response.setStartTime(assignment.getStartTime());
        response.setEndTime(assignment.getEndTime());
        response.setIsActive(assignment.getIsActive());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());
        
        // Set course information
        response.setCourseId(assignment.getCourse().getId());
        response.setCourseName(assignment.getCourse().getName());
        response.setCourseCode(assignment.getCourse().getCode());
        
        // Calculate totalQuestions from assignment questions
        Long totalQuestions = assignment.getQuestions() != null ? (long) assignment.getQuestions().size() : 0L;
        response.setTotalQuestions(totalQuestions);
        
        return response;
    }
    
    // Test Case Management Methods
    public List<TestCase> getTestCases(Long assignmentId, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));
        
        validateTeacherCanModifyAssignment(teacher.getId(), assignment);
        
        // Return all test cases from all questions
        return assignment.getQuestions().stream()
            .flatMap(q -> q.getTestCases().stream())
            .collect(Collectors.toList());
    }
    
    public void addTestCase(Long assignmentId, TestCaseRequest request, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));
        
        validateTeacherCanModifyAssignment(teacher.getId(), assignment);
        
        // Implementation for adding test case would go here
        // This is a placeholder method signature to resolve compilation errors
        log.info("Adding test case to assignment {} by teacher {}", assignmentId, teacher.getId());
    }
    
    public void updateTestCase(Long testCaseId, TestCaseRequest request, User teacher) {
        // For now, we'll just validate - full implementation would need TestCase entity
        log.info("Updating test case {} by teacher {}", testCaseId, teacher.getId());
    }
    
    public void deleteTestCase(Long testCaseId, User teacher) {
        // For now, we'll just validate - full implementation would need TestCase entity
        log.info("Deleting test case {} by teacher {}", testCaseId, teacher.getId());
    }
    
    private void validateTeacherCanModifyAssignment(Long teacherId, Assignment assignment) {
        // Check if teacher is assigned to the course
        if (!assignment.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa bài tập này");
        }
    }
}
