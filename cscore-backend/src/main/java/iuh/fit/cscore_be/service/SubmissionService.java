package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.SubmissionResponse;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.AssignmentRepository;
import iuh.fit.cscore_be.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionService {
    
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    
    public List<SubmissionResponse> getSubmissionsByTeacher(User teacher) {
        List<Submission> submissions = submissionRepository.findByTeacher(teacher);
        return submissions.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public Page<SubmissionResponse> getSubmissionsByTeacher(User teacher, Pageable pageable) {
        Page<Submission> submissions = submissionRepository.findByTeacher(teacher, pageable);
        return submissions.map(this::convertToResponse);
    }
    
    public List<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId, User teacher) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem bài nộp của bài tập này");
        }
        
        List<Submission> submissions = submissionRepository.findByAssignment(assignment);
        return submissions.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public Page<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId, User teacher, Pageable pageable) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem bài nộp của bài tập này");
        }
        
        Page<Submission> submissions = submissionRepository.findByAssignment(assignment, pageable);
        return submissions.map(this::convertToResponse);
    }
    
    public List<SubmissionResponse> getPendingSubmissions(User teacher) {
        List<Submission> submissions = submissionRepository.findByTeacherAndStatus(teacher, SubmissionStatus.SUBMITTED);
        return submissions.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public SubmissionResponse getSubmissionById(Long submissionId, User teacher) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));
        
        if (!submission.getAssignment().getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem bài nộp này");
        }
        
        return convertToResponse(submission);
    }
    
    public void gradeSubmission(Long submissionId, Double score, String feedback, User teacher) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));
        
        if (!submission.getAssignment().getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền chấm điểm bài nộp này");
        }
        
        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedTime(LocalDateTime.now());
        
        submissionRepository.save(submission);
    }
    
    public void updateGrade(Long submissionId, Double score, String feedback, User teacher) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));
        
        if (!submission.getAssignment().getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật điểm bài nộp này");
        }
        
        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setGradedTime(LocalDateTime.now());
        
        submissionRepository.save(submission);
    }
    
    private SubmissionResponse convertToResponse(Submission submission) {
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
}
