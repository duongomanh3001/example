package iuh.fit.cscore_be.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal Wrapper System - Intelligent, metadata-driven approach
 * Replaces hardcoded wrapper logic with flexible, configurable system
 */
@Service
@Slf4j
public class UniversalWrapperService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pre-defined parameter type patterns
    private static final Map<String, ParameterType> PARAMETER_PATTERNS = Map.of(
        "string_char", new ParameterType("string_char", Arrays.asList("string", "char"), "Parse string and character input"),
        "array_size", new ParameterType("array_size", Arrays.asList("array", "int"), "Parse array and size"),
        "matrix", new ParameterType("matrix", Arrays.asList("int[][]", "int", "int"), "Parse 2D matrix with dimensions"),
        "graph", new ParameterType("graph", Arrays.asList("int[][]", "int"), "Parse graph adjacency matrix"),
        "tree", new ParameterType("tree", Arrays.asList("TreeNode*"), "Parse tree structure"),
        "single_value", new ParameterType("single_value", Arrays.asList("int"), "Parse single integer"),
        "multiple_values", new ParameterType("multiple_values", Arrays.asList("int", "int"), "Parse multiple integers")
    );

    /**
     * Generate intelligent wrapper for any function type
     */
    public String generateUniversalWrapper(String studentCode, FunctionSignature signature, List<String> testInputs) {
        try {
            // 1. Analyze function signature
            ParameterAnalysis analysis = analyzeParameters(signature);
            
            // 2. Detect input patterns from test cases
            InputPattern inputPattern = detectInputPattern(testInputs);
            
            // 3. Generate appropriate wrapper
            return generateWrapper(studentCode, signature, analysis, inputPattern);
            
        } catch (Exception e) {
            log.error("Error generating universal wrapper", e);
            return generateFallbackWrapper(studentCode, signature);
        }
    }

    /**
     * Analyze function parameters to understand expected input format
     */
    private ParameterAnalysis analyzeParameters(FunctionSignature signature) {
        ParameterAnalysis analysis = new ParameterAnalysis();
        
        String params = signature.getParameters().toLowerCase();
        String functionName = signature.getFunctionName().toLowerCase();
        
        // Detect parameter patterns
        if (isStringCharPattern(params)) {
            analysis.setType("string_char");
            analysis.setParameterCount(2);
            analysis.setExpectedTypes(Arrays.asList("string", "char"));
        } else if (isArraySizePattern(params)) {
            analysis.setType("array_size");
            analysis.setParameterCount(2);
            analysis.setExpectedTypes(Arrays.asList("array", "int"));
        } else if (isMatrixPattern(params)) {
            analysis.setType("matrix");
            analysis.setParameterCount(3);
            analysis.setExpectedTypes(Arrays.asList("int[][]", "int", "int"));
        } else if (isSingleValuePattern(params)) {
            analysis.setType("single_value");
            analysis.setParameterCount(1);
            analysis.setExpectedTypes(Arrays.asList("int"));
        } else {
            // Default to multiple values pattern
            int paramCount = countParameters(params);
            analysis.setType("multiple_values");
            analysis.setParameterCount(paramCount);
            analysis.setExpectedTypes(Collections.nCopies(paramCount, "int"));
        }
        
        return analysis;
    }

    /**
     * Detect input pattern from test case samples
     */
    private InputPattern detectInputPattern(List<String> testInputs) {
        if (testInputs == null || testInputs.isEmpty()) {
            return new InputPattern("unknown", "single_line");
        }

        String firstInput = testInputs.get(0);
        InputPattern pattern = new InputPattern();
        
        // Analyze format
        if (firstInput.contains("\"") && firstInput.contains(" ") && !firstInput.contains(",")) {
            pattern.setType("string_char");
            pattern.setFormat("quoted_string_space_char");
        } else if (firstInput.contains(",")) {
            pattern.setType("comma_separated");
            pattern.setFormat("csv");
        } else if (firstInput.trim().split("\\s+").length > 1) {
            pattern.setType("space_separated");
            pattern.setFormat("space_delimited");
        } else {
            pattern.setType("single_value");
            pattern.setFormat("single_line");
        }
        
        return pattern;
    }

    /**
     * Generate wrapper based on analysis using template-based approach
     * NO HARDCODE - All templates loaded from configuration
     */
    private String generateWrapper(String studentCode, FunctionSignature signature, 
                                 ParameterAnalysis analysis, InputPattern inputPattern) {
        
        try {
            // Load universal wrapper templates from configuration
            Map<String, Object> templates = loadUniversalTemplates();
            
            if (templates == null || !templates.containsKey("python_universal")) {
                log.warn("Universal templates not found, using fallback");
                return generateMinimalFallback(studentCode, signature);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> pythonTemplate = (Map<String, Object>) templates.get("python_universal");
            String template = (String) pythonTemplate.get("template");
            
            if (template == null) {
                log.warn("Python universal template content not found");
                return generateMinimalFallback(studentCode, signature);
            }
            
            // Replace placeholders with actual values
            String result = template
                .replace("{STUDENT_CODE}", studentCode != null ? studentCode : "")
                .replace("{FUNCTION_NAME}", signature != null && signature.getFunctionName() != null ? signature.getFunctionName() : "main")
                .replace("{EXPECTED_TYPES}", analysis != null && analysis.getExpectedTypes() != null ? analysis.getExpectedTypes().toString() : "[]")
                .replace("{PATTERN_TYPE}", analysis != null ? analysis.getType() : "unknown")
                .replace("{INPUT_FORMAT}", inputPattern != null ? inputPattern.getFormat() : "single_line");
            
            log.info("Generated universal wrapper using template-based approach for pattern: {}", 
                     analysis != null ? analysis.getType() : "unknown");
            return result;
            
        } catch (Exception e) {
            log.error("Template-based universal wrapper generation failed: {}", e.getMessage());
            return generateMinimalFallback(studentCode, signature);
        }
    }
    
    /**
     * Load universal wrapper templates from configuration
     */
    private Map<String, Object> loadUniversalTemplates() {
        try {
            // Try to load from classpath resource first
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("universal-wrapper-templates.json");
            if (inputStream != null) {
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
                return objectMapper.readValue(inputStream, typeRef);
            }
            
            // Fallback: create basic template structure
            Map<String, Object> fallbackTemplates = new HashMap<>();
            Map<String, Object> pythonTemplate = new HashMap<>();
            pythonTemplate.put("template", createBasicUniversalTemplate());
            fallbackTemplates.put("python_universal", pythonTemplate);
            
            return fallbackTemplates;
            
        } catch (Exception e) {
            log.error("Failed to load universal templates: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a basic universal template for extreme fallback cases
     */
    private String createBasicUniversalTemplate() {
        return """
                import sys
                
                {STUDENT_CODE}
                
                def parse_input_universally(input_line, pattern_type):
                    \"\"\"Universal input parser - template-driven\"\"\"
                    if not input_line or not input_line.strip():
                        return []
                    
                    # Pattern-based parsing
                    if pattern_type == "string_char":
                        parts = input_line.rsplit(' ', 1)
                        if len(parts) == 2:
                            return [parts[0].strip('"'), parts[1][0] if parts[1] else ' ']
                        return ['', input_line[0] if input_line else ' ']
                    elif pattern_type == "single_value":
                        try:
                            return [int(input_line)]
                        except ValueError:
                            return [0]
                    else:
                        # Default: space-separated values
                        parts = input_line.split()
                        try:
                            return [int(p) for p in parts]
                        except ValueError:
                            return parts
                
                if __name__ == "__main__":
                    try:
                        input_line = input().strip()
                        pattern_type = "{PATTERN_TYPE}"
                        
                        parsed_args = parse_input_universally(input_line, pattern_type)
                        
                        # Dynamic function call
                        function_name = "{FUNCTION_NAME}"
                        if function_name and function_name != "main":
                            result = globals()[function_name](*parsed_args)
                            if result is not None:
                                print(result)
                        else:
                            print("No function name specified")
                            
                    except Exception as e:
                        print(f"Error: {e}")
                        sys.exit(1)
                """;
    }
    
    /**
     * Generate minimal fallback when all template systems fail
     */
    private String generateMinimalFallback(String studentCode, FunctionSignature signature) {
        String functionName = signature != null && signature.getFunctionName() != null ? 
                             signature.getFunctionName() : "main";
        
        return String.format("""
                import sys
                
                %s
                
                if __name__ == "__main__":
                    try:
                        input_line = input().strip()
                        # Minimal fallback - try to call function with input
                        if "%s" in globals():
                            result = %s(input_line)
                            if result is not None:
                                print(result)
                    except Exception as e:
                        print(f"Minimal fallback error: {e}")
                        sys.exit(1)
                """, 
                studentCode != null ? studentCode : "# No student code", 
                functionName, 
                functionName);
    }

    // NOTE: All hardcode parser generator methods removed - now using template-based approach

    // Pattern detection methods
    private boolean isStringCharPattern(String params) {
        return params.contains("char") && (params.contains("[]") || params.contains("*"));
    }

    private boolean isArraySizePattern(String params) {
        return params.contains("[]") && params.contains("int") && 
               params.split(",").length == 2;
    }

    private boolean isMatrixPattern(String params) {
        return params.contains("[][]") || 
               (params.contains("int") && params.split(",").length >= 3);
    }

    private boolean isSingleValuePattern(String params) {
        return params.split(",").length == 1 && 
               (params.contains("int") || params.contains("double"));
    }

    private int countParameters(String params) {
        if (params.trim().isEmpty()) return 0;
        return params.split(",").length;
    }

    private String generateFallbackWrapper(String studentCode, FunctionSignature signature) {
        // Simple fallback wrapper
        return "import sys\n\n" + 
               studentCode + "\n\n" +
               "if __name__ == '__main__':\n" +
               "    try:\n" +
               "        input_line = input().strip()\n" +
               "        print(f\"Fallback wrapper for {signature.getFunctionName()}\")\n" +
               "    except Exception as e:\n" +
               "        print(f\"Error: {e}\")\n";
    }

    // Data classes
    @Data
    public static class FunctionSignature {
        private String functionName;
        private String returnType;
        private String parameters;
        private String language;
    }

    @Data
    public static class ParameterAnalysis {
        private String type;
        private int parameterCount;
        private List<String> expectedTypes;
    }

    @Data
    public static class InputPattern {
        private String type;
        private String format;
        
        public InputPattern() {}
        
        public InputPattern(String type, String format) {
            this.type = type;
            this.format = format;
        }
    }

    @Data
    public static class ParameterType {
        private String name;
        private List<String> expectedTypes;
        private String description;
        
        public ParameterType(String name, List<String> expectedTypes, String description) {
            this.name = name;
            this.expectedTypes = expectedTypes;
            this.description = description;
        }
    }
}