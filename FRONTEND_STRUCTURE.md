# CScore Frontend - Cấu trúc Project

## 📁 **Cấu trúc thư mục mới (đã tối ưu)**

```
src/
├── app/                    # Next.js App Router pages
│   ├── globals.css        # Global styles
│   ├── layout.tsx         # Root layout
│   ├── page.tsx          # Home page
│   ├── admin/            # Admin pages
│   ├── student/          # Student pages
│   ├── teacher/          # Teacher pages
│   ├── login/            # Authentication pages
│   └── unauthorized/     # Error pages
├── components/            # React Components
│   ├── index.ts          # Main export file
│   ├── admin/            # Admin-specific components
│   │   ├── index.ts      # Admin components export
│   │   ├── AdminDashboard.tsx
│   │   ├── AdminCourseManagement.tsx
│   │   ├── UserManagement.tsx
│   │   └── CsvUploadModal.tsx
│   ├── common/           # Shared components
│   │   ├── index.ts      # Common components export
│   │   ├── Navbar.tsx
│   │   ├── Footer.tsx
│   │   ├── TopBar.tsx
│   │   ├── ViewToggle.tsx
│   │   └── ClientOnlyWrapper.tsx
│   ├── teacher/          # Teacher-specific components
│   │   ├── index.ts      # Teacher components export
│   │   ├── TeacherDashboard.tsx
│   │   ├── AssignmentCreationForm.tsx
│   │   └── TeacherAssignmentManagement.tsx
│   ├── ui/              # Reusable UI components
│   │   ├── index.ts     # UI components export
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Modal.tsx
│   │   ├── Hero.tsx
│   │   ├── AssignmentCard.tsx
│   │   ├── CourseCard.tsx
│   │   └── TeacherAssignmentCard.tsx
│   ├── layouts/         # Layout components
│   │   ├── index.ts     # Layout components export
│   │   └── MainLayout.tsx
│   └── hoc/            # Higher-Order Components
│       ├── index.ts    # HOC components export
│       └── withAuth.tsx
├── services/           # API Services
│   ├── index.ts       # Services export
│   ├── auth.service.ts
│   ├── course.service.ts
│   ├── assignment.service.ts
│   ├── user.service.ts
│   ├── dashboard.service.ts
│   └── health.service.ts
├── hooks/             # Custom React Hooks
│   └── useRoleAccess.ts
├── contexts/          # React Contexts
│   └── AuthContext.tsx
├── types/            # TypeScript Type Definitions
│   ├── auth.ts       # Authentication types
│   └── api.ts        # API response types
├── constants/        # Application Constants
│   ├── index.ts      # Constants export
│   ├── api.ts        # API endpoints
│   ├── app.ts        # App configuration
│   └── messages.ts   # User messages
├── utils/           # Utility Functions
│   ├── index.ts     # Utils export
│   ├── date.ts      # Date formatting
│   ├── format.ts    # String/Number formatting
│   ├── validation.ts # Validation helpers
│   └── helpers.ts   # General helpers
└── lib/            # External Library Configs
    └── api-client.ts # API client configuration
```

## 🚀 **Cách sử dụng cấu trúc mới**

### **1. Import Components**

```typescript
// Thay vì:
import AdminDashboard from '../../components/admin/AdminDashboard';
import Button from '../../components/ui/Button';

// Bây giờ sử dụng:
import { AdminDashboard } from '@/components/admin';
import { Button } from '@/components/ui';

// Hoặc import tất cả từ index chính:
import { AdminDashboard, Button } from '@/components';
```

### **2. Import Services**

```typescript
// Thay vì:
import { CourseService } from '@/services/course.service';
import AuthService from '@/services/auth.service';

// Bây giờ sử dụng:
import { CourseService, AuthService } from '@/services';
```

### **3. Import Constants**

```typescript
// Sử dụng constants thay vì hardcode
import { API_ENDPOINTS, MESSAGES, APP_CONFIG } from '@/constants';

// Ví dụ:
const response = await fetch(API_ENDPOINTS.ADMIN.COURSES);
toast.success(MESSAGES.SUCCESS.SAVE);
```

### **4. Import Utils**

```typescript
import { dateUtils, validationUtils, stringUtils } from '@/utils';

// Sử dụng:
const formattedDate = dateUtils.formatDate(new Date());
const isValidEmail = validationUtils.isValidEmail(email);
const slug = stringUtils.toSlug("Khóa học lập trình");
```

## 🏗️ **Các cải tiến đã thực hiện**

### **✅ Đã hoàn thành:**
1. **Xóa file trùng lặp**: `admin-page.tsx`, `teacher-page.tsx`, `ClientNavbar.tsx`
2. **Xóa file không sử dụng**: `lib/courses.ts`, `hooks/useAuth.ts`
3. **Xóa thư mục rỗng**: `layouts/`, `utils/`
4. **Tái cấu trúc**: Di chuyển tất cả vào `src/`
5. **Tạo constants**: API endpoints, messages, app config
6. **Tạo utilities**: Date, validation, formatting helpers
7. **Tạo index.ts**: Để import dễ dàng hơn
8. **Tối ưu services**: Xóa AssignmentService trùng lặp

### **📈 Lợi ích của cấu trúc mới:**

1. **Import sạch sẽ hơn**: Sử dụng alias `@/` và index files
2. **Không hardcode**: Tất cả constants được tập trung
3. **Tái sử dụng**: Utilities có thể dùng ở nhiều nơi
4. **Dễ maintain**: Cấu trúc logic, phân tách rõ ràng
5. **TypeScript friendly**: Type definitions tập trung
6. **Performance**: Loại bỏ code không cần thiết

## 📚 **Hướng dẫn phát triển**

### **Thêm Component mới:**
```bash
# Tạo component trong thư mục phù hợp
src/components/admin/NewAdminComponent.tsx

# Cập nhật index.ts
src/components/admin/index.ts
```

### **Thêm Service mới:**
```bash
# Tạo service
src/services/new.service.ts

# Cập nhật index.ts
src/services/index.ts
```

### **Thêm Utility mới:**
```bash
# Tạo utility file
src/utils/newUtil.ts

# Cập nhật index.ts
src/utils/index.ts
```

## 🔧 **Scripts hữu ích**

```json
{
  "dev": "next dev --turbopack",
  "build": "next build --turbopack",
  "start": "next start",
  "lint": "eslint"
}
```

## 📝 **Lưu ý quan trọng**

1. **Luôn sử dụng** absolute imports với `@/`
2. **Sử dụng constants** thay vì hardcode values
3. **Import từ index files** để code gọn hơn
4. **Validate input** bằng validation utilities
5. **Format data** bằng format utilities
6. **Handle errors** properly với try-catch

---

**Cấu trúc này giúp project dễ maintain, scale và collaborate hơn! 🚀**