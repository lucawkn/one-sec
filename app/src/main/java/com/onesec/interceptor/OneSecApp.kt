package com.onesec.interceptor

import android.app.Application
import com.onesec.interceptor.di.ServiceLocator

class OneSecApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
