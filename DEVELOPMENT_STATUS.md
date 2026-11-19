# openDAW Android — Development Status

Last updated: 2026-11-19
Active branch: `android/phase-1-apk-foundation`
Base commit inspected: `cb79c995af18245055363bf0a3320049dec997b4`

## Project facts

- This is the original openDAW TypeScript monorepo (AGPL-3.0-or-later), not a new DAW implementation.
- The Studio app is `packages/app/studio`; its package build runs TypeScript checking and Vite.
- Audio/DSP functionality includes the Rust/WebAssembly package `packages/studio/core-wasm`.
- The branch already contains an Android Gradle application, Java WebView host, microphone permission handling, and Android document-picker integration.
- The native host loads `https://appassets.androidplatform.net/index.html`; the compiled Studio website must therefore be staged at `android/app/src/main/assets/index.html` for the root `AssetsPathHandler`.
- Root scripts include `npm run build`, `npm run build-wasm`, and `npm test`. Root Node requirement is >=23.
- Android app ID is `org.opendaw.studio`; min SDK 26, target SDK 35, compile SDK 36, Java 17, Android Gradle Plugin 8.13.2.
- Continue only on `android/phase-1-apk-foundation`. Do not switch to or overwrite `main`.

## Completed

- [x] Inspected repository metadata, Android foundation, Studio package, and WASM package.
- [x] Added `.github/workflows/android-apk.yml` for cloud WASM/Studio build, asset staging, tests, Gradle APK assembly, and artifact upload.
- [x] Confirmed the first CI run successfully installed dependencies, built the original Studio app and WebAssembly engine, and staged the WebView assets.
- [x] Diagnosed the first CI failure: 1,176 tests passed, but Vitest reported one worker RPC timeout in `@opendaw/studio-core`.
- [x] Bounded Vitest worker concurrency to 2 (minimum 1) on CI in `packages/studio/core/vitest.config.ts` to reduce runner contention.
- [x] Isolated `hit-od-render.test.ts` from automated CI with `describe.skip`: it is a developer-only diagnostic that requires an untracked personal project at a hard-coded macOS path and writes to a personal Downloads directory.

## Latest CI run

- [Android APK run #3](https://github.com/prodsomu-crypto/openDAW-android/actions/runs/44445514553) hit a Vitest worker timeout.
- [Android APK run #4](https://github.com/prodsomu-crypto/openDAW-android/actions/runs/44446759201) verified the worker-limit change removed that timeout. It reached 150 passing test files / 806 passing tests, then failed on `hit-od-render.test.ts` because it references a developer-only `/Users/.../hit.od` file that is not in the repository and a local Downloads output path.
- Fix commit: `fed08e84ead5b9d98b7e2fac7e397e2dd8bcaa1f` (bounded CI workers).
- Fix commit: `e67dc7e49e1a8e9f35957435b275e979503f8ca4` (mark the private-project diagnostic skipped in automated runs; normal audio render tests remain enabled). The next CI run will validate this change.

## Latest fix

- Corrected both Java compiler errors from run #5 in `android/app/src/main/java/org/opendaw/studio/MainActivity.java`.
- The first successful APK build exposed a physical-device launch issue: the workflow placed the site in `assets/www/`, while the root `AssetsPathHandler` maps `/index.html` to `assets/index.html`. This caused the WebView `ERR_INVALID_RESPONSE` shown on the user's tablet.
- Corrected the workflow to stage the Vite output directly into `android/app/src/main/assets/`, matching the root asset handler and preserving the app URL.
- The next tablet report showed the preloader remained visible. Inspection found the Studio Vite config uses a deployment-prefixed base URL whenever `CI=true`, unless `ANDROID_BUILD=true`; the Android workflow had not set that flag. This would make packaged script/style URLs point into a non-existent deployment folder. Set `ANDROID_BUILD=true` for the Studio/WASM build so asset URLs resolve from the local app origin. A fresh CI build and tablet launch test are required to validate both fixes.

## Important constraints / known risks

- No APK has yet been built or installed on a physical Android device.
- No measured Android audio latency or multitouch-device test results are available.
- WebView SharedArrayBuffer support depends on the installed Android System WebView/Chrome version and cross-origin-isolation configuration.
- Oboe/AAudio has not been evaluated or integrated. Preserve the current WASM/Web Audio engine until profiling proves a native backend is necessary.
- Tablet UI, gesture polish, background audio behavior, and physical-device QA remain to be assessed.
- The Android project has no Gradle wrapper in the inspected files; CI provisions Gradle explicitly.
- Avoid broad rewrites of the existing DAW codebase.

## Next steps (in order)

1. Confirm the Android-local Vite base-path and asset staging fixes in CI.
2. If tests pass, complete Gradle debug APK assembly and upload.
3. Download the rebuilt APK and verify launch on a real Android tablet; record device, Android, and WebView versions and observed behavior.
4. Audit existing touch/pointer handling and responsive CSS before adding tablet gestures.
5. Add focused tablet improvements without breaking mouse/keyboard workflows.
6. Add automated UI tests where practical; keep audio latency and interruption testing device-based.
7. Investigate Android file persistence/export and background audio behavior.
8. Consider Oboe/AAudio only after measured profiling demonstrates a concrete need.

## Continuation instructions

- Read this file, inspect the current branch head, and check the latest workflow run.
- Continue only on `android/phase-1-apk-foundation`.
- Preserve original openDAW functionality and all existing commits.
- Keep commits small and buildable. Update this file when milestones or blockers change.
- Give beginner-friendly instructions; the user works from an Android tablet and should not be asked to run terminal commands.
