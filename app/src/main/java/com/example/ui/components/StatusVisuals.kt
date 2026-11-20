package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.PriorityQuadrant
import com.example.domain.TaskStatus
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.SemanticStatusPalette

data class StatusColors(
    val bg: Color,
    val fg: Color,
    val accent: Color,
)

fun SemanticStatusPalette.colorsFor(status: TaskStatus): StatusColors = when (status) {
    TaskStatus.COMPLETED_GREEN -> StatusColors(greenBg, greenFg, greenAccent)
    TaskStatus.APPROACHING_YELLOW -> StatusColors(yellowBg, yellowFg, yellowAccent)
    TaskStatus.CRITICAL_RED -> StatusColors(redBg, redFg, redAccent)
    TaskStatus.PENDING_GRAY -> StatusColors(grayBg, grayFg, grayAccent)
    TaskStatus.OVERDUE_DARK -> StatusColors(darkBg, darkFg, darkAccent)
    TaskStatus.FLEXIBLE_BLUE -> StatusColors(blueBg, blueFg, blueAccent)
}

@Composable
fun StatusBadge(
    status: TaskStatus,
    modifier: Modifier = Modifier,
) {
    val palette = LocalStatusPalette.current
    val colors = palette.colorsFor(status)
    Surface(
        modifier = modifier,
        color = colors.bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
            Text(
                text = status.labelId,
                style = MaterialTheme.typography.labelSmall,
                color = colors.fg,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun QuadrantChip(
    quadrant: PriorityQuadrant,
    modifier: Modifier = Modifier,
) {
    val palette = LocalStatusPalette.current
    val (bg, fg) = when (quadrant) {
        PriorityQuadrant.Q1_IMPORTANT_URGENT -> palette.redBg to palette.redFg
        PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> palette.blueBg to palette.blueFg
        PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> palette.yellowBg to palette.yellowFg
        PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> palette.grayBg to palette.grayFg
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = "${quadrant.shortCode} • ${quadrant.chipLabelId}",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}
