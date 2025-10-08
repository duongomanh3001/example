package iuh.fit.cscore_be.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.cscore_be.entity.Question;
import iuh.fit.cscore_be.entity.TestCase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * Enhanced Code Wrapper Service - Universal, configurable, intelligent system
 * Replaces hardcoded wrapper logic with flexible, metadata-driven approach
 */
@Service
@Slf4j
public class EnhancedCodeWrapperService {

    @Autowired
    private UniversalWrapperService universalWrapperService;
    
    @Autowired
    private FunctionSignatureAnalyzer signatureAnalyzer;
    
    @Autowired
    private TestCasePatternAnalyzer testCaseAnalyzer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, WrapperTemplate> wrapperTemplates = new HashMap<>();

    @PostConstruct
    public void initializeTemplates() {
        loadDefaultTemplates();
        // In future: loadCustomTemplates();
    }

    /**
     * Main entry point - Generate wrapper with intelligent analysis
     */
    public String wrapFunctionCode(String studentCode, Question question, String language, List<TestCase> testCases) {
        try {
            log.info("Generating intelligent wrapper for function: {}, language: {}", 
                     question != null ? question.getFunctionName() : "unknown", language);

            // 1. Analyze function signature
            FunctionSignatureAnalyzer.FunctionAnalysisResult signatureAnalysis = 
                signatureAnalyzer.analyzeFunctionSignature(studentCode, question);
            
            // 2. Analyze test case patterns
            TestCasePatternAnalyzer.TestCaseAnalysis testCaseAnalysis = 
                testCaseAnalyzer.analyzeTestCases(testCases);
            
            // 3. Check if already has main function
            if (hasMainFunction(studentCode, language)) {
                log.info("Code already has main function, using as-is");
                return studentCode;
            }
            
            // 4. Generate wrapper based on language and analysis
            return generateIntelligentWrapper(studentCode, question, language, signatureAnalysis, testCaseAnalysis);
            
        } catch (Exception e) {
            log.error("Error in enhanced wrapper generation", e);
            return generateFallbackWrapper(studentCode, question, language);
        }
    }

    /**
     * Generate intelligent wrapper based on comprehensive analysis
     * UPDATED: Uses template-driven approach with signature-aware selection
     */
    private String generateIntelligentWrapper(String studentCode, Question question, String language,
                                            FunctionSignatureAnalyzer.FunctionAnalysisResult signatureAnalysis,
                                            TestCasePatternAnalyzer.TestCaseAnalysis testCaseAnalysis) {
        
        log.info("Generating template-based wrapper for language: {}, pattern: {}", 
                language, testCaseAnalysis.getInputPattern().getType());
        
        // Find appropriate template based on language, pattern AND function signature
        WrapperTemplate template = findBestTemplate(language, testCaseAnalysis.getInputPattern().getType(), signatureAnalysis);
        
        if (template != null) {
            return generateFromTemplate(template, studentCode, signatureAnalysis, testCaseAnalysis);
        } else {
            log.warn("No suitable template found for language: {}, pattern: {}", 
                     language, testCaseAnalysis.getInputPattern().getType());
            return generateFallbackWrapper(studentCode, question, language);
        }
    }
    
    /**
     * Find the best template for language and pattern combination with function signature consideration
     */
    private WrapperTemplate findBestTemplate(String language, String patternType, 
                                           FunctionSignatureAnalyzer.FunctionAnalysisResult signatureAnalysis) {
        
        // Extract parameter count from function signature
        int parameterCount = extractParameterCount(signatureAnalysis);
        String functionName = extractFunctionName(signatureAnalysis);
        
        log.info("Template selection: language={}, pattern={}, paramCount={}, function={}", 
                language, patternType, parameterCount, functionName);
        
        // Priority-based template selection
        // 1. Check for specific function name + parameter count matches
        if (functionName != null) {
            WrapperTemplate specificMatch = findTemplateByFunctionSignature(language, functionName, parameterCount);
            if (specificMatch != null) {
                log.info("Selected template by function signature: {}", specificMatch.getName());
                return specificMatch;
            }
        }
        
        // 2. Try pattern-based selection with parameter count validation
        String specificKey = language.toLowerCase() + "_" + patternType;
        WrapperTemplate specificTemplate = wrapperTemplates.get(specificKey);
        
        if (specificTemplate != null) {
            log.info("Found pattern-specific template: {}", specificKey);
            return specificTemplate;
        }
        
        // 3. Parameter count based fallback selection
        WrapperTemplate parameterBasedTemplate = findTemplateByParameterCount(language, parameterCount);
        if (parameterBasedTemplate != null) {
            log.info("Selected template by parameter count: {}", parameterBasedTemplate.getName());
            return parameterBasedTemplate;
        }
        
        // 4. Language-specific fallback
        String fallbackKey = language.toLowerCase() + "_single_value";
        WrapperTemplate fallbackTemplate = wrapperTemplates.get(fallbackKey);
        
        if (fallbackTemplate != null) {
            log.info("Using fallback template: {}", fallbackKey);
            return fallbackTemplate;
        }
        
        // 5. Try to find any template for the language
        for (WrapperTemplate template : wrapperTemplates.values()) {
            if (template.getLanguage().equalsIgnoreCase(language)) {
                log.info("Using generic template for language: {}", language);
                return template;
            }
        }
        
        return null;
    }
    
