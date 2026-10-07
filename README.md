# Bharat ANPR

Offline-first Indian automatic number plate recognition for Android. CameraX analyzes throttled rear-camera frames, a replaceable detector proposes plate crops, quality gates reject unusable crops, Tesseract OCR reads only those crops, Indian-format logic corrects context-specific OCR errors, and an IoU tracker performs weighted multi-frame voting before Room persistence.

## Build and run

Install JDK 17 and Android SDK 35, then run `./gradlew assembleDebug`. Install `app/build/outputs/apk/debug/app-debug.apk`, grant Camera permission, and scan a well-lit, near-horizontal plate. No sign-in or network connection is used at runtime.

The checked-in detector is a small classical vertical-edge-band fallback. It carries no model-weight licensing risk and keeps the entire pipeline operational, but it is less accurate than a trained plate detector. A commercial deployment should implement `PlateDetector`, bundle weights with documented redistribution rights, bind it in `AppModule`, and validate the model against the image harness before release. Do not add weights without recording their exact source, version, checksum, and license in `MODEL_LICENSES.md`.

## Structure

- `camera`: CameraX binding, YUV conversion, frame throttling and frame-source contract.
- `detection`: detector boundary, geometry and model-free fallback.
- `processing`: crop, quality scoring and resize.
- `recognition`: replaceable OCR boundary and singleton Tesseract engine.
- `plate`: normalization, state registry, contextual correction and segmented parsing.
- `tracking`: IoU association, weighted voting and duplicate cooldown.
- `data`: Room persistence and repository.
- `settings`, `pipeline`, `ui`, `di`: configuration, orchestration, screens and dependency bindings.

See [ARCHITECTURE.md](ARCHITECTURE.md), [TESTING.md](TESTING.md), [PERFORMANCE.md](PERFORMANCE.md), [MODEL_LICENSES.md](MODEL_LICENSES.md), and [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

## Privacy and limitations

Frames stay in memory and are discarded after analysis. Full frames are never stored. Crop saving is off by default and the current release does not save crops even if the reserved setting is enabled. There is no INTERNET permission, telemetry, location, or analytics. Plates can still be personal data; define retention and access controls for the deployment jurisdiction.

Accuracy falls with distant, reflective, dirty, occluded, blurred, strongly skewed and two-line motorcycle plates. The fallback detector is intended as a safe baseline and integration point, not a substitute for field validation. Multiple simultaneous plates are accepted by the interfaces, while the fallback generally returns its single strongest region.
