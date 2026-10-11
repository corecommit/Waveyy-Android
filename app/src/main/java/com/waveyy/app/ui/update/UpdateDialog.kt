package com.waveyy.app.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.AppUpdate

/**
 * "New version available" dialog. Shows version + changelog + two actions.
 * When [downloading] is true the body swaps to a progress bar.
 *
 * @param update the available release.
 * @param downloading true while the APK is downloading.
 * @param progress 0f..1f when downloading.
 * @param error transient error, if any.
 * @param onDismiss dismiss handler (marks tag as seen).
 * @param onDownload starts the download + install flow.
 */
@Composable
fun UpdateDialog(
    update: AppUpdate,
    downloading: Boolean,
    progress: Float,
    error: String?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text("New version available") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = update.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                if (downloading) {
                    // progress == 0f can mean "just started" OR "server
                    // omitted Content-Length". Either way, show an
                    // indeterminate-ish spinner state; only render the
                    // progress bar once we have a non-zero value.
                    if (progress > 0f) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (progress > 0f) {
                            "Downloading… ${(progress * 100).toInt()}%"
                        } else {
                            "Downloading… this can take a moment on slow networks."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    MarkdownText(update.changelog.ifBlank { "No changelog provided." })
                }
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload, enabled = !downloading) {
                Text(if (downloading) "Downloading…" else "Download & install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !downloading) {
                Text("Later")
            }
        }
    )
}

/** Minimal markdown renderer for changelog bodies. */
@Composable
private fun MarkdownText(markdown: String) {
    Column {
        markdown.lines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.startsWith("### ") -> Text(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
                line.startsWith("## ") -> Text(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
                line.startsWith("# ") -> Text(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
                line.startsWith("- ") || line.startsWith("* ") -> Text(
                    text = "• ${line.substring(2)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
                line.isBlank() -> Spacer(Modifier.height(6.dp))
                else -> Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
