package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateUserRequest;
import iuh.fit.cscore_be.dto.request.LoginRequest;
import iuh.fit.cscore_be.dto.response.JwtResponse;
import iuh.fit.cscore_be.dto.response.MessageResponse;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.enums.Role;
import iuh.fit.cscore_be.repository.UserRepository;
import iuh.fit.cscore_be.security.JwtUtils;
import iuh.fit.cscore_be.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    @Autowired
    AuthenticationManager authenticationManager;
    
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    PasswordEncoder encoder;
    
    @Autowired
    JwtUtils jwtUtils;
    
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername()).get();
        
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                user.getFullName(),
                user.getStudentId(),
                user.getRole()));
    }
    
    public ResponseEntity<?> createUserByAdmin(CreateUserRequest createUserRequest) {
        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Lỗi: Username đã tồn tại!"));
        }
        
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Lỗi: Email đã tồn tại!"));
        }
        
        // Validate studentId for students
        if (createUserRequest.getRole() == Role.STUDENT) {
            if (createUserRequest.getStudentId() == null || createUserRequest.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Lỗi: Mã số sinh viên không được để trống!"));
            }
            
            if (userRepository.existsByStudentId(createUserRequest.getStudentId())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Lỗi: Mã số sinh viên đã tồn tại!"));
            }
        }
        
        // Validate role (Only TEACHER and STUDENT can be created by admin)
        if (createUserRequest.getRole() != Role.TEACHER && createUserRequest.getRole() != Role.STUDENT) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Lỗi: Chỉ có thể tạo tài khoản TEACHER hoặc STUDENT!"));
        }
        
        // Create new user
        User user = new User();
        user.setUsername(createUserRequest.getUsername());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(encoder.encode(createUserRequest.getPassword()));
        user.setFullName(createUserRequest.getFullName());
        user.setRole(createUserRequest.getRole());
        
        if (createUserRequest.getRole() == Role.STUDENT) {
            user.setStudentId(createUserRequest.getStudentId());
        }
        
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("Tài khoản " + createUserRequest.getRole().name() + " đã được tạo thành công!"));
    }
}
