package com.mindpeace.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.data.UsageHistory
import com.mindpeace.app.ui.components.AppIcon
import com.mindpeace.app.ui.theme.PeaceCard
import com.mindpeace.app.ui.theme.PeaceIconButton
import com.mindpeace.app.ui.theme.peaceContainerColor
import com.mindpeace.app.ui.theme.peaceSurfaceColor
import com.mindpeace.app.util.chartDateLabel
import com.mindpeace.app.util.millisToWholeMinutes
import com.mindpeace.app.util.todayDateKey
import com.mindpeace.app.util.yesterdayDateKey

data class AppStat(
    val packageName: String,
    val label: String,
    val todayMin: Int,
    val yesterdayMin: Int,
)

data class DayPoint(
    val dateKey: String,
    val label: String,
    val minutes: Int,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val watched = app.container.settings.watchedApps.collectAsStateWithLifecycle().value
    val history = app.container.settings.usageHistory.collectAsStateWithLifecycle().value
    val today = todayDateKey()
    val yesterday = yesterdayDateKey()
    val todayMap = history.days[today].orEmpty()
    val yestMap = history.days[yesterday]
    val hasYesterday = yestMap != null

    val window = remember(history) { slidingWindow(history) }
    val rows = remember(watched, history) {
        val pkgs = (watched.map { it.packageName } + todayMap.keys + (yestMap?.keys ?: emptySet()))
            .distinct()
        pkgs.map { pkg ->
            AppStat(
                packageName = pkg,
                label = app.container.installedApps.labelOf(pkg),
                todayMin = millisToWholeMinutes(todayMap[pkg] ?: 0L),
                yesterdayMin = millisToWholeMinutes(yestMap?.get(pkg) ?: 0L),
            )
        }.sortedByDescending { it.todayMin + it.yesterdayMin }
    }
    val todayTotal = rows.sumOf { it.todayMin }
    val yestTotal = rows.sumOf { it.yesterdayMin }
    val delta = todayTotal - yestTotal
    val maxBar = (rows.maxOfOrNull { maxOf(it.todayMin, it.yesterdayMin) } ?: 0).coerceAtLeast(1)

    val totalSeries = remember(window, history) {
        window.map { key ->
            DayPoint(
                dateKey = key,
                label = chartDateLabel(key),
                minutes = millisToWholeMinutes(history.days[key]?.values?.sum() ?: 0L),
            )
        }
    }
    val appSeries = remember(window, history, watched) {
        val pkgs = (
            watched.map { it.packageName } +
                window.flatMap { history.days[it]?.keys.orEmpty() }
            ).distinct()
        pkgs.map { pkg ->
            val points = window.map { key ->
                DayPoint(
                    dateKey = key,
                    label = chartDateLabel(key),
                    minutes = millisToWholeMinutes(history.days[key]?.get(pkg) ?: 0L),
                )
            }
            Triple(pkg, app.container.installedApps.labelOf(pkg), points)
        }.filter { triple ->
            watched.any { it.packageName == triple.first } ||
                triple.third.any { it.minutes > 0 }
        }
    }

    Scaffold(
        containerColor = peaceContainerColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                PeaceCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.stats_today_total, todayTotal),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when {
                                !hasYesterday -> stringResource(R.string.stats_no_yesterday)
                                delta < 0 -> stringResource(R.string.stats_delta_less, -delta)
                                delta > 0 -> stringResource(R.string.stats_delta_more, delta)
                                else -> stringResource(R.string.stats_delta_same)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when {
                                !hasYesterday -> stringResource(R.string.stats_warm_first)
                                delta < 0 -> stringResource(R.string.stats_warm_less)
                                delta > 0 -> stringResource(R.string.stats_warm_more)
                                else -> stringResource(R.string.stats_warm_same)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
            if (totalSeries.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.stats_trend),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                item {
                    ChartCard(
                        title = stringResource(R.string.stats_trend_total),
                        points = totalSeries,
                        lineColor = MaterialTheme.colorScheme.primary,
                    )
                }
                items(appSeries, key = { "chart-${it.first}" }) { (pkg, label, points) ->
                    ChartCard(
                        title = label,
                        points = points,
                        lineColor = MaterialTheme.colorScheme.tertiary,
                        leading = { AppIcon(pkg, Modifier.size(28.dp)) },
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LegendDot(MaterialTheme.colorScheme.primary, stringResource(R.string.stats_legend_today))
                    LegendDot(MaterialTheme.colorScheme.tertiary, stringResource(R.string.stats_legend_yesterday))
                }
            }
            if (rows.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.stats_empty),
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(rows, key = { "row-${it.packageName}" }) { row ->
                PeaceCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .animateItem(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(row.packageName, Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(row.label, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "${stringResource(R.string.stats_app_today, row.todayMin)}  ·  ${stringResource(R.string.stats_app_yesterday, row.yesterdayMin)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        PairBars(
                            today = row.todayMin.toFloat(),
                            yesterday = row.yesterdayMin.toFloat(),
                            max = maxBar.toFloat(),
                        )
                    }
                }
            }
        }
    }
}

