package com.bharatanpr.camera

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

data class Frame(val bitmap:Bitmap,val rotationDegrees:Int,val timestamp:Long=System.currentTimeMillis())
interface FrameSource { val frames:Flow<Frame>; suspend fun start(); suspend fun stop() }
class FrameThrottle(private var fps:Int=6){private var last=0L;fun update(value:Int){fps=value.coerceIn(1,15)};fun shouldProcess(nowNanos:Long):Boolean{val interval=1_000_000_000L/fps;if(nowNanos-last<interval)return false;last=nowNanos;return true}}
