package com.mindpeace.app.ui.picker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mindpeace.app.ui.theme.PeaceChip
import androidx.compose.material3.Icon
import com.mindpeace.app.ui.theme.PeaceIconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mindpeace.app.data.AppEntry
import com.mindpeace.app.data.WatchedApp
import com.mindpeace.app.ui.components.AppIcon
import com.mindpeace.app.ui.theme.peaceContainerColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppPickerScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val watched by app.container.settings.watchedApps.collectAsStateWithLifecycle()
    val watchedMap = remember(watched) { watched.associateBy { it.packageName } }
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showApps by remember { mutableStateOf(true) }
    var showSystem by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            app.container.installedApps.loadLaunchable()
        }
        loading = false
    }

    val bothOff = !showApps && !showSystem
    val includeApps = showApps || bothOff
    val includeSystem = showSystem || bothOff
    val filtered = remember(apps, query, includeApps, includeSystem) {
        val q = query.trim()
        apps.asSequence()
            .filter { entry ->
                when {
                    includeApps && includeSystem -> true
                    includeApps -> !entry.isSystem
                    includeSystem -> entry.isSystem
                    else -> true
                }
            }
            .filter {
                q.isEmpty() ||
                    it.label.contains(q, true) ||
                    it.packageName.contains(q, true)
            }
            .toList()
    }

    Scaffold(
        containerColor = peaceContainerColor(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.picker_title))
                        if (!loading) {
                            Text(
                                text = stringResource(R.string.picker_subtitle, filtered.size),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        PeaceIconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.picker_loading),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                com.mindpeace.app.ui.theme.PeaceTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = stringResource(R.string.picker_search),
                    singleLine = true,
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PeaceChip(
                        selected = showApps,
                        onClick = { showApps = !showApps },
                        label = { Text(stringResource(R.string.picker_chip_apps)) },
                    )
                    PeaceChip(
                        selected = showSystem,
                        onClick = { showSystem = !showSystem },
                        label = { Text(stringResource(R.string.picker_chip_system)) },
                    )
                }
            }
            if (filtered.isEmpty()) {
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = stringResource(R.string.picker_empty),
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }
            items(filtered, key = { it.packageName }) { entry ->
                val existing = watchedMap[entry.packageName]
                val on = existing?.enabled == true
                ListItem(
                    headlineContent = { Text(entry.label) },
                    supportingContent = { Text(entry.packageName) },
                    leadingContent = { AppIcon(entry.packageName, Modifier.size(40.dp)) },
                    trailingContent = {
                        Switch(
                            checked = on,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    if (checked) {
                                        app.container.settings.upsertWatched(
                                            (existing ?: WatchedApp(entry.packageName)).copy(enabled = true),
                                        )
                                    } else if (existing != null) {
                                        app.container.settings.removeWatched(entry.packageName)
                                    }
                                }
                            },
                        )
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
