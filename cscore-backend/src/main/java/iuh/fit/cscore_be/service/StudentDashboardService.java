package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.*;
import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentDashboardService {
    
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;
    private final CourseRepository courseRepository;
    
    public StudentDashboardResponse getDashboardData(User student) {
        // Get enrolled courses
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        List<Course> courses = enrollments.stream()
            .map(Enrollment::getCourse)
            .collect(Collectors.toList());
        
        // Basic statistics
        Long totalCourses = (long) courses.size();
        Long totalAssignments = courses.stream()
            .mapToLong(course -> assignmentRepository.findByCourseAndIsActiveTrue(course).size())
            .sum();
        
        List<Submission> submissions = submissionRepository.findByStudent(student);
        Long submittedAssignments = (long) submissions.size();
        
        Double averageScore = submissions.stream()
            .filter(s -> s.getScore() != null)
            .mapToDouble(Submission::getScore)
            .average()
            .orElse(0.0);
        
        // Recent data
        List<StudentCourseResponse> enrolledCourses = enrollments.stream()
            .limit(5)
            .map(this::convertToCourseResponse)
            .collect(Collectors.toList());
        
        List<StudentAssignmentResponse> recentAssignments = getRecentAssignments(student, courses);
        
        List<SubmissionResponse> recentSubmissions = submissions.stream()
            .sorted((s1, s2) -> s2.getSubmissionTime().compareTo(s1.getSubmissionTime()))
            .limit(5)
            .map(this::convertToSubmissionResponse)
            .collect(Collectors.toList());
        
        // Statistics
        StudentDashboardResponse.StudentStatistics statistics = calculateStatistics(student, submissions);
        
        return new StudentDashboardResponse(
            totalCourses,
            totalAssignments,
            submittedAssignments,
            averageScore,
            enrolledCourses,
            recentAssignments,
            recentSubmissions,
            statistics
        );
    }
    
    public List<StudentAssignmentResponse> getAllAssignmentsForStudent(User student) {
        // Get enrolled courses
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        List<Course> courses = enrollments.stream()
            .map(Enrollment::getCourse)
            .collect(Collectors.toList());
            
        return courses.stream()
            .flatMap(course -> assignmentRepository.findByCourseAndIsActiveTrue(course).stream())
            .map(assignment -> convertToAssignmentResponse(assignment, student))
            .collect(Collectors.toList());
    }
    
    public StudentAssignmentResponse getAssignmentForStudent(Long assignmentId, User student) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));
            
        // Check if student is enrolled in the course
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        boolean isEnrolled = enrollments.stream()
            .anyMatch(enrollment -> enrollment.getCourse().getId().equals(assignment.getCourse().getId()));
            
        if (!isEnrolled) {
            throw new RuntimeException("Student is not enrolled in this course");
        }
        
        return convertToAssignmentResponse(assignment, student);
    }
    
    private List<StudentAssignmentResponse> getRecentAssignments(User student, List<Course> courses) {
        return courses.stream()
            .flatMap(course -> assignmentRepository.findByCourseAndIsActiveTrue(course).stream())
            .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
            .limit(5)
            .map(assignment -> convertToAssignmentResponse(assignment, student))
            .collect(Collectors.toList());
    }
    
    private StudentDashboardResponse.StudentStatistics calculateStatistics(User student, List<Submission> submissions) {
        Long totalSubmissions = (long) submissions.size();
        Long pendingSubmissions = submissions.stream()
            .mapToLong(s -> s.getStatus() == SubmissionStatus.SUBMITTED ? 1 : 0)
            .sum();
        Long gradedSubmissions = submissions.stream()
            .mapToLong(s -> s.getStatus() == SubmissionStatus.GRADED ? 1 : 0)
            .sum();
        
        Double bestScore = submissions.stream()
            .filter(s -> s.getScore() != null)
            .mapToDouble(Submission::getScore)
            .max()
            .orElse(0.0);
        
        Integer coursesEnrolled = enrollmentRepository.findByStudentAndIsActiveTrue(student).size();
        
        return new StudentDashboardResponse.StudentStatistics(
            totalSubmissions,
            pendingSubmissions,
            gradedSubmissions,
            bestScore,
            coursesEnrolled
        );
    }
    
    private StudentCourseResponse convertToCourseResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        List<Assignment> assignments = assignmentRepository.findByCourseAndIsActiveTrue(course);
        
        List<Submission> studentSubmissions = submissionRepository.findByStudent(enrollment.getStudent());
        int completedAssignments = (int) assignments.stream()
            .mapToLong(assignment -> 
                studentSubmissions.stream()
                    .anyMatch(submission -> submission.getAssignment().getId().equals(assignment.getId())) ? 1 : 0
            )
            .sum();
        
        return new StudentCourseResponse(
            course.getId(),
            course.getName(),
            course.getCode(),
            course.getDescription(),
            course.getSemester(),
            course.getAcademicYear(),
            course.getTeacher().getFullName(),
            enrollment.getEnrollmentDate(),
            enrollment.getFinalGrade(),
            assignments.size(),
            completedAssignments
        );
    }
    
    private StudentAssignmentResponse convertToAssignmentResponse(Assignment assignment, User student) {
        Submission submission = submissionRepository.findByAssignmentAndStudent(assignment, student)
            .orElse(null);
        
        // Convert questions to student format
        List<StudentQuestionResponse> questions = assignment.getQuestions().stream()
            .sorted((q1, q2) -> q1.getOrderIndex().compareTo(q2.getOrderIndex())) // Sort by orderIndex
            .map(question -> convertToStudentQuestionResponse(question, student))
            .collect(Collectors.toList());
        
        return new StudentAssignmentResponse(
            assignment.getId(),
            assignment.getTitle(),
            assignment.getDescription(),
            assignment.getRequirements(),
            assignment.getType(),
            assignment.getCourse().getId(),
            assignment.getCourse().getName(),
            assignment.getMaxScore(),
            assignment.getTimeLimit(),
            assignment.getStartTime(),
            assignment.getEndTime(),
            assignment.getAllowLateSubmission(),
            submission != null,
            submission != null ? submission.getScore() : null,
            submission != null ? submission.getSubmissionTime() : null,
            submission != null ? submission.getStatus().name() : null,
            null, // Public test cases will be loaded separately
            assignment.getQuestions().size(), // totalQuestions - ADD this field
            assignment.getQuestions().stream().mapToInt(q -> q.getTestCases().size()).sum(), // totalTestCases
            questions, // Add questions list
            assignment.getCreatedAt()
        );
    }
    
    private SubmissionResponse convertToSubmissionResponse(Submission submission) {
        int totalTestCases = submission.getAssignment().getQuestions().stream()
            .mapToInt(q -> q.getTestCases().size()).sum();
        int testCasesPassed = (int) submission.getTestResults().stream()
            .mapToLong(result -> result.getIsPassed() ? 1 : 0)
            .sum();
        
        return new SubmissionResponse(
            submission.getId(),
            submission.getAssignment().getId(), // Add missing assignmentId
            submission.getAssignment().getTitle(),
            submission.getStudent().getFullName(),
            submission.getStudent().getStudentId(),
            submission.getProgrammingLanguage(),
            submission.getStatus(),
            submission.getScore(),
            submission.getExecutionTime(),
            submission.getMemoryUsed(),
            submission.getFeedback(),
            submission.getSubmissionTime(),
            submission.getGradedTime(),
            testCasesPassed,
            totalTestCases
        );
    }
    
    private StudentQuestionResponse convertToStudentQuestionResponse(Question question, User student) {
        // Get public test cases for this question (example test cases)
        List<PublicTestCaseResponse> publicTestCases = testCaseRepository.findByQuestionAndIsHiddenFalse(question)
            .stream()
            .map(this::convertToPublicTestCaseResponse)
            .collect(Collectors.toList());
        
        // Get question options (for multiple choice questions)
        List<QuestionOptionResponse> options = question.getQuestionOptions()
            .stream()
            .sorted((o1, o2) -> o1.getOptionOrder().compareTo(o2.getOptionOrder()))
            .map(option -> new QuestionOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getOptionOrder()
            ))
            .collect(Collectors.toList());
        
        // Check if student has answered this question
        boolean isAnswered = false;
        String userAnswer = null;
        List<Long> selectedOptionIds = new ArrayList<>();
        
        // Calculate total test cases for this question
        int totalTestCases = question.getTestCases().size();
        
        return new StudentQuestionResponse(
            question.getId(),
            question.getTitle(),
            question.getDescription(),
            question.getQuestionType(),
            question.getPoints(),
            question.getOrderIndex(),
            publicTestCases, // This serves as both publicTestCases and exampleTestCases
            options,
            isAnswered,
            userAnswer,
            selectedOptionIds,
            // Enhanced fields for programming questions
            null, // starterCode - not available in Question entity, should be provided separately
            publicTestCases, // exampleTestCases (same as publicTestCases)
            "java", // language - default to java, should be configurable per question
            totalTestCases // totalTestCases
        );
    }
    
    private PublicTestCaseResponse convertToPublicTestCaseResponse(TestCase testCase) {
        return new PublicTestCaseResponse(
            testCase.getId(),
            testCase.getInput(),
            testCase.getExpectedOutput(),
            testCase.getWeight()
        );
    }
}
