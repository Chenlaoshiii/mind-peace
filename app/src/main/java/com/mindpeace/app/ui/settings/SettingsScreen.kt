package com.mindpeace.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.data.ColorMode
import com.mindpeace.app.data.COOLAPK_AUTHOR_URL
import com.mindpeace.app.data.GITHUB_REPO_URL
import com.mindpeace.app.ui.components.PeacePageTitle
import com.mindpeace.app.ui.components.SelfMessageEditor
import com.mindpeace.app.ui.theme.PeaceIconButton
import com.mindpeace.app.ui.theme.PeaceListGroup
import com.mindpeace.app.ui.theme.PeaceListRow
import com.mindpeace.app.ui.theme.peaceContainerColor
import com.mindpeace.app.ui.theme.peaceSurfaceColor
import com.mindpeace.app.util.AppLocale
import com.mindpeace.app.util.Permissions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onReopenSetup: () -> Unit,
    onAbout: () -> Unit,
    onTheme: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val quote by app.container.settings.selfMessage.collectAsStateWithLifecycle()
    val appearance by app.container.settings.appearance.collectAsStateWithLifecycle()
    val appLocale by app.container.settings.appLocale.collectAsStateWithLifecycle()
    var a11y by remember { mutableStateOf(Permissions.isAccessibilityEnabled(context)) }
    var battery by remember { mutableStateOf(Permissions.isIgnoringBatteryOptimizations(context)) }
    var notif by remember { mutableStateOf(Permissions.areNotificationsEnabled(context)) }
    var overlay by remember { mutableStateOf(Permissions.canDrawOverlays(context)) }
    var showLock by remember { mutableStateOf(false) }
    var showSelf by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notif = it || Permissions.areNotificationsEnabled(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                a11y = Permissions.isAccessibilityEnabled(context)
                battery = Permissions.isIgnoringBatteryOptimizations(context)
                notif = Permissions.areNotificationsEnabled(context)
                overlay = Permissions.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onLabel = stringResource(R.string.settings_status_on)
    val offLabel = stringResource(R.string.settings_status_off)
    val themeSub = when (appearance.colorMode) {
        ColorMode.LIGHT -> stringResource(R.string.settings_color_light)
        ColorMode.DARK -> stringResource(R.string.settings_color_dark)
        ColorMode.SYSTEM -> stringResource(R.string.settings_color_system)
    }

    val body: @Composable (Modifier, Boolean) -> Unit = { modifier, showPageTitle ->
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
        ) {
            if (showPageTitle) {
                PeacePageTitle(stringResource(R.string.settings_title))
            }
            PeaceListGroup {
                PeaceListRow(
                    title = stringResource(R.string.settings_a11y),
                    trailing = if (a11y) onLabel else offLabel,
                    trailingHighlight = a11y,
                    onClick = { Permissions.openAccessibilitySettings(context) },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_battery),
                    trailing = if (battery) onLabel else offLabel,
                    trailingHighlight = battery,
                    onClick = { Permissions.openBatteryOptimization(context) },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_sub),
                    trailing = if (notif) onLabel else offLabel,
                    trailingHighlight = notif,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && !notif) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            Permissions.openNotificationSettings(context)
                        }
                    },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_overlay),
                    subtitle = stringResource(R.string.settings_overlay_sub),
                    trailing = if (overlay) onLabel else offLabel,
                    trailingHighlight = overlay,
                    onClick = { Permissions.openOverlaySettings(context) },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = themeSub,
                    onClick = onTheme,
                )
                PeaceListRow(
                    title = "${stringResource(R.string.settings_language_word)}/Language",
                    subtitle = languageSubtitle(appLocale),
                    onClick = { showLanguage = true },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_self_message),
                    subtitle = quote.ifBlank { stringResource(R.string.settings_self_message_sub) },
                    onClick = { showSelf = true },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_lock),
                    onClick = { showLock = true },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_setup),
                    onClick = onReopenSetup,
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_about),
                    showDivider = false,
                    onClick = onAbout,
                )
            }
            Spacer(Modifier.height(12.dp))
            CreditLinkCard(
                title = stringResource(R.string.settings_credit_title),
                line = stringResource(R.string.settings_credit_line),
                onClick = { openBilibili(context) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            CreditLinkCard(
                title = stringResource(R.string.settings_github_title),
                line = stringResource(R.string.settings_github_line),
                onClick = { openUrl(context, GITHUB_REPO_URL) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            CreditLinkCard(
                title = stringResource(R.string.settings_coolapk_title),
                line = stringResource(R.string.settings_coolapk_line),
                onClick = { openUrl(context, COOLAPK_AUTHOR_URL) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (onBack == null) {
        body(Modifier.fillMaxSize(), true)
    } else {
        Scaffold(
            containerColor = peaceContainerColor(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        PeaceIconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = peaceSurfaceColor()),
                )
            },
        ) { padding ->
            body(Modifier.fillMaxSize().padding(padding), false)
        }
    }

    if (showLanguage) {
        AlertDialog(
            onDismissRequest = { showLanguage = false },
            confirmButton = {
                TextButton(onClick = { showLanguage = false }) {
                    Text(stringResource(R.string.overlay_timeup_ok))
                }
            },
            title = { Text("${stringResource(R.string.settings_language_word)}/Language") },
            text = {
                Column {
                    AppLocale.options.forEach { opt ->
                        val selected = AppLocale.normalize(appLocale) == opt.tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { app.container.settings.setAppLocale(opt.tag) }
                                    showLanguage = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(opt.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = stringResource(R.string.settings_status_on),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
        )
    }
    if (showLock) {
        AlertDialog(
            onDismissRequest = { showLock = false },
            confirmButton = {
                TextButton(onClick = { showLock = false }) { Text(stringResource(R.string.overlay_timeup_ok)) }
            },
            title = { Text(stringResource(R.string.settings_lock)) },
            text = {
                Column {
                    Text(stringResource(R.string.onboarding_lock_xiaomi), modifier = Modifier.padding(bottom = 8.dp))
                    Text(stringResource(R.string.onboarding_lock_huawei), modifier = Modifier.padding(bottom = 8.dp))
                    Text(stringResource(R.string.onboarding_lock_oppo), modifier = Modifier.padding(bottom = 8.dp))
                    Text(stringResource(R.string.onboarding_lock_vivo), modifier = Modifier.padding(bottom = 8.dp))
                    Text(stringResource(R.string.onboarding_lock_pixel))
                }
            },
        )
    }
    if (showSelf) {
        var draft by remember { mutableStateOf(quote) }
        AlertDialog(
            onDismissRequest = { showSelf = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { app.container.settings.setSelfMessage(draft) }
                        showSelf = false
                    },
                    enabled = draft.trim().length >= 4,
                ) { Text(stringResource(R.string.settings_self_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSelf = false }) { Text(stringResource(R.string.overlay_cap_cancel)) }
            },
            title = { Text(stringResource(R.string.settings_self_message)) },
            text = {
                SelfMessageEditor(value = draft, onChange = { draft = it }, minLines = 2)
            },
        )
    }
}

@Composable
private fun languageSubtitle(appLocale: String): String {
    val stored = AppLocale.normalize(appLocale)
    val label = stringResource(AppLocale.labelRes(stored))
    if (!AppLocale.isSystem(stored)) return label
    val resolved = AppLocale.resolve(stored, AppLocale.systemLocaleList())
    val resolvedLabel = stringResource(AppLocale.labelRes(resolved))
    return if (resolvedLabel.isNotBlank() && resolvedLabel != label) {
        "$label · $resolvedLabel"
    } else {
        label
    }
}
