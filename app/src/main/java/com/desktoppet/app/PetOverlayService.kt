package com.desktoppet.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.AudioManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

class PetOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var petView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var handler: Handler

    private var petState = "idle" // idle, eating, drinking, sleeping
    private var isSleeping = false
    private var volumeCheckRunnable: Runnable? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        handler = Handler(Looper.getMainLooper())

        createNotificationChannel()
        startForeground(1, buildNotification())
        createPetOverlay()
        startVolumeCheck()
    }

    private fun createPetOverlay() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        petView = inflater.inflate(R.layout.pet_overlay, null)

        params = WindowManager.LayoutParams(
            200, 200,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        windowManager.addView(petView, params)
        setupDrag()
        setupButtons()
        updatePetVisual()
    }

    private fun setupDrag() {
        val petBody = petView.findViewById<View>(R.id.pet_body)
        var lastX = 0f
        var lastY = 0f

        petBody.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()
                    params.x += dx
                    params.y += dy
                    windowManager.updateViewLayout(petView, params)
                    lastX = event.rawX
                    lastY = event.rawY
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButtons() {
        petView.findViewById<View>(R.id.btn_feed)?.setOnClickListener {
            if (!isSleeping) {
                petState = "eating"
                updatePetVisual()
                handler.postDelayed({ petState = "idle"; updatePetVisual() }, 2000)
            }
        }
        petView.findViewById<View>(R.id.btn_drink)?.setOnClickListener {
            if (!isSleeping) {
                petState = "drinking"
                updatePetVisual()
                handler.postDelayed({ petState = "idle"; updatePetVisual() }, 2000)
            }
        }
        petView.findViewById<View>(R.id.btn_sleep)?.setOnClickListener {
            isSleeping = !isSleeping
            petState = if (isSleeping) "sleeping" else "idle"
            updatePetVisual()
        }
    }

    private fun updatePetVisual() {
        val petBody = petView.findViewById<PetDrawView>(R.id.pet_draw)
        petBody?.setState(petState)
        petBody?.invalidate()
    }

    private fun startVolumeCheck() {
        volumeCheckRunnable = object : Runnable {
            override fun run() {
                if (isSleeping) {
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val pct = (curVol.toFloat() / maxVol) * 100
                    if (pct > 30f) {
                        handler.postDelayed({
                            isSleeping = false
                            petState = "idle"
                            updatePetVisual()
                        }, 4000)
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(volumeCheckRunnable!!)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("pet_channel", "Desktop Pet", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "pet_channel")
            .setContentTitle("Desktop Pet")
            .setContentText("Your pet is on screen")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeCheckRunnable?.let { handler.removeCallbacks(it) }
        if (::petView.isInitialized) windowManager.removeView(petView)
    }
}
