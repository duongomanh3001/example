// 

"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import AdminCourseManagement from "@/components/admin/AdminCourseManagement";
import CsvUploadModal from "@/components/admin/CsvUploadModal";
import { UserService } from "@/services/user.service";
import { CourseService } from "@/services/course.service";
import { Role } from "@/types/auth";
import { Toaster } from "react-hot-toast";
import MainLayout from "@/components/layouts/MainLayout";
import Image from "next/image";

interface DashboardStats {
  totalUsers: number;
  totalStudents: number;
  totalTeachers: number;
  totalCourses: number;
  totalAssignments: number;
  activeCourses: number;
}

export default function AdminDashboard() {
  const { state, hasRole } = useAuth();
  const [activeTab, setActiveTab] = useState("overview");
  const [csvModalOpen, setCsvModalOpen] = useState(false);
  const [csvModalType, setCsvModalType] = useState<
    "teachers" | "students" | "enrollment"
  >("teachers");
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0,
    totalStudents: 0,
    totalTeachers: 0,
    totalCourses: 0,
    totalAssignments: 0,
    activeCourses: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDashboardStats();
  }, []);

  const loadDashboardStats = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const [users, courses] = await Promise.all([
        UserService.getAllUsers(),
        CourseService.getAllCourses(),
      ]);

      const students = users.filter((user) => user.role === Role.STUDENT);
      const teachers = users.filter((user) => user.role === Role.TEACHER);
      const activeCourses = courses.filter((course) => course.isActive);

      setStats({
        totalUsers: users.length,
        totalStudents: students.length,
        totalTeachers: teachers.length,
        totalCourses: courses.length,
        totalAssignments: 0, // fetch riêng
        activeCourses: activeCourses.length,
      });
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Có lỗi xảy ra khi tải dữ liệu"
      );
    } finally {
      setIsLoading(false);
    }
  };

  // Redirect nếu không phải Admin
  if (!hasRole(Role.ADMIN)) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-600 mb-2">
            Truy cập bị từ chối
          </h1>
          <p className="text-slate-600">
            Bạn không có quyền truy cập trang này.
          </p>
        </div>
      </div>
    );
  }

  // Tabs menu
  const tabs = [
    { id: "overview", label: "Tổng quan", icon: "/icon/chart-pie-solid-full.svg" },
    { id: "courses", label: "Quản lý khóa học", icon: "/icon/graduation-cap-solid-full.svg" },
    { id: "users", label: "Quản lý người dùng", icon: "/icon/users-gear-solid-full.svg" },
    { id: "reports", label: "Báo cáo", icon: "/icon/chart-line-solid-full.svg" },
    { id: "download", label: "Tải xuống", icon: "/icon/cloud-arrow-down-solid-full.svg" }, // thêm tab download
  ];

  const renderOverview = () => (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">
                Tổng số người dùng
              </p>
              <p className="text-2xl font-bold text-slate-900">
                {stats.totalUsers}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <p className="text-sm font-medium text-slate-500">Sinh viên</p>
          <p className="text-2xl font-bold text-slate-900">
            {stats.totalStudents}
          </p>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <p className="text-sm font-medium text-slate-500">Giáo viên</p>
          <p className="text-2xl font-bold text-slate-900">
            {stats.totalTeachers}
          </p>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <p className="text-sm font-medium text-slate-500">Tổng số khóa học</p>
          <p className="text-2xl font-bold text-slate-900">
            {stats.totalCourses}
          </p>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <p className="text-sm font-medium text-slate-500">
            Khóa học đang hoạt động
          </p>
          <p className="text-2xl font-bold text-slate-900">
            {stats.activeCourses}
          </p>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-6">
          <p className="text-sm font-medium text-slate-500">Tổng số bài tập</p>
          <p className="text-2xl font-bold text-slate-900">
            {stats.totalAssignments}
          </p>
        </div>
      </div>
    </div>
  );

  const renderUserManagement = () => (
    <div className="space-y-6">
      {/* CSV Import */}
      <div className="bg-white rounded-lg border border-slate-200 p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">Import CSV</h3>
        <p className="text-slate-600 mb-6">
          thêm người dùng bằng file csv
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            onClick={() => {
              setCsvModalType("teachers");
              setCsvModalOpen(true);
            }}
            className="p-4 bg-purple-50 border border-purple-200 rounded-lg hover:bg-purple-100 transition-colors text-left"
          >
            Import giáo viên
          </button>
          <button
            onClick={() => {
              setCsvModalType("students");
              setCsvModalOpen(true);
            }}
            className="p-4 bg-green-50 border border-green-200 rounded-lg hover:bg-green-100 transition-colors text-left"
          >
            Import sinh viên
          </button>
        </div>
      </div>
    </div>
  );

  const renderReports = () => (
    <div className="bg-white rounded-lg border border-slate-200 p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">
        Báo cáo hệ thống
      </h3>
      <p className="text-slate-600">Chức năng báo cáo sẽ được triển khai sau...</p>
    </div>
  );

  const renderDownload = () => (
    <div className="bg-white rounded-lg border border-slate-200 p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">Tải xuống dữ liệu</h3>
      <p className="text-slate-600">Tính năng tải xuống CSV/Excel sẽ được triển khai sau...</p>
    </div>
  );

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <MainLayout>
      <div className="min-h-screen bg-slate-50">
        <Toaster position="top-right" />
        <div className="max-w-7xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-slate-900 mb-2">
              Bảng điều khiển Quản trị viên
            </h1>
            <p className="text-slate-600">
              Chào mừng trở lại, {state.user?.fullName}!
            </p>
          </div>

          {/* Error */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md mb-6">
              {error}
              <button
                onClick={() => setError(null)}
                className="ml-4 text-red-900"
              >
                ×
              </button>
            </div>
          )}

          {/* Tabs */}
          <div className="border-b border-slate-200 mb-8">
            <nav className="-mb-px flex space-x-8">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors flex items-center ${
                    activeTab === tab.id
                      ? "border-emerald-500 text-emerald-600"
                      : "border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300"
                  }`}
                >
                  <Image
                    src={tab.icon}
                    alt={tab.label}
                    width={18}
                    height={18}
                    className="mr-2"
                  />
                  {tab.label}
                </button>
              ))}
            </nav>
          </div>

          {/* Tab content */}
          <div>
            {activeTab === "overview" && renderOverview()}
            {activeTab === "courses" && <AdminCourseManagement />}
            {activeTab === "users" && renderUserManagement()}
            {activeTab === "reports" && renderReports()}
            {activeTab === "download" && renderDownload()}
          </div>

          {/* CSV Modal */}
          <CsvUploadModal
            isOpen={csvModalOpen}
            onClose={() => setCsvModalOpen(false)}
            type={csvModalType}
            onSuccess={() => {
              loadDashboardStats();
            }}
          />
        </div>
      </div>
    </MainLayout>
  );
}
