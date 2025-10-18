"use client";

import React, { createContext, useContext, useReducer, useEffect } from 'react';
import { AuthState, User, Role } from '@/types/auth';
import { AuthService } from '@/services/auth.service';

interface AuthContextType {
  state: AuthState;
  signIn: (usernameOrEmail: string, password: string) => Promise<void>;
  signOut: () => void;
  hasRole: (role: Role) => boolean;
  hasAnyRole: (roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

type AuthAction =
  | { type: 'SIGN_IN_START' }
  | { type: 'SIGN_IN_SUCCESS'; payload: { user: User; token: string } }
  | { type: 'SIGN_IN_ERROR'; payload: string }
  | { type: 'SIGN_OUT' }
  | { type: 'LOAD_USER'; payload: { user: User; token: string } }
  | { type: 'INITIALIZE_COMPLETE' };

const initialState: AuthState = {
  isAuthenticated: false,
  user: null,
  token: null,
  loading: true, // Bắt đầu với loading: true để tránh redirect khi F5
  error: null,
};

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'SIGN_IN_START':
      return {
        ...state,
        loading: true,
        error: null,
      };
    case 'SIGN_IN_SUCCESS':
      return {
        ...state,
        isAuthenticated: true,
        user: action.payload.user,
        token: action.payload.token,
        loading: false,
        error: null,
      };
    case 'SIGN_IN_ERROR':
      return {
        ...state,
        isAuthenticated: false,
        user: null,
        token: null,
        loading: false,
        error: action.payload,
      };
    case 'SIGN_OUT':
      return {
        ...initialState,
        loading: false, // Đặt loading: false khi đăng xuất để hiển thị trang chủ
      };
    case 'LOAD_USER':
      return {
        ...state,
        isAuthenticated: true,
        user: action.payload.user,
        token: action.payload.token,
        loading: false,
      };
    case 'INITIALIZE_COMPLETE':
      return {
        ...state,
        loading: false,
      };
    default:
      return state;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  // Load user from localStorage on app start
  useEffect(() => {
    const token = AuthService.getToken();
    const user = AuthService.getCurrentUser();
    
    if (token && user) {
      dispatch({
        type: 'LOAD_USER',
        payload: { user, token },
      });
    } else {
      // Nếu không có token/user, chỉ set loading = false
      dispatch({ type: 'INITIALIZE_COMPLETE' });
    }

    // Listen for unauthorized events (e.g., when token expires and api-client clears it)
    const handleUnauthorized = () => {
      dispatch({ type: 'SIGN_OUT' });
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const signIn = async (usernameOrEmail: string, password: string) => {
    try {
      dispatch({ type: 'SIGN_IN_START' });
      
      const response = await AuthService.signIn({
        usernameOrEmail,
        password,
      });

      const user: User = {
        id: response.id,
        username: response.username,
        email: response.email,
        fullName: response.fullName,
        studentId: response.studentId,
        role: response.role,
        isActive: true,
        createdAt: '',
        updatedAt: '',
      };

      dispatch({
        type: 'SIGN_IN_SUCCESS',
        payload: { user, token: response.token },
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Đăng nhập thất bại';
      dispatch({
        type: 'SIGN_IN_ERROR',
        payload: errorMessage,
      });
      throw error;
    }
  };

  const signOut = () => {
    AuthService.signOut();
    dispatch({ type: 'SIGN_OUT' });
  };

  const hasRole = (role: Role): boolean => {
    return state.user?.role === role;
  };

  const hasAnyRole = (roles: Role[]): boolean => {
    return state.user ? roles.includes(state.user.role) : false;
  };

  return (
    <AuthContext.Provider
      value={{
        state,
        signIn,
        signOut,
        hasRole,
        hasAnyRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
