# Bharat ANPR — Morning Handoff

Date: 2026-10-06  
Workspace: `/home/deepakrajb/Desktop/OCR`

## Current working state

The latest debug build is installed on the connected OPPO CPH2603. Camera preview, offline OCR, Indian plate parsing, multi-frame finalization, duplicate suppression, and Room history are operational.

Verified on the physical device:

- CameraX preview opens and remains stable.
- Analysis is capped at 2 FPS to avoid preview stutter.
- Multiple plate-shaped regions are evaluated instead of only the strongest edge region.
- Invalid surrounding text is rejected by Indian plate validation.
- Raw unstable OCR fragments no longer replace a valid displayed plate.
- Two matching valid observations finalize a result.
- `JH03MF4477` reached multi-frame verification.
- `DL09U9111` finalized and displayed as recognized.
- A tilted/two-line sample finalized as `KL39K8288` with 77% evidence confidence.
- The difficult yellow commercial plate is expected to normalize to `RJ45CM2308`; ML Kit produced close variants such as `RJL5CM2308`, and contextual district correction now maps the open-top `4` correctly.

## OCR implementation

Primary OCR is now the bundled ML Kit Latin recognizer:

```kotlin
implementation("com.google.mlkit:text-recognition:16.0.1")
```

The model is statically packaged and works offline. Google states ML Kit image processing happens on-device. Its SDK and model are governed by the Google ML Kit and Google APIs terms, not an open-source model license.

Tesseract remains in the project but is no longer the injected primary recognizer. It can be removed after ML Kit field validation to reduce APK size.

## Recognition pipeline

```text
CameraX
  → ImageProxy.toBitmap()
  → 2 FPS throttle
  → top-three plate-shaped edge bands
  → expanded crop
  → quality filtering
  → ML Kit OCR
  → Otsu fallback
  → ±10° rotation fallback
  → noisy-prefix/suffix recovery
  → contextual character correction
  → Indian registration validation
  → IoU tracking
  → two-frame weighted voting
  → duplicate suppression
  → Room history
```

## Resume commands

Check the device:

```bash
$ANDROID_HOME/platform-tools/adb devices -l
```

Build and run JVM tests:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest assembleDebug
```

Install the latest debug APK:

```bash
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch it:

```bash
$ANDROID_HOME/platform-tools/adb shell am force-stop com.bharatanpr.debug
$ANDROID_HOME/platform-tools/adb shell monkey -p com.bharatanpr.debug -c android.intent.category.LAUNCHER 1
```

Watch recognition diagnostics:

```bash
$ANDROID_HOME/platform-tools/adb logcat -c
$ANDROID_HOME/platform-tools/adb logcat -v brief | rg 'BharatANPR|FATAL EXCEPTION'
```

Run the connected-device test:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew connectedDebugAndroidTest
```

## Morning test sequence

Use one large plate image at a time. Keep it centered and steady for 2–3 seconds.

1. Confirm `DL09U9111` finalizes.
2. Confirm `JH03MF4477` finalizes.
3. Retest the yellow tilted plate and expect `RJ45CM2308`.
4. Test one real white car plate in daylight.
5. Test one yellow commercial plate in daylight.
6. Test a motorcycle/two-line plate.
7. Open History and verify only one row is stored during the cooldown.
8. Background and reopen the app; verify the camera closes and reopens cleanly.
9. Rotate portrait → landscape → portrait and inspect overlay alignment.

## Settings still to finish

- Decide whether the user-facing inference setting should remain configurable or be replaced by adaptive presets.
- Implement actual sound and vibration behavior after final recognition.
- Implement crop-image saving when enabled.
- Implement retention cleanup using the configured retention period.
- Hide or remove settings that are currently only reserved infrastructure.
- Add a reset-to-defaults action.
- Add a selector for rear/front camera only if genuinely needed.
- Add an accuracy/performance mode selector:
  - Fast: one OCR pass, 2 FPS.
  - Balanced: threshold fallback, 2 FPS.
  - Difficult plate: threshold plus rotation variants, 1 FPS.

## Important remaining engineering work

The current detector is a classical edge-band fallback. It is useful and functional but cannot provide FASTag-grade accuracy in every condition. Production accuracy requires a plate-specific detector and preferably a plate-specific character recognizer trained on a legally usable Indian dataset.

Before bundling another model, record:

- Exact repository and immutable commit/tag
- Model file URL and SHA-256
- Weight license
- Training-code license
- Training dataset provenance and commercial rights
- Supported plate types and measured field accuracy

Do not add YOLO or other weights merely because the source repository has an open-source code license; model weights and training datasets require separate verification.

## Known limitations

- Strong glare, night lighting, extreme perspective, motion blur, distant plates, decorative fonts, and occlusion remain difficult.
- The multi-candidate classical detector can select nearby text, although format validation rejects most false positives.
- Multiple OCR fallbacks increase latency on difficult plates.
- Exact CameraX preview-to-analysis overlay transforms still need validation across aspect ratios.
- The yellow `RJ45CM2308` case needs a final physical-device confirmation after the latest multi-candidate build.
- ML Kit may emit limited operational metrics under Google’s terms; review disclosure requirements before commercial release.

## Files to review first

- `app/src/main/java/in/bharatanpr/detection/HeuristicPlateDetector.kt`
- `app/src/main/java/in/bharatanpr/recognition/MlKitPlateRecognizer.kt`
- `app/src/main/java/in/bharatanpr/pipeline/AnprPipeline.kt`
- `app/src/main/java/in/bharatanpr/plate/PlateLogic.kt`
- `app/src/main/java/in/bharatanpr/tracking/Tracking.kt`
- `app/src/main/java/in/bharatanpr/ui/MainViewModel.kt`
- `MODEL_LICENSES.md`
- `THIRD_PARTY_LICENSES.md`
- `ENGINEERING_REPORT.md`

## Last known build status

- JVM tests: passing
- Debug APK: building successfully
- Connected instrumentation launch test: passing after Hilt test-rule fix
- Physical camera preview: verified
- ML Kit OCR: verified on-device
- Room database created on-device
- Release build should be rerun after the ML Kit dependency change:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew lintDebug assembleRelease bundleRelease
```
