package com.onesec.interceptor.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Decides whether a package name reported by the AccessibilityService is
 * something we should ever consider intercepting. This keeps the service
 * from reacting to itself, the launcher, the system UI, keyboards, etc.
 */
class ForegroundAppFilter(context: Context) {

    private val ownPackageName = context.packageName
    private val launcherPackageName: String? = resolveDefaultLauncherPackage(context)

    private val staticIgnoreList = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin"
    )

    fun shouldIgnore(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == ownPackageName) return true
        if (packageName == launcherPackageName) return true
        if (packageName in staticIgnoreList) return true
        return false
    }

    private fun resolveDefaultLauncherPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName
    }
}
