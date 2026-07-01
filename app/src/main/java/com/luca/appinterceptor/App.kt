package com.luca.appinterceptor

import android.app.Application
import com.luca.appinterceptor.data.Graph
import com.luca.appinterceptor.util.Notifications

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifications.createChannel(this)
    }
}
