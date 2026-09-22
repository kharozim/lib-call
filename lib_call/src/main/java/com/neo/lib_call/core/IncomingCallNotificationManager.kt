package com.neo.lib_call.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.neo.lib_call.model.CallRequest
import com.neo.lib_call.ui.CallActivity

internal object IncomingCallNotificationManager {
  internal const val ACTION_ANSWER = "com.neo.lib_call.action.ANSWER_INCOMING_CALL"
  internal const val ACTION_REJECT = "com.neo.lib_call.action.REJECT_INCOMING_CALL"

  private const val CHANNEL_ID = "incoming_calls"
  private const val NOTIFICATION_ID = 1001
  private const val EXTRA_CALLER = "com.neo.lib_call.extra.CALLER"

  fun show(caller: String) {
    val context = ContextProvider.requireContext()
    createChannel(context)

    val answerIntent = actionIntent(context, ACTION_ANSWER, caller)
    val rejectIntent = actionIntent(context, ACTION_REJECT, caller)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.sym_call_incoming)
      .setContentTitle("Incoming call")
      .setContentText(caller)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(false)
      .setOngoing(true)
      .addAction(android.R.drawable.ic_menu_call, "Answer", answerIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectIntent)
      .build()

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
  }

  fun dismiss() {
    NotificationManagerCompat.from(ContextProvider.requireContext()).cancel(NOTIFICATION_ID)
  }

  private fun actionIntent(context: Context, action: String, caller: String): PendingIntent {
    val intent = Intent(context, IncomingCallActionReceiver::class.java).apply {
      this.action = action
      putExtra(EXTRA_CALLER, caller)
    }
    return PendingIntent.getBroadcast(
      context,
      action.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Incoming calls",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "Notifications for incoming SIP calls"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }
}

internal class IncomingCallActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      IncomingCallNotificationManager.ACTION_ANSWER -> {
        val caller = intent.getStringExtra("com.neo.lib_call.extra.CALLER").orEmpty()
        val callIntent = CallActivity.createIncomingIntent(
          context,
          CallRequest(caller, caller, null, emptyMap()),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        context.startActivity(callIntent)
      }

      IncomingCallNotificationManager.ACTION_REJECT -> LinphoneManager.rejectIncomingCall()
    }
  }
}
