'use client';

import { Suspense } from 'react';
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import AdminDashboard from '@/components/admin/AdminDashboard';

function AdminPage() {
  return (
    <Suspense fallback={
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    }>
      <AdminDashboard />
    </Suspense>
  );
}

export default withAuth(AdminPage, {
  requiredRoles: [Role.ADMIN],
});
