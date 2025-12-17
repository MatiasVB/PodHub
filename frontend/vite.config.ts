import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0', // Required for Docker - allows external connections
    port: 5173,
    proxy: {
      '/api': {
        // Use environment variable if available, otherwise default to localhost
        // In Docker, this will use the backend service name
        target: process.env.VITE_API_BASE_URL || 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
