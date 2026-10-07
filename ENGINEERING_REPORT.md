# Engineering report

## TL;DR

Bharat ANPR is a buildable offline Android application with CameraX preview/analysis, crop-only Tesseract OCR, segmented Indian registration parsing, contextual OCR correction, IoU tracking, evidence-weighted temporal voting, duplicate suppression, Room history, persistent settings, Compose screens, Hilt wiring, R8 release output, tests, CI, and license documentation. It has no INTERNET permission, telemetry, account, paid API, or detector weights of uncertain provenance.

## Architecture and libraries

The data flow is CameraX → throttling/single-flight guard → replaceable `PlateDetector` → quality gate/crop/enhancement → replaceable `PlateRecognizer` → parser/correction/validation → IoU tracker → weighted vote → configurable duplicate cooldown → Room → Compose UI. Hilt constructs application-scoped detector, OCR, database, repository, and pipeline objects. Coroutines keep analysis off the UI thread; DataStore persists settings.

CameraX provides maintained camera lifecycle and backpressure; Compose/Material 3 provides UI; Room provides transactional local persistence; Hilt provides testable bindings; Coroutines/Flow provides cancellation and observable state; Tesseract4Android provides offline OCR. Exact versions and license inventory are in `THIRD_PARTY_LICENSES.md`.

## Licensing and models

Tesseract4Android 4.9.0 and the bundled `tessdata_fast` English model use permissive licenses documented in `MODEL_LICENSES.md`; the bundled model SHA-256 is recorded there. The detector uses project-owned classical edge-band source and no weights. This avoids asserting rights for an unverified ANPR checkpoint. A fleet release should replace it through `PlateDetector` only after verifying the weights and training-data provenance.

## Recognition behavior

The parser handles 1–2 digit district codes, 1–3 letter series, 1–4 digit vehicle numbers, validated State/UT prefixes, and `22BH1234AA` Bharat-series plates. Correction is segment-aware: ambiguous characters convert only where a parsed segment expects digits or letters. The original OCR remains in Room for diagnosis.

Tracking associates detections by IoU and expires stale tracks after 1.5 seconds. Voting combines OCR confidence (40%), detector confidence (20%), crop quality (15%), and format validity (25%). Three observations normally finalize; two require confidence of at least 94%. A configurable cooldown prevents repeat database events while the UI remains live.

## Verification results

Environment: Linux host, OpenJDK 17, Android SDK platform 36/build tools 35, Gradle 8.13. On 2026-10-06, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`, and `bundleRelease` passed. Eight JVM tests passed with zero failures. R8 and resource shrinking completed successfully. Debug APK: 39,109,533 bytes; unsigned release APK: 19,411,488 bytes; release AAB: 12,461,786 bytes. CI runs the same lint/test/debug/release build stages.

No emulator or physical Android device was connected, so instrumentation tests, live camera preview, OCR accuracy, bounding-box alignment across all aspect ratios, lifecycle/background behavior, thermal behavior, memory, and real latency were not executed. Run `./gradlew connectedDebugAndroidTest`, install the debug APK on an arm64 device, and follow `TESTING.md`. Expected behavior is permission UI followed by a smooth rear-camera preview, yellow boxes on plausible plates, multi-frame status before finalization, one history row within the cooldown, and analysis stopping with the activity lifecycle.

## Known limitations and production risks

The classical detector is deliberately conservative and materially less accurate than a plate-specific ML detector, especially for two-line motorcycle plates, skew, night glare, motion blur, multiple vehicles, distant plates, and occlusion. Tesseract's general English weights are commercially redistributable but not trained specifically for Indian plate crops. The crop-saving, sound, vibration, retention cleanup, and future sync switches/seams are present but their operational implementations are not enabled. Device benchmarks and a legally sourced representative image corpus remain required before commercial deployment. Bounding-box mapping uses rotated analysis dimensions; device validation is still needed for CameraX center-crop differences.

Recommended next work is to license or train a small plate detector and plate-character recognizer on a documented dataset, add immutable model checksums and golden-image tests, implement CameraX transform matrices for exact overlay mapping, benchmark on target low/mid-range devices, add retention cleanup and encrypted-at-rest policy if the deployment requires it, and complete a field accuracy/false-positive study.
