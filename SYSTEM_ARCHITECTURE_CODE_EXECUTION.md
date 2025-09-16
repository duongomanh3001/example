# Hệ Thống Biên Dịch và Thực Thi Code - CScore

## Tổng Quan Kiến Trúc

Hệ thống CScore sử dụng **kiến trúc lai (Hybrid Architecture)** để biên dịch và thực thi code, kết hợp giữa:

1. **Jobe Server** (Ưu tiên) - Server chuyên dụng cho code execution
2. **Local Execution** (Fallback) - Biên dịch trực tiếp trên server backend

## Chi Tiết Cách Thức Hoạt Động

### 1. HybridCodeExecutionService - Điều Phối Chính

```java
@Value("${execution.strategy:hybrid}")
private String executionStrategy; // hybrid, jobe, local

@Value("${jobe.server.enabled:true}")  
private boolean jobeEnabled;
```

**Chiến lược thực thi:**
- **HYBRID** (Mặc định): Thử Jobe trước, nếu fail thì fallback về Local
- **JOBE**: Chỉ dùng Jobe Server
- **LOCAL**: Chỉ dùng Local Compilation

### 2. Jobe Server Integration

**Cấu hình:**
```properties
jobe.server.url=http://localhost:4000
jobe.server.enabled=true
jobe.server.api-key=2AAA7A5F538F4E4B5C4A8B2E9AA2B248FFF
```

**Đặc điểm:**
- ✅ **An toàn cao**: Chạy trong sandbox môi trường
- ✅ **Hiệu năng tốt**: Tối ưu cho code execution
- ✅ **Hỗ trợ đa ngôn ngữ**: C, C++, Java, Python, JavaScript
- ✅ **Giới hạn tài nguyên**: CPU time (30s), Memory (256MB)
- ✅ **API RESTful**: Dễ mở rộng và maintain

**Cách hoạt động:**
1. Gửi request POST đến `/jobe/index.php/restapi/runs`
2. Payload bao gồm: source code, language_id, input, parameters
3. Jobe server compile và execute trong sandbox
4. Trả về kết quả: stdout, stderr, execution time, memory usage

### 3. Local Code Execution (Fallback)

**Đặc điểm:**
- 🔧 **Sử dụng compiler local**: gcc, g++, javac, python
- 📁 **Temporary directories**: Tạo thư mục riêng cho mỗi execution
- ⏰ **Timeout protection**: Compilation (60s), Execution (30s)
- 💾 **Memory limits**: 256MB per process
- 🧹 **Auto cleanup**: Xóa files tạm sau khi hoàn thành

**Quy trình execution:**
1. Tạo unique working directory
2. Write source code to file
3. Compile using appropriate compiler
4. Execute với input (nếu có)
5. Capture output và cleanup

## Ngôn Ngữ Được Hỗ Trợ

### 1. **C Language**
```properties
Language ID: "c"
Compiler: gcc
File extension: .c
Jobe mapping: "c"
```

**Ví dụ execution:**
```c
#include <stdio.h>
#include <string.h>

int countCharacter(const char str[], char key) {
    int count = 0;
    for (int i = 0; str[i] != '\0'; i++) {
        if(str[i] == key) count++;
    }
    return count;
}

int main() {
    char data[] = "Hello";
    char key = 'l';
    printf("%d", countCharacter(data, key));
    return 0;
}
```

### 2. **C++ Language**
```properties
Language ID: "cpp" or "c++"
Compiler: g++
File extension: .cpp
Jobe mapping: "cpp"
```

### 3. **Java**
```properties
Language ID: "java"
Compiler: javac
File extension: .java
Jobe mapping: "java"
Main class: Main
```

### 4. **Python**
```properties
Language ID: "python"
Interpreter: python3
File extension: .py
Jobe mapping: "python3"
```

### 5. **JavaScript/Node.js**
```properties
Language ID: "javascript" or "js"
Runtime: nodejs
File extension: .js
Jobe mapping: "nodejs"
```

## Khả Năng Xử Lý Bài Tập Phức Tạp

### ✅ **Các dạng bài được hỗ trợ:**

1. **Algorithm Problems**
   - Sorting algorithms
   - Search algorithms
   - Dynamic programming
   - Graph algorithms

2. **Data Structure Problems**
   - Arrays, Linked Lists
   - Stacks, Queues
   - Trees, Graphs
   - Hash tables

3. **Mathematical Problems**
   - Number theory
   - Geometry
   - Statistics
   - Matrix operations

4. **String Processing**
   - Pattern matching
   - String manipulation
   - Regular expressions

5. **Object-Oriented Programming**
   - Class definitions
   - Inheritance
   - Polymorphism
   - Encapsulation

### 🔧 **Tính năng nâng cao:**

1. **Multiple Test Cases**
   - Batch execution với nhiều input/output
   - Scoring theo từng test case
   - Hidden test cases

2. **Performance Monitoring**
   - Execution time tracking
   - Memory usage monitoring
   - CPU usage limits

3. **Security Features**
   - Sandbox execution
   - Resource limitations  
   - Safe input/output handling

4. **Error Handling**
   - Compilation errors
   - Runtime errors
   - Timeout handling
   - Memory overflow protection

## Quy Trình Validation Code (Answer Checking)

### 1. **Frontend gửi request:**
```typescript
POST /api/teacher/validate-code
{
  "code": "#include <stdio.h>\n...",  // Combined code
  "language": "c",
  "input": ""
}
```

### 2. **Backend xử lý:**
```java
// TeacherController.validateAnswerCode()
-> HybridCodeExecutionService.executeCodeWithInput()
   -> Thử JobeExecutionService trước
   -> Nếu fail, fallback về CodeExecutionService (local)
```

### 3. **Code được format:**
```c
#include <stdio.h>
#include <string.h>

// Teacher's answer function
int countCharacter(const char str[], char key) {
    // implementation
}

int main() {
    // Test case code
    char data[] = "Hello";
    char key = 'l';
    printf("%d", countCharacter(data, key));
    return 0;
}
```

### 4. **Execution và Response:**
```json
{
  "success": true,
  "output": "2",
  "executionTime": 150,
  "error": null,
  "compilationError": null
}
```

## Performance & Scalability

### **Concurrent Execution:**
- Thread pool: `Runtime.getRuntime().availableProcessors() / 2`
- Async test result saving
- Parallel test case execution

### **Resource Management:**
- CPU time limit: 30 seconds
- Memory limit: 256MB
- Max output length: 10,000 characters
- Compilation timeout: 60 seconds

### **High Availability:**
- Auto fallback từ Jobe về Local
- Health check cho Jobe server
- Error recovery mechanisms

## Debugging và Monitoring

### **Logging Levels:**
```java
log.info("Executing code using strategy: {} for language: {}", strategy, language);
log.warn("Jobe execution failed, falling back to local: {}", e.getMessage());
log.error("Error executing code via Jobe server", e);
```

### **Metrics Tracking:**
- Execution time per request
- Success/failure rates
- Resource usage statistics
- Strategy usage distribution

## Kết Luận

Hệ thống CScore có khả năng:

✅ **Chạy được các bài tập phức tạp** từ cơ bản đến nâng cao
✅ **Hỗ trợ đa ngôn ngữ** với performance tốt  
✅ **An toàn và bảo mật** với sandbox execution
✅ **Scalable và reliable** với fallback mechanisms
✅ **Real-time validation** cho việc tạo đề bài

**Jobe Server** được sử dụng làm engine chính, đảm bảo tính an toàn và hiệu năng cao cho việc thực thi code trong môi trường giáo dục.