import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from './types';

const axiosInstance = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

// Request interceptor to add token
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const rememberMe = localStorage.getItem('rememberMe') === 'true';
    const storage = rememberMe ? localStorage : sessionStorage;
    const token = storage.getItem('accessToken');

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token refresh
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => {
            return axiosInstance(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const rememberMe = localStorage.getItem('rememberMe') === 'true';
      const storage = rememberMe ? localStorage : sessionStorage;
      const refreshToken = storage.getItem('refreshToken');

      if (!refreshToken) {
        // No refresh token, redirect to login
        clearAuth();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const response = await axios.post<AuthResponse>(
          '/api/auth/refresh',
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data;

        storage.setItem('accessToken', accessToken);
        storage.setItem('refreshToken', newRefreshToken);
        storage.setItem('user', JSON.stringify(response.data.user));

        processQueue(null, accessToken);
        isRefreshing = false;

        return axiosInstance(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error, null);
        isRefreshing = false;
        clearAuth();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export const clearAuth = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('refreshToken');
  sessionStorage.removeItem('user');
};

export default axiosInstance;


