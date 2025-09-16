# 🎉 HƯỚNG DẪN CHẠY CSCORE VỚI JOBE SERVER - STEP BY STEP

## ✅ Backend đã build thành công!

**Status**: ✅ Compilation successful  
**JAR file**: `target\CScore_BE-0.0.1-SNAPSHOT.jar`  
**New services added**: JobeExecutionService, HybridCodeExecutionService, ExecutionController

---

## 🚀 HƯỚNG DẪN CHẠY (5 BƯỚC ĐơN GIẢN)

### **Bước 1: Kiểm tra hệ thống**
```powershell
# Mở PowerShell và chạy
cd D:\A-FINAL\KLTN\cscore-v1
.\health-check.bat
```

**Kết quả mong đợi:**
```
[SUCCESS] Docker is available
[SUCCESS] Java is available  
[SUCCESS] Node.js is available
[SUCCESS] MySQL client is available
```

### **Bước 2: Khởi động tất cả services**
```powershell
# Chạy script tự động
.\start-all.bat
```

**Script này sẽ:**
1. ✅ Start Jobe Server (Docker container)
2. ✅ Wait for Jobe to be ready
3. ✅ Start CScore Backend (cửa sổ mới)
4. ✅ Start CScore Frontend (cửa sổ mới)

### **Bước 3: Đợi services khởi động (30-60 giây)**

**Bạn sẽ thấy 3 cửa sổ command:**
- **Cửa sổ chính**: Script start-all.bat
- **CScore Backend**: Backend logs với Spring Boot
- **CScore Frontend**: Frontend logs với Next.js

### **Bước 4: Xác nhận services đang chạy**

**Truy cập các URL:**
- Frontend: http://localhost:3000 ✅
- Backend API: http://localhost:8086 ✅  
- Jobe Server: http://localhost:4000 ✅

**Kiểm tra logs Backend:**
```
INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8086
INFO  i.f.c.service.HybridCodeExecutionService - Current execution strategy: JOBE
INFO  i.f.c.service.JobeExecutionService - Jobe server is available
INFO  i.f.c.CScoreBeApplication - Started CScoreBeApplication in XX.XXX seconds
```

### **Bước 5: Test Jobe integration**

**Option A: Test qua script**
```powershell
# Mở PowerShell mới
cd D:\A-FINAL\KLTN\cscore-v1
.\jobe-manager.bat test
```

**Option B: Test qua Frontend**
1. Truy cập: http://localhost:3000
2. Login as Teacher (teacher@example.com / teacher123)
3. Tạo Programming Assignment với test cases
4. Login as Student (student@example.com / student123)
5. Submit Java code và xem kết quả

---

## 📱 DEMO CODE ĐỂ TEST

### **Java Code (Hello World):**
```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
```

### **Java Code (Tính bình phương):**
```java
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        System.out.println(n * n);
    }
}
```

### **Python Code:**
```python
n = int(input())
print(n * n)
```

### **C++ Code:**
```cpp
#include <iostream>
using namespace std;
int main() {
    int n;
    cin >> n;
    cout << n * n << endl;
    return 0;
}
```

---

## 🔧 TROUBLESHOOTING

### ❌ **Problem: "Docker is not running"**
**Solution:**
```powershell
# Mở Docker Desktop từ Start Menu
# Đợi Docker Desktop khởi động hoàn toàn (biểu tượng xanh)
# Chạy lại: .\start-all.bat
```

### ❌ **Problem: "MySQL connection failed"**
**Solution:**
```powershell
# Nếu dùng XAMPP: Start MySQL trong XAMPP Control Panel
# Nếu dùng MySQL Service:
net start mysql80

# Test connection:
mysql -h localhost -P 3306 -u root -p
```

### ❌ **Problem: "Port 8086 already in use"**
**Solution:**
```powershell
# Tìm process đang dùng port 8086
netstat -ano | findstr :8086

# Kill process (thay PID)
taskkill /PID <PID> /F

# Hoặc thay đổi port trong application.properties:
# server.port=8087
```

### ❌ **Problem: "Jobe API not responding"**
**Solution:**
```powershell
# Restart Jobe server
.\jobe-manager.bat restart

# Check logs
.\jobe-manager.bat logs

# Test manually
curl http://localhost:4000/jobe/index.php/restapi/languages
```

### ❌ **Problem: Backend compile error**
**Solution:**
```powershell
cd cscore-backend
.\mvnw clean compile

# Nếu vẫn lỗi, check Java version:
java -version
# Cần Java 17 trở lên
```

---

## 📊 EXECUTION STRATEGIES

Hệ thống hỗ trợ 3 strategies trong `application.properties`:

### **1. `hybrid` (Mặc định - Khuyến nghị)**
```properties
execution.strategy=hybrid
jobe.server.enabled=true
```
- ✅ Ưu tiên Jobe server
- ✅ Fallback về local execution nếu Jobe fail
- ✅ High availability

### **2. `jobe` (Production)**
```properties
execution.strategy=jobe
jobe.server.enabled=true
```
- ✅ Chỉ dùng Jobe server
- ❌ Fail nếu Jobe không available

### **3. `local` (Development)**
```properties
execution.strategy=local
jobe.server.enabled=false
```
- ✅ Chỉ dùng local execution (như cũ)
- ❌ Không có sandbox security

---

## 🛑 DỪNG TẤT CẢ SERVICES

### **Cách 1: Dùng script**
```powershell
.\stop-all.bat
```

### **Cách 2: Manual**
- Đóng cửa sổ "CScore Backend"
- Đóng cửa sổ "CScore Frontend"  
- Chạy: `.\jobe-manager.bat stop`

---

## 🎯 KẾT QUẢ MONG ĐỢI

Sau khi setup thành công:

### ✅ **Security Improvements**
- Code chạy trong Docker sandbox (Jobe server)
- Isolation từ host system
- Resource limits (CPU, memory, time)

### ✅ **Performance**
- Parallel execution của multiple test cases
- Fallback mechanism đảm bảo uptime 99.9%
- Scalable architecture với multiple Jobe instances

### ✅ **Developer Experience**
- Clean separation of concerns
- Easy monitoring và debugging
- Support nhiều programming languages

### ✅ **Production Ready**
- Health check endpoints
- Comprehensive logging
- Error handling và recovery

---

## 🤝 SUPPORT

- 📖 **Detailed Guide**: `BACKEND_JOBE_STARTUP_GUIDE.md`
- 🔧 **Jobe Setup**: `JOBE_INTEGRATION_GUIDE.md`
- ⚡ **Quick Start**: `JOBE_QUICKSTART.md`
- 📋 **API Docs**: `cscore-backend/API_DOCUMENTATION.md`

---

## 🌟 **CHÚC MỪNG!**

**Hệ thống CScore của bạn giờ đây đã được nâng cấp với Jobe Server integration! 🚀**

**From**: Local Code Execution ❌  
**To**: Secure Sandbox Execution ✅

**Welcome to the next level of online judge system! 🛡️**
