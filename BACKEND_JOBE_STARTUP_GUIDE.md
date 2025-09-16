# 🚀 Hướng dẫn Chạy Backend Mới với Jobe Server

## 📋 Yêu cầu hệ thống

### Phần mềm cần thiết:
- **Docker Desktop** (cho Windows/Mac) hoặc **Docker Engine** (cho Linux)
- **Java 17+** 
- **Maven 3.6+**
- **MySQL 8.0+**
- **Node.js 18+** (cho frontend)

### Kiểm tra phần mềm:
```powershell
# Kiểm tra Docker
docker --version
docker-compose --version

# Kiểm tra Java
java -version
javac -version

# Kiểm tra Maven
mvn -version

# Kiểm tra MySQL
mysql --version
```

## 🏗️ Bước 1: Khởi động Jobe Server

### Option A: Sử dụng Script quản lý (Khuyến nghị)

```powershell
# Di chuyển về thư mục root của project
cd D:\A-FINAL\KLTN\cscore-v1

# Khởi động Jobe server đơn
.\jobe-manager.bat start

# Hoặc khởi động multiple instances với load balancer
.\jobe-manager.bat start multi
```

### Option B: Sử dụng Docker Compose trực tiếp

```powershell
# Khởi động single instance
docker-compose -f docker-compose.jobe.yml up -d jobe-server

# Khởi động multiple instances
docker-compose -f docker-compose.jobe.yml --profile multi-instance up -d
```

### Xác nhận Jobe Server đang chạy:

```powershell
# Kiểm tra container
docker ps | findstr jobe

# Test Jobe API
curl http://localhost:4000/jobe/index.php/restapi/languages

# Hoặc sử dụng script test
.\jobe-manager.bat test
```

**Kết quả mong đợi:**
```
CONTAINER ID   IMAGE                    STATUS          PORTS
abc123def456   trampgeek/jobeinabox    Up 2 minutes    0.0.0.0:4000->80/tcp
```

## 🗄️ Bước 2: Chuẩn bị Database

### Khởi động MySQL:
```powershell
# Nếu dùng XAMPP
# Start MySQL trong XAMPP Control Panel

# Hoặc nếu MySQL service
net start mysql80
```

### Tạo/Kiểm tra Database:
```sql
-- Kết nối MySQL
mysql -u root -p

-- Tạo database (nếu chưa có)
CREATE DATABASE IF NOT EXISTS cscoredb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Kiểm tra database
SHOW DATABASES;
USE cscoredb;
SHOW TABLES;
```

## 🔧 Bước 3: Build Backend với Dependencies mới

### Clean và Rebuild:
```powershell
cd D:\A-FINAL\KLTN\cscore-v1\cscore-backend

# Clean previous build
.\mvnw clean

# Compile và build
.\mvnw compile

# Chạy tests (optional)
.\mvnw test

# Package (tạo JAR file)
.\mvnw package -DskipTests
```

### Kiểm tra dependencies mới:
```powershell
# Xem dependency tree
.\mvnw dependency:tree | findstr jobe

# Kiểm tra class files mới được tạo
dir target\classes\iuh\fit\cscore_be\service\ | findstr -i "jobe\|hybrid\|execution"
```

**Files mới sẽ được compile:**
- `JobeExecutionService.class`
- `HybridCodeExecutionService.class`
- `ExecutionController.class`

## 🚀 Bước 4: Khởi động Backend

### Khởi động với Maven:
```powershell
cd D:\A-FINAL\KLTN\cscore-v1\cscore-backend

# Start với profile development
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Hoặc với JVM arguments
.\mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g -Dspring.profiles.active=dev"
```

### Hoặc chạy JAR file trực tiếp:
```powershell
# Chạy JAR đã build
java -jar target\CScore_BE-0.0.1-SNAPSHOT.jar

# Hoặc với custom properties
java -jar target\CScore_BE-0.0.1-SNAPSHOT.jar --jobe.server.enabled=true --execution.strategy=hybrid
```

## ✅ Bước 5: Xác nhận Backend khởi động thành công

### Kiểm tra Logs:
```
INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8086 (http)
INFO  i.f.c.service.HybridCodeExecutionService - Current execution strategy: JOBE
INFO  i.f.c.service.JobeExecutionService - Jobe server is available at: http://localhost:4000
INFO  i.f.c.CScoreBeApplication - Started CScoreBeApplication in 15.234 seconds
```

### Test Health Endpoints:
```powershell
# Basic health check
curl http://localhost:8086/actuator/health

# Backend specific health
curl http://localhost:8086/api/admin/health

# Execution status (cần JWT token)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:8086/api/admin/execution/status
```

