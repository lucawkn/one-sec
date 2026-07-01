package com.onesec.interceptor.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onesec.interceptor.data.local.dao.AppStatSummary
import com.onesec.interceptor.data.repository.StatsRange
import com.onesec.interceptor.ui.theme.SoftGreen
import com.onesec.interceptor.ui.theme.SoftRed

private val StatsRange.label: String
    get() = when (this) {
        StatsRange.TODAY -> "Heute"
        StatsRange.WEEK -> "Diese Woche"
        StatsRange.ALL_TIME -> "Gesamt"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel()
) {
    val range by viewModel.range.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val ranges = StatsRange.entries

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text("Statistik", style = MaterialTheme.typography.headlineSmall)
        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ranges.forEachIndexed { index, r ->
                SegmentedButton(
                    selected = r == range,
                    onClick = { viewModel.setRange(r) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size)
                ) {
                    Text(r.label)
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        if (stats.isEmpty()) {
            Text(
                "Noch keine Daten für diesen Zeitraum.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stats, key = { it.packageName }) { stat ->
                    StatRow(stat)
                }
            }
        }
    }
}

@Composable
private fun StatRow(stat: AppStatSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stat.appName, style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

            val total = (stat.allowedCount + stat.cancelledCount).coerceAtLeast(1)
            val allowedWeight = stat.allowedCount.toFloat() / total
            val cancelledWeight = stat.cancelledCount.toFloat() / total

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                if (allowedWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(allowedWeight)
                            .fillMaxSize()
                            .background(SoftGreen)
                    )
                }
                if (cancelledWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(cancelledWeight)
                            .fillMaxSize()
                            .background(SoftRed)
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Geöffnet: ${stat.allowedCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Abgebrochen: ${stat.cancelledCount}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
