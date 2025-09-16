package iuh.fit.cscore_be.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class IdGeneratorService {
    
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Sinh mã sinh viên 8 chữ số duy nhất
     * Format: 10000000 - 99999999
     */
    public String generateStudentId() {
        // Sinh số từ 10000000 đến 99999999 (8 chữ số)
        int randomNumber = 10000000 + random.nextInt(90000000);
        return String.valueOf(randomNumber);
    }
    
    /**
     * Sinh mã giáo viên 8 chữ số duy nhất
     * Format: 20000000 - 29999999 (bắt đầu bằng 2 để phân biệt với sinh viên)
     */
    public String generateTeacherId() {
        // Sinh số từ 20000000 đến 29999999 (8 chữ số, bắt đầu bằng 2)
        int randomNumber = 20000000 + random.nextInt(10000000);
        return String.valueOf(randomNumber);
    }
}
