"use client";

import { useState, useEffect } from "react";
import { UserService } from "@/services/user.service";
import { User, Role, CreateUserRequest } from "@/types/auth";

export default function UserManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");

  // Form state for creating user
  const [newUser, setNewUser] = useState<CreateUserRequest>({
    username: "",
    email: "",
    password: "",
    fullName: "",
    studentId: "",
    role: Role.STUDENT,
  });

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const userData = await UserService.getAllUsers();
      setUsers(userData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi xảy ra");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      fetchUsers();
      return;
    }

    try {
      setLoading(true);
      const searchResults = await UserService.searchUsers(searchKeyword);
      setUsers(searchResults);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi xảy ra khi tìm kiếm");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await UserService.createUser(newUser);
      setShowCreateForm(false);
      setNewUser({
        username: "",
        email: "",
        password: "",
        fullName: "",
        studentId: "",
        role: Role.STUDENT,
      });
      fetchUsers(); // Refresh the list
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi xảy ra khi tạo người dùng");
    }
  };

  const handleToggleStatus = async (userId: number) => {
    try {
      await UserService.toggleUserStatus(userId);
      fetchUsers(); // Refresh the list
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi xảy ra khi thay đổi trạng thái");
    }
  };

  const handleDeleteUser = async (userId: number) => {
    if (!confirm("Bạn có chắc chắn muốn xóa người dùng này?")) {
      return;
    }

    try {
      await UserService.deleteUser(userId);
      fetchUsers(); // Refresh the list
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi xảy ra khi xóa người dùng");
    }
  };

  if (loading) {
    return (
      <div className="p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-4 bg-slate-200 rounded w-1/4"></div>
          <div className="h-32 bg-slate-200 rounded"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Quản lý người dùng</h1>
        <button
          onClick={() => setShowCreateForm(true)}
          className="bg-emerald-600 text-white px-4 py-2 rounded-md hover:bg-emerald-700"
        >
          Tạo người dùng mới
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-md p-4 mb-4">
          <p className="text-red-600">{error}</p>
          <button 
            onClick={() => setError(null)}
            className="text-red-600 underline text-sm"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Search */}
      <div className="flex gap-2 mb-6">
        <input
          type="text"
          placeholder="Tìm kiếm theo tên, email hoặc username..."
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          className="flex-1 border border-slate-300 rounded-md px-3 py-2"
          onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
        />
        <button
          onClick={handleSearch}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          Tìm kiếm
        </button>
        <button
          onClick={fetchUsers}
          className="bg-slate-600 text-white px-4 py-2 rounded-md hover:bg-slate-700"
        >
          Làm mới
        </button>
      </div>

      {/* Users Table */}
      <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Tên đầy đủ</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Username</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Email</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Vai trò</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Trạng thái</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-t border-slate-200">
                <td className="px-4 py-3 text-sm">{user.fullName}</td>
                <td className="px-4 py-3 text-sm">{user.username}</td>
                <td className="px-4 py-3 text-sm">{user.email}</td>
                <td className="px-4 py-3 text-sm">
                  <span className={`px-2 py-1 rounded-full text-xs ${
                    user.role === Role.ADMIN ? 'bg-red-100 text-red-700' :
                    user.role === Role.TEACHER ? 'bg-purple-100 text-purple-700' :
                    'bg-emerald-100 text-emerald-700'
                  }`}>
                    {user.role === Role.ADMIN ? 'Admin' :
                     user.role === Role.TEACHER ? 'Giảng viên' : 'Sinh viên'}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm">
                  <span className={`px-2 py-1 rounded-full text-xs ${
                    user.isActive ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                  }`}>
                    {user.isActive ? 'Hoạt động' : 'Vô hiệu hóa'}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm">
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleToggleStatus(user.id)}
                      className={`px-2 py-1 rounded text-xs ${
                        user.isActive 
                          ? 'bg-red-100 text-red-700 hover:bg-red-200' 
                          : 'bg-green-100 text-green-700 hover:bg-green-200'
                      }`}
                    >
                      {user.isActive ? 'Vô hiệu hóa' : 'Kích hoạt'}
                    </button>
                    <button
                      onClick={() => handleDeleteUser(user.id)}
                      className="px-2 py-1 rounded text-xs bg-red-100 text-red-700 hover:bg-red-200"
                    >
                      Xóa
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Create User Modal */}
      {showCreateForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-lg font-semibold mb-4">Tạo người dùng mới</h2>
            <form onSubmit={handleCreateUser} className="space-y-4">
              <input
                type="text"
                placeholder="Username"
                value={newUser.username}
                onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
                className="w-full border border-slate-300 rounded-md px-3 py-2"
                required
              />
              <input
                type="email"
                placeholder="Email"
                value={newUser.email}
                onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                className="w-full border border-slate-300 rounded-md px-3 py-2"
                required
              />
              <input
                type="password"
                placeholder="Mật khẩu"
                value={newUser.password}
                onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                className="w-full border border-slate-300 rounded-md px-3 py-2"
                required
              />
              <input
                type="text"
                placeholder="Tên đầy đủ"
                value={newUser.fullName}
                onChange={(e) => setNewUser({ ...newUser, fullName: e.target.value })}
                className="w-full border border-slate-300 rounded-md px-3 py-2"
                required
              />
              {newUser.role === Role.STUDENT && (
                <input
                  type="text"
                  placeholder="Mã sinh viên"
                  value={newUser.studentId}
                  onChange={(e) => setNewUser({ ...newUser, studentId: e.target.value })}
                  className="w-full border border-slate-300 rounded-md px-3 py-2"
                />
              )}
              <select
                value={newUser.role}
                onChange={(e) => setNewUser({ ...newUser, role: e.target.value as Role })}
                className="w-full border border-slate-300 rounded-md px-3 py-2"
              >
                <option value={Role.STUDENT}>Sinh viên</option>
                <option value={Role.TEACHER}>Giảng viên</option>
                <option value={Role.ADMIN}>Quản trị viên</option>
              </select>
              <div className="flex gap-2">
                <button
                  type="submit"
                  className="flex-1 bg-emerald-600 text-white py-2 rounded-md hover:bg-emerald-700"
                >
                  Tạo
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateForm(false)}
                  className="flex-1 bg-slate-600 text-white py-2 rounded-md hover:bg-slate-700"
                >
                  Hủy
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
