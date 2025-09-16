# Hướng dẫn Test Hệ thống Auto-Grading với JOBE Server

## 1. Test Cases Mẫu cho Java

### Bài 1: Tính tổng hai số
**Đề bài**: Viết chương trình Java nhận vào hai số nguyên và trả về tổng của chúng.

**Input Format**: Hai số nguyên trên một dòng, cách nhau bởi dấu cách
**Output Format**: Một số nguyên là tổng của hai số đầu vào

**Code mẫu đúng**:
```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a + b);
        scanner.close();
    }
}
```

**Test Cases**:
1. Input: `5 3` → Expected Output: `8`
2. Input: `-2 7` → Expected Output: `5`
3. Input: `0 0` → Expected Output: `0`
4. Input: `1000 2000` → Expected Output: `3000`
5. Input: `-10 -5` → Expected Output: `-15`

### Bài 2: Kiểm tra số chẵn lẻ
**Đề bài**: Viết chương trình kiểm tra số đầu vào là chẵn hay lẻ.

**Input Format**: Một số nguyên
**Output Format**: "EVEN" nếu là số chẵn, "ODD" nếu là số lẻ

**Code mẫu đúng**:
```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        if (n % 2 == 0) {
            System.out.println("EVEN");
        } else {
            System.out.println("ODD");
        }
        scanner.close();
    }
}
```

**Test Cases**:
1. Input: `4` → Expected Output: `EVEN`
2. Input: `7` → Expected Output: `ODD`
3. Input: `0` → Expected Output: `EVEN`
4. Input: `-3` → Expected Output: `ODD`
5. Input: `100` → Expected Output: `EVEN`

## 2. Test Cases Mẫu cho C++

### Bài 1: Tìm số lớn nhất trong hai số
**Đề bài**: Viết chương trình C++ tìm số lớn nhất trong hai số đầu vào.

**Code mẫu đúng**:
```cpp
#include <iostream>
#include <algorithm>
using namespace std;

int main() {
    int a, b;
    cin >> a >> b;
    cout << max(a, b) << endl;
    return 0;
}
```

**Test Cases**:
1. Input: `10 5` → Expected Output: `10`
2. Input: `3 8` → Expected Output: `8`
3. Input: `7 7` → Expected Output: `7`
4. Input: `-5 -2` → Expected Output: `-2`
5. Input: `0 -1` → Expected Output: `0`

### Bài 2: Tính giai thừa
**Đề bài**: Viết chương trình tính giai thừa của số n (n >= 0).

**Code mẫu đúng**:
```cpp
#include <iostream>
using namespace std;

int main() {
    int n;
    cin >> n;
    
    long long factorial = 1;
    for (int i = 1; i <= n; i++) {
        factorial *= i;
    }
    
    cout << factorial << endl;
    return 0;
}
```

**Test Cases**:
1. Input: `5` → Expected Output: `120`
2. Input: `0` → Expected Output: `1`
3. Input: `1` → Expected Output: `1`
4. Input: `4` → Expected Output: `24`
5. Input: `6` → Expected Output: `720`

## 3. Test Cases Mẫu cho Python

### Bài 1: Reverse String
**Đề bài**: Viết chương trình Python đảo ngược chuỗi đầu vào.

**Code mẫu đúng**:
```python
s = input().strip()
print(s[::-1])
```

**Test Cases**:
1. Input: `hello` → Expected Output: `olleh`
2. Input: `Python` → Expected Output: `nohtyP`
3. Input: `12345` → Expected Output: `54321`
4. Input: `a` → Expected Output: `a`
5. Input: `racecar` → Expected Output: `racecar`

### Bài 2: Kiểm tra Palindrome
**Đề bài**: Kiểm tra xem chuỗi đầu vào có phải là palindrome không.

**Code mẫu đúng**:
```python
s = input().strip().lower()
if s == s[::-1]:
    print("YES")
else:
    print("NO")
```

**Test Cases**:
1. Input: `racecar` → Expected Output: `YES`
2. Input: `hello` → Expected Output: `NO`
3. Input: `A` → Expected Output: `YES`
4. Input: `Madam` → Expected Output: `YES`
5. Input: `python` → Expected Output: `NO`

## 4. Code Samples với Lỗi (để test Error Handling)

### Java - Lỗi Compilation
```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a + b;  // Thiếu dấu )
        scanner.close();
    }
}
```

### Java - Lỗi Runtime (Exception)
```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a / b);  // Có thể gây lỗi divide by zero
        scanner.close();
    }
}
```

### C++ - Lỗi Compilation
```cpp
#include <iostream>
using namespace std;

int main() {
    int a, b;
    cin >> a >> b;
    cout << max(a, b) << endl  // Thiếu dấu ;
    return 0;
}
```

### Python - Lỗi Runtime
```python
n = int(input())
print(10 / n)  # Có thể gây lỗi ZeroDivisionError
```

## 5. Kết quả mong đợi khi test

### Test thành công:
```json
{
  "success": true,
  "output": "8\n",
  "error": "",
  "language": "java",
  "executionTime": 1234,
  "memoryUsed": 2048,
  "testResults": [
    {
      "testCaseId": 1,
      "passed": true,
      "actualOutput": "8",
      "expectedOutput": "8",
      "executionTime": 50
    }
  ],
  "passedTests": 5,
  "totalTests": 5,
  "score": 100.0,
  "isCompiled": true
}
```

### Test với lỗi compilation:
```json
{
  "success": false,
  "output": "",
  "error": "",
  "compilationError": "Main.java:7: error: ';' expected\n        System.out.println(a + b\n                                ^\n1 error",
  "language": "java",
  "executionTime": 0,
  "testResults": [],
  "passedTests": 0,
  "totalTests": 5,
  "score": 0.0,
  "isCompiled": false
}
```

### Test với lỗi runtime:
```json
{
  "success": false,
  "output": "",
  "error": "Exception in thread \"main\" java.lang.ArithmeticException: / by zero\n\tat Main.main(Main.java:7)",
  "language": "java",
  "executionTime": 45,
  "testResults": [
    {
      "testCaseId": 1,
      "passed": false,
      "actualOutput": "",
      "expectedOutput": "8",
      "errorMessage": "Runtime Error: ArithmeticException"
    }
  ],
  "passedTests": 0,
  "totalTests": 5,
  "score": 0.0,
  "isCompiled": true
}
```

## 6. Performance Benchmarks

### Thời gian thực thi mong đợi:
- **Java**: 500ms - 2000ms (bao gồm compile time)
- **C++**: 300ms - 1500ms (nhanh hơn Java)
- **Python**: 100ms - 800ms (nhanh nhất cho code đơn giản)

### Memory Usage mong đợi:
- **Java**: 8MB - 32MB (JVM overhead)
- **C++**: 1MB - 8MB (tối ưu memory)
- **Python**: 4MB - 16MB (interpreter overhead)

### Success Rate mong đợi:
- **Code đúng**: 99%+ success rate
- **Code có lỗi compilation**: 0% success rate, nhanh chóng detect lỗi
- **Code có lỗi runtime**: 0% success rate với error message rõ ràng

## 7. Reliability Tests

### Test tải (Load Testing):
- Submit 10 bài cùng lúc
- Submit 1 bài với 50 test cases
- Submit code với infinite loop (timeout handling)
- Submit code với memory leak (memory limit handling)

### Failover Testing:
- Test khi JOBE server down → fallback to local execution
- Test khi cả hai methods fail → proper error reporting