private fun slidingWindow(history: UsageHistory): List<String> {
    val today = todayDateKey()
    val keys = history.days.keys.filter { it.isNotBlank() }.sorted()
    if (keys.isEmpty()) return listOf(today)
    return keys.filter { it <= today }.takeLast(7)
}

@Composable
private fun ChartCard(
    title: String,
    points: List<DayPoint>,
    lineColor: Color,
    leading: (@Composable () -> Unit)? = null,
) {
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    PeaceCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            MinutesLineChart(
                points = points,
                lineColor = lineColor,
                gridColor = grid,
                labelColor = onVar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
    }
}

@Composable
private fun MinutesLineChart(
    points: List<DayPoint>,
    lineColor: Color,
    gridColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxY = (points.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    val niceMax = when {
        maxY <= 10 -> 10
        maxY <= 30 -> 30
        maxY <= 60 -> 60
        else -> ((maxY + 9) / 10) * 10
    }
    Column(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Canvas(Modifier.fillMaxSize().padding(start = 28.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)) {
                val n = points.size.coerceAtLeast(1)
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f || !w.isFinite() || !h.isFinite()) return@Canvas
                val yTicks = 4
                val span = niceMax.toFloat().coerceAtLeast(1f)
                for (i in 0..yTicks) {
                    val y = h * (1f - i / yTicks.toFloat())
                    if (y.isFinite()) drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                if (points.isEmpty()) return@Canvas
                fun xOf(i: Int): Float {
                    val x = if (n <= 1) w / 2f else w * i / (n - 1).toFloat()
                    return if (x.isFinite()) x else 0f
                }
                fun yOf(v: Int): Float {
                    val y = h * (1f - v / span)
                    return if (y.isFinite()) y.coerceIn(0f, h) else h
                }
                val path = Path()
                var started = false
                points.forEachIndexed { i, p ->
                    val x = xOf(i)
                    val y = yOf(p.minutes)
                    if (!x.isFinite() || !y.isFinite()) return@forEachIndexed
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                if (started) {
                    drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    points.forEachIndexed { i, p ->
                        val x = xOf(i)
                        val y = yOf(p.minutes)
                        if (x.isFinite() && y.isFinite()) {
                            drawCircle(lineColor, radius = 7f, center = Offset(x, y))
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${niceMax}", style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text(stringResource(R.string.stats_y_minutes), style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text("0", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 8.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEach { p ->
                Text(p.label, style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PairBars(today: Float, yesterday: Float, max: Float) {
    val denom = max.coerceAtLeast(1f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BarRow((today / denom).coerceIn(0f, 1f), MaterialTheme.colorScheme.primary)
        BarRow((yesterday / denom).coerceIn(0f, 1f), MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun BarRow(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceAtLeast(0.03f))
                    .background(color, RoundedCornerShape(6.dp)),
            )
        }
    }
}
