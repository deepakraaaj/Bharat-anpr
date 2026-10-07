# Model licenses

| Asset | Exact source | License | Redistribution decision |
|---|---|---|---|
| `eng.traineddata` | `tesseract-ocr/tessdata_fast` (`main` snapshot fetched 2026-10-06), SHA-256 `7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2` | Apache-2.0 | Bundled; commercial redistribution permitted subject to notice/license terms. |
| Plate detector weights | None | Not applicable | No weights are silently bundled. The fallback is project source code. |
| ML Kit bundled Latin text-recognition model | `com.google.mlkit:text-recognition:16.0.1` | Google ML Kit / Google APIs Terms of Service | Bundled for offline OCR. Commercial distribution must comply with Google's terms; this is not an open-source weight artifact. |

Before replacing the detector, record the upstream repository and file URL, immutable commit/tag, SHA-256, dataset provenance, training-code license, weight license, commercial-use conditions, and an internal approval owner. A code license does not establish a weight or dataset license.
