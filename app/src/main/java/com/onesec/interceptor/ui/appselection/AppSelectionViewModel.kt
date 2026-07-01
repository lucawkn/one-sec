package com.onesec.interceptor.ui.appselection

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onesec.interceptor.di.ServiceLocator
import com.onesec.interceptor.util.InstalledAppsProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppSelectionItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val isMonitored: Boolean
)

class AppSelectionViewModel : ViewModel() {

    private val repository = ServiceLocator.monitoredAppRepository
    private val installedApps = InstalledAppsProvider.getLaunchableApps(ServiceLocator.appContext)

    val items: StateFlow<List<AppSelectionItem>> = repository.observeMonitoredPackageNames()
        .map { monitored ->
            val monitoredSet = monitored.toSet()
            installedApps.map { app ->
                AppSelectionItem(
                    packageName = app.packageName,
                    appName = app.appName,
                    icon = app.icon,
                    isMonitored = app.packageName in monitoredSet
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMonitored(packageName: String, appName: String, monitored: Boolean) {
        viewModelScope.launch {
            repository.setMonitored(packageName, appName, monitored)
        }
    }
}
