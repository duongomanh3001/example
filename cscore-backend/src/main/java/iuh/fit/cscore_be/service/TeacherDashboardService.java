package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.*;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherDashboardService {
    
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    
    public TeacherDashboardResponse getDashboardData(User teacher) {
        // Thống kê tổng quan
        Long totalCourses = (long) courseRepository.findByTeacherAndIsActiveTrue(teacher).size();
        Long totalStudents = enrollmentRepository.findAllByTeacher(teacher).stream()
            .mapToLong(enrollment -> 1L)
            .sum();
        Long totalAssignments = (long) assignmentRepository.findByTeacherId(teacher.getId()).size();
        Long pendingSubmissions = submissionRepository.countPendingSubmissionsByTeacher(teacher);
        
        // Khóa học gần đây
        List<Course> recentCourses = courseRepository.findRecentCoursesByTeacher(teacher)
            .stream()
            .limit(5)
            .collect(Collectors.toList());
        
        List<CourseResponse> recentCourseResponses = recentCourses.stream()
            .map(this::convertToCourseResponse)
            .collect(Collectors.toList());
        
        // Bài tập gần đây
        List<Assignment> recentAssignments = assignmentRepository.findByTeacherId(teacher.getId())
            .stream()
            .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
            .limit(5)
            .collect(Collectors.toList());
        
        List<AssignmentResponse> recentAssignmentResponses = recentAssignments.stream()
            .map(this::convertToAssignmentResponse)
            .collect(Collectors.toList());
        
        // Bài nộp cần chấm
        List<Submission> pendingGrades = submissionRepository.findByTeacherAndStatus(teacher, SubmissionStatus.SUBMITTED)
            .stream()
            .limit(10)
            .collect(Collectors.toList());
        
        List<SubmissionResponse> pendingGradeResponses = pendingGrades.stream()
            .map(this::convertToSubmissionResponse)
            .collect(Collectors.toList());
        
        // Thống kê chi tiết
        TeacherDashboardResponse.TeacherStatistics statistics = calculateStatistics(teacher);
        
        return new TeacherDashboardResponse(
            totalCourses,
            totalStudents,
            totalAssignments,
            pendingSubmissions,
            recentCourseResponses,
            recentAssignmentResponses,
            pendingGradeResponses,
            statistics
        );
    }
    
    private TeacherDashboardResponse.TeacherStatistics calculateStatistics(User teacher) {
        List<Submission> allSubmissions = submissionRepository.findByTeacher(teacher);
        
        Double averageScore = allSubmissions.stream()
            .filter(s -> s.getScore() != null)
            .mapToDouble(Submission::getScore)
            .average()
            .orElse(0.0);
        
        Long totalSubmissions = (long) allSubmissions.size();
        
        Long gradedSubmissions = allSubmissions.stream()
            .mapToLong(s -> s.getStatus() == SubmissionStatus.GRADED ? 1 : 0)
            .sum();
        
        Integer activeStudents = enrollmentRepository.findAllByTeacher(teacher).size();
        
        return new TeacherDashboardResponse.TeacherStatistics(
            averageScore,
            totalSubmissions,
            gradedSubmissions,
            activeStudents
        );
    }
    
    private CourseResponse convertToCourseResponse(Course course) {
        Long studentCount = enrollmentRepository.countByCourseAndIsActiveTrue(course);
        
        return new CourseResponse(
            course.getId(),
            course.getName(),
            course.getCode(),
            course.getDescription(),
            course.getCreditHours(),
            course.getSemester(),
            course.getAcademicYear(),
            course.getTeacher().getFullName(),
            studentCount,
            (long) course.getAssignments().size(),
            course.getIsActive(),
            course.getCreatedAt()
        );
    }
    
    private AssignmentResponse convertToAssignmentResponse(Assignment assignment) {
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
    
    private SubmissionResponse convertToSubmissionResponse(Submission submission) {
        int totalTestCases = submission.getAssignment().getQuestions().stream()
            .mapToInt(question -> question.getTestCases().size())
            .sum();
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
}
