package com.onesec.interceptor.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)
