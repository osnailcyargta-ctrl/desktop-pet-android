package com.desktoppet.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_OVERLAY = 1001
        const val REQUEST_IMAGE = 1002
        const val PREFS = "pet_prefs"
        const val KEY_PET_MODE = "pet_mode"
        const val KEY_PET_IMAGE = "pet_image_path"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var petPreview: ImageView
    private lateinit var modeGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        petPreview = findViewById(R.id.pet_preview)
        modeGroup = findViewById(R.id.mode_group)

        loadPetImage()
        loadMode()

        findViewById<Button>(R.id.btn_change_image).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE)
        }

        findViewById<Button>(R.id.btn_launch).setOnClickListener {
            saveMode()
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQUEST_OVERLAY)
            } else {
                startPet()
            }
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopService(Intent(this, PetOverlayService::class.java))
            Toast.makeText(this, "Pet stopped!", Toast.LENGTH_SHORT).show()
        }

        updatePetStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePetStatus()
    }

    private fun updatePetStatus() {
        val running = isServiceRunning()
        findViewById<TextView>(R.id.tv_status).text = if (running) "Pet: Active" else "Pet: Stopped"
        findViewById<Button>(R.id.btn_stop).isEnabled = running
    }

    private fun isServiceRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == PetOverlayService::class.java.name
        }
    }

    private fun loadPetImage() {
        val path = prefs.getString(KEY_PET_IMAGE, null)
        if (path != null && File(path).exists()) {
            petPreview.setImageBitmap(BitmapFactory.decodeFile(path))
        } else {
            petPreview.setImageResource(R.drawable.default_pet)
        }
    }

    private fun loadMode() {
        when (prefs.getString(KEY_PET_MODE, "idle")) {
            "idle" -> modeGroup.check(R.id.radio_idle)
            "walk" -> modeGroup.check(R.id.radio_walk)
            "sleep" -> modeGroup.check(R.id.radio_sleep)
        }
    }

    private fun saveMode() {
        val mode = when (modeGroup.checkedRadioButtonId) {
            R.id.radio_walk -> "walk"
            R.id.radio_sleep -> "sleep"
            else -> "idle"
        }
        prefs.edit().putString(KEY_PET_MODE, mode).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE -> {
                if (resultCode == RESULT_OK && data?.data != null) {
                    savePetImage(data.data!!)
                }
            }
            REQUEST_OVERLAY -> {
                if (Settings.canDrawOverlays(this)) startPet()
            }
        }
    }

    private fun savePetImage(uri: Uri) {
        try {
            val bm = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val side = minOf(bm.width, bm.height)
            val x = (bm.width - side) / 2
            val y = (bm.height - side) / 2
            val cropped = Bitmap.createBitmap(bm, x, y, side, side)
            val scaled = Bitmap.createScaledBitmap(cropped, 512, 512, true)
            val file = File(filesDir, "custom_pet.jpg")
            FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            prefs.edit().putString(KEY_PET_IMAGE, file.absolutePath).apply()
            petPreview.setImageBitmap(scaled)
            Toast.makeText(this, "Pet image updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPet() {
        val intent = Intent(this, PetOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        Toast.makeText(this, "Pet launched!", Toast.LENGTH_SHORT).show()
        updatePetStatus()
    }
}
