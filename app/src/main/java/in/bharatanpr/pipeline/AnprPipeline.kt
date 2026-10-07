package com.bharatanpr.pipeline

import android.graphics.Bitmap
import com.bharatanpr.data.local.PlateEntity
import com.bharatanpr.data.repository.PlateRepository
import com.bharatanpr.detection.*
import com.bharatanpr.plate.IndianPlateParser
import com.bharatanpr.plate.ParsedPlate
import com.bharatanpr.processing.*
import com.bharatanpr.recognition.PlateRecognizer
import com.bharatanpr.recognition.RecognitionResult
import com.bharatanpr.tracking.*
import com.bharatanpr.util.AnprLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.floor

data class PipelineMetrics(val detectorMs: Long = 0, val ocrMs: Long = 0, val totalMs: Long = 0)

data class PlateCandidate(
    val plateNumber: String,
    val rawOcrText: String,
    val recognitionConfidence: Float,
    val detectorConfidence: Float,
    val firstSeen: Long,
    val lastSeen: Long
)

data class PipelineResult(
    val detections: List<Detection> = emptyList(),
    val candidate: String? = null,
    val finalized: String? = null,
    val confidence: Float = 0f,
    val status: String = "Scanning",
    val metrics: PipelineMetrics = PipelineMetrics(),
    val sourceWidth: Int = 1,
    val sourceHeight: Int = 1,
    val selectableCandidates: List<PlateCandidate> = emptyList()
)

