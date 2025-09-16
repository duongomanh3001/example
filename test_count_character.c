// Test file to verify the C wrapper functionality
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

// Student's countCharacter function
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

    // Parse input: "string character" format
    char str[500];
    char key;
    int i = 0, j = 0;
    
    // Skip leading whitespace
    while (i < len && input[i] == ' ') i++;
    
    // Check if string starts with quote
    if (i < len && input[i] == '"') {
        i++; // Skip opening quote
        
        // Handle escaped quotes for empty string case
        if (i < len - 1 && input[i] == '\\' && input[i+1] == '"') {
            // This handles \"\" pattern for empty string
            while (i < len && !(input[i] == '"' && input[i-1] != '\\')) {
                if (input[i] == '\\' && i+1 < len && input[i+1] == '"') {
                    i += 2; // Skip escaped quote
                } else {
                    i++;
                }
            }
            str[0] = '\0'; // Empty string
        } else {
            // Extract string content until closing quote
            while (i < len && input[i] != '"') {
                str[j++] = input[i++];
            }
            str[j] = '\0';
        }
        
        if (i < len) i++; // Skip closing quote
    } else {
        // No quotes, extract until last space
        int last_space = -1;
        for (int k = len - 1; k >= i; k--) {
            if (input[k] == ' ') {
                last_space = k;
                break;
            }
        }
        
        if (last_space == -1) {
            printf("Error: Invalid input format");
            return 1;
        }
        
        // Extract string part
        while (i < last_space) {
            str[j++] = input[i++];
        }
        str[j] = '\0';
    }
    
    // Skip spaces before character
    while (i < len && input[i] == ' ') i++;
    
    // Extract character
    if (i < len) {
        key = input[i];
    } else {
        printf("Error: No character parameter found");
        return 1;
    }
    
    // Call function and print result
    int result = countCharacter(str, key);
    printf("%d", result);
    return 0;
}