"use client";

import { ReactNode } from "react";
import Navbar from "@/components/common/Navbar";
import Footer from "@/components/common/Footer";

interface MainLayoutProps {
  children: ReactNode;
  className?: string;
}

export default function MainLayout({ children, className = "" }: MainLayoutProps) {
  return (
    <div className={`min-h-screen flex flex-col ${className}`} style={{ backgroundColor: 'rgb(187, 187, 187)' }}>
      <Navbar />
      <main className="flex-1 bg-white">
        {children}
      </main>
      <Footer />
    </div>
  );
}
