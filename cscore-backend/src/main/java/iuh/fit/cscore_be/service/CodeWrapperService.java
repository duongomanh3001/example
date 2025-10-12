package iuh.fit.cscore_be.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.TestCase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code Wrapper Service
 * Unified service providing intelligent code wrapping with:
 * - Template-driven approach using JSON templates
 * - Automatic function signature analysis
 * - Test case pattern detection
 * - Multi-language support
 * - Fallback strategies for edge cases
 */
@Service
@Slf4j
public class CodeWrapperService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, WrapperTemplate> wrapperTemplates = new HashMap<>();
    private Map<String, Map<String, WrapperTemplate>> languageTemplates = new HashMap<>();
    
    @PostConstruct
    public void initializeTemplates() {
        loadTemplatesFromResources();
    }

    /**
     * Main entry point - Wrap function code into complete executable program
     */
    public String wrapFunctionCode(String studentCode, Question question, String language) {
        return wrapFunctionCode(studentCode, question, language, null);
    }

    /**
     * Enhanced wrapper with test case analysis
     */
    public String wrapFunctionCode(String studentCode, Question question, String language, List<TestCase> testCases) {
        try {
            log.info("Wrapping function code for language: {}, function: {}", 
                     language, question != null ? question.getFunctionName() : "unknown");

            // 1. Check if already has main function
            if (hasMainFunction(studentCode, language)) {
                log.info("Code already has main function, using as-is");
                return studentCode;
            }
            
            // 2. Analyze function signature
            FunctionAnalysisResult signatureAnalysis = analyzeFunctionSignature(studentCode, question);
            
            // 3. Analyze test case patterns
            TestCaseAnalysis testCaseAnalysis = analyzeTestCases(testCases);
            
            // 4. Generate wrapper using templates
            return generateTemplateWrapper(studentCode, question, language, signatureAnalysis, testCaseAnalysis);
            
        } catch (Exception e) {
            log.error("Error in unified wrapper generation", e);
            return generateFallbackWrapper(studentCode, question, language);
        }
    }

    // ========== TEMPLATE-BASED WRAPPER GENERATION ==========
    
    private String generateTemplateWrapper(String studentCode, Question question, String language,
                                         FunctionAnalysisResult signatureAnalysis,
                                         TestCaseAnalysis testCaseAnalysis) {
        
        // Find best template
        WrapperTemplate template = findBestTemplate(language, testCaseAnalysis.getInputPattern(), signatureAnalysis);
        
        if (template != null) {
            return generateFromTemplate(template, studentCode, signatureAnalysis, testCaseAnalysis);
        } else {
            log.warn("No suitable template found for language: {}, pattern: {}", 
                     language, testCaseAnalysis.getInputPattern());
            return generateFallbackWrapper(studentCode, question, language);
        }
    }
    
    private WrapperTemplate findBestTemplate(String language, String pattern, FunctionAnalysisResult signatureAnalysis) {
        String languageKey = language.toLowerCase();
        
        // First, try to find universal template
        WrapperTemplate universalTemplate = wrapperTemplates.get(languageKey + "_universal");
        if (universalTemplate != null && universalTemplate.getPatterns().contains(pattern)) {
            log.info("Using universal template for {}", language);
            return universalTemplate;
        }
        
        // Then try specific pattern templates
        if (languageTemplates.containsKey(languageKey)) {
            Map<String, WrapperTemplate> langTemplates = languageTemplates.get(languageKey);
            WrapperTemplate specificTemplate = langTemplates.get(pattern);
            if (specificTemplate != null) {
                log.info("Using specific template for {} pattern: {}", language, pattern);
                return specificTemplate;
            }
        }
        
        // Fallback to any available template for the language
        if (languageTemplates.containsKey(languageKey)) {
            Map<String, WrapperTemplate> langTemplates = languageTemplates.get(languageKey);
            if (!langTemplates.isEmpty()) {
                WrapperTemplate fallbackTemplate = langTemplates.values().iterator().next();
                log.info("Using fallback template for {}", language);
                return fallbackTemplate;
            }
        }
        
        return null;
    }
    
    private String generateFromTemplate(WrapperTemplate template, String studentCode,
                                      FunctionAnalysisResult signatureAnalysis,
                                      TestCaseAnalysis testCaseAnalysis) {
        
        String templateContent = template.getTemplate();
        
        // Replace template placeholders
        templateContent = templateContent.replace("{STUDENT_CODE}", studentCode);
        templateContent = templateContent.replace("{FUNCTION_NAME}", 
                         signatureAnalysis.getFunctionName() != null ? signatureAnalysis.getFunctionName() : "main");
        templateContent = templateContent.replace("{PATTERN_TYPE}", testCaseAnalysis.getInputPattern());
        
        // Replace parameter types if available
        if (signatureAnalysis.getParameterTypes() != null && !signatureAnalysis.getParameterTypes().isEmpty()) {
            String expectedTypes = "[\"" + String.join("\", \"", signatureAnalysis.getParameterTypes()) + "\"]";
            templateContent = templateContent.replace("{EXPECTED_TYPES}", expectedTypes);
        } else {
            templateContent = templateContent.replace("{EXPECTED_TYPES}", "[]");
        }
        
        // Additional language-specific replacements
        templateContent = performLanguageSpecificReplacements(templateContent, signatureAnalysis);
        
        log.debug("Generated wrapper code from template");
        return templateContent;
    }
    
    private String performLanguageSpecificReplacements(String template, FunctionAnalysisResult analysis) {
        // Handle language-specific patterns
        if (analysis.getFunctionName() != null) {
            template = template.replace("{FUNCTION_CALL}", analysis.getFunctionName());
        }
        
        if (analysis.getReturnType() != null) {
            template = template.replace("{RETURN_TYPE}", analysis.getReturnType());
        }
        
        return template;
    }

    // ========== FUNCTION SIGNATURE ANALYSIS ==========
    
    private FunctionAnalysisResult analyzeFunctionSignature(String code, Question question) {
        FunctionAnalysisResult result = new FunctionAnalysisResult();
        
        // Try to extract from question metadata first
        if (question != null) {
            result.setFunctionName(question.getFunctionName());
            if (question.getFunctionSignature() != null) {
                parseSignatureFromString(question.getFunctionSignature(), result);
            }
        }
        
        // If no question metadata, try to extract from code
        if (result.getFunctionName() == null) {
            extractFunctionFromCode(code, result);
        }
        
        // Set defaults if nothing found
        if (result.getFunctionName() == null) {
            result.setFunctionName("main");
            result.setParameterTypes(Arrays.asList("string"));
            result.setReturnType("void");
        }
        
        return result;
    }
    
    private void parseSignatureFromString(String signature, FunctionAnalysisResult result) {
        try {
            // Parse function signature like "int add(int a, int b)"
            Pattern pattern = Pattern.compile("(\\w+)\\s+(\\w+)\\s*\\(([^)]*)\\)");
            Matcher matcher = pattern.matcher(signature.trim());
            
            if (matcher.find()) {
                result.setReturnType(matcher.group(1));
                result.setFunctionName(matcher.group(2));
                
                String params = matcher.group(3).trim();
                if (!params.isEmpty()) {
                    List<String> paramTypes = new ArrayList<>();
                    String[] paramParts = params.split(",");
                    for (String param : paramParts) {
                        String[] typeName = param.trim().split("\\s+");
                        if (typeName.length > 0) {
                            paramTypes.add(typeName[0]);
                        }
                    }
                    result.setParameterTypes(paramTypes);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse function signature: {}", signature, e);
        }
    }
    
    private void extractFunctionFromCode(String code, FunctionAnalysisResult result) {
        // Try to find function definitions in code
        List<Pattern> functionPatterns = Arrays.asList(
            Pattern.compile("def\\s+(\\w+)\\s*\\(([^)]*)\\):"),  // Python
            Pattern.compile("(\\w+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{"),  // C/C++/Java (simplified)
            Pattern.compile("function\\s+(\\w+)\\s*\\(([^)]*)\\)")  // JavaScript
        );
        
        for (Pattern pattern : functionPatterns) {
            Matcher matcher = pattern.matcher(code);
            if (matcher.find()) {
                result.setFunctionName(matcher.group(matcher.groupCount() - 1));
                break;
            }
        }
    }

    // ========== TEST CASE ANALYSIS ==========
    
    private TestCaseAnalysis analyzeTestCases(List<TestCase> testCases) {
        TestCaseAnalysis analysis = new TestCaseAnalysis();
        
        if (testCases == null || testCases.isEmpty()) {
            analysis.setInputPattern("single_value");
            analysis.setConfidence(0.5);
            return analysis;
        }
        
        // Analyze input patterns
        Map<String, Integer> patternCounts = new HashMap<>();
        
        for (TestCase testCase : testCases) {
            String input = testCase.getInput();
            String detectedPattern = detectInputPattern(input);
            patternCounts.put(detectedPattern, patternCounts.getOrDefault(detectedPattern, 0) + 1);
        }
        
        // Find most common pattern
        String mostCommonPattern = patternCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("single_value");
        
        analysis.setInputPattern(mostCommonPattern);
        analysis.setConfidence(patternCounts.get(mostCommonPattern) / (double) testCases.size());
        
        return analysis;
    }
    
    private String detectInputPattern(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "single_value";
        }
        
        String trimmed = input.trim();
        
        // Check for string + character pattern (e.g., "hello w")
        if (trimmed.matches(".*\".*\"\\s+\\w")) {
            return "string_char";
        }
        
        // Check for array pattern (numbers separated by spaces/commas)
        if (trimmed.matches("^[\\d\\s,.-]+$")) {
            String[] parts = trimmed.split("[\\s,]+");
            if (parts.length > 2) {
                return "array_size";
            } else if (parts.length == 1) {
                return "single_value";
            } else {
                return "multiple_values";
            }
        }
        
        // Check for matrix pattern (starts with dimensions)
        if (trimmed.matches("^\\d+\\s+\\d+\\s+[\\d\\s]+$")) {
            return "matrix";
        }
        
        // Default to single value
        return "single_value";
    }

    // ========== MAIN FUNCTION DETECTION ==========
    
    private boolean hasMainFunction(String code, String language) {
        if (code == null) return false;
        
        switch (language.toLowerCase()) {
            case "java":
                return code.contains("public static void main") || code.contains("static void main");
            case "c":
            case "cpp":
            case "c++":
                return code.contains("int main(") || code.contains("void main(");
            case "python":
                return code.contains("if __name__ == \"__main__\":") || 
                       code.contains("if __name__ == '__main__':");
            default:
                return false;
        }
    }

    // ========== FALLBACK WRAPPER GENERATION ==========
    
    private String generateFallbackWrapper(String studentCode, Question question, String language) {
        log.info("Generating fallback wrapper for language: {}", language);
        
        String functionName = question != null && question.getFunctionName() != null ? 
                             question.getFunctionName() : "main";
        
        switch (language.toLowerCase()) {
            case "python":
                return generatePythonFallback(studentCode, functionName);
            case "java":
                return generateJavaFallback(studentCode, functionName);
            case "c":
                return generateCFallback(studentCode, functionName);
            case "cpp":
            case "c++":
                return generateCppFallback(studentCode, functionName);
            default:
                log.warn("No fallback available for language: {}", language);
                return studentCode;
        }
    }
    
    private String generatePythonFallback(String studentCode, String functionName) {
        return studentCode + "\n\n" +
               "if __name__ == '__main__':\n" +
               "    try:\n" +
               "        input_line = input().strip()\n" +
               "        # Try to parse as numbers\n" +
               "        try:\n" +
               "            parts = input_line.split()\n" +
               "            numbers = [int(x) for x in parts]\n" +
               "            if len(numbers) == 1:\n" +
               "                " + functionName + "(numbers[0])\n" +
               "            else:\n" +
               "                " + functionName + "(numbers)\n" +
               "        except:\n" +
               "            " + functionName + "(input_line)\n" +
               "    except Exception as e:\n" +
               "        print(f'Error: {e}')\n";
    }
    
    private String generateJavaFallback(String studentCode, String functionName) {
        return "import java.util.*;\n\n" +
               "public class Solution {\n" +
               "    " + studentCode + "\n\n" +
               "    public static void main(String[] args) {\n" +
               "        Scanner scanner = new Scanner(System.in);\n" +
               "        try {\n" +
               "            String input = scanner.nextLine();\n" +
               "            // Basic parsing logic\n" +
               "            try {\n" +
               "                int value = Integer.parseInt(input.trim());\n" +
               "                " + functionName + "(value);\n" +
               "            } catch (NumberFormatException e) {\n" +
               "                " + functionName + "(input);\n" +
               "            }\n" +
               "        } finally {\n" +
               "            scanner.close();\n" +
               "        }\n" +
               "    }\n" +
               "}\n";
    }
    
    private String generateCFallback(String studentCode, String functionName) {
        return "#include <stdio.h>\n" +
               "#include <stdlib.h>\n" +
               "#include <string.h>\n" +
               "#include <math.h>\n\n" +
               studentCode + "\n\n" +
               "int main() {\n" +
               "    char input[1000];\n" +
               "    if (fgets(input, sizeof(input), stdin)) {\n" +
               "        input[strcspn(input, \"\\n\")] = 0;\n" +
               "        int value = atoi(input);\n" +
               "        " + functionName + "(value);\n" +
               "    }\n" +
               "    return 0;\n" +
               "}\n";
    }
    
    private String generateCppFallback(String studentCode, String functionName) {
        return "#include <iostream>\n" +
               "#include <string>\n" +
               "#include <vector>\n" +
               "#include <sstream>\n" +
               "using namespace std;\n\n" +
               studentCode + "\n\n" +
               "int main() {\n" +
               "    string input;\n" +
               "    getline(cin, input);\n" +
               "    try {\n" +
               "        int value = stoi(input);\n" +
               "        " + functionName + "(value);\n" +
               "    } catch (...) {\n" +
               "        " + functionName + "(input);\n" +
               "    }\n" +
               "    return 0;\n" +
               "}\n";
    }

    // ========== TEMPLATE LOADING ==========
    
    private void loadTemplatesFromResources() {
        try {
            // Load universal templates
            loadUniversalTemplates();
            
            // Load legacy templates for backward compatibility
            loadLegacyTemplates();
            
            log.info("Loaded {} wrapper templates", wrapperTemplates.size());
            
        } catch (Exception e) {
            log.error("Failed to load wrapper templates", e);
            // Initialize with empty templates to prevent NPE
            wrapperTemplates = new HashMap<>();
            languageTemplates = new HashMap<>();
        }
    }
    
    private void loadUniversalTemplates() {
        try {
            ClassPathResource resource = new ClassPathResource("universal-wrapper-templates.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, Object> templates = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
                    
                    for (Map.Entry<String, Object> entry : templates.entrySet()) {
                        String templateName = entry.getKey();
                        Map<String, Object> templateData = (Map<String, Object>) entry.getValue();
                        
                        WrapperTemplate template = new WrapperTemplate();
                        template.setDescription((String) templateData.get("description"));
                        template.setTemplate((String) templateData.get("template"));
                        template.setPatterns((List<String>) templateData.get("patterns"));
                        template.setLanguages((List<String>) templateData.get("languages"));
                        template.setComplexity((String) templateData.get("complexity"));
                        
                        wrapperTemplates.put(templateName, template);
                        
                        // Also organize by language
                        for (String language : template.getLanguages()) {
                            languageTemplates.computeIfAbsent(language.toLowerCase(), k -> new HashMap<>())
                                            .put("universal", template);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load universal templates", e);
        }
    }
    
    private void loadLegacyTemplates() {
        try {
            ClassPathResource resource = new ClassPathResource("wrapper-templates.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, Object> templates = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
                    
                    Map<String, Object> wrapperTemplatesData = (Map<String, Object>) templates.get("wrapperTemplates");
                    if (wrapperTemplatesData != null) {
                        for (Map.Entry<String, Object> languageEntry : wrapperTemplatesData.entrySet()) {
                            String language = languageEntry.getKey();
                            Map<String, Object> languageTemplates = (Map<String, Object>) languageEntry.getValue();
                            
                            for (Map.Entry<String, Object> patternEntry : languageTemplates.entrySet()) {
                                String pattern = patternEntry.getKey();
                                Map<String, Object> patternData = (Map<String, Object>) patternEntry.getValue();
                                
                                WrapperTemplate template = new WrapperTemplate();
                                template.setDescription((String) patternData.get("description"));
                                template.setTemplate((String) patternData.get("template"));
                                template.setPatterns(Arrays.asList(pattern));
                                template.setLanguages(Arrays.asList(language));
                                template.setComplexity("legacy");
                                
                                String templateKey = language + "_" + pattern;
                                wrapperTemplates.put(templateKey, template);
                                
                                // Organize by language and pattern
                                this.languageTemplates.computeIfAbsent(language.toLowerCase(), k -> new HashMap<>())
                                                      .put(pattern, template);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load legacy templates", e);
        }
    }

    // ========== DATA CLASSES ==========
    
    @Data
    public static class WrapperTemplate {
        private String description;
        private String template;
        private List<String> patterns;
        private List<String> languages;
        private String complexity;
    }
    
    @Data
    public static class FunctionAnalysisResult {
        private String functionName;
        private String returnType;
        private List<String> parameterTypes;
        private int parameterCount;
        
        public FunctionAnalysisResult() {
            this.parameterTypes = new ArrayList<>();
            this.parameterCount = 0;
        }
    }
    
    @Data
    public static class TestCaseAnalysis {
        private String inputPattern;
        private double confidence;
        private Map<String, Object> metadata;
        
        public TestCaseAnalysis() {
            this.inputPattern = "single_value";
            this.confidence = 1.0;
            this.metadata = new HashMap<>();
        }
    }
}