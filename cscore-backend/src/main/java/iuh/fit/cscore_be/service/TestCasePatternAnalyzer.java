package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.entity.TestCase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Test Case Pattern Recognition System
 * Analyzes test cases to automatically determine input/output patterns
 */
@Service
@Slf4j
public class TestCasePatternAnalyzer {

    /**
     * Analyze test cases to detect input patterns
     */
    public TestCaseAnalysis analyzeTestCases(List<TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return createDefaultAnalysis();
        }

        TestCaseAnalysis analysis = new TestCaseAnalysis();
        
        // Extract input samples
        List<String> inputSamples = testCases.stream()
            .map(TestCase::getInput)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // Extract expected outputs
        List<String> outputSamples = testCases.stream()
            .map(TestCase::getExpectedOutput)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // Analyze input patterns
        InputPatternResult inputPattern = analyzeInputPatterns(inputSamples);
        analysis.setInputPattern(inputPattern);
        
        // Analyze output patterns
        OutputPatternResult outputPattern = analyzeOutputPatterns(outputSamples);
        analysis.setOutputPattern(outputPattern);
        
        // Determine overall complexity
        analysis.setComplexity(determineComplexity(inputPattern, outputPattern));
        
        // Generate parsing strategy
        analysis.setParsingStrategy(generateParsingStrategy(inputPattern));
        
