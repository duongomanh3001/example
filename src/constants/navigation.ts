// Navigation constants for different user roles

export interface NavigationItem {
  href: string;
  label: string;
  icon: string;
}

export const studentNavigation: NavigationItem[] = [
  {
    href: "/student",
    label: "Dashboard", 
    icon: "dashboard"
  },
  {
    href: "/student/courses",
    label: "Khóa học của tôi",
    icon: "courses"
  },
  {
    href: "/student/assignments", 
    label: "Bài tập",
    icon: "assignments"
  },
  {
    href: "/student/results",
    label: "Kết quả", 
    icon: "results"
  },
  {
    href: "/notifications",
    label: "Thông báo",
    icon: "notifications"
  },
  {
    href: "/settings",
    label: "Cài đặt",
    icon: "settings"
  }
];

export const teacherNavigation: NavigationItem[] = [
  {
    href: "/teacher",
    label: "Dashboard",
    icon: "dashboard"
  },
  {
    href: "/teacher/courses",
    label: "Khóa học",
    icon: "courses"
  },
  {
    href: "/teacher/assignments",
    label: "Bài tập",
    icon: "assignments"
  },
  {
    href: "/teacher/assignment/create",
    label: "Tạo bài tập",
    icon: "create"
  },
  {
    href: "/teacher/students", 
    label: "Học viên",
    icon: "students"
  },
  {
    href: "/teacher/reports",
    label: "Báo cáo", 
    icon: "reports"
  },
  {
    href: "/notifications",
    label: "Thông báo",
    icon: "notifications"
  },
  {
    href: "/settings",
    label: "Cài đặt",
    icon: "settings"
  }
];

export const adminNavigation: NavigationItem[] = [
  {
    href: "/admin",
    label: "Dashboard",
    icon: "dashboard"
  },
  {
    href: "/admin/users",
    label: "Quản lý người dùng",
    icon: "users"
  },
  {
    href: "/admin/courses",
    label: "Quản lý khóa học",
    icon: "courses"
  },
  {
    href: "/admin/system",
    label: "Cài đặt hệ thống",
    icon: "settings"
  },
  {
    href: "/notifications",
    label: "Thông báo",
    icon: "notifications"
  },
  {
    href: "/settings",
    label: "Cài đặt cá nhân",
    icon: "user-settings"
  }
];