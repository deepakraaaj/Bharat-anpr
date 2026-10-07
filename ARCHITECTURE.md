# Architecture

`MainActivity` owns only lifecycle/UI work. CameraX uses `KEEP_ONLY_LATEST`; `FrameThrottle` caps analysis while a single-flight guard prevents parallel OCR. `AnprPipeline` orchestrates injected interfaces and runs off the main thread:

```text
CameraX → throttle → PlateDetector → crop → quality gate → enhancement
       → PlateRecognizer → contextual parser → IoU track → weighted vote
       → duplicate cooldown → Room → Compose history
```

`PlateDetector` and `PlateRecognizer` are stable boundaries. The current detector is classical and contains no external weights. A TFLite or ONNX implementation can replace it without changing camera, parser, tracker, persistence, or UI code. The OCR engine is initialized lazily once, serialized with a mutex, and recycled with its application-scoped owner.

Voting weights OCR confidence (40%), detector confidence (20%), crop quality (15%), and format validity (25%). Three supported observations normally finalize; two can finalize only above 94%. Final confidence is derived from weighted observations and support, rather than fabricated.

The future sync seam belongs after repository insertion. Add a WorkManager HTTPS client and explicit opt-in; never make scanning depend on it.
