package com.mindpeace.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.mindpeace.app.ui.theme.PeaceIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mindpeace.app.R
import com.mindpeace.app.ui.theme.PeaceCard
import com.mindpeace.app.ui.theme.peaceContainerColor
import com.mindpeace.app.ui.theme.peaceSurfaceColor
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.util.Permissions

private data class LabRow(
    val title: String,
    val filled: String,
    val raw: String,
    val celebration: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val celebrationTitle = stringResource(R.string.celebration_notif_title)
    val summaryTitle = stringResource(R.string.summary_notif_title)
    val sampleApp = stringResource(R.string.lab_sample_app)
    val templates = remember { context.resources.getStringArray(R.array.celebration_templates).toList() }
    val celebrationRows = remember(templates, celebrationTitle) {
        templates.map { body ->
            LabRow(title = celebrationTitle, filled = body, raw = body, celebration = true)
        }
    }
    val summaryRows = listOf(
        LabRow(
            title = summaryTitle,
            filled = stringResource(R.string.summary_less, 12, 5),
            raw = stringResource(R.string.summary_less),
            celebration = false,
        ),
        LabRow(
            title = summaryTitle,
            filled = stringResource(R.string.summary_more, 12, 5),
            raw = stringResource(R.string.summary_more),
            celebration = false,
        ),
        LabRow(
            title = summaryTitle,
            filled = stringResource(R.string.summary_same, 12),
            raw = stringResource(R.string.summary_same),
            celebration = false,
        ),
        LabRow(
            title = summaryTitle,
            filled = stringResource(R.string.summary_no_yesterday, 12),
            raw = stringResource(R.string.summary_no_yesterday),
            celebration = false,
        ),
        LabRow(
            title = summaryTitle,
            filled = stringResource(R.string.summary_none),
            raw = stringResource(R.string.summary_none),
            celebration = false,
        ),
        LabRow(
            title = summaryTitle,
            filled = stringResource(R.string.summary_standout_less, sampleApp, 8),
            raw = stringResource(R.string.summary_standout_less),
            celebration = false,
        ),
        LabRow(
            title = summaryTitle,
            filled = stringResource(R.string.summary_standout_more, sampleApp, 8),
            raw = stringResource(R.string.summary_standout_more),
            celebration = false,
        ),
    )

    Scaffold(
        containerColor = peaceContainerColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lab_title)) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.lab_group_celebration),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            itemsIndexed(celebrationRows, key = { i, _ -> "c$i" }) { index, row ->
                LabCard(row) {
                    postPreview(context, row, 2000 + index)
                }
            }
            item {
                Text(
                    text = stringResource(R.string.lab_group_summary),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            itemsIndexed(summaryRows, key = { i, _ -> "s$i" }) { index, row ->
                LabCard(row) {
                    postPreview(context, row, 3000 + index)
                }
            }
        }
    }
}

@Composable
private fun LabCard(row: LabRow, onPreview: () -> Unit) {
    PeaceCard(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(row.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(row.filled, style = MaterialTheme.typography.bodyLarge)
            if (row.raw != row.filled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.lab_raw) + "  " + row.raw,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onPreview) {
                Text(stringResource(R.string.lab_preview))
            }
        }
    }
}

private fun postPreview(context: android.content.Context, row: LabRow, id: Int) {
    Notifications.ensureChannels(context)
    val nm = Permissions.notificationManager(context)
    val n = if (row.celebration) {
        Notifications.celebrationNotification(context, row.filled)
    } else {
        Notifications.summaryNotification(context, row.filled)
    }
    try {
        nm.notify(id, n)
    } catch (_: Exception) {
    }
}
