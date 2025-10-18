package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.request.AssignmentRequest;
import iuh.fit.cscore_be.dto.request.CodeValidationRequest;
import iuh.fit.cscore_be.dto.request.CreateAssignmentRequest;
import iuh.fit.cscore_be.dto.request.CourseRequest;
import iuh.fit.cscore_be.dto.request.TestCaseRequest;
import iuh.fit.cscore_be.dto.response.TestCaseResponse;
import iuh.fit.cscore_be.dto.response.*;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import iuh.fit.cscore_be.security.UserPrincipal;
import iuh.fit.cscore_be.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {
    
    private final DashboardService dashboardService;
    private final CourseService courseService;
    private final AssignmentManagementService assignmentManagementService;
    private final SubmissionService submissionService;
    private final UserService userService;
    private final AutoGradingService autoGradingService;
    private final CodeExecutionService codeExecutionService;
    private final SectionService sectionService;
    
    // ======================== DASHBOARD ========================
    
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherDashboardResponse> getDashboard(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        TeacherDashboardResponse dashboard = dashboardService.getTeacherDashboardData(teacher);
        return ResponseEntity.ok(dashboard);
    }
    
    // ======================== COURSE MANAGEMENT ========================
    
    @GetMapping("/courses")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<CourseResponse>> getCourses(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<CourseResponse> courses = courseService.getCoursesByTeacher(teacher);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/paginated")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<CourseResponse>> getCoursesPaginated(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        User teacher = userService.findById(userPrincipal.getId());
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CourseResponse> courses = courseService.getCoursesByTeacher(teacher, pageable);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseResponse> getCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        CourseResponse course = courseService.getCourseById(courseId, teacher);
        return ResponseEntity.ok(course);
    }
    
    // ======================== STUDENT MANAGEMENT ========================
    // Note: Teachers can only view students in their courses, not add/remove them
    // Only Admin can add/remove students from courses
    
    @GetMapping("/courses/{courseId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<StudentResponse>> getStudentsInCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<StudentResponse> students = courseService.getStudentsInCourse(courseId, teacher);
        return ResponseEntity.ok(students);
    }
    
    // ======================== ASSIGNMENT MANAGEMENT ========================
    
    @GetMapping("/assignments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<AssignmentResponse> assignments = assignmentManagementService.getAssignmentsByTeacher(teacher);
        return ResponseEntity.ok(assignments);
    }
    
    @GetMapping("/assignments/paginated")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<AssignmentResponse>> getAssignmentsPaginated(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        User teacher = userService.findById(userPrincipal.getId());
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AssignmentResponse> assignments = assignmentManagementService.getAssignmentsByTeacher(teacher, pageable);
        return ResponseEntity.ok(assignments);
    }
    
    @GetMapping("/courses/{courseId}/assignments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<AssignmentResponse> assignments = assignmentManagementService.getAssignmentsByCourse(courseId);
        return ResponseEntity.ok(assignments);
    }
    
    @GetMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<DetailedAssignmentResponse> getAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        DetailedAssignmentResponse assignment = assignmentManagementService.getAssignmentById(assignmentId, teacher);
        return ResponseEntity.ok(assignment);
    }
    
    @PostMapping("/assignments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<DetailedAssignmentResponse> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("📝 Creating assignment '{}' for course {} with sectionId: {}", 
                request.getTitle(), request.getCourseId(), request.getSectionId());
        User teacher = userService.findById(userPrincipal.getId());
        DetailedAssignmentResponse assignment = assignmentManagementService.createAssignmentWithQuestions(request, teacher);
        log.info("✅ Assignment created successfully with ID: {}", assignment.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }
    
    @PutMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<DetailedAssignmentResponse> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        DetailedAssignmentResponse assignment = assignmentManagementService.updateAssignment(assignmentId, request, teacher);
        return ResponseEntity.ok(assignment);
    }
    
    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> deleteAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        assignmentManagementService.deleteAssignment(assignmentId, teacher);
        return ResponseEntity.ok(new MessageResponse("Bài tập đã được vô hiệu hóa thành công"));
    }
    
    @PatchMapping("/assignments/{assignmentId}/toggle-status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<DetailedAssignmentResponse> toggleAssignmentStatus(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        DetailedAssignmentResponse assignment = assignmentManagementService.toggleAssignmentStatus(assignmentId, teacher);
        return ResponseEntity.ok(assignment);
    }
    
    // ======================== TEST CASE MANAGEMENT ========================
    
    @GetMapping("/assignments/{assignmentId}/test-cases")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TestCaseResponse>> getTestCases(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<TestCase> testCases = assignmentManagementService.getTestCases(assignmentId, teacher);
        
        List<TestCaseResponse> testCaseResponses = testCases.stream()
            .map(testCase -> new TestCaseResponse(
                testCase.getId(),
                testCase.getInput(),
                testCase.getExpectedOutput(),
                testCase.getTestCode(),
                testCase.getIsHidden(),
                testCase.getWeight(),
                testCase.getTimeLimit(),
                testCase.getMemoryLimit()
            ))
            .collect(java.util.stream.Collectors.toList());
            
        return ResponseEntity.ok(testCaseResponses);
    }
    
    @PostMapping("/assignments/{assignmentId}/test-cases")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> addTestCase(
            @PathVariable Long assignmentId,
            @Valid @RequestBody TestCaseRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        assignmentManagementService.addTestCase(assignmentId, request, teacher);
        return ResponseEntity.ok(new MessageResponse("Test case đã được thêm thành công"));
    }
    
    @PutMapping("/test-cases/{testCaseId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> updateTestCase(
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        assignmentManagementService.updateTestCase(testCaseId, request, teacher);
        return ResponseEntity.ok(new MessageResponse("Test case đã được cập nhật thành công"));
    }
    
    @DeleteMapping("/test-cases/{testCaseId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> deleteTestCase(
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        assignmentManagementService.deleteTestCase(testCaseId, teacher);
        return ResponseEntity.ok(new MessageResponse("Test case đã được xóa thành công"));
    }
    
    // ======================== SUBMISSION MANAGEMENT ========================
    
    @GetMapping("/submissions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SubmissionResponse>> getSubmissions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<SubmissionResponse> submissions = submissionService.getSubmissionsByTeacher(teacher);
        return ResponseEntity.ok(submissions);
    }
    
    @GetMapping("/submissions/paginated")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<SubmissionResponse>> getSubmissionsPaginated(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submissionTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        User teacher = userService.findById(userPrincipal.getId());
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SubmissionResponse> submissions = submissionService.getSubmissionsByTeacher(teacher, pageable);
        return ResponseEntity.ok(submissions);
    }
    
    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<SubmissionResponse> submissions = submissionService.getSubmissionsByAssignment(assignmentId, teacher);
        return ResponseEntity.ok(submissions);
    }
    
    @GetMapping("/submissions/pending")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        List<SubmissionResponse> submissions = submissionService.getPendingSubmissions(teacher);
        return ResponseEntity.ok(submissions);
    }
    
    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SubmissionResponse> getSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        SubmissionResponse submission = submissionService.getSubmissionById(submissionId, teacher);
        return ResponseEntity.ok(submission);
    }
    
    @PostMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestParam Double score,
            @RequestParam(required = false) String feedback,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        submissionService.gradeSubmission(submissionId, score, feedback, teacher);
        return ResponseEntity.ok(new MessageResponse("Bài nộp đã được chấm điểm thành công"));
    }
    
    @PutMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> updateGrade(
            @PathVariable Long submissionId,
            @RequestParam Double score,
            @RequestParam(required = false) String feedback,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        submissionService.updateGrade(submissionId, score, feedback, teacher);
        return ResponseEntity.ok(new MessageResponse("Điểm đã được cập nhật thành công"));
    }
    
    // ======================== AUTO-GRADING MANAGEMENT ========================
    
    @PostMapping("/assignments/{assignmentId}/regrade")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> regradeAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        
        try {
            autoGradingService.regradeAssignment(assignmentId);
            return ResponseEntity.ok(new MessageResponse("Đã khởi tạo quá trình chấm lại điểm cho tất cả bài nộp"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Lỗi khi chấm lại điểm: " + e.getMessage()));
        }
    }
    
    @GetMapping("/assignments/{assignmentId}/grading-stats")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> getGradingStats(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        
        try {
            Map<String, Object> stats = autoGradingService.getGradingStats(assignmentId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/submissions/{submissionId}/auto-grade")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MessageResponse> autoGradeSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User teacher = userService.findById(userPrincipal.getId());
        
        try {
            autoGradingService.gradeSubmissionAsync(submissionId);
            return ResponseEntity.ok(new MessageResponse("Đã khởi tạo quá trình chấm điểm tự động"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Lỗi khi chấm điểm tự động: " + e.getMessage()));
        }
    }
    
    @GetMapping("/system/compilers")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> getCompilerStatus() {
        try {
            Map<String, Object> systemRequirements = codeExecutionService.getSystemRequirements();
            return ResponseEntity.ok(systemRequirements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
        @GetMapping("/system/supported-languages")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Set<ProgrammingLanguage>> getSupportedLanguages() {
        Set<ProgrammingLanguage> supportedLanguages = new HashSet<>();
        
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            if (codeExecutionService.isLanguageSupported(lang)) {
                supportedLanguages.add(lang);
            }
        }
        
        return ResponseEntity.ok(supportedLanguages);
    }
    
    @PostMapping("/system/test-compilation")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> testCompilation(
            @RequestBody TestCompilationRequest request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Simple test C code
            String testCode = """
                #include <stdio.h>
                
                int main() {
                    printf("Hello, CScore!");
                    return 0;
                }
                """;
            
            CodeExecutionResponse response = codeExecutionService.executeCode(testCode, "c");
            
            result.put("success", response.isSuccess());
            result.put("output", response.getOutput());
            result.put("error", response.getError());
            result.put("compilationError", response.getCompilationError());
            result.put("language", "c");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Test compilation failed: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    // DTO for test compilation request
    public static class TestCompilationRequest {
        private String language;
        private String code;
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
    
    // ======================== CODE VALIDATION ========================
    
    /**
     * Validate teacher's answer code by executing it with test cases
     * This is used during question creation to auto-generate expected outputs
     */
    @PostMapping("/validate-code")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> validateAnswerCode(
            @Valid @RequestBody CodeValidationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            log.info("Teacher {} is validating code for language: {}", 
                    userPrincipal.getUsername(), request.getLanguage());
            log.info("Code to execute: {}", request.getCode());
            log.info("Input: {}", request.getInput());
            
            // Use the hybrid code execution service to execute the code
            long startTime = System.currentTimeMillis();
            var result = codeExecutionService.executeCodeWithInput(
                    request.getCode(), 
                    request.getLanguage(), 
                    request.getInput()
            );
            long duration = System.currentTimeMillis() - startTime;
            
            // Prepare response in the format expected by frontend
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("output", result.getOutput());
            response.put("error", result.getError());
            response.put("compilationError", result.getCompilationError());
            response.put("executionTime", duration);
            response.put("language", request.getLanguage());
            
            log.info("Code validation completed in {}ms, success: {}", duration, result.isSuccess());
            log.info("Execution result - Output: {}, Error: {}", result.getOutput(), result.getError());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error validating teacher's code", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Code validation failed: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    // ======================== SECTIONS ========================
    
    @GetMapping("/courses/{courseId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<iuh.fit.cscore_be.dto.response.SectionResponse>> getSectionsByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Teacher {} getting sections for course {}", userPrincipal.getUsername(), courseId);
        List<iuh.fit.cscore_be.dto.response.SectionResponse> sections = sectionService.getSectionsByCourse(courseId);
        return ResponseEntity.ok(sections);
    }
    
    @PostMapping("/courses/{courseId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<iuh.fit.cscore_be.dto.response.SectionResponse> createSection(
            @PathVariable Long courseId,
            @Valid @RequestBody iuh.fit.cscore_be.dto.request.CreateSectionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Teacher {} creating section for course {}", userPrincipal.getUsername(), courseId);
        request.setCourseId(courseId);
        iuh.fit.cscore_be.dto.response.SectionResponse section = sectionService.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(section);
    }
    
    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<iuh.fit.cscore_be.dto.response.SectionResponse> updateSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody iuh.fit.cscore_be.dto.request.UpdateSectionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Teacher {} updating section {}", userPrincipal.getUsername(), sectionId);
        iuh.fit.cscore_be.dto.response.SectionResponse section = sectionService.updateSection(sectionId, request);
        return ResponseEntity.ok(section);
    }
    
    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long sectionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Teacher {} deleting section {}", userPrincipal.getUsername(), sectionId);
        sectionService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }
    
    // Note: toggle-collapse endpoint removed - handled on frontend only
    
    @PutMapping("/courses/{courseId}/sections/reorder")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<iuh.fit.cscore_be.dto.response.SectionResponse>> reorderSections(
            @PathVariable Long courseId,
            @RequestBody Map<String, List<Long>> request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Teacher {} reordering sections for course {}", userPrincipal.getUsername(), courseId);
        List<Long> sectionIds = request.get("sectionIds");
        List<iuh.fit.cscore_be.dto.response.SectionResponse> sections = sectionService.reorderSections(courseId, sectionIds);
        return ResponseEntity.ok(sections);
    }
}
