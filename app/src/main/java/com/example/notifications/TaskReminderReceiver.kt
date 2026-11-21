package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Kegiatan JadwalNih"
        val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL) ?: ""
        val quadrantLabel = intent.getStringExtra(EXTRA_QUADRANT_LABEL) ?: ""
        val isCheckIn = intent.getBooleanExtra(EXTRA_IS_CHECK_IN, false)

        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            taskId.toInt() + if (isCheckIn) 100_000 else 0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notifTitle = if (isCheckIn) {
            "Sedang ikut \"$title\"?"
        } else {
            "Pengingat: $title"
        }

        val bodyText = buildString {
            if (isCheckIn) {
                append("Ketuk untuk konfirmasi kehadiranmu")
                if (timeLabel.isNotBlank()) append(" • $timeLabel")
            } else {
                if (timeLabel.isNotBlank()) append("Waktu: $timeLabel")
                if (quadrantLabel.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(quadrantLabel)
                }
                if (isEmpty()) append("Saatnya bersiap memulai kegiatanmu.")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .build()

        runCatching {
            val notifId = taskId.toInt() + if (isCheckIn) 100_000 else 0
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "jadwalnih_reminders"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TIME_LABEL = "extra_time_label"
        const val EXTRA_QUADRANT_LABEL = "extra_quadrant_label"
        const val EXTRA_IS_CHECK_IN = "extra_is_check_in"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existing = manager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.notification_channel_desc)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }
}
