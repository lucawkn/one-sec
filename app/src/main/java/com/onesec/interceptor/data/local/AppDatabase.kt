package com.onesec.interceptor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.onesec.interceptor.data.local.dao.InterceptEventDao
import com.onesec.interceptor.data.local.dao.MonitoredAppDao
import com.onesec.interceptor.data.local.dao.SnoozedAppDao
import com.onesec.interceptor.data.local.entity.InterceptEventEntity
import com.onesec.interceptor.data.local.entity.MonitoredAppEntity
import com.onesec.interceptor.data.local.entity.SnoozedAppEntity

@Database(
    entities = [
        MonitoredAppEntity::class,
        InterceptEventEntity::class,
        SnoozedAppEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun interceptEventDao(): InterceptEventDao
    abstract fun snoozedAppDao(): SnoozedAppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "onesec_interceptor.db"
                ).build().also { instance = it }
            }
        }
    }
}
