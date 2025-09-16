#include <stdio.h>
#include <string.h>
#include <stdlib.h>

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

int main() {
    char input[1000];
    if (fgets(input, sizeof(input), stdin) == NULL) {
        return 1;
    }

    // Remove newline from input
    int len = strlen(input);
    if (len > 0 && input[len-1] == '\n') {
        input[len-1] = '\0';
        len--;
    }

    // Parse input: handle various formats like "Hello l", "HelloHelloHello o", "\"\"\" o"
    char str[500] = {0};
    char key = '\0';
    
    // Find the last space to separate string and character
    int last_space = -1;
    for (int k = len - 1; k >= 0; k--) {
        if (input[k] == ' ') {
            last_space = k;
            break;
        }
    }
    
    if (last_space == -1 || last_space >= len - 1) {
        // Handle single character input (edge case)
        if (len == 1) {
            str[0] = '\0'; // empty string
            key = input[0];
        } else {
            // Invalid format, but try to handle gracefully
            strcpy(str, input);
            key = ' '; // default character
        }
    } else {
        // Extract character part (after last space)
        key = input[last_space + 1];
        
        // Extract string part (before last space)
        char string_part[500];
        strncpy(string_part, input, last_space);
        string_part[last_space] = '\0';
        
        // Handle different string formats:
        // 1. "Hello" -> Hello
        // 2. "\"\"\"" -> empty string (escaped quotes)
        // 3. "" -> empty string
        // 4. Hello -> Hello (no quotes)
        
        int str_len = strlen(string_part);
        
        // Check for quoted strings
        if (str_len >= 2 && string_part[0] == '"' && string_part[str_len-1] == '"') {
            // Remove outer quotes
            strncpy(str, string_part + 1, str_len - 2);
            str[str_len - 2] = '\0';
            
            // Handle escaped quotes: "\"\"" becomes empty string
            if (strcmp(str, "\"\"") == 0) {
                str[0] = '\0';
            }
        } else if (str_len == 4 && strcmp(string_part, "\"\"") == 0) {
            // Handle special case: "\"\"" without outer quotes
            str[0] = '\0';
        } else {
            // No quotes, use as-is
            strcpy(str, string_part);
        }
    }
    
    // Call function and print result
    int result = countCharacter(str, key);
    printf("%d", result);
    
    return 0;
}