        return analysis;
    }

    /**
     * Analyze input patterns from test case samples
     */
    private InputPatternResult analyzeInputPatterns(List<String> inputs) {
        InputPatternResult result = new InputPatternResult();
        
        if (inputs.isEmpty()) {
            result.setType("unknown");
            return result;
        }

        // Analyze first few samples to determine pattern
        String firstInput = inputs.get(0).trim();
        
        // 1. Check for string + character pattern
        if (isStringCharPattern(firstInput)) {
            result.setType("string_char");
            result.setDelimiter(" ");
            result.setDescription("String followed by character");
            result.setParsingHints(Arrays.asList("quoted_string", "space_separated"));
            return result;
        }
        
        // 2. Check for three float values pattern FIRST (before array pattern)
        if (isThreeFloatValuesPattern(firstInput)) {
            result.setType("three_float_values");
            result.setDelimiter(detectDelimiter(Arrays.asList(firstInput)));
            result.setDescription("Three float parameters (e.g., equation coefficients)");
            result.setParsingHints(Arrays.asList("three_floats", "equation_params"));
            return result;
        }
        
        // 3. Check for array pattern
        if (isArrayPattern(inputs)) {
            result.setType("array");
            result.setDelimiter(detectDelimiter(inputs));
            result.setDescription("Array of numbers");
            result.setParsingHints(Arrays.asList("numeric_array", "variable_length"));
            return result;
        }
        
        // 4. Check for matrix pattern
        if (isMatrixPattern(inputs)) {
            result.setType("matrix");
            result.setDelimiter(" ");
            result.setDescription("2D matrix with dimensions");
            result.setParsingHints(Arrays.asList("dimensions_first", "row_by_row"));
            return result;
        }
        
        // 5. Check for multiple values pattern
        if (isMultipleValuesPattern(firstInput)) {
            result.setType("multiple_values");
            result.setDelimiter(detectDelimiter(Arrays.asList(firstInput)));
            result.setDescription("Multiple space/comma separated values");
            result.setParsingHints(Arrays.asList("fixed_count", "numeric"));
            return result;
        }
        
        // 6. Default to single value
        result.setType("single_value");
        result.setDelimiter("");
        result.setDescription("Single value input");
        result.setParsingHints(Arrays.asList("single_line", "numeric_or_string"));
        
        return result;
    }

    /**
     * Analyze output patterns
     */
    private OutputPatternResult analyzeOutputPatterns(List<String> outputs) {
        OutputPatternResult result = new OutputPatternResult();
        
        if (outputs.isEmpty()) {
            result.setType("unknown");
            return result;
        }

        String firstOutput = outputs.get(0).trim();
        
        // Check if numeric
        if (isNumericOutput(outputs)) {
            result.setType("numeric");
            result.setFormat("single_number");
        }
        // Check if boolean-like
        else if (isBooleanOutput(outputs)) {
            result.setType("boolean");
            result.setFormat("true_false_or_yes_no");
        }
        // Check if array-like
        else if (isArrayOutput(outputs)) {
            result.setType("array");
            result.setFormat("space_or_comma_separated");
        }
        // Default to string
        else {
            result.setType("string");
            result.setFormat("free_text");
        }
        
        return result;
    }

    /**
     * Generate parsing strategy based on input pattern
     */
    private ParsingStrategy generateParsingStrategy(InputPatternResult inputPattern) {
        ParsingStrategy strategy = new ParsingStrategy();
        
        switch (inputPattern.getType()) {
            case "string_char":
                strategy.setParserType("string_char_parser");
                strategy.setSteps(Arrays.asList(
                    "Find last space in input",
                    "Extract string part (remove quotes if present)",
                    "Extract character part",
                    "Return [string, char]"
                ));
                strategy.setErrorHandling("Default to empty string and space character");
                break;
                
            case "array":
                strategy.setParserType("array_parser");
                strategy.setSteps(Arrays.asList(
                    "Split input by delimiter: " + inputPattern.getDelimiter(),
                    "Convert each part to integer",
                    "Return [array, length]"
                ));
                strategy.setErrorHandling("Skip invalid numbers, default to empty array");
                break;
                
            case "matrix":
                strategy.setParserType("matrix_parser");
                strategy.setSteps(Arrays.asList(
                    "Parse dimensions from first line or first numbers",
                    "Parse matrix data row by row",
                    "Return [matrix, rows, cols]"
                ));
                strategy.setErrorHandling("Default to empty matrix with 0 dimensions");
                break;
                
            case "three_float_values":
                strategy.setParserType("three_float_parser");
                strategy.setSteps(Arrays.asList(
                    "Split input by delimiter: " + inputPattern.getDelimiter(),
                    "Parse exactly 3 float values",
                    "Return [float1, float2, float3]"
                ));
                strategy.setErrorHandling("Default to 0.0 for missing/invalid float values");
                break;
                
            case "multiple_values":
                strategy.setParserType("multiple_values_parser");
                strategy.setSteps(Arrays.asList(
                    "Split by delimiter: " + inputPattern.getDelimiter(),
                    "Convert to appropriate types",
                    "Return list of values"
                ));
                strategy.setErrorHandling("Default values for missing/invalid inputs");
                break;
                
            default:
                strategy.setParserType("single_value_parser");
                strategy.setSteps(Arrays.asList(
                    "Parse single value from input",
                    "Convert to appropriate type",
                    "Return single value"
                ));
                strategy.setErrorHandling("Default to 0 or empty string");
        }
        
        return strategy;
    }

    // Pattern detection helper methods
    private boolean isStringCharPattern(String input) {
        // Pattern: "hello" a or hello a or "string with spaces" x
        return (input.contains("\"") && input.lastIndexOf(' ') > input.lastIndexOf('"')) ||
               (input.split(" ").length == 2 && !isAllNumeric(input));
    }

    private boolean isArrayPattern(List<String> inputs) {
        // Check if inputs look like arrays (multiple numbers)
        for (String input : inputs) {
            String[] parts = input.trim().split("[,\\s]+");
            if (parts.length > 2) {
                // Check if most parts are numeric
                long numericCount = Arrays.stream(parts)
                    .filter(this::isNumeric)
                    .count();
                if (numericCount >= parts.length * 0.7) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMatrixPattern(List<String> inputs) {
        // Look for patterns like "2 3 1 2 3 4 5 6" (dimensions + data)
        for (String input : inputs) {
            String[] parts = input.trim().split("\\s+");
            if (parts.length >= 3) {
                // First two might be dimensions
                if (isNumeric(parts[0]) && isNumeric(parts[1])) {
                    int rows = Integer.parseInt(parts[0]);
                    int cols = Integer.parseInt(parts[1]);
                    if (rows * cols + 2 <= parts.length) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isThreeFloatValuesPattern(String input) {
        String[] parts = input.trim().split("[,\\s]+");
        if (parts.length != 3) {
            return false;
        }
        
        // Check if all three parts are numeric (can be float)
        for (String part : parts) {
            if (!isNumeric(part) && !isFloatNumeric(part)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isFloatNumeric(String str) {
        try {
            Float.parseFloat(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isMultipleValuesPattern(String input) {
        String[] parts = input.trim().split("[,\\s]+");
        return parts.length > 1 && parts.length <= 5 && 
               Arrays.stream(parts).allMatch(this::isNumeric);
    }

    private String detectDelimiter(List<String> inputs) {
        Map<String, Integer> delimiterCount = new HashMap<>();
        
        for (String input : inputs) {
            if (input.contains(",")) {
                delimiterCount.merge(",", 1, Integer::sum);
            }
            if (input.contains(" ")) {
                delimiterCount.merge(" ", 1, Integer::sum);
            }
            if (input.contains("\t")) {
                delimiterCount.merge("\t", 1, Integer::sum);
            }
        }
        
        return delimiterCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(" ");
    }

    private boolean isNumericOutput(List<String> outputs) {
        return outputs.stream()
            .allMatch(output -> isNumeric(output.trim()));
    }

    private boolean isBooleanOutput(List<String> outputs) {
        return outputs.stream()
            .allMatch(output -> {
                String lower = output.trim().toLowerCase();
                return lower.equals("true") || lower.equals("false") ||
                       lower.equals("yes") || lower.equals("no") ||
                       lower.equals("1") || lower.equals("0");
            });
    }

    private boolean isArrayOutput(List<String> outputs) {
        return outputs.stream()
            .anyMatch(output -> output.contains(" ") || output.contains(","));
    }

    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isAllNumeric(String input) {
        return Arrays.stream(input.split("\\s+"))
            .allMatch(this::isNumeric);
    }

    private String determineComplexity(InputPatternResult input, OutputPatternResult output) {
        int complexityScore = 0;
        
        // Input complexity
        switch (input.getType()) {
            case "matrix": complexityScore += 3; break;
            case "array": complexityScore += 2; break;
            case "string_char": complexityScore += 2; break;
            case "multiple_values": complexityScore += 1; break;
            default: complexityScore += 0;
        }
        
        // Output complexity
        switch (output.getType()) {
            case "array": complexityScore += 2; break;
            case "string": complexityScore += 1; break;
            default: complexityScore += 0;
        }
        
        if (complexityScore >= 4) return "high";
        if (complexityScore >= 2) return "medium";
        return "low";
    }

    private TestCaseAnalysis createDefaultAnalysis() {
        TestCaseAnalysis analysis = new TestCaseAnalysis();
        
        InputPatternResult input = new InputPatternResult();
        input.setType("single_value");
        input.setDescription("Default single value input");
        
        OutputPatternResult output = new OutputPatternResult();
        output.setType("numeric");
        output.setFormat("single_number");
        
        analysis.setInputPattern(input);
        analysis.setOutputPattern(output);
        analysis.setComplexity("low");
        
        return analysis;
    }

    // Data classes
    @Data
    public static class TestCaseAnalysis {
        private InputPatternResult inputPattern;
        private OutputPatternResult outputPattern;
        private String complexity;
        private ParsingStrategy parsingStrategy;
    }

    @Data
    public static class InputPatternResult {
        private String type;
        private String delimiter;
        private String description;
        private List<String> parsingHints;
    }

    @Data
    public static class OutputPatternResult {
        private String type;
        private String format;
    }

    @Data
    public static class ParsingStrategy {
        private String parserType;
        private List<String> steps;
        private String errorHandling;
    }
}