package com.mindpeace.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindpeace.app.ui.MindPeaceRoot
import com.mindpeace.app.ui.theme.MindPeaceThemed

class MainActivity : ComponentActivity() {

    private var pendingDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDestination = destinationFrom(intent)
        enableEdgeToEdge()
        setContent {
            MindPeaceThemed {
                MindPeaceRoot(
                    pendingDestination = pendingDestination,
                    onPendingConsumed = { pendingDestination = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = destinationFrom(intent)
    }

    private fun destinationFrom(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.getStringExtra(EXTRA_OPEN) == OPEN_STATS) return "stats"
        if (intent.data?.scheme == "mindpeace" && intent.data?.host == "stats") return "stats"
        return null
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_STATS = "stats"
    }
}
