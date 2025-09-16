package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Enrollment;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.repository.CourseRepository;
import iuh.fit.cscore_be.repository.EnrollmentRepository;
import iuh.fit.cscore_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CsvImportService {
    
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGeneratorService idGeneratorService;
    
    public int importTeachersFromCsv(MultipartFile file) throws IOException {
        log.info("Starting CSV import for teachers");
        
        validateCsvFile(file);
        
        List<User> teachers = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                try {
                    User teacher = parseTeacherFromCsvLine(line, lineNumber);
                    if (teacher != null) {
                        teachers.add(teacher);
                    }
                } catch (Exception e) {
                    log.error("Error parsing line {}: {}", lineNumber, e.getMessage());
                    throw new RuntimeException("Lỗi tại dòng " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        
        // Save all teachers
        userRepository.saveAll(teachers);
        
        log.info("Successfully imported {} teachers", teachers.size());
        return teachers.size();
    }
    
    public int importStudentsFromCsv(MultipartFile file) throws IOException {
        log.info("Starting CSV import for students");
        
        validateCsvFile(file);
        
        List<User> students = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                try {
                    User student = parseStudentFromCsvLine(line, lineNumber);
                    if (student != null) {
                        students.add(student);
                    }
                } catch (Exception e) {
                    log.error("Error parsing line {}: {}", lineNumber, e.getMessage());
                    throw new RuntimeException("Lỗi tại dòng " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        
        // Save all students
        userRepository.saveAll(students);
        
        log.info("Successfully imported {} students", students.size());
        return students.size();
    }
    
    public int importCourseEnrollmentFromCsv(Long courseId, MultipartFile file) throws IOException {
        log.info("Starting CSV import for course enrollment, courseId: {}", courseId);
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học với ID: " + courseId));
        
        validateCsvFile(file);
        
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                try {
                    Enrollment enrollment = parseEnrollmentFromCsvLine(line, course, lineNumber);
                    if (enrollment != null) {
                        enrollments.add(enrollment);
                    }
                } catch (Exception e) {
                    log.error("Error parsing line {}: {}", lineNumber, e.getMessage());
                    throw new RuntimeException("Lỗi tại dòng " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        
        // Save all enrollments
        enrollmentRepository.saveAll(enrollments);
        
        log.info("Successfully enrolled {} students to course {}", enrollments.size(), courseId);
        return enrollments.size();
    }
    
    private void validateCsvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File CSV không được để trống");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new RuntimeException("File phải có định dạng CSV");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new RuntimeException("File không được vượt quá 5MB");
        }
    }
    
    private User parseTeacherFromCsvLine(String line, int lineNumber) {
        String[] fields = line.split(",");
        
        if (fields.length < 4) {
            throw new RuntimeException("Dòng phải có ít nhất 4 trường: username,email,fullName,password");
        }
        
        String username = fields[0].trim();
        String email = fields[1].trim();
        String fullName = fields[2].trim();
        String password = fields[3].trim();
        
        // Validate required fields
        if (username.isEmpty() || email.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            throw new RuntimeException("Các trường bắt buộc không được để trống");
        }
        
        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            log.warn("Teacher with username {} already exists, skipping", username);
            return null;
        }
        
        if (userRepository.existsByEmail(email)) {
            log.warn("Teacher with email {} already exists, skipping", email);
            return null;
        }
        
        User teacher = new User();
        teacher.setUsername(username);
        teacher.setEmail(email);
        teacher.setFullName(fullName);
        teacher.setPassword(passwordEncoder.encode(password));
        teacher.setRole(Role.TEACHER);
        teacher.setIsActive(true);
        
        // Tự động sinh mã giáo viên
        String teacherId;
        do {
            teacherId = idGeneratorService.generateTeacherId();
        } while (userRepository.existsByTeacherId(teacherId));
        teacher.setTeacherId(teacherId);
        
        return teacher;
    }
    
    private User parseStudentFromCsvLine(String line, int lineNumber) {
        String[] fields = line.split(",");
        
        if (fields.length < 4) {
            throw new RuntimeException("Dòng phải có ít nhất 4 trường: username,email,fullName,password");
        }
        
        String username = fields[0].trim();
        String email = fields[1].trim();
        String fullName = fields[2].trim();
        String password = fields[3].trim();
        
        // Validate required fields
        if (username.isEmpty() || email.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            throw new RuntimeException("Các trường bắt buộc không được để trống");
        }
        
        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            log.warn("Student with username {} already exists, skipping", username);
            return null;
        }
        
        if (userRepository.existsByEmail(email)) {
            log.warn("Student with email {} already exists, skipping", email);
            return null;
        }
        
        User student = new User();
        student.setUsername(username);
        student.setEmail(email);
        student.setFullName(fullName);
        student.setPassword(passwordEncoder.encode(password));
        student.setRole(Role.STUDENT);
        student.setIsActive(true);
        
        // Tự động sinh mã sinh viên
        String studentId;
        do {
            studentId = idGeneratorService.generateStudentId();
        } while (userRepository.existsByStudentId(studentId));
        student.setStudentId(studentId);
        
        return student;
    }
    
    private Enrollment parseEnrollmentFromCsvLine(String line, Course course, int lineNumber) {
        String[] fields = line.split(",");
        
        if (fields.length < 2) {
            throw new RuntimeException("Dòng phải có ít nhất 2 trường: username hoặc email");
        }
        
        String identifier = fields[0].trim(); // Can be username or email
        
        // Find student by username or email
        User student = userRepository.findByUsername(identifier).orElse(null);
        if (student == null) {
            student = userRepository.findByEmail(identifier).orElse(null);
        }
        
        if (student == null) {
            throw new RuntimeException("Không tìm thấy sinh viên với identifier: " + identifier);
        }
        
        if (!student.getRole().equals(Role.STUDENT)) {
            throw new RuntimeException("User " + identifier + " không phải là sinh viên");
        }
        
        // Check if already enrolled
        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            log.warn("Student {} already enrolled in course {}, skipping", identifier, course.getId());
            return null;
        }
        
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setIsActive(true);
        
        return enrollment;
    }
}
