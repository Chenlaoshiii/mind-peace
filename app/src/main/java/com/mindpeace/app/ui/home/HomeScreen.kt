package com.mindpeace.app.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.ui.components.AppIcon
import com.mindpeace.app.ui.components.PeacePageTitle
import com.mindpeace.app.ui.theme.PeaceButton
import com.mindpeace.app.ui.theme.PeaceCard
import com.mindpeace.app.ui.theme.PeaceChip
import com.mindpeace.app.ui.theme.peaceSliderColors
import com.mindpeace.app.util.Permissions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val GLOBAL_PRESETS = listOf(0, 30, 60, 90, 120, 180, 240)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdd: () -> Unit,
    onApp: (String) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val global by viewModel.globalDailyLimitMinutes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val scope = rememberCoroutineScope()
    var notif by remember { mutableStateOf(Permissions.areNotificationsEnabled(context)) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notif = it || Permissions.areNotificationsEnabled(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notif = Permissions.areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val watched = app.container.settings.watchedApps.collectAsStateWithLifecycle().value
    val allocatedSum = watched.sumOf { it.dailyLimitMinutes }
    val unallocated = if (global <= 0) 0 else (global - allocatedSum).coerceAtLeast(0)
    val globalUsed = app.container.settings.usedMillisTodayTotal()
    val globalLabel = stringResource(R.string.quota_raise_global_label)

    var challenge by remember { mutableStateOf<QuotaRaisePending?>(null) }
    var globalPreview by remember { mutableIntStateOf(global) }
    var globalDragging by remember { mutableStateOf(false) }
    LaunchedEffect(global) {
        if (!globalDragging && challenge == null) globalPreview = global
    }

    fun commitGlobal(minutes: Int) {
        scope.launch { app.container.settings.setGlobalDailyLimitMinutes(minutes) }
    }

    fun requestGlobal(minutes: Int) {
        val snapped = minutes.coerceIn(0, 360)
        globalPreview = snapped
        requestQuotaLimitChange(
            oldMinutes = global,
            newMinutes = snapped,
            usedTodayMillis = globalUsed,
            appLabel = globalLabel,
            apply = { commitGlobal(snapped) },
            revert = { globalPreview = global },
            startChallenge = { challenge = it },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "title") {
            PeacePageTitle(stringResource(R.string.budget_title))
        }
        if (!notif) {
            item(key = "notif") {
                NotifBanner(false) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Permissions.openNotificationSettings(context)
                    }
                }
            }
        }
        item(key = "global") {
            PeaceCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.budget_global),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.budget_global_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (globalPreview <= 0) stringResource(R.string.budget_none)
                        else stringResource(R.string.budget_minutes, globalPreview),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Slider(
                        value = globalPreview.coerceIn(0, 360).toFloat(),
                        onValueChange = {
                            globalDragging = true
                            globalPreview = ((it / 5f).roundToInt() * 5).coerceIn(0, 360)
                        },
                        onValueChangeFinished = {
                            globalDragging = false
                            requestGlobal(globalPreview)
                        },
                        valueRange = 0f..360f,
                        colors = peaceSliderColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GLOBAL_PRESETS.forEach { m ->
                            PeaceChip(
                                selected = globalPreview == m,
                                onClick = { requestGlobal(m) },
                                label = {
                                    Text(
                                        if (m == 0) stringResource(R.string.budget_none)
                                        else stringResource(R.string.detail_limit_minutes, m),
                                    )
                                },
                            )
                        }
                    }
                    if (global > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.budget_allocated, allocatedSum, global),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            if (unallocated > 0) stringResource(R.string.budget_unallocated, unallocated)
                            else stringResource(R.string.budget_unallocated_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.budget_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.budget_empty_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    PeaceButton(onClick = onAdd, outlined = true) {
                        Text(stringResource(R.string.home_add))
                    }
                }
            }
        }
        items(items, key = { it.packageName }) { item ->
            val current = watched.firstOrNull { it.packageName == item.packageName }
            val alloc = current?.dailyLimitMinutes ?: 0
            val others = allocatedSum - alloc
            val maxAlloc = if (global <= 0) 360 else (global - others).coerceAtLeast(0)
            AppBudgetRow(
                item = item,
                alloc = alloc,
                maxAlloc = maxAlloc,
                global = global,
                usedTodayMillis = app.container.settings.usedMillisToday(item.packageName),
                challengeHeld = challenge != null,
                onApp = onApp,
                onCommit = { minutes ->
                    scope.launch {
                        app.container.settings.setAppAllocation(item.packageName, minutes)
                    }
                },
                onChallenge = { challenge = it },
                modifier = Modifier.animateItem(),
            )
        }
    }
    QuotaRaiseChallenge(pending = challenge, onClear = { challenge = null })
}

@Composable
private fun AppBudgetRow(
    item: HomeItem,
    alloc: Int,
    maxAlloc: Int,
    global: Int,
    usedTodayMillis: Long,
    challengeHeld: Boolean,
    onApp: (String) -> Unit,
    onCommit: (Int) -> Unit,
    onChallenge: (QuotaRaisePending) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderMax = maxOf(1, maxAlloc, alloc)
    var preview by remember(item.packageName) { mutableIntStateOf(alloc) }
    var dragging by remember(item.packageName) { mutableStateOf(false) }
    LaunchedEffect(alloc) {
        if (!dragging && !challengeHeld) preview = alloc
    }

    fun request(minutes: Int) {
        val snapped = minutes.coerceIn(0, sliderMax)
        preview = snapped
        requestQuotaLimitChange(
            oldMinutes = alloc,
            newMinutes = snapped,
            usedTodayMillis = usedTodayMillis,
            appLabel = item.label,
            apply = { onCommit(snapped) },
            revert = { preview = alloc },
            startChallenge = onChallenge,
        )
    }

    PeaceCard(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        onClick = { onApp(item.packageName) },
    ) {
        ListItem(
            headlineContent = { Text(item.label) },
            supportingContent = {
                Text(item.usedLine)
            },
            leadingContent = {
                AppIcon(item.packageName, Modifier.size(44.dp))
            },
        )
        Slider(
            value = preview.coerceIn(0, sliderMax).toFloat(),
            onValueChange = {
                dragging = true
                preview = ((it / 5f).roundToInt() * 5).coerceIn(0, sliderMax)
            },
            onValueChangeFinished = {
                dragging = false
                request(preview)
            },
            valueRange = 0f..sliderMax.toFloat(),
            colors = peaceSliderColors(),
            enabled = global <= 0 || maxAlloc > 0 || alloc > 0,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun NotifBanner(granted: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible = !granted, enter = fadeIn(), exit = fadeOut()) {
        PeaceCard(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(20.dp),
            onClick = onClick,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.home_notif_banner),
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(onClick = onClick) {
                    Text(stringResource(R.string.onboarding_notif_action))
                }
            }
        }
    }
}
