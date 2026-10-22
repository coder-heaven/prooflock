package app.prooflock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager

internal class ScreenShareService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notification()
        if (Build.VERSION.SDK_INT >= 29) startForeground(4201, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(4201, notification)
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (projection == null && intent != null) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, data)
            val metrics = DisplayMetrics()
            getSystemService(WindowManager::class.java).defaultDisplay.getMetrics(metrics)
            reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, android.graphics.PixelFormat.RGBA_8888, 2)
            projection?.createVirtualDisplay("ProofLock online task", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null)
        }
        return START_STICKY
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("prooflock_screen_share", "Online task sharing", NotificationManager.IMPORTANCE_LOW))
        return androidx.core.app.NotificationCompat.Builder(this, "prooflock_screen_share")
            .setSmallIcon(R.drawable.prooflock_icon).setContentTitle("ProofLock screen sharing is active")
            .setContentText("Your selected online task can see this session. Stop sharing when finished.")
            .setOngoing(true).build()
    }

    override fun onDestroy() {
        reader?.close(); reader = null; projection?.stop(); projection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "app.prooflock.STOP_SCREEN_SHARE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "projection_data"
    }
}
