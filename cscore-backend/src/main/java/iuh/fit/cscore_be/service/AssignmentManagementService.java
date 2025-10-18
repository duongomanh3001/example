package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateQuestionRequest;
import iuh.fit.cscore_be.dto.request.CreateAssignmentRequest;
import iuh.fit.cscore_be.dto.request.AssignmentRequest;
import iuh.fit.cscore_be.dto.request.TestCaseRequest;
import iuh.fit.cscore_be.dto.response.AssignmentResponse;
import iuh.fit.cscore_be.dto.response.DetailedAssignmentResponse;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.entity.Section;
import iuh.fit.cscore_be.exception.ResourceNotFoundException;
import iuh.fit.cscore_be.exception.BadRequestException;
import iuh.fit.cscore_be.repository.AssignmentRepository;
import iuh.fit.cscore_be.repository.CourseRepository;
import iuh.fit.cscore_be.repository.SubmissionRepository;
import iuh.fit.cscore_be.repository.TestCaseRepository;
import iuh.fit.cscore_be.repository.SectionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Assignment Management Service
 * Comprehensive assignment management with:
 * - Assignment CRUD operations
 * - Teacher-specific assignment management
 * - Question and test case management
 * - Assignment validation and security
 * - Notification integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssignmentManagementService {
    
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final SectionRepository sectionRepository;
    private final QuestionService questionService;
    
    @Autowired
    private NotificationService notificationService;

    // ========== ASSIGNMENT CREATION ==========
    
    /**
     * Create a basic assignment
     */
    public AssignmentResponse createAssignment(AssignmentRequest request, User teacher) {
        log.info("Creating basic assignment '{}' by teacher {}", request.getTitle(), teacher.getUsername());
        
        Course course = validateCourseAccess(request.getCourseId(), teacher);
        validateAssignmentRequest(request);
        
        Assignment assignment = buildAssignmentFromRequest(request, course);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        log.info("Successfully created basic assignment with ID: {}", savedAssignment.getId());
        return convertToAssignmentResponse(savedAssignment);
    }
    
    /**
     * Create assignment with questions and test cases
     */
    public DetailedAssignmentResponse createAssignmentWithQuestions(CreateAssignmentRequest request, User teacher) {
        log.info("Creating detailed assignment '{}' with {} questions by teacher {}", 
                request.getTitle(), 
                request.getQuestions() != null ? request.getQuestions().size() : 0, 
                teacher.getUsername());
        
        Course course = validateCourseAccess(request.getCourseId(), teacher);
        validateCreateAssignmentRequest(request);
        
        // Create assignment
        Assignment assignment = buildAssignmentFromCreateRequest(request, course);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        // Create questions if provided
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            createQuestionsForAssignment(request.getQuestions(), savedAssignment);
        }
        
        // Send notifications if configured
        sendAssignmentNotifications(savedAssignment);
        
        log.info("Successfully created detailed assignment with ID: {} with {} questions", 
                savedAssignment.getId(), 
                request.getQuestions() != null ? request.getQuestions().size() : 0);
        
        return convertToDetailedAssignmentResponse(savedAssignment);
    }

    // ========== ASSIGNMENT UPDATES ==========
    
    /**
     * Update assignment (teacher access only)
     */
    public DetailedAssignmentResponse updateAssignment(Long assignmentId, AssignmentRequest request, User teacher) {
        log.info("Updating assignment {} by teacher {}", assignmentId, teacher.getUsername());
        
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        validateAssignmentUpdateRequest(request, assignment);
        
        updateAssignmentFromRequest(assignment, request);
        Assignment updatedAssignment = assignmentRepository.save(assignment);
        
        log.info("Successfully updated assignment with ID: {}", assignmentId);
        return convertToDetailedAssignmentResponse(updatedAssignment);
    }
    
    /**
     * Soft delete assignment (deactivate)
     */
    public void deleteAssignment(Long assignmentId, User teacher) {
        log.info("Deleting assignment {} by teacher {}", assignmentId, teacher.getUsername());
        
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        validateAssignmentDeletion(assignment);
        
        assignment.setIsActive(false);
        assignmentRepository.save(assignment);
        
        log.info("Successfully deactivated assignment with ID: {}", assignmentId);
    }
    
    /**
     * Activate/Deactivate assignment
     */
    public DetailedAssignmentResponse toggleAssignmentStatus(Long assignmentId, User teacher) {
        log.info("Toggling status for assignment {} by teacher {}", assignmentId, teacher.getUsername());
        
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        assignment.setIsActive(!assignment.getIsActive());
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        log.info("Assignment {} status changed to: {}", assignmentId, savedAssignment.getIsActive());
        return convertToDetailedAssignmentResponse(savedAssignment);
    }

    // ========== ASSIGNMENT RETRIEVAL ==========
    
    /**
     * Get assignment by ID (with teacher access validation)
     */
    public DetailedAssignmentResponse getAssignmentById(Long assignmentId, User teacher) {
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        return convertToDetailedAssignmentResponse(assignment);
    }
    
    /**
     * Get assignment by ID (public access for students)
     */
    public AssignmentResponse getAssignmentByIdPublic(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        
        if (!assignment.getIsActive()) {
            throw new BadRequestException("Assignment is not active");
        }
        
        return convertToAssignmentResponse(assignment);
    }
    
    /**
     * Get all assignments by teacher
     */
    public List<AssignmentResponse> getAssignmentsByTeacher(User teacher) {
        List<Course> teacherCourses = courseRepository.findByTeacherAndIsActiveTrue(teacher);
        List<Assignment> assignments = assignmentRepository.findByCourseInAndIsActiveTrueOrderByCreatedAtDesc(teacherCourses);
        
        return assignments.stream()
                .map(this::convertToAssignmentResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get assignments by teacher with pagination
     */
    public Page<AssignmentResponse> getAssignmentsByTeacher(User teacher, Pageable pageable) {
        List<Course> teacherCourses = courseRepository.findByTeacherAndIsActiveTrue(teacher);
        Page<Assignment> assignments = assignmentRepository.findByCourseInAndIsActiveTrue(teacherCourses, pageable);
        
        return assignments.map(this::convertToAssignmentResponse);
    }
    
    /**
     * Get assignments by course
     */
    public List<AssignmentResponse> getAssignmentsByCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        
        List<Assignment> assignments = assignmentRepository.findByCourseAndIsActiveTrueOrderByCreatedAtDesc(course);
        
        return assignments.stream()
                .map(this::convertToAssignmentResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get assignments by course with teacher access validation
     */
    public List<DetailedAssignmentResponse> getAssignmentsByCourseForTeacher(Long courseId, User teacher) {
        Course course = validateCourseAccess(courseId, teacher);
        List<Assignment> assignments = assignmentRepository.findByCourseOrderByCreatedAtDesc(course);
        
        return assignments.stream()
                .map(this::convertToDetailedAssignmentResponse)
                .collect(Collectors.toList());
    }

    // ========== ASSIGNMENT STATISTICS ==========
    
    /**
     * Get assignment statistics for teacher
     */
    public AssignmentStatistics getAssignmentStatistics(Long assignmentId, User teacher) {
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        
        AssignmentStatistics stats = new AssignmentStatistics();
        stats.setAssignmentId(assignmentId);
        stats.setTitle(assignment.getTitle());
        
        // Count enrolled students
        Long enrolledStudents = courseRepository.countEnrolledStudents(assignment.getCourse().getId());
        stats.setTotalStudents(enrolledStudents);
        
        // Count submissions
        Long totalSubmissions = submissionRepository.countByAssignment(assignment);
        stats.setTotalSubmissions(totalSubmissions);
        
        Long pendingSubmissions = submissionRepository.countByAssignmentAndStatusSubmitted(assignment);
        stats.setPendingGrading(pendingSubmissions);
        
        Long gradedSubmissions = submissionRepository.countByAssignmentAndStatusGraded(assignment);
        stats.setGradedSubmissions(gradedSubmissions);
        
        // Calculate submission rate
        double submissionRate = enrolledStudents > 0 ? (double) totalSubmissions / enrolledStudents * 100 : 0.0;
        stats.setSubmissionRate(submissionRate);
        
        // Calculate average score
        Double averageScore = submissionRepository.calculateAverageScoreByAssignment(assignment);
        stats.setAverageScore(averageScore != null ? averageScore : 0.0);
        
        return stats;
    }
    
    /**
     * Get teacher's overall assignment statistics
     */
    public TeacherAssignmentStatistics getTeacherAssignmentStatistics(User teacher) {
        List<Course> teacherCourses = courseRepository.findByTeacherAndIsActiveTrue(teacher);
        List<Assignment> assignments = assignmentRepository.findByCourseInAndIsActiveTrueOrderByCreatedAtDesc(teacherCourses);
        
        TeacherAssignmentStatistics stats = new TeacherAssignmentStatistics();
        
        stats.setTotalAssignments((long) assignments.size());
        stats.setActiveAssignments(assignments.stream().mapToLong(a -> a.getIsActive() ? 1 : 0).sum());
        
        Long totalSubmissions = assignments.stream()
                .mapToLong(a -> submissionRepository.countByAssignment(a))
                .sum();
        stats.setTotalSubmissions(totalSubmissions);
        
        Long pendingGrading = assignments.stream()
                .mapToLong(a -> submissionRepository.countByAssignmentAndStatusSubmitted(a))
                .sum();
        stats.setPendingGrading(pendingGrading);
        
        // Calculate average score across all assignments
        Double overallAverage = submissionRepository.calculateAverageScoreByTeacher(teacher);
        stats.setOverallAverageScore(overallAverage != null ? overallAverage : 0.0);
        
        return stats;
    }

    // ========== TEST CASE MANAGEMENT ==========
    
    /**
     * Get test cases for an assignment (through questions)
     */
    public List<TestCase> getTestCases(Long assignmentId, User teacher) {
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        
        // Get all test cases from all questions in this assignment
        List<TestCase> allTestCases = new ArrayList<>();
        for (Question question : assignment.getQuestions()) {
            List<TestCase> questionTestCases = testCaseRepository.findByQuestion(question);
            allTestCases.addAll(questionTestCases);
        }
        
        return allTestCases;
    }
    
    /**
     * Add test case to assignment (to first question - simplified for now)
     */
    public void addTestCase(Long assignmentId, TestCaseRequest request, User teacher) {
        Assignment assignment = findAssignmentByIdAndTeacher(assignmentId, teacher);
        
        // Find the first question in the assignment (simplified approach)
        if (assignment.getQuestions().isEmpty()) {
            throw new BadRequestException("Assignment must have at least one question to add test cases");
        }
        
        Question question = assignment.getQuestions().get(0);
        
        TestCase testCase = new TestCase();
        testCase.setQuestion(question);
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setTestCode(request.getTestCode());
        testCase.setIsHidden(request.getIsHidden());
        testCase.setWeight(request.getWeight());
        testCase.setTimeLimit(request.getTimeLimit());
        testCase.setMemoryLimit(request.getMemoryLimit());
                
        testCaseRepository.save(testCase);
        log.info("Added test case to assignment {} by teacher {}", assignmentId, teacher.getUsername());
    }
    
    /**
     * Update test case
     */
    public void updateTestCase(Long testCaseId, TestCaseRequest request, User teacher) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found: " + testCaseId));
        
        // Verify teacher has access to this test case's assignment
        Question question = testCase.getQuestion();
        Assignment assignment = question.getAssignment();
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new BadRequestException("Access denied: You are not the teacher of this assignment");
        }
        
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setTestCode(request.getTestCode());
        testCase.setIsHidden(request.getIsHidden());
        testCase.setWeight(request.getWeight());
        testCase.setTimeLimit(request.getTimeLimit());
        testCase.setMemoryLimit(request.getMemoryLimit());
        
        testCaseRepository.save(testCase);
        log.info("Updated test case {} by teacher {}", testCaseId, teacher.getUsername());
    }
    
    /**
     * Delete test case
     */
    public void deleteTestCase(Long testCaseId, User teacher) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found: " + testCaseId));
        
        // Verify teacher has access to this test case's assignment
        Question question = testCase.getQuestion();
        Assignment assignment = question.getAssignment();
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new BadRequestException("Access denied: You are not the teacher of this assignment");
        }
        
        testCaseRepository.delete(testCase);
        log.info("Deleted test case {} by teacher {}", testCaseId, teacher.getUsername());
    }

    // ========== VALIDATION METHODS ==========
    
    private Course validateCourseAccess(Long courseId, User teacher) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new BadRequestException("Access denied: You are not the teacher of this course");
        }
        
        if (!course.getIsActive()) {
            throw new BadRequestException("Cannot create assignments for inactive courses");
        }
        
        return course;
    }
    
    private void validateAssignmentRequest(AssignmentRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Assignment title is required");
        }
        
        if (request.getEndTime() != null && request.getStartTime() != null && 
            request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        
        if (request.getMaxScore() != null && request.getMaxScore() <= 0) {
            throw new BadRequestException("Max score must be positive");
        }
    }
    
    private void validateAssignmentRequest(CreateAssignmentRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Assignment title is required");
        }
        
        if (request.getEndTime() != null && request.getStartTime() != null && 
            request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        
        if (request.getMaxScore() != null && request.getMaxScore() <= 0) {
            throw new BadRequestException("Max score must be positive");
        }
    }
    
    private void validateCreateAssignmentRequest(CreateAssignmentRequest request) {
        validateAssignmentRequest(request);
        
        if (request.getQuestions() != null) {
            for (var questionRequest : request.getQuestions()) {
                if (questionRequest.getTitle() == null || questionRequest.getTitle().trim().isEmpty()) {
                    throw new BadRequestException("All questions must have titles");
                }
            }
        }
    }
    
    private void validateAssignmentUpdateRequest(AssignmentRequest request, Assignment assignment) {
        validateAssignmentRequest(request);
        
        // Check if assignment has submissions
        Long submissionCount = submissionRepository.countByAssignment(assignment);
        if (submissionCount > 0) {
            // Restrict certain updates if there are submissions
            if (request.getMaxScore() != null && !request.getMaxScore().equals(assignment.getMaxScore())) {
                log.warn("Attempting to change max score for assignment with existing submissions");
            }
        }
    }
    
    private void validateAssignmentDeletion(Assignment assignment) {
        // Check if assignment has submissions
        Long submissionCount = submissionRepository.countByAssignment(assignment);
        if (submissionCount > 0) {
            log.info("Assignment {} has {} submissions, performing soft delete", 
                    assignment.getId(), submissionCount);
        }
        
        // Additional business rules can be added here
    }

    // ========== HELPER METHODS ==========
    
    private Assignment findAssignmentByIdAndTeacher(Long assignmentId, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new BadRequestException("Access denied: You are not the teacher of this assignment");
        }
        
        return assignment;
    }
    
    private Assignment buildAssignmentFromRequest(AssignmentRequest request, Course course) {
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
    
    private Assignment buildAssignmentFromCreateRequest(CreateAssignmentRequest request, Course course) {
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
        
        // Set section if provided
        if (request.getSectionId() != null) {
            log.info("📁 Attempting to set section ID: {}", request.getSectionId());
            sectionRepository.findById(request.getSectionId()).ifPresentOrElse(
                section -> {
                    assignment.setSection(section);
                    log.info("✅ Section set successfully: {} (ID: {})", section.getName(), section.getId());
                },
                () -> log.warn("⚠️ Section with ID {} not found", request.getSectionId())
            );
        } else {
            log.info("ℹ️ No section ID provided for assignment");
        }
        
        return assignment;
    }
    
    private void updateAssignmentFromRequest(Assignment assignment, AssignmentRequest request) {
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setRequirements(request.getRequirements());
        assignment.setType(request.getType());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setTimeLimit(request.getTimeLimit());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        assignment.setAutoGrade(request.getAutoGrade());
    }
    
    private void createQuestionsForAssignment(List<CreateQuestionRequest> questions, Assignment assignment) {
        for (int i = 0; i < questions.size(); i++) {
            var questionRequest = questions.get(i);
            questionRequest.setOrderIndex(i);
            questionService.createQuestion(questionRequest, assignment);
        }
    }
    
    private void sendAssignmentNotifications(Assignment assignment) {
        try {
            // Send notification to teacher who created the assignment
            User teacher = assignment.getCourse().getTeacher();
            notificationService.createTeacherAssignmentNotification(
                teacher,
                assignment.getId(),
                assignment.getTitle(),
                assignment.getCourse()
            );
            
            // Send notifications to enrolled students
            notificationService.notifyNewAssignment(assignment);
        } catch (Exception e) {
            log.warn("Failed to send assignment notifications for assignment {}: {}", 
                    assignment.getId(), e.getMessage());
        }
    }

    // ========== CONVERSION METHODS ==========
    
    private AssignmentResponse convertToAssignmentResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .courseId(assignment.getCourse().getId())
                .courseTitle(assignment.getCourse().getName())
                .type(assignment.getType())
                .maxScore(assignment.getMaxScore())
                .timeLimit(assignment.getTimeLimit())
                .startTime(assignment.getStartTime())
                .endTime(assignment.getEndTime())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .autoGrade(assignment.getAutoGrade())
                .isActive(assignment.getIsActive())
                .createdAt(assignment.getCreatedAt())
                .totalQuestions((long) assignment.getQuestions().size())
                .sectionId(assignment.getSection() != null ? assignment.getSection().getId() : null)
                .build();
    }
    
    private DetailedAssignmentResponse convertToDetailedAssignmentResponse(Assignment assignment) {
        // Get submission statistics
        Long totalSubmissions = submissionRepository.countByAssignment(assignment);
        Long pendingGrading = submissionRepository.countByAssignmentAndStatusSubmitted(assignment);
        Double averageScore = submissionRepository.calculateAverageScoreByAssignment(assignment);
        
        return DetailedAssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .requirements(assignment.getRequirements())
                .courseId(assignment.getCourse().getId())
                .courseTitle(assignment.getCourse().getName())
                .teacherName(assignment.getCourse().getTeacher().getFullName())
                .type(assignment.getType())
                .maxScore(assignment.getMaxScore())
                .timeLimit(assignment.getTimeLimit())
                .startTime(assignment.getStartTime())
                .endTime(assignment.getEndTime())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .autoGrade(assignment.getAutoGrade())
                .isActive(assignment.getIsActive())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .totalQuestions((long) assignment.getQuestions().size())
                .submissionCount(totalSubmissions.intValue())
                .questions(assignment.getQuestions().stream()
                          .map(questionService::convertToResponse)
                          .collect(Collectors.toList()))
                .build();
    }

    // ========== DATA CLASSES ==========
    
    @Data
    public static class AssignmentStatistics {
        private Long assignmentId;
        private String title;
        private Long totalStudents;
        private Long totalSubmissions;
        private Long pendingGrading;
        private Long gradedSubmissions;
        private Double submissionRate;
        private Double averageScore;
    }
    
    @Data
    public static class TeacherAssignmentStatistics {
        private Long totalAssignments;
        private Long activeAssignments;
        private Long totalSubmissions;
        private Long pendingGrading;
        private Double overallAverageScore;
    }
}