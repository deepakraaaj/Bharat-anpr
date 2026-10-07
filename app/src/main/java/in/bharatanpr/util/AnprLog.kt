package com.bharatanpr.util

import android.util.Log
import com.bharatanpr.BuildConfig

object AnprLog {
    fun debug(event:String,vararg fields:Pair<String,Any?>){if(BuildConfig.DEBUG)Log.d("BharatANPR",build(event,fields))}
    fun error(event:String,error:Throwable){Log.e("BharatANPR",event,error)}
    private fun build(event:String,fields:Array<out Pair<String,Any?>>)=buildString{append(event);fields.forEach{(k,v)->append(' ').append(k).append('=').append(v)}}
}
