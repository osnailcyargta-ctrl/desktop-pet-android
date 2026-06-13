package com.desktoppet.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import java.io.File

class PetOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var petView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var handler: Handler

    private var petMode = "idle"
    private var walkDir = 1
    private var walkRunnable: Runnable? = null
    private var volumeRunnable: Runnable? = null
    private var bounceUp = true

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        startForeground(1, buildNotification())
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        petMode = prefs.getString(MainActivity.KEY_PET_MODE, "idle") ?: "idle"
        updateBehavior()
        return START_STICKY
    }

    private fun createOverlay() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        petView = inflater.inflate(R.layout.pet_overlay, null)

        params = WindowManager.LayoutParams(
            180, 220,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100; y = 300
        }

        windowManager.addView(petView, params)
        loadPetImage()
        setupDrag()
        setupCloseButton()
        updateBehavior()
    }

    private fun loadPetImage() {
        val prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        val path = prefs.getString(MainActivity.KEY_PET_IMAGE, null)
        val imgView = petView.findViewById<ImageView>(R.id.pet_image)
        if (path != null && File(path).exists()) {
            imgView.setImageBitmap(android.graphics.BitmapFactory.decodeFile(path))
        } else {
            imgView.setImageResource(R.drawable.default_pet)
        }
    }

    private fun setupDrag() {
        val petImg = petView.findViewById<View>(R.id.pet_image)
        var lastX = 0f; var lastY = 0f
        petImg.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x += (event.rawX - lastX).toInt()
                    params.y += (event.rawY - lastY).toInt()
                    windowManager.updateViewLayout(petView, params)
                    lastX = event.rawX; lastY = event.rawY; true
                }
                else -> false
            }
        }
    }

    private fun setupCloseButton() {
        petView.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            stopSelf()
        }
    }

    private fun updateBehavior() {
        walkRunnable?.let { handler.removeCallbacks(it) }
        volumeRunnable?.let { handler.removeCallbacks(it) }

        val overlay = petView.findViewById<View>(R.id.sleep_overlay)
        overlay?.visibility = if (petMode == "sleep") View.VISIBLE else View.GONE

        when (petMode) {
            "walk" -> startWalking()
            "sleep" -> startSleepMonitor()
        }
    }

    private fun startWalking() {
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        display.getMetrics(metrics)
        val screenW = metrics.widthPixels

        walkRunnable = object : Runnable {
            override fun run() {
                params.x += walkDir * 4
                if (params.x <= 0) { params.x = 0; walkDir = 1 }
                if (params.x >= screenW - 180) { params.x = screenW - 180; walkDir = -1 }
                if (bounceUp) params.y -= 2 else params.y += 2
                bounceUp = !bounceUp
                windowManager.updateViewLayout(petView, params)
                handler.postDelayed(this, 30)
            }
        }
        handler.post(walkRunnable!!)
    }

    private fun startSleepMonitor() {
        volumeRunnable = object : Runnable {
            override fun run() {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if ((cur.toFloat() / max) * 100 > 30f) {
                    handler.postDelayed({
                        petMode = "idle"
                        updateBehavior()
                    }, 4000)
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(volumeRunnable!!)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("pet_ch", "Desktop Pet", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, "pet_ch")
        .setContentTitle("Desktop Pet")
        .setContentText("Your pet is running")
        .setSmallIcon(android.R.drawable.ic_menu_myplaces)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        walkRunnable?.let { handler.removeCallbacks(it) }
        volumeRunnable?.let { handler.removeCallbacks(it) }
        if (::petView.isInitialized) windowManager.removeView(petView)
    }
}
