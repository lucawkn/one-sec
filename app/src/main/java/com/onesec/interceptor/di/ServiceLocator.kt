package com.onesec.interceptor.di

import android.content.Context
import com.onesec.interceptor.data.local.AppDatabase
import com.onesec.interceptor.data.repository.MonitoredAppRepository
import com.onesec.interceptor.data.repository.StatsRepository
import com.onesec.interceptor.data.settings.SettingsDataStore

/**
 * Minimal hand-rolled service locator. The app is small enough that a full
 * DI framework (Hilt/Koin) would add build complexity without real benefit.
 */
object ServiceLocator {

    @Volatile
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(appContext) }

    val monitoredAppRepository: MonitoredAppRepository by lazy {
        MonitoredAppRepository(database.monitoredAppDao(), database.snoozedAppDao())
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepository(database.interceptEventDao())
    }
}
