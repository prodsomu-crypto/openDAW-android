# openDAW Android — Development Status

Last updated: 2026-11-19
Active branch: `android/phase-1-apk-foundation`
Base commit inspected: `cb79c995af18245055363bf0a3320049dec997b4`

## Project facts

- This is the original openDAW TypeScript monorepo (AGPL-3.0-or-later), not a new DAW implementation.
- The Studio app is `packages/app/studio`; its package build runs TypeScript checking and Vite.
- Audio/DSP functionality includes the Rust/WebAssembly package `packages/studio/core-wasm`.
- The foundation branch already contains an Android Gradle application at `android/`, a Java WebView host, a WebView asset loader, microphone permission handling, and Android document-picker integration.
- The native host expects the compiled Studio website at `android/app/src/main/assets/www/index.html` and uses the AndroidX WebKit cross-origin isolation APIs for SharedArrayBuffer.
- Existing root scripts include `npm run build`, `npm run build-wasm`, and `npm test`. Root Node requirement is >=23.
- Android app configuration: application ID `org.opendaw.studio`, min SDK 26, target SDK 35, compile SDK 36, Java 17, Android Gradle Plugin 8.13.2.
- The branch is named `android/phase-1-apk-foundation` (not the display label with spaces). Do not switch to or overwrite `main`.

## Completed

- [x] Inspected repository metadata, active branches, Android foundation files, Studio package metadata, and WASM package metadata.
- [x] Confirmed the Android wrapper and the existing WebView integration are already present.
- [ ] Add/verify cloud APK build workflow (current task).
- [ ] Run the first cloud build and fix any actual build errors.

## Important constraints / known risks

- No APK has been built or installed on a physical Android device in this session yet.
- No measured Android audio latency or multitouch-device test results are available. Do not describe estimates as measurements.
- WebView SharedArrayBuffer support depends on the installed Android System WebView/Chrome version and the cross-origin-isolation configuration. The native host currently shows a compatibility message when required WebView features are unavailable.
- Oboe/AAudio has not been evaluated or integrated. Preserve the current WASM/Web Audio engine until profiling proves a native backend is necessary.
- Do not claim the app is fully professional-feature complete: tablet UI, gesture polish, background audio behavior, and physical-device QA remain to be assessed.
- The foundation branch's Android project does not yet include a Gradle wrapper in the inspected files; CI should provision Gradle explicitly unless a wrapper is added later.
- The repository contains a very large existing DAW codebase and build graph. Avoid broad rewrites.

## Next steps (in order)

1. Add a GitHub Actions workflow that installs Node 23+, Rust nightly + `wasm32-unknown-unknown` + `rust-src`, builds the original Studio app/WASM, copies the Vite output into Android assets, builds a debug APK, and uploads it as an artifact.
2. Trigger the workflow and inspect logs. Fix build issues before changing app behavior.
3. Verify APK installs and launches on a real Android tablet; record device model, Android version, WebView version, and observed behavior.
4. Audit existing touch/pointer handling and responsive CSS before adding new gestures.
5. Add focused tablet improvements without breaking mouse/keyboard workflows.
6. Add automated UI tests where practical; keep real audio latency and interruption testing explicitly device-based.
7. Investigate Android file persistence/export and background audio behavior.
8. Only consider Oboe/AAudio after measured profiling demonstrates a concrete need and a compatibility plan exists.

## Continuation instructions for another AI agent

- Read this file first, then inspect the current branch head and latest workflow run.
- Continue only on `android/phase-1-apk-foundation`.
- Preserve all original openDAW functionality and all existing commits.
- Keep commits small and buildable. Update this file whenever a milestone, blocker, or next task changes.
- Explain user-facing instructions in beginner-friendly language; the user works from an Android tablet and should not be asked to run terminal commands.