### Test Code Execution:
```powershell
# Test execution qua API (cần login trước để có JWT token)
curl -X POST http://localhost:8086/api/admin/execution/test ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer YOUR_JWT_TOKEN" ^
  -d "{\"code\":\"public class Main{public static void main(String[] args){System.out.println(\\\"Hello Jobe!\\\");}}\",\"language\":\"java\"}"
```

## 🌐 Bước 6: Khởi động Frontend

### Cài đặt dependencies:
```powershell
cd D:\A-FINAL\KLTN\cscore-v1

# Install dependencies
npm install

# Hoặc nếu dùng yarn
yarn install
```

### Start development server:
```powershell
# Development mode
npm run dev

# Hoặc
yarn dev
```

### Truy cập ứng dụng:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8086
- **Jobe Server**: http://localhost:4000

## 🔍 Bước 7: Kiểm tra Integration hoạt động

### Test qua Frontend:

1. **Login as Teacher:**
   - Username: teacher@example.com
   - Password: teacher123

2. **Tạo Assignment mới:**
   - Chọn type: "Programming Assignment"
   - Language: Java/Python/C++
   - Thêm test cases

3. **Login as Student:**
   - Username: student@example.com  
   - Password: student123

4. **Submit code:**
   ```java
   public class Main {
       public static void main(String[] args) {
           System.out.println("Hello World!");
       }
   }
   ```

5. **Xác nhận kết quả:**
   - Submission được chấm điểm ngay lập tức
   - Logs backend hiển thị: "Executing code via Jobe server"

### Debug và Monitoring:

```powershell
# Xem logs backend real-time
Get-Content -Path "logs\cscore-backend.log" -Wait | Select-String -Pattern "jobe|execution"

# Xem Jobe server logs
docker logs cscore-jobe-server --follow

# Monitor resource usage
docker stats cscore-jobe-server
```

## 🛠️ Troubleshooting

### ❌ Backend không start:

**Lỗi**: `Failed to configure a DataSource`
```powershell
# Kiểm tra MySQL đang chạy
netstat -an | findstr :3306

# Test connection
mysql -h localhost -P 3306 -u root -p
```

**Lỗi**: `ClassNotFoundException: JobeExecutionService`
```powershell
# Clean và rebuild
.\mvnw clean compile
```

### ❌ Jobe server không kết nối:

```powershell
# Kiểm tra Jobe container
docker ps | findstr jobe

# Test Jobe API
curl http://localhost:4000/jobe/index.php/restapi/languages

# Restart nếu cần
.\jobe-manager.bat restart
```

### ❌ Execution strategy không đúng:

```powershell
# Kiểm tra config
curl -H "Authorization: Bearer JWT_TOKEN" http://localhost:8086/api/admin/execution/status

# Expected response:
# {
#   "currentStrategy": "JOBE",
#   "jobeEnabled": true,
#   "jobeAvailable": true,
#   "configuredStrategy": "hybrid"
# }
```

## 📊 Performance Monitoring

### System Resources:
```powershell
# CPU và Memory usage
Get-Process -Name java | Format-Table ProcessName,CPU,WorkingSet

# Docker resources
docker stats --no-stream
```

### Application Metrics:
```powershell
# Request count và response time
curl http://localhost:8086/actuator/metrics/http.server.requests

# Jobe execution metrics
curl -H "Authorization: Bearer JWT_TOKEN" http://localhost:8086/api/admin/execution/status
```

## 🎯 Kết quả mong đợi

Sau khi hoàn thành các bước trên, bạn sẽ có:

✅ **Jobe Server**: Running trên port 4000  
✅ **Backend**: Running trên port 8086 với Jobe integration  
✅ **Frontend**: Running trên port 3000  
✅ **Database**: MySQL với schema updated  
✅ **Code Execution**: Hybrid strategy với fallback mechanism  

### Logs thành công:
```
[INFO] Starting CScore Backend with Jobe integration...
[INFO] Jobe server detected at: http://localhost:4000
[INFO] Execution strategy: HYBRID
[INFO] Supported languages via Jobe: [java, python3, cpp, c, nodejs]
[INFO] Backend ready at: http://localhost:8086
```

## 🔄 Script tổng hợp

Để thuận tiện, tôi sẽ tạo script khởi động tất cả services:

```powershell
# start-all.bat
@echo off
echo Starting CScore with Jobe Server...

echo [1/4] Starting Jobe Server...
call jobe-manager.bat start

echo [2/4] Waiting for Jobe to be ready...
timeout /t 10 /nobreak

echo [3/4] Starting Backend...
cd cscore-backend
start "CScore Backend" cmd /k "mvnw spring-boot:run"

echo [4/4] Starting Frontend...
cd ..
start "CScore Frontend" cmd /k "npm run dev"

echo All services started!
echo Frontend: http://localhost:3000
echo Backend: http://localhost:8086  
echo Jobe: http://localhost:4000
```

Chạy script này sẽ khởi động tất cả services cùng lúc!
