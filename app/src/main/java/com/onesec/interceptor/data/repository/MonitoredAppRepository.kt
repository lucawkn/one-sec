package com.onesec.interceptor.data.repository

import com.onesec.interceptor.data.local.dao.MonitoredAppDao
import com.onesec.interceptor.data.local.dao.SnoozedAppDao
import com.onesec.interceptor.data.local.entity.MonitoredAppEntity
import com.onesec.interceptor.data.local.entity.SnoozedAppEntity
import kotlinx.coroutines.flow.Flow

class MonitoredAppRepository(
    private val monitoredAppDao: MonitoredAppDao,
    private val snoozedAppDao: SnoozedAppDao
) {
    fun observeAll(): Flow<List<MonitoredAppEntity>> = monitoredAppDao.observeAll()

    fun observeMonitoredPackageNames(): Flow<List<String>> =
        monitoredAppDao.observeMonitoredPackageNames()

    suspend fun setMonitored(packageName: String, appName: String, monitored: Boolean) {
        val existing = monitoredAppDao.getByPackageName(packageName)
        monitoredAppDao.upsert(
            MonitoredAppEntity(
                packageName = packageName,
                appName = appName,
                isMonitored = monitored,
                addedAtMillis = existing?.addedAtMillis ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun isMonitored(packageName: String): Boolean =
        monitoredAppDao.isMonitored(packageName) ?: false

    /** Returns true if [packageName] is currently within its "already allowed" grace period. */
    suspend fun isSnoozed(packageName: String, nowMillis: Long): Boolean {
        val until = snoozedAppDao.getSnoozedUntil(packageName) ?: return false
        return nowMillis < until
    }

    suspend fun snooze(packageName: String, untilMillis: Long) {
        snoozedAppDao.upsert(SnoozedAppEntity(packageName, untilMillis))
    }
}
