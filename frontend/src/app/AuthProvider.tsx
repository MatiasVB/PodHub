import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authAPI } from '../api/endpoints';
import { clearAuth } from '../api/axiosInstance';
import type { UserResponse } from '../api/types';

interface AuthContextType {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (identifier: string, password: string, rememberMe: boolean) => Promise<void>;
  logout: () => void;
  refreshUser: () => void;
  refreshTokenAndUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Load user from storage on mount
  useEffect(() => {
    const loadUser = () => {
      try {
        const rememberMe = localStorage.getItem('rememberMe') === 'true';
        const storage = rememberMe ? localStorage : sessionStorage;
        const userStr = storage.getItem('user');
        const accessToken = storage.getItem('accessToken');

        if (userStr && accessToken) {
          setUser(JSON.parse(userStr));
        }
      } catch (error) {
        console.error('Error loading user:', error);
        clearAuth();
      } finally {
        setIsLoading(false);
      }
    };

    loadUser();
  }, []);

  const login = async (identifier: string, password: string, rememberMe: boolean) => {
    try {
      const response = await authAPI.login({ identifier, password, rememberMe });
      const { accessToken, refreshToken, user: userData } = response.data;

      const storage = rememberMe ? localStorage : sessionStorage;
      storage.setItem('accessToken', accessToken);
      storage.setItem('refreshToken', refreshToken);
      storage.setItem('user', JSON.stringify(userData));
      localStorage.setItem('rememberMe', rememberMe.toString());

      setUser(userData);
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const logout = () => {
    clearAuth();
    setUser(null);
  };

  const refreshUser = () => {
    try {
      const rememberMe = localStorage.getItem('rememberMe') === 'true';
      const storage = rememberMe ? localStorage : sessionStorage;
      const userStr = storage.getItem('user');

      if (userStr) {
        setUser(JSON.parse(userStr));
      }
    } catch (error) {
      console.error('Error refreshing user:', error);
    }
  };

  const refreshTokenAndUser = async (): Promise<void> => {
    try {
      const rememberMe = localStorage.getItem('rememberMe') === 'true';
      const storage = rememberMe ? localStorage : sessionStorage;
      const refreshToken = storage.getItem('refreshToken');

      if (!refreshToken) {
        throw new Error('No refresh token available');
      }

      // Call /auth/refresh to get new token with updated authorities
      const response = await authAPI.refresh(refreshToken);
      const { accessToken, refreshToken: newRefreshToken, user: userData } = response.data;

      // Update storage with new tokens and user data
      storage.setItem('accessToken', accessToken);
      storage.setItem('refreshToken', newRefreshToken);
      storage.setItem('user', JSON.stringify(userData));

      // Update AuthProvider state
      setUser(userData);

      return Promise.resolve();
    } catch (error) {
      console.error('Error refreshing token and user:', error);
      return Promise.reject(error);
    }
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
    refreshUser,
    refreshTokenAndUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};


