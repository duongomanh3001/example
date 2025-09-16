package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.response.MessageResponse;
import iuh.fit.cscore_be.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/csv")
@RequiredArgsConstructor
public class CsvImportController {
    
    private final CsvImportService csvImportService;
    
    @PostMapping("/import-teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> importTeachers(@RequestParam("file") MultipartFile file) {
        try {
            int count = csvImportService.importTeachersFromCsv(file);
            return ResponseEntity.ok(new MessageResponse(
                "Đã import thành công " + count + " giáo viên"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                "Lỗi khi import giáo viên: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/import-students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> importStudents(@RequestParam("file") MultipartFile file) {
        try {
            int count = csvImportService.importStudentsFromCsv(file);
            return ResponseEntity.ok(new MessageResponse(
                "Đã import thành công " + count + " sinh viên"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                "Lỗi khi import sinh viên: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/import-course-enrollment/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> importCourseEnrollment(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {
        try {
            int count = csvImportService.importCourseEnrollmentFromCsv(courseId, file);
            return ResponseEntity.ok(new MessageResponse(
                "Đã thêm thành công " + count + " sinh viên vào khóa học"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                "Lỗi khi thêm sinh viên vào khóa học: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/template/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> getTeacherTemplate() {
        try {
            String csvContent = "username,email,fullName,password\n" +
                    "teacher_example,teacher@example.com,Tên Giáo Viên Mẫu,password123\n";
            
            byte[] data = csvContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teachers_template.csv");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/template/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> getStudentTemplate() {
        try {
            String csvContent = "username,email,fullName,password\n" +
                    "student_example,student@example.com,Tên Sinh Viên Mẫu,password123\n";
            
            byte[] data = csvContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students_template.csv");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/template/enrollment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> getEnrollmentTemplate() {
        try {
            String csvContent = "username\n" +
                    "student_example\n";
            
            byte[] data = csvContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enrollment_template.csv");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
