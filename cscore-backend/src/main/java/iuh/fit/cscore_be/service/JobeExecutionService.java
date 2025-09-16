package iuh.fit.cscore_be.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.dto.response.TestResultResponse;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.entity.TestResult;
import iuh.fit.cscore_be.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobeExecutionService {

    private final TestResultRepository testResultRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${jobe.server.url:http://localhost:4000}")
    private String jobeServerUrl;
    
    @Value("${jobe.server.enabled:false}")
    private boolean jobeEnabled;
    
    @Value("${jobe.server.api-key:2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF}")
    private String jobeApiKey;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    );

    /**
     * Execute code using Jobe server
     */
    public CodeExecutionResponse executeCode(String code, String language) {
        if (!jobeEnabled) {
            log.warn("Jobe server is disabled, falling back to local execution");
            return createErrorResponse("Jobe server is not enabled");
        }

        try {
            log.info("Executing code via Jobe server - Language: {}", language);
            
            // Create request for Jobe server
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("run_spec", createJobeRequest(code, language, null));
            
            // Send request to Jobe server
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", jobeApiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = jobeServerUrl + "/jobe/index.php/restapi/runs";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            return parseJobeResponse(response.getBody(), language);
            
        } catch (Exception e) {
            log.error("Error executing code via Jobe server", e);
            return createErrorResponse("Lỗi khi thực thi code qua Jobe server: " + e.getMessage());
        }
    }

    /**
     * Execute code with input using Jobe server
     */
    public CodeExecutionResponse executeCodeWithInput(String code, String language, String input) {
        if (!jobeEnabled) {
            log.warn("Jobe server is disabled, falling back to local execution");
            return createErrorResponse("Jobe server is not enabled");
        }

        try {
            log.info("Executing code with input via Jobe server - Language: {}", language);
            
            // Create request for Jobe server with input
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("run_spec", createJobeRequest(code, language, input));
            
            // Send request to Jobe server
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", jobeApiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = jobeServerUrl + "/jobe/index.php/restapi/runs";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            return parseJobeResponse(response.getBody(), language);
            
        } catch (Exception e) {
            log.error("Error executing code with input via Jobe server", e);
            return createErrorResponse("Lỗi khi thực thi code với input qua Jobe server: " + e.getMessage());
        }
    }

    /**
     * Execute code with test cases using Jobe server
     */
    public CodeExecutionResponse executeCodeWithTestCases(String code, String language, List<TestCase> testCases, Submission submission) {
        if (!jobeEnabled) {
            log.warn("Jobe server is disabled, falling back to local execution");
            return createErrorResponse("Jobe server is not enabled");
        }

        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setLanguage(language);
        
        List<TestResultResponse> testResults = new ArrayList<>();
        int passedTests = 0;
        double totalScore = 0.0;
        long totalExecutionTime = 0L;
        
        try {
            log.info("Executing code with {} test cases via Jobe server - Language: {}", testCases.size(), language);
            
            // Execute each test case
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                log.debug("Executing test case {} of {}", i + 1, testCases.size());
                
                try {
                    TestResultResponse testResult = executeTestCaseViaJobe(code, language, testCase);
                    testResults.add(testResult);
                    
                    // Update statistics
                    if (testResult.isPassed()) {
                        passedTests++;
                        totalScore += testCase.getWeight();
                    }
                    
                    if (testResult.getExecutionTime() != null) {
                        totalExecutionTime += testResult.getExecutionTime();
                    }
                    
                    // Save test result to database asynchronously
                    if (submission != null) {
                        CompletableFuture.runAsync(() -> saveTestResult(submission, testCase, testResult), executorService);
                    }
                    
                } catch (Exception e) {
                    log.error("Error executing test case {}: {}", testCase.getId(), e.getMessage());
                    
                    // Create failed test result
                    TestResultResponse failedResult = createFailedTestResult(testCase, e.getMessage());
                    testResults.add(failedResult);
                    
                    if (submission != null) {
                        CompletableFuture.runAsync(() -> saveTestResult(submission, testCase, failedResult), executorService);
                    }
                }
            }
            
            response.setSuccess(true);
            response.setTestResults(testResults);
            response.setPassedTests(passedTests);
            response.setTotalTests(testCases.size());
            response.setScore(totalScore);
            response.setExecutionTime(totalExecutionTime);
            
        } catch (Exception e) {
            log.error("Error executing code with test cases via Jobe server", e);
            response.setSuccess(false);
            response.setError("Lỗi khi thực thi test cases qua Jobe server: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Execute single test case via Jobe server
     */
    private TestResultResponse executeTestCaseViaJobe(String code, String language, TestCase testCase) {
        TestResultResponse result = new TestResultResponse();
        result.setTestCaseId(testCase.getId());
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput().trim());

        try {
            long startTime = System.currentTimeMillis();
            
            // Create request for Jobe server
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("run_spec", createJobeRequest(code, language, testCase.getInput()));
            
            // Send request to Jobe server
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", jobeApiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = jobeServerUrl + "/jobe/index.php/restapi/runs";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Parse Jobe response
            JsonNode jobeResult = objectMapper.readTree(response.getBody());
            
            if (jobeResult.has("outcome") && jobeResult.get("outcome").asInt() == 15) { // Success
                String actualOutput = jobeResult.has("stdout") ? jobeResult.get("stdout").asText().trim() : "";
                String stderr = jobeResult.has("stderr") ? jobeResult.get("stderr").asText() : "";
                
                result.setActualOutput(actualOutput);
                result.setExecutionTime(executionTime);
                
                if (jobeResult.has("cmpinfo")) {
                    result.setMemoryUsed((long) jobeResult.get("cmpinfo").asInt());
                }
                
                // Compare outputs
                boolean passed = actualOutput.equals(testCase.getExpectedOutput().trim());
                result.setPassed(passed);
                
                if (!passed) {
                    result.setErrorMessage("Output không khớp với kết quả mong đợi");
                }
                
                if (!stderr.isEmpty()) {
                    result.setErrorMessage(stderr);
                }
                
            } else {
                // Execution failed
                result.setPassed(false);
                String error = "Thực thi thất bại";
                
                if (jobeResult.has("stderr")) {
                    error = jobeResult.get("stderr").asText();
                } else if (jobeResult.has("cmpinfo")) {
                    error = "Compilation error: " + jobeResult.get("cmpinfo").asText();
                }
                
                result.setErrorMessage(error);
            }
            
        } catch (Exception e) {
            log.error("Error executing test case {} via Jobe", testCase.getId(), e);
            result.setPassed(false);
            result.setErrorMessage("Lỗi khi thực thi: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Create Jobe request object
     */
    private Map<String, Object> createJobeRequest(String code, String language, String input) {
        Map<String, Object> request = new HashMap<>();
        
        // Set language ID for Jobe
        String jobeLanguageId = getJobeLanguageId(language);
        request.put("language_id", jobeLanguageId);
        request.put("sourcecode", code);
        request.put("sourcefilename", getSourceFileName(language));
        
        // Set execution parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cputime", 30);  // CPU time limit in seconds
        parameters.put("memorylimit", 256000);  // Memory limit in KB
        request.put("parameters", parameters);
        
        // Set input if provided
        if (input != null && !input.trim().isEmpty()) {
            request.put("input", input);
        }
        
        return request;
    }

    /**
     * Get Jobe language ID from our language string
     */
    private String getJobeLanguageId(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return "java";
            case "python":
                return "python3";
            case "cpp":
            case "c++":
                return "cpp";
            case "c":
                return "c";
            case "javascript":
            case "js":
                return "nodejs";
            default:
                return language.toLowerCase();
        }
    }

    /**
     * Get source filename for JOBE based on language
     */
    private String getSourceFileName(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return "Main.java";
            case "python":
                return "main.py";
            case "cpp":
            case "c++":
                return "main.cpp";
            case "c":
                return "main.c";
            case "javascript":
            case "js":
                return "main.js";
            default:
                return "main." + language.toLowerCase();
        }
    }

    /**
     * Parse Jobe server response
     */
    private CodeExecutionResponse parseJobeResponse(String jobeResponseBody, String language) {
        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setLanguage(language);
        
        try {
            JsonNode jobeResult = objectMapper.readTree(jobeResponseBody);
            
            if (jobeResult.has("outcome")) {
                int outcome = jobeResult.get("outcome").asInt();
                
                if (outcome == 15) { // Success
                    response.setSuccess(true);
                    response.setOutput(jobeResult.has("stdout") ? jobeResult.get("stdout").asText() : "");
                    
                    if (jobeResult.has("stderr") && !jobeResult.get("stderr").asText().isEmpty()) {
                        response.setError(jobeResult.get("stderr").asText());
                    }
                    
                } else {
                    // Execution failed
                    response.setSuccess(false);
                    
                    String error = "Execution failed";
                    if (jobeResult.has("stderr")) {
                        error = jobeResult.get("stderr").asText();
                    } else if (jobeResult.has("cmpinfo")) {
                        error = "Compilation error: " + jobeResult.get("cmpinfo").asText();
                        response.setCompilationError(error);
                    }
                    
                    response.setError(error);
                }
            } else {
                response.setSuccess(false);
                response.setError("Invalid response from Jobe server");
            }
            
        } catch (Exception e) {
            log.error("Error parsing Jobe response", e);
            response.setSuccess(false);
            response.setError("Error parsing Jobe server response: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Create error response
     */
    private CodeExecutionResponse createErrorResponse(String error) {
        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }

    /**
     * Create failed test result
     */
    private TestResultResponse createFailedTestResult(TestCase testCase, String error) {
        TestResultResponse result = new TestResultResponse();
        result.setTestCaseId(testCase.getId());
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput().trim());
        result.setPassed(false);
        result.setErrorMessage(error);
        return result;
    }

    /**
     * Save test result to database
     */
    private void saveTestResult(Submission submission, TestCase testCase, TestResultResponse testResult) {
        try {
            TestResult result = new TestResult();
            result.setSubmission(submission);
            result.setTestCase(testCase);
            result.setIsPassed(testResult.isPassed());
            result.setActualOutput(testResult.getActualOutput());
            result.setExecutionTime(testResult.getExecutionTime());
            result.setMemoryUsed(testResult.getMemoryUsed());
            result.setErrorMessage(testResult.getErrorMessage());
            
            testResultRepository.save(result);
            
        } catch (Exception e) {
            log.error("Error saving test result", e);
        }
    }

    /**
     * Check if Jobe server is available
     */
    public boolean isJobeServerAvailable() {
        if (!jobeEnabled) {
            return false;
        }
        
        try {
            String url = jobeServerUrl + "/jobe/index.php/restapi/languages";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Jobe server is not available", e);
            return false;
        }
    }

    /**
     * Get supported languages from Jobe server
     */
    public List<String> getSupportedLanguages() {
        if (!jobeEnabled) {
            return Collections.emptyList();
        }
        
        try {
            String url = jobeServerUrl + "/jobe/index.php/restapi/languages";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            JsonNode languages = objectMapper.readTree(response.getBody());
            List<String> supportedLanguages = new ArrayList<>();
            
            if (languages.isArray()) {
                for (JsonNode lang : languages) {
                    if (lang.isArray() && lang.size() > 0) {
                        supportedLanguages.add(lang.get(0).asText());
                    }
                }
            }
            
            return supportedLanguages;
            
        } catch (Exception e) {
            log.error("Error getting supported languages from Jobe server", e);
            return Collections.emptyList();
        }
    }
}
