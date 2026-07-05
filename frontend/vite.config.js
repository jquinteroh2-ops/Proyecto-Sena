import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Configuracion de Vite. En dev, /api se proxya al backend (evita CORS).
// La URL del backend es configurable por VITE_API_URL para docker/prod.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
