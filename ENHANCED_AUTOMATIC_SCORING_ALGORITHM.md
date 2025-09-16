# Enhanced Automatic Scoring Algorithm

## Overview

The enhanced automatic scoring system implements a sophisticated algorithm that compares student implementations with teacher's reference implementations, providing more accurate and comprehensive grading than traditional test-case-only approaches.

## Algorithm Workflow

### 1. Teacher Creates Function Problem with Reference Implementation

**Teacher Setup Process:**
- Teacher creates a programming question with specific function requirements
- **Reference Implementation**: Teacher provides the correct solution code
- **Function Details**: Specifies function name, signature, and programming language
- **Test Cases**: Creates input/output test cases
- **Test Template**: Optional custom test harness template

**Enhanced Question Fields:**
```java
// New fields in Question entity
private String referenceImplementation;  // Teacher's correct solution
private String functionName;             // Name of function to test
private String functionSignature;       // Function signature/parameters
private String programmingLanguage;     // Language (Java, Python, C++)
private String testTemplate;            // Custom test program template
```

### 2. Teacher Creates Test Cases with Input/Expected Output

**Test Case Structure:**
- **Input**: Parameters to pass to the function
- **Expected Output**: What the function should return/print
- **Weight**: Scoring weight for this test case
- **Hidden/Visible**: Whether students can see this test case

### 3. Student Submits Function Implementation

**Student Submission Process:**
- Student implements the required function
- System extracts the student's function from submitted code
- Function can be standalone or part of multi-question submission

### 4. System Automatically Executes Enhanced Scoring

#### A. Test Code Generation

The system generates **complete executable test programs** for both implementations:

**For Java:**
```java
// Generated test program structure
import java.util.*;
import java.io.*;

public class Solution {
    // STUDENT_FUNCTION_CODE or REFERENCE_FUNCTION_CODE inserted here
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Solution solution = new Solution();
        // Function call with test case input
        System.out.println(solution.functionName(param1, param2, ...));
    }
}
```

**For Python:**
```python
# Generated test program structure
# STUDENT_FUNCTION_CODE or REFERENCE_FUNCTION_CODE inserted here

# Function call with test case input
print(functionName(param1, param2, ...))
```

**For C++:**
```cpp
// Generated test program structure
#include <iostream>
#include <vector>
#include <string>
using namespace std;

// STUDENT_FUNCTION_CODE or REFERENCE_FUNCTION_CODE inserted here

int main() {
    // Function call with test case input
    cout << functionName(param1, param2, ...) << endl;
    return 0;
}
```

#### B. Parallel Execution

**Execution Process:**
1. **Generate Student Test Program**: Combines student function with test harness
2. **Generate Reference Test Program**: Combines reference function with identical test harness
3. **Execute Both Programs**: Run with same input parameters
4. **Capture Results**: Collect outputs, execution time, memory usage, errors

#### C. Output Comparison → PASS/FAIL

**Comparison Logic:**
```java
// Detailed comparison process
TestCaseComparisonResult result = new TestCaseComparisonResult();

// 1. Execute reference implementation
CodeExecutionResponse referenceResponse = executeCode(referenceProgram, input);

// 2. Execute student implementation  
CodeExecutionResponse studentResponse = executeCode(studentProgram, input);

// 3. Handle execution failures
if (!referenceResponse.isSuccess()) {
    result.setError("Reference implementation failed");
    return result;
}

if (!studentResponse.isSuccess()) {
    result.setPassed(false);
    result.setErrorMessage(studentResponse.getError());
    return result;
}

// 4. Compare normalized outputs
String studentOutput = normalizeOutput(studentResponse.getOutput());
String referenceOutput = normalizeOutput(referenceResponse.getOutput());

result.setPassed(studentOutput.equals(referenceOutput));
result.setStudentOutput(studentOutput);
result.setReferenceOutput(referenceOutput);
```

#### D. Scoring Algorithm

**Score Calculation:**
```java
// Per test case scoring
if (testCaseResult.isPassed()) {
    earnedScore += testCase.getWeight();
}

// Final question score
double scorePercentage = totalWeight > 0 ? (earnedScore / totalWeight) : 0.0;
questionScore = scorePercentage * question.getPoints();

// Overall submission score
finalScore = sum(allQuestionScores) / sum(allMaxScores) * 100;
```

## Key Features

### 1. Reference Implementation Comparison
- **Direct Comparison**: Student output vs reference output (not just expected output)
- **Dynamic Testing**: Reference implementation runs with same inputs
- **Accurate Results**: Eliminates issues with static expected outputs

