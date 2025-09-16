package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.dto.request.QuestionCodeCheckRequest;
import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.service.QuestionCodeCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class StudentCodeController {

    private final QuestionCodeCheckService questionCodeCheckService;

    /**
     * Check student code against test cases for a specific question
     * This allows real-time feedback without submitting the entire assignment
     */
    @PostMapping("/check-question-code")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CodeExecutionResponse> checkQuestionCode(
            @RequestBody QuestionCodeCheckRequest request,
            Authentication authentication) {
        
        try {
            String studentId = authentication.getName();
            log.info("Student {} checking code for question {} with input: {}", 
                studentId, request.getQuestionId(), request.getInput() != null ? "yes" : "no");
            
            CodeExecutionResponse result = questionCodeCheckService.checkQuestionCode(
                request.getQuestionId(), 
                request.getCode(), 
                request.getLanguage(),
                studentId,
                request.getInput()  // Pass custom input if provided
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error checking question code", e);
            
            CodeExecutionResponse errorResponse = new CodeExecutionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Lỗi khi kiểm tra code: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get current score for a question (based on last successful check)
     */
    @GetMapping("/question/{questionId}/score")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Double> getQuestionScore(
            @PathVariable Long questionId,
            Authentication authentication) {
        
        try {
            String studentId = authentication.getName();
            Double score = questionCodeCheckService.getQuestionScore(questionId, studentId);
            return ResponseEntity.ok(score);
            
        } catch (Exception e) {
            log.error("Error getting question score", e);
            return ResponseEntity.ok(0.0);
        }
    }
    
    /**
     * Submit final answer for a question (locks the score)
     */
    @PostMapping("/submit-question-answer")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CodeExecutionResponse> submitQuestionAnswer(
            @RequestBody QuestionCodeCheckRequest request,
            Authentication authentication) {
        
        try {
            String studentId = authentication.getName();
            log.info("Student {} submitting final answer for question {}", studentId, request.getQuestionId());
            
            CodeExecutionResponse result = questionCodeCheckService.submitQuestionAnswer(
                request.getQuestionId(), 
                request.getCode(), 
                request.getLanguage(),
                studentId
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error submitting question answer", e);
            
            CodeExecutionResponse errorResponse = new CodeExecutionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Lỗi khi submit câu trả lời: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
}