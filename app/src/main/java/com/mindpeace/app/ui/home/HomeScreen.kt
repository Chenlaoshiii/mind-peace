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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mindpeace.app.ui.theme.PeaceFab
import androidx.compose.material3.Icon
import com.mindpeace.app.ui.theme.PeaceIconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.mindpeace.app.ui.theme.PeaceButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mindpeace.app.R
import com.mindpeace.app.ui.components.AppIcon
import com.mindpeace.app.ui.theme.PeaceCard
import com.mindpeace.app.ui.theme.peaceContainerColor
import com.mindpeace.app.ui.theme.peaceSurfaceColor
import com.mindpeace.app.util.Permissions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onApp: (String) -> Unit,
    onStats: () -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
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

    Scaffold(
        containerColor = peaceContainerColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    PeaceIconButton(onClick = onStats) {
                        Icon(Icons.Outlined.Insights, contentDescription = stringResource(R.string.cd_stats))
                    }
                    PeaceIconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = peaceSurfaceColor(),
                ),
            )
        },
        floatingActionButton = {
            PeaceFab(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.cd_add))
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NotifBanner(notif) {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else Permissions.openNotificationSettings(context)
                }
                StatsCard(onStats)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.home_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_empty_body),
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                item(key = "stats") { StatsCard(onStats) }
                items(items, key = { it.packageName }) { item ->
                    PeaceCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .animateItem(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(20.dp),
                        onClick = { onApp(item.packageName) },
                    ) {
                        ListItem(
                            headlineContent = { Text(item.label) },
                            supportingContent = {
                                Column {
                                    Text(item.usedLine)
                                    if (item.remainingLine.isNotBlank()) {
                                        Text(
                                            item.remainingLine,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            },
                            leadingContent = {
                                AppIcon(item.packageName, Modifier.size(44.dp))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(onStats: () -> Unit) {
    PeaceCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        onClick = onStats,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.home_stats_card), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_stats_card_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
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
