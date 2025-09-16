# JOBE Server Direct Testing Script

## Cách test trực tiếp với JOBE Server bằng curl

### 1. Kiểm tra JOBE Server có hoạt động không

```bash
# Windows PowerShell
curl -X GET "http://localhost:4000" -H "Content-Type: application/json"

# Kết quả mong đợi: "Jobe: a jobish object entity"
```

### 2. Lấy danh sách ngôn ngữ được hỗ trợ

```bash
# Windows PowerShell
curl -X GET "http://localhost:4000/jobe/index.php/restapi/languages" -H "Content-Type: application/json"
```

### 3. Test chạy code Java

```bash
# Windows PowerShell
$body = @{
    run_spec = @{
        language_id = "java"
        sourcefilename = "Main.java"
        sourcecode = @"
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
"@
        input = "5 3"
    }
} | ConvertTo-Json -Depth 10

curl -X PUT "http://localhost:4000/jobe/index.php/restapi/runs" `
     -H "Content-Type: application/json" `
     -H "X-API-KEY: 2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF" `
     -d $body
```

### 4. Test chạy code C++

```bash
# Windows PowerShell  
$body = @{
    run_spec = @{
        language_id = "cpp"
        sourcefilename = "main.cpp"
        sourcecode = @"
#include <iostream>
#include <algorithm>
using namespace std;

int main() {
    int a, b;
    cin >> a >> b;
    cout << max(a, b) << endl;
    return 0;
}
"@
        input = "10 5"
    }
} | ConvertTo-Json -Depth 10

curl -X PUT "http://localhost:4000/jobe/index.php/restapi/runs" `
     -H "Content-Type: application/json" `
     -H "X-API-KEY: 2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF" `
     -d $body
```

### 5. Test chạy code Python

```bash
# Windows PowerShell
$body = @{
    run_spec = @{
        language_id = "python3"
        sourcefilename = "main.py" 
        sourcecode = @"
s = input().strip()
print(s[::-1])
"@
        input = "hello"
    }
} | ConvertTo-Json -Depth 10

curl -X PUT "http://localhost:4000/jobe/index.php/restapi/runs" `
     -H "Content-Type: application/json" `
     -H "X-API-KEY: 2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF" `
     -d $body
```

## Kết quả mong đợi

### Successful Response:
```json
{
  "run_id": "12345",
  "outcome": 15,
  "cmpinfo": "",
  "stdout": "8\n",
  "stderr": "",
  "files": []
}
```

### Compilation Error Response:
```json
{
  "run_id": "12346", 
  "outcome": 11,
  "cmpinfo": "Main.java:7: error: ';' expected\n        System.out.println(a + b\n                                ^\n1 error\n",
  "stdout": "",
  "stderr": "",
  "files": []
}
```

### Runtime Error Response:
```json
{
  "run_id": "12347",
  "outcome": 12, 
  "cmpinfo": "",
  "stdout": "",
  "stderr": "Exception in thread \"main\" java.lang.ArithmeticException: / by zero\n\tat Main.main(Main.java:7)\n",
  "files": []
}
```

## Outcome Codes

- **15**: Successful execution
- **11**: Compilation error
- **12**: Runtime error  
- **13**: Time limit exceeded
- **17**: Memory limit exceeded
- **19**: Illegal function call
- **20**: Other error

## Advanced Testing với Multiple Test Cases

### PowerShell Script để test multiple cases

```powershell
# test-multiple-cases.ps1

# Array of test cases
$testCases = @(
    @{ input = "5 3"; expected = "8" },
    @{ input = "-2 7"; expected = "5" },
    @{ input = "0 0"; expected = "0" },
    @{ input = "1000 2000"; expected = "3000" },
    @{ input = "-10 -5"; expected = "-15" }
)

$javaCode = @"
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
"@

$passedTests = 0
$totalTests = $testCases.Count

foreach ($testCase in $testCases) {
    $body = @{
        run_spec = @{
            language_id = "java"
            sourcefilename = "Main.java" 
            sourcecode = $javaCode
            input = $testCase.input
        }
    } | ConvertTo-Json -Depth 10
    
    $response = curl -X PUT "http://localhost:4000/jobe/index.php/restapi/runs" `
                     -H "Content-Type: application/json" `
                     -H "X-API-KEY: 2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF" `
                     -d $body | ConvertFrom-Json
    
    $actualOutput = $response.stdout.Trim()
    $expectedOutput = $testCase.expected
    
    if ($actualOutput -eq $expectedOutput -and $response.outcome -eq 15) {
        Write-Host "✓ Test PASSED - Input: $($testCase.input), Expected: $expectedOutput, Got: $actualOutput" -ForegroundColor Green
        $passedTests++
    } else {
        Write-Host "✗ Test FAILED - Input: $($testCase.input), Expected: $expectedOutput, Got: $actualOutput, Outcome: $($response.outcome)" -ForegroundColor Red
        if ($response.cmpinfo) { Write-Host "Compile Error: $($response.cmpinfo)" -ForegroundColor Red }
        if ($response.stderr) { Write-Host "Runtime Error: $($response.stderr)" -ForegroundColor Red }
    }
}

$score = ($passedTests / $totalTests) * 100
Write-Host "`nTest Summary: $passedTests/$totalTests passed (Score: $score%)" -ForegroundColor Cyan
```

## Cách chạy script

1. Mở PowerShell as Administrator
2. Chạy lệnh: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser` (nếu cần)
3. Save script trên thành file `test-multiple-cases.ps1`
4. Chạy: `.\test-multiple-cases.ps1`

## Performance Testing

### Test thời gian thực thi
```powershell
$startTime = Get-Date

# ... your test code here ...

$endTime = Get-Date
$duration = ($endTime - $startTime).TotalMilliseconds
Write-Host "Execution Time: $duration ms"
```

### Test concurrent execution
```powershell
# Chạy 5 tests cùng lúc
1..5 | ForEach-Object -Parallel {
    # Test code here
} -ThrottleLimit 5
```