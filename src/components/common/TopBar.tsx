// Minimal top bar with contact info
import Link from "next/link";

export default function TopBar() {
  return (
  <div className="w-full bg-emerald-600 text-white text-xs">
      <div className="mx-auto max-w-7xl px-4 py-2 flex items-center justify-between gap-3">
        <div className="flex items-center gap-4">
          <span className="inline-flex items-center gap-1">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden>
              <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.8 19.8 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.08 4.18 2 2 0 0 1 4.06 2h3a2 2 0 0 1 2 1.72c.12.9.32 1.78.59 2.63a2 2 0 0 1-.45 2.11L8.09 9.91a16.5 16.5 0 0 0 6 6l1.45-1.11a2 2 0 0 1 2.11-.45c.85.27 1.73.47 2.63.59A2 2 0 0 1 22 16.92z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            <span>09 68 68 68 88</span>
          </span>
          <span className="hidden sm:inline">|</span>
          <span className="inline-flex items-center gap-1">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden>
              <path d="M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" stroke="currentColor" strokeWidth="2"/>
              <path d="m22 6-10 7L2 6" stroke="currentColor" strokeWidth="2"/>
            </svg>
            <span>manh_minh@gmail.com</span>
          </span>
        </div>
        <div className="flex items-center gap-4">
          <span className="opacity-90">Vietnamese (VI)</span>
          <Link href="/login" className="hover:underline">Đăng nhập</Link>
        </div>
      </div>
    </div>
  );
}
