# Simple JOBE Server Test Script

Write-Host "="*60 -ForegroundColor Cyan
Write-Host "TESTING JOBE SERVER FUNCTIONALITY" -ForegroundColor Cyan  
Write-Host "="*60 -ForegroundColor Cyan

# Test 1: Java Code
Write-Host "`n1. Testing Java - Sum of two numbers" -ForegroundColor Yellow

$javaCode = 'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a + b);
        scanner.close();
    }
}'

$body = @{
    run_spec = @{
        language_id = "java"
        sourcefilename = "Main.java"
        sourcecode = $javaCode
        input = "5 3"
    }
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" -Method PUT -ContentType "application/json" -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} -Body $body
    
    Write-Host "  ✓ Response received" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"
    Write-Host "  Expected: 8, Got: '$($response.stdout.Trim())'"
    
    if ($response.outcome -eq 15 -and $response.stdout.Trim() -eq "8") {
        Write-Host "  RESULT: PASSED ✓" -ForegroundColor Green
    } else {
        Write-Host "  RESULT: FAILED ✗" -ForegroundColor Red
        if ($response.cmpinfo) { Write-Host "  Compile Error: $($response.cmpinfo)" }
        if ($response.stderr) { Write-Host "  Runtime Error: $($response.stderr)" }
    }
}
catch {
    Write-Host "  ✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Python Code  
Write-Host "`n2. Testing Python - Reverse string" -ForegroundColor Yellow

$pythonCode = 's = input().strip()
print(s[::-1])'

$body = @{
    run_spec = @{
        language_id = "python3"
        sourcefilename = "main.py"
        sourcecode = $pythonCode
        input = "hello"
    }
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" -Method PUT -ContentType "application/json" -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} -Body $body
    
    Write-Host "  ✓ Response received" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"
    Write-Host "  Expected: olleh, Got: '$($response.stdout.Trim())'"
    
    if ($response.outcome -eq 15 -and $response.stdout.Trim() -eq "olleh") {
        Write-Host "  RESULT: PASSED ✓" -ForegroundColor Green
    } else {
        Write-Host "  RESULT: FAILED ✗" -ForegroundColor Red
        if ($response.stderr) { Write-Host "  Error: $($response.stderr)" }
    }
}
catch {
    Write-Host "  ✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: C++ Code
Write-Host "`n3. Testing C++ - Max of two numbers" -ForegroundColor Yellow

$cppCode = '#include <iostream>
#include <algorithm>
using namespace std;

int main() {
    int a, b;
    cin >> a >> b;
    cout << max(a, b) << endl;
    return 0;
}'

$body = @{
    run_spec = @{
        language_id = "cpp"
        sourcefilename = "main.cpp"
        sourcecode = $cppCode
        input = "10 5"
    }
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" -Method PUT -ContentType "application/json" -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} -Body $body
    
    Write-Host "  ✓ Response received" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"
    Write-Host "  Expected: 10, Got: '$($response.stdout.Trim())'"
    
    if ($response.outcome -eq 15 -and $response.stdout.Trim() -eq "10") {
        Write-Host "  RESULT: PASSED ✓" -ForegroundColor Green
    } else {
        Write-Host "  RESULT: FAILED ✗" -ForegroundColor Red
        if ($response.cmpinfo) { Write-Host "  Compile Error: $($response.cmpinfo)" }
        if ($response.stderr) { Write-Host "  Runtime Error: $($response.stderr)" }
    }
}
catch {
    Write-Host "  ✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Compilation Error Test
Write-Host "`n4. Testing Java - Compilation Error" -ForegroundColor Yellow

$javaErrorCode = 'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a + b;
        scanner.close();
    }
}'

$body = @{
    run_spec = @{
        language_id = "java"
        sourcefilename = "Main.java"
        sourcecode = $javaErrorCode
        input = "5 3"
    }
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "http://localhost:4000/jobe/index.php/restapi/runs" -Method PUT -ContentType "application/json" -Headers @{"X-API-KEY"="2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF"} -Body $body
    
    Write-Host "  ✓ Response received" -ForegroundColor Green
    Write-Host "  Run ID: $($response.run_id)"
    Write-Host "  Outcome: $($response.outcome)"
    
    if ($response.outcome -eq 11) {
        Write-Host "  RESULT: COMPILATION ERROR DETECTED ✓" -ForegroundColor Green
        Write-Host "  Error: $($response.cmpinfo)" -ForegroundColor Yellow
    } else {
        Write-Host "  RESULT: UNEXPECTED OUTCOME ✗" -ForegroundColor Red
        Write-Host "  Expected outcome 11 (compilation error), got $($response.outcome)"
    }
}
catch {
    Write-Host "  ✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n" + "="*60 -ForegroundColor Cyan
Write-Host "JOBE SERVER TEST COMPLETED" -ForegroundColor Cyan
Write-Host "="*60 -ForegroundColor Cyan

Write-Host "`nOutcome Codes Reference:" -ForegroundColor White
Write-Host "  15 = Success"
Write-Host "  11 = Compilation Error"
Write-Host "  12 = Runtime Error"
Write-Host "  13 = Time Limit Exceeded"
Write-Host "  17 = Memory Limit Exceeded"