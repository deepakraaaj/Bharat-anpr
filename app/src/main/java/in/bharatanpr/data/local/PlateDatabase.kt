package com.bharatanpr.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="plates",indices=[Index(value=["plateNumber"]),Index(value=["timestamp"])])
data class PlateEntity(@PrimaryKey(autoGenerate=true)val id:Long=0,val plateNumber:String,val rawOcrText:String,val recognitionConfidence:Float,val detectorConfidence:Float,val timestamp:Long,val firstSeen:Long,val lastSeen:Long,val imagePath:String?=null,val syncStatus:String="LOCAL",val latitude:Double?=null,val longitude:Double?=null)
@Dao interface PlateDao {
    @Query("SELECT * FROM plates ORDER BY timestamp DESC") fun observeAll():Flow<List<PlateEntity>>
    @Query("SELECT * FROM plates WHERE id=:id") fun observe(id:Long):Flow<PlateEntity?>
    @Insert suspend fun insert(entity:PlateEntity):Long
    @Query("DELETE FROM plates WHERE timestamp < :cutoff") suspend fun deleteOlderThan(cutoff:Long):Int
    @Query("DELETE FROM plates") suspend fun clear()
}
@Database(entities=[PlateEntity::class],version=1,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){abstract fun plateDao():PlateDao}
