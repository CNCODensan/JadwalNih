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
import com.example.data.AppDatabase
import com.example.data.NotificationLogEntity
import com.example.domain.NotificationCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Kegiatan JadwalNih"
        val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL) ?: ""
        val quadrantLabel = intent.getStringExtra(EXTRA_QUADRANT_LABEL) ?: ""
        val isCheckIn = intent.getBooleanExtra(EXTRA_IS_CHECK_IN, false)
        val isWorship = intent.getBooleanExtra(EXTRA_IS_WORSHIP, false)
        val worshipSound = intent.getBooleanExtra(EXTRA_WORSHIP_SOUND, true)

        ensureChannel(context)

        val notifTitle = when {
            isWorship -> "Waktu Ibadah: $title"
            isCheckIn -> "Lagi ikut \"$title\"?"
            else -> "Pengingat: $title"
        }

        val bodyText = when {
            isWorship -> buildString {
                if (timeLabel.isNotBlank()) append("Masuk jam $timeLabel")
                if (quadrantLabel.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(quadrantLabel)
                }
                if (isEmpty()) append("Yuk luangkan waktu sebentar untuk ibadah.")
            }
            isCheckIn -> buildString {
                append("Ketuk buat konfirmasi kamu hadir ya")
                if (timeLabel.isNotBlank()) append(" • $timeLabel")
            }
            else -> buildString {
                if (timeLabel.isNotBlank()) append("Jam: $timeLabel")
                if (quadrantLabel.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(quadrantLabel)
                }
                if (isEmpty()) append("Bentar lagi waktunya mulai nih.")
            }
        }

        // Persist fired notification in Room NotificationLog (30-day history)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dao = AppDatabase.getInstance(context.applicationContext).notificationLogDao()
                dao.insertLog(
                    NotificationLogEntity(
                        id = 0L,
                        category = if (isWorship) NotificationCategory.WORSHIP.name else NotificationCategory.TASK.name,
                        title = notifTitle,
                        message = bodyText,
                        createdAtEpochMs = System.currentTimeMillis(),
                        isRead = false,
                        relatedTaskId = if (taskId > 0L) taskId else null,
                    ),
                )
            }
            pendingResult.finish()
        }

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
        val notifId = when {
            isWorship -> 500_000 + title.hashCode().and(0xFFFF)
            isCheckIn -> taskId.toInt() + 100_000
            else -> taskId.toInt()
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = when {
            isWorship && worshipSound -> CHANNEL_WORSHIP_SOUND
            isWorship && !worshipSound -> CHANNEL_WORSHIP_SILENT
            else -> CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notif_calendar)
            .setContentTitle(notifTitle)
            .setContentText(bodyText)
            .setPriority(if (isWorship && !worshipSound) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)

        if (isWorship && !worshipSound) {
            builder.setSilent(true)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    companion object {
        const val CHANNEL_ID = "jadwalnih_reminders"
        const val CHANNEL_WORSHIP_SOUND = "jadwalnih_worship_sound"
        const val CHANNEL_WORSHIP_SILENT = "jadwalnih_worship_silent"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TIME_LABEL = "extra_time_label"
        const val EXTRA_QUADRANT_LABEL = "extra_quadrant_label"
        const val EXTRA_IS_CHECK_IN = "extra_is_check_in"
        const val EXTRA_IS_WORSHIP = "extra_is_worship"
        const val EXTRA_WORSHIP_SOUND = "extra_worship_sound"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.notification_channel_desc)
                    }
                    manager.createNotificationChannel(channel)
                }

                if (manager.getNotificationChannel(CHANNEL_WORSHIP_SOUND) == null) {
                    val soundChannel = NotificationChannel(
                        CHANNEL_WORSHIP_SOUND,
                        context.getString(R.string.notification_worship_sound_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.notification_worship_sound_desc)
                    }
                    manager.createNotificationChannel(soundChannel)
                }

                if (manager.getNotificationChannel(CHANNEL_WORSHIP_SILENT) == null) {
                    val silentChannel = NotificationChannel(
                        CHANNEL_WORSHIP_SILENT,
                        context.getString(R.string.notification_worship_silent_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = context.getString(R.string.notification_worship_silent_desc)
                        setSound(null, null)
                        enableVibration(false)
                    }
                    manager.createNotificationChannel(silentChannel)
                }
            }
        }
    }
}
