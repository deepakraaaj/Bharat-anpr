package com.bharatanpr.tracking

import com.bharatanpr.detection.BoundingBox
import java.util.UUID

data class VoteObservation(val value:String,val ocrConfidence:Float,val detectorConfidence:Float,val quality:Float,val validity:Float,val timestamp:Long=System.currentTimeMillis()) { val weight get()=(ocrConfidence*.40f+detectorConfidence*.20f+quality*.15f+validity*.25f).coerceIn(0f,1f) }
data class VotingResult(val value:String,val confidence:Float,val observations:Int,val finalized:Boolean)
class TemporalVoting(private val minimumObservations:Int=2,private val threshold:Float=.75f) {
    private val votes=mutableListOf<VoteObservation>()
    fun add(o:VoteObservation):VotingResult { votes+=o; if(votes.size>12)votes.removeAt(0); val groups=votes.groupBy{it.value}; val best=groups.maxByOrNull{(_,v)->v.sumOf{it.weight.toDouble()}}!!; val support=best.value.size; val confidence=(best.value.map{it.weight}.average().toFloat()*(.7f+.3f*(support.toFloat()/minimumObservations).coerceAtMost(1f))).coerceIn(0f,1f); return VotingResult(best.key,confidence,support,support>=minimumObservations&&confidence>=threshold) }
}
data class TrackedPlate(val id:String=UUID.randomUUID().toString(),var box:BoundingBox,val firstSeen:Long,var lastSeen:Long,val voting:TemporalVoting=TemporalVoting())
class PlateTracker(private val iouThreshold:Float=.10f,private val maxAgeMs:Long=5_000) { private val tracks=mutableListOf<TrackedPlate>(); fun match(box:BoundingBox,now:Long):TrackedPlate { tracks.removeAll{now-it.lastSeen>maxAgeMs}; val found=tracks.maxByOrNull{it.box.iou(box)}?.takeIf{it.box.iou(box)>=iouThreshold}; return (found?:TrackedPlate(box=box,firstSeen=now,lastSeen=now).also(tracks::add)).also{it.box=box;it.lastSeen=now} } }
class DuplicateSuppressor(private val defaultCooldownMs:Long=20_000) { private val seen=mutableMapOf<String,Long>(); @Synchronized fun shouldAccept(value:String,now:Long=System.currentTimeMillis(),cooldownMs:Long=defaultCooldownMs):Boolean { seen.entries.removeAll{now-it.value>cooldownMs*2}; val last=seen[value]; if(last!=null&&now-last<cooldownMs)return false;seen[value]=now;return true } }
