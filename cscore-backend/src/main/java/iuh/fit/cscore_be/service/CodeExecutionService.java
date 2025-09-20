package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.response.CodeExecutionResponse;
import iuh.fit.cscore_be.dto.response.TestResultResponse;
import iuh.fit.cscore_be.entity.Assignment;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.Submission;
import iuh.fit.cscore_be.entity.TestCase;
import iuh.fit.cscore_be.entity.TestResult;
import iuh.fit.cscore_be.enums.SubmissionStatus;
import iuh.fit.cscore_be.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final TestResultRepository testResultRepository;
    private final CodeWrapperService codeWrapperService;
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int EXECUTION_TIMEOUT = 30; // seconds
    private static final int COMPILATION_TIMEOUT = 60; // seconds
    private static final long MEMORY_LIMIT = 256 * 1024 * 1024; // 256MB
    private static final int MAX_OUTPUT_LENGTH = 10000; // characters
    
    // Thread pools for concurrent execution
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    );

    public CodeExecutionResponse executeCode(String code, String language) {
        try {
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(TEMP_DIR, "cscore_execution", uniqueId);
            Files.createDirectories(workDir);

            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setLanguage(language);

            switch (language.toLowerCase()) {
                case "java":
                    return executeJavaCode(code, workDir, response);
                case "python":
                    return executePythonCode(code, workDir, response);
                case "cpp":
                case "c++":
                    return executeCppCode(code, workDir, response);
                case "c":
                    return executeCCode(code, workDir, response);
                default:
                    response.setSuccess(false);
                    response.setError("Ngôn ngữ lập trình không được hỗ trợ: " + language);
                    return response;
            }
        } catch (Exception e) {
            log.error("Error executing code", e);
            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setSuccess(false);
            response.setError("Lỗi hệ thống khi thực thi code: " + e.getMessage());
            return response;
        }
    }

    public CodeExecutionResponse executeCodeWithInput(String code, String language, String input) {
        try {
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(TEMP_DIR, "cscore_execution", uniqueId);
            Files.createDirectories(workDir);

            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setLanguage(language);

            long startTime = System.currentTimeMillis();
            
            String output = executeCodeWithInput(code, language, input, workDir, EXECUTION_TIMEOUT * 1000);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            response.setSuccess(true);
            response.setOutput(output);
            response.setExecutionTime(executionTime);

            // Cleanup
            deleteDirectory(workDir);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error executing code with input", e);
            CodeExecutionResponse response = new CodeExecutionResponse();
            response.setSuccess(false);
            response.setError("Lỗi hệ thống khi thực thi code: " + e.getMessage());
            return response;
        }
    }

    public CodeExecutionResponse executeCodeWithTestCases(String code, String language, List<TestCase> testCases, Submission submission) {
        return executeCodeWithTestCases(code, language, testCases, submission, null);
    }

    public CodeExecutionResponse executeCodeWithTestCases(String code, String language, List<TestCase> testCases, Submission submission, Question question) {
        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setLanguage(language);
        
        // Apply code wrapping if question is provided and code doesn't have main function
        String executableCode = code;
        if (question != null) {
            log.debug("Original student code:\n{}", code);
            // Use enhanced wrapper with test case analysis
            executableCode = codeWrapperService.wrapFunctionCode(code, question, language, testCases);
            if (!executableCode.equals(code)) {
                log.info("Applied enhanced code wrapping for question {} in language {} with {} test cases", 
                    question.getId(), language, testCases.size());
                log.debug("Wrapped code:\n{}", executableCode);
            }
        }
        
        List<TestResultResponse> testResults = new ArrayList<>();
        int passedTests = 0;
        double totalScore = 0.0;
        long totalExecutionTime = 0L;
        long maxMemoryUsed = 0L;
        boolean compilationSuccessful = true;
        String compilationError = null;
        
        try {
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(TEMP_DIR, "cscore_execution", uniqueId);
            Files.createDirectories(workDir);

            log.info("Starting code execution for {} test cases with language: {}", testCases.size(), language);
            
            // Pre-compile if needed (for compiled languages)
            if (isCompiledLanguage(language)) {
                try {
                    preCompileCode(executableCode, language, workDir);
                } catch (CompilationException e) {
                    response.setSuccess(false);
                    response.setError(e.getMessage());
                    response.setCompilationError(e.getMessage());
                    response.setCompiled(false);
                    return response;
                }
            }

            // Execute test cases with improved error handling
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                log.debug("Executing test case {} of {}", i + 1, testCases.size());
                
                try {
                    TestResultResponse testResult = executeTestCase(executableCode, language, testCase, workDir);
                    testResults.add(testResult);
                    
                    // Update statistics
                    if (testResult.isPassed()) {
                        passedTests++;
                        totalScore += testCase.getWeight();
                    }
                    
                    if (testResult.getExecutionTime() != null) {
                        totalExecutionTime += testResult.getExecutionTime();
                    }
                    
                    if (testResult.getMemoryUsed() != null) {
                        maxMemoryUsed = Math.max(maxMemoryUsed, testResult.getMemoryUsed());
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
            
            // Cleanup
            deleteDirectory(workDir);
            
        } catch (Exception e) {
            log.error("Error executing code with test cases", e);
            response.setSuccess(false);
            response.setError("Lỗi khi thực thi test cases: " + e.getMessage());
        }
        
        return response;
    }

    private TestResultResponse executeTestCase(String code, String language, TestCase testCase, Path workDir) {
        TestResultResponse result = new TestResultResponse();
        result.setTestCaseId(testCase.getId());
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput().trim());
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Preprocess input to handle escaped quotes and formatting
            String processedInput = preprocessTestInput(testCase.getInput());
            log.debug("Original input: '{}', Processed input: '{}'", testCase.getInput(), processedInput);
            
            String output = executeCodeWithInput(code, language, processedInput, workDir, testCase.getTimeLimit());
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTime(executionTime);
            result.setActualOutput(output.trim());
            
            boolean passed = output.trim().equals(testCase.getExpectedOutput().trim());
            result.setPassed(passed);
            
            if (!passed) {
                result.setErrorMessage("Output không khớp với kết quả mong đợi");
            }
            
        } catch (TimeoutException e) {
            result.setPassed(false);
            result.setErrorMessage("Vượt quá thời gian thực thi cho phép");
            result.setExecutionTime((long) testCase.getTimeLimit());
        } catch (Exception e) {
            result.setPassed(false);
            result.setErrorMessage("Lỗi thực thi: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Preprocess test input to handle escaped quotes and proper formatting
     */
    private String preprocessTestInput(String input) {
        if (input == null) return "";
        
        // Handle escaped quotes in JSON-like format
        String processed = input;
        
        // If input starts and ends with quotes, remove outer quotes
        if (processed.startsWith("\"") && processed.endsWith("\"") && processed.length() > 1) {
            processed = processed.substring(1, processed.length() - 1);
        }
        
        // Replace escaped quotes with actual quotes
        processed = processed.replace("\\\"", "\"");
        
        // Handle other common escape sequences
        processed = processed.replace("\\\\", "\\");
        processed = processed.replace("\\n", "\n");
        processed = processed.replace("\\t", "\t");
        
        return processed;
    }

    private void saveTestResult(Submission submission, TestCase testCase, TestResultResponse testResult) {
        TestResult entity = new TestResult();
        entity.setSubmission(submission);
        entity.setTestCase(testCase);
        entity.setActualOutput(testResult.getActualOutput());
        entity.setIsPassed(testResult.isPassed());
        entity.setExecutionTime(testResult.getExecutionTime());
        entity.setErrorMessage(testResult.getErrorMessage());
        
        testResultRepository.save(entity);
    }

    private CodeExecutionResponse executeJavaCode(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Extract class name from code
            String className = extractJavaClassName(code);
            if (className == null) {
                className = "Solution"; // Default fallback
            }
            
            // Write Java file with correct class name
            Path javaFile = workDir.resolve(className + ".java");
            Files.write(javaFile, code.getBytes());

            // Compile
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", javaFile.toString());
            compileBuilder.directory(workDir.toFile());
            Process compileProcess = compileBuilder.start();
            
            if (compileProcess.waitFor() != 0) {
                response.setSuccess(false);
                response.setError(readErrorStream(compileProcess));
                return response;
            }

            // Run with the correct class name
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", workDir.toString(), className);
            runBuilder.directory(workDir.toFile());
            Process runProcess = runBuilder.start();
            
            String output = readOutputStream(runProcess, EXECUTION_TIMEOUT);
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Lỗi thực thi Java: " + e.getMessage());
        } finally {
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private CodeExecutionResponse executePythonCode(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Write Python file
            Path pythonFile = workDir.resolve("solution.py");
            Files.write(pythonFile, code.getBytes());

            // Try different Python commands based on OS
            String[] pythonCommands = getPythonCommands();
            ProcessBuilder runBuilder = null;
            
            for (String pythonCmd : pythonCommands) {
                try {
                    runBuilder = new ProcessBuilder(pythonCmd, pythonFile.toString());
                    runBuilder.directory(workDir.toFile());
                    break;
                } catch (Exception e) {
                    // Try next command
                    continue;
                }
            }
            
            if (runBuilder == null) {
                response.setSuccess(false);
                response.setError("Python không được cài đặt hoặc không tìm thấy trong PATH");
                return response;
            }
            
            Process runProcess = runBuilder.start();
            
            String output = readOutputStream(runProcess, EXECUTION_TIMEOUT);
            
            // Check if process completed successfully
            int exitCode = runProcess.waitFor();
            if (exitCode != 0) {
                String errorOutput = readErrorStream(runProcess);
                response.setSuccess(false);
                response.setError("Lỗi thực thi Python (Exit Code: " + exitCode + "): " + errorOutput);
                return response;
            }
            
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Lỗi thực thi Python: " + e.getMessage());
        } finally {
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private CodeExecutionResponse executeCppCode(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Write C++ file
            Path cppFile = workDir.resolve("solution.cpp");
            Files.write(cppFile, code.getBytes());
            
            Path executable = workDir.resolve("solution.exe");

            // Compile
            ProcessBuilder compileBuilder = new ProcessBuilder("g++", "-o", executable.toString(), cppFile.toString());
            compileBuilder.directory(workDir.toFile());
            Process compileProcess = compileBuilder.start();
            
            if (compileProcess.waitFor() != 0) {
                response.setSuccess(false);
                response.setError(readErrorStream(compileProcess));
                return response;
            }

            // Run
            ProcessBuilder runBuilder = new ProcessBuilder(executable.toString());
            runBuilder.directory(workDir.toFile());
            Process runProcess = runBuilder.start();
            
            String output = readOutputStream(runProcess, EXECUTION_TIMEOUT);
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Lỗi thực thi C++: " + e.getMessage());
        } finally {
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private CodeExecutionResponse executeCCode(String code, Path workDir, CodeExecutionResponse response) {
        try {
            // Write C file with UTF-8 encoding
            Path cFile = workDir.resolve("solution.c");
            Files.write(cFile, code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            Path executable = workDir.resolve("solution.exe");

            // Get C compiler command based on OS
            String[] compilerCommands = getCCompilerCommands();
            ProcessBuilder compileBuilder = null;
            String usedCompiler = null;
            
            for (String compilerCmd : compilerCommands) {
                try {
                    compileBuilder = createGccCompileCommand(compilerCmd, executable.toString(), cFile.toString(), workDir);
                    compileBuilder.directory(workDir.toFile());
                    usedCompiler = compilerCmd;
                    log.debug("Trying C compiler: {}", compilerCmd);
                    break;
                } catch (Exception e) {
                    log.debug("C compiler {} not available: {}", compilerCmd, e.getMessage());
                    // Try next compiler
                    continue;
                }
            }
            
            if (compileBuilder == null) {
                response.setSuccess(false);
                response.setError("GCC compiler không được cài đặt hoặc không tìm thấy trong PATH");
                return response;
            }
            
            log.info("Using C compiler: {}", usedCompiler);
            
            Process compileProcess = compileBuilder.start();
            
            // Read streams immediately to prevent blocking
            StringBuilder compileStdout = new StringBuilder();
            StringBuilder compileStderr = new StringBuilder();
            
            // Start threads to read stdout and stderr concurrently
            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        compileStdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("Error reading compilation stdout", e);
                }
            });
            
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        compileStderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("Error reading compilation stderr", e);
                }
            });
            
            stdoutReader.start();
            stderrReader.start();
            
            int compileExitCode = compileProcess.waitFor();
            
            // Wait for stream readers to complete
            try {
                stdoutReader.join(5000); // 5 second timeout
                stderrReader.join(5000);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for stream readers");
            }
            
            if (compileExitCode != 0) {
                String stdoutStr = compileStdout.toString().trim();
                String stderrStr = compileStderr.toString().trim();
                
                String errorMessage = "Compilation failed (Exit Code: " + compileExitCode + ")";
                if (!stderrStr.isEmpty()) {
                    errorMessage += "\nCompilation Error: " + stderrStr;
                }
                if (!stdoutStr.isEmpty()) {
                    errorMessage += "\nCompilation Output: " + stdoutStr;
                }
                
                // Also log the actual source code being compiled for debugging
                try {
                    String sourceCode = new String(Files.readAllBytes(cFile), java.nio.charset.StandardCharsets.UTF_8);
                    log.error("Source code that failed to compile:\n{}", sourceCode);
                } catch (Exception e) {
                    log.warn("Could not read source file for debugging", e);
                }
                
                log.error("C compilation failed: {}", errorMessage);
                response.setSuccess(false);
                response.setError("Lỗi biên dịch C: " + errorMessage);
                return response;
            }

            // Run
            ProcessBuilder runBuilder = new ProcessBuilder(executable.toString());
            runBuilder.directory(workDir.toFile());
            Process runProcess = runBuilder.start();
            
            String output = readOutputStream(runProcess, EXECUTION_TIMEOUT);
            
            // Check if execution completed successfully
            int runExitCode = runProcess.waitFor();
            if (runExitCode != 0) {
                String errorOutput = readErrorStream(runProcess);
                response.setSuccess(false);
                response.setError("Lỗi thực thi C (Exit Code: " + runExitCode + "): " + errorOutput);
                return response;
            }
            
            response.setSuccess(true);
            response.setOutput(output);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Lỗi thực thi C: " + e.getMessage());
        } finally {
            deleteDirectory(workDir);
        }
        
        return response;
    }

    private String executeCodeWithInput(String code, String language, String input, Path workDir, Integer timeLimit) throws Exception {
        String uniqueFileName = UUID.randomUUID().toString();
        
        switch (language.toLowerCase()) {
            case "java":
                return executeJavaWithInput(code, input, workDir, uniqueFileName, timeLimit);
            case "python":
                return executePythonWithInput(code, input, workDir, uniqueFileName, timeLimit);
            case "cpp":
            case "c++":
                return executeCppWithInput(code, input, workDir, uniqueFileName, timeLimit);
            case "c":
                return executeCWithInput(code, input, workDir, uniqueFileName, timeLimit);
            default:
                throw new UnsupportedOperationException("Ngôn ngữ không hỗ trợ: " + language);
        }
    }

    private String executeJavaWithInput(String code, String input, Path workDir, String fileName, Integer timeLimit) throws Exception {
        // Extract class name from code
        String className = extractJavaClassName(code);
        if (className == null) {
            className = "Solution"; // Default fallback
        }
        
        Path javaFile = workDir.resolve(className + ".java");
        Files.write(javaFile, code.getBytes());

        // Compile
        ProcessBuilder compileBuilder = new ProcessBuilder("javac", javaFile.toString());
        compileBuilder.directory(workDir.toFile());
        Process compileProcess = compileBuilder.start();
        
        if (compileProcess.waitFor() != 0) {
            throw new RuntimeException("Compilation error: " + readErrorStream(compileProcess));
        }

        // Run with input
        ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", workDir.toString(), className);
        runBuilder.directory(workDir.toFile());
        Process runProcess = runBuilder.start();
        
        // Send input
        if (input != null && !input.isEmpty()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(runProcess.getOutputStream())) {
                writer.write(input);
                writer.flush();
            }
        } else {
            // Close the output stream even if no input
            runProcess.getOutputStream().close();
        }
        
        return readOutputStream(runProcess, timeLimit != null ? timeLimit / 1000 : EXECUTION_TIMEOUT);
    }

    private String executePythonWithInput(String code, String input, Path workDir, String fileName, Integer timeLimit) throws Exception {
        Path pythonFile = workDir.resolve(fileName + ".py");
        Files.write(pythonFile, code.getBytes());

        // Try different Python commands based on OS
        String[] pythonCommands = getPythonCommands();
        ProcessBuilder runBuilder = null;
        
        for (String pythonCmd : pythonCommands) {
            try {
                runBuilder = new ProcessBuilder(pythonCmd, pythonFile.toString());
                runBuilder.directory(workDir.toFile());
                break;
            } catch (Exception e) {
                // Try next command
                continue;
            }
        }
        
        if (runBuilder == null) {
            throw new RuntimeException("Python không được cài đặt hoặc không tìm thấy trong PATH");
        }
        
        Process runProcess = runBuilder.start();
        
        // Send input
        if (input != null && !input.isEmpty()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(runProcess.getOutputStream())) {
                writer.write(input);
                writer.flush();
            }
        } else {
            // Close the output stream even if no input
            runProcess.getOutputStream().close();
        }
        
        String output = readOutputStream(runProcess, timeLimit != null ? timeLimit / 1000 : EXECUTION_TIMEOUT);
        
        // Check exit code
        int exitCode = runProcess.waitFor();
        if (exitCode != 0) {
            String errorOutput = readErrorStream(runProcess);
            throw new RuntimeException("Python execution failed (Exit Code: " + exitCode + "): " + errorOutput);
        }
        
        return output;
    }

    private String executeCppWithInput(String code, String input, Path workDir, String fileName, Integer timeLimit) throws Exception {
        Path cppFile = workDir.resolve(fileName + ".cpp");
        Files.write(cppFile, code.getBytes());
        
        Path executable = workDir.resolve(fileName + ".exe");

        // Compile
        ProcessBuilder compileBuilder = new ProcessBuilder("g++", "-o", executable.toString(), cppFile.toString());
        compileBuilder.directory(workDir.toFile());
        Process compileProcess = compileBuilder.start();
        
        if (compileProcess.waitFor() != 0) {
            throw new RuntimeException("Compilation error: " + readErrorStream(compileProcess));
        }

        ProcessBuilder runBuilder = new ProcessBuilder(executable.toString());
        runBuilder.directory(workDir.toFile());
        Process runProcess = runBuilder.start();
        
        // Send input
        if (input != null && !input.isEmpty()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(runProcess.getOutputStream())) {
                writer.write(input);
                writer.flush();
            }
        } else {
            // Close the output stream even if no input
            runProcess.getOutputStream().close();
        }
        
        return readOutputStream(runProcess, timeLimit != null ? timeLimit / 1000 : EXECUTION_TIMEOUT);
    }

    private String executeCWithInput(String code, String input, Path workDir, String fileName, Integer timeLimit) throws Exception {
        // Write C file with UTF-8 encoding
        Path cFile = workDir.resolve(fileName + ".c");
        Files.write(cFile, code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        Path executable = workDir.resolve(fileName + ".exe");

        // Get C compiler command based on OS
        String[] compilerCommands = getCCompilerCommands();
        ProcessBuilder compileBuilder = null;
        String usedCompiler = null;
        
        for (String compilerCmd : compilerCommands) {
            try {
                compileBuilder = createGccCompileCommand(compilerCmd, executable.toString(), cFile.toString(), workDir);
                compileBuilder.directory(workDir.toFile());
                usedCompiler = compilerCmd;
                log.debug("Trying C compiler: {}", compilerCmd);
                break;
            } catch (Exception e) {
                log.debug("C compiler {} not available: {}", compilerCmd, e.getMessage());
                // Try next compiler
                continue;
            }
        }
        
        if (compileBuilder == null) {
            throw new RuntimeException("GCC compiler không được cài đặt hoặc không tìm thấy trong PATH");
        }
        
        log.info("Using C compiler: {}", usedCompiler);
        
        Process compileProcess = compileBuilder.start();
        
        // Read streams immediately to prevent blocking
        StringBuilder compileStdout = new StringBuilder();
        StringBuilder compileStderr = new StringBuilder();
        
        // Start threads to read stdout and stderr concurrently
        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    compileStdout.append(line).append("\n");
                }
            } catch (IOException e) {
                log.warn("Error reading compilation stdout", e);
            }
        });
        
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    compileStderr.append(line).append("\n");
                }
            } catch (IOException e) {
                log.warn("Error reading compilation stderr", e);
            }
        });
        
        stdoutReader.start();
        stderrReader.start();
        
        int compileExitCode = compileProcess.waitFor();
        
        // Wait for stream readers to complete
        try {
            stdoutReader.join(5000); // 5 second timeout
            stderrReader.join(5000);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for stream readers");
        }
        
        if (compileExitCode != 0) {
            String stdoutStr = compileStdout.toString().trim();
            String stderrStr = compileStderr.toString().trim();
            
            String errorMessage = "Compilation failed (Exit Code: " + compileExitCode + ")";
            if (!stderrStr.isEmpty()) {
                errorMessage += "\nCompilation Error: " + stderrStr;
            }
            if (!stdoutStr.isEmpty()) {
                errorMessage += "\nCompilation Output: " + stdoutStr;
            }
            
            // Also log the actual source code being compiled for debugging
            try {
                String sourceCode = new String(Files.readAllBytes(cFile), java.nio.charset.StandardCharsets.UTF_8);
                log.error("Source code that failed to compile:\n{}", sourceCode);
            } catch (Exception e) {
                log.warn("Could not read source file for debugging", e);
            }
            
            log.error("C compilation failed: {}", errorMessage);
            throw new RuntimeException(errorMessage);
        }

        ProcessBuilder runBuilder = new ProcessBuilder(executable.toString());
        runBuilder.directory(workDir.toFile());
        Process runProcess = runBuilder.start();
        
        // Send input
        if (input != null && !input.isEmpty()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(runProcess.getOutputStream())) {
                writer.write(input);
                writer.flush();
            }
        } else {
            // Close the output stream even if no input
            runProcess.getOutputStream().close();
        }
        
        String output = readOutputStream(runProcess, timeLimit != null ? timeLimit / 1000 : EXECUTION_TIMEOUT);
        
        // Check exit code
        int runExitCode = runProcess.waitFor();
        if (runExitCode != 0) {
            String errorOutput = readErrorStream(runProcess);
            throw new RuntimeException("C execution failed (Exit Code: " + runExitCode + "): " + errorOutput);
        }
        
        return output;
    }

    private String readOutputStream(Process process, int timeoutSeconds) throws Exception {
        StringBuilder output = new StringBuilder();
        
        Future<String> future = Executors.newSingleThreadExecutor().submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                process.waitFor(); // Wait for process to complete
                return output.toString();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            String result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return result != null ? result.trim() : "";
        } catch (TimeoutException e) {
            process.destroyForcibly();
            throw new TimeoutException("Vượt quá thời gian thực thi cho phép");
        }
    }

    private String readErrorStream(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
            return error.toString();
        } catch (IOException e) {
            return "Không thể đọc error stream";
        }
    }

    private void deleteDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Could not delete file: " + p, e);
                        }
                    });
            }
        } catch (Exception e) {
            log.warn("Could not delete directory: " + path, e);
        }
    }

    /**
     * Extract the main class name from Java source code
     */
    private String extractJavaClassName(String code) {
        try {
            // Look for public class first
            String publicClassPattern = "public\\s+class\\s+(\\w+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(publicClassPattern);
            java.util.regex.Matcher matcher = pattern.matcher(code);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // If no public class found, look for any class with main method
            String classPattern = "class\\s+(\\w+)\\s*\\{[^}]*public\\s+static\\s+void\\s+main";
            pattern = java.util.regex.Pattern.compile(classPattern, java.util.regex.Pattern.DOTALL);
            matcher = pattern.matcher(code);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // If still not found, just look for any class
            String anyClassPattern = "class\\s+(\\w+)";
            pattern = java.util.regex.Pattern.compile(anyClassPattern);
            matcher = pattern.matcher(code);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
        } catch (Exception e) {
            log.warn("Error extracting class name from Java code", e);
        }
        
        return null; // Return null if no class found
    }
    
    /**
     * Get Python command candidates based on operating system
     */
    private String[] getPythonCommands() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows - try py launcher first, then python3, then python
            return new String[]{"py", "python3", "python"};
        } else {
            // Linux/Unix/Mac - try python3 first, then python
            return new String[]{"python3", "python"};
        }
    }
    
    /**
     * Get C compiler command candidates based on operating system
     */
    private String[] getCCompilerCommands() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows - try MSYS2 GCC first, then standard GCC locations
            return new String[]{
                "C:\\msys64\\ucrt64\\bin\\gcc.exe",
                "C:\\msys64\\mingw64\\bin\\gcc.exe",
                "gcc"
            };
        } else {
            // Linux/Unix/Mac - try gcc
            return new String[]{"gcc"};
        }
    }
    
    /**
     * Create GCC compilation command with proper encoding and flags
     */
    private ProcessBuilder createGccCompileCommand(String gccPath, String outputPath, String sourcePath, Path workDir) {
        ProcessBuilder pb;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows - add UTF-8 support, math library and proper flags
            pb = new ProcessBuilder(gccPath, "-finput-charset=UTF-8", "-fexec-charset=UTF-8", "-o", outputPath, sourcePath, "-lm");
            
            // Add MSYS2 directories to PATH for DLL dependencies
            java.util.Map<String, String> env = pb.environment();
            String currentPath = env.get("PATH");
            if (currentPath == null) currentPath = "";
            
            // Add MSYS2 bin directories to PATH
            String msys2Paths = "C:\\msys64\\ucrt64\\bin;C:\\msys64\\mingw64\\bin;C:\\msys64\\usr\\bin";
            if (!currentPath.isEmpty()) {
                env.put("PATH", msys2Paths + ";" + currentPath);
            } else {
                env.put("PATH", msys2Paths);
            }
        } else {
            // Linux/Unix/Mac - standard compilation with math library
            pb = new ProcessBuilder(gccPath, "-o", outputPath, sourcePath, "-lm");
        }
        return pb;
    }
    
    /**
     * Check if a language requires compilation
     */
    private boolean isCompiledLanguage(String language) {
        return switch (language.toLowerCase()) {
            case "java", "c", "cpp", "c++" -> true;
            default -> false;
        };
    }
    
    /**
     * Pre-compile code for compiled languages
     */
    private void preCompileCode(String code, String language, Path workDir) throws CompilationException {
        try {
            switch (language.toLowerCase()) {
                case "java":
                    preCompileJava(code, workDir);
                    break;
                case "c":
                    preCompileC(code, workDir);
                    break;
                case "cpp", "c++":
                    preCompileCpp(code, workDir);
                    break;
                default:
                    // No pre-compilation needed
                    break;
            }
        } catch (Exception e) {
            throw new CompilationException("Pre-compilation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Pre-compile Java code
     */
    private void preCompileJava(String code, Path workDir) throws Exception {
        String className = extractJavaClassName(code);
        if (className == null) {
            throw new CompilationException("Could not determine Java class name");
        }
        
        Path javaFile = workDir.resolve(className + ".java");
        Files.write(javaFile, code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        ProcessBuilder compileBuilder = new ProcessBuilder("javac", "-encoding", "UTF-8", javaFile.toString());
        compileBuilder.directory(workDir.toFile());
        Process compileProcess = compileBuilder.start();
        
        boolean finished = compileProcess.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
        if (!finished) {
            compileProcess.destroyForcibly();
            throw new CompilationException("Java compilation timeout");
        }
        
        if (compileProcess.exitValue() != 0) {
            String errorMessage = readErrorStream(compileProcess);
            throw new CompilationException("Java compilation failed: " + errorMessage);
        }
    }
    
    /**
     * Pre-compile C code
     */
    private void preCompileC(String code, Path workDir) throws Exception {
        Path cFile = workDir.resolve("solution.c");
        Files.write(cFile, code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        Path executable = workDir.resolve("solution.exe");
        String[] compilerCommands = getCCompilerCommands();
        
        for (String compilerCmd : compilerCommands) {
            try {
                ProcessBuilder compileBuilder = createGccCompileCommand(compilerCmd, executable.toString(), cFile.toString(), workDir);
                Process compileProcess = compileBuilder.start();
                
                boolean finished = compileProcess.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
                if (!finished) {
                    compileProcess.destroyForcibly();
                    throw new CompilationException("C compilation timeout");
                }
                
                if (compileProcess.exitValue() == 0) {
                    return; // Compilation successful
                }
            } catch (Exception e) {
                log.debug("Failed with compiler {}: {}", compilerCmd, e.getMessage());
            }
        }
        
        throw new CompilationException("C compilation failed with all available compilers");
    }
    
    /**
     * Pre-compile C++ code
     */
    private void preCompileCpp(String code, Path workDir) throws Exception {
        Path cppFile = workDir.resolve("solution.cpp");
        Files.write(cppFile, code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        Path executable = workDir.resolve("solution.exe");
        
        ProcessBuilder compileBuilder = new ProcessBuilder("g++", "-std=c++17", "-O2", "-o", executable.toString(), cppFile.toString());
        compileBuilder.directory(workDir.toFile());
        Process compileProcess = compileBuilder.start();
        
        boolean finished = compileProcess.waitFor(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
        if (!finished) {
            compileProcess.destroyForcibly();
            throw new CompilationException("C++ compilation timeout");
        }
        
        if (compileProcess.exitValue() != 0) {
            String errorMessage = readErrorStream(compileProcess);
            throw new CompilationException("C++ compilation failed: " + errorMessage);
        }
    }
    
    /**
     * Create a failed test result
     */
    private TestResultResponse createFailedTestResult(TestCase testCase, String errorMessage) {
        TestResultResponse result = new TestResultResponse();
        result.setTestCaseId(testCase.getId());
        result.setInput(testCase.getInput());
        result.setExpectedOutput(testCase.getExpectedOutput());
        result.setActualOutput("");
        result.setPassed(false);
        result.setExecutionTime(0L);
        result.setMemoryUsed(0L);
        result.setErrorMessage(truncateOutput(errorMessage));
        result.setWeight(testCase.getWeight());
        result.setHidden(testCase.getIsHidden());
        
        return result;
    }
    
    /**
     * Truncate output if it's too long
     */
    private String truncateOutput(String output) {
        if (output == null) return null;
        if (output.length() <= MAX_OUTPUT_LENGTH) return output;
        
        return output.substring(0, MAX_OUTPUT_LENGTH) + "\n... (output truncated)";
    }
    
    /**
     * Custom exception for compilation errors
     */
    public static class CompilationException extends Exception {
        public CompilationException(String message) {
            super(message);
        }
        
        public CompilationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