### 2. Complete Test Code Generation
- **Language-Specific**: Generates appropriate test harness per language
- **Function Extraction**: Automatically extracts target function from student code
- **Input Parsing**: Converts test case inputs to proper function parameters

### 3. Comprehensive Error Handling
- **Compilation Errors**: Detected and reported separately
- **Runtime Errors**: Captured with detailed error messages
- **Timeout Handling**: Prevents infinite loops or long-running code
- **Memory Limits**: Enforces resource constraints

### 4. Detailed Feedback
```java
// Example feedback output
"Kết quả so sánh với đáp án reference:
✓ Test case 1: PASS
✗ Test case 2: FAIL
  Input: 5, 3
  Reference output: 8
  Your output: 15
  Error: Logic error in addition

✓ Test case 3: PASS

Tổng kết: 2/3 test cases đạt yêu cầu
Điểm: 6.67/10.00"
```

### 5. Fallback Mechanism
- **Automatic Fallback**: Falls back to traditional grading if no reference implementation
- **Hybrid Approach**: Can mix reference-based and traditional grading per question
- **Error Recovery**: Graceful handling when enhanced grading fails

## Advantages Over Traditional Systems

### Traditional System Limitations:
- **Static Expected Outputs**: May not cover edge cases or different valid outputs
- **Limited Feedback**: Only shows "expected vs actual" without context
- **Manual Test Case Creation**: Requires extensive manual test case writing
- **No Function Isolation**: Tests entire programs rather than specific functions

### Enhanced System Benefits:
1. **Accurate Scoring**: Reference implementation ensures correctness
2. **Better Feedback**: Shows exactly how student code differs from correct solution  
3. **Function-Level Testing**: Isolates and tests specific functions
4. **Reduced Test Case Maintenance**: Reference implementation handles edge cases
5. **Multi-Language Support**: Consistent behavior across programming languages
6. **Detailed Error Analysis**: Distinguishes compilation, runtime, and logic errors

## Configuration Options

### Question-Level Configuration:
```java
// Enhanced question setup
CreateQuestionRequest request = new CreateQuestionRequest();
request.setReferenceImplementation(teacherSolution);
request.setFunctionName("calculateSum");
request.setFunctionSignature("public int calculateSum(int a, int b)");
request.setProgrammingLanguage("java");
request.setTestTemplate(customTestTemplate); // Optional
```

### Grading Strategy Selection:
```java
// Automatic strategy selection
if (question.hasReferenceImplementation()) {
    enhancedAutoGradingService.performEnhancedGrading(submission);
} else {
    autoGradingService.gradeSubmission(submission); // Fallback
}
```

## API Endpoints

### Enhanced Grading Endpoints:
- `POST /api/admin/enhanced-grading/grade-submission/{id}` - Manually trigger enhanced grading
- `GET /api/admin/enhanced-grading/info` - Get system capabilities
- `GET /api/admin/enhanced-grading/results/{id}` - Get detailed grading results
- `POST /api/admin/enhanced-grading/batch-grade` - Batch re-grade multiple submissions

### Question Management:
- Questions now support additional fields for reference implementations
- Teacher dashboard shows enhanced grading status
- Detailed comparison results available in submission feedback

## Security Considerations

### Data Privacy:
- **Reference implementations**: Only visible to teachers/admins
- **Student code isolation**: Each submission executed in isolated environment
- **Resource limits**: Prevent resource exhaustion attacks

### Access Control:
- Enhanced grading endpoints require TEACHER or ADMIN role
- Reference implementations excluded from student API responses
- Detailed comparison results restricted to appropriate users

## Implementation Status

### Completed Components:
✅ Enhanced Question entity with reference implementation fields
✅ EnhancedAutoGradingService with complete algorithm
✅ Test code generation for Java, Python, C++
✅ Output comparison and scoring logic
✅ Fallback mechanism to traditional grading
✅ Enhanced DTOs for requests/responses
✅ Controller endpoints for enhanced grading
✅ Integration with existing submission workflow

### Future Enhancements:
- [ ] Support for more programming languages
- [ ] Advanced test case generation from reference implementation
- [ ] Plagiarism detection using reference comparison
- [ ] Performance analysis and optimization suggestions
- [ ] AI-powered feedback generation

This enhanced automatic scoring algorithm provides a robust, accurate, and comprehensive grading system that significantly improves upon traditional test-case-only approaches while maintaining backward compatibility and graceful fallback mechanisms.