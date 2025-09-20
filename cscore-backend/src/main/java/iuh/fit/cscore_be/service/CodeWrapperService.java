package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to wrap student function implementations into complete executable programs
 * Enhanced with Universal Wrapper System for intelligent, configurable code wrapping
 * This solves the issue where students submit only function code but the system expects complete programs
 */
@Service
@Slf4j
public class CodeWrapperService {

    @Autowired
    private EnhancedCodeWrapperService enhancedWrapperService;

    /**
     * Wrap student function code into a complete executable program
     * Enhanced with Universal Wrapper System for intelligent code analysis
     * @param studentCode The function code submitted by the student
     * @param question The question containing metadata about the expected function
     * @param language The programming language
     * @return Complete executable program
     */
    public String wrapFunctionCode(String studentCode, Question question, String language) {
        return wrapFunctionCode(studentCode, question, language, null);
    }

    /**
     * Enhanced wrapper method with test case analysis
     * @param studentCode The function code submitted by the student
     * @param question The question containing metadata about the expected function
     * @param language The programming language
     * @param testCases Test cases for pattern analysis (optional)
     * @return Complete executable program
     */
    public String wrapFunctionCode(String studentCode, Question question, String language, List<TestCase> testCases) {
        try {
            // First, try the enhanced universal wrapper system
            log.info("Attempting enhanced wrapper generation for language: {}", language);
            String enhancedResult = enhancedWrapperService.wrapFunctionCode(studentCode, question, language, testCases);
            
            if (enhancedResult != null && !enhancedResult.equals(studentCode)) {
                log.info("Successfully generated enhanced wrapper");
                return enhancedResult;
            }
        } catch (Exception e) {
            log.warn("Enhanced wrapper generation failed, falling back to legacy system: {}", e.getMessage());
        }
        
        // Fallback to legacy system
        log.info("Using legacy wrapper system as fallback");
        return wrapFunctionCodeLegacy(studentCode, question, language);
    }

    /**
     * Legacy wrapper method (preserved for backward compatibility)
     * @param studentCode The function code submitted by the student
     * @param question The question containing metadata about the expected function
     * @param language The programming language
     * @return Complete executable program
     */
    private String wrapFunctionCodeLegacy(String studentCode, Question question, String language) {
        // Check if the code already has a main function
        if (hasMainFunction(studentCode, language)) {
            log.info("Student code already has main function, using as-is");
            return studentCode;
        }
        
        // Extract function information from student code or use question metadata
        FunctionInfo functionInfo = extractFunctionInfo(studentCode, question, language);
        
        if (functionInfo == null || functionInfo.functionName == null) {
            log.warn("Could not extract function information for language {}, returning original code. Question function name: {}, signature: {}", 
                     language, 
                     question != null ? question.getFunctionName() : "null",
                     question != null ? question.getFunctionSignature() : "null");
            return studentCode;
        }
        
        log.debug("Extracted function info - Name: {}, Parameters: {}", functionInfo.functionName, functionInfo.parameters);
        
        // Generate wrapper code based on language
        switch (language.toLowerCase()) {
            case "c":
                return wrapCFunction(studentCode, functionInfo);
            case "cpp":
            case "c++":
                return wrapCppFunction(studentCode, functionInfo);
            case "java":
                return wrapJavaFunction(studentCode, functionInfo);
            case "python":
                return wrapPythonFunction(studentCode, functionInfo);
            default:
                log.warn("Unsupported language for code wrapping: {}", language);
                return studentCode;
        }
    }

    /**
     * Check if the code already has a main function
     */
    private boolean hasMainFunction(String code, String language) {
        switch (language.toLowerCase()) {
            case "c":
            case "cpp":
            case "c++":
                // Look for main function in C/C++
                Pattern cMainPattern = Pattern.compile("int\\s+main\\s*\\([^)]*\\)\\s*\\{", Pattern.CASE_INSENSITIVE);
                return cMainPattern.matcher(code).find();
            case "java":
                // Look for main method in Java
                Pattern javaMainPattern = Pattern.compile("public\\s+static\\s+void\\s+main\\s*\\(", Pattern.CASE_INSENSITIVE);
                return javaMainPattern.matcher(code).find();
            case "python":
                // Look for if __name__ == "__main__" pattern - this indicates a complete program
                // Just having print() or input() doesn't mean it's a complete program
                return code.contains("if __name__ == \"__main__\"") || 
                       code.contains("if __name__=='__main__'") ||
                       code.contains("if __name__ == '__main__'") ||
                       code.contains("if __name__=='__main__'");
            default:
                return false;
        }
    }

