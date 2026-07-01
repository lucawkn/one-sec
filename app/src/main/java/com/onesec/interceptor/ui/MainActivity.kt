package com.onesec.interceptor.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onesec.interceptor.ui.navigation.OneSecNavHost
import com.onesec.interceptor.ui.theme.OneSecTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneSecTheme {
                OneSecNavHost()
            }
        }
    }
}
