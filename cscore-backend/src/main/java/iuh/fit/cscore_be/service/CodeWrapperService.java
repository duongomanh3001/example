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
     */
    private String wrapCFunction(String functionCode, FunctionInfo info) {
        StringBuilder wrapper = new StringBuilder();
        
        wrapper.append("#include <stdio.h>\n");
        wrapper.append("#include <string.h>\n");
        wrapper.append("#include <stdlib.h>\n\n");
        
        // Add the student's function code
        wrapper.append(functionCode).append("\n\n");
        
        // Add main function with improved input parsing
        wrapper.append("int main() {\n");
        wrapper.append("    char input[1000];\n");
        wrapper.append("    if (fgets(input, sizeof(input), stdin) == NULL) {\n");
        wrapper.append("        return 1;\n");
        wrapper.append("    }\n\n");
        
        // Parse input based on function signature
        log.debug("Checking if function '{}' with parameters '{}' is a string-char function", info.functionName, info.parameters);
        if (isStringCharFunction(info.parameters) || isCountCharacterFunction(info.functionName)) {
            // Function expects string and character (like countCharacter)
            wrapper.append("    // Remove newline from input\n");
            wrapper.append("    int len = strlen(input);\n");
            wrapper.append("    if (len > 0 && input[len-1] == '\\n') {\n");
            wrapper.append("        input[len-1] = '\\0';\n");
            wrapper.append("        len--;\n");
            wrapper.append("    }\n\n");
            
            wrapper.append("    // Parse input: handle various formats like \\\"Hello l\\\", etc.\n");
            wrapper.append("    char str[500] = {0};\n");
            wrapper.append("    char key = '\\0';\n");
            wrapper.append("    \n");
            wrapper.append("    // Find the last space to separate string and character\n");
            wrapper.append("    int last_space = -1;\n");
            wrapper.append("    for (int k = len - 1; k >= 0; k--) {\n");
            wrapper.append("        if (input[k] == ' ') {\n");
            wrapper.append("            last_space = k;\n");
            wrapper.append("            break;\n");
            wrapper.append("        }\n");
            wrapper.append("    }\n");
            wrapper.append("    \n");
            wrapper.append("    if (last_space == -1 || last_space >= len - 1) {\n");
            wrapper.append("        // Handle single character input (edge case)\n");
            wrapper.append("        if (len == 1) {\n");
            wrapper.append("            str[0] = '\\0'; // empty string\n");
            wrapper.append("            key = input[0];\n");
            wrapper.append("        } else {\n");
            wrapper.append("            // Invalid format, but try to handle gracefully\n");
            wrapper.append("            strcpy(str, input);\n");
            wrapper.append("            key = ' '; // default character\n");
            wrapper.append("        }\n");
            wrapper.append("    } else {\n");
            wrapper.append("        // Extract character part (after last space)\n");
            wrapper.append("        key = input[last_space + 1];\n");
            wrapper.append("        \n");
            wrapper.append("        // Extract string part (before last space)\n");
            wrapper.append("        char string_part[500];\n");
            wrapper.append("        strncpy(string_part, input, last_space);\n");
            wrapper.append("        string_part[last_space] = '\\0';\n");
            wrapper.append("        \n");
            wrapper.append("        // Handle different string formats:\n");
            wrapper.append("        // 1. \\\"Hello\\\" -> Hello\n");
            wrapper.append("        // 2. Empty strings and special cases\n");
            wrapper.append("        // 3. Simple strings without quotes\n");
            wrapper.append("        \n");
            wrapper.append("        int str_len = strlen(string_part);\n");
            wrapper.append("        \n");
            wrapper.append("        // Check for quoted strings\n");
            wrapper.append("        if (str_len >= 2 && string_part[0] == '\"' && string_part[str_len-1] == '\"') {\n");
            wrapper.append("            // Remove outer quotes\n");
            wrapper.append("            strncpy(str, string_part + 1, str_len - 2);\n");
            wrapper.append("            str[str_len - 2] = '\\0';\n");
            wrapper.append("        } else {\n");
            wrapper.append("            // No quotes, use as-is\n");
            wrapper.append("            strcpy(str, string_part);\n");
            wrapper.append("        }\n");
            wrapper.append("    }\n");
            wrapper.append("    \n");
            wrapper.append("    // Call function and print result\n");
            wrapper.append("    int result = ").append(info.functionName).append("(str, key);\n");
            wrapper.append("    printf(\"%d\", result);\n");
            wrapper.append("    \n");
            wrapper.append("    return 0;\n");
        } else {
            // Generic parameter parsing - try to parse based on common patterns
            wrapper.append("    // Generic parameter parsing\n");
            wrapper.append("    // TODO: Add specific parsing logic based on function signature\n");
            wrapper.append("    printf(\"Error: Unsupported parameter types\");\n");
            wrapper.append("    return 1;\n");
        }
        
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
        
        // Similar parsing logic as improved C version
        if (isStringCharFunction(info.parameters) || isCountCharacterFunction(info.functionName)) {
            wrapper.append("    int len = strlen(input);\n");
            wrapper.append("    char str[500] = {0};\n");
            wrapper.append("    char key = '\\0';\n");
            wrapper.append("    \n");
            wrapper.append("    // Find the last space to separate string and character\n");
            wrapper.append("    int last_space = -1;\n");
            wrapper.append("    for (int i = len - 1; i >= 0; i--) {\n");
            wrapper.append("        if (input[i] == ' ') {\n");
            wrapper.append("            last_space = i;\n");
            wrapper.append("            break;\n");
            wrapper.append("        }\n");
            wrapper.append("    }\n");
            wrapper.append("    \n");
            wrapper.append("    if (last_space == -1 || last_space >= len - 1) {\n");
            wrapper.append("        if (len == 1) {\n");
            wrapper.append("            str[0] = '\\0';\n");
            wrapper.append("            key = input[0];\n");
            wrapper.append("        } else {\n");
            wrapper.append("            strcpy(str, input);\n");
            wrapper.append("            key = ' ';\n");
            wrapper.append("        }\n");
            wrapper.append("    } else {\n");
            wrapper.append("        key = input[last_space + 1];\n");
            wrapper.append("        \n");
            wrapper.append("        char string_part[500];\n");
            wrapper.append("        strncpy(string_part, input, last_space);\n");
            wrapper.append("        string_part[last_space] = '\\0';\n");
            wrapper.append("        \n");
            wrapper.append("        int str_len = strlen(string_part);\n");
            wrapper.append("        \n");
            wrapper.append("        if (str_len >= 2 && string_part[0] == '\"' && string_part[str_len-1] == '\"') {\n");
            wrapper.append("            strncpy(str, string_part + 1, str_len - 2);\n");
            wrapper.append("            str[str_len - 2] = '\\0';\n");
            wrapper.append("            \n");
            wrapper.append("            if (strcmp(str, \\\"\\\\\\\"\\\\\\\"\\\") == 0) {\n");
            wrapper.append("                str[0] = '\\0';\n");
            wrapper.append("            }\n");
            wrapper.append("        } else if (str_len == 4 && strcmp(string_part, \\\"\\\\\\\"\\\\\\\"\\\") == 0) {\n");
            wrapper.append("            str[0] = '\\0';\n");
            wrapper.append("        } else {\n");
            wrapper.append("            strcpy(str, string_part);\n");
            wrapper.append("        }\n");
            wrapper.append("    }\n");
            wrapper.append("    \n");
            wrapper.append("    int result = ").append(info.functionName).append("(str, key);\n");
            wrapper.append("    cout << result;\n");
        } else {
            wrapper.append("    cout << \\\"Error: Unsupported parameter types\\\";\n");
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
        wrapper.append("        if (!scanner.hasNextLine()) {\n");
        wrapper.append("            System.exit(1);\n");
        wrapper.append("        }\n");
        wrapper.append("        String input = scanner.nextLine().trim();\n");
        wrapper.append("        \n");
        
        // Check if this is a string + character function
        if (info != null && (isStringCharFunction(info.parameters) || isCountCharacterFunction(info.functionName))) {
            wrapper.append("        try {\n");
            wrapper.append("            String stringValue = \"\";\n");
            wrapper.append("            char charValue = ' ';\n");
            wrapper.append("            \n");
            wrapper.append("            if (input.isEmpty()) {\n");
            wrapper.append("                stringValue = \"\";\n");
            wrapper.append("                charValue = ' ';\n");
            wrapper.append("            } else {\n");
            wrapper.append("                // Find the last space to separate string and character\n");
            wrapper.append("                int lastSpace = input.lastIndexOf(' ');\n");
            wrapper.append("                \n");
            wrapper.append("                if (lastSpace == -1 || lastSpace >= input.length() - 1) {\n");
            wrapper.append("                    // Handle single character input\n");
            wrapper.append("                    if (input.length() == 1) {\n");
            wrapper.append("                        stringValue = \"\";\n");
            wrapper.append("                        charValue = input.charAt(0);\n");
            wrapper.append("                    } else {\n");
            wrapper.append("                        stringValue = input;\n");
            wrapper.append("                        charValue = ' ';\n");
            wrapper.append("                    }\n");
            wrapper.append("                } else {\n");
            wrapper.append("                    // Extract character part (after last space)\n");
            wrapper.append("                    charValue = input.charAt(lastSpace + 1);\n");
            wrapper.append("                    \n");
            wrapper.append("                    // Extract string part (before last space)\n");
            wrapper.append("                    String stringPart = input.substring(0, lastSpace).trim();\n");
            wrapper.append("                    \n");
            wrapper.append("                    // Handle different string formats:\n");
            wrapper.append("                    if (stringPart.equals(\\\"\\\\\\\"\\\\\\\"\\\") || stringPart.equals(\\\"\\\"\\\"\\\")) {\n");
            wrapper.append("                        // Handle empty string cases\n");
            wrapper.append("                        stringValue = \"\";\n");
            wrapper.append("                    } else if (stringPart.length() >= 2 && stringPart.startsWith(\\\"\\\"\\\") && stringPart.endsWith(\\\"\\\"\\\")) {\n");
            wrapper.append("                        // Remove outer quotes\n");
            wrapper.append("                        stringValue = stringPart.substring(1, stringPart.length() - 1);\n");
            wrapper.append("                        \n");
            wrapper.append("                        // Handle escaped quotes\n");
            wrapper.append("                        if (stringValue.equals(\\\"\\\\\\\"\\\\\\\"\\\")) {\n");
            wrapper.append("                            stringValue = \"\";\n");
            wrapper.append("                        }\n");
            wrapper.append("                    } else {\n");
            wrapper.append("                        // No quotes, use as-is\n");
            wrapper.append("                        stringValue = stringPart;\n");
            wrapper.append("                    }\n");
            wrapper.append("                }\n");
            wrapper.append("            }\n");
            wrapper.append("            \n");
            wrapper.append("            // Call function and print result\n");
            wrapper.append("            Solution solution = new Solution();\n");
            wrapper.append("            int result = solution.").append(info.functionName).append("(stringValue, charValue);\n");
            wrapper.append("            System.out.print(result);\n");
            wrapper.append("            \n");
            wrapper.append("        } catch (Exception e) {\n");
            wrapper.append("            System.out.println(\\\"Error: \\\" + e.getMessage());\n");
            wrapper.append("            System.exit(1);\n");
            wrapper.append("        }\n");
        } else {
            // Generic parameter parsing
            wrapper.append("        try {\n");
            wrapper.append("            String[] params = input.split(\\\" \\\");\n");
            wrapper.append("            \n");
            wrapper.append("            // Convert parameters to appropriate types\n");
            wrapper.append("            // TODO: Add specific conversion logic based on function signature\n");
            wrapper.append("            \n");
            wrapper.append("            System.out.println(\\\"Error: Generic Java wrapper not fully implemented\\\");\n");
            wrapper.append("        } catch (Exception e) {\n");
            wrapper.append("            System.out.println(\\\"Error: \\\" + e.getMessage());\n");
            wrapper.append("            System.exit(1);\n");
            wrapper.append("        }\n");
        }
        
        wrapper.append("        scanner.close();\n");
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
        wrapper.append("    \n");
        wrapper.append("    # Read input from stdin\n");
        wrapper.append("    try:\n");
        wrapper.append("        input_line = input().strip()\n");
        wrapper.append("        \n");
        
        // Check if this is a string + character function
        if (info != null && isStringCharFunction(info.parameters)) {
            wrapper.append("        # Parse string and character from input (format: \"string char\")\n");
            wrapper.append("        if not input_line:\n");
            wrapper.append("            string_value = \"\"\n");
            wrapper.append("            char_value = ' '\n");
            wrapper.append("        else:\n");
            wrapper.append("            # Find last space to separate string and character\n");
            wrapper.append("            last_space = input_line.rfind(' ')\n");
            wrapper.append("            if last_space == -1:\n");
            wrapper.append("                string_value = \"\"\n");
            wrapper.append("                char_value = input_line[0] if input_line else ' '\n");
            wrapper.append("            else:\n");
            wrapper.append("                string_part = input_line[:last_space].strip()\n");
            wrapper.append("                char_part = input_line[last_space + 1:].strip()\n");
            wrapper.append("                \n");
            wrapper.append("                # Parse string part\n");
            wrapper.append("                if string_part == '\"\"' or string_part == '\\\\\"\\\\\"':\n");
            wrapper.append("                    string_value = \"\"\n");
            wrapper.append("                elif string_part.startswith('\"') and string_part.endswith('\"') and len(string_part) >= 2:\n");
            wrapper.append("                    string_value = string_part[1:-1]  # Remove quotes\n");
            wrapper.append("                else:\n");
            wrapper.append("                    string_value = string_part\n");
            wrapper.append("                \n");
            wrapper.append("                # Parse character part\n");
            wrapper.append("                char_value = char_part[0] if char_part else ' '\n");
            wrapper.append("        \n");
            wrapper.append("        # Call the function with parsed parameters\n");
            wrapper.append("        result = ").append(info.functionName).append("(string_value, char_value)\n");
        } else {
            // Generic parameter parsing for other function types
            wrapper.append("        # Generic parameter parsing\n");
            wrapper.append("        params = input_line.split()\n");
            wrapper.append("        \n");
            wrapper.append("        # Convert parameters to appropriate types\n");
            wrapper.append("        converted_params = []\n");
            wrapper.append("        for param in params:\n");
            wrapper.append("            try:\n");
            wrapper.append("                # Try to convert to int first\n");
            wrapper.append("                converted_params.append(int(param))\n");
            wrapper.append("            except ValueError:\n");
            wrapper.append("                try:\n");
            wrapper.append("                    # Then try float\n");
            wrapper.append("                    converted_params.append(float(param))\n");
            wrapper.append("                except ValueError:\n");
            wrapper.append("                    # Keep as string\n");
            wrapper.append("                    converted_params.append(param)\n");
            wrapper.append("        \n");
            wrapper.append("        # Call the function\n");
            wrapper.append("        result = ").append(info != null ? info.functionName : "main_function").append("(*converted_params)\n");
        }
        
        wrapper.append("        \n");
        wrapper.append("        # Print the result\n");
        wrapper.append("        print(result)\n");
        wrapper.append("        \n");
        wrapper.append("    except Exception as e:\n");
        wrapper.append("        print(f\"Error: {e}\")\n");
        wrapper.append("        sys.exit(1)\n");
        
        return wrapper.toString();
    }

    /**
     * Check if function parameters match string + character pattern
     */
    private boolean isStringCharFunction(String parameters) {
        if (parameters == null) return false;
        
        // Remove whitespace and normalize
        String normalized = parameters.replaceAll("\\s+", " ").toLowerCase().trim();
        
        // Common patterns for string + character functions
        return normalized.contains("char") && (
            normalized.contains("[]") ||
            normalized.contains("*") ||
            normalized.matches(".*const\\s+char\\s*\\*.*char.*") ||
            normalized.matches(".*char\\s*\\[\\s*\\].*char.*") ||
            normalized.matches(".*char\\s+\\w+\\s*\\[\\s*\\].*char\\s+\\w+.*")
        );
    }

    /**
     * Check if function name suggests it's a character counting function
     */
    private boolean isCountCharacterFunction(String functionName) {
        if (functionName == null) return false;
        
        String normalized = functionName.toLowerCase();
        return normalized.contains("count") && normalized.contains("char");
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