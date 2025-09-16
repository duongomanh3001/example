package iuh.fit.cscore_be.dto.response;

import iuh.fit.cscore_be.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String studentId;
    private Role role;
    
    public JwtResponse(String accessToken, Long id, String username, String email, 
                      String fullName, String studentId, Role role) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.studentId = studentId;
        this.role = role;
    }
}
