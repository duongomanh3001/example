# 🚀 Quick Start Guide - Jobe Server Integration

## Tóm tắt về thay đổi

Hệ thống CScore hiện tại đang sử dụng **Local Code Execution** - tức là biên dịch và chạy code trực tiếp trên backend server. Sau khi tích hợp **Jobe Server**, bạn sẽ có:

### ✅ **Trước đây (Local Execution)**
```
Student Code → Backend Server → Local Compiler → Execute → Result
```
- ❌ Rủi ro bảo mật cao  
- ❌ Không kiểm soát tài nguyên
- ❌ Khó scale

### ✅ **Bây giờ (Jobe Server Integration)**
```
Student Code → Backend Server → Jobe Server (Sandbox) → Execute → Result
```
- ✅ An toàn với sandbox isolation
- ✅ Kiểm soát CPU, memory, time limits
- ✅ Dễ scale với multiple Jobe instances
- ✅ Fallback mechanism nếu Jobe fail

## 🏃‍♂️ Khởi động nhanh (5 phút)

### Bước 1: Start Jobe Server

```bash
# Linux/Mac
chmod +x jobe-manager.sh
./jobe-manager.sh start

# Windows  
jobe-manager.bat start
```

### Bước 2: Cấu hình Backend

Chỉnh sửa `application.properties`:
```properties
# Bật Jobe server
jobe.server.enabled=true

# Chọn strategy (khuyến nghị: hybrid)
execution.strategy=hybrid
```

### Bước 3: Restart Backend

```bash
cd cscore-backend
./mvnw spring-boot:run
```

### Bước 4: Test

```bash
# Test Jobe server
./jobe-manager.sh test

# Hoặc qua API
curl -X GET http://localhost:8086/api/admin/execution/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🎯 Xác nhận hoạt động

### 1. Kiểm tra Backend Logs
```
[INFO] Starting code execution for 2 test cases with language: java
[INFO] Executing code using strategy: JOBE for language: java  
[INFO] Auto-grading completed for submission: 123, final score: 100.0
```

### 2. Kiểm tra Jobe Container
```bash
docker ps | grep jobe
# Should show: cscore-jobe-server running
```

### 3. Test qua Frontend
- Login as Student
- Submit assignment với Java code
- Kết quả sẽ hiển thị "Executed via Jobe" trong logs

## ⚙️ Các Execution Strategies

### 1. `hybrid` (Khuyến nghị)
```properties
execution.strategy=hybrid
```
- Ưu tiên Jobe, fallback local nếu Jobe fail
- Đảm bảo high availability

### 2. `jobe` (Production)
```properties  
execution.strategy=jobe
```
- Chỉ dùng Jobe server
- Fail nếu Jobe không available

### 3. `local` (Development)
```properties
execution.strategy=local  
```
- Chỉ dùng local execution
- Giống như trước khi tích hợp

## 🔧 Advanced Configuration

### Multiple Jobe Instances (Load Balancing)

```bash
# Start với load balancer
./jobe-manager.sh start multi
```

Cấu hình application.properties:
```properties
jobe.server.url=http://localhost:4002  # Load balancer URL
```

### Custom Resource Limits

Chỉnh sửa `JobeExecutionService.java`:
```java
parameters.put("cputime", 30);        // 30 seconds CPU time
parameters.put("memorylimit", 256000); // 256MB memory
```

### Monitoring và Logs

```bash
# Xem Jobe logs
./jobe-manager.sh logs

# Xem backend logs  
tail -f logs/cscore-backend.log | grep -i jobe
```

## 🚨 Troubleshooting

### ❌ "Connection refused" 
```bash
# Check Jobe status
./jobe-manager.sh status

# Restart if needed
./jobe-manager.sh restart
```

### ❌ "Compilation failed"
```bash
# Test specific language
./jobe-manager.sh test
```

### ❌ "Jobe server not available"
- Kiểm tra `jobe.server.enabled=true`
- Verify Jobe URL trong config
- Check Docker container running

## 📊 Performance Comparison

| Metric | Local Execution | Jobe Server |
|--------|----------------|-------------|
| **Security** | ❌ Low | ✅ High (Sandbox) |
| **Resource Control** | ❌ None | ✅ CPU/Memory limits |
| **Scalability** | ❌ Single server | ✅ Multiple instances |
| **Isolation** | ❌ Same process | ✅ Container isolation |
| **Language Support** | ⚠️ Manual setup | ✅ Pre-configured |

## 🔄 Migration Plan

### Phase 1: Setup (Current)
- ✅ Jobe server installed  
- ✅ Code integration complete
- ✅ Config: `hybrid` mode

### Phase 2: Testing (Next 1-2 days)
- Test all supported languages
- Verify fallback mechanism  
- Monitor performance

### Phase 3: Production (After testing)
- Switch to `jobe` mode
- Setup monitoring alerts
- Document operational procedures

## 🎉 Kết quả mong đợi

Sau khi tích hợp thành công:

### ✅ **Immediate Benefits**
- Improved security với sandbox execution
- Better error handling và feedback
- Resource isolation per submission

### ✅ **Long-term Benefits**  
- Easy horizontal scaling
- Support for more languages
- Better performance monitoring
- Reduced server maintenance

### ✅ **Developer Experience**
- Cleaner separation of concerns
- Easier to add new execution features
- Better testing and debugging

---

## 🤝 Need Help?

- 📖 **Detailed Guide**: [JOBE_INTEGRATION_GUIDE.md](./JOBE_INTEGRATION_GUIDE.md)
- 🔧 **API Docs**: [API_DOCUMENTATION.md](./cscore-backend/API_DOCUMENTATION.md)
- 📋 **Testing Guide**: [AUTO_GRADING_TESTING_GUIDE.md](./AUTO_GRADING_TESTING_GUIDE.md)

**Welcome to Safer Code Execution! 🛡️**
