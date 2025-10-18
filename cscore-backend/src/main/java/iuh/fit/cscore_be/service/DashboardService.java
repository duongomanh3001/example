package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.*;
import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.exception.ResourceNotFoundException;
import iuh.fit.cscore_be.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard Service
 * Unified service providing role-based dashboard data with:
 * - Student dashboard functionality
 * - Teacher dashboard functionality
 * - Admin dashboard functionality (extensible)
 * - Unified data models and calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {
    
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * Main entry point - Get dashboard data based on user role
     */
    public DashboardResponse getDashboardData(User user) {
        log.info("Getting dashboard data for user: {} with role: {}", user.getUsername(), user.getRole());
        
        return switch (user.getRole()) {
            case STUDENT -> getStudentDashboard(user);
            case TEACHER -> getTeacherDashboard(user);
            case ADMIN -> getAdminDashboard(user);
        };
    }

    // ========== STUDENT DASHBOARD ==========
    
    public DashboardResponse getStudentDashboard(User student) {
        log.debug("Generating student dashboard for: {}", student.getStudentId());
        
        // Get enrolled courses
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        List<Course> courses = enrollments.stream()
                .map(Enrollment::getCourse)
                .collect(Collectors.toList());
        
        // Calculate statistics
        StudentStatistics stats = calculateStudentStatistics(student, courses);
        
        // Get recent data
        List<StudentCourseResponse> recentCourses = enrollments.stream()
                .limit(5)
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
        
        List<StudentAssignmentResponse> recentAssignments = getRecentAssignmentsForStudent(student, courses);
        List<SubmissionResponse> recentSubmissions = getRecentSubmissionsForStudent(student);
        
        // Create student dashboard response
        StudentDashboardResponse studentDashboard = new StudentDashboardResponse(
                stats.getTotalCourses(),
                stats.getTotalAssignments(),
                stats.getSubmittedAssignments(),
                stats.getAverageScore(),
                recentCourses,
                recentAssignments,
                recentSubmissions,
                StudentDashboardResponse.StudentStatistics.builder()
                        .totalSubmissions(stats.getTotalSubmissions())
                        .pendingSubmissions(stats.getPendingAssignments())
                        .gradedSubmissions(stats.getCompletedAssignments())
                        .bestScore(stats.getAverageScore())
                        .coursesEnrolled(recentCourses.size())
                        .build()
        );
        
        return DashboardResponse.builder()
                .userRole(Role.STUDENT)
                .studentDashboard(studentDashboard)
                .build();
    }
    
    private StudentStatistics calculateStudentStatistics(User student, List<Course> courses) {
        StudentStatistics stats = new StudentStatistics();
        
        // Basic counts
        stats.setTotalCourses((long) courses.size());
        
        List<Assignment> allAssignments = courses.stream()
                .flatMap(course -> assignmentRepository.findByCourseAndIsActiveTrue(course).stream())
                .collect(Collectors.toList());
        stats.setTotalAssignments((long) allAssignments.size());
        
        // Submission statistics
        List<Submission> submissions = submissionRepository.findByStudent(student);
        stats.setTotalSubmissions((long) submissions.size());
        stats.setSubmittedAssignments((long) submissions.size());
        
        // Calculate completed vs pending
        long completedAssignments = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                .count();
        stats.setCompletedAssignments(completedAssignments);
        stats.setPendingAssignments(stats.getTotalAssignments() - stats.getSubmittedAssignments());
        
        // Calculate average score
        double averageScore = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(Submission::getScore)
                .average()
                .orElse(0.0);
        stats.setAverageScore(averageScore);
        
        return stats;
    }
    
    private List<StudentAssignmentResponse> getRecentAssignmentsForStudent(User student, List<Course> courses) {
        return courses.stream()
                .flatMap(course -> assignmentRepository.findByCourseAndIsActiveTrue(course).stream())
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .limit(5)
                .map(assignment -> convertToStudentAssignmentResponse(assignment, student))
                .collect(Collectors.toList());
    }
    
    private List<SubmissionResponse> getRecentSubmissionsForStudent(User student) {
        return submissionRepository.findByStudent(student).stream()
                .sorted((s1, s2) -> s2.getSubmissionTime().compareTo(s1.getSubmissionTime()))
                .limit(5)
                .map(this::convertToSubmissionResponse)
                .collect(Collectors.toList());
    }

    // ========== TEACHER DASHBOARD ==========
    
    public DashboardResponse getTeacherDashboard(User teacher) {
        log.debug("Generating teacher dashboard for: {}", teacher.getUsername());
        
        // Calculate statistics
        TeacherStatistics stats = calculateTeacherStatistics(teacher);
        
        // Get recent data
        List<CourseResponse> recentCourses = getRecentCoursesForTeacher(teacher);
        List<AssignmentResponse> recentAssignments = getRecentAssignmentsForTeacher(teacher);
        List<SubmissionResponse> pendingGrades = getPendingGradesForTeacher(teacher);
        
        // Create teacher dashboard response
        TeacherDashboardResponse teacherDashboard = new TeacherDashboardResponse(
                stats.getTotalCourses(),
                stats.getTotalStudents(),
                stats.getTotalAssignments(),
                stats.getPendingSubmissions(),
                recentCourses,
                recentAssignments,
                pendingGrades,
                new TeacherDashboardResponse.TeacherStatistics(
                        stats.getAverageScore(),
                        stats.getTotalSubmissions(),
                        stats.getGradedSubmissions(),
                        stats.getActiveStudents()
                )
        );
        
        return DashboardResponse.builder()
                .userRole(Role.TEACHER)
                .teacherDashboard(teacherDashboard)
                .build();
    }
    
    private TeacherStatistics calculateTeacherStatistics(User teacher) {
        TeacherStatistics stats = new TeacherStatistics();
        
        // Course statistics
        List<Course> teacherCourses = courseRepository.findByTeacherAndIsActiveTrue(teacher);
        stats.setTotalCourses((long) teacherCourses.size());
        
        // Student statistics
        List<Enrollment> allEnrollments = enrollmentRepository.findAllByTeacher(teacher);
        stats.setTotalStudents((long) allEnrollments.size());
        stats.setActiveStudents(allEnrollments.size());
        
        // Assignment statistics
        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacher.getId());
        stats.setTotalAssignments((long) assignments.size());
        
        // Submission statistics
        List<Submission> allSubmissions = submissionRepository.findByTeacher(teacher);
        stats.setTotalSubmissions((long) allSubmissions.size());
        
        long gradedSubmissions = allSubmissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                .count();
        stats.setGradedSubmissions(gradedSubmissions);
        
        long pendingSubmissions = submissionRepository.countPendingSubmissionsByTeacher(teacher);
        stats.setPendingSubmissions(pendingSubmissions);
        
        // Calculate average score
        double averageScore = allSubmissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(Submission::getScore)
                .average()
                .orElse(0.0);
        stats.setAverageScore(averageScore);
        
        return stats;
    }
    
    private List<CourseResponse> getRecentCoursesForTeacher(User teacher) {
        return courseRepository.findRecentCoursesByTeacher(teacher).stream()
                .limit(5)
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }
    
    private List<AssignmentResponse> getRecentAssignmentsForTeacher(User teacher) {
        return assignmentRepository.findByTeacherId(teacher.getId()).stream()
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .limit(5)
                .map(this::convertToAssignmentResponse)
                .collect(Collectors.toList());
    }
    
    private List<SubmissionResponse> getPendingGradesForTeacher(User teacher) {
        return submissionRepository.findByTeacherAndStatus(teacher, SubmissionStatus.SUBMITTED).stream()
                .limit(10)
                .map(this::convertToSubmissionResponse)
                .collect(Collectors.toList());
    }

    // ========== ADMIN DASHBOARD ==========
    
    private DashboardResponse getAdminDashboard(User admin) {
        log.debug("Generating admin dashboard for: {}", admin.getUsername());
        
        // Calculate system-wide statistics
        AdminStatistics stats = calculateAdminStatistics();
        
        // Get recent system activity
        List<CourseResponse> recentCourses = getRecentCoursesSystemWide();
        List<UserResponse> recentUsers = getRecentUsers();
        List<SubmissionResponse> recentSubmissions = getRecentSubmissionsSystemWide();
        
        // Create admin dashboard response
        AdminDashboardResponse adminDashboard = AdminDashboardResponse.builder()
                .totalUsers(stats.getTotalUsers())
                .totalCourses(stats.getTotalCourses())
                .totalAssignments(stats.getTotalAssignments())
                .totalSubmissions(stats.getTotalSubmissions())
                .activeUsers(stats.getActiveUsers())
                .recentCourses(recentCourses)
                .recentUsers(recentUsers)
                .recentSubmissions(recentSubmissions)
                .systemStatistics(AdminDashboardResponse.SystemStatistics.builder()
                        .totalStudents(stats.getTotalStudents())
                        .totalTeachers(stats.getTotalTeachers())
                        .averageScore(stats.getAverageScore())
                        .submissionsToday(stats.getSubmissionsToday())
                        .build())
                .build();
        
        return DashboardResponse.builder()
                .userRole(Role.ADMIN)
                .adminDashboard(adminDashboard)
                .build();
    }
    
    private AdminStatistics calculateAdminStatistics() {
        AdminStatistics stats = new AdminStatistics();
        
        // User statistics
        stats.setTotalUsers(userRepository.count());
        stats.setTotalStudents(userRepository.countByRole(Role.STUDENT));
        stats.setTotalTeachers(userRepository.countByRole(Role.TEACHER));
        stats.setActiveUsers(userRepository.countActiveUsers()); // Assuming this method exists
        
        // System statistics
        stats.setTotalCourses(courseRepository.count());
        stats.setTotalAssignments(assignmentRepository.count());
        stats.setTotalSubmissions(submissionRepository.count());
        
        // Daily statistics
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        stats.setSubmissionsToday(submissionRepository.countSubmissionsAfter(todayStart));
        
        // Calculate system-wide average score
        double averageScore = submissionRepository.findAll().stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(Submission::getScore)
                .average()
                .orElse(0.0);
        stats.setAverageScore(averageScore);
        
        return stats;
    }
    
    private List<CourseResponse> getRecentCoursesSystemWide() {
        return courseRepository.findAll().stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .limit(5)
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }
    
    private List<UserResponse> getRecentUsers() {
        return userRepository.findAll().stream()
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(5)
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    private List<SubmissionResponse> getRecentSubmissionsSystemWide() {
        return submissionRepository.findAll().stream()
                .sorted((s1, s2) -> s2.getSubmissionTime().compareTo(s1.getSubmissionTime()))
                .limit(10)
                .map(this::convertToSubmissionResponse)
                .collect(Collectors.toList());
    }

    // ========== SPECIFIC METHODS FOR STUDENT ==========
    
    public List<StudentAssignmentResponse> getAllAssignmentsForStudent(User student) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        List<Course> courses = enrollments.stream()
                .map(Enrollment::getCourse)
                .collect(Collectors.toList());
                
        return courses.stream()
                .flatMap(course -> assignmentRepository.findByCourseAndIsActiveTrue(course).stream())
                .map(assignment -> convertToStudentAssignmentResponse(assignment, student))
                .collect(Collectors.toList());
    }
    
    public StudentAssignmentResponse getAssignmentForStudent(Long assignmentId, User student) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));
                
        // Check if student is enrolled
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        boolean isEnrolled = enrollments.stream()
                .anyMatch(enrollment -> enrollment.getCourse().getId().equals(assignment.getCourse().getId()));
                
        if (!isEnrolled) {
            throw new RuntimeException("Student is not enrolled in this course");
        }
        
        return convertToStudentAssignmentResponse(assignment, student);
    }

    // ========== CONVERSION METHODS ==========
    
    private StudentCourseResponse convertToCourseResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        return StudentCourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .teacherName(course.getTeacher().getFullName())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .build();
    }
    
    private CourseResponse convertToCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .teacherName(course.getTeacher().getFullName())
                .createdAt(course.getCreatedAt())
                .isActive(course.getIsActive())
                .build();
    }
    
    private StudentAssignmentResponse convertToStudentAssignmentResponse(Assignment assignment, User student) {
        // Get submission if exists
        Submission submission = submissionRepository.findByAssignmentAndStudent(assignment, student)
                .orElse(null);
        
        // Convert questions to StudentQuestionResponse if exist
        List<StudentQuestionResponse> questionResponses = null;
        if (assignment.getQuestions() != null && !assignment.getQuestions().isEmpty()) {
            questionResponses = assignment.getQuestions().stream()
                    .map(question -> convertToStudentQuestionResponse(question, student))
                    .collect(Collectors.toList());
        }
        
        // Get public test cases
        List<PublicTestCaseResponse> publicTestCases = null;
        if (assignment.getQuestions() != null) {
            publicTestCases = assignment.getQuestions().stream()
                    .flatMap(question -> question.getTestCases().stream())
                    .filter(testCase -> !testCase.getIsHidden()) // Use isHidden instead of isPublic
                    .map(this::convertToPublicTestCaseResponse)
                    .collect(Collectors.toList());
        }
        
        // Calculate total test cases
        int totalTestCases = 0;
        if (assignment.getQuestions() != null) {
            totalTestCases = assignment.getQuestions().stream()
                    .mapToInt(question -> question.getTestCases().size())
                    .sum();
        }
        
        return StudentAssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .requirements(assignment.getRequirements())
                .type(assignment.getType())
                .courseId(assignment.getCourse().getId())
                .courseName(assignment.getCourse().getName())
                .sectionId(assignment.getSection() != null ? assignment.getSection().getId() : null)
                .maxScore(assignment.getMaxScore())
                .timeLimit(assignment.getTimeLimit())
                .startTime(assignment.getStartTime())
                .endTime(assignment.getEndTime())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .programmingLanguages(assignment.getProgrammingLanguages())
                .questions(questionResponses)
                .publicTestCases(publicTestCases)
                .totalQuestions(assignment.getQuestions() != null ? assignment.getQuestions().size() : 0)
                .totalTestCases(totalTestCases)
                .isSubmitted(submission != null)
                .submissionTime(submission != null ? submission.getSubmissionTime() : null)
                .currentScore(submission != null ? submission.getScore() : null)
                .submissionStatus(submission != null ? submission.getStatus().toString() : null)
                .createdAt(assignment.getCreatedAt())
                .build();
    }
    
    private AssignmentResponse convertToAssignmentResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .courseTitle(assignment.getCourse().getName())
                .maxScore(assignment.getMaxScore())
                .startTime(assignment.getStartTime())
                .endTime(assignment.getEndTime())
                .createdAt(assignment.getCreatedAt())
                .totalQuestions((long) assignment.getQuestions().size())
                .sectionId(assignment.getSection() != null ? assignment.getSection().getId() : null)
                .build();
    }
    
    private SubmissionResponse convertToSubmissionResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentName(submission.getStudent().getFullName())
                .submissionTime(submission.getSubmissionTime())
                .score(submission.getScore())
                .status(submission.getStatus())
                .feedback(submission.getFeedback())
                .build();
    }
    
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .isActive(user.getIsActive())
                .build();
    }

    // ========== DATA CLASSES ==========
    
    @Data
    public static class DashboardResponse {
        private Role userRole;
        private StudentDashboardResponse studentDashboard;
        private TeacherDashboardResponse teacherDashboard;
        private AdminDashboardResponse adminDashboard;
        
        public static DashboardResponseBuilder builder() {
            return new DashboardResponseBuilder();
        }
        
        public static class DashboardResponseBuilder {
            private Role userRole;
            private StudentDashboardResponse studentDashboard;
            private TeacherDashboardResponse teacherDashboard;
            private AdminDashboardResponse adminDashboard;
            
            public DashboardResponseBuilder userRole(Role userRole) {
                this.userRole = userRole;
                return this;
            }
            
            public DashboardResponseBuilder studentDashboard(StudentDashboardResponse studentDashboard) {
                this.studentDashboard = studentDashboard;
                return this;
            }
            
            public DashboardResponseBuilder teacherDashboard(TeacherDashboardResponse teacherDashboard) {
                this.teacherDashboard = teacherDashboard;
                return this;
            }
            
            public DashboardResponseBuilder adminDashboard(AdminDashboardResponse adminDashboard) {
                this.adminDashboard = adminDashboard;
                return this;
            }
            
            public DashboardResponse build() {
                DashboardResponse response = new DashboardResponse();
                response.userRole = this.userRole;
                response.studentDashboard = this.studentDashboard;
                response.teacherDashboard = this.teacherDashboard;
                response.adminDashboard = this.adminDashboard;
                return response;
            }
        }
    }
    
    @Data
    private static class StudentStatistics {
        private Long totalCourses;
        private Long totalAssignments;
        private Long submittedAssignments;
        private Long totalSubmissions;
        private Long completedAssignments;
        private Long pendingAssignments;
        private Double averageScore;
    }
    
    @Data
    private static class TeacherStatistics {
        private Long totalCourses;
        private Long totalStudents;
        private Long totalAssignments;
        private Long totalSubmissions;
        private Long gradedSubmissions;
        private Long pendingSubmissions;
        private Integer activeStudents;
        private Double averageScore;
    }
    
    @Data
    private static class AdminStatistics {
        private Long totalUsers;
        private Long totalStudents;
        private Long totalTeachers;
        private Long activeUsers;
        private Long totalCourses;
        private Long totalAssignments;
        private Long totalSubmissions;
        private Long submissionsToday;
        private Double averageScore;
    }
    
    // ========== PUBLIC METHODS FOR CONTROLLERS ==========
    
    /**
     * Get student dashboard data in correct format for StudentController
     */
    public StudentDashboardResponse getStudentDashboardData(User student) {
        DashboardResponse response = getStudentDashboard(student);
        return response.getStudentDashboard();
    }
    
    /**
     * Get teacher dashboard data in correct format for TeacherController
     */
    public TeacherDashboardResponse getTeacherDashboardData(User teacher) {
        DashboardResponse response = getTeacherDashboard(teacher);
        return response.getTeacherDashboard();
    }
    
    /**
     * Get student assignments
     */
    public List<StudentAssignmentResponse> getStudentAssignments(User student) {
        // Get enrolled courses
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        List<Course> courses = enrollments.stream()
                .map(Enrollment::getCourse)
                .collect(Collectors.toList());
        
        // Get all assignments for these courses
        List<Assignment> assignments = assignmentRepository.findByCourseInAndIsActiveTrueOrderByCreatedAtDesc(courses);
        
        return assignments.stream()
                .map(assignment -> convertToStudentAssignmentResponse(assignment, student))
                .collect(Collectors.toList());
    }
    
    /**
     * Get specific student assignment by ID
     */
    public StudentAssignmentResponse getStudentAssignmentById(Long assignmentId, User student) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
        
        return convertToStudentAssignmentResponse(assignment, student);
    }
    
    // Helper methods for conversion
    private StudentQuestionResponse convertToStudentQuestionResponse(Question question, User student) {
        // Find submission answer for this question
        String userAnswer = null;
        List<Long> selectedOptionIds = new ArrayList<>();
        
        // Try to find existing answer from submission
        submissionRepository.findByAssignmentAndStudent(question.getAssignment(), student)
                .ifPresent(submission -> {
                    // For now, we'll handle basic cases
                    // This can be expanded based on your submission structure
                });
        
        StudentQuestionResponse response = new StudentQuestionResponse();
        response.setId(question.getId());
        response.setTitle(question.getTitle());
        response.setDescription(question.getDescription());
        response.setQuestionType(question.getQuestionType());
        response.setPoints(question.getPoints());
        response.setOrderIndex(question.getOrderIndex());
        response.setPublicTestCases(question.getTestCases().stream()
                .filter(testCase -> !testCase.getIsHidden()) // Use isHidden instead of isPublic
                .map(this::convertToPublicTestCaseResponse)
                .collect(Collectors.toList()));
        // Set exampleTestCases as the same as publicTestCases for frontend compatibility
        response.setExampleTestCases(question.getTestCases().stream()
                .filter(testCase -> !testCase.getIsHidden())
                .map(this::convertToPublicTestCaseResponse)
                .collect(Collectors.toList()));
        response.setOptions(question.getQuestionOptions().stream() // Use questionOptions instead of options
                .map(this::convertToQuestionOptionResponse)
                .collect(Collectors.toList()));
        response.setLanguage(question.getProgrammingLanguage());
        response.setProgrammingLanguage(question.getProgrammingLanguage());
        // response.setStarterCode(question.getStarterCode()); // No starterCode field in entity
        response.setTotalTestCases(question.getTestCases().size());
        response.setIsAnswered(userAnswer != null && !userAnswer.trim().isEmpty());
        response.setUserAnswer(userAnswer);
        response.setSelectedOptionIds(selectedOptionIds);
        response.setFunctionName(question.getFunctionName());
        response.setFunctionSignature(question.getFunctionSignature());
        response.setHasReferenceImplementation(question.getReferenceImplementation() != null);
        
        return response;
    }
    
    private PublicTestCaseResponse convertToPublicTestCaseResponse(TestCase testCase) {
        return new PublicTestCaseResponse(
                testCase.getId(),
                testCase.getInput(),
                testCase.getExpectedOutput(),
                testCase.getWeight()
        );
    }
    
    private QuestionOptionResponse convertToQuestionOptionResponse(QuestionOption option) {
        return new QuestionOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getOptionOrder()
        );
    }
}