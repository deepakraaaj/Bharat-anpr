package com.bharatanpr.detection

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init { require(left <= right && top <= bottom) }
    val width get() = right - left
    val height get() = bottom - top
    val area get() = width * height
    fun clamp(width: Int, height: Int) = BoundingBox(left.coerceIn(0f,width.toFloat()), top.coerceIn(0f,height.toFloat()), right.coerceIn(0f,width.toFloat()), bottom.coerceIn(0f,height.toFloat()))
    fun iou(other: BoundingBox): Float { val x1=max(left,other.left); val y1=max(top,other.top); val x2=min(right,other.right); val y2=min(bottom,other.bottom); val intersection=max(0f,x2-x1)*max(0f,y2-y1); return intersection/(area+other.area-intersection).coerceAtLeast(1f) }
}
data class Detection(val boundingBox: BoundingBox, val confidence: Float, val classId: Int = 0, val className: String = "plate")
data class DetectorConfig(val confidenceThreshold: Float = .50f, val nmsIouThreshold: Float = .45f, val minWidthPx: Int = 80)
interface PlateDetector : AutoCloseable { suspend fun detect(image: Bitmap): List<Detection>; override fun close() {} }
