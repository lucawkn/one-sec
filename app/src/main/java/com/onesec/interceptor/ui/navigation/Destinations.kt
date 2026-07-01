package com.onesec.interceptor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    STATUS("status", "Status", Icons.Filled.CheckCircle),
    APPS("apps", "Apps", Icons.Filled.List),
    STATS("stats", "Statistik", Icons.Filled.DateRange),
    SETTINGS("settings", "Einstellungen", Icons.Filled.Settings)
}
