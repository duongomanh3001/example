package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to wrap student function implementations into complete executable programs
 * This solves the issue where students submit only function code but the system expects complete programs
 */
@Service
@Slf4j
public class CodeWrapperService {

    /**
     * Wrap student function code into a complete executable program
     * @param studentCode The function code submitted by the student
     * @param question The question containing metadata about the expected function
     * @param language The programming language
     * @return Complete executable program
     */
    public String wrapFunctionCode(String studentCode, Question question, String language) {
        // Check if the code already has a main function
        if (hasMainFunction(studentCode, language)) {
            log.info("Student code already has main function, using as-is");
            return studentCode;
        }
        
        // Extract function information from student code or use question metadata
        FunctionInfo functionInfo = extractFunctionInfo(studentCode, question, language);
        
        if (functionInfo == null) {
            log.warn("Could not extract function information, returning original code");
            return studentCode;
        }
        
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
                // Look for if __name__ == "__main__" or direct execution code
                return code.contains("if __name__") || 
                       code.contains("input(") || 
                       code.contains("print(") ||
                       code.matches(".*\\n\\s*[a-zA-Z_].*");
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
                // Pattern to match C/C++ function definition
                Pattern cPattern = Pattern.compile(
                    "(\\w+(?:\\s*\\*?)*)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{", 
                    Pattern.MULTILINE
                );
                Matcher cMatcher = cPattern.matcher(code);
                if (cMatcher.find()) {
                    info.returnType = cMatcher.group(1).trim();
                    info.functionName = cMatcher.group(2).trim();
                    info.parameters = cMatcher.group(3).trim();
                    info.functionSignature = info.returnType + " " + info.functionName + "(" + info.parameters + ")";
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
     */
    private String wrapCFunction(String functionCode, FunctionInfo info) {
        StringBuilder wrapper = new StringBuilder();
        
        wrapper.append("#include <stdio.h>\n");
        wrapper.append("#include <string.h>\n");
        wrapper.append("#include <stdlib.h>\n\n");
        
        // Add the student's function code
        wrapper.append(functionCode).append("\n\n");
        
        // Add main function with generic input parsing
        wrapper.append("int main() {\n");
        wrapper.append("    char input[1000];\n");
        wrapper.append("    if (fgets(input, sizeof(input), stdin) == NULL) {\n");
        wrapper.append("        return 1;\n");
        wrapper.append("    }\n\n");
        
        // Parse input based on function signature
        if (info.parameters.contains("char") && info.parameters.contains("[]")) {
            // Function expects string and character (like countCharacter)
            wrapper.append("    // Remove newline\n");
            wrapper.append("    int len = strlen(input);\n");
            wrapper.append("    if (len > 0 && input[len-1] == '\\n') {\n");
            wrapper.append("        input[len-1] = '\\0';\n");
            wrapper.append("        len--;\n");
            wrapper.append("    }\n\n");
            
            wrapper.append("    // Find last space to separate string and character\n");
            wrapper.append("    int last_space = -1;\n");
            wrapper.append("    for (int i = len - 1; i >= 0; i--) {\n");
            wrapper.append("        if (input[i] == ' ') {\n");
            wrapper.append("            last_space = i;\n");
            wrapper.append("            break;\n");
            wrapper.append("        }\n");
            wrapper.append("    }\n\n");
            
            wrapper.append("    if (last_space == -1) return 1;\n\n");
            
            wrapper.append("    // Get character parameter\n");
            wrapper.append("    char key = input[last_space + 1];\n");
            wrapper.append("    input[last_space] = '\\0';\n\n");
            
            wrapper.append("    // Parse string (handle quotes)\n");
            wrapper.append("    char str[500];\n");
            wrapper.append("    if (strcmp(input, \"\\\"\\\"\") == 0) {\n");
            wrapper.append("        str[0] = '\\0';\n");
            wrapper.append("    } else if (input[0] == '\"' && input[strlen(input)-1] == '\"') {\n");
            wrapper.append("        int str_len = strlen(input) - 2;\n");
            wrapper.append("        strncpy(str, input + 1, str_len);\n");
            wrapper.append("        str[str_len] = '\\0';\n");
            wrapper.append("    } else {\n");
            wrapper.append("        strcpy(str, input);\n");
            wrapper.append("    }\n\n");
            
            wrapper.append("    // Call function and print result\n");
            wrapper.append("    int result = ").append(info.functionName).append("(str, key);\n");
            wrapper.append("    printf(\"%d\", result);\n");
        } else {
            // Generic parameter parsing - try to parse based on common patterns
            wrapper.append("    // Generic parameter parsing\n");
            wrapper.append("    // TODO: Add specific parsing logic based on function signature\n");
            wrapper.append("    printf(\"Error: Unsupported parameter types\");\n");
        }
        
        wrapper.append("    return 0;\n");
        wrapper.append("}\n");
        
        return wrapper.toString();
    }

    /**
     * Wrap C++ function (similar to C but with C++ includes)
     */
    private String wrapCppFunction(String functionCode, FunctionInfo info) {
        StringBuilder wrapper = new StringBuilder();
        
        wrapper.append("#include <iostream>\n");
        wrapper.append("#include <string>\n");
        wrapper.append("#include <cstring>\n");
        wrapper.append("using namespace std;\n\n");
        
        // Add the student's function code
        wrapper.append(functionCode).append("\n\n");
        
        // Add main function similar to C version
        wrapper.append("int main() {\n");
        wrapper.append("    char input[1000];\n");
        wrapper.append("    if (!cin.getline(input, sizeof(input))) {\n");
        wrapper.append("        return 1;\n");
        wrapper.append("    }\n\n");
        
        // Similar parsing logic as C version
        if (info.parameters.contains("char") && info.parameters.contains("[]")) {
            wrapper.append("    int len = strlen(input);\n");
            wrapper.append("    int last_space = -1;\n");
            wrapper.append("    for (int i = len - 1; i >= 0; i--) {\n");
            wrapper.append("        if (input[i] == ' ') {\n");
            wrapper.append("            last_space = i;\n");
            wrapper.append("            break;\n");
            wrapper.append("        }\n");
            wrapper.append("    }\n\n");
            
            wrapper.append("    if (last_space == -1) return 1;\n\n");
            
            wrapper.append("    char key = input[last_space + 1];\n");
            wrapper.append("    input[last_space] = '\\0';\n\n");
            
            wrapper.append("    char str[500];\n");
            wrapper.append("    if (strcmp(input, \"\\\"\\\"\") == 0) {\n");
            wrapper.append("        str[0] = '\\0';\n");
            wrapper.append("    } else if (input[0] == '\"' && input[strlen(input)-1] == '\"') {\n");
            wrapper.append("        int str_len = strlen(input) - 2;\n");
            wrapper.append("        strncpy(str, input + 1, str_len);\n");
            wrapper.append("        str[str_len] = '\\0';\n");
            wrapper.append("    } else {\n");
            wrapper.append("        strcpy(str, input);\n");
            wrapper.append("    }\n\n");
            
            wrapper.append("    int result = ").append(info.functionName).append("(str, key);\n");
            wrapper.append("    cout << result;\n");
        } else {
            wrapper.append("    cout << \"Error: Unsupported parameter types\";\n");
        }
        
        wrapper.append("    return 0;\n");
        wrapper.append("}\n");
        
        return wrapper.toString();
    }

    /**
     * Wrap Java function with main method
     */
    private String wrapJavaFunction(String functionCode, FunctionInfo info) {
        StringBuilder wrapper = new StringBuilder();
        
        wrapper.append("import java.util.Scanner;\n\n");
        wrapper.append("public class Solution {\n\n");
        
        // Add the student's function code
        wrapper.append("    ").append(functionCode.replaceAll("\n", "\n    ")).append("\n\n");
        
        wrapper.append("    public static void main(String[] args) {\n");
        wrapper.append("        Scanner scanner = new Scanner(System.in);\n");
        wrapper.append("        String input = scanner.nextLine();\n");
        wrapper.append("        \n");
        wrapper.append("        // Parse input and call function\n");
        wrapper.append("        // TODO: Add parsing logic based on function signature\n");
        wrapper.append("        System.out.println(\"Error: Java wrapper not fully implemented\");\n");
        wrapper.append("    }\n");
        wrapper.append("}\n");
        
        return wrapper.toString();
    }

    /**
     * Wrap Python function with main execution
     */
    private String wrapPythonFunction(String functionCode, FunctionInfo info) {
        StringBuilder wrapper = new StringBuilder();
        
        // Add the student's function code
        wrapper.append(functionCode).append("\n\n");
        
        wrapper.append("if __name__ == \"__main__\":\n");
        wrapper.append("    import sys\n");
        wrapper.append("    input_line = input().strip()\n");
        wrapper.append("    \n");
        wrapper.append("    # Parse input and call function\n");
        wrapper.append("    # TODO: Add parsing logic based on function signature\n");
        wrapper.append("    print(\"Error: Python wrapper not fully implemented\")\n");
        
        return wrapper.toString();
    }

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