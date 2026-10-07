package com.bharatanpr.camera

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Deterministic frame source for developer and end-to-end tests. */
class AssetFrameSource(private val context: Context, private val assetNames: List<String>) : FrameSource {
    private val output=MutableSharedFlow<Frame>(extraBufferCapacity=1)
    override val frames:Flow<Frame> = output
    override suspend fun start(){assetNames.forEach{name->context.assets.open(name).use{BitmapFactory.decodeStream(it)}?.let{output.emit(Frame(it,0))}}}
    override suspend fun stop()=Unit
}

/** Camera adapter target; CameraX analyzers submit ownership-transferred bitmaps here. */
class CameraFrameSource : FrameSource {
    private val output=MutableSharedFlow<Frame>(extraBufferCapacity=1,onBufferOverflow=kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    override val frames:Flow<Frame> = output
    fun submit(frame:Frame)=output.tryEmit(frame)
    override suspend fun start()=Unit
    override suspend fun stop()=Unit
}
