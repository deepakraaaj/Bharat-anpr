package com.bharatanpr.detection

import android.graphics.Bitmap
import javax.inject.Inject
import kotlin.math.abs
import com.bharatanpr.util.AnprLog

/** Model-free fallback. Finds dense vertical-edge bands; replace through [PlateDetector] for deployment-specific ML. */
class HeuristicPlateDetector @Inject constructor() : PlateDetector {
    override suspend fun detect(image: Bitmap): List<Detection> {
        val scale = (image.width / 320f).coerceAtLeast(1f)
        val w = (image.width / scale).toInt(); val h = (image.height / scale).toInt()
        val small = if (scale > 1f) Bitmap.createScaledBitmap(image,w,h,true) else image
        val pixels = IntArray(w*h); small.getPixels(pixels,0,w,0,0,w,h)
        var bestScore=0f; var best: BoundingBox?=null;val candidates=mutableListOf<Pair<BoundingBox,Float>>()
        var minLuma=255; var maxLuma=0
        val winW=(w*.36f).toInt().coerceAtLeast(60); val winH=(winW/4.2f).toInt().coerceAtLeast(18)
        val sx=(winW/4).coerceAtLeast(8); val sy=(winH/2).coerceAtLeast(6)
        for (y in winH until h-winH step sy) for (x in 1 until w-winW step sx) {
            var edges=0; var bright=0L; var samples=0
            for (yy in y until (y+winH).coerceAtMost(h-1) step 2) for (xx in x+1 until (x+winW).coerceAtMost(w-1) step 2) {
                fun lum(c:Int)=((c shr 16 and 255)*77+(c shr 8 and 255)*150+(c and 255)*29) shr 8
                val l=lum(pixels[yy*w+xx]); minLuma=minOf(minLuma,l);maxLuma=maxOf(maxLuma,l);if(abs(l-lum(pixels[yy*w+xx-1]))>22) edges++; bright+=l; samples++
            }
            val edgeDensity=edges.toFloat()/samples; val mean=bright.toFloat()/samples
            val score=edgeDensity * if(mean in 55f..235f) 1f else .55f
            val candidate=BoundingBox(x*scale,y*scale,(x+winW)*scale,(y+winH)*scale)
            candidates+=candidate to score
            if(score>bestScore){bestScore=score; best=candidate}
        }
        if(small!==image) small.recycle()
        // Classical edge density is intentionally calibrated below neural-model scores.
        // A candidate still passes crop quality, format validation and temporal voting.
        val confidence=((bestScore-.02f)*5.0f).coerceIn(.55f,.88f)
        AnprLog.debug("detector_result","size" to "${image.width}x${image.height}","luma" to "$minLuma..$maxLuma","edgeDensity" to bestScore,"confidence" to confidence)
        val selected=mutableListOf<Pair<BoundingBox,Float>>()
        for((box,score) in candidates.sortedByDescending{it.second}) {
            if(selected.none{it.first.iou(box)>.25f})selected+=box to score
            if(selected.size==3)break
        }
        return selected.map { (box,score) ->
            val padX=box.width*.80f;val padY=box.height*.65f
            val expanded=BoundingBox(box.left-padX,box.top-padY,box.right+padX,box.bottom+padY).clamp(image.width,image.height)
            Detection(expanded,((score-.02f)*5f).coerceIn(.55f,.88f))
        }
    }
}
