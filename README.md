# CScore - Online Code Judge Platform

CScore là một hệ thống chấm điểm code trực tuyến được phát triển với Next.js và Spring Boot, hỗ trợ nhiều ngôn ngữ lập trình và tích hợp Jobe server để thực thi code an toàn.

## ✨ Tính năng chính

- 🏫 **Quản lý khóa học**: Tạo và quản lý courses, assignments
- 👥 **Đa vai trò**: Student, Teacher, Admin với quyền hạn riêng biệt  
- 🔧 **Enhanced Auto-Grading**: Hệ thống chấm điểm thông minh với reference implementation
- 🌐 **Đa ngôn ngữ**: Java, Python, C/C++ với intelligent code wrapping
- 🔒 **Thực thi an toàn**: Tích hợp Jobe server cho sandbox execution
- 📊 **Dashboard**: Thống kê chi tiết cho từng vai trò
- 🎯 **Realtime feedback**: Kết quả chấm điểm ngay lập tức với detailed feedback

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- Java 17+
- MySQL 8.0+
- Docker (for Jobe server)

### Setup
1. **Clone repository**
   ```bash
   git clone <repository-url>
   cd CScore-FE-main
   ```

2. **Start all services**
   ```bash
   ./start-all.bat
   ```

3. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8086
   - Jobe Server: http://localhost:4000

## 📁 Project Structure

Xem chi tiết trong [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

## 📚 Documentation

- [Setup Guide](FINAL_STARTUP_GUIDE.md) - Hướng dẫn cài đặt chi tiết
- [Instructor Guide](INSTRUCTOR_GUIDE.md) - Hướng dẫn cho giáo viên
- [Frontend Architecture](FRONTEND_STRUCTURE.md) - Kiến trúc frontend
- [API Documentation](cscore-backend/API_DOCUMENTATION.md) - Backend API docs

## 🛠️ Tech Stack

**Frontend:**
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- React Hook Form

**Backend:**
- Spring Boot 3
- Spring Security
- JPA/Hibernate
- MySQL
- Jobe Integration

**Deployment:**
- Docker
- Nginx Load Balancer
- MySQL Database

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## � License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## � Contact

- Email: duongomanh3001@gmail.com
- GitHub: [@duongomanh3001](https://github.com/duongomanh3001)

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
