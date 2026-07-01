package com.onesec.interceptor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onesec.interceptor.data.settings.InterceptorSettings
import com.onesec.interceptor.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val dataStore = ServiceLocator.settingsDataStore

    val settings: StateFlow<InterceptorSettings> = dataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InterceptorSettings())

    fun setInterceptorEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setInterceptorEnabled(enabled) }
    }

    fun setGraceMinutes(minutes: Int) {
        viewModelScope.launch { dataStore.setGraceMinutes(minutes) }
    }

    fun setBreathingSeconds(seconds: Int) {
        viewModelScope.launch { dataStore.setBreathingSeconds(seconds) }
    }
}
