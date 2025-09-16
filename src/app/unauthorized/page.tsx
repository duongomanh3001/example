import Link from "next/link";
import MainLayout from "@/components/layouts/MainLayout";

export default function UnauthorizedPage() {
  return (
    <MainLayout>
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-slate-900 mb-2">Không có quyền truy cập</h1>
          <Link
            href="/"
            className="inline-block bg-emerald-600 text-white px-6 py-2 rounded-md hover:bg-emerald-700"
          >
            Về trang chủ
          </Link>
        </div>
      </div>
    </MainLayout>
  );
}
