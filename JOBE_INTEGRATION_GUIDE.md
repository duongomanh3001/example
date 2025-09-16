# Hướng dẫn tích hợp Jobe Server

## Giới thiệu về Jobe Server

Jobe (Job Server) là một server sandbox an toàn để biên dịch và chạy code từ xa. Nó được phát triển bởi University of Canterbury và được sử dụng rộng rãi trong các hệ thống online judge.

### Ưu điểm của Jobe Server:
- **An toàn**: Chạy code trong sandbox, cô lập với hệ thống host
- **Đa ngôn ngữ**: Hỗ trợ nhiều ngôn ngữ lập trình (Java, Python, C/C++, JavaScript, ...)
- **Kiểm soát tài nguyên**: Giới hạn CPU, memory, thời gian thực thi
- **Ổn định**: Được test kỹ lưỡng và sử dụng trong production
- **Mở rộng**: Dễ dàng thêm ngôn ngữ mới

## Cài đặt Jobe Server

### Phương pháp 1: Docker (Khuyến nghị)

1. **Tải Docker Image:**
   ```bash
   docker pull trampgeek/jobeinabox
   ```

2. **Chạy Jobe Server:**
   ```bash
   docker run -d -p 4000:80 --name jobe-server trampgeek/jobeinabox
   ```

3. **Kiểm tra Jobe Server:**
   ```bash
   curl http://localhost:4000/jobe/index.php/restapi/languages
   ```

### Phương pháp 2: Cài đặt trực tiếp trên Ubuntu

1. **Cài đặt dependencies:**
   ```bash
   sudo apt update
   sudo apt install apache2 php php-cli php-mbstring php-xml php-zip
   sudo apt install python3 python3-pip openjdk-11-jdk gcc g++ nodejs npm
   ```

2. **Clone Jobe repository:**
   ```bash
   git clone https://github.com/trampgeek/jobe.git
   cd jobe
   sudo cp -r . /var/www/html/jobe/
   ```

3. **Cấu hình Apache:**
   ```bash
   sudo a2enmod rewrite
   sudo systemctl restart apache2
   ```

4. **Cài đặt Jobe:**
   ```bash
   cd /var/www/html/jobe
   sudo ./install
   ```

## Cấu hình trong CScore

### 1. Cập nhật application.properties

Trong file `application.properties`, bạn đã có các cấu hình sau:

```properties
# Jobe Server Configuration
jobe.server.url=http://localhost:4000
jobe.server.enabled=true

# Code Execution Configuration
execution.strategy=hybrid
```

### 2. Các chiến lược thực thi

Hệ thống hỗ trợ 3 chiến lược:

- **`hybrid`** (Khuyến nghị): Ưu tiên Jobe, fallback về local nếu Jobe không khả dụng
- **`jobe`**: Chỉ sử dụng Jobe server
- **`local`**: Chỉ sử dụng local execution

### 3. Kiểm tra trạng thái

Sau khi khởi động backend, bạn có thể kiểm tra trạng thái thông qua API:

```bash
GET /api/admin/execution/status
```

Response sẽ chứa:
```json
{
    "currentStrategy": "JOBE",
    "jobeEnabled": true,
    "jobeAvailable": true,
    "configuredStrategy": "hybrid",
    "supportedLanguages": ["java", "python3", "cpp", "c", "nodejs"]
}
```

## Test Jobe Server

### 1. Test cơ bản

```bash
curl -X POST http://localhost:4000/jobe/index.php/restapi/runs \
  -H "Content-Type: application/json" \
  -d '{
    "language_id": "java",
    "sourcecode": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Jobe!\");\n    }\n}"
  }'
```

### 2. Test qua CScore API

```bash
curl -X POST http://localhost:8086/api/admin/execution/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "code": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, CScore!\");\n    }\n}",
    "language": "java"
  }'
```

## Ngôn ngữ được hỗ trợ

### Mapping ngôn ngữ CScore → Jobe:

