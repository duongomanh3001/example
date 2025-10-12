package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.dto.response.TestResultResponse;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.entity.TestResult;
import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.TestResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Code Execution Service
 * Unified service supporting multiple execution strategies:
 * - LOCAL: Execute code on local server
 * - JOBE: Execute code via Jobe server
 * - HYBRID: Try Jobe first, fallback to local
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final TestResultRepository testResultRepository;
    private final CodeWrapperService codeWrapperService;
    
    private ExecutionStrategy currentStrategy = ExecutionStrategy.LOCAL;
    private boolean jobeAvailable = false;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Configuration
    @Value("${execution.strategy:hybrid}")
    private String executionStrategy; // hybrid, jobe, local
    
    @Value("${jobe.server.url:http://localhost:4000}")
    private String jobeServerUrl;
    
    @Value("${jobe.server.enabled:false}")
    private boolean jobeEnabled;
    
    @Value("${jobe.server.api-key:2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF}")
    private String jobeApiKey;
    
    // Execution limits
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int EXECUTION_TIMEOUT = 30; // seconds
    private static final int COMPILATION_TIMEOUT = 60; // seconds
    private static final long MEMORY_LIMIT = 256 * 1024 * 1024; // 256MB
    private static final int MAX_OUTPUT_LENGTH = 10000; // characters
    
    // Thread pool for concurrent execution
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    );

    /**
     * Execute code using the configured strategy
     */
    public CodeExecutionResponse executeCode(String code, String language) {
        ExecutionStrategy strategy = determineExecutionStrategy();
        
        log.info("Executing code using strategy: {} for language: {}", strategy, language);
        
        switch (strategy) {
            case JOBE:
                return executeWithJobe(code, language);
            case LOCAL:
                return executeWithLocal(code, language);
            case HYBRID:
            default:
                return executeWithHybrid(code, language);
        }
    }

    /**
     * Execute code with input using the configured strategy
     */
    public CodeExecutionResponse executeCodeWithInput(String code, String language, String input) {
        ExecutionStrategy strategy = determineExecutionStrategy();
        
        log.info("Executing code with input using strategy: {} for language: {}", strategy, language);
        
        switch (strategy) {
            case JOBE:
                return executeWithInputJobe(code, language, input);
            case LOCAL:
                return executeWithInputLocal(code, language, input);
            case HYBRID:
            default:
                return executeWithInputHybrid(code, language, input);
        }
    }

    /**
     * Execute code with test cases (enhanced with wrapper support)
     */
    public CodeExecutionResponse executeCodeWithTestCases(String code, String language, 
                                                         List<TestCase> testCases, 
                                                         Submission submission, 
                                                         Question question) {
        ExecutionStrategy strategy = determineExecutionStrategy();
        
        log.info("Executing code with {} test cases using strategy: {} for language: {}", 
                testCases.size(), strategy, language);
        
        CodeExecutionResponse response;
        switch (strategy) {
            case JOBE:
                response = executeWithTestCasesJobe(code, language, testCases, submission, question);
                break;
            case LOCAL:
                response = executeWithTestCasesLocal(code, language, testCases, submission, question);
                break;
            case HYBRID:
            default:
                response = executeWithTestCasesHybrid(code, language, testCases, submission, question);
                break;
        }
        
        // Add detailed grading message
        if (response != null && (response.getMessage() == null || response.getMessage().isEmpty())) {
            response.setMessage(generateGradingMessage(response));
        }
        
        return response;
    }
    
    // Overloaded method for backward compatibility
    public CodeExecutionResponse executeCodeWithTestCases(String code, String language, 
                                                         List<TestCase> testCases, 
                                                         Submission submission) {
        return executeCodeWithTestCases(code, language, testCases, submission, null);
    }

    /**
     * Check if Jobe server is available
     */
    public boolean isJobeServerAvailable() {
        if (!jobeEnabled) {
            return false;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", jobeApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = jobeServerUrl + "/jobe/index.php/restapi/languages";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Jobe server availability check failed: {}", e.getMessage());
            return false;
        }
    }

    // ========== STRATEGY DETERMINATION ==========
    
    private ExecutionStrategy determineExecutionStrategy() {
        switch (executionStrategy.toLowerCase()) {
            case "jobe":
                return jobeEnabled && isJobeServerAvailable() ? 
                       ExecutionStrategy.JOBE : ExecutionStrategy.LOCAL;
            case "local":
                return ExecutionStrategy.LOCAL;
            case "hybrid":
            default:
                return jobeEnabled && isJobeServerAvailable() ? 
                       ExecutionStrategy.JOBE : ExecutionStrategy.LOCAL;
        }
    }

    // ========== JOBE EXECUTION METHODS ==========
    
    private CodeExecutionResponse executeWithJobe(String code, String language) {
        try {
            return performJobeExecution(code, language, null);
        } catch (Exception e) {
            log.warn("Jobe execution failed, falling back to local: {}", e.getMessage());
            return executeWithLocal(code, language);
        }
    }

    private CodeExecutionResponse executeWithInputJobe(String code, String language, String input) {
        try {
            CodeExecutionResponse response = performJobeExecution(code, language, input);
            
            // Check for math library issues
            if (!response.isSuccess() && response.getError() != null && 
                isMathLibraryError(response.getError())) {
                log.warn("Jobe execution failed due to math library issues, falling back to local");
                return executeWithInputLocal(code, language, input);
            }
            
            return response;
        } catch (Exception e) {
            log.warn("Jobe execution with input failed, falling back to local: {}", e.getMessage());
            return executeWithInputLocal(code, language, input);
        }
    }

    private CodeExecutionResponse executeWithTestCasesJobe(String code, String language, 
                                                          List<TestCase> testCases, 
                                                          Submission submission, 
                                                          Question question) {
        // If question parameter is provided, we need code wrapping - fall back to local
        if (question != null) {
            log.info("Question parameter provided, falling back to local execution for code wrapping support");
            return executeWithTestCasesLocal(code, language, testCases, submission, question);
        }
        
        try {
            return performJobeTestCaseExecution(code, language, testCases, submission);
        } catch (Exception e) {
            log.warn("Jobe execution with test cases failed, falling back to local: {}", e.getMessage());
            return executeWithTestCasesLocal(code, language, testCases, submission, question);
        }
    }

    // ========== LOCAL EXECUTION METHODS ==========
    
    private CodeExecutionResponse executeWithLocal(String code, String language) {
        try {
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(TEMP_DIR, "cscore_execution", uniqueId);
            Files.createDirectories(workDir);

            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setLanguage(language);

            switch (language.toLowerCase()) {
                case "java":
                    return executeJavaCodeLocal(code, workDir, response);
                case "python":
                    return executePythonCodeLocal(code, workDir, response);
                case "cpp":
                case "c++":
                    return executeCppCodeLocal(code, workDir, response);
                case "c":
                    return executeCCodeLocal(code, workDir, response);
                default:
                    response.setSuccess(false);
                    response.setError("Ngôn ngữ lập trình không được hỗ trợ: " + language);
                    return response;
            }
        } catch (Exception e) {
            log.error("Error executing code locally", e);
            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setSuccess(false);
            response.setError("Lỗi hệ thống khi thực thi code: " + e.getMessage());
            return response;
        }
    }

    private CodeExecutionResponse executeWithInputLocal(String code, String language, String input) {
        try {
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(TEMP_DIR, "cscore_execution", uniqueId);
            Files.createDirectories(workDir);

            long startTime = System.currentTimeMillis();
            String output = executeCodeWithInputLocal(code, language, input, workDir, EXECUTION_TIMEOUT * 1000);
            long executionTime = System.currentTimeMillis() - startTime;
            
            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setLanguage(language);
            response.setSuccess(true);
            response.setOutput(output);
            response.setExecutionTime(executionTime);

            // Cleanup
            deleteDirectory(workDir);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error executing code with input locally", e);
            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setSuccess(false);
            response.setError("Lỗi khi thực thi code với input: " + e.getMessage());
            return response;
        }
    }

    private CodeExecutionResponse executeWithTestCasesLocal(String code, String language, 
                                                           List<TestCase> testCases, 
                                                           Submission submission, 
                                                           Question question) {
        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setLanguage(language);
        
        List<TestResultResponse> testResults = new ArrayList<>();
        int passedTests = 0;
        double totalScore = 0.0;
        long totalExecutionTime = 0L;
        
        try {
            log.info("Executing code with {} test cases locally - Language: {}", testCases.size(), language);
            
            // Wrap code if question provided
            String executableCode = code;
            if (question != null) {
                executableCode = codeWrapperService.wrapFunctionCode(code, question, language, testCases);
                log.debug("Code wrapped for execution");
            }
            
            // Execute each test case
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                log.debug("Executing test case {} of {}", i + 1, testCases.size());
                
                try {
                    TestResultResponse testResult = executeTestCaseLocal(executableCode, language, testCase);
                    testResults.add(testResult);
                    
                    // Update statistics
                    if (testResult.isPassed()) {
                        passedTests++;
                        totalScore += testCase.getWeight();
                    }
                    
                    if (testResult.getExecutionTime() != null) {
                        totalExecutionTime += testResult.getExecutionTime();
                    }
                    
                    // Save test result asynchronously
                    if (submission != null) {
                        CompletableFuture.runAsync(() -> saveTestResult(submission, testCase, testResult), executorService);
                    }
                    
                } catch (Exception e) {
                    log.error("Error executing test case {}: {}", testCase.getId(), e.getMessage());
                    
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
            log.error("Error executing code with test cases locally", e);
            response.setSuccess(false);
            response.setError("Lỗi khi thực thi test cases: " + e.getMessage());
        }
        
        return response;
    }

    // ========== HYBRID EXECUTION METHODS ==========
    
    private CodeExecutionResponse executeWithHybrid(String code, String language) {
        return jobeEnabled && isJobeServerAvailable() ? 
               executeWithJobe(code, language) : executeWithLocal(code, language);
    }

    private CodeExecutionResponse executeWithInputHybrid(String code, String language, String input) {
        return jobeEnabled && isJobeServerAvailable() ? 
               executeWithInputJobe(code, language, input) : executeWithInputLocal(code, language, input);
    }

    private CodeExecutionResponse executeWithTestCasesHybrid(String code, String language, 
                                                            List<TestCase> testCases, 
                                                            Submission submission, 
                                                            Question question) {
        return jobeEnabled && isJobeServerAvailable() ? 
               executeWithTestCasesJobe(code, language, testCases, submission, question) : 
               executeWithTestCasesLocal(code, language, testCases, submission, question);
    }

    // ========== JOBE IMPLEMENTATION DETAILS ==========
    
    private CodeExecutionResponse performJobeExecution(String code, String language, String input) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("run_spec", createJobeRequest(code, language, input));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", jobeApiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        String url = jobeServerUrl + "/jobe/index.php/restapi/runs";
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        return parseJobeResponse(response.getBody(), language);
    }

    private CodeExecutionResponse performJobeTestCaseExecution(String code, String language, 
                                                              List<TestCase> testCases, 
                                                              Submission submission) throws Exception {
        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setLanguage(language);
        
        List<TestResultResponse> testResults = new ArrayList<>();
        int passedTests = 0;
        double totalScore = 0.0;
        long totalExecutionTime = 0L;
        
        for (TestCase testCase : testCases) {
            try {
                TestResultResponse testResult = executeTestCaseViaJobe(code, language, testCase);
                testResults.add(testResult);
                
                if (testResult.isPassed()) {
                    passedTests++;
                    totalScore += testCase.getWeight();
                }
                
                if (testResult.getExecutionTime() != null) {
                    totalExecutionTime += testResult.getExecutionTime();
                }
                
                if (submission != null) {
                    CompletableFuture.runAsync(() -> saveTestResult(submission, testCase, testResult), executorService);
                }
                
            } catch (Exception e) {
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
        
        return response;
    }

    private Map<String, Object> createJobeRequest(String code, String language, String input) {
        Map<String, Object> runSpec = new HashMap<>();
        runSpec.put("language_id", mapLanguageToJobeId(language));
        runSpec.put("sourcefilename", getSourceFileName(language));
        runSpec.put("sourcecode", code);
        
        if (input != null && !input.trim().isEmpty()) {
            runSpec.put("input", input);
        }
        
        // Add compilation and execution parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("memorylimit", (int)(MEMORY_LIMIT / (1024 * 1024))); // Convert to MB
        parameters.put("cputime", EXECUTION_TIMEOUT);
        runSpec.put("parameters", parameters);
        
        return runSpec;
    }

    private String mapLanguageToJobeId(String language) {
        switch (language.toLowerCase()) {
            case "java": return "java";
            case "python": return "python3";
            case "cpp":
            case "c++": return "cpp";
            case "c": return "c";
            default: return language.toLowerCase();
        }
    }

    private String getSourceFileName(String language) {
        switch (language.toLowerCase()) {
            case "java": return "Main.java";
            case "python": return "main.py";
            case "cpp":
            case "c++": return "main.cpp";
            case "c": return "main.c";
            default: return "main.txt";
        }
    }

    private CodeExecutionResponse parseJobeResponse(String responseBody, String language) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        
        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setLanguage(language);
        
        // Check for compilation or runtime errors
        if (jsonResponse.has("outcome") && jsonResponse.get("outcome").asInt() != 15) {
            response.setSuccess(false);
            String stderr = jsonResponse.has("stderr") ? jsonResponse.get("stderr").asText() : "";
            String cmpinfo = jsonResponse.has("cmpinfo") ? jsonResponse.get("cmpinfo").asText() : "";
            response.setError(stderr.isEmpty() ? cmpinfo : stderr);
        } else {
            response.setSuccess(true);
            response.setOutput(jsonResponse.has("stdout") ? jsonResponse.get("stdout").asText() : "");
        }
        
        // Set execution time if available
        if (jsonResponse.has("cputime")) {
            response.setExecutionTime((long)(jsonResponse.get("cputime").asDouble() * 1000));
        }
        
        return response;
    }

    private TestResultResponse executeTestCaseViaJobe(String code, String language, TestCase testCase) throws Exception {
        long startTime = System.currentTimeMillis();
        
        CodeExecutionResponse executionResult = performJobeExecution(code, language, testCase.getInput());
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        TestResultResponse testResult = new TestResultResponse();
        testResult.setTestCaseId(testCase.getId());
        testResult.setInput(testCase.getInput());
        testResult.setExpectedOutput(testCase.getExpectedOutput());
        testResult.setActualOutput(executionResult.getOutput());
        testResult.setExecutionTime(executionTime);
        
        if (!executionResult.isSuccess()) {
            testResult.setPassed(false);
            testResult.setErrorMessage(executionResult.getError());
        } else {
            boolean passed = compareOutputs(testCase.getExpectedOutput(), executionResult.getOutput());
            testResult.setPassed(passed);
            
            if (!passed) {
                testResult.setErrorMessage("Kết quả không khớp với expected output");
            }
        }
        
        return testResult;
    }

    // ========== LOCAL EXECUTION IMPLEMENTATION ==========
    
    private CodeExecutionResponse executeJavaCodeLocal(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Write Java source file
            Path sourceFile = workDir.resolve("Main.java");
            Files.write(sourceFile, code.getBytes());
            
            // Compile
            Process compileProcess = new ProcessBuilder("javac", sourceFile.toString())
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            
            boolean compiled = compileProcess.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
            if (!compiled || compileProcess.exitValue() != 0) {
                String error = readProcessOutput(compileProcess.getInputStream());
                response.setSuccess(false);
                response.setError("Compilation error: " + error);
                return response;
            }
            
            // Execute
            Process execProcess = new ProcessBuilder("java", "-cp", workDir.toString(), "Main")
                    .directory(workDir.toFile())
                    .start();
            
            boolean executed = execProcess.waitFor(EXECUTION_TIMEOUT, TimeUnit.SECONDS);
            if (!executed) {
                execProcess.destroyForcibly();
                response.setSuccess(false);
                response.setError("Execution timeout");
                return response;
            }
            
            if (execProcess.exitValue() != 0) {
                String error = readProcessOutput(execProcess.getErrorStream());
                response.setSuccess(false);
                response.setError("Runtime error: " + error);
                return response;
            }
            
            String output = readProcessOutput(execProcess.getInputStream());
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Error executing Java code: " + e.getMessage());
        } finally {
            // Cleanup
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private CodeExecutionResponse executePythonCodeLocal(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Write Python source file
            Path sourceFile = workDir.resolve("main.py");
            Files.write(sourceFile, code.getBytes());
            
            // Execute
            Process execProcess = new ProcessBuilder("python", sourceFile.toString())
                    .directory(workDir.toFile())
                    .start();
            
            boolean executed = execProcess.waitFor(EXECUTION_TIMEOUT, TimeUnit.SECONDS);
            if (!executed) {
                execProcess.destroyForcibly();
                response.setSuccess(false);
                response.setError("Execution timeout");
                return response;
            }
            
            if (execProcess.exitValue() != 0) {
                String error = readProcessOutput(execProcess.getErrorStream());
                response.setSuccess(false);
                response.setError("Runtime error: " + error);
                return response;
            }
            
            String output = readProcessOutput(execProcess.getInputStream());
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Error executing Python code: " + e.getMessage());
        } finally {
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private CodeExecutionResponse executeCppCodeLocal(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Write C++ source file
            Path sourceFile = workDir.resolve("main.cpp");
            Files.write(sourceFile, code.getBytes());
            
            // Compile
            Path executableFile = workDir.resolve("main");
            Process compileProcess = new ProcessBuilder("g++", "-o", executableFile.toString(), 
                    sourceFile.toString(), "-lm", "-std=c++17")
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            
            boolean compiled = compileProcess.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
            if (!compiled || compileProcess.exitValue() != 0) {
                String error = readProcessOutput(compileProcess.getInputStream());
                response.setSuccess(false);
                response.setError("Compilation error: " + error);
                return response;
            }
            
            // Execute
            Process execProcess = new ProcessBuilder(executableFile.toString())
                    .directory(workDir.toFile())
                    .start();
            
            boolean executed = execProcess.waitFor(EXECUTION_TIMEOUT, TimeUnit.SECONDS);
            if (!executed) {
                execProcess.destroyForcibly();
                response.setSuccess(false);
                response.setError("Execution timeout");
                return response;
            }
            
            if (execProcess.exitValue() != 0) {
                String error = readProcessOutput(execProcess.getErrorStream());
                response.setSuccess(false);
                response.setError("Runtime error: " + error);
                return response;
            }
            
            String output = readProcessOutput(execProcess.getInputStream());
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Error executing C++ code: " + e.getMessage());
        } finally {
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private CodeExecutionResponse executeCCodeLocal(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Write C source file
            Path sourceFile = workDir.resolve("main.c");
            Files.write(sourceFile, code.getBytes());
            
            // Compile
            Path executableFile = workDir.resolve("main");
            Process compileProcess = new ProcessBuilder("gcc", "-o", executableFile.toString(), 
                    sourceFile.toString(), "-lm", "-std=c99")
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            
            boolean compiled = compileProcess.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
            if (!compiled || compileProcess.exitValue() != 0) {
                String error = readProcessOutput(compileProcess.getInputStream());
                response.setSuccess(false);
                response.setError("Compilation error: " + error);
                return response;
            }
            
            // Execute
            Process execProcess = new ProcessBuilder(executableFile.toString())
                    .directory(workDir.toFile())
                    .start();
            
            boolean executed = execProcess.waitFor(EXECUTION_TIMEOUT, TimeUnit.SECONDS);
            if (!executed) {
                execProcess.destroyForcibly();
                response.setSuccess(false);
                response.setError("Execution timeout");
                return response;
            }
            
            if (execProcess.exitValue() != 0) {
                String error = readProcessOutput(execProcess.getErrorStream());
                response.setSuccess(false);
                response.setError("Runtime error: " + error);
                return response;
            }
            
            String output = readProcessOutput(execProcess.getInputStream());
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Error executing C code: " + e.getMessage());
        } finally {
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private String executeCodeWithInputLocal(String code, String language, String input, 
                                            Path workDir, long timeoutMs) throws Exception {
        Process process = null;
        
        try {
            switch (language.toLowerCase()) {
                case "java":
                    // Compile and run Java
                    Path javaFile = workDir.resolve("Main.java");
                    Files.write(javaFile, code.getBytes());
                    
                    Process compileProcess = new ProcessBuilder("javac", javaFile.toString())
                            .directory(workDir.toFile()).start();
                    compileProcess.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
                    
                    if (compileProcess.exitValue() != 0) {
                        throw new RuntimeException("Compilation failed");
                    }
                    
                    process = new ProcessBuilder("java", "-cp", workDir.toString(), "Main")
                            .directory(workDir.toFile()).start();
                    break;
                    
                case "python":
                    Path pythonFile = workDir.resolve("main.py");
                    Files.write(pythonFile, code.getBytes());
                    process = new ProcessBuilder("python", pythonFile.toString())
                            .directory(workDir.toFile()).start();
                    break;
                    
                case "cpp":
                case "c++":
                    Path cppFile = workDir.resolve("main.cpp");
                    Files.write(cppFile, code.getBytes());
                    
                    Path cppExec = workDir.resolve("main");
                    Process cppCompile = new ProcessBuilder("g++", "-o", cppExec.toString(), 
                            cppFile.toString(), "-lm", "-std=c++17")
                            .directory(workDir.toFile()).start();
                    cppCompile.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
                    
                    if (cppCompile.exitValue() != 0) {
                        throw new RuntimeException("Compilation failed");
                    }
                    
                    process = new ProcessBuilder(cppExec.toString())
                            .directory(workDir.toFile()).start();
                    break;
                    
                case "c":
                    Path cFile = workDir.resolve("main.c");
                    Files.write(cFile, code.getBytes());
                    
                    Path cExec = workDir.resolve("main");
                    Process cCompile = new ProcessBuilder("gcc", "-o", cExec.toString(), 
                            cFile.toString(), "-lm", "-std=c99")
                            .directory(workDir.toFile()).start();
                    cCompile.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
                    
                    if (cCompile.exitValue() != 0) {
                        throw new RuntimeException("Compilation failed");
                    }
                    
                    process = new ProcessBuilder(cExec.toString())
                            .directory(workDir.toFile()).start();
                    break;
                    
                default:
                    throw new RuntimeException("Unsupported language: " + language);
            }
            
            // Send input to process
            if (input != null && !input.trim().isEmpty()) {
                try (PrintWriter writer = new PrintWriter(process.getOutputStream())) {
                    writer.println(input);
                    writer.flush();
                }
            }
            
            // Wait for completion with timeout
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Execution timeout");
            }
            
            if (process.exitValue() != 0) {
                String error = readProcessOutput(process.getErrorStream());
                throw new RuntimeException("Runtime error: " + error);
            }
            
            return readProcessOutput(process.getInputStream());
            
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private TestResultResponse executeTestCaseLocal(String code, String language, TestCase testCase) {
        long startTime = System.currentTimeMillis();
        
        TestResultResponse testResult = new TestResultResponse();
        testResult.setTestCaseId(testCase.getId());
        testResult.setInput(testCase.getInput());
        testResult.setExpectedOutput(testCase.getExpectedOutput());
        
        try {
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(TEMP_DIR, "cscore_test", uniqueId);
            Files.createDirectories(workDir);
            
            String actualOutput = executeCodeWithInputLocal(code, language, testCase.getInput(), 
                                                           workDir, EXECUTION_TIMEOUT * 1000);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            testResult.setActualOutput(actualOutput);
            testResult.setExecutionTime(executionTime);
            
            boolean passed = compareOutputs(testCase.getExpectedOutput(), actualOutput);
            testResult.setPassed(passed);
            
            if (!passed) {
                testResult.setErrorMessage("Kết quả không khớp với expected output");
            }
            
            // Cleanup
            deleteDirectory(workDir);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            testResult.setExecutionTime(executionTime);
            testResult.setPassed(false);
            testResult.setErrorMessage("Lỗi thực thi: " + e.getMessage());
            testResult.setActualOutput("");
        }
        
        return testResult;
    }

    // ========== UTILITY METHODS ==========
    
    private boolean isMathLibraryError(String error) {
        return error.contains("undefined reference to `sqrt'") ||
               error.contains("undefined reference to `pow'") ||
               error.contains("undefined reference to `sin'") ||
               error.contains("undefined reference to `cos'") ||
               error.contains("undefined reference to `tan'") ||
               error.contains("math.h");
    }

    private String generateGradingMessage(CodeExecutionResponse response) {
        if (response.isSuccess()) {
            int passedTests = response.getPassedTests();
            int totalTests = response.getTotalTests();
            return String.format(
                "Chấm điểm tự động hoàn thành: Code của bạn được thực thi với %d test case(s) từ giảng viên. " +
                "Kết quả: %d/%d test case(s) đạt yêu cầu. " +
                "Điểm số được tính dựa trên output thực tế so với expected output và trọng số của từng test case. " +
                "Không có so sánh trực tiếp code với đáp án của giảng viên.",
                totalTests, passedTests, totalTests
            );
        } else {
            return "Chấm điểm tự động: Code có lỗi trong quá trình biên dịch hoặc thực thi. " +
                   "Hệ thống không thể chạy test cases. Vui lòng kiểm tra lại code và thử lại.";
        }
    }

    private boolean compareOutputs(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        
        // Normalize whitespace and compare
        String normalizedExpected = expected.trim().replaceAll("\\s+", " ");
        String normalizedActual = actual.trim().replaceAll("\\s+", " ");
        
        return normalizedExpected.equals(normalizedActual);
    }

    private TestResultResponse createFailedTestResult(TestCase testCase, String errorMessage) {
        TestResultResponse testResult = new TestResultResponse();
        testResult.setTestCaseId(testCase.getId());
        testResult.setInput(testCase.getInput());
        testResult.setExpectedOutput(testCase.getExpectedOutput());
        testResult.setActualOutput("");
        testResult.setPassed(false);
        testResult.setErrorMessage(errorMessage);
        testResult.setExecutionTime(0L);
        return testResult;
    }

    private void saveTestResult(Submission submission, TestCase testCase, TestResultResponse testResult) {
        try {
            TestResult entity = new TestResult();
            entity.setSubmission(submission);
            entity.setTestCase(testCase);
            entity.setPassed(testResult.isPassed());
            entity.setActualOutput(testResult.getActualOutput());
            entity.setErrorMessage(testResult.getErrorMessage());
            entity.setExecutionTime(testResult.getExecutionTime());
            
            testResultRepository.save(entity);
        } catch (Exception e) {
            log.error("Error saving test result", e);
        }
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (output.length() > MAX_OUTPUT_LENGTH) {
                    output.append("... (output truncated)");
                    break;
                }
            }
        }
        return output.toString().trim();
    }

    private void deleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            log.warn("Failed to delete directory: {}", directory, e);
        }
    }

    // ========== ENUM ==========
    
    private enum ExecutionStrategy {
        LOCAL, JOBE, HYBRID
    }
    
    // ========== ADDITIONAL METHODS FOR CONTROLLER COMPATIBILITY ==========
    
    /**
     * Get system requirements
     */
    public Map<String, Object> getSystemRequirements() {
        Map<String, Object> requirements = new HashMap<>();
        requirements.put("javaVersion", System.getProperty("java.version"));
        requirements.put("osName", System.getProperty("os.name"));
        requirements.put("osVersion", System.getProperty("os.version"));
        requirements.put("architecture", System.getProperty("os.arch"));
        requirements.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        requirements.put("maxMemory", Runtime.getRuntime().maxMemory());
        requirements.put("freeMemory", Runtime.getRuntime().freeMemory());
        
        return requirements;
    }
    
    /**
     * Get supported languages
     */
    public Set<ProgrammingLanguage> getSupportedLanguages() {
        return Set.of(
            ProgrammingLanguage.JAVA,
            ProgrammingLanguage.PYTHON,
            ProgrammingLanguage.C,
            ProgrammingLanguage.CPP
        );
    }
    
    /**
     * Check if language is supported
     */
    public boolean isLanguageSupported(ProgrammingLanguage language) {
        return getSupportedLanguages().contains(language);
    }
    
    /**
     * Get compiler information for a language
     */
    public Map<String, Object> getCompilerInfo(ProgrammingLanguage language) {
        Map<String, Object> info = new HashMap<>();
        
        switch (language) {
            case JAVA:
                info.put("compiler", "javac");
                info.put("version", System.getProperty("java.version"));
                info.put("runtime", "java");
                break;
            case PYTHON:
                info.put("interpreter", "python3");
                info.put("version", "3.x");
                break;
            case C:
                info.put("compiler", "gcc");
                info.put("version", "latest");
                break;
            case CPP:
                info.put("compiler", "g++");
                info.put("version", "latest");
                break;
            default:
                info.put("error", "Unsupported language");
        }
        
        return info;
    }
    
    /**
     * Get execution information
     */
    public Map<String, Object> getExecutionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("strategy", currentStrategy.toString());
        info.put("supportedLanguages", getSupportedLanguages());
        info.put("maxTimeLimit", 30); // seconds
        info.put("maxMemoryLimit", "256MB");
        info.put("jobeAvailable", jobeAvailable);
        info.put("localExecutionEnabled", true);
        
        return info;
    }
}