package com.mindpeace.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.mindpeace.app.data.InstalledAppsRepository

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawable: Drawable? = remember(packageName) {
        InstalledAppsRepository(context).iconOf(packageName)
    }
    AppIconDrawable(drawable, modifier)
}

@Composable
fun AppIconDrawable(drawable: Drawable?, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        try {
            drawable?.toBitmap()?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    val shape = RoundedCornerShape(12.dp)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}
