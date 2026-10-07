package com.bharatanpr.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import com.bharatanpr.data.local.AppDatabase
import com.bharatanpr.data.local.PlateDao
import com.bharatanpr.detection.HeuristicPlateDetector
import com.bharatanpr.detection.PlateDetector
import com.bharatanpr.recognition.PlateRecognizer
import com.bharatanpr.recognition.MlKitPlateRecognizer
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class) abstract class Bindings {
    @Binds @Singleton abstract fun detector(impl:HeuristicPlateDetector):PlateDetector
    @Binds @Singleton abstract fun recognizer(impl:MlKitPlateRecognizer):PlateRecognizer
}
@Module @InstallIn(SingletonComponent::class) object AppModule {
    @Provides @Singleton fun database(@ApplicationContext c:Context)=Room.databaseBuilder(c,AppDatabase::class.java,"bharat-anpr.db").fallbackToDestructiveMigration(false).build()
    @Provides fun dao(db:AppDatabase):PlateDao=db.plateDao()
}
