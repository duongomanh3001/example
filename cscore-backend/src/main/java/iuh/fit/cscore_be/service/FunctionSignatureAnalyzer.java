package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.entity.Question;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced Function Signature Parser - Intelligent analysis of function signatures
 * Replaces hardcoded isStringCharFunction() and similar methods
 */
@Service
@Slf4j
public class FunctionSignatureAnalyzer {

    // Common programming patterns
    private static final Map<String, FunctionPattern> FUNCTION_PATTERNS = new HashMap<>();
    
    static {
        // String manipulation patterns
        FUNCTION_PATTERNS.put("STRING_CHAR", new FunctionPattern(
            "string_char",
            Arrays.asList(".*char.*\\*.*char.*", ".*string.*char.*", ".*char\\s*\\[\\].*char.*"),
            Arrays.asList("string", "char"),
            "String and character manipulation"
        ));
        
        // Array processing patterns
        FUNCTION_PATTERNS.put("ARRAY_SIZE", new FunctionPattern(
            "array_size", 
            Arrays.asList(".*int.*\\[\\].*int.*", ".*arr.*n.*", ".*array.*size.*"),
            Arrays.asList("array", "int"),
            "Array processing with size parameter"
        ));
        
        // Matrix patterns
        FUNCTION_PATTERNS.put("MATRIX", new FunctionPattern(
            "matrix",
            Arrays.asList(".*int.*\\[\\]\\[\\].*", ".*matrix.*", ".*\\*\\*.*int.*int.*"),
            Arrays.asList("matrix", "rows", "cols"),
            "2D matrix processing"
        ));
        
        // Graph patterns
        FUNCTION_PATTERNS.put("GRAPH", new FunctionPattern(
            "graph",
            Arrays.asList(".*graph.*", ".*adjacency.*", ".*vertex.*edge.*"),
            Arrays.asList("graph", "vertices"),
            "Graph algorithms"
        ));
        
        // Mathematical patterns
        FUNCTION_PATTERNS.put("MATH_MULTIPLE", new FunctionPattern(
            "math_multiple",
            Arrays.asList(".*int.*int.*", ".*double.*double.*", ".*float.*float.*"),
            Arrays.asList("number", "number"),
            "Mathematical operations with multiple parameters"
        ));
    }

    /**
     * Analyze function signature and determine the best pattern match
     */
    public FunctionAnalysisResult analyzeFunctionSignature(String functionCode, Question question) {
        try {
            // Extract function information
            FunctionInfo functionInfo = extractFunctionInfo(functionCode);
            
            // Enhance with question metadata
            if (question != null) {
                enhanceWithQuestionMetadata(functionInfo, question);
            }
            
            // Determine pattern
            FunctionPattern bestPattern = findBestPattern(functionInfo);
            
            // Create analysis result
            return new FunctionAnalysisResult(functionInfo, bestPattern);
            
        } catch (Exception e) {
            log.error("Error analyzing function signature", e);
            return createFallbackResult(functionCode, question);
        }
    }

    /**
     * Extract function information from code using advanced regex
     */
    private FunctionInfo extractFunctionInfo(String code) {
        FunctionInfo info = new FunctionInfo();
        
        // C/C++ function pattern
        Pattern cPattern = Pattern.compile(
            "(\\w+)\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*\\{?", 
            Pattern.MULTILINE | Pattern.DOTALL
        );
        
        // Python function pattern
        Pattern pythonPattern = Pattern.compile(
            "def\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*:", 
            Pattern.MULTILINE
        );
        
        // Java function pattern
        Pattern javaPattern = Pattern.compile(
            "(?:public|private|protected)?\\s*(?:static)?\\s*(\\w+)\\s+(\\w+)\\s*\\(([^)]+)\\)", 
            Pattern.MULTILINE
        );
        
        Matcher matcher;
        
        // Try C/C++ pattern
        matcher = cPattern.matcher(code);
        if (matcher.find()) {
            info.setReturnType(matcher.group(1));
            info.setFunctionName(matcher.group(2));
            info.setParameters(matcher.group(3));
            info.setLanguage("c");
            return info;
        }
        
        // Try Python pattern
        matcher = pythonPattern.matcher(code);
        if (matcher.find()) {
            info.setFunctionName(matcher.group(1));
            info.setParameters(matcher.group(2));
            info.setLanguage("python");
            return info;
        }
        
        // Try Java pattern
        matcher = javaPattern.matcher(code);
        if (matcher.find()) {
            info.setReturnType(matcher.group(1));
            info.setFunctionName(matcher.group(2));
            info.setParameters(matcher.group(3));
            info.setLanguage("java");
            return info;
        }
        
        return info; // Return empty info if no pattern matches
    }

    /**
     * Enhance function info with question metadata
     */
    private void enhanceWithQuestionMetadata(FunctionInfo info, Question question) {
        if (question.getFunctionName() != null && info.getFunctionName() == null) {
            info.setFunctionName(question.getFunctionName());
        }
        
        if (question.getFunctionSignature() != null && info.getParameters() == null) {
            // Extract parameters from signature
            Pattern sigPattern = Pattern.compile("\\(([^)]+)\\)");
            Matcher matcher = sigPattern.matcher(question.getFunctionSignature());
            if (matcher.find()) {
                info.setParameters(matcher.group(1));
            }
        }
        
        // Analyze question content for hints
        if (question.getDescription() != null) {
            String content = question.getDescription().toLowerCase();
            
            if (content.contains("string") && content.contains("character")) {
                info.addHint("string_char");
            }
            if (content.contains("array") || content.contains("mảng")) {
                info.addHint("array");
            }
            if (content.contains("matrix") || content.contains("ma trận")) {
                info.addHint("matrix");
            }
            if (content.contains("graph") || content.contains("đồ thị")) {
                info.addHint("graph");
            }
        }
    }

