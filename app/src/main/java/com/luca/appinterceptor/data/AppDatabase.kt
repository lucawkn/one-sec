package com.luca.appinterceptor.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Ein Interceptions-Ereignis: opened = true → "Ja, öffnen", false → abgebrochen. */
@Entity(tableName = "events")
data class InterceptEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    val opened: Boolean,
)

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: InterceptEvent)

    @Query("SELECT * FROM events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun eventsSince(since: Long): Flow<List<InterceptEvent>>
}

@Database(entities = [InterceptEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
