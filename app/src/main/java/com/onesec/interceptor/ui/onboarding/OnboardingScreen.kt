package com.onesec.interceptor.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.onesec.interceptor.service.AppInterceptorAccessibilityService
import com.onesec.interceptor.ui.theme.SoftGreen
import com.onesec.interceptor.ui.theme.SoftRed

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var overlayGranted by remember {
        mutableStateOf(com.onesec.interceptor.util.PermissionUtils.hasOverlayPermission(context))
    }
    var accessibilityGranted by remember {
        mutableStateOf(
            com.onesec.interceptor.util.PermissionUtils.isAccessibilityServiceEnabled(
                context,
                AppInterceptorAccessibilityService::class.java
            )
        )
    }

    // Permissions are granted in system settings, outside our activity, so
    // re-check whenever the user comes back to this screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = com.onesec.interceptor.util.PermissionUtils.hasOverlayPermission(context)
                accessibilityGranted = com.onesec.interceptor.util.PermissionUtils.isAccessibilityServiceEnabled(
                    context,
                    AppInterceptorAccessibilityService::class.java
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Einrichtung",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Der App-Interceptor braucht zwei Systemberechtigungen, um überwachte Apps abzufangen.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            PermissionCard(
                title = "Bedienungshilfe aktivieren",
                description = "Wird benötigt, um zu erkennen, wenn eine überwachte App geöffnet wird.",
                granted = accessibilityGranted,
                buttonLabel = "Zu den Bedienungshilfen",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }

        item {
            PermissionCard(
                title = "Overlay-Berechtigung erteilen",
                description = "Wird benötigt, um die Atempause über anderen Apps anzuzeigen.",
                granted = overlayGranted,
                buttonLabel = "Overlay-Berechtigung öffnen",
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }

        if (accessibilityGranted && overlayGranted) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SoftGreen)
                        Spacer(Modifier.height(8.dp))
                        Text("Alles bereit! Wähle jetzt unter \"Apps\" aus, welche Apps unterbrochen werden sollen.")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (granted) SoftGreen else SoftRed
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (!granted) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onClick) {
                    Text(buttonLabel)
                }
            }
        }
    }
}
