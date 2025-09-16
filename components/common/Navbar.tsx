"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useRoleAccess } from "@/hooks/useRoleAccess";
import { useAuth } from "@/contexts/AuthContext";
import { Role } from "@/types/auth";

export default function Navbar() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    // Return a placeholder navbar during SSR
    return (
      <header className="sticky top-0 z-40 bg-white/90 backdrop-blur border-b border-slate-200">
        <div className="mx-auto max-w-7xl px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2 font-semibold">
            <div className="w-7 h-7 bg-gray-200 rounded animate-pulse"></div>
            <span className="hidden sm:inline">CSCORE</span>
          </div>
          <nav className="flex items-center gap-6 text-sm text-slate-700">
            <span>Trang chủ</span>
          </nav>
        </div>
      </header>
    );
  }
  const pathname = usePathname();
  const router = useRouter();
  const { isAuthenticated, getUserDisplayName, getRoleName, canAccessAdmin, canAccessTeacher } = useRoleAccess();
  const { signOut } = useAuth();

  const handleSignOut = () => {
    signOut();
    router.push('/');
  };

  return (
    <header className="sticky top-0 z-40 bg-white/90 backdrop-blur border-b border-slate-200">
      <div className="mx-auto max-w-7xl px-4 h-16 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 font-arial">
          <Image src="/cscore.png" alt="CSCORE" width={30} height={30} />
        </Link>
        <nav className="flex items-center gap-6 text-sm text-primary">
          <Link href="/" className={`hover:text-primary-600 ${pathname === "/" ? "text-primary-600 font-medium" : ""}`}>
            Trang chủ
          </Link>
          
          {isAuthenticated ? (
            <>
              {canAccessAdmin() && (
                <Link href="/admin" className={`hover:text-primary-600 ${pathname.startsWith("/admin") ? "text-primary-600 font-medium" : ""}`}>
                  Quản trị
                </Link>
              )}
              {canAccessTeacher() && (
                <Link href="/teacher" className={`hover:text-primary-600 ${pathname.startsWith("/teacher") ? "text-primary-600 font-medium" : ""}`}>
                  Giảng dạy
                </Link>
              )}
              <Link href="/student" className={`hover:text-primary-600 ${pathname.startsWith("/student") ? "text-primary-600 font-medium" : ""}`}>
                Khóa học
              </Link>
              
              {/* User Menu */}
              <div className="relative group">
                <button className="flex items-center gap-2 hover:text-primary-600 px-3 py-2 rounded-md hover:bg-primary-50">
                  <div className="w-6 h-6 bg-primary rounded-full flex items-center justify-center text-white text-xs font-semibold">
                    {getUserDisplayName().charAt(0).toUpperCase()}
                  </div>
                  <span className="hidden md:inline">{getUserDisplayName()}</span>
                </button>
                
                {/* Dropdown Menu */}
                <div className="absolute right-0 top-full mt-1 w-64 bg-white border border-primary-200 rounded-md shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
                  <div className="p-3 border-b border-primary-100">
                    <div className="font-medium text-primary">{getUserDisplayName()}</div>
                    <div className="text-sm text-primary-400">{getRoleName()}</div>
                  </div>
                  <div className="py-2">
                    <button
                      onClick={handleSignOut}
                      className="w-full text-left px-3 py-2 text-sm hover:bg-primary-50 text-red-600"
                    >
                      Đăng xuất
                    </button>
                  </div>
                </div>
              </div>
            </>
          ) : (
            <>
              <Link href="/student" className={`hover:text-primary-600 ${pathname.startsWith("/student") ? "text-primary-600 font-medium" : ""}`}>
                Khóa học
              </Link>
              <Link href="/login" className="bg-primary text-white px-4 py-2 rounded-md hover:bg-primary-600">
                Đăng nhập
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
