"use client";
import { useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useRoleAccess } from "@/hooks/useRoleAccess";
import MainLayout from "@/components/layouts/MainLayout";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const router = useRouter();
  const { signIn, state } = useAuth();
  const { isAuthenticated, getDefaultRedirectPath } = useRoleAccess();

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      router.push(getDefaultRedirectPath());
    }
  }, [isAuthenticated, router, getDefaultRedirectPath]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    
    if (!username.trim() || !password.trim()) {
      setError("Vui lòng nhập đầy đủ thông tin");
      return;
    }

    setIsLoading(true);
    setError("");

    try {
      await signIn(username, password);
      // Redirect will happen through useEffect above
    } catch (err) {
      setError(err instanceof Error ? err.message : "Đăng nhập thất bại");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <MainLayout>
      <div className="min-h-[70vh] flex items-start md:items-center justify-center px-4">
        <div className="w-full max-w-xl mt-10 md:mt-0">
          <div className="rounded-lg border border-slate-200 bg-white shadow-sm">
            <div className="px-6 pt-6 text-center">
              <h1 className="text-primary font-bold text-xl leading-tight">
                CSCORE LOGIN
              </h1>
            </div>
            <form onSubmit={onSubmit} className="p-6 space-y-4">
              {error && (
                <div className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md">
                  {error}
                </div>
              )}
              <input
                type="text"
                placeholder="Tên tài khoản hoặc Email"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full rounded-md border border-primary-200 h-10 px-3 text-sm text-primary focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
                disabled={isLoading}
                required
              />
              <input
                type="password"
                placeholder="Mật khẩu"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-md border border-primary-200 h-10 px-3 text-sm text-primary focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
                disabled={isLoading}
                required
              />
              <button
                type="submit"
                disabled={isLoading}
                className="w-full h-10 rounded-md bg-primary text-white text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
              >
                {isLoading ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Đang đăng nhập...
                  </>
                ) : (
                  "Đăng nhập"
                )}
              </button>
              <div className="text-right">
                <a href="#" className="text-primary-600 text-sm hover:text-primary">Quên mật khẩu?</a>
              </div>
              <div className="flex items-center justify-between pt-2">
                <select className="h-9 rounded-md border border-primary-200 px-2 text-sm text-primary focus:outline-none focus:ring-2 focus:ring-primary">
                  <option>Vietnamese</option>
                  <option>English</option>
                </select>
                <button type="button" className="h-9 rounded-md border border-primary-200 px-3 text-sm text-primary hover:bg-primary-50">
                  Thông báo từ các Cookies
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </MainLayout>
  );
}