    /**
     * Find the best matching pattern for the function
     */
    private FunctionPattern findBestPattern(FunctionInfo info) {
        int bestScore = 0;
        FunctionPattern bestPattern = null;
        
        for (FunctionPattern pattern : FUNCTION_PATTERNS.values()) {
            int score = calculatePatternScore(info, pattern);
            if (score > bestScore) {
                bestScore = score;
                bestPattern = pattern;
            }
        }
        
        // If no good pattern found, create dynamic pattern
        if (bestPattern == null) {
            bestPattern = createDynamicPattern(info);
        }
        
        return bestPattern;
    }

    /**
     * Calculate how well a pattern matches the function info
     */
    private int calculatePatternScore(FunctionInfo info, FunctionPattern pattern) {
        int score = 0;
        
        // Check parameter patterns
        if (info.getParameters() != null) {
            String params = info.getParameters().toLowerCase();
            for (String patternRegex : pattern.getParameterPatterns()) {
                if (params.matches(patternRegex)) {
                    score += 10;
                }
            }
        }
        
        // Check function name hints
        if (info.getFunctionName() != null) {
            String funcName = info.getFunctionName().toLowerCase();
            switch (pattern.getType()) {
                case "string_char":
                    if (funcName.contains("char") || funcName.contains("count")) score += 5;
                    break;
                case "array_size":
                    if (funcName.contains("array") || funcName.contains("sort") || funcName.contains("search")) score += 5;
                    break;
                case "matrix":
                    if (funcName.contains("matrix") || funcName.contains("2d")) score += 5;
                    break;
            }
        }
        
        // Check content hints
        for (String hint : info.getHints()) {
            if (pattern.getType().contains(hint)) {
                score += 3;
            }
        }
        
        return score;
    }

    /**
     * Create dynamic pattern when no predefined pattern matches
     */
    private FunctionPattern createDynamicPattern(FunctionInfo info) {
        List<String> expectedTypes = new ArrayList<>();
        String patternType = "dynamic";
        
        if (info.getParameters() != null) {
            String[] params = info.getParameters().split(",");
            
            for (String param : params) {
                param = param.trim().toLowerCase();
                if (param.contains("char") && param.contains("*")) {
                    expectedTypes.add("string");
                } else if (param.contains("char")) {
                    expectedTypes.add("char");
                } else if (param.contains("int") && param.contains("[]")) {
                    expectedTypes.add("array");
                } else if (param.contains("int")) {
                    expectedTypes.add("int");
                } else if (param.contains("double") || param.contains("float")) {
                    expectedTypes.add("double");
                } else {
                    expectedTypes.add("unknown");
                }
            }
            
            // Determine pattern type based on parameter analysis
            if (expectedTypes.contains("string") && expectedTypes.contains("char")) {
                patternType = "string_char";
            } else if (expectedTypes.contains("array")) {
                patternType = "array_processing";
            } else if (expectedTypes.size() == 1) {
                patternType = "single_parameter";
            } else {
                patternType = "multiple_parameters";
            }
        }
        
        return new FunctionPattern(
            patternType,
            Arrays.asList(".*"),
            expectedTypes,
            "Dynamically generated pattern"
        );
    }

    /**
     * Create fallback result when analysis fails
     */
    private FunctionAnalysisResult createFallbackResult(String code, Question question) {
        FunctionInfo info = new FunctionInfo();
        
        if (question != null) {
            info.setFunctionName(question.getFunctionName());
        }
        
        FunctionPattern fallbackPattern = new FunctionPattern(
            "fallback",
            Arrays.asList(".*"),
            Arrays.asList("unknown"),
            "Fallback pattern"
        );
        
        return new FunctionAnalysisResult(info, fallbackPattern);
    }

    // Data classes
    @Data
    public static class FunctionInfo {
        private String functionName;
        private String returnType;
        private String parameters;
        private String language;
        private List<String> hints = new ArrayList<>();
        
        public void addHint(String hint) {
            if (!hints.contains(hint)) {
                hints.add(hint);
            }
        }
    }

    @Data
    public static class FunctionPattern {
        private String type;
        private List<String> parameterPatterns;
        private List<String> expectedTypes;
        private String description;
        
        public FunctionPattern(String type, List<String> parameterPatterns, 
                             List<String> expectedTypes, String description) {
            this.type = type;
            this.parameterPatterns = parameterPatterns;
            this.expectedTypes = expectedTypes;
            this.description = description;
        }
    }

    @Data
    public static class FunctionAnalysisResult {
        private FunctionInfo functionInfo;
        private FunctionPattern pattern;
        private double confidence;
        
        public FunctionAnalysisResult(FunctionInfo functionInfo, FunctionPattern pattern) {
            this.functionInfo = functionInfo;
            this.pattern = pattern;
            this.confidence = calculateConfidence();
        }
        
        private double calculateConfidence() {
            // Calculate confidence based on available information
            double conf = 0.3; // Base confidence
            
            if (functionInfo.getFunctionName() != null) conf += 0.2;
            if (functionInfo.getParameters() != null) conf += 0.3;
            if (!functionInfo.getHints().isEmpty()) conf += 0.2;
            
            return Math.min(conf, 1.0);
        }
    }
}