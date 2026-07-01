package com.onesec.interceptor.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onesec.interceptor.data.local.entity.MonitoredAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps ORDER BY appName ASC")
    fun observeAll(): Flow<List<MonitoredAppEntity>>

    @Query("SELECT packageName FROM monitored_apps WHERE isMonitored = 1")
    fun observeMonitoredPackageNames(): Flow<List<String>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): MonitoredAppEntity?

    @Query("SELECT isMonitored FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun isMonitored(packageName: String): Boolean?

    @Upsert
    suspend fun upsert(entity: MonitoredAppEntity)
}
