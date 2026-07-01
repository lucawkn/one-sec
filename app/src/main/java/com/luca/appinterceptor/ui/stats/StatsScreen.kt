package com.luca.appinterceptor.ui.stats

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luca.appinterceptor.data.Graph
import com.luca.appinterceptor.data.InterceptEvent
import java.util.Calendar

private data class StatRow(
    val packageName: String,
    val opened: Int,
    val blocked: Int,
)

private fun aggregate(events: List<InterceptEvent>): List<StatRow> =
    events
        .groupBy { it.packageName }
        .map { (pkg, list) ->
            StatRow(pkg, list.count { it.opened }, list.count { !it.opened })
        }
        .sortedByDescending { it.opened + it.blocked }

private fun appLabel(context: Context, packageName: String): String = try {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (e: Exception) {
    packageName
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val weekStart = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val events by remember { Graph.db.eventDao().eventsSince(weekStart) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistik") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            statSection("Heute", events.filter { it.timestamp >= todayStart })
            item { Spacer(Modifier.height(24.dp)) }
            statSection("Letzte 7 Tage", events)
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun LazyListScope.statSection(title: String, events: List<InterceptEvent>) {
    val rows = aggregate(events)
    val totalOpened = rows.sumOf { it.opened }
    val totalBlocked = rows.sumOf { it.blocked }

    item {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "$totalOpened× geöffnet · $totalBlocked× abgebrochen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))
    }
    if (rows.isEmpty()) {
        item {
            Text(
                "Noch keine Daten.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    } else {
        items(rows, key = { "$title-${it.packageName}" }) { row ->
            val context = LocalContext.current
            val label = remember(row.packageName) { appLabel(context, row.packageName) }
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(Modifier.padding(16.dp)) {
                    Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text("${row.opened}× geöffnet", color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text("${row.blocked}× abgebrochen", color = Color(0xFF2E7D32))
                }
            }
        }
    }
}
