import {defineConfig} from "vitest/config"
import * as path from "node:path"

export default defineConfig({
    test: {
        globals: true,
        environment: "jsdom",
        // Bound parallel workers on GitHub-hosted runners to avoid Vitest RPC
        // timeouts when the full monorepo build and test suite share a small VM.
        maxWorkers: process.env.CI ? 2 : undefined,
        minWorkers: process.env.CI ? 1 : undefined
    },
    esbuild: {
        target: "ESNext"
    },
    resolve: {
        alias: {
            "@test-files": path.resolve(__dirname, "../../../test-files")
        }
    }
})