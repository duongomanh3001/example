# CScore Backend Authentication System

## API Endpoints

### Authentication APIs

#### 1. Đăng nhập
```
POST /api/auth/signin
Content-Type: application/json

{
    "usernameOrEmail": "student01",
    "password": "123456"
}
```

### Admin APIs (chỉ ADMIN)

#### 1. Tạo tài khoản mới
```
POST /api/admin/create-user
Authorization: Bearer <jwt_token>
Content-Type: application/json

// Tạo tài khoản STUDENT
{
    "username": "student01",
    "email": "student01@example.com",
    "password": "123456",
    "fullName": "Nguyễn Văn A",
    "studentId": "20001234",
    "role": "STUDENT"
}

// Tạo tài khoản TEACHER
{
    "username": "teacher01",
    "email": "teacher01@example.com",
    "password": "123456",
    "fullName": "Thầy Nguyễn Văn B",
    "role": "TEACHER"
}
```

#### 2. Dashboard Admin
```
GET /api/admin/dashboard
Authorization: Bearer <jwt_token>
```

#### 3. Xem danh sách người dùng
```
GET /api/admin/users
Authorization: Bearer <jwt_token>
```

### Protected APIs (cần JWT token)

#### Teacher APIs (TEACHER và ADMIN)
```
GET /api/teacher/dashboard
GET /api/teacher/courses
GET /api/teacher/assignments
Authorization: Bearer <jwt_token>
```

#### Student APIs (STUDENT, TEACHER và ADMIN)
```
GET /api/student/dashboard
GET /api/student/assignments
GET /api/student/submissions
Authorization: Bearer <jwt_token>
```

## Roles và Permissions

### 1. STUDENT (Sinh viên)
- Có thể truy cập: `/api/student/**`
- Cần có studentId khi đăng ký
- Permissions: Xem bài tập, nộp bài, xem điểm

### 2. TEACHER (Giảng viên)
- Có thể truy cập: `/api/teacher/**` và `/api/student/**`
- Permissions: Tạo bài tập, chấm điểm, quản lý lớp học

### 3. ADMIN (Quản trị viên)
- Có thể truy cập: tất cả APIs
- Permissions: Quản lý người dùng, hệ thống

## Sample Data để test

### Tạo Admin
```json
{
    "username": "admin",
    "email": "admin@cscore.com",
    "password": "admin123",
    "fullName": "Administrator",
    "role": "ADMIN"
}
```

### Tạo Teacher
```json
{
    "username": "teacher01",
    "email": "teacher01@iuh.edu.vn",
    "password": "teacher123",
    "fullName": "Nguyễn Thị B",
    "role": "TEACHER"
}
```

### Tạo Student
```json
{
    "username": "student01",
    "email": "student01@student.iuh.edu.vn",
    "password": "student123",
    "fullName": "Trần Văn C",
    "studentId": "21001234",
    "role": "STUDENT"
}
```

## Cách sử dụng JWT Token

1. Đăng nhập để lấy token
2. Thêm token vào header Authorization:
   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzdHVkZW50MDEi...
   ```
3. Gọi các API protected với token

## Database Schema

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    student_id VARCHAR(20),
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```
