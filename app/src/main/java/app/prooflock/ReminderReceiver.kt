package app.prooflock

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat

internal class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val repository = ProofLockRepository(context)
        val state = repository.state()
        if (state.allComplete) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("prooflock_reminders", "ProofLock reminders", NotificationManager.IMPORTANCE_HIGH))
        if (android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val pending = android.app.PendingIntent.getActivity(context, 4102, Intent(context, ProofLockActivity::class.java), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        manager.notify(4102, NotificationCompat.Builder(context, "prooflock_reminders")
            .setSmallIcon(app.prooflock.R.drawable.prooflock_icon)
            .setContentTitle("Your promise is still waiting")
            .setContentText("Complete ${state.activeHabits.count { !it.completedToday() }} habit(s) and check in with fresh proof.")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pending).build())
        ReminderScheduler.schedule(context, state.reminders)
    }
}
