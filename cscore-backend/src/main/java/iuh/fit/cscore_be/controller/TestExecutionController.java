package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.service.HybridCodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/test-execution")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class TestExecutionController {

    private final HybridCodeExecutionService hybridCodeExecutionService;

    /**
     * Get current execution status and configuration
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<HybridCodeExecutionService.ExecutionInfo> getExecutionStatus() {
        try {
            HybridCodeExecutionService.ExecutionInfo info = hybridCodeExecutionService.getExecutionInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Error getting execution status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Test code execution with current configuration
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> testExecution(@RequestBody TestExecutionRequest request) {
        try {
            log.info("Testing code execution with language: {}", request.getLanguage());
            
            long startTime = System.currentTimeMillis();
            var result = hybridCodeExecutionService.executeCode(request.getCode(), request.getLanguage());
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("output", result.getOutput());
            response.put("error", result.getError());
            response.put("compilationError", result.getCompilationError());
            response.put("executionTime", duration);
            response.put("language", result.getLanguage());
            response.put("executionInfo", hybridCodeExecutionService.getExecutionInfo());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing code execution", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Test execution failed: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Test code execution with input
     */
    @PostMapping("/test-with-input")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> testExecutionWithInput(@RequestBody TestExecutionWithInputRequest request) {
        try {
            log.info("Testing code execution with input for language: {}", request.getLanguage());
            
            long startTime = System.currentTimeMillis();
            var result = hybridCodeExecutionService.executeCodeWithInput(
                    request.getCode(), 
                    request.getLanguage(), 
                    request.getInput()
            );
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("output", result.getOutput());
            response.put("error", result.getError());
            response.put("compilationError", result.getCompilationError());
            response.put("executionTime", duration);
            response.put("language", result.getLanguage());
            response.put("input", request.getInput());
            response.put("executionInfo", hybridCodeExecutionService.getExecutionInfo());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing code execution with input", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Test execution with input failed: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    // Request DTOs
    public static class TestExecutionRequest {
        private String code;
        private String language;

        // Constructors
        public TestExecutionRequest() {}
        
        public TestExecutionRequest(String code, String language) {
            this.code = code;
            this.language = language;
        }

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    public static class TestExecutionWithInputRequest extends TestExecutionRequest {
        private String input;

        // Constructors
        public TestExecutionWithInputRequest() {}
        
        public TestExecutionWithInputRequest(String code, String language, String input) {
            super(code, language);
            this.input = input;
        }

        // Getters and setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
    }
}
