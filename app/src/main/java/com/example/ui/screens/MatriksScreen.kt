package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DateTimeHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.TaskWithStatus
import com.example.ui.JadwalNihUiState
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle

@Composable
fun MatriksScreen(
    state: JadwalNihUiState,
    onSelectScope: (ScheduleScope) -> Unit,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(state.matrixTasks) {
        PriorityQuadrant.entries.associateWith { q ->
            state.matrixTasks.filter { it.task.quadrant == q }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "matrix_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Matriks Prioritas",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Susun prioritas biar harimu makin terarah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("matrix_scope_segmented"),
                    ) {
                        ScheduleScope.entries.forEachIndexed { idx, scope ->
                            SegmentedButton(
                                selected = state.matrixScope == scope,
                                onClick = { onSelectScope(scope) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = ScheduleScope.entries.size,
                                ),
                            ) {
                                Text(scope.label)
                            }
                        }
                    }
                }
            }

            if (isWide) {
                item(key = "matrix_grid_row_1") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QuadrantBox(
                            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
                            items = grouped[PriorityQuadrant.Q1_IMPORTANT_URGENT].orEmpty(),
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                            modifier = Modifier.weight(1f),
                        )
                        QuadrantBox(
                            quadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
                            items = grouped[PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT].orEmpty(),
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item(key = "matrix_grid_row_2") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QuadrantBox(
                            quadrant = PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT,
                            items = grouped[PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT].orEmpty(),
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                            modifier = Modifier.weight(1f),
                        )
                        QuadrantBox(
                            quadrant = PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT,
                            items = grouped[PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT].orEmpty(),
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                PriorityQuadrant.entries.forEach { q ->
                    item(key = q.name) {
                        QuadrantBox(
                            quadrant = q,
                            items = grouped[q].orEmpty(),
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantBox(
    quadrant: PriorityQuadrant,
    items: List<TaskWithStatus>,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val (headerBg, headerFg, borderAccent) = when (quadrant) {
        PriorityQuadrant.Q1_IMPORTANT_URGENT -> Triple(palette.redBg, palette.redFg, palette.redAccent)
        PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> Triple(palette.blueBg, palette.blueFg, palette.blueAccent)
        PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> Triple(palette.yellowBg, palette.yellowFg, palette.yellowAccent)
        PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> Triple(palette.grayBg, palette.grayFg, palette.grayAccent)
    }

    FrostedGlassCard(
        modifier = modifier.testTag("matrix_quadrant_${quadrant.shortCode}"),
        cornerRadius = 18.dp,
        borderBrush = Brush.linearGradient(
            colors = listOf(borderAccent.copy(alpha = 0.5f), atmo.glassBorderEnd),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(headerBg, headerBg.copy(alpha = 0.55f)),
                        ),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${quadrant.shortCode} • ${quadrant.titleId}",
                            style = MaterialTheme.typography.titleSmall,
                            color = headerFg,
                        )
                        Text(
                            text = quadrant.subtitleId,
                            style = MaterialTheme.typography.bodySmall,
                            color = headerFg.copy(alpha = 0.85f),
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "${items.size} tugas",
                            style = MonoSmallStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "Masih kosong.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { item ->
                        MatrixTaskRow(
                            item = item,
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixTaskRow(
    item: TaskWithStatus,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
) {
    val task = item.task
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = { onEditTask(task) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete(task) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${DateTimeHelper.formatShortDate(task.dateIso)} ${
                            DateTimeHelper.formatMinuteOfDay(task.startMinuteOfDay)
                        }",
                        style = MonoSmallStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusBadge(status = item.status)
                }
            }

            IconButton(
                onClick = { onMoveToTomorrow(task) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Pindah ke Besok",
                    modifier = Modifier.size(18.dp),
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Pindah kuadran",
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    PriorityQuadrant.entries.filter { it != task.quadrant }.forEach { target ->
                        DropdownMenuItem(
                            text = { Text("Pindah ke ${target.shortCode}: ${target.titleId}") },
                            onClick = {
                                menuExpanded = false
                                onChangeQuadrant(task, target)
                            },
                        )
                    }
                }
            }
        }
    }
}
