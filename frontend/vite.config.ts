import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { readFileSync } from 'fs'

const pkg = JSON.parse(readFileSync(resolve(__dirname, 'package.json'), 'utf-8'))

export default defineConfig({
  plugins: [vue()],
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
    __BUILD_TIME__:  JSON.stringify(new Date().toISOString().slice(0, 16).replace('T', ' ')),
  },

  resolve: {
    alias: { '@': resolve(__dirname, 'src') },
  },

  build: {
    // Output directly into Quarkus static resources
    outDir: resolve(__dirname, '../src/main/resources/META-INF/resources'),
    emptyOutDir: true,
  },

  server: {
    port: 5173,
    proxy: {
      // WebSocket + REST → Quarkus dev server
      '/metriq': {
        target: 'http://localhost:38790',
        ws: true,
        changeOrigin: true,
      },
      '/aivox': {
        target: 'http://localhost:38790',
        changeOrigin: true,
      },
      '/jesoos': {
        target: 'http://localhost:38790',
        changeOrigin: true,
      },
      '/stream': {
        target: 'http://localhost:38790',
        changeOrigin: true,
      },
    },
  },
})
