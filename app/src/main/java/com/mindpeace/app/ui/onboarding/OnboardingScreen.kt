package com.mindpeace.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import com.mindpeace.app.ui.theme.PeaceButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.ui.components.SelfMessageEditor
import com.mindpeace.app.util.Permissions
import com.mindpeace.app.work.WorkScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LAST_STEP = 5

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    fromSettings: Boolean,
    onCancelSetup: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val settings = app.container.settings
    val recents by settings.recentsLockedConfirmed.collectAsStateWithLifecycle()
    val savedQuote by settings.selfMessage.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(0) }
    var a11y by remember { mutableStateOf(Permissions.isAccessibilityEnabled(context)) }
    var battery by remember { mutableStateOf(Permissions.isIgnoringBatteryOptimizations(context)) }
    var notif by remember { mutableStateOf(Permissions.areNotificationsEnabled(context)) }
    var selfText by remember { mutableStateOf(savedQuote) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(savedQuote) {
        if (selfText.isBlank() && savedQuote.isNotBlank()) selfText = savedQuote
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notif = granted || Permissions.areNotificationsEnabled(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                a11y = Permissions.isAccessibilityEnabled(context)
                battery = Permissions.isIgnoringBatteryOptimizations(context)
                notif = Permissions.areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            a11y = Permissions.isAccessibilityEnabled(context)
            battery = Permissions.isIgnoringBatteryOptimizations(context)
            notif = Permissions.areNotificationsEnabled(context)
            delay(800)
        }
    }

    val canFinish = a11y && recents
    val selfOk = selfText.trim().length >= 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_step, step + 1, LAST_STEP + 1),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                )
                (fadeIn(tween(320)) + slideInHorizontally(springSpec) { dir * it / 8 }) togetherWith
                    (fadeOut(tween(240)) + slideOutHorizontally(springSpec) { -dir * it / 8 })
            },
            label = "onboarding",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                when (current) {
                    0 -> WelcomeStep()
                    1 -> SelfMessageStep(selfText) { selfText = it }
                    2 -> HowStep()
                    3 -> A11yStep(a11y)
                    4 -> BatteryNotifStep(battery, notif, notifLauncher)
                    else -> LockStep(recents) { checked ->
                        scope.launch { settings.setRecentsLockedConfirmed(checked) }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (step == LAST_STEP) {
            if (!canFinish) {
                Text(
                    text = stringResource(R.string.onboarding_finish_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            PeaceButton(
                onClick = {
                    scope.launch {
                        settings.setSelfMessage(selfText)
                        settings.setOnboarded(true)
                        WorkScheduler.ensureScheduled(context)
                        onFinished()
                    }
                },
                enabled = canFinish,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.onboarding_finish)) }
        } else {
            val nextEnabled = step != 1 || selfOk
            PeaceButton(
                onClick = {
                    if (step == 1) {
                        scope.launch { settings.setSelfMessage(selfText) }
                    }
                    step += 1
                },
                enabled = nextEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.onboarding_next)) }
            if (step == 1 && !selfOk) {
                Text(
                    text = stringResource(R.string.onboarding_self_need),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = { if (step > 0) step -= 1 },
                enabled = step > 0,
            ) { Text(stringResource(R.string.onboarding_back)) }
            if (fromSettings) {
                TextButton(onClick = onCancelSetup) { Text(stringResource(R.string.cd_back)) }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text(stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    Bullet(stringResource(R.string.onboarding_welcome_p1))
    Bullet(stringResource(R.string.onboarding_welcome_p2))
    Bullet(stringResource(R.string.onboarding_welcome_p3))
}

@Composable
private fun SelfMessageStep(value: String, onChange: (String) -> Unit) {
    Text(stringResource(R.string.onboarding_self_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_self_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    SelfMessageEditor(
        value = value,
        onChange = onChange,
        showPresetBodies = true,
        minLines = 2,
    )
}

@Composable
private fun HowStep() {
    Text(stringResource(R.string.onboarding_how_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    Bullet("1. " + stringResource(R.string.onboarding_how_1))
    Bullet("2. " + stringResource(R.string.onboarding_how_2))
    Bullet("3. " + stringResource(R.string.onboarding_how_3))
    Bullet("4. " + stringResource(R.string.onboarding_how_4))
}

@Composable
private fun A11yStep(enabled: Boolean) {
    val context = LocalContext.current
    Text(stringResource(R.string.onboarding_a11y_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_a11y_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    StatusLine(enabled, stringResource(R.string.onboarding_a11y_on), stringResource(R.string.onboarding_a11y_off))
    Spacer(Modifier.height(12.dp))
    PeaceButton(
        onClick = { Permissions.openAccessibilitySettings(context) },
        modifier = Modifier.fillMaxWidth(),
        outlined = true,
    ) { Text(stringResource(R.string.onboarding_a11y_action)) }
}

@Composable
private fun BatteryNotifStep(
    battery: Boolean,
    notif: Boolean,
    notifLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    val context = LocalContext.current
    Text(stringResource(R.string.onboarding_battery_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_battery_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    StatusLine(battery, stringResource(R.string.onboarding_battery_on), stringResource(R.string.onboarding_battery_off))
    Spacer(Modifier.height(12.dp))
    PeaceButton(
        onClick = { Permissions.openBatteryOptimization(context) },
        modifier = Modifier.fillMaxWidth(),
        outlined = true,
    ) { Text(stringResource(R.string.onboarding_battery_action)) }
    Spacer(Modifier.height(28.dp))
    Text(stringResource(R.string.onboarding_notif_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_notif_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    StatusLine(notif, stringResource(R.string.onboarding_notif_on), stringResource(R.string.onboarding_notif_off))
    Spacer(Modifier.height(12.dp))
    if (notif) {
        PeaceButton(
            onClick = { Permissions.openNotificationSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            outlined = true,
        ) { Text(stringResource(R.string.onboarding_notif_action)) }
    } else {
        PeaceButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Permissions.openNotificationSettings(context)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.onboarding_notif_action)) }
    }
}

@Composable
private fun LockStep(checked: Boolean, onChecked: (Boolean) -> Unit) {
    Text(stringResource(R.string.onboarding_lock_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_lock_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Bullet(stringResource(R.string.onboarding_lock_xiaomi))
    Bullet(stringResource(R.string.onboarding_lock_huawei))
    Bullet(stringResource(R.string.onboarding_lock_oppo))
    Bullet(stringResource(R.string.onboarding_lock_vivo))
    Bullet(stringResource(R.string.onboarding_lock_pixel))
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChecked)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = stringResource(R.string.onboarding_lock_check),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    Spacer(Modifier.height(28.dp))
    Text(stringResource(R.string.onboarding_privacy_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_privacy_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Bullet(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 10.dp),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun StatusLine(ok: Boolean, on: String, off: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Text(
            text = if (ok) on else off,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
