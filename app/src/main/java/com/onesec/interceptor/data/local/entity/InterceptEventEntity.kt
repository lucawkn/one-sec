package com.onesec.interceptor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InterceptOutcome {
    ALLOWED,
    CANCELLED
}

/**
 * One row per time the interceptor overlay was shown and resolved,
 * used to build the statistics screen.
 */
@Entity(tableName = "intercept_events")
data class InterceptEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestampMillis: Long,
    val outcome: InterceptOutcome
)
