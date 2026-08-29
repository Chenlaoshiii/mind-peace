package com.mindpeace.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.mindpeace.app.ui.theme.PeaceChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mindpeace.app.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelfMessageEditor(
    value: String,
    onChange: (String) -> Unit,
    showPresetBodies: Boolean = false,
    minLines: Int = 2,
) {
    val p1 = stringResource(R.string.onboarding_self_preset_1)
    val p2 = stringResource(R.string.onboarding_self_preset_2)
    val p3 = stringResource(R.string.onboarding_self_preset_3)
    var custom by remember {
        mutableStateOf(value.isNotBlank() && value != p1 && value != p2 && value != p3)
    }
    LaunchedEffect(p1, p2, p3) {
        if (value.isNotBlank() && value != p1 && value != p2 && value != p3) custom = true
    }
    val selectedPreset = when {
        custom -> -1
        value == p1 -> 1
        value == p2 -> 2
        value == p3 -> 3
        else -> 0
    }
    Column(Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PeaceChip(
                selected = selectedPreset == 1,
                onClick = {
                    custom = false
                    onChange(p1)
                },
                label = { Text("1") },
            )
            PeaceChip(
                selected = selectedPreset == 2,
                onClick = {
                    custom = false
                    onChange(p2)
                },
                label = { Text("2") },
            )
            PeaceChip(
                selected = selectedPreset == 3,
                onClick = {
                    custom = false
                    onChange(p3)
                },
                label = { Text("3") },
            )
            PeaceChip(
                selected = custom,
                onClick = {
                    custom = true
                    onChange("")
                },
                label = { Text(stringResource(R.string.onboarding_self_custom)) },
            )
        }
        if (showPresetBodies) {
            Spacer(Modifier.height(8.dp))
            Text(p1)
            Spacer(Modifier.height(6.dp))
            Text(p2)
            Spacer(Modifier.height(6.dp))
            Text(p3)
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = {
                custom = it != p1 && it != p2 && it != p3
                onChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (custom) stringResource(R.string.onboarding_self_custom_hint)
                    else stringResource(R.string.onboarding_self_hint),
                )
            },
            minLines = minLines,
        )
    }
}
