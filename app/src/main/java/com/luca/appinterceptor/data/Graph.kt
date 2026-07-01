package com.luca.appinterceptor.data

import android.content.Context
import androidx.room.Room

/**
 * Minimaler Service-Locator. Wird in [com.luca.appinterceptor.App.onCreate]
 * initialisiert, bevor Activity oder AccessibilityService laufen.
 */
object Graph {
    lateinit var settings: SettingsRepository
        private set
    lateinit var db: AppDatabase
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        settings = SettingsRepository(app)
        db = Room.databaseBuilder(app, AppDatabase::class.java, "interceptor.db").build()
    }
}
