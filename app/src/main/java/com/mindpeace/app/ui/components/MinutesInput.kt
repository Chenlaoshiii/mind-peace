package com.mindpeace.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mindpeace.app.R

@Composable
fun MinutesInputField(
    minutes: Int,
    maxMinutes: Int,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var text by remember { mutableStateOf(minutes.toString()) }
    var focused by remember { mutableStateOf(false) }
    var committing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(minutes) {
        if (!focused) text = minutes.toString()
    }

    fun commit() {
        if (committing) return
        committing = true
        try {
            val parsed = text.toIntOrNull()
            if (parsed == null) {
                text = minutes.toString()
            } else {
                val clamped = parsed.coerceIn(0, maxMinutes.coerceAtLeast(0))
                text = clamped.toString()
                onCommit(clamped)
            }
            focusManager.clearFocus()
        } finally {
            committing = false
        }
    }

    Row(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = {},
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { incoming ->
                text = incoming.filter { it.isDigit() }.take(4)
            },
            modifier = Modifier
                .width(148.dp)
                .onFocusChanged { state ->
                    val now = state.isFocused
                    if (focused && !now && !committing) commit()
                    focused = now
                },
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(R.string.budget_minutes_input)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            shape = RoundedCornerShape(16.dp),
        )
        TextButton(onClick = { commit() }, enabled = enabled) {
            Text(stringResource(R.string.overlay_confirm))
        }
    }
}
