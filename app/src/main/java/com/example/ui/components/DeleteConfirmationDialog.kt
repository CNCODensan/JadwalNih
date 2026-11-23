package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAtmospherePalette

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Ya, Hapus",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = atmo.sheetBg,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            BouncyButton(
                onClick = {
                    haptics.tapConfirm()
                    onConfirm()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_delete_button"),
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            BouncyTextButton(
                onClick = {
                    haptics.tapSoft()
                    onDismiss()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_delete_button"),
            ) {
                Text("Batal")
            }
        },
        modifier = Modifier.testTag("confirm_delete_dialog"),
    )
}

@Composable
fun ConfirmRecurringDeleteDialog(
    taskTitle: String,
    onDeleteSingle: () -> Unit,
    onDeleteSeries: () -> Unit,
    onDismiss: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = atmo.sheetBg,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            Text(
                text = "Hapus jadwal berulang?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "\"$taskTitle\" punya pengulangan rutin. Kamu mau hapus yang tanggal ini saja atau semua jadwal berulangnya?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                BouncyOutlinedButton(
                    onClick = {
                        haptics.tapConfirm()
                        onDeleteSingle()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("delete_recurring_single_button"),
                ) {
                    Text("Hapus hari ini saja", fontWeight = FontWeight.SemiBold)
                }
                BouncyButton(
                    onClick = {
                        haptics.tapConfirm()
                        onDeleteSeries()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("delete_recurring_series_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus semua pengulangan", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BouncyTextButton(
                    onClick = {
                        haptics.tapSoft()
                        onDismiss()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_recurring_delete_button"),
                ) {
                    Text("Batal")
                }
            }
        },
        modifier = Modifier.testTag("confirm_recurring_delete_dialog"),
    )
}
