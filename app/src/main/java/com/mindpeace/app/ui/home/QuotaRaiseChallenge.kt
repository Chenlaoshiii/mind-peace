package com.mindpeace.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mindpeace.app.R

data class QuotaRaisePending(
    val appLabel: String,
    val apply: () -> Unit,
    val revert: () -> Unit,
)

fun needsQuotaRaiseChallenge(
    oldLimitMinutes: Int,
    newLimitMinutes: Int,
    usedTodayMillis: Long,
): Boolean {
    if (oldLimitMinutes <= 0) return false
    if (newLimitMinutes <= oldLimitMinutes) return false
    return usedTodayMillis + 1_000L >= oldLimitMinutes * 60_000L
}

fun requestQuotaLimitChange(
    oldMinutes: Int,
    newMinutes: Int,
    usedTodayMillis: Long,
    appLabel: String,
    apply: () -> Unit,
    revert: () -> Unit,
    startChallenge: (QuotaRaisePending) -> Unit,
) {
    if (newMinutes == oldMinutes) return
    if (needsQuotaRaiseChallenge(oldMinutes, newMinutes, usedTodayMillis)) {
        startChallenge(QuotaRaisePending(appLabel = appLabel, apply = apply, revert = revert))
    } else {
        apply()
    }
}

@Composable
fun QuotaRaiseChallenge(
    pending: QuotaRaisePending?,
    onClear: () -> Unit,
) {
    if (pending == null) return
    var step by remember(pending) { mutableStateOf(1) }
    var typed by remember(pending) { mutableStateOf("") }
    val phrase = stringResource(R.string.quota_raise_phrase)
    val matches = typed.trim() == phrase

    fun dismiss() {
        pending.revert()
        onClear()
    }

    if (step == 1) {
        AlertDialog(
            onDismissRequest = ::dismiss,
            title = { Text(stringResource(R.string.quota_raise_title)) },
            text = {
                Text(stringResource(R.string.quota_raise_body, pending.appLabel))
            },
            confirmButton = {
                TextButton(onClick = { step = 2 }) {
                    Text(stringResource(R.string.quota_raise_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = ::dismiss) {
                    Text(stringResource(R.string.quota_raise_back))
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = ::dismiss,
            title = { Text(stringResource(R.string.quota_raise_type_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.quota_raise_type_body))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            phrase,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        isError = typed.isNotBlank() && !matches,
                        supportingText = if (typed.isNotBlank() && !matches) {
                            { Text(stringResource(R.string.quota_raise_mismatch)) }
                        } else {
                            null
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending.apply()
                        onClear()
                    },
                    enabled = matches,
                ) {
                    Text(stringResource(R.string.quota_raise_type_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = ::dismiss) {
                    Text(stringResource(R.string.quota_raise_back))
                }
            },
        )
    }
}
