package com.mindpeace.app.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.mindpeace.app.data.VisualStyle

val AppleCapsule = RoundedCornerShape(16.dp)
val AppleGroupShape = RoundedCornerShape(12.dp)
val AppleChipShape = RoundedCornerShape(16.dp)

data class PeaceTab(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun peaceCardShape(): RoundedCornerShape = RoundedCornerShape(20.dp)

@Composable
fun Modifier.peaceGlass(shape: Shape = RoundedCornerShape(28.dp)): Modifier {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val dark = LocalDarkTheme.current
    return appleLiquidGlass(shape = shape, dark = dark, enabled = apple, pressed = false)
}

@Composable
fun Modifier.peaceGlass(shape: Shape, dark: Boolean, enabled: Boolean): Modifier {
    return appleLiquidGlass(shape = shape, dark = dark, enabled = enabled, pressed = false)
}

fun Modifier.frostedFallback(
    shape: Shape,
    dark: Boolean,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    val fill = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.28f)
    val rim = if (dark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.55f)
    val highlight = if (dark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.40f)
    return this
        .clip(shape)
        .background(fill, shape)
        .border(1.dp, rim, shape)
        .drawWithContent {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(highlight, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.42f,
                ),
            )
            drawContent()
        }
}

@Composable
fun Modifier.appleLiquidGlass(
    shape: Shape,
    dark: Boolean,
    enabled: Boolean,
    pressed: Boolean,
): Modifier {
    if (!enabled) return this
    val backdrop = LocalPeaceBackdrop.current
    if (backdrop == null) {
        return frostedFallback(shape, dark, true)
    }
    val surface = if (dark) {
        Color.White.copy(alpha = if (pressed) 0.20f else 0.08f)
    } else {
        Color.White.copy(alpha = if (pressed) 0.36f else 0.16f)
    }
    val highlightAlpha = if (pressed) 1f else 0.72f
    val intensity = if (pressed) 0.88f else 0.52f
    val scale = if (pressed) 1.045f else 1f
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            val lensBoost = if (pressed) 1.25f else 1f
            vibrancy()
            blur(3.5f.dp.toPx())
            lens(
                12f.dp.toPx() * lensBoost,
                26f.dp.toPx() * lensBoost,
                depthEffect = true,
                chromaticAberration = true,
            )
        },
        highlight = {
            Highlight(
                alpha = highlightAlpha,
                style = HighlightStyle.Default(intensity = intensity, angle = 48f, falloff = 1.1f),
            )
        },
        layerBlock = {
            scaleX = scale
            scaleY = scale
        },
        onDrawSurface = { drawRect(surface) },
    )
}

@Composable
fun peaceSliderColors(): SliderColors {
    val dark = LocalDarkTheme.current
    val scheme = MaterialTheme.colorScheme
    val inactive = if (dark) {
        scheme.onPrimaryContainer.copy(alpha = 0.40f)
    } else {
        scheme.onSurface.copy(alpha = 0.24f)
    }
    return SliderDefaults.colors(
        thumbColor = scheme.primary,
        activeTrackColor = scheme.primary,
        inactiveTrackColor = inactive,
        disabledThumbColor = scheme.onSurface.copy(alpha = 0.38f),
        disabledActiveTrackColor = scheme.onSurface.copy(alpha = 0.38f),
        disabledInactiveTrackColor = scheme.onSurface.copy(alpha = 0.12f),
        activeTickColor = androidx.compose.ui.graphics.Color.Transparent,
        inactiveTickColor = androidx.compose.ui.graphics.Color.Transparent,
        disabledActiveTickColor = androidx.compose.ui.graphics.Color.Transparent,
        disabledInactiveTickColor = androidx.compose.ui.graphics.Color.Transparent,
    )
}

@Composable
fun PeaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = peaceCardShape(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val dark = LocalDarkTheme.current
    val colors = CardDefaults.cardColors(
        containerColor = if (apple) Color.Transparent else containerColor,
    )
    val elevationValues = CardDefaults.cardElevation(defaultElevation = if (apple) 0.dp else elevation)
    val cardMod = modifier
        .clip(shape)
        .appleLiquidGlass(shape, dark, apple, pressed = false)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardMod,
            colors = colors,
            elevation = elevationValues,
            shape = shape,
            content = content,
        )
    } else {
        Card(
            modifier = cardMod,
            colors = colors,
            elevation = elevationValues,
            shape = shape,
            content = content,
        )
    }
}