    /**
     * Extract function information from student code or question metadata
     */
    private FunctionInfo extractFunctionInfo(String studentCode, Question question, String language) {
        FunctionInfo info = new FunctionInfo();
        
        // Try to extract from question metadata first
        if (question.getFunctionName() != null && !question.getFunctionName().trim().isEmpty()) {
            info.functionName = question.getFunctionName().trim();
            log.info("Using function name from question metadata: {}", info.functionName);
        }
        
        if (question.getFunctionSignature() != null && !question.getFunctionSignature().trim().isEmpty()) {
            info.functionSignature = question.getFunctionSignature().trim();
            log.info("Using function signature from question metadata: {}", info.functionSignature);
        }
        
        // If metadata is not available, try to extract from student code
        if (info.functionName == null) {
            info = extractFromStudentCode(studentCode, language);
        }
        
        return info;
    }

    /**
     * Extract function information from student code
     */
    private FunctionInfo extractFromStudentCode(String code, String language) {
        FunctionInfo info = new FunctionInfo();
        
        switch (language.toLowerCase()) {
            case "c":
            case "cpp":
            case "c++":
                // Pattern to match C/C++ function definition - handle const keywords and pointer types
                Pattern cPattern = Pattern.compile(
                    "((?:const\\s+)?\\w+(?:\\s*\\*?)*)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{", 
                    Pattern.MULTILINE | Pattern.DOTALL
                );
                Matcher cMatcher = cPattern.matcher(code);
                if (cMatcher.find()) {
                    info.returnType = cMatcher.group(1).trim();
                    info.functionName = cMatcher.group(2).trim();
                    info.parameters = cMatcher.group(3).trim();
                    info.functionSignature = info.returnType + " " + info.functionName + "(" + info.parameters + ")";
                    log.debug("Extracted C function: {} with parameters: {}", info.functionName, info.parameters);
                }
                break;
            case "java":
                // Pattern to match Java method definition
                Pattern javaPattern = Pattern.compile(
                    "(public|private|protected)?\\s*(static)?\\s*(\\w+(?:<[^>]+>)?)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{",
                    Pattern.MULTILINE
                );
                Matcher javaMatcher = javaPattern.matcher(code);
                if (javaMatcher.find()) {
                    info.returnType = javaMatcher.group(3).trim();
                    info.functionName = javaMatcher.group(4).trim();
                    info.parameters = javaMatcher.group(5).trim();
                }
                break;
            case "python":
                // Pattern to match Python function definition
                Pattern pythonPattern = Pattern.compile("def\\s+(\\w+)\\s*\\(([^)]*)\\):", Pattern.MULTILINE);
                Matcher pythonMatcher = pythonPattern.matcher(code);
                if (pythonMatcher.find()) {
                    info.functionName = pythonMatcher.group(1).trim();
                    info.parameters = pythonMatcher.group(2).trim();
                }
                break;
        }
        
        return info.functionName != null ? info : null;
    }

    /**
     * Wrap C function with main function and I/O handling
     * UPDATED: Uses Enhanced Universal Wrapper System instead of hardcoded logic
     */
    private String wrapCFunction(String functionCode, FunctionInfo info) {
        log.info("Legacy C wrapper: Attempting to use Enhanced system for function: {}", 
                 info != null ? info.functionName : "unknown");
        
        try {
            // Try to create a basic question object from FunctionInfo
            Question tempQuestion = createQuestionFromFunctionInfo(info);
            
            // Attempt to use Enhanced system
            String enhancedResult = enhancedWrapperService.wrapFunctionCode(
                functionCode, tempQuestion, "c", null);
            
            if (enhancedResult != null && !enhancedResult.equals(functionCode)) {
                log.info("Successfully generated C wrapper using Enhanced system");
                return enhancedResult;
            }
        } catch (Exception e) {
            log.warn("Enhanced C wrapper failed in legacy fallback: {}", e.getMessage());
        }
        
        // Ultimate fallback: Basic C wrapper without hardcoded parsing
        log.warn("Using basic C wrapper as ultimate fallback");
        return generateBasicCWrapper(functionCode, info);
    }
    
