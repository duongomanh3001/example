"use client";

import { ReactNode } from "react";
import Navbar from "@/components/common/Navbar";
import Footer from "@/components/common/Footer";
import StudentSidebar from "../student/StudentSidebar";

interface StudentLayoutProps {
  children: ReactNode;
  className?: string;
}

export default function StudentLayout({ children, className = "" }: StudentLayoutProps) {
  return (
    <div className={`h-screen flex flex-col ${className}`} style={{ backgroundColor: 'rgb(187, 187, 187)' }}>
      <Navbar />
      <div className="flex flex-1 overflow-hidden">
        <StudentSidebar />
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