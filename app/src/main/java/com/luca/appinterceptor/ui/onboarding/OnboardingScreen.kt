package com.luca.appinterceptor.ui.onboarding

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.luca.appinterceptor.data.Graph
import com.luca.appinterceptor.ui.OnResumeEffect
import com.luca.appinterceptor.util.MiuiIntents
import com.luca.appinterceptor.util.PermissionChecks
import kotlinx.coroutines.launch

private data class StepUi(
    val title: String,
    val description: String,
    val manualHint: String,
    val done: Boolean?, // null = nicht automatisch prüfbar
    val onOpen: () -> Boolean, // false = Deep-Link nicht gefunden
)

private fun buildSteps(
    context: Context,
    isMiui: Boolean,
    requestNotifications: () -> Unit,
): List<StepUi> {
    val steps = mutableListOf<StepUi>()

    steps += StepUi(
        title = "Anzeige über anderen Apps",
        description = "Erlaubt der App, das Atem-Overlay über anderen Apps anzuzeigen (SYSTEM_ALERT_WINDOW).",
        manualHint = "Einstellungen > Apps > Apps verwalten > App Interceptor > " +
            "Andere Berechtigungen > \"Anzeige über anderen Apps\" aktivieren.",
        done = PermissionChecks.canDrawOverlays(context),
        onOpen = { MiuiIntents.openOverlaySettings(context) },
    )

    steps += StepUi(
        title = "Bedienungshilfen-Dienst aktivieren",
        description = "Der Kern der App: erkennt, wenn eine überwachte App geöffnet wird. " +
            "Es werden keine Bildschirminhalte gelesen.",
        manualHint = "Einstellungen > Weitere Einstellungen > Bedienungshilfen > " +
            "Heruntergeladene Apps > App Interceptor einschalten.\n\n" +
            "WICHTIG bei Sideloading (Android 13+): Ist der Schalter ausgegraut " +
            "(\"Eingeschränkte Einstellung\"), zuerst: Einstellungen > Apps > App Interceptor > " +
            "Drei-Punkte-Menü oben rechts > \"Eingeschränkte Einstellungen zulassen\", " +
            "dann erneut versuchen.",
        done = PermissionChecks.accessibilityEnabled(context),
        onOpen = { MiuiIntents.openAccessibilitySettings(context) },
    )

    if (isMiui) {
        steps += StepUi(
            title = "MIUI: Popup-Fenster im Hintergrund",
            description = "Separate MIUI-Berechtigung, ohne die Overlays aus dem Hintergrund " +
                "blockiert werden. Wird oft übersehen! In \"Weitere Berechtigungen\" beide " +
                "Popup-Einträge erlauben.",
            manualHint = "Einstellungen > Apps > Apps verwalten > App Interceptor > " +
                "Weitere Berechtigungen:\n" +
                "• \"Popup-Fenster anzeigen\" → Erlauben\n" +
                "• \"Popup-Fenster anzeigen, während die App im Hintergrund ausgeführt wird\" → Erlauben",
            done = PermissionChecks.miuiBackgroundPopupAllowed(context),
            onOpen = { MiuiIntents.openMiuiPermissionEditor(context) },
        )

        steps += StepUi(
            title = "MIUI: Autostart erlauben",
            description = "Damit der Dienst nach einem Geräteneustart wieder anläuft und der " +
                "Boot-Check funktioniert.",
            manualHint = "Security-App (Sicherheit) > Apps verwalten > Berechtigungen > Autostart > " +
                "App Interceptor aktivieren.\nAlternativ: Einstellungen > Apps > Apps verwalten > " +
                "App Interceptor > Autostart-Schalter.",
            done = null, // MIUI bietet dafür keine öffentliche Abfrage
            onOpen = { MiuiIntents.openAutostart(context) },
        )
    }

    steps += StepUi(
        title = "Akku-Optimierung deaktivieren",
        description = "MIUI beendet Hintergrunddienste aggressiv. Stelle die App auf " +
            "\"Keine Einschränkungen\".",
        manualHint = "Einstellungen > Apps > Apps verwalten > App Interceptor > " +
            "Energiesparmodus (Akkusparer) > \"Keine Einschränkungen\" wählen.",
        done = PermissionChecks.ignoresBatteryOptimizations(context),
        onOpen = { MiuiIntents.openBatterySettings(context) },
    )

    if (Build.VERSION.SDK_INT >= 33) {
        steps += StepUi(
            title = "Benachrichtigungen erlauben",
            description = "Nur für Warnungen, falls MIUI den Dienst deaktiviert hat (z. B. nach Neustart).",
            manualHint = "Einstellungen > Apps > Apps verwalten > App Interceptor > Benachrichtigungen erlauben.",
            done = PermissionChecks.notificationsAllowed(context),
            onOpen = { requestNotifications(); true },
        )
    }

    if (isMiui) {
        steps += StepUi(
            title = "MIUI: App im Speicher sperren (empfohlen)",
            description = "Verhindert, dass MIUI die App beim Aufräumen der letzten Apps beendet.",
            manualHint = "Übersicht der letzten Apps öffnen (Menü-Taste), die Karte von " +
                "App Interceptor gedrückt halten und das Schloss-Symbol antippen (\"Sperren\").",
            done = null,
            onOpen = { false }, // nur manuell möglich → Anleitung anzeigen
        )
    }

    return steps
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tick by remember { mutableIntStateOf(0) }
    OnResumeEffect { tick++ }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { tick++ }

    val isMiui = remember { MiuiIntents.isMiui() }
    val steps = remember(tick) {
        buildSteps(context, isMiui) {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Einrichtung") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isMiui) {
                        "Xiaomi/MIUI erkannt. Damit der Interceptor zuverlässig funktioniert, " +
                            "müssen ALLE folgenden Schritte erledigt werden – Standard-Android-" +
                            "Berechtigungen reichen auf MIUI nicht aus."
                    } else {
                        "Bitte die folgenden Berechtigungen erteilen. Hinweis: Dieses Gerät " +
                            "scheint kein MIUI/HyperOS zu sein – die Xiaomi-Schritte entfallen."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(steps) { index, step ->
                StepCard(index = index + 1, step = step)
            }
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            Graph.settings.setOnboardingDone()
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Einrichtung abschließen")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepCard(index: Int, step: StepUi) {
    val context = LocalContext.current
    var showHint by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (step.done) {
                    true -> Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32))
                    false -> Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                    null -> Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "$index. ${step.title}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(step.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = {
                    val opened = step.onOpen()
                    if (!opened) {
                        showHint = true
                        Toast.makeText(
                            context,
                            "Einstellungsseite nicht gefunden – bitte der manuellen Anleitung folgen",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }) {
                    Text("Einstellungen öffnen")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showHint = !showHint }) {
                    Text(if (showHint) "Anleitung ausblenden" else "Manuelle Anleitung")
                }
            }
            if (showHint) {
                Spacer(Modifier.height(8.dp))
                Text(
                    step.manualHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
