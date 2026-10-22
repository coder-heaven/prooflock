package app.prooflock

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

internal class HabitBlockerService : AccessibilityService() {
    companion object {
        @Volatile private var instance: HabitBlockerService? = null
        fun requestCheck() = instance?.checkForeground()
    }

    override fun onServiceConnected() { instance = this }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            checkForeground(event.packageName?.toString())
        }
    }

    private fun checkForeground(packageName: String? = null) {
        val prefs = getSharedPreferences("prooflock", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("protection_active", false)) return
        val selected = prefs.getStringSet("selected_packages", emptySet()).orEmpty()
        if (packageName.isNullOrBlank() || packageName == this.packageName || packageName !in selected) return
        startActivity(Intent(this, BlockedActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("blocked_package", packageName))
    }

    override fun onInterrupt() = Unit
    override fun onDestroy() { instance = null; super.onDestroy() }
}
