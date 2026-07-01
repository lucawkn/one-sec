package com.luca.appinterceptor.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luca.appinterceptor.data.Graph
import com.luca.appinterceptor.service.InterceptorService
import com.luca.appinterceptor.ui.OnResumeEffect
import com.luca.appinterceptor.util.MiuiIntents
import com.luca.appinterceptor.util.PermissionChecks
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class HealthItem(
    val title: String,
    val ok: Boolean?, // null = nicht automatisch prüfbar
    val fix: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenApps: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Health-Check bei jedem Resume neu auswerten (MIUI deaktiviert
    // Berechtigungen gern stillschweigend nach Updates)
    var tick by remember { mutableIntStateOf(0) }
    OnResumeEffect { tick++ }

    val healthItems = remember(tick) {
        listOf(
            HealthItem(
                title = "Bedienungshilfen-Dienst aktiviert",
                ok = PermissionChecks.accessibilityEnabled(context),
                fix = { MiuiIntents.openAccessibilitySettings(context) },
            ),
            HealthItem(
                title = "Dienst läuft gerade",
                ok = InterceptorService.isRunning,
                fix = { MiuiIntents.openAccessibilitySettings(context) },
            ),
            HealthItem(
                title = "Anzeige über anderen Apps",
                ok = PermissionChecks.canDrawOverlays(context),
                fix = { MiuiIntents.openOverlaySettings(context) },
            ),
            HealthItem(
                title = "MIUI: Popup im Hintergrund",
                ok = PermissionChecks.miuiBackgroundPopupAllowed(context),
                fix = { MiuiIntents.openMiuiPermissionEditor(context) },
            ),
            HealthItem(
                title = "Akku-Optimierung deaktiviert",
                ok = PermissionChecks.ignoresBatteryOptimizations(context),
                fix = { MiuiIntents.openBatterySettings(context) },
            ),
        )
    }
    val hasProblem = healthItems.any { it.ok == false }

    val watched by Graph.settings.watchedApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val cooldown by Graph.settings.cooldownMinutes.collectAsStateWithLifecycle(initialValue = 5)
    var sliderValue by remember(cooldown) { mutableFloatStateOf(cooldown.toFloat()) }

    Scaffold(topBar = { TopAppBar(title = { Text("App Interceptor") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (hasProblem) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = "Es fehlen Berechtigungen – der Interceptor funktioniert " +
                            "möglicherweise nicht. Bitte die rot markierten Punkte beheben.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Health-Check", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                    healthItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            when (item.ok) {
                                true -> Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32))
                                false -> Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                                null -> Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(item.title, modifier = Modifier.weight(1f))
                            if (item.ok != true) {
                                TextButton(onClick = item.fix) { Text("Öffnen") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Einstellungen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenApps, modifier = Modifier.fillMaxWidth()) {
                Text("Überwachte Apps auswählen (${watched.size})")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
                Text("Statistik ansehen")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("Einrichtung (MIUI-Berechtigungen) öffnen")
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Cooldown nach \"Ja, öffnen\": ${sliderValue.roundToInt()} Minuten",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..30f,
                steps = 28,
                onValueChangeFinished = {
                    scope.launch { Graph.settings.setCooldownMinutes(sliderValue.roundToInt()) }
                },
            )
            Text(
                "So lange wird eine App nach bewusstem Öffnen nicht erneut unterbrochen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
