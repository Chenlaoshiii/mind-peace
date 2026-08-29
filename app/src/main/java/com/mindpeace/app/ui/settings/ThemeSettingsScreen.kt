package com.mindpeace.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.data.ColorMode
import com.mindpeace.app.data.VisualStyle
import com.mindpeace.app.ui.theme.PeaceIconButton
import com.mindpeace.app.ui.theme.PeaceListGroup
import com.mindpeace.app.ui.theme.PeaceListRow
import com.mindpeace.app.ui.theme.peaceContainerColor
import com.mindpeace.app.ui.theme.peaceSurfaceColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val appearance by app.container.settings.appearance.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val onLabel = stringResource(R.string.settings_status_on)

    Scaffold(
        containerColor = peaceContainerColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_appearance)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.settings_style),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            PeaceListGroup {
                PeaceListRow(
                    title = stringResource(R.string.settings_style_material),
                    subtitle = stringResource(R.string.settings_style_material_sub),
                    trailing = if (appearance.style == VisualStyle.MATERIAL_YOU) onLabel else null,
                    trailingHighlight = appearance.style == VisualStyle.MATERIAL_YOU,
                    onClick = { scope.launch { app.container.settings.setVisualStyle(VisualStyle.MATERIAL_YOU) } },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_style_white),
                    subtitle = stringResource(R.string.settings_style_white_sub),
                    trailing = if (appearance.style == VisualStyle.WHITE) onLabel else null,
                    trailingHighlight = appearance.style == VisualStyle.WHITE,
                    showDivider = false,
                    onClick = { scope.launch { app.container.settings.setVisualStyle(VisualStyle.WHITE) } },
                )
            }
            Text(
                text = stringResource(R.string.settings_color_mode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            PeaceListGroup {
                PeaceListRow(
                    title = stringResource(R.string.settings_color_light),
                    trailing = if (appearance.colorMode == ColorMode.LIGHT) onLabel else null,
                    trailingHighlight = appearance.colorMode == ColorMode.LIGHT,
                    onClick = { scope.launch { app.container.settings.setColorMode(ColorMode.LIGHT) } },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_color_dark),
                    trailing = if (appearance.colorMode == ColorMode.DARK) onLabel else null,
                    trailingHighlight = appearance.colorMode == ColorMode.DARK,
                    onClick = { scope.launch { app.container.settings.setColorMode(ColorMode.DARK) } },
                )
                PeaceListRow(
                    title = stringResource(R.string.settings_color_system),
                    trailing = if (appearance.colorMode == ColorMode.SYSTEM) onLabel else null,
                    trailingHighlight = appearance.colorMode == ColorMode.SYSTEM,
                    showDivider = false,
                    onClick = { scope.launch { app.container.settings.setColorMode(ColorMode.SYSTEM) } },
                )
            }
        }
    }
}
