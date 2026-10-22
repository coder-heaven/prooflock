package app.prooflock

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

internal object ProofLockBackend {
    fun verify(habit: Habit, bitmap: Bitmap): ProofResult {
        val endpoint = BuildConfig.PROOFLOCK_API_URL
        if (endpoint.isBlank()) return ProofResult("pass", "Local prototype mode accepted the fresh camera capture", "local", Instant.now().toString())
        return runCatching {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
            val connection = URL("$endpoint/v1/verify").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val body = "{" +
                "\"habitId\":\"${escape(habit.id)}\",\"habitTitle\":\"${escape(habit.title)}\"," +
                "\"proofPrompt\":\"${escape(habit.proofPrompt)}\",\"capturedAt\":\"${Instant.now()}\"," +
                "\"imageBase64\":\"${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}\"}"
            connection.outputStream.use { it.write(body.toByteArray()) }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val status = Regex("\"status\"\\s*:\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1) ?: "uncertain"
            val reason = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"").find(response)?.groupValues?.get(1) ?: "Backend verification completed"
            ProofResult(status, reason, "backend", Instant.now().toString())
        }.getOrElse { ProofResult("unavailable", "AI verification is temporarily unavailable. Keep your phone protected and try again.", "backend", Instant.now().toString()) }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
