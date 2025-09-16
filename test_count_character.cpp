#include <iostream>
#include <cstring>
#include <vector>
#include <string>
using namespace std;

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
    // Test case 1: "Hello l"
    char data1[] = "Hello";
    char key1 = 'l';
    cout << countCharacter(data1, key1) << endl;
    
    // Test case 2: "HelloHelloHello o"
    char data2[] = "HelloHelloHello";
    char key2 = 'o';
    cout << countCharacter(data2, key2) << endl;
    
    // Test case 3: "" o
    char data3[] = "";
    char key3 = 'o';
    cout << countCharacter(data3, key3) << endl;
    
    return 0;
}