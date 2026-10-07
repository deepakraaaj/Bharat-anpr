package com.bharatanpr.network

import com.bharatanpr.data.local.PlateEntity

/** Optional future HTTPS boundary. No implementation is bound and the app declares no network permission. */
interface PlateApi { suspend fun upload(event:PlateEntity):Result<Unit> }
interface SyncManager { suspend fun enqueue(eventId:Long):Result<Unit> }
