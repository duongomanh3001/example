# Test JOBE Server với PowerShell

# Test case 1: Java - Tính tổng hai số
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

$body = @{
    run_spec = @{
        language_id = "java"
        sourcefilename = "Main.java"
        sourcecode = $javaCode
        input = "5 3"
    }
} | ConvertTo-Json -Depth 10

Write-Host "Testing Java code execution..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" `
                                  -Method PUT `
                                  -ContentType "application/json" `
                                  -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} `
                                  -Body $body
    
    Write-Host "✓ Java Test Result:" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"  
    Write-Host "  Output: '$($response.stdout.Trim())'"
    if ($response.stderr) { Write-Host "  Error: $($response.stderr)" -ForegroundColor Red }
    if ($response.cmpinfo) { Write-Host "  Compile Info: $($response.cmpinfo)" -ForegroundColor Yellow }
    
    if ($response.outcome -eq 15 -and $response.stdout.Trim() -eq "8") {
        Write-Host "  STATUS: PASSED ✓" -ForegroundColor Green
    } else {
        Write-Host "  STATUS: FAILED ✗" -ForegroundColor Red
    }
}
catch {
    Write-Host "✗ Java Test Failed: $_" -ForegroundColor Red
}

Write-Host "`n" + "="*50 + "`n"

# Test case 2: C++ - Tìm max của hai số
$cppCode = @"
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

$body = @{
    run_spec = @{
        language_id = "cpp" 
        sourcefilename = "main.cpp"
        sourcecode = $cppCode
        input = "10 5"
    }
} | ConvertTo-Json -Depth 10

Write-Host "Testing C++ code execution..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" `
                                  -Method PUT `
                                  -ContentType "application/json" `
                                  -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} `
                                  -Body $body
    
    Write-Host "✓ C++ Test Result:" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"
    Write-Host "  Output: '$($response.stdout.Trim())'"
    if ($response.stderr) { Write-Host "  Error: $($response.stderr)" -ForegroundColor Red }
    if ($response.cmpinfo) { Write-Host "  Compile Info: $($response.cmpinfo)" -ForegroundColor Yellow }
    
    if ($response.outcome -eq 15 -and $response.stdout.Trim() -eq "10") {
        Write-Host "  STATUS: PASSED ✓" -ForegroundColor Green
    } else {
        Write-Host "  STATUS: FAILED ✗" -ForegroundColor Red
    }
}
catch {
    Write-Host "✗ C++ Test Failed: $_" -ForegroundColor Red
}

Write-Host "`n" + "="*50 + "`n"

# Test case 3: Python - Reverse string
$pythonCode = @"
s = input().strip()
print(s[::-1])
"@

$body = @{
    run_spec = @{
        language_id = "python3"
        sourcefilename = "main.py"
        sourcecode = $pythonCode
        input = "hello"
    }
} | ConvertTo-Json -Depth 10

Write-Host "Testing Python code execution..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" `
                                  -Method PUT `
                                  -ContentType "application/json" `
                                  -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} `
                                  -Body $body
    
    Write-Host "✓ Python Test Result:" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"
    Write-Host "  Output: '$($response.stdout.Trim())'"
    if ($response.stderr) { Write-Host "  Error: $($response.stderr)" -ForegroundColor Red }
    if ($response.cmpinfo) { Write-Host "  Compile Info: $($response.cmpinfo)" -ForegroundColor Yellow }
    
    if ($response.outcome -eq 15 -and $response.stdout.Trim() -eq "olleh") {
        Write-Host "  STATUS: PASSED ✓" -ForegroundColor Green
    } else {
        Write-Host "  STATUS: FAILED ✗" -ForegroundColor Red
    }
}
catch {
    Write-Host "✗ Python Test Failed: $_" -ForegroundColor Red
}

Write-Host "`n" + "="*50 + "`n"

# Test case 4: Java với lỗi compilation
$javaErrorCode = @"
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a + b;  // Missing closing parenthesis
        scanner.close();
    }
}
"@

$body = @{
    run_spec = @{
        language_id = "java"
        sourcefilename = "Main.java" 
        sourcecode = $javaErrorCode
        input = "5 3"
    }
} | ConvertTo-Json -Depth 10

Write-Host "Testing Java compilation error handling..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" `
                                  -Method PUT `
                                  -ContentType "application/json" `
                                  -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} `
                                  -Body $body
    
    Write-Host "✓ Java Error Test Result:" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"
    Write-Host "  Output: '$($response.stdout)'"
    if ($response.stderr) { Write-Host "  Error: $($response.stderr)" -ForegroundColor Red }
    if ($response.cmpinfo) { 
        Write-Host "  Compile Error: $($response.cmpinfo)" -ForegroundColor Red 
    }
    
    if ($response.outcome -eq 11) {
        Write-Host "  STATUS: COMPILATION ERROR DETECTED ✓" -ForegroundColor Green
    } else {
        Write-Host "  STATUS: UNEXPECTED OUTCOME ✗" -ForegroundColor Red
    }
}
catch {
    Write-Host "✗ Java Error Test Failed: $_" -ForegroundColor Red
}

Write-Host "`n" + "="*80
Write-Host "JOBE SERVER TEST SUMMARY" -ForegroundColor Cyan
Write-Host "="*80