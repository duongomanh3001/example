/*
 * Test case to validate the code wrapper functionality
 * This simulates the exact scenario from the user's problem
 */

// Student's function code (what they actually submit):
int countCharacter(const char str[], char key)
{
    int count = 0;
    for (int i = 0; str[i] != '\0'; i++)
    {
        if(str[i] == key)
            count++;
    }
    return count;
}

/*
 * Expected wrapped output after CodeWrapperService processing:
 * This should generate a complete C program with main() function
 * that can handle the test case inputs:
 * 
 * Test Cases:
 * 1. Input: "Hello l" -> Expected: 2
 * 2. Input: "HelloHelloHello o" -> Expected: 3  
 * 3. Input: "\"\" o" -> Expected: 0
 */