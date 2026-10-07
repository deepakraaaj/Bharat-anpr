# Testing

Run `./gradlew lintDebug testDebugUnitTest assembleDebug assembleRelease bundleRelease`. Unit tests cover normalization, standard and Bharat formats, contextual ambiguity, invalid text, IoU, weighted voting, and cooldown boundaries. `connectedDebugAndroidTest` requires an emulator/device and covers launch/permission UI; expand it for camera hardware and rotation on the device matrix.

The image fixture contract is in `app/src/test/assets/README.md`. Add only consented or appropriately licensed images. For each fixture assert detection geometry, OCR text, validity, and final vote. A physical-device acceptance pass must cover preview lifecycle, bounding-box transform, autofocus transitions, background/foreground, memory pressure and thermal throttling.
