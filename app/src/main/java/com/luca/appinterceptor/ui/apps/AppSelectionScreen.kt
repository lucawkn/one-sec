package com.luca.appinterceptor.ui.apps

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import com.luca.appinterceptor.data.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppListViewModel(app: Application) : AndroidViewModel(app) {

    data class AppEntry(
        val packageName: String,
        val label: String,
        val icon: Drawable?,
    )

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val application = getApplication<Application>()
            val pm = application.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }
            _apps.value = resolved
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != application.packageName }
                .map { info ->
                    AppEntry(
                        packageName = info.activityInfo.packageName,
                        label = info.loadLabel(pm).toString(),
                        icon = try {
                            info.loadIcon(pm)
                        } catch (e: Exception) {
                            null
                        },
                    )
                }
                .sortedBy { it.label.lowercase() }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    onBack: () -> Unit,
    viewModel: AppListViewModel = viewModel(),
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val watched by Graph.settings.watchedApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Überwachte Apps (${watched.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Suchen") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = apps.filter {
                    query.isBlank() ||
                        it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.packageName }) { entry ->
                        AppRow(
                            entry = entry,
                            checked = entry.packageName in watched,
                            onToggle = {
                                scope.launch { Graph.settings.toggleWatched(entry.packageName) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    entry: AppListViewModel.AppEntry,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconPainter = remember(entry.packageName) {
            entry.icon?.let { drawable ->
                runCatching { BitmapPainter(drawable.toBitmap(96, 96).asImageBitmap()) }.getOrNull()
            }
        }
        if (iconPainter != null) {
            Image(painter = iconPainter, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                entry.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}
