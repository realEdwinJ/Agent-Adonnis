package com.adonnis.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Small copy-to-clipboard action with a brief "Copied" confirmation.
 * Used on chat bubbles and diary entries/messages.
 */
@Composable
fun CopyButton(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    val clipboard = LocalClipboardManager.current
    var copyCount by remember { mutableStateOf(0) }

    // Keyed on a counter so rapid re-taps restart the confirmation timer.
    LaunchedEffect(copyCount) {
        if (copyCount > 0) {
            delay(1600)
            copyCount = 0
        }
    }

    val showing = copyCount > 0
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (showing) {
            Text(
                text = "Copied",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2ECC71)
            )
        }
        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(text))
                copyCount++
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (showing) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                contentDescription = if (showing) "Copied" else "Copy",
                tint = if (showing) Color(0xFF2ECC71) else tint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