    /**
     * Find template that matches function name and parameter count exactly
     */
    private WrapperTemplate findTemplateByFunctionSignature(String language, String functionName, int paramCount) {
        // For 2-parameter functions like inSoLe(arr[], int n), prefer array_size template
        if (paramCount == 2 && functionName.toLowerCase().contains("insole")) {
            return wrapperTemplates.get(language.toLowerCase() + "_array_size");
        }
        
        // For 3-parameter functions like quickSort(arr[], int left, int right), prefer quicksort template
        if (paramCount == 3 && (functionName.toLowerCase().contains("quicksort") || 
                                functionName.toLowerCase().contains("quick_sort"))) {
            return wrapperTemplates.get(language.toLowerCase() + "_quicksort");
        }
        
        return null;
    }
    
    /**
     * Find template based on parameter count
     */
    private WrapperTemplate findTemplateByParameterCount(String language, int paramCount) {
        String baseKey = language.toLowerCase();
        
        switch (paramCount) {
            case 1:
                return wrapperTemplates.get(baseKey + "_single_value");
            case 2:
                // Try array_size first, then general array
                WrapperTemplate arraySize = wrapperTemplates.get(baseKey + "_array_size");
                if (arraySize != null) return arraySize;
                return wrapperTemplates.get(baseKey + "_array");
            case 3:
                // For 3 parameters, could be quicksort or matrix
                WrapperTemplate quicksort = wrapperTemplates.get(baseKey + "_quicksort");
                if (quicksort != null) return quicksort;
                return wrapperTemplates.get(baseKey + "_array");
            default:
                return wrapperTemplates.get(baseKey + "_array");
        }
    }
    
    /**
     * Extract parameter count from function signature analysis
     */
    private int extractParameterCount(FunctionSignatureAnalyzer.FunctionAnalysisResult signatureAnalysis) {
        if (signatureAnalysis == null || signatureAnalysis.getFunctionInfo() == null) {
            return 1; // Default
        }
        
        String parameters = signatureAnalysis.getFunctionInfo().getParameters();
        if (parameters == null || parameters.trim().isEmpty()) {
            return 1;
        }
        
        // Count comma-separated parameters
        return parameters.split(",").length;
    }
    
    /**
     * Generate wrapper code from template with intelligent substitution
     */
    private String generateFromTemplate(WrapperTemplate template, String studentCode,
                                      FunctionSignatureAnalyzer.FunctionAnalysisResult signatureAnalysis,
                                      TestCasePatternAnalyzer.TestCaseAnalysis testCaseAnalysis) {
        
        String templateCode = template.getTemplate();
        
        // Replace template placeholders with actual values
        templateCode = templateCode.replace("{STUDENT_CODE}", studentCode);
        
        // Function name substitution
        String functionName = extractFunctionName(signatureAnalysis);
        templateCode = templateCode.replace("{FUNCTION_NAME}", functionName);
        
        // Generate intelligent parsing code based on pattern
        String parsingCode = generateIntelligentParsingCode(testCaseAnalysis, template.getLanguage());
        templateCode = templateCode.replace("{PARSING_CODE}", parsingCode);
        
        // Additional placeholders can be added as needed
        templateCode = templateCode.replace("{INPUT_PATTERN}", testCaseAnalysis.getInputPattern().getType());
        templateCode = templateCode.replace("{COMPLEXITY}", testCaseAnalysis.getComplexity());
        
        log.debug("Generated wrapper from template: {}", template.getName());
        return templateCode;
    }
    
    /**
     * Extract function name from signature analysis
     */
    private String extractFunctionName(FunctionSignatureAnalyzer.FunctionAnalysisResult signatureAnalysis) {
        if (signatureAnalysis != null && signatureAnalysis.getFunctionInfo() != null 
            && signatureAnalysis.getFunctionInfo().getFunctionName() != null) {
            return signatureAnalysis.getFunctionInfo().getFunctionName();
        }
        return "processInput"; // Default fallback
    }
    
    /**
     * Generate intelligent parsing code based on pattern and language
     */
    private String generateIntelligentParsingCode(TestCasePatternAnalyzer.TestCaseAnalysis testCaseAnalysis, String language) {
        String patternType = testCaseAnalysis.getInputPattern().getType();
        
        // This could be further expanded with more sophisticated logic
        // For now, return pattern-specific hints that templates can use
        switch (patternType) {
            case "string_char":
                return "// Parse string and character input";
            case "array":
                return "// Parse array input with delimiter: " + testCaseAnalysis.getInputPattern().getDelimiter();
            case "matrix":
                return "// Parse 2D matrix input";
            case "multiple_values":
                return "// Parse multiple space/comma separated values";
            default:
                return "// Parse single value input";
        }
    }

