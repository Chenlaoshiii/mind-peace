package com.mindpeace.app.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindpeace.app.data.VisualStyle

val AppleCapsule = RoundedCornerShape(16.dp)
val AppleGroupShape = RoundedCornerShape(12.dp)
val AppleChipShape = RoundedCornerShape(18.dp)

/**
 * Apple-style glass. Kyant `io.github.kyant0:backdrop` 2.0.1 needs Kotlin 2.4 / CMP 1.12,
 * and 1.0.x needs Kotlin 2.2 / Compose 1.9 — both newer than this project's Kotlin 2.0.21
 * and Compose BOM 2024.12.01. Fallback: RenderEffect blur on a subtle blue wash, plus
 * translucent fill and a thin specular rim (API < 31: translucent + border only).
 */
fun Modifier.peaceGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    dark: Boolean,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    val fill = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.58f)
    val rim = if (dark) Color.White.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.78f)
    val highlight = if (dark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.48f)
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
fun Modifier.peaceGlass(shape: Shape = RoundedCornerShape(28.dp)): Modifier {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val dark = LocalDarkTheme.current
    return peaceGlass(shape, dark, apple)
}

@Composable
fun PeaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val dark = LocalDarkTheme.current
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier
            .peaceGlass(shape, dark, apple)
            .then(clickMod),
        colors = CardDefaults.cardColors(
            containerColor = if (apple) Color.Transparent else containerColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (apple) 0.dp else elevation),
        shape = shape,
    ) {
        Column { content() }
    }
}

@Composable
fun AppleAtmosphere(modifier: Modifier = Modifier, dark: Boolean) {
    val blurMod = if (Build.VERSION.SDK_INT >= 31) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        Modifier
    }
    val base = if (dark) AppleGroupedDark else AppleGrouped
    val wash = if (dark) AppleBlueDark.copy(alpha = 0.10f) else AppleBlue.copy(alpha = 0.07f)
    Box(modifier.fillMaxSize().background(base)) {
        Canvas(Modifier.fillMaxSize().then(blurMod)) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || !w.isFinite() || !h.isFinite()) return@Canvas
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(wash, Color.Transparent),
                    startY = 0f,
                    endY = h * 0.38f,
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
    val shape = AppleCapsule
    if (apple) {
        val dark = LocalDarkTheme.current
        val tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f)
        Box(
            modifier = modifier
                .defaultMinSize(minHeight = 50.dp)
                .peaceGlass(shape, dark, true)
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
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
    } else if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            content = content,
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            content = content,
        )
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
    if (!apple) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = label,
            modifier = modifier,
        )
        return
    }
    val dark = LocalDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurface
    val tint = if (selected) primary else idle
    val extraBorder = if (selected) {
        Modifier.border(1.5.dp, primary.copy(alpha = 0.75f), AppleChipShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .peaceGlass(AppleChipShape, dark, true)
            .then(extraBorder)
            .clip(AppleChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
    IconButton(
        onClick = onClick,
        modifier = if (apple) modifier.peaceGlass(CircleShape, dark, true) else modifier,
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
    if (!apple) {
        FloatingActionButton(onClick = onClick, modifier = modifier, content = { content() })
        return
    }
    val dark = LocalDarkTheme.current
    val tint = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(56.dp)
            .peaceGlass(CircleShape, dark, true)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) { content() }
    }
}

@Composable
fun PeaceListGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val apple = LocalVisualStyle.current == VisualStyle.APPLE
    val orange = LocalVisualStyle.current == VisualStyle.ORANGE
    val shape = if (apple) AppleGroupShape else RoundedCornerShape(16.dp)
    val bg = when {
        apple -> if (LocalDarkTheme.current) AppleTableDark else AppleTable
        orange -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val border = if (apple) {
        Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), shape)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(title) },
            supportingContent = if (subtitle != null) {
                { Text(subtitle) }
            } else {
                null
            },
            trailingContent = if (trailing != null) {
                {
                    Text(
                        text = trailing,
                        color = if (trailingHighlight) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            modifier = Modifier.clickable(onClick = onClick),
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (apple) 16.dp else 0.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
        }
    }
}
