package iuh.fit.cscore_be.repository;

import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    Optional<User> findByStudentId(String studentId);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByStudentId(String studentId);
    
    Boolean existsByTeacherId(String teacherId);
    
    // Phương thức kiểm tra tồn tại với loại trừ ID hiện tại
    Boolean existsByUsernameAndIdNot(String username, Long id);
    
    Boolean existsByEmailAndIdNot(String email, Long id);
    
    Boolean existsByStudentIdAndIdNot(String studentId, Long id);
    
    Boolean existsByTeacherIdAndIdNot(String teacherId, Long id);
    
    // Tìm theo role
    List<User> findByRole(Role role);
    
    Page<User> findByRole(Role role, Pageable pageable);
    
    List<User> findByRoleOrderByCreatedAtDesc(Role role);
    
    // Kiểm tra tồn tại theo role
    Boolean existsByRole(Role role);
    
    // Tìm theo trạng thái active
    List<User> findByIsActive(Boolean isActive);
    
    // Đếm users
    long countByRole(Role role);
    
    long countByIsActive(Boolean isActive);
    
    long countByRoleAndIsActive(Role role, Boolean isActive);
    
    // Additional methods for UserStatsResponse
    long countByIsActiveTrue();
    
    List<User> findByIsActiveTrueOrderByCreatedAtDesc();
    
    // Tìm kiếm users
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username, String email, String fullName);
    
    List<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String fullName, String email);
}
