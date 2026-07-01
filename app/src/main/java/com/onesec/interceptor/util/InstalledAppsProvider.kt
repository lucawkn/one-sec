package com.onesec.interceptor.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.onesec.interceptor.model.InstalledAppInfo

/**
 * Lists launchable apps via an explicit MAIN/LAUNCHER intent query. This works
 * together with the <queries> declaration in the manifest and avoids needing
 * the broad QUERY_ALL_PACKAGES permission on Android 11+.
 */
object InstalledAppsProvider {

    fun getLaunchableApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val ownPackageName = context.packageName
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = queryIntentActivitiesCompat(pm, launcherIntent)

        return resolveInfos
            .asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != ownPackageName }
            .mapNotNull { packageName ->
                runCatching {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    InstalledAppInfo(
                        packageName = packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo)
                    )
                }.getOrNull()
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    @Suppress("DEPRECATION")
    private fun queryIntentActivitiesCompat(
        pm: PackageManager,
        intent: Intent
    ): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
    }
}
