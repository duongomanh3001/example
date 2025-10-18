"use client";

import { ReactNode } from "react";
import Navbar from "@/components/common/Navbar";
import Footer from "@/components/common/Footer";

interface AdminLayoutProps {
  children: ReactNode;
  className?: string;
}

export default function AdminLayout({ children, className = "" }: AdminLayoutProps) {
  return (
    <div className={`min-h-screen flex flex-col ${className}`} style={{ backgroundColor: 'rgb(187, 187, 187)' }}>
      <Navbar />
      <main className="flex-1 bg-white">
        <div className="p-6">
          {children}
        </div>
      </main>
      <Footer />
    </div>
  );
}