package app.prooflock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

internal object ReminderScheduler {
    private const val REQUEST = 4101

    fun schedule(context: Context, settings: ReminderSettings) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(pending)
        if (!settings.enabled) return
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.hour); set(Calendar.MINUTE, settings.minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }
}
