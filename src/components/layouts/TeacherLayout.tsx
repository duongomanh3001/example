"use client";

import { ReactNode } from "react";
import Navbar from "@/components/common/Navbar";
import Footer from "@/components/common/Footer";
import TeacherSidebar from "../teacher/TeacherSidebar";

interface TeacherLayoutProps {
  children: ReactNode;
  className?: string;
}

export default function TeacherLayout({ children, className = "" }: TeacherLayoutProps) {
  return (
    <div className={`h-screen flex flex-col ${className}`} style={{ backgroundColor: 'rgb(187, 187, 187)' }}>
      <Navbar />
      <div className="flex flex-1 overflow-hidden">
        <TeacherSidebar />
        <main className="flex-1 bg-white flex flex-col">
          <div className="flex-1 p-6 overflow-auto">
            {children}
          </div>
        </main>
      </div>
      <Footer />
    </div>
  );
}