package com.onesec.interceptor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.onesec.interceptor.data.local.entity.InterceptEventEntity
import kotlinx.coroutines.flow.Flow

data class AppStatSummary(
    val packageName: String,
    val appName: String,
    val allowedCount: Int,
    val cancelledCount: Int
)

@Dao
interface InterceptEventDao {

    @Insert
    suspend fun insert(event: InterceptEventEntity)

    @Query(
        """
        SELECT packageName,
               appName,
               SUM(CASE WHEN outcome = 'ALLOWED' THEN 1 ELSE 0 END) AS allowedCount,
               SUM(CASE WHEN outcome = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledCount
        FROM intercept_events
        WHERE timestampMillis >= :sinceMillis
        GROUP BY packageName
        ORDER BY (allowedCount + cancelledCount) DESC
        """
    )
    fun observeStatsSince(sinceMillis: Long): Flow<List<AppStatSummary>>
}
