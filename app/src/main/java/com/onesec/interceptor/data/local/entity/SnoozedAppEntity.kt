package com.onesec.interceptor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the "grace period" after the user allowed opening an app: while
 * now() < snoozedUntilMillis the interceptor lets the app through without
 * showing the overlay again.
 */
@Entity(tableName = "snoozed_apps")
data class SnoozedAppEntity(
    @PrimaryKey val packageName: String,
    val snoozedUntilMillis: Long
)
