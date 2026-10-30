package `fun`.morphly.vc

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity

/** Brand splash, based on the supplied Morphly phone screenshot. */
class SplashActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goHome = Runnable {
        if (!isFinishing && !isDestroyed) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed(goHome, 650L)
    }
    override fun onDestroy() {
        handler.removeCallbacks(goHome)
        super.onDestroy()
    }
}
