package app.prooflock

import java.time.LocalDate

internal enum class HabitKind { MOVEMENT, MINDFULNESS, LEARNING, CREATIVE, HOME, ONLINE }

internal data class Habit(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: HabitKind,
    val proofPrompt: String,
    val active: Boolean = true,
    val completedDates: Set<String> = emptySet(),
    val lastProofTime: String = ""
) {
    fun completedToday(today: String = LocalDate.now().toString()): Boolean = today in completedDates
    fun streak(today: LocalDate = LocalDate.now()): Int {
        var count = 0
        var cursor = today
        while (completedDates.contains(cursor.toString())) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}

internal data class AppEntry(
    val packageName: String,
    val label: String,
    val distracting: Boolean,
    val selected: Boolean
)

internal data class ReminderSettings(
    val enabled: Boolean = true,
    val hour: Int = 20,
    val minute: Int = 0,
    val escalationMinutes: Int = 15,
    val maxEscalations: Int = 3
)

internal data class ProofResult(
    val status: String,
    val reason: String,
    val provider: String = "local",
    val checkedAt: String = ""
)

internal data class ProofLockState(
    val habits: List<Habit>,
    val selectedPackages: Set<String>,
    val protectionEnabled: Boolean,
    val reminders: ReminderSettings,
    val backendConfigured: Boolean
) {
    val activeHabits: List<Habit> get() = habits.filter { it.active }
    val completedToday: Int get() = activeHabits.count { it.completedToday() }
    val allComplete: Boolean get() = activeHabits.isNotEmpty() && completedToday == activeHabits.size
}

internal object HabitTemplates {
    val all = listOf(
        Habit("move", "Move for 20 minutes", "Walk, stretch, dance, or train", HabitKind.MOVEMENT, "Show the activity you completed"),
        Habit("water", "Drink water", "Build a simple hydration check-in", HabitKind.MOVEMENT, "Show your water bottle or glass"),
        Habit("read", "Read 10 pages", "Give your attention somewhere good", HabitKind.LEARNING, "Show the book and the page you reached"),
        Habit("study", "Study for 30 minutes", "Make progress before scrolling", HabitKind.LEARNING, "Show your study setup and work"),
        Habit("sketch", "Sketch for 15 minutes", "Make something without judging it", HabitKind.CREATIVE, "Show today's sketch in progress"),
        Habit("music", "Practice an instrument", "One focused practice block", HabitKind.CREATIVE, "Show your instrument and practice space"),
        Habit("meditate", "Meditate for 10 minutes", "Create a little quiet", HabitKind.MINDFULNESS, "Show your calm space after the session"),
        Habit("journal", "Journal", "Write one honest page", HabitKind.MINDFULNESS, "Show the page without private writing visible"),
        Habit("reset", "Reset one space", "Leave one corner better", HabitKind.HOME, "Show the finished space"),
        Habit("cook", "Prepare a meal", "Choose something that supports you", HabitKind.HOME, "Show the meal you prepared"),
        Habit("online-course", "Complete an online lesson", "Finish one intentional lesson", HabitKind.ONLINE, "Show the lesson completion screen"),
        Habit("deep-work", "Complete a focus block", "Work online without distraction", HabitKind.ONLINE, "Share the task screen when complete")
    )
}
