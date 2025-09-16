package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.request.SubmissionRequest;
import iuh.fit.cscore_be.dto.response.StudentAssignmentResponse;
import iuh.fit.cscore_be.dto.response.StudentDashboardResponse;
import iuh.fit.cscore_be.dto.response.SubmissionResponse;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.security.JwtUtils;
import iuh.fit.cscore_be.security.UserPrincipal;
import iuh.fit.cscore_be.service.StudentDashboardService;
import iuh.fit.cscore_be.service.StudentService;
import iuh.fit.cscore_be.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class StudentController {

    private final StudentService studentService;
    private final StudentDashboardService studentDashboardService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @GetMapping("/health")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Student service is running!");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentDashboardResponse> getDashboard(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User student = userService.findById(userPrincipal.getId());
        StudentDashboardResponse dashboard = studentDashboardService.getDashboardData(student);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAssignmentResponse>> getAssignments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User student = userService.findById(userPrincipal.getId());
        List<StudentAssignmentResponse> assignments = studentDashboardService.getAllAssignmentsForStudent(student);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAssignmentResponse> getAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User student = userService.findById(userPrincipal.getId());
        StudentAssignmentResponse assignment = studentDashboardService.getAssignmentForStudent(assignmentId, student);
        return ResponseEntity.ok(assignment);
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmissionRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        User student = userService.findByUsername(username);
        
        // Set assignment ID from path
        request.setAssignmentId(assignmentId);
        
        SubmissionResponse submission = studentService.submitAssignment(assignmentId, student.getId(), request);
        
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<SubmissionResponse>> getMySubmissions(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        User student = userService.findByUsername(username);
        
        List<SubmissionResponse> submissions = studentService.getMySubmissions(student.getId());
        return ResponseEntity.ok(submissions);
    }
    
    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")  
    public ResponseEntity<SubmissionResponse> getSubmissionDetails(@PathVariable Long submissionId, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        User student = userService.findByUsername(username);
        
        SubmissionResponse submission = studentService.getSubmissionDetails(submissionId, student.getId());
        return ResponseEntity.ok(submission);
    }

    // TODO: Implement these methods gradually as needed
    /*
    @GetMapping("/assignments")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAssignmentResponse>> getAvailableAssignments(@RequestHeader("Authorization") String authHeader) {
        // Implementation needed
    }
    
    @GetMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAssignmentResponse> getAssignmentDetails(@PathVariable Long assignmentId, @RequestHeader("Authorization") String authHeader) {
        // Implementation needed
    }
    
    @PostMapping("/run-code")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<CodeExecutionResponse> runCodeWithInput(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String authHeader) {
        // Implementation needed
    }
    
    @PostMapping("/compile-run")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<CodeExecutionResponse> compileAndRunCode(@RequestBody Map<String, String> request) {
        // Implementation needed  
    }
    
    @PostMapping("/test-code")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<TestResultResponse>> testCodeWithPublicTestCases(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String authHeader) {
        // Implementation needed
    }
    */
}
