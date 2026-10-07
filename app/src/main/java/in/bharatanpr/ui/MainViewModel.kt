package com.bharatanpr.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.bharatanpr.camera.FrameThrottle
import com.bharatanpr.data.local.PlateEntity
import com.bharatanpr.data.repository.PlateRepository
import com.bharatanpr.pipeline.AnprPipeline
import com.bharatanpr.pipeline.PipelineResult
import com.bharatanpr.settings.AppSettings
import com.bharatanpr.settings.SettingsRepository
import com.bharatanpr.plate.IndianPlateValidator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class ScannerState(val result:PipelineResult=PipelineResult(),val error:String?=null)
@HiltViewModel class MainViewModel @Inject constructor(private val pipeline:AnprPipeline,val repository:PlateRepository,private val settingsRepository:SettingsRepository):ViewModel(){
    val settings=settingsRepository.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),AppSettings());val history=repository.history.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList());private val _scanner=MutableStateFlow(ScannerState());val scanner=_scanner.asStateFlow();private val busy=AtomicBoolean();val throttle=FrameThrottle()
    fun submit(bitmap:Bitmap){val s=settings.value;throttle.update(s.inferenceFps.coerceAtMost(2));if(!busy.compareAndSet(false,true)){bitmap.recycle();return};viewModelScope.launch{try{val next=pipeline.process(bitmap,s.detectorConfidence,s.ocrConfidence,s.duplicateCooldownSeconds*1_000L);val previous=_scanner.value.result;val stable=next.candidate?.takeIf(IndianPlateValidator::isValid);_scanner.value=ScannerState(if(stable!=null||previous.candidate==null)next else next.copy(candidate=previous.candidate,finalized=previous.finalized,confidence=previous.confidence,status="Hold steady…"))}catch(t:Throwable){_scanner.value=ScannerState(error=t.message?:t.javaClass.simpleName)}finally{bitmap.recycle();busy.set(false)}}}
    fun update(s:AppSettings)=viewModelScope.launch{settingsRepository.update(s)};fun clearHistory()=viewModelScope.launch{repository.clear()}
}
