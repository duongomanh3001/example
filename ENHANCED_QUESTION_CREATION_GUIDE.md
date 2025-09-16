# Enhanced Question Creation System - Feature Guide

## Overview
The enhanced question creation system for lecturers provides comprehensive tools for creating, testing, and validating programming exercises with automated code execution and grading capabilities.

## New Features

### 1. Answer Code Input
**Location**: Answer section in Programming Questions
**Purpose**: Allows lecturers to enter their correct solution code that will be used for validation

**Usage**:
- Write the complete function implementation
- This code will be combined with test cases for execution
- Used to automatically generate expected outputs

**Example**:
```c
int countCharacter(const char str[], char key) {
    int count = 0;
    for (int i = 0; str[i] != '\0'; i++) {
        if (str[i] == key) {
            count++;
        }
    }
    return count;
}
```

### 2. Validate on Save Checkbox
**Purpose**: Enables automatic generation of expected outputs by running the lecturer's answer code

**How it works**:
1. Check "Validate on Save" checkbox
2. Enter your answer code in the Answer section
3. Create test cases with test code
4. Click "Chạy thử nghiệm" button
5. System automatically runs your code and generates expected outputs

**Benefits**:
- Eliminates manual output calculation
- Reduces human error in expected results
- Ensures consistency between test cases and answers

### 3. Answer Box Preload (Student Template)
**Location**: Answer box preload section
**Purpose**: Provides starter code template for students with TODO comments

**Features**:
- Pre-written function signatures
- TODO comments to guide students
- Helpful hints and structure
- Reduces setup time for students

**Example Template**:
```c
// TODO: Implement this function
function countCharacter(str, key) {
    // Your code here
    // Hint: Use a loop to iterate through the string
    // Count occurrences of the key character
}
```

### 4. Enhanced Test Case Creation
**New Fields**:
- **Test Code**: Code segment that calls your function with specific parameters
- **Use as Example**: Checkbox to make test case visible to students
- **Standard Input**: Traditional input for the program
- **Expected Output**: Auto-generated or manually entered expected result

**Test Code Example**:
```c
char data[] = "Hello";
char key = 'l';
printf("%d", countCharacter(data, key));
```

### 5. Visibility Controls
**Use as Example**: When checked, students can see this test case as a reference
**Hide rest if fail**: When checked, remaining test cases are hidden if this one fails

**Mark & Ordering**:
- **Mark**: Points awarded for this test case
- **Ordering**: Sequence order of test case execution

## Workflow Process

### For Lecturers:
1. **Create Basic Question**
   - Enter question title and description
   - Select "Programming" question type

2. **Write Answer Code**
   - Enter your complete solution in the Answer section
   - Enable "Validate on save" if you want auto-generation

3. **Create Student Template**
   - Write starter code with TODO comments in "Answer box preload"
   - Provide helpful hints and structure

4. **Create Test Cases**
   - Write test code that calls your function
   - Add any standard input if needed
   - Configure visibility settings (Use as Example)
   - Set point values and ordering

5. **Validate & Generate Outputs**
   - Click "Chạy thử nghiệm" to run validation
   - System generates expected outputs automatically
   - Review and adjust if needed

### For Students (Resulting Experience):
1. Students see the question description and requirements
2. Code editor is pre-populated with your template code
3. Students see example test cases (marked as "Use as Example")
4. Students complete the TODO sections
5. System runs their code against all test cases
6. Automatic grading based on test case results

## API Integration

The system integrates with your existing backend through:
- **Code Execution Service**: `/api/teacher/validate-code`
- **Assignment Service**: Enhanced with validation methods
- **Jobe Integration**: Uses existing code execution infrastructure

## Technical Implementation

### New TypeScript Interfaces:
```typescript
interface EnhancedTestCase extends TestCaseRequest {
  testCode?: string;        // Code to run/test
  useAsExample?: boolean;   // Show to students
  isValidated?: boolean;    // Auto-generated output
}

interface Question {
  // ... existing fields
  answerCode?: string;      // Lecturer's solution
  starterCode?: string;     // Student template
  validateOnSave?: boolean; // Auto-generate outputs
}
```

### Service Methods:
- `validateAnswerCode()`: Single test case validation
- `validateMultipleTestCases()`: Batch validation
- Enhanced error handling and result processing

## Benefits

1. **Time Saving**: Automatic output generation reduces manual work
2. **Accuracy**: Eliminates human error in expected results
3. **Consistency**: Ensures all test cases work with the same solution
4. **Student Experience**: Better guidance with templates and examples
5. **Scalability**: Easy to create multiple similar questions
6. **Quality Assurance**: Test your own code before publishing

## Best Practices

1. **Test Your Code First**: Always validate your answer code before publishing
2. **Provide Clear Examples**: Use "Use as Example" for at least one test case
3. **Progressive Difficulty**: Order test cases from simple to complex
4. **Helpful Templates**: Include meaningful TODO comments and hints
5. **Error Handling**: Test edge cases and error conditions
6. **Documentation**: Write clear question descriptions with examples

## Troubleshooting

**Common Issues**:
- **Validation Fails**: Check syntax and logic in answer code
- **No Output Generated**: Ensure test code calls the function correctly
- **Compilation Errors**: Verify code syntax and variable names
- **Wrong Output**: Review algorithm logic and test data

**Debug Steps**:
1. Test answer code manually first
2. Check test code syntax
3. Verify function names match exactly
4. Review input/output format expectations
5. Check for trailing whitespace or formatting issues

## Integration with Existing Features

This enhancement builds upon your existing:
- Course management system
- Assignment creation workflow
- Code execution infrastructure (Jobe)
- Student submission system
- Auto-grading capabilities

The new features seamlessly integrate without breaking existing functionality.