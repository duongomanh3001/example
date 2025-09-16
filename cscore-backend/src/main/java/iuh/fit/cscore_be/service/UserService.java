package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateUserRequest;
import iuh.fit.cscore_be.dto.request.UpdateUserRequest;
import iuh.fit.cscore_be.dto.response.MessageResponse;
import iuh.fit.cscore_be.dto.response.UserResponse;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    PasswordEncoder encoder;
    
    @Autowired
    IdGeneratorService idGeneratorService;
    
    // Find user by username
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));
    }
    
    // Lấy User entity theo ID (for internal use)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));
    }
    
    // Lấy tất cả users
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    // Lấy users theo role với phân trang
    public Page<UserResponse> getUsersByRole(Role role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = userRepository.findByRole(role, pageable);
        return users.map(this::convertToUserResponse);
    }
    
    // Lấy tất cả students
    public List<UserResponse> getAllStudents() {
        List<User> students = userRepository.findByRoleOrderByCreatedAtDesc(Role.STUDENT);
        return students.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    // Lấy tất cả teachers
    public List<UserResponse> getAllTeachers() {
        List<User> teachers = userRepository.findByRoleOrderByCreatedAtDesc(Role.TEACHER);
        return teachers.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    // Lấy user theo ID
    public Optional<UserResponse> getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(this::convertToUserResponse);
    }
    
    // Cập nhật user
    public ResponseEntity<?> updateUser(Long id, UpdateUserRequest updateRequest) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Không tìm thấy user với ID: " + id));
        }
        
        User user = userOptional.get();
        
        // Kiểm tra username đã tồn tại (trừ user hiện tại)
        if (userRepository.existsByUsernameAndIdNot(updateRequest.getUsername(), id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Username đã tồn tại!"));
        }
        
        // Kiểm tra email đã tồn tại (trừ user hiện tại)
        if (userRepository.existsByEmailAndIdNot(updateRequest.getEmail(), id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email đã tồn tại!"));
        }
        
        // Kiểm tra studentId đã tồn tại (nếu có và trừ user hiện tại)
        if (updateRequest.getStudentId() != null && 
            !updateRequest.getStudentId().trim().isEmpty() &&
            userRepository.existsByStudentIdAndIdNot(updateRequest.getStudentId(), id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Mã sinh viên đã tồn tại!"));
        }
        
        // Cập nhật thông tin
        user.setUsername(updateRequest.getUsername());
        user.setEmail(updateRequest.getEmail());
        user.setFullName(updateRequest.getFullName());
        user.setPhone(updateRequest.getPhone());
        user.setRole(updateRequest.getRole());
        user.setStudentId(updateRequest.getStudentId());
        user.setIsActive(updateRequest.getIsActive());
        
        // Cập nhật password nếu có
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().trim().isEmpty()) {
            user.setPassword(encoder.encode(updateRequest.getPassword()));
        }
        
        User updatedUser = userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("Cập nhật thông tin user thành công!"));
    }
    
    // Xóa user
    public ResponseEntity<?> deleteUser(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Không tìm thấy user với ID: " + id));
        }
        
        User user = userOptional.get();
        user.setIsActive(false); // Soft delete
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("Xóa user thành công!"));
    }
    
    // Tạo user mới
    public ResponseEntity<?> createUser(CreateUserRequest createRequest) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(createRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Username đã tồn tại!"));
        }
        
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(createRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email đã tồn tại!"));
        }
        
        // Tạo user mới
        User user = new User();
        user.setUsername(createRequest.getUsername());
        user.setEmail(createRequest.getEmail());
        user.setPassword(encoder.encode(createRequest.getPassword()));
        user.setFullName(createRequest.getFullName());
        user.setPhone(createRequest.getPhone());
        user.setRole(createRequest.getRole());
        user.setIsActive(true);
        
        // Tự động sinh mã dựa trên role
        if (createRequest.getRole() == Role.STUDENT) {
            String studentId;
            do {
                studentId = idGeneratorService.generateStudentId();
            } while (userRepository.existsByStudentId(studentId));
            user.setStudentId(studentId);
        } else if (createRequest.getRole() == Role.TEACHER) {
            String teacherId;
            do {
                teacherId = idGeneratorService.generateTeacherId();
            } while (userRepository.existsByTeacherId(teacherId));
            user.setTeacherId(teacherId);
        }
        
        User savedUser = userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("Tạo user mới thành công!"));
    }
    
    // Chuyển đổi User entity sang UserResponse DTO
    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStudentId(),
                user.getTeacherId(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
    
    // Tìm kiếm users
    public List<UserResponse> searchUsers(String keyword) {
        List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                keyword, keyword, keyword);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    // Thống kê số lượng users theo role
    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }
    
    // Toggle user status (active/inactive)
    public ResponseEntity<MessageResponse> toggleUserStatus(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + userId));
            
            user.setIsActive(!user.getIsActive());
            userRepository.save(user);
            
            String status = user.getIsActive() ? "kích hoạt" : "vô hiệu hóa";
            return ResponseEntity.ok(new MessageResponse("Đã " + status + " user thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Lỗi khi thay đổi trạng thái user: " + e.getMessage()));
        }
    }
    
    // Lấy users active
    public List<UserResponse> getActiveUsers() {
        List<User> users = userRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    // Lấy thống kê tổng quan của users
    public UserStatsResponse getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long teacherCount = userRepository.countByRole(Role.TEACHER);
        long studentCount = userRepository.countByRole(Role.STUDENT);
        
        return new UserStatsResponse(totalUsers, activeUsers, adminCount, teacherCount, studentCount);
    }
    
    // Inner class for UserStatsResponse
    public static class UserStatsResponse {
        private long totalUsers;
        private long activeUsers;
        private long adminCount;
        private long teacherCount;
        private long studentCount;
        
        public UserStatsResponse() {}
        
        public UserStatsResponse(long totalUsers, long activeUsers, long adminCount, long teacherCount, long studentCount) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.adminCount = adminCount;
            this.teacherCount = teacherCount;
            this.studentCount = studentCount;
        }
        
        // Getters and Setters
        public long getTotalUsers() {
            return totalUsers;
        }
        
        public void setTotalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
        }
        
        public long getActiveUsers() {
            return activeUsers;
        }
        
        public void setActiveUsers(long activeUsers) {
            this.activeUsers = activeUsers;
        }
        
        public long getAdminCount() {
            return adminCount;
        }
        
        public void setAdminCount(long adminCount) {
            this.adminCount = adminCount;
        }
        
        public long getTeacherCount() {
            return teacherCount;
        }
        
        public void setTeacherCount(long teacherCount) {
            this.teacherCount = teacherCount;
        }
        
        public long getStudentCount() {
            return studentCount;
        }
        
        public void setStudentCount(long studentCount) {
            this.studentCount = studentCount;
        }
    }
}
