package app.prooflock

import android.content.Context
import android.content.Intent

internal object AppCatalog {
    private val distractionWords = listOf("social", "video", "stream", "game", "chat", "reddit", "instagram", "facebook", "tiktok", "youtube", "netflix", "twitch", "snapchat", "twitter", "x ")

    fun launchable(context: Context): List<AppEntry> {
        val packageManager = context.packageManager
        val selected = context.getSharedPreferences("prooflock", Context.MODE_PRIVATE).getStringSet("selected_packages", emptySet()).orEmpty()
        return packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .mapNotNull { info ->
                val packageName = info.activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                val label = info.loadLabel(packageManager).toString()
                AppEntry(packageName, label, isDistracting(packageName, label), packageName in selected)
            }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<AppEntry> { it.distracting }.thenBy { it.label.lowercase() })
    }

    private fun isDistracting(packageName: String, label: String): Boolean {
        val haystack = "$packageName $label".lowercase()
        return distractionWords.any(haystack::contains)
    }
}
