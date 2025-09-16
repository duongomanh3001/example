package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CourseRequest;
import iuh.fit.cscore_be.dto.request.AdminCourseRequest;
import iuh.fit.cscore_be.dto.response.CourseResponse;
import iuh.fit.cscore_be.dto.response.StudentResponse;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Enrollment;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.repository.CourseRepository;
import iuh.fit.cscore_be.repository.EnrollmentRepository;
import iuh.fit.cscore_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    
    // Note: Course creation by teachers is now disabled
    // Only admins can create courses
    public CourseResponse createCourse(CourseRequest request, User teacher) {
        throw new RuntimeException("Giáo viên không có quyền tạo khóa học. Vui lòng liên hệ Admin.");
    }
    
    public CourseResponse updateCourse(Long courseId, CourseRequest request, User teacher) {
        throw new RuntimeException("Giáo viên không có quyền chỉnh sửa khóa học. Vui lòng liên hệ Admin.");
    }
    
    public void deleteCourse(Long courseId, User teacher) {
        throw new RuntimeException("Giáo viên không có quyền xóa khóa học. Vui lòng liên hệ Admin.");
    }
    
    public List<CourseResponse> getCoursesByTeacher(User teacher) {
        List<Course> courses = courseRepository.findByTeacherAndIsActiveTrue(teacher);
        return courses.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public Page<CourseResponse> getCoursesByTeacher(User teacher, Pageable pageable) {
        Page<Course> courses = courseRepository.findByTeacherAndIsActiveTrue(teacher, pageable);
        return courses.map(this::convertToResponse);
    }
    
    public CourseResponse getCourseById(Long courseId, User teacher) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem khóa học này");
        }
        
        return convertToResponse(course);
    }
    
    public List<StudentResponse> getStudentsInCourse(Long courseId, User teacher) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền xem danh sách sinh viên");
        }
        
        List<Enrollment> enrollments = enrollmentRepository.findByCourseAndIsActiveTrue(course);
        return enrollments.stream()
            .map(this::convertToStudentResponse)
            .collect(Collectors.toList());
    }
    
    // Student self-enrollment methods
    public List<CourseResponse> getAvailableCourses() {
        List<Course> courses = courseRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        return courses.stream()
            .filter(Course::isEnrollmentOpen)
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<CourseResponse> getEnrolledCourses(User student) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        return enrollments.stream()
            .map(enrollment -> convertToResponse(enrollment.getCourse()))
            .collect(Collectors.toList());
    }
    
    public void selfEnrollInCourse(Long courseId, User student) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        if (!course.getIsActive()) {
            throw new RuntimeException("Khóa học không còn hoạt động");
        }
        
        if (!course.isEnrollmentOpen()) {
            throw new RuntimeException("Khóa học đã đầy hoặc không mở đăng ký");
        }
        
        if (enrollmentRepository.existsByCourseAndStudent(course, student)) {
            throw new RuntimeException("Bạn đã đăng ký khóa học này rồi");
        }
        
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollment.setIsActive(true);
        enrollment.setEnrollmentDate(java.time.LocalDateTime.now());
        
        enrollmentRepository.save(enrollment);
    }
    
    public void unenrollFromCourse(Long courseId, User student) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        Enrollment enrollment = enrollmentRepository.findByCourseAndStudent(course, student)
            .orElseThrow(() -> new RuntimeException("Bạn chưa đăng ký khóa học này"));
        
        enrollment.setIsActive(false);
        enrollmentRepository.save(enrollment);
    }
    
    // Teachers can only add students by username (for backward compatibility)
    // But this functionality should be moved to Admin only
    public void enrollStudent(Long courseId, String studentUsername, User teacher) {
        throw new RuntimeException("Giáo viên không có quyền thêm sinh viên. Vui lòng liên hệ Admin.");
    }
    
    public void removeStudent(Long courseId, Long studentId, User teacher) {
        throw new RuntimeException("Giáo viên không có quyền xóa sinh viên. Vui lòng liên hệ Admin.");
    }
    
    private CourseResponse convertToResponse(Course course) {
        Long studentCount = enrollmentRepository.countByCourseAndIsActiveTrue(course);
        
        CourseResponse response = new CourseResponse(
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
        
        response.setMaxStudents(course.getMaxStudents());
        response.setEnrollmentOpen(course.isEnrollmentOpen());
        
        return response;
    }
    
    private StudentResponse convertToStudentResponse(Enrollment enrollment) {
        User student = enrollment.getStudent();
        return new StudentResponse(
            student.getId(),
            student.getUsername(),
            student.getEmail(),
            student.getFullName(),
            student.getStudentId(),
            enrollment.getIsActive(),
            enrollment.getEnrollmentDate(),
            enrollment.getFinalGrade(),
            0, // TODO: Calculate total submissions
            0.0 // TODO: Calculate average score
        );
    }
    
    // ============ ADMIN METHODS ============
    
    public List<CourseResponse> getAllCourses() {
        List<Course> courses = courseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return courses.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public Page<CourseResponse> getAllCourses(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Course> courses = courseRepository.findAll(pageable);
        return courses.map(this::convertToResponse);
    }
    
    public CourseResponse getCourseByIdForAdmin(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        return convertToResponse(course);
    }
    
    public CourseResponse createCourseByAdmin(AdminCourseRequest request) {
        // Check if teacher exists and has TEACHER role
        User teacher = userRepository.findById(request.getTeacherId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
        
        if (!teacher.getRole().equals(Role.TEACHER)) {
            throw new RuntimeException("Người dùng không phải là giáo viên");
        }
        
        // Check if course code already exists (globally, not per teacher)
        if (courseRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Mã khóa học đã tồn tại");
        }
        
        Course course = new Course();
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        course.setCreditHours(request.getCreditHours());
        course.setSemester(request.getSemester());
        course.setAcademicYear(request.getAcademicYear());
        course.setMaxStudents(request.getMaxStudents());
        course.setTeacher(teacher);
        course.setIsActive(true);
        course.setCreatedAt(java.time.LocalDateTime.now());
        
        Course savedCourse = courseRepository.save(course);
        return convertToResponse(savedCourse);
    }
    
    public CourseResponse updateCourseByAdmin(Long courseId, AdminCourseRequest request) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        // Check if teacher exists and has TEACHER role
        User teacher = userRepository.findById(request.getTeacherId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
        
        if (!teacher.getRole().equals(Role.TEACHER)) {
            throw new RuntimeException("Người dùng không phải là giáo viên");
        }
        
        // Check if course code already exists (exclude current course)
        if (!course.getCode().equals(request.getCode()) && 
            courseRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Mã khóa học đã tồn tại");
        }
        
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        course.setCreditHours(request.getCreditHours());
        course.setSemester(request.getSemester());
        course.setAcademicYear(request.getAcademicYear());
        course.setMaxStudents(request.getMaxStudents());
        course.setTeacher(teacher);
        
        Course savedCourse = courseRepository.save(course);
        return convertToResponse(savedCourse);
    }
    
    public void deleteCourseByAdmin(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        course.setIsActive(false);
        courseRepository.save(course);
    }
    
    public void assignTeacherToCourse(Long courseId, Long teacherId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
        
        if (!teacher.getRole().equals(Role.TEACHER)) {
            throw new RuntimeException("Người dùng không phải là giáo viên");
        }
        
        course.setTeacher(teacher);
        courseRepository.save(course);
    }
    
    public void enrollStudentToCourseByAdmin(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));
        
        if (!student.getRole().equals(Role.STUDENT)) {
            throw new RuntimeException("Người dùng không phải là sinh viên");
        }
        
        if (enrollmentRepository.existsByCourseAndStudent(course, student)) {
            throw new RuntimeException("Sinh viên đã được đăng ký vào khóa học này");
        }
        
        // Check if course is full
        Long currentStudentCount = enrollmentRepository.countByCourseAndIsActiveTrue(course);
        if (course.getMaxStudents() != null && currentStudentCount >= course.getMaxStudents()) {
            throw new RuntimeException("Khóa học đã đầy");
        }
        
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setIsActive(true);
        enrollment.setEnrollmentDate(java.time.LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }
    
    public void removeStudentFromCourseByAdmin(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));
        
        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
            .orElseThrow(() -> new RuntimeException("Sinh viên chưa đăng ký khóa học này"));
        
        enrollment.setIsActive(false);
        enrollmentRepository.save(enrollment);
    }
    
    public List<StudentResponse> getStudentsInCourseByAdmin(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        List<Enrollment> enrollments = enrollmentRepository.findByCourseAndIsActiveTrue(course);
        return enrollments.stream()
            .map(this::convertToStudentResponse)
            .collect(Collectors.toList());
    }
}
