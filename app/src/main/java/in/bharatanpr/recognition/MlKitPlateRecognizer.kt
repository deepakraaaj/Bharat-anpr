package com.bharatanpr.recognition

import android.graphics.Bitmap
import com.bharatanpr.plate.PlateNormalizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bundled Latin model: available offline immediately and optimized for live camera text. */
@Singleton class MlKitPlateRecognizer @Inject constructor():PlateRecognizer {
    private val client=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    override suspend fun recognize(crop:Bitmap):RecognitionResult=suspendCancellableCoroutine { continuation ->
        client.process(InputImage.fromBitmap(crop,0))
            .addOnSuccessListener { result ->
                if(!continuation.isActive)return@addOnSuccessListener
                val raw=result.text
                val confidences=result.textBlocks.flatMap{it.lines}.flatMap{it.elements}.mapNotNull{it.confidence}
                val confidence=if(confidences.isEmpty())if(raw.isBlank())0f else .70f else confidences.average().toFloat().coerceIn(0f,1f)
                continuation.resume(RecognitionResult(raw,PlateNormalizer.normalize(raw),confidence))
            }
            .addOnFailureListener { error -> if(continuation.isActive)continuation.resumeWithException(error) }
    }
    override fun close(){client.close()}
}
