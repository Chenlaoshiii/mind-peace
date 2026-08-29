package com.mindpeace.app.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.R
import com.mindpeace.app.data.OverlayState
import com.mindpeace.app.data.UNLIMITED_BUDGET
import com.mindpeace.app.data.VisualStyle
import com.mindpeace.app.session.SessionCoordinator
import com.mindpeace.app.ui.Motion
import com.mindpeace.app.ui.theme.LocalDarkTheme
import com.mindpeace.app.ui.theme.LocalVisualStyle
import com.mindpeace.app.ui.theme.PeaceButton
import com.mindpeace.app.ui.theme.PeaceChip
import com.mindpeace.app.ui.theme.peaceGlass
import com.mindpeace.app.util.formatDurationMillis

private val DURATIONS = listOf(1, 3, 5, 10, 15, 30)
private val CardShape = RoundedCornerShape(28.dp)

@Composable
fun InterceptHost(coordinator: SessionCoordinator) {
    val state by coordinator.overlay.collectAsStateWithLifecycle()
    val liveQuote by coordinator.selfMessage.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        if (state is OverlayState.Hidden) return@Box
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { Motion.paneTransform() },
            label = "overlay-pane",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
        ) { s ->
            when (s) {
                OverlayState.Hidden -> {}
                is OverlayState.Confirm -> ConfirmPane(
                    s,
                    coordinator,
                    s.selfMessage.ifBlank { liveQuote },
                )
                is OverlayState.DurationPick -> DurationPane(s, coordinator)
                is OverlayState.CapWarning -> CapPane(s, coordinator)
                is OverlayState.CustomDuration -> CustomPane(s, coordinator)
                is OverlayState.TimeUp -> TimeUpPane(s, coordinator)
            }
        }
    }
}

@Composable
private fun ConfirmPane(
    state: OverlayState.Confirm,
    coordinator: SessionCoordinator,
    quote: String,
) {
    val budgetGone = state.remainingDailyMillis <= 0L
    OverlayCard(
        title = if (budgetGone) {
            stringResource(R.string.overlay_budget_none)
        } else {
            stringResource(R.string.overlay_question, state.appLabel)
        },
        subtitle = budgetLine(state.remainingDailyMillis),
        extra = if (quote.isNotBlank()) {
            {
                val style = LocalVisualStyle.current
                val appleQuote = style == VisualStyle.APPLE
                val whiteQuote = style == VisualStyle.WHITE
                val darkQuote = LocalDarkTheme.current
                val quoteShape = RoundedCornerShape(if (whiteQuote) 12.dp else 18.dp)
                Surface(
                    modifier = if (appleQuote) Modifier.peaceGlass(quoteShape, darkQuote, true) else Modifier,
                    color = when {
                        appleQuote -> Color.Transparent
                        whiteQuote -> MaterialTheme.colorScheme.surfaceContainerLow
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
                    },
                    shape = quoteShape,
                    tonalElevation = if (appleQuote || whiteQuote) 0.dp else 1.dp,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.overlay_self_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (appleQuote) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = quote,
                            style = MaterialTheme.typography.titleLarge,
                            fontStyle = FontStyle.Italic,
                            color = if (appleQuote) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        } else null,
        bottom = {
            if (budgetGone) {
                PeaceButton(
                    onClick = { coordinator.onExit() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.overlay_exit)) }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PeaceButton(
                        onClick = { coordinator.onExit() },
                        modifier = Modifier.weight(1f),
                        outlined = true,
                    ) { Text(stringResource(R.string.overlay_exit)) }
                    Spacer(Modifier.width(12.dp))
                    PeaceButton(
                        onClick = { coordinator.onConfirm() },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.overlay_confirm)) }
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationPane(state: OverlayState.DurationPick, coordinator: SessionCoordinator) {
    OverlayCard(
        title = stringResource(R.string.overlay_pick_title),
        subtitle = budgetLine(state.remainingDailyMillis),
        extra = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DURATIONS.forEach { minutes ->
                    PeaceChip(
                        selected = false,
                        onClick = { coordinator.onPickDuration(minutes) },
                        label = { Text(stringResource(R.string.overlay_minutes, minutes)) },
                    )
                }
                PeaceChip(
                    selected = false,
                    onClick = { coordinator.onOpenCustom() },
                    label = { Text(stringResource(R.string.overlay_custom)) },
                )
            }
        },
        bottom = {
            PeaceButton(
                onClick = { coordinator.onExit() },
                modifier = Modifier.fillMaxWidth(),
                outlined = true,
            ) { Text(stringResource(R.string.overlay_exit)) }
        },
    )
}

@Composable
private fun CapPane(state: OverlayState.CapWarning, coordinator: SessionCoordinator) {
    OverlayCard(
        title = stringResource(R.string.overlay_cap_title),
        subtitle = stringResource(
            R.string.overlay_cap_body,
            state.appLabel,
            formatDurationMillis(LocalContext.current, state.remainingDailyMillis),
            state.requestedMinutes,
        ),
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PeaceButton(
                    onClick = { coordinator.onAcceptCap() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.overlay_cap_accept)) }
                PeaceButton(
                    onClick = { coordinator.onCancelCap() },
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true,
                ) { Text(stringResource(R.string.overlay_cap_cancel)) }
            }
        },
    )
}

