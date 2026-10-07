# Performance

The normal default is six analyses per second, `KEEP_ONLY_LATEST`, single-flight processing, one detector instance, one serialized OCR instance, and OCR on accepted crops only. Debug overlay exposes detector, OCR, and total latency.

No representative device was attached when this repository was created, so latency, thermal behavior, memory, APK size, and camera FPS require measurement on release hardware. Record device model, SoC, RAM, Android version, lighting, plate distance, median/P95 latency, sustained 10-minute temperature, dropped-frame rate, RSS, APK/AAB size, and accuracy. Expected pass behavior is a smooth preview, stable memory, analysis near the configured cap, and automatic lifecycle stop when the activity leaves the foreground.
