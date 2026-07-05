import axios from 'axios';
import { useAuthStore } from '../store/authStore';

// Cliente Axios central. La base '/api' se proxya al backend en dev (vite.config.js)
// y se sirve tras Nginx en produccion.
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Interceptor de request: adjunta el JWT si hay sesion activa (RS-04).
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor de response: ante 401 (token expirado/invalido) cierra la sesion
// y redirige al login (RNF-06: expiracion de sesion).
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      useAuthStore.getState().logout();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
