"use client";

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { Role } from '@/types/auth';

interface WithAuthProps {
  requiredRoles?: Role[];
  redirectTo?: string;
}

export function withAuth<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  options: WithAuthProps = {}
) {
  const { requiredRoles, redirectTo = '/login' } = options;

  return function AuthenticatedComponent(props: P) {
    const { state, hasAnyRole } = useAuth();
    const router = useRouter();

    useEffect(() => {
      // If not authenticated, redirect to login
      if (!state.isAuthenticated && !state.loading) {
        router.push(redirectTo);
        return;
      }

      // If authenticated but doesn't have required roles, redirect to unauthorized page
      if (
        state.isAuthenticated &&
        requiredRoles &&
        requiredRoles.length > 0 &&
        !hasAnyRole(requiredRoles)
      ) {
        router.push('/unauthorized');
        return;
      }
    }, [state.isAuthenticated, state.loading, router]);

    // Show loading while checking authentication
    if (state.loading) {
      return (
        <div className="min-h-screen flex items-center justify-center">
          <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-emerald-600"></div>
        </div>
      );
    }

    // Show nothing while redirecting
    if (!state.isAuthenticated) {
      return null;
    }

    // Show unauthorized message if user doesn't have required roles
    if (
      requiredRoles &&
      requiredRoles.length > 0 &&
      !hasAnyRole(requiredRoles)
    ) {
      return null;
    }

    return <WrappedComponent {...props} />;
  };
}

export default withAuth;
