package com.bharatanpr.data.repository

import com.bharatanpr.data.local.PlateDao
import com.bharatanpr.data.local.PlateEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class PlateRepository @Inject constructor(private val dao:PlateDao){val history=dao.observeAll();fun detail(id:Long)=dao.observe(id);suspend fun save(e:PlateEntity)=dao.insert(e);suspend fun clear()=dao.clear()}
