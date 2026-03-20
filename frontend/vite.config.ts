import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],

  build: {
    // Output directly into Quarkus static resources
    outDir: resolve(__dirname, '../src/main/resources/META-INF/resources'),
    // Step 7: flip to true once old index.html + CDN files are removed
    emptyOutDir: false,
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
