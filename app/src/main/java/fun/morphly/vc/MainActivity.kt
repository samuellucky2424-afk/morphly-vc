package `fun`.morphly.vc

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import `fun`.morphly.vc.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {
    private lateinit var ui: ActivityMainBinding
    private var videoUri: Uri? = null
    private var faceUri: Uri? = null
    private var engine: Engine = Engine.XMAX
    private val prefs by lazy { getSharedPreferences("morphly_vc", MODE_PRIVATE) }
    private val probe by lazy { CameraCapabilityProbe(this) }

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            saveReadPermission(uri)
            videoUri = uri
            prefs.edit().putString("video_uri", uri.toString()).apply()
            displayVideo(uri)
        }
    }
    private val pickFace = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            saveReadPermission(uri)
            faceUri = uri
            prefs.edit().putString("face_uri", uri.toString()).apply()
            ui.facePreview.setImageURI(uri)
            ui.faceStatus.text = "Reference selected: ${displayName(uri)}"
            ui.statusText.text = "Face image stored locally. Upload to provider is not yet wired."
        }
    }
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) showCameraDiagnostics() else showMessage("Camera permission denied. Only local-video preview is available.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        val qualities = listOf("Balanced · 480p", "HD · 720p", "Best · 1080p (experimental)")
        ui.spinnerQuality.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, qualities)
        ui.buttonXmax.setOnClickListener { engine = Engine.XMAX; refreshEngineButtons() }
        ui.buttonVidu.setOnClickListener { engine = Engine.VIDU; refreshEngineButtons() }
        ui.buttonUploadVideo.setOnClickListener { pickVideo.launch(arrayOf("video/*")) }
        ui.buttonUploadFace.setOnClickListener { pickFace.launch(arrayOf("image/*")) }
        ui.buttonMiniWindow.setOnClickListener { launchFloatingPreview() }
        ui.buttonConnect.setOnClickListener { explainProviderIntegration() }
        ui.buttonDiagnostics.setOnClickListener {
            if (!probe.permissionGranted()) requestCamera.launch(Manifest.permission.CAMERA)
            else showCameraDiagnostics()
        }
        refreshEngineButtons()
        prefs.getString("video_uri", null)?.let { raw ->
            runCatching { Uri.parse(raw) }.onSuccess {
                videoUri = it
                displayVideo(it)
            }.onFailure { videoUri = null }
        }
        prefs.getString("face_uri", null)?.let { raw ->
            runCatching { Uri.parse(raw) }.onSuccess {
                faceUri = it
                ui.facePreview.setImageURI(it)
                ui.faceStatus.text = "Reference selected: ${displayName(it)}"
            }
        }
    }

    private fun refreshEngineButtons() {
        ui.buttonXmax.setBackgroundColor(getColor(if (engine == Engine.XMAX) R.color.brand else R.color.outline))
        ui.buttonVidu.setBackgroundColor(getColor(if (engine == Engine.VIDU) R.color.brand else R.color.outline))
        ui.buttonXmax.setTextColor(getColor(if (engine == Engine.XMAX) R.color.white else R.color.ink))
        ui.buttonVidu.setTextColor(getColor(if (engine == Engine.VIDU) R.color.white else R.color.ink))
        ui.statusText.text = "Selected ${engine.label}. Local-video preview is available; live provider SDK not connected."
    }

    private fun displayVideo(uri: Uri) {
        ui.previewEmpty.visibility = View.GONE
        ui.selectedVideoText.text = displayName(uri)
        ui.statusText.text = "Local video loaded. Use Pop-out window for a floating preview."
        ui.videoPreview.setOnErrorListener { _, what, extra ->
            ui.statusText.text = "Cannot decode video on this device (MediaPlayer $what/$extra). Try H.264 MP4."
            true
        }
        ui.videoPreview.setVideoURI(uri)
        ui.videoPreview.setOnPreparedListener { player ->
            player.isLooping = true
            player.setVolume(0f, 0f)
            ui.videoPreview.start()
        }
    }

    private fun launchFloatingPreview() {
        val uri = videoUri ?: run { showMessage("Choose a video before opening the mini-window."); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Floating-window permission")
                .setMessage("Android requires Display over other apps to show a mini video window. This does NOT grant camera interception.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                }.show()
            return
        }
        val intent = Intent(this, FloatingPreviewService::class.java).apply {
            putExtra(FloatingPreviewService.KEY_URI, uri.toString())
        }
        try {
            ContextCompat.startForegroundService(this, intent)
            ui.statusText.text = "Mini-window requested. It is a floating video player, not an Android camera override."
        } catch (e: Exception) {
            ui.statusText.text = "Cannot start overlay: ${e.javaClass.simpleName} ${e.message}"
        }
    }

    private fun explainProviderIntegration() {
        val quality = ui.spinnerQuality.selectedItem.toString()
        AlertDialog.Builder(this)
            .setTitle("${engine.label} · $quality")
            .setMessage("Provider integration is not yet active in this isolated prototype. " +
                "Your older Morphlysm2 repository already contains XmaxRealtimeBridge and " +
                "ViduRealtimeBridge. Those modules require their Android SDKs and your secure " +
                "backend's short-lived session tokens. No provider request was made and no credits were charged. " +
                "Local upload + floating preview can be tested right now.")
            .setPositiveButton("OK", null)
            .show()
        ui.statusText.text = "${engine.label}: pending SDK/backend integration; no connection attempted."
    }

    private fun showCameraDiagnostics() {
        val diagnostic = probe.reportHardware()
        AlertDialog.Builder(this)
            .setTitle("Non-root Camera2 diagnostic")
            .setMessage("$diagnostic\n\nRun physical-camera open/close test? This does not modify the OS camera feed.")
            .setNegativeButton("Close", null)
            .setPositiveButton("Test camera") { _, _ ->
                probe.tryOpenCamera { result ->
                    runOnUiThread {
                        ui.statusText.text = result
                        AlertDialog.Builder(this).setTitle("Camera2 result").setMessage(result)
                            .setPositiveButton("OK", null).show()
                    }
                }
            }.show()
    }

    private fun saveReadPermission(uri: Uri) {
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { /* Still usable during the current app session. */ }
    }
    private fun displayName(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment.orEmpty()
        } catch (_: Exception) { uri.lastPathSegment.orEmpty() }
    }
    private fun showMessage(text: String) { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    override fun onPause() { super.onPause(); ui.videoPreview.pause() }
    override fun onResume() { super.onResume(); if (videoUri != null) ui.videoPreview.start() }
    override fun onDestroy() { ui.videoPreview.stopPlayback(); super.onDestroy() }
}
