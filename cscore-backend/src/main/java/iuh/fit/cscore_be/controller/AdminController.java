package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.request.CreateUserRequest;
import iuh.fit.cscore_be.dto.request.UpdateUserRequest;
import iuh.fit.cscore_be.dto.request.CreateCourseRequest;
import iuh.fit.cscore_be.dto.request.UpdateCourseRequest;
import iuh.fit.cscore_be.dto.response.UserResponse;
import iuh.fit.cscore_be.dto.response.CourseResponse;
import iuh.fit.cscore_be.dto.response.DetailedCourseResponse;
import iuh.fit.cscore_be.dto.response.StudentResponse;
import iuh.fit.cscore_be.dto.response.MessageResponse;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.service.AuthService;
import iuh.fit.cscore_be.service.UserService;
import iuh.fit.cscore_be.service.AdminCourseManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AuthService authService;
    private final UserService userService;
    private final AdminCourseManagementService courseManagementService;
    
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminAccess() {
        UserService.UserStatsResponse stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }
    
    // ============ USER MANAGEMENT APIs ============
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<UserResponse> user = userService.getUserById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        return authService.createUserByAdmin(createUserRequest);
    }
    
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                       @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        return userService.updateUser(id, updateUserRequest);
    }
    
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
    
    @PatchMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        return userService.toggleUserStatus(id);
    }
    
    // ============ STUDENT MANAGEMENT APIs ============
    
    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllStudents() {
        List<UserResponse> students = userService.getAllStudents();
        return ResponseEntity.ok(students);
    }
    
    @GetMapping("/students/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getStudentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserResponse> students = userService.getUsersByRole(Role.STUDENT, page, size);
        return ResponseEntity.ok(students);
    }
    
    // ============ TEACHER MANAGEMENT APIs ============
    
    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllTeachers() {
        List<UserResponse> teachers = userService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }
    
    @GetMapping("/teachers/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getTeachersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserResponse> teachers = userService.getUsersByRole(Role.TEACHER, page, size);
        return ResponseEntity.ok(teachers);
    }
    
    // ============ SEARCH APIs ============
    
    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String keyword) {
        List<UserResponse> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }
    
    // ============ COURSE MANAGEMENT APIs ============
    
    @GetMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        List<CourseResponse> courses = courseManagementService.getAllActiveCourses();
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CourseResponse>> getCoursesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CourseResponse> courses = courseManagementService.getAllCourses(pageable);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DetailedCourseResponse> getCourseById(@PathVariable Long courseId) {
        DetailedCourseResponse course = courseManagementService.getCourseById(courseId);
        return ResponseEntity.ok(course);
    }
    
    @PostMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DetailedCourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        DetailedCourseResponse course = courseManagementService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }
    
    @PutMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DetailedCourseResponse> updateCourse(
            @PathVariable Long courseId, 
            @Valid @RequestBody UpdateCourseRequest request) {
        DetailedCourseResponse course = courseManagementService.updateCourse(courseId, request);
        return ResponseEntity.ok(course);
    }
    
    @DeleteMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteCourse(@PathVariable Long courseId) {
        courseManagementService.deleteCourse(courseId);
        return ResponseEntity.ok(new MessageResponse("Khóa học đã được vô hiệu hóa thành công"));
    }
    
    @PostMapping("/courses/{courseId}/teachers/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> assignTeacherToCourse(
            @PathVariable Long courseId, 
            @PathVariable Long teacherId) {
        courseManagementService.assignTeacherToCourse(courseId, teacherId);
        return ResponseEntity.ok(new MessageResponse("Giáo viên đã được phân công vào khóa học thành công"));
    }
    
    @PostMapping("/courses/{courseId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> enrollStudentToCourse(
            @PathVariable Long courseId, 
            @PathVariable Long studentId) {
        courseManagementService.enrollStudentInCourse(courseId, studentId);
        return ResponseEntity.ok(new MessageResponse("Sinh viên đã được thêm vào khóa học thành công"));
    }
    
    @DeleteMapping("/courses/{courseId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> removeStudentFromCourse(
            @PathVariable Long courseId, 
            @PathVariable Long studentId) {
        courseManagementService.removeStudentFromCourse(courseId, studentId);
        return ResponseEntity.ok(new MessageResponse("Sinh viên đã được xóa khỏi khóa học thành công"));
    }
    
    @GetMapping("/courses/{courseId}/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentResponse>> getStudentsInCourse(@PathVariable Long courseId) {
        List<StudentResponse> students = courseManagementService.getStudentsInCourse(courseId);
        return ResponseEntity.ok(students);
    }
    
    // ============ STATISTICS APIs ============
    
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserService.UserStatsResponse> getUserStats() {
        UserService.UserStatsResponse stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }
}
