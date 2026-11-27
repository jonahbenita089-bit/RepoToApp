package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CodeBlockBackground
import com.example.ui.theme.CodeBlockText
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.util.Locale

@Composable
fun PlatformPill(
    label: String,
    isAndroid: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val accent = if (isAndroid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    val container = if (isAndroid) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val onContainer = if (isAndroid) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) container else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (isAndroid) Icons.Rounded.Android else Icons.Rounded.PhoneIphone,
                contentDescription = null,
                tint = if (enabled) onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MonoBadgeStyle,
                color = if (enabled) onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RunStatusPill(
    status: String?,
    conclusion: String?,
    modifier: Modifier = Modifier,
) {
    val normalizedStatus = status?.lowercase(Locale.ROOT).orEmpty()
    val normalizedConclusion = conclusion?.lowercase(Locale.ROOT).orEmpty()

    val (label, color, icon) = when {
        normalizedStatus == "queued" || normalizedStatus == "waiting" -> Triple(
            "QUEUED",
            StatusWarning,
            Icons.Rounded.HourglassTop,
        )
        normalizedStatus == "in_progress" -> Triple(
            "BUILDING",
            StatusRunning,
            Icons.Rounded.Sync,
        )
        normalizedConclusion == "success" -> Triple(
            "SUCCESS",
            StatusSuccess,
            Icons.Rounded.CheckCircle,
        )
        normalizedConclusion == "failure" -> Triple(
            "FAILED",
            StatusError,
            Icons.Rounded.ErrorOutline,
        )
        normalizedConclusion == "cancelled" -> Triple(
            "CANCELLED",
            StatusWarning,
            Icons.Rounded.Close,
        )
        else -> Triple(
            normalizedStatus.ifBlank { "UNKNOWN" }.uppercase(Locale.ROOT),
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Rounded.HourglassTop,
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                style = MonoBadgeStyle.copy(fontWeight = FontWeight.SemiBold),
                color = color,
            )
        }
    }
}

@Composable
fun MonospaceCodeBox(
    code: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = CodeBlockBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ".github/workflows/repo2app-build.yml",
                    style = MonoBadgeStyle,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${code.lines().size} lines",
                    style = MonoBadgeStyle,
                    color = Color(0xFF94A3B8),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
            ) {
                Text(
                    text = code,
                    style = MonoCodeStyle,
                    color = CodeBlockText,
                )
            }
        }
    }
}

@Composable
fun EmptySectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                actionContent()
            }
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    trailingText: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MonoBadgeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