@Composable
private fun CustomPane(state: OverlayState.CustomDuration, coordinator: SessionCoordinator) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull() ?: 0
    OverlayCard(
        title = stringResource(R.string.overlay_custom_title),
        subtitle = budgetLine(state.remainingDailyMillis),
        extra = {
            com.mindpeace.app.ui.theme.PeaceTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() }.take(4) },
                placeholder = stringResource(R.string.overlay_custom_hint),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PeaceButton(
                    onClick = { coordinator.onPickDuration(minutes) },
                    enabled = minutes > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.overlay_custom_ok)) }
                PeaceButton(
                    onClick = { coordinator.onCancelCustom() },
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true,
                ) { Text(stringResource(R.string.overlay_cap_cancel)) }
            }
        },
    )
}

@Composable
private fun TimeUpPane(state: OverlayState.TimeUp, coordinator: SessionCoordinator) {
    OverlayCard(
        title = stringResource(R.string.overlay_timeup_title),
        subtitle = stringResource(R.string.overlay_timeup_body, state.appLabel),
        bottom = {
            PeaceButton(
                onClick = { coordinator.onTimeUpAck() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.overlay_timeup_ok)) }
        },
    )
}

@Composable
private fun OverlayCard(
    title: String,
    subtitle: String,
    extra: (@Composable () -> Unit)? = null,
    bottom: @Composable () -> Unit,
) {
    val style = LocalVisualStyle.current
    val apple = style == VisualStyle.APPLE
    val white = style == VisualStyle.WHITE
    val dark = LocalDarkTheme.current
    val cardShape = if (white) RoundedCornerShape(16.dp) else CardShape
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .peaceGlass(cardShape, dark, apple),
        shape = cardShape,
        tonalElevation = if (apple || white) 0.dp else 6.dp,
        shadowElevation = if (apple || white) 0.dp else 2.dp,
        color = when {
            apple -> Color.Transparent
            white -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (extra != null) {
                Spacer(Modifier.height(20.dp))
                extra()
            }
            Spacer(Modifier.height(24.dp))
            bottom()
        }
    }
}

@Composable
private fun budgetLine(remainingDailyMillis: Long): String {
    val context = LocalContext.current
    return when {
        remainingDailyMillis >= UNLIMITED_BUDGET / 2 -> stringResource(R.string.overlay_budget_unlimited)
        remainingDailyMillis <= 0L -> stringResource(R.string.overlay_budget_none)
        else -> stringResource(R.string.overlay_budget_left, formatDurationMillis(context, remainingDailyMillis))
    }
}
