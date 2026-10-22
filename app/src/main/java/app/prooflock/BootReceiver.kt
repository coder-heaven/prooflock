package app.prooflock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderScheduler.schedule(context, ProofLockRepository(context).state().reminders)
        HabitBlockerService.requestCheck()
    }
}