@Singleton
class AnprPipeline @Inject constructor(
    private val detector: PlateDetector,
    private val recognizer: PlateRecognizer,
    private val repository: PlateRepository
) {
    private val tracker = PlateTracker()
    private val duplicates = DuplicateSuppressor()

    suspend fun accept(candidate: PlateCandidate, duplicateCooldownMs: Long): Boolean {
        val now = System.currentTimeMillis()
        if (!duplicates.shouldAccept(candidate.plateNumber, now, duplicateCooldownMs)) return false
        repository.save(
            PlateEntity(
                plateNumber = candidate.plateNumber,
                rawOcrText = candidate.rawOcrText,
                recognitionConfidence = candidate.recognitionConfidence,
                detectorConfidence = candidate.detectorConfidence,
                timestamp = now,
                firstSeen = candidate.firstSeen,
                lastSeen = candidate.lastSeen
            )
        )
        return true
    }

    private fun preferRecognition(
        candidate: RecognitionResult,
        candidateParsed: ParsedPlate,
        current: RecognitionResult?,
        currentParsed: ParsedPlate?
    ): Boolean {
        if (currentParsed == null) return true
        val candidateComplete = IndianPlateParser.isCompleteScanResult(candidateParsed)
        val currentComplete = IndianPlateParser.isCompleteScanResult(currentParsed)
        if (candidateComplete != currentComplete) return candidateComplete
        if (candidateParsed.validationConfidence != currentParsed.validationConfidence) {
            return candidateParsed.validationConfidence > currentParsed.validationConfidence
        }
        if (candidateParsed.normalized.length != currentParsed.normalized.length) {
            return candidateParsed.normalized.length > currentParsed.normalized.length
        }
        return candidate.confidence > (current?.confidence ?: 0f)
    }

    suspend fun process(
        frame: Bitmap,
        minDetector: Float = .35f,
        minOcr: Float = .55f,
        duplicateCooldownMs: Long = 20_000,
        region: BoundingBox? = null
    ): PipelineResult = withContext(Dispatchers.Default) {
        val begin = System.nanoTime()
        val scanRegion = (region ?: BoundingBox(0f, 0f, frame.width.toFloat(), frame.height.toFloat()))
            .clamp(frame.width, frame.height)
        val regionLeft = floor(scanRegion.left).toInt()
        val regionTop = floor(scanRegion.top).toInt()
        val regionRight = ceil(scanRegion.right).toInt()
        val regionBottom = ceil(scanRegion.bottom).toInt()

        if (regionRight <= regionLeft || regionBottom <= regionTop) {
            return@withContext PipelineResult(
                status = "Invalid detection region",
                sourceWidth = frame.width,
                sourceHeight = frame.height
            )
        }

        val detectorFrame = Bitmap.createBitmap(
            frame,
            regionLeft,
            regionTop,
            regionRight - regionLeft,
            regionBottom - regionTop
        )
        val localDetections = try {
            detector.detect(detectorFrame)
        } catch (t: Throwable) {
            return@withContext PipelineResult(
                status = "Detector error: ${t.javaClass.simpleName}",
                sourceWidth = frame.width,
                sourceHeight = frame.height
            )
        } finally {
            if (detectorFrame !== frame) detectorFrame.recycle()
        }

        val detections = localDetections
            .map { detection ->
                detection.copy(
                    boundingBox = BoundingBox(
                        detection.boundingBox.left + regionLeft,
                        detection.boundingBox.top + regionTop,
                        detection.boundingBox.right + regionLeft,
                        detection.boundingBox.bottom + regionTop
                    )
                )
            }
            .filter { it.confidence >= minDetector.coerceAtMost(.5f) }
        val detectorMs = (System.nanoTime() - begin) / 1_000_000
        var ocrMs = 0L
        var incompletePlateSeen = false
        var verifyingPlate: String? = null
        var verifyingConfidence = 0f
        var verifyingObservations = 0
        val selectableCandidates = mutableListOf<PlateCandidate>()

        for (detection in detections) {
            val crop = runCatching { PlateCropper.crop(frame, detection.boundingBox) }
                .onFailure { AnprLog.error("crop_error", it) }
                .getOrNull() ?: continue
            val quality = ImageQualityAnalyzer.analyze(crop)
            if (!quality.usable) {
                AnprLog.debug(
                    "quality_rejected",
                    "reason" to quality.reason,
                    "sharpness" to quality.sharpness,
                    "brightness" to quality.brightness,
                    "size" to "${crop.width}x${crop.height}"
                )
                crop.recycle()
                continue
            }

            val enhanced = ImageEnhancer.enhance(crop)
            crop.recycle()
            val ocrStart = System.nanoTime()
            var recognition = runCatching { recognizer.recognize(enhanced) }
                .onFailure { AnprLog.error("ocr_error", it) }
                .getOrNull()
            var parsed = recognition?.let { IndianPlateParser.parse(it.normalizedText) }

            if (
                parsed == null ||
                parsed.validationConfidence < .70f ||
                !IndianPlateParser.isCompleteScanResult(parsed)
            ) {
                val variants = listOf(
                    "otsu" to ImageEnhancer.threshold(enhanced),
                    "rotate-10" to ImageEnhancer.rotate(enhanced, -10f),
                    "rotate10" to ImageEnhancer.rotate(enhanced, 10f)
                )
                val alternatives = coroutineScope {
                    variants.map { (type, bitmap) ->
                        async {
                            val result = runCatching { recognizer.recognize(bitmap) }
                                .onFailure { AnprLog.error("ocr_variant_error_$type", it) }
                                .getOrNull()
                            Triple(type, result, result?.let { IndianPlateParser.parse(it.normalizedText) })
                        }
                    }.awaitAll()
                }
                variants.forEach { it.second.recycle() }
                for ((type, alternate, alternateParsed) in alternatives) {
                    if (
                        alternate != null &&
                        alternateParsed != null &&
                        preferRecognition(alternate, alternateParsed, recognition, parsed)
                    ) {
                        recognition = alternate
                        parsed = alternateParsed
                        AnprLog.debug("ocr_variant", "type" to type)
                    }
                }
            }

            ocrMs += (System.nanoTime() - ocrStart) / 1_000_000
            enhanced.recycle()
            AnprLog.debug(
                "ocr_result",
                "text" to recognition?.normalizedText,
                "confidence" to recognition?.confidence
            )

            if (recognition == null || parsed == null) continue
            if (parsed.validationConfidence < .70f) {
                AnprLog.debug(
                    "plate_rejected",
                    "text" to parsed.normalized,
                    "validity" to parsed.validationConfidence
                )
                continue
            }
            if (!IndianPlateParser.isCompleteScanResult(parsed)) {
                incompletePlateSeen = true
                AnprLog.debug(
                    "plate_incomplete",
                    "text" to parsed.normalized,
                    "segments" to parsed.segments.joinToString("-")
                )
                continue
            }

            val effectiveOcr = maxOf(recognition.confidence, .75f)
            val now = System.currentTimeMillis()
            val track = tracker.match(detection.boundingBox, now)
            val vote = track.voting.add(
                VoteObservation(
                    parsed.normalized,
                    effectiveOcr,
                    detection.confidence,
                    (quality.sharpness / 30f).coerceIn(0f, 1f),
                    parsed.validationConfidence,
                    now
                )
            )
            if (vote.finalized) {
                selectableCandidates += PlateCandidate(
                    vote.value,
                    recognition.rawText,
                    vote.confidence,
                    detection.confidence,
                    track.firstSeen,
                    now
                )
            } else if (verifyingPlate == null || vote.confidence > verifyingConfidence) {
                verifyingPlate = vote.value
                verifyingConfidence = vote.confidence
                verifyingObservations = vote.observations
            }
        }

        val uniqueCandidates = selectableCandidates.distinctBy { it.plateNumber }
        if (uniqueCandidates.isNotEmpty()) {
            return@withContext PipelineResult(
                detections = detections,
                candidate = uniqueCandidates.first().plateNumber,
                confidence = uniqueCandidates.maxOf { it.recognitionConfidence },
                status = if (uniqueCandidates.size == 1) "Confirm detected plate" else "Select a detected plate",
                metrics = PipelineMetrics(detectorMs, ocrMs, (System.nanoTime() - begin) / 1_000_000),
                sourceWidth = frame.width,
                sourceHeight = frame.height,
                selectableCandidates = uniqueCandidates
            )
        }

        if (verifyingPlate != null) {
            return@withContext PipelineResult(
                detections = detections,
                candidate = verifyingPlate,
                confidence = verifyingConfidence,
                status = "Verifying $verifyingObservations/2",
                metrics = PipelineMetrics(detectorMs, ocrMs, (System.nanoTime() - begin) / 1_000_000),
                sourceWidth = frame.width,
                sourceHeight = frame.height
            )
        }

        PipelineResult(
            status = when {
                incompletePlateSeen -> "Fit the full plate inside the box"
                detections.isEmpty() -> "Scanning"
                else -> "Reading plate…"
            },
            metrics = PipelineMetrics(detectorMs, ocrMs, (System.nanoTime() - begin) / 1_000_000),
            sourceWidth = frame.width,
            sourceHeight = frame.height
        )
    }
}
