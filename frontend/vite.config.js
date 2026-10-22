import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En desarrollo, /api se reenvía al backend Spring Boot (puerto 8080).
// Así el navegador solo habla con un origen y no hay problemas de CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.MECAUTO_API || 'http://localhost:8080', changeOrigin: true },
    },
  },
});