    /**
     * Generate basic C wrapper using UniversalWrapperService as fallback
     */
    private String generateBasicCWrapper(String functionCode, FunctionInfo info) {
        log.warn("Basic C wrapper fallback - attempting UniversalWrapperService");
        
        try {
            // Use UniversalWrapperService as final fallback
            UniversalWrapperService universalService = new UniversalWrapperService();
            UniversalWrapperService.FunctionSignature signature = new UniversalWrapperService.FunctionSignature();
            
            if (info != null) {
                signature.setFunctionName(info.functionName);
                signature.setParameters(info.parameters);
                signature.setReturnType(info.returnType);
                signature.setLanguage("c");
            }
            
            String result = universalService.generateUniversalWrapper(functionCode, signature, null);
            if (result != null && !result.equals(functionCode)) {
                log.info("UniversalWrapperService generated C fallback successfully");
                return result;
            }
        } catch (Exception e) {
            log.error("UniversalWrapperService C fallback failed: {}", e.getMessage());
        }
        
        // Return original code if all systems fail
        log.error("All wrapper systems failed for C - returning original code");
        return functionCode;
    }

    /**
     * Wrap C++ function using Enhanced Universal Wrapper System
     */
    private String wrapCppFunction(String functionCode, FunctionInfo info) {
        log.info("Legacy C++ wrapper: Attempting to use Enhanced system for function: {}", 
                 info != null ? info.functionName : "unknown");
        
        try {
            // Try to create a basic question object from FunctionInfo
            Question tempQuestion = createQuestionFromFunctionInfo(info);
            
            // Attempt to use Enhanced system first
            String enhancedResult = enhancedWrapperService.wrapFunctionCode(
                functionCode, tempQuestion, "cpp", null);
            
            if (enhancedResult != null && !enhancedResult.equals(functionCode)) {
                log.info("Successfully generated C++ wrapper using Enhanced system");
                return enhancedResult;
            }
        } catch (Exception e) {
            log.warn("Enhanced C++ wrapper failed in legacy fallback: {}", e.getMessage());
        }
        
        // Ultimate fallback: Use UniversalWrapperService
        log.warn("Using UniversalWrapperService as C++ ultimate fallback");
        return generateUniversalCppWrapper(functionCode, info);
    }
    
    /**
     * Generate C++ wrapper using UniversalWrapperService as fallback
     */
    private String generateUniversalCppWrapper(String functionCode, FunctionInfo info) {
        try {
            // Use UniversalWrapperService as final fallback
            UniversalWrapperService universalService = new UniversalWrapperService();
            UniversalWrapperService.FunctionSignature signature = new UniversalWrapperService.FunctionSignature();
            
            if (info != null) {
                signature.setFunctionName(info.functionName);
                signature.setParameters(info.parameters);
                signature.setReturnType(info.returnType);
                signature.setLanguage("cpp");
            }
            
            String result = universalService.generateUniversalWrapper(functionCode, signature, null);
            if (result != null && !result.equals(functionCode)) {
                log.info("UniversalWrapperService generated C++ fallback successfully");
                return result;
            }
        } catch (Exception e) {
            log.error("UniversalWrapperService C++ fallback failed: {}", e.getMessage());
        }
        
        // Return original code if all systems fail
        log.error("All wrapper systems failed for C++ - returning original code");
        return functionCode;
    }

    /**
     * Wrap Java function with main method
     * UPDATED: Uses Enhanced Universal Wrapper System instead of hardcoded logic
     */
    private String wrapJavaFunction(String functionCode, FunctionInfo info) {
        log.info("Legacy Java wrapper: Attempting to use Enhanced system for function: {}", 
                 info != null ? info.functionName : "unknown");
        
        try {
            // Try to create a basic question object from FunctionInfo
            Question tempQuestion = createQuestionFromFunctionInfo(info);
            
            // Attempt to use Enhanced system
            String enhancedResult = enhancedWrapperService.wrapFunctionCode(
                functionCode, tempQuestion, "java", null);
            
            if (enhancedResult != null && !enhancedResult.equals(functionCode)) {
                log.info("Successfully generated Java wrapper using Enhanced system");
                return enhancedResult;
            }
        } catch (Exception e) {
            log.warn("Enhanced Java wrapper failed in legacy fallback: {}", e.getMessage());
        }
        
        // Ultimate fallback: Basic Java wrapper without hardcoded parsing
        log.warn("Using basic Java wrapper as ultimate fallback");
        return generateBasicJavaWrapper(functionCode, info);
    }
    
