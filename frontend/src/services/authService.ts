import { api } from './api';
import { AuthResponse } from '@/types';

export const authService = {
  async register(payload: { email: string; password: string; firstName: string; lastName: string }) {
    const { data } = await api.post<AuthResponse>('/auth/register', payload);
    persistAuth(data);
    return data;
  },

  async login(email: string, password: string) {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password });
    persistAuth(data);
    return data;
  },

  async logout() {
    try { await api.post('/auth/logout'); } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  getCurrentUser() {
    if (typeof window === 'undefined') return null;
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
  },

  isAuthenticated() {
    return typeof window !== 'undefined' && !!localStorage.getItem('accessToken');
  },
};

function persistAuth(data: AuthResponse) {
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  localStorage.setItem('user', JSON.stringify(data.user));
}
