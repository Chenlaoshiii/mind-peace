package com.mindpeace.app.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import com.mindpeace.app.ui.theme.PeaceButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.data.OverlayState
import com.mindpeace.app.ui.theme.MindPeaceThemed
import com.mindpeace.app.util.AppLocale
import java.lang.ref.WeakReference

class TimeUpActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = WeakReference(this)
        enableEdgeToEdge()
        val coordinator = (application as MindPeaceApp).container.session
        val fallbackLabel = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        setContent {
            val state by coordinator.overlay.collectAsStateWithLifecycle()
            val label = (state as? OverlayState.TimeUp)?.appLabel
                ?: fallbackLabel.ifBlank { getString(R.string.app_name) }
            MindPeaceThemed {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.overlay_timeup_title),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = stringResource(R.string.overlay_timeup_body, label),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                        PeaceButton(
                            onClick = {
                                coordinator.onTimeUpAck()
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.overlay_timeup_ok))
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (instance?.get() === this) instance = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_LABEL = "label"
        private var instance: WeakReference<TimeUpActivity>? = null

        fun close(@Suppress("UNUSED_PARAMETER") context: Context) {
            instance?.get()?.finish()
        }
    }
}
