package com.onesec.interceptor.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App-Interceptor aktiv", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = settings.interceptorEnabled,
                        onCheckedChange = viewModel::setInterceptorEnabled
                    )
                }
                Text(
                    "Wenn deaktiviert, wird keine überwachte App unterbrochen.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Ruhezeit nach \"Ja, öffnen\"", style = MaterialTheme.typography.titleMedium)
                Text(
                    "So lange wird eine erlaubte App nicht erneut unterbrochen.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${settings.graceMinutes} Minuten",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = settings.graceMinutes.toFloat(),
                    onValueChange = { viewModel.setGraceMinutes(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Dauer der Atem-Anleitung", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Wie lange die Pause vor der Rückfrage angezeigt wird.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${settings.breathingSeconds} Sekunden",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = settings.breathingSeconds.toFloat(),
                    onValueChange = { viewModel.setBreathingSeconds(it.toInt()) },
                    valueRange = 5f..30f,
                    steps = 24
                )
            }
        }
    }
}
