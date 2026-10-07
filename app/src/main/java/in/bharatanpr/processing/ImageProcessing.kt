package com.bharatanpr.processing

import android.graphics.Bitmap
import android.graphics.Color
import com.bharatanpr.detection.BoundingBox
import kotlin.math.max

data class PlateQuality(val sharpness:Float,val brightness:Float,val plateWidth:Int,val plateHeight:Int,val usable:Boolean,val reason:String?=null)
object PlateCropper { fun crop(source:Bitmap,box:BoundingBox):Bitmap { val b=box.clamp(source.width,source.height); return Bitmap.createBitmap(source,b.left.toInt(),b.top.toInt(),max(1,b.width.toInt()),max(1,b.height.toInt())) } }
object ImageQualityAnalyzer {
    fun analyze(image:Bitmap):PlateQuality { if(image.width<70||image.height<18)return PlateQuality(0f,0f,image.width,image.height,false,"crop too small")
        val p=IntArray(image.width*image.height); image.getPixels(p,0,image.width,0,0,image.width,image.height); var sum=0L;var gradient=0L;var n=0
        fun lum(c:Int)=(Color.red(c)*77+Color.green(c)*150+Color.blue(c)*29) shr 8
        for(y in 1 until image.height step 2) for(x in 1 until image.width step 2){val l=lum(p[y*image.width+x]);sum+=l;gradient+=kotlin.math.abs(l-lum(p[y*image.width+x-1]));n++}
        val bright=sum.toFloat()/n;val sharp=gradient.toFloat()/n;val ratio=image.width.toFloat()/image.height; val ok=bright in 20f..250f&&sharp>=3f&&ratio in 1.5f..8f
        return PlateQuality(sharp,bright,image.width,image.height,ok,when{bright<20->"underexposed";bright>250->"overexposed";sharp<3->"blur";ratio !in 1.5f..8f->"aspect ratio";else->null}) }
}
object ImageEnhancer {
    fun enhance(src:Bitmap):Bitmap { val targetW=640; val scale=targetW.toFloat()/src.width; return Bitmap.createScaledBitmap(src,targetW,(src.height*scale).toInt().coerceAtLeast(1),true) }

    /** Otsu binarization improves black characters on yellow/white reflective plates. */
    fun threshold(src:Bitmap):Bitmap {
        val width=src.width;val height=src.height;val pixels=IntArray(width*height);src.getPixels(pixels,0,width,0,0,width,height)
        val histogram=IntArray(256)
        fun luma(c:Int)=((Color.red(c)*77+Color.green(c)*150+Color.blue(c)*29) shr 8).coerceIn(0,255)
        pixels.forEach{histogram[luma(it)]++}
        val total=pixels.size;var sum=0L;for(i in 0..255)sum+=i.toLong()*histogram[i];var background=0;var backgroundSum=0L;var bestVariance=-1.0;var threshold=127
        for(i in 0..255){background+=histogram[i];if(background==0)continue;val foreground=total-background;if(foreground==0)break;backgroundSum+=i.toLong()*histogram[i];val meanBack=backgroundSum.toDouble()/background;val meanFore=(sum-backgroundSum).toDouble()/foreground;val variance=background.toDouble()*foreground*(meanBack-meanFore)*(meanBack-meanFore);if(variance>bestVariance){bestVariance=variance;threshold=i}}
        for(i in pixels.indices){val value=if(luma(pixels[i])>threshold)255 else 0;pixels[i]=Color.rgb(value,value,value)}
        return Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888)
    }

    fun rotate(src:Bitmap,degrees:Float):Bitmap {
        val matrix=android.graphics.Matrix().apply{postRotate(degrees)}
        return Bitmap.createBitmap(src,0,0,src.width,src.height,matrix,true)
    }
}
