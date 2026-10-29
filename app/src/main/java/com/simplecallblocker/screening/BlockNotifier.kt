package com.simplecallblocker.screening

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.simplecallblocker.MainActivity
import com.simplecallblocker.R
import com.simplecallblocker.domain.BlockReason

/** Notificação discreta de bloqueio. Uma só, substituída a cada nova chamada bloqueada. */
class BlockNotifier(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun notifyBlocked(displayNumber: String?, reason: BlockReason) {
        if (!manager.areNotificationsEnabled()) return
        ensureChannel()

        val number = displayNumber ?: context.getString(R.string.hidden_number)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_blocked)
            .setContentTitle(context.getString(R.string.notification_blocked_title, number))
            .setContentText(context.getString(reason.labelRes()))
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_blocked),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "blocked_calls"
        const val NOTIFICATION_ID = 1
    }
}

@StringRes
fun BlockReason.labelRes(): Int = when (this) {
    BlockReason.RULE_EXACT -> R.string.reason_rule_exact
    BlockReason.RULE_PREFIX -> R.string.reason_rule_prefix
    BlockReason.HIDDEN -> R.string.reason_hidden
    BlockReason.UNKNOWN -> R.string.reason_unknown
}
