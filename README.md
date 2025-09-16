# CScore - Online Code Judge Platform

CScore là một hệ thống chấm điểm code trực tuyến được phát triển với Next.js và Spring Boot, hỗ trợ nhiều ngôn ngữ lập trình và tích hợp Jobe server để thực thi code an toàn.

## ✨ Tính năng chính

- 🏫 **Quản lý khóa học**: Tạo và quản lý courses, assignments
- 👥 **Đa vai trò**: Student, Teacher, Admin với quyền hạn riêng biệt  
- 🔧 **Chấm điểm tự động**: Hỗ trợ auto-grading với test cases
- 🌐 **Đa ngôn ngữ**: Java, Python, C/C++, JavaScript và nhiều ngôn ngữ khác
- 🔒 **Thực thi an toàn**: Tích hợp Jobe server để sandbox execution
- 📊 **Dashboard**: Thống kê chi tiết cho từng vai trò
- 🎯 **Realtime feedback**: Kết quả chấm điểm ngay lập tức

## 🚀 Code Execution Strategies

### 1. Local Execution (Mặc định)
- Biên dịch và chạy code trực tiếp trên server
- Phù hợp cho development và testing
- Dễ setup, không cần dependencies bên ngoài

### 2. Jobe Server (Khuyến nghị cho Production)
- Sử dụng Jobe server để thực thi code trong sandbox
- An toàn, ổn định và có thể mở rộng
- Hỗ trợ nhiều ngôn ngữ và kiểm soát tài nguyên

### 3. Hybrid Mode (Tối ưu)
- Ưu tiên Jobe server, fallback về local nếu Jobe không khả dụng
- Đảm bảo high availability
- Cấu hình linh hoạt

## 🛠️ Cài đặt và Chạy

### Frontend (Next.js)

```bash
# Cài đặt dependencies
npm install

# Chạy development server
npm run dev

# Build cho production
npm run build
npm start
```

### Backend (Spring Boot)

```bash
cd cscore-backend

# Sử dụng Maven wrapper
./mvnw spring-boot:run

# Hoặc build và chạy
./mvnw clean package
java -jar target/cscore-be-*.jar
```

### Database

Cấu hình MySQL trong `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cscoredb
spring.datasource.username=root
spring.datasource.password=your-password
```

### Jobe Server Integration

#### Cài đặt nhanh với Docker:

```bash
# Khởi động Jobe server
./jobe-manager.sh start

# Hoặc trên Windows
jobe-manager.bat start
```

#### Cấu hình trong application.properties:

```properties
# Bật Jobe server
jobe.server.enabled=true
jobe.server.url=http://localhost:4000

# Chọn execution strategy
execution.strategy=hybrid  # hybrid, jobe, local
```

#### Kiểm tra trạng thái:

```bash
# Test Jobe server
./jobe-manager.sh test

# Xem status
./jobe-manager.sh status
```

## 📁 Cấu trúc dự án

```
cscore-v1/
├── src/app/                    # Next.js App Router
│   ├── admin/                  # Admin pages
│   ├── teacher/                # Teacher pages  
│   ├── student/                # Student pages
│   └── api/                    # API routes
├── components/                 # React components
├── services/                   # API services
├── cscore-backend/            # Spring Boot backend
│   ├── src/main/java/         # Java source
│   └── src/main/resources/    # Configurations
├── docker-compose.jobe.yml    # Jobe server setup
├── jobe-manager.sh           # Jobe management script
└── JOBE_INTEGRATION_GUIDE.md # Chi tiết tích hợp Jobe
```

## 🔧 Cấu hình

### Environment Variables

```bash
# Frontend (.env.local)
NEXT_PUBLIC_API_URL=http://localhost:8086

# Backend (application.properties)
server.port=8086
jobe.server.enabled=true
execution.strategy=hybrid
```

### Supported Languages

| Language   | Local | Jobe | Compiler/Runtime |
|------------|-------|------|------------------|
| Java       | ✅    | ✅   | OpenJDK 11+      |
| Python     | ✅    | ✅   | Python 3.x       |
| C++        | ✅    | ✅   | g++              |
| C          | ✅    | ✅   | gcc              |
| JavaScript | ✅    | ✅   | Node.js          |

## 🎯 Sử dụng

### Roles và Permissions

#### 🔹 **Student**
- Xem courses đã đăng ký
- Làm assignments và submit code
- Xem kết quả chấm điểm và feedback
- Dashboard với progress tracking

#### 🔹 **Teacher**  
- Tạo và quản lý courses
- Tạo assignments với test cases
- Xem submissions của students
- Chấm điểm manual và auto-grading
- Analytics và reports

#### 🔹 **Admin**
- Quản lý users (students, teachers)
- Quản lý hệ thống courses
- Xem system statistics
- Cấu hình execution settings

### Workflow

1. **Admin** tạo users và assign roles
2. **Teacher** tạo courses và assignments với test cases  
3. **Students** làm bài và submit code
4. **System** auto-grade với test cases
5. **Teachers** review và adjust scores nếu cần

## 🔍 API Endpoints

### Authentication
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/register` - Đăng ký (nếu enabled)

### Students
- `GET /api/student/assignments` - Lấy danh sách assignments
- `POST /api/student/assignments/{id}/submit` - Submit code
- `GET /api/student/submissions/{id}` - Xem kết quả submission

### Teachers  
- `POST /api/teacher/courses` - Tạo course mới
- `POST /api/teacher/assignments` - Tạo assignment
- `GET /api/teacher/assignments/{id}/submissions` - Xem submissions

### Admin
- `GET /api/admin/users` - Quản lý users
- `GET /api/admin/execution/status` - Kiểm tra execution status

## 🚨 Troubleshooting

### Common Issues

**1. Jobe server connection failed**
```bash
# Check Jobe server status
./jobe-manager.sh status

# Restart if needed
./jobe-manager.sh restart
```

**2. Compilation errors**
- Kiểm tra syntax code
- Verify ngôn ngữ được support
- Check compiler availability

**3. Database connection issues**
- Verify MySQL running
- Check connection string
- Ensure database exists

**4. Permission denied**
- Check user roles
- Verify JWT token
- Review API endpoints permissions

## 📚 Documentation

- [Jobe Integration Guide](./JOBE_INTEGRATION_GUIDE.md)
- [Auto Grading Testing Guide](./AUTO_GRADING_TESTING_GUIDE.md)
- [Teacher Assignment Guide](./TEACHER_ASSIGNMENT_GUIDE.md)
- [API Documentation](./cscore-backend/API_DOCUMENTATION.md)

## 🤝 Contributing

1. Fork repository
2. Create feature branch
3. Commit changes  
4. Push to branch
5. Create Pull Request

## 📄 License

This project is licensed under the MIT License.

---

## 📞 Support

Nếu gặp vấn đề, vui lòng:
1. Check [troubleshooting guide](#-troubleshooting)
2. Review logs: `docker logs cscore-jobe-server`
3. Create issue với detailed description

**Happy Coding! 🚀**
