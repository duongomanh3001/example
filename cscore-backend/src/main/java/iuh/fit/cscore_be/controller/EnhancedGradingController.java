package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.response.EnhancedAutoGradingResponse;
import iuh.fit.cscore_be.entity.User;
import iuh.fit.cscore_be.security.UserPrincipal;
import iuh.fit.cscore_be.service.AutoGradingService;
import iuh.fit.cscore_be.service.SubmissionService;
import iuh.fit.cscore_be.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Controller for enhanced automatic grading functionality
 * Provides endpoints for teachers to trigger and monitor enhanced grading
 */
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RestController
@RequestMapping("/api/admin/enhanced-grading")
@RequiredArgsConstructor
@Slf4j
public class EnhancedGradingController {

    private final AutoGradingService autoGradingService;
    private final SubmissionService submissionService;
    private final UserService userService;

    /**
     * Manually trigger enhanced grading for a specific submission
     * This allows teachers to re-grade submissions using the enhanced algorithm
     */
    @PostMapping("/grade-submission/{submissionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<String> gradeSubmissionEnhanced(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            User user = userService.findById(userPrincipal.getId());
            log.info("Teacher {} manually triggered enhanced grading for submission {}", 
                    user.getUsername(), submissionId);
            
            // Trigger enhanced grading asynchronously
            CompletableFuture<Double> futureScore = autoGradingService.gradeSubmissionEnhanced(submissionId);
            
            return ResponseEntity.ok("Enhanced grading started for submission " + submissionId + 
                    ". The process will run in background and results will be available shortly.");
            
        } catch (Exception e) {
            log.error("Error triggering enhanced grading for submission {}", submissionId, e);
            return ResponseEntity.badRequest().body("Error starting enhanced grading: " + e.getMessage());
        }
    }

    /**
     * Get enhanced grading capabilities info
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<EnhancedGradingInfo> getEnhancedGradingInfo() {
        EnhancedGradingInfo info = new EnhancedGradingInfo();
        info.setDescription("Enhanced Auto-Grading System");
        info.setFeatures(new String[]{
            "Reference Implementation Comparison",
            "Complete Test Code Generation",
            "Parallel Execution of Student and Reference Code",
            "Detailed Output Comparison",
            "Function-based Testing Support",
            "Multi-language Support (Java, Python, C++)",
            "Fallback to Traditional Grading"
        });
        info.setSupportedLanguages(new String[]{"java", "python", "cpp", "c"});
        info.setVersion("1.0.0");
        
        return ResponseEntity.ok(info);
    }

    /**
     * Get detailed grading results for a submission
     * This provides comprehensive feedback including reference comparison details
     */
    @GetMapping("/results/{submissionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<EnhancedAutoGradingResponse> getGradingResults(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            // This would need to be implemented to convert submission data to EnhancedAutoGradingResponse
            // For now, return a placeholder response
            return ResponseEntity.ok(createPlaceholderResponse(submissionId));
            
        } catch (Exception e) {
            log.error("Error retrieving enhanced grading results for submission {}", submissionId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Batch re-grade multiple submissions using enhanced algorithm
     */
    @PostMapping("/batch-grade")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<String> batchGradeSubmissions(
            @RequestBody BatchGradingRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            User user = userService.findById(userPrincipal.getId());
            log.info("Teacher {} started batch enhanced grading for {} submissions", 
                    user.getUsername(), request.getSubmissionIds().size());
            
            for (Long submissionId : request.getSubmissionIds()) {
                autoGradingService.gradeSubmissionEnhanced(submissionId);
            }
            
            return ResponseEntity.ok("Batch enhanced grading started for " + 
                    request.getSubmissionIds().size() + " submissions.");
            
        } catch (Exception e) {
            log.error("Error in batch enhanced grading", e);
            return ResponseEntity.badRequest().body("Error starting batch grading: " + e.getMessage());
        }
    }

    // Helper methods and DTOs
    private EnhancedAutoGradingResponse createPlaceholderResponse(Long submissionId) {
        EnhancedAutoGradingResponse response = new EnhancedAutoGradingResponse();
        response.setSubmissionId(submissionId);
        response.setGradingMethod("enhanced");
        response.setOverallFeedback("Enhanced grading results would be displayed here");
        return response;
    }

    // Inner classes for request/response DTOs
    public static class BatchGradingRequest {
        private java.util.List<Long> submissionIds;
        private boolean forceRegrade = false;

        // Getters and setters
        public java.util.List<Long> getSubmissionIds() { return submissionIds; }
        public void setSubmissionIds(java.util.List<Long> submissionIds) { this.submissionIds = submissionIds; }
        
        public boolean isForceRegrade() { return forceRegrade; }
        public void setForceRegrade(boolean forceRegrade) { this.forceRegrade = forceRegrade; }
    }

    public static class EnhancedGradingInfo {
        private String description;
        private String[] features;
        private String[] supportedLanguages;
        private String version;

        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String[] getFeatures() { return features; }
        public void setFeatures(String[] features) { this.features = features; }
        
        public String[] getSupportedLanguages() { return supportedLanguages; }
        public void setSupportedLanguages(String[] supportedLanguages) { this.supportedLanguages = supportedLanguages; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}