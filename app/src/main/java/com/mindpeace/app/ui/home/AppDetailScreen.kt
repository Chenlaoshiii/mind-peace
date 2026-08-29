package com.mindpeace.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mindpeace.app.ui.theme.PeaceChip
import androidx.compose.material3.Icon
import com.mindpeace.app.ui.theme.PeaceIconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.mindpeace.app.ui.theme.PeaceButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.data.WatchedApp
import com.mindpeace.app.ui.components.AppIcon
import com.mindpeace.app.ui.theme.peaceContainerColor
import com.mindpeace.app.util.formatDurationMillis
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PRESETS = listOf(0, 10, 15, 30, 45, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppDetailScreen(packageName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val watched by app.container.settings.watchedApps.collectAsStateWithLifecycle()
    val current = watched.firstOrNull { it.packageName == packageName }
        ?: WatchedApp(packageName)
    val label = remember(packageName) { app.container.installedApps.labelOf(packageName) }
    val used = app.container.settings.usedMillisToday(packageName)
    val scope = rememberCoroutineScope()
    var customText by remember { mutableStateOf("") }

    fun save(next: WatchedApp) {
        scope.launch {
            app.container.settings.upsertWatched(next)
            app.container.settings.setAppAllocation(packageName, next.dailyLimitMinutes)
        }
    }

    Scaffold(
        containerColor = peaceContainerColor(),
        topBar = {
            TopAppBar(
                title = { Text(label) },
                navigationIcon = {
                    PeaceIconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            AppIcon(packageName, Modifier.size(64.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.detail_today, formatDurationMillis(context, used)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.detail_enable)) },
                supportingContent = { Text(stringResource(R.string.detail_enable_sub)) },
                trailingContent = {
                    Switch(
                        checked = current.enabled,
                        onCheckedChange = { save(current.copy(enabled = it)) },
                    )
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.detail_limit), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.detail_limit_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Text(
                if (current.dailyLimitMinutes <= 0) stringResource(R.string.detail_limit_none)
                else stringResource(R.string.detail_limit_minutes, current.dailyLimitMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = current.dailyLimitMinutes.coerceIn(0, 180).toFloat(),
                onValueChange = { save(current.copy(dailyLimitMinutes = it.roundToInt())) },
                valueRange = 0f..180f,
                steps = 35,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                PRESETS.forEach { m ->
                    PeaceChip(
                        selected = current.dailyLimitMinutes == m,
                        onClick = { save(current.copy(dailyLimitMinutes = m)) },
                        label = {
                            Text(
                                if (m == 0) stringResource(R.string.detail_limit_none)
                                else stringResource(R.string.detail_limit_minutes, m),
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                    )
                }
            }
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text(stringResource(R.string.detail_custom)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            customText.toIntOrNull()?.let { save(current.copy(dailyLimitMinutes = it.coerceIn(0, 24 * 60))) }
                        },
                        enabled = customText.toIntOrNull() != null,
                    ) { Text(stringResource(R.string.overlay_custom_ok)) }
                },
            )
            Spacer(Modifier.height(32.dp))
            PeaceButton(
                onClick = {
                    scope.launch {
                        app.container.settings.removeWatched(packageName)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                outlined = true,
            ) { Text(stringResource(R.string.detail_remove)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
