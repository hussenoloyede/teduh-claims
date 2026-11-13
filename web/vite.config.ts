/// <reference types="vitest/config" />
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';
import react from '@vitejs/plugin-react';
import { playwright } from '@vitest/browser-playwright';
import { defineConfig } from 'vite';

const dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],

  // In development the page runs on port 5173 and Spring Boot on 8080.
  // Vite forwards /api calls to Spring Boot, so the browser sees one site
  // and the session cookie and CSRF token work as they do in production.
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },

  // Every story is also a test: it renders in a headless Chromium,
  // runs its play function, and runs the accessibility checks.
  test: {
    projects: [
      {
        extends: true,
        plugins: [storybookTest({ configDir: path.join(dirname, '.storybook') })],
        test: {
          name: 'storybook',
          browser: {
            enabled: true,
            // Vitest's default port (63315) sits in a range Windows keeps for
            // Hyper-V and WSL, so it fails with EACCES. Use a fixed low port.
            api: { host: '127.0.0.1', port: 5199, strictPort: true },
            headless: true,
            provider: playwright({}),
            instances: [{ browser: 'chromium' }],
          },
        },
      },
    ],
  },
});
