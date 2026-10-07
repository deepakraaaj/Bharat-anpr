package com.bharatanpr.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

class CameraController(private val context:Context,private val lifecycleOwner:LifecycleOwner,private val previewView:PreviewView,private val executor:Executor,private val analyzer:ImageAnalysis.Analyzer){
    private var provider:ProcessCameraProvider?=null
    fun start(onError:(Throwable)->Unit={}){val future=ProcessCameraProvider.getInstance(context);future.addListener({runCatching{future.get().also{provider=it}.apply{unbindAll();val preview=Preview.Builder().build().also{it.surfaceProvider=previewView.surfaceProvider};val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build().also{it.setAnalyzer(executor,analyzer)};bindToLifecycle(lifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)}}.onFailure(onError)},ContextCompat.getMainExecutor(context))}
    fun stop(){provider?.unbindAll();provider=null}
}
