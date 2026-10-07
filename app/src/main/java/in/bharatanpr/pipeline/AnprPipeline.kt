package com.bharatanpr.pipeline

import android.graphics.Bitmap
import com.bharatanpr.data.local.PlateEntity
import com.bharatanpr.data.repository.PlateRepository
import com.bharatanpr.detection.*
import com.bharatanpr.plate.IndianPlateParser
import com.bharatanpr.processing.*
import com.bharatanpr.recognition.PlateRecognizer
import com.bharatanpr.tracking.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.bharatanpr.util.AnprLog
import kotlin.math.ceil
import kotlin.math.floor

data class PipelineMetrics(val detectorMs:Long=0,val ocrMs:Long=0,val totalMs:Long=0)
data class PipelineResult(val detections:List<Detection> = emptyList(),val candidate:String?=null,val finalized:String?=null,val confidence:Float=0f,val status:String="Scanning",val metrics:PipelineMetrics=PipelineMetrics(),val sourceWidth:Int=1,val sourceHeight:Int=1)
@Singleton class AnprPipeline @Inject constructor(private val detector:PlateDetector,private val recognizer:PlateRecognizer,private val repository:PlateRepository){
    private val tracker=PlateTracker();private val duplicates=DuplicateSuppressor()
    suspend fun process(frame:Bitmap,minDetector:Float=.35f,minOcr:Float=.55f,duplicateCooldownMs:Long=20_000,region:BoundingBox?=null):PipelineResult=withContext(Dispatchers.Default){val begin=System.nanoTime();var mark=begin
        val scanRegion=(region?:BoundingBox(0f,0f,frame.width.toFloat(),frame.height.toFloat())).clamp(frame.width,frame.height)
        val regionLeft=floor(scanRegion.left).toInt();val regionTop=floor(scanRegion.top).toInt();val regionRight=ceil(scanRegion.right).toInt();val regionBottom=ceil(scanRegion.bottom).toInt()
        if(regionRight<=regionLeft||regionBottom<=regionTop)return@withContext PipelineResult(status="Invalid detection region",sourceWidth=frame.width,sourceHeight=frame.height)
        val detectorFrame=Bitmap.createBitmap(frame,regionLeft,regionTop,regionRight-regionLeft,regionBottom-regionTop)
        val localDetections=try{detector.detect(detectorFrame)}catch(t:Throwable){return@withContext PipelineResult(status="Detector error: ${t.javaClass.simpleName}",sourceWidth=frame.width,sourceHeight=frame.height)}finally{if(detectorFrame!==frame)detectorFrame.recycle()}
        val detections=localDetections.map{d->d.copy(boundingBox=BoundingBox(d.boundingBox.left+regionLeft,d.boundingBox.top+regionTop,d.boundingBox.right+regionLeft,d.boundingBox.bottom+regionTop))}.filter{it.confidence>=minDetector.coerceAtMost(.5f)};val detMs=(System.nanoTime()-mark)/1_000_000;var ocrMs=0L;var latestCandidate:String?=null;var latestConfidence=0f
        for(d in detections){val crop=runCatching{PlateCropper.crop(frame,d.boundingBox)}.onFailure{AnprLog.error("crop_error",it)}.getOrNull()?:continue;val quality=ImageQualityAnalyzer.analyze(crop);if(!quality.usable){AnprLog.debug("quality_rejected","reason" to quality.reason,"sharpness" to quality.sharpness,"brightness" to quality.brightness,"size" to "${crop.width}x${crop.height}");crop.recycle();continue};val enhanced=ImageEnhancer.enhance(crop);crop.recycle();mark=System.nanoTime();var rec=runCatching{recognizer.recognize(enhanced)}.onFailure{AnprLog.error("ocr_error",it)}.getOrNull();var parsed=rec?.let{IndianPlateParser.parse(it.normalizedText)};if(parsed==null||parsed.validationConfidence<.70f){val thresholded=ImageEnhancer.threshold(enhanced);val alternate=runCatching{recognizer.recognize(thresholded)}.onFailure{AnprLog.error("ocr_threshold_error",it)}.getOrNull();thresholded.recycle();val alternateParsed=alternate?.let{IndianPlateParser.parse(it.normalizedText)};if(alternateParsed!=null&&(alternateParsed.validationConfidence>(parsed?.validationConfidence?:0f) || (alternateParsed.validationConfidence==(parsed?.validationConfidence?:0f) && (alternate?.confidence?:0f)>(rec?.confidence?:0f)))){rec=alternate;parsed=alternateParsed;AnprLog.debug("ocr_variant","type" to "otsu")}};if(parsed==null||parsed.validationConfidence<.70f){for(angle in listOf(-10f,10f)){val rotated=ImageEnhancer.rotate(enhanced,angle);val rotatedRec=runCatching{recognizer.recognize(rotated)}.getOrNull();rotated.recycle();val rotatedParsed=rotatedRec?.let{IndianPlateParser.parse(it.normalizedText)};AnprLog.debug("ocr_variant","type" to "rotate$angle","text" to rotatedRec?.normalizedText);if(rotatedParsed!=null&&rotatedParsed.validationConfidence>=.70f){rec=rotatedRec;parsed=rotatedParsed;break}}};ocrMs+=(System.nanoTime()-mark)/1_000_000;enhanced.recycle();AnprLog.debug("ocr_result","text" to rec?.normalizedText,"confidence" to rec?.confidence);latestCandidate=rec?.normalizedText?.takeIf{it.isNotBlank()};latestConfidence=rec?.confidence?:0f;if(rec==null||parsed==null)continue;if(parsed.validationConfidence<.70f){AnprLog.debug("plate_rejected","text" to parsed.normalized,"validity" to parsed.validationConfidence);continue};val effectiveOcr=maxOf(rec.confidence,.75f);val now=System.currentTimeMillis();val track=tracker.match(d.boundingBox,now);val vote=track.voting.add(VoteObservation(parsed.normalized,effectiveOcr,d.confidence,(quality.sharpness/30f).coerceIn(0f,1f),parsed.validationConfidence,now));if(vote.finalized){if(duplicates.shouldAccept(vote.value,now,duplicateCooldownMs))repository.save(PlateEntity(plateNumber=vote.value,rawOcrText=rec.rawText,recognitionConfidence=vote.confidence,detectorConfidence=d.confidence,timestamp=now,firstSeen=track.firstSeen,lastSeen=now));return@withContext PipelineResult(detections,vote.value,vote.value,vote.confidence,"Recognized",PipelineMetrics(detMs,ocrMs,(System.nanoTime()-begin)/1_000_000),frame.width,frame.height)};return@withContext PipelineResult(detections,vote.value,null,vote.confidence,"Verifying ${vote.observations}/2",PipelineMetrics(detMs,ocrMs,(System.nanoTime()-begin)/1_000_000),frame.width,frame.height)}
        PipelineResult(detections,status=if(detections.isEmpty())"Scanning" else "Reading plate…",metrics=PipelineMetrics(detMs,ocrMs,(System.nanoTime()-begin)/1_000_000),sourceWidth=frame.width,sourceHeight=frame.height) }
}
