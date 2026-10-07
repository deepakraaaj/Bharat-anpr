package com.bharatanpr.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("anpr_settings")
data class AppSettings(val detectorConfidence:Float=.15f,val ocrConfidence:Float=.35f,val inferenceFps:Int=2,val duplicateCooldownSeconds:Int=20,val savePlateImages:Boolean=false,val debugOverlay:Boolean=false,val sound:Boolean=false,val vibration:Boolean=true,val retentionDays:Int=30)
@Singleton class SettingsRepository @Inject constructor(@ApplicationContext private val context:Context){
    private object K{val detector=floatPreferencesKey("detector");val ocr=floatPreferencesKey("ocr");val fps=intPreferencesKey("fps");val cooldown=intPreferencesKey("cooldown");val images=booleanPreferencesKey("images");val debug=booleanPreferencesKey("debug");val sound=booleanPreferencesKey("sound");val vibration=booleanPreferencesKey("vibration");val retention=intPreferencesKey("retention")}
    val settings=context.dataStore.data.map{p->AppSettings(p[K.detector]?:.15f,p[K.ocr]?:.35f,p[K.fps]?:2,p[K.cooldown]?:20,p[K.images]?:false,p[K.debug]?:false,p[K.sound]?:false,p[K.vibration]?:true,p[K.retention]?:30)}
    suspend fun update(s:AppSettings)=context.dataStore.edit{p->p[K.detector]=s.detectorConfidence;p[K.ocr]=s.ocrConfidence;p[K.fps]=s.inferenceFps;p[K.cooldown]=s.duplicateCooldownSeconds;p[K.images]=s.savePlateImages;p[K.debug]=s.debugOverlay;p[K.sound]=s.sound;p[K.vibration]=s.vibration;p[K.retention]=s.retentionDays}
}
