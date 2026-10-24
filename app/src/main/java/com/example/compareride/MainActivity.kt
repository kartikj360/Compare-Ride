package com.example.compareride

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Button to request overlay permission
        findViewById<Button>(R.id.btnOverlayPerm).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay Permission Already Granted", Toast.LENGTH_SHORT).show()
            }
        }

        // Button to open accessibility settings
        findViewById<Button>(R.id.btnAccessPerm).setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // Button to launch the floating UI
        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingUIWindow::class.java))
                finish() // Close main activity so just the bubble remains
            } else {
                Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}