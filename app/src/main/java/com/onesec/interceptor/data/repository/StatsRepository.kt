package com.onesec.interceptor.data.repository

import com.onesec.interceptor.data.local.dao.AppStatSummary
import com.onesec.interceptor.data.local.dao.InterceptEventDao
import com.onesec.interceptor.data.local.entity.InterceptEventEntity
import com.onesec.interceptor.data.local.entity.InterceptOutcome
import kotlinx.coroutines.flow.Flow

enum class StatsRange {
    TODAY,
    WEEK,
    ALL_TIME
}

class StatsRepository(private val interceptEventDao: InterceptEventDao) {

    suspend fun recordEvent(packageName: String, appName: String, outcome: InterceptOutcome) {
        interceptEventDao.insert(
            InterceptEventEntity(
                packageName = packageName,
                appName = appName,
                timestampMillis = System.currentTimeMillis(),
                outcome = outcome
            )
        )
    }

    fun observeStats(range: StatsRange): Flow<List<AppStatSummary>> {
        val since = when (range) {
            StatsRange.TODAY -> startOfTodayMillis()
            StatsRange.WEEK -> startOfTodayMillis() - 6 * DAY_MILLIS
            StatsRange.ALL_TIME -> 0L
        }
        return interceptEventDao.observeStatsSince(since)
    }

    private fun startOfTodayMillis(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
