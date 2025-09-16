package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.response.CourseResponse;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.security.UserPrincipal;
import iuh.fit.cscore_be.service.CourseService;
import iuh.fit.cscore_be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    
    private final CourseService courseService;
    private final UserService userService;
    
    // Student endpoints - for viewing available courses and enrolling
    @GetMapping("/available")
    public ResponseEntity<List<CourseResponse>> getAvailableCourses() {
        List<CourseResponse> courses = courseService.getAvailableCourses();
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/enrolled")
    public ResponseEntity<List<CourseResponse>> getEnrolledCourses(
            @AuthenticationPrincipal UserPrincipal studentPrincipal) {
        User student = userService.findById(studentPrincipal.getId());
        List<CourseResponse> courses = courseService.getEnrolledCourses(student);
        return ResponseEntity.ok(courses);
    }
    
    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<Void> enrollInCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal studentPrincipal) {
        User student = userService.findById(studentPrincipal.getId());
        courseService.selfEnrollInCourse(courseId, student);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{courseId}/unenroll")
    public ResponseEntity<Void> unenrollFromCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal studentPrincipal) {
        User student = userService.findById(studentPrincipal.getId());
        courseService.unenrollFromCourse(courseId, student);
        return ResponseEntity.ok().build();
    }
}
