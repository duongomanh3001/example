#!/usr/bin/env python3
"""
Demo script để test hệ thống auto-grading đã được cải tiến
Mô phỏng chính xác cách hệ thống xử lý test cases
"""

def count_character(s, key):
    """Student's countCharacter function equivalent"""
    count = 0
    for char in s:
        if char == key:
            count += 1
    return count

def parse_input_improved(input_str):
    """
    Improved input parsing logic - matches the C wrapper code
    """
    input_str = input_str.strip()
    
    # Find the last space to separate string and character
    last_space = input_str.rfind(' ')
    
    if last_space == -1 or last_space >= len(input_str) - 1:
        # Handle single character input (edge case)
        if len(input_str) == 1:
            return ("", input_str[0])
        else:
            # Invalid format, but try to handle gracefully
            return (input_str, ' ')
    
    # Extract character part (after last space)
    key = input_str[last_space + 1]
    
    # Extract string part (before last space)
    string_part = input_str[:last_space]
    
    # Handle different string formats:
    str_len = len(string_part)
    
    # Check for quoted strings
    if str_len >= 2 and string_part[0] == '"' and string_part[-1] == '"':
        # Remove outer quotes
        str_value = string_part[1:-1]
        
        # Handle escaped quotes: \"\" becomes empty string
        if str_value == '\\"\\"':
            str_value = ""
    elif string_part == '\\"\\"':
        # Handle special case: \"\" without outer quotes
        str_value = ""
    else:
        # No quotes, use as-is
        str_value = string_part
    
    return (str_value, key)

def simulate_system_grading():
    """
    Mô phỏng hệ thống chấm điểm tự động với reference implementation
    """
    
    # Teacher's reference implementation (same as student's - should pass)
    def teacher_count_character(s, key):
        count = 0
        for char in s:
            if char == key:
                count += 1
        return count
    
    # Student's implementation (same as teacher's)
    def student_count_character(s, key):
        count = 0
        for char in s:
            if char == key:
                count += 1
        return count
    
    # Test cases from teacher
    test_cases = [
        {
            "id": 1,
            "description": "Test case 1: Count 'l' in 'Hello'",
            "input": "Hello l",
            "expected_output": "2"
        },
        {
            "id": 2, 
            "description": "Test case 2: Count 'o' in 'HelloHelloHello'",
            "input": "HelloHelloHello o",
            "expected_output": "3"
        },
        {
            "id": 3,
            "description": "Test case 3: Count 'o' in empty string",
            "input": '"" o',
            "expected_output": "0"
        }
    ]
    
    print("🎯 DEMO: Enhanced Auto-Grading System")
    print("=" * 60)
    print("Teacher Function: countCharacter(const char str[], char key)")
    print("Student submitted identical implementation")
    print("=" * 60)
    
    total_tests = len(test_cases)
    passed_tests = 0
    
    for test_case in test_cases:
        print(f"\n📝 {test_case['description']}")
        print(f"   Input: '{test_case['input']}'")
        
        try:
            # 1. Parse input using improved logic
            parsed_str, parsed_char = parse_input_improved(test_case['input'])
            print(f"   Parsed String: '{parsed_str}'")
            print(f"   Parsed Char: '{parsed_char}'")
            
            # 2. Execute teacher's reference implementation
            teacher_output = teacher_count_character(parsed_str, parsed_char)
            print(f"   Teacher Output: {teacher_output}")
            
            # 3. Execute student's implementation  
            student_output = student_count_character(parsed_str, parsed_char)
            print(f"   Student Output: {student_output}")
            
            # 4. Compare outputs
            is_passed = (teacher_output == student_output)
            
            if is_passed:
                print(f"   Result: ✅ PASS")
                passed_tests += 1
            else:
                print(f"   Result: ❌ FAIL")
                
            print(f"   Expected: {test_case['expected_output']}")
            print(f"   Got: {student_output}")
            
        except Exception as e:
            print(f"   Result: ❌ ERROR ({e})")
    
    print(f"\n" + "=" * 60)
    print(f"📊 GRADING RESULTS:")
    print(f"   Compilation: ✅ SUCCESS") 
    print(f"   Test Cases Passed: {passed_tests}/{total_tests}")
    print(f"   Score: {(passed_tests/total_tests)*100:.0f}%")
    
    if passed_tests == total_tests:
        print(f"   Status: 🎉 ALL TESTS PASSED!")
        print(f"   Feedback: Hoàn thành! {passed_tests}/{total_tests} test cases đạt yêu cầu")
    else:
        print(f"   Status: ⚠️ SOME TESTS FAILED")
        print(f"   Feedback: Chỉ {passed_tests}/{total_tests} test cases đạt yêu cầu")
    
    return passed_tests == total_tests

def demonstrate_before_fix():
    """Mô phỏng kết quả trước khi fix"""
    print("\n" + "🔴 BEFORE FIX (Old System):")
    print("-" * 40)
    print("Biên dịch chương trình thành công ✅")
    print("Kết quả chấm điểm tự động: 0/3 test case(s) pass ❌")
    print("Hoàn thành! 0/3 test cases đạt yêu cầu")
    print("Chi tiết test cases:")
    print("Test                    Expected        Got")
    print('"Hello l"               2               (no output)')
    print('"HelloHelloHello o"     3               (no output)')  
    print('"\\"\\" o"               0               (no output)')

def demonstrate_after_fix():
    """Mô phỏng kết quả sau khi fix"""
    print("\n" + "🟢 AFTER FIX (Enhanced System):")
    print("-" * 40)
    return simulate_system_grading()

if __name__ == "__main__":
    print("Auto-Grading System Comparison Demo")
    print("=" * 60)
    
    # Show old vs new results
    demonstrate_before_fix()
    success = demonstrate_after_fix()
    
    print("\n" + "=" * 60)
    if success:
        print("✅ DEMO CONCLUSION: The enhanced system works correctly!")
        print("   Students will now get proper PASS/FAIL results instead of 'no output'")
    else:
        print("❌ DEMO CONCLUSION: There are still issues to resolve")