| CScore | Jobe ID | Compiler/Interpreter |
|--------|---------|----------------------|
| java   | java    | OpenJDK 11          |
| python | python3 | Python 3.x          |
| cpp    | cpp     | g++                 |
| c      | c       | gcc                 |
| javascript | nodejs | Node.js           |

### Thêm ngôn ngữ mới

Để thêm ngôn ngữ mới, cập nhật method `getJobeLanguageId()` trong `JobeExecutionService.java`:

```java
private String getJobeLanguageId(String language) {
    switch (language.toLowerCase()) {
        case "java":
            return "java";
        case "python":
            return "python3";
        case "cpp":
        case "c++":
            return "cpp";
        case "c":
            return "c";
        case "javascript":
        case "js":
            return "nodejs";
        case "php":
            return "php";
        case "ruby":
            return "ruby";
        default:
            return language.toLowerCase();
    }
}
```

## Troubleshooting

### Lỗi thường gặp:

1. **Connection refused**
   - Kiểm tra Jobe server có đang chạy không: `docker ps` hoặc `sudo systemctl status apache2`
   - Kiểm tra port 4000 có open không: `netstat -tlnp | grep 4000`

2. **Compilation errors**
   - Kiểm tra code syntax
   - Kiểm tra Jobe server có cài đặt compiler/interpreter không

3. **Timeout errors**
   - Tăng timeout trong application.properties
   - Kiểm tra server resources

4. **Permission denied**
   - Kiểm tra quyền thư mục /tmp trong Docker container
   - Restart Jobe server

### Debug commands:

```bash
# Check Jobe server status
curl http://localhost:4000/jobe/index.php/restapi/languages

# Check Docker logs
docker logs jobe-server

# Check system resources
docker stats jobe-server

# Test specific language
curl -X POST http://localhost:4000/jobe/index.php/restapi/runs \
  -H "Content-Type: application/json" \
  -d '{"language_id": "python3", "sourcecode": "print(\"Hello World\")"}'
```

## Performance và Scaling

### Cấu hình performance:

1. **CPU và Memory limits trong Docker:**
   ```bash
   docker run -d -p 4000:80 \
     --cpus="2.0" \
     --memory="2g" \
     --name jobe-server \
     trampgeek/jobeinabox
   ```

2. **Load balancing với nhiều Jobe instances:**
   ```bash
   # Instance 1
   docker run -d -p 4000:80 --name jobe-1 trampgeek/jobeinabox
   
   # Instance 2  
   docker run -d -p 4001:80 --name jobe-2 trampgeek/jobeinabox
   ```

3. **Caching compiled code** (sẽ implement trong version sau)

## Migration từ Local Execution

Quá trình migration được thiết kế để không downtime:

1. **Phase 1**: Cài đặt Jobe server, để `jobe.server.enabled=false`
2. **Phase 2**: Bật Jobe với `execution.strategy=hybrid`
3. **Phase 3**: Sau khi stable, có thể chuyển sang `execution.strategy=jobe`

System sẽ tự động fallback về local execution nếu Jobe không khả dụng.

## Monitoring

### Metrics để theo dõi:

- Response time của Jobe server
- Success rate của code execution
- Resource usage (CPU/Memory)
- Queue length (nếu có nhiều requests đồng thời)

### Logs locations:

- **CScore Backend**: `logs/cscore-backend.log`
- **Jobe Server**: `docker logs jobe-server`

## Security Considerations

1. **Network security**: Chạy Jobe trong private network
2. **Resource limits**: Giới hạn CPU, memory, disk usage
3. **Sandboxing**: Jobe đã có built-in sandboxing
4. **Input validation**: Validate code trước khi gửi đến Jobe
5. **Rate limiting**: Implement rate limiting cho API endpoints

## Kết luận

Việc tích hợp Jobe server sẽ cải thiện:
- **Bảo mật**: Code chạy trong sandbox an toàn
- **Ổn định**: Giảm risk của local code execution
- **Performance**: Có thể scale horizontal với nhiều Jobe instances
- **Maintainability**: Centralized code execution service

Hệ thống hybrid execution đảm bảo high availability với automatic fallback mechanism.