    /**
     * Generate basic Java wrapper using UniversalWrapperService as fallback
     */
    private String generateBasicJavaWrapper(String functionCode, FunctionInfo info) {
        log.warn("Basic Java wrapper fallback - attempting UniversalWrapperService");
        
        try {
            // Use UniversalWrapperService as final fallback
            UniversalWrapperService universalService = new UniversalWrapperService();
            UniversalWrapperService.FunctionSignature signature = new UniversalWrapperService.FunctionSignature();
            
            if (info != null) {
                signature.setFunctionName(info.functionName);
                signature.setParameters(info.parameters);
                signature.setReturnType(info.returnType);
                signature.setLanguage("java");
            }
            
            String result = universalService.generateUniversalWrapper(functionCode, signature, null);
            if (result != null && !result.equals(functionCode)) {
                log.info("UniversalWrapperService generated Java fallback successfully");
                return result;
            }
        } catch (Exception e) {
            log.error("UniversalWrapperService Java fallback failed: {}", e.getMessage());
        }
        
        // Return original code if all systems fail
        log.error("All wrapper systems failed for Java - returning original code");
        return functionCode;
    }

    /**
     * Wrap Python function with main execution
     * UPDATED: Uses Enhanced Universal Wrapper System instead of hardcoded logic
     */
    private String wrapPythonFunction(String functionCode, FunctionInfo info) {
        log.info("Legacy Python wrapper: Attempting to use Enhanced system for function: {}", 
                 info != null ? info.functionName : "unknown");
        
        try {
            // Try to create a basic question object from FunctionInfo
            Question tempQuestion = createQuestionFromFunctionInfo(info);
            
            // Attempt to use Enhanced system
            String enhancedResult = enhancedWrapperService.wrapFunctionCode(
                functionCode, tempQuestion, "python", null);
            
            if (enhancedResult != null && !enhancedResult.equals(functionCode)) {
                log.info("Successfully generated Python wrapper using Enhanced system");
                return enhancedResult;
            }
        } catch (Exception e) {
            log.warn("Enhanced Python wrapper failed in legacy fallback: {}", e.getMessage());
        }
        
        // Ultimate fallback: Basic wrapper without hardcoded parsing
        log.warn("Using basic Python wrapper as ultimate fallback");
        return generateBasicPythonWrapper(functionCode, info);
    }
    
    /**
     * Create a temporary Question object from FunctionInfo for Enhanced system
     */
    private Question createQuestionFromFunctionInfo(FunctionInfo info) {
        Question tempQuestion = new Question();
        if (info != null) {
            tempQuestion.setFunctionName(info.functionName);
            tempQuestion.setFunctionSignature(info.functionSignature);
        }
        return tempQuestion;
    }
    
    /**
     * Generate basic Python wrapper using UniversalWrapperService as fallback
     * This is the ultimate fallback when all else fails
     */
    private String generateBasicPythonWrapper(String functionCode, FunctionInfo info) {
        log.warn("Basic Python wrapper fallback - attempting UniversalWrapperService");
        
        try {
            // Use UniversalWrapperService as final fallback
            UniversalWrapperService universalService = new UniversalWrapperService();
            UniversalWrapperService.FunctionSignature signature = new UniversalWrapperService.FunctionSignature();
            
            if (info != null) {
                signature.setFunctionName(info.functionName);
                signature.setParameters(info.parameters);
                signature.setReturnType(info.returnType);
                signature.setLanguage("python");
            }
            
            String result = universalService.generateUniversalWrapper(functionCode, signature, null);
            if (result != null && !result.equals(functionCode)) {
                log.info("UniversalWrapperService generated Python fallback successfully");
                return result;
            }
        } catch (Exception e) {
            log.error("UniversalWrapperService Python fallback failed: {}", e.getMessage());
        }
        
        // Return original code if all systems fail
        log.error("All wrapper systems failed for Python - returning original code");
        return functionCode;
    }

    // NOTE: Hardcode helper methods removed - pattern detection now handled by Enhanced system

    /**
     * Helper class to store function information
     */
    private static class FunctionInfo {
        String functionName;
        String returnType;
        String parameters;
        String functionSignature;
    }
}