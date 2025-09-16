package iuh.fit.cscore_be.config;

import iuh.fit.cscore_be.entity.*;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.enums.AssignmentType;
import iuh.fit.cscore_be.repository.*;
import iuh.fit.cscore_be.service.IdGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private TestCaseRepository testCaseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private IdGeneratorService idGeneratorService;
    
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultAdmin();
        updateExistingUsersWithIds(); // Thêm method này
        initializeTestData();
    }
    
    private void initializeDefaultAdmin() {
        // Kiểm tra xem đã có admin chưa
        if (userRepository.existsByRole(Role.ADMIN)) {
            System.out.println("Admin user already exists, skipping initialization");
            return;
        }
        
        // Tạo admin mặc định
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@cscore.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFullName("System Administrator");
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        
        userRepository.save(admin);
        
        System.out.println("✅ Default admin user created:");
        System.out.println("Username: admin");
        System.out.println("Password: admin123");
        System.out.println("Email: admin@cscore.com");
        System.out.println("Role: ADMIN");
        
        // Tạo thêm admin user khác để test
        User admin2 = new User();
        admin2.setUsername("superadmin");
        admin2.setEmail("superadmin@cscore.com");
        admin2.setPassword(passwordEncoder.encode("123456"));
        admin2.setFullName("Super Administrator");
        admin2.setRole(Role.ADMIN);
        admin2.setIsActive(true);
        
        userRepository.save(admin2);
        
        System.out.println("✅ Additional admin user created:");
        System.out.println("Username: superadmin");
        System.out.println("Password: 123456");
        System.out.println("Email: superadmin@cscore.com");
        System.out.println("Role: ADMIN");
    }
    
    private void initializeTestData() {
        // Kiểm tra xem đã có test data chưa
        if (courseRepository.count() > 0) {
            System.out.println("Test data already exists, skipping initialization");
            return;
        }
        
        // Tạo teacher
        User teacher = new User();
        teacher.setUsername("teacher");
        teacher.setEmail("teacher@cscore.com");
        teacher.setPassword(passwordEncoder.encode("teacher123"));
        teacher.setFullName("Programming Teacher");
        teacher.setRole(Role.TEACHER);
        teacher.setIsActive(true);
        userRepository.save(teacher);
        
        // Tạo student
        User student = new User();
        student.setUsername("student");
        student.setEmail("student@cscore.com");
        student.setPassword(passwordEncoder.encode("student123"));
        student.setFullName("Programming Student");
        student.setRole(Role.STUDENT);
        student.setIsActive(true);
        userRepository.save(student);
        
        // Tạo course
        Course course = new Course();
        course.setName("Data Structures and Algorithms");
        course.setCode("DSA2025");
        course.setDescription("Learn fundamental data structures and algorithms");
        course.setTeacher(teacher);
        course.setIsActive(true);
        courseRepository.save(course);
        
        // Enroll student vào course
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
        
        // Tạo assignment 1: Array Sum
        Assignment assignment1 = new Assignment();
        assignment1.setTitle("Array Sum Problem");
        assignment1.setDescription("Tính tổng các phần tử trong mảng");
        assignment1.setRequirements("Viết chương trình đọc n số nguyên từ input, sau đó tính và in ra tổng của chúng.\n\n" +
                "Input:\n" +
                "- Dòng đầu: số nguyên n (1 ≤ n ≤ 1000)\n" +
                "- Dòng thứ hai: n số nguyên\n\n" +
                "Output:\n" +
                "- In ra tổng của n số nguyên\n\n" +
                "Ví dụ:\n" +
                "Input:\n" +
                "5\n" +
                "3 1 4 1 5\n\n" +
                "Output:\n" +
                "14");
        assignment1.setType(AssignmentType.EXERCISE);
        assignment1.setCourse(course);
        assignment1.setMaxScore(100.0);
        assignment1.setTimeLimit(30);
        assignment1.setStartTime(LocalDateTime.now().minusDays(1));
        assignment1.setEndTime(LocalDateTime.now().plusDays(7));
        assignment1.setIsActive(true);
        assignment1.setAllowLateSubmission(true);
        assignment1.setAutoGrade(true);
        assignmentRepository.save(assignment1);
        
        // Tạo test cases cho assignment 1
        createTestCase(assignment1, "5\n3 1 4 1 5", "14", 50.0, true);
        createTestCase(assignment1, "3\n10 20 30", "60", 50.0, false);
        createTestCase(assignment1, "1\n42", "42", 25.0, false);
        createTestCase(assignment1, "4\n-1 -2 -3 -4", "-10", 25.0, false);
        
        // Tạo assignment 2: Fibonacci
        Assignment assignment2 = new Assignment();
        assignment2.setTitle("Fibonacci Number");
        assignment2.setDescription("Tính số Fibonacci thứ n");
        assignment2.setRequirements("Viết chương trình tính số Fibonacci thứ n.\n\n" +
                "Input:\n" +
                "- Một số nguyên n (0 ≤ n ≤ 30)\n\n" +
                "Output:\n" +
                "- Số Fibonacci thứ n\n\n" +
                "Lưu ý: F(0) = 0, F(1) = 1, F(n) = F(n-1) + F(n-2)\n\n" +
                "Ví dụ:\n" +
                "Input: 6\n" +
                "Output: 8");
        assignment2.setType(AssignmentType.EXERCISE);
        assignment2.setCourse(course);
        assignment2.setMaxScore(100.0);
        assignment2.setTimeLimit(45);
        assignment2.setStartTime(LocalDateTime.now().minusDays(1));
        assignment2.setEndTime(LocalDateTime.now().plusDays(14));
        assignment2.setIsActive(true);
        assignment2.setAllowLateSubmission(true);
        assignment2.setAutoGrade(true);
        assignmentRepository.save(assignment2);
        
        // Tạo test cases cho assignment 2
        createTestCase(assignment2, "6", "8", 40.0, true);
        createTestCase(assignment2, "0", "0", 20.0, false);
        createTestCase(assignment2, "1", "1", 20.0, false);
        createTestCase(assignment2, "10", "55", 20.0, false);
        
        System.out.println("✅ Test data created successfully:");
        System.out.println("Teacher: teacher/teacher123");
        System.out.println("Student: student/student123");
        System.out.println("Course: Data Structures and Algorithms");
        System.out.println("Assignments: Array Sum Problem, Fibonacci Number");
    }
    
    private void createTestCase(Assignment assignment, String input, String expectedOutput, Double weight, boolean isPublic) {
        // TODO: Refactor after Questions implementation
        // TestCase testCase = new TestCase();
        // testCase.setQuestion(question);
        // testCase.setInput(input);
        // testCase.setExpectedOutput(expectedOutput);
        // testCase.setWeight(weight);
        // testCase.setIsHidden(!isPublic); // isHidden is the opposite of isPublic
        // testCase.setTimeLimit(5000); // 5 seconds
        // testCaseRepository.save(testCase);
    }
    
    private void updateExistingUsersWithIds() {
        // Cập nhật student_id cho các sinh viên chưa có
        userRepository.findByRole(Role.STUDENT).forEach(student -> {
            if (student.getStudentId() == null || student.getStudentId().isEmpty()) {
                String studentId;
                do {
                    studentId = idGeneratorService.generateStudentId();
                } while (userRepository.existsByStudentId(studentId));
                student.setStudentId(studentId);
                userRepository.save(student);
                System.out.println("✅ Generated student ID " + studentId + " for " + student.getUsername());
            }
        });
        
        // Cập nhật teacher_id cho các giáo viên chưa có
        userRepository.findByRole(Role.TEACHER).forEach(teacher -> {
            if (teacher.getTeacherId() == null || teacher.getTeacherId().isEmpty()) {
                String teacherId;
                do {
                    teacherId = idGeneratorService.generateTeacherId();
                } while (userRepository.existsByTeacherId(teacherId));
                teacher.setTeacherId(teacherId);
                userRepository.save(teacher);
                System.out.println("✅ Generated teacher ID " + teacherId + " for " + teacher.getUsername());
            }
        });
    }
}
