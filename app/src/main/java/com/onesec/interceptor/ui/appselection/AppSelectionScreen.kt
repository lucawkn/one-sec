package com.onesec.interceptor.ui.appselection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: AppSelectionViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Wähle Apps aus, die vor dem Öffnen eine kurze Pause bekommen sollen.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(20.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items, key = { it.packageName }) { item ->
                AppRow(
                    item = item,
                    onToggle = { checked ->
                        viewModel.setMonitored(item.packageName, item.appName, checked)
                    }
                )
            }
        }
    }
}

@Composable
private fun AppRow(item: AppSelectionItem, onToggle: (Boolean) -> Unit) {
    val bitmap = remember(item.packageName) { item.icon.toBitmap().asImageBitmap() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = item.appName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = item.isMonitored, onCheckedChange = onToggle)
    }
}
