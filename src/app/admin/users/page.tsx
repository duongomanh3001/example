"use client";

import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import UserManagement from "@/components/admin/UserManagement";
import AdminLayout from "@/components/layouts/AdminLayout";

function AdminUserManagementPage() {
  return (
    <AdminLayout>
      <UserManagement />
    </AdminLayout>
  );
}

export default withAuth(AdminUserManagementPage, {
  requiredRoles: [Role.ADMIN],
});
