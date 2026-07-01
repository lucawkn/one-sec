package com.onesec.interceptor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per app the user has picked in the "monitored apps" screen.
 * [isMonitored] lets us keep the row (and its historic stats) around even
 * if the user later unchecks the app instead of deleting it outright.
 */
@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isMonitored: Boolean,
    val addedAtMillis: Long
)
