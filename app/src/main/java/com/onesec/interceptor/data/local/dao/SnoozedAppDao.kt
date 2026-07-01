package com.onesec.interceptor.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onesec.interceptor.data.local.entity.SnoozedAppEntity

@Dao
interface SnoozedAppDao {

    @Query("SELECT snoozedUntilMillis FROM snoozed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getSnoozedUntil(packageName: String): Long?

    @Upsert
    suspend fun upsert(entity: SnoozedAppEntity)

    @Query("DELETE FROM snoozed_apps WHERE snoozedUntilMillis < :nowMillis")
    suspend fun deleteExpired(nowMillis: Long)
}
