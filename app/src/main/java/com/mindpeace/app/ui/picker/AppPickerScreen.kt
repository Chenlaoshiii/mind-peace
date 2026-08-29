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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.mindpeace.app.ui.components.PeacePageTitle
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
    var query by rememberSaveable { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showApps by rememberSaveable { mutableStateOf(true) }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isTab = onBack == null

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

    val subtitle = if (loading) null else stringResource(R.string.picker_subtitle, filtered.size)

    if (isTab) {
        AppPickerBody(
            loading = loading,
            filtered = filtered,
            query = query,
            onQuery = { query = it },
            showApps = showApps,
            showSystem = showSystem,
            onToggleApps = { showApps = !showApps },
            onToggleSystem = { showSystem = !showSystem },
            watchedMap = watchedMap,
            showPageTitle = true,
            titleSubtitle = subtitle,
            onToggle = { entry, existing, checked ->
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
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Scaffold(
            containerColor = peaceContainerColor(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.picker_title))
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        PeaceIconButton(onClick = onBack!!) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            AppPickerBody(
                loading = loading,
                filtered = filtered,
                query = query,
                onQuery = { query = it },
                showApps = showApps,
                showSystem = showSystem,
                onToggleApps = { showApps = !showApps },
                onToggleSystem = { showSystem = !showSystem },
                watchedMap = watchedMap,
                showPageTitle = false,
                titleSubtitle = subtitle,
                onToggle = { entry, existing, checked ->
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppPickerBody(
    loading: Boolean,
    filtered: List<AppEntry>,
    query: String,
    onQuery: (String) -> Unit,
    showApps: Boolean,
    showSystem: Boolean,
    onToggleApps: () -> Unit,
    onToggleSystem: () -> Unit,
    watchedMap: Map<String, WatchedApp>,
    showPageTitle: Boolean,
    titleSubtitle: String?,
    onToggle: (AppEntry, WatchedApp?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (loading) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.picker_loading),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (showPageTitle) {
            item(key = "title") {
                PeacePageTitle(
                    text = stringResource(R.string.picker_title),
                    subtitle = titleSubtitle,
                )
            }
        }
        item {
            com.mindpeace.app.ui.theme.PeaceTextField(
                value = query,
                onValueChange = onQuery,
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
                    onClick = onToggleApps,
                    label = { Text(stringResource(R.string.picker_chip_apps)) },
                )
                PeaceChip(
                    selected = showSystem,
                    onClick = onToggleSystem,
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
                        onCheckedChange = { checked -> onToggle(entry, existing, checked) },
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
}
