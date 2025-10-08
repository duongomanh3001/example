# CScore - Hệ thống Chấm điểm Tự động

## Cấu trúc Project

```
CScore-FE-main/
├── src/                          # Frontend source code
│   ├── app/                      # Next.js App Router pages
│   │   ├── admin/               # Admin dashboard pages
│   │   ├── login/               # Login page
│   │   ├── student/             # Student dashboard pages  
│   │   ├── teacher/             # Teacher dashboard pages
│   │   └── unauthorized/        # Unauthorized access page
│   ├── components/              # React components
│   │   ├── admin/              # Admin-specific components
│   │   ├── common/             # Shared components (Navbar, etc.)
│   │   ├── layouts/            # Layout components
│   │   ├── student/            # Student-specific components
│   │   ├── teacher/            # Teacher-specific components
│   │   └── ui/                 # UI components
│   ├── services/               # API service functions
│   ├── types/                  # TypeScript type definitions
│   ├── utils/                  # Utility functions
│   └── constants/              # Application constants
├── cscore-backend/             # Spring Boot backend
│   ├── src/main/java/          # Java source code
│   │   └── iuh/fit/cscore_be/  # Main package
│   │       ├── controller/     # REST controllers
│   │       ├── service/        # Business logic services
│   │       ├── entity/         # JPA entities
│   │       ├── repository/     # Data repositories
│   │       ├── dto/            # Data Transfer Objects
│   │       └── enums/          # Enum definitions
│   ├── src/main/resources/     # Configuration files
│   └── migrations/             # Database migration scripts
├── public/                     # Static assets
└── docs/                       # Documentation files
```

## Tính năng chính

### 🎯 Enhanced Auto-Grading System
- **Reference Implementation Comparison**: So sánh code sinh viên với đáp án chuẩn
- **Multiple Language Support**: C, C++, Java, Python
- **Intelligent Test Case Execution**: Tự động tạo và chạy test cases
- **Detailed Feedback**: Cung cấp phản hồi chi tiết về lỗi và kết quả

### 👨‍🏫 Teacher Features
- Tạo và quản lý assignments
- Tạo câu hỏi programming với test cases
- Xem kết quả và thống kê sinh viên
- Enhanced grading với reference implementation

### 👨‍🎓 Student Features  
- Làm bài tập trực tuyến
- Test code với example test cases
- Xem kết quả và feedback chi tiết
- Theo dõi tiến độ học tập

### ⚙️ Admin Features
- Quản lý người dùng (students, teachers)
- Quản lý courses và enrollments
- System monitoring và health checks
- Enhanced grading configuration

## Services Architecture

### Frontend Services
- `auth.service.ts` - Authentication và authorization
- `course.service.ts` - Quản lý courses
- `assignment.service.ts` - Quản lý assignments và submissions
- `user.service.ts` - Quản lý users
- `dashboard.service.ts` - Dashboard data

### Backend Services
- `StudentService` - Logic cho student operations
- `TeacherService` - Logic cho teacher operations
- `EnhancedAutoGradingService` - Enhanced grading system
- `HybridCodeExecutionService` - Code execution với Jobe
- `UniversalWrapperService` - Intelligent code wrapping

## Database Schema

### Core Tables
- `users` - User information và roles
- `courses` - Course definitions  
- `assignments` - Assignment configurations
- `questions` - Question details với metadata
- `test_cases` - Test cases cho programming questions
- `submissions` - Student submissions
- `test_results` - Detailed test execution results

## Deployment

### Development
```bash
# Start all services
./start-all.bat

# Stop all services  
./stop-all.bat
```

### Production
- Frontend: Next.js deployment
- Backend: Spring Boot với embedded Tomcat
- Database: MySQL 8.0
- Code Execution: Jobe server integration

## Documentation Files

- `README.md` - General project information
- `FINAL_STARTUP_GUIDE.md` - Complete setup guide
- `INSTRUCTOR_GUIDE.md` - Guide for instructors
- `FRONTEND_STRUCTURE.md` - Frontend architecture details
- `API_DOCUMENTATION.md` - Backend API documentation (trong cscore-backend/)