    // REMOVED: Hardcoded Python wrapper generation
    // Now uses template-based approach via generateFromTemplate()

    // REMOVED: Hardcoded C wrapper generation
    // Now uses template-based approach via generateFromTemplate()

    // REMOVED: Hardcoded C++ wrapper generation
    // Now uses template-based approach via generateFromTemplate()

    // REMOVED: Hardcoded Java wrapper generation  
    // Now uses template-based approach via generateFromTemplate()

    // REMOVED: Hardcoded parser generation
    // Now uses template-based approach with {PARSING_CODE} placeholder

    // REMOVED: Hardcoded C parsing generation
    // Now handled by templates in wrapper-templates.json

    // REMOVED: Hardcoded C++ parsing generation
    // Now handled by templates in wrapper-templates.json

    // REMOVED: Hardcoded Java parsing generation
    // Now handled by templates in wrapper-templates.json

    // Utility methods
    private boolean hasMainFunction(String code, String language) {
        switch (language.toLowerCase()) {
            case "c":
            case "cpp":
                return code.contains("int main(") || code.contains("int main (");
            case "python":
                return code.contains("if __name__") && code.contains("__main__");
            case "java":
                return code.contains("public static void main") || code.contains("static void main");
            default:
                return false;
        }
    }

    private String getDefaultFunctionName(FunctionSignatureAnalyzer.FunctionPattern pattern) {
        // Try to extract function name from pattern or use default
        return "processInput"; // This should be extracted from actual analysis
    }

    private String generateFallbackWrapper(String studentCode, Question question, String language) {
        return "// Fallback wrapper\n" + studentCode;
    }

    private void loadDefaultTemplates() {
        try {
            log.info("Loading default wrapper templates from wrapper-templates.json");
            
            ClassPathResource resource = new ClassPathResource("wrapper-templates.json");
            if (resource.exists()) {
                Map<String, Object> templatesData = objectMapper.readValue(
                    resource.getInputStream(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                @SuppressWarnings("unchecked")
                Map<String, Object> wrapperTemplates = (Map<String, Object>) templatesData.get("wrapperTemplates");
                
                if (wrapperTemplates != null) {
                    for (Map.Entry<String, Object> languageEntry : wrapperTemplates.entrySet()) {
                        String language = languageEntry.getKey();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> languageTemplates = (Map<String, Object>) languageEntry.getValue();
                        
                        for (Map.Entry<String, Object> templateEntry : languageTemplates.entrySet()) {
                            String templateName = templateEntry.getKey();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> templateData = (Map<String, Object>) templateEntry.getValue();
                            
                            WrapperTemplate template = new WrapperTemplate();
                            template.setName((String) templateData.get("name"));
                            template.setLanguage(language);
                            template.setTemplate((String) templateData.get("template"));
                            
                            // Handle patterns if they exist
                            @SuppressWarnings("unchecked")
                            List<String> patterns = (List<String>) templateData.get("patterns");
                            if (patterns != null) {
                                template.setSupportedPatterns(patterns);
                            } else {
                                template.setSupportedPatterns(Arrays.asList(".*"));
                            }
                            
                            String key = language + "_" + templateName;
                            this.wrapperTemplates.put(key, template);
                            
                            log.debug("Loaded template: {} for language: {}", templateName, language);
                        }
                    }
                    
                    log.info("Successfully loaded {} wrapper templates", this.wrapperTemplates.size());
                } else {
                    log.warn("No wrapperTemplates section found in configuration");
                }
            } else {
                log.warn("wrapper-templates.json not found in classpath, using fallback templates");
                loadFallbackTemplates();
            }
        } catch (IOException e) {
            log.error("Failed to load wrapper templates from configuration: {}", e.getMessage());
            loadFallbackTemplates();
        }
    }
    
    private void loadFallbackTemplates() {
        log.info("Loading fallback wrapper templates");
        
        // Create basic fallback templates for common patterns
        WrapperTemplate pythonSingle = new WrapperTemplate();
        pythonSingle.setName("Python Single Value");
        pythonSingle.setLanguage("python");
        pythonSingle.setTemplate("import sys\n\n{STUDENT_CODE}\n\nif __name__ == '__main__':\n    try:\n        value = int(input().strip())\n        result = {FUNCTION_NAME}(value)\n        if result is not None:\n            print(result)\n    except Exception as e:\n        print(f\"Error: {e}\")\n        sys.exit(1)");
        pythonSingle.setSupportedPatterns(Arrays.asList(".*int.*", ".*number.*"));
        this.wrapperTemplates.put("python_single_value", pythonSingle);
        
        // Add more fallback templates as needed...
        
        log.info("Loaded {} fallback templates", this.wrapperTemplates.size());
    }

    // Data classes for configuration
    @Data
    public static class WrapperTemplate {
        private String name;
        private String language;
        private String template;
        private List<String> supportedPatterns;
    }
}