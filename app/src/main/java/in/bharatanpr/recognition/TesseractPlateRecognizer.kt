package com.bharatanpr.recognition

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import dagger.hilt.android.qualifiers.ApplicationContext
import com.bharatanpr.plate.PlateNormalizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class TesseractPlateRecognizer @Inject constructor(@ApplicationContext private val context: Context) : PlateRecognizer {
    private val mutex=Mutex(); private var api:TessBaseAPI?=null
    private fun instance():TessBaseAPI {
        api?.let{return it}; val root=File(context.filesDir,"tesseract"); val data=File(root,"tessdata/eng.traineddata")
        if(!data.exists()){data.parentFile!!.mkdirs(); context.assets.open("tessdata/eng.traineddata").use{input->data.outputStream().use(input::copyTo)}}
        return TessBaseAPI().also { check(it.init(root.absolutePath,"eng",TessBaseAPI.OEM_LSTM_ONLY)){"Tesseract initialization failed"}; it.pageSegMode=TessBaseAPI.PageSegMode.PSM_SINGLE_LINE; it.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"); api=it }
    }
    override suspend fun recognize(crop: Bitmap)=mutex.withLock { val tess=instance(); tess.setImage(crop); val raw=tess.utF8Text.orEmpty(); val confidence=(tess.meanConfidence()/100f).coerceIn(0f,1f); tess.clear(); RecognitionResult(raw,PlateNormalizer.normalize(raw),confidence) }
    override fun close(){ api?.recycle(); api=null }
}
