'use client';

import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import Hero from '@/components/ui/Hero';
import Navbar from '@/components/common/Navbar';
import Footer from '@/components/common/Footer';

export default function Home() {
  const { state } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!state.loading && state.user) {
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
  }, [state.user, state.loading, router]);

  if (state.loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary"></div>
      </div>
    );
  }

  // Show landing page for unauthenticated users
  return (
    <div className="min-h-screen bg-white">
      <Navbar />
      <Hero />
      <Footer />
    </div>
  );
}
