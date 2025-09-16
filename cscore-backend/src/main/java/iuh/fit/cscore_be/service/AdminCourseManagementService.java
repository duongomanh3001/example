package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateCourseRequest;
import iuh.fit.cscore_be.dto.request.UpdateCourseRequest;
import iuh.fit.cscore_be.dto.response.DetailedCourseResponse;
import iuh.fit.cscore_be.dto.response.CourseResponse;
import iuh.fit.cscore_be.dto.response.StudentResponse;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Enrollment;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.exception.ResourceNotFoundException;
import iuh.fit.cscore_be.exception.BadRequestException;
import iuh.fit.cscore_be.repository.CourseRepository;
import iuh.fit.cscore_be.repository.EnrollmentRepository;
import iuh.fit.cscore_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseManagementService {
    
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    
    public DetailedCourseResponse createCourse(CreateCourseRequest request) {
        log.info("Creating new course with code: {}", request.getCode());
        
        validateCourseCreationRequest(request);
        
        User teacher = findTeacherById(request.getTeacherId());
        
        Course course = buildCourseFromRequest(request, teacher);
        Course savedCourse = courseRepository.save(course);
        
        log.info("Successfully created course with ID: {}", savedCourse.getId());
        return mapToDetailedCourseResponse(savedCourse);
    }
    
    public DetailedCourseResponse updateCourse(Long courseId, UpdateCourseRequest request) {
        log.info("Updating course with ID: {}", courseId);
        
        Course course = findCourseById(courseId);
        updateCourseFromRequest(course, request);
        
        Course updatedCourse = courseRepository.save(course);
        
        log.info("Successfully updated course with ID: {}", courseId);
        return mapToDetailedCourseResponse(updatedCourse);
    }
    
    public void deleteCourse(Long courseId) {
        log.info("Deleting course with ID: {}", courseId);
        
        Course course = findCourseById(courseId);
        
        validateCourseDeletion(course);
        
        course.setIsActive(false);
        courseRepository.save(course);
        
        log.info("Successfully deactivated course with ID: {}", courseId);
    }
    
    public DetailedCourseResponse getCourseById(Long courseId) {
        Course course = findCourseById(courseId);
        return mapToDetailedCourseResponse(course);
    }
    
    public Page<CourseResponse> getAllCourses(Pageable pageable) {
        Page<Course> courses = courseRepository.findAll(pageable);
        return courses.map(this::mapToCourseResponse);
    }
    
    public List<CourseResponse> getAllActiveCourses() {
        List<Course> courses = courseRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        return courses.stream()
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());
    }
    
    public void assignTeacherToCourse(Long courseId, Long teacherId) {
        log.info("Assigning teacher {} to course {}", teacherId, courseId);
        
        Course course = findCourseById(courseId);
        User teacher = findTeacherById(teacherId);
        
        course.setTeacher(teacher);
        courseRepository.save(course);
        
        log.info("Successfully assigned teacher {} to course {}", teacherId, courseId);
    }
    
    public void enrollStudentInCourse(Long courseId, Long studentId) {
        log.info("Enrolling student {} in course {}", studentId, courseId);
        
        Course course = findCourseById(courseId);
        User student = findStudentById(studentId);
        
        validateStudentEnrollment(course, student);
        
        Enrollment enrollment = new Enrollment(student, course);
        enrollmentRepository.save(enrollment);
        
        log.info("Successfully enrolled student {} in course {}", studentId, courseId);
    }
    
    public void removeStudentFromCourse(Long courseId, Long studentId) {
        log.info("Removing student {} from course {}", studentId, courseId);
        
        Enrollment enrollment = enrollmentRepository
                .findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sinh viên chưa đăng ký khóa học này"));
        
        enrollmentRepository.delete(enrollment);
        
        log.info("Successfully removed student {} from course {}", studentId, courseId);
    }
    
    public List<StudentResponse> getStudentsInCourse(Long courseId) {
        Course course = findCourseById(courseId);
        
        List<Enrollment> enrollments = enrollmentRepository.findByCourseOrderByEnrollmentDateDesc(course);
        
        return enrollments.stream()
                .map(enrollment -> mapToStudentResponse(enrollment.getStudent(), enrollment))
                .collect(Collectors.toList());
    }
    
    // Private helper methods following Clean Code principles
    
    private void validateCourseCreationRequest(CreateCourseRequest request) {
        if (courseRepository.existsByCodeAndIsActiveTrue(request.getCode())) {
            throw new BadRequestException("Mã khóa học đã tồn tại: " + request.getCode());
        }
    }
    
    private void validateCourseDeletion(Course course) {
        long enrollmentCount = enrollmentRepository.countByCourse(course);
        if (enrollmentCount > 0) {
            throw new BadRequestException(
                    "Không thể xóa khóa học có sinh viên đang học. Hiện có " + enrollmentCount + " sinh viên.");
        }
    }
    
    private void validateStudentEnrollment(Course course, User student) {
        if (!course.getIsActive()) {
            throw new BadRequestException("Khóa học không còn hoạt động");
        }
        
        if (enrollmentRepository.existsByCourseAndStudent(course, student)) {
            throw new BadRequestException("Sinh viên đã đăng ký khóa học này");
        }
        
        long currentEnrollmentCount = enrollmentRepository.countByCourse(course);
        if (currentEnrollmentCount >= course.getMaxStudents()) {
            throw new BadRequestException("Khóa học đã đầy");
        }
    }
    
    private Course findCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId));
    }
    
    private User findTeacherById(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với ID: " + teacherId));
        
        if (!teacher.getRole().equals(Role.TEACHER) && !teacher.getRole().equals(Role.ADMIN)) {
            throw new BadRequestException("Người dùng không phải là giáo viên");
        }
        
        return teacher;
    }
    
    private User findStudentById(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sinh viên với ID: " + studentId));
        
        if (!student.getRole().equals(Role.STUDENT)) {
            throw new BadRequestException("Người dùng không phải là sinh viên");
        }
        
        return student;
    }
    
    private Course buildCourseFromRequest(CreateCourseRequest request, User teacher) {
        Course course = new Course();
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        course.setCreditHours(request.getCreditHours());
        course.setSemester(request.getSemester());
        course.setYear(request.getYear());
        course.setMaxStudents(request.getMaxStudents());
        course.setTeacher(teacher);
        course.setIsActive(true);
        return course;
    }
    
    private void updateCourseFromRequest(Course course, UpdateCourseRequest request) {
        if (request.getName() != null) {
            course.setName(request.getName());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCreditHours() != null) {
            course.setCreditHours(request.getCreditHours());
        }
        if (request.getSemester() != null) {
            course.setSemester(request.getSemester());
        }
        if (request.getYear() != null) {
            course.setYear(request.getYear());
        }
        if (request.getMaxStudents() != null) {
            course.setMaxStudents(request.getMaxStudents());
        }
        if (request.getTeacherId() != null) {
            User teacher = findTeacherById(request.getTeacherId());
            course.setTeacher(teacher);
        }
        if (request.getIsActive() != null) {
            course.setIsActive(request.getIsActive());
        }
    }
    
    private DetailedCourseResponse mapToDetailedCourseResponse(Course course) {
        DetailedCourseResponse response = new DetailedCourseResponse();
        response.setId(course.getId());
        response.setName(course.getName());
        response.setCode(course.getCode());
        response.setDescription(course.getDescription());
        response.setCreditHours(course.getCreditHours());
        response.setSemester(course.getSemester());
        response.setYear(course.getYear());
        response.setMaxStudents(course.getMaxStudents());
        response.setIsActive(course.getIsActive());
        response.setCreatedAt(course.getCreatedAt());
        response.setUpdatedAt(course.getUpdatedAt());
        
        // Set teacher information
        if (course.getTeacher() != null) {
            DetailedCourseResponse.TeacherInfo teacherInfo = new DetailedCourseResponse.TeacherInfo();
            teacherInfo.setId(course.getTeacher().getId());
            teacherInfo.setUsername(course.getTeacher().getUsername());
            teacherInfo.setFullName(course.getTeacher().getFullName());
            teacherInfo.setEmail(course.getTeacher().getEmail());
            response.setTeacher(teacherInfo);
        }
        
        // Set enrollment count
        long enrollmentCount = enrollmentRepository.countByCourse(course);
        response.setCurrentStudentCount((int) enrollmentCount);
        
        // Set assignment count
        int assignmentCount = course.getAssignments().size();
        response.setAssignmentCount(assignmentCount);
        
        return response;
    }
    
    private CourseResponse mapToCourseResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setName(course.getName());
        response.setCode(course.getCode());
        response.setDescription(course.getDescription());
        response.setSemester(course.getSemester());
        response.setYear(course.getYear());
        response.setMaxStudents(course.getMaxStudents());
        response.setIsActive(course.getIsActive());
        response.setCreatedAt(course.getCreatedAt());
        response.setUpdatedAt(course.getUpdatedAt());
        
        // Set current student count
        long enrollmentCount = enrollmentRepository.countByCourse(course);
        response.setCurrentStudentCount((int) enrollmentCount);
        
        return response;
    }
    
    private StudentResponse mapToStudentResponse(User student, Enrollment enrollment) {
        StudentResponse response = new StudentResponse();
        response.setId(student.getId());
        response.setUsername(student.getUsername());
        response.setEmail(student.getEmail());
        response.setFullName(student.getFullName());
        response.setStudentId(student.getStudentId());
        response.setIsActive(student.getIsActive());
        response.setEnrolledAt(enrollment.getEnrollmentDate().toString());
        return response;
    }
}
