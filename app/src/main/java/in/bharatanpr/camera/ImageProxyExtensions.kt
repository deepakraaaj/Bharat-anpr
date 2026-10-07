package com.bharatanpr.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

fun ImageProxy.toArgbBitmap():Bitmap {
    val raw=toBitmap()
    val rotation=imageInfo.rotationDegrees
    if(rotation==0)return raw
    val matrix=Matrix().apply{postRotate(rotation.toFloat())}
    return Bitmap.createBitmap(raw,0,0,raw.width,raw.height,matrix,true).also{raw.recycle()}
}