@Composable
fun AppleAtmosphere(modifier: Modifier = Modifier, dark: Boolean) {
    val base = if (dark) AppleGroupedDark else AppleGrouped
    val wash = if (dark) AppleGreenDark.copy(alpha = 0.16f) else AppleGreen.copy(alpha = 0.10f)
    val soft = if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.55f)
    Box(modifier.fillMaxSize().background(base)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || !w.isFinite() || !h.isFinite()) return@Canvas
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(wash, Color.Transparent),
                    startY = 0f,
                    endY = h * 0.42f,
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(soft, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.18f),
                    radius = w.coerceAtLeast(h) * 0.55f,
                ),
            )
        }
    }
}

@Composable
fun PeaceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    if (apple) {
        val dark = LocalDarkTheme.current
        val corner by animateDpAsState(if (pressed) 12.dp else 16.dp, label = "btn-corner")
        val shape = RoundedCornerShape(corner)
        val tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f)
        Box(
            modifier = modifier
                .defaultMinSize(minHeight = 44.dp)
                .appleLiquidGlass(shape, dark, true, pressed = pressed)
                .clip(shape)
                .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(color = tint)) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content,
                    )
                }
            }
        }
    } else {
        val shape = AppleCapsule
        if (outlined) {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interaction,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                content = content,
            )
        } else {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interaction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeaceChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val dark = LocalDarkTheme.current
    if (!apple) {
        val scheme = MaterialTheme.colorScheme
        val unselectedBorder = if (dark) {
            scheme.onSurface.copy(alpha = 0.55f)
        } else {
            scheme.outline
        }
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = label,
            modifier = modifier,
            interactionSource = interaction,
            shape = AppleChipShape,
            colors = FilterChipDefaults.filterChipColors(),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = unselectedBorder,
                selectedBorderColor = scheme.outline,
            ),
        )
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurface
    val tint = if (selected) primary else idle
    val extraBorder = if (selected) {
        Modifier.border(1.dp, primary.copy(alpha = 0.7f), AppleChipShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .appleLiquidGlass(AppleChipShape, dark, true, pressed = pressed)
            .then(extraBorder)
            .clip(AppleChipShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(color = tint)) {
                label()
            }
        }
    }
}

@Composable
fun PeaceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val dark = LocalDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = if (apple) {
            modifier
                .size(40.dp)
                .appleLiquidGlass(CircleShape, dark, true, pressed = pressed)
        } else {
            modifier
        },
        content = content,
    )
}

@Composable
fun PeaceFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    if (!apple) {
        FloatingActionButton(onClick = onClick, modifier = modifier, interactionSource = interaction, content = { content() })
        return
    }
    val dark = LocalDarkTheme.current
    val tint = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(48.dp)
            .appleLiquidGlass(CircleShape, dark, true, pressed = pressed)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) { content() }
    }
}

@Composable
fun PeaceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val dark = LocalDarkTheme.current
    val style = MaterialTheme.typography.bodyLarge
    if (!apple) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            leadingIcon = leadingIcon,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(16.dp),
        )
        return
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .appleLiquidGlass(RoundedCornerShape(16.dp), dark, true, pressed = false)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leadingIcon != null) leadingIcon()
        Box(Modifier.weight(1f)) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = style.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PeaceBottomBar(
    tabs: List<PeaceTab>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    if (!apple) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = tab.key == selectedKey,
                    onClick = { onSelect(tab.key) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        return
    }
    val dark = LocalDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(52.dp)
            .appleLiquidGlass(RoundedCornerShape(26.dp), dark, true, pressed = false)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab.key == selectedKey
            val tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(tab.key) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                )
            }
        }
    }
}

@Composable
fun PeaceListGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val orange = LocalVisualStyle.current == VisualStyle.ORANGE
    val dark = LocalDarkTheme.current
    val shape = when {
        apple -> AppleGroupShape
        else -> RoundedCornerShape(16.dp)
    }
    val bg = when {
        apple -> Color.Transparent
        orange -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val border = if (apple) {
        Modifier
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(if (apple) Modifier.appleLiquidGlass(shape, dark, true, pressed = false) else Modifier)
            .clip(shape)
            .background(bg, shape)
            .then(border)
            .fillMaxWidth(),
        content = content,
    )
}

@Composable
fun PeaceListRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: String? = null,
    trailingHighlight: Boolean = false,
    showDivider: Boolean = true,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (subtitle != null) 72.dp else 56.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (trailingHighlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (apple) 16.dp else 0.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
        }
    }
}
