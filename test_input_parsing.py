#!/usr/bin/env python3
"""
Test script to verify the improved input parsing logic for the countCharacter function.
This simulates the C wrapper logic in Python to validate the parsing works correctly.
"""

def count_character(s, key):
    """Student's countCharacter function equivalent"""
    return s.count(key)

def parse_input(input_str):
    """
    Parse input string to extract string and character parameters.
    Handles formats like:
    - "Hello l" -> ("Hello", 'l')
    - "HelloHelloHello o" -> ("HelloHelloHello", 'o') 
    - '"" o' -> ("", 'o')
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
    # 1. "Hello" -> Hello
    # 2. "\"\"" -> empty string (escaped quotes)
    # 3. "" -> empty string  
    # 4. Hello -> Hello (no quotes)
    
    # Check for quoted strings
    if len(string_part) >= 2 and string_part[0] == '"' and string_part[-1] == '"':
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

def test_input_parsing():
    """Test the input parsing with the three test cases"""
    test_cases = [
        ("Hello l", "Hello", 'l', 2),
        ("HelloHelloHello o", "HelloHelloHello", 'o', 3), 
        ('"" o', "", 'o', 0)
    ]
    
    print("Testing improved input parsing logic:")
    print("=" * 50)
    
    all_passed = True
    
    for i, (input_str, expected_str, expected_char, expected_count) in enumerate(test_cases, 1):
        print(f"\nTest Case {i}:")
        print(f"  Input: '{input_str}'")
        
        try:
            # Parse the input
            parsed_str, parsed_char = parse_input(input_str)
            
            print(f"  Parsed string: '{parsed_str}'")
            print(f"  Parsed char: '{parsed_char}'")
            print(f"  Expected string: '{expected_str}'")
            print(f"  Expected char: '{expected_char}'")
            
            # Check if parsing is correct
            if parsed_str == expected_str and parsed_char == expected_char:
                # Calculate result
                result = count_character(parsed_str, parsed_char)
                print(f"  Function result: {result}")
                print(f"  Expected result: {expected_count}")
                
                if result == expected_count:
                    print(f"  Status: ✅ PASS")
                else:
                    print(f"  Status: ❌ FAIL (wrong function result)")
                    all_passed = False
            else:
                print(f"  Status: ❌ FAIL (parsing error)")
                all_passed = False
                
        except Exception as e:
            print(f"  Status: ❌ ERROR ({e})")
            all_passed = False
    
    print("\n" + "=" * 50)
    if all_passed:
        print("🎉 All tests PASSED! The input parsing logic works correctly.")
    else:
        print("❌ Some tests FAILED. The input parsing logic needs fixes.")
    
    return all_passed

if __name__ == "__main__":
    test_input_parsing()