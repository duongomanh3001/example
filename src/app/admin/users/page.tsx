"use client";

import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import UserManagement from "@/components/admin/UserManagement";

function AdminUserManagementPage() {
  return <UserManagement />;
}

export default withAuth(AdminUserManagementPage, {
  requiredRoles: [Role.ADMIN],
});
