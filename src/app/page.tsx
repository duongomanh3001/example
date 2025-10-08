'use client';

import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import Hero from '@/components/ui/Hero';
import Features from '@/components/ui/Features';

import Navbar from '@/components/common/Navbar';
import Footer from '@/components/common/Footer';

export default function Home() {
  const { state } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // Chỉ redirect khi đã hoàn thành việc kiểm tra authentication
    if (!state.loading && state.isAuthenticated && state.user) {
      // Redirect authenticated users to their respective dashboards
      switch (state.user.role) {
        case 'ADMIN':
          router.push('/admin');
          break;
        case 'TEACHER':
          router.push('/teacher');
          break;
        case 'STUDENT':
          router.push('/student');
          break;
      }
    }
  }, [state.isAuthenticated, state.user, state.loading, router]);

  if (state.loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary"></div>
      </div>
    );
  }

  // Show landing page for unauthenticated users
  return (
    <div className="min-h-screen bg-white flex flex-col">
      <Navbar />
      <main>
        <Hero />
        <Features />
      </main>
      <Footer className="mt-auto" />
    </div>
  );
}
