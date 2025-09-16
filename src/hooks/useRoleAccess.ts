"use client";

import { useAuth } from '@/contexts/AuthContext';
import { Role } from '@/types/auth';

export function useRoleAccess() {
  const { state, hasRole, hasAnyRole } = useAuth();

  const isAdmin = () => hasRole(Role.ADMIN);
  const isTeacher = () => hasRole(Role.TEACHER);
  const isStudent = () => hasRole(Role.STUDENT);

  const canAccessAdmin = () => hasRole(Role.ADMIN);
  const canAccessTeacher = () => hasAnyRole([Role.TEACHER, Role.ADMIN]);
  const canAccessStudent = () => hasAnyRole([Role.STUDENT, Role.TEACHER, Role.ADMIN]);

  const getUserDisplayName = () => {
    if (!state.user) return '';
    return state.user.fullName || state.user.username;
  };

  const getUserRole = () => {
    return state.user?.role;
  };

  const getRoleName = (role?: Role) => {
    const userRole = role || state.user?.role;
    switch (userRole) {
      case Role.ADMIN:
        return 'Quản trị viên';
      case Role.TEACHER:
        return 'Giảng viên';
      case Role.STUDENT:
        return 'Sinh viên';
      default:
        return 'Người dùng';
    }
  };

  const getDefaultRedirectPath = () => {
    if (!state.user) return '/login';
    
    switch (state.user.role) {
      case Role.ADMIN:
        return '/admin';
      case Role.TEACHER:
        return '/teacher';
      case Role.STUDENT:
        return '/student';
      default:
        return '/';
    }
  };

  return {
    user: state.user,
    isAuthenticated: state.isAuthenticated,
    loading: state.loading,
    error: state.error,
    isAdmin,
    isTeacher,
    isStudent,
    canAccessAdmin,
    canAccessTeacher,
    canAccessStudent,
    getUserDisplayName,
    getUserRole,
    getRoleName,
    getDefaultRedirectPath,
  };
}

export default useRoleAccess;
