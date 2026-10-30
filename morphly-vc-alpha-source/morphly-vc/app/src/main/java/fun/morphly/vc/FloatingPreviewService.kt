package fun.morphly.vc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.core.app.NotificationCompat

/** A legitimate movable TYPE_APPLICATION_OVERLAY video window.
 * It does not register a camera device, hook Camera2, or modify another app's camera output.
 */
class FloatingPreviewService : Service() {
    companion object {
        const val KEY_URI = "video_uri"
        private const val CHANNEL = "morphly_preview"
        private const val NOTIFICATION_ID = 2401
    }
    private lateinit var wm: WindowManager
    private var container: FrameLayout? = null
    private var player: VideoView? = null

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        val raw = intent?.getStringExtra(KEY_URI)
        if (raw.isNullOrBlank()) { stopSelf(); return START_NOT_STICKY }
        ensureForeground()
        createWindow(Uri.parse(raw))
        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Morphly mini preview", NotificationManager.IMPORTANCE_LOW))
        val home = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Morphly VC")
            .setContentText("Floating local video preview is active")
            .setContentIntent(home)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else startForeground(NOTIFICATION_ID, notification)
    }

    private fun createWindow(uri: Uri) {
        removeWindow()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width = (resources.displayMetrics.density * 210).toInt()
        val height = (resources.displayMetrics.density * 286).toInt()
        val params = WindowManager.LayoutParams(
            width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 160 }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(34, 28, 34))
            elevation = resources.displayMetrics.density * 10
        }
        val video = VideoView(this).apply {
            setVideoURI(uri)
            setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); start() }
            setOnErrorListener { _, _, _ -> stopSelf(); true }
        }
        root.addView(video, FrameLayout.LayoutParams(-1, -1))
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10.dp, 0, 6.dp, 0)
            setBackgroundColor(Color.argb(225, 226, 60, 49))
        }
        val title = TextView(this).apply {
            text = "≡  MORPHLY VC"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val expand = TextView(this).apply {
            text = "⤢"
            setTextColor(Color.WHITE)
            textSize = 21f
            setPadding(12.dp, 5.dp, 12.dp, 5.dp)
            setOnClickListener {
                startActivity(Intent(this@FloatingPreviewService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                stopSelf()
            }
        }
        val close = TextView(this).apply {
            text = "×"
            setTextColor(Color.WHITE)
            textSize = 25f
            setPadding(12.dp, 5.dp, 8.dp, 5.dp)
            setOnClickListener { stopSelf() }
        }
        header.addView(expand)
        header.addView(close)
        root.addView(header, FrameLayout.LayoutParams(-1, 42.dp, Gravity.TOP))
        var initialX = 0; var initialY = 0; var touchX = 0f; var touchY = 0f
        title.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; touchX = event.rawX; touchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(root, params)
                    true
                }
                else -> false
            }
        }
        wm.addView(root, params)
        container = root
        player = video
        video.start()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    private fun removeWindow() {
        player?.stopPlayback()
        player = null
        container?.let { root -> if (::wm.isInitialized) runCatching { wm.removeView(root) } }
        container = null
    }
    override fun onDestroy() {
        removeWindow()
        super.onDestroy()
    }
}
