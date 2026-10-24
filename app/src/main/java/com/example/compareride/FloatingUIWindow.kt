package com.example.compareride

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class FloatingUIWindow : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the floating layout you created earlier
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        // Set layout parameters for the overlay
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20 // Starting X position
            y = 200 // Starting Y position
        }

        windowManager.addView(floatingView, params)

        val btnCompare = floatingView.findViewById<Button>(R.id.btnCompare)
        val txtPrices = floatingView.findViewById<TextView>(R.id.txtPrices)

        // When clicked, tell the Accessibility Service to start doing its job
        btnCompare.setOnClickListener {
            txtPrices.text = "Starting automation..."
            val intent = Intent("START_COMPARISON_SEQUENCE")
            sendBroadcast(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}