package com.bharatanpr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.bharatanpr.camera.CameraController
import com.bharatanpr.camera.toArgbBitmap
import com.bharatanpr.data.local.PlateEntity
import com.bharatanpr.settings.AppSettings
import com.bharatanpr.ui.MainViewModel
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

@AndroidEntryPoint class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFFFB300))){App()}}}}
private enum class Screen{SCANNER,HISTORY,SETTINGS,ABOUT}
@Composable private fun App(vm:MainViewModel=hiltViewModel()){var screen by remember{mutableStateOf(Screen.SCANNER)};Scaffold(bottomBar={NavigationBar{listOf(Screen.SCANNER to Icons.Default.CameraAlt,Screen.HISTORY to Icons.Default.History,Screen.SETTINGS to Icons.Default.Settings,Screen.ABOUT to Icons.Default.Info).forEach{(s,i)->NavigationBarItem(selected=s==screen,onClick={screen=s},icon={Icon(i,null)},label={Text(s.name.lowercase().replaceFirstChar{it.uppercase()})})}}}){padding->Box(Modifier.padding(padding)){when(screen){Screen.SCANNER->Scanner(vm);Screen.HISTORY->History(vm);Screen.SETTINGS->Settings(vm);Screen.ABOUT->About()}}}}
@Composable private fun Scanner(vm:MainViewModel){val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current;val state by vm.scanner.collectAsState();val settings by vm.settings.collectAsState();var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it}
    if(!granted){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Camera access is required to scan plates.");Spacer(Modifier.height(16.dp));Button(onClick={launcher.launch(Manifest.permission.CAMERA)}){Text("Grant camera access")}}};return}
    Box(Modifier.fillMaxSize()){AndroidView(factory={ctx->PreviewView(ctx).apply{scaleType=PreviewView.ScaleType.FILL_CENTER}},modifier=Modifier.fillMaxSize(),update={view->if(view.tag==null){val executor=Executors.newSingleThreadExecutor();val analyzer=ImageAnalysis.Analyzer{proxy->try{if(vm.throttle.shouldProcess(proxy.imageInfo.timestamp))vm.submit(proxy.toArgbBitmap())}finally{proxy.close()}};CameraController(context,lifecycle,view,executor,analyzer).also{view.tag=it;it.start()}}});Canvas(Modifier.fillMaxSize()){state.result.detections.forEach{d->val b=d.boundingBox;val sx=size.width/state.result.sourceWidth;val sy=size.height/state.result.sourceHeight;drawRect(if(state.result.finalized!=null)Color(0xFF72E096)else Color(0xFFFFB300),topLeft=Offset(b.left*sx,b.top*sy),size=androidx.compose.ui.geometry.Size(b.width*sx,b.height*sy),style=Stroke(3.dp.toPx()))}};Surface(Modifier.align(Alignment.BottomCenter).padding(20.dp),shape=MaterialTheme.shapes.large,color=Color.Black.copy(alpha=.76f)){Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(state.result.finalized?:state.result.candidate?:"Point camera at a number plate",style=MaterialTheme.typography.headlineSmall,color=if(state.result.finalized!=null)Color(0xFF72E096)else Color.White);Text(state.error?:state.result.status);if(state.result.confidence>0)Text("${(state.result.confidence*100).toInt()}% confidence");if(settings.debugOverlay)Text("Detector ${state.result.metrics.detectorMs} ms • OCR ${state.result.metrics.ocrMs} ms • Total ${state.result.metrics.totalMs} ms",style=MaterialTheme.typography.labelSmall)}}}}
@Composable private fun History(vm:MainViewModel){val rows by vm.history.collectAsState();var selected by remember{mutableStateOf<PlateEntity?>(null)};Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Detection history",style=MaterialTheme.typography.headlineSmall);if(rows.isNotEmpty())TextButton(onClick=vm::clearHistory){Text("Clear")}};if(rows.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("No detections yet")}else LazyColumn{items(rows,key={it.id}){e->ListItem(headlineContent={Text(e.plateNumber)},supportingContent={Text(DateFormat.getDateTimeInstance().format(Date(e.timestamp)))},trailingContent={Text("${(e.recognitionConfidence*100).toInt()}%")},modifier=Modifier.fillMaxWidth().clickable{selected=e}.padding(horizontal=8.dp));HorizontalDivider()}}};selected?.let{e->AlertDialog(onDismissRequest={selected=null},confirmButton={TextButton(onClick={selected=null}){Text("Close")}},title={Text(e.plateNumber)},text={Text("OCR: ${e.rawOcrText}\nRecognition: ${(e.recognitionConfidence*100).toInt()}%\nDetector: ${(e.detectorConfidence*100).toInt()}%\n${DateFormat.getDateTimeInstance().format(Date(e.timestamp))}")})}}
@Composable private fun Settings(vm:MainViewModel){val s by vm.settings.collectAsState();LazyColumn(Modifier.fillMaxSize().padding(16.dp)){item{Text("Settings",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));SliderSetting("Detector confidence",s.detectorConfidence,.1f..0.9f){vm.update(s.copy(detectorConfidence=it))};SliderSetting("OCR confidence",s.ocrConfidence,.3f..0.9f){vm.update(s.copy(ocrConfidence=it))};SliderSetting("Inference rate",s.inferenceFps.toFloat(),2f..10f,"${s.inferenceFps} FPS"){vm.update(s.copy(inferenceFps=it.toInt()))};SliderSetting("Duplicate cooldown",s.duplicateCooldownSeconds.toFloat(),5f..60f,"${s.duplicateCooldownSeconds}s"){vm.update(s.copy(duplicateCooldownSeconds=it.toInt()))};SwitchSetting("Save plate crops",s.savePlateImages){vm.update(s.copy(savePlateImages=it))};SwitchSetting("Debug metrics",s.debugOverlay){vm.update(s.copy(debugOverlay=it))};SwitchSetting("Recognition sound",s.sound){vm.update(s.copy(sound=it))};SwitchSetting("Vibration",s.vibration){vm.update(s.copy(vibration=it))}}}}
@Composable private fun SliderSetting(label:String,value:Float,range:ClosedFloatingPointRange<Float>,display:String="${(value*100).toInt()}%",change:(Float)->Unit){Text("$label · $display");Slider(value=value,onValueChange=change,valueRange=range)}
@Composable private fun SwitchSetting(label:String,value:Boolean,change:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label);Switch(value,change)}}
@Composable private fun About(){LazyColumn(Modifier.fillMaxSize().padding(20.dp)){item{Text("Bharat ANPR",style=MaterialTheme.typography.headlineMedium);Text("Offline Indian number plate recognition");Spacer(Modifier.height(20.dp));Text("Privacy",style=MaterialTheme.typography.titleLarge);Text("Camera frames stay on this device. No analytics, accounts, uploads, or network permission are used. Plate crops are disabled by default.");Spacer(Modifier.height(20.dp));Text("Open source",style=MaterialTheme.typography.titleLarge);Text("CameraX, Compose, Room, Hilt, Kotlin Coroutines and Tesseract4Android. Notices are documented in THIRD_PARTY_LICENSES.md.");Spacer(Modifier.height(20.dp));Text("Detector",style=MaterialTheme.typography.titleLarge);Text("This build uses a lightweight edge-band detector with no external model weights. For fleet deployments, plug a validated model into PlateDetector.")}}}
