package app.prooflock

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal class ProofLockRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("prooflock", Context.MODE_PRIVATE)

    fun state(): ProofLockState {
        val saved = prefs.getString("habits_json", null)
        val habits = if (saved.isNullOrBlank()) HabitTemplates.all.take(3) else decodeHabits(saved)
        val selected = prefs.getStringSet("selected_packages", emptySet()).orEmpty()
        val reminder = ReminderSettings(
            enabled = prefs.getBoolean("reminder_enabled", true),
            hour = prefs.getInt("reminder_hour", 20),
            minute = prefs.getInt("reminder_minute", 0),
            escalationMinutes = prefs.getInt("reminder_escalation", 15),
            maxEscalations = prefs.getInt("reminder_max", 3)
        )
        return ProofLockState(habits, selected, prefs.getBoolean("protection_enabled", true), reminder, BuildConfig.PROOFLOCK_API_URL.isNotBlank())
    }

    fun saveHabits(habits: List<Habit>) {
        prefs.edit().putString("habits_json", encodeHabits(habits)).apply()
        updateProtection(habits)
    }

    fun markComplete(id: String, proofTime: String) {
        val today = LocalDate.now().toString()
        saveHabits(state().habits.map { habit ->
            if (habit.id == id) habit.copy(completedDates = habit.completedDates + today, lastProofTime = proofTime) else habit
        })
    }

    fun setSelectedPackages(packages: Set<String>) = prefs.edit().putStringSet("selected_packages", packages).apply()

    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("protection_enabled", enabled).apply()
        HabitBlockerService.requestCheck()
    }

    fun setReminders(value: ReminderSettings) {
        prefs.edit().putBoolean("reminder_enabled", value.enabled).putInt("reminder_hour", value.hour)
            .putInt("reminder_minute", value.minute).putInt("reminder_escalation", value.escalationMinutes)
            .putInt("reminder_max", value.maxEscalations).apply()
        ReminderScheduler.schedule(appContext, value)
    }

    private fun updateProtection(habits: List<Habit>) {
        val enabled = prefs.getBoolean("protection_enabled", true) && habits.any { it.active && !it.completedToday() }
        prefs.edit().putBoolean("protection_active", enabled).apply()
        HabitBlockerService.requestCheck()
    }

    private fun encodeHabits(habits: List<Habit>): String = JSONArray().apply {
        habits.forEach { habit ->
            put(JSONObject().apply {
                put("id", habit.id); put("title", habit.title); put("subtitle", habit.subtitle)
                put("kind", habit.kind.name); put("proofPrompt", habit.proofPrompt); put("active", habit.active)
                put("proofTime", habit.lastProofTime); put("dates", JSONArray(habit.completedDates.toList()))
            })
        }
    }.toString()

    private fun decodeHabits(raw: String): List<Habit> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val dates = item.optJSONArray("dates") ?: JSONArray()
                add(Habit(
                    id = item.getString("id"), title = item.getString("title"), subtitle = item.getString("subtitle"),
                    kind = HabitKind.valueOf(item.optString("kind", HabitKind.LEARNING.name)),
                    proofPrompt = item.optString("proofPrompt", "Show today's proof"), active = item.optBoolean("active", true),
                    completedDates = buildSet { for (date in 0 until dates.length()) add(dates.getString(date)) },
                    lastProofTime = item.optString("proofTime", "")
                ))
            }
        }
    }.getOrDefault(HabitTemplates.all.take(3))
}
