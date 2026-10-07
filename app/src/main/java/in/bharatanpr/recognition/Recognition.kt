package com.bharatanpr.recognition

import android.graphics.Bitmap

data class RecognitionResult(val rawText: String, val normalizedText: String, val confidence: Float)
data class OcrConfig(val minimumConfidence: Float = .55f, val whitelist: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
interface PlateRecognizer : AutoCloseable { suspend fun recognize(crop: Bitmap): RecognitionResult; override fun close() {} }
